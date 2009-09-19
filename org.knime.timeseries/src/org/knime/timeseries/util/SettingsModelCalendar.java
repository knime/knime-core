/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   05.06.2009 (Fabian Dill): created
 */
package org.knime.timeseries.util;

import java.util.Calendar;

import org.knime.core.data.date.TimeRenderUtil;
import org.knime.core.data.date.TimestampCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.config.Config;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Settings model that stores a {@link Calendar} in order to store either a 
 * date, a time or both. 
 * 
 * @author Fabian Dill, KNIME.com GmbH
 *
 */
public class SettingsModelCalendar extends SettingsModel {
    
    private static final String KEY_TIMEZONE = "timezone";
    private static final String KEY_TIME = "time";
    private static final String KEY_USE_DATE = "useDate";
    private static final String KEY_USE_TIME = "useTime";
    
    private final String m_key;
    
    private Calendar m_value;
    
    private boolean m_useDate;
    private boolean m_useTime;
    
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
     */
    public SettingsModelCalendar(final String configKey, 
            final Calendar defaultValue, final boolean useDate, 
            final boolean useTime) {
        if (!useDate && !useTime) {
            throw new IllegalArgumentException(
                    "Use at least one of time or date!");
        }        
        if ((configKey == null) || (configKey == "")) {
            throw new IllegalArgumentException("The configName must be a "
                    + "non-empty string");
        }
        m_key = configKey;
        if (defaultValue != null) {
            m_value = defaultValue;
        } else {
            m_value = Calendar.getInstance();
        }
        m_useDate = useDate;
        m_useTime = useTime;
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
     * @return the represented time and/or date
     */
    public Calendar getCalendar() {
        return m_value;
    }
    
    /**
     * 
     * @param calendar the time and/or date
     */
    public void setCalendar(final Calendar calendar) {
        if (useTime() && !useDate()) {
            m_value = TimestampCell.resetDateFields(calendar);
        } else if (!useTime() && useDate()) {
            m_value = TimestampCell.resetTimeFields(calendar);
        } else {
            m_value = calendar;
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
     * @param useDate true if date is of interest
     */
    public void setUseDate(final boolean useDate) {
        m_useDate = useDate;
        if (!useDate) {
            m_value = TimestampCell.resetDateFields(m_value);
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
            m_value = TimestampCell.resetTimeFields(m_value); 
        }
    }
    
    
    

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected SettingsModelCalendar createClone() {
        return new SettingsModelCalendar(m_key, m_value, m_useDate, m_useTime);
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
            m_value = Calendar.getInstance();
        }
    }
    
    

    private void loadFromInternals(final Config internals) {
        Calendar tmpTime = TimestampCell.getUTCCalendar();
        long time = internals.getLong(KEY_TIME, 
                tmpTime.getTimeInMillis());
        tmpTime.setTimeInMillis(time);
        m_value = tmpTime;
        m_useDate = internals.getBoolean(KEY_USE_DATE, true);
        m_useTime = internals.getBoolean(KEY_USE_TIME, false);
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
        // timezone
        internals.addString(KEY_TIMEZONE, m_value.getTimeZone().getID());
        // time (as long)
        internals.addLong(KEY_TIME, m_value.getTimeInMillis());
        // we ignore the locale since I don't know how to access it from 
        // calendar...
        // TODO: do we need the locale???
        internals.addBoolean(KEY_USE_DATE, m_useDate);
        internals.addBoolean(KEY_USE_TIME, m_useTime);
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
                TimeRenderUtil.getStringForDateField(m_value.get(Calendar.YEAR))
                + "-" + TimeRenderUtil.getStringForDateField(
                        m_value.get(Calendar.MONTH)) + "-" 
                + TimeRenderUtil.getStringForDateField(
                        m_value.get(Calendar.DAY_OF_MONTH)) + " ";
        }
        if (m_useTime) {
            if (!m_useDate) {
                result = "Time: ";
            }
           result += TimeRenderUtil.getStringForDateField(
                   m_value.get(Calendar.HOUR_OF_DAY)) + ":" 
               + TimeRenderUtil.getStringForDateField(
                       m_value.get(Calendar.MINUTE)) + ":" 
               + TimeRenderUtil.getStringForDateField(
                       m_value.get(Calendar.SECOND)) + "." 
               + m_value.get(Calendar.MILLISECOND); 
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
        if (internals.getString(KEY_TIMEZONE) == null) {
            throw new InvalidSettingsException("No value for timezone defined");
        }
        // this probably throws an exception if there is no value for the key
        // since every possible long is a valid one (even negatives = in order 
        // to be able to store dates before 1970) 
        internals.getLong(KEY_TIME);
    }

}
