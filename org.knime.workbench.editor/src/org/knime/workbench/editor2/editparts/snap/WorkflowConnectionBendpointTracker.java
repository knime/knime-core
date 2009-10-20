/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 *   14.07.2006 (sieb): created
 */
package org.knime.workbench.editor2.editparts.snap;

import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.requests.BendpointRequest;
import org.eclipse.gef.tools.ConnectionBendpointTracker;

import org.knime.workbench.editor2.WorkflowEditor;

/**
 * Updates the location of a dragging bendbpoint to snap to nice angles.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class WorkflowConnectionBendpointTracker extends
        ConnectionBendpointTracker {

    private ZoomManager m_zoomManager;

    /**
     * The sensitivity of the snapping. Corrections greater than this value will
     * not occur.
     */
    protected static final double THRESHOLD = 7.0001;

    /**
     * Constructs a tracker for the given connection and index.
     * 
     * @param editpart the connection
     * @param i the index of the bendpoint
     */
    public WorkflowConnectionBendpointTracker(
            final ConnectionEditPart editpart, final int i) {
        super(editpart, i);
        m_zoomManager = (ZoomManager)editpart.getRoot().getViewer()
                .getProperty(ZoomManager.class.toString());

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateSourceRequest() {
        BendpointRequest request = (BendpointRequest)getSourceRequest();

        // get the currently dragging bendpoint
        // PrecisionPoint dragPoint = new PrecisionPoint(getLocation());
        Point dragPoint = getLocation();
        WorkflowEditor.adaptZoom(m_zoomManager, dragPoint, false);

        // get the two points next to the dragging bendpoint from the
        // list of all points of the connection
        PointList pList = ((Connection)request.getSource().getFigure())
                .getPoints();

        // System.out.println("Number points: " + pList.size());

        Point[] neighbourPoints = getNeighbourPoints(dragPoint, pList);

        // if the neighbours could not be determined to not apply snapping
        if (neighbourPoints == null) {
            request.setLocation(getLocation());
            return;
        }

        // System.out.println("x vals: drag:" + dragPoint.x + " neig1: "
        // + neighbourPoints[0].x + "neig2: " + neighbourPoints[1].x);

        double xCorrection = 0.0;
        // check the drag point for all 4 vertical / horizontal lines
        // to snap to those lines
        double diff1 = Math.abs(dragPoint.x - neighbourPoints[0].x);
        if (diff1 < THRESHOLD) {
            xCorrection = diff1;
        }
        double diff2 = Math.abs(dragPoint.x - neighbourPoints[1].x);
        if (diff2 < THRESHOLD) {

            // always apply the smaller correction
            if (diff2 < diff1) {
                xCorrection = diff2;
            }
        }

        double yCorrection = 0.0;
        diff1 = Math.abs(dragPoint.y - neighbourPoints[0].y);
        if (diff1 < THRESHOLD) {
            yCorrection = diff1;
        }
        diff2 = Math.abs(dragPoint.y - neighbourPoints[1].y);
        if (diff2 < THRESHOLD) {

            // always apply the smaller correction
            if (diff2 < diff1) {
                yCorrection = diff2;
            }
        }

        // ((AbstractPortEditPart)request.getSource().getSource()).getFigure();
        request.setLocation(getLocation().translate((int)xCorrection,
                (int)yCorrection));
    }

    private Point[] getNeighbourPoints(final Point point,
            final PointList pointList) {

        Point[] neighbours = new Point[2];

        double bestDist = Double.MAX_VALUE;
        int indexOfClosestPoint = 0;
        for (int i = 0; i < pointList.size(); i++) {
            Point currentPoint = pointList.getPoint(i);

            double dist = currentPoint.getDistance(point);
            if (dist < bestDist) {

                indexOfClosestPoint = i;
                bestDist = dist;
            }
        }
        // senity checks
        if (indexOfClosestPoint <= 1
                || indexOfClosestPoint >= pointList.size() - 1) {
            return null;
        }
        neighbours[0] = pointList.getPoint(indexOfClosestPoint - 1);
        neighbours[1] = pointList.getPoint(indexOfClosestPoint + 1);
        return neighbours;
    }
}
