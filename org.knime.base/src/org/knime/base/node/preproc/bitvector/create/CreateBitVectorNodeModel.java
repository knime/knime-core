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
 * -------------------------------------------------------------------
 *
 * History
 *   06.12.2005 (dill): created
 */
package org.knime.base.node.preproc.bitvector.create;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.data.bitvector.BitString2BitVectorCellFactory;
import org.knime.base.data.bitvector.BitVectorCellFactory;
import org.knime.base.data.bitvector.Collection2BitVectorCellFactory;
import org.knime.base.data.bitvector.Hex2BitVectorCellFactory;
import org.knime.base.data.bitvector.IdString2BitVectorCellFactory;
import org.knime.base.data.bitvector.MultiString2BitVectorCellFactory;
import org.knime.base.data.bitvector.Numeric2BitVectorMeanCellFactory;
import org.knime.base.data.bitvector.Numeric2BitVectorThresholdCellFactory;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.vector.bitvector.BitVectorType;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ButtonGroupEnumInterface;
import org.knime.core.node.util.filter.NameFilterConfiguration;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.util.filter.column.DataTypeColumnFilter;

/**
 * The BitvectorGenerator translates all values above or equal to a given
 * threshold to a set bit, values below that threshold to bits set to zero.
 * Thus, an n-column input will be translated to a 1-column table, where each
 * column contains a bit vector of length n.
 *
 * @author Tobias Koetter
 * @author Fabian Dill
 */
public class CreateBitVectorNodeModel extends NodeModel {

    private static final String FILE_NAME = "bitVectorParams";
    private static final String INT_CFG_ROWS = "nrOfRows";
    private static final String INT_CFG_NR_ZEROS = "nrOfZeros";
    private static final String INT_CFG_NR_ONES = "nrOfOnes";

    /**
     * Enum with the matching options.
     * @author koetter
     */
    enum SetMatching implements ButtonGroupEnumInterface {
        /**Matching.*/
        MATCHING("does match", "Set the bit for matching columns"),
        /**Not matching.*/
        NOT_MATCHING("does not match", "Set the bit for not matching columns");

        private final String m_toolTip;
        private final String m_text;

        private SetMatching(final String text, final String toolTip) {
            m_text = text;
            m_toolTip = toolTip;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getText() {
            return m_text;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getActionCommand() {
            return name();
        }

        /**
         * @param actionCommand to get the enum for
         * @return the enum that matches the action command
         */
        public static SetMatching get(final String actionCommand) {
            return valueOf(actionCommand);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getToolTip() {
            return m_toolTip;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isDefault() {
            return MATCHING == this;
        }
    }

    /**
     * Enum with the different supported column types to choose from.
     * @author Tobias Koetter
     */
    enum ColumnType implements ButtonGroupEnumInterface {
        /**Single string option.*/
        SINGLE_STRING("Create bit vectors from a single string column", null, false, StringValue.class),
        /**Single collection option.*/
        SINGLE_COLLECTION("Create bit vectors from a single collection column", null, false, CollectionDataValue.class),
        /**Multi string option.*/
        MULTI_STRING("Create bit vectors form multiple string columns", null, true, StringValue.class),
        /**Multi numeric option.*/
        MULTI_NUMERICAL("Create bit vector from multiple numeric columns", null, true, DoubleValue.class);

        private final String m_text;
        private final String m_toolTip;
        private final boolean m_multiColumn;
        private final Class<? extends DataValue>[] m_supportedClasses;

        @SafeVarargs
        private ColumnType(final String text, final String toolTip, final boolean multiColumn,
            final Class<? extends DataValue>... supportedClasses) {
            m_text = text;
            m_toolTip = toolTip;
            m_multiColumn = multiColumn;
            m_supportedClasses = supportedClasses;
        }

        /**
         * @return the multiColumn
         */
        public boolean isMultiColumn() {
            return m_multiColumn;
        }

        /**
         * @return the supportedClasses
         */
        public Class<? extends DataValue>[] getSupportedClasses() {
            return m_supportedClasses;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getText() {
            return m_text;
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
            return m_toolTip;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isDefault() {
            //have the single string as default option
            return SINGLE_STRING.equals(this);
        }

        /**
         * @return the default column type to use
         */
        public static ColumnType getDefault() {
            ColumnType[] values = values();
            for (final ColumnType type : values) {
                if (type.isDefault()) {
                    return type;
                }
            }
            //return the first if none is default
            return values[0];
        }

        /**
         * @param actionCommand the action command to get the type for
         * @return the type for the given action command
         */
        public static ColumnType getType(final String actionCommand) {
            return valueOf(actionCommand);
        }

        /**
         * @param dataType the {@link DataType} to check
         * @return <code>true</code> if the method supports the given {@link DataType}
         */
        public boolean isCompatible(final DataType dataType) {
            Class<? extends DataValue>[] supportedClasses = getSupportedClasses();
            for (Class<? extends DataValue> class1 : supportedClasses) {
                if (dataType.isCompatible(class1)) {
                    return true;
                }
            }
            return false;
        }

    }

    /**
     * Represents the string types that can be parsed.
     *
     * @author Fabian Dill, University of Konstanz
     */
    enum StringType {
        /** A hexadecimal representation of bits. */
        HEX("Hexadecimal representation of bits"),
        /**
         * A string of ids, where every id indicates the position in the bitset to set.
         */
        ID("A string of ids, where every id indicates the position in the bitset to set"),
        /** A string of '0' and '1'. */
        BIT("A string of '0' and '1'");

        private final String m_tooltip;

        /**
         *
         */
        private StringType(final String tooltip) {
            m_tooltip = tooltip;
        }

        /**
         * @return the tooltip
         */
        public String getTooltip() {
            return m_tooltip;
        }

        /**
         * @return array with all options as string
         */
        static String[] getStringValues() {
            StringType[] types = values();
            final String[] names = new String[types.length];
            for (int i = 0, length = types.length; i < length; i++) {
                names[i] = types[i].getActionCommand();
            }
            return names;
        }

        /**
         * @param actionCommand to get the string type for
         * @return the corresponding string type
         */
        static StringType getType(final String actionCommand) {
            return StringType.valueOf(actionCommand);
        }

        /**
         * @return the action command to use
         */
        String getActionCommand() {
            return name();
        }
    }


    private static final boolean DEFAULT_USE_MEAN = false;

    private SettingsModelBoolean m_failOnError = createFailOnErrorModel();

    private SettingsModelString m_columnType = createColumnTypeModel();

    //multi column options
    //multi numeric options
    private DataColumnSpecFilterConfiguration m_multiColumnsConfig = createMultiColumnConfig(null);
    private SettingsModelDouble m_threshold = createThresholdModel();
    private SettingsModelBoolean m_useMean = createUseMeanModel();
    private SettingsModelInteger m_meanPercentage = createMeanPercentageModel();
    //multi string options
    private SettingsModelString m_mscPattern = createMSCPattern();
    private SettingsModelBoolean m_mscRegex = createMSCRegexModel();
    private SettingsModelBoolean m_mscHasWildcards = createMSCHasWildcardsModel();
    private SettingsModelBoolean m_mscCaseSensitiv = createMSCCaseSensitiveModel();
    private SettingsModelString m_mscSetMatching = createMSCSetMatchingModel();

    //single column options
    private final SettingsModelString m_singleColumn = createSingleColumnModel();
    private SettingsModelString m_singleStringColumnType = createSingleStringColumnTypeModel();

    //general options
    private final SettingsModelString m_outputColumn = createOutputColumnModel();
    private final SettingsModelString m_vectorType = createVectorTypeModel();
    private SettingsModelBoolean m_remove = createRemoveColumnsModel();

    private int m_nrOfProcessedRows = 0;

    private int m_totalNrOf0s = 0;

    private int m_totalNrOf1s = 0;

    /**
     * @param type {@link ColumnType}
     * @return {@link DataColumnSpecFilterConfiguration}
     */
    static DataColumnSpecFilterConfiguration createMultiColumnConfig(final ColumnType type) {
        final DataTypeColumnFilter filter;
        if (type != null) {
            filter = new DataTypeColumnFilter(type.getSupportedClasses());
        } else {
            filter = null;
        }
        return new DataColumnSpecFilterConfiguration("multiColumnNames", filter,
            NameFilterConfiguration.FILTER_BY_NAMEPATTERN);
    }

    /**
     * @return the fail on parse error model
     */
    static SettingsModelBoolean createFailOnErrorModel() {
        return new SettingsModelBoolean("failOnParseError", false);
    }

    /**
     * @return the replace columns model
     */
    static SettingsModelBoolean createRemoveColumnsModel() {
        return new SettingsModelBoolean("removeInputColumns", false);
    }

    /**@return the multi string column set matching model*/
    static SettingsModelString createMSCSetMatchingModel() {
        final SettingsModelString m = new SettingsModelString("multiStringColumnSetMatching",
            SetMatching.MATCHING.getActionCommand());
        m.setEnabled(false);
        return m;
    }

    /**@return the multi string pattern.*/
    static SettingsModelString createMSCPattern() {
        final SettingsModelString m = new SettingsModelString("multiStringColumnPatter", null);
        m.setEnabled(false);
        return m;
    }

    /**@return the multi string column case sensitive model*/
    static SettingsModelBoolean createMSCCaseSensitiveModel() {
        final SettingsModelBoolean m = new SettingsModelBoolean("multiStringColumnCaseSensitive", false);
        m.setEnabled(false);
        return m;
    }

    /**@return the multi string column has wild card model*/
    static SettingsModelBoolean createMSCHasWildcardsModel() {
        final SettingsModelBoolean m = new SettingsModelBoolean("multiStringColumnHasWildcards", false);
        m.setEnabled(false);
        return m;
    }

    /**@return the multi string column is regex model*/
    static SettingsModelBoolean createMSCRegexModel() {
        final SettingsModelBoolean m = new SettingsModelBoolean("multiStringColumnIsRegex", false);
        m.setEnabled(false);
        return m;
    }

    /**
     * @return the from string model
     */
    static SettingsModelString createColumnTypeModel() {
        return new SettingsModelString("columnType", ColumnType.getDefault().getActionCommand());
    }

    /**
     * @return the use mean model
     */
    static SettingsModelBoolean createUseMeanModel() {
        final SettingsModelBoolean model = new SettingsModelBoolean("singleNumericUseMean", DEFAULT_USE_MEAN);
        model.setEnabled(ColumnType.getDefault().equals(ColumnType.MULTI_NUMERICAL));
        return model;
    }

    /**
     * @return the threshold model
     */
    static SettingsModelDouble createThresholdModel() {
        final SettingsModelDoubleBounded model = new SettingsModelDoubleBounded(
            "singleNumericThreshold", 1.0, -Double.MAX_VALUE, Double.MAX_VALUE);
        model.setEnabled(!DEFAULT_USE_MEAN && ColumnType.getDefault().equals(ColumnType.MULTI_NUMERICAL));
        return model;
    }

    /**
     * @return the mean percentage model from 0 to 100.
     */
    @SuppressWarnings("unused")
    static SettingsModelInteger createMeanPercentageModel() {
        final SettingsModelIntegerBounded model =
                new SettingsModelIntegerBounded("singleNumericMeanThreshold", 100, 0, Integer.MAX_VALUE);
      //disable the model since we use threshold by default
        model.setEnabled(DEFAULT_USE_MEAN && ColumnType.getDefault().equals(ColumnType.MULTI_NUMERICAL));
        return model;
    }

    /**
     * @return the settings model holding the selected string column name
     */
    static SettingsModelString createSingleColumnModel() {
        final SettingsModelString model = new SettingsModelString("singleColumnName", null);
        model.setEnabled(!ColumnType.getDefault().isMultiColumn());
        return model;
    }

    /**
     * @return the single string column type model.
     */
    static SettingsModelString createSingleStringColumnTypeModel() {
        final SettingsModelString model = new SettingsModelString("singleStringType",
            StringType.BIT.getActionCommand());
        model.setEnabled(ColumnType.getDefault().equals(ColumnType.SINGLE_STRING));
        return model;
    }

    /**
     *
     * @return the settings model for output column name
     */
    static SettingsModelString createOutputColumnModel() {
        return new SettingsModelString("outputColumnName", "BitVector");
    }

    /**
     * Creates an instance of the BitVectorGeneratorNodeModel with one inport
     * and one outport. The numerical values from the inport are translated to
     * bit vectors in the output.
     */
    CreateBitVectorNodeModel() {
        super(1, 1);
        //keep the models in sync with any changes to the column type model which might occur in the
        //load settings because of backward compatibility issues
        m_columnType.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                final ColumnType type =
                        CreateBitVectorNodeModel.ColumnType.getType(m_columnType.getStringValue());
                m_singleColumn.setEnabled(!type.isMultiColumn());
                m_singleStringColumnType.setEnabled(CreateBitVectorNodeModel.ColumnType.SINGLE_STRING.equals(type));
                m_useMean.setEnabled(CreateBitVectorNodeModel.ColumnType.MULTI_NUMERICAL.equals(type));
                m_threshold.setEnabled(!m_useMean.getBooleanValue()
                    && CreateBitVectorNodeModel.ColumnType.MULTI_NUMERICAL.equals(type));
                m_meanPercentage.setEnabled(m_useMean.getBooleanValue()
                    && CreateBitVectorNodeModel.ColumnType.MULTI_NUMERICAL.equals(type));
                if (type.isMultiColumn()) {
                    m_multiColumnsConfig = createMultiColumnConfig(type);
                }
            }
        });
    }

    /**
     * @return the vector type model
     */
    static SettingsModelString createVectorTypeModel() {
        return new SettingsModelString("vectorType",
            org.knime.core.data.vector.bitvector.BitVectorType.getDefault().getActionCommand());
    }

    /**
     * @return the number of processed rows
     */
    int getNumberOfProcessedRows() {
        return m_nrOfProcessedRows;
    }

    /**
     *
     * @return the number of 1s generated
     */
    int getTotalNrOf1s() {
        return m_totalNrOf1s;
    }

    /**
     *
     * @return the number of 0s generated
     */
    int getTotalNrOf0s() {
        return m_totalNrOf0s;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        final ColumnType columnType = ColumnType.getType(
            ((SettingsModelString)m_columnType.createCloneWithValidatedValue(settings)).getStringValue());

        final DataColumnSpecFilterConfiguration tmpMultiColumnConfig = createMultiColumnConfig(null);
        tmpMultiColumnConfig.loadConfigurationInModel(settings);

        m_mscPattern.validateSettings(settings);
        m_mscHasWildcards.validateSettings(settings);
        m_mscCaseSensitiv.validateSettings(settings);
        m_mscRegex.validateSettings(settings);
        SetMatching.get(
            ((SettingsModelString)m_mscSetMatching.createCloneWithValidatedValue(settings)).getStringValue());

        m_threshold.validateSettings(settings);
        m_useMean.validateSettings(settings);
        m_meanPercentage.validateSettings(settings);

        final String singleCol =
                ((SettingsModelString)m_singleColumn.createCloneWithValidatedValue(settings)).getStringValue();
        if (!columnType.isMultiColumn() && singleCol == null) {
            throw new InvalidSettingsException("Please select the single column to process");
        }
        m_singleStringColumnType.validateSettings(settings);

        m_remove.validateSettings(settings);
        final String outCol =
                ((SettingsModelString)m_outputColumn.createCloneWithValidatedValue(settings)).getStringValue();
        if (outCol == null || outCol.trim().isEmpty()) {
            throw new InvalidSettingsException("Please specify the name of the output column");
        }
        m_failOnError.validateSettings(settings);
        BitVectorType.getType(
            ((SettingsModelString)m_vectorType.createCloneWithValidatedValue(settings)).getStringValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_columnType.loadSettingsFrom(settings);

        m_multiColumnsConfig.loadConfigurationInModel(settings);

        m_mscPattern.loadSettingsFrom(settings);
        m_mscHasWildcards.loadSettingsFrom(settings);
        m_mscCaseSensitiv.loadSettingsFrom(settings);
        m_mscRegex.loadSettingsFrom(settings);
        m_mscSetMatching.loadSettingsFrom(settings);

        m_threshold.loadSettingsFrom(settings);
        m_useMean.loadSettingsFrom(settings);
        m_meanPercentage.loadSettingsFrom(settings);

        m_singleColumn.loadSettingsFrom(settings);
        m_singleStringColumnType.loadSettingsFrom(settings);

        m_remove.loadSettingsFrom(settings);
        m_outputColumn.loadSettingsFrom(settings);
        m_failOnError.loadSettingsFrom(settings);
        m_vectorType.loadSettingsFrom(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_columnType.saveSettingsTo(settings);

        m_multiColumnsConfig.saveConfiguration(settings);

        m_mscPattern.saveSettingsTo(settings);
        m_mscHasWildcards.saveSettingsTo(settings);
        m_mscCaseSensitiv.saveSettingsTo(settings);
        m_mscRegex.saveSettingsTo(settings);
        m_mscSetMatching.saveSettingsTo(settings);

        m_threshold.saveSettingsTo(settings);
        m_useMean.saveSettingsTo(settings);
        m_meanPercentage.saveSettingsTo(settings);

        m_singleColumn.saveSettingsTo(settings);
        m_singleStringColumnType.saveSettingsTo(settings);

        m_remove.saveSettingsTo(settings);
        m_outputColumn.saveSettingsTo(settings);
        m_failOnError.saveSettingsTo(settings);
        m_vectorType.saveSettingsTo(settings);
    }

    /**
     * Assume to get numeric data only. Output is one column of type BitVector.
     *
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        final DataTableSpec spec = inSpecs[0];
        //check the uniqueness of the output column
        if (spec.containsName(m_outputColumn.getStringValue())) {
            throw new InvalidSettingsException("Input table contains column with name "
                    + m_outputColumn.getStringValue() + " please specifiy a new output column name.");
        }
        final ColumnType columnType = ColumnType.getType(m_columnType.getStringValue());
        final BitVectorType vectorType = BitVectorType.getType(m_vectorType.getStringValue());
        final DataTableSpec newSpec;
        if (columnType.isMultiColumn()) {
            final FilterResult multiColFilter = m_multiColumnsConfig.applyTo(spec);
            final String[] includes = multiColFilter.getIncludes();
            if (includes == null || includes.length < 1) {
                throw new InvalidSettingsException(
                    "No column selected in the multi column selection panel. Please (re-)configure the node.");
            }
            final String[] removedFromIncludes = multiColFilter.getRemovedFromIncludes();
            if (removedFromIncludes != null && removedFromIncludes.length > 0) {
                setWarningMessage("Columns " + convert2String(5, removedFromIncludes) + " not found in input table. ");
            }
            //create the output spec
            final DataColumnSpec newColSpec = createMultiColumnOutputSpec(spec, includes, vectorType);
            if (m_remove.getBooleanValue()) {
                final ColumnRearranger colR = new ColumnRearranger(spec);
                colR.remove(includes);
                newSpec = new DataTableSpec(colR.createSpec(), new DataTableSpec(newColSpec));
            } else {
                newSpec = new DataTableSpec(spec, new DataTableSpec(newColSpec));
            }
        } else {
            // parse from single column
            if (m_singleColumn.getStringValue() == null) {
                throw new InvalidSettingsException("No single column selected. Please (re-)configure the node.");
            }
            final int stringColIdx = spec.findColumnIndex(m_singleColumn.getStringValue());
            // -> check if selected column exists in the input table
            if (stringColIdx < 0) {
                throw new InvalidSettingsException("Selected column " + m_singleColumn.getStringValue()
                        + " not in the input table");
            }
            //check that the data type is supported by the selected method
            final DataType selectedDataType = spec.getColumnSpec(m_singleColumn.getStringValue()).getType();
            if (!columnType.isCompatible(selectedDataType)) {
                throw new InvalidSettingsException("Data type of column " + m_singleColumn.getStringValue()
                    + " is not compatible with selected method");
            }
            //create the output spec
            final ColumnRearranger c = createSingleColumnRearranger(inSpecs[0], stringColIdx, columnType, vectorType);
            newSpec = c.createSpec();
        }
        return new DataTableSpec[]{newSpec};
    }

    private static String convert2String(final int limit, final String... removedFromIncludes) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0, length = removedFromIncludes.length; i < length; i++) {
            if (i > 0) {
                buf.append(", ");
                if (i > limit) {
                    buf.append("...");
                    break;
                }
            }
            buf.append(removedFromIncludes[i]);
        }
        return buf.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        final BufferedDataTable data = inData[0];
        final DataTableSpec spec = data.getDataTableSpec();
        final ColumnType columnType = ColumnType.getType(m_columnType.getStringValue());
        final BitVectorType vectorType = BitVectorType.getType(m_vectorType.getStringValue());
        final String[] parsedColumnNames;
        final BitVectorCellFactory factory;
        if (columnType.isMultiColumn()) {
            final FilterResult multiColFilter = m_multiColumnsConfig.applyTo(spec);
            parsedColumnNames = multiColFilter.getIncludes();
            factory = createMultiColumnCellFactory(data, exec, columnType, vectorType, parsedColumnNames);
        } else {
            final int colIdx = spec.findColumnIndex(m_singleColumn.getStringValue());
            factory = getSingleColFactory(exec, colIdx, spec, data, columnType, vectorType);
            parsedColumnNames = new String[] {m_singleColumn.getStringValue()};
        }
        final ColumnRearranger c = new ColumnRearranger(spec);
        if (m_remove.getBooleanValue()) {
            if (columnType.isMultiColumn()) {
                c.remove(parsedColumnNames);
                c.append(factory);
            } else {
                c.replace(factory, m_singleColumn.getStringValue());
            }
        } else {
            c.append(factory);
        }
        factory.setFailOnError(m_failOnError.getBooleanValue());
        final ExecutionMonitor subExec;
        if (ColumnType.MULTI_NUMERICAL.equals(columnType) || (ColumnType.MULTI_NUMERICAL.equals(columnType)
                && StringType.ID.equals(StringType.getType(m_singleStringColumnType.getStringValue())))
                || ColumnType.SINGLE_COLLECTION.equals(columnType)) {
            subExec = exec.createSubProgress(0.5);
        } else {
            subExec = exec;
        }
        final BufferedDataTable out = exec.createColumnRearrangeTable(data, c, subExec);
        if (!factory.wasSuccessful() && data.size() > 0) {
            final String errorMessage = factory.getNoOfPrintedErrors() + " errors found. Last message: "
                    + factory.getLastErrorMessage() + ". See log file for details on all errors.";
            setWarningMessage(errorMessage);
        }
        m_nrOfProcessedRows = factory.getNrOfProcessedRows();
        m_totalNrOf0s = factory.getNumberOfNotSetBits();
        m_totalNrOf1s = factory.getNumberOfSetBits();
        return new BufferedDataTable[]{out};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_totalNrOf0s = 0;
        m_totalNrOf1s = 0;
        m_nrOfProcessedRows = 0;
    }

    private BitVectorCellFactory createMultiColumnCellFactory(final BufferedDataTable data,
            final ExecutionContext exec, final ColumnType columnType, final BitVectorType vectorType,
            final String[] multiCols)
                    throws CanceledExecutionException, InvalidSettingsException {
        final DataColumnSpec colSpec =
                createMultiColumnOutputSpec(data.getDataTableSpec(), multiCols, vectorType);
        // get the indices for included columns
        final int[]colIndices = new int[multiCols.length];
        int idx = 0;
        for (String colName : multiCols) {
            int index = data.getDataTableSpec().findColumnIndex(colName);
            if (index < 0) {
                throw new IllegalArgumentException(
                        "Column " + colName + " is not available in input table. Please re-configure the node.");
            }
            colIndices[idx++] = index;
        }
        final BitVectorCellFactory factory;
        if (ColumnType.MULTI_NUMERICAL.equals(columnType)) {
            // calculate bits from numeric data
            if (m_useMean.getBooleanValue()) {
                // either from a percentage of the mean
                final double meanFactor = m_meanPercentage.getIntValue() / 100.0;
                final double[] meanValues = calculateMeanValues(exec.createSubProgress(0.5), data, colIndices);
                factory = new Numeric2BitVectorMeanCellFactory(vectorType, colSpec, meanFactor, meanValues, colIndices);
            } else {
                // or dependent on fixed threshold
                factory = new Numeric2BitVectorThresholdCellFactory(vectorType, colSpec,
                    m_threshold.getDoubleValue(), colIndices);
            }
        } else if (ColumnType.MULTI_STRING.equals(columnType)) {
            final boolean setMatching =
                    SetMatching.MATCHING.equals(SetMatching.get(m_mscSetMatching.getStringValue()));
            factory = new MultiString2BitVectorCellFactory(vectorType, colSpec, m_mscCaseSensitiv.getBooleanValue(),
                m_mscHasWildcards.getBooleanValue(), m_mscRegex.getBooleanValue(), setMatching,
                m_mscPattern.getStringValue(), colIndices);
        } else {
            throw new IllegalStateException("Not implemeted column type " + columnType.getText());
        }
        return factory;
    }

    private double[] calculateMeanValues(final ExecutionMonitor exec, final BufferedDataTable input,
        final int[] colIndices)
            throws CanceledExecutionException {
        double[] meanValues = new double[input.getDataTableSpec().getNumColumns()];
        long nrOfRows = 0;
        final long rowCount = input.size();
        for (DataRow row : input) {
            exec.setProgress(nrOfRows / (double) rowCount, "Computing mean value. Processing row "
                    + nrOfRows + " of " + rowCount);
            exec.checkCanceled();
            for (int i = 0; i < colIndices.length; i++) {
                DataCell cell = row.getCell(colIndices[i]);
                if (cell.isMissing()) {
                    continue;
                }
                if (cell instanceof DoubleValue) {
                    meanValues[i] += ((DoubleValue)cell).getDoubleValue();
                } else {
                    throw new RuntimeException("Found incompatible type in row " + row.getKey().getString());
                }
            }
            nrOfRows++;
        }
        for (int i = 0; i < meanValues.length; i++) {
            meanValues[i] = meanValues[i] / nrOfRows;
        }
        return meanValues;
    }

    private int scanMaxPos(final BufferedDataTable data, final ExecutionMonitor exec)
            throws CanceledExecutionException {
        int maxPos = Integer.MIN_VALUE;
        int cellIdx = data.getDataTableSpec().findColumnIndex(m_singleColumn.getStringValue());
        long nrRows = data.size();
        long currRow = 0;
        for (DataRow row : data) {
            currRow++;
            exec.setProgress((double)currRow / (double)nrRows, "processing row " + currRow + " of " + nrRows);
            exec.checkCanceled();
            DataCell cell = row.getCell(cellIdx);
            if (cell.isMissing()) {
                continue;
            }
            if (cell instanceof StringValue) {
                final String toParse = ((StringValue)cell).getStringValue();
                final String[] numbers = toParse.split("\\s");
                for (int i = 0; i < numbers.length; i++) {
                    int pos = -1;
                    try {
                        pos = Integer.parseInt(numbers[i].trim());
                        maxPos = Math.max(maxPos, pos);
                    } catch (NumberFormatException nfe) {
                        // nothing to do here
                        // same exception will be logged from cell factory
                    }
                }
            } else {
                throw new RuntimeException("Found incompatible type in row " + row.getKey().getString());
            }
        }
        return maxPos + 1;
    }

    private ColumnRearranger createSingleColumnRearranger(final DataTableSpec spec, final int colIdx,
        final ColumnType columnType, final BitVectorType vectorType)
            throws InvalidSettingsException {
        // BW: fixed here locally: the annotation and the column name
        // are taken from input spec (21 Sep 2006)
        try {
            final BitVectorCellFactory factory = getSingleColFactory(null, colIdx, spec, null, columnType, vectorType);
            ColumnRearranger c = new ColumnRearranger(spec);
            if (m_remove.getBooleanValue()) {
                c.replace(factory , colIdx);
            } else {
                c.append(factory);
            }
            return c;
        } catch (CanceledExecutionException e) {
            //this shouldn't happen since we do not provide the data to perform the preprocessing
            throw new RuntimeException(e);
        }
    }

    private BitVectorCellFactory getSingleColFactory(final ExecutionMonitor exec, final int colIdx,
        final DataTableSpec spec, final BufferedDataTable data, final ColumnType columnType,
        final BitVectorType vectorType) throws InvalidSettingsException, CanceledExecutionException {
        final String outColName = m_outputColumn.getStringValue();
        final DataColumnSpecCreator creator = new DataColumnSpecCreator(outColName, vectorType.getCellDataType());
        final BitVectorCellFactory factory;
        if (ColumnType.SINGLE_STRING.equals(columnType)) {
            final StringType singleStringColType = StringType.getType(m_singleStringColumnType.getStringValue());
            final DataColumnSpec colSpec = creator.createSpec();
            switch (singleStringColType) {
                case BIT:
                    factory = new BitString2BitVectorCellFactory(vectorType, colSpec, colIdx);
                    break;
                case HEX:
                    factory = new Hex2BitVectorCellFactory(vectorType, colSpec, colIdx);
                    break;
                case ID:
                    final int maxPosition;
                    if (data != null) {
                        final ExecutionMonitor scanExec = exec.createSubProgress(0.5);
                        exec.setMessage("preparing");
                        maxPosition = scanMaxPos(data, scanExec);
                    } else {
                        maxPosition = 0;
                    }
                    factory = new IdString2BitVectorCellFactory(vectorType, colSpec, colIdx, maxPosition);
                    break;
                default:
                    throw new InvalidSettingsException(
                        "String type to parse bit vectors from unknown type " + singleStringColType.getActionCommand());
            }
        } else if (ColumnType.SINGLE_COLLECTION.equals(columnType)) {
            final Map<String, Integer> idxMap;
            if (data != null) {
                final ExecutionMonitor scanExec = exec.createSubProgress(0.5);
                scanExec.setMessage("preparing");
                final List<String> elementNames = new ArrayList<>();
                idxMap = new HashMap<>();
                long nrRows = data.size();
                long currRow = 0;
                for (DataRow row : data) {

                    currRow++;
                    scanExec.setProgress((double)currRow / (double)nrRows,
                        "Counting uniqe elements. Processing row " + currRow + " of " + nrRows);
                    scanExec.checkCanceled();
                    final DataCell cell = row.getCell(colIdx);
                    if (cell.isMissing()) {
                        //ignore missing cells
                        continue;
                    }
                    if (cell instanceof CollectionDataValue) {
                        final CollectionDataValue collCell = (CollectionDataValue)cell;
                        for (DataCell collVal : collCell) {
                            String stringRep = collVal.toString();
                            Integer idx = idxMap.get(stringRep);
                            if (idx == null) {
                                idx = Integer.valueOf(idxMap.size());
                                idxMap.put(stringRep, idx);
                                elementNames.add(stringRep);
                            }
                        }
                    } else {
                        throw new RuntimeException("Found incompatible type in row " + row.getKey().getString());
                    }
                }
                creator.setElementNames(elementNames.toArray(new String[0]));
            } else {
                idxMap = Collections.EMPTY_MAP;
            }
            factory = new Collection2BitVectorCellFactory(vectorType, creator.createSpec(), colIdx, idxMap);
        } else {
            throw new java.lang.IllegalStateException("Single column type not implemented: " + columnType);
        }
        return factory;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException {
        File f = new File(internDir, FILE_NAME);
        FileInputStream fis = new FileInputStream(f);
        NodeSettingsRO internSettings = NodeSettings.loadFromXML(fis);
        try {
            m_nrOfProcessedRows = internSettings.getInt(INT_CFG_ROWS);
            m_totalNrOf0s = internSettings.getInt(INT_CFG_NR_ZEROS);
            m_totalNrOf1s = internSettings.getInt(INT_CFG_NR_ONES);
        } catch (InvalidSettingsException ise) {
            throw new IOException(ise.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException {
        NodeSettings internSettings = new NodeSettings(FILE_NAME);
        internSettings.addInt(INT_CFG_ROWS, m_nrOfProcessedRows);
        internSettings.addInt(INT_CFG_NR_ZEROS, m_totalNrOf0s);
        internSettings.addInt(INT_CFG_NR_ONES, m_totalNrOf1s);
        File f = new File(internDir, FILE_NAME);
        FileOutputStream fos = new FileOutputStream(f);
        internSettings.saveToXML(fos);
    }

    private DataColumnSpec createMultiColumnOutputSpec(final DataTableSpec spec, final String[] colNames,
        final BitVectorType vectorType)
            throws InvalidSettingsException {
        final String name = m_outputColumn.getStringValue();
        // get the names of numeric columns
        final DataColumnSpecCreator creator = new DataColumnSpecCreator(name, vectorType.getCellDataType());
        creator.setElementNames(colNames);
        return creator.createSpec();
    }
}
