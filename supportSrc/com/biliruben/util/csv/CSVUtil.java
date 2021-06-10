package com.biliruben.util.csv;

import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import sailpoint.salesforce.reports.csv.SFDCCSVUtil;

public class CSVUtil {

	/**
	 * Returns a {@link Date} object given a string adhering to the pattern datePattern.
	 * @param dateString String to parse
	 * @param datePattern Pattern to apply when parsing the date.  
	 * @return Date object representing the date specified by dateString.  Null if the dateString is null.
	 * @throws ParseException
	 * @see SimpleDateFormat
	 */
	public static Date getDate(String dateString, String datePattern) throws ParseException {
		SimpleDateFormat dateFormat = new SimpleDateFormat();
		dateFormat.applyPattern(datePattern);
		Date theDate = dateFormat.parse(dateString);
		return theDate;
	}

	/**
	 * This takes a proided {@link CSVSource} and converts it into a PDF file.  This method is actually more universally
	 * reusable outside of Salesforce related data.  It should be moved to a class under com.biliruben.util.csv
	 * @param csvSource CSVSource that contains the csv data.
	 * @param detailReport JasperReport used to fill.
	 * @param reportParams Report parameters used when filling the report.
	 * @param outFile Target pdf file.
	 * @throws IOException
	 * @throws CSVIllegalOperationException
	 * @throws JRException
	 */
	public static void generatePdf(CSVSource csvSource, JasperReport detailReport, Map reportParams, String outFile) 
		throws IOException, CSVIllegalOperationException, JRException {
		JRDataSource source = SFDCCSVUtil.convertCsvData(csvSource);
	
		// Step 3
		if (detailReport == null) {
			throw new JRException("No JasperReport provided!");
		}
		
		if (reportParams == null) {
			reportParams = new HashMap();
		}
		
		JasperPrint print = JasperFillManager.fillReport(detailReport, reportParams, source);
	
		// Step 5
		JasperExportManager.exportReportToPdfFile(print, outFile);		
	}
	
	public static void exportToCsv(CSVRecord csvRecord, OutputStream out) throws IOException {
		for (String record : csvRecord) {
			out.write(record.getBytes());
		}
		out.flush();
	}

}
