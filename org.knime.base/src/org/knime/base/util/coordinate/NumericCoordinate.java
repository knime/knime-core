/*
 * ------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   26.07.2006 (koetter): created
 */
package org.knime.base.util.coordinate;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;

/**
 * The abstract class which should be implemented by all coordinates which map
 * numeric values.
 * @see org.knime.base.util.coordinate.DoubleCoordinate 
 * @see org.knime.base.util.coordinate.IntegerCoordinate
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public abstract class NumericCoordinate extends Coordinate {

    /**Constructor for class NumericCoordinate.
     * @param dataColumnSpec the specification of the coordinate column
     */
    NumericCoordinate(final DataColumnSpec dataColumnSpec) {
        super(dataColumnSpec);
    }

    /**
     * Returns an array with the position of all ticks and their corresponding
     * domain values given an absolute length. The prespecified tick policy also
     * influences the tick positions.
     * 
     * @param absolutLength the absolute length the domain is mapped on
     * @param naturalMapping if <code>true</code> the mapping values are
     *            rounded to the next integer equivalent
     * 
     * @return the mapping of tick positions and corresponding domain values
     */
    @Override
    public abstract CoordinateMapping[] getTickPositions(
            final double absolutLength, final boolean naturalMapping);

    /**
     * Calculates a numeric mapping assuming a
     * {@link org.knime.core.data.def.DoubleCell}.
     * 
     * @see org.knime.base.util.coordinate.Coordinate
     *      #calculateMappedValue(org.knime.core.data.DataCell,double,
     *      boolean)
     */
    @Override
    public abstract double calculateMappedValue(final DataCell domainValueCell,
            final double absolutLength, final boolean naturalMapping);

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNominal() {
        return false;
    }

    /**
     * A numeric coordinate does not has a unused distance range.
     * 
     * @see org.knime.base.util.coordinate.Coordinate
     *      #getUnusedDistBetweenTicks(double)
     */
    @Override
    public double getUnusedDistBetweenTicks(final double absoluteLength) {
        return 0;
    }

    /**
     * @return <code>true</code> if the lower domain range is set properly
     */
    public abstract boolean isMinDomainValueSet();

    /**
     * @return <code>true</code> if the upper domain range is set properly
     */
    public abstract boolean isMaxDomainValueSet();

    /**
     * Sets the lower domain value.
     * 
     * @param value the lower value
     */
    public abstract void setMinDomainValue(final double value);

    /**
     * Sets the upper domain value.
     * 
     * @param value the upper value
     */
    public abstract void setMaxDomainValue(final double value);

    /**
     * @return Returns the maxDomainValue.
     */
    public abstract double getMaxDomainValue();

    /**
     * @return Returns the minDomainValue.
     */
    public abstract double getMinDomainValue();

}
