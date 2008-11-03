/* ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   01.11.2008 (wiswedel): created
 */
package org.knime.testing.node.blocking;

import java.util.concurrent.locks.ReentrantLock;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public final class BlockingNodeFactory extends NodeFactory<BlockingNodeModel> {
    
    private final ReentrantLock m_lock;

    /** Factory constructor that keeps the lock being used by any 
     * node model created with this factory.
     * @param lock The lock to use. Must not be null.
     */
    public BlockingNodeFactory(final ReentrantLock lock) {
        if (lock == null) {
            throw new NullPointerException();
        }
        m_lock = lock;
    }

    /** {@inheritDoc} */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public BlockingNodeModel createNodeModel() {
        return new BlockingNodeModel(m_lock);
    }

    /** {@inheritDoc} */
    @Override
    public NodeView<BlockingNodeModel> createNodeView(
            int viewIndex, BlockingNodeModel nodeModel) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    protected boolean hasDialog() {
        return false;
    }

}
