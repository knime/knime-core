/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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


/**
 *
 * @author Tobias Koetter, University of Konstanz
 */
public final class GeometryUtil {

    private GeometryUtil() {
        //avoid object creation
    }

    /**
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
            throw new IllegalArgumentException("Fraction < 0 || > 1");
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
        final double hiliteExtend = calculatePartialExtent(arc, fraction);
        final Arc2D hiliteArc =
            new Arc2D.Double(arc.getBounds(), arc.getAngleStart(),
                    hiliteExtend, Arc2D.PIE);
        return hiliteArc;
    }

    /**
     * @param arc the basic arc
     * @param fraction the fraction defines the size of the new arc in
     * comparison to the basic arc in percentage 0.9 = 90%
     * @return the fraction of the basic arc extend
     */
    public static double calculatePartialExtent(final Arc2D arc,
            final double fraction) {
        return arc.getAngleExtent() * fraction;
    }
}
