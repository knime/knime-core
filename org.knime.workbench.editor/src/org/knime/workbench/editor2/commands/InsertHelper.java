/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   25.03.2015 (tibuch): created
 */
package org.knime.workbench.editor2.commands;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 *
 * @author Tim-Oliver Buchholz, KNIME.com AG, Zurich, Switzerland
 */
public class InsertHelper {

    private ConnectionContainer m_edge;
    private WorkflowManager hostWFM;

    /**
     * @param wfm the workflow manager
     * @param edge on which the new node will be inserted
     */
    public InsertHelper(final WorkflowManager wfm, final ConnectionContainer edge) {
        hostWFM = wfm;
        m_edge = edge;
    }

    /**
     * Checks execution status of downstream nodes and pops up reset warning if enabled.
     *
     * @param askForReset <code>true</code> if the user should be asked whether executed nodes that are affected by the
     *            insertion are allowed to be reset. If <code>false</code> this question is omitted.
     * @return if new node can be inserted
     */
    public boolean canInsertNode(final boolean askForReset) {
        NodeID destNode = m_edge.getDest();
        if (!hostWFM.canRemoveConnection(m_edge)) {
            return false;
        }
        boolean isDestinationExecuted;
        if (destNode.equals(hostWFM.getID())) {
            // TODO missing method in WFM: hasExecutedDownstreamNode(ConnectionContainer)
            isDestinationExecuted = false;
        } else {
            isDestinationExecuted = hostWFM.findNodeContainer(destNode).getNodeContainerState().isExecuted();
        }
        if (isDestinationExecuted && askForReset) {
            IPreferenceStore store = KNIMEUIPlugin.getDefault().getPreferenceStore();
            if (!store.contains(PreferenceConstants.P_CONFIRM_RESET)
                    || store.getBoolean(PreferenceConstants.P_CONFIRM_RESET)) {
                MessageDialogWithToggle dialog =
                    MessageDialogWithToggle.openOkCancelConfirm(
                        Display.getDefault().getActiveShell(),
                        "Confirm reset...",
                        "Do you really want to reset all downstream node(s) ?",
                        "Do not ask again", false, null, null);
                if (dialog.getReturnCode() != IDialogConstants.OK_ID) {
                    return false;
                }
                if (dialog.getToggleState()) {
                    store.setValue(PreferenceConstants.P_CONFIRM_RESET, false);
                    KNIMEUIPlugin.getDefault().savePluginPreferences();
                }
            }
        }
        return true;
    }

    /**
     * Guesses the incoming/outgoing connections of the new node based on the connections of the old node.
     * The connections are added from port 0 to port n. If no port type matches no connection is added.
     * @param container of the new created node
     * @param snapToGrid should new node snap to grid
     * @param x coordinate of the new node
     * @param y coordinate of the new node
     */
    public void reconnect(final NodeContainer container, final boolean snapToGrid, final int x, final int y) {

        // reset node position
        NodeUIInformation info = new NodeUIInformation(x, y, -1, -1, false);
        info.setSnapToGrid(snapToGrid);
        info.setIsDropLocation(false);
        container.setUIInformation(info);

        int p = (container instanceof WorkflowManager) ? 0 : 1; //skip fv port of nodes for now
        while (!hostWFM.canAddConnection(m_edge.getSource(), m_edge.getSourcePort(), container.getID(), p)) {
            // search for a valid connection of the source node to this node
            p++;
            if (p >= container.getNrInPorts()) {
                break;
            }
        }
        if (p < container.getNrInPorts()) {
            hostWFM.addConnection(m_edge.getSource(), m_edge.getSourcePort(), container.getID(), p);
        } else if (!(container instanceof WorkflowManager)) {
            // try if the fv port of nodes would work
            if (hostWFM.canAddConnection(m_edge.getSource(), m_edge.getSourcePort(), container.getID(), 0)) {
                hostWFM.addConnection(m_edge.getSource(), m_edge.getSourcePort(), container.getID(), 0);
            }
        }

        int pout = (container instanceof WorkflowManager) ? 0 : 1; //skip fv port of nodes for now;
        while (!hostWFM.canAddConnection(container.getID(), pout, m_edge.getDest(), m_edge.getDestPort())) {
            // search for a valid connection of this node to the destination node of the edge
            pout++;
            if (pout >= container.getNrOutPorts()) {
                break;
            }
        }
        if (pout < container.getNrOutPorts()) {
            hostWFM.addConnection(container.getID(), pout, m_edge.getDest(), m_edge.getDestPort());
        } else if (!(container instanceof WorkflowManager)) {
            // try if the fv port of nodes would work
            if (hostWFM.canAddConnection(container.getID(), 0, m_edge.getDest(), m_edge.getDestPort())) {
                hostWFM.addConnection(container.getID(), 0, m_edge.getDest(), m_edge.getDestPort());
            }
        }
    }
}
