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
 *   23.02.2007 (Rosaria Silipo): created
 */
package org.knime.timeseries.node.display.barchart;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.util.DataArray;
import org.knime.base.node.viz.plotter.AbstractDrawingPane;
import org.knime.base.node.viz.plotter.AbstractPlotterProperties;
import org.knime.base.node.viz.plotter.props.ColorLegendTab;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.node.property.hilite.KeyEvent;
import org.knime.timeseries.node.display.FinancialDotInfo;
import org.knime.timeseries.node.display.FinancialDotInfoArray;
import org.knime.timeseries.node.display.FinancialShapeFactory;
import org.knime.timeseries.node.display.timeplot.TimePlotter;

/**
 * Plots the values of all selected numeric columns as lines in a plot. 
 * Points n X-axis are dates from a column chosen in the dialog window. 
 * 
 * The <code>TimePlotter</code> extends the 
 * {@link org.knime.base.node.viz.plotter.line.LinePlotter} to inherit 
 * the missing point interpolation functionality.
 * Like LinePlotter, it determines the overall minimum and maximum 
 * of the selected columns, 
 * creates the referring coordinates and calculates the mapped values. The so 
 * mapped data points are passed to the drawing pane in one large
 * {@link org.knime.base.node.viz.plotter.scatter.DotInfoArray}. The 
 * {@link org.knime.base.node.viz.plotter.line.LinePlotterDrawingPane} connects
 * the points by lines. Due to performance issues it initially plots the first 
 * five numeric columns.
 * 
 * @author Rosaria Silipo
 */
public class BarChartPlotter extends TimePlotter {
        
    private List<Integer> m_columns2Draw;
    private Set<String> m_columnNames;
    private Map<String, Color>m_colorMapping;

    private int m_selectedXColIndex;
    
    private int m_highColIdx = -1;
    private int m_lowColIdx = -1;
    private int m_closeColIdx = -1;
    private int m_openColIdx = -1;

    /**
     * The construction kit constructor. Registers all necessary listeners.
     * 
     * @param panel the drawing panel
     * @param properties the properties
     */
    public BarChartPlotter(final AbstractDrawingPane panel, 
            final AbstractPlotterProperties properties) {
        
        super(panel, properties);

        int dotSize = 2;
        setDotSize(dotSize);
        m_columns2Draw = new ArrayList<Integer>();
        
        
        if (getProperties() instanceof BarChartProperties) {

            final ColorLegendTab legend 
                = ((BarChartProperties)getProperties()).getColorLegend();
            legend.addChangeListener(new ChangeListener() {
    
                /**
                 * @see javax.swing.event.ChangeListener#stateChanged(
                 * javax.swing.event.ChangeEvent)
                 */
                    public void stateChanged(final ChangeEvent e) {
                        // get the mapping and update model
                        m_colorMapping = legend.getColorMapping();
                        updatePaintModel();
                    }        
            });
        }        
    }
    
    /**
     * Default constructor.
     *
     */
    public BarChartPlotter() {
        this(new BarChartDrawingPane(), new BarChartProperties());
    }
    
    /**
     * Sets color mapping and column selection to <code>null</code>.
     * 
     * @see org.knime.base.node.viz.plotter.AbstractPlotter#reset()
     */
    @Override
    public void reset() {
        m_colorMapping = null;
        m_columnNames = null;
        m_columns2Draw.clear();
    }
    
    /**
     * Creates the color mapping by dividing the hue circle of the HSB color 
     * model by the number of selected columns, calculates the coordinates by 
     * determining the overall minimum and maximum values of the selected 
     * columns and maps the data points to the resulting screen coordinates.
     * 
     * @see org.knime.base.node.viz.plotter.AbstractPlotter#updatePaintModel()
     */
    @Override
    public void updatePaintModel() {
        if (getDataProvider() != null
                && getDataProvider().getDataArray(0) != null) {
            // draw the points and add the lines
            DataArray array = getDataProvider().getDataArray(0);
            if (m_columnNames == null) {
               initColumnNames(array);
            }
            // create the color mapping if necessary
            if (m_colorMapping == null) {
                m_colorMapping = new LinkedHashMap<String, Color>();
                m_colorMapping.put("Bar Chart", 
                        Color.getHSBColor(1, 1, 1));
            }
            calculateCoordinates(array);
            calculateDots();
            if (getProperties() instanceof BarChartProperties) {
               ((BarChartProperties)getProperties()).updateColorLegend(
                    m_colorMapping);
            }
            getDrawingPane().repaint();
        }
    }
    
    /**
     * Selects the first five numeric columns. If there are some columns left,
     * the column filter tab is set to be on top.
     * 
     * @param array the data to visualize
     */
    private void initColumnNames(final DataArray array) {
        m_columnNames = new LinkedHashSet<String>();
        int colIdx = 0;
        for (DataColumnSpec colSpec : array.getDataTableSpec()) {
            if (colSpec.getType().isCompatible(DoubleValue.class)) {
                if (colSpec.getName().equalsIgnoreCase("High")) {
                   m_columnNames.add(colSpec.getName());
                   m_columns2Draw.add(colIdx++);
                   m_highColIdx = colIdx;
                } else if (colSpec.getName().equalsIgnoreCase("Low")) {
                   m_columnNames.add(colSpec.getName());
                   m_columns2Draw.add(colIdx++);
                   m_lowColIdx = colIdx;
                } else if (colSpec.getName().equalsIgnoreCase("Close")) {
                    m_columnNames.add(colSpec.getName());
                    m_columns2Draw.add(colIdx++);
                    m_closeColIdx = colIdx; 
                } else if (colSpec.getName().equalsIgnoreCase("Open")) {
                    m_columnNames.add(colSpec.getName());
                    m_columns2Draw.add(colIdx++);
                    m_openColIdx = colIdx; 
                }
            }
        } 
   }
    
    /**
     * Calculates the screen coordinates (dots) for the lines and puts them in 
     * a large {@link org.knime.base.node.viz.plotter.scatter.DotInfoArray}, 
     * which is passed to the 
     * {@link org.knime.base.node.viz.plotter.line.LinePlotterDrawingPane}.
     *
     */
    @Override
    protected void calculateDots() {
        if (!(getDrawingPane() instanceof BarChartDrawingPane)) {
            return;
        }
        if (m_columnNames == null) {
            return;
        }
        if (getDataProvider() != null
                && getDataProvider().getDataArray(0) != null) {
            DataArray array = getDataProvider().getDataArray(0);
            int nrOfRows = array.size();
            
            // set the empty dots to delete the old ones 
            // if we have no columns to display
            ((BarChartDrawingPane)getDrawingPane()).setFinancialDotInfoArray(
                    new FinancialDotInfoArray(new FinancialDotInfo[0]));
           
            // first store them in a list to avoid keep tracking of indices
            List<FinancialDotInfo> dotList = new ArrayList<FinancialDotInfo>();

            Color c = m_colorMapping.get("Bar Chart");
            if (c == null) {
                c = Color.black;
            }
            ColorAttr color = ColorAttr.getInstance(c);
            
            // store the last point with valid value for interpolation
/*            Point p1 = new Point(-1, -1);
            Point p2;
            List<FinancialDotInfo> missingValues = new ArrayList<FinancialDotInfo>();
            */
            // create the dots
            for (int row = 0; row < nrOfRows; row++) {
                    
                 DataCell highDataCell = array.getRow(row).getCell(m_highColIdx);
                 DataCell lowDataCell = array.getRow(row).getCell(m_lowColIdx);
                 DataCell closeDataCell = array.getRow(row).getCell(m_closeColIdx);
                 DataCell openDataCell = array.getRow(row).getCell(m_openColIdx);
                                    
                 int yHigh = -1;
                 int yLow = -1;
                 int yClose = -1;
                 int yOpen = -1;
                    
                 FinancialDotInfo dot;
                 DataCell dc = 
                        array.getRow(row).getCell(m_selectedXColIndex);
                    
                 int x = (int)getXAxis().getCoordinate()
                    .calculateMappedValue(dc, getDrawingPaneDimension().width, 
                            true);
                 
                 if (!highDataCell.isMissing() && !lowDataCell.isMissing()
                          && !closeDataCell.isMissing() 
                          && !openDataCell.isMissing()) {
                        yHigh = (int)getYAxis().getCoordinate()
                        .calculateMappedValue(highDataCell,
                                getDrawingPaneDimension().height, true);
                        
                        yLow = (int)getYAxis().getCoordinate()
                        .calculateMappedValue(lowDataCell,
                                getDrawingPaneDimension().height, true);
                        
                        yClose = (int)getYAxis().getCoordinate()
                           .calculateMappedValue(closeDataCell,
                                getDrawingPaneDimension().height, true);

                        yOpen = (int)getYAxis().getCoordinate()
                        .calculateMappedValue(openDataCell,
                             getDrawingPaneDimension().height, true);
                                       
                       dot = new FinancialDotInfo(x, 
                            (int)getScreenYCoordinate(yOpen), (int)getScreenYCoordinate(yClose),
                            (int)getScreenYCoordinate(yHigh), (int)getScreenYCoordinate(yLow),
                            array.getRow(row).getKey(),
                            delegateIsHiLit(array.getRow(row).getKey()),
                                    color, 1, row);
                       dot.setShape(
                         FinancialShapeFactory.getShape(FinancialShapeFactory.VERTICAL_BAR));

                       dot.setXDomainValue(dc);
                       
                       dot.setOpenPriceDomainValue(openDataCell);
                       dot.setClosePriceDomainValue(closeDataCell);
                       dot.setHighPriceDomainValue(highDataCell);
                       dot.setLowPriceDomainValue(lowDataCell);
                       
                       dotList.add(dot);         
                    }
                }
            FinancialDotInfo[] dots = new FinancialDotInfo[dotList.size()];
            dotList.toArray(dots);
            ((BarChartDrawingPane)getDrawingPane()).setFinancialDotInfoArray(
                    new FinancialDotInfoArray(dots));
        }
    }
    
    /**
     * Determines the overall minimum and maximum value of all selected columns.
     * 
     * @param array the data to visualize
     */
    private void calculateCoordinates(final DataArray array) {
        Set<DataCell> rowKeys = new LinkedHashSet<DataCell>(array.size());
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (DataRow row : array) {
            rowKeys.add(row.getCell(m_selectedXColIndex));
            for (String column : m_columnNames) {
                int colIdx = array.getDataTableSpec().findColumnIndex(column);
                if (colIdx == -1) {
                    initColumnNames(array);
                    calculateCoordinates(array);
                    break;
                }
                DataCell cell = row.getCell(colIdx);
                if (cell.isMissing()) {
                    continue;
                }
                double value = ((DoubleValue)cell).getDoubleValue();
                minY = Math.min(minY, value);
                maxY = Math.max(maxY, value);
            }
        }
        createNominalXCoordinate(rowKeys);
        setPreserve(false);
        createYCoordinate(minY, maxY);
        setPreserve(true);
    }    

    private void changeHiliteStateTo(final Set<RowKey> rowIds,
            final boolean state) {
        if (state) {
            delegateHiLite(rowIds);
        } else {
            delegateUnHiLite(rowIds);
        }
        if (isScatterPlotterDrawingPane()) {
            FinancialDotInfoArray dots =
                    getBarChartPlotterDrawingPane().getFinancialDotInfoArray();
            if (dots == null) {
                return;
            }
            for (FinancialDotInfo dot : dots.getDots()) {
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
    @Override
    public void unHiLiteAll(final KeyEvent evt) {
        if (isScatterPlotterDrawingPane()) {
            FinancialDotInfoArray dotArray =
                    getBarChartPlotterDrawingPane().getFinancialDotInfoArray();
            if (dotArray == null) {
                return;
            }
            for (FinancialDotInfo dot : dotArray.getDots()) {
                dot.setHiLit(false);
            }
            updatePaintModel();
        }
    }

    /**
     * @see org.knime.base.node.viz.plotter.AbstractPlotter#hiLite(
     *      org.knime.core.node.property.hilite.KeyEvent)
     */
    @Override
    public void hiLite(final KeyEvent event) {
        changeHiliteStateTo(event.keys(), true);
    }

    /**
     * @see org.knime.base.node.viz.plotter.AbstractPlotter#unHiLite(
     *      org.knime.core.node.property.hilite.KeyEvent)
     */
    @Override
    public void unHiLite(final KeyEvent event) {
        changeHiliteStateTo(event.keys(), false);
    }

    private BarChartDrawingPane getBarChartPlotterDrawingPane() {
        return (BarChartDrawingPane)getDrawingPane();
    }

}
