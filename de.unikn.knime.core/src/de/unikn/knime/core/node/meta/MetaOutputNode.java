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
 * The specific implementation of an <code>MetaIONode</code>. It 
 * allows to get the <code>DataTable</code> and the <code>DataTableSpec</code>
 * manually.
 * 
 * @author cebron, University of Konstanz
 */
public class MetaOutputNode extends MetaIONode {

    /**
     * Constructs a new MetaOutputNode.
     * 
     * @param nodeFactory The node factory for the creation of model, view,
     *        dialog.
     * @see Node
     */
    public MetaOutputNode(final NodeFactory nodeFactory) {
        super(nodeFactory);
    }

    /**
     * static method to create a new Node based on <code>NodeSettings</code>.
     * 
     * @param settings The object to read the node settings from.
     * @return newly created <code>Node</code>, initialized using the config
     *         object
     * @throws InvalidSettingsException If a property is not available
     */
    public static Node createNode(final NodeSettings settings)
            throws InvalidSettingsException {
        
        // create new node
        Node newNode = new MetaOutputNode(Node.createNodeFactory(settings));
        newNode.loadConfigFrom(settings); // load remaining settings
        return newNode; // return fully initialized Node
    }
    
    /**
     * @return <code>DataTable</code> at this node.
     */
    public DataTable getOutDataTable() {
        return ((MetaOutputNodeModel)getNodeModel()).getOutDataTable();
    }

    /**
     * @return the output <code>DataTableSpec</code>
     */
    public DataTableSpec getOutTableSpec() {
        return ((MetaOutputNodeModel)getNodeModel()).getOutTableSpec();
    } 
}
