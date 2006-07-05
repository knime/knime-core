/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   29.06.2005 (ohl): created
 */
package de.unikn.knime.base.node.filter.row;

import java.io.File;
import java.io.IOException;

import de.unikn.knime.base.node.filter.row.rowfilter.RowFilter;
import de.unikn.knime.base.node.filter.row.rowfilter.RowFilterFactory;
import de.unikn.knime.core.data.DataTable;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.CanceledExecutionException;
import de.unikn.knime.core.node.ExecutionMonitor;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeModel;
import de.unikn.knime.core.node.NodeSettings;

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
 * @author ohl, University of Konstanz
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
     * @see NodeModel#saveSettingsTo(NodeSettings)
     */
    protected void saveSettingsTo(final NodeSettings settings) {
        assert settings != null;

        if (m_rowFilter != null) {
            NodeSettings filterCfg = settings.addConfig(CFGFILTER);
            m_rowFilter.saveSettingsTo(filterCfg);
        }
    }

    /**
     * @see NodeModel#validateSettings(NodeSettings)
     */
    protected void validateSettings(final NodeSettings settings)
            throws InvalidSettingsException {
        loadOrValidateSettingsFrom(settings, true);
    }

    /**
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettings)
     */
    protected void loadValidatedSettingsFrom(final NodeSettings settings)
            throws InvalidSettingsException {
        loadOrValidateSettingsFrom(settings, false);
    }

    private void loadOrValidateSettingsFrom(final NodeSettings settings,
            final boolean verifyOnly) throws InvalidSettingsException {

        RowFilter tmpFilter = null;

        if (settings.containsKey(CFGFILTER)) {
            NodeSettings filterCfg = settings.getConfig(CFGFILTER);
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

    /**
     * @see NodeModel#execute(DataTable[], ExecutionMonitor)
     */
    protected DataTable[] execute(final DataTable[] inData,
            final ExecutionMonitor exec) throws Exception {
        if (m_rowFilter != null) {
            m_rowFilter.configure(inData[0].getDataTableSpec());
            return new DataTable[]{new RowFilterTable(inData[0], m_rowFilter)};
        } else {
            throw new InvalidSettingsException(
                    "No row filter set in RowFilter table");
        }
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel#reset()
     */
    protected void reset() {
        // nothing to do.
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel #loadInternals(java.io.File,
     *      de.unikn.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to load
        return;
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel #saveInternals(java.io.File,
     *      de.unikn.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to save
        return;
    }

    /**
     * @see NodeModel#configure(DataTableSpec[])
     */
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
