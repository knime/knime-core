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
 * ---------------------------------------------------------------------
 *
 * History
 *   27.09.2007 (cebron): created
 */
package org.knime.base.node.mine.svm.learner;

import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeDialogPane;

/**
 * NodeFactory for the SVM Learner Node.
 *
 * @author cebron, University of Konstanz
 */
public class SVMLearnerNodeFactory extends
        NodeFactory<SVMLearnerNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new SVMLearnerNodeDialog();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SVMLearnerNodeModel createNodeModel() {
        return new SVMLearnerNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SVMLearnerNodeView createNodeView(final int viewIndex,
            final SVMLearnerNodeModel nodeModel) {
        return new SVMLearnerNodeView(nodeModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getNrNodeViews() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasDialog() {
        return true;
    }

}
