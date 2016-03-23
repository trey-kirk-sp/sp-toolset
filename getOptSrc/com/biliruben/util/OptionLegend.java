/**
 * OptionLegend is part of the GetOpts package.  This class defines how incoming options are to be parsed.
 */
package com.biliruben.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The OptionLegend class defined how an option is parsed by a GetOpts object.  Each option passed
 * to the GetOpts object will be represented by an OptionLegend.  An OptionLegend can be constructed
 * in one of two ways:<br>
 * <br>
 * By passing in only the name of the option: OptionLegend("opt1")<br>
 * By passing in the name and description of the option: OptionLegend("opt1", "the option I need to have")<br>
 * <br>
 * These methods require that the implementer use the supplied setter methods to set further
 * characteristics of the OptionLegned.
 *   
 * @see GetOpts
 * @author trey.kirk
 *
 */
public class OptionLegend implements Comparable<OptionLegend>{

	//TODO: add a setExample method to allow the user to provide an example
	// value or short description.  Detect whitespace and provide quotes when
	// found.
	
	private static final String UNSET = "unset";

	/**
	 * Index value for the option name
	 * @deprecated
	 */
	private static final int OPT_NAME = 0;
	/**
	 * Index value for the option description
	 * @deprecated
	 */
	private static final int OPT_DESCRIPTION = 1;
	/**
	 * Index value for the option required flag
	 * @deprecated
	 */
	private static final int OPT_REQ = 2;
	/**
	 * Index value for the option is flag flag
	 * @deprecated
	 */
	private static final int OPT_FLAG = 3;
	/**
	 * Index value for the option is multi-valued flag
	 * @deprecated
	 */
	private static final int OPT_MULTI = 4;
	/**
	 * Index value for the required value list
	 * @deprecated
	 */
	private static final int OPT_VALUES = 5;

	private static Log _log = LogFactory.getLog(OptionLegend.class);
	/**
	 * Key used to create an OptionLegend for unswitched options (those values that don't
	 * require a switched option).  Implementers of the GetOpts rarely need this value to 
	 * create OptionLegend objects as the default unswitch OptionLegend is provided automatically.
	 * Use {@link GetOpts#getUnswitchedOptions()} to retrieve those values.
	 */
	protected static final String OPT_DEFAULT_NAME = "DEFAULT";
	public static final String OPT_PROPERTY_FILE = "properties";
	protected static final String OPT_HELP = "?";
	protected static final String OPT_PROPERTY_GROUP = "propertyGroup";

	private boolean _required = false;
	private boolean _multi = false;
	private String _optName = UNSET;
	private String _optDescription;
	private String[] _optValues;
	private String _defaultValue;
	private boolean _flag = false;
	private boolean _isLimited = false;
	private boolean _isHidden = false;
	private boolean _isDerived = false;
	private String _exampleValue;

	private String[] _derivisionFormula;

	private GetOpts _opts;



	/**
	 * Constructor requiring the option's name and description.  Other values are set using the following
	 * default values.<br>
	 * Required: false <br>
	 * Multi: false <br>
	 * Flag: false<br>
	 * Allowed Values: null<br>
	 * @param optName String indicating the option name
	 * @param optDescription String indicating the option description
	 */
	public OptionLegend (String optName, String optDescription) {
		_log.trace ("Creating legend: name=" + optName + ", description=" + optDescription);
		setName(optName);
		setDescription(optDescription);
	}

	/**
	 * Constructor requiring only the option's name.  Other values are set using the following default
	 * values.<br>
	 * Description: null <br>
	 * Required: false <br>
	 * Multi: false <br>
	 * Flag: false<br>
	 * Allowed Values: null<br>
	 * @param optName String indicating the option name
	 */
	public OptionLegend(String optName) {
		this (optName, null);
	}

	@Deprecated
	private OptionLegend(String optName, boolean allow) {
        _log.trace ("Creating legend: name=" + optName);
        setName(optName);
	}

	/**
	 * Sets the hidden flag.  OptionLegends that are hidden will not be shown in the GetOpts usage.
	 * While not enforced, isHidden should be treated as mutually exclusive with isRequired.  Otherwise
	 * usage may not clearly explain to a user what required option was missing.  This value is set
	 * to false by default.
	 * @param isHidden Boolean indicating that this option is hidden from the usage statement.
	 */
	public void setIsHidden(boolean isHidden) {
		_log.trace("Setting isHidden: " + isHidden);
		_isHidden = isHidden;
	}
	
	/**
	 * Returns the hidden status of this OptionLegend
	 * @return false if this OptionLegend should be reported in the usage, true if it should be hidden.
	 */
	public boolean isHidden() {
		return _isHidden;
	}
	
	/**
	 * Sets the description to be used for this option.
	 * @param optDescription String describing this OptionLegend
	 */
	public void setDescription (String optDescription){
		_log.trace("Setting description: " + optDescription);
		try {
			setLegend(OPT_DESCRIPTION, optDescription);
		} catch (OptionLegendException e) {
			//no validation is done for description or name, so this
			//exception should never be thrown.
		}
	}

	/**
	 * @return The description.
	 */
	public String getDescription () {
		return _optDescription;
	}
	
	/**
	 * Sets the 'is flag' flag to true or false.  If an option is a flag, no value is expected for the option.
	 * @param flag Boolean indicating that this OptionLegend is a flag or not
	 * @throws OptionLegendException
	 */
	public void setFlag(boolean flag)
	throws OptionLegendException {
		_log.trace ("Setting isFlag: " + flag);
		setLegend(OPT_FLAG, new Boolean(flag).toString());
		if (getDefaultValue() == null) {
			setDefaultValue("false");
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return _optName + ": " + _optDescription;
	}

	/**
	 * @return The name of the option described by this OptionLegend
	 */
	public String getName() {
		return _optName;
	}

	/**
	 * Sets the 'is multi' flag indicating that the option can be specified more than once.  If it
	 * is specified more than once, an ArrayList is creatd to store the values.  Use {@link GetOpts#getList(String)}
	 * to return that list.
	 * @param multi Boolean indicating this option is a multi-valued option.
	 * @throws OptionLegendException
	 */
	public void setMulti(boolean multi)
	throws OptionLegendException {
		_log.trace("Setting isMulti: " + multi);
		setLegend(OPT_MULTI, new Boolean(multi).toString());
	}

	/**
	 * Sets the option name.
	 * @param optName Name of the option
	 */
	public void setName(String optName) {
		try {
			setLegend(OPT_NAME, optName);
		} catch (OptionLegendException e) {
			//no validation is done for description or name, so this
			//exception should never be thrown.
		}
	}

	/**
	 * Sets the 'is required' flag.  Required options must be passed.
	 * @param required Boolean indicating this OptionLegend is required.
	 * @throws OptionLegendException
	 */
	public void setRequired(boolean required)
	throws OptionLegendException {
		_log.trace("Setting isRequired: " + required);
		setLegend(OPT_REQ, new Boolean(required).toString());
	}
	
	/**
	 * A default value can be derived via concatonating static values plus
	 * an option value.  Static values are passed as is.  Values dervied from
	 * another option are passed in ala '$optionName'<br>
	 * <br>
	 * Setting this infers a default value is available and that this option is
	 * hidden from the command line usage.  If the user is intended to override
	 * this value, one should un-hide the option.  Note that hidden options like this
	 * can always be overridden regardless of viewability.<br>
	 * <br>
	 * A derived value may be a multi-valued option, but derivision looses its meaning then.
	 * @param formulae
	 */
	public void setDerivision(String... formula) {
		if (_log.isTraceEnabled()) _log.trace("Setting derivation: " + Arrays.toString(formula));
		if (getDefaultValue() != null) {
			throw new OptionLegendException("Legend cannot have a default value and a derived value.");
		}
		_derivisionFormula = formula;
		setIsDerived(true);
		setIsHidden(true);
		setRequired(false);
	}
	
	/**
	 * Package-private getter to be used by {@link GetOpts}.  GetOpts will expand the formula values
	 * upon request by the caller.
	 * @return
	 */
	String[] getDerivisionFormula() {
		return _derivisionFormula;
	}
	
	/**
	 * Sets the isDerived flag.  This flag is not intended to be set directly by the user, rather
	 * by {@link OptionLegend#setDerivision(String...)}
	 */
	protected void setIsDerived(boolean isDerived) {
		_log.trace("Setting isDerived: " + isDerived);
		_isDerived = isDerived;
	}
	
	/**
	 * Returns true if this option has a dervied default value
	 * @return
	 */
	public boolean isDerived() {
		return _isDerived;
	}
	
	/**
	 * @return true if this option is a flag only option
	 */
	public boolean isFlag() {
		return _flag;
	}

	/**
	 * @return true if this option is a required option
	 */
	public boolean isRequired() {
		return _required;
	}

	/**
	 * @return true if this option is a multi-valued option
	 */
	public boolean isMulti() {
		return _multi;
	}
	
	/**
	 * @return true if this option can be specified multiple times
	 */
	public boolean isLimited() {
		return _isLimited;
	}
	
	/**
	 * @param values
	 */
	public void setAllowedValues (String[] values) {
		if (_log.isTraceEnabled()) _log.trace("Setting allowed values: " + Arrays.toString(values));
		_optValues = values;
		_isLimited = true;
	}
	
	/**
	 * @return An Array of String indicating the allowed values this option can have.
	 */
	public String[] getAllowedValues () {
		return _optValues;
	}
	
	/**
	 * Sets the example value to be displayed during Usage
	 * @param example String of example value
	 */
	public void setExampleValue (String example) {
		_log.trace("Setting example value: " + example);
		_exampleValue = example;
	}
	
	/**
	 * Returns the current example value to be displayed during Usage.  If not yet set, it will default
	 * to the name of the option.
	 * @return
	 */
	public String getExampleValue () {
		if (null == _exampleValue) {
			_exampleValue = _optName;
		}
		return _exampleValue;
	}

	/*
	 * Convinience method used to set the various behaviors of this option.  The array index
	 * this option was specified in is also passed to identify the type of behavior being set.
	 * @param item
	 * @param value
	 * @throws OptionLegendException
	 * 
	 * Seems like we've re-implemented a very basic Map... Why not just back this data up with a map?
	 */
	private void setLegend (int item, String value)
	throws OptionLegendException {
		switch (item) {
		case OPT_NAME: {
			_optName = value; 
			break;
		}
		case OPT_FLAG: {
			_flag = Boolean.valueOf(value);
			break;
		}
		case OPT_DESCRIPTION: {
			_optDescription = value;
			break;
		}
		case OPT_REQ:
			_required = Boolean.valueOf(value);
			break;
		case OPT_MULTI:
			_multi = Boolean.valueOf(value);
			break;
		case OPT_VALUES:
			_optValues = value.split(":");
			_isLimited = true;
			break;
		default:
			throw (new OptionLegendException(item + " is not an undefined option index."));
		}
	}

	/**
	 * Provided an array of OptionLegend objects, this method searches for one of a given name.
	 * @param name Name of the option corresponding to the OptionLegend to find
	 * @param legends The array of OptionLegends to search in
	 * @return The OptionLegend if it's found, otherwise null if not found
	 */
	public static OptionLegend findLegend (String name, OptionLegend[] legends) {
		for (int i = 0; i < legends.length; i++) {
			if (legends[i].getName().equals(name)) {
				return legends[i];
			}
		}
		return null;
	}

	/**
	 * @return the _defaultValue
	 */
	public String getDefaultValue() {
		return getDefaultValue(false);
	}
	public String getDefaultValue(boolean forPrint) {
		String defaultValue = _defaultValue;
		if (forPrint && defaultValue != null && defaultValue.length() == 1) {
			char c = (char) defaultValue.getBytes()[0];
			switch (c) {
				case '\t':
					defaultValue = "(tab)";
					break;
				case '\n':
					defaultValue = "(newline)";
					break;
				case '\r':
					defaultValue = "(carraige return)";
					break;
				case ' ':
					defaultValue = "(space character)";
					break;
			}
		}
		return defaultValue;
	}

	/**
	 * @param value the _defaultValue to set
	 */
	public void setDefaultValue(String value) {
		_log.trace("Setting default value: " + value);
		if (isDerived()) {
			throw new OptionLegendException("Legend cannot have a default value and a derived value.");
		}
		setRequired(false);
		_defaultValue = value;
	}

	public static OptionLegend createDefaultLegend() {
		OptionLegend defaultLegend = new OptionLegend(OPT_DEFAULT_NAME);
		defaultLegend.setRequired(false);
		defaultLegend.setFlag(false);
		defaultLegend.setMulti(true);
		defaultLegend.setIsHidden(true);
		return defaultLegend;
	}
	
	public static OptionLegend createPropertyLegend() {
		OptionLegend propertyLegend = new OptionLegend(OPT_PROPERTY_FILE, true);
		propertyLegend.setRequired(false);
		propertyLegend.setMulti(false);
		propertyLegend.setIsHidden(true);
		propertyLegend.setDescription("Properties file containing additional options desired to be passed at runtime");
		return propertyLegend;
	}
	
	public static List<OptionLegend> createDefaultLegends() {
		List<OptionLegend> defaultLegends = new ArrayList<OptionLegend>();
		OptionLegend unswitched = createDefaultLegend();
		OptionLegend propertyLegend = createPropertyLegend();
		OptionLegend helpLegend = createHelpLegend();
		OptionLegend groupLegend = createPropertyGroup();
		
		defaultLegends.add(unswitched);
		defaultLegends.add(propertyLegend);
		defaultLegends.add(helpLegend);
		defaultLegends.add(groupLegend);
		
		return defaultLegends;
	}
	
	public static OptionLegend createHelpLegend() {
		OptionLegend helpLegend = new OptionLegend(OPT_HELP);
		helpLegend.setFlag(true);
		helpLegend.setRequired(false);
		helpLegend.setIsHidden(true);
		helpLegend.setDescription("Generates this usage");
		return helpLegend;
	}
	
	public static OptionLegend createPropertyGroup() {
		OptionLegend groupLegend = new OptionLegend(OPT_PROPERTY_GROUP);
		groupLegend.setRequired(false);
		groupLegend.setIsHidden(true);
		groupLegend.setDescription("The property group to read option properties for");
		return groupLegend;
	}

    @Override
    public int compareTo(OptionLegend yours) {
        if (yours == null) {
            // I win
            return 1;
        } else {
            // I want an alphabetical sort
            return this.getName().compareToIgnoreCase(yours.getName());
        }
    }
    
}

