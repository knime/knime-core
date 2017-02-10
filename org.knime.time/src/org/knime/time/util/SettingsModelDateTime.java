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
 *   Nov 8, 2016 (simon): created
 */
package org.knime.time.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;

/**
 *  The {@link SettingsModel} for the default date&time dialog component ({@link DialogComponentDateTimeSelection}.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
public class SettingsModelDateTime extends SettingsModel {

    private static final String KEY_DATE = "date";

    private static final String KEY_TIME = "time";

    private static final String KEY_ZONE = "zone";

    private static final String KEY_USE_DATE = "useDate";

    private static final String KEY_USE_TIME = "useTime";

    private static final String KEY_USE_ZONE = "useZone";

    private final String m_configName;

    private LocalDate m_date;

    private LocalTime m_time;

    private ZoneId m_zone;

    private boolean m_useDate;

    private boolean m_useTime;

    private boolean m_useZone;

    private boolean m_useMillis;

    /**
     * @param configName the identifier the value is stored with in the {@link org.knime.core.node.NodeSettings} object
     * @param defaultDate the initial value, if <code>null</code> the current date is used
     * @param defaultTime the initial value, if <code>null</code> the current time is used
     * @param defaultZoneId the initial value, if <code>null</code> the system default is used
     *
     */
    public SettingsModelDateTime(final String configName, final LocalDate defaultDate, final LocalTime defaultTime,
        final ZoneId defaultZoneId) {
        if ((configName == null) || configName.isEmpty()) {
            throw new IllegalArgumentException("The configName must be a non-empty string");
        }

        m_configName = configName;
        if (defaultDate != null) {
            m_date = defaultDate;
        } else {
            m_date = LocalDate.now();
        }
        if (defaultTime != null) {
            m_time = defaultTime;
        } else {
            m_time = LocalTime.now().withNano(0);
        }
        if (defaultZoneId != null) {
            m_zone = defaultZoneId;
        } else {
            m_zone = ZoneId.systemDefault();
        }

        m_useDate = true;
        m_useTime = true;
        m_useZone = true;
    }

    /**
     * @param configName the identifier the value is stored with in the {@link org.knime.core.node.NodeSettings} object
     * @param defaultDateTime the initial value, if <code>null</code> the current date and time is used
     *
     */
    public SettingsModelDateTime(final String configName, final ZonedDateTime defaultDateTime) {
        if ((configName == null) || configName.isEmpty()) {
            throw new IllegalArgumentException("The configName must be a " + "non-empty string");
        }
        m_configName = configName;
        if (defaultDateTime != null) {
            m_date = defaultDateTime.toLocalDate();
            m_time = defaultDateTime.toLocalTime();
            m_zone = defaultDateTime.getZone();
        } else {
            m_date = LocalDate.now();
            m_time = LocalTime.now().withNano(0);
            m_zone = ZoneId.systemDefault();
        }

        m_useDate = true;
        m_useTime = true;
        m_useZone = true;
    }

    /**
     * @return the represented date
     */
    public LocalDate getLocalDate() {
        return m_date;
    }

    /**
     * @return the represented time
     */
    public LocalTime getLocalTime() {
        return m_time;
    }

    /**
     * @return the represented time zone
     */
    public ZoneId getZone() {
        return m_zone;
    }

    /**
     * @return the represented date and time
     */
    public LocalDateTime getLocalDateTime() {
        return LocalDateTime.of(m_date, m_time);
    }

    /**
     * @return the represented zoned date and time
     */
    public ZonedDateTime getZonedDateTime() {
        return ZonedDateTime.of(m_date, m_time, m_zone);
    }

    /**
     * @param localDate {@link LocalDate}
     */
    public void setLocalDate(final LocalDate localDate) {
        boolean sameValue;

        if (localDate == null) {
            sameValue = (m_date == null);
        } else {
            sameValue = localDate.equals(m_date);
        }
        m_date = localDate;

        if (!sameValue) {
            notifyChangeListeners();
        }
    }

    /**
     * @param zone {@link ZoneId}
     */
    public void setZone(final ZoneId zone) {
        boolean sameValue;
        if (zone == null) {
            sameValue = (m_zone == null);
        } else {
            sameValue = zone.equals(m_zone);
        }
        m_zone = zone;

        if (!sameValue) {
            notifyChangeListeners();
        }
    }

    /**
     * @param localTime {@link LocalTime}
     */
    public void setLocalTime(final LocalTime localTime) {
        boolean sameValue;

        if (localTime == null) {
            sameValue = (m_time == null);
        } else {
            sameValue = localTime.equals(m_time);
        }
        m_time = localTime;

        if (!sameValue) {
            notifyChangeListeners();
        }
    }

    /**
     * @param localDateTime {@link LocalDateTime}
     */
    public void setLocalDateTime(final LocalDateTime localDateTime) {
        setLocalDate(localDateTime.toLocalDate());
        setLocalTime(localDateTime.toLocalTime());
    }

    /**
     * @param zonedDateTime {@link ZonedDateTime}
     */
    public void setZonedDateTime(final ZonedDateTime zonedDateTime) {
        setLocalDate(zonedDateTime.toLocalDate());
        setLocalTime(zonedDateTime.toLocalTime());
        setZone(zonedDateTime.getZone());
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected SettingsModelDateTime createClone() {
        final SettingsModelDateTime modelClone = new SettingsModelDateTime(m_configName, m_date, m_time, m_zone);
        modelClone.setUseDate(m_useDate);
        modelClone.setUseTime(m_useTime);
        modelClone.setUseZone(m_useZone);
        return modelClone;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getModelTypeID() {
        return "settingsmodel.date&time";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getConfigName() {
        return m_configName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        try {
            loadSettingsForModel(settings);
        } catch (InvalidSettingsException ise) {
            // load current date and time
            setLocalDate(LocalDate.now());
            setLocalTime(LocalTime.now().withNano(0));
            setZone(ZoneId.systemDefault());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        final NodeSettingsRO internals = settings.getNodeSettings(m_configName);
        final long date = internals.getLong(KEY_DATE, LocalDate.now().getLong(ChronoField.EPOCH_DAY));
        final long time = internals.getLong(KEY_TIME, LocalTime.now().getLong(ChronoField.NANO_OF_DAY));
        final String zone = internals.getString(KEY_ZONE, ZoneId.systemDefault().getId());
        m_useMillis = LocalTime.ofNanoOfDay(time).getNano() > 0;
        setLocalDate(LocalDate.ofEpochDay(date));
        setLocalTime(LocalTime.ofNanoOfDay(time));
        setZone(ZoneId.of(zone));
        setUseDate(internals.getBoolean(KEY_USE_DATE, true));
        setUseTime(internals.getBoolean(KEY_USE_TIME, true));
        setUseZone(internals.getBoolean(KEY_USE_ZONE, true));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        final NodeSettingsRO internals = settings.getNodeSettings(m_configName);
        internals.getLong(KEY_DATE);
        internals.getLong(KEY_TIME);
        internals.getString(KEY_ZONE);
        internals.getBoolean(KEY_USE_DATE);
        internals.getBoolean(KEY_USE_TIME);
        internals.getBoolean(KEY_USE_ZONE);
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
    protected void saveSettingsForModel(final NodeSettingsWO settings) {
        final NodeSettingsWO internals = settings.addNodeSettings(m_configName);
        internals.addLong(KEY_DATE, m_date.getLong(ChronoField.EPOCH_DAY));
        internals.addLong(KEY_TIME, m_time.getLong(ChronoField.NANO_OF_DAY));
        internals.addString(KEY_ZONE, m_zone.getId());
        internals.addBoolean(KEY_USE_DATE, m_useDate);
        internals.addBoolean(KEY_USE_TIME, m_useTime);
        internals.addBoolean(KEY_USE_ZONE, m_useZone);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        if (!m_useDate && !m_useTime && !m_useZone) {
            return "";
        }
        if (!m_useDate && !m_useZone) {
            return m_time.toString();
        }
        if (!m_useTime && !m_useZone) {
            return m_date.toString();
        }
        if (!m_useTime && !m_useDate) {
            return m_zone.toString();
        }
        if (!m_useZone) {
            return LocalDateTime.of(m_date, m_time).toString();
        }
        return ZonedDateTime.of(m_date, m_time, m_zone).toString();
    }

    /**
     * @return the date used
     */
    public boolean useDate() {
        return m_useDate;
    }

    /**
     * @param useDate set date used
     */
    public void setUseDate(final boolean useDate) {
        if (useDate != m_useDate) {
            m_useDate = useDate;
            notifyChangeListeners();
        }
    }

    /**
     * @return is time used
     */
    public boolean useTime() {
        return m_useTime;
    }

    /**
     * @param useTime set time used
     */
    public void setUseTime(final boolean useTime) {
        if (useTime != m_useTime) {
            m_useTime = useTime;
            notifyChangeListeners();
        }
    }

    /**
     * @return is time zone used
     */
    public boolean useZone() {
        return m_useZone;
    }

    /**
     * @param useZone set time zone used
     */
    public void setUseZone(final boolean useZone) {
        if (useZone != m_useZone) {
            m_useZone = useZone;
            notifyChangeListeners();
        }
    }

    /**
     * @return the useMillis
     */
    public boolean useMillis() {
        return m_useMillis;
    }
}
