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
 *   Nov 23, 2016 (simon): created
 */
package org.knime.base.node.preproc.filter.definition.apply;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.property.filter.FilterHandler;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.viewproperty.FilterDefinitionHandlerPortObject;
import org.knime.core.node.streamable.BufferedDataTableRowOutput;
import org.knime.core.node.streamable.DataTableRowInput;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortObjectInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperator;

/**
 * The node model of the node which applies a filter definition to a data table.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
public class FilterApplyNodeModel extends NodeModel {

    /**
     * One data table and one {@link FilterDefinitionHandlerPortObject} as input, one data table as output.
     */
    protected FilterApplyNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE, FilterDefinitionHandlerPortObject.TYPE_OPTIONAL},
            new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new PortObjectSpec[]{inSpecs[0]};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        exec.setProgress(0);
        PortObject portObject = inObjects[1];
        DataTableSpec filterSpec = portObject == null ? ((BufferedDataTable)inObjects[0]).getDataTableSpec()
            : ((FilterDefinitionHandlerPortObject)portObject).getSpec();
        final BufferedDataTableRowOutput out = new BufferedDataTableRowOutput(
            exec.createDataContainer(((BufferedDataTable)inObjects[0]).getDataTableSpec()));
        execute(new DataTableRowInput((BufferedDataTable)inObjects[0]), out, filterSpec, exec,
            ((BufferedDataTable)inObjects[0]).size());
        return new BufferedDataTable[]{out.getDataTable()};
    }

    /**
     * Helper method to compute output, used both in streaming and non-streaming context
     */
    private void execute(final RowInput inData, final RowOutput output, final DataTableSpec filterSpec,
        final ExecutionContext exec, final long rowCount) throws Exception {
        DataRow row;
        DataTableSpec inSpec = inData.getDataTableSpec();
        FilterHandler[] filterHandlers = new FilterHandler[inData.getDataTableSpec().getNumColumns()];
        for (String colName : filterSpec.getColumnNames()) {
            int columnIndex = inSpec.findColumnIndex(colName);
            if (columnIndex < 0) {
                setWarningMessage("Filter for column \"" + colName
                    + "\" could not be applied, because the column was not found in the input table.");
            } else {
                Optional<FilterHandler> filterHandler = filterSpec.getColumnSpec(colName).getFilterHandler();
                filterHandlers[columnIndex] = filterHandler.isPresent() ? filterHandler.get() : null;
            }
        }

        long currentRowIndex = 0;
        while ((row = inData.poll()) != null) {
            exec.checkCanceled();
            // set progress if not streaming
            if (rowCount >= 0) {
                exec.setProgress(currentRowIndex / (double)rowCount);
            }
            final long currentRowIndexFinal = currentRowIndex;
            exec.setMessage(() -> "Row " + currentRowIndexFinal + "/" + rowCount);

            // check if row is filtered or not
            boolean isInAllFilters = true;
            int j = 0;
            for (DataCell cell : row) {
                if (filterHandlers[j] != null && !filterHandlers[j].isInFilter(cell)) {
                    isInAllFilters = false;
                }
                j++;
            }

            // push row to output if it should not be filtered
            if (isInAllFilters) {
                output.push(row);
            }
            currentRowIndex += 1;
        }
        inData.close();
        output.close();
    }

    /** {@inheritDoc} */
    @Override
    public InputPortRole[] getInputPortRoles() {
        return new InputPortRole[]{InputPortRole.DISTRIBUTED_STREAMABLE, InputPortRole.NONDISTRIBUTED_NONSTREAMABLE};
    }

    /** {@inheritDoc} */
    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.DISTRIBUTED};
    }

    /** {@inheritDoc} */
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new StreamableOperator() {

            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
                final DataTableRowInput in = (DataTableRowInput)inputs[0];
                final RowOutput out = (RowOutput)outputs[0];
                PortObjectInput portObjectInput = (PortObjectInput)inputs[1];
                DataTableSpec filterSpec = portObjectInput == null ? in.getDataTableSpec()
                    : ((FilterDefinitionHandlerPortObject)portObjectInput.getPortObject()).getSpec();
                FilterApplyNodeModel.this.execute(in, out, filterSpec, exec, -1);
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

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        // nothing to save
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        // nothing to validate
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        // nothing to load
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do

    }

}
