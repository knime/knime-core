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
 * History
 *   Aug 12, 2006 (wiswedel): created
 */
package org.knime.base.node.preproc.domain;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public class DomainNodeFactory extends NodeFactory {

    /**
     * @see NodeFactory#createNodeDialogPane()
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new DomainNodeDialogPane();
    }

    /**
     * @see NodeFactory#createNodeModel()
     */
    @Override
    public NodeModel createNodeModel() {
        return new DomainNodeModel();
    }

    /**
     * @see NodeFactory#createNodeView(int, NodeModel)
     */
    @Override
    public NodeView createNodeView(
            final int viewIndex, final NodeModel nodeModel) {
        return null;
    }

    /**
     * @see NodeFactory#getNrNodeViews()
     */
    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    /**
     * @see NodeFactory#hasDialog()
     */
    @Override
    protected boolean hasDialog() {
        return true;
    }

}
