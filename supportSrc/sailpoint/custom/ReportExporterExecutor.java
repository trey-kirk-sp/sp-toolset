package sailpoint.custom;

import java.util.Date;

import sailpoint.api.SailPointContext;
import sailpoint.object.Attributes;
import sailpoint.object.Configuration;
import sailpoint.object.EmailOptions;
import sailpoint.object.EmailTemplate;
import sailpoint.object.JasperResult;
import sailpoint.object.TaskDefinition;
import sailpoint.object.TaskResult;
import sailpoint.object.TaskSchedule;
import sailpoint.reporting.JasperExecutor;
import sailpoint.reporting.JasperRenderer;
import sailpoint.task.AbstractTaskExecutor;
import sailpoint.tools.GeneralException;
import sailpoint.tools.LocalizedMessage;
import sailpoint.tools.Util;
import sailpoint.tools.LocalizedMessage.Type;
import sailpoint.tools.xml.ConfigurationException;


/**
 * @author trey.kirk
 * Custom task to wrap a JasperExecutor to allow exporting a report directly to csv.
 * Updated for 3.0
 * 
 * TODO: should only be doing this for Grid reports.  Should outta mebe check for that and bail
 * when it's not.
 *
 */
public class ReportExporterExecutor extends AbstractTaskExecutor {

	private static final String RELATIVE_BASE_PATH = "exports/reports";
	private static final String FILE_FORMAT = "csv";

	public static final String DEFAULT_TEMPLATE_ID = "Report Exporter Template";
	public static final String OP_BASE_URL = "baseUrl";
	public static final String OP_REPORT = "report";
	public static final String OP_EMAIL_TEMPLATE_ID = "emailTemplateId";
	public static final String OP_EMAIL_TO = "emailRecipients";
	public static final String PROP_RELATIVE_BASE_PATH = "reportExporter.relativeBasePath";
	public static final String RET_OUT_FILE = "outfile";
	public static final String RET_URL = "url";
	

	/* (non-Javadoc)
	 * @see sailpoint.object.TaskExecutor#execute(sailpoint.api.SailPointContext, sailpoint.object.TaskSchedule, sailpoint.object.TaskResult, sailpoint.object.Attributes)
	 */
	public void execute(SailPointContext context, TaskSchedule schedule,
			TaskResult result, Attributes<String, Object> args)
	throws Exception {

		TaskDefinition def = 
			context.getObject(TaskDefinition.class, (String)args.get(OP_REPORT));
		JasperExecutor executor = (JasperExecutor)def.getTaskExecutor();
		Attributes<String, Object> inputs = new Attributes<String, Object>();
        Attributes<String, Object> reportArgs = def.getArguments();
        if ( reportArgs != null ) {
            inputs.putAll(reportArgs);
        }


		// inject how we ar rendering so the executor can be aware 
		inputs.put(JasperExecutor.OP_RENDER_TYPE, FILE_FORMAT);

		JasperResult jResult = executor.buildResult(def, inputs, context);

		// Don't change the name until the report is finished.  We don't want simultaneous executions of buildResult()
		result.setName(def.getName() + "_" + new Date());
		context.saveObject(result);

		String fileName = result.getName().replaceAll("[ ,.:/\\*$]", "_") + FILE_FORMAT;

		String sphome = "";

		try{
			sphome = Util.getApplicationHome();
		} catch (GeneralException e) {
			//result.addError(e);
			result.addMessage(new LocalizedMessage(Type.Error, e));
		}

		StringBuffer path = new StringBuffer(128);

		// Allows for a different path to be specified from ciq.prop, dunno if this actually works
		// Don't totally care either
		String relBasePath = System.getProperty(PROP_RELATIVE_BASE_PATH);
		if (relBasePath == null) {
			path.append(sphome + "/" + RELATIVE_BASE_PATH);
		} else {
			path.append(sphome + "/" + relBasePath);
		}

		fileName = path.toString() + "/" + fileName;
		result.setAttribute(RET_OUT_FILE, fileName);

		Configuration jasperConf = context.getObject(Configuration.class,
				JasperExecutor.JASPER_CONFIGURATION);
		JasperRenderer renderer = new JasperRenderer(jResult, jasperConf);

		renderer.renderToCSVFile(fileName);

		// Saving the JasperResult into the repo is a headache.  Commenting
		// out for now and will consider alternatives later.  Right now,
		// just gimmie my csv.

		String baseUrl = (String)args.get(OP_BASE_URL);
		if (baseUrl == null) {
			result.addMessage(new LocalizedMessage(Type.Warn, "URL not available - baseUrl provided was null."));
		}

		String url = baseUrl + "/" + fileName.substring(sphome.length());
		result.setAttribute(RET_URL, url);
		emailNotification(url, args, context);

		executor.setInputs(new Attributes<String, Object>());
		
		context.saveObject(result);
		context.commitTransaction();
	}

	/* (non-Javadoc)
	 * @see sailpoint.object.TaskExecutor#terminate()
	 * 
	 */
	public boolean terminate() {
		// TODO Auto-generated method stub
		return true;
	}

	/**
	 * Take the result, render it to pdf and send it as an attachment.
	 */
	public void emailNotification(String url, Attributes inputs,
			SailPointContext ctx )
	throws GeneralException {

		String to = inputs.getString(OP_EMAIL_TO);

		if ( to == null ) 
			return;

		String id = inputs.getString(OP_EMAIL_TEMPLATE_ID);
		if ( id == null ) id = DEFAULT_TEMPLATE_ID;

		EmailTemplate et = ctx.getObject(EmailTemplate.class, id);
		if ( et == null ) 
			throw new GeneralException("Cannot find EmailTemplate: " + id);

		EmailOptions options = new EmailOptions(to, null);
		options.setVariable(RET_URL, url);

		ctx.sendEmailNotification(et, options);
	}



}
