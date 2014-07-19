package biliruben.tools.xml;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSException;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.w3c.dom.ls.LSSerializerFilter;

public class LSSerializerAttributeFiltered implements LSSerializer {

	private LSSerializer _serializer;
	private boolean _recursive;

	/**
	 * Constructor to create an LSSerialzierAttributeFiltered object designed to use one
	 * or more {@link AttributeFilter} objects to filter the content of a {@link Document}
	 * @param recursive Flag to indicate if the full Document tree should be followed.  Current implementation
	 * would not be very effective with a 'false' flag.
	 * @throws ClassCastException
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public LSSerializerAttributeFiltered(boolean recursive) throws ClassCastException, 
	ClassNotFoundException, InstantiationException, IllegalAccessException {
		DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
		DOMImplementationLS lsImpl = (DOMImplementationLS)registry.getDOMImplementation("LS");
		_serializer = lsImpl.createLSSerializer();
		_recursive = recursive;

	}

	public DOMConfiguration getDomConfig() {
		return _serializer.getDomConfig();
	}

	public LSSerializerFilter getFilter() {
		return _serializer.getFilter();
	}

	public String getNewLine() {
		return _serializer.getNewLine();
	}

	public void setFilter(LSSerializerFilter filter) {
		_serializer.setFilter(filter);

	}

	public void setFilters(List<AttributeFilter> filters) {
		_filters = filters;
	}
	
	public void setNewLine(String newLine) {
		_serializer.setNewLine(newLine);
	}

	public boolean write(Node nodeArg, LSOutput destination) throws LSException {
		return _serializer.write(filterNode(nodeArg), destination);
	}

	public String writeToString(Node nodeArg) throws DOMException, LSException {
		return _serializer.writeToString(filterNode(nodeArg));
	}

	public boolean writeToURI(Node nodeArg, String uri) throws LSException {
		return _serializer.writeToURI(filterNode(nodeArg), uri);
	}

	protected Node filterNode(Node node) {
		if (_filters != null && _filters.size() > 0) {
			for (AttributeFilter filter : _filters) {
				filter.filterNode(node, _recursive);
			}
		}
		return node;
	}

	private List<AttributeFilter> _filters;

	public void addAttributeFilter (AttributeFilter filter) {
		if (_filters == null) {
			_filters = new ArrayList<AttributeFilter>();
		}
		_filters.add(filter);
	}

}
