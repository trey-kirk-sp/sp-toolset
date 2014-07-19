/**
 * GetOpts is a package that's likely been redone time and time again with various improvements
 * (and unimprovements) through each developer's own interpretation.  This one can be considered
 * to be yet another re-invented wheel.  So then why was it written?  It was an excercise for me
 * and something else to put in my own personal tool box.  How you got it, who knows.
 */
package com.biliruben.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * GetOpts is the processing class that reads {@link OptionLegend} objects to determine how command
 * line options should be passed.  It then, by way of the {@link #setOpts(String[])} method, parses
 * an array of String objects to determine the options and their values.  These values can then
 * be retrieved using {@link #getStr(String)} and {@link #getList(String)}.<br>
 * <br>
 * One thing that I believe is unique to this iteration of GetOpts is the {@link #genUsage()} method
 * which will use the provided {@link OptionLegend} objects to generate usage.<br>
 * <br>
 * Example, consider a Copy class that can take a -R (for recurisive copying) and -host (for remote copying).
 * The GetOpts class can be used in the following manner:<br>
 * <code><br>
 * <br>
 *&nbsp;GetOpts options = new GetOpts(MyCopy.class);<br>
 *<br>
 *&nbsp;OptionLegend legend = new OptionLegend("R", "Recursive copy");<br>
 *&nbsp;legend.setFlag(true);<br>
 *&nbsp;options.addLegend(legend);<br>
 *		<br>
 *&nbsp;legend = new OptionLegend("host", "Host to execute the copy on");<br>
 *&nbsp;options.addLegend(legend);<br>
 *				<br>
 *&nbsp;options.setUsageTail("fromFile toFile");<br>
 *&nbsp;options.setDescriptionTail("fromFile: source file\ntoFile: target file\n");<br>
 *		<br>
 *&nbsp;try {<br>
 *&nbsp;&nbsp;&nbsp;options.setOpts (args);<br>
 *&nbsp;} catch (OptionParseException e) {<br>
 *&nbsp;&nbsp;&nbsp;System.err.print("ERROR: ");<br>
 *&nbsp;&nbsp;&nbsp;System.err.print(e.getMessage());<br>
 *&nbsp;&nbsp;&nbsp;System.err.println(options.genUsage());<br>
 *&nbsp;}<br>
 *		
 *</code>
 * Which will generate the following usage if the options passed are insufficient:<br>
 * <br>
 * ERROR: length not a defined option!<br>
 * Usage:<br>
 * MyCopy [-R] [-host host] fromFile toFile<br>
 * -R: Recursive copy; flag only<br>
 * -host: Host to execute the copy on<br>
 * fromFile: source file<br>
 * toFile: target file<br>
 *<br>
 * @see OptionLegend
 * @since JDK 1.5
 * @author trey.kirk 
 */
public class GetOpts {

	private static Log _log = LogFactory.getLog(GetOpts.class);
	
	// Our options map
	private HashMap<String, Object> _opts = new HashMap<String, Object>();

	// Where we keep all of the OptionLegend objects
	private HashMap<String, OptionLegend> _masterLegend = new HashMap<String, OptionLegend>();

	// The string that identifies the switch for options.  Ala in "-swtiched", the '-' switch marks 
	// 'switched' as an option.
	private String _optSwitch;

	// String that is appended to the generated usage information.  See setUsageTail(String)
	private String _usageTail;

	// String that is appeneded to the generated description (part of the usage statement).
	// See setDescriptionTail(String).
	private String _descriptionTail;

	// String used to denote that no other switched options should be expected.
	private String _endOptsSwitch;

	private Class _caller;

	private boolean _propertiesLegendIsHidden = true;

	/**
	 * Default string used for the option switch.  Use {@link #GetOpts(Class, String, OptionLegend[])} 
	 * constructor to override
	 */
	public static final String DEFAULT_SWITCH = "-";

	/**
	 * Default string used to denote no further values are designated by a switched option.
	 * @see #setEndOptsSwitch(String)
	 * @see #getUnswitchedOptions()
	 */
	public static final String DEFAULT_END_OPT_SWITCH = "--";

	/**
	 * Main method used during testing.  Ignore.
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		GetOpts options = new GetOpts(GetOpts.class);

		OptionLegend legend = new OptionLegend("R", "Recursive copy");
		legend.setFlag(true);
		options.addLegend(legend);

		legend = new OptionLegend("host", "Host to execute the copy on");
		legend.setRequired(true);
		options.addLegend(legend);

		legend = new OptionLegend("myDerived", "Derived option");
		legend.setDerivision("this", "$host", "that", "$R", "the other thing", "$Narp");
		legend.setIsHidden(false);
		options.addLegend(legend);

		legend = new OptionLegend("list", "This is a list!");
		legend.setMulti(true);
		options.addLegend(legend);

		options.setUsageTail("fromFile toFile");
		options.setDescriptionTail("fromFile: source file\ntoFile: target file\n");

		options.parseOpts(args);

		System.out.println(options.toString());
	}

	/**
	 * Basic constructor. {@link #DEFAULT_SWITCH} and {@link #DEFAULT_END_OPT_SWITCH} are used.
	 * @param caller Class that represents the calling class.  It's referenced in {@link #genUsage()}
	 * @see #addLegend(OptionLegend) to add OptionLegend objects. 
	 */
	public GetOpts (Class caller) {
		_caller = caller;
		_optSwitch = DEFAULT_SWITCH;
		_endOptsSwitch = DEFAULT_END_OPT_SWITCH;
		//addLegend(OptionLegend.createDefaultLegend());
		addAllLegends(OptionLegend.createDefaultLegends());
	}

	/**
	 * GetOpts can only deduce so much information about what options are expected and how.  Elements
	 * such as the unswitched options would need to be appended to the usage statement.  Take the following
	 * usage as an exaple:
	 * 
	 * copy [-R] [-h host] file1 file2
	 * 
	 * Where -R and -h are described well using OptionLegend objects, file1 and file2 are not.  Thus
	 * the implementer would need to use the following call to get the above full usage:
	 * 
	 * setUsageTail("file1 file2")
	 * 
	 * @param usageTail String to append to the end of the generated usage.
	 * @see #genUsage()
	 */
	public void setUsageTail(String usageTail) {
		_log.debug("Setting usage tail: " + usageTail);
		this._usageTail = usageTail;
	}

	public void setPropertiesDefaultFileName (String fileName) {
		_log.debug("Default properties file name: " + fileName);
		OptionLegend properties = _masterLegend.get(OptionLegend.OPT_PROPERTY_FILE);
		if (properties != null) {
			properties.setDefaultValue(fileName);
		} else {
			// throw a warning (log.warn)
			_log.warn("No properties legend found!");
		}
	}

	/**
	 * When creating {@link OptionLegend} objects, a description can be included.  These descriptions are
	 * used for clarification comments when generating usage.  Unswitched options do not have any description
	 * that can be used thus the implementer would need to use the following to include descriptive comments
	 * for unswitched options.
	 * @param descriptionTail String to append to the description comments from the {@link #genUsage()} method.
	 * @see #genUsage()
	 */
	public void setDescriptionTail(String descriptionTail) {
		_log.debug("Setting description tail: " + descriptionTail);
		this._descriptionTail = descriptionTail;
	}

	/**
	 * Sometimes it's convenient to generate usage and include the OptionParseException message that led to the usage
	 * statement being needed in the first place.
	 * @param errMsg
	 * @return
	 */
	public String genUsage(OptionParseException errMsg) {
		_log.debug("Generating usage for error: " + errMsg.toString());
		if (errMsg != null) {
			StringBuffer msg = new StringBuffer(errMsg.toString() + "\n");
			msg.append(genUsage());
			return msg.toString();
		} else {
			return (genUsage());
		}
	}

	public String genUsage() {
		return genUsage(false);
	}

	/**
	 * Usage can be inferred simply by reading the passed in OptionLegend objects.  The notation uses
	 * the following notation:
	 * -option - this option is required.
	 * [-option] - this option is optional.
	 * -option[] - this option can be passed multiple times and will generate a list of values.
	 * @return A usage string that can be used by the implementer as a help statement.
	 */
	public String genUsage(boolean showFull) {
		_log.debug("Generating usage, full option: " + showFull);
		if (!showFull) {
			// set properties Legend "hideness"
			OptionLegend properties = _masterLegend.get(OptionLegend.OPT_PROPERTY_FILE);
			properties.setIsHidden(_propertiesLegendIsHidden);
		} else {
			// full description, unhide everything.
			for (OptionLegend legend : _masterLegend.values()) {
				if (!legend.getName().equals(OptionLegend.OPT_DEFAULT_NAME)) {
					// still don't want to unhide the default option
					legend.setIsHidden(false);
				}
			}
		}


		// Initializes the StringBuffer that will be used to create the usage information
		StringBuffer usage = new StringBuffer(256);
		StringBuffer description = new StringBuffer(512);
		usage.append("\nUsage:\n" + _caller.getSimpleName());

		// Need to iterate over the OptionLegend objects
		Collection<OptionLegend> legends = _masterLegend.values();
		Iterator<OptionLegend> legendsIt = legends.iterator();
		while (legendsIt.hasNext()) {
			OptionLegend legend = legendsIt.next();
			// Don't try to generate usage for the DEFAULT OptionLegend
			if (!legend.getName().equals(OptionLegend.OPT_DEFAULT_NAME) 
					&& !legend.isHidden()) {
				// Append '-optionname' to the description text
				description.append("\n\t" + _optSwitch + legend.getName() + ": ");
				// If there is a null value for a legend's description, change it to an empty String
				String legendDescription = legend.getDescription();
				if (legendDescription == null) {
					legendDescription = "";
				}
				// Append the OptionLegend's description to the current description text
				description.append(legendDescription);
				// Append a space to the usage statement in preperation for the usage information of the option
				usage.append(" ");
				if (!legend.isRequired()) {
					// Begins the surrounding brackets for required options
					usage.append("[");
				} else {
					// Adds "required" to the description if required
					description.append("; required");
				}

				// Adds the -option to the usage statement
				usage.append(this._optSwitch + legend.getName());

				// Display default value
				if (legend.getDefaultValue(true) != null 
						&& !legend.getName().equals(OptionLegend.OPT_HELP)
						&& !legend.isFlag()) {
					description.append("; Default value: " + legend.getDefaultValue());
				}

				// Check if the option can be used multiple times
				if (legend.isMulti()) {
					description.append("; can be used multiple times");
					usage.append("[]");
				}

				// Check if the option has a limited set of values
				// Display the allowed values in the usage
				if (legend.isLimited()) {
					description.append("; must be one of the following: ");
					String[] allowedValues = legend.getAllowedValues();
					usage.append(" [");
					for (int i = 0; i < allowedValues.length; i++) {
						description.append(allowedValues[i]);
						usage.append(allowedValues[i] + " ");
						if (i < allowedValues.length - 1) {
							description.append(", ");
							usage.append("| ");
						}
					}
					usage.append("]");
					description.append("; ");
				} else {
					// Check if the option is a flag only
					if (!legend.isFlag()) {
						// Append the example value for the legend
						usage.append(" " + legend.getExampleValue());
					} else if (!legend.getName().equals(OptionLegend.OPT_HELP)) {
						description.append("; flag only");
					}
				}

				// Close up some stuff
				if (!legend.isRequired()) {
					usage.append("]");
				}
			}
		}


		// Add any extra usage information
		if (_usageTail != null) {
			usage.append(" " + _usageTail);
		}

		usage.append("\n");

		// little extra \n never hurt nobody
		description.append("\n");
		
		// Add any extra description information
		if (_descriptionTail != null) {
			description.append("\n" + _descriptionTail.toString());
		}

		usage.append(description);
		return usage.toString();
	}

	/**
	 * Once all OptionLegend objects have been created and registered, pass the arguments into
	 * this method and they will be parsed.  Any required option that is missing, option that is specified
	 * multiple times when it should not be, or an option is specified when not expected will generate
	 * an {@link OptionParseException}
	 * @param args An array of Strings representing the passed-in options.  Typically, this is the same String[] passed into
	 * the main method
	 * @throws OptionParseException
	 */
	public void parseOpts (String[] args) {

		if (_log.isDebugEnabled()) _log.debug("Parsing options: " + Arrays.toString(args));
		// Since our properties file is potentially passed in as an option, we need to parse the commandline first.
		// Afterwards, we'll parse the propeties.  This means we need to pull out the validation steps

		//Instead of throwing the parse exceptions immediately, collect the error messages and throw
		// one at the end of parsing.  That way a full disclosure of what's wrong is provided.
		boolean endOpts = false;
		for (int i = 0; i < args.length; i++) {

			// Checks to see if the switch that indicates no further options are expected is passed
			if (args[i].equals(_endOptsSwitch)) {
				endOpts = true;
				_log.trace("end switch found, ending option parse");
				continue;
			}

			if (args[i].startsWith(_optSwitch) && !endOpts) {
				_log.trace ("Parsing option: " + args[i]);
				// pop out the option name
				String option = args[i].split(_optSwitch)[1];
				OptionLegend legend = _masterLegend.get(option);
				if (legend == null) {
					// Since we want properties to be portable and reusable, we'll allow 'undefined' properties to fly
					// keep this change on cmd line options only
					OptionParseException e = new OptionParseException (option + " not a defined option!", this);
					_log.debug(e);
					throw e;
				}

				// process the option
				if (legend.isFlag()) {
					_opts.put(option, true);

				} else if (legend.isMulti()) {
					// If this throws a ClassCast exception, then somebody has been mucking with our
					// options map behind our back, and that's bad.  Let it throw unchecked.
					ArrayList<String> optList = (ArrayList<String>)_opts.get(option);
					if (optList == null) {
						optList = new ArrayList<String>();
					}
					i++;

					if (i >= args.length) {
						OptionParseException e = new OptionParseException ("No value specified for option " + legend.getName(), this);
						_log.debug(e);
						throw e;
					} else {
						testIfAllowed (legend, args[i]);
						optList.add(args[i]);
						_opts.put(option, optList);
					}

				} else {
					// Just a regular string, just add it in
					i++;
					if (i >= args.length) {
						OptionParseException e = new OptionParseException ("No value specified for option " + legend.getName(), this);
						_log.debug(e);
						throw e;
					} else {
						testIfAllowed (legend, args[i]);
						testIfSet(option);
						_opts.put(option,args[i]);
					}
				}	
			} else {
				// This isn't an option and we're not looking for a value, assume this to be
				// an "unswitched" option, or default
				String option = OptionLegend.OPT_DEFAULT_NAME;
				OptionLegend legend = _masterLegend.get(option);
				if (legend == null) {
					throw new OptionParseException ("Unswitched option unexpected for value: " + args[i], this);
				}
				testIfAllowed (legend, args[i]);
				if (legend.isMulti()) {
					ArrayList<String> optList = (ArrayList<String>)_opts.get(option);
					if (optList == null) {
						optList = new ArrayList<String>();
					}
					optList.add(args[i]);
					_opts.put(option, optList);
				} else {
					testIfSet(option);
					_opts.put(option, args[i]);
				}
			}		
		}
		
		// got runtime options, check for -?
		Object help = _opts.get(OptionLegend.OPT_HELP);
		if (help != null) {
			// help requested!
			String usage = genUsage(true);
			System.out.println(usage);
			System.exit(0);
		}
		
		// Do we need to fetch properties from a group
		String propGroup = this.getStr(OptionLegend.OPT_PROPERTY_GROUP);

		// Parse properties file now
		String propertyFileName = this.getStr(OptionLegend.OPT_PROPERTY_FILE);
		_log.trace("Parsing property file: " + propertyFileName);
		PropertyOptions properties = null;
		try {
			if (propertyFileName != null) {
				properties = new PropertyOptions(propertyFileName);
			} else {
				properties = new PropertyOptions();
			}
			if (propGroup != null) {
				properties.setPropertyGroup(propGroup);
			}
		} catch (IOException e) {
			// If we throw, I want to expose the -properties option
			OptionLegend propertiesLegend = _masterLegend.get(OptionLegend.OPT_PROPERTY_FILE);
			Object passedPropertyFileName = _opts.get(propertiesLegend.getName());
			_log.debug("Loading properties file: passedIn=" + passedPropertyFileName + ", derived=" + propertyFileName);
			if (passedPropertyFileName != null) {
				// specified a property file and we got an IOException.  Bail!
				propertiesLegend.setIsHidden(false);
				OptionParseException parseException = new OptionParseException ("Property file not loaded! " + propertyFileName + "\nCause: " + e.getMessage() + "\n", this);
				_log.error(parseException);
				throw parseException;
			} else {
				// No file was specified. Only throw if the exception was a FileNotFound
				if (!(e instanceof FileNotFoundException)) {
					propertiesLegend.setIsHidden(false);
					OptionParseException parseException = new OptionParseException ("Property file not loaded! " + OptionLegend.OPT_DEFAULT_NAME + "\nCause: " + e.getMessage() + "\n", this);
					_log.error(parseException);
					throw parseException;
				} else {
					// Future: when we start logging, toss in a log.debug about property file not found
					_log.debug("Default property file not found!  Skipping property file parse.");
					_log.debug(e);
				}
			}
		}

		// The options have been set, check to see that our required options were given
		Iterator<OptionLegend> values = _masterLegend.values().iterator();
		String missing = new String();
		boolean notFound = false;

		while (values.hasNext()) {
			// for each optionLegend, check:
			// - is it present?  If yes, no further action
			// - when not present, is it provided via properties?  If yes, set it
			// - when not present or provided by properties, is it required?  If yes, throw
			OptionLegend legend = (OptionLegend)values.next();

			String option = legend.getName();
			// is it present?
			if (_opts.get(option) != null) {
				// no action
				continue;
			}

			// not present, is it provided via properties?
			if (properties != null && properties.isDefined(option)) {
				// provided by properties.  Populate
				if (!legend.isMulti()) {
					_opts.put(option, properties.getString(option));
				} else {
					_opts.put(option, properties.getList(option));
				}
			}

			// not present, may or may not be provided by properties.  Is it
			// required and has it been set?
			if (legend.isRequired()) {
				if (_opts.get(option) == null) {
					missing += "Option missing: " + legend.getName() + "\n";
					notFound = true;
				}
			}
		}

		if (notFound) {
			OptionParseException e = new OptionParseException(missing, this);
			_log.debug(e);
			throw e;
		}
	}


	/**
	 * @Deprecated Use {@link #parseOpts(String[])} instead 
	 */
	public void setOpts (String[] args) {
		parseOpts(args);
	}


	/*
	 * Test method to determine if the passed in value is an expected value as well as not
	 * being an option
	 * @param legend
	 * @param value
	 * @throws OptionParseException
	 */
	private void testIfAllowed (OptionLegend legend, String value)
	throws OptionParseException {
		// First check to see if it's an option (look for the switch)
		if (value.startsWith(_optSwitch)) {
			throw new OptionParseException("Option specified when value expected: " + value, this);
		}

		// then see if it's been already set
		if (legend.isLimited()) {
			String[] allowedValues = legend.getAllowedValues();
			Arrays.sort(allowedValues);
			if (Arrays.binarySearch(allowedValues, value) < 0) {
				throw new OptionParseException(value + " is not allowed in the provided list for the " + legend.getName() + " option!", this);
			}
		}
	}

	/*
	 * Checks to see if an option is already set.
	 * @param testOption
	 * @throws OptionParseException
	 */
	private void testIfSet (String testOption)
	throws OptionParseException {
		if (_opts.get(testOption) != null) {
			throw new OptionParseException ("Duplicate option specified: " + testOption, this);
		}
	}


	/**
	 * Returns the value matched by the passed in option returned as a String.  If the matched
	 * value is not found or not a String, null is returned.
	 * @param opt Name of the option that's matched to the desired value.
	 * @return String value or null
	 */
	public String getStr (String opt) {
		_log.trace("Getting string option: " + opt);
		Object option = _opts.get(opt);
		OptionLegend legend = _masterLegend.get(opt);
		if (option != null) { // was specified on command line.  Overrides any default or derived value.
			_log.trace("Return: " + option.toString());
			return option.toString();	
		} else {
			if (legend != null) { // Legend is defined but a value wasn't found.  Pass derived or default value
				if (legend.isDerived()) {
					StringBuffer buff = new StringBuffer();
					String[] formula = legend.getDerivisionFormula();
					if (_log.isTraceEnabled()) _log.trace("Value is derived, derivation: " + Arrays.toString(formula));
					for (String formulette : formula) {
						if (formulette.matches("\\$.*")) {
							// Legend name was specied as a variable, expand
							String legendName = formulette.substring(1);
							String value = this.getStr(legendName);
							if (value == null) {
								// null option, append raw value instead
								buff.append(formulette);
							} else {
								buff.append(value);
							}
						} else {
							buff.append(formulette);
						}
					}
					_log.trace("Return: " + buff.toString());
					return buff.toString();
				} else {
					_log.trace("Return: " + legend.getDefaultValue());
					return legend.getDefaultValue();  // Will return null if one isn't specified, which is fine
				}
			} else { // was not a legend that was defined.  Should this maybe throw an error?
				_log.trace("Return: null");
				return null;
			}
		}

	}

	/**
	 * Returns the value matched by the passed in option returned as an ArrayList.  If the matched
	 * values is not found or is not an ArrayList, null is returned.
	 * @param opt Name of the option that's matched to the desired value.
	 * @return List value or null
	 */
	@SuppressWarnings("unchecked")
	public List<String> getList (String opt) {
		_log.trace ("Getting list option: " + opt);
		if (_opts.get(opt) instanceof ArrayList) {
			List<String> retVal = (ArrayList<String>)_opts.get(opt);
			if (_log.isTraceEnabled()) _log.trace ("Returning: " + retVal);
			return retVal;
		} else {
			_log.trace("Returning: null");
			return (List<String>)null;
		}
	}

	/**
	 * Registers the {@link OptionLegend} object to this object to be used in later
	 * option parsing. 
	 * @param legend OptionLegend to parse options.
	 */
	public void addLegend(OptionLegend legend) {
		_log.debug("Adding legend: " + legend);
		_masterLegend.put(legend.getName(), legend);
	}

	public void addAllLegends(Collection<OptionLegend> legends) {
		for (OptionLegend legend : legends) {
			addLegend(legend);
		}
	}

	/**
	 * Sets the switch that will designate options.
	 * @param optsSwitch Switch as String.
	 */
	public void setEndOptsSwitch(String optsSwitch) {
		this._endOptsSwitch = optsSwitch;
	}

	/**
	 * Returns values that are not matched to an option.
	 * @return ArrayList<String> of values.
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<String> getUnswitchedOptions() {
		return (ArrayList<String>)_opts.get(OptionLegend.OPT_DEFAULT_NAME);
	}

	public void setPropertiesLegendHidden(boolean isHidden) {
		_propertiesLegendIsHidden = isHidden;
	}

	/**
	 * This method iterates the provided options and their specified values
	 */
	@Override
	public String toString() {
		StringBuffer buff = new StringBuffer();
		buff.append(_caller.getName());
		for (String key : _opts.keySet()) {
			Object value = _opts.get(key);
			buff.append("\n\t" + key + " :: " + value);
		}
		return buff.toString();
	}
}
