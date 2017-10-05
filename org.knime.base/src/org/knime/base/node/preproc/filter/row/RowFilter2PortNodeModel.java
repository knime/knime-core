/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * -------------------------------------------------------------------
 *
 * History
 *   15.09.2009 (ohl): created
 */
package org.knime.base.node.preproc.filter.row;

import java.io.File;
import java.io.IOException;

import org.knime.base.node.preproc.filter.row.rowfilter.EndOfTableException;
import org.knime.base.node.preproc.filter.row.rowfilter.IRowFilter;
import org.knime.base.node.preproc.filter.row.rowfilter.IncludeFromNowOn;
import org.knime.base.node.preproc.filter.row.rowfilter.RowFilterFactory;
import org.knime.base.node.preproc.filter.row.rowfilter.RowNoRowFilter;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataContainer;
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
 * Model of a node filtering rows. This node has two ports: index 0 contains the
 * matching rows, index 1 the other rows of the input table.
 *
 * @author Peter Ohl, University of Konstanz
 */
public class RowFilter2PortNodeModel extends NodeModel {

    // the row filter
    private IRowFilter m_rowFilter;

    /** key for storing settings in config object. */
    static final String CFGFILTER = "rowFilter";

    /**
     * Creates a new Row Filter Node Model.
     */
    RowFilter2PortNodeModel() {
        super(1, 2); // one input, two outputs.

        m_rowFilter = null;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        assert settings != null;

        if (m_rowFilter != null) {
            NodeSettingsWO filterCfg = settings.addNodeSettings(CFGFILTER);
            m_rowFilter.saveSettingsTo(filterCfg);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        loadOrValidateSettingsFrom(settings, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        loadOrValidateSettingsFrom(settings, false);
    }

    private void loadOrValidateSettingsFrom(final NodeSettingsRO settings,
            final boolean verifyOnly) throws InvalidSettingsException {

        IRowFilter tmpFilter = null;

        if (settings.containsKey(CFGFILTER)) {
            NodeSettingsRO filterCfg = settings.getNodeSettings(CFGFILTER);
            tmpFilter = RowFilterFactory.createRowFilter(filterCfg);
        } else {
            throw new InvalidSettingsException("Row Filter config contains no"
                    + " row filter.");
        }

        if (verifyOnly) {
            return;
        }

        // take over settings
        m_rowFilter = tmpFilter;

        return;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable in = inData[0];
        // in case the node was configured and the workflow is closed
        // (and saved), the row filter isn't configured upon reloading.
        // here, we give it a chance to configure itself (e.g. find the column
        // index)
        m_rowFilter.configure(in.getDataTableSpec());

        BufferedDataContainer match =
                exec.createDataContainer(in.getDataTableSpec());
        BufferedDataContainer miss =
                exec.createDataContainer(in.getDataTableSpec());
        RowOutput rowOutput1 = new BufferedDataTableRowOutput(match);
        RowOutput rowOutput2 = new BufferedDataTableRowOutput(miss);
        RowInput rowInput = new DataTableRowInput(inData[0]);

        //do it
        this.execute(rowInput, rowOutput1, rowOutput2, inData[0].size(), exec);

        //note: tables are closed in the private execute method
        return new BufferedDataTable[]{match.getTable(), miss.getTable()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo, final PortObjectSpec[] inSpecs)
        throws InvalidSettingsException {
        m_rowFilter.configure((DataTableSpec) inSpecs[0]);
        return new StreamableOperator() {

            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec) throws Exception {

                //do it
                RowFilter2PortNodeModel.this.execute((RowInput) inputs[0], (RowOutput) outputs[0], (RowOutput) outputs[1], -1, exec);
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputPortRole[] getInputPortRoles() {
        InputPortRole in;
        if(m_rowFilter instanceof RowNoRowFilter) {
            //if the row count is used as filter criteria is cannot be distributed (because the RowNoRowFilter uses the row index)
            in = InputPortRole.NONDISTRIBUTED_STREAMABLE;
        } else {
            in = InputPortRole.DISTRIBUTED_STREAMABLE;
        }
        return new InputPortRole[]{in};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.DISTRIBUTED, OutputPortRole.DISTRIBUTED};
    }



    /*
     * The main work is done here
     *
     * @param rows total number of rows. Can be -1 if not available.
     */
    private void execute(final RowInput in, final RowOutput match, final RowOutput miss, final long rows,
        final ExecutionContext exec) throws InterruptedException, CanceledExecutionException {
        try {

            long rowIdx = -1;
            boolean allMatch = false;
            boolean allMiss = false;

            DataRow row;
            while ((row = in.poll()) != null) {
                rowIdx++;
                if (rows > 0) {
                    exec.setProgress(rowIdx / (double)rows, "Adding row " + rowIdx + " of " + rows);
                } else {
                    exec.setProgress("Adding row " + rowIdx + ".");
                }
                exec.checkCanceled();

                if (allMatch) {
                    match.push(row);
                    continue;
                }
                if (allMiss) {
                    miss.push(row);
                    continue;
                }

                try {
                    if (m_rowFilter.matches(row, rowIdx)) {
                        match.push(row);
                    } else {
                        miss.push(row);
                    }
                } catch (EndOfTableException eote) {
                    miss.push(row);
                    allMiss = true;
                } catch (IncludeFromNowOn ifnoe) {
                    match.push(row);
                    allMatch = true;
                }
            }
        } finally {
            match.close();
            miss.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to load
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to save
        return;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_rowFilter == null) {
            throw new InvalidSettingsException("No row filter specified");
        }
        DataTableSpec newSpec = m_rowFilter.configure(inSpecs[0]);
        if (newSpec == null) {
            // we are not changing the structure of the table.
            return inSpecs;
        } else {
            return new DataTableSpec[]{newSpec, newSpec};
        }
    }
}
