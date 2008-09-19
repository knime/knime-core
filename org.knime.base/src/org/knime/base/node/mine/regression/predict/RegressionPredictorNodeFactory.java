/* 
 * -------------------------------------------------------------------
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
 *   Feb 23, 2006 (wiswedel): created
 */
package org.knime.base.node.mine.regression.predict;

import org.knime.base.node.mine.regression.linear.predict.LinRegPredictorNodeFactory;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeView;

/**
 * Factory for linear regression predictor node.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class RegressionPredictorNodeFactory 
    extends NodeFactory<RegressionPredictorNodeModel> {
    
    static {
        try {
            NodeFactory.addLoadedFactory(LinRegPredictorNodeFactory.class);
        } catch (Throwable e) {
            NodeLogger.getLogger(RegressionPredictorNodeFactory.class).warn(
                    "Couldn't add obsolete lin reg predictor node", e);
        }
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public RegressionPredictorNodeModel createNodeModel() {
        return new RegressionPredictorNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<RegressionPredictorNodeModel> 
        createNodeView(final int index, final RegressionPredictorNodeModel m) {
        throw new IndexOutOfBoundsException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        throw new IllegalStateException();
    }
}
