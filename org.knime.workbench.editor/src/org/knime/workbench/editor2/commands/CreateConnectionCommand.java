/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * -------------------------------------------------------------------
 *
 * History
 *   09.06.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.commands;

import static org.knime.core.ui.wrapper.Wrapper.unwrapNC;
import static org.knime.core.ui.wrapper.Wrapper.wraps;
import static org.knime.workbench.ui.async.AsyncUtil.wfmAsyncSwitchRethrow;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.ConnectionID;
import org.knime.core.node.workflow.ConnectionUIInformation;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeTimer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.node.workflow.ConnectionContainerUI;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.core.ui.node.workflow.async.OperationNotAllowedException;
import org.knime.core.util.SWTUtilities;
import org.knime.workbench.editor2.editparts.ConnectableEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 * Command for creating connections between an in-port and an out-port.
 *
 * TODO ldq sez: The naïve me says that this should be a superclass of AbstractCreateNewConnectedNodeCommand
 *          and that hierarchy downwards should be modified to suit descending from that class.
 *
 * @author Florian Georg, University of Konstanz
 */
public class CreateConnectionCommand extends AbstractKNIMECommand {
    /**
     * This text is displayed to the user if they are trying to replace an inport connection on a node which is already
     * executed.
     **/
    public static final String REPLACE_CONNECTION_MESSAGE =
        "Do you want to replace the existing connection?\nThis will reset the target node!";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(CreateConnectionCommand.class);

    private ConnectableEditPart m_sourceNode;

    private ConnectableEditPart m_targetNode;

    private int m_sourcePortID = -1;

    private int m_targetPortID = -1;

    private boolean m_startedOnOutPort;

    private ConnectionUIInformation m_newConnectionUIInfo;

    private ConnectionContainerUI m_connection;

    // for undo
    private ConnectionContainerUI m_oldConnection;

    private boolean m_confirm;


    /**
     * Initializes from preference store, whether to confirm reconnection or
     * not.
     * @param hostWFM The host workflow
     */
    public CreateConnectionCommand(final WorkflowManagerUI hostWFM) {
        super(hostWFM);
        m_confirm = KNIMEUIPlugin.getDefault().getPreferenceStore()
            .getBoolean(PreferenceConstants.P_CONFIRM_RECONNECT);
    }

    /**
     *
     * @param confirm if the replacement of  an existing connection should be
     *  confirmed by the user
     */
    public void setConfirm(final boolean confirm) {
        m_confirm = confirm;
    }

    /**
     *
     * @return true if the replacement of  an existing connection should be
     *  confirmed by the user
     */
    public boolean doConfirm() {
        return m_confirm;
    }

    /**
     * @param newConnectionUIInfo the newConnectionUIInfo to set
     */
    public void setNewConnectionUIInfo(
            final ConnectionUIInformation newConnectionUIInfo) {
        m_newConnectionUIInfo = newConnectionUIInfo;
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
    // TODO: allow also workflow root edit parts
    public ConnectableEditPart getSourceNode() {
        return m_sourceNode;
    }

    /**
     * @param sourceNode The sourceNode to set.
     */
    // TODO: allow also WorkflowRootEditParts
    public void setSourceNode(final ConnectableEditPart sourceNode) {
        m_sourceNode = sourceNode;
    }

    /**
     * @return Returns the sourcePortID.
     */
    // TODO: rename in index
    public int getSourcePortID() {
        return m_sourcePortID;
    }

    /**
     * @param sourcePortID The sourcePortID to set.
     */
    // TODO: rename in index
    public void setSourcePortID(final int sourcePortID) {
        m_sourcePortID = sourcePortID;
    }

    /**
     * @return Returns the targetNode.
     */
    // TODO: allow also WorkflowRootEditParts
    public ConnectableEditPart getTargetNode() {
        return m_targetNode;
    }

    /**
     * @param targetNode The targetNode to set.
     */
    // TODO: allow also WorkflowRootEditPart
    public void setTargetNode(final ConnectableEditPart targetNode) {
        m_targetNode = targetNode;
    }

    /**
     * @return Returns the targetPortID.
     */
    // TODO: rename in index
    public int getTargetPortID() {
        return m_targetPortID;
    }

    /**
     * @param targetPortID The targetPortID to set.
     */
    // TODO: rename in index
    public void setTargetPortID(final int targetPortID) {
        m_targetPortID = targetPortID;
    }

    /**
     * @return Returns the connection, <code>null</code> if execute() was not
     *         called before.
     */
    public ConnectionContainerUI getConnection() {
        return m_connection;
    }

    /**
     * @return <code>true</code> if the connection can be added (that is, all
     *         fields were set to valid values before and the corresponding edit
     *         parts are not locked
     * @see org.eclipse.gef.commands.Command#canExecute()
     */
    @Override
    public boolean canExecute() {
        // TODO this could be handled better: we do ultimately useless work if source port id has never
        //          been set
        if (!super.canExecute()) {
            return false;
        }
        if (m_sourceNode == null || m_targetNode == null) {
            return false;
        }
        WorkflowManagerUI wm = getHostWFMUI();
        if (m_targetPortID < 0) {
            ConnectableEditPart target = getTargetNode();
            if (target instanceof NodeContainerEditPart) {
                m_targetPortID = ((NodeContainerEditPart)target).getFreeInPort(getSourceNode(), getSourcePortID());
            }
        }

        // check whether an existing connection can be removed
        ConnectionContainerUI conn = wm.getIncomingConnectionFor(
                m_targetNode.getNodeContainer().getID(), m_targetPortID);
        boolean canRemove = conn == null || wm.canRemoveConnection(conn.getID());
        // let the workflow manager check if the connection can be created
        // or removed
        boolean canAdd = wm.canAddConnection(
                m_sourceNode.getNodeContainer().getID(), m_sourcePortID,
                m_targetNode.getNodeContainer().getID(), m_targetPortID);
        return canRemove && canAdd;
    }


    /**
     * We can undo, if the connection was created and the edit parts are not
     * locked.
     *
     * {@inheritDoc}
     */
    @Override
    public boolean canUndo() {
        if (m_connection == null) {
            return false;
        }
        return getHostWFMUI().canRemoveConnection(m_connection.getID());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        // check whether it is the same connection
        ConnectionContainerUI conn = getHostWFMUI().getIncomingConnectionFor(
                m_targetNode.getNodeContainer().getID(), m_targetPortID);
        if (conn != null
                && conn.getSource().equals(
                        m_sourceNode.getNodeContainer().getID())
                && conn.getSourcePort() == m_sourcePortID
                && conn.getDest().equals(
                        m_targetNode.getNodeContainer().getID())
                && conn.getDestPort() == m_targetPortID) {
            // it is the very same connection -> do nothing
            return;
        }
        WorkflowManagerUI wm = getHostWFMUI();

        // let the workflow manager check if the connection can be created
        // in case it cannot an exception is thrown which is caught and
        // displayed to the user
        try {
            // if target nodeport is already connected
            if (conn != null) {
                // ask user if it should be replaced...
                if (m_confirm && m_targetNode.getNodeContainer().getNodeContainerState().isExecuted()) {
                    // show confirmation message only if target node is executed
                    MessageDialogWithToggle msgD = openReconnectConfirmDialog(m_confirm, null);
                    m_confirm = !msgD.getToggleState();
                    if (msgD.getReturnCode() != IDialogConstants.YES_ID) {
                        return;
                    }
                }
                //removing the existing connection will be done while creating the new one
                //removeConnection(wm, conn);
                m_oldConnection = conn;
            }

            LOGGER.debug("adding connection from "
                    + m_sourceNode.getNodeContainer().getID() + " "
                    + m_sourcePortID + " to "
                    + m_targetNode.getNodeContainer().getID()
                    + " " + m_targetPortID);

            m_connection = createConnection(wm, m_sourceNode.getNodeContainer().getID(), m_sourcePortID,
                m_targetNode.getNodeContainer().getID(), m_targetPortID);

            if (wraps(wm, WorkflowManager.class)) {
                //only record node stats for a local workflows
                NodeTimer.GLOBAL_TIMER.addConnectionCreation(unwrapNC(m_sourceNode.getNodeContainer()),
                    unwrapNC(m_targetNode.getNodeContainer()));
            }
            if (m_newConnectionUIInfo != null) {
                m_connection.setUIInfo(m_newConnectionUIInfo);
            }
        } catch (Exception e) {
            //LOGGER.error("Connection could not be created.", e);
            m_connection = null;
            m_oldConnection = null;
            m_sourceNode = null;
            m_targetNode = null;
            m_sourcePortID = -1;
            m_targetPortID = -1;
            MessageDialog.openError(SWTUtilities.getActiveShell(),
                    "Connection could not be created",
                    "The two nodes could not be connected due to "
                    + "the following reason:\n " + e.getMessage());

        }
    }

    /**
     * @param confirm initial toggle state
     * @param question of the confirmation dialog (not the toggle), if this is null, this class'
     *            REPLACE_CONNECTION_MESSAGE will be used
     * @return a confirmation dialog
     */
    public static MessageDialogWithToggle openReconnectConfirmDialog(final boolean confirm, final String question) {
        final String questionText = (question != null) ? question : REPLACE_CONNECTION_MESSAGE;
        return MessageDialogWithToggle.openYesNoQuestion(SWTUtilities.getActiveShell(), "Replace Connection?",
            questionText, "Always replace without confirming.", !confirm,
            KNIMEUIPlugin.getDefault().getPreferenceStore(), PreferenceConstants.P_CONFIRM_RECONNECT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undo() {
        WorkflowManagerUI wm = getHostWFMUI();
        try {
            removeConnection(wm, m_connection);
            ConnectionContainerUI old = m_oldConnection;
            if (old != null) {
                createConnection(wm, old.getSource(), old.getSourcePort(), old.getDest(), old.getDestPort(),
                    old.getUIInfo() != null ? old.getUIInfo().getAllBendpoints() : null);
            }
        } catch (OperationNotAllowedException e) {
            MessageDialog.openError(SWTUtilities.getActiveShell(),
                "Connection could not be restored",
                "Connection couldn't be restored (undo) due to "
                + "the following reason:\n " + e.getMessage());
        }
    }

    private static void removeConnection(final WorkflowManagerUI wfm, final ConnectionContainerUI cc)
        throws OperationNotAllowedException {
        ConnectionID[] connId = new ConnectionID[]{cc.getID()};
        wfmAsyncSwitchRethrow(wm -> {
            wm.remove(null, connId, null);
            return null;
        }, wm -> wm.removeAsync(null, connId, null), wfm, "Removing connection ...");
    }

    private static ConnectionContainerUI createConnection(final WorkflowManagerUI wfm, final NodeID source,
        final int sourcePort, final NodeID dest, final int destPort, final int[]... bendpoints)
        throws OperationNotAllowedException {
        return wfmAsyncSwitchRethrow(wm -> wm.addConnection(source, sourcePort, dest, destPort, bendpoints),
            wm -> wm.addConnectionAsync(source, sourcePort, dest, destPort, bendpoints), wfm,
            "Creating connection ...");
    }
}
