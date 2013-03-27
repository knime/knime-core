/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * -------------------------------------------------------------------
 */

package org.knime.core.node.defaultnodesettings;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.config.Config;
import org.knime.core.node.port.PortObjectSpec;

import java.awt.Color;


/**
 * {@link SettingsModel} implementation that holds a color value.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class SettingsModelColor extends SettingsModel {

    private static final String CNFG_RED = "Red";
    private static final String CNFG_GREEN = "Green";
    private static final String CNFG_BLUE = "Blue";
    private static final String CNFG_ALPHA = "Alpha";

    private final String m_configName;
    private Color m_value;

    /**Constructor for class SettingsModelColor that holds a color value.
     *
     * @param configName the identifier the value is stored with in the
     *            {@link org.knime.core.node.NodeSettings} object
     * @param defaultValue the initial value
     */
    public SettingsModelColor(final String configName,
            final Color defaultValue) {
        if ((configName == null) || (configName.isEmpty())) {
            throw new IllegalArgumentException("The configName must be a "
                    + "non-empty string");
        }
        m_configName = configName;
        m_value = defaultValue;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected SettingsModelColor createClone() {
        return new SettingsModelColor(getConfigName(), getColorValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getModelTypeID() {
        return "SMID_color";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getConfigName() {
        return m_configName;
    }

    /**
     * Set the value stored to the new value.
     *
     * @param newValue the new value to store
     */
    public void setColorValue(final Color newValue) {
        boolean sameValue;
        if (newValue == null) {
            sameValue = (m_value == null);
        } else {
            sameValue = newValue.equals(m_value);
        }
        m_value = newValue;

        if (!sameValue) {
            notifyChangeListeners();
        }
    }

    /**
     * @return the current value stored
     */
    public Color getColorValue() {
        return m_value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsForModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        final Config config = settings.getConfig(getConfigName());
        //check all color values
        final int red = config.getInt(CNFG_RED);
        final int green = config.getInt(CNFG_GREEN);
        final int blue = config.getInt(CNFG_BLUE);
        final int alpha = config.getInt(CNFG_ALPHA);
        if (red < 0 || green < 0 || blue < 0 || alpha < 0) {
            //the color is null
            return;
        }
        //constructor checks the value ranges
        new Color(red, green, blue, alpha);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        if (!settings.containsKey(getConfigName())) {
            // use the current value, if no value is stored in the settings
            setColorValue(m_value);
            return;
        }
        try {
            final Config config = settings.getConfig(getConfigName());
            final int red = config.getInt(CNFG_RED);
            final int green = config.getInt(CNFG_GREEN);
            final int blue = config.getInt(CNFG_BLUE);
            final int alpha = config.getInt(CNFG_ALPHA);
            Color color;
            if (red < 0 || green < 0 || blue < 0 || alpha < 0) {
                //the color is null
                color = null;
            } else {
                color = new Color(red, green, blue, alpha);
            }
            setColorValue(color);
        } catch (final IllegalArgumentException iae) {
            // if the argument is not accepted: keep the old value.
        } catch (final InvalidSettingsException e) {
            // if the argument is not accepted: keep the old value.
        } finally {
            // always notify the listeners. That is, because there could be an
            // invalid value displayed in the listener.
            notifyChangeListeners();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        try {
            // no default value, throw an exception instead
            final Config config = settings.getConfig(getConfigName());
            final int red = config.getInt(CNFG_RED);
            final int green = config.getInt(CNFG_GREEN);
            final int blue = config.getInt(CNFG_BLUE);
            final int alpha = config.getInt(CNFG_ALPHA);
            Color color;
            if (red < 0 || green < 0 || blue < 0 || alpha < 0) {
                //the color is null
                color = null;
            } else {
                color = new Color(red, green, blue, alpha);
            }
            setColorValue(color);
        } catch (final IllegalArgumentException iae) {
            throw new InvalidSettingsException(iae.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForModel(final NodeSettingsWO settings) {
        final Config config = settings.addConfig(getConfigName());
        final int red, green, blue, alpha;
        if (m_value != null) {
            red = m_value.getRed();
            green = m_value.getGreen();
            blue = m_value.getBlue();
            alpha = m_value.getAlpha();
        } else {
            red = -1;
            green = -1;
            blue = -1;
            alpha = -1;
        }
        config.addInt(CNFG_RED, red);
        config.addInt(CNFG_GREEN, green);
        config.addInt(CNFG_BLUE, blue);
        config.addInt(CNFG_ALPHA, alpha);
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
    public String toString() {
        return getClass().getSimpleName() + " ('" + getConfigName() + "'):"
        + (m_value == null ? "<none>" : m_value.toString());
    }
}
