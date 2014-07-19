package sailpoint.services.log.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.biliruben.util.csv.CSVRecord;
import com.biliruben.util.csv.CSVUtil;


/**
 * Concrete implementation of {@link AbstractTraceAspectLogAnalyzer}.  This class
 * aggregates timing information and provides method timing for all methods found.
 * @author trey.kirk
 */
public class LogTimer extends AbstractTraceAspectLogAnalyzer {


    /*
     * Inner class used to store and calculate method timings
     */
    private static class MethodTimer {
        private static final String MAP_TOTAL = "total";
        private static final String MAP_CALLS = "calls";
        private static final String MAP_LONGEST = "longest";
        private static final String MAP_SHORTEST = "shortest";
        private static final String MAP_AVERAGE = "average";
        private static final String MAP_METHOD = "method";
        private static final String MAP_THREAD = "thread";
        private static final String MAP_CALL_ORDER = "order";
        String _method;
        List<Long> _durations;
        private String _thread;
        private int _order;

        /*
         * Single Constructor takes in all relevant initial tidbits
         */
        private MethodTimer(String thread, String method, int order) {
            _method = method;
            _thread = thread;
            _durations = new ArrayList<Long>();
            _order = order;
        }

        public String getMethod() {
            return _method;
        }

        public String getThread() {
            return _thread;
        }

        /*
         * Durations are simply a list of elapsed durations per call.  This list is then
         * ordered and averaged.  At some later time, one may incorporate additional statistical
         * analytics to this list.
         */
        private void addDuration(long duration) {
            _durations.add(duration);
        }

        private Long[] getSortedDuruations() {
            Long[] longArry = _durations.toArray(new Long[0]);
            Arrays.sort(longArry);
            return longArry;
        }

        private long getShortest() {
            return getSortedDuruations()[0];
        }

        private long getLongest() {
            Long[] sorted = getSortedDuruations();
            return sorted[sorted.length - 1];
        }

        private long getCalls() {
            return _durations.size();
        }

        private double getAverage() {
            if (_durations.size() == 0) {
                return 0;
            }

            return getTotal() / _durations.size();
        }

        /*
         * The MethodTimer class is a glorified Map
         */
        private Map<String, String> toMap() {
            Map<String, String> m = new HashMap<String, String>();
            m.put(MAP_THREAD, getThread());
            m.put(MAP_METHOD, getMethod());
            m.put(MAP_CALL_ORDER, String.valueOf(getOrder()));
            m.put(MAP_AVERAGE, String.valueOf(getAverage()));
            m.put(MAP_SHORTEST, String.valueOf(getShortest()));
            m.put(MAP_LONGEST, String.valueOf(getLongest()));
            m.put(MAP_CALLS, String.valueOf(getCalls()));
            m.put(MAP_TOTAL, String.valueOf(getTotal()));

            return m;
        }

        /*
         * This order attribute is intended to be used to declare in what
         * order a method was found.  This helps to visualize the call stack somewhat.
         */
        private int getOrder() {
            return _order;
        }

        private long getTotal() {
            long sum = 0;
            for (Long l : _durations) {
                sum = sum + l;
            }
            return sum;
        }
    }

    // static index position for the time stamp
    private static final int INDEX_TIME_STAMP = 1;

    // static index position for the method name
    private static final int INDEX_METHOD_NAME = 0;

    // Map storing all of our timers.
    private Map<String, MethodTimer> _timers;
    // Map storing all of our method call stacks
    private Map<String, Stack<Object[]>> _threads;
    // Counter that increments for every new MethodTimer
    private int _timerCount;

    /**
     * Default constructor
     */
    public LogTimer() {
        this((String)null);
    }

    /**
     * Constructor taking in a specific Log4j LayoutPattern
     * @param layoutPattern
     */
    public LogTimer(String layoutPattern) {
        super(layoutPattern);
        //_methods = new Stack<Object[]>();
        _timers = new HashMap<String, MethodTimer>();	
        _threads = new HashMap<String, Stack<Object[]>>();
        _timerCount = 0;
    }

    /*
     * Returns the method call stack from the threads map.  If
     * none is found, one is created and an empty stack is returned.
     */
    private Stack<Object[]> getMethodStack(String thread) {
        Stack<Object[]> methods = _threads.get(thread);
        if (methods == null) {
            methods = new Stack<Object[]> ();
            _threads.put(thread, methods);
        }
        return methods;
    }

    /**
     * Adds the next log event message.  Date information is extracted from it if i
     */
    public void addLogEvent(String message) {
        super.addLogEvent(message);
        Date timeStamp = getDate();
        String method = getMethod();
        String thread = getThread();
        Stack<Object[]> methods = getMethodStack(thread);
        if (isEntering()) {
            Object[] bundle = new Object[2];
            bundle[INDEX_METHOD_NAME] = method;
            bundle[INDEX_TIME_STAMP] = timeStamp;
            methods.add(bundle);
        } else if (isExiting()) {
            // some messages are neither
            if (methods.isEmpty()) {
                // nothing in the stack, ignore
                return;
            }
            Object[] bundle = methods.pop();
            String thisMethod = ((String)bundle[INDEX_METHOD_NAME]).split("\\(")[0];
            String thatMethod = method.split("\\(")[0];
            while (!thisMethod.equals(thatMethod) && !methods.isEmpty()) {
                // method mis-match.  This happens when an exception throws us
                // out of the method call stack.  Now we have to try and re-match or reset
                System.err.println("Method mismatch: " + thisMethod + " vs. " + thatMethod);
                bundle = methods.pop();
                thisMethod = ((String)bundle[INDEX_METHOD_NAME]).split("\\(")[0];
                thatMethod = method.split("\\(")[0];
            }
            if (thisMethod.equals(thatMethod)) {
                // sanity check, good method
                method = (String) bundle[INDEX_METHOD_NAME];
                long exitTime = timeStamp.getTime();
                long entryTime = ((Date)bundle[INDEX_TIME_STAMP]).getTime();				
                long diff = exitTime - entryTime;
                addTimer(thread, method, diff);
            }
        }
    }


    private void addTimer(String thread, String method, long diff) {
        String threadMethod = thread + ":" + method;
        MethodTimer mt = _timers.get(threadMethod);
        if (mt == null) {
            _timerCount++;
            mt = new MethodTimer(thread, method, _timerCount);
            _timers.put(threadMethod, mt);
        }
        mt.addDuration(diff);
    }

    public String compileSummary() {
        // Not sure the best way to do this... how about CSV (sort yer own)
        String[] headers = {
                MethodTimer.MAP_THREAD, MethodTimer.MAP_METHOD, MethodTimer.MAP_CALL_ORDER, MethodTimer.MAP_CALLS, MethodTimer.MAP_SHORTEST, MethodTimer.MAP_LONGEST, MethodTimer.MAP_AVERAGE, MethodTimer.MAP_TOTAL
        };
        CSVRecord record = new CSVRecord(headers);
        for (MethodTimer timer : _timers.values()) {
            Map<String, String> timerMap = timer.toMap();
            record.addLine(timerMap);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            CSVUtil.exportToCsv(record, out);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return out.toString();
    }

}
