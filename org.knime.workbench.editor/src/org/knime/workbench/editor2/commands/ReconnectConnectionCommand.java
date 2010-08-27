/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 * -------------------------------------------------------------------
 * 
 * History
 *   24.02.2006 (Christoph Sieb): created
 */
package org.knime.workbench.editor2.commands;

import java.util.Collections;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.commands.Command;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.editparts.AbstractPortEditPart;
import org.knime.workbench.editor2.editparts.ConnectableEditPart;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;
import org.knime.workbench.editor2.editparts.MetaNodeOutPortEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.editparts.NodeInPortEditPart;
import org.knime.workbench.editor2.editparts.NodeOutPortEditPart;
import org.knime.workbench.editor2.editparts.WorkflowInPortEditPart;
import org.knime.workbench.editor2.editparts.WorkflowOutPortEditPart;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 * Command that reconnects a connection. This basically corresponds to remove
 * the old connection edit part and insert a new one. Therefore this command
 * encapsulates the delete and create connection command.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class ReconnectConnectionCommand extends Command {
    
//    private static final NodeLogger LOGGER = NodeLogger.getLogger(
//            ReconnectConnectionCommand.class); 
    
    private DeleteCommand m_deleteCommand;

    private CreateConnectionCommand m_createCommand;
    
    private final WorkflowManager m_manager;
    
    private final NodeID m_oldTarget;
    
    private final NodeID m_newTarget;
    
    private boolean m_confirm;
    
    private boolean m_identical = false;

    /**
     * Creates a reconnection command encapsulating a delete and create command.
     * 
     * @param connection The edit part of the connection to delete
     * @param host the source (NodeOutPortEditPart - normaly) of the connection
     * @param target the target edit part
     */
    public ReconnectConnectionCommand(
            final ConnectionContainerEditPart connection,
            final AbstractPortEditPart host,
            final AbstractPortEditPart target) {
        
        m_confirm = KNIMEUIPlugin.getDefault().getPreferenceStore()
            .getBoolean(PreferenceConstants.P_CONFIRM_RECONNECT);

        // get the source node
        
        EditPart hostParent = host.getParent();
        ConnectableEditPart srcEP = (ConnectableEditPart)hostParent;
        // get the responsible workflow manager
        if (srcEP instanceof NodeContainerEditPart) {
            m_manager = srcEP.getNodeContainer().getParent();
        } else {
            // srcEP instanceof WorkflowInPortEditPart
            m_manager = (WorkflowManager)srcEP.getNodeContainer();
        }
        
        // create the delete command
        m_deleteCommand = 
            new DeleteCommand(Collections.singleton(connection), m_manager);
        
        ConnectionContainer oldConnection = connection.getModel(); 
        m_oldTarget = oldConnection.getDest();
        
        m_newTarget = target.getID();
        
        if (m_oldTarget.equals(m_newTarget)) {
            if (oldConnection.getDestPort() == target.getIndex()) {
                m_identical = true;
            }
        }

        // create the create command
        CreateConnectionCommand cmd = new CreateConnectionCommand();
        cmd.setNewConnectionUIInfo(oldConnection.getUIInfo());

        if (host instanceof NodeOutPortEditPart 
                    || host instanceof WorkflowInPortEditPart
                    || host instanceof MetaNodeOutPortEditPart) {
            cmd.setSourceNode(srcEP);
            cmd.setSourcePortID(host.getIndex());
            cmd.setStartedOnOutPort(true);
            // we need the manager to execute the command
            cmd.setManager(m_manager);
        } else {
            return;
        }
        if (target instanceof NodeInPortEditPart 
                || target instanceof WorkflowOutPortEditPart) {
            cmd.setTargetPortID(target.getIndex());
            cmd.setTargetNode((ConnectableEditPart)target.getParent());
        } else {
            return;
        }

        m_createCommand = cmd;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canExecute() {
        return !m_identical 
            && m_deleteCommand.canExecute() 
            && m_createCommand.canExecute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        // confirm replacement?
        // check if old target node was executed 
        NodeContainer oldTarget = m_manager.getNodeContainer(m_oldTarget);
        NodeContainer newTarget = m_manager.getNodeContainer(m_newTarget);
        if (oldTarget == null || newTarget == null) {
            // something is very wrong here
            return;
        }
        boolean oldExecuted = oldTarget.getState().equals(
                NodeContainer.State.EXECUTED);
        boolean newExecuted = newTarget.getState().equals(
                NodeContainer.State.EXECUTED);
        // or new target node is executed
        if (m_confirm && (oldExecuted || newExecuted)) {
            // create comprehensible and correct confirmation message
            StringBuffer message = new StringBuffer(
                    "Do you want to delete the existing connection? \n");
            if (oldExecuted || newExecuted) {
                message.append("This will reset ");
                if (oldExecuted) {
                    message.append("the current destination node");
                }
                if (oldExecuted && newExecuted) {
                    message.append(" and");
                }
                if (newExecuted) {
                    message.append(" the new target node");
                }
                message.append("!");
            }
            MessageDialogWithToggle msgD = CreateConnectionCommand
                .openReconnectConfirmDialog(m_confirm,
                        message.toString());
            m_confirm = !msgD.getToggleState();
            if (msgD.getReturnCode() != IDialogConstants.YES_ID) {
                return;
            }
        }

        // execute both commands
        m_deleteCommand.execute();
        m_createCommand.setConfirm(false);
        m_createCommand.execute();
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean canUndo() {
        return m_createCommand.canExecute() && m_deleteCommand.canUndo();
    }
    
    /** {@inheritDoc} */
    @Override
    public void undo() {
        m_createCommand.undo();
        m_deleteCommand.undo();
    }
}
