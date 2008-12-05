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
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;

/**
 * The abstract class which should be implemented by all coordinates which map
 * numeric values.
 *
 * @see org.knime.base.util.coordinate.DoubleCoordinate
 *
 * @author Tobias Koetter, University of Konstanz
 * @author Stephan Sellien, University of Konstanz
 */
public abstract class NumericCoordinate extends Coordinate {

    private double m_minDomainValue;

    private double m_maxDomainValue;

    /**
     * Constructor for class NumericCoordinate.
     *
     * @param dataColumnSpec the specification of the coordinate column
     */
    NumericCoordinate(final DataColumnSpec dataColumnSpec) {
        super(dataColumnSpec);
        setMinDomainValue(((DoubleValue)dataColumnSpec.getDomain()
                .getLowerBound()).getDoubleValue());
        setMaxDomainValue(((DoubleValue)dataColumnSpec.getDomain()
                .getUpperBound()).getDoubleValue());
    }

    /**
     * Returns an array with the positions of all ticks and their corresponding
     * domain values given an absolute length. The pre-specified tick policy
     * also influences the tick positions, e.g. ascending or descending.
     *
     * @param absolutLength the absolute length the domain is mapped on
     *
     * @return the mapping of tick positions and corresponding domain values
     */
    protected abstract CoordinateMapping[] getTickPositionsInternal(
            final double absolutLength);

    /**
     *
     * Returns an array with the positions of all ticks and their corresponding
     * domain values given an absolute length. The pre-specified tick policy
     * also influences the tick positions, e.g. ascending or descending.
     *
     * @param absolutLength the absolute length the domain is mapped on
     *
     * @return the mapping of tick positions and corresponding domain values
     */
    @Override
    protected CoordinateMapping[] getTickPositionsWithLabels(
            final double absolutLength) {
        CoordinateMapping[] coordMap = getTickPositionsInternal(absolutLength);
        if (coordMap == null || coordMap.length < 1
                || getActiveMappingMethod() == null
                || getCurrentPolicy() == null
                || !getCurrentPolicy().isMappingAllowed()) {
            return coordMap;
        }
        CoordinateMapping[] result = new CoordinateMapping[coordMap.length];

        int index = 0;
        for (CoordinateMapping cm : coordMap) {
            // each numeric coordinate must have exactly one value per tick
            // and each tick a numeric value ( = DoubleValue ) set.
            DoubleValue value = (DoubleValue)cm.getValues()[0];
            double val =
                    getActiveMappingMethod().getLabel(
                            new DoubleCell(value.getDoubleValue()));
            result[index++] =
                    new DoubleCoordinateMapping("" + val, val, cm
                            .getMappingValue());
        }

        return result;
    }

    /**
     * Calculates a numeric mapping assuming a
     * {@link org.knime.core.data.def.DoubleCell}.
     *
     * {@inheritDoc}
     */
    @Override
    protected abstract double calculateMappedValueInternal(
            final DataCell domainValueCell, final double absolutLength);

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
     * @param absoluteLength the absolute length
     * @return 0
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
    public void setMinDomainValue(final double value) {
        m_minDomainValue = value;
    }

    /**
     * Sets the upper domain value.
     *
     * @param value the upper value
     */
    public void setMaxDomainValue(final double value) {
        m_maxDomainValue = value;
    }

    /**
     * @return Returns the maxDomainValue.
     */
    public final double getMaxDomainValue() {
        DataCell cell = new DoubleCell(m_maxDomainValue);
        cell = applyMappingMethod(cell);
        return ((DoubleValue)cell).getDoubleValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getPositiveInfinity() {
            return ((DoubleValue)applyMappingMethod(new DoubleCell(
                    Double.POSITIVE_INFINITY))).getDoubleValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getNegativeInfinity() {
            return ((DoubleValue)applyMappingMethod(new DoubleCell(
                    Double.NEGATIVE_INFINITY))).getDoubleValue();
    }

    /**
     * @return Returns the minDomainValue.
     */
    public final double getMinDomainValue() {
        DataCell cell = new DoubleCell(m_minDomainValue);
        cell = applyMappingMethod(cell);
        return ((DoubleValue)cell).getDoubleValue();
    }

    /**
     * {@inheritDoc} Only double values are accepted!
     */
    @Override
    public void addDesiredValues(final DataValue... values) {
        for (DataValue v : values) {
            if (v instanceof DoubleValue) {
                getDesiredValuesSet().add(v);
            } else {
                throw new IllegalArgumentException(
                        "Desired values must be numeric values.");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataValue[] getDesiredValues() {
        return super.getDesiredValues();
    }
}
