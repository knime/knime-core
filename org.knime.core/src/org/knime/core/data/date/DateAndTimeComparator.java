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
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 * 
 * History
 *   14.09.2009 (Fabian Dill): created
 */
package org.knime.core.data.date;

import java.util.Calendar;

import org.knime.core.data.DataValue;
import org.knime.core.data.DataValueComparator;

/**
 * Compares to {@link DateAndTimeValue}s by comparing their UTC time.
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
class DateAndTimeComparator extends DataValueComparator {

    /** {@inheritDoc} */
    @Override
    protected int compareDataValues(final DataValue v1, final DataValue v2) {
        Calendar c1;
        Calendar c2;
        if (v1 instanceof DateAndTimeCell && v2 instanceof DateAndTimeCell) {
            c1 = ((DateAndTimeCell)v1).getInternalUTCCalendarMember();
            c2 = ((DateAndTimeCell)v2).getInternalUTCCalendarMember();
        } else {
            // not native implementation: compare via public methods:
            c1 = ((DateAndTimeValue)v1).getUTCCalendarClone();
            c2 = ((DateAndTimeValue)v2).getUTCCalendarClone();
        }
        return c1.compareTo(c2);
    }

}
