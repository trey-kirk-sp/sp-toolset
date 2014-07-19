/**
 * 
 */
package biliruben.tools.xml;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;



/**
 * Wraps a Document object.  Not sure why, though.
 * @author trey.kirk
 *
 */
public class DOMWrapper {

	private Document _domObj;
	private String _encoding;
	private String _pubIdentifier;
	private String _sysIdentifier;
	private String _docType;

	public static DOMWrapper parseXml (File inputXml) {
		DOMWrapper dwrap = new DOMWrapper();
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			db.setEntityResolver(new LightEntityResolver());
			Document doc = db.parse(inputXml);
			doc.getDocumentElement().normalize();
			dwrap.setDomObj(doc);


		} catch (Exception e) {
			throw (new RuntimeException(e));
		}

		return dwrap;

	}

	public static DOMWrapper parseXml (String inputXml) {

		File inputFile = new File (inputXml);
		return DOMWrapper.parseXml(inputFile);

	}

	public String getDocType() {
		return this._docType;
	}

	public void setDocType(String docType) {
		this._docType = docType;
	}

	public Document getDomObj() {
		return this._domObj;
	}

	public void setDomObj(Document domObj) {
		this._domObj = domObj;
	}

	public String getEncoding() {
		return this._encoding;
	}

	public void setEncoding(String encoding) {
		this._encoding = encoding;
	}

	public String getPubIdentifier() {
		return this._pubIdentifier;
	}

	public void setPubIdentifier(String pubIdentifier) {
		this._pubIdentifier = pubIdentifier;
	}

	public String getSysIdentifier() {
		return this._sysIdentifier;
	}

	public void setSysIdentifier(String sysIdentifier) {
		this._sysIdentifier = sysIdentifier;
	}


}
