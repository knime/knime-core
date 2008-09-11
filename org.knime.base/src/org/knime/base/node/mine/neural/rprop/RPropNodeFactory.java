/*
 * ------------------------------------------------------------------
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
 *   26.10.2005 (cebron): created
 */
package org.knime.base.node.mine.neural.rprop;

import org.knime.core.node.GenericNodeFactory;
import org.knime.core.node.NodeDialogPane;

/**
 * Factory for the RProp Node, a MultiLayerPerceptron with resilient
 * backpropagation.
 *
 * @author Nicolas Cebron, University of Konstanz
 */
public class RPropNodeFactory extends GenericNodeFactory<RPropNodeModel> {
    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new RPropNodeDialog();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RPropNodeModel createNodeModel() {
        return new RPropNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RPropNodeView createNodeView(final int viewIndex,
            final RPropNodeModel nodeModel) {
        return new RPropNodeView(nodeModel);
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
    public boolean hasDialog() {
        return true;
    }
}
