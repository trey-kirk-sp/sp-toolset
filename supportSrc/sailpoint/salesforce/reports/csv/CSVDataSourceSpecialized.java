package sailpoint.salesforce.reports.csv;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class CSVDataSourceSpecialized {

	public static final char DEFAULT_DELIM = ',';

	// Fields that are of interest
	public static final String COL_EDIT_DATE = "Edit Date";
	public static final String COL_FIELD_EVENT = "Field / Event";
	public static final String COL_OLD_VALUE = "Old Value";
	public static final String COL_NEW_VALUE = "New Value";
	public static final String COL_TYPE = "Type";
	public static final String COL_STATUS = "Status";
	public static final String COL_PRIORITY = "Priority";
	public static final String COL_CASE_REASON = "Case Reason";
	public static final String COL_CASE_NUMBER = "Case Number";
	public static final String COL_OPEN_DATE = "Opened Date";
	public static final String COL_CLOSED_DATE = "Closed Date";
	public static final String COL_AGE = "Age";
	public static final String COL_AGE_HOURS = "Age (Hours)";
	public static final String COL_DATE_TIME_OPENED = "Date/Time Opened";
	public static final String COL_DATE_TIME_CLOSED = "Date/Time Closed";
	public static final String COL_CASE_OWNER = "Case Owner";
	public static final String COL_ACCOUNT_NAME = "Account Name";
	public static final String COL_SUBJECT = "Subject";
	

	// Fields that are required.  The data MUST have the following fields to be
	// any use to us.
	public static final String[] REQUIRED_COL_STATUS_HISTORY = {
		COL_EDIT_DATE,
		COL_FIELD_EVENT,
		COL_OLD_VALUE,
		COL_NEW_VALUE,
		COL_DATE_TIME_OPENED,
		COL_DATE_TIME_CLOSED,
		COL_CASE_NUMBER
	};
	
	public static final String[] REQUIRED_COL_CASE_DETAIL = {
		COL_PRIORITY,
		COL_DATE_TIME_OPENED,
		COL_DATE_TIME_CLOSED,
		COL_CASE_NUMBER,
		COL_ACCOUNT_NAME
	};

	private Reader _input;

	private char _delim = DEFAULT_DELIM;
	private String[] _fields = null;
	private String[] _currentDataLine;
	private ReportType _type;
	
	public enum ReportType {
		StatusHistory,
		CaseDetail
	}

	private CSVDataSourceSpecialized() throws CSVDataFormatException {

	}


	/**
	 * Generates string of required fields.
	 * @return
	 */
	private String genString(String[] data) {
		StringBuffer reqFields = new StringBuffer();
		for (String field : data) {
			reqFields.append(field + "\t");
		}
		return reqFields.toString();
	}

	/**
	 * Constructor using provided InputStream (typically a FileInputStream)
	 * @param input
	 * @throws CSVDataFormatException 
	 * @throws IOException 
	 */
	public CSVDataSourceSpecialized (InputStream input, ReportType type) throws CSVDataFormatException, IOException {
		_input = new InputStreamReader(input);
		_type = type;
		validateSource();
	}

	/**
	 * Constructor using provided InputStream (typically a FileInputStream)
	 * @param input
	 * @throws CSVDataFormatException 
	 * @throws IOException 
	 */
	public CSVDataSourceSpecialized (InputStream input, char delim, ReportType type) throws CSVDataFormatException, IOException {
		setDelim(delim);
		_input = new InputStreamReader(input);
		_type = type;
		validateSource();
	}


	/**
	 * Constructor using provided String representing actual CSV data.
	 * @param input
	 * @throws CSVDataFormatException 
	 * @throws IOException 
	 */
	public CSVDataSourceSpecialized (String input, ReportType type) throws CSVDataFormatException, IOException {
		_input = new StringReader(input);
		_type = type;
		validateSource();
	}

	/**
	 * Constructor using provided String representing actual CSV data.
	 * @param input
	 * @throws CSVDataFormatException 
	 * @throws IOException 
	 */
	public CSVDataSourceSpecialized (String input,char delim, ReportType type) throws CSVDataFormatException, IOException {
		setDelim(delim);
		_type = type;
		_input = new StringReader(input);
		validateSource();
	}

	/**
	 * Constructor using provided File object which will be converted to InputStream
	 * @param input
	 * @throws CSVDataFormatException 
	 * @throws IOException 
	 */
	public CSVDataSourceSpecialized (File input, ReportType type) throws CSVDataFormatException, IOException {
		this(new FileInputStream(input), type);
	}

	/**
	 * Constructor using provided File object which will be converted to InputStream
	 * @param input
	 * @throws CSVDataFormatException 
	 * @throws IOException 
	 */
	public CSVDataSourceSpecialized (File input, char delim, ReportType type) throws CSVDataFormatException, IOException {
		this(new FileInputStream(input), type);
		setDelim(delim);
	}

	/**
	 * True or throws... not much point to the boolean
	 * @return
	 * @throws CSVDataFormatException
	 * @throws IOException
	 */
	private boolean validateSource () throws CSVDataFormatException, IOException {
		String[] fields = getFields();

		String[] reqFields = null;
		if (_type.equals(ReportType.StatusHistory)) {
			reqFields = REQUIRED_COL_STATUS_HISTORY;
		} else if (_type.equals(ReportType.CaseDetail)) {
			reqFields = REQUIRED_COL_CASE_DETAIL;
		} else {
			// type unsupported, so nothing to validate
			return true;
		}
		if (reqFields.length > fields.length) {
			throw new CSVDataFormatException("Format Invalid (Probably missing required fields):\n" + genString(REQUIRED_COL_STATUS_HISTORY));
		}
		Arrays.sort(fields);
		for (String reqField : reqFields) {
			if (Arrays.binarySearch(fields, reqField) < 0) {
				throw new CSVDataFormatException("Format Invalid (Probably missing required fields):\n" + genString(REQUIRED_COL_STATUS_HISTORY));
			}
		}

		return true;
	}

	/**
	 * Returns the first line of the CSV which should contain the headers.  
	 * @return
	 * @throws IOException
	 * @throws CSVDataFormatException 
	 */
	public String[] getFields() throws IOException, CSVDataFormatException {
		if (_fields == null) {
			_fields = getNextLine();
		}

		// return a clone.  It's expected that binary searching will be done regularly which
		// requires reordering.  Since the order is ultimately important, let's preserve
		// the original.
		return _fields.clone();
	}

	/**
	 * Returns the next line
	 * @return String[] or null if data stream has ended
	 * @throws IOException
	 * @throws CSVDataFormatException 
	 */
	public String[] getNextLine() throws IOException, CSVDataFormatException {
		List<String> data = new ArrayList<String>();
		String[] ret = new String[0];
		char c = (char)_input.read();
		if (!_input.ready()) {
			// End of stream, return null data
			return null;
		}
		StringBuffer sb = new StringBuffer();
		// Unix 'n Winders both have \n, Winders just has \r.  Check for \n and ignore any \r.s
		// Also look for end of stream
		int countQuotes = 0;
		// while the data is not a newline or if we are currently in a quoted phrase, keep going.
		while ((c != '\n' || countQuotes % 2 == 1) && _input.ready() ) {
			if (c == '\r'){
				// do nothing
			}
			if (c == '\\') {
				// could be regular backslash, could be an escape sequence.  Regardless, treat next
				// character as regular data.
				sb.append(c);
				if (_input.ready()) {
					char next = (char)_input.read();
					sb.append(next);
				}
			} else {
				// Here, we will preserve all double quotes and still allow them to be used as tools to treat delims characters
				// as regular data.  A post processor will strip surrounding quotes.
				if (c == '"') {
					countQuotes++;
				}
				// Simple test, if the number of quotes is odd, we are in a quoted phrase... ignore delims
				if (c != getDelim() || countQuotes % 2 != 0) {
					sb.append(c);
				} else {
					data.add(stripSurroundingQuotes(sb.toString()));
					sb.delete(0, sb.length());
				}
			}
			c = (char)_input.read();
		}
		// Get that last field in there.
		data.add(stripSurroundingQuotes(sb.toString()));
		setCurrentDataLine(data.toArray(ret));
		// getNextLine() is called from getFields, so _fields may be null
		if (_fields != null) {
			if (getCurrentLine().length > _fields.length) {
				// no bueno
				throw new CSVDataFormatException("Data exceeds defined columns!\n" + genString(getCurrentLine()));
			}
		}
		return getCurrentLine();
	}
	
	private String stripSurroundingQuotes (String strIn) {
		
		Pattern pattern = Pattern.compile("^\"(.*)\"$", Pattern.DOTALL);
		Matcher matcher = pattern.matcher(strIn);
		String out = matcher.replaceAll("$1");
		
		return out;
	}

	private void setCurrentDataLine(String[] data) {
		_currentDataLine = data;
	}

	public String[] getCurrentLine() {
		return _currentDataLine;
	}

	public Map<String, String> getCurrentLineAsMap() throws IOException, CSVDataFormatException {
		// Could be just highly convinient
		Map<String, String> dataMap = new HashMap<String, String>();
		int i = 0;
		String[] fields = getFields();
		String[] data = getCurrentLine();
		if (data != null) {
			for (i = 0; i < fields.length; i++) {
				if (i >= data.length) {
					// got fewer data fields than the header has, assume null value
					dataMap.put(fields[i], null);
				} else {
					dataMap.put(fields[i], data[i]);
				}
			}
			return dataMap;
		} else {
			return null;
		}
	}

	public Map<String, String> getNextLineAsMap() throws IOException, CSVDataFormatException {
		getNextLine();
		return getCurrentLineAsMap();
	}

	public void setDelim (char delim) {
		_delim = delim;
	}

	public char getDelim () {
		return _delim;
	}

	public String getFieldValue(String fieldName) throws IOException, CSVDataFormatException {
		String[] fields = getFields();
		int i = 0;
		for (String field : fields) {
			if (field.equalsIgnoreCase(fieldName)) {
				// found our column
				return getCurrentLine()[i]; 
			} else {
				i++;
			}
		}
		// didn't find it
		return null;
	}
}
