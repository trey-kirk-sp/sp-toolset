package sailpoint.salesforce.reports.object;

import java.util.Date;
import java.util.List;

public class CaseComment extends AbstractSFDCObject {

	private String _comments;
	private Date _createDate;
	private String _createdBy;
	private String _createdByRole;
	private Date _lastModifiedDate;
	private String _lastModifiedBy;
	private boolean _isPublic;

	/**
	 * @return the comments
	 */
	public String getComments() {
		return _comments;
	}
	/**
	 * @param comments the comments to set
	 */
	public void setComments(String comments) {
		this._comments = comments;
	}
	/**
	 * @return the createDate
	 */
	public Date getCreateDate() {
		return _createDate;
	}
	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this._createDate = createDate;
	}
	/**
	 * @return the createdBy
	 */
	public String getCreatedBy() {
		return _createdBy;
	}
	/**
	 * @param createdBy the createdBy to set
	 */
	public void setCreatedBy(String createdBy) {
		this._createdBy = createdBy;
	}
	/**
	 * @return the createdByRole
	 */
	public String getCreatedByRole() {
		return _createdByRole;
	}
	/**
	 * @param createdByRole the createdByRole to set
	 */
	public void setCreatedByRole(String createdByRole) {
		this._createdByRole = createdByRole;
	}
	/**
	 * @return the isPublic
	 */
	public boolean isPublic() {
		return _isPublic;
	}
	/**
	 * @param isPublic the isPublic to set
	 */
	public void setPublic(boolean isPublic) {
		this._isPublic = isPublic;
	}
	/**
	 * @return the lastModifiedBy
	 */
	public String getLastModifiedBy() {
		return _lastModifiedBy;
	}
	/**
	 * @param lastModifiedBy the lastModifiedBy to set
	 */
	public void setLastModifiedBy(String lastModifiedBy) {
		this._lastModifiedBy = lastModifiedBy;
	}
	/**
	 * @return the lastModifiedDate
	 */
	public Date getLastModifiedDate() {
		return _lastModifiedDate;
	}
	/**
	 * @param lastModifiedDate the lastModifiedDate to set
	 */
	public void setLastModifiedDate(Date lastModifiedDate) {
		this._lastModifiedDate = lastModifiedDate;
	}

	public int compareTo(AbstractSFDCObject compareTo) {

		if (compareTo instanceof CaseComment) {
			CaseComment otherComment = (CaseComment)compareTo;
			// The order of comparison is:
			// Compare the create date, if they match
			//		Compare the create dates, if they match
			//			
			
			int test = compareHelper (this.getCreateDate(), otherComment.getCreateDate());
			if (test == 0) {
				test = compareHelper (this.getLastModifiedDate(), otherComment.getLastModifiedDate());
				if (test ==0) {
					test = compareHelper (this.getComments(), otherComment.getComments());
				}
				return test;
			}
			return test;
		} else {
			return 1;
		}

	}

	@Override
	public boolean equals (Object otherComment) {
		if (otherComment instanceof CaseComment) {
			CaseComment compareTo = (CaseComment)otherComment;
			
			return (isEqual(compareTo.getComments(), this.getComments()) &&
					isEqual(compareTo.getCreateDate(), this.getCreateDate()) &&
					isEqual(compareTo.getLastModifiedDate(), this.getLastModifiedDate()) &&
					isEqual(compareTo.getCreatedBy(), this.getCreatedBy()));			
		}
		
		return false;
	}
	
	@Override
	public int hashCode() {
		int code = 0;
		code += hashHelper(this.getComments(), 3);
		code += hashHelper(this.getCreateDate(), 5);
		code += hashHelper(this.getLastModifiedDate(), 7);
		code += hashHelper(this.getCreatedBy(), 11);
		return code;
	}
	@Override
	protected <T extends AbstractSFDCObject> List<T> getChildren() {
		// TODO Auto-generated method stub
		return null;
	}

	public String toString() {
		StringBuffer comment = new StringBuffer(getCreateDate().toString());
		comment.append(": " + getComments() + "\n");
		return comment.toString();
	}

}
