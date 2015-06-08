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
 * History
 *   Sept 17 2008 (mb): created (from wiswedel's TableToVariableNode)
 */
package org.knime.base.node.util.timerinfo;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.inactive.InactiveBranchConsumer;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeTimer;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * A simple node collecting timer information from the current workflow and
 * providing it as output table.
 *
 * @author Michael Berthold, University of Konstanz
 */
public class TimerinfoNodeModel extends NodeModel implements InactiveBranchConsumer {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(TimerinfoNodeModel.class);

    /**
     * One optional variable input, one data output.
     */
    protected TimerinfoNodeModel() {
        super(new PortType[] {FlowVariablePortObject.TYPE_OPTIONAL},
              new PortType[] {BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new PortObjectSpec[] { createSpec() };
    }

    private DataTableSpec createSpec() {
        DataTableSpecCreator dtsc = new DataTableSpecCreator();
        DataColumnSpec[] colSpecs = new DataColumnSpec[] {
            new DataColumnSpecCreator("Name", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Execution Time", LongCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Execution Time since last Reset", LongCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Execution Time since Start", LongCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Nr of Executions since last Reset", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Nr of Executions since Start", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("NodeID", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Classname", StringCell.TYPE).createSpec()
        };
        dtsc.addColumns(colSpecs);
        return dtsc.createSpec();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        BufferedDataContainer result = exec.createDataContainer(createSpec());
        WorkflowManager wfm = NodeContext.getContext().getWorkflowManager();
        for (NodeContainer nc : wfm.getNodeContainers()) {
            NodeTimer nt = nc.getNodeTimer();
            DataRow row = new DefaultRow(
                new RowKey("Node " + nc.getID().getIndex()),
                new StringCell(nc.getName()),
                nt.getLastExecutionDuration() >= 0
                    ? new LongCell(nt.getLastExecutionDuration()) : DataType.getMissingCell(),
                new LongCell(nt.getExecutionDurationSinceReset()),
                new LongCell(nt.getExecutionDurationSinceStart()),
                new IntCell(nt.getNrExecsSinceReset()),
                new IntCell(nt.getNrExecsSinceStart()),
                new StringCell(nc.getID().toString()),
                new StringCell(nc instanceof NativeNodeContainer
                    ? ((NativeNodeContainer)nc).getNodeModel().getClass().getName() : "n/a")
            );
            result.addRowToTable(row);
        }
        result.close();
        return new PortObject[] { result.getTable() };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // no model or view to load (all relevant information is already in the output table
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        // no settings
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        // no settings
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // ignore -> no view
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        // no settings
    }

}
