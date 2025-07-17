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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   09.07.2010 (hofer): created
 */
package org.knime.core.util.binning.auto;

import java.math.RoundingMode;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.filter.InputFilter;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;

/**
 * This class hold the settings for the DB Auto Binner node.
 *
 * @author Mor Kalla
 * @since 3.6
 *
 * @deprecated Outdated, not used by modern auto binner nodes.
 */
@Deprecated
public final class AutoBinnerLearnSettings {

    private static final String CFG_METHOD = "method";

    private static final String CFG_BIN_COUNT = "binCount";

    private static final String CFG_EQUALITY_METHOD = "equalityMethod";

    private static final String CFG_INTEGER_BOUNDS = "integerBounds";

    private static final String CFG_SAMPLE_QUANTILES = "SAMPLE_QUANTILES";

    private static final String CFG_BIN_NAMING = "binNaming";

    private static final String CFG_REPLACE_COLUMN = "replaceColumn";

    private static final String CFG_ADVANCED_FORMATTING = "advancedFormatting";

    private static final String CFG_OUTPUT_FORMAT = "outputFormat";

    private static final String CFG_PRECISION = "precision";

    private static final String CFG_PRECISION_MODE = "precisionMode";

    private static final String CFG_ROUNDING_MODE = "roundingMode";

    private static final int DEFAULT_VALUE_PRECISION = 3;

    private static final int DEFAULT_VALUE_BIN_COUNT = 5;

    private static final boolean DEFAULT_VALUE_INTEGER_BOUNDS = false;

    private static final boolean DEFAULT_VALUE_REPLACE_COLUMN = false;

    private static final boolean DEFAULT_VALUE_ADVENCED_FORMATTING = false;

    private static final double VALUE_QUARTER = 0.25;

    private static final double VALUE_HALF = 0.5;

    private static final double VALUE_THREE_QUARTER = 0.75;

    private static final double VALUE_FULL = 1;

    private BinningMethod m_method = BinningMethod.FIXED_NUMBER;

    private int m_binCount = DEFAULT_VALUE_BIN_COUNT;

    private EqualityMethod m_equalityMethod = EqualityMethod.WIDTH;

    private boolean m_integerBounds = DEFAULT_VALUE_INTEGER_BOUNDS;

    private double[] m_sampleQuantiles = new double[]{0, VALUE_QUARTER, VALUE_HALF, VALUE_THREE_QUARTER, VALUE_FULL};

    private BinNaming m_binNaming = BinNaming.NUMBERED;

    private boolean m_replaceColumn = DEFAULT_VALUE_REPLACE_COLUMN;

    private boolean m_advancedFormatting = DEFAULT_VALUE_ADVENCED_FORMATTING;

    private OutputFormat m_outputFormat = OutputFormat.STANDARD;

    private int m_precision = DEFAULT_VALUE_PRECISION;

    private PrecisionMode m_precisionMode = PrecisionMode.DECIMAL;

    private RoundingMode m_roundingMode = RoundingMode.HALF_UP;

    private DataColumnSpecFilterConfiguration m_filterConfiguration = createDCSFilterConfiguration();

    /**
     * A new configuration to store the settings. Only Columns of Type String are available.
     *
     * @return filter configuration
     */
    static DataColumnSpecFilterConfiguration createDCSFilterConfiguration() {
        return new DataColumnSpecFilterConfiguration("column-filter", new InputFilter<DataColumnSpec>() {
            @Override
            public boolean include(final DataColumnSpec name) {
                return name.getType().isCompatible(DoubleValue.class);
            }
        });
    }

    /**
     * Gets the binning method.
     *
     * @return the {@link BinningMethod} object
     */
    public BinningMethod getMethod() {
        return m_method;
    }

    /**
     * Sets the binning method.
     *
     * @param method the {@link BinningMethod} to set
     */
    public void setMethod(final BinningMethod method) {
        m_method = method;
    }

    /**
     * Gets the bin count.
     *
     * @return the bin count
     */
    public int getBinCount() {
        return m_binCount;
    }

    /**
     * Sets the bin count.
     *
     * @param binCount the bin count to set
     */
    public void setBinCount(final int binCount) {
        m_binCount = binCount;
    }

    /**
     * Gets the equality method.
     *
     * @return the {@link EqualityMethod} object
     */
    public EqualityMethod getEqualityMethod() {
        return m_equalityMethod;
    }

    /**
     * Sets the equality method.
     *
     * @param equalityMethod the {@link EqualityMethod} to set
     */
    public void setEqualityMethod(final EqualityMethod equalityMethod) {
        m_equalityMethod = equalityMethod;
    }

    /**
     * Gets the integer bounds.
     *
     * @return the integer bounds
     */
    public boolean getIntegerBounds() {
        return m_integerBounds;
    }

    /**
     * Sets the integer bounds.
     *
     * @param integerBounds the integer bounds to set
     */
    public void setIntegerBounds(final boolean integerBounds) {
        m_integerBounds = integerBounds;
    }

    /**
     * Gets the sample quantiles.
     *
     * @return the sample quantiles
     */
    public double[] getSampleQuantiles() {
        return m_sampleQuantiles;
    }

    /**
     * Sets the sample quantiles.
     *
     * @param sampleQuantiles the sample quantiles to set
     */
    public void setSampleQuantiles(final double[] sampleQuantiles) {
        m_sampleQuantiles = sampleQuantiles;
    }

    /**
     * Gets the bin naming.
     *
     * @return the {@link BinNaming} object
     */
    public BinNaming getBinNaming() {
        return m_binNaming;
    }

    /**
     * @param binNaming the binNaming to set
     */
    public void setBinNaming(final BinNaming binNaming) {
        m_binNaming = binNaming;
    }

    /**
     * Gets the replace column.
     *
     * @return the replace column
     */
    public boolean getReplaceColumn() {
        return m_replaceColumn;
    }

    /**
     * Sets the replace column.
     *
     * @param replaceColumn the replace column to set
     */
    public void setReplaceColumn(final boolean replaceColumn) {
        m_replaceColumn = replaceColumn;
    }

    /**
     * Gets the advanced formatting.
     *
     * @return the advanced formatting
     */
    public boolean getAdvancedFormatting() {
        return m_advancedFormatting;
    }

    /**
     * Sets the advanced formatting.
     *
     * @param advancedFormatting the advanced formatting to set
     */
    public void setAdvancedFormatting(final boolean advancedFormatting) {
        m_advancedFormatting = advancedFormatting;
    }

    /**
     * Gets the output format.
     *
     * @return the {@link OutputFormat} object
     */
    public OutputFormat getOutputFormat() {
        return m_outputFormat;
    }

    /**
     * Sets the output format.
     *
     * @param outputFormat the {@link OutputFormat} to set
     */
    public void setOutputFormat(final OutputFormat outputFormat) {
        m_outputFormat = outputFormat;
    }

    /**
     * Gets the precision.
     *
     * @return the precision
     */
    public int getPrecision() {
        return m_precision;
    }

    /**
     * Sets the precision.
     *
     * @param precision the precision to set
     */
    public void setPrecision(final int precision) {
        m_precision = precision;
    }

    /**
     * Gets the precision mode.
     *
     * @return the {@link PrecisionMode} object
     */
    public PrecisionMode getPrecisionMode() {
        return m_precisionMode;
    }

    /**
     * Sets the precision mode.
     *
     * @param precisionMode the {@link PrecisionMode} to set
     */
    public void setPrecisionMode(final PrecisionMode precisionMode) {
        m_precisionMode = precisionMode;
    }

    /**
     * Gets the rounding mode.
     *
     * @return the {@link RoundingMode} object
     */
    public RoundingMode getRoundingMode() {
        return m_roundingMode;
    }

    /**
     * Sets the rounding mode.
     *
     * @param roundingMode the {@link RoundingMode} to set
     */
    public void setRoundingMode(final RoundingMode roundingMode) {
        m_roundingMode = roundingMode;
    }

    /**
     * Gets the filter configuration.
     *
     * @return the {@link DataColumnSpecFilterConfiguration} object
     */
    public DataColumnSpecFilterConfiguration getFilterConfiguration() {
        return m_filterConfiguration;
    }

    /**
     * Sets the filter configuration.
     *
     * @param config the {@link DataColumnSpecFilterConfiguration} object
     */
    public void setFilterConfiguration(final DataColumnSpecFilterConfiguration config) {
        m_filterConfiguration = config;

    }

    /**
     * Loads the settings from the node settings object.
     *
     * @param settings a node settings object
     * @throws InvalidSettingsException if some settings are missing
     */
    public void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        final DataColumnSpecFilterConfiguration config = createDCSFilterConfiguration();
        config.loadConfigurationInModel(settings);
        m_filterConfiguration = config;
        m_method = BinningMethod.valueOf(settings.getString(CFG_METHOD));
        m_binCount = settings.getInt(CFG_BIN_COUNT);
        m_equalityMethod = EqualityMethod.valueOf(settings.getString(CFG_EQUALITY_METHOD));
        m_integerBounds = settings.getBoolean(CFG_INTEGER_BOUNDS);
        m_sampleQuantiles = settings.getDoubleArray(CFG_SAMPLE_QUANTILES);
        m_binNaming = BinNaming.valueOf(settings.getString(CFG_BIN_NAMING));
        m_replaceColumn = settings.getBoolean(CFG_REPLACE_COLUMN);
        m_advancedFormatting = settings.getBoolean(CFG_ADVANCED_FORMATTING);
        m_outputFormat = OutputFormat.valueOf(settings.getString(CFG_OUTPUT_FORMAT));
        m_precision = settings.getInt(CFG_PRECISION);
        m_precisionMode = PrecisionMode.valueOf(settings.getString(CFG_PRECISION_MODE));
        m_roundingMode = RoundingMode.valueOf(settings.getString(CFG_ROUNDING_MODE));
    }

    /**
     * Loads the settings from the node settings object using default values if some settings are missing.
     *
     * @param settings a node settings object
     * @param spec the {@link DataTableSpec} object
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings, final DataTableSpec spec) {
        final DataColumnSpecFilterConfiguration config = createDCSFilterConfiguration();
        config.loadConfigurationInDialog(settings, spec);
        m_filterConfiguration = config;
        m_method = BinningMethod.valueOf(settings.getString(CFG_METHOD, BinningMethod.FIXED_NUMBER.toString()));
        m_binCount = settings.getInt(CFG_BIN_COUNT, DEFAULT_VALUE_BIN_COUNT);
        m_equalityMethod = EqualityMethod.valueOf(settings.getString(CFG_EQUALITY_METHOD, EqualityMethod.WIDTH.name()));
        m_integerBounds = settings.getBoolean(CFG_INTEGER_BOUNDS, DEFAULT_VALUE_INTEGER_BOUNDS);
        m_sampleQuantiles = settings.getDoubleArray(CFG_SAMPLE_QUANTILES,
            new double[]{0, VALUE_QUARTER, VALUE_HALF, VALUE_THREE_QUARTER, VALUE_FULL});
        m_binNaming = BinNaming.valueOf(settings.getString(CFG_BIN_NAMING, BinNaming.NUMBERED.toString()));
        m_replaceColumn = settings.getBoolean(CFG_REPLACE_COLUMN, DEFAULT_VALUE_REPLACE_COLUMN);
        m_advancedFormatting = settings.getBoolean(CFG_ADVANCED_FORMATTING, DEFAULT_VALUE_ADVENCED_FORMATTING);
        m_outputFormat = OutputFormat.valueOf(settings.getString(CFG_OUTPUT_FORMAT, OutputFormat.STANDARD.name()));
        m_precision = settings.getInt(CFG_PRECISION, DEFAULT_VALUE_PRECISION);
        m_precisionMode = PrecisionMode.valueOf(settings.getString(CFG_PRECISION_MODE, PrecisionMode.DECIMAL.name()));
        m_roundingMode = RoundingMode.valueOf(settings.getString(CFG_ROUNDING_MODE, RoundingMode.HALF_UP.name()));
    }

    /**
     * Saves the settings into the node settings object.
     *
     * @param settings a node settings object
     */
    public void saveSettings(final NodeSettingsWO settings) {
        m_filterConfiguration.saveConfiguration(settings);
        settings.addString(CFG_METHOD, m_method.name());
        settings.addInt(CFG_BIN_COUNT, m_binCount);
        settings.addString(CFG_EQUALITY_METHOD, m_equalityMethod.name());
        settings.addBoolean(CFG_INTEGER_BOUNDS, m_integerBounds);
        settings.addDoubleArray(CFG_SAMPLE_QUANTILES, m_sampleQuantiles);
        settings.addString(CFG_BIN_NAMING, m_binNaming.name());
        settings.addBoolean(CFG_REPLACE_COLUMN, m_replaceColumn);
        settings.addBoolean(CFG_ADVANCED_FORMATTING, m_advancedFormatting);
        settings.addString(CFG_OUTPUT_FORMAT, m_outputFormat.name());
        settings.addInt(CFG_PRECISION, m_precision);
        settings.addString(CFG_PRECISION_MODE, m_precisionMode.name());
        settings.addString(CFG_ROUNDING_MODE, m_roundingMode.name());
    }

}
