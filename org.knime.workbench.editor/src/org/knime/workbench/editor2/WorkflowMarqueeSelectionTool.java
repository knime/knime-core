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
import org.eclipse.gef.SharedCursors;
import org.eclipse.gef.tools.AbstractTool;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Display;
import org.knime.workbench.editor2.editparts.AbstractWorkflowPortBarEditPart;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

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
public class WorkflowMarqueeSelectionTool extends AbstractTool implements
        DragTracker {

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
    public static final int BEHAVIOR_NODES_CONTAINED =
            new Integer(1).intValue();

    /**
     * This behaviour selects connections that intersect the marquee rectangle.
     * 
     * @since 3.1
     */
    public static final int BEHAVIOR_CONNECTIONS_TOUCHED =
            new Integer(2).intValue();

    /**
     * This behaviour selects nodes completely encompassed by the marquee
     * rectangle, and all connections between those nodes.
     * 
     * @since 3.1
     */
    public static final int BEHAVIOR_NODES_AND_CONNECTIONS =
            new Integer(3).intValue();

    /**
     * This behaviour selects nodes completely encompassed by the marquee
     * rectangle, and all connections between those nodes.
     * 
     * @since KNIME development (knime 1.2.2)
     */
    public static final int BEHAVIOR_NODES_AND_CONNECTIONS_TOUCHED =
            new Integer(3).intValue();

    static final int DEFAULT_MODE = 0;

    static final int TOGGLE_MODE = 1;

    static final int APPEND_MODE = 2;

    private Figure marqueeRectangleFigure;

    private Set allChildren = new HashSet();

    private Collection selectedEditParts;

    private Collection deselectedEditParts;

    private Collection alreadySelectedEditParts;

    private Request targetRequest;

    private int marqueeBehavior = BEHAVIOR_NODES_CONTAINED;

    private int mode;

    private static final Request MARQUEE_REQUEST =
            new Request(RequestConstants.REQ_SELECTION);

    /**
     * Creates a new MarqueeSelectionTool of default type
     * {@link #BEHAVIOR_NODES_CONTAINED}.
     */
    public WorkflowMarqueeSelectionTool() {
        setDefaultCursor(SharedCursors.CROSS);
        setUnloadWhenFinished(false);
    }

    /**
     * @see org.eclipse.gef.tools.AbstractTool#applyProperty(java.lang.Object,
     *      java.lang.Object)
     */
    protected void applyProperty(Object key, Object value) {
        if (PROPERTY_MARQUEE_BEHAVIOR.equals(key)) {
            if (value instanceof Integer)
                setMarqueeBehavior(((Integer)value).intValue());
            return;
        }
        super.applyProperty(key, value);
    }

    private void calculateConnections(Collection newSelections,
            Collection deselections) {
        // determine the currently selected nodes minus the ones that are to be
        // deselected
        Collection currentNodes = new HashSet();
        if (getSelectionMode() != DEFAULT_MODE) { // everything is deselected
            // in default mode
            Iterator iter =
                    getCurrentViewer().getSelectedEditParts().iterator();
            while (iter.hasNext()) {
                EditPart selected = (EditPart)iter.next();
                if (!(selected instanceof ConnectionEditPart)
                        && !deselections.contains(selected))
                    currentNodes.add(selected);
            }
        }
        // add new connections to be selected to newSelections
        Collection connections = new ArrayList();
        for (Iterator nodes = newSelections.iterator(); nodes.hasNext();) {
            GraphicalEditPart node = (GraphicalEditPart)nodes.next();
            for (Iterator itr = node.getSourceConnections().iterator(); itr
                    .hasNext();) {
                ConnectionEditPart sourceConn = (ConnectionEditPart)itr.next();
                if (sourceConn.getSelected() == EditPart.SELECTED_NONE
                        && (newSelections.contains(sourceConn.getTarget()) || currentNodes
                                .contains(sourceConn.getTarget())))
                    connections.add(sourceConn);
            }
            for (Iterator itr = node.getTargetConnections().iterator(); itr
                    .hasNext();) {
                ConnectionEditPart targetConn = (ConnectionEditPart)itr.next();
                if (targetConn.getSelected() == EditPart.SELECTED_NONE
                        && (newSelections.contains(targetConn.getSource()) || currentNodes
                                .contains(targetConn.getSource())))
                    connections.add(targetConn);
            }
        }
        newSelections.addAll(connections);
        // add currently selected connections that are to be deselected to
        // deselections
        connections = new HashSet();
        for (Iterator nodes = deselections.iterator(); nodes.hasNext();) {
            GraphicalEditPart node = (GraphicalEditPart)nodes.next();
            for (Iterator itr = node.getSourceConnections().iterator(); itr
                    .hasNext();) {
                ConnectionEditPart sourceConn = (ConnectionEditPart)itr.next();
                if (sourceConn.getSelected() != EditPart.SELECTED_NONE)
                    connections.add(sourceConn);
            }
            for (Iterator itr = node.getTargetConnections().iterator(); itr
                    .hasNext();) {
                ConnectionEditPart targetConn = (ConnectionEditPart)itr.next();
                if (targetConn.getSelected() != EditPart.SELECTED_NONE)
                    connections.add(targetConn);
            }
        }
        deselections.addAll(connections);
    }

    private void calculateNewSelection(Collection newSelections,
            Collection deselections) {
        Rectangle marqueeRect = getMarqueeSelectionRectangle();
        for (Iterator itr = getAllChildren().iterator(); itr.hasNext();) {
            GraphicalEditPart child = (GraphicalEditPart)itr.next();
            IFigure figure = child.getFigure();
            if (!child.isSelectable()
                    || child.getTargetEditPart(MARQUEE_REQUEST) != child
                    || !isFigureVisible(figure) || !figure.isShowing())
                continue;
            if (!(child instanceof NodeContainerEditPart
                    || child instanceof ConnectionContainerEditPart 
                    || child instanceof AbstractWorkflowPortBarEditPart)) {
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
            } else if (marqueeBehavior == BEHAVIOR_NODES_AND_CONNECTIONS_TOUCHED) {
                included = marqueeRect.intersects(r);
            } else {
                included = marqueeRect.contains(r);
            }

            if (included) {
                if (isToggle()) {
                    if (wasSelected(child)) {
                        // System.out.println("was selected");
                        deselections.add(child);
                    } else {
                        // System.out.println("was not selected");
                        newSelections.add(child);
                    }
                } else {
                    newSelections.add(child);
                }
                // if (child.getSelected() == EditPart.SELECTED_NONE
                // || getSelectionMode() != TOGGLE_MODE)
                // newSelections.add(child);
                // else {
                // deselections.add(child);
                // System.out.println("was selected");
                // }
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

        if (marqueeBehavior == BEHAVIOR_NODES_AND_CONNECTIONS
                || marqueeBehavior == BEHAVIOR_NODES_AND_CONNECTIONS_TOUCHED)
            calculateConnections(newSelections, deselections);
    }

    private boolean wasSelected(EditPart part) {
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
        if (selectedEditParts == null)
            return;
        Iterator oldEditParts = selectedEditParts.iterator();
        while (oldEditParts.hasNext()) {
            EditPart editPart = (EditPart)oldEditParts.next();
            editPart.eraseTargetFeedback(getTargetRequest());
        }
    }

    private Set getAllChildren() {
        if (allChildren.isEmpty())
            getAllChildren(getCurrentViewer().getRootEditPart(), allChildren);
        return allChildren;
    }

    private void getAllChildren(EditPart editPart, Set allChildren) {
        List children = editPart.getChildren();
        for (int i = 0; i < children.size(); i++) {
            GraphicalEditPart child = (GraphicalEditPart)children.get(i);
            if (marqueeBehavior == BEHAVIOR_NODES_CONTAINED
                    || marqueeBehavior == BEHAVIOR_NODES_AND_CONNECTIONS
                    || marqueeBehavior == BEHAVIOR_NODES_AND_CONNECTIONS_TOUCHED)
                allChildren.add(child);
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
    protected String getCommandName() {
        return REQ_SELECTION;
    }

    /**
     * @see org.eclipse.gef.tools.AbstractTool#getDebugName()
     */
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
        if (targetRequest == null)
            targetRequest = createTargetRequest();
        return targetRequest;
    }

    /**
     * @see org.eclipse.gef.tools.AbstractTool#handleButtonDown(int)
     */
    protected boolean handleButtonDown(int button) {
        if (!isGraphicalViewer())
            return true;
        if (button != 1) {
            setState(STATE_INVALID);
            handleInvalidInput();
        }
        if (stateTransition(STATE_INITIAL, STATE_DRAG_IN_PROGRESS)) {
            if (getCurrentInput().isModKeyDown(SWT.MOD1))
                setSelectionMode(TOGGLE_MODE);
            else if (getCurrentInput().isShiftKeyDown())
                setSelectionMode(APPEND_MODE);
            else
                setSelectionMode(DEFAULT_MODE);
        }
        alreadySelectedEditParts = new ArrayList();
        alreadySelectedEditParts.addAll(getCurrentViewer()
                .getSelectedEditParts());
        // System.out.println("already selected set");
        return true;
    }

    /**
     * @see org.eclipse.gef.tools.AbstractTool#handleButtonUp(int)
     */
    protected boolean handleButtonUp(int button) {
        if (stateTransition(STATE_DRAG_IN_PROGRESS, STATE_TERMINAL)) {
            eraseTargetFeedback();
            eraseMarqueeFeedback();
            performMarqueeSelect();
        }
        handleFinished();
        return true;
    }

    static void printCollection(Collection c) {
        for (Object o : c) {
            System.out.println("in list: " + o);
        }
    }

    boolean nodesAndConnectionsEqual(Collection c1, Collection c2) {
        if (c1.size() != c2.size()) {
            return false;
        }
        for (Object o : c1) {
            // only node and connection container parts are relevant
            if (o instanceof NodeContainerEditPart
                    || o instanceof ConnectionContainerEditPart
                    || o instanceof AbstractWorkflowPortBarEditPart) {
                // now check if o is also in c2
                boolean found = false;
                for (Object o2 : c2) {
                    if (o2 == o) {
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
     * @see org.eclipse.gef.tools.AbstractTool#handleDragInProgress()
     */
    protected boolean handleDragInProgress() {
        if (isInState(STATE_DRAG | STATE_DRAG_IN_PROGRESS)) {
            showMarqueeFeedback();
            eraseTargetFeedback();
            ArrayList previousSelection = new ArrayList();
            ArrayList previousDeselection = new ArrayList();
            try {
                if (selectedEditParts != null)
                    previousSelection.addAll(selectedEditParts);
                else
                    performMarqueeSelect();
                if (isToggle() && deselectedEditParts != null) {
                    previousDeselection.addAll(deselectedEditParts);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            // System.out.println("Already selected:");
            // printCollection(alreadySelectedEditParts);
            calculateNewSelection(selectedEditParts = new ArrayList(),
                    deselectedEditParts = new ArrayList());
            // System.out.println("Current selection");
            // printCollection(deselectedEditParts);
            // System.out.println("Previous selection");
            // printCollection(previousDeselection);
            showTargetFeedback();
            if (!nodesAndConnectionsEqual(previousSelection, selectedEditParts)
                    || !nodesAndConnectionsEqual(previousDeselection,
                            deselectedEditParts)) {
                // System.out.println("Previous selection differs");
                // System.out.println("Current selection");
                // printCollection(selectedEditParts);
                // System.out.println("Previous selection");
                // printCollection(previousSelection);
                // System.out.println("Selection changed");
                performMarqueeSelect();
            }
        }
        return true;
    }

    /**
     * @see org.eclipse.gef.tools.AbstractTool#handleFocusLost()
     */
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
    protected boolean handleKeyDown(KeyEvent e) {
        if (super.handleKeyDown(e))
            return true;
        if (getCurrentViewer().getKeyHandler() != null)
            return getCurrentViewer().getKeyHandler().keyPressed(e);
        return false;
    }

    private boolean isFigureVisible(IFigure fig) {
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
    protected boolean isViewerImportant(EditPartViewer viewer) {
        return viewer instanceof GraphicalViewer;
    }

    private void performMarqueeSelect() {
        EditPartViewer viewer = getCurrentViewer();
        Collection newSelections = new LinkedHashSet(), deselections =
                new HashSet();
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
     *            {@link #BEHAVIOR_NODES_CONTAINED} or
     *            {@link #BEHAVIOR_NODES_AND_CONNECTIONS}
     * @since 3.1
     */
    public void setMarqueeBehavior(int type) {
        if (type != BEHAVIOR_CONNECTIONS_TOUCHED
                && type != BEHAVIOR_NODES_CONTAINED
                && type != BEHAVIOR_NODES_AND_CONNECTIONS)
            throw new IllegalArgumentException(
                    "Invalid marquee behaviour specified."); //$NON-NLS-1$
        marqueeBehavior = type;
    }

    private void setSelectionMode(int mode) {
        this.mode = mode;
    }

    /**
     * @see org.eclipse.gef.Tool#setViewer(org.eclipse.gef.EditPartViewer)
     */
    public void setViewer(EditPartViewer viewer) {
        if (viewer == getCurrentViewer())
            return;
        super.setViewer(viewer);
        if (viewer instanceof GraphicalViewer)
            setDefaultCursor(SharedCursors.CROSS);
        else
            setDefaultCursor(SharedCursors.NO);
        // if (selectedEditParts == null) {
        // if (mode == TOGGLE_MODE)
        // selectedEditParts = getCurrentViewer().getSelectedEditParts();
        // }
    }

    private void showMarqueeFeedback() {
        Rectangle rect = getMarqueeSelectionRectangle().getCopy();
        getMarqueeFeedbackFigure().translateToRelative(rect);
        getMarqueeFeedbackFigure().setBounds(rect);
    }

    private void showTargetFeedback() {
        for (Iterator itr = selectedEditParts.iterator(); itr.hasNext();) {
            EditPart editPart = (EditPart)itr.next();
            editPart.showTargetFeedback(getTargetRequest());
        }
    }

    class MarqueeRectangleFigure extends Figure {

        private static final int DELAY = 110; // animation delay in

        // millisecond

        private int offset = 0;

        private boolean schedulePaint = true;

        /**
         * @see org.eclipse.draw2d.Figure#paintFigure(org.eclipse.draw2d.Graphics)
         */
        protected void paintFigure(Graphics graphics) {
            Rectangle bounds = getBounds().getCopy();
            graphics.translate(getLocation());

            graphics.setXORMode(true);
            graphics.setForegroundColor(ColorConstants.white);
            graphics.setBackgroundColor(ColorConstants.black);

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
                    public void run() {
                        offset++;
                        if (offset > 5)
                            offset = 0;

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
    protected void handleFinished() {
    }
}
