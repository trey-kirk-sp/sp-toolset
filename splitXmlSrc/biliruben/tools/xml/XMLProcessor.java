package biliruben.tools.xml;
import java.beans.DefaultPersistenceDelegate;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;



/**
 * Convenience class to initiate an xml parser and reader as well as register a handler
 * @author trey.kirk
 *
 */
public class XMLProcessor {
	
	/**
	 * The default EntityResolver is a {@link LightEntityResolver} which simply avoids any DTD verification.
	 * This is useful if you do not wish to implement the overhead of an EntityResolver and only want
	 * to read the xml contents.  If it is important to verify the XML in addition to reading the content, you
	 * will need to use the full constructor.
	 */
	public static final EntityResolver DEFAULT_ENTITY_RESOLVER = new LightEntityResolver();
	
	/**
	 * The default parser is org.apache.xerces.parsers.SAXParser.  Use the full constructor or the setter method
	 * to specify a different parser.
	 */
	public static final String DEFAULT_PARSER_CLASS = "org.apache.xerces.parsers.SAXParser";
	
	private DefaultHandler _defaultHandler;
	private String _parser;
	private XMLReader _reader;
	private EntityResolver _resolver;
	private File _xmlFile;

	private InputStream _xmlStream;
	
	/**
	 * Constructs the XMLProcessor given an xml file and DefaultHandler.  A default EntityResolver and parser class
	 * are used.  
	 * @param xmlFile Xml file to parse, may be provided as a full or relative path.
	 * @param handler DefaultHandler that will handle the xml events during the parse.
	 * @throws SAXException
	 * @see DEFAULT_ENTITY_RESOLVER
	 * @see DEFAULT_PARSER_CLASS
	 */
	public XMLProcessor (String xmlFile, DefaultHandler handler) throws SAXException {
		this (xmlFile, DEFAULT_PARSER_CLASS, handler, new LightEntityResolver());
	}
	
	/**
	 * Constructs the XMLProcessor
	 * @param xmlFile
	 * @param parser
	 * @param handler
	 * @param resolver
	 * @throws SAXException
	 */
	public XMLProcessor (String xmlFile, String parser, DefaultHandler handler, EntityResolver resolver) throws SAXException {
		_xmlFile = new File(xmlFile);
		_defaultHandler = handler;
		_resolver = resolver;
		setParser (parser);
	}
	
	public XMLProcessor (InputStream xmlStream, DefaultHandler handler) throws SAXException {
		this (xmlStream, DEFAULT_PARSER_CLASS, handler, new LightEntityResolver());
	}
	
	public XMLProcessor (InputStream xmlStream, String parser, DefaultHandler handler, EntityResolver resolver) throws SAXException {
		_xmlStream = xmlStream;
		_defaultHandler = handler;
		_resolver = resolver;
		setParser (parser);
	}
	
	private void init() throws SAXException {
		_reader = XMLReaderFactory.createXMLReader(_parser);
		_reader.setContentHandler(_defaultHandler);
		_reader.setEntityResolver(_resolver);
	}
	
	public void parse() throws IOException, SAXException {
		// Set FileInputStream and InputSource
		InputStream inputStream;
		if (_xmlFile != null) {
			inputStream = new FileInputStream(_xmlFile);
		} else {
			inputStream = _xmlStream;
		}
		InputSource inputXml = new InputSource(inputStream);
		_reader.parse(inputXml);
	}
	
	public void setHandler (DefaultHandler handler) {
		_defaultHandler = handler;
	}
	
	public void setParser (String parser) throws SAXException {
		_parser = parser;
			init();
	}
	
	public void setReader (XMLReader reader) {
		_reader = reader;
	}
	
	
	public void setResolver (EntityResolver resolver) {
		_resolver = resolver;
	}
	
	public void setXmlFile (String xmlFile) {
		_xmlFile = new File(xmlFile);
	}
}
