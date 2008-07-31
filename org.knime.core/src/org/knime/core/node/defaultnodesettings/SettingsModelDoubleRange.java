/*
 * ------------------------------------------------------------------ *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 * 
 * History
 *   17.01.2007 (mb): created
 */
package org.knime.core.node.defaultnodesettings;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 * Settings model for a double range [min, max]. It stores two floating point
 * numbers. It ensures that the minimum is smaller than the maximum at any time.
 * 
 * @author M. Berthold, University of Konstanz
 */
public class SettingsModelDoubleRange extends SettingsModel {

    /* private members: min and max of range */
    private double m_minRange;

    private double m_maxRange;

    /* ... and identifier */
    private final String m_configName;

    /**
     * Create setting object.
     * 
     * @param configName identifier in the config file.
     * @param minRange minimum default minimum value
     * @param maxRange maximum default maximum value
     * @throws IllegalArgumentException if the specified configName is invalid
     * @throws IllegalArgumentException if the specified range is invalid
     */
    public SettingsModelDoubleRange(final String configName,
            final double minRange, final double maxRange)
            throws IllegalArgumentException {
        if ((configName == null) || (configName == "")) {
            throw new IllegalArgumentException("The configName must be a "
                    + "non-empty string");
        }
        m_configName = configName;

        // ensures min<max
        setRange(minRange, maxRange);

    }

    /**
     * @return the current min value of the range
     */
    public double getMinRange() {
        return m_minRange;
    }

    /**
     * @return the current max value of the range.
     */
    public double getMaxRange() {
        return m_maxRange;
    }

    /**
     * Sets a new min and a new max value.
     * 
     * @param newMin the new min value
     * @param newMax the new max value
     * @throws IllegalArgumentException if the min is larger than the max or
     *             those numbers are not really numbers (NaN).
     */
    void setRange(final double newMin, final double newMax)
            throws IllegalArgumentException {
        if (Double.isNaN(newMax)) {
            throw new IllegalArgumentException(m_configName
                    + ": The specified maximum (" + newMax
                    + ") is not a number (NaN).");
        }
        if (Double.isNaN(newMin)) {
            throw new IllegalArgumentException(m_configName
                    + ": The specified minimum (" + newMin
                    + ") is not a number (NaN).");
        }
        if (newMin > newMax) {
            throw new IllegalArgumentException(m_configName
                    + ": The specified minimum (" + newMin
                    + ") is larger than the specified maximum (" + newMax
                    + ").");
        }
        m_minRange = newMin;
        m_maxRange = newMax;
    }

    /**
     * Sets a new min value of the range.
     * 
     * @param minRange the new min vale of the range
     * @throws IllegalArgumentException if the new min is larger than the
     *             current max
     * @see #setRange(double, double)
     */
    void setMinRange(final double minRange) throws IllegalArgumentException {
        setRange(minRange, m_maxRange);
    }

    /**
     * Sets a new max value of the range.
     * 
     * @param maxRange the new max value of the range
     * @throws IllegalArgumentException if the current min is larger than the
     *             new max
     * @see #setRange(double, double)
     */
    void setMaxRange(final double maxRange) throws IllegalArgumentException {
        setRange(m_minRange, maxRange);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected SettingsModelDoubleRange createClone() {
        try {
            return new SettingsModelDoubleRange(m_configName, m_minRange,
                    m_maxRange);
        } catch (IllegalArgumentException ise) {
            // can't happen.
            return null;
        }
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
    protected String getModelTypeID() {
        return "SMID_doubleRange";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        try {
            NodeSettingsRO mySettings = settings.getNodeSettings(m_configName);
            double min = mySettings.getDouble("MIN");
            double max = mySettings.getDouble("MAX");
            setRange(min, max);
        } catch (IllegalArgumentException iae) {
            // ignore, keep the old values
        } catch (InvalidSettingsException ise) {
            // ignore, keep the old values            
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        try {
            NodeSettingsRO mySettings = settings.getNodeSettings(m_configName);
            setRange(mySettings.getDouble("MIN"), mySettings.getDouble("MAX"));
        } catch (InvalidSettingsException ise) {
            throw new InvalidSettingsException(getClass().getSimpleName()
                    + " - " + m_configName + ": " + ise.getMessage());
        } catch (IllegalArgumentException iae) {
            throw new InvalidSettingsException(getClass().getSimpleName()
                    + " - " + m_configName + ": " + iae.getMessage());
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
        NodeSettingsWO mySettings = settings.addNodeSettings(m_configName);
        mySettings.addDouble("MIN", m_minRange);
        mySettings.addDouble("MAX", m_maxRange);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + " - " + m_configName + ":[" 
        + m_minRange + "," + m_maxRange + "]";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsForModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        double min;
        double max;
        try {
            NodeSettingsRO mySettings = settings.getNodeSettings(m_configName);
            min = mySettings.getDouble("MIN");
            max = mySettings.getDouble("MAX");
        } catch (InvalidSettingsException ise) {
            throw new InvalidSettingsException(getClass().getSimpleName()
                    + " - " + m_configName + ": " +  ise.getMessage());
        }
        if (min > max) {
            throw new InvalidSettingsException("min>max in "
                    + getClass().getSimpleName() + " - " + m_configName);
        }
    }

}
