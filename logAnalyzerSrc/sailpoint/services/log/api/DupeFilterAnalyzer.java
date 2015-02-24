package sailpoint.services.log.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DupeFilterAnalyzer extends AbstractTraceAspectLogAnalyzer {

    private Log4jPatternConverter _converter;
    private LogEvent _lastEvent;
    private List<String> _events;

    private static class LogEvent {
        private String _message;
        private Date _date;
        private String _thread;
        
        LogEvent(Date date, String message, String thread) {
            _date = date;
            _message = message;
            _thread = thread;
        }
            
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof LogEvent)) {
                return false;
            }
            LogEvent that = (LogEvent)obj;
            
            boolean dateMatch = _date == null && that._date == null;
            if (!dateMatch && _date != null) {
                dateMatch = _date.equals(that._date);
            }
            
            boolean threadMatch = _thread == null && that._thread == null;
            if (!threadMatch && _thread != null) {
                threadMatch = _thread.equals(that._thread);
            }
            
            boolean msgMatch = _message == null && that._message == null;
            if (!msgMatch && _message != null) {
                msgMatch = _message.equals(that._message);
            }
            return dateMatch && threadMatch && msgMatch;
        }
        
        @Override
        public int hashCode() {
            int code = 7;
            code *= _message != null ? _message.hashCode() : 1;
            code *= _thread != null ? _thread.hashCode() * 2 : 1;
            code *= _message != null ? _message.hashCode() * 3 : 1;
            return code;
        }
    }
    
    public DupeFilterAnalyzer(String layoutPattern) {
        super(layoutPattern);
        _converter = new Log4jPatternConverter(layoutPattern);
        _events = new ArrayList<String>();
    }
    
    @Override
    public boolean addLogEvent(String event) {
        super.addLogEvent(event);
        
        LogEvent thisEvent = new LogEvent(getDate(), parseMsg(), getThread());
        if (_lastEvent != null && !_lastEvent.equals(thisEvent)) {
            // no match, keep it
            _events.add(event);
        }
        _lastEvent = thisEvent;
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
