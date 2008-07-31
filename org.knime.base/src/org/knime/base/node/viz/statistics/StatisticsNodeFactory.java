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
 *   18.04.2005 (cebron): created
 */
package org.knime.base.node.viz.statistics;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;

/**
 * Factory class for the Statistics Node.
 * 
 * @author Nicolas Cebron, University of Konstanz
 */
public class StatisticsNodeFactory extends NodeFactory {
    /**
     * This node has no dialog.
     * @see NodeFactory#createNodeDialogPane()
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return null;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public NodeModel createNodeModel() {
        return new StatisticsNodeModel();
    }
    /**
     * The view offers statistical information on the input table.
     * @see NodeFactory#createNodeView(int, NodeModel)
     */
    @Override
    public NodeView createNodeView(final int viewIndex, 
            final NodeModel nodeModel) {
        if (viewIndex != 0) {
            throw new IllegalArgumentException();
        }
        return new StatisticsNodeView(nodeModel);
    }
    /**
     * This node has one view.
     * @see NodeFactory#getNrNodeViews()
     */
    @Override
    public int getNrNodeViews() {
          return 1;
    }
    /**
     * No dialog for this node.
     * @see NodeFactory#hasDialog()
     */
    @Override
    public boolean hasDialog() {
        return false;
    }
}
