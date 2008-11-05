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
 *   Mar 20, 2008 (sellien): created
 */
package org.knime.base.util.coordinate;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;

/**
 * A logarithmic mapping method. This will be applied to the data before
 * creating ticks.
 *
 * @author Stephan Sellien, University of Konstanz
 */
public class LogarithmicMappingMethod implements MappingMethod {

    /**
     * Identifier for a logarithmic mapping method with base e ( ln ).
     */
    public static final String ID_BASE_E = "lnMappingMethod";

    /**
     * Identifier for a logarithmic mapping method with base 2 ( ld ).
     */
    public static final String ID_BASE_2 = "ldMappingMethod";

    /**
     * Identifier for a logarithmic mapping method with base 10 ( log ).
     */
    public static final String ID_BASE_10 = "logMappingMethod";

    private double m_base;

    private double m_logBase;

    /**
     * Creates a logarithmic mapping method. The standard base is e.
     */
    public LogarithmicMappingMethod() {
        m_logBase = 1; // log_e(e) = 1
        m_base = Math.E;
    }

    /**
     * Creates a logarithmic mapping method with the given base.
     *
     * @param base the base of the logarithm
     */
    public LogarithmicMappingMethod(final double base) {
        if (base <= 0.0) {
            throw new IllegalArgumentException(
                    "Base of logarithm must be greater than 0.");
        }
        m_logBase = Math.log(base);
        m_base = base;
    }

    /**
     * {@inheritDoc}
     */
    public DataCell doMapping(final DataCell in) {
        if (!(in.getType().isCompatible(DoubleValue.class))) {
            // we can only map double values
            return in;
        }
        double value = ((DoubleValue)in).getDoubleValue();
        if (value <= 0.0) {
            // should not get this input at all
            // no guarantee for output..
            return in;
        }
        if (Double.isInfinite(value)) {
            value = Double.MAX_VALUE;
        }
        value = Math.log(value) / m_logBase;

        return new DoubleCell(value);
    }

    /**
     * {@inheritDoc}
     */
    public String getDisplayName() {
        if (m_base == Math.E) { // natural logarithm
            return "ln(x)";
        } else if (m_base == 2.0) { // binary logarithm
            return "ld(x)";
        } else {
            return "log_" + m_base + "(x)";
        }
    }

    /**
     * {@inheritDoc}
     *
     * The logarithmic mapping method is usable if lower bound is greater or
     * equal 0 and the upper bound is greater than 1 for scaling reasons.
     */
    public boolean isCompatibleWithDomain(final DataColumnDomain domain) {
        if (domain == null || !domain.hasBounds()) {
            return false;
        }
        if (domain.getLowerBound().getType().isCompatible(DoubleValue.class)
                && domain.getUpperBound().getType().isCompatible(
                        DoubleValue.class)) {
            double upperBound =
                    ((DoubleValue)domain.getUpperBound()).getDoubleValue();
            double lowerBound =
                    ((DoubleValue)domain.getLowerBound()).getDoubleValue();
            if (lowerBound >= 0 && upperBound > 1) {
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
                "DataCell must not be null and of type DoubleValue!");
        }
        double value = ((DoubleValue)cell).getDoubleValue();

        return Math.pow(m_base, value);
    }
}
