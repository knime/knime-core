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
 *   21 Oct 2016 (albrecht): created
 */
package org.knime.core.node.util.dialog.field;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;

/**
 * Settings for dialog out flow variable list field with a default value.
 *
 * <p>This class might change and is not meant as public API.
 *
 * @author Christian Albrecht, KNIME.com GmbH, Konstanz, Germany
 * @since 3.3
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
public class DefaultOutFlowVariableField extends OutFlowVariableField {

    private static final String DEFAULT_VALUE = "defaultValue";

    private String m_defaultValueString;
    private double m_defaultValueDouble;
    private int m_defaultValueInt;

    /**
     * @param defaultValue the defaultValue to set
     */
    public void setDefaultValue(final String defaultValue) {
        m_defaultValueString = defaultValue;
    }

    public void setDefaultValue(final double defaultValue) {
        m_defaultValueDouble = defaultValue;
    }

    public void setDefaultValue(final int defaultValue) {
        m_defaultValueInt = defaultValue;
    }

    public String getDefaultValueString() {
        return m_defaultValueString;
    }

    public double getDefaultValueDouble() {
        return m_defaultValueDouble;
    }

    public int getDefaultValueInt() {
        return m_defaultValueInt;
    }

    public Object getDefaultValue() {
        switch (getKnimeType()) {
            case INTEGER:
                return m_defaultValueInt;
            case DOUBLE:
                return m_defaultValueDouble;
            default:
                return m_defaultValueString;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveSettings(final Config config) {
        super.saveSettings(config);
        switch (getKnimeType()) {
            case INTEGER:
                config.addInt(DEFAULT_VALUE, m_defaultValueInt);
                break;
            case DOUBLE:
                config.addDouble(DEFAULT_VALUE, m_defaultValueDouble);
                break;
            default:
                config.addString(DEFAULT_VALUE, m_defaultValueString);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadSettings(final Config config) throws InvalidSettingsException {
        super.loadSettings(config);
        switch (getKnimeType()) {
            case INTEGER:
                m_defaultValueInt = config.getInt(DEFAULT_VALUE);
                break;
            case DOUBLE:
                m_defaultValueDouble = config.getDouble(DEFAULT_VALUE);
                break;
            default:
                m_defaultValueString = config.getString(DEFAULT_VALUE);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadSettingsForDialog(final Config config) {
        super.loadSettingsForDialog(config);
        if (getKnimeType() != null) {
            switch (getKnimeType()) {
                case INTEGER:
                    m_defaultValueInt = config.getInt(DEFAULT_VALUE, 0);
                    break;
                case DOUBLE:
                    m_defaultValueDouble = config.getDouble(DEFAULT_VALUE, 0.0);
                    break;
                default:
                    m_defaultValueString = config.getString(DEFAULT_VALUE, null);
            }
        }
    }

}
