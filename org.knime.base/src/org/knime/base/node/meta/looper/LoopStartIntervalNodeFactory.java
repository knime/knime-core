/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 *   24.02.2009 (meinl): created
 */
package org.knime.base.node.meta.looper;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * This is the factory for the interval loop node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class LoopStartIntervalNodeFactory extends
        NodeFactory<LoopStartIntervalNodeModel> {
    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new LoopStartIntervalNodeDialog();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LoopStartIntervalNodeModel createNodeModel() {
        return new LoopStartIntervalNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<LoopStartIntervalNodeModel> createNodeView(
            final int viewIndex, final LoopStartIntervalNodeModel nodeModel) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasDialog() {
        return true;
    }
}
