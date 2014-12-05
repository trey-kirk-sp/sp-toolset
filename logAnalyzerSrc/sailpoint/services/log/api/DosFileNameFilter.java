package sailpoint.services.log.api;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;

/**
 * Basic DOS pattern uses ? as a single wild character and * as 0 or more wild characters.  All other
 * characters are treated as verbatum.
 * @author trey.kirk
 *
 */
public class DosFileNameFilter implements FilenameFilter {

	private Pattern _pattern;

	public DosFileNameFilter (String filePattern) {
		// need to quote non * and ?
		int start = 0;
		int starEnd = filePattern.indexOf("*");
		int questEnd = filePattern.indexOf("?");
		int end = getLowestValid(starEnd, questEnd);
		
		StringBuffer quotedText = new StringBuffer();
		while (end != -1) {
			// found a string to 'quote'
			quotedText.append("\\Q");
			quotedText.append(filePattern.substring(start, end));
			quotedText.append("\\E");
			quotedText.append(filePattern.substring(end, end + 1));
			start = end + 1;
			starEnd = filePattern.indexOf("*", start);
			questEnd = filePattern.indexOf("?", start);
			end = getLowestValid(starEnd, questEnd);
		}

		if (start < filePattern.length()) {
			quotedText.append("\\Q" + filePattern.substring(start, filePattern.length()) + "\\E");
		}

		String regex = quotedText.toString().replaceAll("\\*", ".*");
		regex = regex.replaceAll("\\?", ".");
		_pattern = Pattern.compile(regex);
	}

	private int getLowestValid(int starEnd, int questEnd) {
		
		if (starEnd < 0 && questEnd < 0) {
			return -1;
		} else if (starEnd < 0) {
			return questEnd;
		} else if (questEnd < 0) {
			return starEnd;
		} else {
			return (questEnd < starEnd ? questEnd : starEnd);
		}
	}

	public boolean accept(File dir, String name) {
		return _pattern.matcher(name).matches();
	}
	
	@Override
	public String toString() {
		return _pattern.toString();
	}
}
