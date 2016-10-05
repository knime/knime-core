/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 */
package org.knime.workbench.editor2.commands;

import org.knime.core.api.node.workflow.IConnectionContainer;
import org.knime.core.api.node.workflow.IWorkflowManager;
import org.knime.core.node.NodeLogger;
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

    /* for undo */
    private int m_oldPort = -1;

    /* for undo */
    private int m_newPort = -1;

    /**
     *
     */
    public ShiftConnectionCommand(final IWorkflowManager wfm,
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
        if ((lastConnPort < 0) && (startIdx > 0) && (getHostWFM().getIncomingConnectionFor(m_nodeID, 0) != null)) {
            // test the implicit flow var port, if it is the only connected port
            lastConnPort = 0;
        }
        return lastConnPort;
    }

    private int getNextMatchingPort(final IConnectionContainer conn) {
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
        IConnectionContainer existingConn =
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
        IConnectionContainer existingConn =
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
        IConnectionContainer newConn =
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
