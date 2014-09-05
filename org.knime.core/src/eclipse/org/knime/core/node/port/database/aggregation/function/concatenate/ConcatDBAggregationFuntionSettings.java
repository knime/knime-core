/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 */

package org.knime.core.node.port.database.aggregation.function.concatenate;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;


/**
 * Class that save the settings of the {@link ConcatDBAggregationFuntionSettingsPanel}.
 *
 * @author Tobias Koetter
 * @since 2.11
 */
public class ConcatDBAggregationFuntionSettings {

    private static final String CFG_SEPARATOR = "groupSeparator";

    private final SettingsModelString m_separator;

    /**
     * @param separator the default separator
     */
    public ConcatDBAggregationFuntionSettings(final String separator) {
        m_separator = new SettingsModelString(CFG_SEPARATOR, separator);
    }

    /**
     * @return the separator model
     */
    SettingsModelString getSeparatorModel() {
        return m_separator;
    }

    /**
     * @param settings the {@link NodeSettingsRO} to read the settings from
     * @throws InvalidSettingsException if the settings are invalid
     */
    public void validateSettings(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        String val = ((SettingsModelString) m_separator.createCloneWithValidatedValue(settings)).getStringValue();
        if (val == null || val.trim().isEmpty()) {
            throw new InvalidSettingsException("Please specify the group separator");
        }
    }

    /**
     * @param settings the {@link NodeSettingsRO} to read the settings from
     * @throws InvalidSettingsException if the settings are invalid
     */
    public void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_separator.loadSettingsFrom(settings);
    }
    /**
     * @param settings the {@link NodeSettingsWO} to write to
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        m_separator.saveSettingsTo(settings);
    }

    /**
     * @return the separator
     */
    public String getSeparator() {
        return m_separator.getJavaUnescapedStringValue();
    }

    /**
     * @return a clone of this settings object
     */
    public ConcatDBAggregationFuntionSettings createClone() {
        return new ConcatDBAggregationFuntionSettings(m_separator.getStringValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_separator == null) ? 0 : m_separator.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ConcatDBAggregationFuntionSettings other = (ConcatDBAggregationFuntionSettings)obj;
        if (m_separator == null) {
            if (other.m_separator != null) {
                return false;
            }
        } else if (!m_separator.equals(other.m_separator)) {
            return false;
        }
        return true;
    }
}
