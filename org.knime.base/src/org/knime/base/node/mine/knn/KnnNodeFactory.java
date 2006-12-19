/* ------------------------------------------------------------------
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
 * 
 * History
 *   10.11.2006 (berthold): created
 */
package org.knime.base.node.mine.knn;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;

/**
 * This factory creates all necessary object for the kNN node.
 * 
 * @author Michael Berthold, University of Konstanz
 */
public class KnnNodeFactory extends NodeFactory {
    /**
     * @see org.knime.core.node.NodeFactory#createNodeDialogPane()
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new KnnNodeDialogPane();
    }

    /**
     * @see org.knime.core.node.NodeFactory#createNodeModel()
     */
    @Override
    public NodeModel createNodeModel() {
        return new KnnNodeModel();
    }

    /**
     * @see org.knime.core.node.NodeFactory #createNodeView(int,
     *      org.knime.core.node.NodeModel)
     */
    @Override
    public NodeView createNodeView(final int viewIndex,
            final NodeModel nodeModel) {
        return null;
    }

    /**
     * @see org.knime.core.node.NodeFactory#getNrNodeViews()
     */
    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    /**
     * @see org.knime.core.node.NodeFactory#hasDialog()
     */
    @Override
    protected boolean hasDialog() {
        return true;
    }
}
