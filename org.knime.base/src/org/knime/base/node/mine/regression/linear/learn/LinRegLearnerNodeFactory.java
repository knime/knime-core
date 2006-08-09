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
 *   Feb 22, 2006 (wiswedel): created
 */
package org.knime.base.node.mine.regression.linear.learn;

import org.knime.base.node.mine.regression.linear.view.LinRegLineNodeView;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;

/**
 * Factory class for linear regression learner node.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class LinRegLearnerNodeFactory extends NodeFactory {
    /**
     * @see NodeFactory#createNodeModel()
     */
    @Override
    public NodeModel createNodeModel() {
        return new LinRegLearnerNodeModel();
    }

    /**
     * @see NodeFactory#getNrNodeViews()
     */
    @Override
    public int getNrNodeViews() {
        return 2;
    }

    /**
     * @see NodeFactory#createNodeView(int, NodeModel)
     */
    @Override
    public NodeView createNodeView(final int index, final NodeModel model) {
        LinRegLearnerNodeModel m = (LinRegLearnerNodeModel)model;
        switch (index) {
        case 0:
            return new LinRegLearnerNodeView(m);
        case 1:
            return new LinRegLineNodeView(m);
        default:
            throw new IndexOutOfBoundsException();
        }
    }

    /**
     * @see NodeFactory#hasDialog()
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    /**
     * @see NodeFactory#createNodeDialogPane()
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new LinRegLearnerNodeDialogPane();
    }
}
