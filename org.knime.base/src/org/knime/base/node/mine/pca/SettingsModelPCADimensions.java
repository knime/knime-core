/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
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
 *   Sep 8, 2009 (uwe): created
 */
package org.knime.base.node.mine.pca;

import java.text.NumberFormat;
import java.util.Arrays;

import javax.swing.AbstractSpinnerModel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Settings model, where either an integer (like number of dimensions) or a
 * double like reproduction percentage can be chosen.
 * 
 * @author uwe, University of Konstanz
 */
public class SettingsModelPCADimensions extends SettingsModel {

    private final String m_configName;

    private boolean m_dimensionsSelected;

    /** @return is reduction determined by number of dimensions */
    public boolean getDimensionsSelected() {
        return m_dimensionsSelected;
    }

    /** @param intChoosen set info by number of dimensions */
    public void setDimensionsSelected(final boolean intChoosen) {
        m_dimensionsSelected = intChoosen;
    }

    /** @return dimensions to reduce to */
    public int getDimensions() {
        return m_dimensions;
    }

    /** @param intValue dimensions to reduce to */
    public void setDimensions(final int intValue) {
        m_dimensions = intValue;
    }

    /**
     * @return minimum information to preserve
     */
    public double getMinQuality() {
        return m_minQuality;
    }

    /**
     * @param doubleValue minimum information to preserve
     */
    public void setMinQuality(final double doubleValue) {
        m_minQuality = doubleValue;
    }

    private int m_dimensions;

    private double m_minQuality;

    /**
     * @param configName key for the config
     * @param intDefault default for integer value
     * @param doubleDefault default for double value
     * @param intChoosen default for "is integer value configured?"
     */
    public SettingsModelPCADimensions(final String configName,
            final int intDefault, final double doubleDefault,
            final boolean intChoosen) {
        m_dimensions = intDefault;
        m_minQuality = doubleDefault;
        m_dimensionsSelected = intChoosen;
        if (configName == null || configName == "") {
            throw new IllegalArgumentException("The configName must be a "
                    + "non-empty string");
        }
        m_configName = configName;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected SettingsModelPCADimensions createClone() {

        return new SettingsModelPCADimensions(m_configName, m_dimensions,
                m_minQuality, m_dimensionsSelected);
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

        return "SettingsModelChoiceIntDouble.modelTypeID";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        try {
            final NodeSettingsRO mySettings =
                    settings.getNodeSettings(m_configName);
            setValues(mySettings.getDouble("doubleVal"), mySettings
                    .getInt("intVal"), mySettings.getBoolean("choice"));
            m_eigenvalues = mySettings.getDoubleArray("eigenvalues", null);

        } catch (final InvalidSettingsException e) {
            setValues(100, 2, false);
        }
    }

    /**
     * set all values of the model.
     * 
     * @param quality min quality
     * @param dimensions dimensions to reduce to
     * @param dimensionsSelected selection by dimension
     */
    void setValues(final double quality, final int dimensions,
            final boolean dimensionsSelected) {
        m_minQuality = quality;
        m_dimensions = dimensions;
        m_dimensionsSelected = dimensionsSelected;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        final NodeSettingsRO mySettings =
                settings.getNodeSettings(m_configName);
        setValues(mySettings.getDouble("doubleVal"), mySettings
                .getInt("intVal"), mySettings.getBoolean("choice"));
        m_eigenvalues = mySettings.getDoubleArray("eigenvalues", null);

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
        final NodeSettingsWO set = settings.addNodeSettings(m_configName);
        set.addBoolean("choice", m_dimensionsSelected);
        set.addDouble("doubleVal", m_minQuality);
        set.addInt("intVal", m_dimensions);
        set.addDoubleArray("eigenvalues", m_eigenvalues);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + " - " + m_configName + ":["
                + (m_dimensionsSelected ? "int" : "double") + ", int:"
                + m_dimensions + "double:" + m_minQuality + "]";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsForModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // nothing
    }

    /**
     * @param eigenvalues eigenvalues of pca matrix
     */
    public void setEigenValues(final double[] eigenvalues) {
        m_eigenvalues = eigenvalues;

    }

    /**
     * get number of dimensions to reduce to based on these settings.
     * @param maxDimensions dimensionality of input data
     * 
     * @return number of output dimensions as configured, or -1 of it cannot be
     *         determined (i.e. <code>evs==null</code> and selection by quality
     *         with quality <100)
     */
    public int getNeededDimensions(final int maxDimensions) {
        if (getDimensionsSelected()) {
            return getDimensions();
        }
        double qual = getMinQuality();
        if (qual == 100) {
            return maxDimensions;
        }
        // no quality selection without eigenvalues
        if (m_eigenvalues == null) {
            return -1;
        }

        final double[] ev = getSortedCopy(m_eigenvalues);
        qual /= 100;
        double sum = 0;
        for (final double e : ev) {
            sum += e;
        }
        int dim = 0;
        double frac = 0;
        for (int i = ev.length - 1; i >= 0; i--) {
            frac += ev[i];
            dim++;
            if (frac / sum >= qual) {
                return dim;
            }
        }
        return ev.length;
    }

    /**
     * get copy of array sorted ascending.
     * 
     * @param evs ev array
     */
    private double[] getSortedCopy(final double[] evs) {
        final double[] ev = Arrays.copyOf(evs, evs.length);
        Arrays.sort(ev);
        return ev;
    }

    private double[] m_eigenvalues;

    /**
     * @return labels for quality slider
     */
    public void configureQualitySlider(final JSpinner spinner) {

        if (m_eigenvalues != null) {
            final double[] ev = getSortedCopy(m_eigenvalues);
            double sum = 0;
            for (final double e : ev) {
                sum += e;
            }
            double frac = 0;
            int[] values = new int[ev.length];
            final NumberFormat nf = NumberFormat.getPercentInstance();
            nf.setMaximumFractionDigits(0);
            int index = 0;
            for (int i = ev.length - 1; i >= 0; i--) {
                frac += ev[i];
                // floor
                final int p = (int)(frac / sum * 100);
                // care for duplicates
                if (index == 0 || index != 0 && values[index - 1] != p) {
                    values[index++] = p;
                }
            }
            // care for duplicates
            if (index != values.length) {
                final int[] v = new int[index];
                for (int i = 0; i < index; i++) {
                    v[i] = values[i];
                }
                values = v;
            }
            final Object currentValue = spinner.getValue();
            spinner.setModel(new ArraySpinnerModel(values));
            spinner.setValue(currentValue);
        } else {
            final int val =
                    Math.max(Math.min((Integer)spinner.getValue(), 100), 25);
            spinner.setModel(new SpinnerNumberModel(val, 25, 100, 1));
        }

    }

    public class ArraySpinnerModel extends AbstractSpinnerModel {
        int[] m_values;

        int index = 0;

        public ArraySpinnerModel(final int[] values) {
            m_values = values;
            index = m_values.length - 1;
        }

        public Object getNextValue() {
            return index == m_values.length - 1 ? m_values[index]
                    : m_values[index + 1];

        }

        public Object getPreviousValue() {
            return index > 0 ? m_values[index - 1] : m_values[index];

        }

        public Object getValue() {
            return m_values[index];
        }

        public void setValue(final Object value) {
            final int oldIndex = index;
            final int v = (Integer)value;
            if (v <= m_values[0]) {
                index = 0;

            } else if (v >= m_values[m_values.length - 1]) {
                index = m_values.length - 1;

            } else {
                int minDist = Integer.MAX_VALUE;
                int minDistIndex = 0;
                for (int i = 0; i < m_values.length; i++) {

                    final int dist = Math.abs(m_values[i] - v);
                    if (dist < minDist) {
                        minDist = dist;
                        minDistIndex = i;
                    }

                }
                index = minDistIndex;
            }
            if (oldIndex != index) {

                fireStateChanged();
            }

        }

    }

}
