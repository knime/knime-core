/* Created on May 29, 2006 3:11:11 PM by thor
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
package de.unikn.knime.core.node.meta;

import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.BufferedDataTable;
import de.unikn.knime.core.node.ExecutionMonitor;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeSettings;

/**
 * This model is for injecting data into a meta workflow. It should not be
 * used for anything else.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class DataInputNodeModel extends MetaInputModel {
    private DataTableSpec m_spec;
    private BufferedDataTable m_datatable;

    
    /**
     * Creates a new data table input model with no input ports and one 
     * data output node. 
     */
    public DataInputNodeModel() {
        super(1, 0);
    }

    /**
     * Does nothing but return the data table set by
     * {@link #setBufferedDataTable(BufferedDataTable)}.
     * 
     * @param inData the input data table array
     * @param exec the execution monitor
     * @return the datatable set by 
     * {@link #setBufferedDataTable(BufferedDataTable)}
     * @throws Exception actually, no exception is thrown
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionMonitor exec) throws Exception {
        return new BufferedDataTable[] {m_datatable};
    }

    /**
     * Does nothing but return the data table spec set by
     * {@link #setDataTableSpec(DataTableSpec)}.
     * 
     * @param inSpecs the input specs
     * @return the datatable spec set by
     * {@link #setDataTableSpec(DataTableSpec)}
     * @throws InvalidSettingsException actually, no exception is thrown
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[] {m_spec};
    }


    /**
     * Sets the datatable spec that should be passed on in
     * {@link #configure(DataTableSpec[])}.
     * 
     * @param spec the data table spec
     */
    public void setDataTableSpec(final DataTableSpec spec) {
        m_spec = spec;
    }
    

    /**
     * Sets the datatable that should be passed on in
     * {@link #execute(BufferedDataTable[], ExecutionMonitor)}.
     * 
     * @param table the data table
     */
    public void setBufferedDataTable(final BufferedDataTable table) {
        m_datatable = table;
    }

    /**
     * @see de.unikn.knime.core.node.meta.MetaInputModel#canBeExecuted()
     */
    @Override
    public boolean canBeExecuted() {
        return (m_datatable != null);
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel
     *  #saveSettingsTo(de.unikn.knime.core.node.NodeSettings)
     */
    @Override
    protected void saveSettingsTo(final NodeSettings settings) {
        // nothing to save here        
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel
     *  #validateSettings(de.unikn.knime.core.node.NodeSettings)
     */
    @Override
    protected void validateSettings(final NodeSettings settings)
    throws InvalidSettingsException {
        // nothing to do here        
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel
     *  #loadValidatedSettingsFrom(de.unikn.knime.core.node.NodeSettings)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettings settings)
    throws InvalidSettingsException {
        // nothing to do here
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
        // nothing to do here
    }
    
    /**
     * Returns the set datatable.
     * 
     * @return a datatable
     */    
    BufferedDataTable getBufferedDataTable() {
        return m_datatable;
    }
    
    /**
     * Returns the set datatable spec.
     * 
     * @return a datatable spec
     */
    DataTableSpec getDataTableSpec() {
        return m_spec;
    }
}
