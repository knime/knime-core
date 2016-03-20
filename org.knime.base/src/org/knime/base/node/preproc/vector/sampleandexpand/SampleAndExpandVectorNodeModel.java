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

import org.apache.commons.math3.random.RandomDataGenerator;
import org.knime.base.node.preproc.vector.expand.BaseExpandVectorNodeModel;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
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
        if (getSampledIndices() == null) {
            generateSamplingSchema(
                inSpecs[0].getColumnSpec(getSourceColumnIndex()).getElementNames(), m_nrSampledCols.getIntValue());
        }
        return new DataTableSpec[]{createFirstSpec(inSpecs[0]), createdSecondSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new BaseStreamableOperator() {
            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec) throws Exception {
                //write to the distributed output ports only - non-distributed output ports will be set in the finishStreamableExecution-method
                executeStreaming((RowInput)inputs[0], (RowOutput)outputs[0], -1, exec);
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MergeOperator createMergeOperator() {
        return new MergeOperator() {

            @Override
            public StreamableOperatorInternals mergeFinal(final StreamableOperatorInternals[] operators) {
                return new SimpleStreamableOperatorInternals();
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finishStreamableExecution(final StreamableOperatorInternals internals, final ExecutionContext exec,
        final PortOutput[] output) throws Exception {
        //write to the non-distributed output ports
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
        for (int i = 0; i < getSampledIndices().length; i++) {
            int ix = getSampledIndices()[i];
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
        setSampledIndices(r.nextPermutation(names.size(), nrSamples));
        Arrays.sort(getSampledIndices());
        m_sampledNames = new String[getSampledIndices().length];
        for (int i = 0; i < getSampledIndices().length; i++) {
            m_sampledNames[i] = names.get(getSampledIndices()[i]);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // delete previous random sampling scheme so that a new one is selected upon the next configure.
        setSampledIndices(null);
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
