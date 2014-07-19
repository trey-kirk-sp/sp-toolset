/**
 * 
 */
package com.biliruben.util;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * @author trey.kirk
 *
 */
@SuppressWarnings("serial")
public class OptionParseException extends RuntimeException {

	
	private boolean _suppressStackTrace;

	public OptionParseException(String message, GetOpts opts) {
		this (message, opts, true);
	}
	
	
	public OptionParseException(String message, GetOpts opts, boolean showUsageOnly) {
		if (showUsageOnly) {
			System.err.println(message + opts.genUsage() + "\n");
			_suppressStackTrace = true;
		} else {
			System.err.println(message);
			_suppressStackTrace = false;
		}
	}


	@Override
	public void printStackTrace() {
		// Suppress stack trace
		if (!_suppressStackTrace) {
			super.printStackTrace();
		}
	}
	
	@Override
	public void printStackTrace(PrintStream s) {
		// Suppress stack trace
		if (!_suppressStackTrace) {
			super.printStackTrace(s);
		}
	}
	
	@Override
	public void printStackTrace(PrintWriter s) {
		// Suppress stack trace
		if (!_suppressStackTrace) {
			super.printStackTrace(s);
		}
	}

}
