/* -------------------------------------------------------------------
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
class IntegerCoordinate extends NumericCoordinate {
    private static final int DEFAULT_TICK_DIST = 20;

    /**The minimum value covered by this coordinate object.*/
    private int m_minDomainValue;
    /**The maximum value covered by this coordinate object.*/
    private int m_maxDomainValue;

    /**The <code>List</code> of values which should be returned as
     * tick positions by this coordinate.*/
    private final List<DataCell> m_values;

    /**The range which is covered by this coordinate object.*/
    private long m_domainRange;

    /**The basic value which is used to calculate the position in pixel for
     * a given value.*/
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
        updateInternalData();
    }

    /**
     * @see de.unikn.knime.base.util.coordinate.Coordinate#
     * getTickPositions(double, boolean)
     */
    @Override
    public CoordinateMapping[] getTickPositions(final double absolutLength,
            final boolean naturalMapping) {
        CoordinateMapping[] mapping = null;
        // do a special treatment if the domain range is zero
        if (m_domainRange == 0.0) {
            // just one mapping is created in the middle of the available
            // absolute length
            double mappingValue = Math.round(absolutLength / 2);
            mapping = new IntegerCoordinateMapping[1];
            mapping[0] = 
                new IntegerCoordinateMapping(Integer.toString(
                        m_minDomainValue), m_minDomainValue, mappingValue);
            return mapping;
        }

        // the height per 1 value in pixel
        double heightPerVal = absolutLength / m_domainRange;
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
            double range = Math.ceil(m_domainRange / noOfTicks);
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
                while ((range / 10) > divider) {
                    divider *= 10;
                    addition *= 5;
                }
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
     * @see de.unikn.knime.base.util.coordinate.Coordinate
     *      #calculateMappedValue(de.unikn.knime.core.data.DataCell, double,
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
        double heightPerVal = absolutLength / m_domainRange;
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
     * @see de.unikn.knime.base.util.coordinate.NumericCoordinate#
     * isMinDomainValueSet()
     */
    @Override
    public boolean isMinDomainValueSet() {
        return true;
    }

    /**
     * @see de.unikn.knime.base.util.coordinate.NumericCoordinate#
     * isMaxDomainValueSet()
     */
    @Override
    public boolean isMaxDomainValueSet() {
        return true;
    }
    
    /**
     * @see de.unikn.knime.base.util.coordinate.NumericCoordinate#
     * getMaxDomainValue()
     */
    @Override
    public double getMaxDomainValue() {
        return m_maxDomainValue;
    }

    /**
     * @see de.unikn.knime.base.util.coordinate.NumericCoordinate#
     * getMinDomainValue()
     */
    @Override
    public double getMinDomainValue() {
        return m_minDomainValue;
    }

    /**
     * @see de.unikn.knime.base.util.coordinate.NumericCoordinate#
     * setMinDomainValue(double)
     */
    @Override
    public void setMinDomainValue(final double value) {
        if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
            throw new IllegalArgumentException("Value to big or small for " 
                    + IntegerCoordinate.class.getName());
        }
        m_minDomainValue = (int) value;
        updateInternalData();
    }

    /**
     * @see de.unikn.knime.base.util.coordinate.NumericCoordinate#
     * setMaxDomainValue(double)
     */
    @Override
    public void setMaxDomainValue(final double value) {
        if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
            throw new IllegalArgumentException("Value to big or small for " 
                    + IntegerCoordinate.class.getName());
        }
        m_maxDomainValue = (int) value;
        updateInternalData();
    }
    /**
     * @param minValue the minimum value to map on the coordinate
     * @param maxValue the maximum value to map on the coordinate
     * @return the range which is covered by this coordinate
     */
    private int createDomainRange(final int minValue, final int maxValue) {
        int range = Math.abs(maxValue - minValue);
        return roundRange(range);
    }

    /**
     * @param minValue the minimum value of this coordinate
     * @return the base value which is used to calculate the position
     */
    private static int getBaseVal(final int minValue) {
        if (minValue < 0) {
            return minValue;
        } else {
            return 0;
        }
    }
    
    /**
     * This method is called every time a domain range (min or max) changes to
     * set the domain range and base value.
     */
    private void updateInternalData() {
        m_domainRange = createDomainRange(m_minDomainValue, m_maxDomainValue);
        m_baseVal = getBaseVal(m_minDomainValue);
    }

    
}
