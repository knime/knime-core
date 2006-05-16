/*
 * @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * --------------------------------------------------------------------- *
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
package de.unikn.knime.base.node.view.table;

import de.unikn.knime.core.node.NodeDialogPane;
import de.unikn.knime.core.node.NodeFactory;
import de.unikn.knime.core.node.NodeModel;
import de.unikn.knime.core.node.NodeView;

/** 
 * Factory to create model and view for a table node. 
 * 
 * @author Bernd Wiswedel, University of Konstanz 
 */
public class TableNodeFactory extends NodeFactory {
    
    /**
     * @return <b>false</b>.
     * @see de.unikn.knime.core.node.NodeFactory#hasDialog()
     */
    public boolean hasDialog() {
        return false;
    }

    /**
     * The <code>TableNode</code> has no Dialog (i.e. Controller) - it is
     * incorporated in the view.
     * @return Nothing as this method throws an exception.
     * @throws IllegalStateException Always.
     * @see de.unikn.knime.core.node.NodeFactory#createNodeDialogPane()
     */ 
    public NodeDialogPane createNodeDialogPane() {
        throw new InternalError();
    }

    /**
     * @see de.unikn.knime.core.node.NodeFactory#createNodeModel()
     */
    public NodeModel createNodeModel() {
        return new TableNodeModel();
    }

    /**
     * @see de.unikn.knime.core.node.NodeFactory#createNodeView(int,NodeModel)
     */
    public NodeView createNodeView(final int i, final NodeModel nodeModel) {
        if (i != 0) {
            throw new IllegalArgumentException();
        }
        return new TableNodeView((TableNodeModel)nodeModel);
    }

    /**
     * @see de.unikn.knime.core.node.NodeFactory#getNrNodeViews()
     */
    public int getNrNodeViews() {
        return 1;
    }

}
