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
 *    18.09.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.pie.node;

import org.knime.core.node.GenericNodeFactory;
import org.knime.core.node.NodeDialogPane;

import org.knime.base.node.viz.pie.datamodel.PieVizModel;

/**
 * Basic node factory class of all pie charts.
 * @author Tobias Koetter, University of Konstanz
 * @param <D> the {@link PieVizModel} implementation
 * @param <M> the {@link PieNodeModel} implementation
 * @param <V> the {@link PieNodeView} implementation
 */
public abstract class PieNodeFactory<D extends PieVizModel,
M extends PieNodeModel<D>, V extends PieNodeView>
extends GenericNodeFactory<M> {

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract M createNodeModel();

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
    public abstract V createNodeView(
            final int viewIndex, final M nodeModel);

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new PieNodeDialogPane();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

}
