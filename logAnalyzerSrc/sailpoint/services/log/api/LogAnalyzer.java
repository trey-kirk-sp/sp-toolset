package sailpoint.services.log.api;

/**
 * Interface that defines required behavior of a Log Analyzer.  The original intent was to analyze Log4j
 * events.  However, any log file in a unified format could be implemented.
 * @author trey.kirk
 *
 */
public interface LogAnalyzer {

	/**
	 * The "feeder" method for the analyzer
	 * @param message
	 * @return boolean - Signals if the event was successfully parsed. The calling app can use that information
	 * to determine if it should continue processing that event with other analyzers.
	 */
	public boolean addLogEvent (String message);
	
	/**
	 * The "analysis" results method for the analyzer
	 * @return 
	 */
	public String compileSummary();
	
}
