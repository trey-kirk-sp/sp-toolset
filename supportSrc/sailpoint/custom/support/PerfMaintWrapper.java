/**
 * 
 */
package sailpoint.custom.support;

import sailpoint.api.SailPointContext;
import sailpoint.object.TaskDefinition;
import sailpoint.object.TaskExecutor;
import sailpoint.task.Housekeeper;
import sailpoint.tools.GeneralException;

/**
 * @author trey.kirk
 *
 */
public class PerfMaintWrapper {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		TaskExecutorWrapper wrapper = new TaskExecutorWrapper();
		SailPointContext context = wrapper.initialize();
		
		try {
			TaskDefinition housekeeper = context.getObject(TaskDefinition.class, "Perform Maintenance");
			wrapper.execute(context, new Housekeeper().getClass(), housekeeper.getArguments());
			System.out.println("Suck it");
		} catch (GeneralException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

	}

}
