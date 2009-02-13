/*
 * -------------------------------------------------------------------
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
 *    26.02.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.histogram.datamodel;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.NodeLogger;

import org.knime.base.node.viz.aggregation.AggregationMethod;
import org.knime.base.node.viz.aggregation.AggregationValModel;
import org.knime.base.node.viz.aggregation.AggregationValSubModel;
import org.knime.base.node.viz.aggregation.HiliteShapeCalculator;
import org.knime.base.node.viz.aggregation.util.GUIUtils;
import org.knime.base.node.viz.aggregation.util.LabelDisplayPolicy;
import org.knime.base.node.viz.histogram.HistogramLayout;
import org.knime.base.node.viz.histogram.util.BinningUtil;
import org.knime.base.node.viz.histogram.util.ColorColumn;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * This is the basic visualization model for a histogram. It handles bin
 * creation and hilite handling and defines the constants which effect the
 * drawing like minimum space between bins and elements.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public abstract class AbstractHistogramVizModel {
    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(AbstractHistogramVizModel.class);

     /** The caption of the bar which holds all missing values. */
    public static final String MISSING_VAL_BAR_CAPTION = "Missing_values";

    /** Defines the minimum width of a bin. Should be more than the base line
     * stroke.*/
    public static final int MIN_BIN_WIDTH = 6;

    /**
     * The default number of bars which get created if the createBinnedBars
     * method is called with a number smaller then 1.
     */
    public static final int DEFAULT_NO_OF_BINS = 10;

    /**
     * Defines the number of decimal places which are used by default in the
     * binning method.
     */
    public static final int INTERVAL_DIGITS = 2;

    /**
     * The width of the hilite rectangle in percent of the surrounding
     * rectangle. Should be greater 0 and less than 1. 0.8 = 80%
     */
    public static final double HILITE_RECT_WIDTH_FACTOR = 0.5;
    /**The thickness of a bin which is used to show the different bins.*/
    public static final int BIN_SURROUNDING_SPACE = 4;

    /** This is the minimum space between two bins. */
    public static final int SPACE_BETWEEN_BINS = 2 * BIN_SURROUNDING_SPACE + 3;

    /**The space around a bar which is used to show the aggregation
     * column color.*/
    public static final int BAR_SURROUNDING_SPACE =
        Math.min(BIN_SURROUNDING_SPACE, 3);

    /**The space between to bars in pixel. Must be greater 0.*/
    public static final int SPACE_BETWEEN_BARS = 2 * BAR_SURROUNDING_SPACE + 3;

    /**
     * The space between to elements in the SIDE_BY_SIDE {@link HistogramLayout}
     * layout in  pixel. Must be greater 0.
     */
    public static final int SPACE_BETWEEN_ELEMENTS = 2;

    /**The minimum width of an bar/element.*/
    public static final int MINIMUM_ELEMENT_WIDTH = 6;

    /** The minimum height of a bar.*/
    public static final int MINIMUM_BAR_HEIGHT = 4;

    /**
    * The histogram hilite shape calculator.
    * @author Tobias Koetter, University of Konstanz
    */
   public class HistogramHiliteCalculator implements
            HiliteShapeCalculator<Rectangle2D, Rectangle2D> {

        /** Constructor for class HistogramHiliteCalculator. */
        protected HistogramHiliteCalculator() {
            // nothing todo
        }

        /**
         * @return the {@link AggregationMethod}
         */
        public AggregationMethod getAggrMethod() {
            return AbstractHistogramVizModel.this.getAggregationMethod();
        }

        /**
         * @return the {@link HistogramLayout}
         */
        public HistogramLayout getLayout() {
            return AbstractHistogramVizModel.this.getHistogramLayout();
        }

        /**
         * {@inheritDoc}
         */
        public Rectangle2D calculateHiliteShape(final AggregationValModel
                <AggregationValSubModel<Rectangle2D, Rectangle2D>,
                Rectangle2D, Rectangle2D> model) {
            if (!supportsHiliting()) {
                return null;
            }
            final Rectangle2D barRectangle = model.getShape();
            if (model.isPresentable() || barRectangle == null) {
                return null;
            }
            final int noOfHilitedRows = model.getHiliteRowCount();
            if (noOfHilitedRows <= 0) {
                return null;
            }
            final int barY = (int)barRectangle.getY();
            final int barHeight = (int)barRectangle.getHeight();
            final int barWidth = (int)barRectangle.getWidth();
            final int rowCount = model.getRowCount();
            final double fraction = noOfHilitedRows / (double)rowCount;
            int hiliteHeight = (int)(barHeight * fraction);
            final int hiliteWidth = Math.max((int)(barWidth
                    * AbstractHistogramVizModel.HILITE_RECT_WIDTH_FACTOR), 1);
            final int hiliteX =
                    (int)(barRectangle.getX() + (barWidth - hiliteWidth) / 2);
            int hiliteY = barY;
            if (model.getAggregationValue(
                            getAggregationMethod()) > 0) {
                hiliteY = hiliteY + barHeight - hiliteHeight;
            }
            // check for possible rounding errors
            if (hiliteHeight > barHeight) {
                hiliteHeight = barHeight;
                LOGGER.warn("Hilite rectangle higher than surrounding bar");
            }
            if (hiliteY < barY) {
                hiliteY = barY;
                LOGGER.warn("Hilite rectangle y coordinate above "
                        + "surrounding bar y coordinate");
            }
            final Rectangle hiliteRect =
                    new Rectangle(hiliteX, hiliteY, hiliteWidth, hiliteHeight);
            return hiliteRect;
        }

        /**
         * {@inheritDoc}
         */
        public Rectangle2D calculateHiliteShape(
                final AggregationValSubModel<Rectangle2D, Rectangle2D> model) {
            if (!supportsHiliting()) {
                return null;
            }
            final int noOfHilitedKeys = model.getHiliteRowCount();
            final Rectangle2D elementRect = model.getShape();
            if (noOfHilitedKeys < 1 || elementRect == null) {
                // if their are no rows hilited we have no hilite rectangle
                return null;
            }
            final int totalWidth = (int)elementRect.getWidth();
            final int hiliteWidth = Math.max((int)(totalWidth
                    * AbstractHistogramVizModel.HILITE_RECT_WIDTH_FACTOR), 1);
            final int totalHeight = (int)elementRect.getHeight();
            final double heightPerRow =
                    (double)totalHeight / model.getRowCount();
            final int hiliteHeight =
                    Math.max((int)(heightPerRow * noOfHilitedKeys), 1);
            final int xCoord =
                    (int)elementRect.getX() + (totalWidth / 2)
                            - (hiliteWidth / 2);
            final int startY = (int)elementRect.getY();
            final double aggrVal = model.getAggregationValue(
                    getAggregationMethod());
            int yCoord = 0;
            if (aggrVal >= 0) {
                // if it's a positive value we draw the hilite rectangle from
                // bottom to top of the bar
                yCoord = startY + (totalHeight - hiliteHeight);
            } else {
                // if it's a negative value we draw the hilite rectangle from
                //top to bottom of the bar
                yCoord = startY;
            }
            final Rectangle hiliteRect =
                    new Rectangle(xCoord, yCoord, hiliteWidth, hiliteHeight);
            return hiliteRect;
        }
    }

    private final List<Color> m_rowColors;

    private boolean m_binNominal = false;

    private AggregationMethod m_aggrMethod;

    private HistogramLayout m_layout;

    private boolean m_showMissingValBin = true;

    /**If set to true the plotter paints the grid lines for the y axis values.*/
    private boolean m_showGridLines = true;

    /**If set to true the plotter paints the bin outline.*/
    private boolean m_showBinOutline = true;

    /**If set to true the plotter paints the bar outline.*/
    private boolean m_showBarOutline = false;

    /**If set to true the plotter paints the outline of the bars. The outline
     * is always painted for highlighted blocks!.*/
    private boolean m_showElementOutlines = false;

    /**If set to <code>true</code> the bar labels are displayed vertical
     * otherwise they are displayed horizontal.*/
    private boolean m_showLabelVertical = true;

    /**The label display policy defines for which bars the labels should be
     * displayed.*/
    private LabelDisplayPolicy m_labelDisplayPolicy =
        LabelDisplayPolicy.getDefaultOption();

    /** The current basic width of the bins. */
    private int m_binWidth = Integer.MAX_VALUE;

    private int m_maxBinWidth;

    /**
     *The plotter will show all bars empty or not when set to <code>true</code>.
     */
    private boolean m_showEmptyBins = false;

    private BinDataModel m_missingValueBin;

    private final List<BinDataModel> m_bins = new ArrayList<BinDataModel>(50);

    private int m_noOfBins = 1;

    private int m_maxNoOfBins;

    /**Access this field only via the getter method to ensure that the
     * aggregation and layout information are actual.
    */
    private final HistogramHiliteCalculator m_calculator =
        new HistogramHiliteCalculator();

    /**Holds the actual size of the drawing space.*/
    private Dimension m_drawingSpace;


    /**Constructor for class HistogramVizModel.
     * @param rowColors all possible colors the user has defined for a row
     * @param layout the {@link HistogramLayout} to use
     * @param aggrMethod the {@link AggregationMethod} to use
     * @param noOfBins the no of bins to create
     */
    public AbstractHistogramVizModel(final List<Color> rowColors,
            final AggregationMethod aggrMethod, final HistogramLayout layout,
            final int noOfBins) {
        if (rowColors == null) {
            throw new IllegalArgumentException(
                    "Bar elements must not be null");
        }
        m_rowColors = new ArrayList<Color>(rowColors);
        m_aggrMethod = aggrMethod;
        m_layout = layout;
        m_noOfBins = noOfBins;
    }


    /**
     * @return the drawingSpace
     */
    public Dimension getDrawingSpace() {
        return m_drawingSpace;
    }


    /**
     * @param drawingSpace the drawingSpace to set
     * @return <code>true</code> if the parameter has changed
     */
    public boolean setDrawingSpace(final Dimension drawingSpace) {
        if (drawingSpace == null) {
            throw new IllegalArgumentException(
                    "Drawing space must not be null");
        }
        if (drawingSpace.equals(m_drawingSpace)) {
            return false;
        }
        m_drawingSpace = drawingSpace;
        return true;
    }

    /**
     * @return all {@link BinDataModel} objects of this
     * histogram including the missing value bin if the showMissingValue
     * bin variable is set to
     * <code>true</code>
     */
    public List<BinDataModel> getBins() {
        if (containsMissingValueBin()) {
            final BinDataModel missingValueBin = getMissingValueBin();
            if (isShowMissingValBin()) {
                final int missingValBinIdx = m_bins.size() - 1;
                if (missingValBinIdx < 0
                        || m_bins.get(missingValBinIdx) != missingValueBin) {
                    m_bins.add(missingValueBin);
                }
            } else {
                //check the last bin if it's the missing value bin if thats the
                //case remove it from the list
                final int missingValBinIdx = m_bins.size() - 1;
                if (missingValBinIdx >= 0
                        && m_bins.get(missingValBinIdx) == missingValueBin) {
                    //if the list contains the missing value bin remove it
                    m_bins.remove(missingValBinIdx);
                }
            }
        }
        return m_bins;
    }

    /**
     * @return all bins which are selected
     */
    public List<BinDataModel> getSelectedBins() {
        final List<BinDataModel> bins = getBins();
        final List<BinDataModel> selectedBins =
            new ArrayList<BinDataModel>(bins.size());
        for (final BinDataModel model : bins) {
            if (model.isSelected()) {
                selectedBins.add(model);
            }
        }
        return selectedBins;
    }

    /**
     * @return the aggregation columns. Could be null!
     */
    public abstract Collection<ColorColumn> getAggrColumns();

    /**
     * @return the x column specification
     */
    public abstract DataColumnSpec getXColumnSpec();

    /**
     * @return the x column name
     */
    public abstract String getXColumnName();

    /**
     * @return all available element colors. This is the color the user has
     * set for one attribute in the Color Manager node.
     */
    public List<Color> getRowColors() {
        return Collections.unmodifiableList(m_rowColors);
    }

    /**
     * @return the number of different elements which depends on the
     * coloration of the input data
     */
    public int getNoOfElements() {
        return m_rowColors.size();
    }

    /**
     * @return the noOfBins without the missing value bin but including the
     * empty bins displayed or not.
     */
    public int getNoOfBins() {
        return m_noOfBins;
    }

    /**
     * @return the maximum number of bins which fit into the
     * current drawing space
     */
    public int getMaxNoOfBins() {
        calculateMaxNoOfBins();
        return m_maxNoOfBins;
    }

    /**
     * @return the binWidth
     */
    public int getBinWidth() {
        calculateMaxBinWidth();
        checkBinWidth();
        return m_binWidth;
    }

    /**
     * Calculates the current preferred width of the bars.
     */
    private void checkBinWidth() {
        if (m_drawingSpace == null || (m_drawingSpace.getHeight() <= 0
                && m_drawingSpace.getWidth() <= 0)) {
            return;
        }
        int binWidth = m_binWidth;
        if (binWidth < 0) {
            // that only occurs at the first call
            //we have to use the getBinCaptions method which checks if the
            //missing value bin should be included or not and if empty
            //bins should be displayed
            final int noOfBins = getDisplayedNoOfBins();
            binWidth = (int)(m_drawingSpace.getWidth() / noOfBins)
                    - SPACE_BETWEEN_BINS;
        }
        if (binWidth < MIN_BIN_WIDTH) {
            binWidth = MIN_BIN_WIDTH;
        }
        final int maxBinWidth = getMaxBinWidth();
        if (binWidth > maxBinWidth) {
            // to avoid to wide bars after resizing the window!
            binWidth = maxBinWidth;
        }
        //draw at least a small line
        if (binWidth <= 0) {
            binWidth = 1;
        }
        m_binWidth = binWidth;
    }

    /**
     * Calculates the maximum width per bar for the current display settings.
     */
    private void calculateMaxBinWidth() {
        if (m_drawingSpace == null || (m_drawingSpace.getHeight() <= 0
                && m_drawingSpace.getWidth() <= 0)) {
            return;
        }
        //we have to use the getBinCaptions method which checks if the missing
        //value bin should be included or not and if empty bins should be
        //displayed
        final int noOfBins = getDisplayedNoOfBins();
        //the minimum bin width should be at least 1 pixel
        m_maxBinWidth = Math.max((int)(m_drawingSpace.getWidth() / noOfBins)
                - AbstractHistogramVizModel.SPACE_BETWEEN_BINS, 1);
    }

    /**
     * @return the maximum bin width
     */
    public int getMaxBinWidth() {
        calculateMaxBinWidth();
        return m_maxBinWidth;
    }

    /**
     * @param binWidth the binWidth to set
     * @return <code>true</code> if the with has changed
     */
    public boolean setBinWidth(final int binWidth) {
        if (m_binWidth == binWidth) {
            return false;
        }
        if (binWidth < 0) {
            m_binWidth = 0;
        } else {
            m_binWidth = binWidth;
        }
        return true;
    }

    /**
     * @param noOfBins the new number of bins to create
     * @return <code>true</code> if the number of bins has changed
     */
    public boolean setNoOfBins(final int noOfBins) {
        final int noOf = Math.min(noOfBins, getMaxNoOfBins());
        if (m_noOfBins == noOf) {
            return false;
        }
        m_noOfBins = noOf;
        return true;
    }

    /**
     * @param noOfBins updates the number of bins but doesn't check if the
     * number has changed and thus doesn't recreate the bins if the
     * number has changed.
     */
    protected void updateNoOfBins(final int noOfBins) {
        m_noOfBins = Math.min(noOfBins, getMaxNoOfBins());
    }

    /**
     * Calculates the maximum number of bins which could be displayed.
     */
    private void calculateMaxNoOfBins() {
        //handle nominal binning special
        if (isBinNominal()) {
            final DataColumnSpec xColSpec = getXColumnSpec();
            final DataColumnDomain domain = xColSpec.getDomain();
            if (domain == null) {
                throw new IllegalStateException(
                        "Binning column domain must not be null");
            }
            final Set<DataCell> values = domain.getValues();
            if (values == null) {
                throw new IllegalStateException(
                        "Values of binning column domain must not be null");
            }
            m_maxNoOfBins = values.size();
            return;
        }
        if (m_drawingSpace == null || (m_drawingSpace.getHeight() <= 0
                && m_drawingSpace.getWidth() <= 0)) {
            //if no drawing space is defined we set the maximum number
            //of bins to the current number of bins
            m_maxNoOfBins = m_bins.size();
            return;
        }
        int maxNoOfBins = (int)(m_drawingSpace.getWidth()
                / (AbstractHistogramVizModel.MIN_BIN_WIDTH
                        + AbstractHistogramVizModel.SPACE_BETWEEN_BINS));
        if (isShowMissingValBin() && containsMissingValueBin()) {
            maxNoOfBins--;
        }
        //handle integer values special
        final DataColumnSpec xColSpec = getXColumnSpec();
        maxNoOfBins =
            BinningUtil.calculateIntegerMaxNoOfBins(maxNoOfBins, xColSpec);
        // avoid rounding errors and display at least one bar
        if (maxNoOfBins < 1) {
            maxNoOfBins = 1;
        }
        m_maxNoOfBins = maxNoOfBins;
    }


    /**
     * @return the number of bins which are displayed.
     */
    public int getDisplayedNoOfBins() {
        return getBinCaptions().size();
    }

    /**
     * @param caption the caption of the bin of interest
     * @return the bin with the given caption or <code>null</code> if no bin
     * with the given caption exists
     */
    public BinDataModel getBin(final String caption) {
        for (final BinDataModel bin : getBins()) {
            if (bin.getXAxisCaption().equals(caption)) {
                return bin;
            }
        }
        return null;
    }

    /**
     * @param idx the index of the bin
     * @return the {@link BinDataModel} at the given index
     */
    public BinDataModel getBin(final int idx) {
        return m_bins.get(idx);
    }

    /**
     * @return all bin captions in the order they should be displayed
     */
    public Set<DataCell> getBinCaptions() {
        final Collection<BinDataModel> bins = getBins();
        final Set<DataCell> captions =
            new LinkedHashSet<DataCell>(bins.size());
        for (final BinDataModel bin : bins) {
            if (m_showEmptyBins || bin.getMaxBarRowCount() > 0) {
                captions.add(new StringCell(bin.getXAxisCaption()));
            }
        }
        if (m_showMissingValBin && m_missingValueBin.getMaxBarRowCount() > 0) {
            captions.add(new StringCell(m_missingValueBin.getXAxisCaption()));
        }
        return captions;
    }

    /**
     * @return <code>true</code> if the bins are nominal or
     * <code>false</code> if the bins are intervals
     */
    public boolean isBinNominal() {
        return m_binNominal;
    }

    /**
     * @return <code>true</code> if the bins support hiliting otherwise
     * <code>false</code>
     */
    public abstract boolean supportsHiliting();

    /**
     * @return the maximum aggregation value
     */
    public double getMaxAggregationValue() {
        double maxAggrValue = Double.MIN_VALUE;
        for (final BinDataModel bin : getBins()) {
            final double value = bin.getMaxAggregationValue(m_aggrMethod,
                    m_layout);
            if (value > maxAggrValue) {
                maxAggrValue = value;
            }
        }
        return maxAggrValue;
    }

    /**
     * @return the minimum aggregation value
     */
    public double getMinAggregationValue() {
        double minAggrValue = Double.MAX_VALUE;
        for (final BinDataModel bin : getBins()) {
            final double value = bin.getMinAggregationValue(m_aggrMethod,
                    m_layout);
            if (value < minAggrValue) {
                minAggrValue = value;
            }
        }
        return minAggrValue;
    }

    /**
     * @return the missingValueBin
     */
    protected BinDataModel getMissingValueBin() {
        return m_missingValueBin;
    }

    /**
     * @return <code>true</code> if this model contains a missing value bin
     */
    public boolean containsMissingValueBin() {
        return (m_missingValueBin != null
                && m_missingValueBin.getMaxBarRowCount() > 0);
    }

    /**
     * @return <code>true</code> if the histogram contains at least one
     * bin with no rows in it.
     */
    public boolean containsEmptyBins() {
        for (final BinDataModel bin : getBins()) {
            if (bin.getBinRowCount() < 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return <code>true</code> if the empty bins should be displayed
     */
    public boolean isShowEmptyBins() {
        return m_showEmptyBins;
    }

    /**
     * @param showEmptyBins set to <code>true</code> if also the empty bins
     * should be shown
     * @return <code>true</code> if the variable has changed
     */
    public boolean setShowEmptyBins(final boolean showEmptyBins) {
        if (showEmptyBins != m_showEmptyBins) {
            m_showEmptyBins = showEmptyBins;
            return true;
        }
        return false;
    }

    /**
     * @return the aggregation method which is used to calculate the
     * aggregation value
     */
    public AggregationMethod getAggregationMethod() {
        return m_aggrMethod;
    }

    /**
     * @param aggrMethod the aggregation method to use to calculate
     * the aggregation value
     * @return <code>true</code> if the aggregation method has changed
     */
    public boolean setAggregationMethod(final AggregationMethod aggrMethod) {
        if (m_aggrMethod.equals(aggrMethod)) {
            return false;
        }
        if (!AggregationMethod.COUNT.equals(aggrMethod)
                && (getAggrColumns() == null || getAggrColumns().size() < 1)) {
            throw new IllegalArgumentException("Aggregation method only "
                    + "valid with a selected aggregation column");
        }
        m_aggrMethod = aggrMethod;
        return true;
    }

    /**
     * @return the layout
     */
    public HistogramLayout getHistogramLayout() {
        return m_layout;
    }

    /**
     * @param layout the layout to set
     * @return <code>true</code> if the layout has changed
     */
    public boolean setHistogramLayout(final HistogramLayout layout) {
        if (layout != null && !m_layout.equals(layout)) {
            if (HistogramLayout.SIDE_BY_SIDE.equals(layout)) {
                setShowBinOutline(true);
            } else if (getAggrColumns() == null
                    || getAggrColumns().size() < 2) {
                setShowBinOutline(false);
            }
            m_layout = layout;
            return true;
        }
        return false;
    }

    /**
     * @return the inclMissingValBin
     */
    public boolean isShowMissingValBin() {
        return m_showMissingValBin;
    }

    /**
     * @param inclMissingValBin the inclMissingValBin to set
     * @return <code>true</code> if the parameter has changed
     */
    public boolean setShowMissingValBin(final boolean inclMissingValBin) {
        if (m_showMissingValBin == inclMissingValBin) {
            return false;
        }
        m_showMissingValBin = inclMissingValBin;
        return true;
    }

    /**
     * @return the showGridLines
     */
    public boolean isShowGridLines() {
        return m_showGridLines;
    }


    /**
     * @param showGridLines the showGridLines to set
     * @return <code>true</code> if the parameter has changed
     */
    public boolean setShowGridLines(final boolean showGridLines) {
        if (m_showGridLines != showGridLines) {
            m_showGridLines = showGridLines;
            return true;
        }
        return false;
    }


    /**
     * @return the showBinOutline
     */
    public boolean isShowBinOutline() {
        return m_showBinOutline;
    }


    /**
     * @param showBinOutline the showBinOutline to set
     * @return <code>true</code> if the parameter has changed
     */
    public boolean setShowBinOutline(final boolean showBinOutline) {
        if (m_showBinOutline != showBinOutline) {
            m_showBinOutline = showBinOutline;
            return true;
        }
        return false;
    }

    /**
     * @return the showBarOutline
     */
    public boolean isShowBarOutline() {
        return m_showBarOutline;
    }


    /**
     * @param showBarOutline the showBarOutline to set
     * @return <code>true</code> if the parameter has changed
     */
    public boolean setShowBarOutline(final boolean showBarOutline) {
        if (m_showBarOutline != showBarOutline) {
            m_showBarOutline = showBarOutline;
            return true;
        }
        return false;
    }

    /**
     * @param showElementOutline the showElementOutlines to set
     * @return <code>true</code> if the parameter has changed
     */
    public boolean setShowElementOutline(final boolean showElementOutline) {
        if (m_showElementOutlines != showElementOutline) {
            m_showElementOutlines = showElementOutline;
            return true;
        }
        return false;
    }
    /**
     * @return <code>true</code> if the bar outline should be also shown for
     * none highlighted blocks
     */
    public boolean isShowElementOutline() {
        return m_showElementOutlines;
    }

    /**
     * @return the showLabelVertical
     */
    public boolean isShowLabelVertical() {
        return m_showLabelVertical;
    }

    /**
     * @param showLabelVertical if <code>true</code> the bar labels are
     * displayed vertical otherwise horizontal.
     * @return <code>true</code> if the parameter has changed
     */
    public boolean setShowLabelVertical(final boolean showLabelVertical) {
        if (m_showLabelVertical  != showLabelVertical) {
            m_showLabelVertical = showLabelVertical;
            return true;
        }
        return false;
    }

    /**
     * @return the labelDisplayPolicy
     */
    public LabelDisplayPolicy getLabelDisplayPolicy() {
        return m_labelDisplayPolicy;
    }

    /**
     * @param labelDisplayPolicy the display policy
     * @return <code>true</code> if the parameter has changed
     */
    public boolean setLabelDisplayPolicy(
            final LabelDisplayPolicy labelDisplayPolicy) {
        if (m_labelDisplayPolicy != labelDisplayPolicy) {
            m_labelDisplayPolicy = labelDisplayPolicy;
            return true;
        }
        return false;
    }

    /**
     * @return all keys of hilited rows
     */
    public abstract Set<RowKey> getHilitedKeys();

    /**
     * @return all keys of the selected elements
     */
    public abstract Set<RowKey> getSelectedKeys();

    /**
     * @param p the point to select
     * @return the {@link BinDataModel} that contains the point or
     * <code>null</code>
     */
    public BarDataModel getSelectedElement(final Point p) {
        if (p == null) {
            return null;
        }
        for (final BinDataModel bin : getBins()) {
            final Rectangle2D rect = bin.getBinRectangle();
            if (rect != null && rect.contains(p)) {
                final Collection<BarDataModel> bars = bin.getBars();
                for (final BarDataModel bar : bars) {
                    final Rectangle2D shape = bar.getShape();
                    if (shape != null && shape.contains(p)) {
                        return bar;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Selects the element which contains the given point.
     * @param point the point on the screen to select
     */
    public void selectElement(final Point point) {
        for (final BinDataModel bin : getBins()) {
            bin.selectElement(point);
        }
        return;
    }

    /**
     * Selects all elements which are touched by the given rectangle.
     * @param rect the rectangle on the screen select
     */
    public void selectElement(final Rectangle rect) {
        for (final BinDataModel bin : getBins()) {
            bin.selectElement(rect);
        }
        return;
    }

    /**
     * Clears all selections.
     */
    public void clearSelection() {
        for (final BinDataModel bin : getBins()) {
            bin.setSelected(false);
        }
    }

    /**
     * This method un/hilites all rows with the given key.
     * @param hilited the rowKeys of the rows to un/hilite
     * @param hilite if the given keys should be hilited <code>true</code>
     * or unhilited <code>false</code>
     */
    public abstract void updateHiliteInfo(final Set<RowKey> hilited,
            final boolean hilite);

    /**
     * Unhilites all rows.
     */
    public abstract void unHiliteAll();

    /**
     * @return a HTML <code>String</code> which contains details information
     * about the current selected elements
     */
    public String getHTMLDetailData() {
        final List<BinDataModel> selectedBins = getSelectedBins();
        if (selectedBins == null || selectedBins.size() < 1) {
            return (GUIUtils.NO_ELEMENT_SELECTED_TEXT);
        }
        final double[] vals = new double[3];
        Arrays.fill(vals, 0);
        final StringBuilder buf = new StringBuilder();
        buf.append("<table border='1'>");
        for (final BinDataModel bin : selectedBins) {
            buf.append("<tr>");
            buf.append("<td title='");
            buf.append(bin.getXAxisCaption());
            buf.append("'>");
            buf.append(bin.getXAxisCaption());
            buf.append("</td>");
            buf.append("<td>");
            final List<BarDataModel> selectedBars =
                bin.getSelectedBars();
            if (selectedBars == null || selectedBars.size() < 1) {
                buf.append("No bars selected");
            } else {
                buf.append(GUIUtils.createHTMLDetailData(selectedBars, vals));
            }
            buf.append("</td>");
            buf.append("</tr>");
        }
        if (selectedBins.size() > 1) {
            buf.append("<tr>");
            buf.append("<td title='");
            buf.append("Total");
            buf.append("'>");
            buf.append("Total");
            buf.append("</td>");
            buf.append("<td>");
            buf.append(GUIUtils.createHTMLTotalData(vals));
            buf.append("</td>");
            buf.append("</tr>");
            buf.append("</table>");
        }
        return buf.toString();
    }

    /**
     * @param nominal set to <code>true</code> if the nominal binning method
     * should be used.
     */
    protected void setBinNominal(final boolean nominal) {
        m_binNominal = nominal;
    }

    /**
     * @param bins the bins to display
     * @param missingValueBin the missing value bin
     */
    protected void setBins(final List<? extends BinDataModel> bins,
            final BinDataModel missingValueBin) {
        if (bins == null) {
            throw new NullPointerException("Bins must not be null");
        }
        if (missingValueBin == null) {
            throw new NullPointerException("MissingValueBin must not be null");
        }
        m_missingValueBin = missingValueBin;
        m_bins.clear();
        m_bins.addAll(bins);
        updateNoOfBins(m_bins.size());
    }


    /**
     * @return <code>true</code> if at least one bin is not presentable since
     * the number of aggregation columns doesn't fit into the bin
     */
    public boolean containsNotPresentableBin() {
        for (final BinDataModel bin : getBins()) {
            if (!bin.isPresentable()) {
                return true;
            }
        }
        return false;
    }


    /**
     * Calculates a surrounding rectangle with the given thickness
     * for the given rectangle.
     * @param rect the rectangle to draw the surrounding for
     * @param baseLine the base line to know if the bar is negative or
     * positive
     * @param thickness the  thickness of the surrounding rectangle
     * @return the surrounding rectangle
     */
    public static Rectangle2D calculateSurroundingRectangle(
            final Rectangle2D rect,
            final int baseLine, final int thickness) {
        if (rect == null) {
            return null;
        }
        final int x = (int)rect.getX();
        final int y = (int)rect.getY();
        final int height = (int)rect.getHeight();
        final int width = (int)rect.getWidth();
        final int horizontalFactor;
        if (y < baseLine && y + height > baseLine) {
            //the rectangle is above and below the base line so we have to
            //add the thickness on top and end of the rectangle
            horizontalFactor = 2;
        } else {
            horizontalFactor = 1;
        }
        //calculate the new y coordinate and height
        final int newHeight =
            Math.max(height + thickness * horizontalFactor, 1);
        int newY = y;
        if (y < baseLine) {
            //it's a positive bar so we have to subtract the difference
            newY -= (newHeight - height) / horizontalFactor;
        }

        //calculate the new x coordinate and width
        final int newWidth = Math.max(width + 2 * thickness, 2);
        final int newX = (int)((x + width / 2.0) - newWidth / 2.0);
        return new Rectangle(newX, newY, newWidth, newHeight);
    }

    /**
     * @return the hilite shape calculator
     */
    public HistogramHiliteCalculator getHiliteCalculator() {
        return m_calculator;
    }
}
