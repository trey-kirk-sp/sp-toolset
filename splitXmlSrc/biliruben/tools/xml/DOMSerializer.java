package biliruben.tools.xml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.w3c.dom.Document;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

public class DOMSerializer implements Runnable {

	private static DOMImplementationLS _lsImpl;

	private static DOMImplementationRegistry _registry;

	public static final String DOCTYPE_STR_PUBLIC = "PUBLIC";

	public static final String DOCTYPE_STR_SYSTEM = "SYSTEM";
	
	static {
		try {
			setRegistry(DOMImplementationRegistry.newInstance());
			setLsImpl(getRegistry());
		} catch (Exception e) {
			// DOMImplementationRegistry.newInstance() can throw a butt-ton.  None of them
			// are good.  Bail.
			throw new RuntimeException (e);
		}
		

	}

	protected static DOMImplementationLS getLsImpl() {
		return DOMSerializer._lsImpl;
	}
	protected static DOMImplementationRegistry getRegistry() {
		return DOMSerializer._registry;
	}
	protected static  void setLsImpl(DOMImplementationRegistry registry) {
		DOMSerializer._lsImpl = (DOMImplementationLS)registry.getDOMImplementation("LS");
	}

	protected static void setRegistry(DOMImplementationRegistry registry) {
		DOMSerializer._registry = registry;
	}

	private Document _doc;
	private File _file;
	private LSOutput _output;
	private LSSerializer _serializer;


	/**
	 * Default constructor
	 * 
	 */
	public DOMSerializer() {
		this("UTF-8");
	}

	/**
	 * @param encoding
	 *            Specify the encoding used on the DocType. Defaults is 'UTF-8'
	 * @param lineSep
	 *            Specify the line separator. Default is newline: \n
	 * @param increment
	 *            Specify  the number of spaces used in an increment. Default is
	 *            3 per increment.
	 */
	public DOMSerializer(String encoding) {

		setSerializer(getLsImpl().createLSSerializer());
		getSerializer().getDomConfig().setParameter("format-pretty-print", true);
		getSerializer().getDomConfig().setParameter("element-content-whitespace", false);

		LSOutput output = getLsImpl().createLSOutput();
		setOutput(output);

		// load the implementation
		output.setEncoding(encoding);
	}

	protected LSOutput getOutput() {
		return this._output;
	}

	protected LSSerializer getSerializer() {
		return _serializer;
	}

	public void prepareSerializer(Document doc, File file) {
		_doc = doc;
		_file = file;
	}

	public void run() {
		try {
			this.serialize(_doc, _file);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Provided a DOM Document, begins serializing the document to a file.
	 * 
	 * @param doc
	 *            DOM document
	 * @param file
	 *            File to serialize to
	 * @throws IOException
	 */
	public void serialize(Document doc, File file) 
	throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
		Writer writer = new FileWriter(file);
		serialize(doc, writer);
	}

	/**
	 * Provided a DOM Document, begins serializing the document to an
	 * OutputStream.
	 * 
	 * @param doc
	 *            DOM document
	 * @param out
	 *            OutputStream to serialize to
	 * @throws IOException
	 */
	public void serialize(Document doc, OutputStream out) 
	throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
		Writer writer = new OutputStreamWriter(out, getOutput().getEncoding());
		serialize(doc, writer);
	}

	public void serialize(Document doc, Writer writer) 
	throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {

		LSOutput output = getOutput();
		output.setCharacterStream(writer);
		getSerializer().write(doc, output);
	}

	protected void setOutput(LSOutput output) {
		this._output = output;
	}

	protected void setSerializer(LSSerializer serializer) {
		_serializer = serializer;
	}


}
