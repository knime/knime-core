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
 *   09.06.2005 (Florian Georg): created
 */
package de.unikn.knime.workbench.editor2.commands;

import org.eclipse.gef.commands.Command;

import de.unikn.knime.core.node.workflow.ConnectionContainer;
import de.unikn.knime.core.node.workflow.WorkflowManager;
import de.unikn.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * Command for creating connections between an in-port and an out-port.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class CreateConnectionCommand extends Command {

    private NodeContainerEditPart m_sourceNode;

    private NodeContainerEditPart m_targetNode;

    private int m_sourcePortID = -1;

    private int m_targetPortID = -1;

    private WorkflowManager m_manager;

    private boolean m_startedOnOutPort;

    private ConnectionContainer m_connection;

    /**
     * @param workflowManager The workflow manager to create the connection in
     */
    public void setManager(final WorkflowManager workflowManager) {
        m_manager = workflowManager;

    }

    /**
     * @return Returns the startedOnOutPort.
     */
    public boolean wasStartedOnOutPort() {
        return m_startedOnOutPort;
    }

    /**
     * @param b Wheter creation of this command was started at an out port
     */
    public void setStartedOnOutPort(final boolean b) {
        m_startedOnOutPort = b;

    }

    /**
     * @return Returns the sourceNode.
     */
    public NodeContainerEditPart getSourceNode() {
        return m_sourceNode;
    }

    /**
     * @param sourceNode The sourceNode to set.
     */
    public void setSourceNode(final NodeContainerEditPart sourceNode) {
        m_sourceNode = sourceNode;
    }

    /**
     * @return Returns the sourcePortID.
     */
    public int getSourcePortID() {
        return m_sourcePortID;
    }

    /**
     * @param sourcePortID The sourcePortID to set.
     */
    public void setSourcePortID(final int sourcePortID) {
        m_sourcePortID = sourcePortID;
    }

    /**
     * @return Returns the targetNode.
     */
    public NodeContainerEditPart getTargetNode() {
        return m_targetNode;
    }

    /**
     * @param targetNode The targetNode to set.
     */
    public void setTargetNode(final NodeContainerEditPart targetNode) {
        m_targetNode = targetNode;
    }

    /**
     * @return Returns the targetPortID.
     */
    public int getTargetPortID() {
        return m_targetPortID;
    }

    /**
     * @param targetPortID The targetPortID to set.
     */
    public void setTargetPortID(final int targetPortID) {
        m_targetPortID = targetPortID;
    }

    /**
     * @return Returns the connection, <code>null</code> if execute() was not
     *         called before.
     */
    public ConnectionContainer getConnection() {
        return m_connection;
    }

    /**
     * @return <code>true</code> if the connection can be added (that is, all
     *         fields were set to valid values before and the corresponding edit
     *         parts are not locked
     * 
     * TODO if only a portIndex is -1, try to find an appropriate index on the
     * current source/target node
     * @see org.eclipse.gef.commands.Command#canExecute()
     */
    public boolean canExecute() {
        return (m_sourceNode != null)
                && (m_targetNode != null)
                && (!(m_sourceNode.isLocked()))
                && (!(m_targetNode.isLocked()))
                && (m_manager.canAddConnection(m_sourceNode.getNodeContainer()
                        .getID(), m_sourcePortID, m_targetNode
                        .getNodeContainer().getID(), m_targetPortID));
    }

    /**
     * We can undo, if the connection was created and the edit parts are not
     * locked.
     * 
     * @see org.eclipse.gef.commands.Command#canUndo()
     */
    public boolean canUndo() {
        return (m_connection != null) && (!(m_sourceNode.isLocked()))
                && (!(m_targetNode.isLocked()));
    }

    /**
     * 
     * @see org.eclipse.gef.commands.Command#execute()
     */
    public void execute() {
        // Connection must be registered on workflow
        m_connection = m_manager.addConnection(
                  m_sourceNode.getNodeContainer().getID(), m_sourcePortID, 
                  m_targetNode.getNodeContainer().getID(), m_targetPortID);
    }

    /**
     * 
     * @see org.eclipse.gef.commands.Command#undo()
     */
    public void undo() {
        // Connection must be de-registered on workflow
        m_manager.removeConnection(m_connection);

    }

}
