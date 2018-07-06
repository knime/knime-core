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
 *   Jul 5, 2018 (loki): created
 */
package org.knime.workbench.editor2.actions;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.eclipse.gef.commands.Command;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowLock;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.node.workflow.ConnectionContainerUI;
import org.knime.core.ui.wrapper.WorkflowManagerWrapper;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.ConnectableEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.editparts.WorkflowInPortBarEditPart;
import org.knime.workbench.editor2.editparts.WorkflowInPortEditPart;
import org.knime.workbench.editor2.editparts.WorkflowOutPortBarEditPart;
import org.knime.workbench.editor2.editparts.WorkflowOutPortEditPart;

/**
 * Due to functionality expansion for AP-9581 there was *just* enough commonality to make me feel like both Link and
 * Unlink Nodes actions should share a common superclass; this is that.
 *
 * @author loki der quaeler
 */
abstract class AbstractLinkNodesAction extends AbstractNodeAction {
    private static final Set<ConnectionContainer> EMPTY_CONTAINER_SET =
        Collections.unmodifiableSet(new HashSet<ConnectionContainer>());

    /**
     * Given the connectable edit part and whether the consumer is interested in source connections (aka outport
     * connections) or destination connections (aka inport connections), return a Set of ConnectionContainer instances.
     * This code accounts for the different manner in which we fetch this for Workflow Bars and Node Containers.
     *
     * @param cep a connectable edit part for which we want to know connection information
     * @param sourceConnections if true, source connections for the cep will be returned, else destination connections
     * @param wm the instance of WorkflowManager containing this connectable edit part
     * @return a Set of ConnectionContainer instances if cep is an instance of NodeContainerEditPart,
     *         WorkflowInPortBarEditPart, WorkflowOutPortBarEditPart which may or may not be empty, or an empty set
     *         should cep be an instance of none of those
     */
    static Set<ConnectionContainer> getConnectionsForConnectable(final ConnectableEditPart cep,
        final boolean sourceConnections, final WorkflowManager wm) {
        if ((cep instanceof WorkflowInPortBarEditPart) && sourceConnections) {
            final WorkflowInPortBarEditPart ipbep = (WorkflowInPortBarEditPart)cep;

            if (ipbep.getChildren().size() == 1) {
                final WorkflowInPortEditPart editPart = (WorkflowInPortEditPart)ipbep.getChildren().get(0);
                final List<ConnectionContainerUI> ccUIs = editPart.getModelSourceConnections();
                final Set<ConnectionContainer> connectionContainers = new HashSet<>();

                for (ConnectionContainerUI ccUI : ccUIs) {
                    connectionContainers.add(wm.getConnection(ccUI.getID()));
                }

                return connectionContainers;
            }
        } else if ((cep instanceof WorkflowOutPortBarEditPart) && (!sourceConnections)) {
            final WorkflowOutPortBarEditPart opbep = (WorkflowOutPortBarEditPart)cep;

            if (opbep.getChildren().size() == 1) {
                final WorkflowOutPortEditPart editPart = (WorkflowOutPortEditPart)opbep.getChildren().get(0);
                final List<ConnectionContainerUI> ccUIs = editPart.getModelTargetConnections();
                final Set<ConnectionContainer> connectionContainers = new HashSet<>();

                for (ConnectionContainerUI ccUI : ccUIs) {
                    connectionContainers.add(wm.getConnection(ccUI.getID()));
                }

                return connectionContainers;
            }
        } else if (cep instanceof NodeContainerEditPart) {
            NodeID nid = ((NodeContainerEditPart)cep).getNodeContainer().getID();

            return sourceConnections ? wm.getOutgoingConnectionsFor(nid) : wm.getIncomingConnectionsFor(nid);
        }

        return EMPTY_CONTAINER_SET;
    }

    /**
     * We cannot rely upon WorkflowManager.containsNodeContainer because this will return false, in our situation, for
     * workflow in and out bar edit parts. This method returns an appropriate check depending of which concrete class
     * <code>cep</code> is an instance.
     *
     * @param cep the connectable edit part in question
     * @param wm the workflow potentially containing the edit part
     * @return true if the cep is connectable in the specified workflow
     */
    static boolean workflowManagerContainsConnectable(final ConnectableEditPart cep,
        final WorkflowManager wm) {
        if (cep instanceof WorkflowInPortBarEditPart) {
            final WorkflowInPortBarEditPart ipbep = (WorkflowInPortBarEditPart)cep;
            WorkflowManagerWrapper wmw = (WorkflowManagerWrapper)ipbep.getNodeContainer();

            return wm.equals(wmw.unwrap());
        } else if (cep instanceof WorkflowOutPortBarEditPart) {
            final WorkflowOutPortBarEditPart opbep = (WorkflowOutPortBarEditPart)cep;
            WorkflowManagerWrapper wmw = (WorkflowManagerWrapper)opbep.getNodeContainer();

            return wm.equals(wmw.unwrap());
        } else if (cep instanceof NodeContainerEditPart) {
            NodeID nid = ((NodeContainerEditPart)cep).getNodeContainer().getID();

            return wm.containsNodeContainer(nid);
        }

        return false;
    }


    /**
     * @param editor The workflow editor which this action will work within
     */
    protected AbstractLinkNodesAction(final WorkflowEditor editor) {
        super(editor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runInSWT() {
        final ConnectableEditPart[] selected = getSelectedConnectables(ConnectableEditPart.class);
        final WorkflowManager wm = getManager();

        try (WorkflowLock lock = wm.lock()) {
            execute(createCommandForConnectablesInManager(selected, wm));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        throw new IllegalStateException("This method should not be called.");
    }

    /**
     * {@inheritDoc}
     *
     * @see org.eclipse.gef.ui.actions.WorkbenchPartAction#calculateEnabled()
     */
    @Override
    protected boolean internalCalculateEnabled() {
        final ConnectableEditPart[] selected = getSelectedConnectables(ConnectableEditPart.class);

        if (selected.length < 2) {
            return false;
        }

        final WorkflowManager wm = getManager();
        final Predicate<ConnectableEditPart> p = (cep) -> {
            return workflowManagerContainsConnectable(cep, wm);
        };
        try (WorkflowLock lock = wm.lock()) {
            // disable if there are no nodes that are (no longer) contained in the workflow
            // this is a very common case when nodes get deleted and actions in the toolbar are
            // updated in response to that
            if (!Arrays.stream(selected).anyMatch(p)) {
                return false;
            }

            return canEnableForConnectables(selected);
        }
    }

    /**
     * Subclasses must implement this to return the Command instance to execute.
     *
     * @param selected the currently selected connectable edit parts in the workflow
     * @param wm the current workflow manager
     * @return an instance of Command which will get executed
     */
    protected abstract Command createCommandForConnectablesInManager(final ConnectableEditPart[] selected,
        final WorkflowManager wm);

    /**
     * Subclasses must implement this to return true or false dictating whether the action should be enabled or not
     *
     * @param selected the currently selected connectable edit parts in the workflow
     * @return true of false dictating whether the action should be enabled or not
     */
    protected abstract boolean canEnableForConnectables(final ConnectableEditPart[] selected);
}
