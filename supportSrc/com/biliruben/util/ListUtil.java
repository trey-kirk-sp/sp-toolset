package com.biliruben.util;

import java.util.ArrayList;
import java.util.List;

public final class ListUtil {
	
	private ListUtil() {
		//not for you!
	}
	
	public static String listAsString (List list, String delim) {
		StringBuffer line = new StringBuffer();
		for (Object o : list) {
			line.append(o.toString());
			line.append(delim);
		}
		
		line.delete(line.length() - delim.length(), line.length());
		return line.toString();
	}
	
	public static void main(String[] args) {
		ArrayList<String> them = new ArrayList<String>();
		them.add("one");
		them.add("two");
		them.add("three");
		System.out.println(listAsString(them, ","));
	}

}
