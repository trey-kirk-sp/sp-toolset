package sailpoint.services.log.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * This analyzer will trim log events that are outside of call stack of a given method. So for example, 
 * if the method to isolate was Identitizer#refreshIdentity, only that method and any method that is entered
 * while 'refreshIdentity' is in the call hierarchy will be retained. Note that method signature
 * isn't required. Maybe later?
 * @author trey.kirk
 *
 */
public class MethodIsolationAnalzyer extends MethodStackAnalyzer {

    private String _className;
    private String _methodName;
    private List<String> _isolatedEvents;

    public MethodIsolationAnalzyer(String className, String methodName, String layoutPattern) {
        super(layoutPattern);
        this._className = className;
        this._methodName = methodName;
        this._isolatedEvents = new ArrayList<String>();
    }
    
    
    @Override
    public boolean addLogEvent(String logEvent) {
        super.addLogEvent(logEvent);
        if (isInMethod()) {
            _isolatedEvents.add(logEvent);
        }
        return true;
    }
    
    private boolean isInMethod() {
        // Consult the current log event's Thread.
        // Then get the call stack of that thread.
        // If the current call stack contains our isolating class and method, return true
        String thread = getThread();
        String isolatedMethod = formatMethodFullName(_className, _methodName);
        // First, check this event's class and method. If it's a match, no need to consult the stack
        List<String> methodSignature = getMethodSignature();
        if (methodSignature == null || methodSignature.size() < 2) {
            return false;
        }
        String eventCategory = methodSignature.get(0);
        String eventMethod = methodSignature.get(1);
        String eventFullMethod = formatMethodFullName(eventCategory, eventMethod);
        if (isolatedMethod.equals(eventFullMethod)) {
            return true;
        }
        
        if (thread != null) {
            Stack<String[]> callStack = _threads.get(thread);
            if (callStack != null) {
                // our desired information is in element 0, ala "cateogryName:methodName"
                for (String[] call : callStack) {
                    if (isolatedMethod.equals(call[0])) {
                        return true;
                    }
                }
            }
        } else {
            _log.warn("No Thread information for this event");
        }
        // get here? So sad
        return false;
    }

    @Override
    public String compileSummary() {
        StringBuilder buff = new StringBuilder();
        for (String event : _isolatedEvents) {
            buff.append(event).append("\n");
        }
        return buff.toString();
    }

}
