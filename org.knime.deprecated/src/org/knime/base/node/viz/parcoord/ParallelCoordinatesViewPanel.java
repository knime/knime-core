/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 * 
 */
package org.knime.base.node.viz.parcoord;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Set;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.event.MouseInputListener;

import org.knime.base.node.viz.parcoord.visibility.VisibilityEvent;
import org.knime.base.node.viz.parcoord.visibility.VisibilityHandler;
import org.knime.base.node.viz.parcoord.visibility.VisibilityListener;
import org.knime.core.data.RowKey;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteListener;
import org.knime.core.node.property.hilite.KeyEvent;

/**
 * The view's panel in which the Parallel Coordinates are drawn.
 * 
 * @author Simona Pintilie, University of Konstanz
 */
public final class ParallelCoordinatesViewPanel extends JPanel implements
        HiLiteListener, MouseInputListener, VisibilityListener {

    // **************** data variables received from model ********************

    /**
     * Comment for <code>m_columnCount</code> the number of columns for
     * m_input.
     */
    private int m_columnCount;

    /**
     * Comment for <code>m_rowCount</code> the number of rows for m_input.
     */
    private int m_rowCount;

    /**
     * <code>m_stringValuesNo</code> the number of distinct nominal values for
     * a certain column.
     */
    private int[] m_stringValuesNo;

    /**
     * Comment for <code>m_color</code> the colors for each row.
     */
    private RowKey[] m_keys;

    /**
     * Comment for <code>m_color</code> the colors for each row.
     */
    private ColorAttr[] m_colors;

    /**
     * also store sizes of all points and translate them to width of line.
     */
    private double[] m_sizes;

    /**
     * <code>m_stringValues</code> the nominal values for each column.
     */
    private String[][] m_stringValues;

    // **************** hiliting descriptive variables ****************
    private boolean[] m_hilited;

    private boolean[] m_selected;

    private boolean[] m_visible;

    /**
     * Comment for <code>m_hdl</code> the hilite handler.
     */
    private HiLiteHandler m_hdl;

    private VisibilityHandler m_vh;

    // **************** painting descriptive variables *******************
    /**
     * Comment for <code>distanceBetweenCoordinates</code> the number of
     * pixels that separate two adiacent coordinates.
     */
    private int m_distanceBetweenCoordinates;

    /**
     * Comment for <code>m_canvasWidth</code>.
     */
    private int m_canvasWidth;

    /**
     * Comment for <code>m_canvasHeight</code>.
     */
    private int m_canvasHeight;

    /**
     * Comment for <code>m_headerHeight</code> the space in pixels assigned
     * for what is to be drawn up from the axis.
     */
    private int m_headerHeight;

    /**
     * Comment for <code>m_footerHeight</code> the space in pixels assigned
     * for what is to be drawn down from the axis.
     */
    private int m_footerHeight;

    /**
     * Comment for <code>m_axisLength</code> the coordinate length in pixels.
     */
    private int m_axisLength;

    /**
     * Comment for <code>m_heightUnit</code> the maxAdvance for the used font.
     */
    private double m_heightUnit;

    /**
     * Comment for <code>m_minimumDBetweenC</code> the minimum distance
     * between coordinates accepted so that the window is painted.
     */
    private int m_minimumDBetweenC;

    /**
     * Comment for <code>m_minimumHeigth</code> the minimum height of the
     * canvas accepted so that the window is painted.
     */
    private int m_minimumHeigth;

    /**
     * Comment for <code>m_coordinateOrder</code> the order in which
     * coordinates are painted.
     */
    private int[] m_coordinateOrder;

    // ********** mouse events descriptive variables & methods *********
    private int m_dragStartX;

    private int m_dragStartY;

    private int m_dragStopX;

    private int m_dragStopY;

    private int m_lastX;

    private boolean m_mouseIsDragged;

    private boolean m_mouseJustPressed;

    private boolean m_grabCoord;

    /** The panel's (view's) underlying content model. */
    private ParallelCoordinatesViewContent m_content;

    private int m_viewType;

    /**
     * Set a new view type.
     * 
     * @param viewType The new view type which can be out of <b>POPUP...</b>.
     */
    public void setViewType(final int viewType) {
        m_viewType = viewType;
    }

    /**
     * Creates a new view panel.
     */
    public ParallelCoordinatesViewPanel() {
        m_content = null;
        // setting initial heights
        m_canvasHeight = 500;
        m_minimumHeigth = 100;
        m_minimumDBetweenC = 5;
        m_canvasWidth = 600;
        // setting the m_panel
        setPreferredSize(new Dimension(m_canvasWidth, m_canvasHeight));
        setMinimumSize(new Dimension((m_columnCount + 1) * m_minimumDBetweenC,
                m_minimumHeigth));

        setLayout(null);
        // register for mouse events
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    /**
     * sets the view panel based on the content model and a HiLiteHandler in
     * order to send and receive hilite events.
     * 
     * @param content The view's content model.
     * @param hdl The hilite handler.
     * @param vh the visibility handler
     */
    public void setNewModel(final ParallelCoordinatesViewContent content,
            final HiLiteHandler hdl, final VisibilityHandler vh) {
        m_content = content;
        if (m_content != null) {
            m_columnCount = m_content.getColumnCount();
            m_rowCount = m_content.getRowCount();
            m_keys = m_content.getKeyVector();
            m_colors = m_content.getColorVector();
            m_sizes = m_content.getSizes();
            m_stringValues = m_content.getStringValues();
            m_stringValuesNo = m_content.getStringValuesNo();
            // TODO read from dialog
            m_distanceBetweenCoordinates = 100;
            // setting canvas width
            m_canvasWidth = (m_columnCount + 1) * m_distanceBetweenCoordinates;
            // setting the order of the coordinates
            m_coordinateOrder = new int[m_columnCount];
            for (int i = 0; i < m_columnCount; i++) {
                m_coordinateOrder[i] = i;

            }

            m_selected = new boolean[m_rowCount];
            m_visible = new boolean[m_rowCount];
            for (int row = 0; row < m_rowCount; row++) {
                m_selected[row] = false;
                m_visible[row] = true;

            }

            setNewHiliteHandler(hdl);
            setNewVisibilityHandler(vh);
        } else {
            // if we had hilite handler before, unregister us from the old one
            if (m_hdl != null) {
                m_hdl.removeHiLiteListener(this);
            }
            if (m_vh != null) {
                m_vh.removeVisibilityListener(this);
            }
            m_hdl = null;
            m_vh = null;
            m_columnCount = 0;
            m_rowCount = 0;
            m_keys = null;
            m_colors = null;
            m_sizes = null;
            m_stringValues = null;
            m_stringValuesNo = null;
        }
        this.setBackground(Color.white);
        this.setToolTipText("");
        repaint();
    }

    /**
     * @param hdl the new hilite handler
     */
    public void setNewHiliteHandler(final HiLiteHandler hdl) {
        // if we had hilite handler before, unregister us from the old one
        if (m_hdl != null) {
            m_hdl.removeHiLiteListener(this);
        }
        // init hilite handler
        m_hdl = hdl;
        if (m_hdl != null) {
            m_hdl.addHiLiteListener(this);
        }
        m_hilited = new boolean[m_rowCount];
        for (int row = 0; row < m_rowCount; row++) {
            m_hilited[row] = m_hdl.isHiLit(m_keys[row]);
        }
    }

    /**
     * @param vh the new visibility handler
     */
    public void setNewVisibilityHandler(final VisibilityHandler vh) {
        // if we had hilite handler before, unregister us from the old one
        if (m_vh != null) {
            m_vh.removeVisibilityListener(this);
        }
        // init hilite handler
        m_vh = vh;
        if (m_vh != null) {
            m_vh.addVisibilityListener(this);
            for (int row = 0; row < m_rowCount; row++) {
                m_visible[row] = m_vh.isVisible(m_keys[row]);
                m_selected[row] = m_vh.isSelected(m_keys[row]);
            }
        }

    }

    /**
     * @param x the x value of the start point of dragging
     * @param y the y value of the start point of dragging
     */
    public void setDragStartCoord(final int x, final int y) {
        m_dragStartX = x;
        m_dragStartY = y;
    }

    /**
     * @param x the x value that the moved axis was last
     */
    public void setLastX(final int x) {
        m_lastX = x;
    }

    /**
     * @param x the x value of the stop point of dragging
     * @param y the y value of the stop point of dragging
     */
    public void setDragStopCoord(final int x, final int y) {
        m_dragStopX = x;
        m_dragStopY = y;
    }

    /**
     * @param value setting whether the mouse is dragged or not
     */
    public void setMouseIsDragged(final boolean value) {
        m_mouseIsDragged = value;
    }

    /**
     * @return the last x value the moved axis was at
     */
    public int getLastX() {
        return m_lastX;
    }

    /**
     * @param value setting whether a coordinate has been grabbed
     */
    public void setGrabCoord(final boolean value) {
        m_grabCoord = value;
    }

    /**
     * @return if mouse is currently dragged
     */
    public boolean getMouseIsDragged() {
        return m_mouseIsDragged;
    }

    /**
     * @param value setting if mouse has just been pressed
     */
    public void setMouseJustPressed(final boolean value) {
        m_mouseJustPressed = value;
    }

    /**
     * @return if mouse has just been pressed
     */
    public boolean getMouseJustPressed() {
        return m_mouseJustPressed;
    }

    /**
     * @return if mouse has just been pressed on a coordinate
     */
    public boolean getGrabCoord() {
        return m_grabCoord;
    }

    /**        
     */
    public void drawScrollRectangle() {
        Graphics2D g2d = (Graphics2D)this.getGraphics();
        // saving properties of g2d
        Color oldColor = g2d.getColor();
        Stroke oldStroke = g2d.getStroke();

        // seting dawing color
        g2d.setXORMode(Color.gray);
        // A dashed stroke
        float miterLimit = 3f;
        float[] dashPattern = {3f};
        float dashPhase = 2f;
        float strokeThickness = 1.0f;
        Stroke stroke = new BasicStroke(strokeThickness, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER, miterLimit, dashPattern, dashPhase);
        g2d.setStroke(stroke);
        // drawing
        int x = Math.min(m_dragStartX, m_dragStopX);
        int y = Math.min(m_dragStartY, m_dragStopY);
        int width = Math.abs(m_dragStopX - m_dragStartX);
        int height = Math.abs(m_dragStopY - m_dragStartY);
        g2d.drawRect(x, y, width, height);
        // reseting properties of g2d
        g2d.setXORMode(oldColor);
        g2d.setStroke(oldStroke);
    }

    /**
     * @param x the x i'm painting the moving axis at
     */
    public void drawScrollCoordinate(final int x) {
        Graphics2D g2d = (Graphics2D)this.getGraphics();
        // saving properties of g2d
        Color oldColor = g2d.getColor();
        // seting dawing color
        g2d.setXORMode(Color.gray);
        // drawing the coordinate
        // int x = Math.min(m_dragStartX, m_dragStopX);
        g2d.drawLine(x, m_headerHeight, x, m_canvasHeight - m_footerHeight);

        // reseting properties of g2d
        g2d.setXORMode(oldColor);

    }

    // ******************* painting methods **********************
    /**
     * Comment for <code>isDrawable</code> if the canvas is drawable or not,
     * if it makes sense to try to paint it .
     * 
     * @param meineHoehe the canvas height
     * @param meineGuete the canvas width
     * @return if the m_panel is large enough to display the content
     */
    private boolean isDrawable(final int meineHoehe, final int meineGuete) {
        return !((meineHoehe < m_minimumHeigth) || (meineGuete <= (m_columnCount + 1)
                * m_minimumDBetweenC));

    }

    /**
     * Comment for <code>drawPointAsRectangle</code> drawing a point on an
     * axis.
     * 
     * @param x the location of the point on x coordinate
     * @param y the location of the point on y coordinate
     */
    private void drawPointAsRectangle(final int x, final int y,
            final Graphics2D g2d) {
        g2d.drawRect(x - 1, y - 1, 2, 2);
    }

    /**
     * Comment for <code>drawStringSmartWay</code> drawing a string in certain
     * width and height bounds, with a certain angle.
     * 
     * @param startX the x location of where to start drawing
     * @param startY the y location of where to start drawing
     * @param allocatedWidth the maximum width that the text has to occupy if
     *            not important, the value can be -1
     * @param allocatedHeight the maximum height that the text has to occupy if
     *            not important, the value can be -1
     * @param the angle the text has to be drawn in
     */
    private void drawStringSmartWay(final String s, final int startX,
            final int startY, final int allocatedWidth,
            final int allocatedHeight, final double angle, final Graphics2D g2d) {
        Rectangle2D r = g2d.getFontMetrics().getStringBounds(s, g2d);
        Rectangle2D rr = g2d.getFontMetrics().getStringBounds("..", g2d);
        double pointsWidth = rr.getWidth();
        String ss;
        double allowedWidth = Double.MAX_VALUE;

        if ((Math.sin(angle) != 0) && (allocatedHeight >= 0)) {
            allowedWidth = Math.abs((allocatedHeight - (g2d.getFontMetrics()
                    .getMaxAscent() * Math.cos(angle)))
                    / Math.sin(angle));
        }

        if ((Math.cos(angle) != 0) && (allocatedWidth >= 0)) {
            allowedWidth = Math.min(Math.abs(allocatedWidth / Math.cos(angle)),
                    allowedWidth);
        }

        if ((allowedWidth < r.getWidth()) && (allowedWidth >= 0)) {

            int noCharacters = ((int)allowedWidth / g2d.getFontMetrics()
                    .getMaxAdvance());

            ss = (String)s.subSequence(0, noCharacters);

            r = g2d.getFontMetrics().getStringBounds(ss, g2d);
            while (r.getWidth() < (int)(allowedWidth - pointsWidth)) {
                noCharacters++;
                ss = (String)s.subSequence(0, noCharacters);
                r = g2d.getFontMetrics().getStringBounds(ss, g2d);
            }
            if (noCharacters != 0) {
                ss = (String)s.subSequence(0, noCharacters - 1);
                ss = ss + "..";
            } else {
                ss = "..";
            }
        } else {
            ss = s;
        }

        // Get the current transform
        AffineTransform saveAT = g2d.getTransform();
        // Perform transformation
        g2d.rotate(angle, startX, startY);
        // Render
        g2d.drawString(ss, startX, startY);
        // Restore original transform
        g2d.setTransform(saveAT);
    }

    private void writeCoordNames(final double angle, final Graphics2D g2d) {
        String[] names = m_content.getNamesVector();
        for (int i = 0; i < m_columnCount; i++) {
            drawStringSmartWay(names[m_coordinateOrder[i]],
                    ((i + 1) * m_distanceBetweenCoordinates), m_headerHeight
                            - (int)m_heightUnit * 3, -1, m_headerHeight
                            - (int)m_heightUnit * 3 + 1, angle, g2d);
        }
    }

    private void drawAxes(final Graphics2D g2d) {
        for (int i = 1; i <= m_columnCount; i++) {
            g2d.drawLine(i * m_distanceBetweenCoordinates, m_headerHeight, i
                    * m_distanceBetweenCoordinates, m_canvasHeight
                    - m_footerHeight);
        }
    }

    private double setCanvasStructure(final int meineHoehe,
            final int meineGuete, final Graphics2D g2d) {
        m_canvasWidth = meineGuete;
        m_distanceBetweenCoordinates = m_canvasWidth / (m_columnCount + 1);
        if (m_distanceBetweenCoordinates > 14) {
            g2d.setFont(new Font("verdana", Font.PLAIN, 12));
        } else {
            g2d.setFont(new Font("verdana", Font.PLAIN,
                    m_distanceBetweenCoordinates - 2));
        }
        m_heightUnit = g2d.getFontMetrics().getMaxAscent();

        double angle = -Math.asin((double)(2 + g2d.getFontMetrics()
                .getMaxAscent())
                / m_distanceBetweenCoordinates);
        double maximumStringLength = 0;
        Rectangle2D r;
        String[] names = m_content.getNamesVector();
        for (int i = 0; i < m_columnCount; i++) {
            r = g2d.getFontMetrics().getStringBounds(
                    names[m_coordinateOrder[i]], g2d);
            maximumStringLength = Math.max(maximumStringLength, r.getWidth());
        }
        if (maximumStringLength < m_distanceBetweenCoordinates) {
            angle = 0;
        }

        m_canvasHeight = meineHoehe;
        m_headerHeight = (int)(Math.abs(Math.sin(angle) * maximumStringLength)
                + (int)m_heightUnit * 3 + Math.abs(Math.cos(angle)
                * g2d.getFontMetrics().getMaxAscent()));
        int allowedHeightOfHeader = m_canvasHeight * 3 / 10;
        if (m_headerHeight > allowedHeightOfHeader) {
            m_headerHeight = allowedHeightOfHeader;
        }
        m_footerHeight = (int)(m_heightUnit * 3);
        m_axisLength = m_canvasHeight - (m_headerHeight + m_footerHeight);
        return angle;

    }

    private void drawCoordBounds(final Graphics2D g2d) {
        String[] types = m_content.getTypesVector();
        double[] minVector = m_content.getMinVector();
        double[] maxVector = m_content.getMaxVector();
        for (int col = 0; col < m_columnCount; col++) {
            if (types[m_coordinateOrder[col]].compareTo("double") == 0
                    || types[m_coordinateOrder[col]].compareTo("fuzzyinterval") == 0) {
                // if attribute is numerical, simply display min/max values
                String s1 = "" + maxVector[m_coordinateOrder[col]];
                String s2 = "" + minVector[m_coordinateOrder[col]];
                drawStringSmartWay(s1,
                        ((col + 1) * m_distanceBetweenCoordinates),
                        m_headerHeight - (int)m_heightUnit,
                        m_distanceBetweenCoordinates - 1, -1, 0, g2d);
                drawStringSmartWay(
                        s2,
                        ((col + 1) * m_distanceBetweenCoordinates),
                        m_canvasHeight + 2 * (int)m_heightUnit - m_footerHeight,
                        m_distanceBetweenCoordinates - 1, -1, 0, g2d);
            } else {
                // probably a nominal attribute, display all possible values
                int allocatedWidth = 0;
                if (col == m_columnCount - 1) {
                    // last column in row
                    allocatedWidth = m_canvasWidth
                            - (m_columnCount * m_distanceBetweenCoordinates);
                } else {
                    // a column somewhere in the middle
                    allocatedWidth = m_distanceBetweenCoordinates;
                }
                for (int ref = 0; ref < m_stringValuesNo[m_coordinateOrder[col]]; ref++) {
                    // iterate over all possible (nominal only?) values
                    // compute double value, that is y-coordinate
                    double dvalue = (double)ref
                            / (m_stringValuesNo[m_coordinateOrder[col]] - 1);
                    // convert it to an integer coordinate
                    int ivalue = getPH(dvalue);
                    // and draw string at this position
                    drawStringSmartWay(
                            m_stringValues[m_coordinateOrder[col]][ref],
                            (col + 1) * m_distanceBetweenCoordinates + 6, // x-c
                            ivalue + 5, // y-coord
                            allocatedWidth, // max width for label
                            m_headerHeight - (int)m_heightUnit, // max height
                            0, // angle
                            g2d);
                }
            }
        }
    }

    /**
     * Comment for <code>drawingPolygon</code> drawing polygon representing
     * the part of a row between two coordinates col and col + 1.
     */
    private void drawingPolygon(final Color c, final int size, final int col,
            final int lPMinDY, final int lPMaxDY, final int cPMaxDY,
            final int cPMinDY, final Graphics2D g2d) {
        Color oldColor = g2d.getColor();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                0.5f));
        g2d.setColor(c);
        int lPDX, // lastPointDimensionX
        cPDX; // currentPointDimensionX
        cPDX = (col + 1) * m_distanceBetweenCoordinates;
        lPDX = col * m_distanceBetweenCoordinates;
        Polygon p = new Polygon();
        p.addPoint(lPDX, lPMinDY - size / 2);
        p.addPoint(lPDX, lPMaxDY + size / 2);
        p.addPoint(cPDX, cPMaxDY + size / 2);
        p.addPoint(cPDX, cPMinDY - size / 2);

        g2d.fillPolygon(p);
        g2d.drawPolygon(p);
        g2d.setComposite(AlphaComposite
                .getInstance(AlphaComposite.SRC_OVER, 1f));

        drawPointAsRectangle(cPDX, cPMinDY, g2d);
        drawPointAsRectangle(cPDX, cPMaxDY, g2d);
        drawPointAsRectangle(lPDX, lPMinDY, g2d);
        drawPointAsRectangle(lPDX, lPMaxDY, g2d);
        g2d.setColor(oldColor);
    }

    private int getPH(final double value) {
        return m_canvasHeight - ((int)(value * m_axisLength) + m_footerHeight);
    }

    /**
     * @param row the index of the row to be painted
     */
    private void paintRow(final int row, final Graphics2D g2d) {
        if ((m_visible[row])
                && ((m_hilited[row]) || (m_viewType != HIDE_UNHILITED))) {
            // painting row

            Color c;
            if (m_viewType == ALL_VISIBLE) {
                c = m_colors[row].getColor(m_selected[row], m_hilited[row]);
            } else {
                if ((m_hilited[row])) {
                    c = m_colors[row].getColor(m_selected[row], false);
                    // LOGGER.debug(c.toString());
                } else {
                    // faded color, unhilited
                    c = ColorAttr.INACTIVE;
                    // LOGGER.debug(c.toString());
                }

            }
            double size = m_sizes[row];

            TwoDim[][] doubleArray = m_content.getDoubleArray();
            int cPMinDY = 0;
            int cPMaxDY = 0;
            int lPMinDY = getPH(doubleArray[row][m_coordinateOrder[0]].getMin());
            int lPMaxDY = getPH(doubleArray[row][m_coordinateOrder[0]].getMax());
            for (int col = 1; col < m_columnCount; col++) {
                // getting the values in pixels
                cPMinDY = getPH(doubleArray[row][m_coordinateOrder[col]]
                        .getMin());
                cPMaxDY = getPH(doubleArray[row][m_coordinateOrder[col]]
                        .getMax());
                // drawing polygon between coordinates
                drawingPolygon(c, (int)(size * 10), col, lPMinDY, lPMaxDY,
                        cPMaxDY, cPMinDY, g2d);
                // reseting coordinates for the last point that was
                // drawn
                lPMinDY = cPMinDY;
                lPMaxDY = cPMaxDY;
            }
        }
    }

    private void paintCoordinatesStage(final Graphics2D g2d) {
        for (int row = 0; row < m_rowCount; row++) {
            if (!m_selected[row] && !m_hilited[row]) {
                paintRow(row, g2d);
            }
        }
        for (int row = 0; row < m_rowCount; row++) {
            if (!m_selected[row] && m_hilited[row]) {
                paintRow(row, g2d);
            }
        }
        for (int row = 0; row < m_rowCount; row++) {
            if (m_selected[row] && !m_hilited[row]) {
                paintRow(row, g2d);
            }
        }
        for (int row = 0; row < m_rowCount; row++) {
            if (m_selected[row] && m_hilited[row]) {
                paintRow(row, g2d);
            }
        }

        drawAxes(g2d);
    }

    /**
     * @param g the graphics
     */
    @Override
    protected synchronized void paintComponent(final Graphics g) {
        super.paintComponent(g);
        if (m_content != null) {
            // get dimensions
            int myHeight = this.getHeight();
            int myWidth = this.getWidth();
            // setting the graphical object to a grapics2D object
            Graphics2D g2d = (Graphics2D)g;
            // draw dimensions(coordinates)
            if (isDrawable(myHeight, myWidth)) {
                // anti-aliasing
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                // setting the canvas structure
                double angle = setCanvasStructure(myHeight, myWidth, g2d);
                // drawing...
                paintCoordinatesStage(g2d);
                // writing names of dimensions
                writeCoordNames(angle, g2d);
                // draw min and max for every number attribute
                drawCoordBounds(g2d);
            }
        } else {
            new JLabel("NO DATA!").paint(g);
            // get dimensions
            int myHeight = this.getHeight();
            int myWidth = this.getWidth();
            // setting the graphical object to a grapics2D object
            Graphics2D g2d = (Graphics2D)g;
            g2d.drawString("NO DATA", myWidth / 2, myHeight / 2);

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText(final MouseEvent e) {
        String[] types = m_content.getTypesVector();
        if (pointInStage(e.getX(), e.getY())) {
            if ((e.getX() + 3) % m_distanceBetweenCoordinates < 7) {
                int col = (e.getX() + 3) / m_distanceBetweenCoordinates - 1;
                TwoDim[][] doubleArray = m_content.getDoubleArray();

                for (int row = 0; row < m_rowCount; row++) {
                    if (isSelectable(row)) {
                        if (types[m_coordinateOrder[col]].compareTo("double") == 0
                                || types[m_coordinateOrder[col]]
                                        .compareTo("fuzzyinterval") == 0) {

                            int differenceMin = Math
                                    .abs(getPH(doubleArray[row][m_coordinateOrder[col]]
                                            .getMin())
                                            - e.getY());

                            if (differenceMin < 5) {
                                double returnValue = doubleArray[row][m_coordinateOrder[col]]
                                        .getMin();
                                double max = m_content.getMaxVector()[m_coordinateOrder[col]];
                                double min = m_content.getMinVector()[m_coordinateOrder[col]];
                                returnValue = returnValue * (max - min) + min;
                                // rounding value
                                returnValue = (double)(Math
                                        .round(returnValue * 1000)) / 1000;
                                return String.valueOf(returnValue);
                            }

                            int differenceMax = Math
                                    .abs(getPH(doubleArray[row][m_coordinateOrder[col]]
                                            .getMax())
                                            - e.getY());

                            if (differenceMax < 5) {

                                double returnValue = doubleArray[row][m_coordinateOrder[col]]
                                        .getMax();
                                double max = m_content.getMaxVector()[m_coordinateOrder[col]];
                                double min = m_content.getMinVector()[m_coordinateOrder[col]];
                                returnValue = returnValue * (max - min) + min;
                                // rounding value
                                returnValue = (double)(Math
                                        .round(returnValue * 1000)) / 1000;
                                return String.valueOf(returnValue);
                            }
                        } else {
                            int difference = Math
                                    .abs(getPH(doubleArray[row][m_coordinateOrder[col]]
                                            .getMin())
                                            - e.getY());
                            if (difference < 3) {
                                int ret = (int)Math
                                        .round(doubleArray[row][m_coordinateOrder[col]]
                                                .getMin()
                                                * (m_content
                                                        .getStringValuesNo()[m_coordinateOrder[col]] - 1));
                                return m_content.getStringValues()[m_coordinateOrder[col]][ret];
                            }

                        }
                    }
                }

            }
        }
        return null;

    }

    // ******************* hiliting methods **********************
    /**
     * {@inheritDoc}
     */
    public void hiLite(final KeyEvent event) {
        Set<RowKey> keySet = event.keys();
        for (int row = 0; row < m_rowCount; row++) {
            if (keySet.contains(m_keys[row])) {
                m_hilited[row] = true;
            }
        }
        repaint();
    }

    /**
     * {@inheritDoc}
     */
    public void unHiLite(final KeyEvent event) {
        Set<RowKey> keySet = event.keys();
        for (int row = 0; row < m_rowCount; row++) {
            if (keySet.contains(m_keys[row])) {
                m_hilited[row] = false;
            }
        }
        repaint();
    }


    /**
     * {@inheritDoc}
     */
    public void unHiLiteAll(final KeyEvent ke) {
        for (int i = 0; i < m_rowCount; i++) {
            m_hilited[i] = false;
        }
        repaint();
    }

    /**
     * hilite the selected rows in all views.
     */
    public void hiliteSelected() {
        for (int i = 0; i < m_rowCount; i++) {
            if (m_vh != null) {
                if (m_vh.isSelected(m_keys[i])) {
                    m_hdl.fireHiLiteEvent(m_keys[i]);
                    m_hilited[i] = true;
                }
            } else {
                if (m_selected[i]) {
                    m_hilited[i] = true;
                    m_hdl.fireHiLiteEvent(m_keys[i]);
                }
            }
        }
        repaint();
    }

    /**
     * unhilite the selected rows in all views.
     */
    public void unHiliteSelected() {
        for (int i = 0; i < m_rowCount; i++) {
            if (m_vh != null) {
                if (m_vh.isSelected(m_keys[i])) {
                    m_hdl.fireUnHiLiteEvent(m_keys[i]);
                    m_hilited[i] = false;
                }
            } else {
                if (m_selected[i]) {
                    m_hilited[i] = false;
                    m_hdl.fireUnHiLiteEvent(m_keys[i]);
                }
            }
        }
        repaint();
    }

    /**
     * clears hilited rows in all views.
     */
    public void clearHilite() {
        m_hdl.fireClearHiLiteEvent();
    }

    // ******************* selection methods **********************

    /**
     * {@inheritDoc}
     */
    public void mouseClicked(final MouseEvent e) {
    }

    /**
     * @param x the x value of point
     * @param y the y value of point
     * @return if point is in the stage bounds
     */
    public boolean pointInStage(final int x, final int y) {
        return ((x <= m_distanceBetweenCoordinates * m_columnCount)
                && (x >= m_distanceBetweenCoordinates)
                && (y <= m_canvasHeight - m_footerHeight) 
                && (y >= m_headerHeight));
    }

    /**
     * {@inheritDoc}
     */
    public void mouseEntered(final MouseEvent e) {

    }

    /**
     * {@inheritDoc}
     */
    public void mouseExited(final MouseEvent e) {

    }

    /**
     * {@inheritDoc}
     */
    public void mousePressed(final MouseEvent e) {
        if (m_content != null) {
            setDragStartCoord(e.getX(), e.getY());
            setMouseJustPressed(true);
            if ((e.getX() + 3) % m_distanceBetweenCoordinates < 5) {
                // if I am currently trying to move a coordinate
                setGrabCoord(true);
                setLastX(e.getX());
            }
        }
        // LOGGER.debug("mouse pressed");
    }

    /**
     * Comment for <code>clearSelection</code> make all rows unselected.
     */
    public void clearSelection() {
        for (int row = 0; row < m_rowCount; row++) {
            setSelected(row, false);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void mouseReleased(final MouseEvent e) {

        if (m_content != null) {
            if (e.isPopupTrigger()) {
                popUpMenu(e);
                return;
            }

            if (!(e.isControlDown()) && !(e.isShiftDown())) {
                clearSelection();
            }

            TwoDim[][] doubleArray = m_content.getDoubleArray();

            if (getMouseIsDragged()) {
                if (getGrabCoord()) {
                    // i was moving an axis
                    // the axis I dragged
                    int no = (m_dragStartX + 1) / m_distanceBetweenCoordinates
                            - 1;

                    // the axis that has to stay before it
                    int no1 = (e.getX() + 1) / m_distanceBetweenCoordinates - 1;
                    int i;
                    if (no1 < -1) {
                        no1 = -1;
                    }
                    if (no1 >= m_columnCount) {
                        no1 = m_columnCount - 1;
                    }

                    if ((no1 >= -1) && (no1 < m_columnCount)) {
                        if (no < no1) {
                            int aux = m_coordinateOrder[no];
                            for (i = no; i < no1; i++) {
                                m_coordinateOrder[i] = m_coordinateOrder[i + 1];
                            }
                            m_coordinateOrder[no1] = aux;
                        } else {
                            if (no > no1) {
                                int aux = m_coordinateOrder[no];
                                for (i = no - 1; i > no1; i--) {
                                    m_coordinateOrder[i + 1] = m_coordinateOrder[i];
                                }
                                m_coordinateOrder[no1 + 1] = aux;
                            }
                        }
                    }
                    setGrabCoord(false);
                    repaint();
                } else {
                    // i selected some stuff
                    // select rows intersecting rectangular selector(mouse drag)
                    for (int row = 0; row < m_rowCount; row++) {
                        if (isSelectable(row)) {
                            Polygon p = new Polygon();
                            for (int col = 0; col < m_columnCount; col++) {
                                p
                                        .addPoint(
                                                (col + 1)
                                                        * m_distanceBetweenCoordinates,
                                                getPH(doubleArray[row][m_coordinateOrder[col]]
                                                        .getMin()));
                            }
                            for (int col = m_columnCount - 1; col >= 0; col--) {
                                p
                                        .addPoint(
                                                (col + 1)
                                                        * m_distanceBetweenCoordinates,
                                                getPH(doubleArray[row][m_coordinateOrder[col]]
                                                        .getMax()));
                            }
                            int x = Math.min(m_dragStartX, m_dragStopX);
                            int y = Math.min(m_dragStartY, m_dragStopY);
                            int width = Math.abs(m_dragStopX - m_dragStartX);
                            int height = Math.abs(m_dragStopY - m_dragStartY);
                            if (p.intersects(x, y, width, height)) {
                                if (m_visible[row]) {
                                    if (e.isControlDown()) {
                                        setSelected(row, !m_selected[row]);
                                    } else {
                                        setSelected(row, true);
                                    }
                                }
                            }
                        }
                    }
                    setMouseIsDragged(false);
                    repaint();
                }
            } else {
                // select rows intersecting point based selector(mouse click)
                if (pointInStage(e.getX(), e.getY())) {
                    int col = (e.getX() - 1) / m_distanceBetweenCoordinates - 1;
                    for (int row = 0; row < m_rowCount; row++) {
                        if (isSelectable(row)) {
                            Polygon p = new Polygon();
                            p
                                    .addPoint(
                                            (col + 1)
                                                    * m_distanceBetweenCoordinates,
                                            getPH(doubleArray[row][m_coordinateOrder[col]]
                                                    .getMin()));
                            // the following +1 for java only:
                            // emtpy, horizontal(!)
                            // polygons don't contain any points...
                            p
                                    .addPoint(
                                            (col + 1)
                                                    * m_distanceBetweenCoordinates,
                                            getPH(doubleArray[row][m_coordinateOrder[col]]
                                                    .getMax()) + 1);
                            p
                                    .addPoint(
                                            (col + 2)
                                                    * m_distanceBetweenCoordinates,
                                            getPH(doubleArray[row][m_coordinateOrder[col + 1]]
                                                    .getMax()));
                            p
                                    .addPoint(
                                            (col + 2)
                                                    * m_distanceBetweenCoordinates,
                                            getPH(doubleArray[row][m_coordinateOrder[col + 1]]
                                                    .getMin()));
                            if (p.intersects(e.getX() - 1, e.getY() - 1, 3, 3)) {
                                if (m_visible[row]) {
                                    if (e.isControlDown()) {
                                        setSelected(row, !m_selected[row]);
                                    } else {
                                        setSelected(row, true);
                                    }
                                }
                            }
                        }
                    }
                    repaint();
                }
            }

        }
    }

    /**
     * {@inheritDoc}
     */
    public void mouseDragged(final MouseEvent e) {
        if (m_content != null) {
            setMouseIsDragged(true);
            if (!getGrabCoord()) {
                if (!getMouseJustPressed()) {
                    drawScrollRectangle();
                }

                setDragStopCoord(e.getX(), e.getY());
                drawScrollRectangle();
                setMouseJustPressed(false);

            } else {

                setDragStopCoord(e.getX(), e.getY());
                if (e.getX() != getLastX()) {
                    drawScrollCoordinate(getLastX());
                    drawScrollCoordinate(e.getX());
                    setLastX(e.getX());
                }

                setMouseJustPressed(false);

            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void mouseMoved(final MouseEvent e) {

    }

    /**
     * 
     * @param e - the popup triggering event
     */
    public void popUpMenu(final MouseEvent e) {
        JPopupMenu menu = new JPopupMenu();
        menu.add(createHiLiteMenu());
        menu.add(createViewTypeMenu());
        menu.show(e.getComponent(), e.getX(), e.getY());
    }

    // create JMenu

    /** Popup menu entry constant. */
    public static final String POPUP_HILITE_SELECTED = HiLiteHandler.HILITE_SELECTED;

    /** Popup menu entry constant. */
    public static final String POPUP_UNHILITE_SELECTED = HiLiteHandler.UNHILITE_SELECTED;

    /** Popup menu entry constant. */
    public static final String POPUP_UNHILITE = HiLiteHandler.CLEAR_HILITE;

    /** Popup menu entry constant. */
    public static final String POPUP_HIDE_UNHILITED = "Hide UnHiLited";

    /** Popup menu entry constant. */
    public static final String POPUP_ALL_VISIBLE = "All Visible";

    /** Popup menu entry constant. */
    public static final String POPUP_FADE_UNHILITED = "Fade UnHiLited";

    /** View Type constant. */
    public static final int ALL_VISIBLE = 0;

    /** View Type constant. */
    public static final int HIDE_UNHILITED = 1;

    /** View Type constant. */
    public static final int FADE_UNHILITED = 2;

    /** Coordinate Order constant. */
    public static final String COORDINATE_ORDER_SET = "Set Coordinate Order";

    /** Coordinate Order constant. */
    public static final String COORDINATE_ORDER_RESET = "Reset Coordinate "
            + "Order to Initial State";

    /**
     * 
     * @return - a JMenu entry handling the hiliting of objects.
     */
    public JMenu createHiLiteMenu() {
        JMenu menu = new JMenu(HiLiteHandler.HILITE);
        menu.setMnemonic('H');
        ActionListener actL = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                if (e.getActionCommand().equals(POPUP_HILITE_SELECTED)) {
                    // hilite selected rows with the hilite manager
                    hiliteSelected();
                } else if (e.getActionCommand().equals(POPUP_UNHILITE_SELECTED)) {
                    unHiliteSelected();
                } else if (e.getActionCommand().equals(POPUP_UNHILITE)) {
                    clearHilite();
                }
            }
        };
        JMenuItem item = new JMenuItem(POPUP_HILITE_SELECTED);
        item.addActionListener(actL);
        item.setMnemonic('H');
        menu.add(item);
        item = new JMenuItem(POPUP_UNHILITE_SELECTED);
        item.addActionListener(actL);
        item.setMnemonic('U');
        menu.add(item);
        item = new JMenuItem(POPUP_UNHILITE);
        item.addActionListener(actL);
        item.setMnemonic('E');
        menu.add(item);
        return menu;
    }

    /**
     * 
     * @return - a JMenu entry handling the view type.
     */
    public JMenu createViewTypeMenu() {
        JMenu menu = new JMenu("View Type");
        menu.setMnemonic('V');
        ActionListener actL = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                if (e.getActionCommand().equals(POPUP_ALL_VISIBLE)) {
                    // set view to ALL VISIBLE
                    setViewType(ALL_VISIBLE);
                    ((JRadioButtonMenuItem)e.getSource()).firePropertyChange(
                            POPUP_ALL_VISIBLE, false, true);
                    repaint();
                } else if (e.getActionCommand().equals(POPUP_HIDE_UNHILITED)) {
                    // set view to HIDE_UNHILITED
                    setViewType(HIDE_UNHILITED);
                    unselectUnhilited();
                    ((JRadioButtonMenuItem)e.getSource()).firePropertyChange(
                            POPUP_HIDE_UNHILITED, false, true);
                    repaint();
                } else if (e.getActionCommand().equals(POPUP_FADE_UNHILITED)) {
                    // set view to FADE_UNHILITED
                    setViewType(FADE_UNHILITED);
                    unselectUnhilited();
                    ((JRadioButtonMenuItem)e.getSource()).firePropertyChange(
                            POPUP_FADE_UNHILITED, false, true);
                    repaint();
                }
            }
        };
        ButtonGroup group = new ButtonGroup();
        // JMenuItem item
        JMenuItem item = new JRadioButtonMenuItem(POPUP_ALL_VISIBLE);
        item.setSelected(m_viewType == ALL_VISIBLE);
        item.addActionListener(actL);
        item.setMnemonic('A');
        item.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(final PropertyChangeEvent e) {
                ((JRadioButtonMenuItem)e.getSource())
                        .setSelected(m_viewType == ALL_VISIBLE);
            }
        });
        group.add(item);
        menu.add(item);
        item = new JRadioButtonMenuItem(POPUP_HIDE_UNHILITED);
        item.setSelected(m_viewType == HIDE_UNHILITED);
        item.addActionListener(actL);
        item.setMnemonic('H');
        item.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(final PropertyChangeEvent e) {
                ((JRadioButtonMenuItem)e.getSource())
                        .setSelected(m_viewType == HIDE_UNHILITED);
            }
        });
        group.add(item);
        menu.add(item);
        item = new JRadioButtonMenuItem(POPUP_FADE_UNHILITED);
        item.setSelected(m_viewType == FADE_UNHILITED);
        item.addActionListener(actL);
        item.setMnemonic('F');
        item.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(final PropertyChangeEvent e) {
                ((JRadioButtonMenuItem)e.getSource())
                        .setSelected(m_viewType == FADE_UNHILITED);
            }
        });
        group.add(item);
        menu.add(item);
        return menu;
    }

    /**
     * 
     * @return - A JMenu entry handling the coordinate order
     */
    public JMenu createCoordinateOrderMenu() {
        JMenu menu = new JMenu("Coordinate Order");
        menu.setMnemonic('C');
        ActionListener actL = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                if (e.getActionCommand().equals(COORDINATE_ORDER_RESET)) {
                    for (int i = 0; i < m_columnCount; i++) {
                        m_coordinateOrder[i] = i;
                    }
                    repaint();
                }
            }

        };
        JMenuItem item = new JMenuItem(COORDINATE_ORDER_RESET);
        item.addActionListener(actL);
        item.setMnemonic('R');
        menu.add(item);
        return menu;
    }

    /**
     * @return Returns the m_columnCount.
     */
    public int getColumnCount() {
        return m_columnCount;
    }

    /**
     * @param i the coordinate for which the order has to be set
     * @param order The m_coordinateOrder[i] to set.
     */
    public void setCoordinateOrder(final int i, final int order) {
        m_coordinateOrder[i] = order;
    }

    /**
     * @param i the row
     * @return if the row is selected
     */
    public boolean isSelected(final int i) {
        return m_selected[i];
    }

    /**
     * @param i the row
     * @return if the row is selected
     */
    public boolean isSelectable(final int i) {
        return ((m_visible[i]) && ((m_hilited[i]) || (m_viewType == ALL_VISIBLE)));
    }

    /**
     * @param i the row
     * @return if the row is visible
     */
    public boolean isVisible(final int i) {
        return m_visible[i];
    }

    /**
     * @param i the row number
     * @param value whether row i is visible or not
     */
    public void setVisible(final int i, final boolean value) {
        if (m_vh != null) {
            if (value) {
                m_vh.makeVisible(m_keys[i]);
            } else {
                m_vh.makeInvisible(m_keys[i]);
                // if it's invisible, it's not selectable, and thus
                // loses selected status
                // m_vh.unselect(m_keys[i].getId());
                // to do: make unselectable in handler
            }
        } else {
            m_visible[i] = value;
        }
    }

    /**
     * @param i the row number
     * @param value whether row i is visible or not
     */
    public void setSelected(final int i, final boolean value) {
        if (m_vh != null) {
            if (value) {
                if (isSelectable(i)) {
                    m_vh.select(m_keys[i]);
                }
            } else {
                m_vh.unselect(m_keys[i]);
            }
        }
        m_selected[i] = value;

    }

    /**
     * routine called in the menu, when the view type changes from all visible.
     */
    void unselectUnhilited() {
        for (int i = 0; i < m_rowCount; i++) {
            if (!m_hilited[i]) {
                m_selected[i] = false;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void select(final VisibilityEvent event) {
        Set<RowKey> keySet = event.keys();
        for (int row = 0; row < m_rowCount; row++) {
            if (keySet.contains(m_keys[row])) {

                m_selected[row] = true;

            }
        }
        repaint();

    }

    /**
     * {@inheritDoc}
     */
    public void unselect(final VisibilityEvent event) {
        Set<RowKey> keySet = event.keys();
        for (int row = 0; row < m_rowCount; row++) {
            if (keySet.contains(m_keys[row])) {
                m_selected[row] = false;
            }
        }
        repaint();

    }

    /**
     * {@inheritDoc}
     */
    public void makeVisible(final VisibilityEvent event) {
        Set<RowKey> keySet = event.keys();
        for (int row = 0; row < m_rowCount; row++) {
            if (keySet.contains(m_keys[row])) {
                m_visible[row] = true;
            }
        }
        repaint();

    }

    /**
     * {@inheritDoc}
     */
    public void makeInvisible(final VisibilityEvent event) {
        Set<RowKey> keySet = event.keys();
        for (int row = 0; row < m_rowCount; row++) {
            if (keySet.contains(m_keys[row])) {
                m_visible[row] = false;
            }
        }
        repaint();
    }

}
