/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ------------------------------------------------------------------------
 *
 * History
 *   09.07.2010 (hofer): created
 */
package org.knime.base.node.preproc.autobinner3;

import java.math.RoundingMode;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.ButtonGroupEnumInterface;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;

/**
 * This class hold the settings for the Logistic Learner Node.
 *
 * @author Heiko Hofer
 */
public final class AutoBinnerLearnSettings {
    /**
     * The name of the autobinning method.
     *
     * @author Heiko Hofer
     */
    public enum Method {
        /** Fixed number of bins. */
        fixedNumber,
        /** Estimated sample quantiles. */
        sampleQuantiles
    }

    /**
     * The name of the equality method.
     *
     * @author Patrick Winter
     * @since 2.10
     */
    public enum EqualityMethod {
        /** Sizes of the bins are equal. */
        width,
        /** Element count of the bins are equal. */
        frequency
    }

    /**
     * The method for naming bins.
     *
     * @author Heiko Hofer
     */
    public enum BinNaming implements ButtonGroupEnumInterface {
        /** Numbered starting from one: Bin 1, Bin2, ... */
        numbered("Numbered", "e.g.: Bin 1, Bin 2, Bin 3"),
        /** Use edges for defining bins: (-,0] (0,1], ... */
        edges("Borders", "e.g.: [-10,0], (0,10], (10,20]" ),
        /**
         * Use midpoint of bins: 0.25, 0.75, ...
         *
         * @since 2.10
         */
        midpoints("Midpoints", "e.g.: -5, 5, 15");


        private String m_label;

        private String m_desc;

        private BinNaming(final String label, final String desc) {
            m_label = label;
            m_desc = desc;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getText() {
            return m_label;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getActionCommand() {
            return name();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getToolTip() {
            return m_desc;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isDefault() {
            return numbered.equals(this);
        }


    }

    /**
     * The format of output decimals.
     *
     * @author "Patrick Winter"
     */
    public enum OutputFormat {
        /** Standard formatting. */
        Standard("Standard String"),
        /** Plain number formatting. */
        Plain("Plain String (no exponent)"),
        /** Engineering formatting. */
        Engineering("Engineering String");
        private String m_label;
        /**
         * Constructor.
         * @param label The label shown in the dialog
         */
        OutputFormat(final String label) {
            m_label = label;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return m_label;
        }
    }

    /**
     * How decimals will be rounded.
     *
     * @author "Patrick Winter"
     */
    public enum PrecisionMode {
        /** Round to given number of decimal places. */
        Decimal("Decimal places"),
        /** Round to given number of significant figures. */
        Significant("Significant figures");
        private String m_label;
        /**
         * Constructor.
         * @param label The label shown in the dialog
         */
        PrecisionMode(final String label) {
            m_label = label;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return m_label;
        }
    }

    private static final String CFG_METHOD = "method";

    private static final String CFG_BIN_COUNT = "binCount";

    private static final String CFG_EQUALITY_METHOD = "equalityMethod";

    private static final String CFG_INTEGER_BOUNDS = "integerBounds";

    private static final String CFG_SAMPLE_QUANTILES = "sampleQuantiles";

    private static final String CFG_BIN_NAMING = "binNaming";

    private static final String CFG_REPLACE_COLUMN = "replaceColumn";

    private static final String CFG_ADVANCED_FORMATTING = "advancedFormatting";

    private static final String CFG_OUTPUT_FORMAT = "outputFormat";

    private static final String CFG_PRECISION = "precision";

    private static final String CFG_PRECISION_MODE = "precisionMode";

    private static final String CFG_ROUNDING_MODE = "roundingMode";

    private Method m_method = Method.fixedNumber;

    private int m_binCount = 5;

    private EqualityMethod m_equalityMethod = EqualityMethod.width;

    private boolean m_integerBounds = false;

    private double[] m_sampleQuantiles = new double[]{0, 0.25, 0.5, 0.75, 1};

    private BinNaming m_binNaming = BinNaming.numbered;

    private boolean m_replaceColumn = false;

    private boolean m_advancedFormatting = false;

    private OutputFormat m_outputFormat = OutputFormat.Standard;

    private int m_precision = 3;

    private PrecisionMode m_precisionMode = PrecisionMode.Decimal;

    private RoundingMode m_roundingMode = RoundingMode.HALF_UP;

    private DataColumnSpecFilterConfiguration m_filterConfiguration = AutoBinnerLearnNodeModel.createDCSFilterConfiguration();


    /**
     * @return the method
     */
    public Method getMethod() {
        return m_method;
    }

    /**
     * @param method the method to set
     */
    public void setMethod(final Method method) {
        m_method = method;
    }

    /**
     * @return the binCount
     */
    public int getBinCount() {
        return m_binCount;
    }

    /**
     * @param binCount the binCount to set
     */
    public void setBinCount(final int binCount) {
        m_binCount = binCount;
    }

    /**
     * @return the equalityMethod
     * @since 2.10
     */
    public EqualityMethod getEqualityMethod() {
        return m_equalityMethod;
    }

    /**
     * @param equalityMethod the equalityMethod to set
     * @since 2.10
     */
    public void setEqualityMethod(final EqualityMethod equalityMethod) {
        m_equalityMethod = equalityMethod;
    }

    /**
     * @return the integerBounds
     */
    public boolean getIntegerBounds() {
        return m_integerBounds;
    }

    /**
     * @param integerBounds the integerBounds to set
     */
    public void setIntegerBounds(final boolean integerBounds) {
        m_integerBounds = integerBounds;
    }

    /**
     * @return the sampleQuantiles
     */
    public double[] getSampleQuantiles() {
        return m_sampleQuantiles;
    }

    /**
     * @param sampleQuantiles the sampleQuantiles to set
     */
    public void setSampleQuantiles(final double[] sampleQuantiles) {
        m_sampleQuantiles = sampleQuantiles;
    }

    /**
     * @return the binNaming
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
     * @return the replaceColumn
     */
    public boolean getReplaceColumn() {
        return m_replaceColumn;
    }

    /**
     * @param replaceColumn the replaceColumn to set
     */
    public void setReplaceColumn(final boolean replaceColumn) {
        m_replaceColumn = replaceColumn;
    }

    /**
     * @return the advancedFormatting
     */
    public boolean getAdvancedFormatting() {
        return m_advancedFormatting;
    }

    /**
     * @param advancedFormatting the advancedFormatting to set
     */
    public void setAdvancedFormatting(final boolean advancedFormatting) {
        m_advancedFormatting = advancedFormatting;
    }

    /**
     * @return the outputFormat
     */
    public OutputFormat getOutputFormat() {
        return m_outputFormat;
    }

    /**
     * @param outputFormat the outputFormat to set
     */
    public void setOutputFormat(final OutputFormat outputFormat) {
        m_outputFormat = outputFormat;
    }

    /**
     * @return the precision
     */
    public int getPrecision() {
        return m_precision;
    }

    /**
     * @param precision the precision to set
     */
    public void setPrecision(final int precision) {
        m_precision = precision;
    }

    /**
     * @return the precisionMode
     */
    public PrecisionMode getPrecisionMode() {
        return m_precisionMode;
    }

    /**
     * @param precisionMode the precisionMode to set
     */
    public void setPrecisionMode(final PrecisionMode precisionMode) {
        m_precisionMode = precisionMode;
    }

    /**
     * @return the roundingMode
     */
    public RoundingMode getRoundingMode() {
        return m_roundingMode;
    }

    /**
     * @param roundingMode the roundingMode to set
     */
    public void setRoundingMode(final RoundingMode roundingMode) {
        m_roundingMode = roundingMode;
    }

    /**
     * Loads the settings from the node settings object.
     *
     * @param settings a node settings object
     * @throws InvalidSettingsException if some settings are missing
     */
    public void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        DataColumnSpecFilterConfiguration config = AutoBinnerLearnNodeModel.createDCSFilterConfiguration();
        config.loadConfigurationInModel(settings);
        m_filterConfiguration = config;
        m_method = Method.valueOf(settings.getString(CFG_METHOD));
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
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings, final DataTableSpec spec) {
        DataColumnSpecFilterConfiguration config = AutoBinnerLearnNodeModel.createDCSFilterConfiguration();
        config.loadConfigurationInDialog(settings, spec);
        m_filterConfiguration = config;
        m_method = Method.valueOf(settings.getString(CFG_METHOD, Method.fixedNumber.toString()));
        m_binCount = settings.getInt(CFG_BIN_COUNT, 5);
        m_equalityMethod = EqualityMethod.valueOf(settings.getString(CFG_EQUALITY_METHOD, EqualityMethod.width.name()));
        m_integerBounds = settings.getBoolean(CFG_INTEGER_BOUNDS, false);
        m_sampleQuantiles = settings.getDoubleArray(CFG_SAMPLE_QUANTILES, new double[]{0, 0.25, 0.5, 0.75, 1});
        m_binNaming = BinNaming.valueOf(settings.getString(CFG_BIN_NAMING, BinNaming.numbered.toString()));
        m_replaceColumn = settings.getBoolean(CFG_REPLACE_COLUMN, false);
        m_advancedFormatting = settings.getBoolean(CFG_ADVANCED_FORMATTING, false);
        m_outputFormat = OutputFormat.valueOf(settings.getString(CFG_OUTPUT_FORMAT, OutputFormat.Standard.name()));
        m_precision = settings.getInt(CFG_PRECISION, 3);
        m_precisionMode = PrecisionMode.valueOf(settings.getString(CFG_PRECISION_MODE, PrecisionMode.Decimal.name()));
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

    /**
     * @param config a filter configuration
     */
    public void setFilterConfiguration(final DataColumnSpecFilterConfiguration config) {
        m_filterConfiguration = config;

    }

    /**
     * @return filter configuration
     */
    public DataColumnSpecFilterConfiguration getFilterConfiguration() {
        return m_filterConfiguration;
    }

}
