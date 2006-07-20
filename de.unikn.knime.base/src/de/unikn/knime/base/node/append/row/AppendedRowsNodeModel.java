/* 
 * -------------------------------------------------------------------
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
 */
package de.unikn.knime.base.node.append.row;

import java.io.File;
import java.io.IOException;

import de.unikn.knime.base.data.append.row.AppendedRowsTable;
import de.unikn.knime.core.data.DataTable;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.BufferedDataTable;
import de.unikn.knime.core.node.CanceledExecutionException;
import de.unikn.knime.core.node.ExecutionContext;
import de.unikn.knime.core.node.ExecutionMonitor;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeModel;
import de.unikn.knime.core.node.NodeSettingsRO;
import de.unikn.knime.core.node.NodeSettingsWO;

/**
 * <code>NodeModel</code> that concatenates its two input table to one output
 * table.
 * 
 * @see de.unikn.knime.base.data.append.row.AppendedRowsTable
 * @author Bernd Wiswedel, University of Konstanz
 */
public class AppendedRowsNodeModel extends NodeModel {

    /** NodeSettings key if to append suffix (boolean). 
     * If false, skip the rows. */
    static final String CFG_APPEND_SUFFIX = "append_suffix";

    /** NodeSettings key: suffix to append. */
    static final String CFG_SUFFIX = "suffix";

    private boolean m_appendSuffix = false;

    private String m_suffix = null;

    /**
     * Creates new node model with two inputs and one output.
     */
    public AppendedRowsNodeModel() {
        super(2, 1);
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel#execute( BufferedDataTable[],
     *      ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        DataTable out = new AppendedRowsTable(
                (m_appendSuffix ? m_suffix : null), (DataTable[])inData);
        return new BufferedDataTable[] {
                exec.createBufferedDataTable(out, exec)};
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel#configure(DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec[] outSpecs = new DataTableSpec[1];
        outSpecs[0] = AppendedRowsTable.generateDataTableSpec(inSpecs);
        return outSpecs;
    }

    /**
     * @see NodeModel#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addBoolean(CFG_APPEND_SUFFIX, m_appendSuffix);
        if (m_suffix != null) {
            settings.addString(CFG_SUFFIX, m_suffix);
        }
    }

    /**
     * @see NodeModel#validateSettings(NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        boolean appendSuffix = settings.getBoolean(CFG_APPEND_SUFFIX);
        if (appendSuffix) {
            String suffix = settings.getString(CFG_SUFFIX);
            if (suffix == null || suffix.equals("")) {
                throw new InvalidSettingsException("Invalid suffix: " + suffix);
            }
        }
    }

    /**
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_appendSuffix = settings.getBoolean(CFG_APPEND_SUFFIX);
        if (m_appendSuffix) {
            m_suffix = settings.getString(CFG_SUFFIX);
        } else {
            // may be in there, but must not necessarily
            m_suffix = settings.getString(CFG_SUFFIX, m_suffix);
        }
    }

    /**
     * @see NodeModel#reset()
     */
    @Override
    protected void reset() {
    }

    /**
     * @see de.unikn.knime.core.node.
     *      NodeModel#loadInternals(File, ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir, 
            final ExecutionMonitor exec) 
            throws IOException, CanceledExecutionException {
        
    }

    /**
     * @see de.unikn.knime.core.node.
     *      NodeModel#saveInternals(File, ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir, 
            final ExecutionMonitor exec) 
            throws IOException, CanceledExecutionException {
        
    }

}
