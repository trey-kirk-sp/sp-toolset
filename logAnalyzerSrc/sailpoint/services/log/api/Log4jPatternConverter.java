package sailpoint.services.log.api;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Converts the layout pattern to Pattern tokens.<br>
 * <br>
 * Example:<br>
 * %d{ABSOLUTE} %5p %c{1}:%L - %m%n<br>
 * <br>
 * Becomes the Pattern(s):<br>
 * (\d\d:\d\d:\d\d,\d\d\d)\Q \E\s*(INFO|TRACE|DEBUG|WARN|ERROR)\Q \E(\?|[a-zA-Z0-9_$]+\.?){1,1}\Q:\E(\?|[0-9]+)\Q - \E(.*)($)<br>
 * @see http://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/PatternLayout.html
 * @author trey.kirk
 *
 */
public class Log4jPatternConverter {

    private static Log _log = LogFactory.getLog(Log4jPatternConverter.class);

    /**
     * Enumerates the various Log4j token identifiers
     * @author trey.kirk
     *
     */
    public enum Identifier {
        CATEGORY ('c'),
        CLASS_NAME ('C'),
        DATE ('d'),
        FILE_NAME ('F'),
        LOCATION ('l'),
        LINE_NUMBER ('L'),
        MESSAGE ('m'),
        METHOD ('M'),
        LINE_SEP ('n'),
        PRIORITY ('p'),
        MILLISECONDS_CALLED ('r'),
        THREAD ('t'),
        NDC ('x'),
        MDC ('X'),
        PERCENT ('%');

        private char _identifier;

        Identifier(char ident) {
            _identifier = ident;
        }

        /**
         * Returns the character used in the PatternLayout
         * @return
         */
        public char getIdentifier() {
            return _identifier;
        }

    }

    /*
     * Utility iterator to tokenize the LayoutPattern.  This assumes the
     * standard delimiter of '%'.  TODO: allow custom delimiter
     */
    private class PatternIterator implements Iterator<String> {

        private int _charPos;
        private String _pattern;
        private char[] _patternArry;

        /*
         * Constructor needs LayoutPattern
         */
        private PatternIterator(String pattern) {
            _log.trace("Init, pattern: " + pattern);
            _pattern = pattern;
            _patternArry = pattern.toCharArray();
            _charPos = 0;
        }

        /**
         * Returns 'true' if there's at least 1 unanalyzed character left in
         * our pattern.  
         */
        public boolean hasNext() {
            if (_charPos < _patternArry.length - 1) {
                return true;
            } else {
                return false;
            }
        }

        /**
         * Returns the next token as delimited by '%' or end of line
         */
        public String next() {
            boolean found = false;
            boolean firstPass = true;
            int start = _charPos;
            while (!found) {
                _charPos++;
                if (_charPos >= _patternArry.length) {
                    found = true;
                } else {
                    char c = _patternArry[_charPos];
                    if (c == '%' && !firstPass) {
                        found = true;
                    }
                }
                firstPass = false;
            }
            String next = _pattern.substring(start + 1, _charPos);
            _log.trace("Next token: " + next);
            return next; // clips the leading '%'
        }

        /**
         * throws UnsupportedOperationException
         */
        public void remove() {
            UnsupportedOperationException e = new UnsupportedOperationException();
            _log.error("Remove not allowed", e);
            throw e;
        }
    }

    /*
     * Utility class to combine a Pattern with a group identifier (integer)
     */
    private class GroupedPattern {
        private Pattern _pattern;
        private int _group;

        GroupedPattern(Pattern p, int group) {
            _log.trace("GroupedPattern, Pattern: " + p + ", group: " + group);
            _group = group;
            _pattern = p;
        }

        int getGroup() {
            return _group;
        }

        Pattern getPattern() {
            return _pattern;
        }

        @Override
        public String toString() {
            return "[" + _group + ", " + _pattern + "]";
        }
    }

    private static String ABSOLUTE = "ABSOLUTE";
    private static String ABSOLUTE_VALUE = "HH:mm:ss,SSS";
    private static String ISO8601 = "ISO8601";
    private static String ISO8601_VALUE = "yyyy-MM-dd HH:mm:ss,SSS";
    private static final String LOG4J_IDENTIFIERS = "cCdFlLmMnprtxX";
    private static final int PATTERN_FLAGS = Pattern.DOTALL;
    public static final String PRIORITY_ERROR = "ERROR";
    public static final String PRIORITY_WARN = "WARN";
    public static final String PRIORITY_DEBUG = "DEBUG";
    public static final String PRIORITY_INFO = "INFO";
    public static final String PRIORITY_TRACE = "TRACE";

    private String _layoutPattern; // The layout pattern we're parsing
    private Pattern _linePattern; // the layout pattern converted to a full regex
    private Map<Character, GroupedPattern> _patterns; // each Pattern in map format
    private String _simpleDateFormat;
    private Matcher _eventMatcher;
    private String _currentEvent;

    /**
     * Standard constructor, requires a LayoutPattern as declared for Log4j
     * @param layoutPattern
     */
    public Log4jPatternConverter (String layoutPattern) {
        _layoutPattern = layoutPattern;
        _patterns = new HashMap<Character, GroupedPattern>();
        parsePattern();
    }

    /*
     * Appends our line pattern with the provided Pattern
     */
    private void addPatternToLine(Pattern pattern) {
        _log.trace("addPatternToLine, original: " + _linePattern);
        if (_linePattern == null) {
            _linePattern = Pattern.compile(pattern.pattern(), PATTERN_FLAGS);
        } else {
            _linePattern = Pattern.compile(_linePattern.pattern() + pattern.pattern(), PATTERN_FLAGS);
        }
        _log.trace("now: " + _linePattern);
    }


    /*
     * Converts the input values into a Pattern.  Each Pattern is wrapped in ( ) to group them
     */
    private Pattern buildPattern(char identifier, String qualifier,
            Integer indention, Integer truncation, String trailing, String token) {

        StringBuffer regExBuff = new StringBuffer();

        /*
         * TODO: this implementation isn't quite there, come back to it
         */
        if (indention != null && indention > 0) {
            regExBuff.append("\\s*");
        }

        // identifier tells us what kind of data to look for, qualifier tweaks it
        // send them both for conversion
        regExBuff.append(identifierRegEx(identifier, qualifier, trailing));

        /*
         * TODO: this implementation isn't quite there, come back to it
         */
        if (indention != null && indention < 0) {
            regExBuff.append("\\s*");
        }
        Pattern pattern = Pattern.compile(regExBuff.toString());
        _log.debug("buildPattern: " + pattern);
        return pattern;		
    }

    /**
     * Extracts the Date pattern as a Date.  While no other token is parsed in this class, parsing
     * the date takes some extra work that's well suited for the {@link Log4jPatternConverter} class
     */
    public Date parseDate() {
        String dateString = parseToken(Identifier.DATE.getIdentifier());
        if (dateString != null && _simpleDateFormat != null) {
            SimpleDateFormat formatter = new SimpleDateFormat(_simpleDateFormat);
            try {
                Date d = formatter.parse(dateString);
                _log.trace("Parsed date: " + d);
                return d;
            } catch (ParseException e) {
                _log.error("Error parsing date", e);
                e.printStackTrace();
            }
        }
        _log.trace("Parsed date: (null)");
        return null;
    }

    /*
     * A converter within a converter.  Creates a RegEx based on the SimpleDateFormat the 'd' token uses
     */
    private String convertSimpleDateFormat(String qualifier) {

        // There are two pre-defined formats, convert them
        if (ISO8601.equals(qualifier)) {
            _simpleDateFormat = ISO8601_VALUE;
            return convertSimpleDateFormat(ISO8601_VALUE);
        } else if (ABSOLUTE.equals(qualifier)) {
            _simpleDateFormat = ABSOLUTE_VALUE;
            return convertSimpleDateFormat(ABSOLUTE_VALUE);
        }
        _simpleDateFormat = qualifier;
        // example: dd MMMMM yyyy HH:mm:ss,SSS
        // need to conver the format to a regex.  this'll be fun
        StringBuffer regex = new StringBuffer();
        // iterate over all characters one at a time and convert them to their
        // regex equivalent.
        for (int i = 0; i < qualifier.length(); i++) {
            char c = qualifier.charAt(i);
            boolean inGenericString = false;
            switch (c) {
            case 'G':
                // G is 'text' of Era Desginator (AD / BC)
                regex.append("[AB][CD]"); // Ok, so this allows 'AC' and 'BD'. Maybe instead us (AD)|(BC)
                inGenericString = false;
                break;
            case 'y':
            case 'w':
            case 'D':
            case 'd':
            case 'F':
            case 'H':
            case 'k':
            case 'K':
            case 'h':
            case 'm':
            case 's':
            case 'S':
                regex.append("\\d");
                inGenericString = false;
                break;
            case 'M':
            case 'E':
            case 'z':
            case 'Z':
                if (!inGenericString) { 
                    regex.append(".*"); // 0 or more of any character
                } 
                inGenericString = true; // "bunches" things like 'MMMM' to one '.*'
                break;
            default:
                // literal text
                regex.append(c);
            }
        }

        String simpleDateFormat = "(" + regex.toString() + ")";
        _log.debug("simpleDateFormat: " + simpleDateFormat);
        return simpleDateFormat;
    }

    /*
     * Creates a regex string for values that could be in the form of 'val.val2.val3'
     */
    private String dotPathRegEx(String qualifier) {
        // the qualifier indicates the 'right most items in a dot path to print'
        //
        // For me, all that means is the number of items in the path
        StringBuffer regExBuff = new StringBuffer();
        regExBuff.append("(");
        if (qualifier != null) {
            // full path could be provided
            int qualInt = Integer.valueOf(qualifier);
            for (int i = 0; i < qualInt; i++) {
                regExBuff.append("[a-zA-Z0-9_$?]+\\.?");
            }
        } else {
            regExBuff.append("[a-zA-Z0-9_$?]+\\.?");
        }
        regExBuff.append(")");
        String dotPathRegEx = regExBuff.toString();
        _log.debug("dotPathRegEx: " + dotPathRegEx);
        return dotPathRegEx;
    }

    /*
     * Extracts the token character
     */
    private char extractIdentifier(String token) {

        Pattern p = Pattern.compile("-?[0-9]*([" + LOG4J_IDENTIFIERS + "]).*");
        Matcher m = p.matcher(token);
        if (m.find()) {
            // the last character is all I want
            return m.group(1).charAt(0);
        } else {
            // No identifier, this is bad, right?
            // Wonder what log4j does when it gets %[invalid Char]?
            _log.warn("No log4j identifier matches requested token: " + token);
            return 0;
        }
    }

    /*
     * Extracts the justification format
     */
    private Integer extractIndention(String token) {
        if (token == null) {
            return null;
        }
        // always before identifier
        char identifier = extractIdentifier(token);

        Pattern p = Pattern.compile("(-?[0-9]+)\\.?[0-9]*" + identifier + ".*");
        Matcher m = p.matcher(token);
        if (m.find()) {
            return Integer.valueOf(m.group(1));
        }

        return null;
    }

    /*
     * Extracts the value specified in the { } characters.
     * Note: java docs for Log4j LayoutPattern has the following for 'd':
     * 
     * 'If no date format specifier is given then ISO8601 format is assumed.'
     * Shall we also assume? 
     */
    private String extractQualifier(String token) {
        if (token == null) {
            return null;
        } else if (token.contains("}")) {
            Pattern p = Pattern.compile(".*\\{(.*)\\}.*");
            Matcher m = p.matcher(token);
            m.find();
            String qual = m.group(1);
            return qual;
        }

        return null;
    }

    /*
     * The trailing data in the token is the 'trailing data', der
     */
    private String extractTrailing(String token) {
        // trailing characters always show up after anchoring } OR identifier
        if (token == null || token.equals("")) {
            return null;
        }

        // Find the identifier and go
        // from identifier pos + 1 to token.length
        char identifier = extractIdentifier(token);
        String qual = extractQualifier(token);
        StringBuffer qualBuff = new StringBuffer();
        if (qual != null) {
            qualBuff.append("\\{?" + qual + "\\}?");
        }
        Pattern p = Pattern.compile("-?[0-9]*\\.?[0-9]*" + identifier + qualBuff.toString() + "(.*)");
        Matcher m = p.matcher(token);
        if (m.find()) {
            String result = m.group(1);
            if ("".equals(result)) {
                return null;
            } else {
                return result;
            }
        }

        // in practice, we never arrive here
        return null;
    }

    /*
     * Determines the truncation format
     */
    private Integer extractTruncation(String token) {
        if (token == null) {
            return null;
        }
        char identifier = extractIdentifier(token);

        Pattern p = Pattern.compile("-?[0-9]*\\.([0-9]+)" + identifier + ".*");
        Matcher m = p.matcher(token);
        if (m.find()) {
            return Integer.valueOf(m.group(1));
        }

        return null;
    }

    /*
     * "All Finding" regular expression.  It'd be nice if I could use this as little as possible and be
     * more specific whenever possible
     */
    private String genericTextRegEx() {
        return "(.*)";
    }

    /**
     * Returns the Pattern that will match a full log event line.  The setLogEvent will apply the current
     * logEvent, calling on the linePattern to do one match.  Thereafter, each token will be extracted by
     * asking for the respective capture group.
     * @return
     */
    public Pattern getLinePattern() {
        return _linePattern;
    }

    /**
     * Returns the pattern associated to the identifier.  Best not to use this
     * @param identifier
     * @return
     */
    private Pattern getPattern(char identifier) {
        GroupedPattern groupedPattern = _patterns.get(identifier);
        if (groupedPattern != null) {
            return groupedPattern.getPattern();
        } else {
            return null;
        }
    }

    /*
     * Creates a Pattern based on the format represented by the identifier
     * char and modified by the qualifier and trailing strings
     */
    private String identifierRegEx(char identifier, String qualifier,
            String trailing) {
        _log.trace("identifierRegEx: identifier = " + identifier + ", qualifier = " + qualifier + ", trailing = " + trailing);
        // for each identifier, the qualifier works a little differently
        // switch off of identifier and use helper methods
        String regex = null;
        if (trailing == null) {
            trailing = "";
        } else {
            trailing = Pattern.quote(trailing);
        }
        switch (identifier) {
        case 'c': // package name
        case 'C': // class name (qualified)
        case 'F': // filename
            regex = dotPathRegEx(qualifier);
            break;
        case 'd': // date
            regex = convertSimpleDateFormat(qualifier);
            break;
        case 'l': // fully qualified method name followed by the callers source, file name, (line numbers)
            // TODO: need to see an example of this
        case 'L': // line number from where the loggin request was issues
        case 'r': // number of milliseconds
            regex = numberRexEx();
            break;

        case 'm': // The message
            regex = genericTextRegEx();
            break;

        case 't': // name of the thread
        case 'M': // used to output the method name where the logging request was issued
        case 'x': // the NDC associated with the thread
            // TODO: example
        case 'X': // MDC associated with thread (qualified)
            // TODO: example
            regex = genericTextRegEx();
            break;

        case 'p': // priority of logging event
            regex = priorityRegEx();
            break;

        case 'n': // line separator
            regex = "($)";
            break;

        case '%': // just a percent
            regex = "(%)";
            break;
        }

        String identifierRegEx = regex + trailing;
        _log.trace("identifierRegEx: " + identifierRegEx);
        return identifierRegEx;
    }

    private String numberRexEx() {
        return "(\\?|[0-9]+)";
    }

    /*
     * Main working method.  Takes the layout pattern, finds the
     * relevant identifiers and converts them into individual Patterns
     */
    private void parsePattern() {
        _log.debug("Entering parsePattern");
        PatternIterator it = new PatternIterator(_layoutPattern);
        int group = 0;
        while (it.hasNext()){
            //for (String token : it) {
            String token = it.next();
            _log.debug("next token: " + token);
            group++;
            Integer indention = extractIndention(token);
            Integer truncation = extractTruncation(token);
            char identifier = extractIdentifier(token);
            String qualifier = extractQualifier(token);
            // everything else is (probably) trailing characters
            String trailing = extractTrailing(token);
            Pattern pattern = buildPattern (identifier, qualifier, indention, truncation, trailing, token);
            _log.debug("token pattern: " + pattern);
            GroupedPattern gp = new GroupedPattern(pattern, group);
            _patterns.put(identifier, gp);
            addPatternToLine(pattern);
            indention = null; // reset
            identifier = 0;
        }
        _log.debug("Patter map: " + _patterns);
    }

    /*
     * Regex to find the various priority log levels
     */
    private String priorityRegEx() {
        // need to make this a little more solid to accomodate the qualifier
        return "\\s*(INFO|TRACE|DEBUG|WARN|ERROR)";
    }

    /**
     * Returns the token string from the log event based on the input {@link Identifier}
     * @param identifier
     * @return
     */
    public String parseToken (Identifier identifier) {
        return this.parseToken(identifier.getIdentifier());
    }

    /**
     * Sets the current log event.
     * @param logEvent
     */
    public void setLogEvent (String logEvent) {
        _log.trace("setLogEvent: " + logEvent);
        // multi-line values are difficult.  So replace all newline characters with a known marker.
        // Then, upon parseToken, re-add \n
        //_currentEvent = logEvent.replace("\n", NEWLINE_SEQUENCE);
        _currentEvent = logEvent;
        _eventMatcher = _linePattern.matcher(_currentEvent);
        _eventMatcher.matches();
    }

    /**
     * Returns the current log event.
     * @return
     */
    public String getLogEvent () {
        return _currentEvent;
    }

    /*
     * TODO: To get back to
     */
    private boolean isTokenDefined (Identifier identifier) {
        return false;
    }

    public String parseToken (char identifier) {
        GroupedPattern gp = _patterns.get(identifier);
        if (gp != null) {
            try {
                String value = _eventMatcher.group(gp.getGroup());
                return value; //.replace(NEWLINE_SEQUENCE, "\n");
            } catch (IllegalStateException e) {
                _log.error("IllegalStateException when parsing token: " + identifier + ", layoutPatter=" + _layoutPattern + 
                        "\nUsually caused by layoutPattern specifying tokens not actually present in log file!", e);
                throw new RuntimeException(e);
            }
        } else {
            return null;
        }
    }

    /**
     * Returns the capture group id for the requested identifier.
     * @param identifier
     * @return
     */
    public int getGroupId (char identifier) {  // who should need this?
        GroupedPattern gp = _patterns.get(identifier);
        if (gp != null) {
            return gp.getGroup();
        } else {
            return -1;
        }
    }
}
