package com.biliruben.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class to do more Array stuff that java.util.Arrays don't
 * @author trey.kirk
 *
 */
public class ArrayUtil {
	
	
	public static <T> T[] insert (T[] array, T obj, int pos) {
		int size;
		if (pos > array.length - 1) {
			size = pos + 1;
		} else {
			size = array.length + 1;
		}
		
		T[] a = array.clone();
        array = (T[])java.lang.reflect.Array.
		newInstance(array.getClass().getComponentType(), size);
        int shift = 0;
        for (int i = 0; i < array.length; i++) {
        	if (i == pos) {
        		array[i] = obj;
        		shift = 1;
        	} else {
        		T next;
        		if (i - shift >= a.length) {
        			next = (T)null;
        		} else {
        			next = a[i - shift];
        		}
        		array[i] = next;
        	}
        }
        return array;
		
	}
	
	/**
	 * Inserts the given object into the array.
	 * @param <T>
	 * @param array
	 * @param obj
	 * @return
	 */
	/*
	public static Object[] insert (Object[] array, Object obj) {
		
		Object[] tmpArry = new Object[array.length + 1];
		for (int i = 0; i < array.length; i++) {
			tmpArry[i] = array[i];
		}
		tmpArry[tmpArry.length - 1] = obj;
		
		return tmpArry;
	}
	*/
	
	/**
	 * Joins two or more arrays together, preserving duplicates.
	 * @param joinArrays
	 * @return
	 */
	public static Object[] join (Object[] ... joinArrays) {
		ArrayList arrays = new ArrayList();
		for (Object[] arry : joinArrays) {
			arrays.addAll(Arrays.asList(arry));
		}
		return arrays.toArray();
	}

	/**
	 * Joins two or more arrays together, preserving duplicates.
	 * @param joinArrays
	 * @return
	 */
	public static String[] join (String[] ... joinArrays) {
		ArrayList<String> arrays = new ArrayList<String>();
		for (String[] arry : joinArrays) {
			arrays.addAll(Arrays.asList(arry));
		} 
		return arrays.toArray(new String[0]);
	}
	
	
	public static void main (String[] args) {
		String[] strings1 = {"this", "is", "a", "test"};
		String[] gay = new String[0];
		String[] teh = insert(gay, "teh", 1);
		String[] strings2 = {"of", "teh", "pubic", "systems"};
		String[] strings3 = join(strings1, strings2);
		System.out.println(strings3);
	}

}
