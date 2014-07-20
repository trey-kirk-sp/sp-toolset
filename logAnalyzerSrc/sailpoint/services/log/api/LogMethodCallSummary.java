package sailpoint.services.log.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Summarizes a method's call stack.  This is similar to what {@link LogErrorSummary} does
 * for ERROR log events, but instead will summarize the call stack for a method at the time
 * the 'Entering' log event is parsed.
 * @author trey.kirk
 * TODO: This is reimpleneting the same code as Abstract and other analyzers.  Refactor required.
 */
public class LogMethodCallSummary extends MethodStackAnalyzer {

    private String _targetClass;
    private String _targetMethod;
    protected Stack<String> _methods;


    public LogMethodCallSummary(String className, String methodName) {
        this(null, className, methodName);
    }
    
    public LogMethodCallSummary(String layoutPattern, String className, String methodName) {
        super(layoutPattern);
        _targetClass = className;
        _targetMethod = methodName;
        _methods = new Stack<String>();
    }

    /**
     * For each log event, test if has an 'Entering' value and capture the method signature information.  Otherwise, test
     * if it is an ERROR and create the summary information for that error.
     */
    @Override
    public void addLogEvent(String logEvent) {
        super.addLogEvent(logEvent);
        String thread = getThread();

        // each method is stored in a Map for each known thread
        // should never be null
        Stack<String[]> methodStack = _threads.get(thread);

        if (isEntering()) {
            List<String> methodSig = getMethodSignature();
            _log.debug("methodSig: " + methodSig);
            String categoryName = methodSig.get(0);
            String methodName = methodSig.get(1);

            boolean classTest = _targetClass == null || _targetClass.equals(categoryName);
            boolean methodTest = _targetMethod == null || _targetMethod.equals(methodName);
            if (classTest && methodTest) {
                StringBuffer buff = new StringBuffer();
                for (int i = 0; methodStack != null && i < methodStack.size(); i++) {
                    String[] next = methodStack.get(i);
                    buff.append(next[0] + " (");
                    if (next[1] != null && !next[1].equals("")) {
                        buff.append("\n");
                    }
                    buff.append(next[1] + " )\n\n");
                }
                // Push the full 'entering' event in case 'fastParse' truncated too much
                buff.append(logEvent + "\n\n");
                _methods.push(buff.toString());
            }
        } else if (isExiting()) {
            List<String> methodSig = getMethodSignature();
            String categoryName = methodSig.get(0);
            String methodName = methodSig.get(1);
            boolean classTest = _targetClass == null || _targetClass.equals(categoryName);
            boolean methodTest = _targetMethod == null || _targetMethod.equals(methodName);

            // Push the full 'exiting' event so we can capture the return value
            if (methodTest && classTest && !_methods.isEmpty()) {
                StringBuffer lastMethodBuff = new StringBuffer(_methods.pop());
                lastMethodBuff.append(logEvent + "\n\n----------------------------------------------------\n\n");
                _methods.push(lastMethodBuff.toString());
            }
        }

    }

    /**
     * Returns a String of the pretty method signatures we've built
     */
    public String compileSummary() {
        StringBuffer out = new StringBuffer();
        for (String nextError : _methods) {
            out.append(nextError + "\n");
        }
        return out.toString();
    }

}
