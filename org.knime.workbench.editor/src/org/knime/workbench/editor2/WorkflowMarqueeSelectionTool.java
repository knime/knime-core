/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.knime.workbench.editor2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Cursors;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.tools.AbstractTool;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Display;
import org.knime.workbench.editor2.editparts.AbstractWorkflowPortBarEditPart;
import org.knime.workbench.editor2.editparts.AnnotationEditPart;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.figures.WorkflowAnnotationFigure;

/**
 * A Tool which selects multiple objects inside a rectangular area of a
 * Graphical Viewer. If the SHIFT key is pressed at the beginning of the drag,
 * the enclosed items will be appended to the current selection. If the MOD1 key
 * is pressed at the beginning of the drag, the enclosed items will have their
 * selection state inverted.
 * <P>
 * By default, only editparts whose figure's are on the primary layer will be
 * considered within the enclosed rectangle.
 */
public class WorkflowMarqueeSelectionTool extends AbstractTool implements DragTracker {

    /**
     * The property to be used in
     * {@link AbstractTool#setProperties(java.util.Map)} for
     * {@link #setMarqueeBehavior(int)}.
     */
    public static final Object PROPERTY_MARQUEE_BEHAVIOR = "marqueeBehavior"; //$NON-NLS-1$

    /**
     * This behaviour selects nodes completely encompassed by the marquee
     * rectangle. This is the default behaviour for this tool.
     *
     * @since 3.1
     */
    public static final int BEHAVIOR_NODES_CONTAINED = 1;

    /**
     * This behaviour selects connections that intersect the marquee rectangle.
     *
     * @since 3.1
     */
    public static final int BEHAVIOR_CONNECTIONS_TOUCHED = 2;

    /**
     * This behaviour selects nodes completely encompassed by the marquee
     * rectangle, and all connections between those nodes.
     *
     * @since KNIME development (knime 1.2.2)
     */
    public static final int BEHAVIOR_NODES_AND_CONNECTIONS_TOUCHED = 3;

    static final int DEFAULT_MODE = 0;

    static final int TOGGLE_MODE = 1;

    static final int APPEND_MODE = 2;

    private static final Request MARQUEE_REQUEST;

    static {
        final Request r = new Request(RequestConstants.REQ_SELECTION);

        r.getExtendedData().put(PROPERTY_MARQUEE_BEHAVIOR, new Object());

        MARQUEE_REQUEST = r;
    }


    private Figure marqueeRectangleFigure;

    private Set<GraphicalEditPart> allChildren = new HashSet<GraphicalEditPart>();

    private Collection<GraphicalEditPart> selectedEditParts;

    private Collection<GraphicalEditPart> deselectedEditParts;

    private Collection<GraphicalEditPart> alreadySelectedEditParts;

    private Request targetRequest;

    private int marqueeBehavior = BEHAVIOR_NODES_CONTAINED;

    private int mode;

    /**
     * Creates a new MarqueeSelectionTool of default type
     * {@link #BEHAVIOR_NODES_CONTAINED}.
     */
    public WorkflowMarqueeSelectionTool() {
        setDefaultCursor(Cursors.CROSS);
        setUnloadWhenFinished(false);
        setMarqueeBehavior(WorkflowMarqueeSelectionTool.BEHAVIOR_NODES_AND_CONNECTIONS_TOUCHED);
    }

    /**
     * @see org.eclipse.gef.tools.AbstractTool#applyProperty(java.lang.Object,
     *      java.lang.Object)
     */
    @Override
    protected void applyProperty(final Object key, final Object value) {
        if (PROPERTY_MARQUEE_BEHAVIOR.equals(key)) {
            if (value instanceof Integer) {
                setMarqueeBehavior(((Integer)value).intValue());
            }
            return;
        }
        super.applyProperty(key, value);
    }

    private void calculateConnections(final Collection<GraphicalEditPart> newSelections,
            final Collection<GraphicalEditPart> deselections) {
        // determine the currently selected nodes minus the ones that are to be
        // deselected
        Collection<EditPart> currentNodes = new HashSet<EditPart>();
        if (getSelectionMode() != DEFAULT_MODE) { // everything is deselected
            // in default mode
            Iterator<EditPart> iter =
                    getCurrentViewer().getSelectedEditParts().iterator();
            while (iter.hasNext()) {
                EditPart selected = iter.next();
                if (!(selected instanceof ConnectionEditPart)
                        && !deselections.contains(selected)) {
                    currentNodes.add(selected);
                }
            }
        }
        // add new connections to be selected to newSelections
        Collection<ConnectionEditPart> connections = new ArrayList<ConnectionEditPart>();
        for (Iterator<GraphicalEditPart> nodes = newSelections.iterator(); nodes.hasNext();) {
            GraphicalEditPart node = nodes.next();
            for (Iterator<ConnectionEditPart> itr = node.getSourceConnections().iterator(); itr.hasNext();) {
                ConnectionEditPart sourceConn = itr.next();
                if (sourceConn.getSelected() == EditPart.SELECTED_NONE
                        && (newSelections.contains(sourceConn.getTarget()) || currentNodes
                                .contains(sourceConn.getTarget()))) {
                    connections.add(sourceConn);
                }
            }
            for (Iterator<ConnectionEditPart> itr = node.getTargetConnections().iterator(); itr.hasNext();) {
                ConnectionEditPart targetConn = itr.next();
                if (targetConn.getSelected() == EditPart.SELECTED_NONE
                        && (newSelections.contains(targetConn.getSource()) || currentNodes
                                .contains(targetConn.getSource()))) {
                    connections.add(targetConn);
                }
            }
        }
        newSelections.addAll(connections);
        // add currently selected connections that are to be deselected to
        // deselections
        connections = new HashSet<ConnectionEditPart>();
        for (Iterator<GraphicalEditPart> nodes = deselections.iterator(); nodes.hasNext();) {
            GraphicalEditPart node = nodes.next();
            for (Iterator<ConnectionEditPart> itr = node.getSourceConnections().iterator(); itr.hasNext();) {
                ConnectionEditPart sourceConn = itr.next();
                if (sourceConn.getSelected() != EditPart.SELECTED_NONE) {
                    connections.add(sourceConn);
                }
            }
            for (Iterator<ConnectionEditPart> itr = node.getTargetConnections().iterator(); itr.hasNext();) {
                ConnectionEditPart targetConn = itr.next();
                if (targetConn.getSelected() != EditPart.SELECTED_NONE) {
                    connections.add(targetConn);
                }
            }
        }
        deselections.addAll(connections);
    }

    private void calculateNewSelection(final Collection<GraphicalEditPart> newSelections,
            final Collection<GraphicalEditPart> deselections) {
        Rectangle marqueeRect = getMarqueeSelectionRectangle();
        for (Iterator<GraphicalEditPart> itr = getAllChildren().iterator(); itr.hasNext();) {
            GraphicalEditPart child = itr.next();
            IFigure figure = child.getFigure();
            if (!child.isSelectable()
                    || child.getTargetEditPart(MARQUEE_REQUEST) != child
                    || !isFigureVisible(figure) || !figure.isShowing()) {
                continue;
            }
            if (!(child instanceof NodeContainerEditPart
                    || child instanceof ConnectionContainerEditPart
                    || child instanceof AbstractWorkflowPortBarEditPart
                    || child instanceof AnnotationEditPart)) {
                continue;
            }

            Rectangle r = figure.getBounds().getCopy();
            figure.translateToAbsolute(r);
            boolean included = false;
            if (child instanceof ConnectionEditPart
                    && marqueeRect.intersects(r)) {
                Rectangle relMarqueeRect = Rectangle.SINGLETON;
                figure.translateToRelative(relMarqueeRect
                        .setBounds(marqueeRect));
                included =
                        ((PolylineConnection)figure).getPoints().intersects(
                                relMarqueeRect);
            } else if (child instanceof AnnotationEditPart) {
                // don't include node annotations at all
                // select WorkflowAnnotations only if they are fully included in the selection
                if (figure instanceof WorkflowAnnotationFigure) {
                    included = marqueeRect.contains(r);
                }
            } else if (marqueeBehavior == BEHAVIOR_NODES_AND_CONNECTIONS_TOUCHED) {
                included = marqueeRect.intersects(r);
            } else {
                included = marqueeRect.contains(r);
            }

            if (included) {
                if (isToggle()) {
                    if (wasSelected(child)) {
                        deselections.add(child);
                    } else {
                        newSelections.add(child);
                    }
                } else {
                    newSelections.add(child);
                }
            } else if (isToggle()) {
                // if in toggle mode, a not included child must be
                // readded if it was in the selection before
                if (wasSelected(child)) {
                    newSelections.add(child);
                } else {
                    deselections.add(child);
                }
            }
        }

        if (marqueeBehavior == BEHAVIOR_NODES_AND_CONNECTIONS_TOUCHED) {
            calculateConnections(newSelections, deselections);
        }
    }

    private boolean wasSelected(final EditPart part) {
        for (Object o : alreadySelectedEditParts) {
            if (o == part) {
                return true;
            }
        }
        return false;
    }

    private Request createTargetRequest() {
        return MARQUEE_REQUEST;
    }

    /**
     * Erases feedback if necessary and puts the tool into the terminal state.
     */
    @Override
    public void deactivate() {
        if (isInState(STATE_DRAG_IN_PROGRESS)) {
            eraseMarqueeFeedback();
            eraseTargetFeedback();
        }
        super.deactivate();
        allChildren.clear();
        setState(STATE_TERMINAL);
    }

    private void eraseMarqueeFeedback() {
        if (marqueeRectangleFigure != null) {
            removeFeedback(marqueeRectangleFigure);
            marqueeRectangleFigure = null;
        }
    }

    private void eraseTargetFeedback() {
        if (selectedEditParts == null) {
            return;
        }
        for (EditPart editPart : selectedEditParts) {
            editPart.eraseTargetFeedback(getTargetRequest());
        }
    }

    private Set<GraphicalEditPart> getAllChildren() {
        if (allChildren.isEmpty()) {
            getAllChildren(getCurrentViewer().getRootEditPart(), allChildren);
        }
        return allChildren;
    }

    private void getAllChildren(final EditPart editPart, final Set<GraphicalEditPart> allChildren) {
        List<GraphicalEditPart> children = editPart.getChildren();
        for (int i = 0; i < children.size(); i++) {
            GraphicalEditPart child = children.get(i);
            if (marqueeBehavior == BEHAVIOR_NODES_CONTAINED
                    || marqueeBehavior == BEHAVIOR_NODES_AND_CONNECTIONS_TOUCHED) {
                allChildren.add(child);
            }
            if (marqueeBehavior == BEHAVIOR_CONNECTIONS_TOUCHED
                    || marqueeBehavior == BEHAVIOR_NODES_AND_CONNECTIONS_TOUCHED) {
                allChildren.addAll(child.getSourceConnections());
                allChildren.addAll(child.getTargetConnections());
            }
            getAllChildren(child, allChildren);
        }
    }

    /**
     * @see org.eclipse.gef.tools.AbstractTool#getCommandName()
     */
    @Override
    protected String getCommandName() {
        return REQ_SELECTION;
    }

    /**
     * @see org.eclipse.gef.tools.AbstractTool#getDebugName()
     */
    @Override
    protected String getDebugName() {
        return "Marquee Tool: " + marqueeBehavior;//$NON-NLS-1$
    }

    private IFigure getMarqueeFeedbackFigure() {
        if (marqueeRectangleFigure == null) {
            marqueeRectangleFigure = new MarqueeRectangleFigure();
            addFeedback(marqueeRectangleFigure);
        }
        return marqueeRectangleFigure;
    }

    private Rectangle getMarqueeSelectionRectangle() {
        return new Rectangle(getStartLocation(), getLocation());
    }

    private int getSelectionMode() {
        return mode;
    }

    private Request getTargetRequest() {
        if (targetRequest == null) {
            targetRequest = createTargetRequest();
        }
        return targetRequest;
    }

    /**
     * @see org.eclipse.gef.tools.AbstractTool#handleButtonDown(int)
     */
    @Override
    protected boolean handleButtonDown(final int button) {
        if (!isGraphicalViewer()) {
            return true;
        }
        if (button != 1) {
            setState(STATE_INVALID);
            handleInvalidInput();
        }
        if (stateTransition(STATE_INITIAL, STATE_DRAG_IN_PROGRESS)) {
            if (getCurrentInput().isModKeyDown(SWT.MOD1)) {
                setSelectionMode(TOGGLE_MODE);
            } else if (getCurrentInput().isShiftKeyDown()) {
                setSelectionMode(APPEND_MODE);
            } else {
                setSelectionMode(DEFAULT_MODE);
            }
        }
        alreadySelectedEditParts = new ArrayList<GraphicalEditPart>();
        alreadySelectedEditParts.addAll(getCurrentViewer()
                .getSelectedEditParts());
        return true;
    }

    /**
     * @see org.eclipse.gef.tools.AbstractTool#handleButtonUp(int)
     */
    @Override
    protected boolean handleButtonUp(final int button) {
        if (stateTransition(STATE_DRAG_IN_PROGRESS, STATE_TERMINAL)) {
            eraseTargetFeedback();
            eraseMarqueeFeedback();
            performMarqueeSelect();
        }
        handleFinished();
        return true;
    }


    boolean nodesAndConnectionsEqual(final Collection<GraphicalEditPart> c1, final Collection<GraphicalEditPart> c2) {
        if (c1.size() != c2.size()) {
            return false;
        }
        for (EditPart o : c1) {
            // only node and connection container parts are relevant
            if ((o instanceof NodeContainerEditPart)
                    || (o instanceof ConnectionContainerEditPart)
                    || (o instanceof AbstractWorkflowPortBarEditPart)) {
                // now check if o is also in c2
                boolean found = false;
                for (EditPart o2 : c2) {
                    if (o2.equals(o)) {
                        found = true;
                    }
                }
                if (!found) {
                    // the second list does not contain the object
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isToggle() {
        return getSelectionMode() == TOGGLE_MODE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean handleDragInProgress() {
        if (isInState(STATE_DRAG | STATE_DRAG_IN_PROGRESS)) {
            showMarqueeFeedback();
            eraseTargetFeedback();
            List<GraphicalEditPart> previousSelection = new ArrayList<GraphicalEditPart>();
            List<GraphicalEditPart> previousDeselection = new ArrayList<GraphicalEditPart>();
            if (selectedEditParts != null) {
                previousSelection.addAll(selectedEditParts);
            } else {
                performMarqueeSelect();
            }
            if (isToggle() && deselectedEditParts != null) {
                previousDeselection.addAll(deselectedEditParts);
            }
            selectedEditParts = new ArrayList<GraphicalEditPart>();
            deselectedEditParts = new ArrayList<GraphicalEditPart>();
            calculateNewSelection(selectedEditParts, deselectedEditParts);
            showTargetFeedback();
            if (!nodesAndConnectionsEqual(previousSelection, selectedEditParts)
                    || !nodesAndConnectionsEqual(previousDeselection,
                            deselectedEditParts)) {
                performMarqueeSelect();
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean handleFocusLost() {
        if (isInState(STATE_DRAG | STATE_DRAG_IN_PROGRESS)) {
            handleFinished();
            return true;
        }
        return false;
    }

    /**
     * This method is called when mouse or keyboard input is invalid and erases
     * the feedback.
     *
     * @return <code>true</code>
     */
    @Override
    protected boolean handleInvalidInput() {
        eraseTargetFeedback();
        eraseMarqueeFeedback();
        return true;
    }

    /**
     * Handles high-level processing of a key down event. KeyEvents are
     * forwarded to the current viewer's {@link KeyHandler}, via
     * {@link KeyHandler#keyPressed(KeyEvent)}.
     *
     * @see AbstractTool#handleKeyDown(KeyEvent)
     */
    @Override
    protected boolean handleKeyDown(final KeyEvent e) {
        if (super.handleKeyDown(e)) {
            return true;
        }
        if (getCurrentViewer().getKeyHandler() != null) {
            return getCurrentViewer().getKeyHandler().keyPressed(e);
        }
        return false;
    }

    private boolean isFigureVisible(final IFigure fig) {
        Rectangle figBounds = fig.getBounds().getCopy();
        IFigure walker = fig.getParent();
        while (!figBounds.isEmpty() && walker != null) {
            walker.translateToParent(figBounds);
            figBounds.intersect(walker.getBounds());
            walker = walker.getParent();
        }
        return !figBounds.isEmpty();
    }

    private boolean isGraphicalViewer() {
        return getCurrentViewer() instanceof GraphicalViewer;
    }

    /**
     * MarqueeSelectionTool is only interested in GraphicalViewers, not
     * TreeViewers.
     *
     * @see org.eclipse.gef.tools.AbstractTool#isViewerImportant(org.eclipse.gef.EditPartViewer)
     */
    @Override
    protected boolean isViewerImportant(final EditPartViewer viewer) {
        return viewer instanceof GraphicalViewer;
    }

    private void performMarqueeSelect() {
        EditPartViewer viewer = getCurrentViewer();
        Collection<GraphicalEditPart> newSelections = new LinkedHashSet<GraphicalEditPart>(),
                deselections = new HashSet<GraphicalEditPart>();
        calculateNewSelection(newSelections, deselections);
        if (getSelectionMode() != DEFAULT_MODE) {
            newSelections.addAll(viewer.getSelectedEditParts());
            newSelections.removeAll(deselections);
        }
        viewer.setSelection(new StructuredSelection(newSelections.toArray()));
    }

    /**
     * Sets the type of parts that this tool will select. This method should
     * only be invoked once: when the tool is being initialized.
     *
     * @param type {@link #BEHAVIOR_CONNECTIONS_TOUCHED} or
     *            {@link #BEHAVIOR_NODES_CONTAINED}
     * @since 3.1
     */
    public void setMarqueeBehavior(final int type) {
        if ((type != BEHAVIOR_CONNECTIONS_TOUCHED)
                && (type != BEHAVIOR_NODES_CONTAINED)
                && (type != BEHAVIOR_NODES_AND_CONNECTIONS_TOUCHED)) {
            throw new IllegalArgumentException(
                    "Invalid marquee behaviour specified."); //$NON-NLS-1$
        }
        marqueeBehavior = type;
    }

    private void setSelectionMode(final int mode) {
        this.mode = mode;
    }

    /**
     * @see org.eclipse.gef.Tool#setViewer(org.eclipse.gef.EditPartViewer)
     */
    @Override
    public void setViewer(final EditPartViewer viewer) {
        if (viewer == getCurrentViewer()) {
            return;
        }
        super.setViewer(viewer);
        if (viewer instanceof GraphicalViewer) {
            setDefaultCursor(Cursors.CROSS);
        } else {
            setDefaultCursor(Cursors.NO);
        }
    }

    private void showMarqueeFeedback() {
        Rectangle rect = getMarqueeSelectionRectangle().getCopy();
        getMarqueeFeedbackFigure().translateToRelative(rect);
        getMarqueeFeedbackFigure().setBounds(rect);
    }

    private void showTargetFeedback() {
        for (Iterator<GraphicalEditPart> itr = selectedEditParts.iterator(); itr.hasNext();) {
            EditPart editPart = itr.next();
            editPart.showTargetFeedback(getTargetRequest());
        }
    }

    static class MarqueeRectangleFigure extends Figure {

        private static final int DELAY = 110; // animation delay in

        // millisecond

        private int offset = 0;

        private boolean schedulePaint = true;

        /**
         * @see org.eclipse.draw2d.Figure#paintFigure(org.eclipse.draw2d.Graphics)
         */
        @Override
        protected void paintFigure(final Graphics graphics) {
            Rectangle bounds = getBounds().getCopy();
            graphics.translate(getLocation());

            graphics.setForegroundColor(ColorConstants.gray);
            graphics.setBackgroundColor(ColorConstants.white);

            graphics.setLineStyle(Graphics.LINE_DOT);

            int[] points = new int[6];

            points[0] = 0 + offset;
            points[1] = 0;
            points[2] = bounds.width - 1;
            points[3] = 0;
            points[4] = bounds.width - 1;
            points[5] = bounds.height - 1;

            graphics.drawPolyline(points);

            points[0] = 0;
            points[1] = 0 + offset;
            points[2] = 0;
            points[3] = bounds.height - 1;
            points[4] = bounds.width - 1;
            points[5] = bounds.height - 1;

            graphics.drawPolyline(points);

            graphics.translate(getLocation().getNegated());

            if (schedulePaint) {
                Display.getCurrent().timerExec(DELAY, new Runnable() {
                    @Override
                    public void run() {
                        offset++;
                        if (offset > 5) {
                            offset = 0;
                        }

                        schedulePaint = true;
                        repaint();
                    }
                });
            }

            schedulePaint = false;
        }

    }

    /**
     * Called when the mouse button is released. Overridden to do nothing, since
     * a drag tracker does not need to unload when finished.
     */
    @Override
    protected void handleFinished() {
    }
}
