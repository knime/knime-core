/*-------------------------------------------------------------------
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
 *   18.08.2006 (Tobias Koetter): created
 */
package org.knime.base.node.viz.histogram;

import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.knime.base.node.viz.plotter2D.AbstractPlotter2D;
import org.knime.base.util.coordinate.Coordinate;
import org.knime.base.util.coordinate.CoordinateMapping;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.IntValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.KeyEvent;

/**
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public abstract class AbstractHistogramPlotter extends AbstractPlotter2D {

    /** The minimum height of a bar with an aggregation value > 0. */
    private static final int MINIMUM_BAR_HEIGHT = 2;
    /** The highlight selected item menu entry. */
    public static final String HILITE = HiLiteHandler.HILITE_SELECTED;
    /** The unhighlight selected item menu entry. */
    public static final String UNHILITE = HiLiteHandler.UNHILITE_SELECTED;
    /** The unhighlight item menu entry. */
    public static final String CLEAR_HILITE = HiLiteHandler.CLEAR_HILITE;
    /** Defines the minimum width of a bar. */
    public static final int MIN_BAR_WIDTH = 20;
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
    /** The <code>HiLiteHandler</code>. */
    private HiLiteHandler m_hiLiteHandler;

    
    /**Constructor for class AbstractHistogramPlotter.
     * @param initialWidth the initial width of the window
     * @param spec the specification of the input table
     * @param histogramProps the histogram properties panel
     * @param handler the HiLiteHandler to use
     * @param xColumn the name of the selected x column
     */
    public AbstractHistogramPlotter(final int initialWidth, 
            final DataTableSpec spec,
            final AbstractHistogramProperties histogramProps,
            final HiLiteHandler handler, final String xColumn) {
        super(initialWidth, histogramProps, new HistogramDrawingPane(handler));
        if (spec == null) {
            throw new IllegalArgumentException("Internal exception:"
                    + " Row container shouldn't be null.");
        }
        m_tableSpec = spec;
        if (m_tableSpec == null) {
            throw new IllegalArgumentException("Internal exception:"
                    + " Table specification shouldn't be null.");
        }

        if (xColumn == null || xColumn.length() < 1) {
            throw new IllegalArgumentException("No column available to set.");
        }
        // set the initial x column
        setXColName(xColumn);
        // select the x column also in the select box of the properties
        // panel
        histogramProps.updateColumnSelection(m_tableSpec, xColumn, null);
        // set the hilitehandler for highlighting stuff
        if (handler != null) {
            this.m_hiLiteHandler = handler;
            this.m_hiLiteHandler.addHiLiteListener(this);
        } else {
            throw new IllegalArgumentException("HiLiteHandler not defined.");
        }
        //disable the cross hair cursor by default for all histogram plots
        setCrosshairCursorEnabled(false);
    }
    
    /**
     * @param row the {@link DataRow} to add
     */
    public void addDataRow(final DataRow row) {
        m_histoData.addDataRow(row);
    }
    
    /**
     * Call this method after adding the last data row.
     */
    public void lastDataRowAdded() {
        setXCoordinates();
        setYCoordinates();
    }

    /**
     * This is called during resize, or any other pane triggered event.
     * 
     * @see org.knime.dev.node.view.plotter2D.AbstractPlotter2D
     *      #updatePaintModel()
     */
    @Override
    protected void updatePaintModel() {
        updateBarsAndPaint();
        // update the Histogram properties panel
        if (m_tableSpec != null) {
            getHistogramPropertiesPanel().setUpdateHistogramSettings(this);
        }
    }

    /**
     * Called by the view, forwarding a model changed event.
     * 
     * @param spec the specification of the input data table
     * @param selectedXCol the name of the new x column
     */
    public void modelChanged(final DataTableSpec spec, 
            final String selectedXCol) {
        m_tableSpec = spec;
        setXColName(selectedXCol);
        setBackground(ColorAttr.getBackground());
        AbstractHistogramProperties props = getHistogramPropertiesPanel();
        props.updateColumnSelection(spec, selectedXCol, null);
        props.setUpdateHistogramSettings(this);
        updatePaintModel();
    }

    /**
     * @see java.awt.event.ActionListener
     *      #actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(final ActionEvent e) {
        return;
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
        // tell the headers to display the new column names
        getColHeader().setToolTipText(colSpec.getName());
        Coordinate xCoordinate = Coordinate.createCoordinate(colSpec);
        getColHeader().setCoordinate(xCoordinate);
    }

    /**
     * Sets the y coordinates for the current 
     * {@link AbstractHistogramDataModel}.
     */
    protected void setYCoordinates() {
        DataColumnSpec aggrColSpec = getAggregationColSpec();
        setYColName(aggrColSpec.getName());
        // tell the headers to display the new column names
        getRowHeader().setToolTipText(aggrColSpec.getName());
        Coordinate yCoordinate = Coordinate.createCoordinate(aggrColSpec);
        getRowHeader().setCoordinate(yCoordinate);
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
        if (m_barWidth < 0) {
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
        if (m_barWidth < 0) {
            m_barWidth = 0;
        }
        return m_barWidth;
    }

    /**
     * @return the maximum width per bar for the current display settings.
     */
    protected int getMaxBarWidth() {
        int noOfBars = getNoOfDisplayedBars();
        Rectangle drawingSpace = calculateDrawingRectangle();
    
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
            // reset the vis bars
            getHistogramDrawingPane().setVisBars(null);
            // and we have to set the new max value for the y axis
            DataColumnSpec yColSpec = getAggregationColSpec();
            Coordinate yCoordinate = Coordinate.createCoordinate(yColSpec);
            getRowHeader().setCoordinate(yCoordinate);
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
        Rectangle rect = calculateDrawingRectangle();
        int maxNoOfBars = 
            (int)(rect.getWidth() / (MIN_BAR_WIDTH + SPACE_BETWEEN_BARS));
        if (m_showMissingValBar) {
            maxNoOfBars--;
        }
        // display at least one bar
        if (maxNoOfBars < 1) {
            maxNoOfBars = 1;
        }
        return maxNoOfBars;
    }

    /**
     * Takes the data from the private row container and recalculates the bars,
     * adjusts sizes and repaints.
     */
    private synchronized void updateBarsAndPaint() {
        Coordinate xCoordinates = getColHeader().getCoordinate();
        Coordinate yCoordinates = getRowHeader().getCoordinate();
        Rectangle drawingSpace = calculateDrawingRectangle();
        if (xCoordinates != null && yCoordinates != null
                && drawingSpace != null) {
            HistogramDrawingPane drawingPane = getHistogramDrawingPane();
            Hashtable<String, BarVisModel> visBars = drawingPane.getVisBars();
            visBars = createUpdateVisBars(visBars, xCoordinates, yCoordinates,
                    drawingSpace);
            drawingPane.setVisBars(visBars);
            repaint();
        }
    }

    /**
     * Sets new aggregation columns and recalculates/repaints.
     * 
     * @param aggrMethod The aggregation method
     * @return <code>true</code> if the method has change otherwise 
     * <code>flase</code>. 
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
        if (aggrMethChanged || getYColName() == null
                || getRowHeader().getCoordinate() == null) {
            setYCoordinates();
            return true;
        }
        return false;
    }
    
    /**
     * Creates the <code>BarVisModel</code> objects which are used for drawing
     * the Histogram. If the bars are already exists they get only updated
     * otherwise would we loose the selected information. This is the case when
     * the user only changes the aggregation method or column.
     * 
     * @param existingBars The currently displayed bars or <code>null</code>
     * if the bars should be created. @param xCoordinates The 
     * <code>Coordinate</code> object which contains the start position of an 
     * bar on the x axis 
     * @param drawingSpace A <code>Rectangle</code> which defines the available
     * drawing space @return a <code>Collection</code> of 
     * <code>BarVisModel</code> objects containing all information needed for 
     * drawing.
     */
    private Hashtable<String, BarVisModel> createUpdateVisBars(
            final Hashtable<String, BarVisModel> existingBars, 
            final Coordinate xCoordinates, final Coordinate yCoordinates, 
            final Rectangle drawingSpace) {
        AbstractHistogramDataModel histoData = getHistogramDataModel();
        Hashtable<String, BarVisModel> visBars = existingBars;
        // this is the minimum size of a bar with an aggregation value > 0
        int minHeight = Math.max((int)HistogramDrawingPane.getMaxStrokeWidth(),
                AbstractHistogramPlotter.MINIMUM_BAR_HEIGHT);
        if (existingBars == null
                || existingBars.size() < getNoOfDisplayedBars()) {
            // +1 because of a possible missing value bar
            visBars = new Hashtable<String, BarVisModel>(
                        histoData.getNumberOfBars() + 1);
        }
        final double drawingWidth = drawingSpace.getWidth();
        final double drawingHeight = drawingSpace.getHeight();
        CoordinateMapping[] xMappingPositions = xCoordinates.getTickPositions(
                drawingWidth, true);
        // get the default width for all bars
        int barWidth = getBarWidth();
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
            double xCoordinate = coordinate.getMappingValue();
            int startX = (int)(xCoordinate - barWidth / 2);
            // double aggrVal = bar.getAggregationValue();
            // I have to use the String value label because this is the rounded
            // aggregation value. If it's a small value which gets rounded
            double aggrVal = Double.parseDouble(bar.getLabel());
            // check for a negative value
            if (aggrVal < 0) {
                aggrVal = Math.abs(aggrVal);
            }
            int height = (int)yCoordinates.calculateMappedValue(new DoubleCell(
                    aggrVal), drawingHeight, true);
            if (height <= minHeight && aggrVal > 0) {
                height = minHeight;
            }
            int startY = (int)(drawingHeight - height);
            // take care of cast problems!
            if (startY < 0) {
                startY = 0;
            } else if (startY > drawingHeight) {
                startY = (int)drawingHeight;
            }
    
            Rectangle rect = new Rectangle(startX, startY, barWidth, height);
            BarVisModel visBar = visBars.get(bar.getCaption());
            if (visBar == null) {
                visBar = new BarVisModel(bar, rect, m_tableSpec);
                visBars.put(bar.getCaption(), visBar);
            } else {
                visBar.updateBarData(bar, rect);
            }
        } // end of for loop over the x axis coordinates
        return visBars;
    }

    /**
     * Creates new rectangle that spans the area in which the plotter can draw.
     * 
     * @return a new rectangle spanning the drawing area
     */
    private Rectangle calculateDrawingRectangle() {
        // the actual size we can draw in
        // the max dot size is subtracted as a dot can vary in size
        int width = getPlotterWidth();
        // - getHistogramPlotterDrawingPane().getCurrentMaxDotSize();
        int height = getPlotterHeight();
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
        return new Rectangle(xOffset, yOffset, width, height);
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
    protected AbstractHistogramProperties getHistogramPropertiesPanel() {
        return (AbstractHistogramProperties)getPropertiesPanel();
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
        String columnName = getHistogramDataModel().getAggregationColumn();
        DataColumnSpec colSpec = m_tableSpec.getColumnSpec(columnName);
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
        String displayColumnName = createAggregationColumnName(columnName,
                m_aggrMethod);
        DataColumnSpec spec = createColumnSpec(displayColumnName, type,
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
        final DataColumnSpec xColSpec = getHistogramDataModel()
                .getOriginalXColSpec();
        String colName = xColSpec.getName();
        Set<DataCell> binCaptions = null;
        if (getHistogramDataModel().isNominal()) {
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
    public void reset() {
        this.m_hiLiteHandler = null;
        resetHistogramData();
        getRowHeader().setCoordinate(null);
        getRowHeader().setToolTipText("");
        getColHeader().setCoordinate(null);
        getColHeader().setToolTipText("");
        getHistogramDrawingPane().reset();
    }

    /**
     * @see org.knime.core.node.property.hilite.HiLiteListener#hiLite(
     *      org.knime.core.node.property.hilite.KeyEvent)
     */
    public void hiLite(final KeyEvent event) {
        // we don't need to take care of un/highlight since we don't save
        // the highlight state in an object but ask the hilitehandler itself
        // every time.
        updatePaintModel();
    }

    /**
     * @see org.knime.core.node.property.hilite.HiLiteListener
     *      #unHiLite(org.knime.core.node.property.hilite.KeyEvent)
     */
    public void unHiLite(final KeyEvent event) {
        // we don't need to take care of un/highlight since we don't save
        // the highlight state in an object but ask the hilitehandler itself
        // every time.
        updatePaintModel();
    }

    /**
     * @see org.knime.core.node.property.hilite.HiLiteListener
     *      #unHiLiteAll()
     */
    public void unHiLiteAll() {
        // we don't need to take care of un/highlight since we don't save
        // the highlight state in an object but ask the hilitehandler itself
        // every time.
        updatePaintModel();
    }

    /**
     * @see org.knime.dev.node.view.plotter2D.AbstractPlotter2D
     *      #getHiLiteMenu()
     */
    @Override
    public JMenu getHiLiteMenu() {
        JMenu menu = new JMenu(HiLiteHandler.HILITE);
        for (JMenuItem item : getMenuItems()) {
            menu.add(item);
        }
        return menu;
    }

    /**
     * @see org.knime.dev.node.view.plotter2D.AbstractPlotter2D
     *      #fillPopupMenu(javax.swing.JPopupMenu)
     */
    @Override
    protected void fillPopupMenu(final JPopupMenu menu) {
        for (JMenuItem item : getMenuItems()) {
            menu.add(item);
        }
    }

    /**
     * Returns a <code>Collection</code> of <code>JMenuItem</code> objects
     * for the highlight menu.
     * 
     * @return a <code>Collection</code> of <code>JMenuItem</code> objects
     */
    private Collection<JMenuItem> getMenuItems() {
        Collection<JMenuItem> items = new ArrayList<JMenuItem>(3);
        JMenuItem hiliteItem = new JMenuItem(HILITE);
        hiliteItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                HiLiteHandler hiLiteHandler = getHiLiteHandler();
                if (hiLiteHandler != null) {
                    hiLiteHandler.hiLite(getHistogramDrawingPane()
                            .getKeys4SelectedBars());
                }
            }
        });
        items.add(hiliteItem);
    
        JMenuItem unHiLiteItem = new JMenuItem(UNHILITE);
        unHiLiteItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                HiLiteHandler hiLiteHandler = getHiLiteHandler();
                if (hiLiteHandler != null) {
                    hiLiteHandler.unHiLite(getHistogramDrawingPane()
                            .getKeys4SelectedBars());
                }
            }
        });
        items.add(unHiLiteItem);
        JMenuItem clearHiLiteItem = new JMenuItem(CLEAR_HILITE);
        clearHiLiteItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                HiLiteHandler hiLiteHandler = getHiLiteHandler();
                if (hiLiteHandler != null) {
                    hiLiteHandler.unHiLiteAll();
                }
            }
        });
        items.add(clearHiLiteItem);
        return items;
    }

    /**
     * @param hiLiteHandler The hiLiteHandler to set.
     */
    public void setHiLiteHandler(final HiLiteHandler hiLiteHandler) {
        m_hiLiteHandler = hiLiteHandler;
        getHistogramDrawingPane().setHiLiteHandler(hiLiteHandler);
    }

    /**
     * @return the table specification
     */
    public DataTableSpec getDataTableSpec() {
        return m_tableSpec;
    }

    protected AbstractHistogramDataModel getHistoData() {
        return m_histoData;
    }

    protected void setHistoData(AbstractHistogramDataModel histoData) {
        m_histoData = histoData;
    }

    protected String getAggregationColName() {
        return m_aggrColName;
    }

    public AggregationMethod getAggregationMethod() {
        return m_aggrMethod;
    }

    protected DataTableSpec getTableSpec() {
        return m_tableSpec;
    }

    public void setAggregationColName(String aggrColName) {
        m_aggrColName = aggrColName;
    }

    protected void setTableSpec(DataTableSpec tableSpec) {
        m_tableSpec = tableSpec;
    }

    protected HiLiteHandler getHiLiteHandler() {
        return m_hiLiteHandler;
    }

}