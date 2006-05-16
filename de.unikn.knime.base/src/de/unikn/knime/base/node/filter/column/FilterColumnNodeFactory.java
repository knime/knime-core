/* --------------------------------------------------------------------- *
 *   This source code, its documentation and all appendant files         *
 *   are protected by copyright law. All rights reserved.                *
 *                                                                       *
 *   Copyright, 2003 - 2006                                              *
 *   Universitaet Konstanz, Germany.                                     *
 *   Lehrstuhl fuer Angewandte Informatik                                *
 *   Prof. Dr. Michael R. Berthold                                       *
 *                                                                       *
 *   You may not modify, publish, transmit, transfer or sell, reproduce, *
 *   create derivative works from, distribute, perform, display, or in   *
 *   any way exploit any of the content, in whole or in part, except as  *
 *   otherwise expressly permitted in writing by the copyright owner.    *
 * --------------------------------------------------------------------- *
 */
package de.unikn.knime.base.node.filter.column;

import de.unikn.knime.core.node.NodeDialogPane;
import de.unikn.knime.core.node.NodeFactory;
import de.unikn.knime.core.node.NodeModel;
import de.unikn.knime.core.node.NodeView;

/**
 * The factory for the column filter node.
 * 
 * @see de.unikn.knime.core.node.NodeFactory
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class FilterColumnNodeFactory extends NodeFactory {

    /**
     * @see de.unikn.knime.core.node.NodeFactory#createNodeModel()
     */
    public NodeModel createNodeModel() {
        return new FilterColumnNodeModel();
    }

    /**
     * @see de.unikn.knime.core.node.NodeFactory#getNrNodeViews()
     */
    public int getNrNodeViews() {
        return 0;
    }

    /**
     * This node has no view.
     * 
     * @see de.unikn.knime.core.node.NodeFactory#createNodeView(int,NodeModel)
     */
    public NodeView createNodeView(final int i, final NodeModel nodeModel) {
        throw new InternalError();
    }
    
    /**
     * @return <b>true</b>.
     * @see de.unikn.knime.core.node.NodeFactory#hasDialog()
     */
    public boolean hasDialog() {
        return true;
    }

    /**
     * @see de.unikn.knime.core.node.NodeFactory#createNodeDialogPane()
     */
    public NodeDialogPane createNodeDialogPane() {
        return new FilterColumnNodeDialog();
    }
}
