package biliruben.tools.xml;

import java.io.IOException;
import java.io.StringReader;

import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class LightEntityResolver implements EntityResolver, LSResourceResolver {

	private InputSource _dtd;
	private LSInput _lsDtd;

	public LightEntityResolver() {
		_dtd = new InputSource(new StringReader(""));
		
	}


	public InputSource resolveEntity(String publicId, String systemId)
	throws SAXException, IOException {
		// TODO Auto-generated method stub
		return _dtd;
	}


	public LSInput resolveResource(String type, String namespaceURI,
			String publicId, String systemId, String baseURI) {
		// TODO Auto-generated method stub
		return _lsDtd;
	}
}

