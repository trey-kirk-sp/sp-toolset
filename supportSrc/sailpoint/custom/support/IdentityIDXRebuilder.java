package sailpoint.custom.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import sailpoint.api.MessageAccumulator;
import sailpoint.api.SailPointContext;
import sailpoint.object.Attributes;
import sailpoint.object.Filter;
import sailpoint.object.Identity;
import sailpoint.object.Link;
import sailpoint.object.QueryOptions;
import sailpoint.object.SailPointObject;
import sailpoint.object.TaskResult;
import sailpoint.object.TaskSchedule;
import sailpoint.task.AbstractTaskExecutor;
import sailpoint.tools.GeneralException;
import sailpoint.tools.LocalizedMessage;
import sailpoint.tools.LocalizedMessage.Type;

public class IdentityIDXRebuilder extends AbstractTaskExecutor implements HibernateIndexRebuilder {

	private static Log log = LogFactory.getLog(IdentityIDXRebuilder.class);

	public void reorderList(SailPointContext context, String id,
			MessageAccumulator errHandler) throws GeneralException {

		//Identity identity = context.getObjectByName(Identity.class, id);
		Identity identity = getObjectByNameOrId(context, Identity.class, id);

		if (identity == null) {
			log.warn("Identity null: " + id);
			errHandler.addMessage(new LocalizedMessage(Type.Warn, "Identity null: " + id, (Object[])null));
			return;
		}
		
		// We should use a query that will get all links owned by this Identity accounting for gapped
		// idx values or dupe idx values.  It should be unlikely that these would have null idx values since
		// Hibernate would likely go nuts by now if that were the case.
		ArrayList<Link> newLinks = new ArrayList<Link>();

		QueryOptions qo = new QueryOptions();
		qo.add(Filter.eq("identity", identity));
		List<Link> allLinks = context.getObjects(Link.class, qo);

		if (allLinks != null) {
			for (Link link : allLinks) {
				// not sure if an item can be null.  But hey, the assumption is stuff is already messed up.  We
				// should double check
				if (link != null) {
					log.debug("Examining Link: " + link.getId());

					Identity parent = link.getIdentity();
					// Same clause as before: dont' expect we could get a null entity, but let's assume stuff ain't right
					// to begin with
					if (parent != null) {
						log.debug(link.getId() + " Identity: " + parent.getId());
						if (parent.getId().equalsIgnoreCase(identity.getId())) {
							// matching parents, put it in our new list
							log.info(parent.getId() + " matches link's identity.");
							newLinks.add(link);
						}
					} else {
						// Should note that it's horked
						log.warn(link.getId() + " is an orphan.");
						// Suppose we should tell the error handler too
						errHandler.addMessage(new LocalizedMessage(Type.Warn, link.getId() + " is an orphan.", (Object[])null));
					}
				}

			}

			// Remove old links
			if (identity.getLinks() != null) {
				List<Link> oldLinks = new ArrayList<Link>(identity.getLinks());
				Collections.copy(oldLinks, identity.getLinks());
				for (Link link : oldLinks) {
					if (link != null) {
						identity.remove(link);
					}

				}
			}
			context.commitTransaction();
			context.decache(identity);
			Identity freshIdentity = null;
			
			try {
				//Identity freshIdentity = context.getObjectByName(Identity.class, id);
				freshIdentity = getObjectByNameOrId(context, Identity.class, id);

				// Replace the old list with the new one

				for (Link newLink : allLinks) {
					freshIdentity.add(newLink);
				}
				
				context.commitTransaction();
				
			} catch (Throwable t) {
				// We've just removed links from an Identity and something happened when
				// checking it back in to restore those links.  It's entirely possible we've
				// just left those links orphaned.  This is bad.  But with the right information
				// reported, this can be manually corrected.
				StringBuffer errMsg = new StringBuffer("Error when comitting Identity " + id + " back in.  Confirm the following links are not orphaned:\n");
				if (freshIdentity != null) {
					errMsg.append("identity_id: " + freshIdentity.getId() + "\n");
					for (Link newLink : allLinks) {
						if (newLink != null) {
							errMsg.append("\tid: " + newLink.getId() + "\tnative_identity: " + newLink.getNativeIdentity() + "\n");
						}
					}
				}
				log.error(errMsg.toString());
				errHandler.addMessage(new LocalizedMessage(Type.Error, errMsg.toString(), (Object[])null));
			}
		}
	}

	public <T extends SailPointObject> T getObjectByNameOrId(SailPointContext context, Class<T> cls, String id) {
		T spObj;
		try {
			spObj = context.getObjectById(cls, id);
			if (spObj == null) {
				return context.getObjectByName(cls, id);
			}
			return spObj;
		} catch (GeneralException e) {
			return null;
		}
	}

	public void execute(SailPointContext context, TaskSchedule schedule, TaskResult result, Attributes<String, Object> args) throws Exception {
		HibernateIndexRebuilder rebuilder = new IdentityIDXRebuilder();
		String identity = args.getString("identity");
		rebuilder.reorderList(context, identity, result);
	}

	public boolean terminate() {
		// TODO Auto-generated method stub
		return false;
	}

}
