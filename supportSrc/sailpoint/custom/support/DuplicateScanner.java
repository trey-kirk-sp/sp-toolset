/**
 * 
 */
package sailpoint.custom.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import sailpoint.api.SailPointContext;
import sailpoint.object.Attributes;
import sailpoint.object.Filter;
import sailpoint.object.Identity;
import sailpoint.object.QueryOptions;
import sailpoint.object.TaskResult;
import sailpoint.object.TaskSchedule;
import sailpoint.task.AbstractTaskExecutor;

/**
 * @author trey.kirk
 *
 */
public class DuplicateScanner extends AbstractTaskExecutor {

	/**
	 * 
	 */
	public DuplicateScanner() {
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see sailpoint.object.TaskExecutor#execute(sailpoint.api.SailPointContext, sailpoint.object.TaskSchedule, sailpoint.object.TaskResult, sailpoint.object.Attributes)
	 */
	public void execute(SailPointContext context, TaskSchedule schedule,
			TaskResult result, Attributes<String, Object> args)
	throws Exception {
		// Inputs: List of attributes, Filter string
		// Action: Iterate through each identity retreived by the filter and attempt to correlate them to
		// 			another identity by way of the provided attributes.

		String strFilter = args.getString("filter");
		QueryOptions qo = new QueryOptions(); 
		if (strFilter != null) {
			Filter.FilterCompiler comp = new Filter.FilterCompiler();
			qo.add(comp.compileFilter(strFilter));
		}

		String attrStr = args.getString("attributes");
		List<String> attributes = Arrays.asList(attrStr.split("\n"));
		
		// A list of lists
		HashSet<List<String>> matches = new HashSet<List<String>>(); 
		Iterator<Identity> idter =  context.search(Identity.class, qo);

		while (idter.hasNext()) {
			Identity currentId = idter.next();
			StringBuffer queryStr = new StringBuffer();
			for (String attribute : attributes) {
				queryStr.append("(" + attribute + " == \"");
				String value = (String) currentId.getAttribute(attribute);
				if (value != null) {
					value.replaceAll("\"", "\\\"");
					queryStr.append(value);
				}
				queryStr.append("\") && ");
			}
			// Delete the trailing " && "
			queryStr.delete(queryStr.length() - 4, queryStr.length());				
			Filter.FilterCompiler identitySearchComp = new Filter.FilterCompiler();
			QueryOptions identityQo = new QueryOptions();
			identityQo.add(identitySearchComp.compileFilter(queryStr.toString()));
			List<Identity> matchedIds = context.getObjects(Identity.class, identityQo);
			if (matchedIds.size() > 1) {
				// found multiples
				ArrayList<String> names = new ArrayList<String>();
				for (Identity matchedId : matchedIds) {
					names.add(matchedId.getName());
				}
				matches.add(names);
			}
		}

		// Break down the results into something human friendly
		for (List<String> names : matches) {
			result.setAttribute(names.get(0), names);
		}
		context.commitTransaction();
	}

	/* (non-Javadoc)
	 * @see sailpoint.object.TaskExecutor#terminate()
	 */
	public boolean terminate() {
		// TODO Auto-generated method stub
		return false;
	}
}
