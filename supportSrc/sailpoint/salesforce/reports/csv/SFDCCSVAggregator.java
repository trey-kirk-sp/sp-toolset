package sailpoint.salesforce.reports.csv;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import com.biliruben.util.ListUtil;
import com.biliruben.util.csv.CSVSource;
import com.biliruben.util.csv.CSVSourceImpl;

/**
 * This class will take in two CSVDataSource objects and aggregate them together.  It will use a common
 * data value, like case number, to aggregate the data.  Since the data represents SalesForce data, we'll
 * build SFDC POJOs when read in and then serialize those objects into CSV for the aggregated values.
 * @author trey.kirk
 *
 */
public class SFDCCSVAggregator {
	
	/*
	 * Information we'll need:
	 * - two CSV datasources (future: why stop at two?)
	 * - correlation value (future: analyze headers and correlate using like headers)
	 * 
	 * Returns:
	 * CSVDatasource including all combined values.
	 */
	
	private List<CSVSourceImpl> sources;
	private List<String> correlationFields;
	
	/**
	 * 
	 * @return
	 * @throws IOException
	 */
	protected List<String> getUniqueFields() throws IOException {
		List<String> allFields = getSourceFields();
		TreeSet<String> uniqueFields = new TreeSet<String>();
		for (String field : allFields) {
			uniqueFields.add(field);
		}
		return new ArrayList<String>(uniqueFields);
		
	}
	
	/**
	 * Returns a list of all the headers of provided sources
	 * @throws IOException 
	 */
	protected List<String> getSourceFields() throws IOException {
		List<String> allFields = new ArrayList<String>(); 
		for (CSVSource source : sources) {
			String[] fields = source.getFields();
			for (String field : fields) {
				allFields.add(field);
			}
		}
		return allFields;
	}
	
	public List<String> getCorrelationFields() {
		return correlationFields;
	}
	
	public void setCorrelationFields(List<String> correlationFields) {
		this.correlationFields = correlationFields;
	}
	
	public void addCorrelation (String fieldName) {
		List<String> correlationFields = getCorrelationFields();
		if (correlationFields == null) {
			correlationFields = new ArrayList<String>();
			setCorrelationFields(correlationFields);
		}
		correlationFields.add(fieldName);
	}

	/**
	 * 
	 * @return
	 * @throws IOException
	 */
	protected List<String> findCorrelationFields() throws IOException {
		List<String> sourceFields = getSourceFields();
		TreeSet<String> uniques = new TreeSet<String>();
		List<String> correlationFields = new ArrayList<String>();
			for (String field : sourceFields) {
				boolean isunique = uniques.add(field);
				if (!isunique) {
					correlationFields.add(field);
				}
			}
		
		return correlationFields;
	}
	
	public void setDataSources(List<CSVSourceImpl> sources) {
		this.sources = sources;
	}
	
	public List<CSVSourceImpl> getDataSources() {
		return this.sources;
	}
	
	public void addDataSource(CSVSourceImpl source) {
		List<CSVSourceImpl> sources = getDataSources();
		if (sources == null) {
			sources = new ArrayList<CSVSourceImpl>();
			setDataSources(sources);
		}
		sources.add(source);
	}
	
	public CSVSource aggregate() throws IOException {
		if (correlationFields == null) {
			correlationFields = findCorrelationFields();
		}
		
		StringBuffer aggData = new StringBuffer();
		String delim = String.valueOf(aggDelim);
		//Add the headers
		List<String> fields = getUniqueFields();
		aggData.append(ListUtil.listAsString(fields, delim));
		
		
		
		
		CSVSource aggSource;		
		
		return null;
		
	}
	
	private boolean aggHasHeader = true;

	/**
	 * @return the aggHasHeader
	 */
	public boolean isAggHasHeader() {
		return aggHasHeader;
	}

	/**
	 * @param aggHasHeader the aggHasHeader to set
	 */
	public void setAggHasHeader(boolean aggHasHeader) {
		this.aggHasHeader = aggHasHeader;
	}
	
	private char aggDelim = CSVSource.DEFAULT_DELIM;

	/**
	 * @return the aggDelim
	 */
	public char getAggDelim() {
		return aggDelim;
	}

	/**
	 * @param aggDelim the aggDelim to set
	 */
	public void setAggDelim(char aggDelim) {
		this.aggDelim = aggDelim;
	}
	
	
}
