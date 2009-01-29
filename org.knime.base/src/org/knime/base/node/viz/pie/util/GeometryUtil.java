/*
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
 *    14.09.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.pie.util;

import java.awt.geom.Arc2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.knime.base.node.viz.pie.datamodel.PieVizModel;


/**
 * Helper class for geometric calculations.
 * @author Tobias Koetter, University of Konstanz
 */
public final class GeometryUtil {

    private GeometryUtil() {
        //avoid object creation
    }

    /**
     * Calculates a sub arc that lies in the given arc.
     * @param arc the basic arc
     * @param fraction the fraction defines the size of the new arc in
     * comparison to the basic arc in percentage 0.9 = 90%
     * @return the arc
     */
    public static Arc2D calculateSubArc(final Arc2D arc,
            final double fraction) {
        if (arc == null) {
            return null;
        }
        if (fraction < 0 || fraction > 1) {
            throw new IllegalArgumentException("Fraction < 0 & > 1");
        }
        if (fraction <= 0) {
            return null;
        }
//        System.out.println("bounds:\t\t\t" + arc.getBounds());
//        System.out.println("bounds2d:\t\t\t" + arc.getBounds2D());
//        System.out.println("startPoint:\t\t\t" + arc.getStartPoint());
//        System.out.println("endPoint:\t\t\t" + arc.getEndPoint());
//        System.out.println("x:\t\t\t" + arc.getX());
//        System.out.println("y:\t\t\t" + arc.getY());
//        System.out.println("width:\t\t\t" + arc.getWidth());
//        System.out.println("height:\t\t\t" + arc.getHeight());
//        System.out.println("maxX:\t\t\t" + arc.getMaxX());
//        System.out.println("maxY:\t\t\t" + arc.getMaxY());
//        System.out.println("minX:\t\t\t" + arc.getMinX());
//        System.out.println("minY:\t\t\t" + arc.getMinY());
//        System.out.println("centerX:\t\t\t" + arc.getCenterX());
//        System.out.println("centerY:\t\t\t" + arc.getCenterY());
//        System.out.println("angleExtent:\t\t\t" + arc.getAngleExtent());
//        System.out.println("angleStart:\t\t\t" + arc.getAngleStart());
        double hiliteExtend = calculatePartialExtent(arc, fraction);
        if (hiliteExtend < PieVizModel.MINIMUM_ARC_ANGLE) {
            hiliteExtend = PieVizModel.MINIMUM_ARC_ANGLE;
        }
        final Arc2D hiliteArc =
            new Arc2D.Double(arc.getBounds(), arc.getAngleStart(),
                    hiliteExtend, Arc2D.PIE);
        return hiliteArc;
    }

    /**
     * @param arc the basic arc
     * @param fraction the fraction defines the size of the extend
     * in comparison to the extend of the basic arc in percentage 0.9 = 90%
     * @return the fraction of the basic arc extend
     */
    public static double calculatePartialExtent(final Arc2D arc,
            final double fraction) {
        return arc.getAngleExtent() * fraction;
    }

    /**
     * Returns a rectangle that can be used to create a pie section (taking
     * into account the amount by which the pie section is 'exploded').
     *
     * @param unexploded  the area inside which the unexploded pie sections are
     *                    drawn.
     * @param exploded  the area inside which the exploded pie sections are
     *                  drawn.
     * @param angle  the start angle.
     * @param extent  the extent of the arc.
     * @param explodePercent  the percent the new arc should reach into the
     * explode area. 1.0 means the section should be shifted to the border of
     * the explode area.
     *
     * @return A rectangle that can be used to create a pie section.
     */
    public static Rectangle2D getArcBounds(final Rectangle2D unexploded,
            final Rectangle2D exploded, final double angle, final double extent,
            final double explodePercent) {
        if (explodePercent == 0.0) {
            return unexploded;
        }
        final Arc2D arc1 = new Arc2D.Double(
            unexploded, angle, extent / 2, Arc2D.OPEN
        );
        final Point2D point1 = arc1.getEndPoint();
        final Arc2D.Double arc2 = new Arc2D.Double(
            exploded, angle, extent / 2, Arc2D.OPEN
        );
        final Point2D point2 = arc2.getEndPoint();
        final double deltaX = (point1.getX() - point2.getX()) * explodePercent;
        final double deltaY = (point1.getY() - point2.getY()) * explodePercent;
        return new Rectangle2D.Double(
            unexploded.getX() - deltaX, unexploded.getY() - deltaY,
            unexploded.getWidth(), unexploded.getHeight()
        );
    }

    /**
     * Calculated the mid angle of the given arc.
     * @param arc the {@link Arc2D} to calculate the mid angle for
     * @return the mid angle of the given arc
     */
    public static double calculateMidAngle(final Arc2D arc) {
        if (arc == null) {
            throw new NullPointerException("Arc must not be null");
        }
        final double startAngle = arc.getAngleStart();
        final double angle = arc.getAngleExtent();
//        final double mid = startAngle + (value / 2 * angle / value);
        final double mid = startAngle + angle / 2.0;
        return mid;
    }
}
