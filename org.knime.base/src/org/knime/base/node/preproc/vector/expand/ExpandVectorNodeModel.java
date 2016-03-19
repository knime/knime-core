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
package org.knime.base.node.preproc.vector.expand;


import java.io.File;
import java.io.IOException;
import java.util.Vector;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.IntValue;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.streamable.BufferedDataTableRowOutput;
import org.knime.core.node.streamable.DataTableRowInput;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.streamable.StreamableOperatorInternals;

/**
 * This is the model implementation for a node which extracts a given subset of elements of
 * a string or double vector to individual string/double columns.
 *
 * @author M. Berthold
 * @since 3.2
 */
public class ExpandVectorNodeModel extends BaseExpandVectorNodeModel {

    /* static factory methods for the SettingsModels used here and in the NodeDialog. */
    static SettingsModelString createIndexColSelectSettingsModel() {
        return new SettingsModelString("IndexColumn", null);
    }
    private final SettingsModelString m_indexColumn = createIndexColSelectSettingsModel();

    /**
     * Initialize model. One Data Inport, two Data Outports.
     */
    protected ExpandVectorNodeModel() {
        super(new PortType[] {BufferedDataTable.TYPE, BufferedDataTable.TYPE},
            new PortType[] {BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputPortRole[] getInputPortRoles() {
        return new InputPortRole[]{InputPortRole.DISTRIBUTED_STREAMABLE,
            InputPortRole.NONDISTRIBUTED_STREAMABLE};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.DISTRIBUTED};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        // user settings must be set and valid
        checkBaseSettings(inSpecs[0]);
        if (!inSpecs[1].getColumnSpec(m_indexColumn.getStringValue()).getType().isCompatible(IntValue.class)) {
            throw new InvalidSettingsException("Selected column '"
                    + m_indexColumn.getStringValue() + "' does not contain indices!");
        }
        // checks passed - we still don't what our output table will look like...
        return new DataTableSpec[]{null};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {
        // retrieve indices from second table
        readIndexTable(new DataTableRowInput(inData[1]));
        // and now start data processing
        BufferedDataContainer out1 = exec.createDataContainer(createFirstSpec(inData[0].getDataTableSpec()));
        RowOutput rowOutput = new BufferedDataTableRowOutput(out1);
        RowInput rowInput = new DataTableRowInput(inData[0]);
        this.executeStreaming(rowInput, rowOutput, inData[0].size(), exec);
        return new BufferedDataTable[]{out1.getTable()};
    }

    /*
     * read second table providing the list of indices to extract.
     */
    private void readIndexTable(final RowInput inData) throws InterruptedException {
        Vector<Integer> indices = new Vector<Integer>();
        DataRow row;
        int ix = 0;
        while ((row = inData.poll()) != null) {
            DataCell cell = row.getCell(m_sourceColumnIndex);
            if (!(cell instanceof IntValue)) {
                throw new IllegalArgumentException("Not an index in row " + ix + "!");
            }
            indices.add(Integer.valueOf(((IntValue)cell).getIntValue()));
            ix++;
        }
        m_sampledIndices = indices.stream().mapToInt(i -> i).toArray();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new BaseStreamableOperator() {
            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                    throws Exception {
                // read the entire (non distributed) table of indices
                readIndexTable((RowInput)inputs[1]);
                // and then stream the actual row processing
                executeStreaming((RowInput)inputs[0], (RowOutput)outputs[0], -1, exec);
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finishStreamableExecution(final StreamableOperatorInternals internals, final ExecutionContext exec,
        final PortOutput[] output) throws Exception {
        // Nothing to do here.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_sampledIndices = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        m_indexColumn.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(settings);
        m_indexColumn.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        m_indexColumn.loadSettingsFrom(settings);
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
