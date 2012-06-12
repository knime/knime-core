/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *   Mar 1, 2008 (wiswedel): created
 */
package org.knime.core.node.util;

import org.knime.core.node.NodeLogger;

/**
 * Collection of methods that are useful in different contexts.
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class ConvenienceMethods {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(ConvenienceMethods.class);

    private ConvenienceMethods() {
        // no op
    }

    /** Determines if both arguments are equal according to their equals
     * method (assumed to be symmetric). This method handles null arguments.
     * @param o1 First object for comparison, may be <code>null</code>.
     * @param o2 Second object for comparison, may be <code>null</code>.
     * @return If both arguments are equal
     * (if either one is null, so must be the other one)
     */
    public static boolean areEqual(final Object o1, final Object o2) {
        if (o1 == o2) {
            return true;
        }
        if (o1 != null) {
            return o1.equals(o2);
        } else if (o2 != null) {
            return o2.equals(o1);
        }
        assert false : "Both objects are null, hence equal";
        return true;
    }

    /** Read system property "line.separator", returns '\n' if that fails
     * (whereby a LOG message is reported).
     * @return The system line separator, never null.
     */
    public static String getLineSeparator() {
        String lineSep;
        try {
            lineSep = System.getProperty("line.separator");
            if (lineSep == null || lineSep.isEmpty()) {
                throw new RuntimeException("line separator must not be empty");
            }
        } catch (Throwable e) {
            LOGGER.error("Unable to get \"line.separator\" from system, "
                    + "using \"\\n\"", e);
            lineSep = "\n";
        }
        return lineSep;
    }

    /** Read system property <code>envVar</code> that is supposed to be of
     * a format such as '1024' (1024 bytes), '5K' (5 kilobyte),
     * '2M' (2 megabyte). If that fails (not parsable or negative), it will
     * return the the default value and log an error message.
     * @param envVar The name of the variable, must not be null (NPE)
     * @param defaultValue The default value (in bytes)
     * @return The size parameters in bytes or the defaultValue if the value
     *         can't be parsed.
     */
    public static long readSizeSystemProperty(
            final String envVar, final long defaultValue) {
        long size = defaultValue;
        String property = System.getProperty(envVar);
        if (property != null) {
            String s = property.trim();
            int multiplier = 1;
            if (s.endsWith("m") || s.endsWith("M")) {
                s = s.substring(0, s.length() - 1);
                multiplier = 1024 * 1024;
            } else if (s.endsWith("k") || s.endsWith("K")) {
                s = s.substring(0, s.length() - 1);
                multiplier = 1024;
            }
            try {
                int newSize = Integer.parseInt(s);
                if (newSize < 0) {
                    throw new NumberFormatException("Size < 0" + newSize);
                }
                size = newSize * multiplier;
                LOGGER.debug("Setting min blob size for SDF cells to "
                        + size + " bytes");
            } catch (NumberFormatException e) {
                LOGGER.warn("Unable to parse property " + envVar
                        + ", using default", e);
            }
        }
        return size;
    }

}
