/* Created on Jun 23, 2006 12:36:10 PM by thor
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
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public abstract class MetaInputModel extends NodeModel {
    /**
     * Creates a new input node for meta workflows.
     * 
     * @param nrDataOuts the number of data output ports
     * @param nrPredParamsOuts the number of model output ports
     */
    public MetaInputModel(final int nrDataOuts, final int nrPredParamsOuts) {
        super(0, nrDataOuts, 0, nrPredParamsOuts);
    }

    
    /**
     * Returns if this model can really be executed.
     * 
     * @return <code>true</code> if it can be executed, <code>false</code>
     * otherwise
     */
    public abstract boolean canBeExecuted();

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
