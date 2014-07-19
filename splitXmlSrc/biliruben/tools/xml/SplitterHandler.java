/**
 * 
 */
package biliruben.tools.xml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.jdom.DocType;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.DOMOutputter;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import biliruben.threads.ThreadRunner;

/**
 * This class is a Default Handler specially designed to break down a single xml
 * object into several smaller xml objects.  Given any xml, it will create smaller
 * xml objects using the tags at the level just above the root element.  For example,
 * given the following xml:<br>
 * <br><code>
 * &lt;rootEl><br>
 * &nbsp;&nbsp;&lt;subEl1 attr="some xml"><br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;textEl>there text here&lt;/textEl><br>
 * &nbsp;&nbsp;&lt;/subEl1><br>
 * &nbsp;&nbsp;&lt;subEl2 attr="some xml"><br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;textEl>there text here&lt;/textEl><br>
 * &nbsp;&nbsp;&lt;/subEl2><br>
 * &lt/rootEl><br></code>
 * 
 * SplitterHandler will create two new xml objects with one using 'subEl1' as the root element
 * and the other using 'subEl2' as the root element.  Currently, the only output SplitterHandler
 * supports is to a file using {@link DOMSerializer}.  A future enhancement will allow
 * the user to provide their own receiving object to either serialize or process in some
 * other manner.
 * @author trey.kirk
 *
 */
public class SplitterHandler extends BreadCrumbHandler {

	/**
	 * We have to make sure we don't create a file name too long for the OS
	 */
	private static final int FILE_NAME_SIZE_LIMIT = 240;

	private static final int DEFAULT_THREAD_CAPACITY = 10;

	private static final int DEFAULT_THREAD_WORKLOAD = Integer.MAX_VALUE;

	private static final String DEFAULT_THREAD_BASE_NAME = "Splitter";

	private static final String DEFAULT_UNNAMED_NAME = "UNNAMED";

	private int _counter = 0;

	/**
	 * jdom Document that backs this object.  As we parse our input xml, we
	 * build Document objects that are serialized as we go.
	 */
	private Document _doc;

	/**
	 * Tracks the list of files we're creating for use at the very end.
	 */
	private ArrayList<String> _fileList;
	/**
	 * Tracks our most recent level
	 */
	private int _lastLevel;

	/**
	 * Tracks our current level
	 */
	private int _level;
	/**
	 * Contains the file path to use for all outbound files
	 */
	private String _outPath;
	/**
	 * Tracks our parent elements as we parse.
	 */
	private Stack<Element> _parents;

	/**
	 * A thread manager to parse out the work
	 */
	private ThreadRunner _runner;

	/**
	 * Flag to indicate if we'll be using threads or not
	 */
	boolean _useRunner;

	/**
	 * Public and System ID for the dtd
	 */
	private String _publicId;
	private String _systemId;

	/**
	 * XML attribute to read from to determine a unique name for the XML object
	 */
	private String _uniqueAttr;

	/**
	 * Flag to indicate that we will append the file names with numbers
	 */
	private boolean _useNumbers;

	private List<String> _filters = null;
	private boolean _excluded = false;
	private String _excludingFilter = null;

	/**
	 * Constructor to create the SplittlerHandler object.  Made package-private since the only consumer of this
	 * class is {@link SplitXml) 
	 * @param outputDir Directory to store the new XML files
	 */
	SplitterHandler(String outputDir) {
		setLevel(0);
		setLastLevel(0);
		_parents = new Stack<Element>();
		_fileList = new ArrayList<String>();
		_outPath = outputDir;
		_filters = new ArrayList<String>();
		setRunner();
	}

	SplitterHandler(String outputDir, ArrayList<String> exclusionFilters) {
		this(outputDir);
		_filters = exclusionFilters;
	}

	/**
	 * Increments the level by 1.
	 *
	 */
	private void addToLevel() {
		addToLevel(1);
	}

	/**
	 * Increments the level by increment.  Can't
	 * imagine how this would be used beyond incrementing
	 * by 1, but hey, here it is.
	 * @param increment
	 */
	private void addToLevel(int increment) {
		_level += increment;
	}


	/**
	 * Call-back method called from {@link XMLReader#parse(InputSource)}
	 * to parse source text data.
	 */
	public void characters(char[] characters, int start, int length) {
		// As we parse elements, we'll sometimes get text between elements: <ref>myTextHere</ref>
		// This call-back method persists that text.
		if (getLevel() > 2) {
			StringBuffer sb = new StringBuffer(length);
			for (int i = 0; i < length; i++) {
				sb.append(characters[start + i]);
			}

			// ignore text that's all white space
			if (sb.toString().matches("^\\s*$")) {

			} else {
				Element element = (Element) _parents.peek();
				element.addContent(sb.toString());
			}
			sb = null;
		}
	}


	/**
	 * Call-back method called from {@link XMLReader#parse(InputSource)}
	 * to signify the end of the xml document.  This method takes this
	 * objects backed ArrayList that stores the list of files created
	 * by SplitXml and serializes it to ./import.lh
	 */
	public void endDocument() throws SAXException {
		// All done, spit out the file list as a .lh file.
		File file = new File(_outPath + "import.me");
		try {
			Writer writer = new FileWriter(file);
			Iterator<String> it = _fileList.iterator();
			while (it.hasNext()) {
				String fileName = (String) it.next();
				// Come back to this and provide a way to specify a full path
				// to the created file.  As it stands, that path is PWD.  It would
				// be better if we could specify the output file of the xml files
				// and, in turn, the full paths to be used in this file list.
				writer.write("import \"" + fileName + "\"\n");
			}
			writer.flush();
		} catch (IOException e) {
			// Could get an IOException.  Let the user know.  Could even just spew
			// the list to STDOUT so it's not completely lost... meh
			System.out.println(file.getAbsolutePath() + " not written: ");
			e.printStackTrace();
		} finally {
			_runner.shutDown();
		}

	}

	/**
	 * Call-back method called from {@link XMLReader#parse(InputSource)}
	 * to signify the end of an xml element.  This method determines if
	 * the element that just closed is an element that is one level below
	 * the original highest level.  If so, the completed jdom {@link org.jdom.Document}
	 * is serialized using {@link DOMSerializer#serialize(org.w3c.dom.Document, java.io.OutputStream)}
	 * Note: DOMOutputter is used to convert from org.jdom.Document to
	 * org.w3c.dom.Document.
	 * 
	 * Otherwise, just decrement the integer tracking our cursor level and
	 * continue building the jdom {@link org.jdom.Document}.
	 */
	public void endElement(String namespaceURI, String localName, String qName)
	throws SAXException {
		// Before level is decremented, we check for a Level 2 object.
		if (!excluding()) {
			if (getLevel() == 2) {
				// Finalize the jdom Document and, serialize, update the file list.
				Element rootEl = _doc.getRootElement();
				_counter++;
				String elName = rootEl.getAttributeValue(getUniqueAttr());
				if (elName == null) {
					elName = DEFAULT_UNNAMED_NAME;
				} else {
					elName = genFSFreindlyName(rootEl.getAttributeValue(getUniqueAttr()));
				}
				StringBuffer fileName = new StringBuffer(_outPath + rootEl.getName() + _space + "-" + _space + elName);
				// We don't want REALLY large file names
				if (fileName.length() > FILE_NAME_SIZE_LIMIT) {
					fileName.delete(FILE_NAME_SIZE_LIMIT, fileName.length() - 1);
					// If truncating, I really must insist on guaranteeing a unique file name, so
					// we force a counter in this instance
					fileName.append(_space + _counter);
				} else if (isUsingNumbers()) {
					fileName.append(_space + _counter); 
				}


				fileName.append(".xml");


				_fileList.add(fileName.toString());
				File file = new File(fileName.toString());


				// Hack:  Our DOMSerializer serializes org.w3c.dom.Document objects, not
				// jdom Document objects.  But jdom was nice enough to provide a converter:
				// DOMOutputter#output
				try {
					DOMSerializer serializer = new DOMSerializer();
					DocType type = _doc.getDocType();
					type.setPublicID(getId("PUBLIC"));
					type.setSystemID(getId("SYSTEM"));
					org.w3c.dom.Document myDoc = new DOMOutputter().output(_doc);

					if (useRunner()) {
						serializer.prepareSerializer(myDoc, file);
						_runner.add(serializer);
					} else {
						try {
							serializer.serialize(myDoc, file);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

				} catch (JDOMException e) {
					e.printStackTrace();
				}

				_doc = null;
				// parents needs to be "unwound"
				_parents.removeAllElements();

			}

			// Done with this level, move up
			addToLevel(-1);

		} else {
			// we're excluding.  Check and see if we still need to be.
			checkDoneExcluding();
		}

		super.endElement(namespaceURI, localName, qName);
	}

	private void checkDoneExcluding() {
		if (matchBreadCrumb(MatchMode.EQUALS, _excludingFilter)) {
			_excluded = false;
		}
	}

	/**
	 * 
	 * @param identifier - SYSTEM or PUBLIC
	 * @return The system or public dtd id
	 */
	public String getId(String identifier) {
		// Ya, this is kinda silly.  Return to this and make it more xml worthy
		if (identifier.equalsIgnoreCase("SYSTEM")) {
			return _systemId;
		}

		if (identifier.equalsIgnoreCase("PUBLIC")) {
			return _publicId;
		}

		return null;
	}

	/**
	 * @return the previous element's level
	 */
	private int getLastLevel() {
		return _lastLevel;
	}

	/**
	 * 
	 * @return the current level
	 */
	private int getLevel() {
		return _level;
	}

	private void setRunner() {
		if (_runner == null) {
			_runner = new ThreadRunner(DEFAULT_THREAD_CAPACITY, DEFAULT_THREAD_WORKLOAD, DEFAULT_THREAD_BASE_NAME);
		}
	}

	public void setRunner(ThreadRunner runner) {
		if (runner != null) {
			_runner = runner;
		}
	}

	/**
	 * Converts non-"word" characters to the "space" character.
	 * Also collapses any repeated "space" characters into just one.
	 */
	private String genFSFreindlyName(String in) {
		return in.replaceAll("\\W+", _space);
	}

	/**
	 * Sets a public or system id to the provided dtd.
	 * This method is called by {@link #getDocType(String)} when
	 * building the DocType object.
	 * @param identifier "SYSTEM" or "PUBLIC"
	 * @param dtd DTD to assign to the provided id
	 */
	public void setId(String identifier, String dtd) {
		if (identifier.equalsIgnoreCase("SYSTEM")) {
			_systemId = dtd;
		}

		if (identifier.equalsIgnoreCase("PUBLIC")) {
			_publicId = dtd;
		}
	}


	/**
	 * Sets the last level visited
	 * @param level Last level visited
	 */
	private void setLastLevel(int level) {
		_lastLevel = level;
	}
	/**
	 * Sets the current level
	 * @param level int of current level
	 */
	private void setLevel(int level) {
		_level = 0;
	}

	protected void setUseRunner(boolean runner) {
		this._useRunner = runner;
	}

	/**
	 * Returns the DocType object to be incorporated into the jdom Document
	 * being built.
	 * @param element Name of the DocType element
	 * @return DocType object
	 */
	private DocType getDocType(String element) {
		// we need to return the same DocType as the inbound xml, not what we specified
		// as a property.  This way we can use the dummy EntityResolver

		return new DocType(element, getId("PUBLIC"), getId("SYSTEM"));
	}



	private boolean excluding() {
		if (_excluded) {
			return true;
		} else {
			for (String filter : _filters) {
				if (matchBreadCrumb(MatchMode.EQUALS, filter)) {
					_excludingFilter = filter;
					_excluded = true;
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Major worker method.  Call-back method called from
	 * {@link XMLReader#parse(InputSource)} as it parses the
	 * original xml file.  For each element started, the level
	 * is examined:<br>
	 * <br>
	 *  - if the level is the 1st level, indicating the root element
	 *  of our original, then the element is ignored.<br>
	 *  - if the level is the 2nd level, it indicates an element we
	 *  want to make as the root element to a new jdom Document.<br>
	 *  - higher levels are nested appropriately in the
	 *  jdom Document.
	 *   
	 */
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) throws SAXException {
		super.startElement(namespaceURI, localName, qName, atts);
		if (!excluding()) {
			addToLevel();
			Element element = new Element(localName);
			for (int i = 0; i < atts.getLength(); i++) {
				element.setAttribute(atts.getLocalName(i), atts.getValue(i));
			}

			if (getLevel() == 2) {
				// Level 2, new "top" level object. Create a Document object
				// and commence with the populating.
				_doc = new Document(element, getDocType(localName));

				//_doc.setDocType(new DocType (localName, getId("SYSTEM"), getId("PUBLIC")));

				setLastLevel(getLevel());
				_parents.push(element);
			}

			if (getLevel() > 2) {
				// All other elements gotta get served here
				if (getLastLevel() >= getLevel()) {
					// Last level is equal or higher than what level is,
					// that means we have to pop out dead-beat parents until
					// we get to one we like.
					//
					// Ex. if lastLevel == 5 and level == 3, level 2 is our
					// parent element. That element is 1st in our stack of
					// (currently) 4. So (lastLevel - level + 1) parents need
					// to be popped off.
					int levelsToPop = getLastLevel() - getLevel() + 1;
					for (int i = 0; i < levelsToPop; i++) {
						_parents.pop();
					}
				}

				// Meet the parent
				Element parent = (Element) _parents.peek();
				parent.addContent(element);
				_parents.push(element);
				setLastLevel(getLevel());
			}
		}
	}
	public boolean useRunner() {
		return this._useRunner;
	}
	/**
	 * @return the _uniqueAttr
	 */
	public String getUniqueAttr() {
		return _uniqueAttr;
	}
	/**
	 * @param attr the _uniqueAttr to set
	 */
	public void setUniqueAttr(String attr) {
		_uniqueAttr = attr;
	}
	/**
	 * @return the _useNumbers
	 */
	public boolean isUsingNumbers() {
		return _useNumbers;
	}
	/**
	 * @param numbers the _useNumbers to set
	 */
	public void setUseNumbers(boolean numbers) {
		_useNumbers = numbers;
	}

	private String _space = " ";

	public void setSpace(String space) {
		_space = space;
	}
	LightEntityResolver _resolver = new LightEntityResolver();
	@Override
	public InputSource resolveEntity(String publicId, String systemId)
	throws IOException, SAXException {
		// TODO Auto-generated method stub
		if (_publicId == null || _systemId == null) {
			setId("PUBLIC", publicId);
			setId("SYSTEM", systemId);
			if (systemId.matches("file:.*")) {
				File f = new File (systemId);
				if (!new File(systemId).exists()) {
					setId("SYSTEM", publicId);
				}
			}
		}
		return _resolver.resolveEntity(publicId, systemId);
	}

	public void setFilters(List<String> filters) {
		_filters = filters;

	}
}