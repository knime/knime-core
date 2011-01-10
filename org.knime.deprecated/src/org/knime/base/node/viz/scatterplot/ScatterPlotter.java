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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.viz.scatterplot;

import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.Set;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

import org.knime.base.node.util.DataArray;
import org.knime.base.node.viz.plotter2D.AbstractPlotter2D;
import org.knime.base.util.coordinate.Coordinate;
import org.knime.base.util.coordinate.NumericCoordinate;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.KeyEvent;

/**
 * Implements the ScrollPane including a scatter plot of data points.
 * 
 * @author Christoph Sieb, University of Konstanz
 * @author Peter Ohl, University of Konstanz
 */
public class ScatterPlotter extends AbstractPlotter2D implements 
    ActionListener {
    // the guy we get the hilite status from
    private HiLiteHandler m_hiliteHdlr;

    // a reference to the model
    private DataArray m_rowContainer;

    /**
     * The table spec index of <code>m_xColName</code>.
     */
    private int m_xIndex;

    /**
     * The table spec index of <code>m_yColName</code>.
     */
    private int m_yIndex;

    private Double m_userXmin;

    private Double m_userXmax;

    private Double m_userYmin;

    private Double m_userYmax;

    // private boolean m_crosshair;

    private JMenu m_hiliteMenu;

    private JMenuItem m_hilite;

    private JMenuItem m_unhilite;

    private JMenuItem m_clearHilite;

    private JMenuItem m_fade;

    private JMenuItem m_hide;
    
    private JMenuItem m_show;
    
    private static final String POPUP_SHOW = "Show UnHiLited";

    /**
     * Remembers if the currently set columns are valid. Set in
     * <code>setSelectedColumns</code> method.
     */
    private boolean m_invalidColumn;

    /**
     * Creates a new instance of a AbstractPlotter2D.
     * 
     * @param rowContainer the rows we are getting the data to plot from
     * @param initialWidth The width at zoom 1x.
     * @param props the scatterplot properties associated with this scatterplot
     */
    public ScatterPlotter(final DataArray rowContainer, final int initialWidth,
            final ScatterProps props) {
        this(rowContainer, initialWidth, props, new ScatterPlotDrawingPane());
    }

    /**
     * Creates a new instance of a AbstractPlotter2D.
     * 
     * @param rowContainer the rows we are getting the data to plot from
     * @param initialWidth The width at zoom 1x.
     * @param props pthe scatterplot properties associated with this scatterplot
     * @param pane The underlying pane where the points are drawn in.
     */
    public ScatterPlotter(final DataArray rowContainer, final int initialWidth,
            final ScatterProps props, final ScatterPlotDrawingPane pane) {

        super(initialWidth, props, pane);

        m_rowContainer = rowContainer;

        // Hilite manager
        m_hiliteHdlr = null; // to get the hilite state from

        // set them null - to indicate user didn't set em yet.
        m_userXmin = null;
        m_userXmax = null;
        m_userYmin = null;
        m_userYmax = null;

        m_invalidColumn = true;

        // BoxLayout is the only layout that honours max/preferred size,
        // which makes our stuff look right
        // setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

        // set the initial start tick offset which depends on the dot size
        getColHeader().setStartTickOffset(pane.getCurrentHalfMaxDotSize());
        getRowHeader().setStartTickOffset(pane.getCurrentHalfMaxDotSize());
        if (m_rowContainer != null) {
            props.setSelectables(m_rowContainer.getDataTableSpec());
        } else {
            props.setSelectables(null);
        }

        updateDotsAndPaint();

        // set the hilite menu
        // setHiliteMenu(getHiLiteMenu());
    }

    /**
     * this function reads in RowInfo from the model and transfers what it needs
     * into our DotInfo container.
     * 
     * @param newRowContainer the new rowcontainer from the model to paint
     */
    public void modelDataChanged(final DataArray newRowContainer) {
        // store the new row contaier coming from the model
        m_rowContainer = newRowContainer;
        checkColumns();
//         if (getXColName() != null) {
//         setXColumn(getXColName());
//         }
//         if (getYColName() != null) {
//         setYColumn(getYColName());
//         }

        // generates dots, calculates coordinates and repaints
        updateDotsAndPaint();
    }

    /**
     * Returns <code>ColorAttr</code> for the given row.
     * 
     * @param row Row to get <code>ColorAttr</code> for.
     * @return The <code>ColorAttr</code> for the given row.
     */
    protected ColorAttr getColorAttr(final DataRow row) {
        return m_rowContainer.getDataTableSpec().getRowColor(row);
    }

    /**
     * Returns the size of the given row.
     * 
     * @param row Row to return its size.
     * @return The given rows size.
     */
    protected double getSize(final DataRow row) {
        return m_rowContainer.getDataTableSpec().getRowSize(row);
    }

    /*
     * takes the data from the private row container and recalculates the dots,
     * the coordinates, adjusts sizes and repaints.
     */
    private void updateDotsAndPaint() {

        getScatterPlotterDrawingPane().clearSelection();
        // get the rowInfo from the model
        DataArray rowsCont = m_rowContainer;
        if (rowsCont != null) {
            // and create a new DotInfo array with the rowKeys in the DotInfos.
            DotInfo[] newDots = new DotInfo[rowsCont.size()];
            for (int r = 0; r < rowsCont.size(); r++) {
                DataRow row = rowsCont.getRow(r);
                double size = getSize(row);
                ColorAttr colorAttr = getColorAttr(row);
                newDots[r] = new DotInfo(0, 0, row.getKey(), false, colorAttr,
                        size, r);
            }

            // now create a new DotInfoArray
            DotInfoArray newDotArray = new DotInfoArray(newDots);
            // store it in the drawing pane
            getScatterPlotterDrawingPane().setDotInfoArray(newDotArray);
            // update hilit and colors.
            updateDotHiLiting();

            // and get the coordinates calculated.
            adjustSizes();
            calculateCoordinates(newDotArray);
            repaint();
        }
    }

    /**
     * Convenience method to cast the drawing pane.
     * 
     * @return the underlying scatter plotter drawing pane
     */
    private ScatterPlotDrawingPane getScatterPlotterDrawingPane() {

        return (ScatterPlotDrawingPane)getDrawingPane();
    }

    /**
     * call this function before killing this object. It will unregister itself
     * from all event creators.
     */
    public void shutDown() {
        // end all OS events
        // m_drawingPane.removeMouseListener(this);
        // m_drawingPane.removeMouseMotionListener(this);
        // the only actions we were listening to are actions from the
        // popup menu. That menu will be dying with us - no need to unregister.

        if (m_hiliteHdlr != null) {
            m_hiliteHdlr.removeHiLiteListener(this);
        }
    }

    /**
     * clears the data and the display.
     */
    public void clear() {
        getScatterPlotterDrawingPane().clearSelection(); // clear the dots
        // and selection
        getRowHeader().setToolTipText("");
        getColHeader().setToolTipText("");
        repaint();
    }

    /**
     * sets a new dot size.
     * 
     * @param size the number of pixles (an odd integer)
     */
    public void setDotSize(final int size) {
        if ((size != getScatterPlotterDrawingPane().getDotSize()) 
                && (size > 0)) {
            getScatterPlotterDrawingPane().setDotSize(size);
            // with bigger dots we have less area to draw in:
            // therefore set the new offset into the header and
            // recalculate the pane
            getRowHeader().setStartTickOffset(
                    getScatterPlotterDrawingPane().getCurrentHalfMaxDotSize());
            getColHeader().setStartTickOffset(
                    getScatterPlotterDrawingPane().getCurrentHalfMaxDotSize());
            calculateCoordinates(getScatterPlotterDrawingPane()
                    .getDotInfoArray());
            repaint();
        }
    }

    /**
     * @return the current size dots are plotted with.
     */
    int getDotSize() {
        return getScatterPlotterDrawingPane().getDotSize();
    }

    /**
     * Sets the flag that determines if unhilited dots are displayed in gray
     * color or with their corresponding color attribute. Defaults to false.
     * 
     * @param fadeThem the value of the flag. If true, unhiglighted dots will be
     *            displayed gray - if false, all dots will be painted with their
     *            corresponding color attribute.
     */
    public void setFadeUnHiLited(final boolean fadeThem) {
        getScatterPlotterDrawingPane().setFadeUnHiLitedDots(fadeThem);
    }

    /**
     * @return true if unhilited dots will be displayed gray, false if all dots
     *         will be painted with their corresponding color attribute.
     */
    public boolean getFadeUnHiLited() {
        return getScatterPlotterDrawingPane().getFadeUnHiLitedDots();
    }

    /**
     * 
     * @return true if unhilited dots are hidden, false otherwise.
     */
    public boolean isHideUnHiLited() {
        return getScatterPlotterDrawingPane().isHideUnHiLited();
    }

    /**
     * 
     * @param hide true if only hilited dots should be displayed, false
     *            otherwise.
     */
    public void setHideUnHiLited(final boolean hide) {
        getScatterPlotterDrawingPane().setHideUnHiLited(hide);
    }

    /*
     * given the actual size of the drawing pane, the actual zoom factor, and
     * min/max values it calculates the screen coordinates for each dot info in
     * the array passed in. It accesses the model to retrieve the actual values
     * of the rows. It changes the contents of the DotInfos passed in. It also
     * triggers resorting of the sorted lists in the dot container.
     */
    private void calculateCoordinates(final DotInfoArray dotsArray) {

        if (dotsArray == null) {
            return;
        }
        DotInfo[] dots = dotsArray.getDots();
        if ((dots == null) || (dots.length == 0)) {
            return;
        }

        // check whether there is a row container
        if (m_rowContainer == null) {
            return;
        }
        // if there is a invalid column return
        checkColumns();
        if (m_invalidColumn) {
            return;
        }

        // the actual size we can draw in
        // the max dot size is subtracted as a dot can vary in size
        Rectangle drawingRectangle = calculateDrawingRectangle();
        int width = drawingRectangle.width;
        int height = drawingRectangle.height;
        int xOffset = drawingRectangle.x;
        int yOffset = drawingRectangle.y;

        assert dots.length <= m_rowContainer.size();

        // get the coordinates from the headers
        Coordinate xCoordinate = getColHeader().getCoordinate();
        Coordinate yCoordinate = getRowHeader().getCoordinate();

        // if one of the coordinates is missing returns
        if (xCoordinate == null || yCoordinate == null) {
            return;
        }

        // calculate the mapping for each domain value
        for (int i = 0; i < dots.length; i++) {

            // as the dots may have been sorted the loop index does not
            // neccessarily corresponds with the row ids any more
            // therefore the row id is retrieved from the dot info
            int rowId = dots[i].getRowIndex();

            DataRow row = m_rowContainer.getRow(rowId);
            DataCell xCell = row.getCell(m_xIndex);
            DataCell yCell = row.getCell(m_yIndex);

            if (!xCell.isMissing() && !yCell.isMissing()) {

                // temp variables for the coordinates
                int x = (int)(xCoordinate.calculateMappedValue(xCell, width,
                        true));
                // need to be transformed to lower left origin later on
                // (see below)
                int y = (int)(yCoordinate.calculateMappedValue(yCell, height,
                        true));

                // if one of the values is not a valid one set -1 for both
                if (x < 0 || y < 0) {
                    dots[i].setXCoord(-1);
                    dots[i].setYCoord(-1);
                } else {
                    // for coordinate origin down there in the left lower
                    // corner:
                    dots[i].setXCoord(xOffset + x);
                    dots[i].setYCoord(yOffset + height - y);
                }
            } else {
                // at least one coordinate is missing, set invalid screen coord
                dots[i].setXCoord(-1);
                dots[i].setYCoord(-1);
            }
        }

        // jitter the dots if there is one or two nominal coordinates
        // first sort the dots according to a nominal value (if both are
        // nominal)
        // it does not matter). the second sort criteria is the other coordinate
        // after sorting it is easy to jitter all points with the same nominal
        // value and the same value of the other coordinate

        if ((xCoordinate.isNominal() || yCoordinate.isNominal())) {

            // for jittering only 90% of the available space are used
            // to avoid that the dots of different nominal values touces each
            // other
            int xAxisJitterRange = (int)(Math.round(xCoordinate
                    .getUnusedDistBetweenTicks(width)) * 0.9);
            int yAxisJitterRange = (int)(Math.round(yCoordinate
                    .getUnusedDistBetweenTicks(height)) * 0.9);
            jitterDots(dots, xAxisJitterRange, yAxisJitterRange);
        }
    }

    /**
     * Jitters the dots according to the kind of axis and there values. The
     * function sorts the dots first for the nominal value and second according
     * the other coordinate (possibly also a nominal coordinate)
     * 
     * @param dots the dots to jitter
     * @param xAxisJitterRange the available range to jitter for the x axis
     * @param yAxisJitterRange the available range ot jitter for the y axis
     */
    private void jitterDots(final DotInfo[] dots, final int xAxisJitterRange,
            final int yAxisJitterRange) {

        DotInfoArray dotArray = new DotInfoArray(dots);

        // sort the array (first sort critera x then y)
        dotArray.sort(DotInfoArray.X_THEN_Y_SORT_POLICY);

        // now we loop for the two blocks induced by the two sorting criterias
        int xVal = -1;
        int nextXVal = -1;
        int i = 0;
        do {

            // set the next x value according to the first sort criteria
            xVal = dots[i].getXCoord();

            int yVal = -1;
            int nextYVal = -1;
            int j = 0;
            do {

                // get the y value to the corresponding x value
                yVal = dots[i].getYCoord();

                // jitter the dot
                int[] jitteredValues = jitterDot(xVal, yVal, xAxisJitterRange,
                        yAxisJitterRange, j);

                dots[i].setXCoord(jitteredValues[0]);
                dots[i].setYCoord(jitteredValues[1]);

                if (i + 1 < dots.length) {
                    nextYVal = dots[i + 1].getYCoord();
                    nextXVal = dots[i + 1].getXCoord();
                } else {
                    nextYVal = -1;
                    nextXVal = -1;
                }

                j++;
                i++;
            } while (i < dots.length - 1 && xVal == nextXVal
                    && yVal == nextYVal);

        } while (i < dots.length);
    }

    /**
     * Jitters the coordinate (given by coordinate1 and coordinate2) in the
     * allowed jitterrange according to the <code>jitterNumber</code>.
     * 
     * @param coordinate1 the first coordinate of the 2D dot
     * @param coordinate2 the second coordinate of the 2D dot
     * @param jitterRange1 the allowed jitter range for <code>coordinate1</code>
     * @param jitterRange2 the allowed jitter range for <code>coordinate2</code>
     * 
     * @return an int array of length 2 with the jittered values. at index 0 is
     *         the jittered value of coordinate1 and vise versa.
     */
    private int[] jitterDot(final int coordinate1, final int coordinate2,
            final int jitterRange1, final int jitterRange2,
            final int jitterNumber) {

        int localJitterRange1 = jitterRange1;
        // if the coordinate values are not valid
        // return -1 for both
        if (coordinate1 < 0 || coordinate2 < 0) {
            int[] result = {-1, -1};
            return result;
        }

        // if there is no jitter range available
        // nothing has to be done. just return the original values
        if (localJitterRange1 <= 0 && jitterRange2 <= 0) {
            int[] result = {coordinate1, coordinate2};
            return result;
        }

        int firstDimJitter = -1;
        int secondDimJitter = -1;
        // if the first jitter range is zero or smaller, it is set to 1
        // to keep the jitter number when divided by jitter range 1
        if (localJitterRange1 <= 0) {
            localJitterRange1 = 1;
            firstDimJitter = 0;
        } else {
            firstDimJitter = jitterNumber % localJitterRange1;
        }

        if (localJitterRange1 > 0 && jitterRange2 > 0) {

            secondDimJitter = ((int)Math.floor(jitterNumber
                    / (double)localJitterRange1))
                    % jitterRange2;
        } else {
            secondDimJitter = 0;
        }

        int coordinate1Diff = getCoordinateDiff(firstDimJitter);
        int coordinate2Diff = getCoordinateDiff(secondDimJitter);

        int[] result = new int[2];
        result[0] = coordinate1 + coordinate1Diff;
        result[1] = coordinate2 + coordinate2Diff;

        return result;
    }

    /**
     * Calculates the coordinate difference for a given one dim jitter number.
     * 
     * @param oneDimJitterNumber the one dim jitter number
     * @return the coordinate difference
     */
    private int getCoordinateDiff(final int oneDimJitterNumber) {
        int sideOfCenter = oneDimJitterNumber % 2;

        // determines if the difference is subtracted left or right
        // (top or down) from the coordinate
        int sideMultiplicator;

        if (sideOfCenter == 0) {
            sideMultiplicator = 1;
        } else {
            sideMultiplicator = -1;
        }

        // calculate the absolut value to change the coordinate
        // this is done dividing by 2 (two sides - right,left - top,down)
        int absoluteChange = (int)Math.ceil(oneDimJitterNumber / 2.0);

        return absoluteChange * sideMultiplicator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void fillPopupMenu(final JPopupMenu menu) {

        if (m_hiliteMenu == null) {
            m_hiliteMenu = getHiLiteMenu();
        }

        boolean dotsSelected;
        if ((getScatterPlotterDrawingPane().getSelectedSet() != null)
                && (getScatterPlotterDrawingPane()
                        .getSelectedSet().size() > 0)) {
            dotsSelected = true;
        } else {
            dotsSelected = false;
        }
        String selCnt = "Selected: "
                + getScatterPlotterDrawingPane().getSelectedSet().size();

        boolean clearPossible = m_hiliteHdlr.getHiLitKeys().size() > 0;

        String hilite = POPUP_HILITE_SELECTED;
        if (m_hiliteHdlr == null) {
            hilite = "<no hilit hdl>";
            dotsSelected = false;
        }
        String unhilite = POPUP_UNHILITE_SELECTED;
        if (m_hiliteHdlr == null) {
            unhilite = "<no hilit hdl>";
            dotsSelected = false;
        }
        String clearhilite = POPUP_CLEAR_HILITED;

        JMenuItem item;
        /* --- "Selected: xxx" --- */
        item = new JMenuItem(selCnt);
        item.setEnabled(false);
        menu.add(item);
        menu.addSeparator();
        /* --- "hilite selected" --- */
        item = new JMenuItem(hilite);
        item.setEnabled(dotsSelected);
        item.addActionListener(this);
        menu.add(item);
        /* --- "unhilite selected" --- */
        item = new JMenuItem(unhilite);
        item.setEnabled(dotsSelected && clearPossible);
        item.addActionListener(this);
        menu.add(item);
        /* --- "clear hilite" --- */
        item = new JMenuItem(clearhilite);
        item.setEnabled(clearPossible);
        item.addActionListener(this);
        menu.add(item);

        /* ------------------------------ */
        menu.addSeparator();
        ButtonGroup group = new ButtonGroup();
        /* --- "[ ] show unhilited" --- */
        item = new JRadioButtonMenuItem(POPUP_SHOW, 
                (!getFadeUnHiLited() && !isHideUnHiLited()));
        item.addActionListener(this);
        group.add(item);
        menu.add(item);
        /* --- "[ ] fade unhilited" --- */
        item = new JRadioButtonMenuItem(POPUP_FADE, getFadeUnHiLited());
        item.addActionListener(this);
        group.add(item);
        menu.add(item);
        /* --- "[ ] hide unhilited" --- */
        item = new JRadioButtonMenuItem(POPUP_HIDE, isHideUnHiLited());
        item.addActionListener(this);
        group.add(item);
        menu.add(item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseReleased(final MouseEvent e) {
        super.mouseReleased(e);
        boolean existSelected =
                getDrawingPane().getNumberSelectedElements() > 0;
        if (m_hilite != null) {
            m_hilite.setEnabled(existSelected);
        }
        boolean existHilited = getHiLiteHandler().getHiLitKeys().size() > 0;
        if (m_unhilite != null) {
            m_unhilite.setEnabled(existSelected && existHilited);
        }
    }

    /**
     * Creates a menue entry for Hiliting if not done so far.
     * 
     * @return a JMenu entry handling the hiliting of objects
     */
    @Override
    public JMenu getHiLiteMenu() {

        // if the menu was already created
        // if (m_hiliteMenu != null) {
        // return m_hiliteMenu;
        // }

        // else create it
        JMenu menu = new JMenu(HiLiteHandler.HILITE);
        menu.setMnemonic('H');

        boolean selected = getDrawingPane().getNumberSelectedElements() > 0;
        // create the entries. by default all entries are disabled first
        m_hilite = new JMenuItem(POPUP_HILITE_SELECTED);
        m_hilite.addActionListener(this);
        m_hilite.setEnabled(selected);
        m_hilite.setMnemonic('H');
        menu.add(m_hilite);
        m_unhilite = new JMenuItem(POPUP_UNHILITE_SELECTED);
        m_unhilite.addActionListener(this);
        m_unhilite.setEnabled(selected);
        m_unhilite.setMnemonic('U');
        menu.add(m_unhilite);
        m_clearHilite = new JMenuItem(POPUP_CLEAR_HILITED);
        m_clearHilite.addActionListener(this);
        m_clearHilite.setEnabled(true);
        m_clearHilite.setMnemonic('E');
        menu.add(m_clearHilite);
        /* ------------------------------ */
        menu.addSeparator();
        ButtonGroup group = new ButtonGroup();
        /* --- "[ ] show unhilited" --- */
        m_show = new JRadioButtonMenuItem(POPUP_SHOW, 
                (!getFadeUnHiLited() && !isHideUnHiLited()));
        m_show.addActionListener(this);
        group.add(m_show);
        menu.add(m_show);
        /* --- "[ ] fade unhilited" --- */
        m_fade = new JRadioButtonMenuItem(POPUP_FADE, getFadeUnHiLited());
        m_fade.addActionListener(this);
        group.add(m_fade);
        menu.add(m_fade);
        /* --- "[ ] hide unhilited" --- */
        m_hide = new JRadioButtonMenuItem(POPUP_HIDE, isHideUnHiLited());
        m_hide.addActionListener(this);
        group.add(m_hide);
        menu.add(m_hide);

        m_hiliteMenu = menu;
        return menu;
    }

    /**
     * Clears all hiliting.
     */
    private void clearHilite() {
        getScatterPlotterDrawingPane().clearHilite();
        setHideUnHiLited(false);
        setFadeUnHiLited(false);
        m_show.setSelected(true);
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(final ActionEvent e) {
        if (e.getActionCommand().equals(POPUP_HILITE_SELECTED)) {
            // hilite selected rows with the hilite manager
            hiliteSelected();
        } else if (e.getActionCommand().equals(POPUP_UNHILITE_SELECTED)) {
            unHiliteSelected();
        } else if (e.getActionCommand().equals(POPUP_FADE)) {
            toggleFadingUnHiLited();
        } else if (e.getActionCommand().equals(POPUP_HIDE)) {
            toggleHideUnHiLited();
        } else if (e.getActionCommand().equals(POPUP_CLEAR_HILITED)) {
            setHideUnHiLited(false);
            setFadeUnHiLited(false);
            m_show.setSelected(true);
            m_hiliteHdlr.fireClearHiLiteEvent();
        } else if (e.getActionCommand().equals(POPUP_SHOW)) {
            setHideUnHiLited(false);
            setFadeUnHiLited(false);
            m_show.setSelected(true);
        }
    }

    /**
     * Registers the currently selected dots as hilited with the hilite
     * manager. And repaints.
     */
    public void hiliteSelected() {
        if (m_hiliteHdlr != null) {
            m_hiliteHdlr.fireHiLiteEvent(
                    getScatterPlotterDrawingPane().getSelectedSet());
            repaint();
        }
    }

    /**
     * Registers the currently selected dots as not highlited with the hilite
     * manager. And repaitns
     */
    public void unHiliteSelected() {
        if (m_hiliteHdlr != null) {
            m_hiliteHdlr.fireUnHiLiteEvent(getScatterPlotterDrawingPane()
                    .getSelectedSet());
            repaint();
        }
    }

    /**
     * toggles the painting of unhilited dots and repaints.
     * 
     * @see #setFadeUnHiLited(boolean)
     */
    public void toggleFadingUnHiLited() {
        setFadeUnHiLited(!getFadeUnHiLited());
        if (getFadeUnHiLited()) {
            setHideUnHiLited(false);
        }
        repaint();
    }

    /**
     * Toggles the painting of (un)hilited dots.
     * 
     */
    public void toggleHideUnHiLited() {
        setHideUnHiLited(!isHideUnHiLited());
        if (isHideUnHiLited()) {
            setFadeUnHiLited(false);
        }
        repaint();
    }

    /**
     * {@inheritDoc}
     */
    public void hiLite(final KeyEvent event) {
        changeHiLiteTo(true, event.keys());
    }

    /**
     * {@inheritDoc}
     */
    public void unHiLite(final KeyEvent event) {
        changeHiLiteTo(false, event.keys());
    }


    /**
     * {@inheritDoc}
     */
    public void unHiLiteAll(final KeyEvent ke) {
        clearHilite();
    }

    /**
     * sets the hilite state of the rows in the set to the passed value. And
     * repaints. Okay, this is ugly. I've changed DotInfoArray not to contain a
     * hash map anymore - now we need to iterate through the entire array
     * everytime a hilite change event comes through.
     */
    private void changeHiLiteTo(final boolean state, final Set<RowKey> keys) {
        if (getScatterPlotterDrawingPane().getDotInfoArray() == null) {
            return;
        }
        DotInfo[] dots = getScatterPlotterDrawingPane().getDotInfoArray()
                .getDots();
        for (int i = 0; i < dots.length; i++) {
            if (keys.contains(dots[i].getRowID())) {
                dots[i].setHiLit(state);
            }
        }
        repaint();
    }

    /**
     * Sets a new hilite handler. It will cause a complete hilite refresh of all
     * dots known to the plot, if a new handler is passed. If null is set as
     * handler, all data is unhilited in the view. The function takes care of
     * registering/unregistering the class with the handlers.
     * 
     * @param hilitHdlr the new hilite handler.
     */
    public void setHiLiteHandler(final HiLiteHandler hilitHdlr) {
        if (m_hiliteHdlr != hilitHdlr) {
            // hilite handler changed
            if (m_hiliteHdlr != null) {
                // unregister with the old handler
                m_hiliteHdlr.removeHiLiteListener(this);
            }
            m_hiliteHdlr = hilitHdlr;
            if (m_hiliteHdlr != null) {
                // register with the new one
                m_hiliteHdlr.addHiLiteListener(this);
            }

            updateDotHiLiting();

        } // end of if (m_hiliteHdlr != newHilitHdlr)

    } // end of setHiLiteHandler()

    /**
     * Returns the HiLiteHandler for the datapoints.
     * 
     * @return - the HiLiteHanlder for the datapoints.
     */
    public HiLiteHandler getHiLiteHandler() {
        return m_hiliteHdlr;
    }

    private void updateDotHiLiting() {
        // change the hilite of existing dots to reflect new hiliter
        if (getScatterPlotterDrawingPane().getDotInfoArray() != null) {
            DotInfo[] dots = getScatterPlotterDrawingPane().getDotInfoArray()
                    .getDots();
            if (dots != null) {
                for (int i = dots.length - 1; i >= 0; i--) {
                    if (m_hiliteHdlr != null) {
                        dots[i].setHiLit(m_hiliteHdlr.isHiLit(dots[i]
                                .getRowID()));
                    } else {
                        dots[i].setHiLit(false);
                    }
                }
            }
        }
    }

    /**
     * Helper method to check if the so far set column names are valid. If not a
     * boolean flag is set for later use.
     */
    private void checkColumns() {

        // see if we got column names and if they are valid
        // if not set a boolean flag
        if (m_rowContainer == null) {
            // m_invalidColumn = true;
            return;
        }

        DataTableSpec tSpec = m_rowContainer.getDataTableSpec();
        int xIdx = tSpec.findColumnIndex(getXColName());
        int yIdx = tSpec.findColumnIndex(getYColName());

        if ((xIdx < 0)) {

            getColHeader().setToolTipText(null);
            m_invalidColumn = true;

        } else if (yIdx < 0) {

            getRowHeader().setToolTipText(null);
            m_invalidColumn = true;

        } else if (m_rowContainer.size() < 1) {

            m_invalidColumn = true;

        } else {
            m_invalidColumn = false;
        }
    }

    /**
     * Sets new x columns and recalculates/repaints.
     * 
     * @param xColName name of the new x column to plot
     */
    public void setXColumn(final String xColName) {
        setXColName(xColName);

        // tell the headers to display the new column names
        getColHeader().setToolTipText(getXColName());

        // check if the column names set so far are valid
        // this check also checks the y axis
        // if invalid a boolean flag is set in the method for later use
        checkColumns();

        if (m_rowContainer == null) {
            return;
        }

        DataTableSpec tSpec = m_rowContainer.getDataTableSpec();

        int idx = tSpec.findColumnIndex(getXColName());

        if (idx >= 0) {
            Coordinate xCoordinate = Coordinate.createCoordinate(tSpec
                    .getColumnSpec(idx));

            if (xCoordinate == null) {
                m_xIndex = -1;
            } else {

                // check whether the bounds are set properly
                if (!xCoordinate.isNominal()) {

                    if (!((NumericCoordinate)xCoordinate)
                            .isMinDomainValueSet()) {
                        ((NumericCoordinate)xCoordinate)
                                .setMinDomainValue(getXmin());
                    }

                    if (!((NumericCoordinate)xCoordinate)
                            .isMaxDomainValueSet()) {
                        ((NumericCoordinate)xCoordinate)
                                .setMaxDomainValue(getXmax());
                    }
                }

                getColHeader().setCoordinate(xCoordinate);

                m_xIndex = idx;
            }
        } else {
            // set -1 to indicate an invalid column index
            m_xIndex = -1;
        }

        // redo everything if two columns are available
        if (!m_invalidColumn) {
            updateDotsAndPaint();
        }
    }

    /**
     * Sets new y columns and recalculates/repaints.
     * 
     * @param yColName name of the new y column to plot
     */
    public void setYColumn(final String yColName) {

        setYColName(yColName);

        // tell the headers to display the new column names
        getRowHeader().setToolTipText(getYColName());

        // check if the column names set so far are valid
        // this check also checks the y axis
        // if invalid a boolean flag is set in the method for later use
        checkColumns();

        if (m_rowContainer == null) {
            return;
        }

        DataTableSpec tSpec = m_rowContainer.getDataTableSpec();

        // set the chosen column specs via a coordinate into the
        // axis headers
        int idx = tSpec.findColumnIndex(getYColName());

        if (idx >= 0) {
            Coordinate yCoordinate = Coordinate.createCoordinate(tSpec
                    .getColumnSpec(idx));

            if (yCoordinate == null) {
                m_yIndex = -1;
            } else {

                // check whether the bounds are set properly
                if (!yCoordinate.isNominal()) {

                    if (!((NumericCoordinate)yCoordinate)
                            .isMinDomainValueSet()) {
                        ((NumericCoordinate)yCoordinate)
                                .setMinDomainValue(getYmin());
                    }

                    if (!((NumericCoordinate)yCoordinate)
                            .isMaxDomainValueSet()) {
                        ((NumericCoordinate)yCoordinate)
                                .setMaxDomainValue(getYmax());
                    }
                }

                getRowHeader().setCoordinate(yCoordinate);
                m_yIndex = idx;
            }

        } else {
            // set -1 to indicate an invalid column index
            m_yIndex = -1;
        }

        // redo everything
        if (!m_invalidColumn) {
            updateDotsAndPaint();
        }
    }

    /**
     * Sets new boundaries for the two dimensions.
     * 
     * @param minX the lower boundary for the X axes - or null for no change.
     * @param maxX the upper boundary for the X axes - or null for no change.
     * @param minY the lower boundary for the Y axes - or null for no change.
     * @param maxY the upper boundary for the Y axes - or null for no change.
     */
    public void setRanges(final Double minX, final Double maxX,
            final Double minY, final Double maxY) {
        boolean change = false;
        if (minX != null) {
            change = true;
            m_userXmin = minX;
            ((NumericCoordinate)getColHeader().getCoordinate())
                    .setMinDomainValue(minX.doubleValue());
        }
        if (maxX != null) {
            change = true;
            m_userXmax = maxX;
            ((NumericCoordinate)getColHeader().getCoordinate())
                    .setMaxDomainValue(maxX.doubleValue());
        }
        if (minY != null) {
            change = true;
            m_userYmin = minY;
            ((NumericCoordinate)getRowHeader().getCoordinate())
                    .setMinDomainValue(minY.doubleValue());
        }
        if (maxY != null) {
            change = true;
            m_userYmax = maxY;
            ((NumericCoordinate)getRowHeader().getCoordinate())
                    .setMaxDomainValue(maxY.doubleValue());
        }

        if (change) {
            updatePaintModel();
            repaint();
        }

    }

    /**
     * removes user min/max values for the x axes.
     */
    public void removeUserXRangeNoReCalc() {
        m_userXmin = null;
        m_userXmax = null;
    }

    /**
     * removes user min/max values for the y axes.
     */
    public void removeUserYRangeNoReCalc() {
        m_userYmin = null;
        m_userYmax = null;
    }

    /**
     * Creates new rectangle that spans the area in which the plotter can draw.
     * This does not include the insets area and also allows a little bit of
     * extra sceen space in which large dots can be painted (half of the maximal
     * dot size is reserved left, right, top and bottom.)
     * 
     * @return A new rectangle spanning the drawing area.
     */
    protected Rectangle calculateDrawingRectangle() {
        // the actual size we can draw in
        // the max dot size is subtracted as a dot can vary in size
        int width = getPlotterWidth()
                - getScatterPlotterDrawingPane().getCurrentMaxDotSize();
        int height = getPlotterHeight()
                - getScatterPlotterDrawingPane().getCurrentMaxDotSize();

        // we only need an offset if we have borders
        int xOffset = getScatterPlotterDrawingPane().getCurrentHalfMaxDotSize();
        int yOffset = xOffset;
        if (getInsets() != null) {
            Insets paneInsets = getScatterPlotterDrawingPane().getInsets();
            width -= paneInsets.left + paneInsets.right;
            xOffset += paneInsets.left;
            height -= paneInsets.top + paneInsets.bottom;
            yOffset += paneInsets.top;
        }
        return new Rectangle(xOffset, yOffset, width, height);
    }

    /**
     * @return the lower limit of the X scale
     */
    public double getXmin() {
        if (getXColName() == null) {
            return 0.0;
        }
        DataArray rows = m_rowContainer;
        if ((rows == null) || (rows.size() == 0)) {
            return 0.0;
        }
        DataTableSpec tSpec = rows.getDataTableSpec();
        int idx = tSpec.findColumnIndex(getXColName());
        if (idx < 0) {
            return 0.0;
        }
        // 'getDoubleValue' returns the first valid double value
        return getDoubleValue(m_userXmin, tSpec.getColumnSpec(idx).getDomain()
                .getLowerBound(), rows.getMinValue(idx), Double.NaN);
    }

    /**
     * @return the upper limit of the X scale
     */
    public double getXmax() {
        if (getXColName() == null) {
            return 0.0;
        }
        DataArray rows = m_rowContainer;
        if ((rows == null) || (rows.size() == 0)) {
            return 0.0;
        }
        DataTableSpec tSpec = rows.getDataTableSpec();
        int idx = tSpec.findColumnIndex(getXColName());
        if (idx < 0) {
            return 0.0;
        }
        // 'getDoubleValue' returns the first valid double value
        return getDoubleValue(m_userXmax, tSpec.getColumnSpec(idx).getDomain()
                .getUpperBound(), rows.getMaxValue(idx), Double.NaN);
    }

    /**
     * @return the lower limit of the Y scale
     */
    public double getYmin() {
        if (getYColName() == null) {
            return 0.0;
        }
        DataArray rows = m_rowContainer;
        if ((rows == null) || (rows.size() == 0)) {
            return 0.0;
        }
        DataTableSpec tSpec = rows.getDataTableSpec();
        int idx = tSpec.findColumnIndex(getYColName());
        if (idx < 0) {
            return 0.0;
        }
        // 'getDoubleValue' returns the first valid double value
        return getDoubleValue(m_userYmin, tSpec.getColumnSpec(idx).getDomain()
                .getLowerBound(), rows.getMinValue(idx), Double.NaN);
    }

    /**
     * @return the upper limit of the Y scale
     */
    public double getYmax() {
        if (getYColName() == null) {
            return 0.0;
        }
        DataArray rows = m_rowContainer;
        if ((rows == null) || (rows.size() == 0)) {
            return 0.0;
        }
        DataTableSpec tSpec = rows.getDataTableSpec();
        int idx = tSpec.findColumnIndex(getYColName());
        if (idx < 0) {
            return 0.0;
        }
        // 'getDoubleValue' returns the first valid double value
        return getDoubleValue(m_userYmax, tSpec.getColumnSpec(idx).getDomain()
                .getUpperBound(), rows.getMaxValue(idx), Double.NaN);
    }

    /*
     * returns the first value of the specified ones that is not null.
     */
    private double getDoubleValue(final Double st, final DataCell nd,
            final DataCell rd, final double last) {
        if ((st != null) && (!st.isNaN()) && !st.isInfinite()) {
            return st.doubleValue();
        }
        if ((nd != null) && !nd.isMissing() && (nd instanceof DoubleValue)
                && !Double.isInfinite(((DoubleValue)nd).getDoubleValue())
                && !Double.isNaN(((DoubleValue)nd).getDoubleValue())) {
            return ((DoubleValue)nd).getDoubleValue();
        }
        if ((rd != null) && !rd.isMissing() && (rd instanceof DoubleValue)
                && !Double.isInfinite(((DoubleValue)rd).getDoubleValue())
                && !Double.isNaN(((DoubleValue)rd).getDoubleValue())) {
            return ((DoubleValue)rd).getDoubleValue();
        }
        return last;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updatePaintModel() {

        calculateCoordinates(getScatterPlotterDrawingPane().getDotInfoArray());
    }
}
