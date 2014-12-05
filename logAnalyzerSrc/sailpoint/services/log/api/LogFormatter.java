package sailpoint.services.log.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Just inserts indentation into the log data for easier reading
 * @author trey.kirk
 *
 */
public class LogFormatter extends MethodStackAnalyzer {

    private static final String INDENT = "   ";
    private List<String> _msgs;

    public LogFormatter(String layoutPattern) {
        super(layoutPattern);
        _msgs = new ArrayList<String>();
    }

    @Override
    public boolean addLogEvent(String logEvent) {
        super.addLogEvent(logEvent);

        // stack appended, now display-lay
        String thread = getThread();
        Stack<String[]> calls = getCallStack(thread);
        StringBuffer msg = new StringBuffer();
        int indentSize = calls.size();
        if (isExiting()) {
            // bump it
            indentSize++;
        }
        for (int i = 0; i < indentSize; i++) {
            msg.append(INDENT);
        }
        msg.append(logEvent);
        _msgs.add(msg.toString());
        return true;
    }

    public String compileSummary() {
        StringBuffer summary = new StringBuffer();
        for (String msg : _msgs) {
            summary.append(msg + "\n");
        }
        return summary.toString();
    }

}
