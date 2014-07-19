package biliruben.tools.xml;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import biliruben.threads.ThreadRunner;
import com.biliruben.util.GetOpts;
import com.biliruben.util.OptionLegend;
/**
 * SplitXml is designed to take in an xml document that would presumably
 * have individual objects represented as child elements of the root and
 * extract these objects into individual xml files.
 * Applications such as Sailpoint ComplianceIQ and Sun Identity Manager
 * often export xml files in such a manner.  The purpose of splitting these
 * objects into individual files allows the user to easily search, edit and
 * generally manage these objects.<br>
 * <br>
 * @author Trey Kirk
 *
 */
public final class SplitXml {	

	static final String DEFAULT_SAX_PARSER = "org.apache.xerces.parsers.SAXParser";
	static final String DEFAULT_SPACE = " ";
	static final int DEFAULT_THREADS_MAX = 10;
	static final int DEFAULT_THREADS_REPORT = 500;
	static final String DEFAULT_UNIQUE_ATTR = "name";
	//static final String OPT_EXPORT_WITH_DTD = "exportWithDTD";
	static final String OPT_IMPORT_FILE = "createImport";
	//static final String OPT_NO_VALIDATION = "noValidation";
	static final String OPT_OUT_DIR = "outDir";
	static final String OPT_PARSER = "useParser";
	static final String OPT_SPACE_STR = "spaceChar";
	static final String OPT_THREAD_REPORT = "threadsReportIncr";
	//static final String OPT_THREADS_MAX = "threads";
	static final String OPT_UNIQUE_ATTR = "uniqueAttr";
	static final String OPT_USE_NUMBER = "useNumbers";
	//static final String OPT_USE_THREADS = "useThreads";
	static final String OPT_XML_IN_FILE = "xmlInFile";
	public static final String OPT_THREAD_RUNNER_PROPERTIES = "threadCfg";
	static final String OPT_EXCLUDE_FILTER = "exclude";

	private static SplitterHandler _handler;
	private static String _inputFile;
	private static GetOpts _opts;
	private static XMLReader _reader;
	private static String _space = DEFAULT_SPACE;

	private static XMLReader genReader() {
		return genReader(DEFAULT_SAX_PARSER);
	}

	private static XMLReader genReader(String parser) {
		XMLReader reader;
		try {
			reader = XMLReaderFactory.createXMLReader(parser);
			// set our content handler
			reader.setContentHandler(_handler);
			reader.setDTDHandler(_handler);

			// Setting the SplitterHandler as the entity resolver.  It, in turn,
			// wraps around a LightEntityResolver
			reader.setEntityResolver(_handler);

		} catch (SAXException e) {
			// Error initializing the reader.  Cannot continue.
			e.printStackTrace();
			throw (new RuntimeException(e.getCause()));
		}

		return reader;
	}

	private static void init(String[] args) {
		_opts = new GetOpts(SplitXml.class);
		OptionLegend legend;

		// Hidden for now.  In abstracting the SplitterHandler and the Serializer, I've now
		// lost a way to abstract this capability to this class, which is where it belongs.
		legend = new OptionLegend(OPT_IMPORT_FILE);
		legend.setDescription("Indicates that this should create an import file");
		legend.setFlag(true);
		//_opts.addLegend(legend);
		
		legend = new OptionLegend(OPT_THREAD_RUNNER_PROPERTIES);
		legend.setDescription("Optional properties file for ThreadRunner configuration");
		legend.setIsHidden(true);
		legend.setRequired(false);
		_opts.addLegend(legend);

		legend = new OptionLegend(OPT_UNIQUE_ATTR);
		legend.setDescription("Specify the attribute that uniquely identifies the xml object.  The default is '" + DEFAULT_UNIQUE_ATTR + "'.");
		_opts.addLegend(legend);

		legend = new OptionLegend(OPT_USE_NUMBER);
		legend.setDescription("Indicates that each xml file name will include an incrementing number to ensure uniqueness.  The default is 'false'.");
		legend.setFlag(true);
		_opts.addLegend(legend);

		legend = new OptionLegend (OPT_OUT_DIR);
		legend.setDescription("Output directory to save the sub xml files to");
		legend.setRequired(true);
		_opts.addLegend(legend);

		legend = new OptionLegend (OPT_PARSER);
		legend.setDescription("SAX parser to parse the input xml");
		legend.setRequired(false);
		//_opts.addLegend(legend);

		legend = new OptionLegend (OPT_XML_IN_FILE);
		legend.setDescription("XML file to parse into sub files");
		legend.setRequired(true);
		_opts.addLegend(legend);

		legend = new OptionLegend (OPT_SPACE_STR);
		legend.setDescription("String used to replace spaces (if spaces are not desired)");
		legend.setRequired(false);
		_opts.addLegend(legend);
		
		legend = new OptionLegend (OPT_EXCLUDE_FILTER);
		legend.setDescription("Breadcrumb paths to exclude");
		legend.setRequired(false);
		legend.setMulti(true);
		legend.setExampleValue("root.Element");
		_opts.addLegend(legend);

		_opts.parseOpts(args);

	}

	public static void main(String[] args) {
		init(args);
		setup();
		parse();
	}

	private static void parse() {
		try {

			// Set FileInputStream and InputSource
			FileInputStream inputStream = new FileInputStream(_inputFile);
			InputSource inputXml = new InputSource(inputStream);

			// Get to the parsing.
			_reader.parse(inputXml);
		} catch (Exception e) {
			e.printStackTrace();
			// FileNotFoundException is the likely exception.  But we can pretty much
			// just catch whatever exception and spit out the info.  Doesn't matter
			// a whole bunch as we're pretty done at this point anyways.
			//
			// May also get parse errors.
		}
	}


	private static void setup() {
		File outFile = new File(_opts.getStr(OPT_OUT_DIR));
		if (!outFile.isDirectory()) {
			throw new RuntimeException(OPT_OUT_DIR + " must be a directory!");
		}

		String outDir = outFile.getAbsolutePath() + File.separator;
		String inFile = _opts.getStr(OPT_XML_IN_FILE);

		// SplitterHandler should do this
		// Init splitter (a content handler in disguise)
		ThreadRunner runner = null;
		//boolean useThreads = _opts.getStr(OPT_USE_THREADS).equalsIgnoreCase("true");
		// just do it
		boolean useThreads = true;

		_handler = new SplitterHandler(outDir);
		
		if (useThreads) {
			int maxThreads;
			int report;
			maxThreads = DEFAULT_THREADS_MAX;
			report = DEFAULT_THREADS_REPORT;
			
			String threadCfg = _opts.getStr(OPT_THREAD_RUNNER_PROPERTIES);
			if (threadCfg != null) {
				// we have thread properties, use those
				runner = new ThreadRunner(threadCfg, "SplitXml");
			} else {
				runner = new ThreadRunner(maxThreads);
				runner.setReportIncrement(report);
			}
			_handler.setUseRunner(true);
			_handler.setRunner(runner);
		}

		List<String> filters = _opts.getList(OPT_EXCLUDE_FILTER);
		if (filters != null) {
			_handler.setFilters(filters);
		}
		
/*
		if (runner != null) {
			handler.setRunner(runner);
			handler.setUseRunner(true);
		} else {
			handler.setUseRunner(false);
		}
		*/
		//_handler = handler;
		_inputFile = inFile;

		if (_opts.getStr(OPT_UNIQUE_ATTR) != null) {
			_handler.setUniqueAttr(_opts.getStr(OPT_UNIQUE_ATTR));
		} else {
			_handler.setUniqueAttr(DEFAULT_UNIQUE_ATTR);
		}

		_handler.setUseNumbers(Boolean.parseBoolean(_opts.getStr(OPT_USE_NUMBER)));

		// What to use for spaces
		String space = _opts.getStr(OPT_SPACE_STR);
		if (space != null) {
			_space = space;
		}
		_handler.setSpace(_space);

		String parserClass = _opts.getStr(OPT_PARSER);

		if (parserClass == null) {
			_reader = genReader();
		} else {
			System.out.println("Using parser: " + parserClass);
			_reader = genReader(parserClass);
		}
	}

	private SplitXml() {}
}