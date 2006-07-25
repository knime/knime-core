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
 *   21.07.2006 (koetter): created
 */
package de.unikn.knime.base.util.coordinate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataColumnDomain;
import de.unikn.knime.core.data.DataColumnSpec;
import de.unikn.knime.core.data.DataType;
import de.unikn.knime.core.data.DataValueComparator;
import de.unikn.knime.core.data.DoubleValue;
import de.unikn.knime.core.data.IntValue;

/**
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class IntegerCoordinate extends Coordinate {
    private static final int DEFAULT_TICK_DIST = 20;

    private final int m_minDomainValue;

    private final int m_maxDomainValue;

    // private final long m_domainRange;

    private final List<DataCell> m_values;

    private final long m_domainLength;

    private int m_baseVal;

    /**
     * Constructor for class IntegerCoordinate.
     * 
     * @param dataColumnSpec the column specification
     */
    protected IntegerCoordinate(final DataColumnSpec dataColumnSpec) {
        super(dataColumnSpec);
        // column specification is check for null in the super constructor
        // check and set the domain range
        DataColumnDomain domain = getDataColumnSpec().getDomain();
        if (domain == null) {
            // if there is no domain a coordinate makes no sense
            throw new IllegalArgumentException(
                    "The domain of the set column spec is null. "
                            + "Coordinate can not be created.");
        }
        final DataType colType = dataColumnSpec.getType();
        if (!colType.isCompatible(IntValue.class)) {
            throw new IllegalArgumentException("Data type should be of type "
                    + IntValue.class.getName() + ".");
        }
        Set<DataCell> valuesSet = domain.getValues();
        if (valuesSet != null && valuesSet.size() > 0) {
            List<DataCell> values = new ArrayList<DataCell>(valuesSet.size());
            for (DataCell cell : valuesSet) {
                if (cell.getType().isCompatible(DoubleValue.class)) {
                    values.add(cell);
                }
            }
            DataValueComparator cellComparator = colType.getComparator();
            Collections.sort(values, cellComparator);
            m_values = values;
            m_minDomainValue = (int)((DoubleValue)m_values.get(0))
                    .getDoubleValue();
            m_maxDomainValue = (int)((DoubleValue)m_values
                    .get(m_values.size() - 1)).getDoubleValue();

        } else {
            m_values = null;
            DataCell lowerBound = domain.getLowerBound();
            if (lowerBound == null) {
                // if there is no lower bound a coordinate makes no sense
                throw new IllegalArgumentException(
                        "The lower bound of the set column spec is null. "
                                + "Coordinate can not be created.");
            } else {
                m_minDomainValue = (int)((DoubleValue)lowerBound)
                        .getDoubleValue();
            }

            DataCell upperBound = domain.getUpperBound();
            if (upperBound == null) {
                // if there is no upper bound a coordinate makes no sense
                throw new IllegalArgumentException(
                        "The upper bound of the set column spec is null. "
                                + "Coordinate can not be created.");
            } else {
                m_maxDomainValue = (int)((DoubleValue)upperBound)
                        .getDoubleValue();
            }
        }
        m_domainLength = createDomainRange(m_minDomainValue, m_maxDomainValue);
        // the basic value for calculating the height needed for negative values
        m_baseVal = 0;
        if (m_minDomainValue < 0) {
            m_baseVal = m_minDomainValue;
        }
    }

    private int createDomainRange(final int minValue, final int maxValue) {
        int range = Math.abs(maxValue - minValue);
        return roundRange(range);
    }

    /**
     * @see de.unikn.knime.base.util.coordinate.Coordinate#
     *      getTickPositions(double, boolean)
     */
    @Override
    public CoordinateMapping[] getTickPositions(final double absolutLength,
            final boolean naturalMapping) {
        CoordinateMapping[] mapping = null;
        // do a special treatment if the domain range is zero
        if (m_domainLength == 0.0) {
            // just one mapping is created in the middle of the available
            // absolute length
            double mappingValue = Math.round(absolutLength / 2);
            mapping = new IntegerCoordinateMapping[1];
            mapping[0] = new IntegerCoordinateMapping(Integer
                    .toString(m_minDomainValue), m_minDomainValue, mappingValue);
            return mapping;
        }

        // the height per 1 value in pixel
        double heightPerVal = absolutLength / m_domainLength;
        if (m_values != null && m_values.size() > 0) {
            // the user has predefined values which he want to have displayed
            mapping = new CoordinateMapping[m_values.size()];
            for (int i = 0, length = m_values.size(); i < length; i++) {
                int value = (int)((DoubleValue)m_values.get(i))
                        .getDoubleValue();
                double position = calculatePosition(heightPerVal, value,
                        m_baseVal);
                mapping[i] = new IntegerCoordinateMapping(Integer
                        .toString(value), value, position);
            }

        } else {
            int noOfTicks = (int)Math.ceil(absolutLength
                    / IntegerCoordinate.DEFAULT_TICK_DIST);
            double range = Math.ceil(m_domainLength / noOfTicks);
            range = roundRange(range);
            while ((m_minDomainValue + noOfTicks * range) < m_maxDomainValue) {
                // this should never happen
                noOfTicks++;
            }
            int value = m_minDomainValue;
            mapping = new IntegerCoordinateMapping[noOfTicks];
            for (int i = 0, length = mapping.length; i < length; i++) {
                double position = calculatePosition(heightPerVal, value,
                        m_baseVal);
                mapping[i] = new IntegerCoordinateMapping(Integer
                        .toString(value), value, position);
                value += range;
            }
        }
        return mapping;
    }

    /**
     * @param range the range to be rounded
     * @return the rounded range
     */
    public static int roundRange(final double range) {
        if (range < 1) {
            return 1;
        } else if (range > 10) {
            // find the next higher number divided by ten.
            int divider = 10;
            int addition = 2;
            if (range > 50 && range <= 100) {
                addition = 5;
            } else if (range > 100 && range <= 1000) {
                addition = 10;
            } else if (range > 1000) {
                addition = 100;
                divider = 100;
            }

            while (range / divider > 1) {
                divider += addition;
            }
            return divider;
        } else {
            return (int)Math.ceil(range);
        }
    }

    /**
     * @see de.unikn.knime.base.util.coordinate.Coordinate#
     *      calculateMappedValue(de.unikn.knime.core.data.DataCell, double,
     *      boolean)
     */
    @Override
    public double calculateMappedValue(final DataCell domainValueCell,
            final double absolutLength, final boolean naturalMapping) {
        if (domainValueCell == null
                || !domainValueCell.getType().isCompatible(DoubleValue.class)) {
            throw new IllegalArgumentException("Value cell not compatible.");
        }
        // the height per 1 value in pixel
        double heightPerVal = absolutLength / m_domainLength;
        int value = (int)((DoubleValue)domainValueCell).getDoubleValue();
        double position = calculatePosition(heightPerVal, value, m_baseVal);
        return position;
    }

    /**
     * @param heightPerVal the height per value
     * @param value the value itself
     * @param baseVal the base value
     * @return the position in pixel for the given value
     */
    private static double calculatePosition(final double heightPerVal,
            final int value, final int baseVal) {
        return (value - baseVal) * heightPerVal;
    }

    /**
     * @see de.unikn.knime.base.util.coordinate.Coordinate#isNominal()
     */
    @Override
    public boolean isNominal() {
        return false;
    }

    /**
     * @see de.unikn.knime.base.util.coordinate.Coordinate#
     *      getUnusedDistBetweenTicks(double)
     */
    @Override
    public double getUnusedDistBetweenTicks(final double absoluteLength) {
        return 0;
    }
}
