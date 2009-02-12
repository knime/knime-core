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
 *   13.09.2006 (Fabian Dill): created
 */
package org.knime.core.data.property;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.Icon;


/**
 * Abstract class for different drawable shapes.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public final class ShapeFactory {
    
    /** Name of and key for the rectangle. */
    public static final String RECTANGLE = "Rectangle";
    /** Name of and key for the circle. */
    public static final String CIRCLE = "Circle";
    /** Name of and key for the triangle. */
    public static final String TRIANGLE = "Triangle";
    /** Name of and key for the reverse triangle. */
    public static final String REVERSE_TRIANGLE = "Reverse Triangle";
    /** Name of and key for the diamond. */
    public static final String DIAMOND = "Diamond";
    /** Name of and key for the cross. */
    public static final String CROSS = "Cross";
    /** Name of and key for the asterisk. */
    public static final String ASTERISK = "Asterisk";
    /** Name of and key for the "X". */
    public static final String X_SHAPE = "X-Shape";
    /** Name of and key for the horizontal line. */
    public static final String HORIZONTAL_LINE = "Horizontal Line";
    /** Name of and key for the vertical line. */
    public static final String VERTICAL_LINE = "Vertical Line";
    /** Name of and key for the default shape. */
    public static final String DEFAULT = "Default";

    
    private static Map<String, Shape> shapes;
    
    static {
        shapes = new LinkedHashMap<String, Shape>();
        shapes.put(RECTANGLE, new Rectangle());
        shapes.put(CIRCLE, new Circle());
        shapes.put(TRIANGLE, new Triangle());
        shapes.put(REVERSE_TRIANGLE, new ReverseTriangle());
        shapes.put(DIAMOND, new Diamond());
        shapes.put(ASTERISK, new Asterisk());
        shapes.put(CROSS, new Cross());
        shapes.put(X_SHAPE, new XShape());
        shapes.put(HORIZONTAL_LINE, new HorizontalStroke());
        shapes.put(VERTICAL_LINE, new VerticalStroke());
    }
    
    private ShapeFactory() {
        
    }
    
    
    /**
     * 
     * @return all registered shapes.
     */
    public static Set<Shape> getShapes() {
        return new LinkedHashSet<Shape>(shapes.values());
    }
    
    /**
     * 
     * @param name the name of the shape (also shape.toString() value).
     * @return the referring shape or a rectangle if the name couldn't resolved.
     */
    public static Shape getShape(final String name) {
        Shape result = shapes.get(name);
        if (result == null) {
            return shapes.get(ShapeFactory.RECTANGLE);
        }
        return result;
    }
    
    /**
     * Abstract implementation of a shape. Handles all common attributes such as
     * position, dimension, color, etc. All implementing classes have to
     * provide a possibility to get a new instance and to paint themselves.
     * 
     * @author Fabian Dill, University of Konstanz
     */
    public abstract static class Shape {
        
        private static final int DEFAULT_SIZE = 11;
        
        private static final double BORDER_SIZE = 0.4;
        
        private static final float DASH_FACTOR = 0.4f;
        
        /**
         * 
         * @return the shape as an icon.
         */
        public Icon getIcon() {
            return new Icon() {
                public final int getIconHeight() {
                    return DEFAULT_SIZE;
                }
                
                public final int getIconWidth() {
                    return DEFAULT_SIZE;
                }
                
                public void paintIcon(final Component c, final Graphics g, 
                        final int x, final int y) {
                    int dotX = x + (DEFAULT_SIZE / 2);
                    int dotY = y + (DEFAULT_SIZE / 2);
                    g.setXORMode(Color.lightGray);
                    ((Graphics2D)g).setRenderingHint(
                            RenderingHints.KEY_ANTIALIASING, 
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    paint(g, dotX, dotY, DEFAULT_SIZE, 
                            ColorAttr.DEFAULT.getColor(), false, false, false);
                    g.setPaintMode();
                }
            };
        };
        
        
        /**
         * Paints the hilite border.
         * @param g the graphics object.
         * @param x the x center position
         * @param y the y center position
         * @param size the dimension of the shape
         * @param hilited falg whether the shape is hilited
         * @param selected flag whether the dot is selected
         */
        public void paintBorder(final Graphics g, final int x, 
                final int y, final int size, final boolean hilited, 
                final boolean selected) {
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
            int rectY = (int)(y - (size / 2.0)) - borderSize;
            Graphics2D g2 = (Graphics2D)g;
            Stroke backupStroke = g2.getStroke();
            if (hilited) {
                g2.setColor(ColorAttr.HILITE);
                Stroke stroke = new BasicStroke(borderSize);
                g2.setStroke(stroke);
                g2.drawRect(rectX, rectY, size + (2 * borderSize), 
                        size + (2 * borderSize));
            }
            if (selected) {
                g2.setColor(Color.BLACK);
                Stroke selectionStroke = new BasicStroke(borderSize, 
                        BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1.0f, 
                        new float[]{dash}, 0);
                g2.setStroke(selectionStroke);
                g2.drawRect(rectX, rectY, size + (2 * borderSize), 
                        size + (2 * borderSize));                
            }
            g2.setColor(backupColor);
            g2.setStroke(backupStroke);
        }
        
        /**
         * Paints the dot and if hilited a border around the dot.
         * @param g the graphics object.
         * @param x the x position (center of the shape)
         * @param y the y position (center of the shape)
         * @param size the size (width and height)
         * @param color the normal color of the shape
         * @param hilited flag whether dot is hilited
         * @param selected flag whether dot is selected.
         * @param faded flag whether point is faded.
         */
        public void paint(final Graphics g, final int x, final int y, 
                final int size, final Color color,
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
            if (size == 1) {
                g.fillRect(x - 1, y - 1, 1, 1);
            } else {
                paintShape(g, x, y, size, selected, hilited);
            }
            if (hilited && !faded || selected) {
                paintBorder(g, x, y, size, hilited, selected);
            }
            g.setColor(backupColor);
        }
        

        /**
         * Paints the shape.
         * @param g the graphics object
         * @param x the center x position
         * @param y the center y position
         * @param size the dimension of the shape
         * @param hilited flag whether the shape is hilited
         * @param selected flag whether the shape is selected
         */
        public abstract void paintShape(final Graphics g,
                final int x, final int y, final int size, 
                final boolean selected, final boolean hilited);
        
        /**
         * {@inheritDoc}
         */
        @Override
        public abstract String toString();
        
    }
    
    /* ------------- the shape implementations: ------------*/
    // Asterisk
    /**
     * 
     * @author Fabian Dill, University of Konstanz
     */
    private static class Asterisk extends Shape {

        /**
         * {@inheritDoc}
         */
        @Override
        public void paintShape(final Graphics g, final int x, final int y,
                final int size, final boolean selected, 
                final boolean hilited) {
            int x1 = x - (size / 2);
            int x2 = x;
            int x3 = x + (size / 2);
            int y1 = y - (size / 2);
            int y2 = y;
            int y3 = y + (size / 2);
            g.drawLine(x1, y3, x3, y1);
            g.drawLine(x1, y2, x3, y2);
            g.drawLine(x1, y1, x3, y3);
            g.drawLine(x2, y3, x2, y1);
            
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return ASTERISK;
        }

    }
    
    // Circle 
    /**
     * 
     * @author Fabian Dill, University of Konstanz
     */
    private static class Circle extends Shape {

        /**
         * {@inheritDoc}
         */
        @Override
        public void paintShape(final Graphics g, final int x, final int y,
                final int size, final boolean selected, 
                final boolean hilited) {
            int circleX = (int)(x - (size / 2.0));
            int circleY = (int)(y - (size / 2.0));
            g.fillOval(circleX, circleY, size, size);
        }
        

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return CIRCLE;
        }

    }
    // Cross
    /**
     * 
     * @author Fabian Dill, University of Konstanz
     */
    private static class Cross extends Shape {

        /**
         * {@inheritDoc}
         */
        @Override
        public void paintShape(final Graphics g, final int x, final int y, 
                final int size, final boolean selected, 
                final boolean hilited) {
            Graphics2D g2 = (Graphics2D)g;
            Stroke backupStroke = g2.getStroke();
            if (selected || hilited) {
                g2.setStroke(new BasicStroke(2));
            }
            int x1 = (int)(x - (size / 2.0));
            int x2 = x;
            int x3 = (int)(x + (size / 2.0));
            int y1 = (int)(y - (size / 2.0));
            int y2 = y;
            int y3 = (int)(y + (size / 2.0));
            g2.drawLine(x2, y1, x2, y3);
            g2.drawLine(x1, y2, x3, y2);
            g2.setStroke(backupStroke);
        }
        

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return CROSS;
        }

    }
    
    // Diamond
    /**
     * 
     * @author Fabian Dill, University of Konstanz
     */
    private static class Diamond extends Shape {

        /**
         * {@inheritDoc}
         */
        @Override
        public void paintShape(final Graphics g, final int x, final int y,
                final int size, final boolean selected, 
                final boolean hilited) {
            int x1 = x - (size / 2);
            int x2 = x;
            int x3 = x + (size / 2);
            int y1 = y - (size / 2);
            int y2 = y;
            int y3 = y + (size / 2);
            Polygon polygon = new Polygon();
            polygon.addPoint(x1, y2);
            polygon.addPoint(x2, y1);
            polygon.addPoint(x3, y2);
            polygon.addPoint(x2, y3);
            g.fillPolygon(polygon);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return DIAMOND;
        }

    }
    // horizontal stroke
    /**
     * 
     * @author Fabian Dill, University of Konstanz
     */
    private static class HorizontalStroke extends Shape {

        /**
         * {@inheritDoc}
         */
        @Override
        public void paintShape(final Graphics g, final int x, final int y, 
                final int size, final boolean selected, final boolean hilited) {
            Graphics2D g2 = (Graphics2D)g;
            Stroke backupStroke = g2.getStroke();
            if (selected || hilited) {
                g2.setStroke(new BasicStroke(2));
            }
            int x1 = x - (size / 2);
            int x2 = x + (size / 2);
            g.drawLine(x1, y, x2, y);
            g2.setStroke(backupStroke);

        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return HORIZONTAL_LINE;
        }

    }
    // Rectangle 
    /**
     * 
     * @author Fabian Dill, University of Konstanz
     */
    private static class Rectangle extends Shape { 

        /**
         * {@inheritDoc}
         */
        @Override
        public void paintShape(final Graphics g, final int x, final int y,
                final int size, final boolean selected, final boolean hilited) {
            // draw here
            int rectX = (int)(x - (size / 2.0));
            int rectY = (int)(y - (size / 2.0));
            g.fillRect(rectX, rectY, size, size);
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return RECTANGLE;
        }

    }
    // reverse triangle
    /**
     * 
     * @author Fabian Dill, University of Konstanz
     */
    private static class ReverseTriangle extends Shape {

        /**
         * {@inheritDoc}
         */
        @Override
        public void paintShape(final Graphics g, final int x, final int y, 
                final int size, final boolean selected, final boolean hilited) {
            // draw here
            int x1 = (int)(x - (size / 2.0));
            int x2 = x;
            int x3 = (int)(x + (size / 2.0));
            int y1 = (int)(y + (size / 2.0));
            int y2 = (int)(y - (size / 2.0));
            g.fillPolygon(new int[]{x1, x2, x3}, new int[]{y2, y1, y2}, 3);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return REVERSE_TRIANGLE;
        }

    }
    // triangle
    /**
     * 
     * @author Fabian Dill, University of Konstanz
     */
    private static class Triangle extends Shape {

        /**
         * {@inheritDoc}
         */
        @Override
        public void paintShape(final Graphics g, final int x, final int y, 
                final int size, final boolean selected, final boolean hilited) {
            // draw here
            int x1 = (int)(x - (size / 2.0));
            int x2 = x;
            int x3 = (int)(x + (size / 2.0));
            int y1 = (int)(y + (size / 2.0));
            int y2 = (int)(y - (size / 2.0));
            int y3 = y1;
            g.fillPolygon(new int[]{x1, x2, x3}, new int[]{y1, y2, y3}, 3);
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return TRIANGLE;
        }

    }
    
    // vertical stroke 
    /**
     * 
     * @author Fabian Dill, University of Konstanz
     */
    private static class VerticalStroke extends Shape {


        /**
         * {@inheritDoc}
         */
        @Override
        public void paintShape(final Graphics g, final int x, final int y, 
                final int size, final boolean selected, final boolean hilited) {
            Graphics2D g2 = (Graphics2D)g;
            Stroke backupStroke = g2.getStroke();
            if (selected || hilited) {
                g2.setStroke(new BasicStroke(2));
            }
            int y1 = y - (size / 2);
            int y2 = y + (size / 2);
            g.drawLine(x, y1, x, y2);
            g2.setStroke(backupStroke);        
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return VERTICAL_LINE;
        }

    }
    // X shape
    /**
     * 
     * @author Fabian Dill, University of Konstanz
     */
    private static class XShape extends Shape {

        /**
         * {@inheritDoc}
         */
        @Override
        public void paintShape(final Graphics g, final int x, final int y,
                final int size, final boolean selected, final boolean hilited) {
            Graphics2D g2 = (Graphics2D)g;
            Stroke backupStroke = g2.getStroke();
            if (selected || hilited) {
                g2.setStroke(new BasicStroke(2));
            }
            int x1 = x - (size / 2);
            int x2 = x + (size / 2);
            int y1 = y - (size / 2);
            int y2 = y + (size / 2);
            g.drawLine(x1, y1, x2, y2);
            g.drawLine(x1, y2, x2, y1);
            g2.setStroke(backupStroke);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return X_SHAPE;
        }

    } 
    
}
