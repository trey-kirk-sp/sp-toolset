/**
 * 
 */
package sailpoint.custom.support;

import sailpoint.api.SailPointContext;
import sailpoint.object.TaskDefinition;
import sailpoint.task.WorkItemExpirationScanner;
import sailpoint.tools.GeneralException;

/**
 * @author trey.kirk
 *
 */
public class CheckExpWorkItem {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TaskExecutorWrapper wrapper = new TaskExecutorWrapper();
		SailPointContext context = wrapper.initialize();
		
		try {
			//TaskDefinition housekeeper = context.getObject(TaskDefinition.class, "Perform Maintenance");
			TaskDefinition workItemScanner = context.getObject(TaskDefinition.class, "Check Expired Work Items");
			wrapper.execute(context, new WorkItemExpirationScanner().getClass(), workItemScanner.getArguments());
			System.out.println("Suck it");
		} catch (GeneralException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
