package sailpoint.services.log.api;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class LogFilter extends FastLogAnalyzer {

    private Pattern[] _patterns;
    private boolean _inclusive;
    private List<String> _filteredEvents;

    public LogFilter(String layoutPattern, boolean inclusive, Pattern... filter) {
        _patterns = filter;
        _inclusive = inclusive;
        _filteredEvents = new ArrayList<String>();
    }
    
    public boolean addLogEvent(String logEvent) {
        if (passesFilter(logEvent)) {
            // all we do is filter the event... no token processing
            _filteredEvents.add(logEvent);
        }
        return true;
    }

    public String compileSummary() {
        StringBuilder buff = new StringBuilder();
        for (String event : _filteredEvents) {
            buff.append(event + "\n");
        }
        return buff.toString();
    }
    
    private boolean passesFilter(String logEvent) {
        boolean passes = false;
        for (Pattern pattern : _patterns) {
            if (pattern.matcher(logEvent).find()) {
                passes = true;
            }
        }
        
        // and about inclusive vs. exclusive
        passes = passes ^ !_inclusive;
        return passes;
    }
    
}
