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
package org.knime.base.node.mine.regression.polynomial.learner;

import org.knime.core.node.GenericNodeFactory;
import org.knime.core.node.GenericNodeView;
import org.knime.core.node.NodeDialogPane;

/**
 * This factory creates all necessary objects for the polynomial regression
 * learner node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class PolyRegLearnerNodeFactory extends
        GenericNodeFactory<PolyRegLearnerNodeModel> {
    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new PolyRegLearnerDialog();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PolyRegLearnerNodeModel createNodeModel() {
        return new PolyRegLearnerNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GenericNodeView<PolyRegLearnerNodeModel> createNodeView(
            final int viewIndex, final PolyRegLearnerNodeModel nodeModel) {
        if (viewIndex == 0) {
            return new PolyRegCoefficientView(nodeModel);
        } else if (viewIndex == 1) {
            return new PolyRegLineNodeView(nodeModel);
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getNrNodeViews() {
        return 2;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasDialog() {
        return true;
    }
}
