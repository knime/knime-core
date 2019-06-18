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
 *
 */
package org.knime.core.node.defaultnodesettings;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 * SettingsModel for random seeds.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @since 4.0
 */
public class SettingsModelSeed extends SettingsModelLong {

    private boolean m_active;

    /**
     * @param configName the key for the settings
     * @param defaultValue default value
     * @param active if the box should be active
     *
     */
    public SettingsModelSeed(final String configName,
            final long defaultValue, final boolean active) {
        super(configName, defaultValue);
        m_active = active;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SettingsModelSeed createClone() {
        return new SettingsModelSeed(getConfigName(),
                getLongValue(), m_active);
    }


    /**
     *
     * @return if the seed is active
     */
    public boolean getIsActive() {
        return m_active;
    }

    /**
     * sets if the seed should be used.
     * @param b false if a random random seed should be used.
     */
    public void setIsActive(final boolean b) {
        boolean notify = (b != m_active);

        m_active = b;

        if (notify) {
            notifyChangeListeners();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getModelTypeID() {
        return "SMN_long_seed";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        try {
            super.loadSettingsForDialog(settings, specs);
            // use the current value, if no value is stored in the settings
            setIsActive(settings.getBoolean(getbooleanConfigkey(), m_active));
        } catch (IllegalArgumentException e) {
            // if the value is not accepted, keep the old value.
        }
    }

    private String getbooleanConfigkey() {
        return super.getConfigName() + "_BOOL";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        try {
            // use the current value, if no value is stored in the settings
            super.loadSettingsForModel(settings);
            setIsActive(settings.getBoolean(getbooleanConfigkey(), m_active));
        } catch (IllegalArgumentException e) {
            // if the value is not accepted, keep the old value.
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForDialog(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        saveSettingsForModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForModel(final NodeSettingsWO settings) {
        super.saveSettingsForModel(settings);
        settings.addBoolean(getbooleanConfigkey(), m_active);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + " ('" + m_active + " - "
                            + super.getConfigName() + "')";
    }

    /**
     * @return if the boolean is active, the stored seed is returned
     * otherwise a random long value.
     */
    public long getSeedOrRandom() {
        if (m_active) {
            return getLongValue();
        }
        final long l1 = Double.doubleToLongBits(Math.random());
        final long l2 = Double.doubleToLongBits(Math.random());
        return ((0xFFFFFFFFL & l1) << 32)
            + (0xFFFFFFFFL & l2);
    }

}
