/* 
 * -------------------------------------------------------------------
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
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   Feb 23, 2006 (wiswedel): created
 */
package org.knime.base.node.mine.regression.linear.predict;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;

/**
 * Factory for linear regression predictor node.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class LinRegPredictorNodeFactory extends NodeFactory {
    /**
     * @see NodeFactory#createNodeModel()
     */
    @Override
    public NodeModel createNodeModel() {
        return new LinRegPredictorNodeModel();
    }

    /**
     * @see NodeFactory#getNrNodeViews()
     */
    @Override
    public int getNrNodeViews() {
        return 0;
    }

    /**
     * @see NodeFactory#createNodeView(int, NodeModel)
     */
    @Override
    public NodeView createNodeView(final int index, final NodeModel m) {
        throw new IndexOutOfBoundsException();
    }

    /**
     * @see NodeFactory#hasDialog()
     */
    @Override
    public boolean hasDialog() {
        return false;
    }

    /**
     * @see NodeFactory#createNodeDialogPane()
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        throw new IllegalStateException();
    }
}
