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
package org.knime.base.node.mine.bfn.fuzzy;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class FuzzyBasisFunctionLearnerNodeFactory
        extends NodeFactory<FuzzyBasisFunctionLearnerNodeModel> {
    
    /**
     * {@inheritDoc}
     */
    @Override
    public FuzzyBasisFunctionLearnerNodeModel createNodeModel() {
        return new FuzzyBasisFunctionLearnerNodeModel();
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
    public NodeView<FuzzyBasisFunctionLearnerNodeModel> createNodeView(
            final int viewIndex, 
            final FuzzyBasisFunctionLearnerNodeModel nodeModel) {
        return new FuzzyBasisFunctionLearnerNodeView(nodeModel);
    }

    /**
     * @return <b>true</b>.
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
        return new FuzzyBasisFunctionLearnerNodeDialog();
    }
}
