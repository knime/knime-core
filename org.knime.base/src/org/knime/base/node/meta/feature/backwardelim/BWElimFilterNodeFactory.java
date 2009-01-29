/*
 * ------------------------------------------------------------------ *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 *   28.02.2008 (thor): created
 */
package org.knime.base.node.meta.feature.backwardelim;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * This factory creates all necessary classes for the feature elimination filter
 * node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class BWElimFilterNodeFactory extends
        NodeFactory<BWElimFilterNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new BWElimFilterNodeDialog();
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
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BWElimFilterNodeModel createNodeModel() {
        return new BWElimFilterNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<BWElimFilterNodeModel> createNodeView(
            final int index, final BWElimFilterNodeModel model) {
        return null;
    }
}
