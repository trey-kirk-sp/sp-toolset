package sailpoint.salesforce.reports.object;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Calendar;

import com.biliruben.util.ArrayUtil;

public class StatusHistory {

	private String _status;
	private Date[] _statusEntered;
	private Date[] _statusExited;
	private int _timeInStatus;
	private Case _case;
	private int _timeFactor;
	
	public static final int TIME_FACTOR_MILLI = 1;
	public static final int TIME_FACTOR_SECONDS = 1000;
	public static final int TIME_FACTOR_MINUTES = 60 * TIME_FACTOR_SECONDS;
	public static final int TIME_FACTOR_HOURS = 60 * TIME_FACTOR_MINUTES;
	public static final int TIME_FACTOR_DAYS = 24 * TIME_FACTOR_HOURS; 

	public StatusHistory(Case theCase, String status) {
		setStatus(status);
		setCase(theCase);
		setTimeFactor(TIME_FACTOR_HOURS);
	}

	public void setTimeFactor(int factor) {
		_timeFactor = factor;
	}
	
	public int getTimeFactor() {
		return _timeFactor;
	}
	
	public Case getCase() {
		return _case;
	}

	public void setCase (Case theCase) {
		_case = theCase;
	}

	private Date[] pruneNulls (Date[] dates) {
		ArrayList<Date> dateList = new ArrayList<Date>();
		if (dates != null) {
			for (Date date : dates) {
				if (date != null) {
					dateList.add(date);
				}
			}
			Date[] newDates;
			if (dateList.size() != dates.length) {
				newDates = new Date[dateList.size()];
				for (int i = 0; i < dateList.size(); i++) {
					newDates[i] = dateList.get(i);
				}
			} else {
				newDates = dates;
			}
			return newDates;
		} else {
			return new Date[0];
		}
	}

	public static final String STATUS_CLOSED = "Closed";

	/**
	 * Returns the number of seconds in the status
	 * @return
	 */
	public float timeInStatus() {
		float totalTime = 0;

		_statusEntered = pruneNulls(_statusEntered);
		_statusExited = pruneNulls(_statusExited);

		// If this is a Closed status, we could not assume a "date exited" as some cases may have been reopened (and thus
		// actually had a "date exited".  So the assumption now is that with the object built and 'timeInStatus()' being called,
		// all relevant status history has been aggregated to it.  Now we can check and see if this status is missing the "closed"
		// exit date.

		if (getStatus().equals(STATUS_CLOSED)) {
			if (_statusExited == null || _statusExited.length == 0) {
				_statusExited = new Date[1];
				_statusExited[0] = new Date();
			} else if (_statusExited.length == _statusEntered.length - 1) {
				// one element is missing, that's the final closed exit date.  It should be the closed date
				// or today's date, depending on if the case is closed or not
				Date[] tmpDates = new Date[_statusExited.length + 1];
				System.arraycopy(_statusExited, 0, tmpDates, _statusExited.length - 1, _statusExited.length);
				tmpDates[tmpDates.length - 1] = new Date();
				_statusExited = tmpDates;
			}
		} else {

			// Occasionally, a case is created and never set to 'New'.  So we check and see if the _statusEntered dates
			// are one off of the _exited.
			if (_statusEntered == null || _statusEntered.length == 0) {
				_statusEntered = new Date[1];
				_statusEntered[0] = getCase().getDateOpened();
			} else if (_statusEntered.length == _statusExited.length - 1) {
				// one element off, insert one for the case create date
				Date[] tmpDates = new Date[_statusEntered.length + 1];
				System.arraycopy(_statusEntered, 0, tmpDates, _statusEntered.length - 1, _statusEntered.length);
				tmpDates[tmpDates.length - 1] = getCase().getDateOpened();
				_statusEntered = tmpDates;
			}

		}
		Calendar calCulator = Calendar.getInstance();
		if (_statusEntered == null || _statusEntered.length == 0) {
			_statusEntered = new Date[1];
			_statusEntered[0] = getCase().getDateOpened();

		}
		if (_statusExited == null || _statusExited.length == 0) {
			_statusExited = new Date[1];
			if (getCase().getDateClosed() == null) {
				getCase().setDateClosed(new Date());
			}
			_statusExited[0] = getCase().getDateClosed();
		}

		Arrays.sort(_statusEntered);
		Arrays.sort(_statusExited);
		for (int i = 0; i < _statusEntered.length; i++) {
			calCulator.setTime(_statusEntered[i]);
			float startTime = calCulator.getTimeInMillis();

			calCulator.setTime(_statusExited[i]);
			float stopTime = calCulator.getTimeInMillis();
			float diffTime = (stopTime - startTime) / getTimeFactor();
			totalTime += diffTime;
		}
		return totalTime;		

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
	 * @return the statusEntered
	 */
	public Date[] getStatusEntered() {
		return _statusEntered;
	}
	/**
	 * @param statusEntered the statusEntered to set
	 */
	public void addStatusEntered(Date statusEntered) {
		Date[] newDates;
		if (_statusEntered == null) {
			newDates = new Date[1];
		} else {
			newDates = new Date[_statusEntered.length + 1];
			System.arraycopy(_statusEntered, 0, newDates, _statusEntered.length - 1, _statusEntered.length);
		}
		newDates[newDates.length - 1] = statusEntered;
		this._statusEntered = newDates;
	}
	/**
	 * @return the statusExisted
	 */
	public Date[] getStatusExited() {
		return _statusExited;
	}
	/**
	 * @param statusExisted the statusExisted to set
	 */
	public void addStatusExited(Date statusExited) {
		Date[] newDates;
		if (_statusExited == null) {
			newDates = new Date[1];
		} else {
			newDates = new Date[_statusExited.length + 1];
			System.arraycopy(_statusExited, 0, newDates, _statusExited.length - 1, _statusExited.length);
		}
		newDates[newDates.length - 1] = statusExited;
		this._statusExited = newDates;
	}

	public String toString() {
		return getStatus();
	}



}
