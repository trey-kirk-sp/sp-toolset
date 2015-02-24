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
 *
 */
public class LogMethodCallSummary extends MethodStackAnalyzer {

    private Map<String,Stack<String[]>> _threads;
    private Stack<String> _methods;
    private int _propNameMaxLength = 10;
    private String _targetClass;
    private String _targetMethod;

    /**
     * Default constructor uses a default layout pattern
     */
    public LogMethodCallSummary(String className, String methodName) {
        this ((String)null, className, methodName);
    }

    public LogMethodCallSummary(String layoutPattern, String className, String methodName) {
        super (layoutPattern);
        _threads = new HashMap<String, Stack<String[]>>();
        _methods = new Stack<String>();
        _targetClass = className;
        _targetMethod = methodName;

    }

    /**
     * For each log event, test if has an 'Entering' value and capture the method signature information.  Otherwise, test
     * if it is an ERROR and create the summary information for that error.
     */
    @Override
    public boolean addLogEvent(String logEvent) {
        super.addLogEvent(logEvent);
        String thread = getThread();

        String[] bundle = new String[2];
        // each method is stored in a Map for each known thread
        Stack<String[]> methodStack = _threads.get(thread);

        if (isEntering()) {
            List<String> methodSig = getMethodSignature();
            _log.debug("methodSig: " + methodSig);
            String categoryName = methodSig.get(0);
            String methodName = methodSig.get(1);
            String fullMethodName = categoryName + ":" + methodName;
            String formattedMethodSig = formatMethodSig(methodSig);
            bundle[0] = fullMethodName;
            bundle[1] = formattedMethodSig;

            if (methodStack == null) {
                methodStack = new Stack<String[]>();
                _threads.put(thread, methodStack);
            }
            methodStack.push(bundle);
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
                buff.append(logEvent + "\n\n");
                //buff.append(logEvent + "\n\n----------------------------------------------------\n\n");
                _methods.push(buff.toString());
            }
        } else if (isExiting()) {
            List<String> methodSig = getMethodSignature();
            String categoryName = methodSig.get(0);
            String methodName = methodSig.get(1);
            boolean classTest = _targetClass == null || _targetClass.equals(categoryName);
            boolean methodTest = _targetMethod == null || _targetMethod.equals(methodName);
            if (methodStack == null || methodStack.isEmpty()) {
                _log.warn("Ignoring (Exiting before having entered): " + logEvent);
                return true;
            }
            // exiting, pop off the stack and see ifn it matches
            String exitMethodName = methodSig.get(0) + ":" + methodSig.get(1);
            boolean match = false;
            String thatMethod = null;
            while (!match && !methodStack.isEmpty()) {
                String[] next = methodStack.pop();
                thatMethod = next[0];
                if (thatMethod.equals(exitMethodName)) {
                    match = true;
                }
            }
            // new: now I want to see how the method returns!
            if (methodTest && classTest && match && !_methods.isEmpty()) {
                StringBuffer lastMethodBuff = new StringBuffer(_methods.pop());
                lastMethodBuff.append(logEvent + "\n\n----------------------------------------------------\n\n");
                _methods.push(lastMethodBuff.toString());
            }
        }
        return true;
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
