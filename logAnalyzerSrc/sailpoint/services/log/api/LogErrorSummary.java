package sailpoint.services.log.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Summarize exceptions and prints out the call stack for that exception
 * @author trey.kirk
 *
 */
public class LogErrorSummary extends MethodStackAnalyzer {

    private Map<String,Stack<String[]>> _threads;
    private List<String> _errors;
    private int _propNameMaxLength = 10;

    /**
     * Default constructor uses a default layout pattern
     */
    public LogErrorSummary() {
        this ((String)null);
    }

    /**
     * The "good" constructor
     * @param layoutPattern
     */
    public LogErrorSummary(String layoutPattern) {
        super (layoutPattern);
        _threads = new HashMap<String, Stack<String[]>>();
        _errors = new ArrayList<String>();
    }

    /**
     * For each log event, test if has an 'Entering' value and capture the method signature information.  Otherwise, test
     * if it is an ERROR and create the summary information for that error.
     */
    @Override
    public void addLogEvent(String logEvent) {
        super.addLogEvent(logEvent);
        String thread = getThread();
        List<String> methodSig = getMethodSignature();
        String[] bundle = new String[2];
        // each method is stored in a Map for each known thread
        Stack<String[]> methodStack = _threads.get(thread);
        if (isEntering()) {
            String className = null;
            String simpleMethodName = null;
            if (methodSig.size() > 1) {
                simpleMethodName = methodSig.get(1);
            }
            if (methodSig.size() > 0) {
                className = methodSig.get(0);
            }
            String methodName = className + ":" + simpleMethodName;
            String formattedMethodSig = formatMethodSig(methodSig);
            bundle[0] = methodName;
            bundle[1] = formattedMethodSig;

            if (methodStack == null) {
                methodStack = new Stack<String[]>();
                _threads.put(thread, methodStack);
            }
            methodStack.push(bundle);
        } else if (isExiting()) {
            if (methodStack == null || methodStack.isEmpty()) {
                _log.warn("Ignoring (Exiting before having entered): " + logEvent);
                return;
            }
            // exiting, pop off the stack and see ifn it matches
            String methodName = methodSig.get(0) + ":" + methodSig.get(1);
            boolean match = false;
            String thatMethod = null;
            while (!match && !methodStack.isEmpty()) {
                String[] next = methodStack.pop();
                thatMethod = next[0];
                if (thatMethod.equals(methodName)) {
                    match = true;
                }
            }
            // this is mostly an 'unwinding' activity.  Not much to actually do with the stack
        } else if (isError()) {
            // found an error!
            // iterate the stack and output earliest method call to latest followed by error msg
            StringBuffer buff = new StringBuffer();
            for (int i = 0; methodStack != null && i < methodStack.size(); i++) {
                String[] next = methodStack.get(i);
                buff.append(next[0] + " (");
                if (next[1] != null && !next[1].equals("")) {
                    buff.append("\n");
                }
                buff.append(next[1] + " )\n\n");
            }
            buff.append(logEvent + "\n\n----------------------------------------------------\n\n");
            _errors.add(buff.toString());
        }

    }

    /**
     * Returns a String of the pretty error messages we've built
     */
    public String compileSummary() {
        StringBuffer out = new StringBuffer();
        for (String nextError : _errors) {
            out.append(nextError + "\n");
        }
        return out.toString();
    }

}
