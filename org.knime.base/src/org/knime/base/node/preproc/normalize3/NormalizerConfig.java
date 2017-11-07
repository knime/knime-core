/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 * Created on 20.12.2013 by Marcel Hanser
 */
package org.knime.base.node.preproc.normalize3;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.util.filter.column.DataTypeColumnFilter;

/**
 * Default configuration object for the normalizer node.
 *
 * @author Marcel Hanser
 * @since 3.2
 */
public final class NormalizerConfig {
    /** Key to store the new minimum value (in min/max mode). */
    private static final String NEWMIN_KEY = "new-min";

    /** Key to store the new maximum value (in min/max mode). */
    private static final String NEWMAX_KEY = "new-max";

    /** Key to store the mode. */
    private static final String MODE_KEY = "mode";

    /** Key to store the columns to use. */
    private static final String COLUMNS_KEY = "data-column-filter";

    /** Default minimum zero. */
    private double m_min = 0;

    /** Default maximum one. */
    private double m_max = 1;

    /** Normalizer mode. */
    private NormalizerMode m_mode = NormalizerMode.MINMAX;

    @SuppressWarnings("unchecked")
    private DataColumnSpecFilterConfiguration m_dataColumnFilterConfig = new DataColumnSpecFilterConfiguration(
        COLUMNS_KEY, new DataTypeColumnFilter(DoubleValue.class));

    /**
     * Get the minimum.
     * @return the min
     */
    public double getMin() {
        return m_min;
    }

    /**
     * Set the minimum.
     * 
     * @param min the min to set
     */
    void setMin(final double min) {
        m_min = min;
    }

    /**
     * Get the maximum.
     * 
     * @return the max
     */
    public double getMax() {
        return m_max;
    }

    /**
     * Set the max.
     * 
     * @param max the max to set
     */
    void setMax(final double max) {
        m_max = max;
    }

    /**
     * Get the mode.
     * 
     * @return the mode
     */
    public NormalizerMode getMode() {
        return m_mode;
    }

    /**
     * Set the mode.
     * 
     * @param mode the mode to set
     */
    void setMode(final NormalizerMode mode) {
        m_mode = mode;
    }

    /**
     * Get the {@link DataColumnSpecFilterConfiguration}.
     *
     * @return the dataColumnFilterConfig
     */
    public DataColumnSpecFilterConfiguration getDataColumnFilterConfig() {
        return m_dataColumnFilterConfig;
    }

    /**
     * Loads the configuration for the dialog with corresponding default values.
     *
     * @param settings the settings to load
     * @param spec the data column spec
     */
    void loadConfigurationInDialog(final NodeSettingsRO settings, final DataTableSpec spec) {
        m_mode = NormalizerMode.valueOf(settings.getString(MODE_KEY, NormalizerMode.MINMAX.toString()));
        m_min = settings.getDouble(NEWMIN_KEY, 0.0);
        m_max = settings.getDouble(NEWMAX_KEY, 1.0);
        m_dataColumnFilterConfig.loadConfigurationInDialog(settings, spec);
    }

    /**
     * Loads the configuration for the model.
     *
     * @param settings the settings to load
     * @throws InvalidSettingsException if the settings are invalid
     */
    public void loadConfigurationInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_mode = NormalizerMode.valueOf(settings.getString(MODE_KEY));

        if (NormalizerMode.MINMAX.equals(m_mode)) {
            m_min = settings.getDouble(NEWMIN_KEY);
            m_max = settings.getDouble(NEWMAX_KEY);
        }

        checkSetting(!NormalizerMode.MINMAX.equals(m_mode) || m_min < m_max,
            "Max cannot be smaller than the min value.");
        m_dataColumnFilterConfig.loadConfigurationInModel(settings);
    }

    /**
     * Sets the {@link DataColumnSpecFilterConfiguration} and the normalization mode to MINMAX.
     * 
     * @param spec the table spec
     */
    public void guessDefaults(final DataTableSpec spec) {
        m_dataColumnFilterConfig.loadDefaults(spec, true);
        m_mode = NormalizerMode.MINMAX;
    }

    /**
     * Called from dialog's and model's save method.
     *
     * @param settings Arg settings.
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addString(MODE_KEY, m_mode.toString());
        settings.addDouble(NEWMIN_KEY, m_min);
        settings.addDouble(NEWMAX_KEY, m_max);
        m_dataColumnFilterConfig.saveConfiguration(settings);
    }

    /**
     * Throws an {@link InvalidSettingsException} with the given string template, if the given predicate is
     * <code>false</code>.
     *
     * @param predicate the predicate
     * @param template the template
     * @throws InvalidSettingsException
     */
    private static void checkSetting(final boolean predicate, final String template, final Object... args)
        throws InvalidSettingsException {
        if (!predicate) {
            throw new InvalidSettingsException(String.format(template, args));
        }
    }

    /**
     * Normalization Mode.
     *
     * @author Marcel Hanser
     */
    public enum NormalizerMode {
        /**
         * Z-Score.
         */
        Z_SCORE,
        /**
         * Decimal Scaling.
         */
        DECIMALSCALING,
        /**
         * Min-Max normalization.
         */
        MINMAX;
    }

}
