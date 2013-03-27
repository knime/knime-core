/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *   Sep 8, 2009 (uwe): created
 */
package org.knime.base.node.mine.pca;

import java.text.NumberFormat;
import java.util.Arrays;

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
 * @author Uwe Nagel, University of Konstanz
 */
public class SettingsModelPCADimensions extends SettingsModel {

    /** eigenvalues smaller than <code>epsilon</code> are considered to
     * be zero. */
    private static final double EPSILON = 1e-15;
    private final String m_configName;

    private boolean m_dimensionsSelected;

    /** @return is reduction determined by number of dimensions */
    public boolean getDimensionsSelected() {
        return m_dimensionsSelected;
    }

    /**
     * @param intChoosen
     *            set info by number of dimensions
     */
    public void setDimensionsSelected(final boolean intChoosen) {
        m_dimensionsSelected = intChoosen;
    }

    /** @return dimensions to reduce to */
    public int getDimensions() {
        return m_dimensions;
    }

    /**
     * @param intValue
     *            dimensions to reduce to
     */
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
     * @param doubleValue
     *            minimum information to preserve
     */
    public void setMinQuality(final double doubleValue) {
        m_minQuality = doubleValue;
    }

    /** number of dimensions to reduce to */
    private int m_dimensions;
    /** minimum information fraction to preserve */
    private double m_minQuality;

    /**
     * @param configName
     *            key for the config
     * @param intDefault
     *            default for integer value, dimensions to preserve
     * @param doubleDefault
     *            default for double value, minimal information to preserve
     * @param intChoosen
     *            default for "is integer value configured?"
     */
    public SettingsModelPCADimensions(final String configName,
            final int intDefault, final double doubleDefault,
            final boolean intChoosen) {
        m_dimensions = intDefault;
        m_minQuality = doubleDefault;
        m_dimensionsSelected = intChoosen;
        if (configName == null || "".equals(configName)) {
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
            final NodeSettingsRO mySettings = settings
            .getNodeSettings(m_configName);
            setValues(mySettings.getDouble("doubleVal"),
                    mySettings.getInt("intVal"),
                    mySettings.getBoolean("choice"));
            if (specs.length > 1 && specs[1] instanceof PCAModelPortObjectSpec) {
                m_eigenvalues = ((PCAModelPortObjectSpec) specs[1])
                .getEigenValues();

            } else {
                m_eigenvalues = null;
            }

        } catch (final InvalidSettingsException e) {
            setValues(100, 2, false);
        }

    }

    /**
     * set all values of the model.
     *
     * @param quality
     *            min quality
     * @param dimensions
     *            dimensions to reduce to
     * @param dimensionsSelected
     *            selection by dimension
     */
    void setValues(final double quality, final int dimensions,
            final boolean dimensionsSelected) {
        m_minQuality = quality;
        m_dimensions = dimensions;
        m_dimensionsSelected = dimensionsSelected;
        notifyChangeListeners();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        final NodeSettingsRO mySettings = settings
        .getNodeSettings(m_configName);
        setValues(mySettings.getDouble("doubleVal"),
                mySettings.getInt("intVal"), mySettings.getBoolean("choice"));
        m_eigenvalues = mySettings.getDoubleArray("eigenvalues", null);
        notifyChangeListeners();
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
     * @param eigenvalues
     *            eigenvalues of pca matrix
     */
    public void setEigenValues(final double[] eigenvalues) {
        m_eigenvalues = eigenvalues;

    }

    /**
     * Get the eigenvalues of the input covariance matrix.
     *
     * @return the eigenvalues
     */
    public double[] getEigenvalues() {
        return m_eigenvalues;
    }

    /**
     * get number of dimensions to reduce to based on these settings.
     *
     * @return number of output dimensions as configured, or -1 of it cannot be
     *         determined (i.e. <code>evs==null</code> and selection by quality
     *         with quality <code><100</code>)
     */
    public int getNeededDimensions() {
        if (getDimensionsSelected()) {
            return getDimensions();
        }
        return getNeededDimensionsByQuality();
    }

    /**
     * get number of dimensions to reduce to based on the the minimal quality
     * settings only
     *
     * @return number of output dimensions as configured, or -1 of it cannot be
     *         determined (i.e. <code>evs==null</code> and selection by quality
     *         with quality <code><100</code>)
     */
    private int getNeededDimensionsByQuality() {
        double qual = getMinQuality();

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
            // epsilon prevents "quasi-zero" eigenvalues to be considered for
            // projection
            if (frac / sum >= qual - EPSILON) {
                return dim;
            }
        }
        return ev.length;
    }

    /**
     * get copy of array sorted ascending.
     *
     * @param evs
     *            ev array
     */
    private double[] getSortedCopy(final double[] evs) {
        final double[] ev = Arrays.copyOf(evs, evs.length);
        Arrays.sort(ev);
        return ev;
    }

    /** eigenvalues of the covariance matrix */
    private double[] m_eigenvalues;

    /**
     * @return output description necessary to preserve configured amount of
     *         information as text
     */
    public String getNeededDimensionDescription() {
        final int dim = getNeededDimensionsByQuality();
        if (dim == -1) {
            return "target dimensionality unknown";
        }
        return dim + " dimensions in output";
    }

    /**
     * @param spinner
     *            spinner component to be updated
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
                final int p = (int) (frac / sum * 100);
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
            // last value is always 100% (numerical problem)
            values[values.length - 1] = 100;
            final Object currentValue = spinner.getValue();
            spinner.setModel(new ArraySpinnerModel(values));
            spinner.setValue(currentValue);
            spinner.setEditor(new JSpinner.NumberEditor(spinner, "###"));
        } else {
            final int val = Math.max(
                    Math.min((Integer) spinner.getValue(), 100), 25);
            spinner.setModel(new SpinnerNumberModel(val, 25, 100, 1));
        }

    }

    public class ArraySpinnerModel extends SpinnerNumberModel {
        int[] m_values;

        int index = 0;

        public ArraySpinnerModel(final int[] values) {
            m_values = values;
            index = m_values.length - 1;
        }

        @Override
        public Object getNextValue() {
            return index == m_values.length - 1 ? m_values[index]
                                                           : m_values[index + 1];

        }

        @Override
        public Object getPreviousValue() {
            return index > 0 ? m_values[index - 1] : m_values[index];

        }

        @Override
        public Object getValue() {
            return m_values[index];
        }

        @Override
        public void setValue(final Object value) {
            final int oldIndex = index;
            final int v = (Integer) value;
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

    /**
     * @param dim
     *            dimensions to reduce to
     * @return preserved information int percent, based on eigenvalues
     */
    public String getInformationPreservation(final int dim) {
        if (m_eigenvalues == null) {
            return "unknown information preservation";
        }
        // numerical problems
        if (dim >= m_eigenvalues.length || dim <= 0) {
            return "100% information preservation";
        }
        double sum = 0;
        for (final double t : m_eigenvalues) {
            sum += t;
        }

        double frac = 0;
        for (int i = 0; i < dim; i++) {
            frac += m_eigenvalues[m_eigenvalues.length - 1 - i];
        }
        // floor
        return (int) (100d * frac / sum) + "% information preservation";

    }



}
