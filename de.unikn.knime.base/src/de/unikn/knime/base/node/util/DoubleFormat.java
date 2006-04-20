/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   Apr 20, 2006 (wiswedel): created
 */
package de.unikn.knime.base.node.util;

import java.text.DecimalFormat;

/**
 * Convenience class that allows to format a double to a string. It will
 * use different NumberFormat instances depending on the value of the
 * double.
 * @author wiswedel, University of Konstanz
 */
public final class DoubleFormat {

    private DoubleFormat() {
    }


    /** for numbers less than 0.0001. */
    private static final DecimalFormat  SMALL_FORMAT = 
        new DecimalFormat("0.00E0");
    /** for numbers in (0.0001, 10]. */
    private static final DecimalFormat  NORMAL_FORMAT = 
        new DecimalFormat("##0.####");
    /** for numbers in (10, 10000). */
    private static final DecimalFormat  LARGE_FORMAT = 
        new DecimalFormat("####0.#");
    /** for numbers larger than 10000. */
    private static final DecimalFormat  VERY_LARGE_FORMAT = 
        new DecimalFormat("0E0");
    
    /** Formats the double to a string. It will use the following formats:
     * <table>
     * <tr>
     *   <th>Range</th><th>Format</th>
     * </tr>
     * <tr>
     *   <td>d < |0.0001|</td><td>0.00E0</td>
     * </tr>
     * <tr>
     *   <td>d < |10|</td><td>##0.####</td>
     * </tr>
     * <tr>
     *   <td>d < |10000|</td><td>####0.#</td>
     * </tr>
     * <tr>
     *   <td>else</td><td>0E0</td>
     * </tr>
     * </table>
     * @param d The double to format.
     * @return The string representation of <code>d</code>.
     */
    public static String formatDouble(final double d) {
        double abs = Math.abs(d);
        if (abs < 0.0001) {
            return SMALL_FORMAT.format(d);
        } 
        if (abs <= 10) {
            return NORMAL_FORMAT.format(d);
        }
        if (abs < 10000) {
            return LARGE_FORMAT.format(d);
        }
        return VERY_LARGE_FORMAT.format(d);
    }
}
