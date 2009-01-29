/*
 * --------------------------------------------------------------------- *
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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.cluster.kmeans;

import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeDialogPane;

/**
 * Create classes for k-means Clustering NodeModel, NodeView and NodeDialogPane.
 * 
 * @author Michael Berthold, University of Konstanz
 */
public class ClusterNodeFactory extends NodeFactory<ClusterNodeModel> {
    /**
     * {@inheritDoc}
     */
    @Override
    public ClusterNodeModel createNodeModel() {
        return new ClusterNodeModel();
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
    public ClusterNodeView createNodeView(final int i,
            final ClusterNodeModel nodeModel) {
        if (i != 0) {
            throw new IllegalStateException();
        }
        return new ClusterNodeView(nodeModel);
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
        return new ClusterNodeDialog();
    }
}
