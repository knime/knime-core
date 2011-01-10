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
