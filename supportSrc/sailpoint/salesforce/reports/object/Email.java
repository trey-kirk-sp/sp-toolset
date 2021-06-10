package sailpoint.salesforce.reports.object;

import java.util.Date;
import java.util.List;

public class Email extends AbstractSFDCObject {

	private String _subject;
	private String _status;
	private Date _messageDate;
	private boolean _isIncoming;
	private boolean _hasAttachment;
	private String _from;
	private String _fromAddress;
	private String _toAddress;
	private String _ccAddress;
	private String _bccAddress;

	@Override
	public int hashCode() {

		int code = 0;
		code += hashHelper(_fromAddress, 3);
		code += hashHelper(_toAddress, 5);
		code += hashHelper(_bccAddress, 7);
		code += hashHelper(_ccAddress, 11);
		code += hashHelper(_messageDate, 13);
		code += hashHelper(_subject, 17);
		return code;
	}


	@Override
	public boolean equals(Object otherEmail) {
		if (otherEmail instanceof Email) {
			Email compareTo = (Email)otherEmail;

			return (isEqual(compareTo.getMessageDate(), this.getMessageDate()) &&
					isEqual(compareTo.getSubject(), this.getSubject()) &&
					isEqual(compareTo.getFromAddress(), this.getFromAddress()) &&
					isEqual(compareTo.getToAddress(), this.getToAddress()) &&
					isEqual(compareTo.getCcAddress(), this.getCcAddress()) &&
					isEqual(compareTo.getBccAddress(), this.getBccAddress()));
		}

		return false;
	}

	/**
	 * @return the bccAddress
	 */
	public String getBccAddress() {
		return _bccAddress;
	}
	/**
	 * @param bccAddress the bccAddress to set
	 */
	public void setBccAddress(String bccAddress) {
		this._bccAddress = bccAddress;
	}
	/**
	 * @return the ccAddress
	 */
	public String getCcAddress() {
		return _ccAddress;
	}
	/**
	 * @param ccAddress the ccAddress to set
	 */
	public void setCcAddress(String ccAddress) {
		this._ccAddress = ccAddress;
	}
	/**
	 * @return the from
	 */
	public String getFrom() {
		return _from;
	}
	/**
	 * @param from the from to set
	 */
	public void setFrom(String from) {
		this._from = from;
	}
	/**
	 * @return the fromAddress
	 */
	public String getFromAddress() {
		return _fromAddress;
	}
	/**
	 * @param fromAddress the fromAddress to set
	 */
	public void setFromAddress(String fromAddress) {
		this._fromAddress = fromAddress;
	}
	/**
	 * @return the hasAttachment
	 */
	public boolean isHasAttachment() {
		return _hasAttachment;
	}
	/**
	 * @param hasAttachment the hasAttachment to set
	 */
	public void setHasAttachment(boolean hasAttachment) {
		this._hasAttachment = hasAttachment;
	}
	/**
	 * @return the isIncoming
	 */
	public boolean isIncoming() {
		return _isIncoming;
	}
	/**
	 * @param isIncoming the isIncoming to set
	 */
	public void setIncoming(boolean isIncoming) {
		this._isIncoming = isIncoming;
	}
	/**
	 * @return the messageDate
	 */
	public Date getMessageDate() {
		return _messageDate;
	}
	/**
	 * @param messageDate the messageDate to set
	 */
	public void setMessageDate(Date messageDate) {
		this._messageDate = messageDate;
	}
	/**
	 * @return the status
	 */
	public String getStatus() {
		return _status;
	}
	/**
	 * @param status the status to set
	 */
	public void setStatus(String status) {
		this._status = status;
	}
	/**
	 * @return the subject
	 */
	public String getSubject() {
		return _subject;
	}
	/**
	 * @param subject the subject to set
	 */
	public void setSubject(String subject) {
		this._subject = subject;
	}
	/**
	 * @return the toAddress
	 */
	public String getToAddress() {
		return _toAddress;
	}
	/**
	 * @param toAddress the toAddress to set
	 */
	public void setToAddress(String toAddress) {
		this._toAddress = toAddress;
	}
	public int compareTo(AbstractSFDCObject compareTo) {
		if (compareTo instanceof Email) {
			Email otherEmail = (Email)compareTo;
			int test = compareHelper(this.getSubject(), otherEmail.getSubject());
			if (test == 0) {
				test = compareHelper(this.getMessageDate(), otherEmail.getMessageDate());
			}
			if (test == 0) {
				test = compareHelper(this.getFrom(), otherEmail.getFrom());	
			}
			if (test == 0) {
				test = compareHelper(this.getToAddress(), otherEmail.getToAddress());
			}
			if (test == 0) {
				test = compareHelper(this.getCcAddress(), otherEmail.getCcAddress());
			}
			if (test == 0) {
				test = compareHelper(this.getBccAddress(), otherEmail.getBccAddress());
			}
			return test;
		} else {
			return 1;
		}
	}


	@Override
	protected <T extends AbstractSFDCObject> List<T> getChildren() {
		// TODO Auto-generated method stub
		return null;
	}

}
