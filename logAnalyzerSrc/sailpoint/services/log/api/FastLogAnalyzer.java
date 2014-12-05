package sailpoint.services.log.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class FastLogAnalyzer implements LogAnalyzer {

    public static final int FAST_LIMIT = 250;
    private boolean _tryFast = false;
    private int _fastLimit = FAST_LIMIT;

    static Log _log = LogFactory.getLog(FastLogAnalyzer.class);
    
    public FastLogAnalyzer() {
        super();
    }

    protected String trimmedMessage(String originalLogEvent) {
        _log.trace("Entering trimmedMessage: originalLogEvent=" + originalLogEvent);
    
        if (_tryFast && originalLogEvent.length() > _fastLimit) {
            originalLogEvent = originalLogEvent.substring(0, _fastLimit);
            // to help with parsing later on, let's close the line with a )
            originalLogEvent = originalLogEvent + ")";
        }
        _log.trace("Exiting trimmedMessage: " + originalLogEvent);
        return originalLogEvent;
    }

    public void setDoFast(boolean tryFast) {
        _log.debug("Fast mode: " + tryFast);
        _tryFast = tryFast;
    }

    public void setFastParseCharacterLimit(int fastParseLimit) {
        _log.debug("Fast mode character limit: " + fastParseLimit);
        _fastLimit = fastParseLimit;
    }

}