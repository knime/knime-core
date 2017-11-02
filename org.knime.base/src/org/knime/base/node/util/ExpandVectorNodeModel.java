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
 * Created on 2014.03.20. by gabor
 */
package org.knime.base.node.util;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortType;

/**
 * Base class for vector expander nodes.
 *
 * @author Gabor Bakos
 * @since 2.10
 */
public abstract class ExpandVectorNodeModel extends NodeModel {

    private static final String CFGKEY_INPUT_COLUMN = "input column";

    private static final String DEFAULT_INPUT_COLUMN = "ByteVector";

    private static final String CFGKEY_REMOVE_ORIGINAL = "remove original column?";

    private static final boolean DEFAULT_REMOVE_ORIGINAL = true;

    /** Configuration key for the output column prefixes. */
    protected static final String CFGKEY_OUTPUT_PREFIX = "output prefix";

    private static final String CFGKEY_START_INDEX = "start index";

    private static final int DEFAULT_START_INDEX = 0;

    private static final String CFGKEY_MAX_NEW_COLUMNS = "maximum number of new columns";

    private static final int DEFAULT_MAX_NEW_COLUMNS = 40000;

    private static final String CFGKEY_USE_NAMES = "use names from properties";

    private static final boolean DEFAULT_USE_NAMES = true;

    private final SettingsModelString m_inputColumn = createInputColumn();

    private final SettingsModelBoolean m_removeOriginal = createRemoveOriginal();

    private final SettingsModelString m_outputPrefix;

    private final SettingsModelInteger m_startIndex = createStartIndex();

    private final SettingsModelInteger m_maxNewColumns = createMaxNewColumns();

    private final SettingsModelBoolean m_useNames = createUseNames();

    private final Class<? extends DataValue> m_valueType;

    /**
     * @return The input column name model.
     */
    public static SettingsModelString createInputColumn() {
        return new SettingsModelString(CFGKEY_INPUT_COLUMN, DEFAULT_INPUT_COLUMN);
    }

    /**
     * @return Whether to remove original (input) column model.
     */
    public static SettingsModelBoolean createRemoveOriginal() {
        return new SettingsModelBoolean(CFGKEY_REMOVE_ORIGINAL, DEFAULT_REMOVE_ORIGINAL);
    }

    /**
     * @return The start index for the generated column names model.
     */
    public static SettingsModelInteger createStartIndex() {
        return new SettingsModelInteger(CFGKEY_START_INDEX, DEFAULT_START_INDEX);
    }

    /**
     * @return The maximal number of new columns model.
     */
    public static SettingsModelIntegerBounded createMaxNewColumns() {
        return new SettingsModelIntegerBounded(CFGKEY_MAX_NEW_COLUMNS, DEFAULT_MAX_NEW_COLUMNS, 0, Integer.MAX_VALUE);
    }

    /**
     * @return Whether use the names from the spec, or not model.
     */
    public static SettingsModelBoolean createUseNames() {
        return new SettingsModelBoolean(CFGKEY_USE_NAMES, DEFAULT_USE_NAMES);
    }

    /**
     * The constructor primarily intended to extend.
     *
     * @param outputPrefix The output prefix {@link SettingsModelString}.
     * @param expandedValueType Class of the {@link DataValue} to expand.
     */
    protected ExpandVectorNodeModel(final SettingsModelString outputPrefix,
        final Class<? extends DataValue> expandedValueType) {
        super(1, 1);
        m_outputPrefix = outputPrefix;
        this.m_valueType = expandedValueType;
    }

    /**
     * @param nrInDataPorts number of input data ports.
     * @param nrOutDataPorts number of output data ports.
     * @param outputPrefix The output prefix {@link SettingsModelString}.
     * @param expandedValueType Class of the {@link DataValue} to expand.
     */
    protected ExpandVectorNodeModel(final int nrInDataPorts, final int nrOutDataPorts,
        final SettingsModelString outputPrefix, final Class<? extends DataValue> expandedValueType) {
        super(nrInDataPorts, nrOutDataPorts);
        m_outputPrefix = outputPrefix;
        this.m_valueType = expandedValueType;
    }

    /**
     * @param inPortTypes input port types.
     * @param outPortTypes output port types.
     * @param outputPrefix The output prefix {@link SettingsModelString}.
     * @param expandedValueType Class of the {@link DataValue} to expand.
     */
    protected ExpandVectorNodeModel(final PortType[] inPortTypes, final PortType[] outPortTypes,
        final SettingsModelString outputPrefix, final Class<? extends DataValue> expandedValueType) {
        super(inPortTypes, outPortTypes);
        m_outputPrefix = outputPrefix;
        this.m_valueType = expandedValueType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        return new BufferedDataTable[]{exec.createColumnRearrangeTable(inData[0], createRearranger(inData[0], exec),
            exec.createSubProgress(.75))};
    }

    /**
     * Creates the {@link ColumnRearranger} to perform the computation.
     *
     * @param table The input table.
     * @param exec An {@link ExecutionMonitor}.
     * @return The {@link ColumnRearranger}.
     * @throws CanceledExecutionException Execution cancelled.
     */
    private ColumnRearranger createRearranger(final BufferedDataTable table, final ExecutionMonitor exec)
        throws CanceledExecutionException {
        DataTableSpec spec = table.getSpec();
        final ColumnRearranger ret = new ColumnRearranger(spec);
        ExecutionMonitor subRead = exec.createSubProgress(.25);
        int maxOutput = 0;
        String inputColumnName = findInputColumnName(spec);
        final int colIndex = spec.findColumnIndex(inputColumnName);
        for (DataRow dataRow : table) {
            subRead.checkCanceled();
            DataCell cell = dataRow.getCell(colIndex);
            long length = getCellLength(cell);
            if (length > m_maxNewColumns.getIntValue()) {
                length = m_maxNewColumns.getIntValue();
            }
            maxOutput = Math.max(maxOutput, (int)length);
        }
        final String[] colNames = new String[maxOutput];
        final DataColumnSpec inputSpec = spec.getColumnSpec(inputColumnName);
        final int namedColumns;
        if (useNames()) {
            String[] origNames = inputSpec.getElementNames().toArray(new String[0]);
            System.arraycopy(origNames, 0, colNames, 0, Math.min(maxOutput, origNames.length));
            namedColumns = origNames.length;
        } else {
            namedColumns = 0;
        }
        for (int i = namedColumns; i < maxOutput; ++i) {
            colNames[i] = m_outputPrefix.getStringValue() + (i - namedColumns + m_startIndex.getIntValue());
        }
        DataColumnSpec[] outputColumns = new DataColumnSpec[maxOutput];
        DataColumnSpecCreator dataColumnSpecCreator = new DataColumnSpecCreator("Dummy", IntCell.TYPE);
        for (int idx = 0; idx < colNames.length; ++idx) {
            dataColumnSpecCreator.setName(DataTableSpec.getUniqueColumnName(spec, colNames[idx]));
            outputColumns[idx] = dataColumnSpecCreator.createSpec();
        }
        ret.append(createCellFactory(colNames, outputColumns, colIndex));
        if (m_removeOriginal.getBooleanValue()) {
            ret.remove(inputColumnName);
        }
        return ret;
    }

    /**
     * Guesses the input column name if it was not specified. It will find the last compatible to the class of the
     * expected {@link DataValue}.
     *
     * @param spec The input {@link DataTableSpec}.
     * @return The name of the last column compatible with the specified {@link DataValue}.
     */
    private String findInputColumnName(final DataTableSpec spec) {
        String inputColumnName = m_inputColumn.getStringValue();
        if (spec.findColumnIndex(inputColumnName) < 0) {
            for (DataColumnSpec colSpec : spec) {
                if (colSpec.getType().isCompatible(m_valueType)) {
                    inputColumnName = colSpec.getName();
                }
            }
        }
        return inputColumnName;
    }

    /**
     * @return The {@link #m_useNames} settings is enabled and should use it.
     */
    private boolean useNames() {
        return m_useNames.isEnabled() && m_useNames.getBooleanValue();
    }

    /**
     * Used in {@link ExpandVectorNodeModel} only to find the max length.
     *
     * @param cell A {@link DataCell}.
     * @return The cell's length.
     */
    protected abstract long getCellLength(DataCell cell);

    /**
     * @param colNames
     * @param outputColumns
     * @param inputIndex
     * @return The {@link CellFactory} creating the new values.
     */
    protected abstract CellFactory createCellFactory(String[] colNames, DataColumnSpec[] outputColumns, int inputIndex);

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // Nothing to do, no internal state.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        String inputColumnName = m_inputColumn.getStringValue();
        if (inSpecs[0].findColumnIndex(inputColumnName) < 0) {
            if (!inSpecs[0].containsCompatibleType(m_valueType)) {
                throw new InvalidSettingsException("No compatible column found!");
            }
        }

        // We do not know how many columns will be created.
        //Maybe if we have the domain computed, can guess it?
        return new DataTableSpec[]{null};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_inputColumn.saveSettingsTo(settings);
        m_removeOriginal.saveSettingsTo(settings);
        m_outputPrefix.saveSettingsTo(settings);
        m_startIndex.saveSettingsTo(settings);
        m_maxNewColumns.saveSettingsTo(settings);
        m_useNames.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_inputColumn.loadSettingsFrom(settings);
        m_removeOriginal.loadSettingsFrom(settings);
        m_outputPrefix.loadSettingsFrom(settings);
        m_startIndex.loadSettingsFrom(settings);
        m_maxNewColumns.loadSettingsFrom(settings);
        m_useNames.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_inputColumn.validateSettings(settings);
        m_removeOriginal.validateSettings(settings);
        m_outputPrefix.validateSettings(settings);
        m_startIndex.validateSettings(settings);
        m_maxNewColumns.validateSettings(settings);
        m_useNames.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // Nothing to do, no internal state.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // Nothing to do, no internal state.
    }

    /**
     * @return the inputColumn
     */
    protected SettingsModelString getInputColumn() {
        return m_inputColumn;
    }

    /**
     * @return the maxNewColumns
     */
    protected SettingsModelInteger getMaxNewColumns() {
        return m_maxNewColumns;
    }

    /**
     * @return the outputPrefix
     */
    protected SettingsModelString getOutputPrefix() {
        return m_outputPrefix;
    }

    /**
     * @return the removeOriginal
     */
    protected SettingsModelBoolean getRemoveOriginal() {
        return m_removeOriginal;
    }

    /**
     * @return the m_startIndex
     */
    protected SettingsModelInteger getStartIndex() {
        return m_startIndex;
    }

    /**
     * @return the useNames
     */
    protected SettingsModelBoolean getUseNames() {
        return m_useNames;
    }
}
