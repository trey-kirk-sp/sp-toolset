package com.biliruben.util.csv;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class CSVSourceImpl implements CSVSource {

	private char _delim = DEFAULT_DELIM;
	private boolean _haveRead = false;
	
	private String[] _fields;
	private Reader _input;
	private String[] _currentDataLine;
	private CSVType _type;
	
	public void test() throws IOException {
		_input.reset();
	}


	public CSVSourceImpl (File inputFile, CSVType type) throws FileNotFoundException {
		this (inputFile, type, DEFAULT_DELIM);
	}

	public CSVSourceImpl (File inputFile, CSVType type, char delim) throws FileNotFoundException {
		this (new FileReader(inputFile), type, delim); 
	}

	public CSVSourceImpl (Reader input, CSVType type) {
		this (input, type, DEFAULT_DELIM);
	}

	public CSVSourceImpl (Reader input, CSVType type, char delim) {
		try {
			setType(type);
		} catch (CSVIllegalOperationException e) {
			// This shouldn't
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		setInput(input);
		setDelim(delim);
	}

	public CSVSourceImpl (String inputCsvData, CSVType type) {
		this (inputCsvData, type, DEFAULT_DELIM);
	}

	public CSVSourceImpl (String inputCsvData, CSVType type, char delim) {
		this (new StringReader(inputCsvData), type, delim);
	}

	/* (non-Javadoc)
	 * @see com.biliruben.util.CSVSource#getCurrentLine()
	 */
	public String[] getCurrentLine() {
		return _currentDataLine;
	}

	/* (non-Javadoc)
	 * @see com.biliruben.util.CSVSource#getCurrentLineAsMap()
	 */
	public Map<String, String> getCurrentLineAsMap() throws IOException, CSVIllegalOperationException {
		if (!getType().equals(CSVType.WithHeader)) {
			throw new CSVIllegalOperationException("Cannot build a map of key value pairs when no field names are available.  Change CSVType to " + CSVType.WithHeader.toString());
		}
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

	/* (non-Javadoc)
	 * @see com.biliruben.util.CSVSource#getDelim()
	 */
	public char getDelim () {
		return _delim;
	}

	/**
	 * Generates string of required fields.
	 * @return
	 */
	/*private String genString(String[] data) {
		StringBuffer reqFields = new StringBuffer();
		for (String field : data) {
			reqFields.append(field + "\t");
		}
		return reqFields.toString();
	}*/

	/* (non-Javadoc)
	 * @see com.biliruben.util.CSVSource#getFields()
	 */
	public String[] getFields() throws IOException {
		// If _fileds is null, we must assume we have not yet fetched the fields.  It is the constructor's job
		// to populate the fields before allowing any data reading.
		if (_fields == null && getType().equals(CSVType.WithHeader)) {
			_fields = getNextLineImpl();

			// return a clone.  It's expected that binary searching will be done regularly which
			// requires reordering.  Since the order is ultimately important, let's preserve
			// the original.
			return _fields.clone();
		} else if (_fields != null){
			return _fields.clone();
		}else {
			return null;
		}



	}

	/* (non-Javadoc)
	 * @see com.biliruben.util.CSVSource#getNextLine()
	 */
	public final String[] getNextLine() throws IOException {
		// garuantees that no row of data can be fetched without first knowing the fields
		if (_fields == null && getType().equals(CSVType.WithHeader)) {
			getFields();
		}
		return getNextLineImpl();
	}

	/* (non-Javadoc)
	 * @see com.biliruben.util.CSVSource#getNextLineAsMap()
	 */

	public Map<String, String> getNextLineAsMap() throws IOException, CSVIllegalOperationException {
		getNextLine();
		return getCurrentLineAsMap();
	}

	private String[] getNextLineImpl() throws IOException {
		List<String> data = new ArrayList<String>();
		String[] ret = new String[0];
		char c = (char)_input.read();
		if (!_haveRead) _haveRead = true;
		if (!_input.ready()) {
			// End of stream, return null data
			setCurrentDataLine(null);
			return getCurrentLine();
		}
		StringBuffer sb = new StringBuffer();
		// Unix 'n Winders both have \n, Winders just has \r.  Check for \n and ignore any \r.s
		// Also look for end of stream
		int countQuotes = 0;
		// while the data is not a newline or if we are currently in a quoted phrase, keep going.
		//while ((c != '\n' || countQuotes % 2 == 1) && _input.ready() ) {
		while ((c != '\n' || countQuotes % 2 == 1) && c != (char)-1 ) {
			if (c == '\\') {
				// could be regular backslash, could be an escape sequence.  Regardless, treat next
				// character as regular data.
				sb.append(c);
				if (_input.ready()) {
					char next = (char)_input.read();
					sb.append(next);
				}
			} else if (c != '\r') {
				// Here, we will preserve all double quotes and still allow them to be used as tools to treat delims characters
				// as regular data.  A post processor will strip surrounding quotes.
				if (c == '"') {
					countQuotes++;
				}
				// Simple test, if the number of quotes is odd, we are in a quoted phrase... ignore delims
				if (c != getDelim() || countQuotes % 2 != 0) {
					sb.append(c);
				} else {
					// We've reached a delimeter outside of a quoated phrase.  We have our next token of data
					data.add(stripSurroundingQuotes(sb.toString()));
					sb.delete(0, sb.length());
				}
			}
			c = (char)_input.read();
		}
		// When we reach the end of the line, we still have data hanging out in the StringBuffer. Get that last field in there.
		data.add(stripSurroundingQuotes(sb.toString()));
		setCurrentDataLine(data.toArray(new String[0]));
		// getNextLine() is called from getFields, so _fields may be null
		if (_fields != null) {

		}
		return getCurrentLine();		
	}

	/**
	 * Method to return the underlying Reader object.  With getNextLine() final, future implementations
	 * may like to have access to the Reader in order to enhance behavior.  
	 * @return Underlying Reader object
	 */
	protected Reader getReader() {
		return _input;
	}

	/* (non-Javadoc)
	 * @see com.biliruben.util.CSVSource#getType()
	 */
	public CSVType getType() {
		return _type;
	}

	private void setCurrentDataLine(String[] data) {
		_currentDataLine = data;
	}

	/* (non-Javadoc)
	 * @see com.biliruben.util.CSVSource#setDelim(char)
	 */
	public void setDelim (char delim) {
		_delim = delim;
	}

	protected void setInput (Reader input) {
		_input = input;
	}

	protected void setType (CSVType type) throws CSVIllegalOperationException {
		// Chaging the type after reading is bad
		if (_haveRead) {
			throw new CSVIllegalOperationException ("Cannot change type after csv input has been read from.");
		}
		_type = type;
	}

	/*
	 * Internal method used to strip data of quotes.  The writer will be in charge of adding them back.
	 */
	private String stripSurroundingQuotes (String strIn) {

		Pattern pattern = Pattern.compile("^\"(.*)\"$", Pattern.DOTALL);
		Matcher matcher = pattern.matcher(strIn);
		String out = matcher.replaceAll("$1");

		return out;
	}


	public Iterator<Map<String, String>> iterator() {
		return new CSVSourceIterator();
	}
	
	private class CSVSourceIterator implements Iterator<Map<String,String>> {
		private Map<String, String> _line;

		public boolean hasNext() {
			try {
				this._line = getNextLineAsMap();
			} catch (IOException e) {
				e.printStackTrace();
				this._line = null;
			} catch (CSVIllegalOperationException e) {
				e.printStackTrace();
				this._line = null;
			}
			return _line != null;
		}

		public Map<String, String> next() {
			
			return _line;
		}

		public void remove() {
			// noop
		}
		
	}

}
