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
 * -------------------------------------------------------------------
 *
 * History
 *   04.02.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.util.TransferDropTargetListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.internal.Workbench;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowCopyContent;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.workbench.editor2.actions.CreateSpaceAction;
import org.knime.workbench.editor2.actions.CreateSpaceAction.CreateSpaceDirection;
import org.knime.workbench.editor2.commands.CreateMetaNodeCommand;
import org.knime.workbench.editor2.commands.CreateNodeCommand;
import org.knime.workbench.editor2.commands.InsertMetaNodeCommand;
import org.knime.workbench.editor2.commands.InsertNewNodeCommand;
import org.knime.workbench.editor2.commands.ReplaceMetaNodeCommand;
import org.knime.workbench.editor2.commands.ReplaceNodeCommand;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.editparts.WorkflowInPortBarEditPart;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;
import org.knime.workbench.editor2.figures.ProgressPolylineConnection;
import org.knime.workbench.repository.NodeUsageRegistry;
import org.knime.workbench.repository.RepositoryFactory;
import org.knime.workbench.repository.model.AbstractNodeTemplate;
import org.knime.workbench.repository.model.MetaNodeTemplate;
import org.knime.workbench.repository.model.NodeTemplate;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class NodeTemplateDropTargetListener2 implements TransferDropTargetListener {

    /**
     *
     */
    private static final int MINIMUM_NODE_DISTANCE = 200;

    /**
     *
     */
    private static final Color RED = new Color(null, 255, 0, 0);

    private static final NodeLogger LOGGER = NodeLogger.getLogger(NodeTemplateDropTargetListener2.class);

    private final EditPartViewer m_viewer;

    private NodeContainerEditPart m_markedNode;

    private ConnectionContainerEditPart m_markedEdge;

    private int nodeCount;

    private int edgeCount;

    private NodeContainerEditPart node;

    private ConnectionContainerEditPart edge;

    private Color m_edgeColor;

    public NodeTemplateDropTargetListener2(final EditPartViewer viewer) {
        m_viewer = viewer;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Transfer getTransfer() {
        return LocalSelectionTransfer.getTransfer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled(final DropTargetEvent event) {
        AbstractNodeTemplate snt = getSelectionNodeTemplate();
        if (snt != null) {
            event.feedback = DND.FEEDBACK_SELECT;
            event.operations = DND.DROP_COPY;
            event.detail = DND.DROP_COPY;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dragEnter(final DropTargetEvent event) {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dragLeave(final DropTargetEvent event) {
        // do nothing
    }

    /**
     *
     * @param event drop target event containing the position (relative to whole display)
     * @return point converted to the editor coordinates
     */
    protected Point getDropLocation(final DropTargetEvent event) {
        /* NB: don't break in this method - it ruins the cursor location! */
        event.x = event.display.getCursorLocation().x;
        event.y = event.display.getCursorLocation().y;
        Point p =
            new Point(m_viewer.getControl().toControl(event.x, event.y).x, m_viewer.getControl().toControl(event.x,
                event.y).y);
        return p;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dragOperationChanged(final DropTargetEvent event) {
        // do nothing -> all is handled during "drop"
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dragOver(final DropTargetEvent event) {
        WorkflowManager wfm = ((WorkflowRootEditPart)m_viewer.getRootEditPart().getContents()).getWorkflowManager();
        node = null;
        edge = null;
        nodeCount = 0;
        edgeCount = 0;

        // edge-/nodedist
        double edgedist = Integer.MAX_VALUE;
        double nodedist = Integer.MAX_VALUE;
        // hitbox size: (-8 to 8 = 16) * (-8 to 8 = 16)
        for (int i = -8; i < 9; i++) {
            for (int j = -8; j < 9; j++) {
                Point dropLocation = getDropLocation(event);
                EditPart ep = m_viewer.findObjectAt(dropLocation.getTranslated(i, j));
                if (ep instanceof NodeContainerEditPart) {
                    double temp = dropLocation.getDistance(dropLocation.getTranslated(i, j));
                    // choose nearest node to mouse position
                    if (nodedist >= temp) {
                        node = (NodeContainerEditPart)ep;
                        nodedist = temp;
                    }
                    nodeCount++;
                } else if (ep instanceof ConnectionContainerEditPart) {
                    double temp = dropLocation.getDistance(dropLocation.getTranslated(i, j));
                    // choose nearest edge to mouse-position
                    if (edgedist >= temp) {
                        edge = (ConnectionContainerEditPart)ep;
                        edgedist = temp;
                    }
                    edgeCount++;
                }
            }
        }

        unmark(wfm);

        if (node != null && nodeCount >= edgeCount) {
            m_markedNode = node;
            m_markedNode.mark();
            // workaround for eclipse bug 393868 (https://bugs.eclipse.org/bugs/show_bug.cgi?id=393868)
            WindowsDNDHelper.hideDragImage();
        } else if (edge != null) {
            m_edgeColor = edge.getFigure().getForegroundColor();
            m_markedEdge = edge;
            ((ProgressPolylineConnection)m_markedEdge.getFigure()).setLineWidth(3);
            m_markedEdge.getFigure().setForegroundColor(RED);

            // workaround for eclipse bug 393868 (https://bugs.eclipse.org/bugs/show_bug.cgi?id=393868)
            WindowsDNDHelper.hideDragImage();
        }
    }

    /**
     * Unmark node and edge.
     *
     * @param wfm the workflow manager
     */
    private void unmark(final WorkflowManager wfm) {
        if (m_markedNode != null) {
            m_markedNode.unmark();
            m_markedNode = null;

            // workaround for eclipse bug 393868 (https://bugs.eclipse.org/bugs/show_bug.cgi?id=393868)
            WindowsDNDHelper.showDragImage();
        }

        if (m_markedEdge != null) {
            m_markedEdge.getFigure().setForegroundColor(m_edgeColor);
            ((ProgressPolylineConnection)m_markedEdge.getFigure()).setLineWidth(1);
            m_markedEdge = null;

            // workaround for eclipse bug 393868 (https://bugs.eclipse.org/bugs/show_bug.cgi?id=393868)
            WindowsDNDHelper.showDragImage();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void drop(final DropTargetEvent event) {

        // TODO: get the Selection from the LocalSelectionTransfer
        // check instanceof NodeTemplate and fire a CreateRequest
        LOGGER.debug("drop: " + event);
        AbstractNodeTemplate ant = getSelectionNodeTemplate();
        WorkflowManager wfm = ((WorkflowRootEditPart)m_viewer.getRootEditPart().getContents()).getWorkflowManager();
        Point dropLocation = getDropLocation(event);
        if (ant instanceof NodeTemplate) {
            NodeTemplate template = (NodeTemplate)ant;
            CreateRequest request = new CreateRequest();
            // TODO for some reason sometimes the event contains no object - but
            // this doesn't seem to matter - dragging continues as expected
            // Set the factory on the current request
            NodeFromNodeTemplateCreationFactory factory = new NodeFromNodeTemplateCreationFactory(template);
            request.setFactory(factory);

            if (node != null && nodeCount >= edgeCount) {

                // more node pixels than edge pixels found in hit-box
                m_viewer.getEditDomain().getCommandStack()
                    .execute(new ReplaceNodeCommand(wfm, factory.getNewObject(), node));

            } else if (edge != null) {
                Point insertLocation = getInsertLocation(dropLocation.x, dropLocation.y);

                CreateSpaceAction m_spaceAction =
                    new CreateSpaceAction((WorkflowEditor)Workbench.getInstance().getActiveWorkbenchWindow()
                        .getActivePage().getActiveEditor(), CreateSpaceDirection.RIGHT, (MINIMUM_NODE_DISTANCE / 2));

                // more edge pixels found
                m_viewer
                    .getEditDomain()
                    .getCommandStack()
                    .execute(
                        new InsertNewNodeCommand(wfm, factory.getNewObject(), edge, insertLocation.x, insertLocation.y,
                            WorkflowEditor.getActiveEditorSnapToGrid()));

                if (!m_viewer.getSelection().isEmpty()) {
                    m_spaceAction.runInSWT();
                }

                m_viewer.getSelectionManager().deselectAll();

            } else {
                // normal insert
                m_viewer
                    .getEditDomain()
                    .getCommandStack()
                    .execute(
                        new CreateNodeCommand(wfm, factory.getNewObject(), dropLocation, WorkflowEditor
                            .getActiveEditorSnapToGrid()));
            }
            NodeUsageRegistry.addNode(template);
            // bugfix: 1500
            m_viewer.getControl().setFocus();
        } else if (ant instanceof MetaNodeTemplate) {
            MetaNodeTemplate mnt = (MetaNodeTemplate)ant;
            NodeID id = mnt.getManager().getID();
            WorkflowManager sourceManager = RepositoryFactory.META_NODE_ROOT;
            WorkflowCopyContent content = new WorkflowCopyContent();
            content.setNodeIDs(id);
            WorkflowPersistor copy = sourceManager.copy(content);

            if (node != null && nodeCount >= edgeCount) {

                // more node pixels than edge pixels found in hit-box
                m_viewer.getEditDomain().getCommandStack()
                    .execute(new ReplaceMetaNodeCommand(wfm, copy, node, WorkflowEditor.getActiveEditorSnapToGrid()));
            } else if (edge != null) {
                Point insertLocation = getInsertLocation(dropLocation.x, dropLocation.y);

                CreateSpaceAction m_spaceAction =
                    new CreateSpaceAction((WorkflowEditor)Workbench.getInstance().getActiveWorkbenchWindow()
                        .getActivePage().getActiveEditor(), CreateSpaceDirection.RIGHT, (MINIMUM_NODE_DISTANCE / 2));

                // more edge pixels found
                m_viewer
                    .getEditDomain()
                    .getCommandStack()
                    .execute(
                        new InsertMetaNodeCommand(wfm, copy, edge, insertLocation.x, insertLocation.y, WorkflowEditor
                            .getActiveEditorSnapToGrid()));

                if (!m_viewer.getSelection().isEmpty()) {
                    m_spaceAction.runInSWT();
                }

                m_viewer.getSelectionManager().deselectAll();
            } else {
                m_viewer
                    .getEditDomain()
                    .getCommandStack()
                    .execute(
                        new CreateMetaNodeCommand(wfm, copy, dropLocation, WorkflowEditor
                            .getActiveEditorSnapToGrid()));
            }
        }

        unmark(wfm);
    }

    /**
     * @return
     */
    private Point getInsertLocation(final int x, final int y) {
        Point insertLocation = null;
        EditPart source = edge.getSource().getParent();
        EditPart target = edge.getTarget().getParent();
        if (source instanceof WorkflowInPortBarEditPart) {
            source = target;
        }
        if (source instanceof NodeContainerEditPart && target instanceof NodeContainerEditPart) {
            NodeContainerEditPart sourceNode = (NodeContainerEditPart)source;
            NodeContainerEditPart targetNode = (NodeContainerEditPart)target;
            m_viewer.getSelectionManager().deselectAll();

            int[] sourceBounds = sourceNode.getNodeContainer().getUIInformation().getBounds();
            int[] targetBounds = targetNode.getNodeContainer().getUIInformation().getBounds();

            int sourceYTop = sourceBounds[1];
            int sourceYBot = sourceYTop + sourceBounds[3] + sourceNode.getNodeAnnotationEditPart().getModel().getHeight();
            int targetYTop = targetBounds[1];
            int targetYBot = targetYTop + targetBounds[3] + targetNode.getNodeAnnotationEditPart().getModel().getHeight();

            int xLoc, yLoc;
            if (sourceBounds[0] + 100 > x) {
                xLoc = sourceBounds[0] + 100;
            } else {
                xLoc = x;
            }

            if (Math.abs(sourceBounds[1] - targetBounds[1]) < 20) {
                yLoc = sourceBounds[1];
            } else {
                yLoc = y;
            }

            insertLocation =
                new Point(xLoc, yLoc);

            if (0 <= targetBounds[0] - sourceBounds[0]
                && targetBounds[0] - sourceBounds[0] < MINIMUM_NODE_DISTANCE
                && ((targetYTop <= sourceYTop && sourceYTop <= targetYBot) || (targetYTop <= sourceYBot && sourceYBot <= targetYBot) ||
                        targetYBot + 100 > sourceYTop)) {
                selectNodes(targetNode);
            }
        }
        if (insertLocation == null && source instanceof NodeContainerEditPart) {
            int[] bounds = ((NodeContainerEditPart)source).getNodeContainer().getUIInformation().getBounds();
            insertLocation = new Point(bounds[0] + (MINIMUM_NODE_DISTANCE / 2), bounds[1]);
        }
        if (insertLocation == null) {
            int[] bounds =
                ((WorkflowInPortBarEditPart)source).getNodeContainer().getUIInformation().getBounds();
            insertLocation = new Point(bounds[0] + (MINIMUM_NODE_DISTANCE / 2), bounds[1]);
        }
        return insertLocation;
    }

    /**
     * @param n
     */
    private void selectNodes(final NodeContainerEditPart n) {
        m_viewer.getSelectionManager().appendSelection(n);
        for (ConnectionContainerEditPart c : n.getOutgoingConnections()) {
            EditPart source = c.getSource().getParent();
            EditPart target = c.getTarget().getParent();
            if (source instanceof NodeContainerEditPart && target instanceof NodeContainerEditPart) {
                NodeContainerEditPart sourceNode = (NodeContainerEditPart)source;
                NodeContainerEditPart targetNode = (NodeContainerEditPart)target;

                int[] sourceBounds = sourceNode.getNodeContainer().getUIInformation().getBounds();
                int[] targetBounds = targetNode.getNodeContainer().getUIInformation().getBounds();
                int sourceYTop = sourceBounds[1];
                int sourceYBot = sourceYTop + sourceBounds[3] + sourceNode.getNodeAnnotationEditPart().getModel().getHeight();
                int targetYTop = targetBounds[1];
                int targetYBot = targetYTop + targetBounds[3] + targetNode.getNodeAnnotationEditPart().getModel().getHeight();

                if (0 < targetBounds[0] - sourceBounds[0]
                    && targetBounds[0] - sourceBounds[0] < MINIMUM_NODE_DISTANCE
                    && ((targetYTop <= sourceYTop && sourceYTop <= targetYBot) || (targetYTop <= sourceYBot && sourceYBot <= targetYBot))) {
                    selectNodes(targetNode);
                }
            }
        }
    }

    private AbstractNodeTemplate getSelectionNodeTemplate() {
        if (LocalSelectionTransfer.getTransfer().getSelection() == null) {
            return null;
        }
        if (((IStructuredSelection)LocalSelectionTransfer.getTransfer().getSelection()).size() > 1) {
            // allow dropping a single node only
            return null;
        }

        Object template = ((IStructuredSelection)LocalSelectionTransfer.getTransfer().getSelection()).getFirstElement();
        if (template instanceof AbstractNodeTemplate) {
            return (AbstractNodeTemplate)template;
        }
        // Last change: Ask adaptables for an adapter object
        if (template instanceof IAdaptable) {
            return (AbstractNodeTemplate)((IAdaptable)template).getAdapter(AbstractNodeTemplate.class);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dropAccept(final DropTargetEvent event) {
    }
}
