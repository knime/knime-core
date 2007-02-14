/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
package org.knime.base.node.viz.histogram;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.viz.histogram.datamodel.BinDataModel;
import org.knime.base.node.viz.histogram.datamodel.ColorColumn;
import org.knime.base.node.viz.histogram.datamodel.HistogramDataModel;
import org.knime.base.node.viz.histogram.datamodel.HistogramVizModel;
import org.knime.base.node.viz.plotter.AbstractPlotter;
import org.knime.base.node.viz.plotter.Axis;
import org.knime.base.util.coordinate.Coordinate;
import org.knime.base.util.coordinate.CoordinateMapping;
import org.knime.base.util.coordinate.NominalCoordinate;
import org.knime.base.util.coordinate.NumericCoordinate;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.IntValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.KeyEvent;

/**
 * Abstract class which is the coordinator between the 
 * {@link HistogramDrawingPane} and the {@link AbstractHistogramDataModel}.
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public abstract class AbstractHistogramPlotter extends AbstractPlotter {
    /**This name is used for the y axis if the aggregation method is count.*/
    public static final String COL_NAME_COUNT = "Count";
    /**Number of digits used in an interval.*/
    public static final int INTERVAL_DIGITS = 2;
    
    /** The minimum height of a bar with an aggregation value > 0. */
    private static final int MINIMUM_BAR_HEIGHT = 2;
    /** The highlight selected item menu entry. */
    public static final String HILITE = HiLiteHandler.HILITE_SELECTED;
    /** The unhighlight selected item menu entry. */
    public static final String UNHILITE = HiLiteHandler.UNHILITE_SELECTED;
    /** The unhighlight item menu entry. */
    public static final String CLEAR_HILITE = HiLiteHandler.CLEAR_HILITE;
    /** Defines the minimum width of a bar. */
    public static final int MIN_BIN_WIDTH = 15;
    /** This is the minimum space between two bars. */
    public static final int SPACE_BETWEEN_BINS = 5;
    /** The <code>DataTableSpec</code> of the input data. */
    private DataTableSpec m_tableSpec;

    /**
     * The <code>HistogramDataModel</code> which holds the basic 
     * information.
     */
    private HistogramVizModel m_histoData;
    
    private final AbstractHistogramProperties m_histoProps;
    /** The current basic width of the bars. */
    private int m_binWidth = -1;
    
    /**If the user changes the layout to side-by-side we automatically
     * set the bin width to maximum. If he goes back to stacked layout
     * show the bars with the original bar width. Thats what this variable
     * stores.*/
    private int m_lastStackedBinWidth = -1;
    
    /**
     *The plotter will show all bars empty or not when set to <code>true</code>.
     */
    private boolean m_showEmptyBins = false;
    /**
     * The plotter will show an additional bar which contains all rows which
     * have a missing value for the selected x axis.
     */
    private boolean m_showMissingValBar = true;

    /**If set to true the plotter paints the grid lines for the y axis values.*/
    private boolean m_showGridLines = false;

    
    /**Constructor for class AbstractHistogramPlotter.
     * @param histogramProps the histogram properties panel
     * @param dataModel the data model on which the plotter based on
     * @param tableSpec the input table specification
     * @param handler the HiLiteHandler to use
     */
    public AbstractHistogramPlotter(
            final AbstractHistogramProperties histogramProps,
            final HistogramDataModel dataModel, final DataTableSpec tableSpec,
            final HiLiteHandler handler) {
        super(new HistogramDrawingPane(), histogramProps);
        if (dataModel == null) {
            throw new IllegalArgumentException("Internal exception: " 
                    + " Histogram data model shouldn't be null.");
        }
        if (tableSpec == null) {
            throw new IllegalArgumentException("Internal exception: " 
                    + " Table specification shouldn't be null.");
        }
        m_histoProps = histogramProps;
        m_histoProps.addAggregationChangedListener(
                new ActionListener() {
            public void actionPerformed(final ActionEvent arg0) {
                onApply();
            }
        });
//      add the visualization listener
        registerPropertiesChangeListener();
//      set the default value
        final HistogramDrawingPane drawingPane = getHistogramDrawingPane();
        m_showGridLines = m_histoProps.isShowGrid();
        drawingPane.setShowElementOutline(m_histoProps.isShowBarOutline());
        drawingPane.setShowLabelVertical(m_histoProps.isShowLabelVertical());
        m_tableSpec = tableSpec;
        setHistogramDataModel(dataModel);
        // set the hilitehandler for highlighting stuff
        if (handler != null) {
            super.setHiLiteHandler(handler);
        } else {
            throw new IllegalArgumentException("HiLiteHandler not defined.");
        }
    }

    /**
     * Registers all histogram properties listener to the histogram
     * properties panel. 
     */
    private void registerPropertiesChangeListener() {
        if (m_histoProps == null) {
            throw new IllegalStateException(
                    "Properties panel shouldn't be null");
        }
        m_histoProps.addShowGridChangedListener(
                new ItemListener() {
                    public void itemStateChanged(final ItemEvent e) {
                        setShowGridLines(
                                e.getStateChange() == ItemEvent.SELECTED);
                    }
                });
        
        m_histoProps.addShowBarOutlineChangedListener(
                new ItemListener() {
                    public void itemStateChanged(final ItemEvent e) {
                        final HistogramDrawingPane histoDrawingPane = 
                            getHistogramDrawingPane();
                        if (histoDrawingPane != null) {
                            histoDrawingPane.setShowElementOutline(
                                e.getStateChange() == ItemEvent.SELECTED);
                        }
                    }
                });
        
        m_histoProps.addLabelOrientationListener(
                new ActionListener() {
                    public void actionPerformed(final ActionEvent e) {
                        final HistogramDrawingPane histoDrawingPane = 
                            getHistogramDrawingPane();
                        final AbstractHistogramProperties histoProps = 
                            getHistogramPropertiesPanel();
                        if (histoDrawingPane != null) {
                            histoDrawingPane.setShowLabelVertical(
                                    histoProps.isShowLabelVertical());
                        }                       
                    }
                });
        
        m_histoProps.addLabelDisplayListener(
            new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    final HistogramDrawingPane histoDrawingPane = 
                        getHistogramDrawingPane();
                    final AbstractHistogramProperties histoProps = 
                        getHistogramPropertiesPanel();
                    if (histoDrawingPane != null) {
                        histoDrawingPane.setLabelDisplayPolicy(
                                histoProps.getLabelDisplayPolicy());
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
        
        m_histoProps.addBarWidthChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                final JSlider source = (JSlider)e.getSource();
                //react only when the user has set the value and not during
                //moving the slider
//                if (!source.getValueIsAdjusting()) {
                    final int barWidth = source.getValue();
                    if (setPreferredBarWidth(barWidth)) {
                        updatePaintModel();
                    }
//                }
            }
        });
    }

    /**
     * Applies the settings to the plotter model.
     */
    protected void onApply() {
        final HistogramVizModel histoModel = getHistogramVizModel();
        if (histoModel == null) {
            throw new IllegalStateException("HistogramModel shouldn't be null");
        }
//        boolean hasChange = setPreferredBarWidth(m_histoProps.getBarWidth());
        boolean hasChange = 
            setAggregationMethod(m_histoProps.getSelectedAggrMethod());
        if (!histoModel.isBinNominal()) {
            // this is only available for none nominal x axis properties
            hasChange = hasChange 
            || setNumberOfBins(m_histoProps.getNoOfBars());
        }
        hasChange = hasChange 
        || setShowEmptyBins(m_histoProps.isShowEmptyBars());
        hasChange = hasChange 
        || setShowMissingvalBar(m_histoProps.isShowMissingValBar());
        if (hasChange) {
            // force the repainting of the plotter
            updatePaintModel();
            // update the labels of the sliders and the select boxes
            m_histoProps.updateHistogramSettings(this);
        }
        return;
    }    
    
    /**
     * @see org.knime.base.node.viz.plotter.AbstractPlotter#updateSize()
     */
    @Override
    public void updateSize() {
        final HistogramVizModel dataModel = getHistogramVizModel();
        if (dataModel != null
                && HistogramLayout.SIDE_BY_SIDE.equals(
                        dataModel.getHistogramLayout())) {
            //set the bin with to the maximum bin if the layout
            //is side-by-side
            m_binWidth = getMaxBinWidth();
        }
        updatePaintModel();
    }
    
    /**
     * @see org.knime.base.node.viz.plotter.AbstractPlotter#updatePaintModel()
     */
    @Override
    public void updatePaintModel() {
        final Coordinate xCoordinates = getXAxis().getCoordinate();
        final Coordinate yCoordinates = getYAxis().getCoordinate();
        final Rectangle drawingSpace = calculateDrawingRectangle();
        if (xCoordinates != null && yCoordinates != null
                && drawingSpace != null) {
            final HistogramDrawingPane drawingPane = getHistogramDrawingPane();
            setHistogramBinRectangle(xCoordinates, yCoordinates, drawingSpace);
            drawingPane.setHistogramData(m_histoData);
            final double drawingHeight = drawingSpace.getHeight();
            if (!yCoordinates.isNominal() 
                    && ((NumericCoordinate)yCoordinates).getMinDomainValue() 
                        < 0) {
                final int baseLine = (int)(drawingHeight 
                        - yCoordinates.calculateMappedValue(
                                new DoubleCell(0), drawingHeight, true));
                drawingPane.setBaseLine(baseLine);
            } else {
                drawingPane.setBaseLine(null);
            }
            if (isShowGridLines()) {
                final int[] gridLines = 
                    getGridLineCoordinates(yCoordinates, drawingHeight);
                drawingPane.setGridLines(gridLines);
            } else {
                drawingPane.setGridLines(null);
            }
            m_histoProps.updateHistogramSettings(this);
            repaint();
        }
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
    private void setHistogramBinRectangle(final Coordinate xCoordinates, 
            final Coordinate yCoordinates, final Rectangle drawingSpace) {
        final HistogramVizModel histoData = getHistogramVizModel();
        final AggregationMethod aggrMethod = histoData.getAggregationMethod();
        final SortedSet<Color> barElementColors = 
            histoData.getBarElementColors();
        final List<ColorColumn> aggrColumns = histoData.getAggrColumns();
        final HistogramLayout layout = histoData.getHistogramLayout();
        final double drawingWidth = drawingSpace.getWidth();
        final double drawingHeight = drawingSpace.getHeight();
        final int baseLine = 
            (int)(drawingHeight - yCoordinates.calculateMappedValue(
                            new DoubleCell(0), drawingHeight, true));
        // this is the minimum size of a bar with an aggregation value > 0
        final int minHeight = Math.max(
                (int)HistogramDrawingPane.getBarStrokeWidth(),
                AbstractHistogramPlotter.MINIMUM_BAR_HEIGHT);
        // get the default width for all bins
        final int binWidth = getBinWidth();
        final Collection<DataCell> binCaptions = histoData.getBinCaptions(
                m_showEmptyBins, m_showMissingValBar);
        for (DataCell captionCell : binCaptions) {
            final double labelCoord = xCoordinates.calculateMappedValue(
                    captionCell, drawingWidth, true);
            final String caption = captionCell.toString();
            BinDataModel bin = histoData.getBin(caption);
            if (bin == null) {
                // if we find no bar for the caption it could be a information
                // caption like only missing values so we simply continue
                // to avoid null pointer exceptions
                if (m_showMissingValBar) {
                    bin = histoData.getMissingValueBin();
                    if (bin == null || !bin.getXAxisCaption().equals(caption)) {
                        continue;
                    }
                } else {
                    continue;
                }
            }
            //subtract half of the bar width from the start position to place
            //the middle point of the bar on the mapped coordinate position
            final int xCoord = (int)(labelCoord - (binWidth / 2));
            
//calculate the starting point on the y axis for the bar        
//since the coordinate system in java is on the y axis reversed the bar gets 
//painted from a starting point in the upper left corner to the bottom (height)
//and to the right (width).
            final double maxAggrVal = 
                bin.getMaxAggregationValue(aggrMethod, layout);
            final double minAggrVal = 
                bin.getMinAggregationValue(aggrMethod, layout);
            int upperY = baseLine;
            if (maxAggrVal > 0) {
                upperY = 
                    (int)(drawingHeight - yCoordinates.calculateMappedValue(
                        new DoubleCell(maxAggrVal), drawingHeight, true));    
            }
            int lowerY = baseLine;
            if (minAggrVal < 0) {
                //if we have negative values in this bar get the y coordinate
                //for them
                lowerY = 
                    (int)(drawingHeight - yCoordinates.calculateMappedValue(
                        new DoubleCell(minAggrVal), drawingHeight, true)); 
            }
            //calculate the height
            final int height = Math.max(lowerY - upperY, minHeight);
            final Rectangle binRect = 
                new Rectangle(xCoord, upperY, binWidth, height);
            bin.setBinRectangle(binRect, m_showMissingValBar, aggrMethod, 
                    layout, baseLine, barElementColors, aggrColumns);
        } // end of for loop over the x axis coordinates
    }

    
    /**
     * This method is called when ever something basic has changed. This method
     * forces the class to reload the HistogramData.
     */
    protected void resetHistogramData() {
        m_histoData = null;
    }

    /**
     * Sets the x coordinates for the current 
     * {@link AbstractHistogramDataModel}.
     */
    protected void setXCoordinates() {
        DataColumnSpec colSpec = getXColumnSpec();
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
     * {@link AbstractHistogramDataModel}.
     */
    protected void setYCoordinates() {
        DataColumnSpec aggrColSpec = getAggregationColSpec();
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
     * Sets the preferred width for the bars. If the window is to small for the 
     * size the bars are painted with the maximum bar width.
     * @see #getMaxBinWidth()
     * @param barWidth the new width of the bar
     * @return <code>true</code> if the value has change otherwise
     *         <code>false</code>
     */
    protected boolean setPreferredBarWidth(final int barWidth) {
        if (m_binWidth == barWidth) {
            return false;
        }
        if (barWidth < 0) {
            m_binWidth = 0;
        } else {
            m_binWidth = barWidth;
        }
        // updatePaintModel();
        return true;
    }

    /**
     * @return the current preferred width of the bars.
     */
    protected int getBinWidth() {
        if (m_binWidth < 0 && getHistogramVizModel() != null) {
            // that only occurs at the first call
            final int noOfBins = getHistogramVizModel().getNoOfBins();
            Rectangle drawingSpace = calculateDrawingRectangle();
            m_binWidth = (int)(drawingSpace.getWidth() / noOfBins)
                    - SPACE_BETWEEN_BINS;
        }
        if (m_binWidth < MIN_BIN_WIDTH) {
            m_binWidth = MIN_BIN_WIDTH;
        }
        if (m_binWidth > getMaxBinWidth()) {
            // to avoid to wide bars after resizing the window!
            m_binWidth = getMaxBinWidth();
        }
        //draw at least a small line
        if (m_binWidth <= 0) {
            m_binWidth = 1;
        }
        return m_binWidth;
    }

    /**
     * @return the maximum width per bar for the current display settings.
     */
    protected int getMaxBinWidth() {
        final int noOfBins = getNoOfDisplayedBins();
        final Rectangle drawingSpace = calculateDrawingRectangle();
        //the minimum bin width should be at least 1 pixel
        final int result = Math.max((int)(drawingSpace.getWidth() / noOfBins)
                - SPACE_BETWEEN_BINS, 1);
        return result;
    }

    /**
     * @param noOfBins sets the number of bins which is used for binning of 
     * none nominal attributes
     * @return <code>true</code> if the value has changed
     */
    protected boolean setNumberOfBins(final int noOfBins) {
        if (getHistogramVizModel().setNoOfBins(noOfBins)) {
            setXCoordinates();
            setYCoordinates();
            final HistogramVizModel dataModel = getHistogramVizModel();
            if (dataModel != null) {
                //set the current hilited keys in the new bins
                dataModel.updateHiliteInfo(delegateGetHiLitKeys(), true);
                if (HistogramLayout.SIDE_BY_SIDE.equals(
                            dataModel.getHistogramLayout())) {
                    //set the bin with to the maximum bin if the layout
                    //is side-by-side
                    m_binWidth = getMaxBinWidth();
                }
            }
            return true;
        }
        return false;
    }

    /**
     * @return the number of bars which are currently displayed
     */
    public int getNoOfDisplayedBins() {
        final NominalCoordinate xCoordinate = 
            (NominalCoordinate)getXAxis().getCoordinate();
        final Rectangle rectangle = calculateDrawingRectangle();
        return xCoordinate.getTickPositions(rectangle.getWidth(), true).length;
    }

    /**
     * @return the maximum number of bars which could be displayed.
     */
    protected int getMaxNoOfBars() {
        final Rectangle rect = calculateDrawingRectangle();
        int maxNoOfBars = 
            (int)(rect.getWidth() / (MIN_BIN_WIDTH + SPACE_BETWEEN_BINS));
        //handle integer values special
        final HistogramVizModel dataModel = getHistogramVizModel();
        final DataColumnSpec xColSpec = dataModel.getXColumnSpec();
        if (xColSpec != null) {
            final boolean isInteger = 
                xColSpec.getType().isCompatible(IntValue.class);
            if (isInteger) {
                final DataColumnDomain domain = xColSpec.getDomain();
                if (domain != null) {
                    final IntCell lowerBound = 
                        (IntCell)domain.getLowerBound();
                    final IntCell upperBound = 
                        (IntCell)domain.getUpperBound();
                    final int range = 
                        upperBound.getIntValue() - lowerBound.getIntValue();
                    if (maxNoOfBars > range) {
                        maxNoOfBars = range;
                    }
                }
            }
        }
        if (m_showMissingValBar && dataModel.containsMissingValueBin()) {
            maxNoOfBars--;
        }
        // display at least one bar
        if (maxNoOfBars < 1) {
            maxNoOfBars = 1;
        }
        return maxNoOfBars;
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
            throw new IllegalArgumentException("Aggregation method shouldn't"
                    + " be null");
        }
        final boolean changed = 
            getHistogramVizModel().setAggregationMethod(aggrMethod);
        if (!changed) {
            return false;
        }
        // if the method has changed we have to update the y coordinates
        setYCoordinates();
        return true;
    }
  
    /**
     * Creates new rectangle that spans the area in which the plotter can draw.
     * 
     * @return a new rectangle spanning the drawing area
     */
    private Rectangle calculateDrawingRectangle() {
        final Dimension dim = getDrawingPaneDimension();
        return new Rectangle(dim);
    }
    
    /**
     * Convenience method to cast the drawing pane.
     * 
     * @return the underlying scatter plotter drawing pane
     */
    protected HistogramDrawingPane getHistogramDrawingPane() {
        return (HistogramDrawingPane)getDrawingPane();
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
        double lowerBound = getHistogramVizModel().getMinAggregationValue();
        double upperBound = getHistogramVizModel().getMaxAggregationValue();
        //set the upper bound for negative values and the lower bound for
        //positive values to 0 to ensure that the 0 is displayed on the 
        //coordinate
        if (lowerBound > 0) {
            lowerBound = 0;
        } else if (upperBound < 0) {
            upperBound = 0;
        }
        final AggregationMethod aggrMethod = 
            getHistogramVizModel().getAggregationMethod();
        // set the column type depending on the aggregation method and type of
        // the aggregation column. If the method is count set it to integer. If
        // the aggregation method is summary and the data type of the
        // aggregation column is integer the result must be an integer itself
        DataType type = DoubleCell.TYPE;
        if (AggregationMethod.COUNT.equals(aggrMethod)) {
            type = IntCell.TYPE;
        }
        if (AggregationMethod.SUM.equals(aggrMethod)) {
            //if the aggregation method is summary and ...
            final List<ColorColumn> columnNames = 
                getHistogramVizModel().getAggrColumns();
            boolean allInteger = true;
            for (ColorColumn column : columnNames) {
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
                getHistogramVizModel().getAggrColumns(), aggrMethod);
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
        final HistogramVizModel model = getHistogramVizModel();
        final DataColumnSpec xColSpec = model.getXColumnSpec();
        String colName = xColSpec.getName();
        Set<DataCell> binCaptions = null;
        if (model.isBinNominal()) {
            // check if the column contains only missing values if that's the
            // case set one value which indicates this
            if (xColSpec.getDomain().getValues() == null
                    || xColSpec.getDomain().getValues().size() < 1) {
                binCaptions = new HashSet<DataCell>(1);
                binCaptions.add(new StringCell(
                        "Only missing or too many values"));
                DataColumnSpec colSpec = createColumnSpec(colName,
                        StringCell.TYPE, Double.NaN, Double.NaN, binCaptions);
                return colSpec;
            }
            binCaptions = xColSpec.getDomain().getValues();
        } else {
            binCaptions = 
                model.getBinCaptions(m_showEmptyBins, m_showMissingValBar);
            colName = "Binned " + xColSpec.getName();
        }
        DataColumnSpec colSpec = createColumnSpec(colName, StringCell.TYPE,
                Double.NaN, Double.NaN, binCaptions);
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
        DataColumnDomainCreator domainCreator = new DataColumnDomainCreator();
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
        DataColumnSpecCreator specCreator = new DataColumnSpecCreator(
                columnName, type);
        specCreator.setDomain(domainCreator.createDomain());
        DataColumnSpec spec = specCreator.createSpec();
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
            final List<ColorColumn> columnNames, 
            final AggregationMethod aggrMethod) {
        if (aggrMethod.equals(AggregationMethod.COUNT)) {
            return AbstractHistogramPlotter.COL_NAME_COUNT;
        }
        StringBuilder name = new StringBuilder();
        if (columnNames == null || columnNames.size() < 0) {
            throw new IllegalArgumentException("Column name not defined.");
        } else if (aggrMethod.equals(AggregationMethod.SUM)) {
            name.append("Sum of ");
        } else if (aggrMethod.equals(AggregationMethod.AVERAGE)) {
            name.append("Avg of ");
        } else {
            throw new IllegalArgumentException(
                    "Aggregation method not supported.");
        }
        for (int i = 0, length = columnNames.size(); i < length; i++) {
            if (i != 0) {
                name.append(", ");
            }
            name.append(columnNames.get(i).getColumnName());
            
        }
        return name.toString(); 
    }

    /**
     * @return <code>true</code> if the y axis grid lines should be shown.
     */
    public boolean isShowGridLines() {
        return m_showGridLines;
    }
    
    /**
     * @param showGridLines set to <code>true</code> if the grid lines of the
     * y axis should be shown
     */
    public void setShowGridLines(final boolean showGridLines) {
        if (showGridLines != m_showGridLines) {
            m_showGridLines = showGridLines;
            final HistogramDrawingPane drawingPane = getHistogramDrawingPane();
            if (drawingPane == null) {
                return;
            }
            if (showGridLines) {
                final Coordinate yCoordinates = getYAxis().getCoordinate();
                final Rectangle drawingSpace = calculateDrawingRectangle();
                if (yCoordinates == null || drawingSpace == null) {
                    return;
                }
                final double drawingHeight = drawingSpace.getHeight();
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
                .getTickPositions(drawingHeight, true);
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
        if (getHistogramVizModel().setHistogramLayout(layout)) {
//          if the layout has changed we have to update the y coordinates
            setYCoordinates();
            if (HistogramLayout.SIDE_BY_SIDE.equals(layout)) {
                //save the current bin width to restore it after changing the
                //layout again
                m_lastStackedBinWidth = getBinWidth();
                //... and set the bin width to the maximum bin 
                //with by changing to the side by side layout
                m_binWidth = getMaxBinWidth();
                
            } else if (HistogramLayout.STACKED.equals(layout)) {
                //set the previous used bin width
                m_binWidth = m_lastStackedBinWidth;
            }
            updatePaintModel();
        }
    }
    
    /**
     * @return the showEmptyBars
     */
    public boolean isShowEmptyBins() {
        return m_showEmptyBins;
    }

    /**
     * @param showEmptyBars the showEmptyBars to set
     * @return <code>true</code> if the value has changed
     */
    public boolean setShowEmptyBins(final boolean showEmptyBars) {
        if (showEmptyBars != m_showEmptyBins) {
            m_showEmptyBins = showEmptyBars;
            setXCoordinates();
            setYCoordinates();
            // updatePaintModel();
            return true;
        }
        return false;
    }

    /**
     * @return the showMissingvalBar
     */
    public boolean isShowMissingValBar() {
        return m_showMissingValBar;
    }

    /**
     * @param showMissingvalBar the showMissingvalBar to set
     * @return <code>true</code> if the value has changed
     */
    public boolean setShowMissingvalBar(final boolean showMissingvalBar) {
        if (showMissingvalBar != m_showMissingValBar) {
            m_showMissingValBar = showMissingvalBar;
//            if (!showMissingvalBar) {
//                // remove a possibly set missing value bar from the vis bars
//                BinDataModel missingValBin = getHistogramDataModel()
//                        .getMissingValueBin();
////                if (missingValBin != null) {
////                    Hashtable<String, BarVisModel> visBars = 
////                        getHistogramDrawingPane().getVisBars();
////                    if (visBars != null) {
////                        visBars.remove(missingValBin.getXAxisCaption());
////                    }
////                }
//            }
            // set the coordinates to the new boundaries
            setXCoordinates();
            setYCoordinates();
            // updatePaintModel();
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
        resetHistogramData();
        getXAxis().setCoordinate(null);
        getXAxis().setToolTipText("");
        getYAxis().setCoordinate(null);
        getYAxis().setToolTipText("");
        getHistogramDrawingPane().reset();
    }

    /**
     * @param hiLiteHandler The hiLiteHandler to set.
     */
    @Override
    public void setHiLiteHandler(final HiLiteHandler hiLiteHandler) {
        super.setHiLiteHandler(hiLiteHandler);
    }

    /**
     * @return the table specification
     */
    public DataTableSpec getDataTableSpec() {
        return m_tableSpec;
    }

    /**
     * @param tableSpec the new {@link DataTableSpec}
     */
    public void setDataTableSpec(final DataTableSpec tableSpec) {
        m_tableSpec = tableSpec;
    }

    /**
     * Returns the <code>AbstractHistogramDataModel</code> on which the 
     * visualisation bases on. It creates the model if it doesn't exists by 
     * using the information of the getXColName method and the m_rowContainer 
     * variable.
     * 
     * @return the model on which the visualisation is based on
     */
    public HistogramVizModel getHistogramVizModel() {
        return m_histoData;
    }
    
    /**
     * @param histoData the new {@link AbstractHistogramDataModel}
     */
    public void setHistogramDataModel(
            final HistogramDataModel histoData) {
        m_histoData = new HistogramVizModel(histoData, 
                HistogramVizModel.DEFAULT_NO_OF_BINS, 
                AggregationMethod.getDefaultMethod(), 
                HistogramLayout.getDefaultLayout());
        if (m_tableSpec == null) {
            throw new IllegalArgumentException("Internal exception:"
                    + " Table specification shouldn't be null.");
        }
        
        //after setting all properties set the coordinate axis as well
        setXCoordinates();
        setYCoordinates();
        // select the x column also in the select box of the properties
        // panel
       m_histoProps.updateColumnSelection(m_tableSpec, 
               histoData.getXColumnName(), histoData.getAggrColumns());
    }

    /**
     * @return the name of the column used as x axis
     */
    public String getXColName() {
        return getHistogramVizModel().getXColumnName();
    }
//*************************************************************************
//Selection and hiliting section
//*************************************************************************
    /**
     * @see org.knime.core.node.property.hilite.HiLiteListener#hiLite(
     *      org.knime.core.node.property.hilite.KeyEvent)
     */
    @Override
    public void hiLite(final KeyEvent event) {
        final Set<DataCell>hilited = event.keys();
        m_histoData.updateHiliteInfo(hilited, true);
        repaint();
    }

    /**
     * @see 
     * org.knime.core.node.property.hilite.HiLiteListener#unHiLite(KeyEvent)
     */
    @Override
    public void unHiLite(final KeyEvent event) {
        final Set<DataCell>hilited = event.keys();
        m_histoData.updateHiliteInfo(hilited, false);
        repaint();
    }


    /**
     * @see org.knime.core.node.property.hilite.HiLiteListener#unHiLiteAll()
     */
    public void unHiLiteAll() {
        m_histoData.unHiliteAll();
        repaint();
    }

    /**
     * @see org.knime.base.node.viz.plotter.AbstractPlotter#hiLiteSelected()
     */
    @Override
    public void hiLiteSelected() {
        final Set<DataCell> selectedKeys = m_histoData.getSelectedKeys(true);
        delegateHiLite(selectedKeys);
        repaint();
    }

    /**
     * @see org.knime.base.node.viz.plotter.AbstractPlotter#unHiLiteSelected()
     */
    @Override
    public void unHiLiteSelected() {
        final Set<DataCell> selectedKeys = m_histoData.getSelectedKeys(false);
        delegateUnHiLite(selectedKeys);
        repaint();
    }
    
    /**
     * @see AbstractPlotter#selectClickedElement(java.awt.Point)
     */
    @Override
    public void selectClickedElement(final Point clicked) {
        m_histoData.selectElement(clicked);
        repaint();
    }

    /**
     * @see AbstractPlotter#selectElementsIn(java.awt.Rectangle)
     */
    @Override
    public void selectElementsIn(final Rectangle selectionRectangle) {
        m_histoData.selectElement(selectionRectangle);
        repaint();
    }
    
    /**
     * @see org.knime.base.node.viz.plotter.AbstractPlotter#clearSelection()
     */
    @Override
    public void clearSelection() {
        m_histoData.clearSelection();
        repaint();
    }
}
