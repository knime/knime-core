/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (c) KNIME.com AG, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 * Created: 21.03.2012
 * Author: Peter Ohl
 */
package org.knime.workbench.editor2.commands;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * Moves the connection of the "last" connected input port one port down.
 *
 * @author Peter Ohl, KNIME.com AG, Zurich, Switzerland
 */
public class ShiftConnectionCommand extends AbstractKNIMECommand {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(ShiftConnectionCommand.class);

    private final NodeContainerEditPart m_node;

    private final NodeID m_nodeID;

    /** for undo */
    private int m_oldPort = -1;

    /** for undo */
    private int m_newPort = -1;

    /**
     *
     */
    public ShiftConnectionCommand(final WorkflowManager wfm,
            final NodeContainerEditPart node) {
        super(wfm);
        m_nodeID = node.getNodeContainer().getID();
        m_node = node;
    }

    /**
     * @return the lowest index of a port that is not connected.
     */
    private int getLastConnectedPort() {
        NodeContainer nc = m_node.getNodeContainer();
        int startIdx = 1;
        if (nc instanceof WorkflowManager) {
            startIdx = 0;
        }
        int lastConnPort = -1;
        for (int p = startIdx; p < nc.getNrInPorts(); p++) {
            if (getHostWFM().getIncomingConnectionFor(m_nodeID, p) != null) {
                lastConnPort = p;
            }
        }
        if (lastConnPort < 0 && startIdx > 0) {
            // test the implicit flow var port, if it is the only connected port
            if (getHostWFM().getIncomingConnectionFor(m_nodeID, 0) != null) {
                lastConnPort = 0;
            }
        }
        return lastConnPort;
    }

    private int getNextMatchingPort(final ConnectionContainer conn) {
        int p = conn.getDestPort() + 1; // start with the next port
        while (p < m_node.getNodeContainer().getNrInPorts() - 1) {
            if (getHostWFM().canAddNewConnection(conn.getSource(),
                    conn.getSourcePort(), m_nodeID, p)) {
                return p;
            }
            p++;
        }
        if (p == m_node.getNodeContainer().getNrInPorts()) {
            return -1;
        }
        return p;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canExecute() {
        int port = getLastConnectedPort();
        if (port < 0 || port >= m_node.getNodeContainer().getNrInPorts() - 1) {
            return false;
        }
        ConnectionContainer existingConn =
                getHostWFM().getIncomingConnectionFor(m_nodeID, port);
        assert existingConn != null;
        if (!getHostWFM().canRemoveConnection(existingConn)) {
            return false;
        }
        if (getNextMatchingPort(existingConn) == -1) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        m_oldPort = getLastConnectedPort();
        ConnectionContainer existingConn =
                getHostWFM().getIncomingConnectionFor(m_nodeID, m_oldPort);
        assert existingConn != null;
        m_newPort = getNextMatchingPort(existingConn);
        getHostWFM().removeConnection(existingConn);
        try {
            getHostWFM().addConnection(existingConn.getSource(),
                    existingConn.getSourcePort(), m_nodeID, m_newPort);
        } catch (Exception e) {
            LOGGER.error("Unable to insert new connection - "
                    + "restoring old connection", e);
            getHostWFM().addConnection(existingConn.getSource(),
                    existingConn.getSourcePort(), m_nodeID, m_oldPort);
            m_oldPort = -1;
            m_newPort = -1;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canUndo() {
        return m_oldPort != -1 && m_newPort != -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undo() {
        ConnectionContainer newConn =
                getHostWFM().getIncomingConnectionFor(m_nodeID, m_newPort);
        assert newConn != null;
        getHostWFM().removeConnection(newConn);
        try {
            getHostWFM().addConnection(newConn.getSource(),
                    newConn.getSourcePort(), m_nodeID, m_oldPort);
        } catch (Exception e) {
            LOGGER.error("Unable to redo connection shift - "
                    + "restoring shifted connection", e);
            getHostWFM().addConnection(newConn.getSource(),
                    newConn.getSourcePort(), m_nodeID, m_newPort);
        }

    }
}
