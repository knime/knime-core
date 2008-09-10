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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.cluster.fuzzycmeans;

import org.knime.core.node.GenericNodeFactory;
import org.knime.core.node.NodeDialogPane;

/**
 * Create classes for fuzzy c-means Clustering NodeModel, NodeView and
 * NodeDialogPane.
 *
 * @author Michael Berthold, University of Konstanz
 * @author Nicolas Cebron, University of Konstanz
 */
public class FuzzyClusterNodeFactory extends
        GenericNodeFactory<FuzzyClusterNodeModel> {
    /**
     * {@inheritDoc}
     */
    @Override
    public FuzzyClusterNodeModel createNodeModel() {
        return new FuzzyClusterNodeModel();
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
    public FuzzyClusterNodeView createNodeView(final int i,
            final FuzzyClusterNodeModel nodeModel) {
        if (i == 0) {
            return new FuzzyClusterNodeView(nodeModel);
        } else {
            throw new IllegalArgumentException(
                    "FuzzyClusterNode has only one view!!");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new FuzzyClusterNodeDialog();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }
}
