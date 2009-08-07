/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 *   ${date} (${user}): created
 */
package org.knime.workbench.editor2.commands;

import org.eclipse.gef.commands.Command;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.ConnectionUIInformation;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;

/**
 * Command for deletion of connection bendpoints.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class NewBendpointDeleteCommand extends Command {

    private final WorkflowManager m_workflowManager;
    private final NodeID m_destNodeID;
    private final int m_destPort;

    private int m_index;
    private ConnectionUIInformation m_uiInfo;
    private int[] m_point;

    /**
     * New Bendpoint deletion command.
     * 
     * @param connection The connecton container
     * @param workflowManager The workflow manager that contains the connection.
     * @param index bendpoint index
     */
    public NewBendpointDeleteCommand(
            final ConnectionContainerEditPart connection,
            final WorkflowManager workflowManager,
            final int index) {
        m_workflowManager = workflowManager;
        m_uiInfo = (ConnectionUIInformation)connection.getUIInformation();
        m_index = index;
        m_destNodeID = connection.getModel().getDest();
        m_destPort = connection.getModel().getDestPort();
    }
    
    private ConnectionContainer getConnectionContainer() {
        return m_workflowManager.getIncomingConnectionFor(
                m_destNodeID, m_destPort);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        m_point = m_uiInfo.getBendpoint(m_index);
        m_uiInfo.removeBendpoint(m_index);

        // issue notification
        getConnectionContainer().setUIInfo(m_uiInfo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void redo() {
        m_uiInfo.removeBendpoint(m_index);
        // issue notification
        getConnectionContainer().setUIInfo(m_uiInfo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undo() {
        m_uiInfo.addBendpoint(m_point[0], m_point[1], m_index);
        // issue notification
        getConnectionContainer().setUIInfo(m_uiInfo);
    }
}
