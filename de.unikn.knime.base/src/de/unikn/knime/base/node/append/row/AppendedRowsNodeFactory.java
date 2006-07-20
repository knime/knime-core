/*
 * -------------------------------------------------------------------
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
 */
package de.unikn.knime.base.node.append.row;

import de.unikn.knime.core.node.NodeDialogPane;
import de.unikn.knime.core.node.NodeFactory;
import de.unikn.knime.core.node.NodeModel;
import de.unikn.knime.core.node.NodeView;

/**
 * Factory to create nodes that concatenate input tables to one output table.
 * @author Bernd Wiswedel, University of Konstanz
 */
public class AppendedRowsNodeFactory extends NodeFactory {

    /**
     * @see NodeFactory#createNodeModel()
     */
    @Override
    public NodeModel createNodeModel() {
        return new AppendedRowsNodeModel();
    }

    /**
     * @see NodeFactory#getNrNodeViews()
     */
    @Override
    public int getNrNodeViews() {
        return 0;
    }

    /**
     * @see NodeFactory#createNodeView(int, NodeModel)
     */
    @Override
    public NodeView createNodeView(
            final int viewIndex, final NodeModel nodeModel) {
        throw new IndexOutOfBoundsException();
    }

    /**
     * @see NodeFactory#hasDialog()
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    /**
     * @see NodeFactory#createNodeDialogPane()
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new AppendedRowsNodeDialog(); 
    }
}
