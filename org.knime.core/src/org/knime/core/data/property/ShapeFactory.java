/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   13.09.2006 (Fabian Dill): created
 */
package org.knime.core.data.property;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
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
    public static final String X_SHAPE = "X Shape";
    /** Name of and key for the horizontal line. */
    public static final String HORIZONTAL_LINE = "Horizontal Line";
    /** Name of and key for the vertical line. */
    public static final String VERTICAL_LINE = "Vertical Line";
    /** Name of and key for the default shape. */
    public static final String DEFAULT = "Default";

    
    private static Map<String, Shape> shapes;
    
    static {
        ShapeFactory s = new ShapeFactory();
        shapes = new LinkedHashMap<String, Shape>();
        shapes.put(RECTANGLE, s.new Rectangle());
        shapes.put(DEFAULT, s.new Rectangle());
        shapes.put(CIRCLE, s.new Circle());
        shapes.put(TRIANGLE, s.new Triangle());
        shapes.put(REVERSE_TRIANGLE, s.new ReverseTriangle());
        shapes.put(DIAMOND, s.new Diamond());
        shapes.put(ASTERISK, s.new Asterisk());
        shapes.put(CROSS, s.new Cross());
        shapes.put(X_SHAPE, s.new XShape());
        shapes.put(HORIZONTAL_LINE, s.new HorizontalStroke());
        shapes.put(VERTICAL_LINE, s.new VerticalStroke());
    }
    
    
    /**
     * Register all shapes.
     *
     */
    private ShapeFactory() {         
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
            return shapes.get(ShapeFactory.DEFAULT);
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
    public abstract class Shape {
        
        private Shape() {
            
        }
        
        private int m_width = 11;
        
        private int m_height = 11;
        
        private ColorAttr m_color = ColorAttr.DEFAULT;
        
        private int m_x = 0;
        
        private int m_y = 0;
        
        private static final double BORDER_SIZE = 0.1;
        
        /**
         * 
         * @return the shape as an icon.
         */
        public Icon getIcon() {
            return new Icon() {
                public final int getIconHeight() {
                    return getHeight();
                }
                
                public final int getIconWidth() {
                    return getWidth();
                }
                
                public void paintIcon(final Component c, final Graphics g, 
                        final int x, final int y) {
                    m_x = x + (m_width / 2);
                    m_y = y + (m_height / 2);
                    g.setXORMode(Color.lightGray);
                    ((Graphics2D)g).setRenderingHint(
                            RenderingHints.KEY_ANTIALIASING, 
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    paintDot(g, false, false, false);
                    g.setPaintMode();
                }
            };
        };
        
        /**
         * 
         * @param position the position, normally the center of the shape.
         */
        public void setPosition(final Point position) {
            m_x = position.x;
            m_y = position.y;
        }
        
        /**
         * 
         * @return the position, normally the center of the shape.
         */
        public Point getPosition() {
            return new Point(m_x, m_y);
        }
        
        /**
         * 
         * @param x the x position, normally the center of the shape.
         */
        public void setXPosition(final int x) {
            m_x = x;
        }
        
        /**
         * 
         * @param y the y position, normally the center of the shape.
         */
        public void setYPosition(final int y) {
            m_y = y;
        }
        
        /**
         * 
         * @return the x position, normally the center of the shape.
         */
        public int getXPosition() {
            return m_x;
        }
        
        /**
         * 
         * @return the y position, normally the center of the shape.
         */
        public int getYPosition() {
            return m_y;
        }
        
        /**
         * 
         * @return the width of the shape.
         */
        public int getWidth() {
            return m_width;
        }
        
        /**
         * 
         * @return the height of the shape.
         */
        public int getHeight() {
            return m_height;
        }
        
        /**
         * 
         * @param width the width of the shape.
         */
        public void setWidth(final int width) {
            m_width = width;
        }
        
        /**
         * 
         * @param height the height of the shape.
         */
        public void setHeight(final int height) {
            m_height = height;
        }
        
        /**
         * 
         * @param dimension the dimension of the shape.
         */
        public void setDimension(final Dimension dimension) {
            m_width = (int)dimension.getWidth();
            m_height = (int)dimension.getHeight();
        }
        
        /**
         * 
         * @return the dimension of the shape.
         */
        public Dimension getDimension() {
            return new Dimension(m_width, m_height);
        }
        
        /**
         * 
         * @param color the color of the shape
         */
        public void setColor(final ColorAttr color) {
            m_color = color;
        }
        
        /**
         * 
         * @return the color of the shape
         */
        public ColorAttr getColor() {
            return m_color;
        }
        
        /**
         * Paints the hilite border.
         * @param g the graphics object.
         * @param selected flag whether the dot is selected
         */
        public void paintBorder(final Graphics g, final boolean selected) {
            int borderSize = (int)Math.ceil(BORDER_SIZE * getWidth());
            if (borderSize == 0) {
                borderSize = 1;
            }
            Color backupColor = g.getColor();
            int x = (int)(getXPosition() - (getWidth() / 2.0)) - borderSize;
            int y = (int)(getYPosition() - (getHeight() / 2.0)) - borderSize;
            Graphics2D g2 = (Graphics2D)g;
            Stroke backupStroke = g2.getStroke();
            g2.setColor(getColor().getBorderColor(selected, true));
            g2.setStroke(new BasicStroke(borderSize));
            g2.drawRect(x, y, getWidth() + (2 * borderSize), 
                    getHeight() + (2 * borderSize));
            g2.setColor(backupColor);
            g2.setStroke(backupStroke);
        }
        
        /**
         * Paints the dot and if hilited a border around the dot.
         * @param g the graphics object.
         * @param hilited flag whether dot is hilited
         * @param selected flag whether dot is selected.
         * @param faded flag whether point is faded.
         */
        public void paintDot(final Graphics g, 
                final boolean hilited, final boolean selected, 
                final boolean faded) {
            Color backupColor = g.getColor();
            if (faded) {
                if (!selected) {
                    g.setColor(ColorAttr.INACTIVE);
                } else {
                    g.setColor(ColorAttr.INACTIVE_SELECTED);                
                }
            } else {
                g.setColor(getColor().getColor(selected, hilited));
            }
            paintShape(g, selected, hilited);
//            if (hilited) {
//                paintBorder(g, selected);
//            }
            g.setColor(backupColor);
        }
        

        /**
         * Paints the shape.
         * @param g the graphics object
         * @param hilited flag whether the shape is hilited
         * @param selected flag whether the shape is selected
         */
        public abstract void paintShape(final Graphics g, 
                final boolean selected, final boolean hilited);
        
        /**
         * 
         * @return a new instance of the shape implementation.
         */
        public abstract Shape newInstance();
        
        /**
         * 
         * @see java.lang.Object#toString()
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
    private class Asterisk extends Shape {
        
        
        
        /**
         * 
         * @see org.knime.core.data.property.ShapeFactory.Shape#newInstance()
         */
        @Override
        public Shape newInstance() {
            return new Asterisk();
        }


        /**
         * 
         * @see org.knime.core.data.property.ShapeFactory.Shape#paintShape(
         * java.awt.Graphics, boolean, boolean)
         */
        @Override
        public void paintShape(final Graphics g, final boolean selected, 
                final boolean hilited) {
            int x1 = getXPosition() - (getWidth() / 2);
            int x2 = getXPosition();
            int x3 = getXPosition() + (getWidth() / 2);
            int y1 = getYPosition() - (getHeight() / 2);
            int y2 = getYPosition();
            int y3 = getYPosition() + (getHeight() / 2);
            g.drawLine(x1, y3, x3, y1);
            g.drawLine(x1, y2, x3, y2);
            g.drawLine(x1, y1, x3, y3);
            g.drawLine(x2, y3, x2, y1);
            
        }


        /**
         * 
         * @see org.knime.core.data.property.ShapeFactory.Shape#toString()
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
    private class Circle extends Shape {
        

        /**
         * 
         * @see org.knime.core.data.property.ShapeFactory.Shape#newInstance()
         */
        @Override
        public Shape newInstance() {
            return new Circle();
        }

        /**
         * 
         * @see org.knime.core.data.property.ShapeFactory.Shape#paintShape(
         * java.awt.Graphics, boolean, boolean)
         */
        @Override
        public void paintShape(final Graphics g, final boolean selected, 
                final boolean hilited) {
            int x = (int)(getXPosition() - (getWidth() / 2.0));
            int y = (int)(getYPosition() - (getHeight() / 2.0));
            g.fillOval(x, y, getWidth(), getHeight());
        }
        

        /**
         * 
         * @see org.knime.core.data.property.ShapeFactory.Shape#toString()
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
    private class Cross extends Shape {
        
        /**
         * 
         * @see org.knime.core.data.property.ShapeFactory.Shape#newInstance()
         */
        @Override
        public Shape newInstance() {
            return new Cross();
        }


        /**
         * 
         * @see org.knime.core.data.property.ShapeFactory.Shape#paintShape(
         * java.awt.Graphics, boolean, boolean)
         */
        @Override
        public void paintShape(final Graphics g, final boolean selected, 
                final boolean hilited) {
            Graphics2D g2 = (Graphics2D)g;
            Stroke backupStroke = g2.getStroke();
            if (selected || hilited) {
                g2.setStroke(new BasicStroke(2));
            }
            int x1 = (int)(getXPosition() - (getWidth() / 2.0));
            int x2 = getXPosition();
            int x3 = (int)(getXPosition() + (getWidth() / 2.0));
            int y1 = (int)(getYPosition() - (getHeight() / 2.0));
            int y2 = getYPosition();
            int y3 = (int)(getYPosition() + (getHeight() / 2.0));
            g2.drawLine(x2, y1, x2, y3);
            g2.drawLine(x1, y2, x3, y2);
            g2.setStroke(backupStroke);
        }
        

        /**
         * 
         * @see org.knime.core.data.property.ShapeFactory.Shape#toString()
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
    private class Diamond extends Shape {
        
        /**
         * 
         * @see org.knime.core.data.property.ShapeFactory.Shape#newInstance()
         */
        @Override
        public Shape newInstance() {
            return new Diamond();
        }
        
        /**
         * 
         * @see org.knime.core.data.property.ShapeFactory.Shape#paintShape(
         * java.awt.Graphics, boolean, boolean)
         */
        @Override
        public void paintShape(final Graphics g, final boolean selected, 
                final boolean hilited) {
            int x1 = getXPosition() - (getWidth() / 2);
            int x2 = getXPosition();
            int x3 = getXPosition() + (getWidth() / 2);
            int y1 = getYPosition() - (getHeight() / 2);
            int y2 = getYPosition();
            int y3 = getYPosition() + (getHeight() / 2);
            Polygon polygon = new Polygon();
            polygon.addPoint(x1, y2);
            polygon.addPoint(x2, y1);
            polygon.addPoint(x3, y2);
            polygon.addPoint(x2, y3);
            g.fillPolygon(polygon);
        }

        /**
         * 
         * @see org.knime.core.data.property.ShapeFactory.Shape#toString()
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
    private class HorizontalStroke extends Shape {
        
        /**
         * 
         * @see org.knime.core.data.property.ShapeFactory.Shape#newInstance()
         */
        @Override
        public Shape newInstance() {
            return new HorizontalStroke();
        }

        /**
         * 
         * @see org.knime.core.data.property.ShapeFactory.Shape#paintShape(
         * java.awt.Graphics, boolean, boolean)
         */
        @Override
        public void paintShape(final Graphics g, final boolean selected, 
                final boolean hilited) {
            Graphics2D g2 = (Graphics2D)g;
            Stroke backupStroke = g2.getStroke();
            if (selected || hilited) {
                g2.setStroke(new BasicStroke(2));
            }
            int x1 = getXPosition() - (getWidth() / 2);
            int x2 = getXPosition() + (getWidth() / 2);
            int y = getYPosition();
            g.drawLine(x1, y, x2, y);
            g2.setStroke(backupStroke);

        }

        /**
         * 
         * @see org.knime.core.data.property.ShapeFactory.Shape#toString()
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
    private class Rectangle extends Shape { 
        
        /**
         * 
         * @see org.knime.core.data.property.ShapeFactory.Shape#newInstance()
         */
        @Override
        public Shape newInstance() {
            return new Rectangle();
        }

        /**
         * 
         * @see org.knime.core.data.property.ShapeFactory.Shape#paintShape(
         * java.awt.Graphics, boolean, boolean)
         */
        @Override
        public void paintShape(final Graphics g, final boolean selected,
                final boolean hilited) {
            // draw here
            int x = (int)(getXPosition() - (getWidth() / 2.0));
            int y = (int)(getYPosition() - (getHeight() / 2.0));
            g.fillRect(x, y, getWidth(), getHeight());
        }


        /**
         * 
         * @see org.knime.core.data.property.ShapeFactory.Shape#toString()
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
    private class ReverseTriangle extends Shape {
        
        /**
         * 
         * @see org.knime.core.data.property.ShapeFactory.Shape#newInstance()
         */
        @Override
        public Shape newInstance() {
            return new ReverseTriangle();
        }

        /**
         * 
         * @see org.knime.core.data.property.ShapeFactory.Shape#paintShape(
         * java.awt.Graphics, boolean, boolean)
         */
        @Override
        public void paintShape(final Graphics g, final boolean selected, 
                final boolean hilited) {
            // draw here
            int x1 = (int)(getXPosition() - (getWidth() / 2.0));
            int x2 = getXPosition();
            int x3 = (int)(getXPosition() + (getWidth() / 2.0));
            int y1 = (int)(getYPosition() + (getHeight() / 2.0));
            int y2 = (int)(getYPosition() - (getHeight() / 2.0));
            g.fillPolygon(new int[]{x1, x2, x3}, new int[]{y2, y1, y2}, 3);
        }

        /**
         * 
         * @see org.knime.core.data.property.ShapeFactory.Shape#toString()
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
    private class Triangle extends Shape {
        
        /**
         * 
         * @see org.knime.core.data.property.ShapeFactory.Shape#newInstance()
         */
        @Override
        public Shape newInstance() {
            return new Triangle();
        }

        /**
         * 
         * @see org.knime.core.data.property.ShapeFactory.Shape#paintShape(
         * java.awt.Graphics, boolean, boolean)
         */
        @Override
        public void paintShape(final Graphics g, 
                final boolean selected, final boolean hilited) {
            // draw here
            int x1 = (int)(getXPosition() - (getWidth() / 2.0));
            int x2 = getXPosition();
            int x3 = (int)(getXPosition() + (getWidth() / 2.0));
            int y1 = (int)(getYPosition() + (getHeight() / 2.0));
            int y2 = (int)(getYPosition() - (getHeight() / 2.0));
            int y3 = y1;
            g.fillPolygon(new int[]{x1, x2, x3}, new int[]{y1, y2, y3}, 3);
        }
        
        /**
         * 
         * @see org.knime.core.data.property.ShapeFactory.Shape#toString()
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
    private class VerticalStroke extends Shape {
        
        /**
         * 
         * @see org.knime.core.data.property.ShapeFactory.Shape#newInstance()
         */
        @Override
        public Shape newInstance() {
            return new VerticalStroke();
        }

        /**
         * 
         * @see org.knime.core.data.property.ShapeFactory.Shape#paintShape(
         * java.awt.Graphics, boolean, boolean)
         */
        @Override
        public void paintShape(final Graphics g, final boolean selected, 
                final boolean hilited) {
            Graphics2D g2 = (Graphics2D)g;
            Stroke backupStroke = g2.getStroke();
            if (selected || hilited) {
                g2.setStroke(new BasicStroke(2));
            }
            int x = getXPosition();
            int y1 = getYPosition() - (getHeight() / 2);
            int y2 = getYPosition() + (getHeight() / 2);
            g.drawLine(x, y1, x, y2);
            g2.setStroke(backupStroke);        
        }

        /**
         * 
         * @see org.knime.core.data.property.ShapeFactory.Shape#toString()
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
    private class XShape extends Shape {
        
        /**
         * 
         * @see org.knime.core.data.property.ShapeFactory.Shape#newInstance()
         */
        @Override
        public Shape newInstance() {
            return new XShape();
        }

        /**
         * 
         * @see org.knime.core.data.property.ShapeFactory.Shape#paintShape(
         * java.awt.Graphics, boolean, boolean)
         */
        @Override
        public void paintShape(final Graphics g, final boolean selected, 
                final boolean hilited) {
            Graphics2D g2 = (Graphics2D)g;
            Stroke backupStroke = g2.getStroke();
            if (selected || hilited) {
                g2.setStroke(new BasicStroke(2));
            }
            int x1 = getXPosition() - (getWidth() / 2);
            int x2 = getXPosition() + (getWidth() / 2);
            int y1 = getYPosition() - (getHeight() / 2);
            int y2 = getYPosition() + (getHeight() / 2);
            g.drawLine(x1, y1, x2, y2);
            g.drawLine(x1, y2, x2, y1);
            g2.setStroke(backupStroke);
        }

        /**
         * 
         * @see org.knime.core.data.property.ShapeFactory.Shape#toString()
         */
        @Override
        public String toString() {
            return X_SHAPE;
        }

    } 
    
}
