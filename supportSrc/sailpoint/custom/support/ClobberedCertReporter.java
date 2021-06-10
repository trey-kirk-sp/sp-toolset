/**
 * 
 */
package sailpoint.custom.support;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import com.biliruben.tools.xml.SPEntityResolver;
import com.biliruben.tools.xml.SplitXml;

/**
 * @author trey.kirk
 *
 */
public class ClobberedCertReporter extends DefaultHandler{

	public static final String CERTIFICATION_ENTITY = "CertificationEntity";
	public static final String CERTIFICATION_ITEM = "CertificationItem";
	public static final String ENTITLEMENT_SNAPSHOT = "EntitlementSnapshot";
	public static final String ENTRY = "entry";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		// Would like some better cmd-line parameter reading here
		String inputFile = args[0];

		// Init reporter (a content handler in disguise)
		ClobberedCertReporter reporter = new ClobberedCertReporter();

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
		reader.setContentHandler(reporter);

		// SPEntityResolver by default tries to locate the DTD provided as properties.  If it's not
		// found, no worries.  A dummy EntityResolver is used instead and no xml validation is done.
		// For the purposes of this tool, that is ideal anyways.
		SPEntityResolver spResolver = new SPEntityResolver();
		reader.setEntityResolver(spResolver);
		try {
			// Set FileInputStream and InputSource
			FileInputStream inputStream = new FileInputStream(inputFile);
			InputSource inputXml = new InputSource(inputStream);

			reader.parse(inputXml);

		} catch (Exception e) {
			throw (new RuntimeException(e));
		}
		
		reporter.close();

	}

	FileWriter writer;
	boolean once;

	public ClobberedCertReporter() {
		File outFile = new File("out.txt");
		try {
			writer = new FileWriter(outFile);
		} catch (Exception e) {
			throw (new RuntimeException(e));
		}
		once = false;
	}

	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) throws SAXException  {

		if (localName.equals(CERTIFICATION_ENTITY)) {
			writeEntityData(atts);
		} else if (localName.equals(CERTIFICATION_ITEM)) {
			writeItemData(atts);
		} else if (localName.equals(ENTRY)) {
			writeEntryData(atts);
		} else if (localName.equals(ENTITLEMENT_SNAPSHOT)) {
			writeSnapshotData(atts);
		}

	}

	private void writeEntityData(Attributes atts) {
		try {
			writer.write("\nIdentity: " + atts.getValue("identity") + "\n");
			writer.write("Name: " + atts.getValue("firstname") + " " + atts.getValue("lastname") + "\n");
			once = false;
		} catch (IOException e) {
			System.err.println("IOException!  \n" + e.getMessage());
			System.out.println(atts);
		}
		return;
	}

	private void writeItemData(Attributes atts) {

		return;
	}

	private void writeSnapshotData(Attributes atts) {
		try {
			if (!once) {
				writer.write ("Entitelments for Native Identity: " + atts.getValue("nativeIdentity") + " on " + atts.getValue("application") + "\n");
			}
			once = true;
		} catch (IOException e) {
			System.err.println("IOException!  \n" + e.getMessage());
			System.out.println(atts);			
		}
		return;
	}
	private void writeEntryData(Attributes atts) {
		try {
			/*
			writer.write ("Entitlement: " + atts.getValue("key") + "\n");
			writer.write ("Value: " +atts.getValue("value") + "\n");
			*/
			writer.write (atts.getValue("key") + ": " + atts.getValue("value") + "\n");
			

		} catch (IOException e) {
			System.err.println("IOException!  \n" + e.getMessage());
			System.out.println(atts);			
		}

		return;
	}

	public void close() {
		try {
			writer.close();
		} catch (IOException e) {
			System.err.println("IOException!  \n" + e.getMessage());
		}

	}

}
