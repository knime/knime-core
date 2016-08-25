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
 *   Aug 23, 2016 (oole): created
 */
package org.knime.core.node.defaultnodesettings;

import java.text.ParseException;

import javax.swing.text.MaskFormatter;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 * The SettingsModel for the default duration dialog component.
 *
 * @author Ole Ostergaard, KNIME.com GmbH
 * @since 3.3
 */
public final class SettingsModelDuration extends SettingsModel {

    private String m_durationString;

    private final String m_configName;

    private MaskFormatter m_maskFormat;


    /**
     * Creates a new object with default mask ##d##h##m##s and the default duration string 00d00h00m00s
     *
     * @param configName the identifier the value is stored with in the {@link org.knime.core.node.NodeSettings} object
     */
    public SettingsModelDuration(final String configName) {
        this(configName, "##d##h##m##s", "00d00h00m00s");
    }

    /**
     * Create a new object with the given values for days, hours, minutes, seconds
     *
     * @param configName the identifier the value is stored with in the org.knime.core.node {@link NodeSettings} object
     * @param mask the mask that should be used for the input it has to include days, hours, minutes, seconds.
     *      Example: ##d##h##m##s . The Amount of #'s can be varied
     * @param defaultString the default string that should be set in the duration field
     */
    public SettingsModelDuration(final String configName, final String mask, final String defaultString) {
        if ((configName == null) || "".equals(configName)) {
            throw new IllegalArgumentException("The configName must be a non-empty string");
        }
        try {
            m_maskFormat = new MaskFormatter(mask);
            m_maskFormat.setPlaceholderCharacter('0');
        } catch (ParseException ex) {
            throw new IllegalArgumentException("THe given mask can not be parsed correctly");
        }
        if ( mask.length() != defaultString.length()) {
            throw new IllegalArgumentException("The mask must correspond to the defaultString");
        }

        m_configName = configName;
        m_durationString = defaultString;
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected SettingsModelDuration createClone() {
        return new SettingsModelDuration(m_configName, m_maskFormat.getMask(), m_durationString);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getModelTypeID() {
        return "SMID_DURATION";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getConfigName() {
        return m_configName;
    }

    /** Set the duration string for the settings model
     * @param duration the duration string to be set for the settings model
     */
    public void setDurationString(final String duration) {
       m_durationString = duration;
   }

    /**
     * Returns the duration string for this settings model
     *
     * @return the duration string for this settings model
     */
    public String getDurationString() {
        return m_durationString;
    }

    /**
     * Returns the mask for this setting model
     * @return the {@link MaskFormatter} for the dialog component
     */
    public MaskFormatter getMask() {
        return m_maskFormat;
    }
    /**
     * Returns the stored string's days value
     * @return the stored days value
     */
    public String getDays() {
        int dayIndex = m_durationString.indexOf('d');
        return m_durationString.substring(0, dayIndex);
    }

    /**
     * Returns the stored string's hours value
     *
     * @return the stored hours value
     */
    public String getHours() {
        int dayIndex = m_durationString.indexOf('d');
        int hourIndex = m_durationString.indexOf('h');
        return m_durationString.substring(dayIndex+1, hourIndex);
    }

    /**
     * Returns the stored string's minutes value
     *
     * @return the stored minutes value
     */
    public String getMinutes() {
        int hourIndex = m_durationString.indexOf('h');
        int minuteIndex = m_durationString.indexOf('m');
        return m_durationString.substring(hourIndex+1, minuteIndex);
    }

    /**
     * Returns the stored string's seconds value
     *
     * @return the stored seconds value
     */
    public String getSeconds() {
        int minuteIndex = m_durationString.indexOf('m');
        int secondIndex = m_durationString.indexOf('s');
        return m_durationString.substring(minuteIndex+1,secondIndex);
    }

    /**
     * Returns the total calculated duration in milliseconds
     * @return the total duration in milliseconds
     */
    public long getDurationInMillis() {
        return (Integer.parseInt(getDays()) * 24l * 60l * 60l * 100l) + (Integer.parseInt(getHours()) * 60l * 60l * 1000l) + (Integer.parseInt(getMinutes()) * 60l * 1000l) + (Integer.parseInt(getSeconds()) * 1000l);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_durationString = settings.getString(m_configName, m_durationString);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        saveSettingsForModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        settings.getString(m_configName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_durationString = settings.getString(m_configName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForModel(final NodeSettingsWO settings) {
        settings.addString(m_configName, m_durationString);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return m_durationString;
    }



}
