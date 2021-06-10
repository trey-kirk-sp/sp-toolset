/**
 * 
 */
package sailpoint.custom.support;

import java.util.ArrayList;
import java.util.Date;

import sailpoint.api.DatabaseVersionException;
import sailpoint.api.SailPointContext;
import sailpoint.api.SailPointFactory;
import sailpoint.object.Attributes;
import sailpoint.object.TaskDefinition;
import sailpoint.object.TaskItemDefinition;
import sailpoint.object.TaskResult;
import sailpoint.object.TaskDefinition.ResultAction;
import sailpoint.spring.SpringStarter;
import sailpoint.task.AbstractTaskExecutor;

/**
 * @author trey.kirk
 * A convinience class that allows me to run a class stand-alone without having
 * to setup Tomcat or a console.  Bascially a re-usable set of code that will
 * get CIQ / IIQ running and get me a context to pass to the task.
 */
public class TaskExecutorWrapper {
	SpringStarter ss;

	public SailPointContext initialize() {
		SailPointContext ctx = null;
		String dflt = "iiqBeans";
		ss = new SpringStarter(dflt, null);

		String configFile = ss.getConfigFile();
		if (!configFile.startsWith(dflt))
			System.out.println("Reading spring config from: " + configFile);

		try {
			// suppress the background schedulers
			ss.setSuppressTaskScheduler(true);
			ss.setSuppressRequestScheduler(true);
			ss.start();

			ctx = SailPointFactory.createContext();

		}
		catch (DatabaseVersionException dve) {
			// format this more better  
			System.out.println(dve.getMessage());
		}
		catch (Throwable t) {
			System.out.println(t);
		}

		return ctx;


	}
	/**
	 * @param args
	 */
	public void execute(SailPointContext ctx, Class taskExecutor, Attributes<String, Object> attr) {
		// TODO Auto-generated method stub
		// First argument specifies the name of the Spring config file

		// Set your task here
		try {
			AbstractTaskExecutor task = (AbstractTaskExecutor)taskExecutor.newInstance();
			String resultName = "Task Executor Wrapper";
			TaskResult result;
			TaskDefinition definition = ctx.getObject(TaskDefinition.class, resultName);
			if (definition == null) {
				definition = new TaskDefinition();
				ctx.saveObject(definition);
			}
			definition.setExecutor(taskExecutor);
			definition.setName(resultName);
			definition.setResultAction(ResultAction.RenameNew);
			result = ctx.getObject(TaskResult.class, resultName);
			if (result == null) {
				result = new TaskResult();
			} else {
				// clear the current set of results
				result.setAttributes(new Attributes());
				result.clear();
			}

			result.setName(resultName);
			result.setType(TaskItemDefinition.Type.Generic);
			result.setLauncher("Admin");
			result.setLaunched(new Date());

			result.setDefinition(definition);

			task.execute(ctx, null, result, attr);
			result.setCompleted(new Date());
			System.out.println(result.toXml());
			ctx.saveObject(definition);
			ctx.saveObject(result);
			ctx.commitTransaction();

		} catch (Throwable t) {
			throw new RuntimeException (t);
		} finally {
			ss.close();
		}
	}
}