/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Apr 3, 2018 (loki): created
 */
package org.knime.workbench.editor2.commands;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeTimer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.workbench.editor2.actions.LinkNodesAction;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 * This is the Command association to the link nodes Action.
 *
 * @author loki der quaeler
 */
public class LinkNodesCommand extends AbstractKNIMECommand {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(LinkNodesCommand.class);


    private final Collection<LinkNodesAction.PlannedConnection> m_connectionPlans;
    private Collection<ConnectionContainer> m_removedConnections;

    private boolean m_confirmConnectionReplacement;

    /**
     * The constructor for this class.
     *
     * @param plans a non-null collection of 0 or more <code>PlannedConnection</code> instances defining the linking to
     *            be performed.
     * @param wm the <code>WorkflowManager</code> governing the workflow in which the targets exist.
     */
    public LinkNodesCommand(final Collection<LinkNodesAction.PlannedConnection> plans, final WorkflowManager wm) {
        super(wm);

        m_connectionPlans = plans;
        m_confirmConnectionReplacement =
            KNIMEUIPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.P_CONFIRM_RECONNECT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canExecute() {
        if (!super.canExecute()) {
            return false;
        }

        return !m_connectionPlans.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        final WorkflowManager wm = getHostWFM();

        if (m_confirmConnectionReplacement) {
            boolean willReplaceAtLeastOneAlreadyExecutedIncoming = false;

            for (final LinkNodesAction.PlannedConnection planAction : m_connectionPlans) {
                if (planAction.getDestinationNode().getNodeContainer().getNodeContainerState().isExecuted()) {
                    willReplaceAtLeastOneAlreadyExecutedIncoming = true;

                    break;
                }
            }

            if (willReplaceAtLeastOneAlreadyExecutedIncoming) {
                MessageDialogWithToggle messageDialog =
                    CreateConnectionCommand.openReconnectConfirmDialog(m_confirmConnectionReplacement, null);

                if (messageDialog.getReturnCode() != IDialogConstants.YES_ID) {
                    return;
                }
            }
        }

        m_removedConnections = new ArrayList<>();
        for (final LinkNodesAction.PlannedConnection planAction : m_connectionPlans) {
            final NodeContainerUI sourceUI = planAction.getSourceNode().getNodeContainer();
            final NodeID sourceNodeID = sourceUI.getID();
            final NodeContainerUI destinationUI = planAction.getDestinationNode().getNodeContainer();
            final NodeID destinationNodeID = destinationUI.getID();

            if (planAction.shouldDetachDestinationFirst()) {
                final ConnectionContainer cc =
                    wm.getIncomingConnectionFor(destinationNodeID, planAction.getDestinationInportIndex());

                try {
                    wm.removeConnection(cc);

                    m_removedConnections.add(cc);
                } catch (Exception e) {
                    LOGGER.error("Could not delete existing inport connection for " + destinationNodeID + ":"
                        + planAction.getDestinationInportIndex() + "; skipping new connection task from " + sourceNodeID
                        + ":" + planAction.getSourceOutportIndex() + " to " + destinationNodeID + ":"
                        + planAction.getDestinationInportIndex() + " due to: " + e.getMessage(), e);
                    continue;
                }
            }

            try {
                wm.addConnection(sourceNodeID, planAction.getSourceOutportIndex(), destinationNodeID,
                    planAction.getDestinationInportIndex());

                NodeTimer.GLOBAL_TIMER.addConnectionCreation(Wrapper.unwrapNC(sourceUI),
                    Wrapper.unwrapNC(destinationUI));
            } catch (Exception e) {
                LOGGER.error("Failed to connect " + sourceNodeID + ":" + planAction.getSourceOutportIndex() + " to "
                    + destinationNodeID + ":" + planAction.getDestinationInportIndex() + " due to: " + e.getMessage(),
                    e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undo() {
        final WorkflowManager wm = getHostWFM();

        for (final LinkNodesAction.PlannedConnection planAction : m_connectionPlans) {
            final NodeID destinationNodeID = planAction.getDestinationNode().getNodeContainer().getID();
            final ConnectionContainer cc =
                    wm.getIncomingConnectionFor(destinationNodeID, planAction.getDestinationInportIndex());

            try {
                wm.removeConnection(cc);
            } catch (Exception e) {
                LOGGER.error("Could not remove connection from " + cc.getSource() + ":" + cc.getSourcePort()
                    + " to " + cc.getDest() + ":" + cc.getDestPort() + " due to: " + e.getMessage(), e);
            }
        }

        for (final ConnectionContainer cc : m_removedConnections) {
            try {
                final ConnectionContainer restoredCC =
                    wm.addConnection(cc.getSource(), cc.getSourcePort(), cc.getDest(), cc.getDestPort());

                restoredCC.setUIInfo(cc.getUIInfo());
            } catch (Exception e) {
                LOGGER.error("Could not restore connection from " + cc.getSource() + ":" + cc.getSourcePort()
                    + " to " + cc.getDest() + ":" + cc.getDestPort() + " due to: " + e.getMessage(), e);
            }
        }
    }
}
