/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
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
 *   17.11.2005 (cebron): created
 */
package de.unikn.knime.core.node.meta;

import de.unikn.knime.core.node.NodeDialogPane;
import de.unikn.knime.core.node.NodeFactory;
import de.unikn.knime.core.node.NodeModel;
import de.unikn.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> to create a MetaNode. The number of inputs and
 * number of outputs can be set, the corresponding <code>MetaNodeModel</code>
 * will have this number of inports and outports. Also the name of this
 * MetaNode will show the current number of inports and outports.
 * 
 * @author cebron, University of Konstanz
 */
public class MetaNodeFactory extends NodeFactory {
    
    /*
     * Number of inputs.
     */
    private int m_nrIns;

    /*
     * Number of outputs.
     */
    private int m_nrOuts;
    
    /**
     * Produces a MetaNode with the given number of inputs and outputs.
     * 
     * @param nrIns number of inputs.
     * @param nrOuts number of outputs.
     */
    public MetaNodeFactory(final int nrIns, final int nrOuts) {
        m_nrIns = nrIns;
        m_nrOuts = nrOuts;
    }
    
    /**
     * no dialog.
     * @see NodeFactory#createNodeDialogPane()
     */
    public NodeDialogPane createNodeDialogPane() {
        return null;
    }
    
    /**
     * @see NodeFactory#createNodeModel()
     */
    public NodeModel createNodeModel() {
        return new MetaNodeModel(m_nrIns, m_nrOuts);
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
        return "MetaNode " + m_nrIns + ":" + m_nrOuts;
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
