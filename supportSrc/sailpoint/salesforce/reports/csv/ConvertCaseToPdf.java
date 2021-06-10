package sailpoint.salesforce.reports.csv;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;

import com.biliruben.util.csv.CSVIllegalOperationException;
import com.biliruben.util.csv.CSVSource;
import com.biliruben.util.csv.CSVSourceImpl;
import com.biliruben.util.csv.CSVUtil;
import com.biliruben.util.csv.CSVSource.CSVType;
import com.biliruben.util.GetOpts;
import com.biliruben.util.OptionLegend;
import com.biliruben.util.OptionParseException;

public class ConvertCaseToPdf {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ConvertCaseToPdf converter = new ConvertCaseToPdf();
		GetOpts opts = null;
		try {
			opts = converter.init();
			opts.setOpts(args);
		} catch (OptionParseException e) {
			// TODO Auto-generated catch block
			System.err.print(opts.genUsage(e));
			System.exit(1);
		}
		
		try {
			converter.convert();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(2);
		} catch (CSVIllegalOperationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(3);
		} catch (JRException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(4);
		}
		

	}
	
	public ConvertCaseToPdf() {
		
	}
	
	public static final String ARG_CSV_FILE_NAME = "csvFile";
	public static final String ARG_PDF_FILE_NAME = "pdfFile";
	public static final String ARG_CASE_DETAIL_JRXML = "caseJrxml";
	public static final String ARG_COMMENT_DETAIL_JRXML = "commentJrxml";
	
	public static final String DEFAULT_CASE_DETAIL_JRXML = "jrxml/CSVToPDF.jrxml";
	public static final String DEFAULT_CASE_COMMENT_JRXML = "jrxml/CaseCommentSubReport.jrxml";
	
	private GetOpts _opts;
	private CSVSource _csv;
	private String _caseDetailJrxml = DEFAULT_CASE_DETAIL_JRXML;
	private JasperReport _caseCommentSubReport;
	private JasperReport _caseDetailReport;
	private String _caseCommentJrxml = DEFAULT_CASE_COMMENT_JRXML;
	
	public GetOpts init() throws OptionParseException {
		this._opts = new GetOpts(this.getClass());
		OptionLegend legend = new OptionLegend(ARG_CSV_FILE_NAME);
		legend.setRequired(true);
		legend.setDescription("Source CSV file");
		_opts.addLegend(legend);
		
		legend = new OptionLegend(ARG_PDF_FILE_NAME);
		legend.setRequired(true);
		legend.setDescription("Target PDF file (any existing file will be overwritten!)");
		_opts.addLegend(legend);
		
		legend = new OptionLegend(ARG_CASE_DETAIL_JRXML);
		legend.setRequired(false);
		legend.setDescription("JRXML file to use to redner the Case Detail report.  Default value: " + DEFAULT_CASE_DETAIL_JRXML);
		_opts.addLegend(legend);
		
		legend = new OptionLegend(ARG_COMMENT_DETAIL_JRXML);
		legend.setRequired(false);
		legend.setDescription("JRXML file used to render Case Comments in the Case Detail report.  Default value: " + DEFAULT_CASE_COMMENT_JRXML);
		_opts.addLegend(legend);
		
		return _opts;
	}
	
	private JasperReport getReport (String reportName) throws JRException {
		JasperReport report = JasperCompileManager.compileReport(reportName);
		return report;
		
	}
	
	public void convert() throws IOException, CSVIllegalOperationException, JRException {
		_csv = getCsv();
		if (_opts.getStr(ARG_CASE_DETAIL_JRXML) != null) {
			_caseDetailJrxml = _opts.getStr(ARG_CASE_DETAIL_JRXML);
		}
		
		if (_opts.getStr(ARG_COMMENT_DETAIL_JRXML) != null) {
			_caseCommentJrxml = _opts.getStr(ARG_COMMENT_DETAIL_JRXML);
		}
		
		//_converter = new CSVToPDF(_csv, _opts.getStr(ARG_PDF_FILE_NAME));
		//_converter = new CSVToPDF(_csv, _opts.getStr(ARG_PDF_FILE_NAME), _caseJrxml);
		
		//_converter.generatePdf();
		
		Map reportParams = new HashMap();
		reportParams.put("ReportTitle", "Salesforce Data Report");
		reportParams.put("CaseCommentSubReport", getReport(_caseCommentJrxml));
		
		//TODO: Create JasperReport
		JasperReport detailReport = getReport(_caseDetailJrxml);
		
		CSVUtil.generatePdf(_csv, detailReport, reportParams, _opts.getStr(ARG_PDF_FILE_NAME));
	}
	
	private CSVSource getCsv() throws FileNotFoundException {
		String csvFile = _opts.getStr(ARG_CSV_FILE_NAME);
		CSVSource dataSource = new CSVSourceImpl(new File(csvFile), CSVType.WithHeader);
		return dataSource;
	}
	

}
