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
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;

/**
 * The {@link SettingsModel} for the default date&time dialog component ({@link DialogComponentDateTimeSelection}.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
public final class SettingsModelDateTime extends SettingsModel {
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
     * @param defaultDateTime the initial value. Sets the settings according to the input type, if <code>null</code>
     *            neither date, time nor zone is used. Input can be a {@link LocalDate}, {@link LocalTime},
     *            {@link LocalDateTime} or {@link ZonedDateTime}, otherwise a {@link IllegalArgumentException} will be
     *            thrown.
     *
     */
    public SettingsModelDateTime(final String configName, final Temporal defaultDateTime) {
        if ((configName == null) || configName.isEmpty()) {
            throw new IllegalArgumentException("The configName must be a " + "non-empty string");
        }
        m_configName = configName;
        if (defaultDateTime instanceof LocalDate) {
            m_date = (LocalDate)defaultDateTime;
            m_time = LocalTime.now().withNano(0);
            m_zone = ZoneId.systemDefault();
            m_useDate = true;
            m_useTime = false;
            m_useZone = false;
        } else if (defaultDateTime instanceof LocalTime) {
            m_date = LocalDate.now();
            m_time = (LocalTime)defaultDateTime;
            m_zone = ZoneId.systemDefault();
            m_useDate = false;
            m_useTime = true;
            m_useZone = false;
        } else if (defaultDateTime instanceof LocalDateTime) {
            m_date = ((LocalDateTime)defaultDateTime).toLocalDate();
            m_time = ((LocalDateTime)defaultDateTime).toLocalTime();
            m_zone = ZoneId.systemDefault();
            m_useDate = true;
            m_useTime = true;
            m_useZone = false;
        } else if (defaultDateTime instanceof ZonedDateTime) {
            m_date = ((ZonedDateTime)defaultDateTime).toLocalDate();
            m_time = ((ZonedDateTime)defaultDateTime).toLocalTime();
            m_zone = ((ZonedDateTime)defaultDateTime).getZone();
            m_useDate = true;
            m_useTime = true;
            m_useZone = true;
        } else if (defaultDateTime == null) {
            m_date = LocalDate.now();
            m_time = LocalTime.now().withNano(0);
            m_zone = ZoneId.systemDefault();
            m_useDate = false;
            m_useTime = false;
            m_useZone = false;
        } else {
            throw new IllegalArgumentException("Unsupported type: " + defaultDateTime.getClass());
        }
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
     * Returns the selected local date, local time, local date and time or zoned date and time.
     *
     * @return a date or <code>null</code>, if no date or time is selected
     */
    public Temporal getSelectedDateTime() {
        if (m_useZone) {
            return getZonedDateTime();
        }
        if (m_useDate && m_useTime) {
            return getLocalDateTime();
        }
        if (m_useDate) {
            return getLocalDate();
        }
        if (m_useTime) {
            return getLocalTime();
        }
        return null;
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
            m_useMillis = localTime.getNano() > 0;
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
     * @param temporal sets the settings according to the input type, if <code>null</code> neither date, time nor zone
     *            is used. Input can be a {@link LocalDate}, {@link LocalTime}, {@link LocalDateTime} or
     *            {@link ZonedDateTime}, otherwise a {@link IllegalArgumentException} will be thrown.
     */
    public void setTemporal(final Temporal temporal) {
        if (temporal instanceof LocalDate) {
            setLocalDate((LocalDate)temporal);
            setLocalTime(LocalTime.now().withNano(0));
            setZone(ZoneId.systemDefault());
            setUseDate(true);
            setUseTime(false);
            setUseZone(false);
        } else if (temporal instanceof LocalTime) {
            setLocalDate(LocalDate.now());
            setLocalTime((LocalTime)temporal);
            setZone(ZoneId.systemDefault());
            setUseDate(false);
            setUseTime(true);
            setUseZone(false);
        } else if (temporal instanceof LocalDateTime) {
            setLocalDate(((LocalDateTime)temporal).toLocalDate());
            setLocalTime(((LocalDateTime)temporal).toLocalTime());
            setZone(ZoneId.systemDefault());
            setUseDate(true);
            setUseTime(true);
            setUseZone(false);
        } else if (temporal instanceof ZonedDateTime) {
            setLocalDate(((ZonedDateTime)temporal).toLocalDate());
            setLocalTime(((ZonedDateTime)temporal).toLocalTime());
            setZone(((ZonedDateTime)temporal).getZone());
            setUseDate(true);
            setUseTime(true);
            setUseZone(true);
        } else if (temporal == null) {
            setLocalDate(LocalDate.now());
            setLocalTime(LocalTime.now().withNano(0));
            setZone(ZoneId.systemDefault());
            setUseDate(false);
            setUseTime(false);
            setUseZone(false);
        } else {
            throw new IllegalArgumentException("Unsupported type: " + temporal.getClass());
        }
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
        modelClone.setUseMillis(m_useMillis);
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
        final String string = settings.getString(m_configName);

        final Optional<ZonedDateTime> asZonedDateTime = DateTimeUtils.asZonedDateTime(string);
        final Optional<LocalDateTime> asLocalDateTime = DateTimeUtils.asLocalDateTime(string);
        final Optional<LocalDate> asLocalDate = DateTimeUtils.asLocalDate(string);
        final Optional<LocalTime> asLocalTime = DateTimeUtils.asLocalTime(string);
        final Optional<ZoneId> asTimezone = DateTimeUtils.asTimezone(string);

        if (StringUtils.isEmpty(string) || string.equals("missing")) {
            // table row to variable returns "missing" for flow variables when the node isn't executed yet
            setZonedDateTime(ZonedDateTime.now().withNano(0));
            setUseDate(false);
            setUseTime(false);
            setUseZone(false);
        } else if (asZonedDateTime.isPresent()) {
            setZonedDateTime(asZonedDateTime.get());
            setUseDate(true);
            setUseTime(true);
            setUseZone(true);
        } else if (asLocalDateTime.isPresent()) {
            setZonedDateTime(ZonedDateTime.of(asLocalDateTime.get(), ZoneId.systemDefault()));
            setUseDate(true);
            setUseTime(true);
            setUseZone(false);
        } else if (asLocalDate.isPresent()) {
            setZonedDateTime(ZonedDateTime.of(asLocalDate.get(), LocalTime.now(), ZoneId.systemDefault()));
            setUseDate(true);
            setUseTime(false);
            setUseZone(false);
        } else if (asLocalTime.isPresent()) {
            setZonedDateTime(ZonedDateTime.of(LocalDate.now(), asLocalTime.get(), ZoneId.systemDefault()));
            setUseDate(false);
            setUseTime(true);
            setUseZone(false);
        } else if (asTimezone.isPresent()) {
            setZonedDateTime(ZonedDateTime.of(LocalDate.now(), LocalTime.now(), asTimezone.get()));
            setUseDate(false);
            setUseTime(false);
            setUseZone(true);
        } else {
            throw new InvalidSettingsException("'" + string + "' could not be parsed as a date, time, or time zone.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        final String string = settings.getString(m_configName);
        if (!StringUtils.isEmpty(string) && !string.equals("missing")
            && !DateTimeUtils.asZonedDateTime(string).isPresent() && !DateTimeUtils.asLocalDateTime(string).isPresent()
            && !DateTimeUtils.asLocalDate(string).isPresent() && !DateTimeUtils.asLocalTime(string).isPresent()
            && !DateTimeUtils.asTimezone(string).isPresent()) {
            throw new InvalidSettingsException("'" + string + "' could not be parsed as a date, time, or time zone.");
        }
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
        if (m_useDate && m_useTime && m_useZone) {
            settings.addString(m_configName, getZonedDateTime().format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
        } else if (m_useDate && !m_useTime && !m_useZone) {
            settings.addString(m_configName, getLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        } else if (!m_useDate && m_useTime && !m_useZone) {
            settings.addString(m_configName, getLocalTime().format(DateTimeFormatter.ISO_LOCAL_TIME));
        } else if (!m_useDate && !m_useTime && m_useZone) {
            settings.addString(m_configName, m_zone.toString());
        } else if (m_useDate && m_useTime && !m_useZone) {
            settings.addString(m_configName, getLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        } else {
            settings.addString(m_configName, null);
        }
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

    /**
     * @param useMillis set millis used
     */
    public void setUseMillis(final boolean useMillis) {
        if (useMillis != m_useMillis) {
            m_useMillis = useMillis;
            notifyChangeListeners();
        }
    }
}
