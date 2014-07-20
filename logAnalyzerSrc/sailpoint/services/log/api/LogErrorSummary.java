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

    private List<String> _errors;

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

        if (isThrowing()) {
            // found an error!
            // get the throwing stack for our current method
            Map<String, Stack<String[]>> threadMap = _throwingMethods.get(thread);
            List<String> methodSig = getMethodSignature();
            String methodName = methodSig.get(0) + ":" + methodSig.get(1);
            Stack<String[]> methodStack = threadMap.get(methodName);
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

        } else if (isError()) {
            // It's unknown if we threw an error first and then reported the exception or the other way around.  
            // That's all driven off of implementation.  However, our isThrowing method will handle 
            // reporting our method stack.  All we do here is report the error message
            _errors.add(logEvent + "\n\n----------------------------------------------------\n\n");
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
