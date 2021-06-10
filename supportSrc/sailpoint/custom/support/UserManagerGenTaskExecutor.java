/**
 * 
 */
package sailpoint.custom.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import sailpoint.api.SailPointContext;
import sailpoint.custom.support.gendata.LUser;
import sailpoint.object.Attributes;
import sailpoint.object.Identity;
import sailpoint.object.SailPointObject;
import sailpoint.object.TaskExecutor;
import sailpoint.object.TaskResult;
import sailpoint.object.TaskSchedule;
import sailpoint.task.AbstractTaskExecutor;
import sailpoint.tools.GeneralException;

/**
 * @author trey.kirk
 *
 */
public class UserManagerGenTaskExecutor extends AbstractTaskExecutor {

	/**
	 * 
	 */
	public UserManagerGenTaskExecutor() {
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see sailpoint.object.TaskExecutor#execute(sailpoint.api.SailPointContext, sailpoint.object.TaskSchedule, sailpoint.object.TaskResult, sailpoint.object.Attributes)
	 */
	public void execute(SailPointContext context, TaskSchedule schedule,
			TaskResult result, Attributes<String, Object> args)
	throws Exception {
		// TODO Auto-generated method stub

		String[] ids = {
				"Hippy.Manager",
				"Firsty.Last",
				"Secondy.Last",
				"Thirdy.Last",
				"Firsty2.Last",
				"Secondy2.Last",
				"Thirdy2.Last",
				"Firsty3.Last",
				"Secondy3.Last",
				"Thirdy3.Last",
				"Firsty4.Last",
				"Secondy4.Last",
				"Thirdy4.Last",
				"Firsty5.Last",
				"Secondy5.Last",
				"Thirdy5.Last",
				"Firsty6.Last",
				"Secondy6.Last",
				"Thirdy6.Last",
				"Firsty7.Last",
				"Secondy7.Last",
				"Thirdy7.Last",
				"Firsty8.Last",
				"Secondy8.Last",
				"Thirdy8.Last",
				"Firsty9.Last",
				"Secondy9.Last",
				"Thirdy9.Last",
				"Firsty10.Last",
				"Secondy10.Last",
				"Thirdy10.Last"
		};
		
		clean(context, Arrays.asList(ids));
		
		createUser(context, "Hippy.Manager", "Hippy", "Manager", "king.ofyou@example.com", "James.McMurtry", false);
		context.commitTransaction();

		createUser(context, "Firsty.Last", "Firsty", "Lasty", "firsty.lasty@example.com", "Hippy.Manager", false);
		createUser(context, "Secondy.Last", "Secondy", "Lasty", "secondy.lasty@example.com", "Hippy.Manager", false);
		createUser(context, "Thirdy.Last", "Thirdy", "Lasty", "thirdy.lasty@example.com", "Hippy.Manager", false);

		createUser(context, "Firsty.Last1", "Firsty", "Lasty", "firsty.lasty@example.com", "Hippy.Manager", false);
		createUser(context, "Secondy.Last1", "Secondy", "Lasty", "secondy.lasty@example.com", "Hippy.Manager", false);
		createUser(context, "Thirdy.Last1", "Thirdy", "Lasty", "thirdy.lasty@example.com", "Hippy.Manager", false);

		createUser(context, "Firsty.Last2", "Firsty", "Lasty", "firsty.lasty@example.com", "Hippy.Manager", false);
		createUser(context, "Secondy.Last2", "Secondy", "Lasty", "secondy.lasty@example.com", "Hippy.Manager", false);
		createUser(context, "Thirdy.Last2", "Thirdy", "Lasty", "thirdy.lasty@example.com", "Hippy.Manager", false);

		createUser(context, "Firsty.Last3", "Firsty", "Lasty", "firsty.lasty@example.com", "Hippy.Manager", false);
		createUser(context, "Secondy.Last3", "Secondy", "Lasty", "secondy.lasty@example.com", "Hippy.Manager", false);
		createUser(context, "Thirdy.Last3", "Thirdy", "Lasty", "thirdy.lasty@example.com", "Hippy.Manager", false);

		createUser(context, "Firsty.Last4", "Firsty", "Lasty", "firsty.lasty@example.com", "Hippy.Manager", false);
		createUser(context, "Secondy.Last4", "Secondy", "Lasty", "secondy.lasty@example.com", "Hippy.Manager", false);
		createUser(context, "Thirdy.Last4", "Thirdy", "Lasty", "thirdy.lasty@example.com", "Hippy.Manager", false);

		createUser(context, "Firsty.Last5", "Firsty", "Lasty", "firsty.lasty@example.com", "Hippy.Manager", false);
		createUser(context, "Secondy.Last5", "Secondy", "Lasty", "secondy.lasty@example.com", "Hippy.Manager", false);
		createUser(context, "Thirdy.Last5", "Thirdy", "Lasty", "thirdy.lasty@example.com", "Hippy.Manager", false);

		createUser(context, "Firsty.Last6", "Firsty", "Lasty", "firsty.lasty@example.com", "Hippy.Manager", false);
		createUser(context, "Secondy.Last6", "Secondy", "Lasty", "secondy.lasty@example.com", "Hippy.Manager", false);
		createUser(context, "Thirdy.Last6", "Thirdy", "Lasty", "thirdy.lasty@example.com", "Hippy.Manager", false);

		createUser(context, "Firsty.Last7", "Firsty", "Lasty", "firsty.lasty@example.com", "Hippy.Manager", false);
		createUser(context, "Secondy.Last7", "Secondy", "Lasty", "secondy.lasty@example.com", "Hippy.Manager", false);
		createUser(context, "Thirdy.Last7", "Thirdy", "Lasty", "thirdy.lasty@example.com", "Hippy.Manager", false);

		createUser(context, "Firsty.Last8", "Firsty", "Lasty", "firsty.lasty@example.com", "Hippy.Manager", false);
		createUser(context, "Secondy.Last8", "Secondy", "Lasty", "secondy.lasty@example.com", "Hippy.Manager", false);
		createUser(context, "Thirdy.Last8", "Thirdy", "Lasty", "thirdy.lasty@example.com", "Hippy.Manager", false);

		createUser(context, "Firsty.Last9", "Firsty", "Lasty", "firsty.lasty@example.com", "Hippy.Manager", false);
		createUser(context, "Secondy.Last9", "Secondy", "Lasty", "secondy.lasty@example.com", "Hippy.Manager", false);
		createUser(context, "Thirdy.Last9", "Thirdy", "Lasty", "thirdy.lasty@example.com", "Hippy.Manager", false);

		createUser(context, "Firsty.Last10", "Firsty", "Lasty", "firsty.lasty@example.com", "Hippy.Manager", false);
		createUser(context, "Secondy.Last10", "Secondy", "Lasty", "secondy.lasty@example.com", "Hippy.Manager", false);
		createUser(context, "Thirdy.Last10", "Thirdy", "Lasty", "thirdy.lasty@example.com", "Hippy.Manager", false);

		context.commitTransaction();
	}

	public void clean(SailPointContext context, List<String> ids) {
		for (String name : ids) {
			try {
				SailPointObject obj = context.getObjectByName(Identity.class, name);
				if (obj != null) {
					context.removeObject(obj);
				}

			}catch (GeneralException e) {}
		}
		try {

			context.commitTransaction();
		}catch (GeneralException e) {}
	}

	public void createUser(SailPointContext context, String username, String fname, String lname, String email, String manager, boolean spew) {
		LUser loozer = new LUser();
		loozer.setName(username);
		loozer.setEmail(email);
		loozer.setFirstname(fname);
		loozer.setLastname(lname);
		loozer.setManager(manager);
		//loozer.setFullname("Firsty Lasty");
		Identity loozerId = loozer.toIdentity(context);
		if (spew) {
			System.out.println(loozer.toXml(loozerId));
		}
		try {
			context.saveObject(loozerId);
		} catch (Throwable e) {
			System.err.println(e.getMessage());
		}
	}

	/* (non-Javadoc)
	 * @see sailpoint.object.TaskExecutor#terminate()
	 */
	public boolean terminate() {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TaskExecutor executor = new UserManagerGenTaskExecutor();
		Attributes attr = new Attributes();
		TaskExecutorWrapper wrapper = new TaskExecutorWrapper();
		SailPointContext ctx = wrapper.initialize();
		try {
			wrapper.execute(ctx, executor.getClass(), attr);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}


	}

}
