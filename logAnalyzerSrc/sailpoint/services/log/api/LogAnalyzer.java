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
	 */
	public void addLogEvent (String message);
	
	/**
	 * The "analysis" results method for the analyzer
	 * @return
	 */
	public String compileSummary();
	
	public void setDoFast (boolean tryFast);
	
	public void setFastParseCharacterLimit (int limit);
}
