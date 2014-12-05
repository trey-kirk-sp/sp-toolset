package sailpoint.services.log.api;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Analyzer that test parses only a few events
 * @author trey.kirk
 *
 */
public class LogTestParse extends AbstractTraceAspectLogAnalyzer {
    
    private static final int MAX_EVENTS = 5;
    private int _events;
    private List<Map<String, Object>> _parsedEvents;

    public LogTestParse(String layoutPattern) {
        super(layoutPattern);
    }

    @Override
    public boolean addLogEvent(String logEvent) {
        super.addLogEvent(logEvent);
        Map<String, Object> parsedTokens = new HashMap<String, Object>();
        parsedTokens.put("thread", getThread());
        parsedTokens.put("method", getMethod());
        parsedTokens.put("methodSig", getMethodSignature());
        parsedTokens.put("category", parseCategory());
        SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        parsedTokens.put("date", df.format(getDate())); // should provide custom format
        String msg = parseMsg();
        if (msg != null && !"".equals(msg.trim())) {
            if (msg.length() > 32) {
                msg = msg.substring(0, 32);
            }
        }
        parsedTokens.put("message", msg);
        addTokens(parsedTokens);
        _events++;
        return _events < MAX_EVENTS;
    }

    private void addTokens(Map<String, Object> tokens) {
        if (_parsedEvents == null) {
            _parsedEvents = new ArrayList<Map<String, Object>>();
        }
        _parsedEvents.add(tokens);
    }
    
    @Override
    public String compileSummary() {
        StringBuffer buff = new StringBuffer();
        for (Map<String, Object> tokens : _parsedEvents) {
            buff.append("*** Next event ***\n");
            buff.append("\t").append("date: ").append(tokens.get("date")).append("\n");
            for (String key : tokens.keySet()) {
                if ("date".equals(key)) {
                    continue;
                }
                buff.append("\t").append(key).append(": ").append(tokens.get(key)).append("\n");
            }
        }
        return buff.toString();
    }

}
