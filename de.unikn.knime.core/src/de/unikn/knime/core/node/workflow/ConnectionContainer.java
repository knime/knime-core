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
 *   22.03.2005 (mb): created
 *   09.01.2006 (mb): clean up for code review
 */
package de.unikn.knime.core.node.workflow;

import java.util.ArrayList;

import de.unikn.knime.core.eclipseUtil.GlobalClassCreator;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeSettings;

/**
 * Wrapper for a connection in a workflow. The connection knows which
 * <code>NodeContainer</code> s it contects and also knows about the port
 * indices. It can save and create itself to/from a <code>NodeSettings</code>
 * object. It also stores an extra object implementing
 * <code>ConnectionExtraInfo</code> which can hold, for example, information
 * about the visual layout (coordinates of the polygon...).
 * The information about source and target is final, the extra-information
 * can change and workflow listeners will be notified accordingly.
 * 
 * TODO FG: implement notifcation mechanism !!!
 * 
 * @author M. Berthold, University of Konstanz
 */
public class ConnectionContainer {

    // unique id
    private final int m_id;

    // store source and target NodeContainer as well as corresponding port
    // indices
    private final NodeContainer m_sourceNode;
    private final int m_sourcePort;

    private final NodeContainer m_targetNode;
    private final int m_targetPort;

    // also hold additional information object, which usually holds
    // graphical information for this edge
    private ConnectionExtraInfo m_extraInfo;

    // listeners for changes to the extra info can register here. Other
    // information (source/destination) can not change!
    private final ArrayList<WorkflowListener> m_listeners = 
        new ArrayList<WorkflowListener>();

    /**
     * create new container holding a connection of a workflow, storing both
     * NodeContainer and port index for source and target.
     * 
     * @param id unique identifier for this connection
     * @param sourceNode container of source node
     * @param sourcePort port index of source
     * @param targetNode container of target node
     * @param targetPort port index of target
     */
    public ConnectionContainer(final int id, final NodeContainer sourceNode,
            final int sourcePort, final NodeContainer targetNode,
            final int targetPort) {
        m_id = id;
        m_sourceNode = sourceNode;
        m_sourcePort = sourcePort;
        m_targetNode = targetNode;
        m_targetPort = targetPort;
        m_extraInfo = null;
    }

    ////////////////////////
    // Getter/Setter Methods
    ////////////////////////
    
    /**
     * @return source node
     */
    public NodeContainer getSource() {
        return m_sourceNode;
    }

    /**
     * @return source node port index
     */
    public int getSourcePort() {
        return m_sourcePort;
    }

    /**
     * @return target node
     */
    public NodeContainer getTarget() {
        return m_targetNode;
    }

    /**
     * @return target node port index
     */
    public int getTargetPort() {
        return m_targetPort;
    }

    /**
     * @return extra information object of this edge
     */
    public ConnectionExtraInfo getExtraInfo() {
        return m_extraInfo;
    }

    /** Sets new extra info object. Notifies listeners.
     * 
     * @param ei new extra information object for this edge
     */
    public void setExtraInfo(final ConnectionExtraInfo ei) {
        m_extraInfo = ei;
        fireExtraInfoChanged();
    }

    /** Return ID of connection.
     * 
     * @return unique identifier
     */
    public int getID() {
        return m_id;
    }
    
    ////////////////////
    // Listener handling
    ////////////////////

    /**
     * Adds a listener, will be notified about Extra-Info changes.
     * 
     * @param listener The listener
     */
    public void addWorkflowListener(final WorkflowListener listener) {
        if (!m_listeners.contains(listener)) {
            m_listeners.add(listener);
        }
    }

    /**
     * Removes a listener.
     * 
     * @param listener The listener
     */
    public void removeWorkflowListener(final WorkflowListener listener) {
        if (m_listeners.contains(listener)) {
            m_listeners.remove(listener);
        }
    }

    /**
     * Removes all listeners.
     */
    public void removeAllWorkflowListeners() {
        m_listeners.clear();
    }


    /**
     * Notifies listeners about extra info changes.
     */
    private void fireExtraInfoChanged() {
        WorkflowEvent event = new WorkflowEvent(
                WorkflowEvent.CONNECTION_EXTRAINFO_CHANGED, -1, null,
                getExtraInfo());
        for (int i = 0; i < m_listeners.size(); i++) {
            m_listeners.get(i).workflowChanged(event);
        }
    }

    ////////////////////
    // Keys for Settings
    ////////////////////
    
    private static final String KEY_ID = "ID";
    private static final String KEY_SOURCE_ID = "sourceID";
    private static final String KEY_SOURCE_PORT = "sourcePort";
    private static final String KEY_TARGET_ID = "targetID";
    private static final String KEY_TARGET_PORT = "targetPort";
    private static final String KEY_EXTRAINFOCLASS = "extraInfoClassName";

    /** Stores all contained information into the given settings obj. Note
     * that we do not store references to the nodes but their IDs instead. This
     * allows us to later re-create the connection by using the Workflow
     * Manager. Loading will therefore not work "stand-alone" since the
     * Workflow needs to be querifed for the corresponding NodeContainer
     * objects based on these IDs.
     * 
     * @param config The configuration to write to current settings into.
     * 
     * @see #insertNewConnectionIntoWorkflow
     */
    public void save(final NodeSettings config) {
        config.addInt(KEY_ID, m_id);
        // save source and target ids and port numbers
        config.addInt(KEY_SOURCE_ID, m_sourceNode.getID());
        config.addInt(KEY_SOURCE_PORT, m_sourcePort);
        config.addInt(KEY_TARGET_ID, m_targetNode.getID());
        config.addInt(KEY_TARGET_PORT, m_targetPort);
        // save type of extrainfo and also it's content - but only if it exists
        if (m_extraInfo != null) {
            config.addString(KEY_EXTRAINFOCLASS, m_extraInfo.getClass()
                    .getName());
            m_extraInfo.save(config);
        }
    }

    /** Creates a new ConnectionContainer by adding a new connection to the
     * workflow, which will then create the ConnectionContainer and add all
     * required information to the source and target NodeContainers.
     * 
     * @param config Retrieve the data for new connection from config
     * @param workflowMgr <code>WorkflowManager</code> to add connection to
     * @throws InvalidSettingsException If the required keys are not available
     *             in the NodeSettings. Also throws exception if for whatever
     *             reason the connection can not be added to the Workflow.
     * 
     * @see #save
     */
    public static void insertNewConnectionIntoWorkflow(
            final NodeSettings config, final WorkflowManager workflowMgr)
            throws InvalidSettingsException {
        // read ids and port indices
        int newSourceID = config.getInt(KEY_SOURCE_ID);
        int newSourcePort = config.getInt(KEY_SOURCE_PORT);
        int newTargetID = config.getInt(KEY_TARGET_ID);
        int newTargetPort = config.getInt(KEY_TARGET_PORT);
        // check if extrainfo exists
        ConnectionExtraInfo extraInfo = null;
        if (config.containsKey(KEY_EXTRAINFOCLASS)) {
            // if it does determine type of extrainfo
            String extraInfoClassName = config.getString(KEY_EXTRAINFOCLASS);
            try {
                // extraInfo = (ConnectionExtraInfo) (Class
                // .forName(extraInfoClassName)
                // .newInstance());
                extraInfo = (ConnectionExtraInfo)GlobalClassCreator
                        .createClass(extraInfoClassName).newInstance();
            } catch (Exception e) {
                throw new InvalidSettingsException("ExtraInfoClass could not " 
                        + "be loaded " + extraInfoClassName + " reason: "
                        + e.getMessage());
            }
            // and load content of extrainfo
            extraInfo.load(config);
        }
        // create new connection in workflow (which will generate the
        // corresponding ConnectionContainer)
        try {
            ConnectionContainer newCC = workflowMgr.addConnection(
                  newSourceID, newSourcePort, newTargetID, newTargetPort);
            newCC.setExtraInfo(extraInfo);
        } catch (Exception e) {
            throw new InvalidSettingsException(e.getMessage());
        }
    } // end insertNewConnectionIntoWorkflow()

} // end class ConnectionContainer.
