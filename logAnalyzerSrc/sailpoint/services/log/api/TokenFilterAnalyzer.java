package sailpoint.services.log.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.biliruben.util.OptionLegend;


public class TokenFilterAnalyzer extends FastLogAnalyzer {

    private Log _log = LogFactory.getLog(TokenFilterAnalyzer.class);
    private boolean _exclusive;
    private Map<Character, List<Pattern>> _tokenFilters;
    private List<String> _events;
    private Log4jPatternConverter _converter;
    

    public TokenFilterAnalyzer(boolean exclusive, String layoutPattern) {
        super();
        _exclusive = exclusive;
        _tokenFilters = new HashMap<Character, List<Pattern>>();
        _events = new ArrayList<String>();
        _converter = new Log4jPatternConverter(layoutPattern);
    }
   
    /**
     * Any number of "token-filters" may be supplied.  Each token-filter represents a filter that should not only be applied
     * to each log event, but specifically to the indicating token of that log event.  This will give folks the ability
     * to be specific in how they want to filter the log:
     * %t "Thread-01" -- only log events with "Thread-01" supplied in the %t (thread) token are accepted
     * %c ".*web\.certification.*" -- only log events with a sub-string match of 'web.certification' in the %c (category) token are accepted
     * Naturally, the 'exclusive' flag provided to the constructor will dictate if these filters are inclusive (by default) or instead exclusive
     * @param token
     * @param filter
     */
    public void addTokenFilter (Character token, Pattern filter) {
        _log.debug("addTokenFilter: token=" + token + " filter=" + filter);
        List<Pattern> patterns = _tokenFilters.get(token);
        if (patterns == null) {
            patterns = new ArrayList<Pattern>();
            _tokenFilters.put(token, patterns);   
        }
        patterns.add(filter);
    }

    public boolean addLogEvent(String message) {
        message = trimmedMessage(message);
        _converter.setLogEvent(message); // There's some activity here that I should really pull up to the parent.
        boolean matched = false;
        for (Character token : _tokenFilters.keySet()) {
            String value = _converter.parseToken(token);
            List<Pattern> patterns = _tokenFilters.get(token);
            for (Pattern p : patterns) {
                Matcher m = p.matcher(value);
                // if exclusive, all matches must be false
                // if inclusive, one match must be true
                // in both cases, we're 'OR'ing the filters
                matched = m.matches();  // reduce regex ops
                if (matched) {
                    break;
                }
            }
            if (matched) {
                break;
            }
        }
        if (matched ^ _exclusive) {
            _events.add(message);
        }
        return true;
    }

    public String compileSummary() {
        StringBuilder buff = new StringBuilder();
        for (String event : _events) {
            buff.append(event + "\n");
        }
        return buff.toString();
    }

}
