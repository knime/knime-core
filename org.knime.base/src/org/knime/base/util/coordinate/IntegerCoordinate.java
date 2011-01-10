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
 *   21.07.2006 (koetter): created
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
    protected CoordinateMapping[] getTickPositionsInternal(
            final double absoluteLength) {
        PolicyStrategy strategy = getCurrentPolicy();
        if (strategy != null) {
            strategy.setValues(getDesiredValues());
            CoordinateMapping[] mapping = null;
            if (getMinDomainValue() % 1 == 0 && getMaxDomainValue() % 1 == 0) {
                mapping =
                        strategy.getTickPositions((int)absoluteLength,
                                (int)getMinDomainValue(),
                                (int)getMaxDomainValue(),
                                Coordinate.DEFAULT_ABSOLUTE_TICK_DIST,
                                getNegativeInfinity(), getPositiveInfinity());
            } else {
                mapping =
                        strategy.getTickPositions((int)absoluteLength,
                                getMinDomainValue(), getMaxDomainValue(),
                                Coordinate.DEFAULT_ABSOLUTE_TICK_DIST,
                                getNegativeInfinity(), getPositiveInfinity());
            }
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
                        // if (doubleVal % 1 == 0) {
                        // is integer
                        mappings.add(new IntegerCoordinateMapping("" + value,
                                value, map.getMappingValue()));
                        // }
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
    protected double calculateMappedValueInternal(
            final DataCell domainValueCell, final double absoluteLength) {
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
