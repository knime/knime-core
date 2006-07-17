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
package de.unikn.knime.core.node.meta;

import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.BufferedDataTable;
import de.unikn.knime.core.node.ExecutionContext;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.ModelContentRO;
import de.unikn.knime.core.node.ModelContentWO;
import de.unikn.knime.core.node.NodeSettingsRO;
import de.unikn.knime.core.node.NodeSettingsWO;

/**
 * This model is for injecting models into a meta workflow. It should not be
 * used for anything else.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class ModelInputNodeModel extends MetaInputModel {
    private ModelContentRO m_predictorParams;
    
    /**
     * Creates a new data table input model with no input ports and one 
     * data output node. 
     */
    public ModelInputNodeModel() {
        super(0, 1);
    }

    /**
     * Does nothing but return an empty datatable array.
     * 
     * @param inData the input data table array
     * @param exec the execution monitor
     * @return an empty (zero-length) array
     * @throws Exception actually, no exception is thrown
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        return new BufferedDataTable[0];
    }

    /**
     * Does nothing but return an empty datatable spec array.
     * 
     * @param inSpecs the input specs
     * @return an empty (zero-length) array
     * @throws InvalidSettingsException actually, no exception is thrown
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[0];
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
     *  #saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        // nothing to save here        
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel
     *  #validateSettings(NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        // nothing to do here        
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel
     *  #loadValidatedSettingsFrom(NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
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
     * Sets the predictor params that should be passed on in
     * {@link #saveModelContent(int, ModelContentWO)}.
     * 
     * @param predParams the predictor parameters
     */
    public void setModelContent(final ModelContentRO predParams) {
        m_predictorParams = predParams;
    }

    /** 
     * @see de.unikn.knime.core.node.NodeModel
     *  #saveModelContent(int, ModelContentWO)
     */
    @Override
    protected void saveModelContent(final int index,
            final ModelContentWO predParams) throws InvalidSettingsException {
        m_predictorParams.copyTo(predParams);
    }    
}
