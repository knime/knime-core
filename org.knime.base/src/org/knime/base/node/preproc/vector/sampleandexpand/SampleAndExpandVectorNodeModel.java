/*
 * ------------------------------------------------------------------------
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
 * Created on 2014.03.20. by gabor
 */
package org.knime.base.node.preproc.vector.sampleandexpand;


import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.stream.IntStream;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.knime.base.node.preproc.vector.expand.BaseExpandVectorNodeModel;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.vector.doublevector.DoubleVectorCellFactory;
import org.knime.core.data.vector.doublevector.DoubleVectorValue;
import org.knime.core.data.vector.stringvector.StringVectorCellFactory;
import org.knime.core.data.vector.stringvector.StringVectorValue;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.streamable.BufferedDataTableRowOutput;
import org.knime.core.node.streamable.DataTableRowInput;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortObjectOutput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.streamable.StreamableOperatorInternals;
import org.knime.core.node.streamable.simple.SimpleStreamableOperatorInternals;

/**
 * This is the model implementation for a node which samples and optionally expands a string/double vector
 * to individual string/double columns.
 *
 * @author M. Berthold
 * @since 3.2
 */
public class SampleAndExpandVectorNodeModel extends BaseExpandVectorNodeModel {

    private String[] m_sampledNames = null;


    static SettingsModelInteger createNrColumnsSettingsModel() {
        return new SettingsModelInteger("NrSampledColumns", 100);
    }
    private final SettingsModelInteger m_nrSampledCols = createNrColumnsSettingsModel();

    static SettingsModelBoolean createStaticSeedSettingsModel() {
        return new SettingsModelBoolean("Static Seed", true);
    }
    private final SettingsModelBoolean m_staticRandomSeed = createStaticSeedSettingsModel();

    static SettingsModelInteger createRandomSeedSettingsModel() {
        return new SettingsModelInteger("Random Seed", 43);
    }
    private final SettingsModelInteger m_randomSeed = createRandomSeedSettingsModel();

    /**
     * Initialize model. One Data Inport, two Data Outports.
     */
    protected SampleAndExpandVectorNodeModel() {
        super(new PortType[] {BufferedDataTable.TYPE},
            new PortType[] {BufferedDataTable.TYPE, BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputPortRole[] getInputPortRoles() {
        return new InputPortRole[]{InputPortRole.DISTRIBUTED_STREAMABLE};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.DISTRIBUTED, OutputPortRole.NONDISTRIBUTED};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        // user settings must be set and valid
        super.checkBaseSettings(inSpecs[0]);
        // checks passed - create the sampling scheme here so we know the column names that
        // execute will produce. After each reset we will shuffle again.
        if (m_sampledIndices == null) {
            generateSamplingSchema(
                inSpecs[0].getColumnSpec(m_sourceColumnIndex).getElementNames(), m_nrSampledCols.getIntValue());
        }
        return new DataTableSpec[]{createFirstSpec(inSpecs[0]), createdSecondSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new StreamableOperator() {
            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec) throws Exception {
                executeStreaming((RowInput)inputs[0], (RowOutput)outputs[0], -1, exec);
                // FIXME: the following should work but results in a NPE in distributed execution:
                ((PortObjectOutput)outputs[1]).setPortObject(null);
            }
            @Override
            public void loadInternals(final StreamableOperatorInternals internals) {
                SimpleStreamableOperatorInternals soi = (SimpleStreamableOperatorInternals)internals;
                try {
                    m_expandToColumns.setBooleanValue(soi.getConfig().getBoolean("Expand"));
                    m_removeSourceCol.setBooleanValue(soi.getConfig().getBoolean("RemoveSource"));
                    m_sampledIndices = soi.getConfig().getIntArray("Indices");
                    m_sampledNames = soi.getConfig().getStringArray("Names");
                } catch (InvalidSettingsException ise) {

                }
            }
            @Override
            public StreamableOperatorInternals saveInternals() {
                return super.saveInternals();
            }
        };
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
        soi.getConfig().addStringArray("Names", m_sampledNames);
        return soi;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finishStreamableExecution(final StreamableOperatorInternals internals, final ExecutionContext exec,
        final PortOutput[] output) throws Exception {
        ((RowOutput)output[1]).setFully(createIndexTable(exec));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec) throws Exception {
        BufferedDataContainer out1 = exec.createDataContainer(createFirstSpec(inData[0].getDataTableSpec()));
        RowOutput rowOutput = new BufferedDataTableRowOutput(out1);
        RowInput rowInput = new DataTableRowInput(inData[0]);
        this.executeStreaming(rowInput, rowOutput, inData[0].size(), exec);
        BufferedDataTable out2 = createIndexTable(exec);
        return new BufferedDataTable[]{out1.getTable(), out2};
    }

    /*
     * The main work is done here - execute processing in streaming mode.
     *
     * @param rows total number of rows. Can be -1 if not available.
     */
    private void executeStreaming(final RowInput in, final RowOutput out, final long rows,
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
            newSpecs = IntStream.range(0, m_nrSampledCols.getIntValue()).mapToObj(
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
                res = new DataCell[m_nrSampledCols.getIntValue()];
                for (int i = 0; i < m_nrSampledCols.getIntValue(); i++) {
                    res[i] = m_vectorType.equals(VType.Double) ?
                          new DoubleCell(((DoubleVectorValue)sourceCell).getValue(m_sampledIndices[i]))
                        : new StringCell(((StringVectorValue)sourceCell).getValue(m_sampledIndices[i]));
                }
            } else {
                res = new DataCell[1];
                if (m_vectorType.equals(VType.Double)) {
                    double[] d = new double[m_nrSampledCols.getIntValue()];
                    for (int i = 0; i < m_nrSampledCols.getIntValue(); i++) {
                        d[i] = ((DoubleVectorValue)sourceCell).getValue(m_sampledIndices[i]);
                    }
                    res[0] = DoubleVectorCellFactory.createCell(d);
                } else {
                    String[] s = new String[m_nrSampledCols.getIntValue()];
                    for (int i = 0; i < m_nrSampledCols.getIntValue(); i++) {
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

    /**
     * Creates the TableSpec for the second outport, holding the list of sampled columns.
     *
     * @return the new spec
     */
    private DataTableSpec createdSecondSpec() {
        DataTableSpecCreator dtsc = new DataTableSpecCreator();
        dtsc.setName("Used Columns");
        dtsc.addColumns(new DataColumnSpecCreator("Index", IntCell.TYPE).createSpec());
        dtsc.addColumns(new DataColumnSpecCreator("Column Name", StringCell.TYPE).createSpec());
        return dtsc.createSpec();
    }

    /**
     * Create second table holding the indeces of the selected cells.
     *
     * @param inSpec
     * @param exec
     * @return table
     */
    private BufferedDataTable createIndexTable(final ExecutionContext exec) {
        // create second output
        BufferedDataContainer out2 = exec.createDataContainer(createdSecondSpec());
        for (int i = 0; i < m_sampledIndices.length; i++) {
            int ix = m_sampledIndices[i];
            String colName = m_sampledNames[i];
            out2.addRowToTable(
                new DefaultRow(RowKey.createRowKey((long)i), new IntCell(ix), new StringCell(colName)));
        }
        out2.close();
        return out2.getTable();
    }

    /**
     * Draw (without replacement) a given number of elements from the list of column names.
     *
     * @param names
     * @param nrSamples
     */
    private void generateSamplingSchema(final List<String> names, final int nrSamples) {
        RandomDataGenerator r = new RandomDataGenerator();
        if (m_staticRandomSeed.getBooleanValue()) {
            r.reSeed(m_randomSeed.getIntValue());
        }
        m_sampledIndices = r.nextPermutation(names.size(), nrSamples);
        Arrays.sort(m_sampledIndices);
        m_sampledNames = new String[m_sampledIndices.length];
        for (int i = 0; i < m_sampledIndices.length; i++) {
            m_sampledNames[i] = names.get(m_sampledIndices[i]);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // delete previous random sampling scheme so that a new one is selected upon the next configure.
        m_sampledIndices = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        m_nrSampledCols.saveSettingsTo(settings);
        m_staticRandomSeed.saveSettingsTo(settings);
        m_randomSeed.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(settings);
        m_nrSampledCols.validateSettings(settings);
        m_staticRandomSeed.validateSettings(settings);
        m_randomSeed.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        m_nrSampledCols.loadSettingsFrom(settings);
        m_staticRandomSeed.loadSettingsFrom(settings);
        m_randomSeed.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // Nothing to do.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // Nothing to do.
    }
}
