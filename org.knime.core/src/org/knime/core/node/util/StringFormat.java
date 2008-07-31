/*
 * ------------------------------------------------------------------ *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 * 
 * History
 *   Feb 5, 2008 (wiswedel): created
 */
package org.knime.core.node.util;

/**
 * Collection of convenience methods to format strings (such as elapsed time or
 * urls).
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class StringFormat {

    private StringFormat() {
    }

    /**
     * Formats paths such as file locations or URLs. This method is used for
     * instance in KNIME's file reader dialog to shrink the file location while
     * maintaining the most important parts of the location (protocol and file
     * name).
     * 
     * <p>
     * The pattern is as follows: If <code>size</code> is smaller than 30,
     * return the last <code>size</code> chars in the string (plus some dots
     * in front); if the <code>size</code> is larger than 30: return the first
     * 12 chars + ... + chars from the end. If it is larger than 55: the first
     * 28 + ... + rest from the end.
     * 
     * @param str The string to format. If <code>null</code> an empty string
     *            is returned.
     * @param size The requested size. Negative or too small (&lt; 3) values are
     *            treated with respect, i.e. no exception is thrown.
     * @return The formatted string.
     */
    public static String formatPath(final String str, final int size) {
        if (str == null) {
            return "";
        }
        int newSize = Math.max(3, size);
        String result;
        if (str.length() <= newSize) {
            // short enough - return it unchanged
            return str;
        }
        if (newSize <= 30) {
            result = "..." 
                + str.substring(str.length() - newSize + 3, str.length());
        } else if (newSize <= 55) {
            result = str.substring(0, 12) + "..."
                + str.subSequence(str.length() - newSize + 15, str.length());
        } else {
            result = str.substring(0, 28) + "..."
                + str.subSequence(str.length() - newSize + 31, str.length());
        }
        return result;
    }
    
    /** Formats a time difference into a string stating elapsed days, hours etc.
     * Days, hours, and minutes are only printed if they need to (meaning that
     * for instance <code>formatElapsedTime(3000L)</code> will only return 
     * &quot;3 secs&quot;).
     * @param timeInMS The time in milliseconds, negative values will 
     *        be preceded by a minus sign
     * @return The formatted string.
     */
    public static String formatElapsedTime(final long timeInMS) {
        StringBuilder timeReport = new StringBuilder(timeInMS < 0 ? "-" : "");
        long decTime = Math.abs(timeInMS); // "corrected" time
        long multiplier = 24 * 60 * 60 * 1000;
        long elapsedTimeDay = decTime / multiplier;
        decTime -= elapsedTimeDay * multiplier;
        multiplier = 60 * 60 * 1000;
        long elapsedTimeHour = decTime / multiplier;
        decTime -= elapsedTimeHour * multiplier;
        multiplier = 60 * 1000;
        long elapsedTimeMin = decTime / multiplier;
        decTime -= elapsedTimeMin * multiplier;
        multiplier = 1000;
        long elapsedTimeSec = decTime / multiplier;
        
        // if to print the next magnitude (e.g. 1 days, 0 mins, 23 sec)
        // (we want the minutes to be printed, even if it's 0)
        boolean printUnit = false; 
        if (elapsedTimeDay > 0) {
            timeReport.append(elapsedTimeDay);
            timeReport.append(elapsedTimeDay == 1 ? " day, " : " days, ");
            printUnit = true;
        }
        if (elapsedTimeHour > 0 || printUnit) {
            timeReport.append(elapsedTimeHour);
            timeReport.append(elapsedTimeHour == 1 ? " hour, " : " hours, ");
            printUnit = true;
        }
        if (elapsedTimeMin > 0 || printUnit) {
            timeReport.append(elapsedTimeMin);
            timeReport.append(elapsedTimeMin == 1 ? " min, " : " mins, ");
        }
        // seconds are always printed
        timeReport.append(elapsedTimeSec);
        timeReport.append(elapsedTimeSec == 1 ? " sec" : " secs");
        return timeReport.toString();
    }
}
