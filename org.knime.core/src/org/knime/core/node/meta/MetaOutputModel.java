/* Created on Jun 23, 2006 1:18:24 PM by thor
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

import java.io.File;
import java.io.IOException;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeModel;


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
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do here
    }    
}
