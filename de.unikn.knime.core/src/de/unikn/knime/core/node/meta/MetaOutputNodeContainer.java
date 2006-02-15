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
 * The specific implementatino of an <code>MetaIONodeContainer</code>. It 
 * allows to get the <code>DataTable</code> and <code>DataTableSpec</code> 
 * of the underlying <code>MetaOutputNode</code>.
 * 
 * @author cebron, University of Konstanz
 */
public class MetaOutputNodeContainer extends MetaIONodeContainer {

    /**
    * The MetaOutputNodeContainer is an extension to the MetaIONodeContainer.
    * It wraps the MetaOutputNode.
    * 
    * @param n node to wrap
    * @param id identifier of the node
    * @see MetaIONodeContainer
    */
    public MetaOutputNodeContainer(final Node n, final int id) {
        super(n, id);
    }
    /**
     * Configures the underlying node.
     * @throws InvalidSettingsException if configure goes wrong.
     */
    void configureNode() throws InvalidSettingsException {
        ((MetaOutputNode)getNode()).configureNode();
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
     *         in the NodeSettings.
     * 
     */
    public static NodeContainer createNodeContainer(final NodeSettings sett)
            throws InvalidSettingsException {
        // create new Node based on configuration
        Node newNode = MetaOutputNode.createNode(sett);
        // read id
        int newID = sett.getInt(NodeContainer.KEY_ID);
        
        // create new NodeContainer and return it
        NodeContainer newNC = new MetaOutputNodeContainer(newNode, newID);
        newNC.setExtraInfo(NodeContainer.createExtraInfo(sett));
        return newNC;
    }
    
    /**
     * @return <code>DataTable</code> of the underlying <code>MetaOutputNode
     * </code>.
     */
    public DataTable getOutDataTable() {
        return ((MetaOutputNode)getNode()).getOutDataTable();
    }
    
    /**
     * Get the output DataTableSpec.
     * @return the output <code>DataTableSpec</code> of the underlying 
     * <code>MetaOutputNode</code>
     */
    public DataTableSpec getOutTableSpec() {
        return ((MetaOutputNode)getNode()).getOutTableSpec();
    } 
}
