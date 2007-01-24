/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 * -------------------------------------------------------------------
 * 
 * History
 *    01.01.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.histogram.datamodel;

import java.util.Comparator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DoubleValue;

/**
 * Compares the x value of to {@link HistogramDataRow} objects.
 * @author Tobias Koetter, University of Konstanz
 */
public class HistogramDataRowXComparator 
implements Comparator<HistogramDataRow> {

    /**
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(final HistogramDataRow o1, final HistogramDataRow o2) {
        if (o1 == o2) {
            return 0;
        }
        if (o1 == null) {
            return -1;
        }
        if (o2 == null) {
            return +1;
        }
        final DataCell x1 = o1.getXVal();
        final DataCell x2 = o2.getXVal();
        if (x1.isMissing()) {
            return -1;
        }
        if (x2.isMissing()) {
            return +1;
        }
        if (x1.getType().isCompatible(DoubleValue.class) 
                && x2.getType().isCompatible(DoubleValue.class)) {
            final double dv1 = ((DoubleValue)x1).getDoubleValue();
            final double dv2 = ((DoubleValue)x2).getDoubleValue();
            if (dv1 < dv2) {
               return -1;
            } else if (dv1 > dv2) {
                return +1;
            } else {
                return 0;
            }
        }
        //if it's a nominal value treat each element as equal
        return 0;
    }
}
