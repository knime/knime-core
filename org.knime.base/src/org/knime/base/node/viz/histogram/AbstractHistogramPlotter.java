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

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Set;

import org.knime.base.node.viz.plotter.AbstractPlotter;
import org.knime.base.node.viz.plotter.Axis;
import org.knime.base.util.coordinate.Coordinate;
import org.knime.base.util.coordinate.CoordinateMapping;
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

    /** The minimum height of a bar with an aggregation value > 0. */
    private static final int MINIMUM_BAR_HEIGHT = 2;
    /** The highlight selected item menu entry. */
    public static final String HILITE = HiLiteHandler.HILITE_SELECTED;
    /** The unhighlight selected item menu entry. */
    public static final String UNHILITE = HiLiteHandler.UNHILITE_SELECTED;
    /** The unhighlight item menu entry. */
    public static final String CLEAR_HILITE = HiLiteHandler.CLEAR_HILITE;
    /** Defines the minimum width of a bar. */
    public static final int MIN_BAR_WIDTH = 15;
    /** This is the minimum space between two bars. */
    public static final int SPACE_BETWEEN_BARS = 5;
    /** The <code>DataTableSpec</code> of the input data. */
    private DataTableSpec m_tableSpec;
    /**
     * Saves the <code>DataColumnSpec</code> which was used to create the x
     * coordinates.
     */
    private DataColumnSpec m_xColumnSpec = null;
    
    /**
     * The name of the x axis column.
     */
    private String m_xColumn = null;
    
    /**
     * The name of the column which is used for the aggregation. Could be
     * <code>null</code> for the count method.
     */
    private String m_aggrColName = null;
    /**
     * The aggregation method. Default is <code>AggregationMethod.COUNT</code>.
     */
    private AggregationMethod m_aggrMethod = AggregationMethod.COUNT;
    /**
     * The <code>AbstractHistogramDataModel</code> which holds the basic 
     * information.
     */
    private AbstractHistogramDataModel m_histoData;
    
    private final AbstractHistogramProperties m_histoProps;
    /** The current basic width of the bars. */
    private int m_barWidth = -1;
    /**
     *The plotter will show all bars empty or not when set to <code>true</code>.
     */
    private boolean m_showEmptyBars = false;
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
     * @param handler the HiLiteHandler to use
     */
    public AbstractHistogramPlotter(
            final AbstractHistogramProperties histogramProps,
            final AbstractHistogramDataModel dataModel,
            final HiLiteHandler handler) {
        super(new HistogramDrawingPane(handler), histogramProps);
        if (dataModel == null) {
            throw new IllegalArgumentException("Internal exception: " 
                    + " Histogram data model shouldn't be null.");
        }
        m_histoProps = histogramProps;
        m_histoProps.addAggregationChangedListener(
                new ActionListener() {
            public void actionPerformed(final ActionEvent arg0) {
                onApply();
            }
        });
        //add the visualization listener and their default values in the
        //drawing pane
        final HistogramDrawingPane drawingPane = getHistogramDrawingPane();
        m_histoProps.addShowGridChangedListener(
                new ItemListener() {
                    public void itemStateChanged(final ItemEvent e) {
                        setShowGridLines(
                                e.getStateChange() == ItemEvent.SELECTED);
                    }
                });
        //set the default value
        m_showGridLines = m_histoProps.isShowGrid();
        m_histoProps.addShowBarOutlineChangedListener(
                new ItemListener() {
                    public void itemStateChanged(final ItemEvent e) {
                        final HistogramDrawingPane drawingPane = 
                            getHistogramDrawingPane();
                        if (drawingPane != null) {
                            drawingPane.setShowBarOutline(
                                e.getStateChange() == ItemEvent.SELECTED);
                        }
                    }
                });
        //set the default value
        drawingPane.setShowBarOutline(m_histoProps.isShowBarOutline());
        //which needs the histoProps
        setHistogramDataModel(dataModel);
        // set the hilitehandler for highlighting stuff
        if (handler != null) {
            super.setHiLiteHandler(handler);
        } else {
            throw new IllegalArgumentException("HiLiteHandler not defined.");
        }
    }

    /**
     * Applies the settings to the plotter model.
     */
    protected void onApply() {
        AbstractHistogramDataModel histoModel = getHistogramDataModel();
        if (histoModel == null) {
            throw new IllegalStateException("HistogramModel shouldn't be null");
        }
        setPreferredBarWidth(m_histoProps.getBarWidth());
        if (!histoModel.isNominal()) {
            // this is only available for none nominal x axis properties
            setNumberOfBars(m_histoProps.getNoOfBars());
        }
        setAggregationMethod(m_histoProps.getSelectedAggrMethod());
        setShowEmptyBars(m_histoProps.getShowEmptyBars());
        setShowMissingvalBar(m_histoProps.getShowMissingvalBar());
        // force the repainting of the plotter
        updatePaintModel();
        // update the labels of the sliders and the select boxes
        m_histoProps.updateHistogramSettings(this);
        return;
    }    
    
//    /**
//     * @param row the {@link DataRow} to add
//     */
//    public void addDataRow(final DataRow row) {
//        m_histoData.addDataRow(row);
//    }
//    
//    /**
//     * Call this method after adding the last data row.
//     */
//    public void lastDataRowAdded() {
//        setXCoordinates();
//        setYCoordinates();
//    }

    /**
     * @see org.knime.base.node.viz.plotter.AbstractPlotter#updateSize()
     */
    @Override
    public void updateSize() {
        updateBarsAndPaint();
    }
    
    /**
     * @see org.knime.base.node.viz.plotter.AbstractPlotter#updatePaintModel()
     */
    @Override
    public void updatePaintModel() {
        updateBarsAndPaint();
//        // update the Histogram properties panel
//        if (m_tableSpec != null) {
//            getHistogramPropertiesPanel().updateHistogramSettings(this);
//        }
    }

    /**
     * Takes the data from the private row container and recalculates the bars,
     * adjusts sizes and repaints.
     */
    private synchronized void updateBarsAndPaint() {
        final Coordinate xCoordinates = getXAxis().getCoordinate();
        final Coordinate yCoordinates = getYAxis().getCoordinate();
        final Rectangle drawingSpace = calculateDrawingRectangle();
        if (xCoordinates != null && yCoordinates != null
                && drawingSpace != null) {
            final HistogramDrawingPane drawingPane = getHistogramDrawingPane();
            Hashtable<String, BarVisModel> visBars = drawingPane.getVisBars();
            visBars = createUpdateVisBars(visBars, xCoordinates, yCoordinates,
                    drawingSpace);
            drawingPane.setVisBars(visBars);
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
//                final Axis yAxis = getYAxis();
//                final int tickOffset = yAxis.getTickOffset();
                final CoordinateMapping[] tickPos = 
                    yCoordinates.getTickPositions(drawingHeight, true);
                final int[] gridLines = new int[tickPos.length];
                for (int i = 0, length = tickPos.length; i < length; i++) {
                    gridLines[i] = 
                        (int)(drawingHeight - tickPos[i].getMappingValue());
                }
                drawingPane.setGridLines(gridLines);
            } else {
                drawingPane.setGridLines(null);
            }
            m_histoProps.updateHistogramSettings(this);
            repaint();
        }
    }
    
    /**
     * Creates the <code>BarVisModel</code> objects which are used for drawing
     * the Histogram. If the bars are already exists they get only updated
     * otherwise would we loose the selected information. This is the case when
     * the user only changes the aggregation method or column.
     * 
     * @param existingBars The currently displayed bars or <code>null</code>
     * if the bars should be created. 
     * @param xCoordinates The <code>Coordinate</code> object which contains 
     * the start position of an bar on the x axis 
     * @param drawingSpace A <code>Rectangle</code> which defines the available
     * drawing space 
     * @return a <code>Collection</code> of <code>BarVisModel</code> objects 
     * containing all information needed for drawing.
     */
    private Hashtable<String, BarVisModel> createUpdateVisBars(
            final Hashtable<String, BarVisModel> existingBars, 
            final Coordinate xCoordinates, final Coordinate yCoordinates, 
            final Rectangle drawingSpace) {
        AbstractHistogramDataModel histoData = getHistogramDataModel();
        Hashtable<String, BarVisModel> visBars = 
            new Hashtable<String, BarVisModel>(histoData.getNumberOfBars() + 1);
        // this is the minimum size of a bar with an aggregation value > 0
        final int minHeight = Math.max(
                (int)HistogramDrawingPane.getBarStrokeWidth(),
                AbstractHistogramPlotter.MINIMUM_BAR_HEIGHT);
        final double drawingWidth = drawingSpace.getWidth();
        final double drawingHeight = drawingSpace.getHeight();
        CoordinateMapping[] xMappingPositions = xCoordinates.getTickPositions(
                drawingWidth, true);
        // get the default width for all bars
        final int barWidth = getBarWidth();
        for (CoordinateMapping coordinate : xMappingPositions) {
            String caption = coordinate.getDomainValueAsString();
            AbstractBarDataModel bar = histoData.getBar(caption);
            if (bar == null) {
                // if we find no bar for the caption it could be a information
                // caption like only missing values so we simply continue
                // to avoid null pointer exceptions
                if (m_showMissingValBar) {
                    bar = histoData.getMissingValueBar();
                    if (bar == null || !bar.getCaption().equals(caption)) {
                        continue;
                    }
                } else {
                    continue;
                }
            }
//calculate the starting point on the x axis for the bar
            final double xCoordinate = coordinate.getMappingValue();
            //subtract half of the bar width from the start position to place
            //the middle point of the bar on the mapped coordinate position
            final int startX = (int)(xCoordinate - (barWidth / 2));
            
            
//calculate the starting point on the y axis for the bar        
//since the coordinate system in java is on the y axis reversed the bar gets 
//painted from a starting point in the upper left corner to the bottom (height)
//and to the right (width).
            // I have to use the String value label because this is the rounded
            // aggregation value. If it's a small value which gets rounded
            double aggrVal = Double.parseDouble(bar.getLabel());
            Rectangle rect = null;
            if (aggrVal >= 0) {
                //if it's a positive value the top left corner starting point
                //is the value itself ...
                int startY = (int)(drawingHeight 
                        - yCoordinates.calculateMappedValue(
                                new DoubleCell(aggrVal), drawingHeight, true));
                //... and the end point (height) of the bar is the 0 value!
                final int fixPoint = (int)(drawingHeight 
                        - yCoordinates.calculateMappedValue(new DoubleCell(0), 
                                drawingHeight, true));
//              the calculateMappedValue method returns the position in a 
//              normal coordinate system since the java screen coordinates 
//              are vice versa we have to subtract the coordinate value from
//              the screen height to get the position on the screen

//check for rounding errors
                if (startY < 0) {
                    //avoid negative coordinates
                    startY = 0;
                }
                //make sure that the start point is above the end point which 
                //can't be moved because it is the 0 value!
                if (startY > fixPoint) {
                    startY = fixPoint;
                }
                //check the height for the minimum height
                int height = fixPoint - startY;
                if (height <= minHeight) {
                    height = minHeight;
                    //adjust the starting point to the new height to avoid
                    //painting in the negatives
                    startY = fixPoint - height;
                }
                rect = new Rectangle(startX, startY, barWidth, height);
            } else {
                //if it's a negative value the top left corner start point is
                // the 0 value ...
                final int fixPoint = (int)(drawingHeight
                        - yCoordinates.calculateMappedValue(
                        new DoubleCell(0), drawingHeight, true));
                //... and the end point (height) is the negative value itself!
                int endY = (int)(drawingHeight 
                        - yCoordinates.calculateMappedValue(
                                new DoubleCell(aggrVal), drawingHeight, true));
//              the calculateMappedValue method returns the position in a 
//              normal coordinate system since the java screen coordinates 
//              are vice versa we have to subtract the coordinate value from
//              the screen height to get the position on the screen
                

//check for rounding errors
                  if (endY > drawingHeight) {
                      //avoid bigger bars than the drawing screen
                      endY = (int)Math.floor(drawingHeight);
                  }
                  //make sure that the end point is below the start point which 
                  //can't be moved because it is the 0 value!
                  if (fixPoint > endY) {
                      endY = fixPoint;
                  }
                  //check the height for the minimum height
                  int height = endY - fixPoint;
                  if (height <= minHeight && aggrVal > 0) {
                      height = minHeight;
                      //adjust the starting point to the new height to avoid
                      //painting in the negatives
                      endY = fixPoint + height;
                  }
                  rect = new Rectangle(startX, fixPoint, barWidth, height);
            }

            BarVisModel visBar = null;
            if (existingBars == null 
                    || !existingBars.containsKey(bar.getCaption())) {
                visBar = new BarVisModel(bar, rect, m_tableSpec);
            } else {
                visBar = existingBars.get(bar.getCaption());
                if (visBar == null) {
                    //this shouldn't happen
                    visBar = new BarVisModel(bar, rect, m_tableSpec);
                }
                visBar.updateBarData(bar, rect);
                
            }
            visBars.put(bar.getCaption(), visBar);
        } // end of for loop over the x axis coordinates
        return visBars;
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
        m_xColumnSpec = colSpec;
//         tell the headers to display the new column names
        final Coordinate xCoordinate = Coordinate.createCoordinate(colSpec);
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
     * @see #getMaxBarWidth()
     * @param barWidth the new width of the bar
     * @return <code>true</code> if the value has change otherwise
     *         <code>false</code>
     */
    protected boolean setPreferredBarWidth(final int barWidth) {
        if (m_barWidth == barWidth) {
            return false;
        } else {
            if (barWidth < 0) {
                m_barWidth = 0;
            } else {
                m_barWidth = barWidth;
            }
            // updatePaintModel();
            return true;
        }
    }

    /**
     * @return the current preferred width of the bars.
     */
    protected int getBarWidth() {
        if (m_barWidth < 0 && getHistogramDataModel() != null) {
            // that only occurs at the first call
            int noOfBars = getHistogramDataModel().getNumberOfBars();
            Rectangle drawingSpace = calculateDrawingRectangle();
            m_barWidth = (int)(drawingSpace.getWidth() / noOfBars)
                    - SPACE_BETWEEN_BARS;
        }
        if (m_barWidth < MIN_BAR_WIDTH) {
            m_barWidth = MIN_BAR_WIDTH;
        }
        if (m_barWidth > getMaxBarWidth()) {
            // to avoid to wide bars after resizing the window!
            m_barWidth = getMaxBarWidth();
        }
        //draw at least a small line
        if (m_barWidth <= 0) {
            m_barWidth = 1;
        }
        return m_barWidth;
    }

    /**
     * @return the maximum width per bar for the current display settings.
     */
    protected int getMaxBarWidth() {
        int noOfBars = getNoOfDisplayedBars();
        final Rectangle drawingSpace = calculateDrawingRectangle();
    
        int result = (int)(drawingSpace.getWidth() / noOfBars)
                - SPACE_BETWEEN_BARS;
        if (result < 0) {
            // avoid negative values
            result = 0;
        }
        return result;
    }

    /**
     * @param noOfBars sets the number of bars which is used for binning of none
     *            nominal attributes
     * @return <code>true</code> if the value has changed
     */
    protected boolean setNumberOfBars(final int noOfBars) {
        if (getHistogramDataModel().setNumberOfBars(noOfBars)) {
            setXCoordinates();
            setYCoordinates();
            // reset the vis bars
            getHistogramDrawingPane().setVisBars(null);
            // and we have to set the new max value for the y axis
//            DataColumnSpec yColSpec = getAggregationColSpec();
//            Coordinate yCoordinate = Coordinate.createCoordinate(yColSpec);
//            getYAxis().setCoordinate(yCoordinate);
            // updatePaintModel();
            return true;
        }
        return false;
    }

    /**
     * @return the number of bars which are currently displayed
     */
    public int getNoOfDisplayedBars() {
        if (m_xColumnSpec != null && m_xColumnSpec.getDomain() != null
                && m_xColumnSpec.getDomain().getValues() != null) {
            return m_xColumnSpec.getDomain().getValues().size();
        } else { // this should never happen
            return getHistogramDataModel().getNumberOfBars();
        }
    }

    /**
     * @return the maximum number of bars which could be displayed.
     */
    protected int getMaxNoOfBars() {
        final Rectangle rect = calculateDrawingRectangle();
        int maxNoOfBars = 
            (int)(rect.getWidth() / (MIN_BAR_WIDTH + SPACE_BETWEEN_BARS));
        //handle integer values special
        final AbstractHistogramDataModel dataModel = getHistogramDataModel();
        final DataColumnSpec xColSpec = dataModel.getOriginalXColSpec();
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
        if (m_showMissingValBar && dataModel.containsMissingValueBar()) {
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
        m_aggrMethod = aggrMethod;
        AbstractHistogramDataModel model = getHistogramDataModel();
        //set the aggregation method first and ...
        final boolean aggrMethChanged = 
            model.changeAggregationMethod(aggrMethod);
        // ... then the column!!!
        if (aggrMethChanged //|| getYColName() == null
                || getYAxis().getCoordinate() == null) {
            setYCoordinates();
            return true;
        }
        return false;
    }
  
    /**
     * Creates new rectangle that spans the area in which the plotter can draw.
     * 
     * @return a new rectangle spanning the drawing area
     */
    private Rectangle calculateDrawingRectangle() {
        final Dimension dim = getDrawingPaneDimension();
        return new Rectangle(dim);
        /*
        // the actual size we can draw in
        // the max dot size is subtracted as a dot can vary in size
        int width = getWidth();
        // - getHistogramPlotterDrawingPane().getCurrentMaxDotSize();
        int height = getHeight();
        // - getHistogramPlotterDrawingPane().getCurrentMaxDotSize();
    
        // we only need an offset if we have borders
        int xOffset = 0;
        // getHistogramPlotterDrawingPane().getCurrentHalfMaxDotSize();
        int yOffset = xOffset;
        if (getInsets() != null) {
            Insets paneInsets = getHistogramDrawingPane().getInsets();
            width -= paneInsets.left + paneInsets.right;
            xOffset += paneInsets.left;
            height -= paneInsets.top + paneInsets.bottom;
            yOffset += paneInsets.top;
        }
        return new Rectangle(xOffset, yOffset, width, height);*/
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
        double lowerBound = getHistogramDataModel().getMinAggregationValue(
                isShowMissingValBar());
        double upperBound = getHistogramDataModel().getMaxAggregationValue(
                isShowMissingValBar());
        //set the upper bound for negative values and the lower bound for
        //positive values to 0 to ensure that the 0 is displayed on the 
        //coordinate
        if (lowerBound > 0) {
            lowerBound = 0;
        } else if (upperBound < 0) {
            upperBound = 0;
        }
        final String columnName = 
            getHistogramDataModel().getAggregationColumn();
        final DataColumnSpec colSpec = m_tableSpec.getColumnSpec(columnName);
        // set the column type depending on the aggregation method and type of
        // the aggregation column. If the method is count set it to integer. If
        // the aggregation method is summary and the data type of the
        // aggregation
        // column is integer the result must be an integer itself
        DataType type = DoubleCell.TYPE;
        if (AggregationMethod.COUNT.equals(m_aggrMethod)
                || (colSpec != null
                        && colSpec.getType().isCompatible(IntValue.class) 
                        && !AggregationMethod.AVERAGE.equals(m_aggrMethod))) {
            type = IntCell.TYPE;
        }
        final String displayColumnName = createAggregationColumnName(columnName,
                m_aggrMethod);
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
        final AbstractHistogramDataModel model = getHistogramDataModel();
        final DataColumnSpec xColSpec = model.getOriginalXColSpec();
        String colName = xColSpec.getName();
        Set<DataCell> binCaptions = null;
        if (model.isNominal()) {
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
            } else {
                binCaptions = xColSpec.getDomain().getValues();
            }
        } else {
            binCaptions = model.getOrderedBarCaptions();
            colName = "Binned " + xColSpec.getName();
        }
        // add or hide bars depending on the user choice
        if (!m_showEmptyBars || m_showMissingValBar) {
            LinkedHashSet<DataCell> extendedBinCaptions = 
                new LinkedHashSet<DataCell>(binCaptions.size() + 1);
            extendedBinCaptions.addAll(binCaptions);
            if (!m_showEmptyBars) {
                for (DataCell cell : binCaptions) {
                    AbstractBarDataModel bar = model.getBar(cell.toString());
                    if (bar == null || bar.isEmpty()) {
                        extendedBinCaptions.remove(cell);
                    }
                }
            }
            if (m_showMissingValBar) {
                AbstractBarDataModel missingValBar = model.getMissingValueBar();
                if (missingValBar != null) {
                    StringCell missingValCell = new StringCell(missingValBar
                            .getCaption());
                    extendedBinCaptions.add(missingValCell);
                }
            }
            binCaptions = extendedBinCaptions;
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
     * @param colName the origin column name
     * @param aggrMethod the aggregation method
     * @return column name with a hint about the aggregation method
     */
    private String createAggregationColumnName(final String colName, 
            final AggregationMethod aggrMethod) {
        if (aggrMethod.equals(AggregationMethod.COUNT)) {
            return AbstractHistogramDataModel.COL_NAME_COUNT;
        } else if (colName == null || colName.length() < 0) {
            throw new IllegalArgumentException("Column name not defined.");
        } else if (aggrMethod.equals(AggregationMethod.SUM)) {
            return "Sum of " + colName;
        } else if (aggrMethod.equals(AggregationMethod.AVERAGE)) {
            return "Avg of " + colName;
        } else {
            throw new IllegalArgumentException(
                    "Aggregation method not supported.");
        }
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
            updateBarsAndPaint();
        }
    }
    
    /**
     * @return the showEmptyBars
     */
    public boolean isShowEmptyBars() {
        return m_showEmptyBars;
    }

    /**
     * @param showEmptyBars the showEmptyBars to set
     * @return <code>true</code> if the value has changed
     */
    public boolean setShowEmptyBars(final boolean showEmptyBars) {
        if (showEmptyBars != m_showEmptyBars) {
            m_showEmptyBars = showEmptyBars;
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
            if (!showMissingvalBar) {
                // remove a possibly set missing value bar from the vis bars
                AbstractBarDataModel missingValBar = getHistogramDataModel()
                        .getMissingValueBar();
                if (missingValBar != null) {
                    Hashtable<String, BarVisModel> visBars = 
                        getHistogramDrawingPane().getVisBars();
                    if (visBars != null) {
                        visBars.remove(missingValBar.getCaption());
                    }
                }
            }
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
        resetHistogramData();
        getXAxis().setCoordinate(null);
        getXAxis().setToolTipText("");
        getYAxis().setCoordinate(null);
        getYAxis().setToolTipText("");
        getHistogramDrawingPane().reset();
    }

    /**
     * @see org.knime.core.node.property.hilite.HiLiteListener#hiLite(
     *      org.knime.core.node.property.hilite.KeyEvent)
     */
    @Override
    public void hiLite(final KeyEvent event) {
        // we don't need to take care of un/highlight since we don't save
        // the highlight state in an object but ask the hilitehandler itself
        // every time.
        updatePaintModel();
    }

    /**
     * @see 
     * org.knime.core.node.property.hilite.HiLiteListener#unHiLite(KeyEvent)
     */
    @Override
    public void unHiLite(final KeyEvent event) {
        // we don't need to take care of un/highlight since we don't save
        // the highlight state in an object but ask the hilitehandler itself
        // every time.
        updatePaintModel();
    }


    /**
     * @see org.knime.core.node.property.hilite.HiLiteListener#unHiLiteAll()
     */
    public void unHiLiteAll() {
        // we don't need to take care of un/highlight since we don't save
        // the highlight state in an object but ask the hilitehandler itself
        // every time.
        updatePaintModel();
    }

    /**
     * @param hiLiteHandler The hiLiteHandler to set.
     */
    @Override
    public void setHiLiteHandler(final HiLiteHandler hiLiteHandler) {
        super.setHiLiteHandler(hiLiteHandler);
        getHistogramDrawingPane().setHiLiteHandler(hiLiteHandler);
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
    protected void setDataTableSpec(final DataTableSpec tableSpec) {
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
    public AbstractHistogramDataModel getHistogramDataModel() {
        return m_histoData;
    }
    
    /**
     * @param histoData the new {@link AbstractHistogramDataModel}
     */
    public void setHistogramDataModel(
            final AbstractHistogramDataModel histoData) {
        m_histoData = histoData;
        m_tableSpec = m_histoData.getTableSpec();
        if (m_tableSpec == null) {
            throw new IllegalArgumentException("Internal exception:"
                    + " Row container shouldn't be null.");
        }
        m_xColumn = m_histoData.getXColumn();
        if (m_xColumn == null || m_xColumn.length() < 1) {
            throw new IllegalArgumentException("No column available to set.");
        }
        m_aggrColName = m_histoData.getAggregationColumn();
        m_aggrMethod = m_histoData.getAggregationMethod();
        //after setting all properties set the coordinate axis as well
        setXCoordinates();
        setYCoordinates();
        // select the x column also in the select box of the properties
        // panel
       m_histoProps.updateColumnSelection(m_tableSpec, m_xColumn, 
                m_aggrColName);
    }

    /**
     * @return the name of the aggregation column
     */
    protected String getAggregationColName() {
        return m_aggrColName;
    }

    /**
     * @param aggrColName sets the new aggregation column
     */
    public void setAggregationColName(final String aggrColName) {
        m_aggrColName = aggrColName;
    }

    /**
     * @return the {@link AggregationMethod}
     */
    public AggregationMethod getAggregationMethod() {
        return m_aggrMethod;
    }

    /**
     * @return the name of the column used as x axis
     */
    public String getXColName() {
        return m_xColumn;
    }
    
    /**
     * @param name the name of the column used as x axis
     */
    public void setXColName(final String name) {
        m_xColumn = name;
    }
    
    /**
     * @return the name of the column used used y axis
     */
    public String getYColName() {
        return m_aggrColName;
    }
    
    /**
     * @param name the name of the column used as y axis
     */
    public void setYColName(final String name) {
        m_aggrColName = name;
    }

    

    /**
     * @see org.knime.base.node.viz.plotter.AbstractPlotter#hiLiteSelected()
     */
    @Override
    public void hiLiteSelected() {
        final HistogramDrawingPane drawingPane = getHistogramDrawingPane();
        delegateHiLite(drawingPane.getKeys4SelectedBars());
    }

    /**
     * @see org.knime.base.node.viz.plotter.AbstractPlotter#unHiLiteSelected()
     */
    @Override
    public void unHiLiteSelected() {
        final HistogramDrawingPane drawingPane = getHistogramDrawingPane();
        delegateUnHiLite(drawingPane.getKeys4SelectedBars());
    }
    
    /**
     * @see AbstractPlotter#selectClickedElement(java.awt.Point)
     */
    @Override
    public void selectClickedElement(final Point clicked) {
        final HistogramDrawingPane drawingPane = getHistogramDrawingPane();
        final Collection<BarVisModel> bars = drawingPane.getVisBars().values();
        if (bars == null) {
            return;
        }
        for (BarVisModel bar : bars) {
            if (bar.getRectangle().contains(clicked)) {
                bar.setSelected(!bar.isSelected());
            } else {
                bar.setSelected(false);
            }
        }
        return;
    }

    /**
     * @see AbstractPlotter#selectElementsIn(java.awt.Rectangle)
     */
    @Override
    public void selectElementsIn(final Rectangle selectionRectangle) {
        final HistogramDrawingPane drawingPane = getHistogramDrawingPane();
        if (drawingPane == null) {
            return;
        }
        Hashtable<String, BarVisModel> visBars = drawingPane.getVisBars();
        if (visBars == null) {
            return;
        }
        final Collection<BarVisModel> bars = visBars.values();
        if (bars == null) {
            return;
        }
        for (BarVisModel bar : bars) {
            if (bar.screenRectOverlapping(selectionRectangle)) {
                bar.setSelected(!bar.isSelected());
            } else {
                bar.setSelected(false);
            }
        }
        return;
    }
    
    /**
     * @see org.knime.base.node.viz.plotter.AbstractPlotter#clearSelection()
     */
    @Override
    public void clearSelection() {
        final HistogramDrawingPane drawingPane = getHistogramDrawingPane();
        Hashtable<String, BarVisModel> visBars = drawingPane.getVisBars();
        if (visBars == null) {
            return;
        }
        final Collection<BarVisModel> bars = visBars.values();
        if (bars == null) {
            return;
        }
        for (BarVisModel bar : bars) {
            bar.setSelected(false);
        }
    }
}
