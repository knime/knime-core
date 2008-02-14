/*
 * --------------------------------------------------------------------- *
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
 * --------------------------------------------------------------------- *
 * History
 *   14.09.2004 (ohl): created
 */
package org.knime.base.node.viz.plotter;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JComponent;

import org.knime.base.util.coordinate.Coordinate;
import org.knime.base.util.coordinate.CoordinateMapping;
import org.knime.base.util.coordinate.NominalCoordinate;

/**
 * Implements a header for a scrollable area. Can be used as horizontal (row) or
 * vertical (columns) header. It tries to find "useful" increments between the
 * ticks. There are various parameters you can play with to get a nice display -
 * but I admit it's hard and only sometimes successful...
 * 
 * @author ohl, University of Konstanz
 * @author Christoph Sieb, University of Konstanz
 * @author Fabian Dill, University of Konstanz
 */
public class Axis extends JComponent {
    
//    private static final NodeLogger LOGGER = NodeLogger.getLogger(Axis.class);

    /**
     * Constant for the horizontal enumeration value.
     */
    public static final int HORIZONTAL = 0;

    /**
     * Constant for the vertical enumeration value.
     */
    public static final int VERTICAL = 1;

    /**
     * the "thickness" of the header.
     */
    public static final int SIZE = 50;

    /** the length of the tick line. */
    private static final int TICKLENGTH = 7;
    


    /** the size of the font we draw with. */
    private static final int FONTSIZE = 10;

    /** the font used to label the ruler. */
    private static final Font RULER_FONT = new Font("SansSerif", Font.PLAIN,
            FONTSIZE);

    /** the offset between adjacent labels in horizontal headers. */
    private static final int HORIZ_OFFSET = FONTSIZE;


    /** holding the orientation of this instance. */
    private final boolean m_horizontal;

    /** the full length of the ruler (in pixels). */
    private int m_fullLength;

    /**
     * The underlying coordinate for this axis header.
     */
    private Coordinate m_coordinate;

    /**
     * This is the offset to start the ticks from. This is neccessary, i.e. for
     * the scatter plotter which would set half the current dotsize, which must
     * be painted below its actual value.
     */
    private int m_startTickOffset;
    
    private boolean m_rotateXLabels;
    
    private boolean m_rotateYLabels;
    
//    private int m_nrOfTicks;
    private int m_tickDist;
    
    private List<Integer>m_tickPositions;
    
    private CoordinateMapping[] m_coordMap;
   

    /**
     * Creates a new ruler in either horizontal or vertical orientation.
     * 
     * @param orientation specifies the orientation of this instance. Use
     *            Header.HORIZONTAL, or Header.VERTICAL
     * @param length the initial entire length of the ruler in pixels.
     */
    public Axis(final int orientation, final int length) {
        if ((orientation != HORIZONTAL) && (orientation != VERTICAL)) {
            throw new IllegalArgumentException("Argument 'orientation' must"
                    + " be either Header.HORIZONTAL or Header.VERTICAL.");
        }
        m_fullLength = length;
        m_tickPositions = new LinkedList<Integer>();
        m_horizontal = (orientation == HORIZONTAL);
        setToolTipText("complete label");
    }
    

    /*
     * 
     * @see javax.swing.JComponent#getToolTipText(java.awt.event.MouseEvent)
     */
    @Override
    public String getToolTipText(final MouseEvent event) {
        if (m_coordMap == null || m_coordMap.length == 0 
        		|| m_tickDist == 0 || !m_coordinate.isNominal()) {
            return "";
        }
        int pivot;
        if (m_horizontal) {
        	pivot = event.getX() - m_startTickOffset;
            for (int i = 0; i < m_coordMap.length; i++) {
            	CoordinateMapping mapping = m_coordMap[i];
            	if (pivot > mapping.getMappingValue() - m_tickDist / 3
            			&& pivot < mapping.getMappingValue() + m_tickDist / 3) {
            		return mapping.getDomainValueAsString();
            	}
            }        	
        } else {
        	// map the y value to screen coordinates
        	pivot = getHeight() - event.getY();
            for (CoordinateMapping mapping : m_coordMap) {
            	if (pivot > mapping.getMappingValue() - m_tickDist / 3
            			&& pivot < mapping.getMappingValue() + m_tickDist / 3) {
            		return mapping.getDomainValueAsString();
            	}
            }
        }

        return "";
    }
    

    /**
     * {@inheritDoc}
     */
    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);

        if (g.getClipBounds() == null) {
            return;
        }
        
        m_tickPositions.clear();

        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        int x = 0;
        int y = 0;
        int width;
        int height;
        if (m_horizontal) {
            width = m_fullLength;
            height = SIZE;
        } else {
            width = SIZE;
            height = m_fullLength;
        }
        // Do the ruler labels in a small font that's black.
        g.setFont(RULER_FONT);
        g.setColor(Color.black);

        // the length we draw the ruler in
        int length;
        if (m_horizontal) {
            length = width;
        } else {
            length = height;
        }
        if (length <= 5) {
            // no use in drawing in such a small area
            return;
        }

        // draw the axis
        if (m_horizontal) {
            g.drawLine(x, y + 2, x + width, y + 2);
        } else {
            g.drawLine(x + width - 1, y, x + width - 1, y + height);
        }

        // draw the ticks and the labels
        // the offset must be subtracted 2 times
        if (m_coordinate == null) {
            return;
        }
        m_coordMap = m_coordinate.getTickPositions(
                length - 2 * m_startTickOffset, true);
        if (m_coordMap.length == 0) {
            return;
        }
        
        m_tickDist = length / m_coordMap.length;
        
        if (m_horizontal) {
        // check the distance between the ticks
            m_rotateXLabels = LabelPaintUtil.rotateLabels(m_coordMap, m_tickDist, 
                g.getFontMetrics());
        } else {
            m_rotateYLabels = LabelPaintUtil.rotateLabels(m_coordMap, 
                    SIZE - TICKLENGTH, g.getFontMetrics());
        }
        if (m_coordinate.isNominal()) {
            // store the complete labels to avoid "..." in the tooltip
            m_coordMap = ((NominalCoordinate)m_coordinate)
                .getReducedTickPositions(length - 2 * m_startTickOffset);
            
        }
        
        
        // this offset is for the labeldrawing in the horizontal header
        // it puts the lables alternatively up and down to have more
        // space for the labeling
        boolean useOffset = false;
        
        // draw all ticks except the last
        for (int i = 0; i < m_coordMap.length; i++) {
            CoordinateMapping mapping = m_coordMap[i];
            String label = mapping.getDomainValueAsString();
            drawTick(g, (long)mapping.getMappingValue() + m_startTickOffset,
                    label, useOffset);
            useOffset = !useOffset;
        }
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
    }
    
    /*
     * draws a tick and writes a label at the corresponding place. Takes into
     * account the orientation of the header.
     */
    private void drawTick(final Graphics g, final long at, final String label,
            final boolean useOffset) {
        if (m_horizontal) {
            drawHorizontalTick(g, at, label, useOffset);
        } else {
            drawVerticalTick(g, at, label);
        }
    }
    
    
    private void drawHorizontalTick(final Graphics g, final long at, 
            final String label, final boolean useOffset) {
        if (at <= m_fullLength) {
            int x = (int)at;
            // if this would mean that the lable is not displayed
            // at the left border set x to 0
            if (x < 0) {
                x = 0;
            }
            g.drawLine(x, 2, x, TICKLENGTH + 2);

            if (!m_coordinate.isNominal()) {
                // for the label we adjust the coordinates
                int lablePixelLength = g.getFontMetrics().stringWidth(
                        label);

                // place the lable in the midle of a tick
                if (x > lablePixelLength) {
                    x -= lablePixelLength / 2;
                }
                if (x + lablePixelLength > m_fullLength) {
                    // if the lable would be printed beyond the right border
                    x = m_fullLength - lablePixelLength - 1;
                }
            }
            
            // store the tick position for later tooltip retrieval
            m_tickPositions.add(x);
            int labelY = SIZE - TICKLENGTH - 3;
            if (useOffset) {
                labelY -= HORIZ_OFFSET;
            }
            if (labelY < FONTSIZE) {
                labelY = FONTSIZE;
            }
            if (m_coordinate.isNominal()) {
            Rectangle rect = new Rectangle(x, 
                    TICKLENGTH + g.getFontMetrics().getHeight(), 
                    m_tickDist, SIZE - TICKLENGTH);
            LabelPaintUtil.drawLabel(label, (Graphics2D)g, rect, 
                    LabelPaintUtil.Position.BOTTOM, m_rotateXLabels);
            } else {
                g.drawString(label, x, labelY);
            }
            
        }
    }
    
    private void drawVerticalTick(final Graphics g, final long at, 
            final String label) {
        if (at <= m_fullLength) {
            // int y = (int) at;
            // have the origin sitting in the lower left

            int y = m_fullLength - (int)at;
            if (y == m_fullLength) {
                y -= 1;
            }

            g.drawLine(SIZE - TICKLENGTH - 1, y, SIZE - 1, y);
            
            m_tickPositions.add(y);
            
            if (!m_coordinate.isNominal()) {
                // for the label we adjust the coordinates
                int lablePixelHeight = g.getFontMetrics().getHeight();

                // if this would mean that the lable is not displayed
                // at the left border set x to 0
                if (y < lablePixelHeight) {
                    y += g.getFontMetrics().getAscent();
                } else if (y + lablePixelHeight > m_fullLength) {
                    // if the lable woulb be printed beyond the border
                    y -= g.getFontMetrics().getDescent();
                }
                g.drawString(label, 
                        SIZE - (2 * TICKLENGTH) 
                        - g.getFontMetrics().stringWidth(label), 
                        y); // + g.getFontMetrics().getHeight() / 3);
            } else {
                Rectangle rect = new Rectangle(TICKLENGTH, y - m_tickDist, 
                    SIZE - (2 * TICKLENGTH), m_tickDist);
                LabelPaintUtil.drawLabel(label, (Graphics2D)g, rect, 
                    LabelPaintUtil.Position.LEFT, m_rotateYLabels);
            }
        }        
    }

    /**
     * Sets the prefered size of the component. Depending on the orientation,
     * the parameter <code>l</code> will either specifiy the width or height.
     * The values set (start and end) will be evenly spreaded over the entire
     * length.
     * 
     * @param l the length of the ruler
     */
    public void setPreferredLength(final int l) {
        m_fullLength = l;
        if (m_horizontal) {
            setPreferredSize(new Dimension(l, SIZE));
        } else {
            setPreferredSize(new Dimension(SIZE, l));
        }
    }

    /**
     * Sets the underlying coordinate for this header.
     * 
     * @param coordinate the coordinate to set
     */
    public void setCoordinate(final Coordinate coordinate) {
        m_coordinate = coordinate;
        repaint();
    }

    /**
     * Set the offset to start the tick-painting from. This is neccessary, i.e.
     * for the scatter plotter which would set half the current dotsize, which
     * must be painted below its actual value.
     * 
     * @param dotSizeOffset the offset
     */
    public void setStartTickOffset(final int dotSizeOffset) {
        m_startTickOffset = dotSizeOffset;
    }
    
    /**
     * 
     * @return the tick offset
     */
    public int getTickOffset() {
        return m_startTickOffset;
    }

    /**
     * @return the underlying coordinate
     */
    public Coordinate getCoordinate() {
        return m_coordinate;
    }
}
