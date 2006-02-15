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
 *   19.12.2005 (cebron): created
 */
package de.unikn.knime.core.node.meta;

import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.Node;
import de.unikn.knime.core.node.workflow.NodeContainer;

/**
 * The MetaIONodeContainer is an extension to a normal NodeContainer. It wraps
 * the MetaInputNode and MetaOutputNode and allows to trigger a configure
 * manually.
 * 
 * @author cebron, University of Konstanz
 */
public class MetaIONodeContainer extends NodeContainer {

    /**
     * Constructor of a MetaIONodeContainer.
     * 
     * @param n node to wrap
     * @param id identifier of the node
     * @see NodeContainer
     */
    public MetaIONodeContainer(final Node n, final int id) {
        super(n, id);
    }

    /**
     * Configures the underlying node.
     * 
     * @throws InvalidSettingsException if configure goes wrong.
     */
    void configureNode() throws InvalidSettingsException {
        ((MetaIONode)getNode()).configureNode();
    }

    /**
     * Used by the <code>MetaWorkflowEditor</code> to create a 
     * <code>NodeTemplate</code>.
     * @return the full qualifying class name of the factory.
     */
    public String getFactoryName() {
        return getNode().getNodeFactory().getClass().getName();
    }
}
