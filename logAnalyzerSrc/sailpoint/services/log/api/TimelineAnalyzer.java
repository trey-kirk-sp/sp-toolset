package sailpoint.services.log.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Initial purpose for this class is to take the incoming log events
 * and then to compile them into a proper timeline as they occurred, which
 * in some cases is not the order in which they were written. For example,
 * given the following events:
 * 
2016-04-07 10:50:23,134 TRACE http-8080-3 sailpoint.web.BaseBean:118 - Entering getContext()
2016-04-07 10:50:23,135  WARN http-8080-7 sailpoint.api.SailPointFactory:124 - contexts.get: sailpoint.server.InternalContext@24c4187c
2016-04-07 10:50:23,133 TRACE http-8080-7 sailpoint.web.BaseBean:124 - Exiting getContext = sailpoint.server.InternalContext@1201fd18

 *  Which genuinely look in order, actually have a timeline of:

2016-04-07 10:50:23,133 TRACE http-8080-7 sailpoint.web.BaseBean:124 - Exiting getContext = sailpoint.server.InternalContext@1201fd18
2016-04-07 10:50:23,134 TRACE http-8080-3 sailpoint.web.BaseBean:118 - Entering getContext()
2016-04-07 10:50:23,135  WARN http-8080-7 sailpoint.api.SailPointFactory:124 - contexts.get: sailpoint.server.InternalContext@24c4187c
 
 * which shows a completely different (and ideally more accurate) picture of events
 * @author trey.kirk
 *
 */
public class TimelineAnalyzer extends AbstractTraceAspectLogAnalyzer {

    private Map<String, List<String>> _timeLineMap;

    public TimelineAnalyzer(String layoutPattern) {
        super(layoutPattern);
        _timeLineMap = new HashMap<String, List<String>>();
        setAdjustDate(false);
    }

    @Override
    public boolean addLogEvent(String logEvent) {
        super.addLogEvent(logEvent);
        Date date = getDate();
        long time = date.getTime();
        String key = time + "";
        List<String> currentEvents = _timeLineMap.get(key);
        if (currentEvents == null) {
            currentEvents = new ArrayList<String>();
            _timeLineMap.put(key, currentEvents);
        }
        currentEvents.add(logEvent);
        return true;
    }

    @Override
    public String compileSummary() {
        Set<String> sortedKeys = new TreeSet<String>();
        for (String date : _timeLineMap.keySet()) {
            sortedKeys.add(date);
        }
        StringBuilder buff = new StringBuilder();
        for (String date : sortedKeys) {
            List<String> events = _timeLineMap.get(date);
            for (String event : events) {
                buff.append(event).append("\n");
            }
        }
        return buff.toString();
    }

}
