/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2004
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
 *   13.06.2005 (cebron): created
 */
package de.unikn.knime.core.node.meta;

import de.unikn.knime.core.node.NodeDialogPane;
import de.unikn.knime.core.node.NodeFactory;
import de.unikn.knime.core.node.NodeModel;
import de.unikn.knime.core.node.NodeView;

/**
 * Factory class to produce a MetaInputNode.
 * The MetaInputNode is a utility node to build fully connected
 * workflows inside a MetaNodeModel. The method <code>createNodeModel</code>
 * returns a singleton <code>MetaInputNodeModel</code>.
 * 
 * @author cebron, University of Konstanz
 */
public class MetaInputNodeFactory extends NodeFactory {

    /*
     * The Singleton MetaInputNodeModel.
     */
    private MetaInputNodeModel m_model;

    /**
     * no dialog.
     * @see NodeFactory#createNodeDialogPane()
     */
    public NodeDialogPane createNodeDialogPane() {
        return null;
    }

    /**
     * Singleton.
     * @see NodeFactory#createNodeModel()
     */
    public NodeModel createNodeModel() {
        if (m_model == null) {
            m_model = new MetaInputNodeModel();
        }
        return m_model;
    }

    /**
     * returns the produced MetaInputNodeModel.
     * @return MetaInputNodeModel
     */
    public MetaInputNodeModel getNodeModel() {
        return m_model;
    }

    /**
     * no view.
     * @see NodeFactory#createNodeView(int, NodeModel)
     */
    public NodeView createNodeView(final int viewIndex,
            final NodeModel nodeModel) {
        return null;
    }

    /**
     * @see NodeFactory#getNodeName()
     */
    public String getNodeName() {
        return "Meta Input Node";
    }

    /**
     * @see NodeFactory#getNrNodeViews()
     */
    public int getNrNodeViews() {
        return 0;
    }

    /**
     * @see NodeFactory#hasDialog()
     */
    public boolean hasDialog() {
        return false;
    }
}
