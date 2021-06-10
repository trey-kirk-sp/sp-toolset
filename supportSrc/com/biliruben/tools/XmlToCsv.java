package com.biliruben.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.biliruben.tools.xml.BreadCrumbHandler;
import com.biliruben.tools.xml.BreadCrumbHandler.MatchMode;
import com.biliruben.tools.xml.XMLProcessor;
import com.biliruben.util.GetOpts;
import com.biliruben.util.OptionLegend;
import com.biliruben.util.csv.CSVRecord;
import com.biliruben.util.csv.CSVUtil;

/**
 * Utility class that takes an XML and outputs the data into a CSV format.  It needs to know the following:<br>
 * - An XML file to parse (no definition validation required)
 * - A bread crumb filter to define what tag(s) to include
 * - An optional flag to parse the full tree of the provided tags
 * @author trey.kirk
 *
 */
public class XmlToCsv {

	private static final String OPT_XML_FILE = "xmlFile";
	private static final String OPT_BREAD_CRUMB = "element";
	private static final String ELEMENT_FIELD_NAME = "Element";
	private static final String OPT_MATCH_MODE = "matchMode";
	private static GetOpts _opts;

	/**
	 * @param args
	 * @throws IOException 
	 * @throws SAXException 
	 */
	public static void main(String[] args) throws SAXException, IOException {

		init(args);
		XmlToCsvOpts opts = new XmlToCsvOpts();
		opts.setCrumbs(_opts.getList(OPT_BREAD_CRUMB));
		opts.setXmlFile(_opts.getStr(OPT_XML_FILE));
		opts.setMatchMode(_opts.getStr(OPT_MATCH_MODE));
		
		XmlToCsv processor = new XmlToCsv(opts);
		processor.parseXml();
	}

	private static void init(String[] args) {
		_opts = new GetOpts(XmlToCsv.class);
		
		OptionLegend legend = new OptionLegend(OPT_XML_FILE);
		legend.setRequired(true);
		legend.setDescription("XML file to parse");
		_opts.addLegend(legend);
		
		legend = new OptionLegend(OPT_BREAD_CRUMB);
		legend.setRequired(false);
		legend.setMulti(true);
		legend.setDescription("Breadcrumb notation of what elements to include. Ex. 'rootElement.subElement.sub2Element' would begin parsing at <rootElement><subElement><sub2Element/>");
		_opts.addLegend(legend);
		
		legend = new OptionLegend(OPT_MATCH_MODE);
		legend.setRequired(false);
		String[] allowedValues = {
				"beginsWith", "contains", "exact"
		};
		legend.setAllowedValues(allowedValues);
		legend.setDefaultValue("exact");
		legend.setDescription("Defines how the bread crumb is matched");
		_opts.addLegend(legend);
		
		_opts.parseOpts(args);
	}

	private OutputStream _out;
	private File _xmlFile;
	private List<String> _crumbs;
	private boolean _optsPassed;
	private CSVRecord _record;
	private MatchMode _mode;

	static public class XmlToCsvOpts {
		private static final String MODE_CONTAINS = "contains";
		private static final String MODE_BEGINS_WITH = "beginsWith";
		private static final String MODE_EXACT = "exact";
		private static final MatchMode DEFAULT_MATCH_MODE = MatchMode.EQUALS;
		private List<String> _crumbs;
		private String _xmlFileName;
		private OutputStream _out;
		private MatchMode _matchMode = DEFAULT_MATCH_MODE;
		
		public XmlToCsvOpts(Map <String, Object> args) {
			this();
			this.setCrumbs((List<String>) args.get(OPT_BREAD_CRUMB));
			this.setXmlFile((String) args.get(OPT_XML_FILE));
			this.setMatchMode((String)args.get(OPT_MATCH_MODE));
		}
		
		public XmlToCsvOpts() {
			
		}

		public void setMatchMode(String matchModeValue) {
			if (MODE_EXACT.equals(matchModeValue)) {
				_matchMode = MatchMode.EQUALS;
			} else if (MODE_BEGINS_WITH.equals(matchModeValue)) {
				_matchMode = MatchMode.BEGINS_WITH;
			} else if (MODE_CONTAINS.equals(matchModeValue)) {
				_matchMode = MatchMode.CONTAINS;
			} else {
				_matchMode = DEFAULT_MATCH_MODE;
			}
		}
		
		public MatchMode getMatchMode() {
			return _matchMode;
		}

		public void setXmlFile(String xmlFileName) {
			if (xmlFileName == null) {
				throw new RuntimeException("XML File Name cannot be null!");
			}
			this._xmlFileName = xmlFileName;
		}
		
		public String getXmlFileName() {
			if (this._xmlFileName == null) {
				throw new RuntimeException("XML File Name has not been specified!");
			}
			return _xmlFileName;
		}
		
		public File getXmlFile() {
			File f = new File(getXmlFileName());
			if (!f.exists()) {
				throw new RuntimeException(f.getPath() + " was not found!");
			} else if (!f.canRead()) {
				throw new RuntimeException(f.getPath() + " could not be read!");
			}
			return f;
		}
		
		public OutputStream getOutputStream() {
			if (this._out == null) {
				return System.out;
			} else {
				return this._out;
			}
		}
		
		public void setOutputStream(OutputStream out) {
			if (out == null) {
				throw new RuntimeException("Output stream cnanot be null!");
			}
			this._out = out;
		}
		
		public void setOutputStream(String fileName) throws FileNotFoundException {
			_out = new FileOutputStream(fileName);
		}
		
		public void setCrumbs(List<String> crumbs) {
			if (crumbs == null) {
				throw new RuntimeException("Crumb list cannot be null!");
			}
			this._crumbs = crumbs;
		}
		
		public void setCrumb(String breadCrumb) {
			List<String> crumbs = new ArrayList<String>();
			crumbs.add(breadCrumb);
			setCrumbs(crumbs);
		}
		
		public void addCrumb(String breadCrumb) {
			if (this._crumbs == null) {
				_crumbs = new ArrayList<String>();
			}
			if (!(_crumbs.contains(breadCrumb))) {
				_crumbs.add(breadCrumb);
			}
		}
		
		public List<String> getCrumbs() {
			if (_crumbs == null) {
				throw new RuntimeException("Bread crumbs have not been specified!");
			}
			return _crumbs;
		}
	}

	protected class XmlToCsvHandler extends BreadCrumbHandler {

		@Override
		public void startElement(String uri, String localName, String name,
				Attributes attributes) throws SAXException {
			super.startElement(uri, localName, name, attributes);
			for (String crumb : _crumbs) {
				if (matchBreadCrumb(_mode, crumb)) {
					// a match, report attributes
					addLine(getBreadCrumb(), attributes);
				}
			}
		}	
		
		protected void addLine (String crumb, Attributes attributes) {
			if (_record == null) {
				_record = new CSVRecord();
				_record.setIncludeFieldNames(true);
			}
			Map<String, String> line = new HashMap<String, String>();
			line.put(ELEMENT_FIELD_NAME, crumb);
			for (int i = 0; i < attributes.getLength(); i++) {
				String keyName = attributes.getLocalName(i);
				String value = attributes.getValue(i);
				line.put(keyName, value);
			}
			_record.addLine(line);
		}
	}
	
	public XmlToCsv(XmlToCsvOpts opts) {
		setOpts (opts);
	}
	
	public void setOpts(XmlToCsvOpts opts) {
		_xmlFile = opts.getXmlFile();
		_out = opts.getOutputStream();
		_crumbs = opts.getCrumbs();
		_optsPassed = true;
		_mode = opts.getMatchMode();
	}
	
	public void parseXml() throws SAXException, IOException {
		if (!_optsPassed) {
			throw new RuntimeException("Parser has not been configured!  " + XmlToCsvOpts.class.getName() + " must be provided before parsing XML.");
		}
		XmlToCsvHandler handler = new XmlToCsvHandler();
		XMLProcessor proc = new XMLProcessor(_xmlFile.getPath(), handler);
		proc.parse();
		report();
	}
	
	public void report() throws IOException {
		CSVUtil.exportToCsv(_record, _out);
		_out.close();
	}
	
}
