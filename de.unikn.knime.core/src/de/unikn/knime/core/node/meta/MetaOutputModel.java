/* Created on Jun 23, 2006 1:18:24 PM by thor
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

import java.io.File;
import java.io.IOException;

import de.unikn.knime.core.node.CanceledExecutionException;
import de.unikn.knime.core.node.ExecutionMonitor;
import de.unikn.knime.core.node.NodeModel;

/**
 * This model is an abstract model for meta output nodes. It is only intended
 * for use inside meta workflows and only by meta nodes and not by the user.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public abstract class MetaOutputModel extends NodeModel {
    /**
     * Create a new MetaOuputModel.
     * 
     * @param nrDataIns the number of data input ports (usually 1)
     * @param nrPredParamsIns the number of model input ports (usually 1)
     */
    public MetaOutputModel(final int nrDataIns, final int nrPredParamsIns) {
        super(nrDataIns, 0, nrPredParamsIns, 0);
    }
    
    /** 
     * @see de.unikn.knime.core.node.NodeModel
     *  #loadInternals(java.io.File, de.unikn.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do here
    }

    /** 
     * @see de.unikn.knime.core.node.NodeModel
     *  #saveInternals(java.io.File, de.unikn.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do here
    }    
}
