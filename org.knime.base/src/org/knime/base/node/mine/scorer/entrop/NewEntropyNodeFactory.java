/*  
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 * History
 *   Apr 13, 2006 (wiswedel): created
 */
package org.knime.base.node.mine.scorer.entrop;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;

/**
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class NewEntropyNodeFactory extends NodeFactory<EntropyNodeModel> {
    
    private final boolean m_enableOutput;
    
    /** @param enableOutput whether node should have output port 
     * (it didn't have one in 1.x.x) */
    protected NewEntropyNodeFactory(final boolean enableOutput) {
        m_enableOutput = enableOutput;
    }
    
    /** Instantiates class with enabled output. */
    public NewEntropyNodeFactory() {
        this(true);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public EntropyNodeModel createNodeModel() {
        return new EntropyNodeModel(m_enableOutput);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EntropyNodeView createNodeView(final int viewIndex,
            final EntropyNodeModel nodeModel) {
        return new EntropyNodeView(nodeModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new EntropyNodeDialogPane();
    }
}
