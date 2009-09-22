/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2009
 * KNIME.com, Zurich, Switzerland
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
 *   28.07.2009 (Fabian Dill): created
 */
package org.knime.core.data.date;

/**
 * Utility class for rendering times and dates.
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public final class DateAndTimeRenderUtil {
    
    private DateAndTimeRenderUtil() {
        // utility class
    }
    
    /**
     * 
     * @param dateField day. month or year
     * @return the date fields with a trailing "0" to be of two digits 
     */
    public static String getStringForDateField(final int dateField) {
        if (dateField < 10) {
            return "0" + Integer.toString(dateField);
        }
        return Integer.toString(dateField);
    }

}
