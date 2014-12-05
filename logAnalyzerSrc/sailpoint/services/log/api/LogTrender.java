package sailpoint.services.log.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.biliruben.util.csv.CSVRecord;
import com.biliruben.util.csv.CSVUtil;

/**
 * Trends the number of calls per method over time.  Parameters:
 * - Time slice granularity (hourly, minutely, daily?, abstract)
 * - layout pattern 
 */
public class LogTrender extends AbstractTraceAspectLogAnalyzer {

	/*
	 * Helper class to house method information
	 */
	private class MethodTrender {

		/*
		 * Call map is key value pairs pairing a Date representing a time segment and the number of times the method
		 * was called during the time segment.
		 */
		private Map<Date, Integer> _callMap;
		
		// The method describe here in
		private String _method;

		public MethodTrender (String method) {
			setMethod (method);
			_callMap = new HashMap<Date, Integer>();
		}

		public void addCall (Date dateCalled) {
			// our slice value will tell us what date value to
			// 'round down' to
			long time = dateCalled.getTime();
			long mod = time % _slice;
			Date callDate = new Date(time - mod); // this is our map key
			Integer currentCalls = _callMap.get(callDate);
			if (currentCalls == null) {
				_callMap.put(callDate, 1);
			} else {
				currentCalls++; // it's an object, right? no need to add back
				_callMap.put(callDate, currentCalls);
			}
		}

		public Collection<Date> getDates() {
			return new ArrayList<Date>(_callMap.keySet());
		}

		/**
		 * Builds a map compatible with our CSVRecord object
		 * @return
		 */
		public Map<String, String> getMap() {
			Map<String, String> map = new HashMap<String, String>();
			for (Date d : getDates()) {
				Integer calls = _callMap.get(d);
				if (calls == null) {
					calls = 0;
				}
				map.put(d.toString(), String.valueOf(calls));
			}
			map.put(MAP_METHOD_NAME, _method);
			return map;
		}

		public void setMethod (String method) {
			_method = method;
		}

	}
	
	public static final long DEFAULT_TIME_SLICE = 60 * 60 * 1000; // 1 hour in milliseconds
	private static final String MAP_METHOD_NAME = "method";
	private long _slice;

	private Map<String, MethodTrender> _trenders;

	/**
	 * Default constructor
	 */
	public LogTrender() {
		this((String)null, DEFAULT_TIME_SLICE);
	}

	public LogTrender(String layoutPattern) {
		this (layoutPattern, DEFAULT_TIME_SLICE);
	}

	/**
	 * Constructor taking in a specific Log4j LayoutPattern
	 * @param layoutPattern
	 */
	public LogTrender(String layoutPattern, long timeSlice) {
		super(layoutPattern);
		_slice = timeSlice;
		_trenders = new HashMap<String, MethodTrender>();
	}

	/**
	 * For each incoming logEvent, extract the method name and 
	 * date exited.  Build our trend data from that.
	 */
	@Override
	public boolean addLogEvent(String logEvent) {
		super.addLogEvent(logEvent);
		if (isExiting()) { // only trend exists
			String method = getMethod();
			Date date = getDate();
			MethodTrender trender = _trenders.get(method);
			if (trender == null) {
				trender = new MethodTrender(method);
				_trenders.put(method, trender);
			}
			trender.addCall(date);
		}
		return true;
	}

	/**
	 * Returns a String representing a CSV of method calls over segments of time
	 */
	public String compileSummary() {
		// Our output will be CSV. I think column names should be the time stamp, representing 'over time'
		// so.... how do we do this?
		//
		// iterate over all of the MethodTrenders, get the 'keys', aka dates
		// shove the dates in a SortedSet.  We now have our finite headers for our CSV
		// we have to define the headers to ensure they're in order.
		// header will need to be string values
		//
		// To pass over the data three times is inefficient.  Find a better way.
		Set<Date> trenderDates = new TreeSet<Date>();
		for (MethodTrender trender : _trenders.values()) {
			trenderDates.addAll(trender.getDates()); // adding them as dates gives me auto-sort
		}

		List<String> headerList = new ArrayList<String>();
		//String[] header = new String[trenderDates.size() + 1];
		headerList.add(MAP_METHOD_NAME);
		//int i = 1;
		// we have to refactor this as it will leave time window gaps (i.e., if nothing is called in a certain 5 second windo, that column goes missing)
		// For linear examination, I need to know the void columns as well as the data columns.
		// Iterator<Date> dateIter = trenderDates.iterator();
		/*
	      while (dateIter.hasNext()) {
	            Date d = dateIter.next();
	            header[i] = d.toString();
	            i++;
	        }
        */
		// instead of using the iterator, we'll use the earliest and latest dates as our headers using _timeslice as our incrementer. Since our data
		// map keys are Date objects, I'll need to construct my own Dates, test if a matching Date is used as a key, and use the key Date when it does
		Date[] dateArray = trenderDates.toArray(new Date[trenderDates.size()]);
		long begin = dateArray[0].getTime();
		long end = dateArray[dateArray.length - 1].getTime();
		for (long time = begin; time <= end; time += _slice) {
		    Date d = new Date(time);
		    headerList.add(d.toString());
		}
		
		String[] header = headerList.toArray(new String[headerList.size()]);
		// Create a CSVRecord to store our data in ready-to-CSV format
		CSVRecord record = new CSVRecord(header);
		record.setNullValue("0");
		for (MethodTrender trender : _trenders.values()) {
			record.addLine(trender.getMap());
		}

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			CSVUtil.exportToCsv(record, out);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		return out.toString();
	}
}
