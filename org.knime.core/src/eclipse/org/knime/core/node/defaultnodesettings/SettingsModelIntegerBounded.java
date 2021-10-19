/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ------------------------------------------------------------------------
 */
package org.knime.core.node.defaultnodesettings;

import org.knime.core.node.InvalidSettingsException;

/**
 * A settingsmodel for integer default components accepting double between a min and max value.
 *
 * @author ohl, University of Konstanz
 */
public class SettingsModelIntegerBounded extends SettingsModelInteger {

    private final int m_minValue;

    private final int m_maxValue;

    private final SettingsModelInteger settingsModelInteger =
        new SettingsModelInteger(super.getConfigName(), super.getIntValue());

    /**
     * Creates a new integer settings model with min and max value for the accepted int value.
     *
     * @param configName id the value is stored with in config objects.
     * @param defaultValue the initial value.
     * @param minValue lower bounds of the acceptable values.
     * @param maxValue upper bounds of the acceptable values.
     */
    public SettingsModelIntegerBounded(final String configName, final int defaultValue, final int minValue,
        final int maxValue) {
        super(configName, defaultValue);

        if (minValue > maxValue) {
            throw new IllegalArgumentException("Specified min value must be" + " smaller than the max value.");
        }

        m_minValue = minValue;
        m_maxValue = maxValue;

        // the actual value is the specified default value
        try {
            checkBounds(defaultValue);
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException("InitialValue: " + iae.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected SettingsModelIntegerBounded createClone() {
        return new SettingsModelIntegerBounded(getConfigName(), getIntValue(), m_minValue, m_maxValue);
    }

    /**
     * @return the lower bound of the acceptable values.
     */
    public int getLowerBound() {
        return m_minValue;
    }

    /**
     * @return the upper bound of the acceptable values.
     */
    public int getUpperBound() {
        return m_maxValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateValue(final int value) throws InvalidSettingsException {
        super.validateValue(value);
        try {
            checkBounds(value);
        } catch (IllegalArgumentException iae) {
            throw new InvalidSettingsException(iae.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setIntValue(final int newValue) {
        checkBounds(newValue);
        super.setIntValue(newValue);
    }

    private void checkBounds(final double val) {
        if ((val < m_minValue) || (m_maxValue < val)) {
            throw new IllegalArgumentException(
                "Value (=" + val + ") must be within the range [" + m_minValue + "..." + m_maxValue + "].");
        }
    }
}
