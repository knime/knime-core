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
 * Represents an ellipse defined by two points, the lower-left and the 
 * upper-right corner, different from normal Java graphics behavior!
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class BasicEllipse extends Basic2DDrawingElement {
    
    
    
    /**
     * 
     * @param filled true if the ellipse should be filled.
     */
    public BasicEllipse(final boolean filled) {
        super(filled);
    }

    
    /**
     * Paints and ellipse that it fills a rectangle defined by two points: 
     * the lower-left and the upper -right corner.
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
            g2.fillOval(p1.x, p2.y, width, height);
        } else {
            g2.drawOval(p1.x, p2.y, width, height);
        }
        g2.setColor(backupColor);
        g2.setStroke(backupStroke);
    }
    

}
