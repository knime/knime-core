/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * -------------------------------------------------------------------
 *
 * History
 *   29.06.2005 (ohl): created
 */
package org.knime.base.node.preproc.filter.row;

import java.io.File;
import java.io.IOException;

import org.knime.base.node.preproc.filter.row.rowfilter.EndOfTableException;
import org.knime.base.node.preproc.filter.row.rowfilter.IncludeFromNowOn;
import org.knime.base.node.preproc.filter.row.rowfilter.RowFilter;
import org.knime.base.node.preproc.filter.row.rowfilter.RowFilterFactory;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
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
 * Model of a node filtering rows. It keeps an instance of a row filter, which
 * tells whether or not to include a row into the result, and a range to allow
 * for row number filtering. The reason the range is kept seperatly and not in a
 * normal filter instance is performance. If we are leaving the row number range
 * we can immediately flag the end of the table, while if we would use a filter
 * instance we would have to run to the end of the input table (always getting a
 * mismatch because the row number is out of the valid range) - which could
 * potentially take a while...
 *
 * @author Peter Ohl, University of Konstanz
 */
public class RowFilterNodeModel extends NodeModel {

    // the row filter
    private RowFilter m_rowFilter;

    /** key for storing settings in config object. */
    static final String CFGFILTER = "rowFilter";

    /**
     * Creates a new Row Filter Node Model.
     */
    RowFilterNodeModel() {
        super(1, 1); // give me one input, one output. Thank you very much.

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

        RowFilter tmpFilter = null;

        if (settings.containsKey(CFGFILTER)) {
            NodeSettingsRO filterCfg = settings.getNodeSettings(CFGFILTER);
            // because we don't know what type of filter is in the config we
            // must ask the factory to figure it out for us (actually the type
            // is also saved in a valid config). When we save row filters they
            // will (hopefully!!) add their type to the config.
            tmpFilter = RowFilterFactory.createRowFilter(filterCfg);
        } else {
            throw new InvalidSettingsException("Row Filter config contains no"
                    + " row filter.");
        }

        // if we got so far settings are valid.
        if (verifyOnly) {
            return;
        }

        // take over settings
        m_rowFilter = tmpFilter;

        return;
    }

    private void execute(final RowInput inData, final RowOutput output,
            final ExecutionContext exec) throws Exception {
        // in case the node was configured and the workflow is closed
        // (and saved), the row filter isn't configured upon reloading.
        // here, we give it a chance to configure itself (e.g. find the column
        // index)
        m_rowFilter.configure(inData.getDataTableSpec());
        exec.setMessage("Searching first matching row...");
        DataRow row;
        int index = 0;
        boolean isAlwaysExcluded = false;
        boolean isAlwaysIncluded = false;
        while ((row = inData.poll()) != null) {
            boolean matches;
            if (isAlwaysExcluded) {
                matches = false;
            } else if (isAlwaysIncluded) {
                matches = true;
            } else {
                try {
                    matches = m_rowFilter.matches(row, index++);
                } catch (EndOfTableException eot) {
                    break;
                } catch (IncludeFromNowOn ifn) {
                    isAlwaysIncluded = true;
                    matches = true;
                }
            }
            exec.checkCanceled();
            if (matches) {
                exec.setMessage("Added row " + index
                        + " (\"" + row.getKey() + "\")");
                output.push(row);
            }
        }
        inData.close();
        output.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        DataTable in = inData[0];
        // in case the node was configured and the workflow is closed
        // (and saved), the row filter isn't configured upon reloading.
        // here, we give it a chance to configure itself (e.g. find the column
        // index)
        m_rowFilter.configure(in.getDataTableSpec());
        BufferedDataContainer container =
            exec.createDataContainer(in.getDataTableSpec());
        exec.setMessage("Searching first matching row...");
        try {
            int count = 0;
            RowFilterIterator it = new RowFilterIterator(in, m_rowFilter, exec);
            while (it.hasNext()) {
                DataRow row = it.next();
                count++;
                container.addRowToTable(row);
                exec.setMessage("Added row " + count + " (\""
                        + row.getKey() + "\")");
            }
        } catch (RowFilterIterator.RuntimeCanceledExecutionException rce) {
            throw rce.getCause();
        } finally {
            container.close();
        }
        return new BufferedDataTable[]{container.getTable()};
    }

    /** {@inheritDoc} */
    @Override
    public InputPortRole[] getInputPortRoles() {
        return new InputPortRole[] {InputPortRole.NONDISTRIBUTED_STREAMABLE};
    }

    /** {@inheritDoc} */
    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[] {OutputPortRole.NONDISTRIBUTED};
    }

    /** {@inheritDoc} */
    @Override
    public StreamableOperator createStreamableOperator(
            final PartitionInfo partitionInfo, final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        return new StreamableOperator() {

            @Override
            public StreamableOperatorInternals saveInternals() {
                return null;
            }

            @Override
            public void runFinal(final PortInput[] inputs,
                    final PortOutput[] outputs,
                    final ExecutionContext ctx) throws Exception {
                RowInput in = (RowInput)inputs[0];
                RowOutput out = (RowOutput)outputs[0];
                RowFilterNodeModel.this.execute(in, out, ctx);
            }
        };
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
        return;
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
            return new DataTableSpec[]{newSpec};
        }
    }
}
