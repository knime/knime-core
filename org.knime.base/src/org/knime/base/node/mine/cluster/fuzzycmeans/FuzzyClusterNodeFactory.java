/*
 * ------------------------------------------------------------------
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
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.cluster.fuzzycmeans;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;

/**
 * Create classes for fuzzy c-means Clustering NodeModel, NodeView and
 * NodeDialogPane.
 * 
 * @author Michael Berthold, University of Konstanz
 * @author Nicolas Cebron, University of Konstanz
 */
public class FuzzyClusterNodeFactory extends NodeFactory {
    /**
     * @see NodeFactory#createNodeModel()
     */
    @Override
    public NodeModel createNodeModel() {
        return new FuzzyClusterNodeModel();
    }

    /**
     * @see NodeFactory#getNrNodeViews()
     */
    @Override
    public int getNrNodeViews() {
        return 1;
    }

    /**
     * @see NodeFactory#createNodeView(int,NodeModel)
     */
    @Override
    public NodeView createNodeView(final int i, final NodeModel nodeModel) {
        if (i == 0) {
            return new FuzzyClusterNodeView(nodeModel);
        } else {
            throw new IllegalArgumentException(
                    "FuzzyClusterNode has only one view!!");
        }
    }

    /**
     * @see NodeFactory#createNodeDialogPane()
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new FuzzyClusterNodeDialog();
    }

    /**
     * @see org.knime.core.node.NodeFactory#hasDialog()
     */
    @Override
    public boolean hasDialog() {
        return true;
    }
}
