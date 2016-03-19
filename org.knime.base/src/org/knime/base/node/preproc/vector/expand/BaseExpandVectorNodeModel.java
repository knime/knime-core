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
 *   Mar 19, 2016 (Berthold): created
 */
package org.knime.base.node.preproc.vector.expand;

import java.util.Arrays;
import java.util.Vector;
import java.util.stream.IntStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.vector.doublevector.DoubleVectorCellFactory;
import org.knime.core.data.vector.doublevector.DoubleVectorValue;
import org.knime.core.data.vector.stringvector.StringVectorCellFactory;
import org.knime.core.data.vector.stringvector.StringVectorValue;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortType;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.streamable.StreamableOperatorInternals;
import org.knime.core.node.streamable.simple.SimpleStreamableOperatorInternals;

/**
 * Abstract base model implementation for a node which extracts a given subset of elements of
 * a string or double vector to individual string/double columns. Derived classes either
 * generate their own random sampling scheme or read it from a predecessor.
 *
 * @author M. Berthold
 * @since 3.2
 */
public abstract class BaseExpandVectorNodeModel extends NodeModel {

    // members holding the sampling scheme:
    protected int[] m_sampledIndices = null;
    protected enum VType { String, Double };
    protected VType m_vectorType = null;
    // other info set during configuration:
    protected int m_sourceColumnIndex = -1;

    /* static factory methods for the SettingsModels used here and in the NodeDialog. */
    /**
     * @return the settings model used to store the source column name.
     */
    static public SettingsModelString createVectorColSelectSettingsModel() {
        return new SettingsModelString("SelectedColumn", null);
    }
    protected final SettingsModelString m_vectorColumn = createVectorColSelectSettingsModel();

    static public SettingsModelBoolean createRemoveSourceColSettingModel() {
        return new SettingsModelBoolean("Remove Source", true);
    }
    protected final SettingsModelBoolean m_removeSourceCol = createRemoveSourceColSettingModel();

    static public SettingsModelBoolean createExpandColumnsSettingModel() {
        return new SettingsModelBoolean("ExpandToColumns", false);
    }
    protected final SettingsModelBoolean m_expandToColumns = createExpandColumnsSettingModel();

    /**
     * @param inPortTypes
     * @param outPortTypes
     */
    protected BaseExpandVectorNodeModel(final PortType[] inPortTypes, final PortType[] outPortTypes) {
        super(inPortTypes, outPortTypes);
    }

    /* Check settings of this base class.
     */
    protected void checkBaseSettings(final DataTableSpec spec) throws InvalidSettingsException {
        assert m_vectorColumn.getStringValue() != null;
        // selected column should exist in input table
        if (!spec.containsName(m_vectorColumn.getStringValue())) {
            throw new InvalidSettingsException("Selected column '"
                    + m_vectorColumn.getStringValue() + "' does not exist in input table!");
        }
        m_sourceColumnIndex = spec.findColumnIndex(m_vectorColumn.getStringValue());
        // and it should be of type double or string vector
        if (spec.getColumnSpec(m_vectorColumn.getStringValue()).getType().isCompatible(DoubleVectorValue.class)) {
            m_vectorType = VType.Double;
        } else if (spec.getColumnSpec(m_vectorColumn.getStringValue()).getType().
                isCompatible(DoubleVectorValue.class)) {
            m_vectorType = VType.String;
        } else {
            throw new InvalidSettingsException("Selected column '"
                    + m_vectorColumn.getStringValue() + "' does not contain double or string vectors!");
        }
    }

    /**
     * Creates the TableSpec for the first outport, holding the data with the sampled/expanded vector.
     *
     * @param inTableSpec the spec of the source table
     * @return the new spec
     */
    protected DataTableSpec createFirstSpec(final DataTableSpec inSpec) throws InvalidSettingsException {
        DataColumnSpec sourceSpec = inSpec.getColumnSpec(m_sourceColumnIndex);
        DataTableSpecCreator dtsc = new DataTableSpecCreator();
        dtsc.setName(inSpec.getName()+ " (sampled)");
        // copy original column specs
        int ix = 0;
        for (DataColumnSpec dcs : inSpec) {
            if (m_removeSourceCol.getBooleanValue() && (ix == m_sourceColumnIndex)) {
                // drop source column (if selected)
            } else {
                dtsc.addColumns(dcs);
            }
            ix++;
        }
        // add new columns
        DataColumnSpec[] newSpecs;
        if (m_expandToColumns.getBooleanValue()) {
            newSpecs = IntStream.range(0, m_sampledIndices.length).mapToObj(
                i -> new DataColumnSpecCreator(sourceSpec.getElementNames().get(m_sampledIndices[i]),
                                               m_vectorType.equals(VType.Double) ? DoubleCell.TYPE : StringCell.TYPE
                                                   ).createSpec()).toArray(DataColumnSpec[]::new);
        } else {
            newSpecs = new DataColumnSpec[1];
            newSpecs[0] = new DataColumnSpecCreator(sourceSpec.getName() + " (sampled)",
                m_vectorType.equals(VType.Double) ? DoubleVectorCellFactory.TYPE : StringVectorCellFactory.TYPE
                ).createSpec();
        }
        dtsc.addColumns(newSpecs);
        return dtsc.createSpec();
    }

    /*
     * The main work is done here - execute processing in streaming mode.
     *
     * @param rows total number of rows. Can be -1 if not available.
     */
    protected void executeStreaming(final RowInput in, final RowOutput out, final long rows,
        final ExecutionContext exec) throws InterruptedException, CanceledExecutionException {
        try {
            long rowIdx = -1;
            DataRow row;
            while ((row = in.poll()) != null) {
                rowIdx++;
                if (rows > 0) {
                    exec.setProgress(rowIdx / (double)rows, "Adding row " + rowIdx + " of " + rows);
                } else {
                    exec.setProgress("Adding row " + rowIdx + ".");
                }
                exec.checkCanceled();
                out.push(computeNewRow(row));
            }
        } finally {
            out.close();
        }
    }

    /**
     * Compute new output row given an input.
     *
     * @param inRow
     * @return corresponding output
     */
    protected DataRow computeNewRow(final DataRow inRow) {
        Vector<DataCell> outCells = new Vector<DataCell>();
        // copy original cells
        int ix = 0;
        for (DataCell dc : inRow) {
            if (m_removeSourceCol.getBooleanValue() && (ix == m_sourceColumnIndex)) {
                // drop cell in source column (if selected)
            } else {
                outCells.add(dc);
            }
            ix++;
        }
        // add new cells
        DataCell sourceCell = inRow.getCell(m_sourceColumnIndex);
        DataCell[] res;
        if ((m_vectorType.equals(VType.Double) && sourceCell instanceof DoubleVectorValue)
                || (m_vectorType.equals(VType.String) && sourceCell instanceof StringVectorValue)) {
            if (m_expandToColumns.getBooleanValue()) {
                res = new DataCell[m_sampledIndices.length];
                for (int i = 0; i < m_sampledIndices.length; i++) {
                    res[i] = m_vectorType.equals(VType.Double) ?
                          new DoubleCell(((DoubleVectorValue)sourceCell).getValue(m_sampledIndices[i]))
                        : new StringCell(((StringVectorValue)sourceCell).getValue(m_sampledIndices[i]));
                }
            } else {
                res = new DataCell[1];
                if (m_vectorType.equals(VType.Double)) {
                    double[] d = new double[m_sampledIndices.length];
                    for (int i = 0; i < m_sampledIndices.length; i++) {
                        d[i] = ((DoubleVectorValue)sourceCell).getValue(m_sampledIndices[i]);
                    }
                    res[0] = DoubleVectorCellFactory.createCell(d);
                } else {
                    String[] s = new String[m_sampledIndices.length];
                    for (int i = 0; i < m_sampledIndices.length; i++) {
                        s[i] = ((StringVectorValue)sourceCell).getValue(m_sampledIndices[i]);
                    }
                    res[0] = StringVectorCellFactory.createCell(s);
                }
            }
        } else {
            res = new MissingCell[m_expandToColumns.getBooleanValue() ? m_sampledIndices.length : 1];
            Arrays.fill(res, DataType.getMissingCell());
        }
        outCells.addAll(Arrays.asList(res));
        return new DefaultRow(inRow.getKey(), outCells);
    }

    protected abstract class BaseStreamableOperator extends StreamableOperator {
        @Override
        public void loadInternals(final StreamableOperatorInternals internals) {
            SimpleStreamableOperatorInternals soi = (SimpleStreamableOperatorInternals)internals;
            try {
                m_expandToColumns.setBooleanValue(soi.getConfig().getBoolean("Expand"));
                m_removeSourceCol.setBooleanValue(soi.getConfig().getBoolean("RemoveSource"));
                m_sampledIndices = soi.getConfig().getIntArray("Indices");
            } catch (InvalidSettingsException ise) {

            }
        }
        @Override
        public StreamableOperatorInternals saveInternals() {
            return super.saveInternals();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamableOperatorInternals createInitialStreamableOperatorInternals() {
        SimpleStreamableOperatorInternals soi = new SimpleStreamableOperatorInternals();
        soi.getConfig().addBoolean("Expand", m_expandToColumns.getBooleanValue());
        soi.getConfig().addBoolean("RemoveSource", m_removeSourceCol.getBooleanValue());
        soi.getConfig().addIntArray("Indices", m_sampledIndices);
        return soi;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_vectorColumn.saveSettingsTo(settings);
        m_removeSourceCol.saveSettingsTo(settings);
        m_expandToColumns.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_vectorColumn.validateSettings(settings);
        m_removeSourceCol.validateSettings(settings);
        m_expandToColumns.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_vectorColumn.loadSettingsFrom(settings);
        m_removeSourceCol.loadSettingsFrom(settings);
        m_expandToColumns.loadSettingsFrom(settings);
    }
}
