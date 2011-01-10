/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 *   ${date} (${user}): created
 */
package org.knime.workbench.editor2.editparts.policy;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.AbsoluteBendpoint;
import org.eclipse.draw2d.Bendpoint;
import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.editpolicies.SelectionHandlesEditPolicy;
import org.eclipse.gef.requests.BendpointRequest;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.commands.NewBendpointCreateCommand;
import org.knime.workbench.editor2.commands.NewBendpointDeleteCommand;
import org.knime.workbench.editor2.commands.NewBendpointMoveCommand;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;
import org.knime.workbench.editor2.editparts.snap.ConnectionBendpointCreationHandel;
import org.knime.workbench.editor2.editparts.snap.ConnectionBendpointMoveHandel;

/**
 * Copied code (BendpointEditPolicy)!! This is to change the BendpointMoveHandel
 * neccessary to return a Bendpoint tracker that makes it possible to change the
 * update method to change bendpoint locations during dragging. This all is done
 * for snapping functionality of bendpoints.
 * 
 * Bendpoint policy, needed for creation of the add/delete/move commands of the
 * connections bendpoints.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class ConnectionBendpointEditPolicy extends SelectionHandlesEditPolicy
        implements PropertyChangeListener {

    private static final List<Object> NULL_CONSTRAINT = new ArrayList<Object>();

    private List<Object> m_originalConstraint;

    private boolean m_isDeleting = false;

    private static final Point REF1 = new Point();

    private static final Point REF2 = new Point();

    /**
     * <code>activate()</code> is extended to add a listener to the
     * <code>Connection</code> figure.
     * 
     * @see org.eclipse.gef.EditPolicy#activate()
     */
    @Override
    public void activate() {
        super.activate();
        getConnection().addPropertyChangeListener(Connection.PROPERTY_POINTS,
                this);
    }

    private List createHandlesForAutomaticBendpoints() {
        List<ConnectionBendpointCreationHandel> list = 
            new ArrayList<ConnectionBendpointCreationHandel>();
        ConnectionEditPart connEP = (ConnectionEditPart)getHost();
        PointList points = getConnection().getPoints();
        for (int i = 0; i < points.size() - 1; i++) {
            list.add(new ConnectionBendpointCreationHandel(connEP, 0, i));
        }

        return list;
    }

    private List createHandlesForUserBendpoints() {
        List<Object> list = new ArrayList<Object>();
        ConnectionEditPart connEP = (ConnectionEditPart)getHost();
        PointList points = getConnection().getPoints();
        List bendPoints = (List)getConnection().getRoutingConstraint();
        int bendPointIndex = 0;
        Point currBendPoint = null;

        if (bendPoints == null) {
            bendPoints = NULL_CONSTRAINT;
        } else if (!bendPoints.isEmpty()) {
            currBendPoint = ((Bendpoint)bendPoints.get(0)).getLocation();
        }

        for (int i = 0; i < points.size() - 1; i++) {
            // Put a create handle on the middle of every segment
            list.add(new ConnectionBendpointCreationHandel(connEP,
                    bendPointIndex, i));

            // If the current user bendpoint matches a bend location, show a
            // move handle
            if (i < points.size() - 1 && bendPointIndex < bendPoints.size()
                    && currBendPoint.equals(points.getPoint(i + 1))) {
                list.add(new ConnectionBendpointMoveHandel(connEP,
                        bendPointIndex, i + 1));

                // Go to the next user bendpoint
                bendPointIndex++;
                if (bendPointIndex < bendPoints.size()) {
                    currBendPoint = ((Bendpoint)bendPoints.get(bendPointIndex))
                            .getLocation();
                }
            }
        }

        return list;
    }

    /**
     * Creates selection handles for the bendpoints. Explicit (user-defined)
     * bendpoints will have {@link org.eclipse.gef.handles.BendpointMoveHandle}s
     * on them with a single
     * {@link org.eclipse.gef.handles.BendpointCreationHandle} between 2
     * consecutive explicit bendpoints. If implicit bendpoints (such as those
     * created by the {@link org.eclipse.draw2d.AutomaticRouter}) are used, one
     * {@link org.eclipse.gef.handles.BendpointCreationHandle} is placed in the
     * middle of the Connection.
     * 
     * @see SelectionHandlesEditPolicy#createSelectionHandles()
     */
    @Override
    protected List createSelectionHandles() {
        List list = new ArrayList();
        if (isAutomaticallyBending()) {
            list = createHandlesForAutomaticBendpoints();
        } else {
            list = createHandlesForUserBendpoints();
        }
        return list;
    }

    /**
     * <code>deactivate()</code> is extended to remove the property change
     * listener on the <code>Connection</code> figure.
     * 
     * @see org.eclipse.gef.EditPolicy#deactivate()
     */
    @Override
    public void deactivate() {
        getConnection().removePropertyChangeListener(
                Connection.PROPERTY_POINTS, this);
        super.deactivate();
    }

    /**
     * Erases all bendpoint feedback. Since the original <code>Connection</code>
     * figure is used for feedback, we just restore the original constraint that
     * was saved before feedback started to show.
     * 
     * @param request the BendpointRequest
     */
    protected void eraseConnectionFeedback(final BendpointRequest request) {
        restoreOriginalConstraint();
        m_originalConstraint = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void eraseSourceFeedback(final Request request) {
        if (REQ_MOVE_BENDPOINT.equals(request.getType())
                || REQ_CREATE_BENDPOINT.equals(request.getType())) {
            eraseConnectionFeedback((BendpointRequest)request);
        }
    }

    /**
     * Factors the Request into either a MOVE, a DELETE, or a CREATE of a
     * bendpoint.
     * 
     * @see org.eclipse.gef.EditPolicy#getCommand(Request)
     */
    @Override
    public Command getCommand(final Request request) {
        if (REQ_MOVE_BENDPOINT.equals(request.getType())) {
            if (m_isDeleting) {
                return getDeleteBendpointCommand((BendpointRequest)request);
            }
            return getMoveBendpointCommand((BendpointRequest)request);
        }
        if (REQ_CREATE_BENDPOINT.equals(request.getType())) {
            return getCreateBendpointCommand((BendpointRequest)request);
        }
        return null;
    }

    /**
     * Convenience method for obtaining the host's <code>Connection</code>
     * figure.
     * 
     * @return the Connection figure
     */
    protected Connection getConnection() {
        return (Connection)((ConnectionEditPart)getHost()).getFigure();
    }

    private boolean isAutomaticallyBending() {
        List constraint = (List)getConnection().getRoutingConstraint();
        PointList points = getConnection().getPoints();
        return ((points.size() > 2) && (constraint == null || constraint
                .isEmpty()));
    }

    private boolean lineContainsPoint(final Point p1, final Point p2,
            final Point p) {
        int tolerance = 7;
        Rectangle rect = Rectangle.SINGLETON;
        rect.setSize(0, 0);
        rect.setLocation(p1.x, p1.y);
        rect.union(p2.x, p2.y);
        rect.expand(tolerance, tolerance);
        if (!rect.contains(p.x, p.y)) {
            return false;
        }

        int v1x, v1y, v2x, v2y;
        int numerator, denominator;
        double result = 0.0;

        if (p1.x != p2.x && p1.y != p2.y) {

            v1x = p2.x - p1.x;
            v1y = p2.y - p1.y;
            v2x = p.x - p1.x;
            v2y = p.y - p1.y;

            numerator = v2x * v1y - v1x * v2y;
            denominator = v1x * v1x + v1y * v1y;

            result = ((numerator << 10) / denominator * numerator) >> 10;
        }

        // if it is the same point, and it passes the bounding box test,
        // the result is always true.
        return result <= tolerance * tolerance;
    }

    /**
     * If the number of bendpoints changes, handles are updated.
     * 
     * @see java.beans.PropertyChangeListener
     *      #propertyChange(PropertyChangeEvent)
     */
    public void propertyChange(final PropertyChangeEvent evt) {
        // $TODO optimize so that handles aren't added constantly.
        if (getHost().getSelected() != EditPart.SELECTED_NONE) {
            addSelectionHandles();
        }
    }

    /**
     * Restores the original constraint that was saved before feedback began to
     * show.
     */
    protected void restoreOriginalConstraint() {
        if (m_originalConstraint != null) {
            if (m_originalConstraint == NULL_CONSTRAINT) {
                getConnection().setRoutingConstraint(null);
            } else {
                getConnection().setRoutingConstraint(m_originalConstraint);
            }
        }
    }

    /**
     * Since the original figure is used for feedback, this method saves the
     * original constraint, so that is can be restored when the feedback is
     * erased.
     */
    protected void saveOriginalConstraint() {
        m_originalConstraint = (List<Object>)getConnection().getRoutingConstraint();
        if (m_originalConstraint == null) {
            m_originalConstraint = NULL_CONSTRAINT;
        }
        getConnection().setRoutingConstraint(
                new ArrayList<Object>(m_originalConstraint));
    }

    private void setReferencePoints(final BendpointRequest request) {
        PointList points = getConnection().getPoints();
        int bpIndex = -1;
        List bendPoints = (List)getConnection().getRoutingConstraint();
        Point bp = ((Bendpoint)bendPoints.get(request.getIndex()))
                .getLocation();

        int smallestDistance = -1;

        for (int i = 0; i < points.size(); i++) {
            if (smallestDistance == -1
                    || points.getPoint(i).getDistance2(bp) < smallestDistance) {
                bpIndex = i;
                smallestDistance = points.getPoint(i).getDistance2(bp);
                if (smallestDistance == 0) {
                    break;
                }
            }
        }

        if (bpIndex > 0) {
            points.getPoint(REF1, bpIndex - 1);
        }
        getConnection().translateToAbsolute(REF1);

        if (bpIndex < points.size() - 1) {
            points.getPoint(REF2, bpIndex + 1);
        }
        getConnection().translateToAbsolute(REF2);
    }

    /**
     * Shows feedback when a bendpoint is being created. The original figure is
     * used for feedback and the original constraint is saved, so that it can be
     * restored when feedback is erased.
     * 
     * @param request the BendpointRequest
     */
    protected void showCreateBendpointFeedback(final BendpointRequest request) {
        Point p = new Point(request.getLocation());
        List<Object> constraint;
        getConnection().translateToRelative(p);
        Bendpoint bp = new AbsoluteBendpoint(p);
        if (m_originalConstraint == null) {
            saveOriginalConstraint();
            constraint = (List<Object>)getConnection().getRoutingConstraint();
            constraint.add(request.getIndex(), bp);
        } else {
            constraint = (List<Object>)getConnection().getRoutingConstraint();
        }
        constraint.set(request.getIndex(), bp);
        getConnection().setRoutingConstraint(constraint);
    }

    /**
     * Shows feedback when a bendpoint is being deleted. This method is only
     * called once when the bendpoint is first deleted, not every mouse move.
     * The original figure is used for feedback and the original constraint is
     * saved, so that it can be restored when feedback is erased.
     * 
     * @param request the BendpointRequest
     */
    protected void showDeleteBendpointFeedback(final BendpointRequest request) {
        if (m_originalConstraint == null) {
            saveOriginalConstraint();
            List constraint = (List)getConnection().getRoutingConstraint();
            constraint.remove(request.getIndex());
            getConnection().setRoutingConstraint(constraint);
        }
    }

    /**
     * Shows feedback when a bendpoint is being moved. Also checks to see if the
     * bendpoint should be deleted and then calls
     * {@link #showDeleteBendpointFeedback(BendpointRequest)} if needed. The
     * original figure is used for feedback and the original constraint is
     * saved, so that it can be restored when feedback is erased.
     * 
     * @param request the BendpointRequest
     */
    protected void showMoveBendpointFeedback(final BendpointRequest request) {
        Point p = new Point(request.getLocation());
        if (!m_isDeleting) {
            setReferencePoints(request);
        }

        if (lineContainsPoint(REF1, REF2, p)) {
            if (!m_isDeleting) {
                m_isDeleting = true;
                eraseSourceFeedback(request);
                showDeleteBendpointFeedback(request);
            }
            return;
        }
        if (m_isDeleting) {
            m_isDeleting = false;
            eraseSourceFeedback(request);
        }
        if (m_originalConstraint == null) {
            saveOriginalConstraint();
        }
        List<Object> constraint = (List<Object>)getConnection()
                .getRoutingConstraint();
        getConnection().translateToRelative(p);
        Bendpoint bp = new AbsoluteBendpoint(p);
        constraint.set(request.getIndex(), bp);
        getConnection().setRoutingConstraint(constraint);
    }

    /**
     * Shows feedback when appropriate. Calls a different method depending on
     * the request type.
     * 
     * @see #showCreateBendpointFeedback(BendpointRequest)
     * @see #showMoveBendpointFeedback(BendpointRequest)
     * @param request the Request
     */
    @Override
    public void showSourceFeedback(final Request request) {
        if (REQ_MOVE_BENDPOINT.equals(request.getType())) {
            showMoveBendpointFeedback((BendpointRequest)request);
        } else if (REQ_CREATE_BENDPOINT.equals(request.getType())) {
            showCreateBendpointFeedback((BendpointRequest)request);
        }
    }
    
    /** @return The workflow manager associated with the host. */
    public WorkflowManager getWorkflowManager() {
        // we need the workflow manager
        // This is a bit tricky here, as the parent of the connection's edit
        // part is the ScalableFreefromEditPart. We need to get the first (and
        // only) child to get a reference to "our" root (WorkflowRootEditPart)
        return ((WorkflowRootEditPart) getHost().getRoot().getChildren().get(0))
            .getWorkflowManager();
    }

    /**
     * {@inheritDoc}
     */
    protected Command getCreateBendpointCommand(final BendpointRequest req) {
        int index = req.getIndex();
        Point loc = req.getLocation();
        ConnectionContainerEditPart editPart 
            = (ConnectionContainerEditPart)getHost();

        ZoomManager zoomManager = (ZoomManager)getHost().getRoot().getViewer()
                .getProperty(ZoomManager.class.toString());

        return new NewBendpointCreateCommand(editPart, getWorkflowManager(), 
                index, loc, zoomManager);
    }

    /**
     * {@inheritDoc}
     */
    protected Command getDeleteBendpointCommand(final BendpointRequest req) {
        // get the index of the bendpoint to delete
        int index = req.getIndex();
        ConnectionContainerEditPart editPart 
            = (ConnectionContainerEditPart)getHost();
        WorkflowManager wfm = getWorkflowManager();
        return new NewBendpointDeleteCommand(editPart, wfm, index);
    }

    /**
     * {@inheritDoc}
     */
    protected Command getMoveBendpointCommand(final BendpointRequest request) {
        // index of the bendpoint to move
        int index = request.getIndex();
        Point loc = request.getLocation();
        ConnectionContainerEditPart edit 
            = (ConnectionContainerEditPart)getHost();

        ZoomManager zoomManager = (ZoomManager)getHost().getRoot().getViewer()
                .getProperty(ZoomManager.class.toString());
        WorkflowManager m = getWorkflowManager();
        return new NewBendpointMoveCommand(edit, m, index, loc, zoomManager);
    }
}
