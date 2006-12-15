/*
 * @(#)$RCSfile$ 
 * $Revision: 4973 $ $Date: 2006-08-01 12:15:56 +0200 (Di, 01 Aug 2006) $
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
package org.knime.base.node.preproc.discretization.caim.modelapply;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;

/**
 * The Factory for the Discretization Applier.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class DiscretizationApplyNodeFactory extends NodeFactory {

    /**
     * @see org.knime.core.node.NodeFactory#createNodeModel()
     */
    public NodeModel createNodeModel() {
        return new DiscretizationApplyNodeModel();
    }

    /**
     * @see org.knime.core.node.NodeFactory#getNrNodeViews()
     */
    public int getNrNodeViews() {
        return 0;
    }

    /**
     * @see org.knime.core.node.NodeFactory#createNodeView(int,NodeModel)
     */
    public NodeView createNodeView(final int i, final NodeModel nodeModel) {
        return null;
    }
    
    /**
     * @return <b>true</b>.
     * @see org.knime.core.node.NodeFactory#hasDialog()
     */
    public boolean hasDialog() {
        return false;
    }

    /**
     * @see org.knime.core.node.NodeFactory#createNodeDialogPane()
     */
    public NodeDialogPane createNodeDialogPane() {
        return null;
    }

}
