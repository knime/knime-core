/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 * --------------------------------------------------------------------- *
 *
 * History
 *   06.05.2008 (gabriel): created
 */
package org.knime.base.node.preproc.filter.rowref;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * The Reference Row Filter node allow the filtering of row IDs based
 * on a second reference table. Two modes are possible, either the corresponding
 * row IDs of the first table are included or excluded in the resulting
 * output table.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public class RowFilterRefNodeModel extends NodeModel {
    
    /** Settings model for include/exclude option. */
    private final SettingsModelString m_inexcludeRows =
        RowFilterRefNodeDialogPane.createInExcludeModel();

    /**
     * Creates a new reference row filter node model with two inputs and
     * one filtered output.
     */
    public RowFilterRefNodeModel() {
        super(2, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[]{inSpecs[0]};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        Set<RowKey> keySet = new HashSet<RowKey>();
        for (DataRow row : inData[1]) {
            keySet.add(row.getKey());
        }
        BufferedDataContainer buf =
            exec.createDataContainer(inData[0].getSpec());
        boolean exclude = m_inexcludeRows.getStringValue().equals(
                RowFilterRefNodeDialogPane.EXCLUDE);

        double rowCnt = 1;
        for (DataRow row : inData[0]) {
            exec.checkCanceled();
            exec.setProgress(
                    rowCnt++ / inData[0].getRowCount(), "Filtering...");
            if (exclude) {
                if (!keySet.contains(row.getKey())) {
                    buf.addRowToTable(row);
                }
            } else {
                if (keySet.contains(row.getKey())) {
                    buf.addRowToTable(row);
                }
            }
        }
        buf.close();
        return new BufferedDataTable[]{buf.getTable()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_inexcludeRows.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_inexcludeRows.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_inexcludeRows.validateSettings(settings);
    }
}
