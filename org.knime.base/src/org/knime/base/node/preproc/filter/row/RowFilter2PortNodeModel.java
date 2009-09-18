/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 *
 * History
 *   15.09.2009 (ohl): created
 */
package org.knime.base.node.preproc.filter.row;

import java.io.File;
import java.io.IOException;

import org.knime.base.node.preproc.filter.row.rowfilter.EndOfTableException;
import org.knime.base.node.preproc.filter.row.rowfilter.IncludeFromNowOn;
import org.knime.base.node.preproc.filter.row.rowfilter.RowFilter;
import org.knime.base.node.preproc.filter.row.rowfilter.RowFilterFactory;
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

/**
 * Model of a node filtering rows. This node has two ports: index 0 contains the
 * matching rows, index 1 the other rows of the input table.
 *
 * @author Peter Ohl, University of Konstanz
 */
public class RowFilter2PortNodeModel extends NodeModel {

    // the row filter
    private RowFilter m_rowFilter;

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

        RowFilter tmpFilter = null;

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

        try {

            int rowIdx = -1;
            int rows = in.getRowCount();
            boolean allMatch = false;
            boolean allMiss = false;

            for (DataRow row : in) {
                rowIdx++;
                exec.setProgress(rowIdx / (double)rows, "Adding row " + rowIdx
                        + " of " + rows);
                exec.checkCanceled();

                if (allMatch) {
                    match.addRowToTable(row);
                    continue;
                }
                if (allMiss) {
                    miss.addRowToTable(row);
                    continue;
                }

                try {
                    if (m_rowFilter.matches(row, rowIdx)) {
                        match.addRowToTable(row);
                    } else {
                        miss.addRowToTable(row);
                    }
                } catch (EndOfTableException eote) {
                    miss.addRowToTable(row);
                    allMiss = true;
                } catch (IncludeFromNowOn ifnoe) {
                    match.addRowToTable(row);
                    allMatch = true;
                }

            }
        } finally {
            match.close();
            miss.close();
        }
        return new BufferedDataTable[]{match.getTable(), miss.getTable()};
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
