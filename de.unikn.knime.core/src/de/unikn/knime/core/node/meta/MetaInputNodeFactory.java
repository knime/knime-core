/* Created on May 29, 2006 10:36:14 AM by thor
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
package de.unikn.knime.core.node.meta;

import de.unikn.knime.core.node.NodeDialogPane;
import de.unikn.knime.core.node.NodeFactory;
import de.unikn.knime.core.node.NodeModel;
import de.unikn.knime.core.node.NodeView;

/**
 * This factory creates models for meta input nodes. The usage of this class
 * is for meta workflows only.
 * Note that because the constructor is parameterized, this factory cannot be
 * created by loading a workflow but must be instantiated by the meta node
 * itself.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class MetaInputNodeFactory extends NodeFactory {
    private final int m_dataPorts, m_modelPorts;
    
    /**
     * Creates a new factory for meta input node models.
     * 
     * @param dataPorts the number of data ports
     * @param modelPorts the number of model ports
     */
    MetaInputNodeFactory(final int dataPorts, final int modelPorts) {
        super(false);
        m_dataPorts = dataPorts;
        m_modelPorts = modelPorts;
    }

    /**
     * @see de.unikn.knime.core.node.NodeFactory#createNodeModel()
     */
    @Override
    protected NodeModel createNodeModel() {
        return new MetaInputNodeModel(m_dataPorts, m_modelPorts);
    }

    /**
     * @see de.unikn.knime.core.node.NodeFactory#getNrNodeViews()
     */
    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    /**
     * @see de.unikn.knime.core.node.NodeFactory
     *  #createNodeView(int, de.unikn.knime.core.node.NodeModel)
     */
    @Override
    public NodeView createNodeView(final int vi, final NodeModel nm) {
        return null;
    }

    /**
     * @see de.unikn.knime.core.node.NodeFactory#hasDialog()
     */
    @Override
    protected boolean hasDialog() {
        return false;
    }

    /**
     * @see de.unikn.knime.core.node.NodeFactory#createNodeDialogPane()
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return null;
    }
}
