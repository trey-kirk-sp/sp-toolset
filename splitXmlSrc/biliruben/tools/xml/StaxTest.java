package biliruben.tools.xml;

import java.io.StringReader;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

public class StaxTest {

	/**
	 * @param args
	 * @throws XMLStreamException 
	 */
	public static void main(String[] args) throws XMLStreamException {
		// test string
		String xml = getXml();
		
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		XMLEventReader eventReader = inputFactory.createXMLEventReader(new StringReader(xml));
		XMLStreamReader streamReader = inputFactory.createXMLStreamReader(new StringReader(xml));
		
		while (eventReader.hasNext()) {
			XMLEvent event = (XMLEvent) eventReader.next();
			if (event.isStartElement()) {
				StartElement startElement = event.asStartElement();
				QName name = startElement.getName();
				System.out.println(startElement.getName().getLocalPart());
			}
			
		}
		/*
		while (streamReader.hasNext()) {
			int tagType = streamReader.nextTag();
			switch (tagType) {
			case XMLStreamConstants.START_ELEMENT :
				
			}
		}
		*/
		
		
		
	}
	
	
	private static String getXml() {
		StringBuffer xml = new StringBuffer();
		xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE Bundle PUBLIC \"sailpoint.dtd\" \"sailpoint.dtd\">");
		xml.append("<Bundle created=\"1239398903215\" id=\"2c9081ff2091ee59012091ee6daf0082\" name=\"Accountant - TTCA\" riskScoreWeight=\"200\">");
		xml.append("   <Children/>");
		xml.append("   <Owner>");
		xml.append("      <Reference class=\"sailpoint.object.Identity\" id=\"2c9081ff2091ee59012091ee6b0f0052\" name=\"Maynard.Rick.947123\"/>");
		xml.append("   </Owner>");
		xml.append("<Profiles>");
		xml.append("<Reference class=\"sailpoint.object.Profile\" id=\"2c9081ff2091ee59012091ee6a540045\" name=\"BSP Group - UNIX\"/>");
		xml.append("<Reference class=\"sailpoint.object.Profile\" id=\"2c9081ff2091ee59012091ee6a440043\" name=\"Oracle_Account - Baan Oracle\"/>");
		xml.append("<Reference class=\"sailpoint.object.Profile\" id=\"2c9081ff2091ee59012091ee6d230077\" name=\"GI_BAAN - Baan\"/>");
		xml.append("<Reference class=\"sailpoint.object.Profile\" id=\"2c9081ff2091ee59012091ee6d42007a\" name=\"GL Accountant - Baan\"/>");
		xml.append("<Reference class=\"sailpoint.object.Profile\" id=\"2c9081ff2091ee59012091ee6daf0081\" name=\"Accountant_TTCA_AD - Active Directory\"/>");
		xml.append("</Profiles>");
		xml.append("</Bundle>");
		return xml.toString();
	}

}
