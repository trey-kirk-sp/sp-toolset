/**
 * 
 */
package sailpoint.custom.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import sailpoint.api.MessageAccumulator;
import sailpoint.api.SailPointContext;
import sailpoint.object.CertificationEntity;
import sailpoint.object.CertificationItem;
import sailpoint.object.Filter;
import sailpoint.object.QueryOptions;
import sailpoint.tools.GeneralException;
import sailpoint.tools.LocalizedMessage;
import sailpoint.tools.LocalizedMessage.Type;

/**
 * @author trey.kirk
 *
 */
public class CertificationItemIDXRebuilder implements HibernateIndexRebuilder {

	private static Log log = LogFactory.getLog(CertificationItemIDXRebuilder.class);

	public void reorderList(SailPointContext context, String certificationEntityId, MessageAccumulator handler) {

		try {
			CertificationEntity entity = context.getObjectById(CertificationEntity.class, certificationEntityId);
			ArrayList<CertificationItem> newItems = new ArrayList<CertificationItem> ();
			log.info("CertificationEntity: " + entity.getName() + ":" + entity.getId());
			/*
			 * Little too resource intensive at the moment.
			 * 
			List<CertificationItem> allItems = context.getObjects(CertificationItem.class);
			*/

			QueryOptions qo = new QueryOptions();
			qo.add(Filter.eq("parent", entity));
			List<CertificationItem> allItems = context.getObjects(CertificationItem.class, qo);

			for (CertificationItem item : allItems) {
				// not sure if an item can be null.  But hey, the assumption is stuff is already messed up.  We
				// should double check
				if (item != null) {
					log.debug("Examining Cert Item: " + item.getId());

					CertificationEntity parent = item.getCertificationEntity();
					// Same clause as before: dont' expect we could get a null entity, but let's assume stuff ain't right
					// to begin with
					if (parent != null) {
						log.debug(item.getId() + " parent: " + parent.getId());
						if (parent.getId().equalsIgnoreCase(entity.getId())) {
							// matching parents, put it in our new list
							log.info(parent.getId() + " matches item's parent.");
							newItems.add(item);
						}
					} else {
						// Should note that it's horked
						log.warn(item.getId() + " is an orphan.");
						// Suppose we should tell the error handler too
						handler.addMessage(new LocalizedMessage(Type.Warn, item.getId() + " is an orphan.", null));
					}
				}

			}
			
			

			// Sort.  Then replace the old list with the new one
			Collections.sort(newItems);
			entity.setItems(newItems);

			context.commitTransaction();

		} catch (GeneralException e) {
			// Hit a snag, abort and add the error.
			handler.addMessage(new LocalizedMessage(Type.Error, e.getMessage(), null));
		}

	}


}
