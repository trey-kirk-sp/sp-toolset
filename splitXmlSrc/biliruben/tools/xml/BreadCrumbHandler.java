package biliruben.tools.xml;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This class is designed to make SAX parsing a bit easier by tracking the xml element currently
 * at the parser cursor.  This is done by creating a bread crumb using a dot notation for each
 * element traversed.  For example, given the following XML structure:<br>
 * <br>
 * &lt;root><br>
 * &nbsp;&nbsp;&nbsp;&lt;sub><br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;sub2>text here&lt;2/sub2><br>
 * &nbsp;&nbsp;&nbsp;&lt;/sub><br>
 * &lt;/root><br>
 * <br>
 * When the parser reaches the sub2 element, the bread crumb would read as 'root.sub.sub2'.  This allows
 * implementations of this class easily locate interesting elements without bothering to implement it's
 * own form of tracking.<br>
 * <br>
 * An additional convenience is the matchBreadCrumb method to test if a particular bread crumb is one of
 * interest.  This uses a {@link MatchMode} class that instructs the method on how to search for the given
 * bread crumb (ala 'starts with', 'ends with', etc.)<br>
 * <br>
 * Children of this class will generally extend either or both {@link #startElement(String, String, String, Attributes)}
 * and {@link #endElement(String, String, String)}.  When doing so, both methods must adhere to a contract on
 * ordering in which the super() method is called to ensure the bread crumb is up to date.
 * 
 * @author trey.kirk
 *
 */
public abstract class BreadCrumbHandler extends DefaultHandler {

	/**
	 * Defines the behavior on how the bread crumb is matched on.
	 * @author trey.kirk
	 *
	 */
	public enum MatchMode {
		BEGINS_WITH,
		CONTAINS,
		ENDS_WITH,
		EQUALS
	}
	private List<String> _breadCrumb = new ArrayList<String>();

	private int _level = -1;

	/**
	 * As is the general contract with DefaultHandler, this method is called when a closing
	 * element is reached.  Implementations of this method must ensure that the super method
	 * is called LAST to ensure the bread crumb current.  Calling {@link #getBreadCrumb()} after
	 * calling super will lead to a prematurely modified bread crumb and at best, the data will
	 * be incorrect.  It may also result in an ArrayIndexOutOfBounds exception.
	 */
	@Override
	public void endElement(String uri, String localName, String name)
	throws SAXException {
		// TODO Auto-generated method stub
		super.endElement(uri, localName, name);
		_breadCrumb.remove(_level);
		_level--;
	}

	/**
	 * Returns the current bread crumb using a dot notation: 'root.el1.el2'
	 * @return
	 */
	public String getBreadCrumb() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i <= _level; i++) {
			sb.append(_breadCrumb.get(i) + ".");
		}
		sb.delete(sb.length() - 1, sb.length());
		return sb.toString();
	}

	/**
	 * Tests a partial or full test bread crumb with the current bread crumb and determines if there's
	 * a match.  The behavior of this match can be defined by a {@link MatchMode} to dictate if the partial
	 * bread crumb should be located at the beginning, end, or anywhere (contains) in the current bread 
	 * crumb.
	 */
	public boolean matchBreadCrumb (MatchMode mode, String partialCrumb) {
		String beginsAnchor = "^";
		String endsAnchor = "$";
		String containsAnchor = ".*";
		String matchingCrumb = "";
		String escapedCrumb = partialCrumb.replaceAll("\\.", "\\\\.");
		switch (mode) {
		case BEGINS_WITH: matchingCrumb = beginsAnchor + escapedCrumb + containsAnchor + endsAnchor; break;
		case ENDS_WITH: matchingCrumb = beginsAnchor + containsAnchor + escapedCrumb + endsAnchor; break;
		case CONTAINS: matchingCrumb = beginsAnchor + containsAnchor + escapedCrumb + containsAnchor + endsAnchor; break;
		default: matchingCrumb = beginsAnchor + escapedCrumb + endsAnchor; break;
		}
		return getBreadCrumb().matches(matchingCrumb);
	}

	/**
	 * As is the general contract with DefaultHandler, this method is called when an opeening elelment is
	 * reached.  Implementations of this method must call super before calling {@link #getBreadCrumb()}.
	 * Otherwise, the data will be incorrect and an ArrayIndexOutOfBounds exception may occur.
	 */
	@Override
	public void startElement(String uri, String localName, String name,
			Attributes attributes) throws SAXException {
		// TODO Auto-generated method stub
		super.startElement(uri, localName, name, attributes);
		_level++;
		_breadCrumb.add(localName);
	}
}
