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
 *   18.08.2006 (Tobias Koetter): created
 */
package org.knime.base.node.viz.histogram.impl;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collection;
import java.util.Set;
import java.util.SortedSet;

import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.viz.aggregation.AggregationMethod;
import org.knime.base.node.viz.histogram.HistogramLayout;
import org.knime.base.node.viz.histogram.datamodel.AbstractHistogramVizModel;
import org.knime.base.node.viz.histogram.datamodel.BinDataModel;
import org.knime.base.node.viz.histogram.datamodel.AbstractHistogramVizModel.HistogramHiliteCalculator;
import org.knime.base.node.viz.histogram.util.ColorColumn;
import org.knime.base.node.viz.histogram.util.NoDomainColumnFilter;
import org.knime.base.node.viz.plotter.AbstractPlotter;
import org.knime.base.node.viz.plotter.Axis;
import org.knime.base.util.coordinate.Coordinate;
import org.knime.base.util.coordinate.CoordinateMapping;
import org.knime.base.util.coordinate.NumericCoordinate;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.KeyEvent;
import org.knime.core.node.util.ColumnFilter;
import org.knime.core.node.util.CombinedColumnFilter;
import org.knime.core.node.util.DataValueColumnFilter;

/**
 * Abstract class which is the coordinator between the
 * {@link HistogramDrawingPane} and the {@link AbstractHistogramVizModel}.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public abstract class AbstractHistogramPlotter extends AbstractPlotter {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(AbstractHistogramPlotter.class);
    /**This name is used for the y axis if the aggregation method is count.*/
    public static final String COL_NAME_COUNT = "Count";
    /**Number of digits used in an interval.*/
    public static final int INTERVAL_DIGITS = 2;

    /**This column filter should be used in all x column select boxes.*/
    public static final ColumnFilter X_COLUMN_FILTER =
        NoDomainColumnFilter.getInstance();

    /**This column filter should be used in all aggregation column
     * select boxes.*/
    @SuppressWarnings("unchecked")
    public static final ColumnFilter AGGREGATION_COLUMN_FILTER =
        new CombinedColumnFilter(new DataValueColumnFilter(DoubleValue.class),
            NoDomainColumnFilter.getInstance());

    /** The <code>DataTableSpec</code> of the input data. */
    private DataTableSpec m_tableSpec;

    /**
     * The <code>HistogramVizModel</code> which holds the basic
     * information.
     */
    private AbstractHistogramVizModel m_vizModel;

    private final AbstractHistogramProperties m_histoProps;

    /**If the user changes the layout to side-by-side we automatically
     * set the bin width to maximum. If he goes back to stacked layout
     * show the bars with the original bar width. Thats what this variable
     * stores.*/
    private int m_lastStackedBinWidth = -1;

    /**Constructor for class AbstractHistogramPlotter.
     * @param histogramProps the histogram properties panel
     * @param handler the HiLiteHandler to use
     */
    public AbstractHistogramPlotter(
            final AbstractHistogramProperties histogramProps,
            final HiLiteHandler handler) {
        super(new HistogramDrawingPane(histogramProps), histogramProps);
        m_histoProps = histogramProps;
//      add the visualization listener
        registerPropertiesChangeListener();
//      set the default value
//        final HistogramDrawingPane drawingPane = getHistogramDrawingPane();
//        drawingPane.setShowElementOutline(m_histoProps.isShowBarOutline());
//        drawingPane.setShowLabelVertical(m_histoProps.isShowLabelVertical());
//        setHistogramDataModel(dataModel);
        // set the hilitehandler for highlighting stuff
        if (handler != null) {
            super.setHiLiteHandler(handler);
        }
//        else {
//            throw new IllegalArgumentException("HiLiteHandler not defined.");
//        }
    }

    /**
     * Registers all histogram properties listener to the histogram
     * properties panel.
     */
    private void registerPropertiesChangeListener() {
        if (m_histoProps == null) {
            throw new IllegalStateException(
                    "Properties panel must not be null");
        }
        m_histoProps.addShowGridChangedListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                setShowGridLines(
                        e.getStateChange() == ItemEvent.SELECTED);
            }
        });
        m_histoProps.addShowBinOutlineChangedListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                final AbstractHistogramVizModel vizModel =
                    getHistogramVizModel();
                if (vizModel != null) {
                    vizModel.setShowBinOutline(
                            e.getStateChange() == ItemEvent.SELECTED);
                    final HistogramDrawingPane histoDrawingPane =
                        getHistogramDrawingPane();
                    histoDrawingPane.repaint();
                }
            }
        });
        m_histoProps.addShowBarOutlineChangedListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                final AbstractHistogramVizModel vizModel =
                    getHistogramVizModel();
                if (vizModel != null) {
                    vizModel.setShowBarOutline(
                            e.getStateChange() == ItemEvent.SELECTED);
                    final HistogramDrawingPane histoDrawingPane =
                        getHistogramDrawingPane();
                    histoDrawingPane.repaint();
                }
            }
        });
        m_histoProps.addShowElementOutlineChangedListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                final AbstractHistogramVizModel vizModel =
                    getHistogramVizModel();
                if (vizModel != null) {
                    vizModel.setShowElementOutline(
                            e.getStateChange() == ItemEvent.SELECTED);
                    final HistogramDrawingPane histoDrawingPane =
                        getHistogramDrawingPane();
                    histoDrawingPane.repaint();
                }
            }
        });
        m_histoProps.addLabelOrientationListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final AbstractHistogramVizModel vizModel =
                    getHistogramVizModel();
                if (vizModel != null) {
                    final AbstractHistogramProperties histoProps =
                        getHistogramPropertiesPanel();
                    if (histoProps != null) {
                        vizModel.setShowLabelVertical(
                                histoProps.isShowLabelVertical());
                        final HistogramDrawingPane histoDrawingPane =
                            getHistogramDrawingPane();
                        histoDrawingPane.repaint();
                    }
                }
            }
        });
        m_histoProps.addLabelDisplayListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final AbstractHistogramVizModel vizModel =
                    getHistogramVizModel();
                if (vizModel != null) {
                    final AbstractHistogramProperties histoProps =
                        getHistogramPropertiesPanel();
                    if (histoProps != null) {
                        vizModel.setLabelDisplayPolicy(
                                histoProps.getLabelDisplayPolicy());
                        final HistogramDrawingPane histoDrawingPane =
                            getHistogramDrawingPane();
                        histoDrawingPane.repaint();
                    }
                }
            }
        });
        m_histoProps.addLayoutListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final AbstractHistogramProperties histoProps =
                    getHistogramPropertiesPanel();
                if (histoProps != null) {
                    setHistogramLayout(histoProps.getHistogramLayout());
                }
            }
        });
        m_histoProps.addBinWidthChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                final JSlider source = (JSlider)e.getSource();
                final int binWidth = source.getValue();
                updateBinWidth(binWidth);
            }
        });
        m_histoProps.addNoOfBinsChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                final JSlider source = (JSlider)e.getSource();
                //react only if the slider is in is final position
                if (!source.getValueIsAdjusting()) {
                    final int noOfBins = source.getValue();
                    if (setNumberOfBins(noOfBins)) {
                        updatePaintModel();
                    }
                }
            }
        });
        m_histoProps.addAggrMethodListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final String methodName = e.getActionCommand();
                if (!AggregationMethod.valid(methodName)) {
                    throw new IllegalArgumentException(
                            "No valid aggregation method");
                }
                final AggregationMethod aggrMethod =
                    AggregationMethod.getMethod4Command(methodName);
                if (setAggregationMethod(aggrMethod)) {
                    updatePaintModel();
                }
            }
        });
        m_histoProps.addShowEmptyBinListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                if (setShowEmptyBins(
                        e.getStateChange() == ItemEvent.SELECTED)) {
                    updatePaintModel();
                }
            }
        });
        m_histoProps.addShowMissingValBinListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                if (setShowMissingValBin(
                        e.getStateChange() == ItemEvent.SELECTED)) {
                    updatePaintModel();
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateSize() {
        final AbstractHistogramVizModel vizModel = getHistogramVizModel();
        if (vizModel == null) {
            LOGGER.debug("VizModel was null");
            return;
        }
        final Dimension newDrawingSpace = getDrawingPaneDimension();
        if (!vizModel.setDrawingSpace(newDrawingSpace)) {
            return;
        }
        if (HistogramLayout.SIDE_BY_SIDE.equals(
                vizModel.getHistogramLayout())
                && vizModel.containsNotPresentableBin()) {
            //set the bin with to the maximum bin if the layout
            //is side-by-side or the bin is not presentable with
            //the current width
            vizModel.setBinWidth(vizModel.getMaxBinWidth());
        }
        updatePaintModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updatePaintModel() {
        final AbstractHistogramVizModel vizModel = getHistogramVizModel();
        if (vizModel == null) {
            LOGGER.debug("VizModel was null");
            return;
        }
        final Coordinate xCoordinates = getXCoordinate();
        final Coordinate yCoordinates = getAggregationCoordinate();
        final HistogramDrawingPane drawingPane = getHistogramDrawingPane();
        final Dimension drawingSpace = vizModel.getDrawingSpace();
        setHistogramBinRectangle(vizModel, xCoordinates, yCoordinates);
        final double drawingHeight = drawingSpace.getHeight();
        if (!yCoordinates.isNominal()
                && ((NumericCoordinate)yCoordinates).getMinDomainValue()
                    < 0) {
            final int baseLine = (int)(drawingHeight
                    - yCoordinates.calculateMappedValue(
                            new DoubleCell(0), drawingHeight));
            drawingPane.setBaseLine(new Integer(baseLine));
        } else {
            drawingPane.setBaseLine(null);
        }
        if (vizModel.isShowGridLines()) {
            final int[] gridLines =
                getGridLineCoordinates(yCoordinates, drawingHeight);
            drawingPane.setGridLines(gridLines);
        } else {
            drawingPane.setGridLines(null);
        }
        //update the properties panel as well since something could have changed
        drawingPane.setHistogramVizModel(vizModel, true);
    }

    /**
     * Updates ONLY the width of the bins.
     * @param binWidth the new bin width
     */
    protected void updateBinWidth(final int binWidth) {
        final AbstractHistogramVizModel vizModel = getHistogramVizModel();
        if (vizModel == null) {
            LOGGER.debug("VizModel was null");
            return;
        }
        if (!vizModel.setBinWidth(binWidth)) {
            return;
        }
        final Dimension drawingSpace = vizModel.getDrawingSpace();
        if (drawingSpace == null) {
            throw new IllegalStateException("Drawing space must not be null");
        }
        final double drawingWidth = drawingSpace.getWidth();
        final double drawingHeight = drawingSpace.getHeight();
        final Coordinate xCoordinates = getXCoordinate();
        final Coordinate aggrCoordinate = getAggregationCoordinate();
        final int baseLine =
            (int)(drawingHeight - aggrCoordinate.calculateMappedValue(
                            new DoubleCell(0), drawingHeight));
        final HistogramDrawingPane drawingPane = getHistogramDrawingPane();

        final int newBinWidth = vizModel.getBinWidth();
        final SortedSet<Color> barElementColors =
            vizModel.getRowColors();
        final HistogramHiliteCalculator calculator =
                vizModel.getHiliteCalculator();
        final Collection<ColorColumn> aggrColumns =
            vizModel.getAggrColumns();
        for (final BinDataModel bin : vizModel.getBins()) {
            final DataCell captionCell = bin.getXAxisCaptionCell();
            final double labelCoord = xCoordinates.calculateMappedValue(
                    captionCell, drawingWidth);
            //subtract half of the bar width from the start position to place
            //the middle point of the bar on the mapped coordinate position
            final int xCoord = (int)(labelCoord - (newBinWidth / 2));
            bin.updateBinWidth(xCoord, newBinWidth, barElementColors,
                    aggrColumns, baseLine, calculator);
        }
        //if only the bar width changes we don't need to update the properties
        //since the bar width change is triggered by the property component
        //itself
        drawingPane.setHistogramVizModel(vizModel, false);
    }

    /**
     * @return the {@link Coordinate} of the x axis.
     */
    private Coordinate getXCoordinate() {
        final Axis xAxis = getXAxis();
        if (xAxis == null) {
            throw new IllegalStateException("X axis must not be null");
        }
        final Coordinate xCoordinate = xAxis.getCoordinate();
        if (xCoordinate == null) {
            throw new IllegalStateException("X coordinate must not be null");
        }
        return xCoordinate;
    }

    /**
     * @return the {@link Coordinate} of the aggregation axis.
     */
    private Coordinate getAggregationCoordinate() {
        final Axis aggrAxis = getYAxis();
        if (aggrAxis == null) {
            throw new IllegalStateException(
                    "Aggregation axis must not be null");
        }
        final Coordinate aggrCoordinate = aggrAxis.getCoordinate();
        if (aggrCoordinate == null) {
            throw new IllegalStateException(
                    "Aggregation coordinate must not be null");
        }
        return aggrCoordinate;
    }

    /**
     * Calculates and sets the drawing rectangle of each bin.
     * @param xCoordinates The <code>Coordinate</code> object which contains
     * the start position of an bar on the x axis
     * @param yCoordinates The <code>Coordinate</code> object which contains
     * the start position of an bar on the y axis
     * @param drawingSpace A <code>Rectangle</code> which defines the available
     * drawing space
     */
    private static void setHistogramBinRectangle(
            final AbstractHistogramVizModel vizModel,
            final Coordinate xCoordinates, final Coordinate yCoordinates) {
        final Dimension drawingSpace = vizModel.getDrawingSpace();
        final int binWidth = vizModel.getBinWidth();
        final AggregationMethod aggrMethod = vizModel.getAggregationMethod();
        final SortedSet<Color> barElementColors =
            vizModel.getRowColors();
        final Collection<ColorColumn> aggrColumns =
            vizModel.getAggrColumns();
        final HistogramLayout layout = vizModel.getHistogramLayout();
        final HistogramHiliteCalculator calculator =
                vizModel.getHiliteCalculator();
        final double drawingWidth = drawingSpace.getWidth();
        final double drawingHeight = drawingSpace.getHeight();
        final int baseLine =
            (int)(drawingHeight - yCoordinates.calculateMappedValue(
                            new DoubleCell(0), drawingHeight));
        // this is the minimum size of a bar with an aggregation value > 0
        final int minHeight = Math.max(
                (int)HistogramDrawingPane.getBarStrokeWidth(),
                AbstractHistogramVizModel.MINIMUM_BAR_HEIGHT);
        // get the default width for all bins
//        final int binWidth = getBinWidth();
        for (final BinDataModel bin : vizModel.getBins()) {
            final DataCell captionCell = bin.getXAxisCaptionCell();
            final double labelCoord = xCoordinates.calculateMappedValue(
                    captionCell, drawingWidth);
            if (labelCoord < 0) {
                //this bin is not on the x axis (because it is empty and the
                //empty bins shouldn't be displayed) so we simply set the
                //rectangle to null and continue
                bin.setBinRectangle(null, baseLine, barElementColors,
                        aggrColumns, calculator);
                continue;
            }

            //if the maximum value is negative use 0 to end at the base line
            final double maxAggrVal = Math.max(
                    bin.getMaxAggregationValue(aggrMethod, layout), 0);
            //if the minimum value is positive use 0 to start at the base line
            final double minAggrVal = Math.min(
                bin.getMinAggregationValue(aggrMethod, layout), 0);
            //subtract half of the bar width from the start position to place
            //the middle point of the bar on the mapped coordinate position
            final int xCoord = (int)(labelCoord - (binWidth / 2));
            final int upperY = (int)(drawingHeight
                    - yCoordinates.calculateMappedValue(
                            new DoubleCell(maxAggrVal), drawingHeight));
            final int lowerY = (int)(drawingHeight
                    - yCoordinates.calculateMappedValue(
                            new DoubleCell(minAggrVal), drawingHeight));
            final Rectangle binRect = calculateBorderRectangle(xCoord, lowerY,
                    upperY, minHeight, binWidth, maxAggrVal, baseLine);
            bin.setBinRectangle(binRect, baseLine, barElementColors,
                    aggrColumns, calculator);
        } // end of for loop over the x axis coordinates
    }

    private static Rectangle calculateBorderRectangle(final int xCoord,
            final int lowerY, final int upperY, final int minHeight,
            final int width, final double maxAggrVal, final int baseLine) {
        int height = lowerY - upperY;
        int yCoord = upperY;
        //calculate the height
        if (height < minHeight) {
            if (maxAggrVal > 0) {
                yCoord = baseLine - minHeight;
            } else if (maxAggrVal < 0) {
                yCoord = baseLine;
            } else {
                //if the aggregation value is zero we have to check
                //if the base line is at coordinate 0 or not
                if (baseLine == 0) {
                    yCoord = baseLine;
                } else {
                    yCoord = baseLine - minHeight;
                }
            }
            height = minHeight;
        }
        return new Rectangle(xCoord, yCoord, width, height);
    }

    /**
     * Sets the x coordinates for the current
     * {@link AbstractHistogramVizModel}.
     */
    protected void setXCoordinates() {
        final DataColumnSpec colSpec = getXColumnSpec();
//         tell the headers to display the new column names
        final Coordinate xCoordinate = Coordinate.createCoordinate(colSpec);
        if (xCoordinate == null) {
            throw new IllegalStateException("Internal exception: "
                    + " Unable to create x coordinates");
        }
        if (getXAxis() == null) {
            final Axis xAxis = new Axis(Axis.HORIZONTAL,
                    getDrawingPaneDimension().width);
            setXAxis(xAxis);
        }
        getXAxis().setCoordinate(xCoordinate);
        getXAxis().setToolTipText(colSpec.getName());
    }

    /**
     * Sets the y coordinates for the current
     * {@link AbstractHistogramVizModel}.
     */
    protected void setYCoordinates() {
        final DataColumnSpec aggrColSpec = getAggregationColSpec();
//        setYColName(aggrColSpec.getName());
//         tell the headers to display the new column names
        final Coordinate yCoordinate = Coordinate.createCoordinate(aggrColSpec);
        if (getYAxis() == null) {
            final Axis yAxis = new Axis(Axis.VERTICAL,
                    getDrawingPaneDimension().height);
            setYAxis(yAxis);
        }
        getYAxis().setCoordinate(yCoordinate);
        getYAxis().setToolTipText(aggrColSpec.getName());
    }

      /**
     * @param noOfBins sets the number of bins which is used for binning of
     * none nominal attributes
     * @return <code>true</code> if the value has changed
     */
    protected boolean setNumberOfBins(final int noOfBins) {
        final AbstractHistogramVizModel vizModel = getHistogramVizModel();
        if (vizModel == null) {
            LOGGER.debug("VizModel was null");
            throw new IllegalStateException(
                    "Exception in setNumberOfBins: "
                    + "Viz model must not be null");
        }
        if (vizModel.setNoOfBins(noOfBins)) {
            setXCoordinates();
            setYCoordinates();
            //set the current hilited keys in the new bins
            vizModel.updateHiliteInfo(delegateGetHiLitKeys(), true);
            if (HistogramLayout.SIDE_BY_SIDE.equals(
                    vizModel.getHistogramLayout())
                    && (vizModel.getAggrColumns() != null
                            && vizModel.getAggrColumns().size() > 1)) {
                vizModel.setBinWidth(vizModel.getMaxBinWidth());
            }
            //update the details tab
            getHistogramPropertiesPanel().updateHTMLDetailsPanel(
                    vizModel.getHTMLDetailData());
            return true;
        }
        return false;
    }

    /**
     * Sets new aggregation columns and recalculates/repaints.
     *
     * @param aggrMethod The aggregation method
     * @return <code>true</code> if the method has change otherwise
     * <code>false</code>.
     */
    public boolean setAggregationMethod(final AggregationMethod aggrMethod) {
        if (aggrMethod == null) {
            throw new IllegalArgumentException("Aggregation method must not"
                    + " be null");
        }
        final AbstractHistogramVizModel vizModel = getHistogramVizModel();
        if (vizModel == null) {
            LOGGER.debug("VizModel was null");
            throw new IllegalStateException(
                    "Exception in setAggregationMethod: "
                    + "Visualization model must not be null");
        }
        if (!vizModel.setAggregationMethod(aggrMethod)) {
            return false;
        }
        // if the method has changed we have to update the y coordinates
        setYCoordinates();
        return true;
    }

    /**
     * Convenience method to cast the drawing pane.
     *
     * @return the underlying plotter drawing pane
     */
    protected HistogramDrawingPane getHistogramDrawingPane() {
        final HistogramDrawingPane myPane =
            (HistogramDrawingPane)getDrawingPane();
        if (myPane == null) {
            throw new IllegalStateException("Drawing pane must not be null");
        }
        return myPane;
    }

    /**
     * @return the {@link AbstractHistogramProperties} panel
     */
    public AbstractHistogramProperties getHistogramPropertiesPanel() {
        return m_histoProps;
    }

    /**
     * @return the <code>DataColumnSpec</code> of the aggregation column
     */
    public DataColumnSpec getAggregationColSpec() {
        final AbstractHistogramVizModel vizModel = getHistogramVizModel();
        if (vizModel == null) {
            LOGGER.debug("VizModel was null");
            throw new IllegalStateException(
                    "Exception in getAggregationColSpec: "
                    + "Viz model must not be null");
        }
        double lowerBound = vizModel.getMinAggregationValue();
        double upperBound = vizModel.getMaxAggregationValue();
        //set the upper bound for negative values and the lower bound for
        //positive values to 0 to ensure that the 0 is displayed on the
        //coordinate
        if (lowerBound > 0) {
            lowerBound = 0;
        } else if (upperBound < 0) {
            upperBound = 0;
        }
        final AggregationMethod aggrMethod =
            vizModel.getAggregationMethod();
        // set the column type depending on the aggregation method and type of
        // the aggregation column. If the method is count set it to integer. If
        // the aggregation method is summary and the data type of the
        // aggregation column is integer the result must be an integer itself
        DataType type = DoubleCell.TYPE;
        final Collection<? extends ColorColumn> columnNames =
            vizModel.getAggrColumns();
        if (AggregationMethod.COUNT.equals(aggrMethod)
                || AggregationMethod.VALUE_COUNT.equals(aggrMethod)) {
            type = IntCell.TYPE;
        }
        if (AggregationMethod.SUM.equals(aggrMethod) && columnNames != null) {
            //if the aggregation method is summary and ...
            boolean allInteger = true;
            for (final ColorColumn column : columnNames) {
                final DataColumnSpec colSpec =
                    m_tableSpec.getColumnSpec(column.getColumnName());
                if (colSpec == null
                        || !colSpec.getType().isCompatible(IntValue.class)) {
                    allInteger = false;
                    break;
                }
            }
            //... all columns of the int type we can set the column type to int
            if (allInteger) {
                type = IntCell.TYPE;
            }
        }
        final String displayColumnName = createAggregationColumnName(
                columnNames, aggrMethod);
        final DataColumnSpec spec = createColumnSpec(displayColumnName, type,
                lowerBound, upperBound, null);
        return spec;
    }

    /**
     * Returns the <code>DataColumnSpec</code> of the x column.
     *
     * @return the <code>DataColumnSpec</code> of the x column
     */
    private DataColumnSpec getXColumnSpec() {
        final AbstractHistogramVizModel vizModel = getHistogramVizModel();
        if (vizModel == null) {
            throw new IllegalStateException(
                    "Exception in getXColumnSpec: "
                    + "Viz model must not be null");
        }
        final DataColumnSpec xColSpec = vizModel.getXColumnSpec();
        String colName = xColSpec.getName();
        final Set<DataCell> binCaptions = vizModel.getBinCaptions();
        if (vizModel.isBinNominal()) {
            colName = "Binned " + xColSpec.getName();
        }
//      check if the column contains only missing values if that's the
        // case set one value which indicates this
        if (binCaptions.size() < 1) {
            binCaptions.add(new StringCell(
                    "No bins available"));
            final DataColumnSpec colSpec = createColumnSpec(colName,
                    StringCell.TYPE, Double.NaN, Double.NaN, binCaptions);
            return colSpec;
        }
        final DataColumnSpec colSpec = createColumnSpec(colName,
                StringCell.TYPE, Double.NaN, Double.NaN, binCaptions);
        return colSpec;
    }

    /**
     * Returns <code>DataColumnSpec</code> object with the given properties.
     *
     * @param columnName name of the column.
     * @param type the <code>DataType</code> of the column
     * @param lowerBound the lower bound if available if not set it to
     * <code>Double.NaN</code> @param upperBoundthe upper bound if available if
     * not set it to <code>Double.NaN</code>
     * @param values the possible values for nominal types or <code>null</code>
     * for discrete types
     * @return the <code>DataColumnSpec</code> object created using the
     * given values
     */
    private DataColumnSpec createColumnSpec(final String columnName,
            final DataType type, final double lowerBound,
            final double upperBound, final Set<DataCell> values) {
        final DataColumnDomainCreator domainCreator =
            new DataColumnDomainCreator();
        if (!Double.isNaN(lowerBound) && !Double.isNaN(upperBound)) {
            DataCell lowerBoundCell = null;
            DataCell upperBoundCell = null;
            if (type.isCompatible(IntValue.class)) {
                lowerBoundCell = new IntCell((int)lowerBound);
                upperBoundCell = new IntCell((int)upperBound);
            } else {
                lowerBoundCell = new DoubleCell(lowerBound);
                upperBoundCell = new DoubleCell(upperBound);
            }
            domainCreator.setLowerBound(lowerBoundCell);
            domainCreator.setUpperBound(upperBoundCell);
        }
        if (values != null && values.size() > 0) {
            domainCreator.setValues(values);
        }
        final DataColumnSpecCreator specCreator = new DataColumnSpecCreator(
                columnName, type);
        specCreator.setDomain(domainCreator.createDomain());
        final DataColumnSpec spec = specCreator.createSpec();
        return spec;
    }

    /**
     * Returns the column name including the aggregation method.
     *
     * @param columnNames the origin column name
     * @param aggrMethod the aggregation method
     * @return column name with a hint about the aggregation method
     */
    private String createAggregationColumnName(
            final Collection<? extends ColorColumn> columnNames,
            final AggregationMethod aggrMethod) {
        if (aggrMethod.equals(AggregationMethod.COUNT)) {
            return AbstractHistogramPlotter.COL_NAME_COUNT;
        }
        final StringBuilder name = new StringBuilder();
        if (columnNames == null || columnNames.size() < 0) {
            //if the method is not count the user has to
            //select a aggregation column
            throw new IllegalArgumentException("Column name not defined.");
        } else if (aggrMethod.equals(AggregationMethod.SUM)) {
            name.append("Sum of ");
        } else if (aggrMethod.equals(AggregationMethod.AVERAGE)) {
            name.append("Avg of ");
        } else if (aggrMethod.equals(AggregationMethod.VALUE_COUNT)) {
            name.append("No of values for ");
        } else {
            throw new IllegalArgumentException(
                    "Aggregation method not supported.");
        }
        boolean first = true;
        for (final ColorColumn column : columnNames) {
            if (first) {
                first = false;
            } else {
                name.append(", ");
            }
            name.append(column.getColumnName());
        }
        return name.toString();
    }

    /**
     * @param showGridLines set to <code>true</code> if the grid lines of the
     * y axis should be shown
     */
    public void setShowGridLines(final boolean showGridLines) {
        final AbstractHistogramVizModel vizModel = getHistogramVizModel();
        if (vizModel == null) {
            throw new IllegalStateException(
                    "Exception in setShowGridLines: "
                    + "Viz model must not be null");
        }
        if (vizModel.setShowGridLines(showGridLines)) {
            final HistogramDrawingPane drawingPane = getHistogramDrawingPane();
            if (drawingPane == null) {
                return;
            }
            if (showGridLines) {
                final Coordinate yCoordinates = getYAxis().getCoordinate();
                if (yCoordinates == null) {
                    return;
                }
                final double drawingHeight =
                    vizModel.getDrawingSpace().getHeight();
                final int[] gridLines =
                    getGridLineCoordinates(yCoordinates, drawingHeight);
                drawingPane.setGridLines(gridLines);
            } else {
                drawingPane.setGridLines(null);
            }
            repaint();
        }
    }

    /**
     * @param yCoordinates the coordinate object of the y axis
     * @param drawingHeight the total height to draw
     * @return the grid lines or null
     */
    private int[] getGridLineCoordinates(
            final Coordinate yCoordinates, final double drawingHeight) {
        if (yCoordinates == null) {
            throw new IllegalArgumentException("Y coordinates not defined");
        }
        final CoordinateMapping[] tickPos = yCoordinates
                .getTickPositions(drawingHeight);
        final int[] gridLines = new int[tickPos.length];
        for (int i = 0, length = tickPos.length; i < length; i++) {
            gridLines[i] = (int)(drawingHeight - tickPos[i]
                    .getMappingValue());
        }
        return gridLines;
    }

    /**
     * @param layout the {@link HistogramLayout} to use
     */
    public void setHistogramLayout(final HistogramLayout layout) {
        final AbstractHistogramVizModel vizModel = getHistogramVizModel();
        if (vizModel == null) {
            return;
        }
        if (vizModel.setHistogramLayout(layout)) {
//          if the layout has changed we have to update the y coordinates
            setYCoordinates();
            if (HistogramLayout.SIDE_BY_SIDE.equals(layout)) {
              //save the current bin width to restore it after changing the
                //layout again
                m_lastStackedBinWidth = vizModel.getBinWidth();
                if (vizModel.getAggrColumns() != null
                            && vizModel.getAggrColumns().size() > 1) {
                    //... and set the bin width to the maximum bin
                    //with by changing to the side by side layout
                    vizModel.setBinWidth(vizModel.getMaxBinWidth());
                }
            } else if (HistogramLayout.STACKED.equals(layout)) {
                //set the previous used bin width
                vizModel.setBinWidth(m_lastStackedBinWidth);
            }
            updatePaintModel();
        }
    }

    /**
     * @param showEmptyBins set to <code>true</code> if the empty bins should
     * be displayed
     * @return <code>true</code> if the value has changed
     */
    public boolean setShowEmptyBins(final boolean showEmptyBins) {
        final AbstractHistogramVizModel vizModel = getHistogramVizModel();
        if (vizModel == null) {
            LOGGER.debug("VizModel was null");
            throw new IllegalStateException(
                    "Exception in setShowEmptyBins: "
                    + "Viz model must not be null");
        }
        if (vizModel.setShowEmptyBins(showEmptyBins)) {
            setXCoordinates();
            setYCoordinates();
            if (HistogramLayout.SIDE_BY_SIDE.equals(
                    vizModel.getHistogramLayout())
                    && vizModel.containsNotPresentableBin()
                    && (vizModel.getAggrColumns() != null
                            && vizModel.getAggrColumns().size() > 1)) {
                vizModel.setBinWidth(vizModel.getMaxBinWidth());
            }
            return true;
        }
        return false;
    }

    /**
     * @param showMissingValBin the showMissingvalBar to set
     * @return <code>true</code> if the value has changed
     */
    public boolean setShowMissingValBin(final boolean showMissingValBin) {
        final AbstractHistogramVizModel vizModel = getHistogramVizModel();
        if (vizModel == null) {
            LOGGER.debug("VizModel was null");
            throw new IllegalStateException(
                    "Exception in setShowMissingValBin: "
                    + "Viz model must not be null");
        }
        if (vizModel.setShowMissingValBin(showMissingValBin)) {
            // set the coordinates to the new boundaries
            setXCoordinates();
            setYCoordinates();
            if (HistogramLayout.SIDE_BY_SIDE.equals(
                    vizModel.getHistogramLayout())
                    && vizModel.containsNotPresentableBin()
                    && (vizModel.getAggrColumns() != null
                            && vizModel.getAggrColumns().size() > 1)) {
                vizModel.setBinWidth(vizModel.getMaxBinWidth());
            }
            return true;
        }
        return false;
    }

    /**
     * Resets the Histogram data, the listener and display settings.
     */
    @Override
    public void reset() {
        super.setHiLiteHandler(null);
        m_tableSpec = null;
        resetHistogramVizModel();
        if (getXAxis() != null) {
            getXAxis().setCoordinate(null);
            getXAxis().setToolTipText("");
        }
        if (getYAxis() != null) {
            getYAxis().setCoordinate(null);
            getYAxis().setToolTipText("");
        }
        getHistogramDrawingPane().reset();
    }

    /**
     * @param hiLiteHandler The hiLiteHandler to set.
     */
    @Override
    public void setHiLiteHandler(final HiLiteHandler hiLiteHandler) {
        if (hiLiteHandler == null) {
            throw new IllegalArgumentException(
                    "HiliteHandler must not be null");
        }
        super.setHiLiteHandler(hiLiteHandler);
    }

    /**
     * @return the table specification
     */
    public DataTableSpec getDataTableSpec() {
        return m_tableSpec;
    }

    /**
     * Returns the <code>AbstractHistogramVizModel</code> on which the
     * visualisation bases on. It creates the model if it doesn't exists by
     * using the information of the getXColName method and the m_rowContainer
     * variable.
     *
     * @return the model on which the visualisation is based on
     */
    public AbstractHistogramVizModel getHistogramVizModel() {
//        if (m_vizModel == null) {
//            throw new IllegalStateException("VizModel must not be null");
//        }
        return m_vizModel;
    }

    /**
     * This method is called when ever something basic has changed. This method
     * forces the class to reload the HistogramData.
     */
    protected void resetHistogramVizModel() {
        m_vizModel = null;
    }

    /**
     * Sets the new {@link AbstractHistogramVizModel}.
     * @param tableSpec the new {@link DataTableSpec}
     * @param vizModel the new {@link AbstractHistogramVizModel}
     */
    public void setHistogramVizModel(final DataTableSpec tableSpec,
            final AbstractHistogramVizModel vizModel) {
        if (tableSpec == null) {
            throw new IllegalArgumentException(
                    "Table specification must not be null");
        }
        if (vizModel == null) {
            throw new IllegalArgumentException("Viz model must not be null");
        }
        m_tableSpec = tableSpec;
        m_vizModel = vizModel;
        vizModel.setDrawingSpace(getDrawingPaneDimension());
        //after setting all properties set the coordinate axis as well
        setXCoordinates();
        setYCoordinates();
        // select the x column also in the select box of the properties
        // panel
       m_histoProps.updateColumnSelection(m_tableSpec,
               vizModel.getXColumnName(), vizModel.getAggrColumns(),
               vizModel.getAggregationMethod());
       if (vizModel.supportsHiliting()) {
           //set the hilite information
           vizModel.updateHiliteInfo(delegateGetHiLitKeys(), true);
       }
       updatePaintModel();
    }

    /**
     * @return the name of the column used as x axis
     */
    public String getXColName() {
        final AbstractHistogramVizModel vizModel = getHistogramVizModel();
        if (vizModel == null) {
            LOGGER.debug("VizModel was null");
            throw new IllegalStateException(
                    "Exception in getAggregationColSpec: "
                    + "Viz model must not be null");
        }
        return vizModel.getXColumnName();
    }
//*************************************************************************
//Selection and hiliting section
//*************************************************************************
    /**
     * {@inheritDoc}
     */
    @Override
    public void hiLite(final KeyEvent event) {
        final AbstractHistogramVizModel vizModel = getHistogramVizModel();
        if (vizModel == null || !vizModel.supportsHiliting()) {
            LOGGER.debug("VizModel doesn't support hiliting or was null");
            return;
        }
        final Set<RowKey>hilited = event.keys();
        vizModel.updateHiliteInfo(hilited, true);
        repaint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unHiLite(final KeyEvent event) {
        final AbstractHistogramVizModel vizModel = getHistogramVizModel();
        if (vizModel == null || !vizModel.supportsHiliting()) {
            LOGGER.debug("VizModel doesn't support hiliting or was null");
            return;
        }
        final Set<RowKey>hilited = event.keys();
        vizModel.updateHiliteInfo(hilited, false);
        repaint();
    }


    /**
     * {@inheritDoc}
     */
    public void unHiLiteAll(final KeyEvent event) {
        final AbstractHistogramVizModel vizModel = getHistogramVizModel();
        if (vizModel == null || !vizModel.supportsHiliting()) {
            LOGGER.debug("VizModel doesn't support hiliting or was null");
            return;
        }
        vizModel.unHiliteAll();
        repaint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void hiLiteSelected() {
        final AbstractHistogramVizModel vizModel = getHistogramVizModel();
        if (vizModel == null || !vizModel.supportsHiliting()) {
            LOGGER.debug("VizModel doesn't support hiliting or was null");
            return;
        }
        final Set<RowKey> selectedKeys =
            vizModel.getSelectedKeys();
        delegateHiLite(selectedKeys);
        repaint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unHiLiteSelected() {
        final AbstractHistogramVizModel vizModel = getHistogramVizModel();
        if (vizModel == null || !vizModel.supportsHiliting()) {
            LOGGER.debug("VizModel doesn't support hiliting or was null");
            return;
        }
        final Set<RowKey> selectedKeys =
            vizModel.getSelectedKeys();
        delegateUnHiLite(selectedKeys);
        repaint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void selectClickedElement(final Point clicked) {
        final AbstractHistogramVizModel vizModel = getHistogramVizModel();
        if (vizModel == null) {
            LOGGER.debug("VizModel was null");
            return;
        }
        vizModel.selectElement(clicked);
        m_histoProps.updateHTMLDetailsPanel(
                vizModel.getHTMLDetailData());
        repaint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void selectElementsIn(final Rectangle selectionRectangle) {
        final AbstractHistogramVizModel vizModel = getHistogramVizModel();
        if (vizModel == null) {
            LOGGER.debug("VizModel was null");
            return;
        }
        vizModel.selectElement(selectionRectangle);
        m_histoProps.updateHTMLDetailsPanel(
                vizModel.getHTMLDetailData());
        repaint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearSelection() {
        final AbstractHistogramVizModel vizModel = getHistogramVizModel();
        if (vizModel == null) {
            return;
        }
        vizModel.clearSelection();
        repaint();
    }
}
