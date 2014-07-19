/**
 * 
 */
package biliruben.tools.xml;

import java.io.File;
import java.io.Writer;

import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSParser;
import org.w3c.dom.ls.LSSerializer;

/**
 * @author trey.kirk
 *
 */
public class LSDomSerializer {

	
	private DOMImplementationLS _lsImpl;
	
	public LSDomSerializer() throws ClassCastException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		loadImplementation();
	}
	
	public void serialize(String fromFile, Writer toWriter) {
		
		// make teh parser
		LSParser parser = _lsImpl.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null);
		DOMConfiguration config = parser.getDomConfig();
		config.setParameter("validate", false);
		
		// make teh doc
		File f = new File(fromFile);
		Document doc = parser.parseURI(f.toURI().toString());
		serialize(doc, toWriter);
	}

	private void loadImplementation() throws ClassCastException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		// load the implementation
		DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
		_lsImpl = (DOMImplementationLS)registry.getDOMImplementation("LS");
	}

	public void serialize(Document fromDoc, Writer toWriter) {
		// right it to te fiel
		LSSerializer serializer = _lsImpl.createLSSerializer();
		LSOutput output = _lsImpl.createLSOutput();
		output.setCharacterStream(toWriter);
		serializer.write(fromDoc, output);
	}

}
