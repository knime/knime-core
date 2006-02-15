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
import de.unikn.knime.core.node.NodeFactory;

/**
 * The MetaIONode is an extension to a normal node. It allows to trigger a
 * configure manually.
 * 
 * @author cebron, University of Konstanz
 */
public class MetaIONode extends Node {

    /**
     * Constructs a new MetaIONode.
     * 
     * @param nodeFactory The node factory for the creation of model, view,
     *        dialog.
     * @see Node
     */
    public MetaIONode(final NodeFactory nodeFactory) {
        super(nodeFactory);
    }

    /**
     * Configures the underlying node.
     * 
     * @throws InvalidSettingsException if configure goes wrong. 
     */
    protected void configureNode() throws InvalidSettingsException {
        super.configureNode();
    }
}
