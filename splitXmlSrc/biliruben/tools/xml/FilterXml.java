package biliruben.tools.xml;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSParser;
import org.w3c.dom.ls.LSResourceResolver;
import org.w3c.dom.ls.LSSerializer;

import com.biliruben.util.GetOpts;
import com.biliruben.util.OptionLegend;

/**
 * Utility class that will filter a provided XML file constructing {@link AttributeFilter} objects
 * based on the provided command line arguments.  Most obvious use case is to filter out 'id' attributes.
 * Other use cases may be to reset / add date attributes to various date sensitive object like WorkItem
 * 
 * TODO: provide a batch mode that given a file filter (ala  *.xml), queue the intended xml files to be
 * 		filtered into a work queue and spawn working threads.
 * 
 * Required SailPoint Services APIs:
 * 	GetOpts 1.2
 * 	XmlAPI 1.1
 * 
 * @author trey.kirk
 *
 */
public class FilterXml {

	private static final String DEFAULT_ATTR_SEP = ":";
	private static final String OPT_XML_FILE = "xmlFile";
	private static final String OPT_REMOVE_ATTR = "remove";
	private static final String OPT_OVERWRITE_ATTR = "set";
	private static final String OPT_REPLACE_ATTR = "replace";
	private static final String OPT_ADD_ATTR = "add";
	private static final String OPT_ATTR_SEP = "sep";
	
	private static GetOpts _opts;
	private static String _attrSep;

	/**
	 * @param args
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws ClassNotFoundException 
	 * @throws ClassCastException 
	 * @throws IOException 
	 */
	public static void main(String[] args) 
	throws ClassCastException, ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
		init(args);
		_attrSep = _opts.getStr(OPT_ATTR_SEP);

		// read in the xml file, get the root node
		// pass the root node into the LS filtered serializer

		List<AttributeFilter> filters = buildFilters();
		LSSerializerAttributeFiltered serializer = new LSSerializerAttributeFiltered(true);
		serializer.setFilters(filters);

		String inputXml = _opts.getStr(OPT_XML_FILE);
		DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
		DOMImplementationLS lsImpl = (DOMImplementationLS)registry.getDOMImplementation("LS");

		// make teh parser
		LSParser parser = lsImpl.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null);


		// create the File.  Used for read and write
		File xmlFile = new File(inputXml);
		LSInput input = lsImpl.createLSInput();
		input.setCharacterStream(new FileReader(xmlFile));

		// Create the configuration object with using the hacked DTD resolver
		// TODO: revisit this, XmlAPI has gotten better with DTD resolvers
		DOMConfiguration config = parser.getDomConfig();
		config.setParameter("resource-resolver", new NonDTDLSResolver(lsImpl));
		
		// parse using an LS parser
		Document doc = parser.parse(input);
		LSOutput output = lsImpl.createLSOutput();
		output.setCharacterStream(new FileWriter(xmlFile));
		output.setEncoding("UTF-8");
		try {
			// serialize the filtered Document
			serializer.write(doc, output);
		} catch (Throwable e) {
			// something failed during serialization.  Revert file to original document and then complain
			LSSerializer newSerializer = lsImpl.createLSSerializer();
			newSerializer.write(doc, output);
			// rethrow error
			throw new Error (e);
		}
	}

	private static List<AttributeFilter> buildFilters() {
		List<AttributeFilter> filters = new ArrayList<AttributeFilter>();

		List<String> removals = _opts.getList(OPT_REMOVE_ATTR);
		if (removals != null) {
			for (String attribute : removals) {
				filters.add(AttributeFilter.remove(attribute));
			}
		}

		List<String> overwrites = _opts.getList(OPT_OVERWRITE_ATTR);
		if (overwrites != null) {
			for (String attributeValue : overwrites) {
				String[] tokens = attributeValue.split(_attrSep, 2);
				if (tokens.length < 2 || tokens[0] == null || tokens[1] == null) {
					throw new NullPointerException (attributeValue + " must be in the format of 'attribute" + _attrSep + "value'");
				}
				filters.add(AttributeFilter.set(tokens[0], tokens[1]));
			}
		}

		List<String> replacements = _opts.getList(OPT_REPLACE_ATTR);
		if (replacements != null) {
			for (String attributeValue : replacements) {
				String[] tokens = attributeValue.split(_attrSep, 3);
				if (tokens.length < 3 || tokens[0] == null || tokens[1] == null || tokens[2] == null) {
					throw new NullPointerException (attributeValue + " must be in the format of 'attribute" + _attrSep + "oldvalue" + _attrSep + "newvalue'");
				}
				filters.add(AttributeFilter.replace(tokens[0], tokens[1], tokens[2]));
			}
		}

		List<String> additions = _opts.getList(OPT_ADD_ATTR);
		if (additions != null) {
			for (String attributeValue : additions) {
				String[] tokens = attributeValue.split(_attrSep, 3);
				if (tokens.length < 2 || tokens[0] == null || tokens [1] == null) {
					throw new NullPointerException (attributeValue + " must be in format of 'elementname" + _attrSep + "attribute" + _attrSep + "[optional]'");
				}
				String newValue = "";
				if (tokens.length > 2 && tokens[2] != null) {
					newValue = tokens[2];
				}
				filters.add(AttributeFilter.add(tokens[1], newValue, tokens[0]));
			}
		}

		return filters;
	}

	private static void init(String[] args) {
		_opts = new GetOpts(FilterXml.class);
		OptionLegend legend = new OptionLegend (OPT_XML_FILE);
		legend.setRequired(true);
		legend.setDescription("XML File to filter.  File will be overwritten with new filtered output");
		legend.setExampleValue("xmlFileToParse");
		_opts.addLegend(legend);

		legend = new OptionLegend(OPT_REMOVE_ATTR);
		legend.setMulti(true);
		legend.setDescription("Attribute names to remove");
		legend.setRequired(false);
		legend.setExampleValue("attrName");
		_opts.addLegend(legend);

		legend = new OptionLegend(OPT_REPLACE_ATTR);
		legend.setMulti(true);
		legend.setDescription("Attribute name-value combo to have values matched and replaced.  Each argument must be passed in the format of attributeName" + DEFAULT_ATTR_SEP + "oldValue" + DEFAULT_ATTR_SEP + "newValue");
		legend.setRequired(false);
		legend.setExampleValue("attrName" + DEFAULT_ATTR_SEP + "oldAttrValue" + DEFAULT_ATTR_SEP + "newAttrValue");
		_opts.addLegend(legend);

		legend = new OptionLegend(OPT_OVERWRITE_ATTR);
		legend.setMulti(true);
		legend.setDescription("Attribute name-value pair to have values overwritten.  Each argument must be passed in the format of attributeName" + DEFAULT_ATTR_SEP + "newValue");
		legend.setRequired(false);
		legend.setExampleValue("attrName" + DEFAULT_ATTR_SEP + "newAttrValue");
		_opts.addLegend(legend);

		legend = new OptionLegend(OPT_ADD_ATTR);
		legend.setMulti(true);
		legend.setDescription("Attribute name-value combo to have values added to specified elements.  Existing attributes of the same name will be overwritten");
		legend.setRequired(false);
		legend.setExampleValue("elementName" + DEFAULT_ATTR_SEP + "attrName" + DEFAULT_ATTR_SEP + "newAttrValue");
		_opts.addLegend(legend);
		
		legend = new OptionLegend(OPT_ATTR_SEP);
		legend.setDescription("Separation character for name value combos");
		legend.setRequired(false);
		legend.setDefaultValue(DEFAULT_ATTR_SEP);
		_opts.addLegend(legend);

		_opts.parseOpts(args);		
	}
}
class MyResolver implements LSResourceResolver {

	private LSInput _input;
	MyResolver (LSInput input) {
		_input = input;
	}
	public LSInput resolveResource(String type, String namespaceURI,
			String publicId, String systemId, String baseURI) {
		// TODO Auto-generated method stub
		return _input;
	}
	
}