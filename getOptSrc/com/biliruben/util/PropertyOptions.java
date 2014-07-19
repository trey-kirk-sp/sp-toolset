package com.biliruben.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class will use a property file to collect option information intended
 * to be passed to an application at runtime.  The calling application is still
 * required to define OptionLegends to ensure passed options are of valid formats.
 * The PropertyOptions will just allow GetOpts to delegate option fetching to values
 * passed in via a properties file.  
 * 
 * This class will need to load properties from a designated properties file.  The name
 * of that file may be an inferred default value or a specified value.
 * 
 * Properties should rely on a 'dot notation' that allow them to be grouped for later
 * cause.  That simply means anything before the last dot is treated as a prefix and
 * subsequent 'get' calls may pass in a prefix to isolate the intended value.  No idea
 * how desirable or useful this is.  Naturally, properties may likewise be ungroupped and
 * provided 'bare'.
 * 
 * Scalar values are provided as one might expect: 'property=value'
 * Flag values are provided simply by defining a property with any value, even 'false'.  Warnings
 * should be produced should a flag value be found with 'property=false' notation.
 * List values are defined with the property repeated.  This requirement disallows us the
 * use of java's core property registering
 * 
 * @author trey.kirk
 *
 */
public class PropertyOptions {
	
	public static final String DEFAULT_PROPERTY_FILE = "options.properties";
	public static final String COMMENT_CHARACTER = "#";
	private Map<String, Object> _options;
	private BufferedReader _reader;
	private String _defaultGroup;
	private static Log _log = LogFactory.getLog(PropertyOptions.class);
	
	public PropertyOptions() throws IOException {
		this(DEFAULT_PROPERTY_FILE);
	}
	
	public PropertyOptions(String fileName) throws IOException {
		this(new File(fileName));
	}
	
	public PropertyOptions (File file) throws IOException {
		this (new FileReader(file));
	}
	
	public PropertyOptions (Reader input) throws IOException {
		_log.debug("Initializing PropertyOptions: reader=" + _reader);
		_reader = new BufferedReader(input);
		_options = new HashMap<String, Object>();
		registerProperties();
	}
	
	/** 
	 * When a property group is set, getList and getString will assume
	 * the provided group to be the default group to fetch properties from.
	 * Usage of the specific get###(Group, Property) methods are still valid and work
	 * as expected.
	 * @param group
	 */
	public void setPropertyGroup (String group) {
		_defaultGroup = group;
	}
	
	private void registerProperties() throws IOException {
		_log.debug("Registering properties");
		String nextLine = "";
		while (_reader.ready() && nextLine != null) {
			nextLine = _reader.readLine();
			_log.trace("Processing: " + nextLine);
			if (nextLine == null) {
				continue;
			}
			// strip comments
			nextLine = (nextLine.split(COMMENT_CHARACTER, 2))[0];
			if (nextLine != null) {
				nextLine = nextLine.trim();
			}
			
			// nextLine has been stripped of comments and anchoring whitespace.
			// might be null or empty string now.  Move on if so
			if (nextLine == null || "".equals(nextLine)) {
				continue;
			}
			
			// sanity check:  Alls my properties needs an =.  Ain't got one?
			// Well then YOU Suck!
			if (!nextLine.contains("=")) {
				IllegalArgumentException e = new IllegalArgumentException("Content of properties file must always be 'property=value':\n" + nextLine);
				_log.error(e);
				throw e;
			}
			
			_log.trace("Pruned line: " + nextLine);
			String[] tokens = nextLine.split("=", 2);
			String fullPropertyName = tokens[0];
			if (fullPropertyName != null) {
				fullPropertyName = fullPropertyName.trim();
			}
			_log.trace("Property: " + fullPropertyName);
			String propertyValue = tokens[1];
			if (propertyValue != null) {
				propertyValue = propertyValue.trim();
			}
			_log.trace("Value: " + propertyValue);
			Object existingValue = _options.get(fullPropertyName);
			_log.trace("Existing value: " + existingValue);
			if (existingValue != null) {
				List<String> existingValueList = new ArrayList<String>();
				if (existingValue instanceof List) {
					existingValueList = (List<String>)existingValue;
					existingValueList.add(propertyValue);
				} else if (existingValue instanceof String) {
					existingValueList.add((String)existingValue);
					existingValueList.add(propertyValue);
					_options.put(fullPropertyName, existingValueList);
				} else {
					// unexpected!
					UnsupportedOperationException e = new UnsupportedOperationException("Non-String or List value found!  existingValue: " + existingValue);
					_log.error(e);
					throw e;
				}
			} else {
				// null existing, just add String
				_options.put(fullPropertyName, propertyValue);
			}
			
		}
		_log.debug("Registered properties: " + _options);
	}
	
	public String getString (String option) {
		_log.trace("Getting String: " + option);
		return getString (_defaultGroup, option);
	}
	
	public String getString (String group, String option) {
		_log.trace("Getting property for: group=" + group + ", option=" + option);
		String propertyName = getFullPropertyName(group, option);

		_log.trace("Property name: " + propertyName);
		Object value = _options.get(propertyName);
		if (value != null) {
			_log.trace("Found: " + value);
			return value.toString();
		} // and if its a list?  Defer to what GetOpts does
		// else
		_log.trace("Found: null");
		return null;  // last resort is to return no value
	}
	
	public List<String> getList (String option) {
		_log.trace("Getting List: " + option);
		return getList (_defaultGroup, option);
	}
	
	public List<String> getList (String group, String option) {
		_log.trace("Getting list property for: group=" + group + ", option=" + option);
		String propertyName = getFullPropertyName(group, option);
		
		Object value = _options.get(propertyName);
		if (value instanceof List) {
			return (List)value;
		} else {
			// coerce to list
			List<String> newList = new ArrayList<String>();
			newList.add((String)value);
			return newList;
		}
	}

	public boolean isDefined (String option) {
		return isDefined(_defaultGroup, option);
	}
	
	public boolean isDefined (String group, String option) {
		_log.trace("Testing definition of: group=" + group + ", option=" + option);
		String propertyName = getFullPropertyName(group, option);
		return _options.get(propertyName) != null;
	}
	
	private String getFullPropertyName (String group, String property) {
		if (group == null) {
			return property;
		} else {
			return group + "." + property;
		}
	}
}
