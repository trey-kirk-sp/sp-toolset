package sailpoint.salesforce.reports.csv;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sailpoint.salesforce.reports.object.Case;
import sailpoint.salesforce.reports.object.StatusHistory;

import com.biliruben.util.ArrayUtil;
import com.biliruben.util.GetOpts;
import com.biliruben.util.OptionLegend;
import com.biliruben.util.OptionParseException;


/**
 * So, provided a source CSV, read it in and compile the data.
 * Source CSV - output from exported History report from SalesForce
 * 	- Do not assume the order of columns or the number.  Use column headers to determine what data is
 * 		available.  Determine base number of columns and throw exception when not found.
 * Output - provide data aggregated on a per case basis.  Include total time in each status.  Try to include
 * 		biz hours.
 * 
 * @author trey.kirk
 *
 */
public class HistoryCSVAggregator {


	private Map<String, Case> _cases = new HashMap<String, Case>();
	private static final String EVENT_STATUS = "Status";
	private static final String STATUS_NEW = "New";
	private static final String STATUS_CLOSED = "Closed";
	private SimpleDateFormat _dateFormat = new SimpleDateFormat();
	private String[] _statusFieldNames;
	
	private static final String OPT_IN_DETAIL_FILE = "detailFile";
	private static final String OPT_IN_HISTORY_FILE = "historyFile";
	private static final String OPT_OUT_FILE = "outFile";
	private static final String OPT_TIME_FACTOR = "timeFactor";

	static void init(GetOpts opts, String[] args) {
		
		OptionLegend legend = new OptionLegend(OPT_IN_DETAIL_FILE, "CSV file containing case detail data");
		legend.setRequired(true);
		opts.addLegend(legend);
		
		legend = new OptionLegend(OPT_IN_HISTORY_FILE, "CSV file containing history data");
		legend.setRequired(true);
		opts.addLegend(legend);
		
		legend = new OptionLegend(OPT_OUT_FILE, "Output CSV file");
		legend.setRequired(true);
		opts.addLegend(legend);
		
		legend = new OptionLegend(OPT_TIME_FACTOR, "Units that summary time data will be calculated in");
		String[] values = {
				"milliseconds",
				"seconds",
				"minutes",
				"hours",
				"days"
		};
		legend.setAllowedValues(values);
		opts.addLegend(legend);
		
		try {
			opts.setOpts(args);
		} catch (OptionParseException e) {
			// TODO Auto-generated catch block
			System.out.print(opts.genUsage());
			System.exit(1);
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		GetOpts opts = new GetOpts(HistoryCSVAggregator.class);
		init(opts, args);

		// TODO Auto-generated method stub
		HistoryCSVAggregator aggregator = new HistoryCSVAggregator();
		try {
			CSVDataSourceSpecialized caseData = new CSVDataSourceSpecialized(new File(opts.getStr(OPT_IN_DETAIL_FILE)), ReportType.CaseDetail);
			CSVDataSourceSpecialized historyData = new CSVDataSourceSpecialized(new File(OPT_IN_HISTORY_FILE), ReportType.StatusHistory);
			OutputStream out = new FileOutputStream(new File(OPT_OUT_FILE));

			aggregator.aggregate(historyData);
			aggregator.aggregate(caseData);
			aggregator.fill(out);


		} catch (CSVDataFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public HistoryCSVAggregator () {
		/*
		 * See: http://java.sun.com/j2se/1.4.2/docs/api/java/text/SimpleDateFormat.html
		 * for format reference.
		 */
		_dateFormat.applyPattern("MM/dd/yyyy hh:mm aa");

	}

	public StatusHistory buildEnteredStatusHistory (Case theCase, Map<String, String> dataMap) {
		StatusHistory sh = new StatusHistory(theCase, dataMap.get(CSVDataSourceSpecialized.COL_NEW_VALUE));
		try {
			Date enteredDate = _dateFormat.parse(dataMap.get(CSVDataSourceSpecialized.COL_EDIT_DATE));
			sh.addStatusEntered(enteredDate);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return sh;
	}

	public StatusHistory buildExitedStatusHistory (Case theCase, Map<String, String> dataMap) {
		StatusHistory sh = new StatusHistory(theCase, dataMap.get(CSVDataSourceSpecialized.COL_OLD_VALUE));
		try {
			Date exitedDate = _dateFormat.parse(dataMap.get(CSVDataSourceSpecialized.COL_EDIT_DATE));
			sh.addStatusExited(exitedDate);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return sh;
	}

	public Map<String, Case> getCases() {
		return _cases;
	}

	public SimpleDateFormat getDateFormat() {
		return _dateFormat;
	}
	
	/*
	 * use this method to insert any data corrections that will have to be made
	 */
	private void correctData(Map<String, String> data) {
		String escalatedToEng = "Escalated to Engineering";
		String escalated = "Escalated";
		String newStatusValue = data.get(CSVDataSourceSpecialized.COL_NEW_VALUE);
		if (newStatusValue != null && newStatusValue.equals(escalated) ) {
			// Escalated was changed to Escalated to Engineering
			data.put(CSVDataSourceSpecialized.COL_NEW_VALUE, escalatedToEng);
		}
		
		String oldStatusValue = data.get(CSVDataSourceSpecialized.COL_OLD_VALUE);
		if (oldStatusValue != null && oldStatusValue.equals(escalated) ) {
			// Escalated was changed to Escalated to Engineering
			data.put(CSVDataSourceSpecialized.COL_OLD_VALUE, escalatedToEng);
		}
		
		
	}

	public void aggregate(CSVDataSourceSpecialized data) throws IOException, CSVDataFormatException {
		// Return data in the form of:
		// Case number - time per each status

		while (data.getNextLine() != null) {
			Map<String, String> line = data.getCurrentLineAsMap();
			correctData(line);
			String caseNumber = line.get(CSVDataSourceSpecialized.COL_CASE_NUMBER);
			Case newCase = this.getCases().get(caseNumber);
			if ( newCase == null) {
				newCase = new Case(caseNumber);
				try {
					if (line.get(CSVDataSourceSpecialized.COL_DATE_TIME_OPENED) != null){
						newCase.setDateOpened(this.getDateFormat().parse(line.get(CSVDataSourceSpecialized.COL_DATE_TIME_OPENED)));
					}
					if (line.get(CSVDataSourceSpecialized.COL_DATE_TIME_CLOSED) != null && !line.get(CSVDataSourceSpecialized.COL_DATE_TIME_CLOSED).equals("")) {
						// occurs when a case is still open
						newCase.setDateClosed(this.getDateFormat().parse(line.get(CSVDataSourceSpecialized.COL_DATE_TIME_CLOSED)));
					}
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				this.getCases().put(caseNumber, newCase);
			}

			if (newCase.getCaseReason() == null) {
				newCase.setCaseReason(line.get(CSVDataSourceSpecialized.COL_CASE_REASON));
			}

			if (newCase.getAccount() == null) {
				newCase.setAccount(line.get(CSVDataSourceSpecialized.COL_ACCOUNT_NAME));
			}

			if (newCase.getAge() == Case.CASE_AGE_UNSET) {
				if (line.get(CSVDataSourceSpecialized.COL_AGE_HOURS) != null) {
					newCase.setAge(line.get(CSVDataSourceSpecialized.COL_AGE_HOURS));
				}
			}

			if (newCase.getCaseOwner() == null) {
				newCase.setCaseOwner(line.get(CSVDataSourceSpecialized.COL_CASE_OWNER));
			}

			if (newCase.getStatus() == null) {
				newCase.setStatus(line.get(CSVDataSourceSpecialized.COL_STATUS));
			}

			if (newCase.getSubject() == null) {
				newCase.setSubject(line.get(CSVDataSourceSpecialized.COL_SUBJECT));
			}

			if (newCase.getType() == null) {
				newCase.setType(line.get(CSVDataSourceSpecialized.COL_TYPE));
			}

			if (newCase.getPriority() == null) {
				newCase.setPriority(line.get(CSVDataSourceSpecialized.COL_PRIORITY));
			}

			String historyEvent = line.get(CSVDataSourceSpecialized.COL_FIELD_EVENT);
			if (historyEvent != null && historyEvent.equals(EVENT_STATUS)) {
				this.addStatusEvent(newCase, line);
			}
		}		
	}


	public void fill (OutputStream out) throws IOException {
		byte[] comma = ",".getBytes();
		byte[] newLine = "\n".getBytes();
		String dateOpened = CSVDataSourceSpecialized.COL_DATE_TIME_OPENED;
		String dateClosed = CSVDataSourceSpecialized.COL_DATE_TIME_CLOSED;
		String caseNumber = CSVDataSourceSpecialized.COL_CASE_NUMBER;
		String caseReason = CSVDataSourceSpecialized.COL_CASE_REASON;
		String account = CSVDataSourceSpecialized.COL_ACCOUNT_NAME;
		String age = CSVDataSourceSpecialized.COL_AGE_HOURS;
		String caseOwner = CSVDataSourceSpecialized.COL_CASE_OWNER;
		String status = CSVDataSourceSpecialized.COL_STATUS;
		String subject = CSVDataSourceSpecialized.COL_SUBJECT;
		String type = CSVDataSourceSpecialized.COL_TYPE;
		String priority = CSVDataSourceSpecialized.COL_PRIORITY;

		// spew the headers
		String[] statusHeaders = getAllFieldNames();
		String[] baseHeaders = {
				caseNumber,
				priority,
				caseOwner,
				account,
				status,
				subject,
				type,
				caseReason,
				dateOpened,
				dateClosed
		};
		String[] ageArry = {age};

		String[] fullHeaders = ArrayUtil.join(baseHeaders, statusHeaders, ageArry);
		writeDataLine(out, fullHeaders, comma, newLine);

		List<String> headerList = Arrays.asList(fullHeaders);
		for (Case theCase : getCases().values()) {
			String[] outLine = new String[fullHeaders.length];

			//case Number
			outLine[headerList.indexOf(caseNumber)] = theCase.getCaseNumber();

			//Date Opened
			outLine[headerList.indexOf(dateOpened)] = theCase.getDateOpened().toString();

			//priority
			outLine[headerList.indexOf(priority)] = theCase.getPriority();

			//owner
			outLine[headerList.indexOf(caseOwner)] = theCase.getCaseOwner();

			//status
			outLine[headerList.indexOf(status)] = theCase.getStatus();

			//subject
			outLine[headerList.indexOf(subject)] = theCase.getSubject();

			//type
			outLine[headerList.indexOf(type)] = theCase.getType();

			//age
			outLine[headerList.indexOf(age)] = String.valueOf(theCase.getAge());


			//Date Closed
			//Could still be open
			if (theCase.getDateClosed() != null) {
				outLine[headerList.indexOf(dateClosed)] = theCase.getDateClosed().toString();
			} else {
				outLine[headerList.indexOf(dateClosed)] = "";
			}

			//Case Reason
			outLine[headerList.indexOf(caseReason)] = theCase.getCaseReason();

			//Account
			outLine[headerList.indexOf(account)] = theCase.getAccount();

			Map<String, StatusHistory> histories = theCase.getStatusHistory();
			//StatusHistories
			for (String historyStatus : statusHeaders) {
				float timeInStatus;
				if (histories.get(historyStatus) != null) {
					timeInStatus = histories.get(historyStatus).timeInStatus();
				} else {
					timeInStatus = 0;
				}
				outLine[headerList.indexOf(historyStatus)] = String.valueOf(timeInStatus); 
			}

			//Spew
			writeDataLine(out, outLine, comma, newLine);
		}
	}

	private void writeDataLine (OutputStream out, String[] data, byte[] sep, byte[] newline) throws IOException {
		byte[] datum;
		for (int i = 0; i < data.length; i++) {
			String dataLine = data[i];
			if (dataLine != null) {
				if (dataLine.contains("\n")) {
					// newline, strip it out
					dataLine = dataLine.replaceAll("\n", "");
				}
				// now double quote it
				dataLine = "\"" + dataLine + "\"";
				datum = dataLine.getBytes();
			} else {
				datum = "".getBytes();
			}
			out.write(datum);
			if (i != data.length - 1) {
				out.write(sep);
			} else {
				out.write(newline);
			}
		}		
	}

	public String[] getAllFieldNames() {
		return _statusFieldNames;
	}

	public void addStatusEvent (Case theCase, Map<String, String> line) {
		StatusHistory enteredStatus = this.buildEnteredStatusHistory(theCase, line);
		StatusHistory exitedStatus = this.buildExitedStatusHistory(theCase, line);
		/*if (enteredStatus.getStatus().equals(STATUS_CLOSED)) {
			// Entered a closed status, there won't be an exit date.  Set it for today.
			enteredStatus.addStatusExited(new Date());
		} else*/ if (exitedStatus.getStatus().equals(STATUS_NEW)) {
			try {
				exitedStatus.addStatusEntered(this.getDateFormat().parse(line.get(CSVDataSourceSpecialized.COL_DATE_TIME_OPENED)));
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		addStatusFieldName(enteredStatus);
		addStatusFieldName(exitedStatus);

		Map<String, StatusHistory> allHistory = theCase.getStatusHistory();
		if (allHistory.get(enteredStatus.getStatus()) == null) {
			theCase.addStatusHistory(enteredStatus);
		} else {
			// we already have this status
			allHistory.get(enteredStatus.getStatus()).addStatusEntered(enteredStatus.getStatusEntered()[0]);
		}

		if (allHistory.get(exitedStatus.getStatus()) == null) {
			theCase.addStatusHistory(exitedStatus);
		} else {
			// we already have this status
			allHistory.get(exitedStatus.getStatus()).addStatusExited(exitedStatus.getStatusExited()[0]);
		}

	}

	private void addStatusFieldName (StatusHistory sh) {
		String[] tmpArry;
		if (_statusFieldNames == null) {
			tmpArry = new String[1];
			tmpArry[0] = sh.getStatus();
		} else {
			Arrays.sort(_statusFieldNames);
			if (Arrays.binarySearch(_statusFieldNames, sh.getStatus()) < 0) {
				tmpArry = new String[_statusFieldNames.length + 1];
				for (int i = 0; i < _statusFieldNames.length; i++) {
					tmpArry[i] = _statusFieldNames[i];
				}
				tmpArry[tmpArry.length - 1] = sh.getStatus();
			}  else {
				tmpArry = _statusFieldNames;
			}
		}
		_statusFieldNames = tmpArry;
	}
}