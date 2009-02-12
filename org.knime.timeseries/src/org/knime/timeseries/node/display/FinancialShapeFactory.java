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
 *   13.02.2007 (rs): created
 *   
 *   This class implements the dot shapes for financial points.
 *   Points in financial charts can be candlestick-like or barchart-like
 *   
 */
package org.knime.timeseries.node.display;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.property.ColorAttr;


/**
 * Abstract class for different drawable shapes.
 * 
 * @author Rosaria Silipo
 */
public final class FinancialShapeFactory {
    
    /** Name of and key for the rectangle. */
    public static final String CANDLESTICK_BAR = "Candlestick Bar";
    /** Name of and key for the vertical bar. */
    public static final String VERTICAL_BAR = "Vertical Bar";
    /** Name of and key for the default shape. */
    public static final String DEFAULT = "Default";
    
    private static Map<String, Shape> shapes;
    
    static {
        shapes = new LinkedHashMap<String, Shape>();
        shapes.put(CANDLESTICK_BAR, new CandlestickBar());
        shapes.put(VERTICAL_BAR, new VerticalBar());
        shapes.put(DEFAULT, new VerticalBar());
    }
    
    private FinancialShapeFactory() {
        
    }
    
    
    /**
     * 
     * @return all registered shapes.
     */
    public static Set<Shape> getShapes() {
        Set<Shape> result = new LinkedHashSet<Shape>();
        result.addAll(shapes.values());
        return result;
    }
    
    /**
     * 
     * @param name the name of the shape (also shape.toString() value).
     * @return the referring shape or a rectangle if the name couldn't resolved.
     */
    public static Shape getShape(final String name) {
        Shape result = shapes.get(name);
        if (result == null) {
            return shapes.get(FinancialShapeFactory.DEFAULT);
        }
        return result;
    }
    
    /**
     * Abstract implementation of a shape. Handles all common attributes such as
     * position, dimension, color, etc. All implementing classes have to
     * provide a possibility to get a new instance and to paint themselves.
     * 
     * @author Rosaria Silipo
     */
    public abstract static class Shape {
        
//        private static final int DEFAULT_SIZE = 11;
        
        private static final double BORDER_SIZE = 0.8;
        
        private static final float DASH_FACTOR = 0.8f;
              
        /**
         * Paints the hilite border.
         * @param g the graphics object.
         * @param x the x center position
         * @param highPrice 
         * @param lowPrice 
         * @param hilited falg whether the shape is hilited
         * @param selected flag whether the dot is selected
         */
         public void paintBorder(final Graphics g, final int x, 
                final int highPrice, final int lowPrice,
                final boolean hilited, 
                final boolean selected) {
            
            int size = 2;
            
            int borderSize = (int)Math.ceil(BORDER_SIZE * size);
            if (borderSize < 1) {
                borderSize = 1;
            }
            float dash = DASH_FACTOR * size;
            if (dash == 0) {
                dash = 1;
            }
            Color backupColor = g.getColor();
            int rectX = (int)(x - (size / 2.0)) - borderSize;
            int rectY = highPrice - borderSize;
            
            Graphics2D g2 = (Graphics2D)g;
            Stroke backupStroke = g2.getStroke();
            if (hilited) {
                g2.setColor(ColorAttr.HILITE);
                Stroke stroke = new BasicStroke(borderSize);
                g2.setStroke(stroke);
                g2.drawRect(rectX, rectY, size + (2 * borderSize), 
                        (lowPrice - highPrice) + (2 * borderSize));
            }
            if (selected) {
                g2.setColor(Color.BLACK);
                Stroke selectionStroke = new BasicStroke(borderSize, 
                        BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1.0f, 
                        new float[]{dash}, 0);
                g2.setStroke(selectionStroke);
                g2.drawRect(rectX, rectY, size + (2 * borderSize), 
                        (lowPrice - highPrice) + (2 * borderSize));                
            }
            g2.setColor(backupColor);
            g2.setStroke(backupStroke);
        }
        
        /**
         * Paints the dot and if hilited a border around the dot.
         * @param g the graphics object.
         * @param x the x position (center of the shape)
         * @param openPrice the y position (center of the shape)
         * @param closePrice the size (width and height)
         * @param highPrice 
         * @param lowPrice 
         * @param color the normal color of the shape
         * @param hilited flag whether dot is hilited
         * @param selected flag whether dot is selected.
         * @param faded flag whether point is faded.
         */
        public void paint(final Graphics g, final int x, 
                final int openPrice, final int closePrice,
                final int highPrice, final int lowPrice,
                final Color color,
                final boolean hilited, final boolean selected, 
                final boolean faded) {
            Color backupColor = g.getColor();
            if (faded && !hilited) {
                if (!selected) {
                    g.setColor(ColorAttr.INACTIVE);
                } else {
                    g.setColor(ColorAttr.INACTIVE_SELECTED);                
                }
            } else {
                g.setColor(color);
            }
            paintShape(g, x, 
                    openPrice, closePrice, highPrice, lowPrice, 
                    selected, hilited);
            if (hilited && !faded || selected) {
                paintBorder(g, x, highPrice, lowPrice, hilited, selected);
            }
            g.setColor(backupColor);
        }
        

        /**
         * Paints the shape.
         * @param g the graphics object
         * @param x the center x position
         * @param openPrice the center y position
         * @param closePrice the dimension of the shape
         * @param highPrice 
         * @param lowPrice 
         * @param hilited flag whether the shape is hilited
         * @param selected flag whether the shape is selected
         */
        public abstract void paintShape(final Graphics g,
                final int x, final int openPrice, final int closePrice, 
                final int highPrice, final int lowPrice,
                final boolean selected, final boolean hilited);
        
        /**
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public abstract String toString();
        
    }
    
    /* ------------- the shape implementations: ------------*/

    
    // Vertical Bar (Barchart)
    /**
     * 
     * @author Rosaria Silipo
     */
    private static class VerticalBar extends Shape {

        /**
         * {@inheritDoc}
         */
        @Override
        public void paintShape(final Graphics g, final int x, 
                final int openPrice, final int closePrice, 
                final int highPrice, final int lowPrice, 
                final boolean selected, final boolean hilited) {
            
            int thickness = 2;
            int size = 2;
            
            Graphics2D g2 = (Graphics2D)g;
            
            Stroke backupStroke = g2.getStroke();
            
            // draw thick horizontal line for close price
            Stroke thickStroke;
            if (selected || hilited) {
                thickStroke = new BasicStroke(thickness * 2);
            } else {
                thickStroke = new BasicStroke(thickness);
            }
            g2.setStroke(thickStroke);
            int x1 = x - (size / 2);
            int x2 = x + (size / 2);
            g.drawLine(x1, closePrice, x2, closePrice);
            g2.setStroke(backupStroke);
            
            if (selected || hilited) {
                g2.setStroke(new BasicStroke(2));
            }
            int y1 = highPrice;
            int y2 = lowPrice;
            g.drawLine(x, y1, x, y2);
            
            g2.setStroke(backupStroke);        

        }

        /**
         * 
         * @see org.knime.core.data.property.ShapeFactory.Shape#toString()
         */
        @Override
        public String toString() {
            return VERTICAL_BAR;
        }

    }
    // Candlestick Bar 
    /**
     * 
     * @author Fabian Dill, University of Konstanz
     */
    private static class CandlestickBar extends Shape { 

        /**
         * {@inheritDoc}
         */
        @Override
        public void paintShape(final Graphics g, final int x, 
                final int openPrice, final int closePrice, 
                final int highPrice, final int lowPrice,
                final boolean selected, final boolean hilited) {
            
            int size = 2;
                        
            // draw rectangle with open and close price here
            int rectX = (int)(x - (size / 2.0));
            int sizeX = size;
            int rectY = -1;
            int sizeY = -1;
            
            if (openPrice > closePrice) {
                // color black
                rectY = closePrice;
                sizeY = openPrice;
            } else {
                // color white
                rectY = openPrice;
                sizeY = closePrice;
            }              
            
            g.fillRect(rectX, rectY, sizeX, sizeY);
        }


        /**
         * 
         * @see org.knime.core.data.property.ShapeFactory.Shape#toString()
         */
        @Override
        public String toString() {
            return CANDLESTICK_BAR;
        }

    }
}
