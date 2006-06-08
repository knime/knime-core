/* Created on May 29, 2006 10:18:52 AM by thor
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
import de.unikn.knime.core.node.NodeModel;
import de.unikn.knime.core.node.NodeSettings;
import de.unikn.knime.core.node.PredictorParams;

/**
 * This node model represents a node that just passes all data at the input
 * ports unchanged to the output ports.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public abstract class PassThroughNodeModel extends NodeModel {
    private final PredictorParams[] m_predictorParams;
    
    /**
     * Creates a new node model that simply passes the input data to the
     * output ports.
     * 
     * @param dataPorts the number of data ports
     * @param modelPorts the number od model ports
     */
    protected PassThroughNodeModel(final int dataPorts,
            final int modelPorts) {
        super(dataPorts, dataPorts, modelPorts, modelPorts);
        m_predictorParams = new PredictorParams[modelPorts];
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel
     *  #saveSettingsTo(de.unikn.knime.core.node.NodeSettings)
     */
    @Override
    protected void saveSettingsTo(final NodeSettings settings) {
        // this node does not have settings
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel
     *  #validateSettings(de.unikn.knime.core.node.NodeSettings)
     */
    @Override
    protected void validateSettings(final NodeSettings settings)
    throws InvalidSettingsException {
        // this node does not have settings
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel
     *  #loadValidatedSettingsFrom(de.unikn.knime.core.node.NodeSettings)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettings settings)
    throws InvalidSettingsException {
        // this node does not have settings
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel
     *  #execute(de.unikn.knime.core.data.DataTable[],
     *  de.unikn.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected DataTable[] execute(final DataTable[] inData,
            final ExecutionMonitor exec) throws Exception {
        return inData;
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
        // nothing to do
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel
     *  #configure(de.unikn.knime.core.data.DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
    throws InvalidSettingsException {
        return inSpecs;
    }

    /** 
     * @see de.unikn.knime.core.node.NodeModel
     *  #loadPredictorParams(int, de.unikn.knime.core.node.PredictorParams)
     */
    @Override
    protected void loadPredictorParams(final int index,
            final PredictorParams predParams) throws InvalidSettingsException {
        m_predictorParams[index] = predParams;
    }

    /** 
     * @see de.unikn.knime.core.node.NodeModel
     *  #savePredictorParams(int, de.unikn.knime.core.node.PredictorParams)
     */
    @Override
    protected void savePredictorParams(final int index,
            final PredictorParams predParams) throws InvalidSettingsException {
        m_predictorParams[index].copyTo(predParams);
    }
}
