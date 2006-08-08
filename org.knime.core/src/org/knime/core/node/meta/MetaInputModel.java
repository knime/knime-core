/* Created on Jun 23, 2006 12:36:10 PM by thor
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
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 */
package org.knime.core.node.meta;

import java.io.File;
import java.io.IOException;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeModel;


/**
 * This model is an abstract model for meta input nodes. It is only intended
 * for use inside meta workflows and only by meta nodes and not by the user.
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
     * @see org.knime.core.node.NodeModel
     *  #loadInternals(java.io.File, org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do here
    }

    /** 
     * @see org.knime.core.node.NodeModel
     *  #saveInternals(java.io.File, org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do here
    }
}
