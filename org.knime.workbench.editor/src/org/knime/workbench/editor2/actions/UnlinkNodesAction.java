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
 *   Mar 6, 2018 (loki): created
 */
package org.knime.workbench.editor2.actions;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.gef.commands.Command;
import org.eclipse.jface.resource.ImageDescriptor;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.commands.UnlinkNodesCommand;
import org.knime.workbench.editor2.editparts.ConnectableEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.editparts.WorkflowInPortBarEditPart;
import org.knime.workbench.editor2.editparts.WorkflowOutPortBarEditPart;

/**
 * This provides the 'action' for removing the connections between two or more nodes; this only removes
 *  connections between nodes that are in the set. For example if we have:
 *
 *          A>----<B>----<C>
 *            \------<D>---<E
 *
 *  and the selection set is {B, C, D}, the resulting graph after executing this action would look like:
 *
 *          A>----<B>    <C>
 *            \------<D>---<E
 *
 * @author loki der quaeler
 */
public class UnlinkNodesAction extends AbstractLinkNodesAction {
    /** actions registry ID for this action. **/
    public static final String ID = "knime.actions.unlinknodes";
    /** org.eclipse.ui.commands command ID for this action. **/
    private static final String COMMAND_ID = "knime.commands.unlinknodes";


    /**
     * Constructs an instance of <code>UnlinkNodesAction</code>.
     *
     * @param editor The workflow editor which this action will work within
     */
    public UnlinkNodesAction(final WorkflowEditor editor) {
        super(editor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return "Disconnect selected nodes\t" + getHotkey(COMMAND_ID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/unlink_nodes.png");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getDisabledImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/unlink_nodes_disabled.png");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return "Disconnect selected nodes";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Command createCommandForConnectablesInManager(final ConnectableEditPart[] selected,
        final WorkflowManager wm) {
        final Collection<ConnectionContainer> toRemove = findRemovableConnections(selected, false);

        return new UnlinkNodesCommand(toRemove, wm);
    }

    /**
     * {@inheritDoc}
     *
     * @return <code>true</code> if we have:
     *                  • at least two nodes which share a connection
     */
    @Override
    protected boolean canEnableForConnectables(final ConnectableEditPart[] selected) {
        final Collection<ConnectionContainer> toRemove = findRemovableConnections(selected, true);

        return !toRemove.isEmpty();
    }

    /**
     * This method examines the nodes passed in for connections that exist between elements of that set.
     *
     * @param nodes the currently selected nodes
     * @param onlyNeedOne if true, we return as soon as the cardinality of the return instance is non-zero; this is all
     *            we need for the case in which we just want to know whether or not to enable an action, and there's
     *            no reason to saddle that frequently called determination with the O(abyssmal) time here.
     * @return a collection of 0-M ConnectionContainer instances representing connections which should be removed (the
     *         are connections between elements of the set of nodes specified via <code>nodes</code>)
     */
    private Collection<ConnectionContainer> findRemovableConnections(final ConnectableEditPart[] nodes,
        final boolean onlyNeedOne) {
        final WorkflowManager wm = getManager();
        final EditPartConnectionsTuple[] tuples = new EditPartConnectionsTuple[nodes.length];

        for (int i = 0; i < nodes.length; i++) {
            tuples[i] = new EditPartConnectionsTuple(nodes[i], wm);
        }

        // relying on a correct hashCode() implementation in ConnectionContainer to let us use
        //          HashSet to avoid duplicates
        final Collection<ConnectionContainer> removableConnections = new HashSet<>();
        for (int i = 0; i < (tuples.length - 1); i++) {
            final EditPartConnectionsTuple tupleA = tuples[i];

            if (workflowManagerContainsConnectable(tupleA.getEditPart(), wm)) {
                for (int j = (i + 1); j < tuples.length; j++) {
                    final EditPartConnectionsTuple tupleB = tuples[j];

                    if (workflowManagerContainsConnectable(tupleB.getEditPart(), wm)) {
                        removableConnections.addAll(tupleA.sharedConnections(tupleB, onlyNeedOne));

                        if (onlyNeedOne && (removableConnections.size() > 0)) {
                            return removableConnections;
                        }
                    }
                }
            }
        }

        return removableConnections;
    }


    /**
     * This class is a part of the re-architecture centered around handling metanode workflow in and out bars; the
     * underlying problem with them is that they share the same NodeID, and so we need more hand holding in order to
     * determine whether a given existing connection exists to the in bar or the out bar edit part.
     */
    private class EditPartConnectionsTuple {
        final private ConnectableEditPart m_editPart;
        final private NodeID m_nodeId;
        final private boolean m_nodeContainer;
        final private Set<ConnectionContainer> m_incomingConnections;
        final private Set<ConnectionContainer> m_outgoingConnections;

        // Private, but scoped as package just to indicate it will be called from the outer class where as the private
        // scoped functions are scoped private to connote that they will only be called from this inner class.
        EditPartConnectionsTuple(final ConnectableEditPart cep, final WorkflowManager wm) {
            m_editPart = cep;
            m_nodeId = cep.getNodeContainer().getID();
            m_nodeContainer = (cep instanceof NodeContainerEditPart);

            m_incomingConnections = getConnectionsForConnectable(cep, false, wm);
            m_outgoingConnections = getConnectionsForConnectable(cep, true, wm);
        }

        Set<ConnectionContainer> sharedConnections(final EditPartConnectionsTuple other, final boolean onlyNeedOne) {
            final Set<ConnectionContainer> shared = new HashSet<>();

            shared.addAll(sharedConnections(other, true, onlyNeedOne));
            if (onlyNeedOne && (shared.size() > 0)) {
                return shared;
            }

            shared.addAll(sharedConnections(other, false, onlyNeedOne));

            return shared;
        }

        ConnectableEditPart getEditPart() {
            return m_editPart;
        }

        private Set<ConnectionContainer> sharedConnections(final EditPartConnectionsTuple other, final boolean incoming, final boolean onlyNeedOne) {
            final Set<ConnectionContainer> shared = new HashSet<>();
            final Set<ConnectionContainer> connectionsToExamine = incoming ? m_incomingConnections : m_outgoingConnections;

            for (ConnectionContainer cc : connectionsToExamine) {
                boolean match = other.getNodeId().equals(incoming ? cc.getSource() : cc.getDest());

                if (match && !other.isNodeContainer()) {
                    match = (((other.getEditPart() instanceof WorkflowOutPortBarEditPart) && !incoming)
                        || ((other.getEditPart() instanceof WorkflowInPortBarEditPart) && incoming));
                }

                if (match) {
                    shared.add(cc);

                    if (onlyNeedOne) {
                        return shared;
                    }
                }
            }

            return shared;
        }

        private NodeID getNodeId() {
            return m_nodeId;
        }

        private boolean isNodeContainer() {
            return m_nodeContainer;
        }
    }
}
