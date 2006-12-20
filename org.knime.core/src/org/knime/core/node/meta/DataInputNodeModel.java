/* Created on May 29, 2006 3:11:11 PM by thor
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 */
package org.knime.core.node.meta;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

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
            final ExecutionContext exec) throws Exception {
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
     * {@link #execute(BufferedDataTable[], ExecutionContext)}.
     * 
     * @param table the data table
     */
    public void setBufferedDataTable(final BufferedDataTable table) {
        m_datatable = table;
    }

    /**
     * @see org.knime.core.node.meta.MetaInputModel#canBeExecuted()
     */
    @Override
    public boolean canBeExecuted() {
        return (m_datatable != null);
    }

    /**
     * @see org.knime.core.node.NodeModel
     *  #saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        // nothing to save here        
    }

    /**
     * @see org.knime.core.node.NodeModel
     *  #validateSettings(NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        // nothing to do here        
    }

    /**
     * @see org.knime.core.node.NodeModel
     *  #loadValidatedSettingsFrom(NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        // nothing to do here
    }

    /**
     * @see org.knime.core.node.NodeModel#reset()
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
