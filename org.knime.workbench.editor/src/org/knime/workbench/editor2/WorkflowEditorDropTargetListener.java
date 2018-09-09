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
 * ------------------------------------------------------------------------
 */

package org.knime.workbench.editor2;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.dnd.AbstractTransferDropTargetListener;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.ContextAwareNodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.workbench.editor2.CreateDropRequest.RequestType;
import org.knime.workbench.editor2.actions.CreateSpaceAction.CreateSpaceDirection;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.editparts.WorkflowInPortBarEditPart;
import org.knime.workbench.editor2.editparts.WorkflowOutPortBarEditPart;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;
import org.knime.workbench.explorer.view.ContentObject;
import org.knime.workbench.repository.util.ContextAwareNodeFactoryMapper;

/**
 *
 * @author Dominik Morent, KNIME AG, Zurich, Switzerland
 * @author Tim-Oliver Buchholz, KNIME AG, Zurich, Switzerland
 * @param <T> a creation factory for the item which will be dropped
 */
public abstract class WorkflowEditorDropTargetListener<T extends CreationFactory> extends
    AbstractTransferDropTargetListener {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(WorkflowEditorDropTargetListener.class);

    /**
     * The minimum distance between two nodes which is need to insert a new one. If the distance between two nodes is
     * smaller some space will be created.
     */
    private static final int MINIMUM_NODE_DISTANCE_BEFORE_INSERTION = 200;

    /**
     * Standard distance between two nodes.
     */
    private static final int MINIMUM_NODE_DISTANCE = MINIMUM_NODE_DISTANCE_BEFORE_INSERTION / 2;

    /**
     * The x coordinate is recomputed if snap to gird is enabled.
     *
     * @param dropLocationX x coordinate of the drop event
     * @param sourceX x coordinate of the source node
     * @return source node x coordinate plus the {@link WorkflowEditorDropTargetListener#MINIMUM_NODE_DISTANCE}
     */
    private static int getXLocation(final int dropLocationX, final int sourceX, final double zoom) {
        if (sourceX < dropLocationX && sourceX + MINIMUM_NODE_DISTANCE * zoom > dropLocationX) {
            return (int)(sourceX + MINIMUM_NODE_DISTANCE * zoom);
        } else {
            return dropLocationX;
        }
    }

    /**
     * If the y coordinate of source and target difference is less than 20 pixel the source y coordinate is returned.
     * Also the y coordinate is recomputed if snap to grid is enabled.
     *
     * @param dropLocationY y coordinate of the drop event
     * @param sourceY y coordinate of the source node
     * @param targetY y coordinate of the target node
     * @return y location for the new node
     */
    private static int getYLocation(final int dropLocationY, final int sourceY, final int targetY, final double zoom) {
        if (Math.abs(sourceY - targetY) < 20 * zoom) {
            return sourceY;
        } else {
            return dropLocationY;

        }
    }


    private final T m_factory;

    private int m_distanceToMoveTarget = 0;

    private final DragPositionProcessor m_dragPositionProcessor;

    /**
     * @param viewer the edit part viewer this drop target listener is attached to
     * @param factory the creation factory for the item which will be dropped
     */
    protected WorkflowEditorDropTargetListener(final EditPartViewer viewer, final T factory) {
        super(viewer);

        m_factory = factory;

        m_dragPositionProcessor = new DragPositionProcessor(viewer);
    }

    /**
     * @param url the URL of the file
     * @return a node factory creating a node that is registered for handling this type of file
     */
    @SuppressWarnings("unchecked")
    protected ContextAwareNodeFactory<NodeModel> getNodeFactory(final URL url) {
        String path = url.getPath();
        @SuppressWarnings("rawtypes")
        Class<? extends ContextAwareNodeFactory> clazz = ContextAwareNodeFactoryMapper.getNodeFactory(path);
        if (clazz == null) {
            LOGGER.warn("No node factory is registered for handling " + " \"" + path + "\"");
            return null;
        }
        try {
            return clazz.newInstance();
        } catch (InstantiationException e) {
            LOGGER.error("Can't create node " + clazz.getName() + ".", e);
        } catch (IllegalAccessException e) {
            LOGGER.error(e);
        }
        return null;
    }

    /**
     * @param event the drop target event
     * @return the first dragged resource or null if the event contains a resource that is not of type
     *         {@link ContentObject}
     */
    protected ContentObject getDragResources(final DropTargetEvent event) {
        LocalSelectionTransfer transfer = (LocalSelectionTransfer)getTransfer();
        ISelection selection = transfer.getSelection();
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection ss = (IStructuredSelection)selection;
            Object firstElement = ss.getFirstElement();
            if (firstElement instanceof ContentObject) {
                return (ContentObject)firstElement;
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    protected Request createTargetRequest() {
        CreateDropRequest request = new CreateDropRequest();
        request.setFactory(m_factory);
        return request;
    }

    /** {@inheritDoc}
     * Based on the cursor location a different drop request is generated.
     * If the cursor is over a node a replace request is generated, if
     * the cursor is over an edge an insert request is generated or a
     * simple create node request if nothing is under the cursor.
     * */
    @Override
    protected void updateTargetRequest() {
        CreateDropRequest request = (CreateDropRequest)getTargetRequest();

        if ((m_dragPositionProcessor.getNode() != null)
                    && (m_dragPositionProcessor.getNodeCount() >= m_dragPositionProcessor.getEdgeCount())) {
            request.setRequestType(RequestType.REPLACE);
            request.setLocation(getDropLocation());
            request.setEditPart(m_dragPositionProcessor.getNode());
            request.setCreateSpace(m_distanceToMoveTarget > 0);
        } else if (m_dragPositionProcessor.getEdge() != null) {
            request.setRequestType(RequestType.INSERT);
            request.setLocation(getInsertLocation());
            request.setEditPart(m_dragPositionProcessor.getEdge());
            request.setCreateSpace(m_distanceToMoveTarget > 0);
            request.setDirection(CreateSpaceDirection.RIGHT);
            request.setDistance(m_distanceToMoveTarget);
        } else {
            request.setRequestType(RequestType.CREATE);
            request.setLocation(getDropLocation());
            request.setEditPart(null);
            request.setCreateSpace(m_distanceToMoveTarget > 0);
        }

        m_dragPositionProcessor.unmarkSelection();
    }

    /**
     * Computes the location of the new node and also determines if some node should be moved.
     *
     * A successor node is selected for the move action if the distance between this node and the successor is
     * smaller than the {@link WorkflowEditorDropTargetListener#MINIMUM_NODE_DISTANCE} and if they overlap vertically.
     *
     * @return point where the new node should be inserted and sets
     *         {@link WorkflowEditorDropTargetListener#m_distanceToMoveTarget}
     */
    private Point getInsertLocation() {
        ZoomManager zoomManager = (ZoomManager)getViewer().getProperty(ZoomManager.class.toString());
        double zoom = zoomManager.getZoom();
        int adjustedMinimumNodeDistance = (int)(MINIMUM_NODE_DISTANCE * zoom);
        double adjustedMinimumNodeDistanceBeforeInsertion = MINIMUM_NODE_DISTANCE_BEFORE_INSERTION * zoom;
        Point dropLocation = getDropLocation();
        Point insertLocation = dropLocation;
        int[] sourceBounds = null;
        int[] targetBounds = null;
        int sourceAnnotationHeight = 0;
        int targetAnnotationHeight = 0;
        EditPart source = m_dragPositionProcessor.getEdge().getSource().getParent();
        EditPart target = m_dragPositionProcessor.getEdge().getTarget().getParent();

        NodeContainerEditPart nextNode = null;

        if (source instanceof WorkflowInPortBarEditPart && target instanceof NodeContainerEditPart) {
            // metanode start --> first node in metanode
            WorkflowInPortBarEditPart sourceBar = ((WorkflowInPortBarEditPart)source);
            NodeContainerEditPart targetNode = (NodeContainerEditPart)target;
            Rectangle bounds = sourceBar.getFigure().getBounds();
            org.eclipse.swt.graphics.Point p = getViewer().getControl().toControl(bounds.x, bounds.y);
            sourceBounds = new int[]{p.x, p.y, bounds.width, bounds.height};
            targetBounds = targetNode.getNodeContainer().getUIInformation().getBounds();
            targetAnnotationHeight = targetNode.getNodeAnnotationEditPart().getModel().getHeight();
            nextNode = targetNode;
        } else if (source instanceof NodeContainerEditPart && target instanceof WorkflowOutPortBarEditPart) {
            // last node in metanode --> metanode end
            NodeContainerEditPart sourceNode = (NodeContainerEditPart)source;
            WorkflowOutPortBarEditPart targetBar = (WorkflowOutPortBarEditPart)target;
            sourceBounds = sourceNode.getNodeContainer().getUIInformation().getBounds();
            Rectangle bounds = targetBar.getFigure().getBounds();
            targetBounds = new int[]{bounds.x, bounds.y, bounds.width, bounds.height};
            sourceAnnotationHeight = sourceNode.getNodeAnnotationEditPart().getModel().getHeight();
        } else if (source instanceof WorkflowInPortBarEditPart && target instanceof WorkflowOutPortBarEditPart) {
            // metanode start --> metanode end
            WorkflowInPortBarEditPart sourceBar = (WorkflowInPortBarEditPart)source;
            WorkflowOutPortBarEditPart targetBar = (WorkflowOutPortBarEditPart)target;
            sourceBounds = sourceBar.getNodeContainer().getUIInformation().getBounds();
            targetBounds = targetBar.getNodeContainer().getUIInformation().getBounds();
        } else if (source instanceof NodeContainerEditPart && target instanceof NodeContainerEditPart) {
            // node --> node
            NodeContainerEditPart sourceNode = (NodeContainerEditPart)source;
            NodeContainerEditPart targetNode = (NodeContainerEditPart)target;
            sourceBounds = sourceNode.getNodeContainer().getUIInformation().getBounds();
            targetBounds = targetNode.getNodeContainer().getUIInformation().getBounds();
            sourceAnnotationHeight = sourceNode.getNodeAnnotationEditPart().getModel().getHeight();
            targetAnnotationHeight = targetNode.getNodeAnnotationEditPart().getModel().getHeight();
            nextNode = targetNode;
        }
        if (sourceBounds != null && targetBounds != null) {
            sourceBounds = recomputeBounds(sourceBounds);
            targetBounds = recomputeBounds(targetBounds);
            if (0 <= targetBounds[0] - sourceBounds[0]
                && targetBounds[0] - sourceBounds[0] >= adjustedMinimumNodeDistanceBeforeInsertion) {
                m_distanceToMoveTarget = 0;
            } else {
                m_distanceToMoveTarget =
                    adjustedMinimumNodeDistance + (sourceBounds[0] + adjustedMinimumNodeDistance - targetBounds[0]);
                m_distanceToMoveTarget = (int)(m_distanceToMoveTarget / zoom);
            }

            insertLocation =
                new Point(getXLocation(dropLocation.x, sourceBounds[0], zoom), getYLocation(dropLocation.y,
                    sourceBounds[1], targetBounds[1], zoom));
            if (WorkflowEditor.getActiveEditorSnapToGrid()) {
                insertLocation = WorkflowEditor.getActiveEditorClosestGridLocation(insertLocation);
            }

            getViewer().getSelectionManager().deselectAll();
            if (nextNode != null
                && selectSuccessor(sourceBounds[0], targetBounds[0], sourceBounds[1], sourceBounds[1] + sourceBounds[3]
                    + sourceAnnotationHeight, targetBounds[1], targetBounds[1] + targetBounds[3]
                    + targetAnnotationHeight, zoom)) {
                selectNodes(nextNode, zoom);
            }
        }
        return insertLocation;
    }

    /**
     * Selects all nodes after this node which are around the same y coordinate and closer than
     * {@link WorkflowEditorDropTargetListener#m_distanceToMoveTarget} +
     * {@link WorkflowEditorDropTargetListener#MINIMUM_NODE_DISTANCE}
     *
     * All selected elements will be moved by the move action.
     *
     * @param node from which on the selection starts
     */
    private void selectNodes(final NodeContainerEditPart node, final double zoom) {
        getViewer().getSelectionManager().appendSelection(node);
        for (ConnectionContainerEditPart c : node.getOutgoingConnections()) {
            EditPart source = c.getSource().getParent();
            EditPart target = c.getTarget().getParent();
            if (source instanceof NodeContainerEditPart && target instanceof NodeContainerEditPart) {
                NodeContainerEditPart sourceNode = (NodeContainerEditPart)source;
                NodeContainerEditPart targetNode = (NodeContainerEditPart)target;

                int[] sourceBounds = sourceNode.getNodeContainer().getUIInformation().getBounds();
                int[] targetBounds = targetNode.getNodeContainer().getUIInformation().getBounds();
                sourceBounds = recomputeBounds(sourceBounds);
                targetBounds = recomputeBounds(targetBounds);
                int sourceYTop = sourceBounds[1];
                int sourceYBot =
                    sourceYTop + sourceBounds[3] + sourceNode.getNodeAnnotationEditPart().getModel().getHeight();
                int targetYTop = targetBounds[1];
                int targetYBot =
                    targetYTop + targetBounds[3] + targetNode.getNodeAnnotationEditPart().getModel().getHeight();

                if (selectSuccessor(sourceBounds[0], targetBounds[0], sourceYTop, sourceYBot, targetYTop, targetYBot,
                    zoom)) {
                    selectNodes(targetNode, zoom);
                }
            }
        }
    }

    /**
     * A successor should be moved if the successor is to close and their y coordinates overlap.
     *
     * @param sourceX x coordinate of the source node
     * @param targetX x coordinate of the target node
     * @param sourceTop y coordinate of the source node
     * @param sourceBot y coordinate plus height of the source node
     * @param targetTop y coordinate of the target node
     * @param targetBot y coordinate plus height of the target node
     * @return if successor should be moved
     */
    private boolean selectSuccessor(final int sourceX, final int targetX, final int sourceTop, final int sourceBot,
        final int targetTop, final int targetBot, final double zoom) {
        if (WorkflowEditor.getActiveEditorSnapToGrid()) {
            // transform coordinates to grid coordinates
            int snappedSourceX = WorkflowEditor.getActiveEditorClosestGridLocation(new Point(sourceX, 0)).x;
            int snappedTargetX = WorkflowEditor.getActiveEditorClosestGridLocation(new Point(targetX, 0)).x;
            int snappedSourceTop = WorkflowEditor.getActiveEditorClosestGridLocation(new Point(0, sourceTop)).y;
            int snappedsourceBot = WorkflowEditor.getActiveEditorClosestGridLocation(new Point(0, sourceBot)).y;
            int snappedTargetTop = WorkflowEditor.getActiveEditorClosestGridLocation(new Point(0, targetTop)).y;
            int snappedTargetBot = WorkflowEditor.getActiveEditorClosestGridLocation(new Point(0, targetBot)).y;

            return 0 < snappedTargetX - snappedSourceX
                && snappedTargetX - snappedSourceX <= (m_distanceToMoveTarget + MINIMUM_NODE_DISTANCE) * zoom
                && ((snappedTargetTop <= snappedSourceTop && snappedSourceTop <= snappedTargetBot) ||
                        (snappedTargetTop <= snappedsourceBot && snappedsourceBot <= snappedTargetBot));
        } else {
            return 0 < targetX - sourceX
                && targetX - sourceX <= (m_distanceToMoveTarget + MINIMUM_NODE_DISTANCE) * zoom
                && ((targetTop <= sourceTop && sourceTop <= targetBot) || (targetTop <= sourceBot && sourceBot <= targetBot));
        }
    }

    /**
     * {@inheritDoc}
     *
     * Marks nodes or edges if a new node should replace an old node or should be inserted on an edge.
     */
    @Override
    public void dragOver(final DropTargetEvent event) {
        m_dragPositionProcessor.processDragEventAtPoint(event.display.getCursorLocation());
    }

    /**
     * @return the creation factory
     */
    protected T getFactory() {
        return m_factory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled(final DropTargetEvent event) {
        return getDragResources(event) != null;
    }

    @Override
    protected void eraseTargetFeedback() {
        super.eraseTargetFeedback();

        m_dragPositionProcessor.unmarkSelection();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked") // generics casting...
    @Override
    protected void handleDrop() {
        updateTargetRequest();
        updateTargetEditPart();

        if (getTargetEditPart() != null) {
            final Request r = getTargetRequest();
            if (r instanceof CreateDropRequest) {
                final EditPart ep = ((CreateDropRequest)r).getEditPart();

                if (ep != null) {
                    final boolean connection = (ep instanceof ConnectionContainerEditPart);

                    if (!NodeSupplantDragListener.replacingNodeOrConnectionBisectionIsAllowed(connection)) {
                        return;
                    }
                }
            }

            final Command command = getCommand();
            if (command instanceof CompoundCommand) {
                // If the command is a compound command the drop request also needs to
                // create space for the new node and therefore moves other nodes.
                // The commands are executed one after another so the user can undo
                // the move if wanted but still has the new node inserted.
                List<?> commands = ((CompoundCommand)command).getCommands();
                if (commands instanceof ArrayList<?>) {
                    for (Command c : (ArrayList<Command>)commands) {

                        Command p = getViewer().getEditDomain().getCommandStack().getUndoCommand();
                        getViewer().getEditDomain().getCommandStack().execute(c);
                        Command a = getViewer().getEditDomain().getCommandStack().getUndoCommand();
                        if (p == null && a == null) {
                            break;
                        } else if (p != null && p.equals(a)) {
                            break;
                        }
                    }
                    getViewer().getSelectionManager().deselectAll();
                }
            } else {
                getViewer().getEditDomain().getCommandStack().execute(command);
            }
            // after adding a node the editor should get the focus
            // this is issued asynchronously, in order to avoid bug #3029
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    IWorkbenchWindow w = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                    if (w != null) {
                        IWorkbenchPage p = w.getActivePage();
                        if (p != null) {
                            IEditorPart e = p.getActiveEditor();
                            if (e != null) {
                                e.setFocus();
                            }
                        }
                    }
                }
            });
        } else {
            getCurrentEvent().detail = DND.DROP_NONE;
        }
    }

    /**
     * Recompute bounds based on the zoom and viewport position.
     *
     * @param bounds the absolute bounds with zoom 100%
     * @return recomputed bounds relative to the viewport and zoom level
     */
    private int[] recomputeBounds(final int[] bounds) {
        int[] result = new int[4];
        ZoomManager zoomManager = (ZoomManager)getViewer().getProperty(ZoomManager.class.toString());
        double zoom = zoomManager.getZoom();
        int x = zoomManager.getViewport().getHorizontalRangeModel().getValue();
        int y = zoomManager.getViewport().getVerticalRangeModel().getValue();
        result[0] = (int)(zoom * bounds[0] + (-1) * x);
        result[1] = (int)(zoom * bounds[1] + (-1) * y);
        result[2] = (int)(zoom * bounds[2]);
        result[3] = (int)(zoom * bounds[3]);
        return result;
    }

    /**
     * @return the workflow manager of the drop-target, can be <code>null</code> if the drop target is not a
     *         {@link WorkflowEditor}
     */
    protected WorkflowManagerUI getWorkflowManager() {
        EditPart contents = getViewer().getRootEditPart().getContents();
        if (contents != null && contents instanceof WorkflowRootEditPart) {
            return ((WorkflowRootEditPart)contents).getWorkflowManager();
        } else {
            return null;
        }
    }
}