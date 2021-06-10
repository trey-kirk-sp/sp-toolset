package com.biliruben.tools.txt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import com.biliruben.util.GetOpts;
import com.biliruben.util.OptionLegend;
import com.biliruben.util.OptionParseException;

/**
 * Text utility based off of the *nix command 'head' which acts as the counterpart to 'tail'.  This utility will
 * display the first 10 lines of a text file by default.  It also has the additional feature of specifying an arbitrary
 * starting line allowing the user to display any sequential lines of text in a given text file.<br>
 * <br>
 * Usage:<br>
 * Head [-lines lines] [-start start] fileName<br>
 * <br>
 * -lines: Total number of lines to display.  Default value is 10<br>
 * -start: Begin displaying text at this line.  By default, it starts at line 1<br>
 * fileName: File to display selected lines.<br>
 * <br>
 * @author trey.kirk
 *
 */
public class Head {

	protected static final String OPT_FILE_NAME = "fileName";
	protected static final String OPT_START_LINE = "start";
	protected static final String OPT_TOTAL_LINES = "lines";

	protected static final int DEFAULT_START_LINE = 1;
	protected static final int DEFAULT_TOTAL_LINES = 10;

	private GetOpts opts;
	private int startAt = DEFAULT_START_LINE;
	private int totalLines = DEFAULT_TOTAL_LINES;
	private String fileName;

	/**
	 * Main method
	 * @param args
	 */
	public static void main(String[] args) {

		Head head = new Head();
		try {
			head.init(args);
		} catch (OptionParseException e) {
			// TODO Auto-generated catch block
			
			head.genUsage(e);
			System.exit(1);
		}
		try {
			head.viewTextWindow();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			head.genUsage(null);
			System.exit(2);
		}	
	}

	/**
	 * Once the main attributes have been parsed, viewTextWindow does the actual work.
	 * @throws IOException
	 */
	public void viewTextWindow() throws IOException {
		File inFile = new File(fileName);
		BufferedReader inReader = new BufferedReader(new FileReader(inFile));

		//"look ahead" to starting point
		for (int i = 1; i < startAt; i++) {
			inReader.readLine();
		}

		String line;
		for (int i = 1; i <= totalLines; i++) {
			line = inReader.readLine();
			if (line != null) {
				System.out.println(line);
			}
		}

		line = inReader.readLine();
		if (line == null) {
			System.out.println("[EOF reached]");
		}
	}

	/**
	 * Generates the usage for Head.  I did deviate from my usual practice an integrated GetOpts into this object.  Typically
	 * I leave the GetOpts activities for the static methods opearting out of main().  Turns out it's easier this way.
	 * @param e - an OptionParseException.  We just pass it to {@link GetOpts#genUsage(OptionParseException))}.
	 */
	public void genUsage(OptionParseException e) {
		System.err.print(opts.genUsage(e));
	}

	/**
	 * This ferrets out our provided options.  Since it takes an array of String, it's expected use from a static main method
	 * is something like:<br>
	 * public static void main (String[] args) {<br>
	 * Head h = new Head();<br>
	 * h.init(args);<br>
	 * }<br>
	 * 
	 * Easy peasy.
	 * @see GetOpts
	 * @param args
	 * @throws OptionParseException
	 * 
	 */
	public void init(String[] args) throws OptionParseException {
		
		// Process our options using GetOpts
		opts = new GetOpts(this.getClass());
		opts.setUsageTail(OPT_FILE_NAME);
		opts.setDescriptionTail(OPT_FILE_NAME + ": File to display selected lines.\n");

		OptionLegend legend = new OptionLegend(OPT_START_LINE);
		legend.setRequired(false);
		legend.setDescription("Begin displaying text at this line.  By default, it starts at line " + DEFAULT_START_LINE);
		opts.addLegend(legend);

		legend = new OptionLegend(OPT_TOTAL_LINES);
		legend.setRequired(false);
		legend.setDescription("Total number of lines to display.  Default value is " + DEFAULT_TOTAL_LINES);
		opts.addLegend(legend);

		opts.setOpts(args);
		// The only required option is an unswitched one
		ArrayList<String> unswitched = opts.getUnswitchedOptions();
		if (unswitched == null || unswitched.size() != 1) {
			// bad
			StringBuffer msg = new StringBuffer("Invalid filename provided: ");
			if (unswitched == null || unswitched.size() == 0) {
				msg.append("no filename given.\n");
			} else {
				for (String filename : unswitched) {
					msg.append(filename + " ");
				}
			}

			throw new OptionParseException(msg + "\n");
		}
		
		// Options parsed, now validate.
		// Since a parse error could be thrown for a number of reasons, just use a boolean to keep track.
		boolean parseError = false;
		fileName = unswitched.get(0);

		// Get starting line
		try {
			if (opts.getStr(OPT_START_LINE) != null) {
				startAt = Integer.valueOf(opts.getStr(OPT_START_LINE));
			}
		} catch (NumberFormatException e) {
			parseError = true;
		}

		if (startAt < 1) {
			parseError = true;
		}

		if (parseError) {
			throw new OptionParseException ("Invalid start line provided (must be an integer greater than 0): " + opts.getStr(OPT_START_LINE));
		}

		// Get total lines
		parseError = false;
		try {
			if (opts.getStr(OPT_TOTAL_LINES) != null) {
				totalLines = Integer.valueOf(opts.getStr(OPT_TOTAL_LINES));
			}
		} catch (NumberFormatException e) {
			parseError = true;
		}

		if (totalLines < 1) {
			parseError = true;
		}

		if (parseError) {
			throw new OptionParseException ("Invalid start line provided (must be an integer greater than 0): " + opts.getStr(OPT_TOTAL_LINES));
		}
	}
}
