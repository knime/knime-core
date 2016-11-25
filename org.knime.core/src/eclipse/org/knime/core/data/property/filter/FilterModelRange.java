/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   Oct 25, 2016 (wiswedel): created
 */
package org.knime.core.data.property.filter;

import java.util.OptionalDouble;
import java.util.UUID;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.core.data.DataCell;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.core.node.util.CheckUtils;

/**
 * A FilterModel representing a numerical range of values described by minimum and maximum. Instances of this object
 * are constructed via {@link FilterModel#newRangeModel(double, double, boolean, boolean)}.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @since 3.3
 */
public final class FilterModelRange extends FilterModel {

    private final OptionalDouble m_minimum;
    private final OptionalDouble m_maximum;
    private final boolean m_minimumInclusive;
    private final boolean m_maximumInclusive;

    /** Constructor for {@link FilterModelRange#newRangeModel(double, double, boolean, boolean)}. */
    FilterModelRange(final double minimum, final double maximum,
        final boolean minimumInclusive, final boolean maximumInclusive) {
        this(null, minimum, maximum, minimumInclusive, maximumInclusive);
    }

    /** Load constructor. */
    private FilterModelRange(final UUID filterUUID, final double minimum, final double maximum,
        final boolean minimumInclusive, final boolean maximumInclusive) {
        super(filterUUID);
        m_minimum = Double.isNaN(minimum) || (minimum < 0.0 && Double.isInfinite(minimum))
                ? OptionalDouble.empty() : OptionalDouble.of(minimum);
        m_maximum = Double.isNaN(maximum) || (maximum > 0.0 && Double.isInfinite(maximum))
                ? OptionalDouble.empty() : OptionalDouble.of(maximum);

        CheckUtils.checkArgument(m_minimum.isPresent() || m_maximum.isPresent(),
            "Either minimum or maximum need to be specified");

        CheckUtils.checkArgument(
            m_minimum.orElse(Double.NEGATIVE_INFINITY) <= m_maximum.orElse(Double.POSITIVE_INFINITY),
            "Minimum not smaller/equal to maximum: min: %d; max: %d", minimum, maximum);

        m_minimumInclusive = minimumInclusive;
        m_maximumInclusive = maximumInclusive;
    }

    /**
     * @return the minimum value or an empty Optional if unbounded.
     */
    public OptionalDouble getMinimum() {
        return m_minimum;
    }

    /**
     * @return the maximum value or an empty Optional if unbounded.
     */
    public OptionalDouble getMaximum() {
        return m_maximum;
    }

    /**
     * @return the minimumInclusive
     */
    public boolean isMinimumInclusive() {
        return m_minimumInclusive;
    }

    /**
     * @return the maximumInclusive
     */
    public boolean isMaximumInclusive() {
        return m_maximumInclusive;
    }

    /** Checks if the argument implements {@link DoubleValue} and then checks the range.
     * {@inheritDoc} */
    @Override
    public boolean isInFilter(final DataCell cell) {
        if (!(cell instanceof DoubleValue)) {
            return false;
        }
        double d = ((DoubleValue)cell).getDoubleValue();
        boolean minimumOK;
        if (m_minimum.isPresent()) {
            minimumOK = m_minimumInclusive ? d >= m_minimum.getAsDouble() : d > m_minimum.getAsDouble();
        } else {
            minimumOK = true;
        }
        boolean maximumOK;
        if (m_maximum.isPresent()) {
            maximumOK = m_maximumInclusive ? d <= m_maximum.getAsDouble() : d < m_maximum.getAsDouble();
        } else {
            maximumOK = true;
        }
        return minimumOK && maximumOK;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(m_minimumInclusive ? "[" : "(");
        b.append(m_minimum.orElse(Double.NEGATIVE_INFINITY)).append(", ");
        b.append(m_maximum.orElse(Double.POSITIVE_INFINITY));
        b.append(m_maximumInclusive ? "]" : ")");
        b.append(" (");
        b.append(super.toString());
        b.append(")");
        return b.toString();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder b = new HashCodeBuilder();
        b.appendSuper(super.hashCode()).append(m_minimum).append(m_maximum);
        b.append(m_maximumInclusive).append(m_maximumInclusive);
        return b.toHashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof FilterModelRange)) {
            return false;
        }
        FilterModelRange fobj = (FilterModelRange)obj;
        EqualsBuilder equalsBuilder = new EqualsBuilder();
        equalsBuilder.appendSuper(super.equals(fobj));
        equalsBuilder.append(m_minimum, fobj.m_minimum);
        equalsBuilder.append(m_maximum, fobj.m_maximum);
        equalsBuilder.append(m_minimumInclusive, fobj.m_minimumInclusive);
        equalsBuilder.append(m_maximumInclusive, fobj.m_maximumInclusive);
        return equalsBuilder.isEquals();
    }


    @Override
    void saveSubclass(final ConfigWO config) {
        config.addDouble("minimum", m_minimum.orElse(Double.NaN));
        config.addDouble("maximum", m_maximum.orElse(Double.NaN));
        config.addBoolean("minimumInclusive", m_minimumInclusive);
        config.addBoolean("maximumInclusive", m_maximumInclusive);
    }

    /** Load factory method.
     * @param filterUUID Non-null ID as loaded by super class.
     * @param config Non-null config to read values from.
     * @return A new Filter.
     * @throws InvalidSettingsException ...
     */
    static FilterModelRange loadSubclass(final UUID filterUUID, final ConfigRO config)
            throws InvalidSettingsException {
        double min = config.getDouble("minimum");
        double max = config.getDouble("maximum");
        boolean minimumInclusive = config.getBoolean("minimumInclusive");
        boolean maximumInclusive = config.getBoolean("maximumInclusive");
        return new FilterModelRange(filterUUID, min, max, minimumInclusive, maximumInclusive);
    }
}
