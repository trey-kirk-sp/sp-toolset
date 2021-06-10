package sailpoint.salesforce.reports.object;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class Case extends AbstractSFDCObject {

	public static int CASE_AGE_UNSET = -1;

	private String _caseNumber;
	private String _account;
	private Date _dateOpened;
	private Date _dateClosed;
	private String _caseReason;
	private String _caseOwner;
	private String _subject;
	private String _status;
	private String _type;
	private int _age;
	private String _priority;
	private String _description;
	private String _closeComment;

	
	public void setCloseComment(String closeComment) {
		_closeComment = closeComment;
	}
	
	public String getCloseComment() {
		return _closeComment;
	}
	
	public void setDescription(String description) {
		_description = description;
	}
	
	public String getDescription() {
		return _description;
	}

	private Set<CaseComment> _comments;

	private Map<String, StatusHistory> _statusHistoryMap = new HashMap<String, StatusHistory>();

	public Case() {
		setAge(String.valueOf(CASE_AGE_UNSET));
	}
	public Case (String caseNumber) {
		this();
		_caseNumber = caseNumber;
	}
	public void addStatusHistory (StatusHistory status) {
		_statusHistoryMap.put(status.getStatus(), status);
	}

	public void addComment(CaseComment comment) {
		if (_comments == null) {
			_comments = new TreeSet<CaseComment>(); 
		}
		_comments.add(comment);
	}

	public void removeComment (CaseComment comment) {
		_comments.remove(comment);
	}

	public Set<CaseComment> getComments () {
		return _comments;
	}
	
	public List<CaseComment> getCommentsAsList () {
		return new ArrayList<CaseComment> (getComments());
	}
	
	public CaseComment[] getCommentsAsArray() {
		ArrayList<CaseComment> aList = new ArrayList<CaseComment>(getComments());
		int length = aList.size();
		return aList.toArray(new CaseComment[length]);
	}

	/**
	 * @return the account
	 */
	public String getAccount() {
		return _account;
	}
	/**
	 * @param account the account to set
	 */
	public void setAccount(String account) {
		this._account = account;
	}
	/**
	 * @return the caseNumber
	 */
	public String getCaseNumber() {
		return _caseNumber;
	}
	/**
	 * @param caseNumber the caseNumber to set
	 */
	public void setCaseNumber(String caseNumber) {
		this._caseNumber = caseNumber;
	}
	/**
	 * @return the dateClosed
	 */
	public Date getDateClosed() {
		return _dateClosed;
	}
	/**
	 * @param date the dateClosed to set
	 */
	public void setDateClosed(Date date) {
		this._dateClosed = date;
	}
	/**
	 * @return the dateOpened
	 */
	public Date getDateOpened() {
		return _dateOpened;
	}
	/**
	 * @param date the dateOpened to set
	 */
	public void setDateOpened(Date date) {
		this._dateOpened = date;
	}
	/**
	 * @return the statusHistory
	 */
	public Map<String, StatusHistory> getStatusHistory() {
		return _statusHistoryMap;
	}
	/**
	 * @param statusHistory the statusHistory to set
	 */
	public void setStatusHistory(Map<String, StatusHistory> statusHistory) {
		this._statusHistoryMap = statusHistory;
	}

	public String toString() {
		return getCaseNumber();
	}
	/**
	 * @return the caseReason
	 */
	public String getCaseReason() {
		return _caseReason;
	}
	/**
	 * @param caseReason the caseReason to set
	 */
	public void setCaseReason(String caseReason) {
		this._caseReason = caseReason;
	}
	/**
	 * @return the age
	 */
	public int getAge() {
		return _age;
	}
	/**
	 * @param age the age to set
	 */
	public void setAge(String age) {
		if (age != null) {
			try {
				this._age = (int)Float.valueOf(age).longValue();
			} catch (NumberFormatException e) {

				this._age = CASE_AGE_UNSET;
			}
		} else {
			this._age = CASE_AGE_UNSET;
		}
	}
	/**
	 * @return the _caseOwner
	 */
	public String getCaseOwner() {
		return _caseOwner;
	}
	/**
	 * @param owner the _caseOwner to set
	 */
	public void setCaseOwner(String owner) {
		_caseOwner = owner;
	}
	/**
	 * @return the _status
	 */
	public String getStatus() {
		return _status;
	}
	/**
	 * @param _status the _status to set
	 */
	public void setStatus(String status) {
		this._status = status;
	}
	/**
	 * @return the _subject
	 */
	public String getSubject() {
		return _subject;
	}
	/**
	 * @param _subject the _subject to set
	 */
	public void setSubject(String subject) {
		this._subject = subject;
	}
	/**
	 * @return the _type
	 */
	public String getType() {
		return _type;
	}
	/**
	 * @param _type the _type to set
	 */
	public void setType(String type) {
		this._type = type;
	}
	/**
	 * @return the priority
	 */
	public String getPriority() {
		return _priority;
	}
	/**
	 * @param priority the priority to set
	 */
	public void setPriority(String priority) {
		this._priority = priority;
	}
	public int compareTo(AbstractSFDCObject compareTo) {
		if (compareTo instanceof Case) {
			Case yourCase = (Case)compareTo;
			String yourCaseNumberStr = yourCase.getCaseNumber();
			int yourCaseNumber =  (yourCaseNumberStr != null ? Integer.getInteger(yourCaseNumberStr) : 0);
			int myCaseNumber = (getCaseNumber() != null ? Integer.getInteger(getCaseNumber()) : 0);
			if (myCaseNumber > yourCaseNumber) {
				return 1;
			} else if (myCaseNumber < yourCaseNumber) {
				return -1;
			}
			return 0;
		}

		return 1;
	}
	@Override
	protected <T extends AbstractSFDCObject> List<T> getChildren() {
		// TODO Auto-generated method stub
		Set<CaseComment> comments = getComments();
		List<T> myKids = new ArrayList(comments);
		//Case does not yet support a case hierarchy.  When that happens, we'll have to include
		//sub-cases as well.
		return myKids;
	}
	
	public static Case findCase(Collection<AbstractSFDCObject> allCases, String caseNumber) {
		Iterator<AbstractSFDCObject> it = allCases.iterator();
		Case nextCase = new Case("-1");
		while (it.hasNext()) {
			AbstractSFDCObject nextObj = it.next();

			if (nextObj instanceof Case) {
				nextCase = (Case)nextObj;
			}
			if (nextCase.getCaseNumber().equals(caseNumber)) {
				return nextCase;
			}
		}
		return null;
	}
}
