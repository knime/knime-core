/* Created on Jun 19, 2006 5:08:56 PM by thor
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

import de.unikn.knime.core.data.DataTable;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.ExecutionMonitor;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeSettings;
import de.unikn.knime.core.node.PredictorParams;

/**
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class ModelInputNodeModel extends MetaInputModel {
    private PredictorParams m_predictorParams;
    
    /**
     * Creates a new data table input model with no input ports and one 
     * data output node. 
     */
    public ModelInputNodeModel() {
        super(0, 1);
    }

    /**
     * Does nothing but return the data table set by
     * {@link #setDataTable(DataTable)}.
     * 
     * @param inData the input data table array
     * @param exec the execution monitor
     * @return the datatable set by {@link #setDataTable(DataTable)}
     * @throws Exception actually, no exception is thrown
     */
    @Override
    protected DataTable[] execute(final DataTable[] inData,
            final ExecutionMonitor exec) throws Exception {
        return new DataTable[0];
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
        return new DataTableSpec[0];
    }


    /** 
     * @see de.unikn.knime.core.node.meta.MetaInputModel
     *  #setDataTableSpec(de.unikn.knime.core.data.DataTableSpec)
     */
    @Override
    public void setDataTableSpec(final DataTableSpec spec) {
        // nothing to do here
    }
    

    /** 
     * @see de.unikn.knime.core.node.meta.MetaInputModel
     *  #setDataTable(de.unikn.knime.core.data.DataTable)
     */
    @Override
    public void setDataTable(final DataTable table) {
        // nothing to do here
    }

    /**
     * @see de.unikn.knime.core.node.meta.MetaInputModel#canBeExecuted()
     */
    @Override
    public boolean canBeExecuted() {
        return (m_predictorParams != null);
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
    
    public void setPredictorParams(final PredictorParams predParams) {
        m_predictorParams = predParams;
    }

    /** 
     * @see de.unikn.knime.core.node.NodeModel
     *  #savePredictorParams(int, de.unikn.knime.core.node.PredictorParams)
     */
    @Override
    protected void savePredictorParams(final int index,
            final PredictorParams predParams) throws InvalidSettingsException {
        m_predictorParams.copyTo(predParams);
    }    
}
