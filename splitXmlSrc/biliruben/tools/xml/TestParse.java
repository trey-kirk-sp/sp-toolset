/**
 * 
 */
package biliruben.tools.xml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import com.biliruben.util.GetOpts;
import com.biliruben.util.OptionLegend;
import com.biliruben.util.OptionParseException;

/**
 * @author trey.kirk
 *
 */
public class TestParse extends DefaultHandler {

	private static GetOpts _opts;
	private static String OPT_VERBOSE = "v";
	private static String _inputFile;
	private static String _indent = "   ";
	private static int _indentValue = 0;
	private EntityResolver _resolver;
	private static String OPT_DTD = "dtd";
	
	private boolean _verbose = false;
	
	private static void init(String[] args) {
		_opts = new GetOpts(TestParse.class);
		_opts.setUsageTail("filename");
		_opts.setDescriptionTail("\tfilename: XML file to perform the test parse on.");
		
		OptionLegend legend = new OptionLegend(OPT_VERBOSE);
		legend.setRequired(false);
		legend.setFlag(true);
		legend.setDescription("Indicates if the verbose information about the xml should be output");
		_opts.addLegend(legend);
		
		legend = new OptionLegend(OPT_DTD);
		legend.setRequired(false);
		legend.setDescription("DTD file used to check the grammar of the xml file.  Otherwise grammar checking is skipped");
		_opts.addLegend(legend);
		
		_opts.setOpts(args);
		
		List<String> unswitched = _opts.getUnswitchedOptions();
		
		if (unswitched == null || unswitched.size() != 1) {
			throw new OptionParseException("Invalid filename provided!", _opts);
		}
		
		_inputFile = unswitched.get(0);
		
		
 		
	}
	
	public TestParse(boolean verbose) {
		_verbose = verbose;
	}
	
	public TestParse() {
		this (false);
	}
	
	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		// TODO Auto-generated method stub
		// Would like some better cmd-line parameter reading here
		//String inputFile = args[0];
		
		// And now I do
		init(args);
		
		// Init splitter (a content handler in disguise)
		TestParse handler = new TestParse(Boolean.parseBoolean(_opts.getStr(OPT_VERBOSE)));

		// set our reader
		XMLReader reader;
		
		try {
			// Tied pretty closely to Xerces parser.  Would like to come
			// back to this and allow more flexibility to allow the user
			// to provide a parser of choice and default to Xerces otherwise
			reader = XMLReaderFactory
					.createXMLReader("org.apache.xerces.parsers.SAXParser");
		} catch (SAXException e) {
			// Error initializing the reader.  Cannot continue.
			e.printStackTrace();
			throw (new RuntimeException(e.getCause()));
		}

		// set our content handler
		reader.setContentHandler(handler);
		
		String dtdFile = _opts.getStr(OPT_DTD);
		final Reader dtdReader;
		
		
		
		if (dtdFile == null) {
			dtdReader = new StringReader("");
		} else {
			dtdReader = new FileReader(dtdFile);
		}

		/*
		reader.setEntityResolver(
				new EntityResolver() {
					public InputSource resolveEntity(String publicId, String systemId)
					throws SAXException, IOException {
						return new InputSource(dtdReader);
					}
				}
		);
		 */

		
		try {
			// Set FileInputStream and InputSource
			FileInputStream inputStream = new FileInputStream(_inputFile);
			InputSource inputXml = new InputSource(inputStream);
			
			// Get to the parsing.
			reader.parse(inputXml);
			System.out.println("Success!");
		} catch (Exception e) {
			System.out.println("FAILURE!\n");
			System.out.println(e.getMessage());
		}

	}

	public String attrsToString(Attributes attrs) {
		StringBuffer buffer = new StringBuffer(); 
		for (int i = 0; i < attrs.getLength(); i++) {
			String lName = attrs.getLocalName(i);
			String type = attrs.getType(i);
			String value = attrs.getValue(i);
			buffer.append(getIndent() + type + ":" + lName + " = " + value);
			if (i < attrs.getLength() - 1) {
				buffer.append("\n");
			}
		}
		return buffer.toString();
	}

	private String getIndent() {
		StringBuffer indent = new StringBuffer();
		for (int i = 0; i < _indentValue; i++) {
			indent.append(_indent);
		}

		return indent.toString();
	}
	private void print(String msg) {
			
		if (_verbose) {
			System.out.println(getIndent() + msg);
		}
	}
	
	private void printPreIndent (String msg) {
		if (_verbose) {
			System.out.println(msg);
		}
	}
	
	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		// TODO Auto-generated method stub
		super.characters(ch, start, length);
		String chars = new String(ch, start, length);
		if (_verbose) {
			print("Characters: " + String.valueOf(ch,start, length));
		}
	}
	
	@Override
	public void endDocument() throws SAXException {
		// TODO Auto-generated method stub
		super.endDocument();
		print ("EndDocument");
	}
	
	@Override
	public void startDocument() throws SAXException {
		// TODO Auto-generated method stub
		super.startDocument();
		print ("StartDocument");
	}
	
	@Override
	public void endElement(String uri, String localName, String name)
			throws SAXException {
		// TODO Auto-generated method stub
		super.endElement(uri, localName, name);
		_indentValue--;
		print ("EndElement: " + localName);
	}
	
	@Override
	public void startElement(String uri, String localName, String name,
			Attributes attributes) throws SAXException {
		// TODO Auto-generated method stub
		super.startElement(uri, localName, name, attributes);
		_indentValue++;
		print ("StartElement: " + localName);
		if (attributes.getLength() > 0) {
		print ("Attributes:");
		_indentValue++;
		 printPreIndent(attrsToString(attributes));
		_indentValue--;
		}
	}
	


}
