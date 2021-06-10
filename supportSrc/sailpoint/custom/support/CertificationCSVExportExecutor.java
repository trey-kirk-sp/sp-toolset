package sailpoint.custom.support;

import sailpoint.api.SailPointContext;
import sailpoint.object.Attributes;
import sailpoint.object.Certification;
import sailpoint.object.TaskResult;
import sailpoint.object.TaskSchedule;
import sailpoint.task.AbstractTaskExecutor;

public class CertificationCSVExportExecutor extends AbstractTaskExecutor {

	public void execute(SailPointContext context, TaskSchedule schedule,
			TaskResult result, Attributes<String, Object> args)
			throws Exception {
		// TODO Auto-generated method stub
		String certificationName = args.getString("certification");
		Certification cert = context.getObject(Certification.class, certificationName);
		
		
		
		
		
		

	}

	public boolean terminate() {
		// TODO Auto-generated method stub
		return false;
	}

}
