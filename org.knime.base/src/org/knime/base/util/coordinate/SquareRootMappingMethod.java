/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
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
 *   Oct 31, 2008 (sellien): created
 */
package org.knime.base.util.coordinate;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;

/**
 *
 * @author Stephan Sellien, University of Konstanz
 */
public class SquareRootMappingMethod implements MappingMethod {

    /**
     * ID for a square root mapping method.
     */
    public static final String ID_SQRT = "sqrtMappingMethod";

    /**
     * {@inheritDoc}
     */
    public DataCell doMapping(final DataCell cell) {
        if (cell == null || cell.isMissing()
                || !cell.getType().isCompatible(DoubleValue.class)) {
            return cell;
        }
        double value = ((DoubleValue)cell).getDoubleValue();
        return new DoubleCell(Math.sqrt(value));
    }

    /**
     * {@inheritDoc}
     */
    public String getDisplayName() {
        return "sqrt(x)";
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCompatibleWithDomain(final DataColumnDomain domain) {
        if (domain == null || !domain.hasBounds()) {
            return false;
        }
        if (domain.hasLowerBound()
                && domain.getLowerBound().getType().isCompatible(
                        DoubleValue.class)
                && domain.getUpperBound().getType().isCompatible(
                        DoubleValue.class)) {
            double lower =
                    ((DoubleValue)domain.getLowerBound()).getDoubleValue();
            double upper =
                    ((DoubleValue)domain.getUpperBound()).getDoubleValue();
            if (lower >= 0 && upper >= 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public double getLabel(final DataCell cell) {
        if (cell == null || !cell.getType().isCompatible(DoubleValue.class)) {
            throw new IllegalArgumentException(
                    "Cell must not be null and must be of type DoubleValue!");
        }
        double value = ((DoubleValue)cell).getDoubleValue();
        return value * value;
    }

}
