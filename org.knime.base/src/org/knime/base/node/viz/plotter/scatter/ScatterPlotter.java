/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 * History
 *   11.09.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.scatter;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.util.DataArray;
import org.knime.base.node.viz.plotter.AbstractDrawingPane;
import org.knime.base.node.viz.plotter.AbstractPlotter;
import org.knime.base.node.viz.plotter.AbstractPlotterProperties;
import org.knime.base.node.viz.plotter.columns.TwoColumnPlotter;
import org.knime.base.util.coordinate.Coordinate;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.node.property.hilite.KeyEvent;

/**
 * Maps the two selected columns to the screen coordinates. The resulting points
 * in 2-dimensional space are represented by
 * {@link org.knime.base.node.viz.plotter.scatter.DotInfo}s, which are stored
 * in a {@link org.knime.base.node.viz.plotter.scatter.DotInfoArray} which is
 * then passed to the
 * {@link org.knime.base.node.viz.plotter.scatter.ScatterPlotterDrawingPane}.
 * For nominal values it is typically that many points are on the same
 * coordinate which leads to overplotting. One possible solution is to jitter
 * the points around the original point, to visualize that many points are on
 * the same coordinate. The jittering for nominal values is done here.
 *
 * @author Fabian Dill, University of Konstanz
 */
public class ScatterPlotter extends TwoColumnPlotter {

    // private static final NodeLogger LOGGER = NodeLogger.getLogger(
    // ScatterPlotter.class);

    private int m_dotSize = ScatterPlotterProperties.DEFAULT_DOT_SIZE;

    private int m_jitterRate = 1;

    /**
     * Flag whether to show or hide the unhilited dots.
     */
    private boolean m_hide;

    /**
     * Construction kit constructor to construct a customized plotter,
     * registeres all necessary listeners.
     *
     * @param panel the drawing pane
     * @param properties the properties
     */
    public ScatterPlotter(final AbstractDrawingPane panel,
            final AbstractPlotterProperties properties) {
        super(panel, properties);
        setDotSize(ScatterPlotterProperties.DEFAULT_DOT_SIZE);
        if (isScatterPlotterProperties()) {
            /* --------- properties ------ */
            getScatterPlotterProperties().addDotSizeChangeListener(
                    new ChangeListener() {
                        /**
                         * {@inheritDoc}
                         */
                        public void stateChanged(final ChangeEvent e) {
                            setDotSize(getScatterPlotterProperties()
                                    .getDotSize());
                            updateSize();
                            getDrawingPane().repaint();
                        }

                    });
            getScatterPlotterProperties().getJitterSlider().setValue(
                    m_jitterRate * 10);
            getScatterPlotterProperties().getJitterSlider().addMouseListener(
                    new MouseAdapter() {

                        /**
                         * {@inheritDoc}
                         */
                        @Override
                        public void mouseReleased(final MouseEvent e) {
                            int jitter =
                                    getScatterPlotterProperties()
                                            .getJitterSlider().getValue();
                            m_jitterRate = jitter / 10;
                            updateSize();
                            getDrawingPane().repaint();
                        }

                    });
        }
        if (isScatterPlotterDrawingPane() && isScatterPlotterProperties()) {
            getScatterPlotterDrawingPane().setDotSize(
                    getScatterPlotterProperties().getDotSize());
        }
    }

    /**
     * Default constructor with
     * {@link org.knime.base.node.viz.plotter.scatter.ScatterPlotterDrawingPane}
     * and
     * {@link org.knime.base.node.viz.plotter.scatter.ScatterPlotterProperties}.
     *
     */
    public ScatterPlotter() {
        this(new ScatterPlotterDrawingPane(), new ScatterPlotterProperties());
    }

    /**
     * Passes an empty
     * {@link org.knime.base.node.viz.plotter.scatter.DotInfoArray} to the
     * {@link org.knime.base.node.viz.plotter.scatter
     * .ScatterPlotterDrawingPane}.
     *
     * @see org.knime.base.node.viz.plotter.AbstractPlotter#reset()
     */
    @Override
    public void reset() {
        super.reset();
        getScatterPlotterDrawingPane().setDotInfoArray(
                new DotInfoArray(new DotInfo[0]));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        getScatterPlotterDrawingPane().setDotInfoArray(
                new DotInfoArray(new DotInfo[0]));
        super.dispose();
    }

    /**
     * Sets the size of the dots and sets a tick offset to both axes to ensure
     * that the points are always completely visible.
     *
     * @param dotSize the dot size.
     */
    public void setDotSize(final int dotSize) {
        m_dotSize = dotSize;
        getXAxis().setStartTickOffset(m_dotSize);
        getXAxis().repaint();
        getYAxis().setStartTickOffset(m_dotSize);
        getYAxis().repaint();
        if (isScatterPlotterDrawingPane()) {
            getScatterPlotterDrawingPane().setDotSize(m_dotSize);
        }
    }

    /**
     *
     * @return the dot size.
     */
    public int getDotSize() {
        return m_dotSize;
    }

    /**
     *
     * @param jitterRate the jitter rate.
     */
    protected void setJitterRate(final int jitterRate) {
        m_jitterRate = jitterRate;
    }

    /**
     *
     * @return the jitter rate (its 1/10 iof the value displayed in the slider).
     */
    protected int getJitterRate() {
        return m_jitterRate;
    }

    /**
     * The <code>ScatterPlotter</code> adds the posssibility to show, fade or
     * hide unhilited dots and this is the show all action. The hide flag is
     * administered in the <code>ScatterPlotter</code>, since hidden points
     * are not passed to the
     * {@link org.knime.base.node.viz.plotter.scatter
     * .ScatterPlotterDrawingPane},
     * the fade flag is administered in the
     * {@link org.knime.base.node.viz.plotter.scatter
     * .ScatterPlotterDrawingPane},
     * since the dots are painted but with a different (faded) color.
     *
     * @return the menu item for show all.
     */
    public Action getShowAllAction() {
        Action show = new AbstractAction(AbstractPlotter.SHOW_ALL) {
            /**
             * {@inheritDoc}
             */
            public void actionPerformed(final ActionEvent e) {
                m_hide = false;
                if (getDrawingPane() instanceof ScatterPlotterDrawingPane) {
                    getScatterPlotterDrawingPane().setFadeUnhilited(false);
                }
                updatePaintModel();
            }
        };
        return show;
    }

    /**
     * The <code>ScatterPlotter</code> adds the posssibility to show, fade or
     * hide unhilited dots and this is the hide unhilited action. The hide flag
     * is administered in the <code>ScatterPlotter</code>, since hidden
     * points are not passed to the
     * {@link org.knime.base.node.viz.plotter.scatter
     * .ScatterPlotterDrawingPane},
     * the fade flag is administered in the
     * {@link org.knime.base.node.viz.plotter.scatter
     * .ScatterPlotterDrawingPane},
     * since the dots are painted but with a different (faded) color.
     *
     * @return the menu item for hide unhilited.
     */
    public Action getHideAction() {
        Action hide = new AbstractAction(AbstractPlotter.HIDE_UNHILITED) {
            /**
             * {@inheritDoc}
             */
            public void actionPerformed(final ActionEvent e) {
                m_hide = true;
                if (getDrawingPane() instanceof ScatterPlotterDrawingPane) {
                    getScatterPlotterDrawingPane().setFadeUnhilited(false);
                }
                updatePaintModel();
            }
        };
        return hide;
    }

    /**
     *
     * @return true if only hilited dots should be painted.
     */
    protected boolean isHideMode() {
        return m_hide;
    }

    /**
     *
     * @param hide flag whether to hide unhilited dots.
     */
    public void setHideMode(final boolean hide) {
        m_hide = hide;
    }

    /**
     * The <code>ScatterPlotter</code> adds the posssibility to show, fade or
     * hide unhilited dots and this is the fade unhilited action. The hide flag
     * is administered in the <code>ScatterPlotter</code>, since hidden
     * points are not passed to the
     * {@link org.knime.base.node.viz.plotter.scatter
     * .ScatterPlotterDrawingPane},
     * the fade flag is administered in the
     * {@link org.knime.base.node.viz.plotter.scatter
     * .ScatterPlotterDrawingPane},
     * since the dots are painted but with a different (faded) color.
     *
     * @return the menu item for fade unhilited.
     */
    public Action getFadeAction() {
        Action fade = new AbstractAction(AbstractPlotter.FADE_UNHILITED) {
            /**
             * {@inheritDoc}
             */
            public void actionPerformed(final ActionEvent e) {
                if (getDrawingPane() instanceof ScatterPlotterDrawingPane) {
                    getScatterPlotterDrawingPane().setFadeUnhilited(true);
                }
                m_hide = false;
                updatePaintModel();
            }
        };
        return fade;
    }

    /**
     * The <code>ScatterPlotter</code> adds the posssibility to show, fade or
     * hide unhilited dots and this is the complete menu to hide, fade. The hide
     * flag is administered in the <code>ScatterPlotter</code>, since hidden
     * points are not passed to the
     * {@link org.knime.base.node.viz.plotter.scatter
     * .ScatterPlotterDrawingPane},
     * the fade flag is administered in the
     * {@link org.knime.base.node.viz.plotter.scatter
     * .ScatterPlotterDrawingPane},
     * since the dots are painted but with a different (faded) color.
     *
     * @return an additional menu for the NodeView's menu bar containing the
     *         actions for show, fade and hide unhilited dots.
     */
    public JMenu getShowHideMenu() {
        JMenu menu = new JMenu(SHOW_HIDE);
        menu.add(getShowAllAction());
        menu.add(getHideAction());
        menu.add(getFadeAction());
        return menu;
    }

    /**
     * The inherited hilite menu from the
     * {@link org.knime.base.node.viz.plotter.AbstractPlotter} and the show all,
     * fade or hide unhilited menu.
     *
     * {@inheritDoc}
     */
    @Override
    public void fillPopupMenu(final JPopupMenu popupMenu) {
        super.fillPopupMenu(popupMenu);
        popupMenu.addSeparator();
        popupMenu.add(getShowAllAction());
        popupMenu.add(getHideAction());
        popupMenu.add(getFadeAction());

    }

    /**
     * The data points of the data to visulaize are mapped to screen
     * coordinates, represented by
     * {@link org.knime.base.node.viz.plotter.scatter.DotInfo} and are passed in
     * a {@link org.knime.base.node.viz.plotter.scatter.DotInfoArray} to the
     * {@link org.knime.base.node.viz.plotter.scatter
     * .ScatterPlotterDrawingPane}. Repaint of the drawing pane is triggered.
     * Jittering is also triggered from here.
     *
     *
     * @see org.knime.base.node.viz.plotter.columns.TwoColumnPlotter
     *      #updatePaintModel()
     */
    @Override
    public void updatePaintModel() {
        if (getDataProvider() == null
                || getDataProvider().getDataArray(getDataArrayIdx()) == null) {
            return;
        }
        if (getSelectedXColumnIndex() == -1
                || getSelectedYColumnIndex() == -1) {
            return;
        }
        // check if the selected column indices are available
        int xIdx = getSelectedXColumnIndex();
        int yIdx = getSelectedYColumnIndex();
        int numCols =
                getDataProvider().getDataArray(getDataArrayIdx())
                    .getDataTableSpec().getNumColumns();
        if (xIdx >= numCols || yIdx >= numCols) {
            return;
        }
        // getScatterPlotterDrawingPane().clearSelection();
        // get the rowInfo from the model
        DataArray rowsCont = getDataProvider().getDataArray(getDataArrayIdx());
        if (rowsCont != null) {
            // LOGGER.debug("row container != null");
            // and create a new DotInfo array with the rowKeys in the DotInfos.
            List<DotInfo> dotList = new ArrayList<DotInfo>();
            int rowNr = 0;
            for (DataRow row : rowsCont) {
                double size = rowsCont.getDataTableSpec().getRowSizeFactor(row);
                ColorAttr colorAttr =
                        rowsCont.getDataTableSpec().getRowColor(row);
                boolean isHilite = delegateIsHiLit(row.getKey());
                if ((isHilite && m_hide) || !m_hide) {
                    if (m_hide) {
                        isHilite = false;
                    }
                    DotInfo dot =
                            new DotInfo(0, 0, row.getKey(), isHilite,
                                    colorAttr, size, rowNr);
                    dot.setShape(rowsCont.getDataTableSpec().getRowShape(row));
                    DataCell xDomain = row.getCell(getSelectedXColumnIndex());
                    dot.setXDomainValue(xDomain);
                    DataCell yDomain = row.getCell(getSelectedYColumnIndex());
                    dot.setYDomainValue(yDomain);
                    dotList.add(dot);
                }
                rowNr++;
            }
            DotInfo[] dotArray = new DotInfo[dotList.size()];
            dotList.toArray(dotArray);
            // now create a new DotInfoArray
            DotInfoArray newDotArray = new DotInfoArray(dotArray);
            // store it in the drawing pane
            if (isScatterPlotterDrawingPane()) {
                getScatterPlotterDrawingPane().setDotInfoArray(newDotArray);
            }
            // and get the coordinates calculated.
            calculateCoordinates(newDotArray);
        }
        getDrawingPane().repaint();
    }

    /**
     * Given the actual size of the drawing pane, the actual zoom factor, and
     * min/max values it calculates the screen coordinates for each dot info in
     * the array passed in. It accesses the model to retrieve the actual values
     * of the rows. It changes the contents of the DotInfos passed in. It also
     * triggers resorting of the sorted lists in the dot container.
     *
     * @param dotsArray the array containing the dots.
     */
    protected void calculateCoordinates(final DotInfoArray dotsArray) {
        if (!isScatterPlotterDrawingPane()) {
            // doesn't make sense to calculate the coordinates
            // TODO: maybe return the calculated dot info array???
            return;
        }
        if (dotsArray == null) {
            return;
        }
        DotInfo[] dots = dotsArray.getDots();
        if ((dots == null) || (dots.length == 0)) {
            return;
        }

        // check whether there is a row container
        if (getDataProvider().getDataArray(getDataArrayIdx()) == null) {
            return;
        }
        // get the coordinates from the headers
        Coordinate xCoordinate = getXAxis().getCoordinate();
        Coordinate yCoordinate = getYAxis().getCoordinate();

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

            DataRow row = getDataProvider().getDataArray(getDataArrayIdx())
                .getRow(rowId);
            DataCell xCell = row.getCell(getSelectedXColumnIndex());
            DataCell yCell = row.getCell(getSelectedYColumnIndex());

            if (!xCell.isMissing() && !yCell.isMissing()) {

                // temp variables for the coordinates
                int x = getMappedXValue(xCell);

                // need to be transformed to lower left origin later on
                // (see below)
                int y = getMappedYValue(yCell);
                // if one of the values is not a valid one set -1 for both
                if (x < 0 || y < 0) {
                    dots[i].setXCoord(-1);
                    dots[i].setYCoord(-1);
                } else {
                    // here was the offset used
                    dots[i].setXCoord(x);
                    dots[i].setYCoord(y);
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
            if (getProperties() instanceof ScatterPlotterProperties) {
                getScatterPlotterProperties().getJitterSlider().setEnabled(
                        true);
            }
            // the max dot size is subtracted as a dot can vary in size
            int width = getDrawingPaneDimension().width - (2 * m_dotSize);
            int height = getDrawingPaneDimension().height - (2 * m_dotSize);
            // for jittering only 90% of the available space are used
            // to avoid that the dots of different nominal values touces each
            // other
            int xAxisJitterRange =
                    (int)(Math.round(xCoordinate
                            .getUnusedDistBetweenTicks(width)) * 0.9);
            int yAxisJitterRange =
                    (int)(Math.round(yCoordinate
                            .getUnusedDistBetweenTicks(height)) * 0.9);
            jitterDots(dots, xAxisJitterRange, yAxisJitterRange);
        } else {
            // bugfix 1253
            if (getProperties() instanceof ScatterPlotterProperties) {
                getScatterPlotterProperties().getJitterSlider().setEnabled(
                        false);
            }
        }
        getScatterPlotterDrawingPane().setDotInfoArray(new DotInfoArray(dots));
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
    protected void jitterDots(final DotInfo[] dots, final int xAxisJitterRange,
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
                int[] jitteredValues =
                        jitterDot(xVal, yVal, xAxisJitterRange,
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
                j += m_jitterRate;
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

            secondDimJitter =
                    ((int)Math.floor(jitterNumber / (double)localJitterRange1))
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
        int sideOfCenter = 0;
        if (m_jitterRate != 0) {
            sideOfCenter = (oneDimJitterNumber / m_jitterRate) % 2;
        }

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

    private ScatterPlotterDrawingPane getScatterPlotterDrawingPane() {
        return (ScatterPlotterDrawingPane)getDrawingPane();
    }

    /**
     *
     * @return true if the drawing pane is a scatterplotter drawing pane.
     */
    protected boolean isScatterPlotterDrawingPane() {
        return getDrawingPane() instanceof ScatterPlotterDrawingPane;
    }

    private ScatterPlotterProperties getScatterPlotterProperties() {
        return (ScatterPlotterProperties)getProperties();
    }

    /**
     *
     * @return true if the properties are scatter plotter properties.
     */
    protected boolean isScatterPlotterProperties() {
        return getProperties() instanceof ScatterPlotterProperties;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateSize() {
        super.updateSize();
        if (isScatterPlotterDrawingPane()) {
            calculateCoordinates(getScatterPlotterDrawingPane()
                    .getDotInfoArray());
            getDrawingPane().repaint();
        }
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected int getMappedXValue(final DataCell x) {
        // the max dot size is subtracted as a dot can vary in size
        int width = getDrawingPaneDimension().width - (2 * m_dotSize);
        int mappedX = (int)getXAxis().getCoordinate().calculateMappedValue(
                x, width);
        // translate the x position to the right to center it at its pos
        mappedX += m_dotSize;
        return mappedX;
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected int getMappedYValue(final DataCell y) {
        // the max dot size is subtracted as a dot can vary in size
        int height = getDrawingPaneDimension().height - (2 * m_dotSize);
        int mappedY = (int)getYAxis().getCoordinate().calculateMappedValue(
                y, height);
        // for coordinate origin down there in the left lower
        // corner:        
        mappedY = height - mappedY;
        // move it up, since we have dotsize offset at the bottom
        mappedY += m_dotSize;
        return mappedY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void selectElementsIn(final Rectangle selectionRectangle) {
        if (isScatterPlotterDrawingPane()) {
            getScatterPlotterDrawingPane().selectElementsIn(
                    selectionRectangle.x, selectionRectangle.y,
                    selectionRectangle.x + selectionRectangle.width,
                    selectionRectangle.y + selectionRectangle.height);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void selectClickedElement(final Point p) {
        if (isScatterPlotterDrawingPane()) {
            getScatterPlotterDrawingPane().selectElementsIn(
                    p.x - (m_dotSize / 2), p.y - (m_dotSize / 2),
                    p.x + (m_dotSize / 2), p.y + (m_dotSize / 2));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearSelection() {
        if (isScatterPlotterDrawingPane()) {
            getScatterPlotterDrawingPane().clearSelection();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void hiLiteSelected() {
        if (isScatterPlotterDrawingPane()) {
            changeHiliteStateTo(getScatterPlotterDrawingPane()
                    .getSelectedDots(), true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unHiLiteSelected() {
        if (isScatterPlotterDrawingPane()) {
            changeHiliteStateTo(getScatterPlotterDrawingPane()
                    .getSelectedDots(), false);
        }
    }

    private void changeHiliteStateTo(final Set<RowKey> rowIds,
            final boolean state) {
        if (state) {
            delegateHiLite(rowIds);
        } else {
            delegateUnHiLite(rowIds);
        }
        if (isScatterPlotterDrawingPane()) {
            DotInfoArray dots =
                    getScatterPlotterDrawingPane().getDotInfoArray();
            if (dots == null) {
                return;
            }
            for (DotInfo dot : dots.getDots()) {
                if (rowIds.contains(dot.getRowID())) {
                    dot.setHiLit(state);
                }
            }
            updatePaintModel();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void unHiLiteAll(final KeyEvent event) {
        if (isScatterPlotterDrawingPane()) {
            DotInfoArray dotArray =
                    getScatterPlotterDrawingPane().getDotInfoArray();
            if (dotArray == null) {
                return;
            }
            for (DotInfo dot : dotArray.getDots()) {
                dot.setHiLit(false);
            }
            updatePaintModel();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void hiLite(final KeyEvent event) {
        changeHiliteStateTo(event.keys(), true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unHiLite(final KeyEvent event) {
        changeHiliteStateTo(event.keys(), false);
    }
}
