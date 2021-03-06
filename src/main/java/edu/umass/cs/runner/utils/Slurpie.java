package edu.umass.cs.runner.utils;

import java.io.*;
import java.net.URL;

/**
 * Slurpie.slurp reads an entire file into a string.
 */
public class Slurpie {

    public static String slurp(String filename) throws IOException {
        return slurp(filename, Integer.MAX_VALUE);
    }

    public static String slurp(String filename, int numChars) throws IOException {
        URL resource = Slurpie.class.getClassLoader().getResource(filename);
        BufferedReader br = null;
        try {
            if (resource == null) {
                try {
                    br = new BufferedReader(new FileReader(filename));
                } catch (FileNotFoundException fe) {
                    br = new BufferedReader(new InputStreamReader(new URL(filename).openStream()));
                }
            } else br = new BufferedReader(new InputStreamReader(resource.openStream()));
            StringBuilder s = new StringBuilder();
            char[] buf = new char[1024 * 1024];
            for (int totalCharsRead = 0; totalCharsRead < numChars; ) {
                int charsRead = br.read(buf);
                if (charsRead == -1)
                    break;
                s.append(buf, 0, charsRead);
                totalCharsRead += buf.length;
            }
            return s.toString();
        } finally {
            if (br != null) br.close();
        }
    }

    public static String slurp(String filename, boolean ignoreErr) throws IOException{
        String retval = "";
        try {
            retval = slurp(filename);
        } catch (IOException io) {
            if (!ignoreErr) throw io;
        }
        return retval;

    }
}
