package biliruben.tools.xml;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * AttributeFilter provides inline filtering of XML elements.  Current implementation filters just Attributes
 * by allowing Attributes to be created, deleted, overwritten unconditionally, or string replacement.
 * @author trey.kirk
 *
 */
public class AttributeFilter {

	public static final String DEFAULT_SEP_CHAR = ":";
	
	/**
	 * Enum denoting the operation of the Filter.  It doesn't seem likely anybody needs to test the operation
	 * of a filter, so hiding for now.
	 * @author trey.kirk
	 *
	 */
	private enum Operation {
		Remove,
		Replace,
		Add,
		Set
	};

	private Operation _op;
	private String _attributeName;
	private String _oldValue;
	private String _newValue;
	private String _element;
	private String _seperator = DEFAULT_SEP_CHAR;
	


	/**
	 * Constructor specifying only Operation.  Currently marked private as static factory methods will do
	 * the job just fine.
	 * @param op
	 */
	private AttributeFilter (Operation op) {
		_op = op;
	}

	/**
	 * Factory method to provide an AttributeFilter invoking the 'add' operation.
	 * @param attributeName Name of attribute to add.  Any existing attribute of that same name is overwritten.
	 * @param newValue Value of the attribute to be added.
	 * @param elementName Element this attribute will be added to.
	 * @return AttributeFilter invoking the 'add' operation.
	 */
	public static AttributeFilter add (String attributeName, String newValue, String elementName) {
		AttributeFilter filter = new AttributeFilter (Operation.Add);
		filter._attributeName = attributeName;
		filter._newValue = newValue;
		filter._element = elementName;
		return filter;
	}

	/**
	 * Factory method to provide an AttributeFilter invoking the 'replace' operation.  The 'replace' operation
	 * is a sub-string match.  Thus, replacing "foo" to "bar" in "fee fi foo fum" will result in "fee fi bar fum"
	 * @param attributeName Name of attribute to have its value replaced.
	 * @param oldValue Old value to replace.
	 * @param newValue New value
	 * @return Attributefilter invoking the 'replace' operation.
	 */
	public static AttributeFilter replace (String attributeName, String oldValue, String newValue) {
		AttributeFilter filter = new AttributeFilter(Operation.Replace);
		filter._attributeName = attributeName;
		filter._oldValue = oldValue;
		filter._newValue = newValue;
		return filter;
	}

	/**
	 * Factory method to provide an AttributeFilter invoking the 'set' operation.  The 'set' operation will add
	 * the Attribute whether it previously existed or not, overwriting any existing value.
	 * @param attributeName Name of attribute to set.
	 * @param newValue Value of the attribute to set to.
	 * @return AttributeFilter invoking the 'add' operation.
	 */
	public static AttributeFilter set (String attributeName, String newValue) {
		AttributeFilter filter = new AttributeFilter (Operation.Set);
		filter._attributeName = attributeName;
		filter._newValue = newValue;
		return filter;
	}

	/**
	 * Factory method to provide an AttributeFilter invoking the 'remove' operation.  Elements without the
	 * specified Attribute are not affected.
	 * @param attributeName Attribute name to remove.
	 * @return AttributeFilter inoking the 'remove' operation.
	 */
	public static AttributeFilter remove (String attributeName) {
		AttributeFilter filter = new AttributeFilter (Operation.Remove);
		filter._attributeName = attributeName;
		return filter;
	}

	@Override
	public String toString() {
		return _op.toString() + ":" + _attributeName + ":" + _oldValue + ":" + _newValue;
	}

	/**
	 * Filters the provided node applying this ApplicationFilter's specified operation.
	 * @param node {@link Node} to filter
	 * @param recursive Flag to indicate if the Node should be traversed to child nodes.
	 */
	public void filterNode (Node node, boolean recursive) {
		switch (_op) {
		case Remove: removeAttribute(node); break;
		case Replace: replaceAttribute(node); break;
		case Set: setAttribute(node); break;
		case Add: addAttribute(node); break;
		}

		if (recursive) {
			NodeList kids = node.getChildNodes();
			for (int i = 0; kids != null && i < kids.getLength(); i++) {
				Node nextKid = kids.item(i);
				filterNode (nextKid, recursive);
			}
		}
	}

	// Adds the attribute to the Node
	private void addAttribute (Node node) {
		if (node instanceof Element) {
			Element el = (Element)node;
			if (el.getTagName().equals(_element)) {
				el.setAttribute(_attributeName, _newValue);
			}
		}
	}

	// Sets the attribute to the Node, if it exists
	private void setAttribute (Node node) {
		if (node instanceof Element) {
			Element el = (Element)node;
			if (!el.getAttribute(_attributeName).equals("")) {
				el.setAttribute(_attributeName, _newValue);
			}
		}
	}

	// Replaces the attribute value with the new value
	private void replaceAttribute (Node node) {
		if (node instanceof Element) {
			Element el = (Element)node;
			String value = el.getAttribute(_attributeName);
			if (value != null && value.contains(_oldValue)) {
				el.setAttribute(_attributeName, value.replace(_oldValue, _newValue));
			}
		}
	}

	// Removes the attribute from the node.
	private void removeAttribute (Node node) {
		if (node instanceof Element) {
			((Element) node).removeAttribute(_attributeName);
		}
	}
}
