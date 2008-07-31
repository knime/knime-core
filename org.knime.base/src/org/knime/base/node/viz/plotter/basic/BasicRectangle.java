/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 *   06.09.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.basic;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;

/**
 * Represents a rectangle which is defined by two points, the upper-left
 * and the lower-right corner as known from Java Graphics.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class BasicRectangle extends Basic2DDrawingElement {

    
    /**
     * 
     * @param filled if the rectangle should be filled.
     */
    public BasicRectangle(final boolean filled) {
        super(filled);
    }
    
    /**
     * Paints a rectangle defined by two points, the upper-left and lower-right
     * corner.
     * 
     * @see org.knime.base.node.viz.plotter.basic.BasicDrawingElement#paint(
     * java.awt.Graphics2D)
     */
    @Override
    public void paint(final Graphics2D g2) {
        if (getPoints() == null || getPoints().size() < 2) {
            return;
        }
        Color backupColor = g2.getColor();
        Stroke backupStroke = g2.getStroke();
        g2.setColor(getColor());
        g2.setStroke(getStroke());
        Point p1 = getPoints().get(0);
        Point p2 = getPoints().get(1);
        int width = p2.x - p1.x;
        int height = p1.y - p2.y;
        if (isFilled()) {
            g2.fillRect(p1.x, p1.y, width, height);
        } else {
            g2.drawRect(p1.x, p1.y, width, height);
        }
        g2.setColor(backupColor);
        g2.setStroke(backupStroke);
    }

}
