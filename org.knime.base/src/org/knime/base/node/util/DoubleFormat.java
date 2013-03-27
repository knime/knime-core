/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * -------------------------------------------------------------------
 *
 * History
 *   Apr 20, 2006 (wiswedel): created
 */
package org.knime.base.node.util;

import java.text.DecimalFormat;

/**
 * Convenience class that allows to format a double to a string. It will use
 * different {@link java.text.NumberFormat} instances depending on the value of
 * the double.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class DoubleFormat {

    private DoubleFormat() {
        // no op
    }

    /** for numbers less than 0.0001. */
    private static final DecimalFormat SMALL_FORMAT = new DecimalFormat(
            "0.00E0");

    /** for numbers in (0.0001, 10]. */
    private static final DecimalFormat NORMAL_FORMAT = new DecimalFormat(
            "##0.####");

    /** for numbers in (10, 100'000'000). */
    private static final DecimalFormat LARGE_FORMAT = new DecimalFormat(
            "###,###,##0.####");

    /** for numbers larger than 100'000'000. */
    private static final DecimalFormat VERY_LARGE_FORMAT = new DecimalFormat(
            "0.00E0");

    /**
     * Formats the double to a string. It will use the following formats:
     * <table>
     * <tr>
     * <th>Range</th>
     * <th>Format</th>
     * </tr>
     * <tr>
     * <td>d < |0.0001|</td>
     * <td>0.00E0</td>
     * </tr>
     * <tr>
     * <td>d < |10|</td>
     * <td>##0.####</td>
     * </tr>
     * <tr>
     * <td>d < |100'000'000|</td>
     * <td>####0.#</td>
     * </tr>
     * <tr>
     * <td>else</td>
     * <td>0.00E0</td>
     * </tr>
     * </table>
     *
     * @param d the double to format
     * @return the string representation of <code>d</code>
     */
    public static String formatDouble(final double d) {
        if (d == 0.0 || Double.isInfinite(d) || Double.isNaN(d)) {
            return Double.toString(d);
        }
        DecimalFormat format;
        double abs = Math.abs(d);
        if (abs < 0.0001) {
            format = SMALL_FORMAT;
        } else if (abs <= 10) {
            format = NORMAL_FORMAT;
        } else if (abs < (100 * 1000 * 1000)) {
            format = LARGE_FORMAT;
        } else {
            format = VERY_LARGE_FORMAT;
        }
        synchronized (format) {
            return format.format(d);
        }
    }
}
