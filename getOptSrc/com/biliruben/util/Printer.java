package com.biliruben.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Printer is a bag of static utilities to print objects to
 * whatever IO channel you want.  Two purposes are served here:
 * - writing to whatever channel you like and avoid dealing with
 *   output streams / print streams / channels yourself
 * - Pretty formats for common object types eliminating the need
 *   to extend and overload 'toString'
 * @author trey.kirk
 *
 */
public class Printer {

    public static void print(Object object, Writer writer)
        throws IOException {
        print(object, writer, 0);
    }
    
    public static void print(Object object, OutputStream out) throws IOException {
        print(object, out, 0);
    }
    
    public static void print(Object object, OutputStream out, int indent) throws IOException {
        Writer writer = new PrintWriter(out);
        print(object, writer, indent);
    }
    
    public static void print(Object object, Writer writer, int indent) 
        throws IOException {
        // TODO: don't like how this played out.  Overloading never works like I epxect it to
        // work and this pretty much needs a rewrite.  On rewrite, instead of passing the Writer/OStream
        // around, pass around a StringBuffer.  Then localize the printing to one set of methods that
        // will be the external facing methods.
    }
    
    public static void print(Map map, Writer writer) throws IOException {
        print(map, writer, 0);
    }
    public static void print(HashMap hMap, Writer writer, int indent) throws IOException {
        print ((Map)hMap, writer, indent);
    }
    public static void print(Map map, Writer writer, int indent) throws IOException {
        for (Object key : map.keySet()) {
            Object value = map.get(key);
            print(key, writer, indent);
            print(" : ", writer, indent);
            if (value instanceof String) {
                print("\n", writer, indent);
                indent++;
            }
            print(value, writer, indent);
            print("\n", writer, indent);
        }
    }
    
    public static void print(List list, Writer writer) throws IOException {
        print(list, writer, 0);
    }
    
    public static void print(List list, Writer writer, int indent) throws IOException {
        print("[", writer, indent);
        int count = 0;
        for (Object item : list) {
            print(item, writer, indent);
            if (count == list.size() - 1) {
                // end of the list
                print("]\n", writer, indent);
            } else {
                print(",\n", writer, indent);
            }
            // use a counter, but don't use classic For Loop / List.get(i)
            // Internal iterator is typically much more efficient
            count++;
        }
    }
    
    public static void print(String value, Writer writer) throws IOException {
        print(value, writer, 0);
    }
    
    public static void print(String value, Writer writer, int indent) throws IOException {
        innerPrint(value, writer, indent);
    }
    
    private static void innerPrint (String line, Writer writer, int indent) throws IOException {
        StringBuffer buff = new StringBuffer();
        for (int i = 0; i < indent; i++) {
            buff.append("   ");
        }
        writer.write(buff.toString());
    }
}
