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
import de.unikn.knime.core.node.NodeSettings;
import de.unikn.knime.core.node.workflow.NodeContainer;

/**
 * The specific implementation of a <code>MetaIONodeContainer</code>. It 
 * allows to set the <code>DataTable</code> and <code>DataTableSpec</code>.
 * 
 * @author cebron, University of Konstanz
 */
public class MetaInputNodeContainer extends MetaIONodeContainer {

    /**
    * The MetaInputNodeContainer is an extension to a MetaIONodeContainer.
    * It wraps the MetaInputNode and allows to trigger a configure manually.
    * @param n node to wrap
    * @param id identifier of the node
    * @see NodeContainer
    */
    public MetaInputNodeContainer(final Node n, final int id) {
        super(n, id);
    }
    /**
     * Configures the underlying node.
     * @throws InvalidSettingsException if configure goes wrong.
     */
    void configureNode() throws InvalidSettingsException {
        ((MetaInputNode)getNode()).configureNode();
    }
    
    /**
     * Creates a new NodeContainer and reads it's status and information from
     * the NodeSettings object. Note that the list of predecessors and
     * successors will NOT be initalized correctly. The Workflow manager is
     * supposed to take care of re-initializing the connections.
     * 
     * @param sett Retrieve the data from.
     * @return new NodeContainer
     * @throws InvalidSettingsException If the required keys are not available
     *             in the NodeSettings.
     * 
     */
    public static NodeContainer createNodeContainer(final NodeSettings sett)
            throws InvalidSettingsException {
        // create new Node based on configuration
        Node newNode = MetaInputNode.createNode(sett);
        // read id
        int newID = sett.getInt(NodeContainer.KEY_ID);
       
        // create new NodeContainer and return it
        NodeContainer newNC = new MetaInputNodeContainer(newNode, newID);
        newNC.setExtraInfo(createExtraInfo(sett));
        return newNC;
    }
    
    /**
     * Set the input <code>DataTable</code>.
     * @param dt the input <code>DataTable</code>
     */
    public void setInDataTable(final DataTable dt) {
        ((MetaInputNode)getNode()).setInDataTable(dt);
    }
    
    /**
     * Set the input <code>DataTableSpec</code>.
     * @param spec the input <code>DataTableSpec</code>
     */
    public void setInTableSpec(final DataTableSpec spec) {
        ((MetaInputNode)getNode()).setInTableSpec(spec);
    } 
}
