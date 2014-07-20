package sailpoint.services.log.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public abstract class MethodStackAnalyzer extends AbstractTraceAspectLogAnalyzer {

    protected Map<String,Stack<String[]>> _threads;
    protected int _propNameMaxLength = 10;
    protected Map<String,Stack<String[]>> _throwingMethods;


    public MethodStackAnalyzer(String layoutPattern) {
        super (layoutPattern);
        _threads = new HashMap<String, Stack<String[]>>();
        _throwingMethods = new HashMap<String, Stack<String[]>>();
    }

    /**
     * For each log event, test if has an 'Entering' value and capture the method signature information.  Otherwise, test
     * if it is an ERROR and create the summary information for that error.
     */
    @Override
    public void addLogEvent(String logEvent) {
        super.addLogEvent(logEvent);
        String thread = getThread();

        String[] bundle = new String[2];
        // each method is stored in a Map for each known thread
        Stack<String[]> methodStack = _threads.get(thread);

        List<String> methodSig = getMethodSignature();
        if (methodSig != null) {
            String categoryName = methodSig.get(0);
            String methodName = methodSig.get(1);

            if (isEntering()) {
                _log.debug("methodSig: " + methodSig);
                String fullMethodName = categoryName + ":" + methodName;
                String formattedMethodSig = formatMethodSig(methodSig);
                bundle[0] = fullMethodName;
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
                String exitMethodName = methodSig.get(0) + ":" + methodSig.get(1);
                if (isThrowing()) {
                    Stack<String[]> throwingStack = new Stack<String[]>();
                    for (String[] methodTokens : methodStack) {
                        // iterates like a Vector, so build like a Vector
                        throwingStack.add(methodTokens);
                    }
                    _throwingMethods.put(exitMethodName, throwingStack);
                }
                boolean match = false;
                String thatMethod = null;
                while (!match && !methodStack.isEmpty()) {
                    String[] next = methodStack.pop();
                    thatMethod = next[0];
                    if (thatMethod.equals(exitMethodName)) {
                        match = true;
                    }
                }
            }
        }
    }

    /*
     * Converts the method signature list into a pretty summary.
     */
    protected String formatMethodSig(List<String> methodSig) {
        /* something like:
         * \tparamName: <-- normalized \s --> paramValue\n
         */
        StringBuffer buff = new StringBuffer();
        for (int i = 2; i < methodSig.size(); i += 2) {
            String propName = methodSig.get(i);
            String propValue = methodSig.get(i + 1);
            String formatted = String.format("%1$-" + _propNameMaxLength + "s", propName);
            buff.append("\t" + formatted + " : " + propValue + "\n");
        }
        if (buff.length() > 0) {
            buff.delete(buff.length() - 1, buff.length());
        }
        return buff.toString();
    }


}
