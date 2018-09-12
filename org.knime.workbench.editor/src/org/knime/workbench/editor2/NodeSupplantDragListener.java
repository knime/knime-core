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
 *   Mar 19, 2018 (loki): created
 */
package org.knime.workbench.editor2;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.ConnectionID;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.WorkflowEvent;
import org.knime.core.node.workflow.WorkflowListener;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.util.SWTUtilities;
import org.knime.workbench.editor2.commands.SupplantationCommand;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 * This exists to support AP-5238; we implement our own "drag listener" as we are warned away from using drag listeners
 * in the code comments for WorkflowGraphicalViewerCreator.createViewer(Composite).
 *
 * TODO we currently ignore whether the dragged node has pre-existing connections (not totally ignore, we'll delete them
 * on the inport if they exist and there is no available port type.. but ignore in the sense of vetoing a drop because
 * the node being dragged doesn't have a free port)
 *
 * @author loki der quaeler
 */
public class NodeSupplantDragListener implements KeyListener, MouseListener, MouseMoveListener, WorkflowListener {
    private static final String CONNECTION_DROP_WARNING =
        "You are altering the existing connection between two nodes; are you sure you want to do this?";

    private static final String NODE_DROP_WARNING =
        "You are replacing an existing node; are you sure you want to do this?";

    /**
     * @param forConnection true if the action is bisecting a connection, false if it is replacing a node
     * @return true if the user's preferences do not require an intervention or if they do but the user ok'd, false
     *         otherwise
     */
    public static boolean replacingNodeOrConnectionBisectionIsAllowed(final boolean forConnection) {
        final IPreferenceStore store = KNIMEUIPlugin.getDefault().getPreferenceStore();
        if (!store.contains(PreferenceConstants.P_CONFIRM_REPLACE)
            || store.getBoolean(PreferenceConstants.P_CONFIRM_REPLACE)) {
            final String msg = forConnection ? CONNECTION_DROP_WARNING : NODE_DROP_WARNING;
            final MessageDialogWithToggle dialog =
                MessageDialogWithToggle.openOkCancelConfirm(SWTUtilities.getActiveShell(), "Confirm ...",
                    msg, "Do not ask again", false, null, null);

            if (dialog.getReturnCode() != IDialogConstants.OK_ID) {
                return false;
            }
            if (dialog.getToggleState()) {
                store.setValue(PreferenceConstants.P_CONFIRM_REPLACE, false);
                KNIMEUIPlugin.getDefault().savePluginPreferences();
            }
        }

        return true;
    }


    // These will only be consulted from the SWT thread
    private NodeContainerEditPart m_nodeInDrag;
    private int[] m_mouseDownNodeBounds;
    private Set<ConnectionID> m_nodeInDragConnectionIds;
    private Set<NodeID> m_nodeInDragDegreeOneNodes;
    private ConnectionManifest m_nodeInDragInportManifest;
    private ConnectionManifest m_nodeInDragOutportManifest;

    private final DragPositionProcessor m_dragPositionProcessor;

    private final WorkflowEditor m_workflowEditor;

    /**
     * @param editor The editor containing the graphical viewer from which we'll track mouse events.
     */
    public NodeSupplantDragListener(final WorkflowEditor editor) {
        if (!editor.getWorkflowManager().isPresent()) {
            //doesn't work with other than the ordinary workflow manager, yet
            m_dragPositionProcessor = null;
            m_workflowEditor = null;
            return;
        }
        final GraphicalViewer viewer = editor.getGraphicalViewer();

        m_workflowEditor = editor;

        m_dragPositionProcessor = new DragPositionProcessor(viewer);

        // We could have done [massive, ugly] lambda functions inline here, but i find it preferable to have
        //      the logic in their own blocks of classes.
        m_dragPositionProcessor.addVetoer(new UnsupportedConnectionSurplantationVetoer());
        m_dragPositionProcessor.addVetoer(new UnsupportedNodeSurplantationVetoer());

        final FigureCanvas fc = (FigureCanvas)viewer.getControl();
        fc.addKeyListener(this);
        fc.addMouseListener(this);
        fc.addMouseMoveListener(this);
    }

    /**
     * This should be called as part of the parent disposal cycle.
     */
    public void dispose() {
        if (m_workflowEditor != null) {
            final FigureCanvas fc = (FigureCanvas)m_workflowEditor.getGraphicalViewer().getControl();

            if (fc != null) {
                fc.removeMouseListener(this);
                fc.removeMouseMoveListener(this);
            }
        }
    }

    private WorkflowManager getManager() {
        return m_workflowEditor.getWorkflowManager().get();
    }

    private void updateManifestsForNodeInDrag() {
        if ((m_nodeInDrag != null) && (m_nodeInDragInportManifest == null)) {
            final WorkflowManager wm = getManager();
            final NodeContainerUI node = m_nodeInDrag.getNodeContainer();

            m_nodeInDragInportManifest = new ConnectionManifest(node, wm, true);
            m_nodeInDragOutportManifest = new ConnectionManifest(node, wm, false);
        }
    }

    private void endDragTracking() {
        if (m_nodeInDrag != null) {
            m_nodeInDrag = null;
            m_mouseDownNodeBounds = null;
            m_nodeInDragConnectionIds = null;
            m_nodeInDragDegreeOneNodes = null;
            m_nodeInDragInportManifest = null;
            m_nodeInDragOutportManifest = null;

            m_dragPositionProcessor.unmarkSelection();
            m_dragPositionProcessor.clearMarkingAvoidance();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void keyPressed(final KeyEvent ke) { }

    /**
     * {@inheritDoc}
     */
    @Override
    public void keyReleased(final KeyEvent ke) {
        if ((m_nodeInDrag != null) && (ke.keyCode == SWT.ESC)) {
            endDragTracking();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseMove(final MouseEvent me) {
        if (m_nodeInDrag != null) {
            final boolean mouseDown = ((me.stateMask & SWT.BUTTON1) != 0);

            if (mouseDown) {
                m_dragPositionProcessor.processDragEventAtPoint(me.display.getCursorLocation());
            } else {
                endDragTracking();  // See AP-9560
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseDoubleClick(final MouseEvent me) {
        // See AP-9560
        // we're going to give everything a handful of milliseconds to settle their stuff out and then end the tracking
        Display.getDefault().asyncExec(() -> {
            endDragTracking();
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseDown(final MouseEvent me) {
        if (m_workflowEditor.getGraphicalViewer().getSelectedEditParts().size() != 1) {
            return;
        }

        m_dragPositionProcessor.processDragEventAtPoint(me.display.getCursorLocation(), false);

        m_nodeInDrag = m_dragPositionProcessor.getNode();

        if (m_nodeInDrag != null) {
            final WorkflowManager wm = getManager();
            final NodeID dragNodeId = m_nodeInDrag.getNodeContainer().getID();
            final Set<ConnectionContainer> incoming = wm.getIncomingConnectionsFor(dragNodeId);

            if (incoming.size() > 0) {
                m_nodeInDrag = null;

                return;
            }

            final Set<ConnectionContainer> outgoing = wm.getOutgoingConnectionsFor(dragNodeId);
            if (outgoing.size() > 0) {
                m_nodeInDrag = null;

                return;
            }

            m_mouseDownNodeBounds = m_nodeInDrag.getNodeContainer().getUIInformation().getBounds();

            m_nodeInDragConnectionIds = new HashSet<>();
            m_nodeInDragDegreeOneNodes = new HashSet<>();
// I am commenting this out (as opposed to nuking it) should the request come back to re-allow drags of nodes with connections
//            for (ConnectionContainer cc : wm.getIncomingConnectionsFor(dragNodeId)) {
//                m_nodeInDragConnectionIds.add(cc.getID());
//                m_nodeInDragDegreeOneNodes.add(cc.getSource());
//            }
//            for (ConnectionContainer cc : wm.getOutgoingConnectionsFor(dragNodeId)) {
//                m_nodeInDragConnectionIds.add(cc.getID());
//                m_nodeInDragDegreeOneNodes.add(cc.getDest());
//            }

            m_dragPositionProcessor.rememberCurrentTargetsToAvoidMarking();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseUp(final MouseEvent me) {
        try {
            // The third condition checked happens in the case of mouse down and mouse up with out mouse move
            //      (otherwise called "selecting a node" :- ) )
            if ((m_nodeInDrag != null) && m_dragPositionProcessor.hasATarget()
                && (!m_nodeInDrag.equals(m_dragPositionProcessor.getNode()))) {

                if (!NodeSupplantDragListener
                    .replacingNodeOrConnectionBisectionIsAllowed(m_dragPositionProcessor.getEdge() != null)) {
                    SupplantationCommand.moveNodeToLocation(m_nodeInDrag, m_mouseDownNodeBounds);

                    return;
                }

                if (m_dragPositionProcessor.getEdge() != null) {
                    final CommandStack cs = (CommandStack)m_workflowEditor.getAdapter(CommandStack.class);
                    final SupplantationCommand command =
                        new SupplantationCommand(m_nodeInDrag, m_mouseDownNodeBounds, m_dragPositionProcessor.getEdge(),
                            m_nodeInDragInportManifest, m_nodeInDragOutportManifest, getManager());

                    cs.execute(command);
                } else if (m_dragPositionProcessor.getNode() != null) { // will always be true as of this writing
                    final CommandStack cs = (CommandStack)m_workflowEditor.getAdapter(CommandStack.class);
                    final SupplantationCommand command =
                        new SupplantationCommand(m_nodeInDrag, m_mouseDownNodeBounds, m_dragPositionProcessor.getNode(),
                            m_nodeInDragInportManifest, m_nodeInDragOutportManifest, getManager());

                    cs.execute(command);
                }
            }
        } finally {
            endDragTracking();
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void workflowChanged(final WorkflowEvent we) {
        switch (we.getType()) {
            case NODE_REMOVED:
                if ((m_nodeInDrag != null) && we.getOldValue() != null) {
                    final Object o = we.getOldValue();

                    if (o instanceof SingleNodeContainer) {
                        final NodeID removedNodeId = ((SingleNodeContainer)o).getID();

                        if ((removedNodeId != null) && removedNodeId.equals(m_nodeInDrag.getNodeContainer().getID())) {
                            Display.getDefault().asyncExec(() -> {
                                endDragTracking();
                            });
                        }
                    }
                }

                break;
            default:
        }
    }


    //
    //
    // Vetoers
    //
    //

   private class UnsupportedConnectionSurplantationVetoer implements DragPositionProcessor.TargetVetoer {
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean shouldVetoTarget(final NodeContainerEditPart potentialNode,
            final ConnectionContainerEditPart potentialEdge) {

            if ((m_nodeInDrag != null) && (potentialEdge != null)) {
                updateManifestsForNodeInDrag();

                return m_nodeInDragConnectionIds.contains(potentialEdge.getModel().getID());
            }

            return false;
        }
    }


    private class UnsupportedNodeSurplantationVetoer implements DragPositionProcessor.TargetVetoer {
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean shouldVetoTarget(final NodeContainerEditPart potentialNode,
            final ConnectionContainerEditPart potentialEdge) {

            if ((m_nodeInDrag != null) && (potentialNode != null)) {
                if (m_nodeInDrag == potentialNode) {
                    return true;
                }

                updateManifestsForNodeInDrag();

                return m_nodeInDragDegreeOneNodes.contains(potentialNode.getNodeContainer().getID());
            }

            return false;
        }
    }
}
