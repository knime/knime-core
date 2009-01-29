/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   11.07.2006 (sieb): created
 */
package org.knime.workbench.editor2.editparts.snap;

import java.util.Collections;
import java.util.List;

import org.eclipse.draw2d.Bendpoint;
import org.eclipse.draw2d.BendpointConnectionRouter;
import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.PrecisionPoint;

/**
 * Overrides the rout method to create a connection router, that snap of the
 * arrow when leaving a port and going into a port.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class SnapOffBendPointConnectionRouter extends BendpointConnectionRouter {
    private static final PrecisionPoint A_POINT = new PrecisionPoint();

    /**
     * Routes the {@link Connection}. Expects the constraint to be a List of
     * {@link org.eclipse.draw2d.Bendpoint Bendpoints}.
     * 
     * @param conn The connection to route
     */
    @Override
    public void route(final Connection conn) {
        PointList points = conn.getPoints();
        points.removeAllPoints();

        List bendpoints = (List)getConstraint(conn);
        if (bendpoints == null) {
            bendpoints = Collections.EMPTY_LIST;
        }

        Point ref1, ref2;

        if (bendpoints.isEmpty()) {
            ref1 = conn.getTargetAnchor().getReferencePoint();
            ref2 = conn.getSourceAnchor().getReferencePoint();
        } else {
            ref1 = new Point(((Bendpoint)bendpoints.get(0)).getLocation());
            conn.translateToAbsolute(ref1);
            ref2 = new Point(((Bendpoint)bendpoints.get(bendpoints.size() - 1))
                    .getLocation());
            conn.translateToAbsolute(ref2);
        }

        A_POINT.setLocation(conn.getSourceAnchor().getLocation(ref1));
        conn.translateToRelative(A_POINT);
        points.addPoint(A_POINT);

        // add a point that forces the arrow to leave the source anchor
        // in a horizontal way
        points.addPoint(A_POINT.translate(8, 0));

        for (int i = 0; i < bendpoints.size(); i++) {
            Bendpoint bp = (Bendpoint)bendpoints.get(i);
            points.addPoint(bp.getLocation());
        }

        A_POINT.setLocation(conn.getTargetAnchor().getLocation(ref2));
        conn.translateToRelative(A_POINT);

        // add a point that forces the arrow to get into the anchor
        // in a horizontal way
        points.addPoint(A_POINT.translate(-8, 0));

        A_POINT.setLocation(conn.getTargetAnchor().getLocation(ref2));
        conn.translateToRelative(A_POINT);

        points.addPoint(A_POINT);
        conn.setPoints(points);
    }
}
