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
 *   21.07.2006 (koetter): created
 *   25.03.2008 (sellien): redesigned
 */
package org.knime.base.util.coordinate;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;

/**
 *
 * @author Tobias Koetter, University of Konstanz
 * @author Stephan Sellien, University of Konstanz
 */
class IntegerCoordinate extends NumericCoordinate {

    /**
     * Constructor for class IntegerCoordinate.
     *
     * @param dataColumnSpec the column specification
     */
    protected IntegerCoordinate(final DataColumnSpec dataColumnSpec) {
        super(dataColumnSpec);

        setPolicy(getPolicyStategy("Ascending"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CoordinateMapping[] getTickPositionsInternal(
            final double absoluteLength) {
        PolicyStrategy strategy = getCurrentPolicy();
        if (strategy != null) {
            strategy.setValues(getDesiredValues());
            CoordinateMapping[] mapping =
                    strategy.getTickPositions(absoluteLength,
                            getMinDomainValue(), getMaxDomainValue(),
                            DoubleCoordinate.DEFAULT_ABSOLUTE_TICK_DIST);
            List<CoordinateMapping> mappings =
                    new ArrayList<CoordinateMapping>();
            boolean hasNegInfinity = false;
            boolean hasPosInfinity = false;
            for (CoordinateMapping map : mapping) {
                try {
                    double doubleVal =
                            Double.parseDouble(map.getDomainValueAsString());

                    int value = (int)doubleVal;
                    if (doubleVal <= Integer.MIN_VALUE) {
                        if (!hasNegInfinity) {
                            mappings.add(new DoubleCoordinateMapping(
                                    "-Infinity", doubleVal, map
                                            .getMappingValue()));
                            hasNegInfinity = true;
                        }
                    } else if (doubleVal >= Integer.MAX_VALUE) {
                        if (!hasPosInfinity) {
                            mappings.add(new DoubleCoordinateMapping(
                                    "+Infinity", doubleVal, map
                                            .getMappingValue()));
                            hasPosInfinity = true;
                        }
                    } else {
                        if (doubleVal % 1 == 0) {
                            // is integer
                            mappings.add(new IntegerCoordinateMapping(""
                                    + value, value, map.getMappingValue()));
                        }
                    }

                } catch (NumberFormatException e) {
                    // do nothing
                }
            }
            if (mappings.size() == 0) {
                // well .. no Integers found, use double scale
                return mapping;
            }
            return mappings.toArray(new CoordinateMapping[0]);
        }

        return new CoordinateMapping[]{new IntegerCoordinateMapping("0", 0,
                absoluteLength / 2)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double calculateMappedValueInternal(final DataCell domainValueCell,
            final double absoluteLength) {
        return getCurrentPolicy().calculateMappedValue(domainValueCell,
                absoluteLength, getMinDomainValue(), getMaxDomainValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMinDomainValueSet() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMaxDomainValueSet() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMinDomainValue(final double value) {
        if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
            throw new IllegalArgumentException("Value to big or small for "
                    + IntegerCoordinate.class.getName());
        }
        super.setMinDomainValue((int)value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMaxDomainValue(final double value) {
        if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
            throw new IllegalArgumentException("Value to big or small for "
                    + IntegerCoordinate.class.getName());
        }
        super.setMaxDomainValue((int)value);
    }

}
