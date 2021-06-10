package sailpoint.salesforce.reports;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import sailpoint.salesforce.reports.object.AbstractSFDCObject;

public interface SFDCJRDataSource extends JRDataSource {

	public abstract void addRecord(AbstractSFDCObject theCase);

	public abstract Object getFieldValue(JRField jrField) throws JRException;

	public abstract boolean next() throws JRException;

}