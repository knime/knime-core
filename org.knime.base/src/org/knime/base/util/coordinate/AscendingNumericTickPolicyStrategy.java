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
 * ---------------------------------------------------------------------
 *
 * History
 *   Mar 17, 2008 (sellien): created
 */
package org.knime.base.util.coordinate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;

/**
 * A strategy which creates tick for numeric values in ascending order.
 *
 * @author Stephan Sellien, University of Konstanz
 */
public class AscendingNumericTickPolicyStrategy extends PolicyStrategy {

    private static final double EPSILON = 0.5;

    /**
     * ID for a ascending tick policy. Used as unique identifier in
     * {@link Coordinate}.
     */
    public static final String ID = "Ascending";

    /**
     * Creates a policy strategy for ascending order. The name is set to
     * "Ascending".
     *
     */
    public AscendingNumericTickPolicyStrategy() {
        super(ID);
    }

    /**
     * Creates a policy strategy for ascending order.
     *
     * @param name the name of this strategy
     */
    public AscendingNumericTickPolicyStrategy(final String name) {
        super(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double calculateMappedValue(final DataCell domainValueCell,
            final double absoluteLength, final double minDomainValue,
            final double maxDomainValue) {

        if (minDomainValue == maxDomainValue) {
            // only one value?
            return absoluteLength / 2;
        }

        double value = ((DoubleValue)domainValueCell).getDoubleValue();

        // is value really a useful double?
        if (Double.isNaN(value)) {
            return -1.0;
        }

        // only 1 value?
        if (minDomainValue == maxDomainValue) {
            return absoluteLength / 2;
        }

        double mappedValue =
                interpolatePosition(value, minDomainValue, maxDomainValue,
                        absoluteLength);

        return mappedValue;
    }

    /**
     * Interpolates the correct mapping for a given value.
     *
     * @param value the domain value to map
     * @param min minimum domain value
     * @param max maximum domain value
     * @param absLength the absolute length in pixel.
     * @return the mapped value as a position
     */
    protected double interpolatePosition(final double value, final double min,
            final double max, final double absLength) {

        double val = value;
        if (val == Double.POSITIVE_INFINITY) {
            val = Double.MAX_VALUE;
        } else if (val == Double.NEGATIVE_INFINITY) {
            val = -Double.MAX_VALUE;
        }

        double minimum = Math.min(min, max);
        minimum =
                (minimum == Double.NEGATIVE_INFINITY ? -Double.MAX_VALUE
                        : minimum);
        double maximum = Math.max(min, max);
        maximum =
                (maximum == Double.POSITIVE_INFINITY ? Double.MAX_VALUE
                        : maximum);

        return (val - minimum) / (maximum - minimum) * absLength;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CoordinateMapping[] getTickPositions(final double absoluteLength,
            final double minDomainValue, final double maxDomainValue,
            final double tickDistance) {

        if (minDomainValue == maxDomainValue) {
            double value = (minDomainValue + maxDomainValue) / 2;
            return new CoordinateMapping[]{new DoubleCoordinateMapping(""
                    + value, value, absoluteLength / 2)};
        }

        double minimum =
                (minDomainValue == Double.NEGATIVE_INFINITY ? -Double.MAX_VALUE
                        : minDomainValue);
        double maximum =
                (maxDomainValue == Double.POSITIVE_INFINITY ? Double.MAX_VALUE
                        : maxDomainValue);

        int count = (int)Math.floor(absoluteLength / tickDistance);

        Double[] values = makeTicks(minimum, maximum, count);
        count = values.length;
        CoordinateMapping[] coordMapping = new CoordinateMapping[count];

        for (int i = 0; i < count; i++) {
            double mappedValue =
                    calculateMappedValue(new DoubleCell(values[i]),
                            absoluteLength, minimum, maximum);
            String stringRepresentation = "" + values[i];
            if (values[i] <= getNegativeInfinity()) {
                stringRepresentation = "-Infinity";
            }
            if (values[i] >= getPositiveInfinity()) {
                stringRepresentation = "+Infinity";
            }
            coordMapping[i] =
                    new DoubleCoordinateMapping(stringRepresentation,
                            values[i], mappedValue);
            // System.out.println(stringRepresentation + " = " + values[i]
            // + " -> " + mappedValue);
        }
        return coordMapping;
    }

    private Double[] makeTicks(final double min, final double max,
            final int count) {

        double minimum = Math.min(min, max);
        minimum =
                (minimum <= Double.NEGATIVE_INFINITY ? -Double.MAX_VALUE
                        : minimum);
        double maximum = Math.max(min, max);
        maximum =
                (maximum >= Double.POSITIVE_INFINITY ? Double.MAX_VALUE
                        : maximum);

        if (count == 1) {
            return new Double[]{(minimum + maximum) / 2};
        }

        // so step is <= Double.MAX_VALUE

        double step = Math.log10((maximum - minimum) / count);
        double base = Math.floor(step);
        double frac = step - base;

        if (minimum == maximum || Double.isInfinite(step) || Double.isNaN(step)
                || Double.isInfinite(base) || Double.isNaN(base)
                || Double.isInfinite(frac) || Double.isNaN(frac)) {
            return new Double[]{(minimum + maximum) / 2};
        }

        ArrayList<Double> result = new ArrayList<Double>();

        if (frac < 0.1) {
            frac = 1;
        } else if (frac < 0.4) {
            frac = 2;
        } else if (frac < 0.6) {
            frac = 2.5;
        } else if (frac < 0.8) {
            frac = 5;
        } else {
            frac = 1;
            base += frac;
        }

        step = frac * Math.pow(10, base);

        double value = minimum;

        while (value + 0.55 * step < maximum) {
            if (result.size() == 0
                    || result.get(result.size() - 1) + EPSILON * step < value) {

                boolean add = true;
                for (DataValue v : getValues()) {
                    if (v instanceof DoubleValue) {
                        double desVal = ((DoubleValue)v).getDoubleValue();
                        if (value + EPSILON * step > desVal && value < desVal) {
                            add = false;
                        } else if (value - EPSILON * step < desVal
                                && value > desVal) {
                            add = false;
                        }
                    }
                }
                if (add) {
                    result.add(value);
                }
            }
            int m = 1;
            while (value + m * step == value) {
                m++;
            }
            value += m * step;
        }

        if (result.get(result.size() - 1) < maximum) {
            // enough space?
            if (result.get(result.size() - 1) + EPSILON * step > maximum) {
                result.remove(result.size() - 1);
            }
            result.add(maximum);
        }

        for (DataValue v : getValues()) {
            if (v instanceof DoubleValue) {
                double val = ((DoubleValue)v).getDoubleValue();
                if (!result.contains(val) && val > minimum && val < maximum) {
                    result.add(val);
                }
            }
        }

        Collections.sort(result);

        return result.toArray(new Double[0]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CoordinateMapping[] getTickPositions(final int absoluteLength,
            final int minDomainValue, final int maxDomainValue,
            final int tickDistance) {
        int nrTicks =
                Math.min(absoluteLength / tickDistance, (maxDomainValue
                        - minDomainValue + 1));
        if (nrTicks < 1) {
            nrTicks = 1;
        }
        int diff = Math.max((maxDomainValue - minDomainValue) / nrTicks, 1);

        List<CoordinateMapping> mapping = new LinkedList<CoordinateMapping>();
        for (int i = 0; minDomainValue + i * diff <= maxDomainValue; i++) {
            int domValue = minDomainValue + i * diff;
            if (domValue + diff > maxDomainValue) {
                domValue = maxDomainValue;
            }
            int mappedValue =
                    (int)calculateMappedValue(new IntCell(domValue),
                            absoluteLength, minDomainValue, maxDomainValue);

            mapping.add(new IntegerCoordinateMapping("" + domValue, domValue,
                    mappedValue));
        }

        return mapping.toArray(new CoordinateMapping[0]);
    }
}
