package sailpoint.tools;

import java.io.File;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;

import com.biliruben.util.GetOpts;
import com.biliruben.util.OptionLegend;

public class Stylizer {

    private static GetOpts _getOpts;

    public static void main(String[] args) throws Throwable {
        init(args);
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        String xslFileName = _getOpts.getStr("xsl");

        File xmlFile = new File(_getOpts.getStr("xml"));

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(xmlFile);
        Templates templates = new net.sf.saxon.BasicTransformerFactory().newTemplates(
                new StreamSource(Stylizer.class.getClassLoader()
                                     .getResourceAsStream(xslFileName)));

        final Transformer transformer = templates.newTransformer();

        // Use a Transformer for output
        DOMSource source = new DOMSource(document);
        StreamResult result = new StreamResult(System.out);
        transformer.transform(source, result);
    }

    private static void init(String[] args) {
        _getOpts = new GetOpts(Stylizer.class);
        
        OptionLegend legend = new OptionLegend("xml");
        legend.setRequired(true);
        legend.setDescription("XML file to style");
        _getOpts.addLegend(legend);
        
        legend = new OptionLegend("xsl");
        legend.setRequired(true); // revisit this. If an xml file declares its style, why pass it here?
        legend.setDescription("Extended style sheet");
        _getOpts.addLegend(legend);
        
        _getOpts.parseOpts(args);
    }

}
