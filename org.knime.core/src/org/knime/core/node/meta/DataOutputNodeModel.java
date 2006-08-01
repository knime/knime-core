/* Created on Jun 23, 2006 1:22:38 PM by thor
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 */
package org.knime.core.node.meta;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This model is for collecting the data tables that are produced by the meta
 * workflow.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class DataOutputNodeModel extends MetaOutputModel {
    private BufferedDataTable m_dataTable;
    private DataTableSpec m_dataTableSpec;
    
    /**
     * Creates a new output node model for datatables.
     */
    public DataOutputNodeModel() {
        super(1, 0);
        setAutoExecutable(true);
    }

    /**
     * @see org.knime.core.node.NodeModel#saveSettingsTo(
     * NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    }

    /**
     * @see org.knime.core.node.NodeModel#validateSettings(
     * NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    }

    /**
     * @see org.knime.core.node.NodeModel#loadValidatedSettingsFrom(
     * NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    }

    /**
     * @see org.knime.core.node.NodeModel#execute(
     * BufferedDataTable[], 
     * ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, 
            final ExecutionContext exec)
            throws Exception {
        assert inData.length == 1;
        m_dataTable = inData[0];
        return new BufferedDataTable[0];
    }

    /**
     * @see org.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
        m_dataTable = null;
        m_dataTableSpec = null;
    }

    /**
     * @see org.knime.core.node.NodeModel#configure(
     * org.knime.core.data.DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        m_dataTableSpec = inSpecs[0];
        return new DataTableSpec[0];
    }
    
    /**
     * Returns the datatable at the input port.
     * 
     * @return a data table
     */
     public BufferedDataTable getBufferedDataTable() {
        return m_dataTable;
    }

    
    /**
     * Returns the data table specs at the input port.
     * 
     * @return a data table spec
     */
    public DataTableSpec getDataTableSpec() {
        return m_dataTableSpec;
    }
}
