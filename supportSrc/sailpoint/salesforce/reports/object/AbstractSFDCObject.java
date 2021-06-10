package sailpoint.salesforce.reports.object;

import java.util.List;

public abstract class AbstractSFDCObject implements Comparable<AbstractSFDCObject> {

	protected boolean isEqual(Object comp1, Object comp2) {
		if (comp1 == null && comp2 == null) {
			return true;
		}
		
		if (comp1 == null || comp2 == null) {
			return false;
		}
		
		return comp1.equals(comp2);
 	}
	
	protected int hashHelper (Object o, int prime) {
		if (o == null) {
			return 0;
		}
		return o.hashCode() * prime;
	}
	
	protected int compareHelper (Comparable comp1, Comparable comp2) {
		if (comp1 == null && comp2 == null) {
			return 0;
		}
		
		if (comp1 == null) {
			return -1;
		} else if (comp2 == null) {
			return 1;
		} else {
			return comp1.compareTo(comp2);
		}
	}
	
	abstract protected <T extends AbstractSFDCObject> List<T> getChildren();
}
