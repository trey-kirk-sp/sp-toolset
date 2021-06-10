/**
 * 
 */
package sailpoint.custom.support;

import sailpoint.api.SailPointContext;
import sailpoint.object.Attributes;
import sailpoint.object.TaskResult;
import sailpoint.object.TaskSchedule;
import sailpoint.task.AbstractTaskExecutor;

/**
 * @author trey.kirk
 *
 */
public class GCExecutor extends AbstractTaskExecutor {

	/* (non-Javadoc)
	 * @see sailpoint.object.TaskExecutor#execute(sailpoint.api.SailPointContext, sailpoint.object.TaskSchedule, sailpoint.object.TaskResult, sailpoint.object.Attributes)
	 */
	public void execute(SailPointContext context, TaskSchedule schedule,
			TaskResult result, Attributes<String, Object> args)
			throws Exception {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see sailpoint.object.TaskExecutor#terminate()
	 */
	public boolean terminate() {
		// TODO Auto-generated method stub
		return false;
	}

}
