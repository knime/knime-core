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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.knime.core.api.node.workflow.IConnectionContainer;
import org.knime.core.api.node.workflow.NodeUIInformation;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 *
 * @author Tim-Oliver Buchholz, KNIME.com AG, Zurich, Switzerland
 */
public class ReplaceHelper {
    private static final Comparator<IConnectionContainer> DEST_PORT_SORTER = new Comparator<IConnectionContainer>() {
        @Override
        public int compare(final IConnectionContainer o1, final IConnectionContainer o2) {
            return Integer.compare(o1.getDestPort(), o2.getDestPort());
        }
    };

    private static final Comparator<IConnectionContainer> SOURCE_PORT_SORTER = new Comparator<IConnectionContainer>() {
        @Override
        public int compare(final IConnectionContainer o1, final IConnectionContainer o2) {
            return Integer.compare(o1.getSourcePort(), o2.getSourcePort());
        }
    };

    private WorkflowManager m_wfm;

    private ArrayList<IConnectionContainer> m_incomingConnections;

    private ArrayList<IConnectionContainer> m_outgoingConnections;

    private NodeContainer m_oldNode;



    /**
     * @param wfm the workflow manager
     * @param oldNode the node which was replaced
     */
    public ReplaceHelper(final WorkflowManager wfm, final NodeContainer oldNode) {
        m_wfm = wfm;
        m_oldNode = oldNode;
        m_incomingConnections = new ArrayList<>(m_wfm.getIncomingConnectionsFor(m_oldNode.getID()));
        m_outgoingConnections = new ArrayList<>(m_wfm.getOutgoingConnectionsFor(m_oldNode.getID()));

        // sort according to ports
        Collections.sort(m_incomingConnections, DEST_PORT_SORTER);
        Collections.sort(m_outgoingConnections, SOURCE_PORT_SORTER);

    }

    /**
     * Checks execution status of downstream nodes and pops up reset warning if enabled.
     *
     * @return if new node can be replaced
     */
    public boolean replaceNode() {
        boolean hasExecutedSuccessor = false;
        for (IConnectionContainer connectionContainer : m_outgoingConnections) {
            hasExecutedSuccessor =
                hasExecutedSuccessor
                    || m_wfm.findNodeContainer(connectionContainer.getDest()).getNodeContainerState().isExecuted()
                    || !m_wfm.canRemoveNode(m_oldNode.getID());
        }

        if (hasExecutedSuccessor) {
            IPreferenceStore store = KNIMEUIPlugin.getDefault().getPreferenceStore();
            if (!store.contains(PreferenceConstants.P_CONFIRM_RESET)
                || store.getBoolean(PreferenceConstants.P_CONFIRM_RESET)) {
                MessageDialogWithToggle dialog =
                    MessageDialogWithToggle.openOkCancelConfirm(Display.getDefault().getActiveShell(),
                        "Confirm reset...", "Do you really want to reset all downstream node(s) ?", "Do not ask again",
                        false, null, null);
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
     * Connects new node with connection of the old node.
     *
     * @param container new node container
     */
    public void reconnect(final NodeContainer container) {

        // reset node location
        NodeUIInformation uiInformation = m_oldNode.getUIInformation();
        int[] bounds = uiInformation.getBounds();
        NodeUIInformation info = NodeUIInformation.builder()
                .setNodeLocation(bounds[0], bounds[1], -1, -1)
                .setHasAbsoluteCoordinates(true)
                .setSnapToGrid(uiInformation.getSnapToGrid())
                .setIsDropLocation(false).build();
        container.setUIInformation(info);

        int inShift;
        int outShift;

        if (m_oldNode instanceof WorkflowManager && !(container instanceof WorkflowManager)) {
            inShift = 0;
            // replacing a metanode (no opt. flow var ports) with a "normal" node (that has optional flow var ports)
            if (m_oldNode.getNrInPorts() > 0 && container.getNrInPorts() > 1) {
                // shift ports one index - unless we need to use the invisible optional flow var port of new node
                if (!m_oldNode.getInPort(0).getPortType().equals(FlowVariablePortObject.TYPE)) {
                    inShift = 1;
                } else if (container.getInPort(1).getPortType().equals(FlowVariablePortObject.TYPE)) {
                    inShift = 1;
                }
            }

            outShift = 0;
            if (m_oldNode.getNrOutPorts() > 0 && container.getNrOutPorts() > 1) {
                if (!m_oldNode.getOutPort(0).getPortType().equals(FlowVariablePortObject.TYPE)) {
                    outShift = 1;
                } else if (container.getOutPort(1).getPortType().equals(FlowVariablePortObject.TYPE)) {
                    outShift = 1;
                }
            }
        } else if (!(m_oldNode instanceof WorkflowManager) && container instanceof WorkflowManager) {
            // replacing a "normal" node with a metanode
            inShift = -1;
            for (IConnectionContainer cc : m_incomingConnections) {
                if (cc.getDestPort() == 0) {
                    inShift = 0;
                    break;
                }
            }

            outShift = -1;
            for (IConnectionContainer cc : m_outgoingConnections) {
                if (cc.getSourcePort() == 0) {
                    outShift = 0;
                    break;
                }
            }
        } else {
            inShift = 0;
            outShift = 0;
        }

        // set incoming connections
        NodeID newId = container.getID();
        for (IConnectionContainer c : m_incomingConnections) {
            if (m_wfm.canAddConnection(c.getSource(), c.getSourcePort(), newId, c.getDestPort() + inShift)) {
                m_wfm.addConnection(c.getSource(), c.getSourcePort(), newId, c.getDestPort() + inShift);
            } else {
                break;
            }
        }

        // set outgoing connections
        for (IConnectionContainer c : m_outgoingConnections) {
            if (m_wfm
                .canAddConnection(newId, c.getSourcePort() + outShift, c.getDest(), c.getDestPort())) {
                m_wfm.addConnection(newId, c.getSourcePort() + outShift, c.getDest(), c.getDestPort());
            } else {
                break;
            }
        }
    }
}
