/* -------------------------------------------------------------------
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
 * Factory class to produce a MetaOutputNode.
 * The MetaOutputNode is a utility node to build fully connected
 * workflows inside a NodeModel. The method <code>createNodeModel</code>
 * returns a singleton <code>MetaOutputNodeModel</code>.
 * 
 * @author cebron, University of Konstanz
 */
public class MetaOutputNodeFactory extends NodeFactory {

    /*
     * The MetaOutputNodeModel singleton.
     */
    private MetaOutputNodeModel m_model;

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
            m_model = new MetaOutputNodeModel();
        }
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
     * no view.
     * @see NodeFactory#getNrNodeViews()
     */
    public int getNrNodeViews() {
        return 0;
    }

    /**
     * no dialog.
     * @see NodeFactory#hasDialog()
     */
    public boolean hasDialog() {
        return false;
    }
}
