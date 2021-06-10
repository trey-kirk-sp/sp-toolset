package sailpoint.tools;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;

import com.biliruben.util.GetOpts;
import com.biliruben.util.OptionLegend;

import biliruben.tools.xml.LightEntityResolver;

/**
 * Converts the output of a connectorDebug iterate command (list of ResourceObjects) into
 * an XML object iterable by the XMLFileConnector
 * @author trey.kirk
 *
 */
public class ResourceObjectToXMLAdapter {
    
    private static final String OPT_OUTPUT_FILE = "outputFile";
    private static final String OPT_INPUT_FILE = "inputFile";
    private static DocumentBuilder docBuilder;
    private static GetOpts opts;
    private Document document;

    public ResourceObjectToXMLAdapter() throws ParserConfigurationException {
        // TODO Auto-generated method stub
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = dbf.newDocumentBuilder();
        this.document = builder.newDocument();
    }

    public static void main(String[] args) throws Exception {
        init (args);
        String inputFile = opts.getStr(OPT_INPUT_FILE);
        String outputFile = opts.getStr(OPT_OUTPUT_FILE);
        FileInputStream fis = new FileInputStream (new File (inputFile));
        FileOutputStream fos = new FileOutputStream (new File (outputFile));
        ResourceObjectToXMLAdapter adapty = new ResourceObjectToXMLAdapter();
        adapty.convertResourceObjects(fis, fos);
    }

    private static void init (String[] args) {
        opts = new GetOpts (ResourceObjectToXMLAdapter.class);
        OptionLegend legend = new OptionLegend (OPT_INPUT_FILE);
        legend.setRequired(true);
        legend.setDescription("Input file containing the ouput from connectorDebug iterate");
        opts.addLegend(legend);

        legend = new OptionLegend (OPT_OUTPUT_FILE);
        legend.setRequired(true);
        legend.setDescription("Output file to be parsed by XML Connector");
        opts.addLegend(legend);

        opts.parseOpts(args);
    }

    private void writeXml (Node node, OutputStream output) {

        DOMImplementationLS lsImpl = (DOMImplementationLS)node.getOwnerDocument().getImplementation().getFeature("LS", "3.0");
        LSSerializer serializer = lsImpl.createLSSerializer();
        serializer.setNewLine("\n");
        DOMConfiguration domConfig = serializer.getDomConfig();
        domConfig.setParameter("xml-declaration", false);
        domConfig.setParameter("format-pretty-print", true);
        LSOutput lsOut = lsImpl.createLSOutput();
        lsOut.setByteStream(output);
        serializer.write(node, lsOut);

    }

    public void convertResourceObjects (InputStream input, OutputStream output) throws IOException, ParserConfigurationException, SAXException {
        BufferedReader reader = new BufferedReader (new InputStreamReader(input));
        // Write starter XML
        Element resourceObject = parseResourceObject (reader);
        output.write("<XmlResource>\n".getBytes());
        while (resourceObject != null) {
            // ro Map already has objectType entry
            Element roMap = extractMap (resourceObject);
            writeXml (roMap, output);
            resourceObject = parseResourceObject (reader);
        }

        // Write closing XML
        output.write("</XmlResource>\n".getBytes());
        output.close();
        input.close();
    }
    
    private Element extractMap (Element resourceObject) {
        String objectType = resourceObject.getAttribute("objectType");
        NodeList mapNodes = resourceObject.getElementsByTagName("Map");
        Element map = null;
        if (mapNodes != null) {
            map = (Element) mapNodes.item(0);
            Element objectTypeEntry = document.createElement("entry");
            objectTypeEntry.setAttribute("key", "objectType");
            objectTypeEntry.setAttribute("value", objectType);
            // child will be from a different document than the one that built the map; causes exception
            Document owner = map.getOwnerDocument();
            Node adopted = owner.adoptNode(objectTypeEntry);
            map.appendChild(adopted);
        }
        return map;
    }

    private Element parseResourceObject (BufferedReader reader) throws IOException, ParserConfigurationException, SAXException {
        // Read until we get a starter element, start building StringBuffer
        boolean foundStart = false;
        String line = null;
        do {
            line = reader.readLine();
            foundStart = line != null ? line.trim().startsWith("<ResourceObject ") : false;
        } while (!foundStart && line != null);
        if (line == null) {
            return null;
        }

        StringBuilder buff = new StringBuilder().append(line);
        do {
            line = (reader.readLine());
            buff.append(line);
            // Finish after closing element
        } while (line != null && !line.trim().startsWith("</ResourceObject>"));

        // Convert StringBuffer to Element
        Element ro = parseResourceObjectXML (buff.toString());

        return ro;
    }

    private static Element parseResourceObjectXML (String xmlData) throws ParserConfigurationException, SAXException, IOException {
        if (docBuilder == null) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            docBuilder = dbf.newDocumentBuilder();
            docBuilder.setEntityResolver(new LightEntityResolver());
        }

        InputStream is = new ByteArrayInputStream(xmlData.getBytes());
        Document doc = docBuilder.parse(is);
        Element element = doc.getDocumentElement();
        return element;
    }

}

