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
 * ------------------------------------------------------------------------
 */
package org.knime.testing.node.failing;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
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

/**
 *
 * @author wiswedel, University of Konstanz
 */
class FailingNodeModel extends NodeModel {

    private FailingNodeConfiguration m_configuration;

    /** One data input, one data output.
     */
    FailingNodeModel() {
        super(1, 1);
    }

    /** {@inheritDoc} */
    @Override
    public InputPortRole[] getInputPortRoles() {
        // not sure if this should better be STREAMABLE
        return new InputPortRole[] {InputPortRole.DISTRIBUTED_STREAMABLE};
    }

    /** {@inheritDoc} */
    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[] {OutputPortRole.DISTRIBUTED};
    }

    /** {@inheritDoc} */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        BufferedDataTableRowOutput tableOutput =
                new BufferedDataTableRowOutput(exec.createDataContainer(inData[0].getDataTableSpec()));
        processInput(new DataTableRowInput(inData[0]), tableOutput);
        return new BufferedDataTable[] {tableOutput.getDataTable()};
    }

    /** {@inheritDoc} */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_configuration == null) { // auto config
            m_configuration = new FailingNodeConfiguration();
        }
        return inSpecs;
    }

    /** {@inheritDoc} */
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo, final PortObjectSpec[] inSpecs)
        throws InvalidSettingsException {
        return new StreamableOperator() {
            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec) throws Exception {
                final RowInput rowInput = (RowInput)inputs[0];
                final RowOutput rowOutput = (RowOutput)outputs[0];
                processInput(rowInput, rowOutput);
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_configuration = new FailingNodeConfiguration().loadSettingsInModel(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        new FailingNodeConfiguration().loadSettingsInModel(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_configuration != null) {
            m_configuration.saveSettings(settings);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
    }

    /**
     * @param rowInput
     * @param rowOutput
     * @throws Exception
     * @throws InterruptedException
     */
    private void processInput(final RowInput rowInput, final RowOutput rowOutput) throws Exception,
        InterruptedException {
        final int failAtIndex = m_configuration.getFailAtIndex();
        if (failAtIndex < 0) {
            throw new Exception("This node fails on each execution.");
        }
        int index = 0;
        DataRow r;
        while ((r = rowInput.poll()) != null) {
            if (index++ == failAtIndex) {
                throw new Exception("This node fails on each execution - row index " + failAtIndex);
            }
            rowOutput.push(r);
        }
        rowOutput.close();
    }

}
