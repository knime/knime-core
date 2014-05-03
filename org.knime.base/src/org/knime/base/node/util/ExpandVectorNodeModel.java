/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by
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
 * Created on 2014.03.20. by gabor
 */
package org.knime.base.node.util;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
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
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
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
    private static final String CFGKEY_USE_INDICES = "use indices from properties";
    private static final boolean DEFAULT_USE_INDICES = true;
    private final SettingsModelColumnName m_inputColumn = createInputColumn();
    private final SettingsModelBoolean m_removeOriginal = createRemoveOriginal();
    private final SettingsModelString m_outputPrefix;
    private final SettingsModelInteger m_startIndex = createStartIndex();
    private final SettingsModelInteger m_maxNewColumns = createMaxNewColumns();
    private final SettingsModelBoolean m_useNames = createUseNames();
    private final SettingsModelBoolean m_useIndices = createUseIndices();

    /**
     * @return The input column name model.
     */
    public static SettingsModelColumnName createInputColumn() {
        return new SettingsModelColumnName(CFGKEY_INPUT_COLUMN, DEFAULT_INPUT_COLUMN);
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
     * @return Whether to use the indices from the spec or not model.
     */
    public static SettingsModelBoolean createUseIndices() {
        return new SettingsModelBoolean(CFGKEY_USE_INDICES, DEFAULT_USE_INDICES);
    }

    /**
     * The constructor primarily intended to extend.
     * @param outputPrefix The output prefix {@link SettingsModelString}.
     */
    protected ExpandVectorNodeModel(final SettingsModelString outputPrefix) {
        super(1, 1);
        m_outputPrefix = outputPrefix;
    }

    /**
     * @param nrInDataPorts
     * @param nrOutDataPorts
     * @param outputPrefix The output prefix {@link SettingsModelString}.
     */
    protected ExpandVectorNodeModel(final int nrInDataPorts, final int nrOutDataPorts, final SettingsModelString outputPrefix) {
        super(nrInDataPorts, nrOutDataPorts);
        m_outputPrefix = outputPrefix;
    }

    /**
     * @param inPortTypes
     * @param outPortTypes
     * @param outputPrefix The output prefix {@link SettingsModelString}.
     */
    protected ExpandVectorNodeModel(final PortType[] inPortTypes, final PortType[] outPortTypes, final SettingsModelString outputPrefix) {
        super(inPortTypes, outPortTypes);
        m_outputPrefix = outputPrefix;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec) throws Exception {
        return new BufferedDataTable[]{exec.createColumnRearrangeTable(inData[0], createRearranger(inData[0], exec),
            exec.createSubProgress(.75))};
    }

    /**
     * @param spec
     * @return
     * @throws CanceledExecutionException
     */
    private ColumnRearranger createRearranger(final BufferedDataTable table, final ExecutionMonitor exec) throws CanceledExecutionException {
        DataTableSpec spec = table.getSpec();
        final ColumnRearranger ret = new ColumnRearranger(spec);
        ExecutionMonitor subRead = exec.createSubProgress(.25);
        int maxOutput = 0;
        final int colIndex = spec.findColumnIndex(m_inputColumn.getColumnName());
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
        final DataColumnSpec inputSpec = spec.getColumnSpec(m_inputColumn.getColumnName());
        final int namedColumns;
        if (useIndices()) {
            int[] indices = SourceColumnsAsProperties.indicesFrom(inputSpec);
            if (useNames()) {
                String[] origNames = SourceColumnsAsProperties.sortNamesAccordingToIndex(inputSpec.getElementNames(), indices).toArray(new String[0]);
                System.arraycopy(origNames, 0, colNames, 0, Math.min(maxOutput, origNames.length));
                namedColumns = origNames.length;
            } else {
                namedColumns = 0;
            }
        } else if (useNames()) {
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
        final int inputIndex = spec.findColumnIndex(m_inputColumn.getColumnName());
        ret.append(createCellFactory(colNames, outputColumns, inputIndex));
        if (m_removeOriginal.getBooleanValue()) {
            ret.remove(m_inputColumn.getColumnName());
        }
        if (useIndices()) {
            int[] indices = SourceColumnsAsProperties.indicesFrom(inputSpec);
            int splitPoint =
                useNames() ? Math.min(indices.length, inputSpec.getElementNames().size()) : 0;
            //We used the lower set of indices sorted, so we have to sort them separately.
            int[] lower = new int[splitPoint], higher = new int[indices.length - splitPoint];
            System.arraycopy(indices, 0, lower, 0, splitPoint);
            Arrays.sort(lower);
            //Important to go backwards
            for (int i = 0; i < Math.min(lower.length, outputColumns.length); ++i) {
                ret.move(spec.getNumColumns() - (m_removeOriginal.getBooleanValue() ? 1 : 0) + i, lower[i]);
            }
            System.arraycopy(indices, splitPoint, higher, 0, higher.length);
            Arrays.sort(higher);
            //Important to go backwards
            for (int i = 0; i < Math.min(higher.length - lower.length, outputColumns.length); ++i) {
                ret.move(spec.getNumColumns() - (m_removeOriginal.getBooleanValue() ? 1 : 0) + lower.length + i,
                    higher[i]);
            }
        }
        return ret;
    }

    /**
     * @return The {@link #m_useIndices} settings is enabled and should use it.
     */
    private boolean useIndices() {
        return m_useIndices.isEnabled() && m_useIndices.getBooleanValue();
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
        m_useIndices.saveSettingsTo(settings);
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
        m_useIndices.loadSettingsFrom(settings);
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
        m_useIndices.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
        // Nothing to do, no internal state.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
        // Nothing to do, no internal state.
    }

    /**
     * @return the inputColumn
     */
    protected SettingsModelColumnName getInputColumn() {
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
     * @return the useIndices
     */
    protected SettingsModelBoolean getUseIndices() {
        return m_useIndices;
    }

    /**
     * @return the useNames
     */
    protected SettingsModelBoolean getUseNames() {
        return m_useNames;
    }
}