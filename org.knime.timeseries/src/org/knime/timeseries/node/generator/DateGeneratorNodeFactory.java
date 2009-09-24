/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2009
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   05.06.2009 (Fabian Dill): created
 */
package org.knime.timeseries.node.generator;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * @author Fabian Dill
 *
 */
public class DateGeneratorNodeFactory 
    extends NodeFactory<DateGeneratorNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new DateGeneratorNodeDialog();
    }
    

    /**
     * {@inheritDoc}
     */
    @Override
    public DateGeneratorNodeModel createNodeModel() {
        return new DateGeneratorNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<DateGeneratorNodeModel> createNodeView(final int viewIndex, 
            final DateGeneratorNodeModel nodeModel) {
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
        return true;
    }

}
