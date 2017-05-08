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
 * ------------------------------------------------------------------------
 */
package org.knime.core.node.defaultnodesettings;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 * {@link SettingsModel} implementation that stores an encrypted string.
 *
 * @author Tobias Koetter, KNIME.com
 * @since 3.4
 */
public class SettingsModelPassword extends SettingsModelString {

    /**The encryption key used when ever we work with the NodeSettings classes.*/
    private final String m_encryptionKey;

    /**
     * Creates a new object holding a string value.
     *
     * @param configName the identifier the value is stored with in the
     *            {@link org.knime.core.node.NodeSettings} object
     * @param encryptionKey key used for encrypting the password
     * @param defaultValue the initial value
     */
    public SettingsModelPassword(final String configName, final String encryptionKey,
            final String defaultValue) {
        super(configName, defaultValue);
        if (encryptionKey == null || encryptionKey.isEmpty()) {
            throw new IllegalArgumentException("The encryption key must not be null or empty");
        }
        m_encryptionKey = encryptionKey;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    protected String getModelTypeID() {
        return "SMID_password";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected SettingsModelPassword createClone() {
        return new SettingsModelPassword(getConfigName(), m_encryptionKey, getStringValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsForModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        settings.getPassword(getConfigName(), m_encryptionKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        try {
            // no default value, throw an exception instead
            setStringValue(settings.getPassword(getConfigName(), m_encryptionKey));
        } catch (final IllegalArgumentException iae) {
            throw new InvalidSettingsException(iae.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        try {
            // use the current value, if no value is stored in the settings
            setStringValue(settings.getPassword(getConfigName(), m_encryptionKey, getStringValue()));
        } catch (final IllegalArgumentException iae) {
            // if the argument is not accepted: keep the old value.
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForModel(final NodeSettingsWO settings) {
        settings.addPassword(getConfigName(), m_encryptionKey, getStringValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForDialog(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        settings.addPassword(getConfigName(), m_encryptionKey, getStringValue());
    }
}
