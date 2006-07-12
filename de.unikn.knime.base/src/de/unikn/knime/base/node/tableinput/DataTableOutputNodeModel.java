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
 *   09.06.2006 (Fabian Dill): created
 */
package de.unikn.knime.base.node.tableinput;

import de.unikn.knime.core.data.DataTable;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.BufferedDataTable;
import de.unikn.knime.core.node.ExecutionMonitor;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeModel;
import de.unikn.knime.core.node.NodeSettings;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class DataTableOutputNodeModel extends NodeModel {

    private DataTable m_dataTable;

    /**
     * 
     *
     */
    public DataTableOutputNodeModel() {
        super(1, 0);
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel#saveSettingsTo(
     * de.unikn.knime.core.node.NodeSettings)
     */
    @Override
    protected void saveSettingsTo(final NodeSettings settings) {
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel#validateSettings(
     * de.unikn.knime.core.node.NodeSettings)
     */
    @Override
    protected void validateSettings(final NodeSettings settings)
            throws InvalidSettingsException {
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel#
     * loadValidatedSettingsFrom(de.unikn.knime.core.node.NodeSettings)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettings settings)
            throws InvalidSettingsException {
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel#execute(
     * BufferedDataTable[], 
     * de.unikn.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, 
            final ExecutionMonitor exec)
            throws Exception {
        assert inData.length == 1;
        m_dataTable = inData[0];
        return new BufferedDataTable[0];
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
        m_dataTable = null;
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel#configure(
     * de.unikn.knime.core.data.DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[0];
    }
    
    /**
     * 
     * @return - the datatable.
     */
    public DataTable getDataTable() {
        return m_dataTable;
    }

}
