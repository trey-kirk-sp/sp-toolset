package sailpoint.services.log.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import sailpoint.services.log.api.Log4jPatternConverter.Identifier;

/**
 * Implementation of {@link LogAnalyzer} for Log4j logging derived from AspectJ's Tracing Aspect.
 * This aspect injects Enter / Exit tracing for each method allowing one to gain information about
 * the method's inputs, outputs, and timing.  This abstract class offers utility methods that compiles
 * that information.<br>
 * Leverages {@link Log4jPatternConverter} to parse the log event messages.
 * @author trey.kirk
 *
 */
public abstract class AbstractTraceAspectLogAnalyzer implements LogAnalyzer {

    static Log _log = LogFactory.getLog(AbstractTraceAspectLogAnalyzer.class);

    // If we have thread inforamation, this map is used to keep them organized
    private Map<String,Stack<String[]>> _threads;
    private int _propNameMaxLength = 10;

    /**
     * Default thread name when one is not available
     */
    public static final String DEFAULT_THREAD_NAME = "?";

    // 12 hours in milliseconds form
    private static final int TWELVE_HOURS = 1000 * 60 * 60 * 12;


    /**
     * Default Log4j LayoutPattern
     */
    public static final String DEFAULT_LAYOUT_PATTERN = "%d{ISO8601} %5p %t %c{4}:%L - %m%n";

    /**
     * When fast parse is enabled, we limit the number of characters from the feed per line to
     * parse.  This results in the compiled output being based on less detail, but that's usually
     * contextual information, ala the actual trace message.  Timing loggers aren't likely to
     * be affected much by this truncation
     */
    public static final int FAST_LIMIT = 250;

    private Log4jPatternConverter _converter;
    private String _layoutPattern;
    private String _message;
    private boolean _tryFast = false;
    private int _fastLimit = FAST_LIMIT;

    /**
     * This attribute enables the behavior of 'correcting' date values.  Correction may need to occur when
     * the LayoutPattern is one that results in ambiguous time stamps.  That is, time stamps that don't specify
     * a date or even a 24-hour designation.<br>
     * <br>
     * This algorithm assumes that a log file is constructed sequentially and thus all dates are in order from earliest
     * to latest.  Therefore, when any date is parsed that is earlier than the previous date, a correction is made
     * to increment the date by 12 hours, and then again by another 12 hours if required.  This algorithm assumes
     * that when date values are actually present, they will always be in a form that includes day, month, and year.
     * Thus, no further correction is anticipated after incrementing by a maximum of 24 hours.  However, a warning
     * will be displayed should such a situation arise.<br>
     * <br>
     * Set this value to 'false' to disable this correction scheme.
     */
    public boolean autoCorrectDates = true;
    private Date _lastDate;
    private long _dateAdjustment = 0;


    /**
     * Constructor that takes in a single argument
     * @param layoutPattern The Log4j LayoutPattern used to parse the inbound logfile
     */
    public AbstractTraceAspectLogAnalyzer(String layoutPattern) {
        _log.debug("layoutPattern: " + layoutPattern);
        _threads = new HashMap<String, Stack<String[]>>();
        if (layoutPattern != null) {
            _layoutPattern = layoutPattern;
        } else {
            _layoutPattern = DEFAULT_LAYOUT_PATTERN;
        }
        _converter = new Log4jPatternConverter(_layoutPattern);
        _log.debug("converter: " + _converter);
    }

    /**
     * Default constructor that uses {@link AbstractTraceAspectLogAnalyzer#DEFAULT_LAYOUT_PATTERN}
     * ... no, don't use this.
     * @deprecated
     */
    public AbstractTraceAspectLogAnalyzer() {
        this ((String)null);
    }

    /**
     * Offers next log event to parse, analyze.  This class simply maintains the method
     * call stack.  Subclasses will want access to the method / thread stacks and use them
     * for their own purposes.  Need to provide accessors for those
     */
    public void addLogEvent(String logEvent) {
        // When the try fast flag is set and the event is greater than some threshold of characters,
        // 	- truncate the message at the limit
        // 	- for now, let's just ditch what's pruned
        //  - future: save the fatty parts and append to the message

        _log.trace("Logging event: " + logEvent);
        if (_tryFast && logEvent.length() > _fastLimit) {
            logEvent = logEvent.substring(0, _fastLimit);
            // to help with parsing later on, let's close the line with a )
            logEvent = logEvent + ")";
        }
        _message = null;
        _converter.setLogEvent(logEvent);

        // AbstractTraceAspectLogAnalyzer is specifically useful because of the known
        // format of 'Entering' and 'Exiting' -- So building the call
        // stacks should happen here
        String thread = getThread();

        String[] bundle = new String[2];
        // each method is stored in a Map for each known thread
        Stack<String[]> methodStack = _threads.get(thread);

        // TODO: Should be able to pull up the method name detection / construction to
        // a single block outside the conditional
        if (isEntering()) {
            List<String> methodSig = getMethodSignature();
            _log.debug("methodSig: " + methodSig);
            String categoryName = methodSig.get(0);
            String methodName = methodSig.get(1);
            String fullMethodName = categoryName + ":" + methodName;
            String formattedMethodSig = formatMethodSig(methodSig);
            bundle[0] = fullMethodName;
            bundle[1] = formattedMethodSig;

            if (methodStack == null) {
                methodStack = new Stack<String[]>();
                _threads.put(thread, methodStack);
            }
            methodStack.push(bundle);
        } else if (isExiting()) {
            List<String> methodSig = getMethodSignature();
            if (methodStack == null || methodStack.isEmpty()) {
                _log.warn("Ignoring (Exiting before having entered): " + logEvent);
                return;
            }
            // exiting, pop off the stack and see ifn it matches
            String exitMethodName = methodSig.get(0) + ":" + methodSig.get(1);
            boolean match = false;
            String thatMethod = null;
            while (!match && !methodStack.isEmpty()) {
                String[] next = methodStack.pop();
                thatMethod = next[0];
                if (thatMethod.equals(exitMethodName)) {
                    match = true;
                }
            }
        }
    }

    /**
     * Extracts the 'Message' token of the log event
     * @return
     */
    protected String parseMsg() {
        if (_message == null) {
            _message = _converter.parseToken(Log4jPatternConverter.Identifier.MESSAGE);
        }
        _log.trace("parseMsg: " + _message);
        return _message;
    }

    public String getPriority() {
        String priority = _converter.parseToken(Identifier.PRIORITY);
        _log.trace("priority: " + priority);
        return priority;
    }

    /**
     * Determines if the log event is an 'Entering method' event
     * @return
     */
    public boolean isEntering() {
        // TODO: This is based on SailPoint's trace injection class.  Why not abstract this
        // string to expand its uses
        String message = parseMsg();
        if (message != null && message.matches("^Entering .*\\Q(\\E.*$")) {
            _log.trace("isEntering: true");
            return true;
        } else {
            _log.trace("isEntering: false");
            return false;
        }
    }

    /**
     * Determines if the log event is an 'Exiting method' event
     * @return
     */
    public boolean isExiting () {
        // TODO: This is based on SailPoint's trace injection class.  Why not abstract this
        // string to expand its uses
        String message = parseMsg();
        if (message != null && message.startsWith("Exiting ")) {
            _log.trace("isExiting: true");
            return true;
        } else if (message != null && message.startsWith("Throwing ")) { // throwing is "like" an exit message
            _log.trace("isExiting: true (throwing)");
            return true;
        } else {
            _log.trace("isExiting: false");
            return false;
        }
    }

    public void setDoFast (boolean tryFast) {
        _log.debug("Fast mode: " + tryFast);
        _tryFast = tryFast;
    }

    public void setFastParseCharacterLimit (int fastParseLimit) {
        _log.debug("Fast mode character limit: " + fastParseLimit);
        _fastLimit = fastParseLimit;
    }

    /**
     * Returns an array of String that is the method signature in the following elements:<br>
     * 0 - The category name<br>
     * 1 - The method name<br>
     * each following odd element - parameter name<br>
     * each subsequent following even element - the parameter's value<br>
     * @return
     */
    public List<String> getMethodSignature() {
        _log.trace("Entering getMethodSignature");
        List<String> methodSignature = new ArrayList<String>();

        if (!isEntering() && !isExiting()) { // isExiting is irrelevant for getting the method sig.
            return null; // not entering or exiting? nothing to return
        }

        String message = parseMsg();
        
        // we know we're entering or exiting, so one of these tokens is expected
        String[] tokens = message.split("^Entering |^Exiting |^Throwing ");
        if (tokens.length != 2) {
            _log.warn("Tokens length != 2 when entering, exiting, or throwing: " + tokens);
            return null; // sanity check failed.. why?
        }

        String msg = tokens[1];
        String methodName;
        String className = parseCategory();
        _log.trace("Found class name: " + className);
        methodSignature.add(className);

        if (isEntering()) {
            // Parse the entering message to pull out the parameter information
            Pattern p = Pattern.compile("^(Entering )([\\S]+)\\((.*)\\)[\\s\\n]*", Pattern.DOTALL);
            _log.trace("Method matching pattern: " + p);
            // the message may have newlines.  the DOTALL flag helps us with that
            Matcher m = p.matcher(message);
            if (m.matches()) {
                _log.trace("Method matched!");
                // method is group 2
                methodName = m.group(2);
                _log.trace("Found method name: " + methodName);
                methodSignature.add(methodName);
                // signature is group 3
                String signature = m.group(3);
                _log.trace("Found signature: " + signature);
                // Pattern looks for 'name = value' where 'name' is alphanumeric plus _?$
                // 'value' is any characters
                Pattern sigPattern = Pattern.compile("([a-zA-Z0-9_$?]+) = .+?,?", Pattern.DOTALL);
                Matcher sigMatcher = sigPattern.matcher(signature);
                int lastEnd = 0;
                int start = 0;
                int end = 0;
                // Finds each name/value pair
                while (sigMatcher.find()) {
                    String nextGroup = sigMatcher.group();
                    start = sigMatcher.start();
                    end = sigMatcher.end();
                    String paramName = nextGroup.split(" = ")[0];
                    if (start != 0) {
                        String lastValue = signature.substring(lastEnd - 1,  start - 2);
                        methodSignature.add(lastValue);
                    }
                    methodSignature.add(paramName);
                    lastEnd = end;
                }
                // left over characters get tacked on
                if (lastEnd > 0) {
                    String lastValue = signature.substring(lastEnd - 1,  signature.length());
                    methodSignature.add(lastValue);
                }
            } else {
                _log.warn("Method pattern not matched! pattern: " + p + "\n\tMsg: " + message);
            }
        } else if (message.startsWith("Exiting ")) {
            // proper exiting methods return with the return value
            methodName = msg.split(" = ")[0];
            methodSignature.add(methodName);
        } else if (message.startsWith("Throwing ")) {
            methodName = msg.split(" - ")[0];
            methodSignature.add(methodName);
        }
        _log.trace("methodSignature: " + methodSignature);
        if (methodSignature.size() < 2) {
            _log.error("Method signature not two elements: " + _converter.getLogEvent());
        }
        return methodSignature;
    }

    /**
     * Extracts the method name from the log event.  Note that while {@link Log4jPatternConverter}
     * support extracting the Method token, this method will attempt to extract the method
     * name as stated in the Message token instead.
     * @see Log4jPatternConverter#parseToken(sailpoint.services.log.api.Log4jPatternConverter.Identifier, String)
     */
    public String getMethod() {
        /*
         * To clarify the javadoc: we're primarily looking for method information from a trace message that looks
         * like this:
         * Entering getWdiget(widgetWhat = "foo")
         * 
         * This is another artifact specific to SailPoint's tracing injection and would have to be abstracted
         * in order to expand the usability of this tool.
         * 
         * Those familiar with Log4j will know that part of the layout pattern is a token that is actually
         * dedicated to the method the log statement was logged from.  The way SailPoint uses trace injection
         * means we have to disuade from using the Log4j method token since the result would be a non-informative
         * method name, like 'traceMethodEnter'
         */
        List<String> methodSig = getMethodSignature();

        if (methodSig != null) {

            String base = methodSig.get(0) + ":" + methodSig.get(1) + "(";
            StringBuffer buff = new StringBuffer(base.length() + 1);
            buff.append(base);
            for (int i = 2; i < methodSig.size(); i++) {
                if (i % 2 == 0) {
                    // parameter names are even indexes
                    buff.append(methodSig.get(i));
                    if (i < methodSig.size() - 3) {
                        // for the ', '
                        buff.append(", ");
                    }
                }
            }
            buff.append(")");
            return buff.toString();
        } else {
            return "";
        }


    }

    /**
     * Extracts the Thread token from the log event.  If no thread is found, {@link AbstractTraceAspectLogAnalyzer#DEFAULT_THREAD_NAME}
     * will be used
     * @return
     */
    public String getThread() {
        String thread = _converter.parseToken(Log4jPatternConverter.Identifier.THREAD);
        if (thread == null) {
            thread  = DEFAULT_THREAD_NAME;
        }
        _log.trace("getThread: " + thread);
        return thread;
    }

    /**
     * Extracts the Category token from the log event.  This is commonly the full class name which is derived
     * commonly by best practices.  Extracting the Class token is not supported from this implementation since
     * many avoid capturing that token due to performance constraints.
     * @return
     */
    public String parseCategory () {
        String className = _converter.parseToken(Log4jPatternConverter.Identifier.CATEGORY);
        _log.trace("parseCategory: " + className);
        return className;
    }

    /**
     * Extracts the Date based off of the SimpleDatePattern interpreted by the LayoutPattern.  If {@link AbstractTraceAspectLogAnalyzer#autoCorrectDates}
     * is enabled, ambiguous time stamps will be adjusted as the logfile is scanned.
     * @return
     * @see Log4jPatternConverter#parseDate(String)
     */
    public Date getDate() {
        _log.trace("Entering getDate");
        Date current = _converter.parseDate();
        if (current != null) {
            // previous adjustments need to persist
            current.setTime(current.getTime() + _dateAdjustment);
            if (autoCorrectDates && _lastDate != null) {
                // make a new adjustment
                if (current.before(_lastDate)) {
                    current.setTime(current.getTime() + TWELVE_HOURS);
                    _dateAdjustment += TWELVE_HOURS;
                }
                // one more time
                if (current.before(_lastDate)) {
                    current.setTime(current.getTime() + TWELVE_HOURS);
                    _dateAdjustment += TWELVE_HOURS;
                }	
            }
            // warning: adjustments didn't fly
            if (_lastDate != null && current.before(_lastDate)) {
                // TODO: Incorporate better logging
                System.err.println("Current date: " + current + " is before last: " + _lastDate);
                System.err.println("From event: " + _converter.getLogEvent());
            }
            _lastDate = current;
        }
        _log.trace("getDate: " + current);
        return current;
    }

    /*
     * Makes the method signature, currently a list of strings, something pretty to look at
     */
    private String formatMethodSig(List<String> methodSig) {
        /* something like:
         * \tparamName: <-- normalized \s --> paramValue\n
         */
        StringBuffer buff = new StringBuffer();
        for (int i = 2; i < methodSig.size(); i += 2) {
            String propName = methodSig.get(i);
            String propValue = methodSig.get(i + 1);
            String formatted = String.format("%1$-" + _propNameMaxLength + "s", propName);
            buff.append("\t" + formatted + " : " + propValue + "\n");
        }
        if (buff.length() > 0) {
            buff.delete(buff.length() - 1, buff.length());
        }
        return buff.toString();
    }

    protected Stack<String[]> getCallStack (String forThread) {
        Stack<String[]> callStack = null;
        Stack<String[]> current = _threads.get(forThread);
        if (current != null) {
            callStack = new Stack<String[]>(); // defensive copy
            callStack.addAll(current);  // need to check this is copied in the right order
        }

        return callStack;
    }

}
