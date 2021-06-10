/**
 * 
 */
package sailpoint.custom.support;

import sailpoint.api.MessageAccumulator;
import sailpoint.api.SailPointContext;
import sailpoint.tools.GeneralException;

/**
 * @author trey.kirk
 *
 */
public interface HibernateIndexRebuilder {
	
	public void reorderList(SailPointContext context, String id, MessageAccumulator errHandler) throws GeneralException;

}
