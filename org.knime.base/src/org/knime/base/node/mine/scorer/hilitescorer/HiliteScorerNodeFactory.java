/*
 * --------------------------------------------------------------------- *
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
package org.knime.base.node.mine.scorer.hilitescorer;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * The factory for the hilite scorer node.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class HiliteScorerNodeFactory 
        extends NodeFactory<HiliteScorerNodeModel> {
    
    /**
     * {@inheritDoc}
     */
    @Override
    public HiliteScorerNodeModel createNodeModel() {
        return new HiliteScorerNodeModel();
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
    public NodeView<HiliteScorerNodeModel> createNodeView(
            final int i, final HiliteScorerNodeModel nodeModel) {
        if (i == 0) {
            return new HiliteScorerNodeView(nodeModel);
//        } else if (i == 1) {
//            return new ROCView((HiliteScorerNodeModel)nodeModel);
        } else {
            throw new IllegalArgumentException("No such view");
        }
    }

    /**
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

        return new HiliteScorerNodeDialog();
    }
}
