/* 
 * 
 * -------------------------------------------------------------------
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
 *   04.03.2005 (ohl): created
 */
package org.knime.base.node.viz.scatterplot;

import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.knime.base.node.viz.plotter2D.AbstractDrawingPane;
import org.knime.core.data.RowKey;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.node.NodeLogger;

/**
 * The component inside the scroll pane. It only draws the data points spreading
 * them over its entire size.
 * 
 * @author Christoph Sieb, University of Konstanz
 * @author Peter Ohl, University of Konstanz
 */
public class ScatterPlotDrawingPane extends AbstractDrawingPane {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(ScatterPlotDrawingPane.class);

    private static final int INITIAL_DOT_SIZE = 5;

    // DotInfo used to draw dots
    private DotInfoArray m_dots;

    // the size of each dot
    private int m_dotSize;

    // flag indicating to gray out not hightlighted dots
    private boolean m_grayUnHilit;

    private boolean m_hideUnHiLited;

    // Hash set of selected dots
    private final HashSet<RowKey> m_selDots;

    /**
     * Creates a new empty drawing pane.
     */
    public ScatterPlotDrawingPane() {
        super();
        m_dots = null;
        m_selDots = new HashSet<RowKey>();
        m_dotSize = INITIAL_DOT_SIZE;
        m_grayUnHilit = false;
        m_hideUnHiLited = false;
        setToolTipText("");
    }

    /**
     * Sets the flag that determines if unhilited dots are displayed in gray
     * color or with their corresponding color attribute.
     * 
     * @param fade the value of the flag. If true, unhilited dots will be
     *            displayed gray - if false, all dots will be painted with their
     *            corresponding color attribute.
     */
    public void setFadeUnHiLitedDots(final boolean fade) {
        m_grayUnHilit = fade;
    }

    /**
     * Sets flag that determines if only the hilited dots should be drawn.
     * 
     * @param hide true, if only the hilited dots should be drawn, false
     *            otherwise.
     */
    public void setHideUnHiLited(final boolean hide) {
        m_hideUnHiLited = hide;
    }

    /**
     * Indicates if unhilited dots are hidden.
     * 
     * @return true, if unhilited dots are hidden, false otherwise.
     */
    public boolean isHideUnHiLited() {
        return m_hideUnHiLited;
    }

    /**
     * Clears the hiliting of all dots.
     */
    public void clearHilite() {
        for (DotInfo dot : m_dots.getDots()) {
            dot.setHiLit(false);
        }

        repaint();
    }

    /**
     * @return true if unhilited dots will be displayed gray, false if all dots
     *         will be painted with their corresponding color attribute.
     */
    public boolean getFadeUnHiLitedDots() {
        return m_grayUnHilit;
    }

    /**
     * Checks if there is at least one dot hilited.
     * 
     * @return true if there is at least one dot hilited
     */
    public boolean isAtLeastOneDotHilited() {

        for (DotInfo dot : m_dots.getDots()) {

            if (dot.isHiLit()) {
                return true;
            }
        }

        return false;
    }

    /*
     * Returns the length of the square representing a dot. Depends on the user
     * chosen general dot size and the size attribute of the table spec's size
     * handler.
     * 
     * @return the lenght of one side of the square representing this dot
     */
    private int calculateDotSize(final double size) {
        return DotInfoArray.calculateDotSize(m_dotSize, size);
    }

    /**
     * Calculates the half max dot size according to the current user specified
     * size. The half max dot size depends on the use specified size and the max
     * size given by the size handler. This max value is 1.0. The max size
     * calculates the same way as <code>calculateDotSize</code> with a dot of
     * size 1.0. The half max dot size is mainly used by the
     * <code>AbstractPlotter2D</code> to reduce the size of the
     * <code>AbstractDrawingPane</code> so that the dots painted larger than 1
     * pixel are fully visible. Further the half size is rounde upwards to
     * guarantee that all points fit in the pane.
     * 
     * @return the current half max dot size given the user chosen value
     *         <code>m_dotSize</code>
     */
    public int getCurrentHalfMaxDotSize() {
        return (int)Math.ceil(getCurrentMaxDotSize() / 2.0);

    }

    /**
     * Calculates the max dot size according to the current user specified size.
     * The half max dot size depends on the use specified size and the max size
     * given by the size handler. This max value is 1.0. The max size calculates
     * the same way as <code>calculateDotSize</code> with a dot of size 1.0.
     * 
     * @return the current max dot size given the user chosen value
     *         <code>m_dotSize</code>
     */
    public int getCurrentMaxDotSize() {
        return calculateDotSize(1.0);

    }

    /**
     * Set the size the dots are plotted with.
     * 
     * @param size the new size
     */
    public void setDotSize(final int size) {
        m_dotSize = size;
    }

    /**
     * get the size dots are plotted with.
     * 
     * @return the size
     */
    public int getDotSize() {
        return m_dotSize;
    }

    /**
     * @return returns the DotInfoArray that stores info about each ploted dot.
     */
    public DotInfoArray getDotInfoArray() {
        return m_dots;
    }

    /**
     * Stores a new DotInfoArray in the object. It will take over the reference
     * to the new structure.
     * 
     * @param dots the new structure storing DotInfos.
     */
    public void setDotInfoArray(final DotInfoArray dots) {
        m_dots = dots;
    }

    /**
     * @return the set of RowKeys (i.e. Datacells) of currently selected dots.
     */
    public Set<RowKey> getSelectedSet() {
        return m_selDots;
    }

    /**
     * Changes the selection state of all dots at the provided coordinates. If
     * the rowkey of a dot at (x,y) has been in the SelectedSet, it will be
     * removed from it, if it was not in the set it will be added.
     * 
     * @param x The x coord.
     * @param y The y coord.
     */
    @Override
    public void toggleSelectionAt(final int x, final int y) {
        if (m_dots != null) {
            List<DotInfo> selected = m_dots.getDotsAt(x, y, m_dotSize);
            // select them all for now...
            for (int i = 0; i < selected.size(); i++) {
                // actually toggle their state
                RowKey rowKey = selected.get(i).getRowID();
                if (m_selDots.contains(rowKey)) {
                    m_selDots.remove(rowKey);
                } else {
                    m_selDots.add(rowKey);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberSelectedElements() {
        return getSelectedSet().size();
    }

    /**
     * Selects all dots in the rectangle specified by the MouseDown coord. and
     * the DragCoord. If one of these coord. is not set (i.e. is -1) nothing
     * will happen. Otherwise all rowkeys of the dots contained in that
     * rectangle will be added to the SelectedSet.
     * 
     * @see org.knime.base.node.viz.plotter2D.AbstractDrawingPane
     *      #selectElementsInDragTangle(int, int, int, int)
     */
    @Override
    protected void selectElementsInDragTangle(final int mouseDownX,
            final int mouseDownY, final int mouseUpX, final int mouseUpY) {

        if ((mouseDownX == -1) || (mouseDownY == -1) || (mouseUpX == -1)
                || (mouseUpY == -1)) {
            return;
        }

        if (m_dots == null) {
            return;
        }
        List<DotInfo> dots = m_dots.getDotsContainedIn(mouseDownX, mouseDownY,
                mouseUpX, mouseUpY, m_dotSize);

        // m_dragX und ist schon als privat in abstract class. rausmachen.
        // �bergeben der bounderies an diese methode

        // select them all ...
        for (int i = 0; i < dots.size(); i++) {
            m_selDots.add(dots.get(i).getRowID());
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText(final MouseEvent e) {

        if (m_dots == null) {
            return null;
        }

        List<DotInfo> dots = m_dots.getDotsAt(e.getX(), e.getY(), m_dotSize);

        if (dots.size() == 0) {
            return null;
        } else {
            StringBuffer result = new StringBuffer("");
            for (int i = 0; i < dots.size(); i++) {
                result.append("'");
                result.append(dots.get(i).getRowID().toString());
                result.append("' ");
            }
            return result.toString();
        }
    }

    // /**
    // * clears out all DotInfos and selected dots.
    // */
    // public void clear() {
    // m_dots = null;
    // m_selDots.clear();
    // }

    /**
     * Prints the list of selected dots. And a LF.
     */
    public void printLnSelectedMap() {
        if (m_selDots == null) {
            LOGGER.debug("Selected #n/a (RowKey): (null)");
            return;
        }
        LOGGER.debug("Selected #" + m_selDots.size() + "(RowKey): ");
        Iterator<RowKey> i = m_selDots.iterator();
        while (i.hasNext()) {
            LOGGER.debug("('" + i.next().getString() + "') ");
        }
    }

    /**
     * @return Returns the m_dots.
     */
    protected DotInfoArray getDots() {
        return m_dots;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void paintPlotDrawingPane(final Graphics g) {

        // paint the data points - if any
        if ((m_dots != null) && (m_dots.length() != 0)) {

            DotInfo[] dotInfo = m_dots.getDots();

            for (int i = 0; i < dotInfo.length; i++) {

                if (!dotInfo[i].paintDot()) {
                    // dont paint dots with negative coord. (These are dots with
                    // missing values or NaN/Infinity values.)
                    continue;
                }

                // if hide unhilited skip loop to next dot.
                if (m_hideUnHiLited && !dotInfo[i].isHiLit()) {
                    continue;
                }

                // draw the data point
                if (m_grayUnHilit && !dotInfo[i].isHiLit()) {
                    g.setColor(ColorAttr.INACTIVE);
                } else {
                    g.setColor(dotInfo[i].getColor().getColor());
                }

                int size = calculateDotSize(dotInfo[i].getSize());
                int halfSize = (int)Math.round(size / 2.0);

                // paint a dot that the actual value is approximately in
                // the middle of the dot
                int xStart = dotInfo[i].getXCoord() - halfSize;
                int yStart = dotInfo[i].getYCoord() - halfSize;

                g.fillRect(xStart, yStart, size, size);

                // paint selection border if selected
                // and the hilited dots
                boolean isSelected = m_selDots.contains(dotInfo[i].getRowID());
                boolean isHilit = dotInfo[i].isHiLit();
                if (m_grayUnHilit || m_hideUnHiLited) {
                    isHilit = false;
                }
                if (isSelected || isHilit) {
                    // as we draw a border to hilit/select: get the border color
                    g.setColor(dotInfo[i].getColor().getBorderColor(isSelected,
                            isHilit));

                    // calculate the width of the border dependent on
                    // the size of the dot (assume 30% of the size)
                    int selectionBorderWidth = 1; // (int)Math.ceil(size *
                    // 0.2);

                    // the width is drawn by repeating the rendering of the
                    // selection rectangle as many times as the width is.
                    // the first one is drawn anyway.
                    g.drawRect(xStart, yStart, size - 1, size - 1);

                    for (int j = 1; j < selectionBorderWidth; j++) {
                        g.drawRect(xStart + j, yStart + j, size - 1 - 2 * j,
                                size - 1 - 2 * j);
                    }
                }

            }

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void clearSelection() {

        // m_dots = null;
        getSelectedSet().clear();
    }
}
