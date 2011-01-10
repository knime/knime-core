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
