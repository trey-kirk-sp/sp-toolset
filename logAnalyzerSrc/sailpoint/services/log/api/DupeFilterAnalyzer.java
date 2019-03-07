package sailpoint.services.log.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DupeFilterAnalyzer extends FastLogAnalyzer {

    private String _lastEvent;
    private List<String> _events;
    
    public DupeFilterAnalyzer(String layoutPattern) {
        super();
        _events = new ArrayList<String>();
    }
    
    @Override
    public boolean addLogEvent(String event) {
        
        if (_lastEvent == null || _lastEvent != null && !_lastEvent.equals(event)) {
            // no match, keep it
            _events.add(event);
        }
        _lastEvent = event;
        return true;
    }

    @Override
    public String compileSummary() {
        StringBuilder buff = new StringBuilder();
        for (String event : _events) {
            buff.append(event).append("\n");
        }
        return buff.toString();
    }

}
