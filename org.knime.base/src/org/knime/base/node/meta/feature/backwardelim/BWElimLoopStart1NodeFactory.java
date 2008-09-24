/*
 * ------------------------------------------------------------------ *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   26.02.2008 (thor): created
 */
package org.knime.base.node.meta.feature.backwardelim;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * This factory creates all necessary classes for the backward elimination head
 * node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class BWElimLoopStart1NodeFactory extends NodeFactory<BWElimLoopStartNodeModel> {
    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
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
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BWElimLoopStartNodeModel createNodeModel() {
        return new BWElimLoopStartNodeModel(1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<BWElimLoopStartNodeModel> createNodeView(final int index,
            final BWElimLoopStartNodeModel model) {
        return null;
    }
}
