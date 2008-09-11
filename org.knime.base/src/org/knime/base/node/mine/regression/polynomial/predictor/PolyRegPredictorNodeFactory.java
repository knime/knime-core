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
 * ------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.regression.polynomial.predictor;

import org.knime.base.node.mine.regression.predict.RegressionPredictorNodeModel;
import org.knime.core.node.GenericNodeFactory;
import org.knime.core.node.GenericNodeView;
import org.knime.core.node.NodeDialogPane;

/**
 * This factory creates all necessary object for the polynomial regression
 * predictor node. Currently this node only has a model and no dialog or view.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class PolyRegPredictorNodeFactory extends
        GenericNodeFactory<RegressionPredictorNodeModel> {
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
    public RegressionPredictorNodeModel createNodeModel() {
        return new RegressionPredictorNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GenericNodeView<RegressionPredictorNodeModel> createNodeView(
            final int viewIndex, final RegressionPredictorNodeModel nodeModel) {
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
}
