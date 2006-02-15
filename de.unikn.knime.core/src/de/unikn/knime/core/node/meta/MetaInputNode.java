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

import de.unikn.knime.core.data.DataTable;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.Node;
import de.unikn.knime.core.node.NodeFactory;
import de.unikn.knime.core.node.NodeSettings;

/**
 * The specific implementation of an <code>MetaIONode</code>. It allows to
 * set the <code>DataTable</code> and the <code>DataTableSpec</code>.
 * 
 * @author cebron, University of Konstanz
 */
public class MetaInputNode extends MetaIONode {

    /**
     * Constructs a new MetaIONode.
     * 
     * @param nodeFactory The node factory for the creation of model, view,
     * dialog.
     * @see Node
     */
    public MetaInputNode(final NodeFactory nodeFactory) {
        super(nodeFactory);
    }

    /**
     * static method to create a new Node based on a <code>NodeSettings</code>-
     * object.
     * 
     * @param settings The object to read the node's settings from.
     * @return newly created <code>Node</code>, initialized using the config
     *         object
     * @throws InvalidSettingsException If a property is not available
     */
    public static Node createNode(final NodeSettings settings)
            throws InvalidSettingsException {

        // create new node
        Node newNode = new MetaInputNode(Node.createNodeFactory(settings));
        newNode.loadConfigFrom(settings); // load remaining settings
        return newNode; // return fully initialized Node
    }

    /**
     * Set the input <code>DataTable</code>.
     * 
     * @param dt the input <code>DataTable</code>
     * @throws NullPointerException if a null <code>DataTable</code> is passed
     * as an argument.
     */
    public void setInDataTable(final DataTable dt) {
        ((MetaInputNodeModel)getNodeModel()).setInDataTable(dt);
    }

    /**
     * Set the input <code>DataTableSpec</code>.
     * 
     * @param spec the input <code>DataTableSpec</code>
     * @throws NullPointerException if a null <code>DataTableSpec</code> is 
     * passed as an argument.
     */
    public void setInTableSpec(final DataTableSpec spec) {
        ((MetaInputNodeModel)getNodeModel()).setInTableSpec(spec);
    }
}
