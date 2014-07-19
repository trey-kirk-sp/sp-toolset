package biliruben.tools.xml;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Utility class to convert a Map into an unvalidating xml.  It will use the following format:<br>
 * &lt;<i>KeyObjectClass</i> key="<i>keyValue</i>"><br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;<i>ValuePrimitiveClass</i>><i>value</i>&lt;/<i>ValuePrimitiveClass</i>><br>
 * &lt;<i>/KeyObjectClass</i>><br>
 * <br>
 *  Values that are not primitive types will be iterated through and their public fields will be
 *  fetched.<br>
 * @author trey.kirk
 *
 */
public class MapToXML {

	public static final String DEFAULT_CR = "\n";

	private Map _inputMap;
	private boolean _topLevelOnly = false;
	private String _indent = " ";
	//private int _indentLevel = 0;

	public MapToXML(Map inputMap, boolean topLevelOnly) {
		super();
		_inputMap = inputMap;
		_topLevelOnly = topLevelOnly;
	}

	public String getXml() {
		StringBuffer xml = new StringBuffer();
		String rootEl = _inputMap.getClass().getSimpleName(); 
		xml.append("<" + rootEl + ">" + DEFAULT_CR);
		for (Object key : _inputMap.keySet()) {
			xml.append(xmlKVP(key, _inputMap.get(key), 1, null));
		}
		xml.append("</" + rootEl + ">");
		return xml.toString();
	}

	private String getIndent(int indent) {
		StringBuffer indentBuffer = new StringBuffer(3 * indent + 1);
		for (int i = 0; i < (3 * indent); i++) {
			indentBuffer.append(_indent);
		}
		return indentBuffer.toString();
	}

	private String getAttributes (Map<String, String> attrs) {
		StringBuffer attrString = new StringBuffer();
		for (String key : attrs.keySet()) {
			if (attrs.get(key) != null) {
				attrString.append(key + "=\"" + attrs.get(key) + "\"");
			} else {
				attrString.append(key + "=\"NULL\"");
			}
			attrString.append(" ");
		}
		attrString.deleteCharAt(attrString.length() - 1);
		return attrString.toString();
	}

	private String xmlKVP (Object key, Object value, int indent, Map<String, String> attributes) {
		StringBuffer newXml = new StringBuffer();
		newXml.append(getIndent(indent));
		String elName = null;
		if (value != null) {
			if (value.getClass().isArray()) {
				elName = "Array";
			} else {
				elName = value.getClass().getSimpleName();
			}
		}
		newXml.append("<" + elName);
		if (attributes != null) {
			newXml.append(" " + getAttributes(attributes));
		}
		newXml.append(" name=\"" + key.toString() + "\"");
		if (value == null) {
			newXml.append("/>" + DEFAULT_CR);
		} else {
			newXml.append(">");
			//newXml.append(" class=" + value.getClass().getSimpleName() + ">");
			
			/*
			if (isPrimitive (value)) {

			} else
			 */
			if (value.getClass().isArray()) {
				newXml.append(DEFAULT_CR);
				Map<String, String> attrs = new HashMap<String, String>();
				for (int i = 0; i < Array.getLength(value); i++) {
					attrs.put("element", new Integer(i).toString());
					newXml.append(xmlKVP(key, Array.get(value, i), indent + 1, attrs));
				}
				newXml.append(getIndent(indent));
			} else if (value instanceof Collection) {
				newXml.append(DEFAULT_CR);

				int i = 0;
				Iterator it = ((Collection)value).iterator();
				while (it.hasNext()) {
					newXml.append(xmlKVP(key + new Integer(i).toString(), it.next(), indent + 1, null));
				}
				newXml.append(getIndent(indent));

			} else if (value instanceof Map) {
				newXml.append(DEFAULT_CR);
				Map mapValue = (Map)value;
				for (Object nextKey : mapValue.keySet()) {
					newXml.append(xmlKVP(nextKey, mapValue.get(nextKey), indent + 1, null));
				}
				newXml.append(getIndent(indent));
			} else {
				newXml.append(value.toString());
			}

			newXml.append("</" + elName + ">");
			newXml.append(DEFAULT_CR);
		}
		return newXml.toString();
	}

	public static void main (String[] args) {
		HashMap testMap = new HashMap();
		testMap.put("prim1", 1);
		testMap.put("test1", "testValue1");
		testMap.put("test2", "testValue2");
		testMap.put("test3", "testValue3");
		testMap.put("test4", "testValue4");

		ArrayList myList = new ArrayList();
		myList.add("poop");
		myList.add("shit");
		testMap.put("myList", myList);
		testMap.put(myList, "myList");

		HashMap newMap = new HashMap();
		newMap.put("test1", "testValue1");
		newMap.put("test2", "testValue2");
		newMap.put("test3", "testValue3");

		Object[] strings = {
				"poop",
				"plap",
				"mcrap"
		};
		newMap.put("fantastical", strings);
		testMap.put(new PrettyXML(), newMap);



		MapToXML mapper = new MapToXML(testMap, false);
		String xml = mapper.getXml();
		System.out.println(xml);

	}

}
