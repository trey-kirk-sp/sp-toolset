package sailpoint.services.log.api;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Pattern;

/**
 * Log4j Line Iterator.  This class will take in a LayoutPattern and a Log4j log file and iterate
 * through it, one log event at a time.  A log event is usually a single line in the log file.  However, this
 * iterator specifically matches an entire log event to the provided LayoutPattern, which may encompass multiple
 * lines.<br>
 * <br>
 * See also: <a href="http://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/PatternLayout.html">LayoutPattern</a>
 * @author trey.kirk
 * 
 *
 */
public class Log4jLineIterator implements Iterable<String> {

	private LineIterator _iterator;

	/**
	 * Standard constructor taking in a file name and layout pattern.  
	 * The log file is immediately opened and the first event read, thus potentially resulting in an IOException
	 * @param fileName
	 * @param layoutPattern
	 * @throws IOException 
	 */
	public Log4jLineIterator(String fileName, String layoutPattern) throws IOException {
		this (new File(fileName), layoutPattern);
	}

	/**
	 * Standard constructor taking in a File and layout pattern.  
	 * The log file is immediately opened and the first event read, thus potentially resulting in an IOException
	 * @param file
	 * @param layoutPattern
	 * @throws IOException
	 */
	public Log4jLineIterator(File file, String layoutPattern) throws IOException {
		this (new BufferedReader(new FileReader(file)), layoutPattern);
	}

	/**
	 * Standard constructor taking in a BufferedReader and layout pattern.  All constructors eventually lead to this one.  
	 * The log file is immediately opened and the first event read, thus potentially resulting in an IOException
	 * @param reader
	 * @param layoutPattern
	 * @throws IOException
	 */
	public Log4jLineIterator(BufferedReader reader, String layoutPattern) throws IOException {
		if (layoutPattern == null) {
			throw new NullPointerException ("Layout pattern cannot be null.");
		}
		_iterator = new LineIterator(reader, layoutPattern);
	}

	/*
	 * Internal iterator. This is the real class.  I suppose I could've made Log4jLineIterator an
	 * implementation of Iterable and Iterator, but this works just as well.
	 */
	private class LineIterator implements Iterator<String> {

		private BufferedReader _reader;
		private Log4jPatternConverter _converter;
		private String _currentLine;
		private String _lastLine;

		private LineIterator (BufferedReader reader, String layoutPattern) throws IOException {
			_reader = reader;
			_converter = new Log4jPatternConverter(layoutPattern);
			getNextLine();
		}

		private void getNextLine() throws IOException {
			boolean complete = false;
			
			_currentLine = null; // null it out to start
			if (_lastLine == null) {
				_lastLine = "";
			}
			StringBuffer buff = new StringBuffer(_lastLine);
			Pattern linePattern = _converter.getLinePattern();
			while (_reader.ready() && !complete) {
				String line = _reader.readLine();
				if (line == null) {
					complete = true;
				} else if (linePattern.matcher(line).matches()) {
					// this is the next line
					_currentLine = buff.toString();
					_lastLine = line;
					complete = true;
				} else {
					// multi-line event, keep appending
					buff.append("\n" + line);
				}
			}
		}


	public boolean hasNext() {
		try {
			getNextLine();
		} catch (IOException e) {

			throw new RuntimeException(e);
		}
		return (_currentLine != null);
	}

	public String next() {
		return _currentLine;
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}



}

public Iterator<String> iterator() {
	return _iterator;
}

}
