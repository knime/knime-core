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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.resource.ImageDescriptor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

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
 */
public class UnlinkNodesAction extends AbstractNodeAction {

    /** actions registry ID for this action. **/
    static public final String ID = "knime.actions.unlinknodes";
    /** org.eclipse.ui.commands command ID for this action. **/
    static private final String COMMAND_ID = "knime.commands.unlinknodes";

    static private final NodeLogger LOGGER = NodeLogger.getLogger(UnlinkNodesAction.class);


    /**
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
        return "Unlink selected nodes\t" + this.getHotkey(COMMAND_ID);
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
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID,
                                                 "icons/unlink_nodes_disabled.png");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return "Unlink selected nodes";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        final WorkflowManager wm = this.getManager();
        final Collection<ConnectionContainer> toRemove = this.findRemoveableConnections(nodeParts);

        for (final ConnectionContainer cc : toRemove) {
            try {
                wm.removeConnection(cc);
            }
            catch (Exception e) {
                LOGGER.error("Could not delete existing connection from " + cc.getSource()
                                    + ":" + cc.getSourcePort() + " to " + cc.getDest() + ":"
                                    + cc.getDestPort());
            }
        }
    }

    /**
     * @return <code>true</code> if we have:
     *                  • at least two nodes which share a connection
     * @see org.eclipse.gef.ui.actions.WorkbenchPartAction#calculateEnabled()
     */
    @Override
    protected boolean internalCalculateEnabled() {
        final NodeContainerEditPart[] selected = this.getSelectedParts(NodeContainerEditPart.class);

        if (selected.length < 2) {
            return false;
        }

        final Collection<ConnectionContainer> toRemove = this.findRemoveableConnections(selected);

        return (toRemove.size() > 0);
    }

    /**
     * @param nodes
     * @return a collection of 0-M ConnectionContainer instances representing connections which should
     *              be removed (the are connections between elements of the set of nodes specified
     *              via <code>nodes</code>
     */
    protected Collection<ConnectionContainer> findRemoveableConnections(final NodeContainerEditPart[] nodes) {
        final WorkflowManager wm = this.getManager();
        // relying on a correct hashCode() implementation in ConnectionContainer to let us use
        //          HashSet to avoid duplicates
        final Collection<ConnectionContainer> rhett = new HashSet<>();
        final HashSet<NodeID> nodeIdSet = new HashSet<>();
        Set<ConnectionContainer> connections;

        Arrays.asList(nodes).forEach((node) -> {
            nodeIdSet.add(node.getNodeContainer().getID());
        });

        for (final NodeContainerEditPart node : nodes) {
            // We should only need check incoming *or* outgoing, since members of the set which connect to
            //      other members of the set will each have references to themselves defined in both.
            connections = wm.getIncomingConnectionsFor(node.getNodeContainer().getID());
            connections.forEach((connection) -> {
                if (nodeIdSet.contains(connection.getSource()) && nodeIdSet.contains(connection.getDest())) {
                    rhett.add(connection);
                }
            });
        }

        return rhett;
    }

}
