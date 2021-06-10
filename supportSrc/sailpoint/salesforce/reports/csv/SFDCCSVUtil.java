package sailpoint.salesforce.reports.csv;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.jasperreports.engine.JRDataSource;

import com.biliruben.util.csv.CSVIllegalOperationException;
import com.biliruben.util.csv.CSVSource;
import com.biliruben.util.csv.CSVUtil;

import sailpoint.salesforce.reports.BaseSFDCJRDataSource;
import sailpoint.salesforce.reports.object.AbstractSFDCObject;
import sailpoint.salesforce.reports.object.Case;
import sailpoint.salesforce.reports.object.CaseComment;

/**
 * Utility class designed to leverage CSV reports from Salesforce.com.  This contains an assortment of
 * static methods designed to help convert CSV data from Salesforce into POJOs, Collections, or JRDataSources.
 * Additional methods can be added as needed.
 * @author trey.kirk
 *
 */
public class SFDCCSVUtil extends CSVUtil {
	
	/**
	 * Enum used when determing an object type based on a line of CSV data.  In situations where top level object
	 * data is intermingled in child object data, the top level object will be created with the child object being
	 * created after and then linked to the parent.  For example, given a data line that contains a case number and
	 * a case comment, the Case object will be created (by virtue of the case number) and then a CaseComment object
	 * will be created (by virtue of the comment) which will be linked to the new Case.<br>
	 * <br>
	 * Several public constants are also defined here.  While these note common column field names, this list is not
	 * complete and is always a work in progress.<br>
	 * @author trey.kirk
	 *
	 */
	public static enum ObjType {
		ACCOUNT,
		CASE,
		CONTACT,
		HISTORY,
		NULL
	}
	
	// General constants
	public static final String DATE_PATTERN_DAY = "MM/dd/yyyy";
	public static final String DATE_PATTERN_DETAILED = "MM/dd/yyyy hh:mm aa";
	
	// Account constants
	public static final String ACCOUNT_NAME = "Account Name";
	
	// Case constants
	public static final String CASE_AGE = "Age";
	public static final String CASE_AGE_HOURS = "Age (Hours)";
	public static final String CASE_CLOSED_DATE = "Closed Date";
	public static final String CASE_DATE_TIME_CLOSED = "Date/Time Closed";
	public static final String CASE_DATE_TIME_OPENED = "Date/Time Opened";
	public static final String CASE_DESCRIPTION = "Description";
	public static final String CASE_NUMBER = "Case Number";
	public static final String CASE_OPEN_DATE = "Opened Date";
	public static final String CASE_OWNER = "Case Owner";
	public static final String CASE_PRIORITY = "Priority";
	public static final String CASE_REASON = "Case Reason";
	public static final String CASE_STATUS = "Status";
	public static final String CASE_SUBJECT = "Subject";
	public static final String CASE_TYPE = "Type";
	public static final String CASE_DUPLICATE_CASE = "Duplicate Case";
	public static final String CASE_PUBLIC_CLOSE_COMMENTS = "Public Closure Comments";
	
	// Case Comment constants
	public static final String CASE_COMMENT_CREATE_DATE = "Case Comment Created Date";
	public static final String CASE_COMMENTS = "Case Comments";
	public static final String CASE_COMMNETS_PUBLIC = "Public Case Commented";
	
	// Case History constants
	public static final String HISTORY_FIELD_EVENT = "Field / Event";
	public static final String HISTORY_NEW_VALUE = "New Value";
	public static final String HISTORY_OLD_VALUE = "Old Value";
	
	// Email constants
	public static final String EMAIL_SUBJECT = "Email Subject";
	public static final String EMAIL_MESSAGE_DATE = "Email Message Date";
	
	// SailPoint Product constants
	public static final String SAILPOINT_PRODUCT = "Product";
	public static final String SAILPOINT_PRODUCT_NAME = "Product: Product Name";
	
	/**
	 * Depricated until I can figure out what column used this value as a name.
	 * @deprecated
	 */
	public static final String EDIT_DATE = "Edit Date";

	/**
	 * Given a {@link CSVSource} of {@link AbstractSFDCObject}, this utility method returns a JRDataSource
	 * @param csvSource {@link CSVSource} containing {@link AbstractSFDCObject} objects
	 * @return JRDataSource to be used in a JasperReports Fill Manager
	 * @throws IOException - Can be thrown at any time while the method reads the underlying csv file
	 * @throws CSVIllegalOperationException - Can be thrown at any time while the method parses the csv data
	 * @see BaseSFDCJRDataSource
	 * @see AbstractSFDCObject
	 */
	public static JRDataSource convertCsvData(CSVSource csvSource) throws IOException, CSVIllegalOperationException {
		//converts CSVSource into JRDataSource
		
		List<AbstractSFDCObject> allObjects = new ArrayList<AbstractSFDCObject>();
		
		Map<String, String> currentLine = csvSource.getNextLineAsMap();
		while (currentLine != null) {
			mergeSFDCObjectFromCsvData(currentLine, allObjects);
			currentLine = csvSource.getNextLineAsMap();
		}
		
		//_dataSource = new BaseSFDCJRDataSource(_allObjects);		
		
		JRDataSource dataSource = new BaseSFDCJRDataSource(allObjects);
		
		return dataSource;
	}
	

	private static CaseComment generateCaseComment(Map<String, String> data) {
		CaseComment comment = new CaseComment();
		comment.setComments(data.get(SFDCCSVUtil.CASE_COMMENTS));
		try {
			comment.setCreateDate(SFDCCSVUtil.getDate(data.get(SFDCCSVUtil.CASE_COMMENT_CREATE_DATE)));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (data.get(SFDCCSVUtil.CASE_COMMNETS_PUBLIC) != null) {
			boolean isPublic = Boolean.valueOf(data.get(SFDCCSVUtil.CASE_COMMNETS_PUBLIC));
			comment.setPublic(isPublic);
		}
		// more to do here.
		return comment;
	}
	
	/**
	 * Returns a {@link Date} object given a string adhering to the pattern {@link SFDCCSVUtil#DATE_PATTERN_DETAILED}.
	 * @param dateString String to parse
	 * @return Date object representing the date specified by dateString.  Null if the dateString is null.
	 * @throws ParseException
	 * @see SimpleDateFormat
	 */
	public static Date getDate(String dateString) throws ParseException {
		if (dateString != null) {
			return CSVUtil.getDate(dateString, DATE_PATTERN_DETAILED);
		} else {
			return null;
		}
	}

	private static void mergeCase (Map<String, String> data, Collection<AbstractSFDCObject> allObjects ) {
		String caseNumber = data.get(SFDCCSVUtil.CASE_NUMBER);
		if (caseNumber == null) {
			throw new RuntimeException("Cannot create case without case number!");
		}
		Case newCase = Case.findCase(allObjects, caseNumber);
		if (newCase == null) {
			newCase = new Case();
			allObjects.add(newCase);			
		}
		if (data.get(SFDCCSVUtil.ACCOUNT_NAME) != null) {
			newCase.setAccount(data.get(SFDCCSVUtil.ACCOUNT_NAME));
		}
		if (data.get(SFDCCSVUtil.CASE_AGE) != null) {
			newCase.setAge(data.get(SFDCCSVUtil.CASE_AGE));
		}
		newCase.setCaseNumber(data.get(SFDCCSVUtil.CASE_NUMBER));
		if (data.get(SFDCCSVUtil.CASE_REASON) != null) {
			newCase.setCaseReason(data.get(SFDCCSVUtil.CASE_REASON));
		}
		try {
			if (data.get(SFDCCSVUtil.CASE_DATE_TIME_OPENED) != null) {
				newCase.setDateOpened(SFDCCSVUtil.getDate(data.get(SFDCCSVUtil.CASE_DATE_TIME_OPENED)));
			}
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			if (data.get(SFDCCSVUtil.CASE_DATE_TIME_CLOSED) != null) {
				newCase.setDateClosed(SFDCCSVUtil.getDate(data.get(SFDCCSVUtil.CASE_DATE_TIME_CLOSED)));
			}
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (data.get(SFDCCSVUtil.CASE_PRIORITY) != null) {
			newCase.setPriority(data.get(SFDCCSVUtil.CASE_PRIORITY));
		}
		if (data.get(SFDCCSVUtil.CASE_STATUS) != null) {
			newCase.setStatus(data.get(SFDCCSVUtil.CASE_STATUS));
		}
		if (data.get(SFDCCSVUtil.CASE_SUBJECT) != null) {
			newCase.setSubject(data.get(SFDCCSVUtil.CASE_SUBJECT));
		}
		if (data.get(SFDCCSVUtil.CASE_TYPE) != null) {
			newCase.setType(data.get(SFDCCSVUtil.CASE_TYPE));
		}
		
		if (data.get(SFDCCSVUtil.CASE_DESCRIPTION) != null) {
			newCase.setDescription(data.get(SFDCCSVUtil.CASE_DESCRIPTION));
		}

		CaseComment caseComment = generateCaseComment(data);
		newCase.addComment(caseComment);
	}

	/**
	 * Given csv data in the form of a map, this method creates the POJO defined by that data and merges it with the
	 * provided Collection.  This method will identify the object to create and first seek out an existing object in the
	 * Collection that may have already been created.  If it finds that object, the new data will be merged onto the
	 * existing object.  If not, a new object is created and added to the Collection.
	 * @param csvData Map of csv data.
	 * @param allObjects Collection of AbstractSFDCObject objects that the new object will be added to.
	 * @return
	 */
	public static AbstractSFDCObject mergeSFDCObjectFromCsvData (Map<String, String> csvData, Collection<AbstractSFDCObject> allObjects) {
		
		ObjType type = ObjType.NULL;
		AbstractSFDCObject sfdcObj = null;
		String caseNumberField = SFDCCSVUtil.CASE_NUMBER;
		Set<String> keys = csvData.keySet();
		if (keys.contains(caseNumberField)) {
			type = ObjType.CASE;
		}
		
		boolean noOp = false;
		
		switch(type) {
			case CASE: mergeCase(csvData, allObjects);	
			break;
			// someday, there shall be more
			default: noOp = true;
		}

		
		return sfdcObj;
	}

	private SFDCCSVUtil() {
		//The soup, she is not for you
	}

}
