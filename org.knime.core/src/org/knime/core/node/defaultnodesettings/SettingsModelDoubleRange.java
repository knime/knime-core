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
 * ---------------------------------------------------------------------
 * 
 * History
 *   17.01.2007 (mb): created
 */
package org.knime.core.node.defaultnodesettings;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

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
            final PortObjectSpec[] specs) throws NotConfigurableException {
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
