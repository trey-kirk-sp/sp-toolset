package sailpoint.salesforce.reports;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.beanutils.PropertyUtils;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import sailpoint.salesforce.reports.object.AbstractSFDCObject;

public class BaseSFDCJRDataSource implements SFDCJRDataSource {
	
	private List<AbstractSFDCObject> _sfdcObjs;
	private AbstractSFDCObject _sfdcObj;
	private Iterator<AbstractSFDCObject> _sfdcIter;
	
	public BaseSFDCJRDataSource(Collection<AbstractSFDCObject> sfdcObjs) {
		_sfdcObjs = new ArrayList<AbstractSFDCObject>(sfdcObjs);
	}
	
	public BaseSFDCJRDataSource() {
		super();
	}
	
	public void setRecords(List<AbstractSFDCObject> sfdcRecords) {
		if (sfdcRecords != null) {
			_sfdcObjs = sfdcRecords;
		} else {
			_sfdcObjs = new ArrayList<AbstractSFDCObject>();
		}
	}
	
	public List<AbstractSFDCObject> getRecords() {
		if (_sfdcObjs == null) {
			setRecords(null);
		} 
		return _sfdcObjs;
	}
	
	protected Iterator<AbstractSFDCObject> getIter() {
		if (_sfdcIter == null) {
			_sfdcIter = getRecords().iterator(); 
		}
		return _sfdcIter;
	}

	public void addRecord(AbstractSFDCObject sfdcObj) {
		getRecords().add(sfdcObj);
	}

	public Object getFieldValue(JRField jrField) throws JRException {
		// TODO Auto-generated method stub
		String fieldName = jrField.getName();
		Object value = null;
		try {
			value = PropertyUtils.getNestedProperty(_sfdcObj, fieldName);
		} catch (Exception e) {
			throw new JRException(e);
		}
		return value;
	}

	public boolean next() throws JRException {
		if (getIter().hasNext()) {
			_sfdcObj = getIter().next();
			return true;
		} else {
			return false;
		}
	}

}
