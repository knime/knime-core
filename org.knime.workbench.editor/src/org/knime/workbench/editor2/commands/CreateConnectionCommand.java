/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 * -------------------------------------------------------------------
 * 
 * History
 *   09.06.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.commands;

import org.eclipse.gef.commands.Command;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.WorkflowInExecutionException;
import org.knime.core.node.workflow.WorkflowManager;

import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

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
    @Override
    public boolean canExecute() {

        if (m_targetPortID < 0) {
            return false;
        }
        if (m_sourceNode == m_targetNode) {
            return false;
        }
        if ((m_sourceNode == null) || (m_targetNode == null)) {

            // do not inform the user!! this check is just for the different
            // stages during a connection creation (dragging) such that it is
            // known once two nodes are selected to connect
            return false;
        }
        if (m_targetNode.isLocked()) {

            return false;
        }

        // let check the workflow manager if the connection can be created
        // in case it can not an exception is thrown which is caught and
        // displayed to the user
        try {
            m_manager.checkAddConnection(m_sourceNode.getNodeContainer()
                    .getID(), m_sourcePortID, m_targetNode.getNodeContainer()
                    .getID(), m_targetPortID);
        } catch (Exception e) {

            return false;
        }

        return true;
    }

    /**
     * We can undo, if the connection was created and the edit parts are not
     * locked.
     * 
     * @see org.eclipse.gef.commands.Command#canUndo()
     */
    @Override
    public boolean canUndo() {
        return (m_connection != null) && (!(m_sourceNode.isLocked()))
                && (!(m_targetNode.isLocked()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {

        // let check the workflow manager if the connection can be created
        // in case it can not an exception is thrown which is caught and
        // displayed to the user
        try {
            m_manager.checkAddConnection(m_sourceNode.getNodeContainer()
                    .getID(), m_sourcePortID, m_targetNode.getNodeContainer()
                    .getID(), m_targetPortID);

            m_connection =
                    m_manager.addConnection(m_sourceNode.getNodeContainer()
                            .getID(), m_sourcePortID, m_targetNode
                            .getNodeContainer().getID(), m_targetPortID);

        } catch (Exception e) {
            showInfoMessage("Connection could not be created.",
                    "The two nodes could not be connected due to "
                            + "the following reason:\n " + e.getMessage());
        }
    }

    private void showInfoMessage(final String header, final String message) {
        MessageBox mb =
                new MessageBox(Display.getDefault().getActiveShell(),
                        SWT.ICON_INFORMATION | SWT.OK);
        mb.setText(header);
        mb.setMessage(message);
        mb.open();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undo() {
        // Connection must be de-registered on workflow
        try {
            m_manager.removeConnection(m_connection);
        } catch (WorkflowInExecutionException ex) {
            MessageBox mb =
                    new MessageBox(Display.getDefault().getActiveShell(),
                            SWT.ICON_INFORMATION | SWT.OK);
            mb.setText("operation not allowed");
            mb.setMessage("You cannot remove a connection while the workflow"
                    + " is in execution.");
            mb.open();
        }
    }
}
