package sailpoint.services.log.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class LogMerger extends FastLogAnalyzer {

    private HashMap<Date, List<String>> _eventMap;
    private Log4jPatternConverter _converter;


    public LogMerger(String layoutPattern) {
        super();
        _eventMap = new HashMap<Date, List<String>>();
        _converter = new Log4jPatternConverter(layoutPattern);
    }
    
    @Override
    public boolean addLogEvent(String logEvent) {
        String trimmedMessage = trimmedMessage(logEvent);
        _converter.setLogEvent(trimmedMessage);
        Date theDate = _converter.parseDate();
        List<String> events = _eventMap.get(theDate);
        if (events == null) {
            events = new ArrayList<String>();
            _eventMap.put(theDate, events);
        }
        events.add(logEvent);
        return true;
    }
    
    
    @Override
    public String compileSummary() {
        Set<Date> orderedDates = new TreeSet<Date>(_eventMap.keySet());
        StringBuilder mergedEvents = new StringBuilder();
        for (Date key : orderedDates) {
            List<String> events = _eventMap.get(key);
            for (String event : events) {
                mergedEvents.append(event).append("\n");
            }
        }
        return mergedEvents.toString();
    }

}
