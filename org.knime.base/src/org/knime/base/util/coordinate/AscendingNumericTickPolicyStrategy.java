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
 * ---------------------------------------------------------------------
 *
 * History
 *   Mar 17, 2008 (sellien): created
 */
package org.knime.base.util.coordinate;

import java.util.ArrayList;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;

/**
 * A strategy which creates tick for numeric values in ascending order.
 *
 * @author Stephan Sellien, University of Konstanz
 */
public class AscendingNumericTickPolicyStrategy extends PolicyStrategy {

    private static final double EPSILON = 0.5;

    /**
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

        int count = (int)Math.ceil(absoluteLength / tickDistance) + 1;

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

        while (value + EPSILON * step < maximum) {
            if (result.size() == 0
                    || result.get(result.size() - 1) + EPSILON * step < value) {

                boolean add = true;
                for (DataValue v : getValues()) {
                    if (v instanceof DoubleValue) {
                        double desVal = ((DoubleValue)v).getDoubleValue();
                        if (value + EPSILON * step > desVal
                                && value < desVal) {
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
            value += step;
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

        return result.toArray(new Double[0]);
    }
}
