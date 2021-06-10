package sailpoint.custom.support;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import sailpoint.api.SailPointContext;
import sailpoint.object.Attributes;
import sailpoint.object.Filter;
import sailpoint.object.Identity;
import sailpoint.object.Link;
import sailpoint.object.QueryOptions;
import sailpoint.object.TaskResult;
import sailpoint.object.TaskSchedule;
import sailpoint.task.AbstractTaskExecutor;

/**
 * IDX rebuilder designed to correct index inconsistencies with the Identity object.  This
 * will compose of duplicate idx values as well as "gapped" idx values (idx values must start at 0
 * and increment by 1.)
 * @author trey.kirk
 *
 */
public class IdentityIDXScanner extends AbstractTaskExecutor {
	
	private static Log log = LogFactory.getLog(IdentityIDXScanner.class); 

	private static final String COL_ID = "id";

	private boolean _terminate = false;
	private HibernateIndexRebuilder _rebuilder = new IdentityIDXRebuilder();
	private StringBuffer _strResults = new StringBuffer();

	/**
	 * Execute method required by AbstractTaskExecutor
	 */
	public void execute(SailPointContext context, TaskSchedule schedule, TaskResult result, Attributes<String, Object> args) throws Exception {

		// Get a list of ids for all Identities.  Using a list of ids will reduce memory consumption
		
		// Use empty ops list.  Future enhancement will be to pass a filter down
		// which will be added to the QueryOpts to reduce the potential size of the list of ids
		QueryOptions ops = new QueryOptions();
		
		// Build the column list that will be fetched.
		List<String> idColsList = new ArrayList<String>();
		idColsList.add(COL_ID);
		int idIndex = idColsList.indexOf(COL_ID);

		// Returns just the ids
		Iterator<Object[]> it = context.search(Identity.class, ops, idColsList);
		
		// Iterate through list
		while (it.hasNext() && !_terminate) {
			Object[] current = it.next();
			String id = (String)current[idIndex];
			log.debug("Identity: " + id);
			// Would prefer to just get the links and the links' associated idx attributes without
			// fetching the actual Identity object.  But that got unneccessarily complicated.  So
			// will stick with the Identity and revisit this if it becomes a performance issue.
			Identity identity = context.getObjectById(Identity.class, id);
			String msg = "Rebuilding links on " + identity.getName();
			List<Link> links = identity.getLinks();
			if (links.contains(null)) {
				// null in the list means idx has gaps: 0, 1, 2, 4
				_strResults.append(msg + ": null link found.\n");
				log.debug(identity.getName() + " : Null link found.");
				_rebuilder.reorderList(context, id, result);
			} else {
				// Get the same list a way that require Hibernate to build a PersistantList.
				// Then compare the lists sizes to each other.  Thought: suppose we could be
				// more exact by comparing ids.  Tough to imagine the lists would be same
				// sized with different ids in the list.
				QueryOptions linkOps = new QueryOptions();
				linkOps.add(Filter.eq("identity", identity));
				Iterator<Link> actualLinksItter = context.search(Link.class, linkOps);
				int counter = 0;
				// This search method returns an Iterator, so we have to trapse through it to get the right
				// count.
				while (actualLinksItter.hasNext()) {
					counter++;
					actualLinksItter.next();
				}
				// The most common cause for being here is our new list has more Identities than our original
				// list.  This would be caused by idx values being duplicated: 0, 1, 2, 2, 3
				// It may be possible for idx values to be null, but Hibernate should've complained
				// about that by now.
				if (counter != links.size()) {
					log.debug(identity.getName() + ": inconsistant Link lists - from Identity: " + links.size() + " vs. from search iterator: " + counter);
					_strResults.append(msg + ": inconsistent link lists (dupe idx values).\n");
					_rebuilder.reorderList(context, id, result);
				}
			}
		}
		if (_terminate) {
			_strResults.append("Task terminated early.\n");
			log.debug("Task terminated early.");
		}
		if (_strResults.length() < 1) {
			// 0 length in the results means we didn't report issues
			_strResults.append("No issues found!\n");
		}
		result.setAttribute("results", _strResults.toString());
	}
		
	public boolean terminate() {
		// TODO Auto-generated method stub
		_terminate = true;
		return _terminate;
	}

}
