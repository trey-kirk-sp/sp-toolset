/**
 * 
 */
package sailpoint.custom.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import sailpoint.api.Certificationer;
import sailpoint.api.SailPointContext;
import sailpoint.object.AbstractCertificationItem;
import sailpoint.object.Attributes;
import sailpoint.object.Certification;
import sailpoint.object.CertificationEntity;
import sailpoint.object.Identity;
import sailpoint.object.TaskResult;
import sailpoint.object.TaskSchedule;
import sailpoint.task.AbstractTaskExecutor;
import sailpoint.tools.GeneralException;
import sailpoint.web.identity.IdentityProxy;

/**
 * Custom Task to "split" certifications into smaller child certifications.  The idea is that one
 * certification can be generated for a large population.  Some requirements make large certifications
 * such as that neccessary.  Other times, the user whishes to go back and reassign chunks of the cert
 * to the actual certifier.  The problem is that dealing with large certifications such as these can be
 * unwieldy, especially when it comes to reassignments.  With this task, several smaller certifications
 * will be created as child certifications as the original to maintain a hierarchy.  These smaller certs
 * can be dealt with in a much easier fashion.
 * @author trey.kirk
 *
 */
public class AccountCertificationSplitterExecutor extends AbstractTaskExecutor {

	public static int DEFAULT_LIMIT = 1000;

	public boolean halt;

	/**
	 * 
	 */
	public AccountCertificationSplitterExecutor() {
		// TODO Auto-generated constructor stub
		this.halt = false;
	};

	/* (non-Javadoc)
	 * @see sailpoint.object.TaskExecutor#execute(sailpoint.api.SailPointContext, sailpoint.object.TaskSchedule, sailpoint.object.TaskResult, sailpoint.object.Attributes)
	 */
	public void execute(SailPointContext context, TaskSchedule schedule,
			TaskResult result, Attributes<String, Object> args)
	throws Exception {
		// TODO Auto-generated method stub
		String origCert = (String)args.get("cert");
		String correlationAttribute = (String)args.get("correlationAttribute");
		String limitStr = (String)args.get("limit");
		int limit = DEFAULT_LIMIT;
		try {
			limit = Integer.valueOf(limitStr);
		} catch (NumberFormatException e) {
			// let it be the default limit
			result.addWarning(e.getMessage() + ".  Using default limit: " + DEFAULT_LIMIT);
		}

		// Get the certification
		Certification cert = context.getObject(Certification.class, origCert);

		// Organize the entities by the given attribute.  Currently, this only works for Entities that
		// are Identities.  A prettier version of this would be more agile.
		HashMap<String, List<CertificationEntity>> organizedEntities = this.organizeEntities(cert.getEntities(), correlationAttribute, context);

		// Using an iterator as it makes it easier to determine if we've reached the end of the set
		Set keySet = organizedEntities.keySet();
		Iterator keyIt = keySet.iterator();

		// Who's the owner of this cert.  Try owner first.  Failing that, get the first certifier.
		Identity owner = cert.getOwner();
		if (owner == null) {
			owner = context.getObject(Identity.class, cert.getCertifiers().get(0));
		}

		int sum = 0;
		ArrayList<AbstractCertificationItem> items = new ArrayList<AbstractCertificationItem>();

		// Create a new list for new children and ensure there are no
		// current children.
		ArrayList<Certification> children = new ArrayList<Certification>();
		cert.setCertifications(new ArrayList<Certification>());
		Certificationer certificationer = new Certificationer(context);

		// After several iterations of this code, I'm not sure which statement, if any, will
		// throw a GeneralException.  Meh, leave it in and catch'em anyways.
		try {
			while (keyIt.hasNext()) {

				List<CertificationEntity> entities = organizedEntities.get(keyIt.next());
				sum = sum + entities.size();
				for (CertificationEntity entity : entities) {
					items.addAll(entity.getItems());
				}

				// jump in here if we've passed the limit, reached the last key, or the terminate
				// command has been sent.  On a terminate, finish what yer doing before moving on.
				if (sum > limit || !keyIt.hasNext() || this.halt) {
					// do reassign.  I like doing the reassign as it handles all of the cert
					// generation and workitem assignments.  I'm not fond of the new "reassign blah blah" name...
					// suppose I could change it since I grab it anyways.
					sum = 0;
					cert.bulkReassign(owner, items, owner, "Break up of original certification: " + cert.getName(), null);

					// copy 'n pasted logic code from bulkReassignment
					context.saveObject(cert);
					result.addErrors(certificationer.refresh(cert));

					// Now here's the trick, we don't want the next reassignment to be merged to this one,
					// which is typical behavior since they're all going to the same person.  So we do that
					// by unlinking this new cert as a child and makeing it stand-alone.
					// We'll keep track of it and re-link it as a child later

					// Gotta find it
					for (Certification child : cert.getCertifications()) {
						if (child.isBulkReassignment() && !child.hasBeenSigned() && this.isCertificationOwner(child, owner, context)) {
							// Found our new kid, disown it for a bit
							child.setParent(null);
							cert.setCertifications(new ArrayList<Certification>());
							children.add(child);
							break;
						}
					}

					// reinit the items list.
					items = new ArrayList<AbstractCertificationItem>();
					context.commitTransaction();
					
					if (this.halt) {
						result.addWarning("Process halted early by user.");
						break;
					}
				}
			}

			// reaquaint the kids with their baby-daddy
			for (Certification child : children) {
				child.setParent(cert);
				cert.add(child);
			}

			// Save
			context.commitTransaction();

		} catch (Exception e) {
			result.addError(e);
			context.rollbackTransaction();
		}
	}

	/*
	 * Test to see if the given identity is the owner of the given cert.
	 */
	private boolean isCertificationOwner(Certification cert, Identity certifier, SailPointContext context)
	throws GeneralException {

		List<String> certifiers = cert.getCertifiers();
		return (null != certifiers) && (1 == certifiers.size()) &&
		certifier.equals(context.getObjectByName(Identity.class, certifiers.get(0)));
	}

	/*
	 * Convinience method that organizes all of the CertificationEntity objects into a hashmap with each bucket being
	 * identified by the values provided by the given attribute.  For example, given the following Identities:
	 * 
	 * Fred:
	 * 	Firstname: Fred
	 * 	Lastname: Flintstone
	 * 	Manager: Willma
	 * 	Application: Rockhead
	 * 
	 * Barney:
	 * 	Firstname: Barney
	 * 	Lastname: Rubble
	 * 	Manager: Betty
	 * 	Application: Rockhead
	 * 
	 * Willma:
	 * 	Firstname: Willma
	 * 	Lastname: Flintstone
	 * 	Manager: Dino
	 * 	Application: Bedrock
	 * 
	 * Betty:
	 * 	Firstname: Betty
	 * 	Lastname: Rubble
	 * 	Manager: Dino
	 * 	Application: Bedrock
	 * 
	 * If these identities were organized based off of 'Manager', the hash map would look like:
	 * 
	 * key: Willma	values: Fred
	 * key: Betty	values: Barney
	 * key: Dino	values: Willma, Betty
	 * 
	 * or by Application:
	 * key: Rockhead	values: Fred, Barney
	 * key: Bedrock		values: Willma, Betty
	 * 
	 */
	private HashMap<String, List<CertificationEntity>> organizeEntities (List<CertificationEntity> entities, String attribute, SailPointContext context) {

		HashMap<String, List<CertificationEntity>> mapOfEntities = new HashMap<String, List<CertificationEntity>>();

		Iterator<CertificationEntity> entityIt = entities.iterator();
		while (entityIt.hasNext()) {
			CertificationEntity entity = entityIt.next();
			Identity id;
			try {
				id = context.getObject(Identity.class, entity.getIdentity());
				String attrValue = IdentityProxy.get(id, attribute);
				if (attrValue == null || attrValue.equals("")) {
					attrValue = "noValue";
				}

				List<CertificationEntity> entityList = mapOfEntities.get(attrValue);
				boolean needToAdd = false;
				if (entityList == null) {
					entityList = new ArrayList<CertificationEntity>();
					needToAdd = true;
				}

				entityList.add(entity);
				if (needToAdd) {
					// should only have to do this if the list wasn't already there.
					mapOfEntities.put(attrValue, entityList);
				}

			} catch (GeneralException e) {
				throw (new RuntimeException ("Can't organize identities:\n" + e.getMessage()));
			}

		}

		return mapOfEntities;
	}

	/* (non-Javadoc)
	 * @see sailpoint.object.TaskExecutor#terminate()
	 */
	public boolean terminate() {
		// TODO Auto-generated method stub
		this.halt = true;
		return this.halt;
	}

}
