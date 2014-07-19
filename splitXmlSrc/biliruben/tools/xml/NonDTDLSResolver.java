package biliruben.tools.xml;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;


public class NonDTDLSResolver implements LSResourceResolver {
	private LSInput _input;
	
	public NonDTDLSResolver (DOMImplementationLS lsImpl) {
		_input = new LSDTDLessInput(lsImpl);
	}
	
	public LSInput resolveResource(String type, String namespaceURI,
			String publicId, String systemId, String baseURI) {
		// TODO Auto-generated method stub
		return _input;
	}
	
	private class LSDTDLessInput implements LSInput {
		private LSInput _input;

		public LSDTDLessInput(DOMImplementationLS lsImpl) {
			_input = lsImpl.createLSInput();
		}
		
		public String getBaseURI() {
			// TODO Auto-generated method stub
			return _input.getBaseURI();
		}

		public InputStream getByteStream() {
			// TODO Auto-generated method stub
			return _input.getByteStream();
		}

		public boolean getCertifiedText() {
			// TODO Auto-generated method stub
			return _input.getCertifiedText();
		}

		public Reader getCharacterStream() {
			// TODO Auto-generated method stub
			//return _input.getCharacterStream();
			return new StringReader("");
		}

		public String getEncoding() {
			// TODO Auto-generated method stub
			return _input.getEncoding();
		}

		public String getPublicId() {
			// TODO Auto-generated method stub
			return _input.getPublicId();
		}

		public String getStringData() {
			// TODO Auto-generated method stub
			return _input.getStringData();
		}

		public String getSystemId() {
			// TODO Auto-generated method stub
			return _input.getSystemId();
		}

		public void setBaseURI(String baseURI) {
			// TODO Auto-generated method stub
			_input.setBaseURI(baseURI);
		}

		public void setByteStream(InputStream byteStream) {
			// TODO Auto-generated method stub
			_input.setByteStream(byteStream);
		}

		public void setCertifiedText(boolean certifiedText) {
			// TODO Auto-generated method stub
			_input.setCertifiedText(certifiedText);
		}

		public void setCharacterStream(Reader characterStream) {
			/*
			// TODO Auto-generated method stub
			StringBuffer sb = new StringBuffer();
			BufferedReader br = new BufferedReader(characterStream);
			try {
				String line = br.readLine();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			*/
			_input.setCharacterStream(characterStream);
		}

		public void setEncoding(String encoding) {
			// TODO Auto-generated method stub
			_input.setEncoding(encoding);
		}

		public void setPublicId(String publicId) {
			// TODO Auto-generated method stub
			_input.setPublicId(publicId);
		}

		public void setStringData(String stringData) {
			// TODO Auto-generated method stub
			_input.setStringData(stringData);
		}

		public void setSystemId(String systemId) {
			// TODO Auto-generated method stub
			_input.setSystemId(systemId);
		}

	}

}
