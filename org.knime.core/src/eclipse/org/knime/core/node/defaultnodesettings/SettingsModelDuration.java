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

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 * The SettingsModel for the default time dialog component.
 *
 * @author Ole Ostergaard, KNIME.com GmbH
 */
public class SettingsModelDuration extends SettingsModel {

    private int m_days;
    private int m_hours;
    private int m_minutes;
    private int m_seconds;

    private final String m_configName;

    private final String m_daysName;
    private final String m_hoursName;
    private final String m_minutesName;
    private final String m_secondsName;

    /**
     * Creates a new object holding with default values 0 days, 0 hours, 0 minutes, 0 seconds
     *
     * @param configName the identifier the value is stored with in the {@link org.knime.core.node.NodeSettings} object
     */
    public SettingsModelDuration(final String configName) {
        this(configName, 0, 0, 0, 0);
    }

    /**
     * Create a new object with the given values for days, hours, minutes, seconds
     *
     * @param configName the identifier the value is stored with in the org.knime.core.node {@link NodeSettings} object
     * @param days the initial value for days
     * @param hours the initial value for hours
     * @param minutes the initial value for minutes
     * @param seconds the initial value for seconds
     */
    public SettingsModelDuration(final String configName,final int days, final int hours, final int minutes, final int seconds) {
        if ((configName == null) || "".equals(configName)) {
            throw new IllegalArgumentException("The configName must be a non-empty string");
        }
        m_configName = configName;
        m_daysName = "days_" + configName;
        m_hoursName = "hours_" + configName;
        m_minutesName = "minutes_" + configName;
        m_secondsName = "seconds_" + configName;
        m_days = days;
        m_hours = hours;
        m_minutes = minutes;
        m_seconds = seconds;
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected SettingsModelDuration createClone() {
        return new SettingsModelDuration(m_configName, m_days, m_hours, m_minutes, m_seconds);
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

    /**
     * Sets the days value stored to the new value. Notifies all registered listeners
     * if the new value is different from the old one.
     *
     * @param days the new days value to be stored
     */
    public void setDaysValue(final int days) {
        boolean changed = (days != m_days);
        m_days = days;
        if (changed) {
            notifyChangeListeners();
        }
    }

    /**
     * Set the hours value stored to the new value. Notifies all registered listeners
     * if the new value is different from the old one.
     *
     * @param hours the new hours value to be stored
     */
    public void setHoursValue(final int hours) {
        boolean changed = (hours != m_hours);
        m_hours = hours;
        if (changed) {
            notifyChangeListeners();
        }
    }

    /**
     * Set the hours value stored to the new value. Notifies all registered listeners
     * if the new value is different from the old one.
     *
     * @param minutes the new minutes value to be stored
     */
    public void setMinutesValue(final int minutes) {
        boolean changed = (minutes != m_minutes);
        m_minutes = minutes;
        if (changed) {
            notifyChangeListeners();
        }
    }

    /**
     * Set the hours value stored to the new value. Notifies all registered listeners
     * if the new value is different from the old one.
     *
     * @param seconds the new seconds value to be stored
     */
    public void setSecondsValue(final int seconds) {
        boolean changed = (seconds != m_seconds);
        m_seconds = seconds;
        if (changed) {
            notifyChangeListeners();
        }
    }

    /**
     * Returns the stored days value
     * @return the stored days value
     */
    public int getDaysValue() {
        return m_days;
    }

    /**
     * Returns the stored hours value
     *
     * @return the stored hours value
     */
    public int getHoursValue() {
        return m_hours;
    }

    /**
     * Returns the stored minutes value
     *
     * @return the stored minutes value
     */
    public int getMinutesValue() {
        return m_minutes;
    }

    /**
     * Returns the stored seconds value
     *
     * @return the stored seconds value
     */
    public int getSecondsValue() {
        return m_seconds;
    }

    /**
     * Returns the total calculated amount of time in milliseconds
     * @return the total amount of time in milliseconds
     */
    public long getDurationInMillis() {
        return (m_days * 24l * 60l * 60l * 100l) + (m_hours * 60l * 60l * 1000l) + (m_minutes * 60l * 1000l) + (m_seconds * 1000l);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        setDaysValue(settings.getInt(m_daysName, m_days));
        setHoursValue(settings.getInt(m_hoursName, m_hours));
        setMinutesValue(settings.getInt(m_minutesName, m_minutes));
        setSecondsValue(settings.getInt(m_secondsName, m_seconds));
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
        settings.getInt(m_daysName, m_days);
        settings.getInt(m_hoursName, m_hours);
        settings.getInt(m_minutesName, m_minutes);
        settings.getInt(m_secondsName, m_seconds);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        setDaysValue(settings.getInt(m_daysName));
        setHoursValue(settings.getInt(m_hoursName));
        setMinutesValue(settings.getInt(m_minutesName));
        setSecondsValue(settings.getInt(m_secondsName));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForModel(final NodeSettingsWO settings) {
        settings.addInt(m_daysName, m_days);
        settings.addInt(m_hoursName, m_hours);
        settings.addInt(m_minutesName, m_minutes);
        settings.addInt(m_secondsName, m_seconds);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Days: " + getDaysValue() + "Hours: " + getHoursValue() +", Minutes: " + getMinutesValue() + ", Seconds: " + getSecondsValue();
    }


}
