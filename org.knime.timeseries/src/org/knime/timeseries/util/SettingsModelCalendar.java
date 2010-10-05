/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 * ------------------------------------------------------------------------
 * 
 * History
 *   05.06.2009 (Fabian Dill): created
 */
package org.knime.timeseries.util;

import java.util.Calendar;

import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.date.DateAndTimeRenderUtil;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.config.Config;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Settings model that stores a {@link Calendar} in order to store either a 
 * date or a time with and without milliseconds. 
 * 
 * @author Fabian Dill, KNIME.com GmbH
 *
 */
public class SettingsModelCalendar extends SettingsModel {
    
    private static final String KEY_TIME = "time";
    private static final String KEY_USE_DATE = "useDate";
    private static final String KEY_USE_TIME = "useTime";
    private static final String KEY_USE_MILLIS = "useMillis";
    
    private final String m_key;
    
    private Calendar m_value;
    
    private boolean m_useDate;
    private boolean m_useTime;
    private boolean m_useMillis;
    
    /**
     * Creates a new object holding a calendar value.
     *
     * @param configKey the identifier the value is stored with in the
     *            {@link org.knime.core.node.NodeSettings} object
     * @param defaultValue the initial value, if <code>null</code> the current 
     *  time is used 
     *  @param useDate true if year, month, day is relevant, false if only the 
     *      time is of interest (Note: use either time, or date or both)
     *  @param useTime true if only the time (hour, minutes, etc.) is relevant, 
     *      false if only the date is of interest (Note: use either time, or 
     *      date or both)
     *  @param useMillis <code>true</code> if milliseconds are relevant, false 
     *      if milliseconds are not of interest
     *      @throws IllegalArgumentException if useDate and useTime are false!
     */
    public SettingsModelCalendar(final String configKey, 
            final Calendar defaultValue, final boolean useDate, 
            final boolean useTime, final boolean useMillis) {
        if (!useDate && !useTime) {
            throw new IllegalArgumentException(
                    "Use at least one of time or date!");
        }
        if (!useTime && useMillis) {
            throw new IllegalArgumentException(
                    "Milliseconds can only be used if time is used as well!"); 
        }
        if ((configKey == null) || configKey.isEmpty()) {
            throw new IllegalArgumentException("The configName must be a "
                    + "non-empty string");
        }
        m_key = configKey;
        if (defaultValue != null) {
            m_value = defaultValue;
        } else {
            m_value = Calendar.getInstance(DateAndTimeCell.UTC_TIMEZONE);
        }
        m_useDate = useDate;
        m_useTime = useTime;
        m_useMillis = useMillis;
        // clear calendar fields appropriately
        if (!m_useDate) {
            DateAndTimeCell.resetDateFields(m_value);
        }
        if (!m_useTime) {
            DateAndTimeCell.resetTimeFields(m_value);
        }
        if (!m_useMillis) {
            m_value.clear(Calendar.MILLISECOND);
        }
    }
    
    /**
     * Creates a new object holding a calendar value.
     *
     * @param configKey the identifier the value is stored with in the
     *            {@link org.knime.core.node.NodeSettings} object
     * @param defaultValue the initial value, if <code>null</code> the current 
     *  time is used 
     *  @param useDate true if year, month, day is relevant, false if only the 
     *      time is of interest (Note: use either time, or date or both)
     *  @param useTime true if only the time (hour, minutes, etc.) is relevant, 
     *      false if only the date is of interest (Note: use either time, or 
     *      date or both)
     *      @throws IllegalArgumentException if useDate and useTime are false!
     */
    public SettingsModelCalendar(final String configKey, 
            final Calendar defaultValue, final boolean useDate, 
            final boolean useTime) {
        this(configKey, defaultValue, useDate, useTime, false);
    }

    /**
     * Creates a new object holding a calendar value representing date but 
     * not time.
     *
     * @param configKey the identifier the value is stored with in the
     *            {@link org.knime.core.node.NodeSettings} object
     * @param defaultValue the initial value, if <code>null</code> the current 
     *  time is used 
     */
    public SettingsModelCalendar(final String configKey, 
            final Calendar defaultValue) {
        this(configKey, defaultValue, true, false);
    }
    
    
    
    /**
     * 
     * @return a clone(!) of the represented time and/or date
     */
    public Calendar getCalendar() {
        return (Calendar)m_value.clone();
    }
    
    /**
     * 
     * @param calendar the time and/or date
     */
    public void setCalendar(final Calendar calendar) {
        m_value = (Calendar)(calendar.clone());
        if (!useDate()) {
            DateAndTimeCell.resetDateFields(m_value);
        }
        if (!useTime()) {
            DateAndTimeCell.resetTimeFields(m_value);
        }
        if (!useMilliseconds()) {
            m_value.clear(Calendar.MILLISECOND);
        }
    }
    
    /**
     * 
     * @return true whether date is of interest
     */
    public boolean useDate() {
        return m_useDate;
    }
    
    /**
     * 
     * @return <code>true</code> if milliseconds are of interest
     */
    public boolean useMilliseconds() {
        return m_useMillis;
    }
    
    /**
     * 
     * @param useDate true if date is of interest
     */
    public void setUseDate(final boolean useDate) {
        m_useDate = useDate;
        if (!useDate) {
            DateAndTimeCell.resetDateFields(m_value);
        }
    }
    
    /**
     * 
     * @return true if time is of interest
     */
    public boolean useTime() {
        return m_useTime;
    }
    
    /**
     * 
     * @param useTime true if time is of interest
     */
    public void setUseTime(final boolean useTime) {
        m_useTime = useTime;
        if (!useTime) {
            DateAndTimeCell.resetTimeFields(m_value);
            // if we do not use the time we also do not use the milliseconds
            setUseMilliseconds(false);
        }
    }
    
    /**
     * 
     * @param useMilliseconds <code>true</code> if milliseconds are of interest,
     * <code>false</code> otherwise
     */
    public void setUseMilliseconds(final boolean useMilliseconds) {
        m_useMillis = useMilliseconds;
        if (!m_useMillis) {
            m_value.clear(Calendar.MILLISECOND);
        }
    }
    

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected SettingsModelCalendar createClone() {
        return new SettingsModelCalendar(m_key, m_value, m_useDate, m_useTime, 
                m_useMillis);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getConfigName() {
        return m_key;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getModelTypeID() {
        return "settingsmodel.datetime";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        try {            
            Config internals = settings.getConfig(m_key);
            loadFromInternals(internals);
        } catch (InvalidSettingsException ise) {
            // load current time
            m_value = Calendar.getInstance(DateAndTimeCell.UTC_TIMEZONE);
        }
    }
    
    

    private void loadFromInternals(final Config internals) {
        Calendar tmpTime = DateAndTimeCell.getUTCCalendar();
        long time = internals.getLong(KEY_TIME, 
                tmpTime.getTimeInMillis());
        tmpTime.setTimeInMillis(time);
        m_value = tmpTime;
        m_useDate = internals.getBoolean(KEY_USE_DATE, true);
        m_useTime = internals.getBoolean(KEY_USE_TIME, false);
        m_useMillis = internals.getBoolean(KEY_USE_MILLIS, false);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        Config internals = settings.getConfig(m_key);
        loadFromInternals(internals);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForDialog(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        saveSettings(settings);
    }
    
    private void saveSettings(final NodeSettingsWO settings) {
        Config internals = settings.addConfig(m_key);
        // time (as long)
        internals.addLong(KEY_TIME, m_value.getTimeInMillis());
        internals.addBoolean(KEY_USE_DATE, m_useDate);
        internals.addBoolean(KEY_USE_TIME, m_useTime);
        internals.addBoolean(KEY_USE_MILLIS, m_useMillis);
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForModel(final NodeSettingsWO settings) {
        saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        String result = "";
        if (m_useDate) {
            result = "Date: ";
            result += 
                DateAndTimeRenderUtil.getStringForDateField(
                        m_value.get(Calendar.YEAR))
                + "-" + DateAndTimeRenderUtil.getStringForDateField(
                        m_value.get(Calendar.MONTH)) + "-" 
                + DateAndTimeRenderUtil.getStringForDateField(
                        m_value.get(Calendar.DAY_OF_MONTH)) + " ";
        }
        if (m_useTime) {
            if (!m_useDate) {
                result = "Time: ";
            }
           result += DateAndTimeRenderUtil.getStringForDateField(
                   m_value.get(Calendar.HOUR_OF_DAY)) + ":" 
               + DateAndTimeRenderUtil.getStringForDateField(
                       m_value.get(Calendar.MINUTE)) + ":" 
               + DateAndTimeRenderUtil.getStringForDateField(
                       m_value.get(Calendar.SECOND)); 
           if (m_useMillis) {
               result += "." + m_value.get(Calendar.MILLISECOND);
           }
                
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsForModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // guess nothing else to do here than to check for existence of 
        // the fields
        Config internals = settings.getConfig(m_key);
        boolean useDate = internals.getBoolean(KEY_USE_DATE);
        boolean useTime = internals.getBoolean(KEY_USE_TIME);
        if (!useDate && !useTime) {
            throw new InvalidSettingsException("Must use date, time or both!");
        }
        internals.getBoolean(KEY_USE_MILLIS);
        // this probably throws an exception if there is no value for the key
        // since every possible long is a valid one (even negatives = in order 
        // to be able to store dates before 1970) 
        internals.getLong(KEY_TIME);
    }

}
