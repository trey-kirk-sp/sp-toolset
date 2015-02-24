package sailpoint.services.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;

import sailpoint.services.log.api.AbstractTraceAspectLogAnalyzer;
import sailpoint.services.log.api.DupeFilterAnalyzer;
import sailpoint.services.log.api.FastLogAnalyzer;
import sailpoint.services.log.api.LogAnalyzer;
import sailpoint.services.log.api.LogErrorSummary;
import sailpoint.services.log.api.LogFilter;
import sailpoint.services.log.api.LogFormatter;
import sailpoint.services.log.api.LogMethodCallSummary;
import sailpoint.services.log.api.LogTestParse;
import sailpoint.services.log.api.LogTimer;
import sailpoint.services.log.api.LogTrender;
import sailpoint.services.log.api.MultiFileLog4jLineIterator;
import sailpoint.services.log.api.TokenFilterAnalyzer;

import com.biliruben.util.GetOpts;
import com.biliruben.util.OptionLegend;
import com.biliruben.util.OptionParseException;

/**
 * Log Analyzer tool to iterate over Log4j logs and apply events to one or more LogAnalyzer objects.  It
 * then outputs each analyzer's summary.
 * @author trey.kirk
 *
 */
public class LogAnalyzerApp {


	private static final String OPT_TAREGET_CLASS = "class";
	private static final String OPT_TARGET_METHOD = "method";
	private static final String LOG4J_PROPERTIES = "log4j.properties";
	private static List<FastLogAnalyzer> _analyzers;
	private static String _layoutPattern;
	private static GetOpts _opts;
	private static Long _timeSlice;

	private static final String ANALYZER_ERROR = "error";
	private static final String ANALYZER_TIMER = "timer";
	private static final String ANALYZER_TRENDER = "trender";
	private static final String ANALYZER_METHOD = "method";
	private static final String ANALYZER_JOINER = "joiner";
	private static final String ANALYZER_FORMATTER = "formatter";
	private static final String ANALYZER_FILTER = "filter";
	private static final String ANALYZER_TEST = "test";
	private static final String ANALYZER_DUPE = "duplicates";
	private static final String[] TYPE_ALLOWED_VALUES = {
		ANALYZER_TIMER,
		ANALYZER_TRENDER,
		ANALYZER_ERROR,
		ANALYZER_JOINER,
		ANALYZER_METHOD,
		ANALYZER_FILTER,
		ANALYZER_FORMATTER,
		ANALYZER_TEST,
		ANALYZER_DUPE
	};

	// Command line arguments
	private static final String OPT_FILE = "file";
	private static final String OPT_LAYOUT_PATTERN = "layoutPattern";
	private static final String OPT_TREND_SEGMENT = "trendSegment";
	private static final String OPT_TYPE = "type";
	private static final String OPT_OUT = "outFile";
	private static final String OPT_PROPERTY_GROUP = "propGroup";
	private static final String OPT_FAST_PARSE = "fastParse";
	private static final String OPT_FAST_PARSE_LIMIT = "parseLimit";
	private static final String OPT_FILTERS = "filter";
	private static final boolean DEBUG = false;
    private static final String OPT_FILTER_EXCLUSIVE = "exclude";

	private static Log _log;
	private static List<String> _fileList;
	private static boolean _join = false;
	private static PrintStream _out;

	/*
	 * Adds the next log event to each analzyer
	 */
	private static boolean analyze(String nextLine) {
	    boolean cont = true;
		for (LogAnalyzer analyzer : _analyzers) {
			cont = analyzer.addLogEvent(nextLine) && cont;
		}
		return cont;
	}
	
	private static void setFilters(TokenFilterAnalyzer analzyer) {
	    // reads in the regex supplied via _opts and convert them to Pattern
	    // Return as an array to help with the constructor
	    
	    List<String> rawPatterns = _opts.getList(OPT_FILTERS);
	    for (String rawPattern : rawPatterns) {
	        // convert rawPattern to real Pattern
	        String[] split = rawPattern.split("=", 2);
	        Character token = split[0].charAt(0);
	        String regEx = split[1];
	        Pattern pattern = Pattern.compile(regEx, Pattern.DOTALL);
	        analzyer.addTokenFilter(token, pattern);
	    }
	}

	/*
	 * Initialization method; sets everything up
	 */
	public static void init (String[] args) throws FileNotFoundException {

		loadLog4j();
		_log = LogFactory.getLog(LogAnalyzerApp.class);
		_log.debug("init:" + args);
		_analyzers = new ArrayList<FastLogAnalyzer>();

		getOptions(args);

		_layoutPattern = _opts.getStr(OPT_LAYOUT_PATTERN);
		_log.debug("layoutPattern: " + _layoutPattern);

		// Setup analyzers
		String timeSlice = _opts.getStr(OPT_TREND_SEGMENT);
		_timeSlice = Long.valueOf(timeSlice);
		_log.debug("timeSlice: " + _timeSlice);

		List<String> types = _opts.getList(OPT_TYPE);
		for (String type : types) {
			if (type.equals(ANALYZER_TIMER)) {
				// currently only one type is supported
				LogTimer analyzer = new LogTimer(_layoutPattern);
				_analyzers.add(analyzer);		
			} else if (type.equals(ANALYZER_TRENDER)) {
				LogTrender trender = new LogTrender(_layoutPattern, _timeSlice);
				_analyzers.add(trender);
			} else if (type.equals(ANALYZER_ERROR)) {
				LogErrorSummary errorSummary = new LogErrorSummary(_layoutPattern);
				_analyzers.add(errorSummary);
			} else if (type.equals(ANALYZER_METHOD)) {
				String className = _opts.getStr(OPT_TAREGET_CLASS);
				String methodName = _opts.getStr(OPT_TARGET_METHOD);
				if (methodName == null) {
					throw new OptionParseException(OPT_TARGET_METHOD + " must be specified when using analyzer type " + ANALYZER_METHOD, _opts, true);
				}
				LogMethodCallSummary methodSummary = new LogMethodCallSummary(_layoutPattern, className, methodName);
				_analyzers.add(methodSummary);
			} else if (type.equals(ANALYZER_JOINER)) {
				// joining is done by this app, not by an analyzer
				_join = true;
			} else if (type.equals(ANALYZER_FORMATTER)) {
				LogFormatter formatter = new LogFormatter(_layoutPattern);
				_analyzers.add(formatter);
			} else if (type.equals(ANALYZER_FILTER)) {
			    boolean exclusive = Boolean.valueOf(_opts.getStr(OPT_FILTER_EXCLUSIVE));
	             TokenFilterAnalyzer filter = new TokenFilterAnalyzer(exclusive, _layoutPattern);
			    setFilters(filter);
			    _analyzers.add(filter);
			} else if (type.equals(ANALYZER_TEST)) {
			    LogTestParse testParse = new LogTestParse(_layoutPattern);
			    _analyzers.add(testParse);
			} else if (type.equals(ANALYZER_DUPE)) {
			    DupeFilterAnalyzer dupeAnalyzer = new DupeFilterAnalyzer(_layoutPattern);
			    _analyzers.add(dupeAnalyzer);
			}
		}
		
		boolean doFast = Boolean.valueOf(_opts.getStr(OPT_FAST_PARSE));
		if (doFast) {
			int limit = Integer.valueOf(_opts.getStr(OPT_FAST_PARSE_LIMIT));
			for (FastLogAnalyzer analyzer : _analyzers) {
				analyzer.setDoFast(doFast);
				analyzer.setFastParseCharacterLimit(limit);
			}
		}
		
		_log.debug("analyzers: " + _analyzers);

		String output = _opts.getStr(OPT_OUT);
		if (output == null) {
			_out = System.out;
		} else {
			_out = new PrintStream(output);
		}

		_fileList = _opts.getList (OPT_FILE);
		//String[] crap = {"C:\\cu_data\\SocGen\\5490 - Performance aCrappy\\30minutes\\sailpoint-UAT01-SCHILLER.log*"};
		//_fileList = Arrays.asList(crap);
		_log.debug("fileName: " + _fileList);
	}
	
	private static void getOptions(String[] args) {
		// Parse options
		_log.debug("Parsing commandline options from: " + Arrays.toString(args));
		_opts = new GetOpts(LogAnalyzerApp.class);

		// Start hiding the secondary options as to encourage using properties
		// Further, unhide the properties options
		_opts.setPropertiesLegendHidden(false);

		// Override the default properties file name
		_opts.setPropertiesDefaultFileName("analyzeLog.properties");

		OptionLegend legend = new OptionLegend(OPT_FILE);
		legend.setRequired(true);
		legend.setMulti(true);
		legend.setDescription("Log4j log to parse.  This may be individual file names or a filename filter (like \"*.log\").  Do note that when using wildcards, the string must be quoted");
		_opts.addLegend(legend);

		legend = new OptionLegend (OPT_TYPE);
		legend.setRequired(true);
		legend.setMulti(true);
		String[] allowedValues = TYPE_ALLOWED_VALUES;
		legend.setAllowedValues(allowedValues);
		legend.setDescription("Type of analysis to perform");
		_opts.addLegend(legend);

		legend = new OptionLegend (OPT_LAYOUT_PATTERN);
		legend.setRequired(false);
		legend.setDescription("Log4j LayoutPattern used for this log.  Some platforms (like Windows) require you to escape the % character with a second %, ala \"%d{ISO8601}\" becomes \"%%d{ISO8601}\"");
		legend.setDefaultValue(AbstractTraceAspectLogAnalyzer.DEFAULT_LAYOUT_PATTERN);
		_opts.addLegend(legend);

		legend = new OptionLegend (OPT_TREND_SEGMENT);
		legend.setRequired(false);
		legend.setDescription("Increment to trend method calls over (in milliseconds)");
		legend.setDefaultValue(String.valueOf(LogTrender.DEFAULT_TIME_SLICE));
		legend.setIsHidden(true);
		_opts.addLegend(legend);

		legend = new OptionLegend (OPT_TAREGET_CLASS);
		legend.setRequired(false);
		legend.setDescription("Target class to determine call stack for.  Used for Method Summary only");
		legend.setIsHidden(true);
		_opts.addLegend(legend);

		legend = new OptionLegend (OPT_TARGET_METHOD);
		legend.setRequired(false);
		legend.setDescription("Target method to determine call stack for.  Used for Method Summary only.  Required when using " +ANALYZER_METHOD + " analyzer type");
		legend.setIsHidden(true);
		_opts.addLegend(legend);

		legend = new OptionLegend (OPT_OUT);
		legend.setRequired(false);
		legend.setDescription("Output file.  STDOUT used if not specified");
		_opts.addLegend(legend);
		
		legend = new OptionLegend (OPT_PROPERTY_GROUP);
		legend.setRequired(false);
		legend.setIsHidden(true);
		legend.setDescription("Property group to parse from supplied properties file");
		//_opts.addLegend(legend);
		// Still working out how to implement this
		
		// enable 'fast parse' mode
		legend = new OptionLegend (OPT_FAST_PARSE);
		legend.setRequired(false);
		legend.setIsHidden(true);
		legend.setFlag(true);
		legend.setDescription("When set to true, a character limit is applied to log events to reduce cycle time during pattern matching");
		_opts.addLegend(legend);
		
		// for 'fast parse' mode, set the character limit
		legend = new OptionLegend (OPT_FAST_PARSE_LIMIT);
		legend.setRequired(false);
		legend.setIsHidden(true);
		legend.setDefaultValue(String.valueOf(FastLogAnalyzer.FAST_LIMIT));
		_opts.addLegend(legend);
		
		// for filter mode
		legend = new OptionLegend(OPT_FILTERS);
		legend.setDescription("Token specific regular expression that will be used to filter against each log event");
		legend.setExampleValue("c=.*web\\.certification.*");
		legend.setRequired(false);
		legend.setIsHidden(true);
		legend.setMulti(true);
		_opts.addLegend(legend);
		
		legend = new OptionLegend(OPT_FILTER_EXCLUSIVE);
		legend.setFlag(true);
		legend.setDescription("When enabled, filters supplied are exclusion filters instead of inclusion filters");
		_opts.addLegend(legend);
		

		_opts.setDescriptionTail("\nAnalyzer types supported are:" +
				"\n\ttimer: Provides call timing for each method logged." +
				"\n\terror: Formats error messages to include the call stack leading up to the error" +
				"\n\ttrender: Compiles a trend of method calls for each method over a defined segment of time." +
				"\n\tjoiner: Joins the provided logs based on individual file names and / or file name filters.  The resulting log will be contiguous and in order" +
		        "\n\tmethod: Formats call stacks for the target method." + 
				"\n\tfilter: Filters the log events");
		_opts.parseOpts(args);

		_log.debug(_opts.toString());
	}

	private static void loadLog4j() {

		File f = new File (LOG4J_PROPERTIES);
		if (f.exists()) {
			if (DEBUG) {
				iterateFile(f);
			}
			PropertyConfigurator.configure(LOG4J_PROPERTIES);
		} else {
			// Default properties
			Properties props = new Properties();
			props.setProperty("log4j.appender.stdout","org.apache.log4j.ConsoleAppender");
			props.setProperty("log4j.appender.stdout.Target","System.out");
			props.setProperty("log4j.appender.stdout.layout","org.apache.log4j.PatternLayout");
			props.setProperty("log4j.appender.stdout.layout.ConversionPattern","%d{ISO8601} %5p %t %c{4}:%L - %m%n");
			props.setProperty("log4j.rootLogger","warn,stdout");
			
			PropertyConfigurator.configure(props);
		}
	}

	private static void iterateFile(File f) {
		try {
			FileReader r = new FileReader(f);
			BufferedReader br = new BufferedReader(r);
			String line = br.readLine();
			System.out.println(line);
			while (line != null && br.ready()) {
				line = br.readLine();
				System.out.println(line);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	/**
	 * Entry method
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		init(args);

		String[] fileNameList = _fileList.toArray(new String[_fileList.size()]);
		MultiFileLog4jLineIterator it =  new MultiFileLog4jLineIterator (fileNameList, _layoutPattern);
		for (String logEvent : it) {
			_log.trace("Analyzing: " + logEvent);
			boolean cont = analyze(logEvent);
			if (_join || !cont) {
				_out.println (logEvent);
			}
			if (!cont) {
			    break;
			}
		}

		// done reading
		summarize();
	}

	/*
	 * Outputs all the summaries
	 */
	private static void summarize() {
		for (LogAnalyzer analyzer : _analyzers) {
			_out.println(analyzer.compileSummary());
		}	
	}
}
