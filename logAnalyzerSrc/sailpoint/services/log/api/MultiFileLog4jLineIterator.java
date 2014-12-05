package sailpoint.services.log.api;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Log4j Line Iterator across multiple files.  This class will take in a LayoutPattern and a Log4j log file pattern and iterate
 * through it, one file at a time and one log event at a time.  A log event is usually a single line in the log file.  However, this
 * iterator specifically matches an entire log event to the provided LayoutPattern, which may encompass multiple
 * lines.<br>
 * <br>
 * See also: <a href="http://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/PatternLayout.html">LayoutPattern</a>
 * @author trey.kirk
 * 
 *
 */
public class MultiFileLog4jLineIterator implements Iterable<String> {

	private static Log _log = LogFactory.getLog(MultiFileLog4jLineIterator.class);
	private LineIterator _iterator;

	/**
	 * Standard constructor taking in a file name and layout pattern.  
	 * The log file is immediately opened and the first event read, thus potentially resulting in an IOException
	 * @param fileName
	 * @param layoutPattern
	 * @throws IOException 
	 */
	public MultiFileLog4jLineIterator(File directory, String filePattern, String layoutPattern) throws IOException {
		if (layoutPattern == null) {
			throw new NullPointerException ("Layout pattern cannot be null.");
		}
		DosFileNameFilter filter = new DosFileNameFilter(filePattern);
		_log.debug(filter);
		_iterator = new LineIterator(directory, filter, layoutPattern);	
	}

	public MultiFileLog4jLineIterator(String[] fileNameList, String layoutPattern) throws IOException {
		if (layoutPattern == null) {
			throw new NullPointerException ("Layout pattern cannot be null.");
		}
		_log.debug(fileNameList);
		_iterator = new LineIterator(fileNameList, layoutPattern);
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
		private File _directory;
		private FilenameFilter _filter;
		private List<File> _fileList;
		private Iterator<File> _fileIterator;
		private File _file;

		private LineIterator (File directory, FilenameFilter filenameFilter, String layoutPattern) throws IOException {
			_converter = new Log4jPatternConverter(layoutPattern);
			_fileIterator = getFileList(directory, filenameFilter).iterator();
			getNextReader();
			getNextLine(null);
		}

		private LineIterator (String[] fileNames, String layoutPattern) throws IOException {
			_converter = new Log4jPatternConverter(layoutPattern);
			_fileIterator = getFileList(fileNames).iterator();
			getNextReader();
			getNextLine(null);
		}

		private List<File> getFileList(String[] fileNames) throws IOException {
			_log.debug("Building file list for: " + fileNames);
			List<File> fileList = new ArrayList<File>();
			for (String fileName : fileNames) {
				File f = new File(fileName);
				if (f.isFile() && f.exists()) {
					_log.debug("File: " + f);
					// I got a file, add it
					fileList.add(f);
				} else if (f.isDirectory() && f.exists()) {
					_log.debug("Directory: " + f);
					// I got a directory, add the contents
					File[] contents = f.listFiles();
					_log.debug("Directory contents: " + contents);
					for (File content : contents) {
						if (content.isFile()) {
							fileList.add(content);
						}
					}
				} else {
					// Didn't get a file, didn't get a directory.  Try a FilenameFilter
					_log.debug("Filter: " + f);
					String directoryName = f.getParent();
					_log.debug("Direcotry: " + directoryName);
					File directory = null;
					if (directoryName != null) {
						directory = new File(directoryName);
					} else {
						directory = new File(".");
					}
					FilenameFilter filenameFilter = new DosFileNameFilter(f.getName());
					File[] files = directory.listFiles(filenameFilter);
					_log.debug("Files from filter: " + files);
					if (files != null) {
						for (File listFile : files) {
							fileList.add(listFile);
						}
					}
				}
			}

			return orderFiles(fileList);
		}

		/*
		 * This method will first generate a list of files that match the filter.  Secondly, it will read the first
		 * log event of each file and order the files based on log events.
		 */
		private List<File> getFileList(File directory,
				FilenameFilter filenameFilter) throws IOException {
			File[] files = directory.listFiles(filenameFilter);
			List<File> filteredList = Arrays.asList(files);

			return orderFiles(filteredList);
		}

		private List<File> orderFiles(List<File> files) throws IOException {
			_log.debug("Ordering file list: " + files);
			Map<Date, File> fileMap = new HashMap<Date, File>();
			for (File tryFile : files) {
				try {
					BufferedReader reader = new BufferedReader(new FileReader(tryFile));
					boolean found = true;
					while (found && reader.ready()) {
						Date d = getFirstDate(reader);
						_log.debug("File: " + tryFile + " :: " + d);
						if (d != null) {
							File f = fileMap.get(d);
							if (f == null) {
								found = false;
								fileMap.put(d, tryFile);
							}
						}
					}

				} catch (FileNotFoundException e) {
					// This shouldn't happen
					e.printStackTrace();
					_log.error(e);
				}
			}

			List<File> orderedList = new ArrayList<File>();
			Set<Date> orderedSet = new TreeSet<Date>(fileMap.keySet());
			for (Date d : orderedSet) {
				orderedList.add(fileMap.get(d));
			}
			_log.debug("Ordered List: " + orderedList);
			return orderedList;

		}
		private Date getFirstDate(BufferedReader reader) throws IOException {
			while (reader.ready()) {
				Pattern linePattern = _converter.getLinePattern();
				String line = reader.readLine();
				if (linePattern.matcher(line).matches()) {
					_converter.setLogEvent(line);
					return _converter.parseDate();
				}
			}
			return null;
		}

		private String ltrim (String text) {
		    if (text == null) {
		        return text;
		    }
		    int i = 0;
		    while (i < text.length() && Character.isWhitespace(text.charAt(i))) {
		        i++;
		    }
		    return text.substring(i);
		}

		private void getNextLine(StringBuffer buff) throws IOException {

			boolean complete = false;
			_currentLine = null; // null it out to start
			if (_lastLine == null) {
				_lastLine = "";
			}
			if (buff == null) {
				buff = new StringBuffer(_lastLine);
			}

			Pattern linePattern = _converter.getLinePattern();
			while (_reader != null && _reader.ready() && !complete) {
				String line = _reader.readLine();
				if (line == null) {
					complete = true;
				} else {
				    // left trim it
				    // why are we trimming?
				    //line = ltrim(line);
				}
				    
				    if (linePattern.matcher(line).matches()) {
					// this is the next line
					_currentLine = buff.toString();
					_lastLine = line;
					complete = true;
				} else {
					// multi-line event, keep appending
					buff.append("\n" + line);
				}
			}

			if (!complete) {
				// here because the reader ran out
				getNextReader();
				if (_reader == null) {
					// nothing left to do
					complete = true;
				} else {
					getNextLine(buff);
				}
			}
		}

		private void getNextReader() throws FileNotFoundException {
			if (_fileIterator.hasNext()) {
				_file = _fileIterator.next();
				_log.debug("Next file: " + _file);
				_reader = new BufferedReader(new FileReader(_file));
			} else {
				_reader = null;
				_log.debug("No next file");
			}
		}


		public boolean hasNext() {
			try {
				getNextLine(null);
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

	/** Test method 
	 * @throws IOException **/

	public static void main (String[] args) throws IOException {
		File directory = new File("c:\\tmp\\test");
		String namePattern = "rolling*";
		MultiFileLog4jLineIterator tester = new MultiFileLog4jLineIterator(directory, namePattern, "%d{ISO8601} %5p %t %c{4}:%L - %m%n");
		LineIterator iter = (LineIterator) tester.iterator();
		while (iter.hasNext()) {
			String next = iter.next();
			System.out.print(iter._file + ":::");
			System.out.println(next);

		}
	}

}
