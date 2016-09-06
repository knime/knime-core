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

import java.time.Duration;

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

    private Duration m_duration;

    private String m_days;
    private String m_hours;
    private String m_minutes;
    private String m_seconds;

    private final String m_configName;

    /**
     * Creates a new object holding with default values 0 days, 0 hours, 0 minutes, 0 seconds
     *
     * @param configName the identifier the value is stored with in the {@link org.knime.core.node.NodeSettings} object
     */
    public SettingsModelDuration(final String configName) {
        this(configName, Duration.ZERO);
    }

    /**
     * Create a new object with the given values for days, hours, minutes, seconds
     *
     * @param configName the identifier the value is stored with in the org.knime.core.node {@link NodeSettings} object
     * @param duration The initial duration value
     */
    public SettingsModelDuration(final String configName,final Duration duration) {
        if ((configName == null) || "".equals(configName)) {
            throw new IllegalArgumentException("The configName must be a non-empty string");
        }
        m_configName = configName;
        m_duration = duration;
        durationToUnits();
    }

    /**
     * Update the duration according to the units stored in the model
     */
    private void durationToUnits() {
        m_days = String.valueOf(m_duration.toDays());
        Duration durationMinusDays = m_duration.minusDays(Long.parseLong(m_days));
        m_hours = String.valueOf(durationMinusDays.toHours());
        Duration durationMinusHours = durationMinusDays.minusHours(Long.parseLong(m_hours));
        m_minutes = String.valueOf(durationMinusHours.toMinutes());
        Duration durationMinusSeconds = durationMinusHours.minusMinutes(Long.parseLong(m_minutes));
        m_seconds = String.valueOf(durationMinusSeconds.getSeconds());
    }

    /**
     * Update the units according to the duration stored in the model
     */
    private void unitsToDuration() {
        m_duration = Duration.ZERO.plusDays(Long.parseLong(m_days)).plusHours(Long.parseLong(m_hours)).plusMinutes(Long.parseLong(m_minutes)).plusSeconds(Long.parseLong(m_seconds));
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected SettingsModelDuration createClone() {
        return new SettingsModelDuration(m_configName,m_duration);
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
     * Set the duration based on the ISO-8601 seconds based representation.
     *  Like  "PnDTnHnMn" see {@link Duration}
     * @param duration the duration to be set (in ISO-8601 second based representation)
     */
    public void setDuration(final String duration) {
        boolean changed = (m_duration.toString().equals(duration));
        m_duration = Duration.parse(duration);
        if (changed) {
            notifyChangeListeners();
        }
    }

    /**
     * Returns the total calculated amount of time in milliseconds
     * @return the total amount of time in milliseconds
     */
    public Duration getDuration() {
        return m_duration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_duration = Duration.parse(settings.getString(m_configName, m_duration.toString()));
        durationToUnits();
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
        m_duration = Duration.parse(settings.getString(m_configName));
        durationToUnits();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForModel(final NodeSettingsWO settings) {
        unitsToDuration();
        settings.addString(m_configName, m_duration.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return m_duration.toString();
    }

    /**
     * Set the days value and update the duration representation.
     *
     * @param days the days value to be set
     */
    public void setDaysValue(final String days) {
        m_days = days;
        unitsToDuration();
    }


    /**
     * Set the hours value and update the duration representation.
     *
     * @param hours the hours value to be set
     */
    public void setHoursValue(final String hours) {
        m_hours = hours;
        unitsToDuration();
    }

    /**
     * Set the minutes value and update the duration representation.
     *
     * @param minutes the seconds value to be set
     */
    public void setMinutesValue(final String minutes) {
        m_minutes = minutes;
        unitsToDuration();
    }

    /**
     * Set the seconds value and update the duration representation.
     *
     * @param seconds the seconds value to be set
     */
    public void setSecondsValue(final String seconds) {
        m_seconds = seconds;
        unitsToDuration();
    }

    /**
     * Gets the number of days stored in the model.
     *
     * @return the number of days
     */
    public long getDaysValue() {
        return Long.parseLong(m_days);
    }

    /**
     * Gets the number of hours stored in the model.
     *
     * @return The number of hours
     */
    public long getHoursValue() {
        return Long.parseLong(m_hours);
    }

    /**
     * Gets the number of minutes stored in the model.
     *
     * @return The number of minutes
     */
    public long getMinutesValue() {
        return Long.parseLong(m_minutes);
    }

    /**
     * Gets the number of seconds stored in the model.
     *
     * @return The number of seconds
     */
    public long getSecondsValue() {
        return Long.parseLong(m_seconds);
    }

}
