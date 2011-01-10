/* This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2011
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
 */
package org.knime.workbench.ui.metanodes;

import org.eclipse.ui.XMLMemento;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * MetaNode template as displayed in the {@link MetaNodeTemplateRepositoryView}.
 * Holds a name and the NodeID from the MetaNode loaded into the 
 * {@link WorkflowManager} of the {@link MetaNodeTemplateRepositoryManager}.
 * This {@link NodeID} is upadted during load by the 
 * {@link MetaNodeTemplateRepositoryManager}, i.e. only the last index is saved.
 * The {@link MetaNodeTemplateRepositoryItem} saves and loads itself to and from
 * an {@link XMLMemento} passed from the 
 * {@link MetaNodeTemplateRepositoryManager}. 
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class MetaNodeTemplateRepositoryItem {

    private static final String CFG_NAME_KEY = "MNRI2-name";
    private static final String CFG_ID_KEY = "MNRI2-id";
    
    private String m_name;
    private NodeID m_nodeID;
    private int m_index;
    
    /**
     * 
     * @param name name of the template 
     * @param nodeID nodeID of the represented meta node
     */
    public MetaNodeTemplateRepositoryItem(final String name, 
            final NodeID nodeID) {
        m_name = name;
        m_nodeID = nodeID;
    }
    
    /**
     * Default constructor for {@link MetaNodeTemplateRepositoryManager} 
     * during load.
     */
    MetaNodeTemplateRepositoryItem() {
    }
    
    /**
     * Called from {@link MetaNodeTemplateRepositoryManager} during load, since
     * the {@link NodeID} is updated each session.
     * @param nodeID the {@link NodeID} from the new WorkflowManager of the 
     *  {@link MetaNodeTemplateRepositoryManager} 
     */
    void updateNodeID(final NodeID nodeID) {
        m_nodeID = new NodeID(nodeID, m_index);
    }
    
    /**
     * 
     * @return name of this template
     */
    public String getName() {
        return m_name;
    }
    
    /**
     * 
     * @return nodeID of the represented MetaNode
     */
    public NodeID getNodeID() {
        return m_nodeID;
    }
    
    /**
     * 
     * @return last digit of the NodeID
     */
    int getIndex() {
        return m_index;
    }
    
    
    /**
     * Saves name and last index of {@link NodeID}.
     * @param memento to save to
     */
    void saveTo(final XMLMemento memento) {
        memento.putString(CFG_NAME_KEY, m_name);
        memento.putInteger(CFG_ID_KEY, m_nodeID.getIndex());
    }
    
    /**
     * Loads name and last index of {@link NodeID}.
     * @param memento to load from 
     */
    void loadFrom(final XMLMemento memento) {
        m_name = memento.getString(CFG_NAME_KEY);
        m_index = memento.getInteger(CFG_ID_KEY);
    }

}
