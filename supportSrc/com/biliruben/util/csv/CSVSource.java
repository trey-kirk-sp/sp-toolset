package com.biliruben.util.csv;

import java.io.IOException;
import java.util.Map;

public interface CSVSource extends Iterable<Map<String,String>> {
	
	public enum CSVType {
		WithHeader,
		WithOutHeader
	}

	public static final char DEFAULT_DELIM = ',';

	/**
	 * Returns the last data set that {@link #getNextLine()} returned.  Note: if this method is called
	 * before {@link #getNextLine()} is ever called, the first line will be returned.  Depending on the type
	 * of CSV file, this may be field names or data.
	 * @return String[] containing each column value in the order defined in the CSV
	 */
	public abstract String[] getCurrentLine();

	/**
	 * Returns the next data set as a Map using the field names  as keys and column values as values.  
	 * Note: if this method is called before {@link #getNextLine()} is ever called, the first line will be returned.
	 * Depending on the type of CSV file, this may be a map where the key value pairs are identicle.
	 * 
	 * If the CSV type is not CSVType.WithHeader, a CSVIllegalOperationException will be thrown.
	 * @return Map of the values using the field names as keys
	 * @throws IOException
	 * @throws CSVIllegalOperationException 
	 */
	public abstract Map<String, String> getCurrentLineAsMap()
			throws IOException, CSVIllegalOperationException;

	/**
	 * Returns the current delimeter
	 * @return current delimeter
	 */
	public abstract char getDelim();

	/**
	 * Returns the first line of the CSV which should contain the headers.  This array is actually a copy of the
	 * internal array where the fields are stored allowing the accessor to rearrange if needed.  
	 * @return String[] if this is a CSV file with headers.  Null otherwise.
	 * @throws IOException
	 * @throws CSVDataFormatException 
	 * @see {@link CSVType}
	 */
	public abstract String[] getFields() throws IOException;

	/**
	 * Returns the next line
	 * @return String[] or null if data stream has ended
	 * @throws IOException
	 * @throws CSVDataFormatException 
	 */
	public abstract String[] getNextLine() throws IOException;

	/**
	 * Returns the next data set as a Map using the field names  as keys and column values as values.
	 * 
	 * If the CSV type is not CSVType.WithHeader, a CSVIllegalOperationException will be thrown.
	 * @return Map of the values using the field names as keys
	 * @throws IOException
	 * @throws CSVIllegalOperationException 
	 */

	public abstract Map<String, String> getNextLineAsMap() throws IOException,
			CSVIllegalOperationException;

	public abstract CSVType getType();

	/**
	 * Sets the delimeter.  If not specified, the default delimeter used is a comma: ,
	 * @param delim char used as the delimeter in the file.
	 */
	public abstract void setDelim(char delim);

}