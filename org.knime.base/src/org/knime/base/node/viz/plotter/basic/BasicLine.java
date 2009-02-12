/*
 * ------------------------------------------------------------------
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
 *   05.09.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.basic;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.util.List;

/**
 * Represents a line by a list of points which are connected to one line. Hence,
 * the ordering of the points in the list is important.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class BasicLine extends BasicDrawingElement {

    
    private int m_lineWidth = 1;
    
    
    

    /**
     * Paints the line by connecting all points. 
     * 
     * @see org.knime.base.node.viz.plotter.basic.BasicDrawingElement
     * #paint(java.awt.Graphics2D)
     */
    @Override
    public void paint(final Graphics2D g) {
        Color backupColor = g.getColor();
        Stroke backupStroke = g.getStroke();
        g.setColor(getColor());
        g.setStroke(new BasicStroke(m_lineWidth));
        int[] x = new int[getPoints().size()];
        int[] y = new int[getPoints().size()];
        for (int i = 0; i < getPoints().size(); i++) {
            x[i] = getPoints().get(i).x;
            y[i] = getPoints().get(i).y;
        }
        g.drawPolyline(x, y, getPoints().size());
        g.setColor(backupColor);
        g.setStroke(backupStroke);
    }
    
    
    
   /**
    * {@inheritDoc}
    */
    @Override
    public void setPoints(final List<Point> points) {
        super.setPoints(points);
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public void setPoints(final Point... points) {
        super.setPoints(points);
    }


    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setStroke(final Stroke stroke) {
        super.setStroke(stroke);
        m_lineWidth = (int)((BasicStroke)stroke).getLineWidth();
    }





}
