/**
 * 
 */
package sailpoint.custom.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import sailpoint.api.SailPointContext;
import sailpoint.api.TaskManager;
import sailpoint.object.Attributes;
import sailpoint.object.TaskDefinition;
import sailpoint.object.TaskResult;
import sailpoint.object.TaskSchedule;
import sailpoint.task.AbstractTaskExecutor;
import sailpoint.tools.GeneralException;
import sailpoint.tools.LocalizedMessage;
import sailpoint.tools.LocalizedMessage.Type;

/**
 * The Batch Task Executor is designed to take in a list of tasks and execute them in parrallel or serially.  It also supports
 * running other BatchTaskExecutor instances to support complex batching.  I.E. running several primary tasks in serial and then
 * running all dependant tasks in parrallel immidiately following.
 * 
 * v1.0 - Support running multiple tasks in parrallel or serially
 * v1.1 - Support running multiple BatchTaskExecutors serially to allow running several task in groups of
 * 			serial or parrallel
 * @author trey.kirk
 *
 */
public class BatchTaskExecutor extends AbstractTaskExecutor {

	private Log _log;
	private Boolean _terminate;
	private Boolean _noFail;
	private static final int MAX_STEPS = 100;
	/**
	 * 
	 */
	public BatchTaskExecutor() {
		_log = LogFactory.getLog("sailpoint.custom.support.BatchTaskExecutor");
		_terminate = false;
		_noFail = false;
	}


	/**
	 * Recursing method that counts the number of steps in the stack and bails when the limit is exhausted.  Basically a
	 * rudimentary StackOverflow check.
	 * @param batchTasks
	 * @param result
	 * @param ctx
	 * @param maxSteps
	 */
	private void checkStack (Stack<TaskDefinition> batchTasks, TaskResult result, SailPointContext ctx, int maxSteps) {

		if (maxSteps == 0) {
			result.addMessage(new LocalizedMessage(Type.Error, "Too many tasks in stack!  Check for looping.\n", null));
			_log.warn("Maximum steps reached, batch execution will abort!");
			if (!_noFail) return;
		} else {
			maxSteps--;
		}

		Stack<TaskDefinition> nextStack = new Stack<TaskDefinition>(); 

		while (!batchTasks.isEmpty()) {
			TaskDefinition def = batchTasks.pop();
			_log.debug("Checking stack with task: " + def.getName());
			List<String> defList = Arrays.asList(((String)def.getArgument("taskDefs")).split("\\n"));
			for (String defName : defList) {
				try{
					_log.debug("Fetching task definition: " + defName);
					TaskDefinition nextDef = ctx.getObject(TaskDefinition.class, defName);
					if (nextDef != null && nextDef.getTaskExecutor() instanceof BatchTaskExecutor) {
						_log.debug("Pushing batch task: " + nextDef.getName());
						nextStack.push(nextDef);
					} else {
						_log.debug(defName + " was not added to the batch stack.");
					}

				} catch (GeneralException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					_log.warn(defName + " task was not found!");
					result.addMessage(new LocalizedMessage(Type.Error, "Parent Task: " + def.getName() + " - down stream task " + defName + " could not be retreived!\n" + e.getLocalizedMessage(), null));
				}

			}

			if (!nextStack.isEmpty()) {
				checkStack (nextStack, result, ctx, maxSteps);
			}
			if (result.isError() && !_noFail) {
				return;
			}
		}
	}

	/**
	 * This is more of a setup task for checkStack.  It analzes the initial list of tasks and singles out the batch 
	 * executor tasks.  Any of those are put into a Stack and sent to the stack checker for an overflow check.  If an
	 * error is detected by the end of this step, the task list is emptied out to ensure no execution occurs.
	 * @param definitions
	 * @param ctx
	 * @param result
	 * @return
	 */
	private List<TaskDefinition> checkTasks (List<String> definitions, SailPointContext ctx, TaskResult result) {
		ArrayList<TaskDefinition> tasks = new ArrayList<TaskDefinition>();
		Stack<TaskDefinition> batchTasks = new Stack<TaskDefinition>();

		for (String def : definitions) {
			TaskDefinition nextDef;
			try {
				nextDef = ctx.getObject(TaskDefinition.class, def);
				if (nextDef == null) {
					// we didn't get a task definition. Add failure.
					_log.warn(def + " task was not found!");
					result.addMessage(new LocalizedMessage(Type.Error, def + " task was not found!", null));
				} else {
					_log.debug("Checking task: " + nextDef.getName());
					tasks.add(nextDef);
					if (nextDef.getTaskExecutor() instanceof BatchTaskExecutor) {
						_log.debug("Adding batch task to stack: " + nextDef.getName());
						batchTasks.add(nextDef);
					}
				}
			} catch (GeneralException e) {

				e.printStackTrace();
				_log.warn(def + " task was not found!");
				result.addMessage(new LocalizedMessage(Type.Error, def + " task could not be retreived!\n" + e.getLocalizedMessage(), null));
			}
		}

		// We may want to allow the user to specify the max number of steps to take via the TaskDefinition
		checkStack(batchTasks, result, ctx, MAX_STEPS);

		// Any failures at this point results in not running any tasks.
		if (result.isError() && !_noFail) {
			tasks = new ArrayList<TaskDefinition> ();
		}

		return tasks;
	}

	/* (non-Javadoc)
	 * @see sailpoint.object.TaskExecutor#execute(sailpoint.api.SailPointContext, sailpoint.object.TaskSchedule, sailpoint.object.TaskResult, sailpoint.object.Attributes)
	 */
	@SuppressWarnings("unchecked")
	public void execute(SailPointContext context, TaskSchedule schedule,
			TaskResult result, Attributes<String, Object> args)
	throws Exception {
		// Takes two args: list of string of TaskDef names and boolean 'Serial'.  True means to
		// execute the tasks serially
		String defStrings = (String)args.get("taskDefs");
		_log.debug("defStrings: " + defStrings);
		List<String> defList = Arrays.asList(defStrings.split("\\n"));
		
		String noFailString = (String)args.get("noFail");
		_log.debug("noFailString: " + noFailString);
		if (noFailString != null && !noFailString.equals("")) {
			_noFail = Boolean.valueOf(noFailString);
		}

		String strSerial = (String)args.get("serial");
		_log.debug("strSerial: " + strSerial);
		Boolean serial = Boolean.valueOf(strSerial);

		List<TaskDefinition> taskDefs = checkTasks(defList, context, result);

		String runResults = "";
		// If taskDefs come in as an empty list, the for loop will automatically be skipped.
		for (TaskDefinition task : taskDefs) {
			// create a TaskManager for managing tasks
			TaskManager tm = new TaskManager(context);

			Map<String, Object> moreArgs = new HashMap<String, Object>();
			// launch it, the runSync method will cause it to be run
			// synchronously, control won't return until the other
			// task finishes

			runResults = runResults.concat(task.getName() + ": Starting");

			result.setAttribute("tasksRun", runResults);
			if (serial) {
				_log.debug("Starting " + task.getName() + " synchronously");

				// Termination check
				if (!_terminate) {
					TaskResult res = tm.runSync(task.getName(), moreArgs);

					// look for errors in the result
					if (res.getErrors() == null || res.getErrors().isEmpty()) {
						_log.debug("Completed " + task.getName());
						runResults = runResults.concat("\n" + task.getName() + ": Complete" + "\n\n");
						result.setAttribute("tasksRun", runResults);
						context.saveObject(res);
						context.saveObject(result);
						context.commitTransaction();

						// go on to the next task...
					} else {
						_log.debug("ERROR: " + task.getName() + " failed.");
						runResults = runResults.concat("\n" + task.getName() + ": Error.  Exiting." + "\n");
						result.setAttribute("tasksRun", runResults);
						context.saveObject(res);
						context.saveObject(result);
						context.commitTransaction();
						// Running serially implies that one task is dependant on the preceding task. If that task fails, no further
						// executions should occur.
						// Hmmm. A scenario to test may be what happens when a task two or three levels in the call stack fails.  Should
						// all task tasks in that same stack fail?  Will they?
						return;
					}
				}
			} else {
				_log.debug("Starting " + task.getName() + " asynchronously");
				if (!_terminate) {
					tm.run(task.getName(), new Attributes(moreArgs));
					runResults = runResults.concat(" -- (Launching Task Asynchronously)\n");
					context.saveObject(result);
					context.commitTransaction();
				}
			}


		}

		runResults = runResults.concat("Execution complete");
		result.setAttribute("tasksRun", runResults);
		context.saveObject(result);
		context.commitTransaction();

	}

	/* (non-Javadoc)
	 * @see sailpoint.object.TaskExecutor#terminate()
	 */
	public boolean terminate() {
		// TODO Auto-generated method stub
		_terminate = true;
		return _terminate;
	}



}
