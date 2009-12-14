/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataValue;

/**
 * Abstract class for policy strategies.
 *
 * @author Stephan Sellien, University of Konstanz
 */
public abstract class PolicyStrategy {

    private Set<DataValue> m_values;

    private String m_displayName;

    private double m_positiveInfinity;

    private double m_negativeInfinity;

    /**
     * Creates a new {@link PolicyStrategy}.
     *
     * @param name the name of this strategy. Name <b>must not</b> be
     *            <code>null</code> or empty!
     */
    public PolicyStrategy(final String name) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("A strategy must have a name.");
        }
        m_displayName = name;
        m_positiveInfinity = Double.POSITIVE_INFINITY;
        m_negativeInfinity = Double.NEGATIVE_INFINITY;
    }

    /**
     * Sets desired values for the policy strategy.
     *
     * @param values the values
     */
    public void setValues(final DataValue... values) {
        m_values = new HashSet<DataValue>();
        m_values.addAll(Arrays.asList(values));
    }

    /**
     * Returns the values.
     *
     * @return the values or <code>null</code> if there are none
     */
    protected Set<DataValue> getValues() {
        return m_values;
    }

    /**
     * Calculates the mapped value.
     *
     * @param domainValueCell the value to be mapped
     * @param absoluteLength the absolute length
     * @param minDomainValue the minimal domain value
     * @param maxDomainValue the maximal domain value
     * @return the mapped value
     */
    public abstract double calculateMappedValue(final DataCell domainValueCell,
            final double absoluteLength, final double minDomainValue,
            final double maxDomainValue);

    /**
     * Calculates the mapped value. Additionally, values for infinity can be
     * changed.
     *
     * @param domainValueCell the value to be mapped
     * @param absoluteLength the absolute length
     * @param minDomainValue the minimal domain value
     * @param maxDomainValue the maximal domain value
     * @param negativeInfinity the value for negative infinity
     * @param positiveInfinity the value for positive infinity
     * @return the mapped value
     */
    public double calculateMappedValue(final DataCell domainValueCell,
            final double absoluteLength, final double minDomainValue,
            final double maxDomainValue, final double negativeInfinity,
            final double positiveInfinity) {
        m_negativeInfinity = negativeInfinity;
        m_positiveInfinity = positiveInfinity;
        return calculateMappedValue(domainValueCell, absoluteLength,
                minDomainValue, maxDomainValue);
    }

    /**
     * Calculates the mappings of the ticks according to the policy.
     *
     * @param absoluteLength the absolute length
     * @param minDomainValue the minimal domain value
     * @param maxDomainValue the maximal domain value
     * @param tickDistance the absolute distance between to ticks
     * @return the mappings of the ticks.
     */
    public abstract CoordinateMapping[] getTickPositions(double absoluteLength,
            double minDomainValue, double maxDomainValue, double tickDistance);

    /**
     * Calculates the mappings of the ticks according to the policy.
     * Additionally, values for infinity can be changed.
     *
     * @param absoluteLength the absolute length
     * @param minDomainValue the minimal domain value
     * @param maxDomainValue the maximal domain value
     * @param tickDistance the absolute distance between to ticks
     * @param negativeInfinity the value for negative infinity
     * @param positiveInfinity the value for positive infinity
     * @return the mappings of the ticks.
     */
    public CoordinateMapping[] getTickPositions(final double absoluteLength,
            final double minDomainValue, final double maxDomainValue,
            final double tickDistance, final double negativeInfinity,
            final double positiveInfinity) {
        m_negativeInfinity = negativeInfinity;
        m_positiveInfinity = positiveInfinity;
        return getTickPositions(absoluteLength, minDomainValue, maxDomainValue,
                tickDistance);
    }

    /**
     * Calculates the mappings of the ticks according to the policy.
     * Additionally, values for infinity can be changed.
     *
     * @param absoluteLength the absolute length
     * @param minDomainValue the minimal domain value
     * @param maxDomainValue the maximal domain value
     * @param tickDistance the absolute distance between to ticks
     * @param negativeInfinity the value for negative infinity
     * @param positiveInfinity the value for positive infinity
     * @return the mappings of the ticks.
     */
    public CoordinateMapping[] getTickPositions(final int absoluteLength,
            final int minDomainValue, final int maxDomainValue,
            final int tickDistance, final double negativeInfinity,
            final double positiveInfinity) {
        m_negativeInfinity = negativeInfinity;
        m_positiveInfinity = positiveInfinity;
        return getTickPositions(absoluteLength, minDomainValue, maxDomainValue,
                tickDistance);
    }

    /**
     * Calculates the mappings of the ticks according to the policy.
     *
     * @param absoluteLength the absolute length
     * @param minDomainValue the minimal domain value
     * @param maxDomainValue the maximal domain value
     * @param tickDistance the absolute distance between to ticks
     * @return the mappings of the ticks.
     */
    public abstract CoordinateMapping[] getTickPositions(int absoluteLength,
            int minDomainValue, int maxDomainValue, int tickDistance);

    /**
     * Returns the name of this strategy.
     *
     * @return the name
     */
    public String getDisplayName() {
        return m_displayName;
    }

    /**
     * Returns the value for positive infinity.
     *
     * @return the value for positive infinity
     */
    protected double getPositiveInfinity() {
        return m_positiveInfinity;
    }

    /**
     * Returns the value for negative infinity.
     *
     * @return the value for negative infinity.
     */
    protected double getNegativeInfinity() {
        return m_negativeInfinity;
    }

    /**
     * Returns whether mapping and relabeling by {@link MappingMethod}s should
     * be allowed for the ticks of this {@link PolicyStrategy}. The default
     * value is <code>true</code>. An example for allowed mapping would be the
     * a logarithmic scaling. An example where a mapping (logarithmic or
     * square root) does not make sense is the percentage policy.
     *
     * @return <code>true</code>, if labels could be relabeled,
     *         <code>false</code> else.
     */
    public boolean isMappingAllowed() {
        return true;
    }
}
