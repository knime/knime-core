/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   Feb 6, 2017 (simon): created
 */
package org.knime.time.node.convert.stringtodurationperiod;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.knime.base.data.replace.ReplacedColumnsDataRow;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.StringValue;
import org.knime.core.data.append.AppendedColumnRow;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.time.duration.DurationCellFactory;
import org.knime.core.data.time.period.PeriodCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.Config;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.DataTableRowInput;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.MergeOperator;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.streamable.StreamableOperatorInternals;
import org.knime.core.node.streamable.simple.SimpleStreamableOperatorInternals;
import org.knime.core.node.util.filter.InputFilter;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.time.util.DurationPeriodFormatUtils;

/**
 * The node model of the node which converts string cells to period or duration cells.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
final class StringToDurationPeriodNodeModel extends NodeModel {
    StringToDurationPeriodNodeModel() {
        super(1, 1);
    }

    static final String OPTION_APPEND = "Append selected columns";

    static final String OPTION_REPLACE = "Replace selected columns";

    static final String OPTION_AUTOMATIC = "Automatically detect type";

    static final String OPTION_DURATION = "Create Duration";

    static final String OPTION_PERIOD = "Create Period";

    private final SettingsModelColumnFilter2 m_colSelect = createColSelectModel();

    private final SettingsModelString m_isReplaceOrAppend = createReplaceAppendStringBool();

    private final SettingsModelString m_suffix = createSuffixModel(m_isReplaceOrAppend);

    private final SettingsModelString m_type = createTypeSelection();

    private final SettingsModelBoolean m_cancelOnFail = createCancelOnFailModel();

    private int m_failCounter;

    private DataType[] m_detectedTypes;

    private boolean m_hasValidatedConfiguration = false;

    /** @return the column select model, used in both dialog and model. */
    @SuppressWarnings("unchecked")
    public static SettingsModelColumnFilter2 createColSelectModel() {
        return new SettingsModelColumnFilter2("col_select", StringValue.class);
    }

    /** @return the string model, used in both dialog and model. */
    public static SettingsModelString createReplaceAppendStringBool() {
        return new SettingsModelString("replace_or_append", OPTION_REPLACE);
    }

    /**
     * @param replaceOrAppendModel model for the replace/append button group
     * @return the string model, used in both dialog and model.
     */
    public static SettingsModelString createSuffixModel(final SettingsModelString replaceOrAppendModel) {
        final SettingsModelString suffixModel = new SettingsModelString("suffix", "(Duration/Period)");
        replaceOrAppendModel.addChangeListener(
            e -> suffixModel.setEnabled(replaceOrAppendModel.getStringValue().equals(OPTION_APPEND)));
        suffixModel.setEnabled(false);
        return suffixModel;
    }

    /** @return the string model, used in both dialog and model. */
    public static SettingsModelString createTypeSelection() {
        return new SettingsModelString("duration_or_period", OPTION_AUTOMATIC);
    }

    /** @return the boolean model, used in both dialog and model. */
    static SettingsModelBoolean createCancelOnFailModel() {
        return new SettingsModelBoolean("cancel_on_fail", true);
    }

    /**
     * Sets the column selections to not include "real" string columns (only).
     *
     * @param tableSpec the corresponding spec
     */
    private void setDefaultColumnSelection(final DataTableSpec tableSpec) {
        final InputFilter<DataColumnSpec> filter = new InputFilter<DataColumnSpec>() {
            @Override
            public boolean include(final DataColumnSpec spec) {
                return spec.getType().getPreferredValueClass() == StringValue.class;
            }
        };
        m_colSelect.loadDefaults(tableSpec, filter, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        if (!m_hasValidatedConfiguration) {
            setDefaultColumnSelection(inSpecs[0]);
        }
        final List<String> includeList =
            new LinkedList<String>(Arrays.asList(m_colSelect.applyTo(inSpecs[0]).getIncludes()));
        if (m_type.getStringValue().equals(OPTION_DURATION)) {
            m_detectedTypes = new DataType[includeList.size()];
            Arrays.fill(m_detectedTypes, DurationCellFactory.TYPE);
        }
        if (m_type.getStringValue().equals(OPTION_PERIOD)) {
            m_detectedTypes = new DataType[includeList.size()];
            Arrays.fill(m_detectedTypes, PeriodCellFactory.TYPE);
        }
        if (m_type.getStringValue().equals(OPTION_AUTOMATIC)) {
            return new DataTableSpec[]{null};
        }
        final DataTableSpec in = inSpecs[0];
        final ColumnRearranger r = createColumnRearranger(in);
        final DataTableSpec out = r.createSpec();
        return new DataTableSpec[]{out};
    }

    /** {@inheritDoc} */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        final BufferedDataTable in = inData[0];
        if (m_type.getStringValue().equals(OPTION_AUTOMATIC)) {
            detectTypes(new DataTableRowInput(in));
            // no more rows to look at, guess that column is Period, if it was not detected
            for (int i = 0; i < m_detectedTypes.length; i++) {
                if (m_detectedTypes[i] == null) {
                    m_detectedTypes[i] = PeriodCellFactory.TYPE;
                }
            }
        }
        final ColumnRearranger r = createColumnRearranger(in.getDataTableSpec());
        final BufferedDataTable out = exec.createColumnRearrangeTable(in, r, exec);
        if (m_failCounter > 0) {
            setWarningMessage(
                m_failCounter + " rows could not be converted. Check the message in the missing cells for details.");
        }
        return new BufferedDataTable[]{out};
    }

    private void detectTypes(final RowInput rowInput) throws InterruptedException {
        final DataTableSpec spec = rowInput.getDataTableSpec();
        final String[] includes = m_colSelect.applyTo(spec).getIncludes();
        if (m_detectedTypes == null) {
            m_detectedTypes = new DataType[includes.length];
        }
        if (m_type.getStringValue().equals(OPTION_DURATION)) {
            Arrays.fill(m_detectedTypes, DurationCellFactory.TYPE);
        }
        if (m_type.getStringValue().equals(OPTION_PERIOD)) {
            Arrays.fill(m_detectedTypes, PeriodCellFactory.TYPE);
        }

        if (m_type.getStringValue().equals(OPTION_AUTOMATIC)) {
            DataRow row;
            while ((row = rowInput.poll()) != null) {
                boolean isCellMissing = false;
                for (int i = 0; i < includes.length; i++) {
                    if (m_detectedTypes[i] == null) {
                        final DataCell cell = row.getCell(spec.findColumnIndex(includes[i]));
                        if (cell.isMissing()) {
                            isCellMissing = true;
                        } else {
                            final String string = ((StringValue)cell).getStringValue();
                            try {
                                DurationPeriodFormatUtils.parseDuration(string);
                                m_detectedTypes[i] = DurationCellFactory.TYPE;
                            } catch (DateTimeParseException e1) {
                                try {
                                    DurationPeriodFormatUtils.parsePeriod(string);
                                    m_detectedTypes[i] = PeriodCellFactory.TYPE;
                                } catch (DateTimeParseException e2) {
                                    isCellMissing = true;
                                }
                            }
                        }
                    }
                }
                if (!isCellMissing) {
                    // finished - every column type is detected
                    break;
                }
            }
        }
    }

    private ColumnRearranger createColumnRearranger(final DataTableSpec spec) throws InvalidSettingsException {
        final ColumnRearranger rearranger = new ColumnRearranger(spec);
        final String[] includeList = m_colSelect.applyTo(spec).getIncludes();
        final int[] includeIndices =
            Arrays.stream(m_colSelect.applyTo(spec).getIncludes()).mapToInt(s -> spec.findColumnIndex(s)).toArray();

        int i = 0;
        for (final String includedCol : includeList) {
            if (m_isReplaceOrAppend.getStringValue().equals(OPTION_REPLACE)) {
                final DataColumnSpecCreator dataColumnSpecCreator =
                    new DataColumnSpecCreator(includedCol, m_detectedTypes[i]);
                final StringToDurationPeriodCellFactory cellFac =
                    new StringToDurationPeriodCellFactory(dataColumnSpecCreator.createSpec(), includeIndices[i++]);
                rearranger.replace(cellFac, includedCol);
            } else {
                final DataColumnSpec dataColSpec = new UniqueNameGenerator(spec)
                    .newColumn(includedCol + m_suffix.getStringValue(), m_detectedTypes[i]);
                final StringToDurationPeriodCellFactory cellFac =
                    new StringToDurationPeriodCellFactory(dataColSpec, includeIndices[i++]);
                rearranger.append(cellFac);
            }
        }
        return rearranger;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_colSelect.saveSettingsTo(settings);
        m_isReplaceOrAppend.saveSettingsTo(settings);
        m_suffix.saveSettingsTo(settings);
        m_type.saveSettingsTo(settings);
        m_cancelOnFail.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_colSelect.validateSettings(settings);
        m_isReplaceOrAppend.validateSettings(settings);
        m_suffix.validateSettings(settings);
        m_type.validateSettings(settings);
        m_cancelOnFail.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_colSelect.loadSettingsFrom(settings);
        m_isReplaceOrAppend.loadSettingsFrom(settings);
        m_suffix.loadSettingsFrom(settings);
        m_type.loadSettingsFrom(settings);
        m_cancelOnFail.loadSettingsFrom(settings);
        m_hasValidatedConfiguration = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_failCounter = 0;
        m_detectedTypes = null;
    }

    private final class StringToDurationPeriodCellFactory extends SingleCellFactory {
        private final int m_colIndex;

        /**
         * @param newColSpec
         * @param colIndex
         */
        public StringToDurationPeriodCellFactory(final DataColumnSpec newColSpec, final int colIndex) {
            super(newColSpec);
            m_colIndex = colIndex;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell getCell(final DataRow row) {
            final DataCell cell = row.getCell(m_colIndex);
            if (cell.isMissing()) {
                return cell;
            }
            final DataColumnSpec newColumnSpec = getColumnSpecs()[0];

            if (newColumnSpec.getType().equals(DurationCellFactory.TYPE)) {
                try {
                    return DurationCellFactory
                        .create(DurationPeriodFormatUtils.parseDuration(((StringValue)cell).getStringValue()));
                } catch (DateTimeParseException e) {
                    if (m_cancelOnFail.getBooleanValue()) {
                        throw new IllegalArgumentException(
                            "Failed to parse duration in row '" + row.getKey() + "': " + e.getMessage());
                    }
                    m_failCounter++;
                    return new MissingCell(e.getMessage());
                }
            } else {
                try {
                    return PeriodCellFactory
                        .create(DurationPeriodFormatUtils.parsePeriod(((StringValue)cell).getStringValue()));
                } catch (DateTimeParseException e) {
                    if (m_cancelOnFail.getBooleanValue()) {
                        throw new IllegalArgumentException(
                            "Failed to parse period in row '" + row.getKey() + "': " + e.getMessage());
                    }
                    m_failCounter++;
                    return new MissingCell(e.getMessage());
                }
            }
        }

    }

    @Override
    public StreamableOperatorInternals createInitialStreamableOperatorInternals() {
        final SimpleStreamableOperatorInternals simpleStreamableOperatorInternals = new SimpleStreamableOperatorInternals();
        simpleStreamableOperatorInternals.getConfig().addBoolean("needsIteration", true);
        simpleStreamableOperatorInternals.getConfig().addInt("sizeRow", 0);
        return simpleStreamableOperatorInternals;
    }

    @Override
    public boolean iterate(final StreamableOperatorInternals internals) {
        return ((SimpleStreamableOperatorInternals)internals).getConfig().getBoolean("needsIteration", false);
    }

    @Override
    public void finishStreamableExecution(final StreamableOperatorInternals internals, final ExecutionContext exec,
        final PortOutput[] output) throws Exception {
        // not needed
    }

    @Override
    public MergeOperator createMergeOperator() {
        return new MergeOperator() {
            @Override
            public StreamableOperatorInternals mergeIntermediate(final StreamableOperatorInternals[] operators) {
                final SimpleStreamableOperatorInternals internals = new SimpleStreamableOperatorInternals();
                final Config config = internals.getConfig();
                for (StreamableOperatorInternals operator : operators) {
                    final Config configToMerge = ((SimpleStreamableOperatorInternals)operator).getConfig();
                    final int sizeRow = configToMerge.getInt("sizeRow", -1);
                    config.addInt("sizeRow", sizeRow);
                    for (int i = 0; i < sizeRow; i++) {
                        if (!config.containsKey("type" + i) && configToMerge.getDataType("type" + i, null) != null) {
                            config.addDataType("type" + i, configToMerge.getDataType("type" + i, null));
                            config.addString("colname" + i, configToMerge.getString("colname" + i, null));
                        }
                        if (!config.containsKey("detected_type" + i)
                            && configToMerge.getDataType("detected_type" + i, null) != null) {
                            config.addDataType("detected_type" + i,
                                configToMerge.getDataType("detected_type" + i, null));
                        }
                    }
                }
                // if a column's type could not be detected, guess it to be a PeriodCell
                final Config configToMerge = ((SimpleStreamableOperatorInternals)operators[0]).getConfig();
                for (int i = 0; i < configToMerge.getInt("sizeRow", -1); i++) {
                    if (!config.containsKey("type" + i)) {
                        config.addDataType("type" + i, PeriodCellFactory.TYPE);
                        config.addString("colname" + i, configToMerge.getString("colname" + i, null));
                    }
                    if (!config.containsKey("detected_type" + i)) {
                        config.addDataType("detected_type" + i, PeriodCellFactory.TYPE);
                    }
                }

                return internals;
            }

            @Override
            public StreamableOperatorInternals mergeFinal(final StreamableOperatorInternals[] operators) {
                return null;
            }
        };

    }

    @Override
    public PortObjectSpec[] computeFinalOutputSpecs(final StreamableOperatorInternals internals,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        if (m_type.getStringValue().equals(OPTION_AUTOMATIC)) {
            final SimpleStreamableOperatorInternals simpleInternals = (SimpleStreamableOperatorInternals)internals;
            final Config config = simpleInternals.getConfig();
            final DataColumnSpec[] colSpecs = new DataColumnSpec[config.getInt("sizeRow")];
            for (int i = 0; i < colSpecs.length; i++) {
                final DataColumnSpecCreator dataColumnSpecCreator =
                    new DataColumnSpecCreator(config.getString("colname" + i), config.getDataType("type" + i));
                colSpecs[i] = dataColumnSpecCreator.createSpec();
            }
            return new DataTableSpec[]{new DataTableSpec(colSpecs)};
        } else {
            return configure(new DataTableSpec[]{(DataTableSpec)inSpecs[0]});
        }
    }

    /** {@inheritDoc} */
    @Override
    public InputPortRole[] getInputPortRoles() {
        return new InputPortRole[]{InputPortRole.DISTRIBUTED_STREAMABLE};
    }

    /** {@inheritDoc} */
    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.DISTRIBUTED};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {

        return new StreamableOperator() {
            private SimpleStreamableOperatorInternals m_internals = new SimpleStreamableOperatorInternals();

            /**
             * {@inheritDoc}
             */
            @Override
            public void runIntermediate(final PortInput[] inputs, final ExecutionContext exec) throws Exception {
                final RowInput rowInput = (RowInput)inputs[0];
                final DataRow row = rowInput.poll();
                if (row != null) {
                    final DataTableSpec inSpec = rowInput.getDataTableSpec();
                    final int[] includeIndexes = Arrays.stream(m_colSelect.applyTo(inSpec).getIncludes())
                        .mapToInt(s -> inSpec.findColumnIndex(s)).toArray();
                    final Config config = m_internals.getConfig();

                    // detect types
                    detectTypes(rowInput);
                    for (int i = 0; i < m_detectedTypes.length; i++) {
                        config.addDataType("detected_type" + i, m_detectedTypes[i]);
                    }

                    // write detected types and column names into config
                    if (m_isReplaceOrAppend.getStringValue().equals(OPTION_REPLACE)) {
                        for (int i = 0; i < row.getNumCells(); i++) {
                            final int searchIdx = Arrays.binarySearch(includeIndexes, i);
                            config.addString("colname" + i, inSpec.getColumnNames()[i]);
                            if (searchIdx < 0) {
                                config.addDataType("type" + i, inSpec.getColumnSpec(i).getType());
                            } else {
                                config.addDataType("type" + i,
                                    m_detectedTypes[searchIdx] != null ? m_detectedTypes[searchIdx] : null);
                            }
                        }
                        config.addInt("sizeRow", row.getNumCells());
                    } else {
                        for (int i = 0; i < inSpec.getNumColumns(); i++) {
                            config.addString("colname" + i, inSpec.getColumnNames()[i]);
                            config.addDataType("type" + i, inSpec.getColumnSpec(i).getType());
                        }
                        for (int i = 0; i < m_detectedTypes.length; i++) {
                            config.addString("colname" + (i + inSpec.getNumColumns()),
                                new UniqueNameGenerator(inSpec).newName(
                                    inSpec.getColumnSpec(includeIndexes[i]).getName() + m_suffix.getStringValue()));
                            config.addDataType("type" + (i + inSpec.getNumColumns()), m_detectedTypes[i]);
                        }
                        config.addInt("sizeRow", inSpec.getNumColumns() + m_detectedTypes.length);
                    }
                    config.addBoolean("needsIteration", false);
                } else {
                    m_internals.getConfig().addInt("sizeRow", 0);
                }
                rowInput.close();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public StreamableOperatorInternals saveInternals() {
                return m_internals;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void loadInternals(final StreamableOperatorInternals internals) {
                m_internals = (SimpleStreamableOperatorInternals)internals;
            }

            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
                final RowInput in = (RowInput)inputs[0];
                final RowOutput out = (RowOutput)outputs[0];
                final DataTableSpec inSpec = in.getDataTableSpec();
                final int[] includeIndexes = Arrays.stream(m_colSelect.applyTo(inSpec).getIncludes())
                    .mapToInt(s -> inSpec.findColumnIndex(s)).toArray();

                // read detected types from config
                final DataType[] detectedTypes = new DataType[includeIndexes.length];
                final Config config = m_internals.getConfig();
                for (int i = 0; i < includeIndexes.length; i++) {
                    detectedTypes[i] = config.getDataType("detected_type" + i, null);
                }

                // compute every row
                DataRow row;
                while ((row = in.poll()) != null) {
                    exec.checkCanceled();
                    DataCell[] datacells = new DataCell[includeIndexes.length];
                    for (int i = 0; i < includeIndexes.length; i++) {
                        if (m_isReplaceOrAppend.getStringValue().equals(OPTION_REPLACE)) {
                            final StringToDurationPeriodCellFactory cellFac = new StringToDurationPeriodCellFactory(
                                new DataColumnSpecCreator(inSpec.getColumnNames()[includeIndexes[i]], detectedTypes[i])
                                    .createSpec(),
                                includeIndexes[i]);
                            datacells[i] = cellFac.getCells(row)[0];
                        } else {
                            final DataColumnSpec dataColSpec = new UniqueNameGenerator(inSpec).newColumn(
                                inSpec.getColumnNames()[includeIndexes[i]] + m_suffix.getStringValue(),
                                detectedTypes[i]);
                            final StringToDurationPeriodCellFactory cellFac =
                                new StringToDurationPeriodCellFactory(dataColSpec, includeIndexes[i]);
                            datacells[i] = cellFac.getCells(row)[0];
                        }
                    }
                    if (m_isReplaceOrAppend.getStringValue().equals(OPTION_REPLACE)) {
                        out.push(new ReplacedColumnsDataRow(row, datacells, includeIndexes));
                    } else {
                        out.push(new AppendedColumnRow(row, datacells));
                    }
                }

                in.close();
                out.close();
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do
    }
}
