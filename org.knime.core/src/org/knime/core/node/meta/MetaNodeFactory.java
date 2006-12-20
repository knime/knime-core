/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *   17.11.2005 (cebron): created
 */
package org.knime.core.node.meta;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> to create a MetaNode. The number of inputs and
 * number of outputs can be set, the corresponding <code>MetaNodeModel</code>
 * will have this number of inports and outports. Also the name of this
 * MetaNode will show the current number of inports and outports.
 * 
 * @author cebron, University of Konstanz
 */
public class MetaNodeFactory extends NodeFactory {
    private int m_nrIns, m_nrOuts;
    
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
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return null;
    }
    
    /**
     * @see NodeFactory#createNodeModel()
     */
    @Override
    public NodeModel createNodeModel() {
        return new MetaNodeModel(m_nrIns, m_nrOuts, 0, 0);
    }
    
    /**
     * no view.
     * @see NodeFactory#createNodeView(int, NodeModel)
     */
    @Override
    public NodeView createNodeView(final int viewIndex,
                                   final NodeModel nodeModel) {
        return null;
    }
    
    /**
     * no view.
     * @see NodeFactory#getNrNodeViews()
     */
    @Override
    protected int getNrNodeViews() {
        return 0;
    }
    
    /**
     * no dialog.
     * @see NodeFactory#hasDialog()
     */
    @Override
    protected boolean hasDialog() {
        return false;
    }
}
