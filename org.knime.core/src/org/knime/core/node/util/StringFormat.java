/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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
