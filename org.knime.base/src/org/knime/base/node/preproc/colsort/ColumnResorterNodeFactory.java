/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
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
 *   Nov 13, 2007 (schweize): created
 */
package org.knime.base.node.preproc.colsort;

import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.core.node.NodeDialogPane;

/**
 * 
 * @author schweize, University of Konstanz
 */
public class ColumnResorterNodeFactory 
    extends NodeFactory<ColumnResorterNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new ColumnResorterNodeDialog();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ColumnResorterNodeModel createNodeModel() {
        return new ColumnResorterNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<ColumnResorterNodeModel> createNodeView(
            final int viewIndex, final ColumnResorterNodeModel nodeModel) {
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
        // TODO Auto-generated method stub
        return true;
    }

}
