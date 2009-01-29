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
 *   13.02.2007 (Rosaria Silipo): created
 *   This class implments a line plotter but reports the price dates 
 *   on the x-axis.
 */
package org.knime.timeseries.node.display.timeplot;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JSpinner;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.util.DataArray;
import org.knime.base.node.viz.plotter.AbstractDrawingPane;
import org.knime.base.node.viz.plotter.AbstractPlotterProperties;
import org.knime.base.node.viz.plotter.columns.MultiColumnPlotterProperties;
import org.knime.base.node.viz.plotter.line.LinePlotter;
import org.knime.base.node.viz.plotter.props.ColorLegendTab;
import org.knime.base.node.viz.plotter.scatter.DotInfo;
import org.knime.base.node.viz.plotter.scatter.DotInfoArray;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.node.util.ColumnFilterPanel;

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
public class TimePlotter extends LinePlotter {

//    private static final NodeLogger LOGGER = NodeLogger.getLogger(
//            LinePlotter.class);
    
    /** Initial dot size. */
    //public static final int SIZE = 4;

    private static final int DEFAULT_NR_COLS = 5;
    
    private Map<String, Color>m_colorMapping;
    
    private List<Integer> m_columns2Draw;
    
    private Set<String> m_columnNames;

    private boolean m_interpolate;
    
    private int m_selectedXColIndex;

    
    /**
     * The construction kit constructor. Registers all necessary listeners.
     * 
     * @param panel the drawing panel
     * @param properties the properties
     */
    public TimePlotter(final AbstractDrawingPane panel, 
            final AbstractPlotterProperties properties) {
        super(panel, properties);
        setDotSize(SIZE);
        m_columns2Draw = new ArrayList<Integer>();
        if (getProperties() instanceof TimePlotterProperties) {
            final ColumnFilterPanel columnFilter = ((TimePlotterProperties)
                    getProperties()).getColumnFilter(); 
            columnFilter.addChangeListener(
                    new ChangeListener() {
                       
                        /**
                         * @see javax.swing.event.ChangeListener#stateChanged(
                         * javax.swing.event.ChangeEvent)
                         */
                       public void stateChanged(final ChangeEvent e) {
                            if (getDataProvider() != null 
                                && getDataProvider().getDataArray(0) != null) {
                                DataTableSpec spec = getDataProvider()
                                    .getDataArray(0).getDataTableSpec();
                                m_columnNames = columnFilter
                                    .getIncludedColumnSet();
                                m_columns2Draw.clear();
                                for (String name : m_columnNames) {
                                    m_columns2Draw.add(spec.findColumnIndex(
                                            name));
                                }
                                updatePaintModel();
                            }
                        }
                    });
            final ColorLegendTab legend 
                = ((TimePlotterProperties)getProperties()).getColorLegend();
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
            final JCheckBox box = ((TimePlotterProperties)getProperties())
                .getInterpolationCheckBox();
            box.addItemListener(new ItemListener() {
            
                /**
                 * @see java.awt.event.ItemListener#itemStateChanged(
                 * java.awt.event.ItemEvent)
                 */
        
                public void itemStateChanged(final ItemEvent e) {
                    setInterpolation(box.isSelected());
                    updatePaintModel();
                }
            });
            final JCheckBox showDotsBox 
                = ((TimePlotterProperties)getProperties()).getShowDotsBox();
            showDotsBox.addItemListener(new ItemListener() {
           
                /**
                 * @see java.awt.event.ItemListener#itemStateChanged(
                 * java.awt.event.ItemEvent)
                 */
                public void itemStateChanged(final ItemEvent e) {
                    ((TimePlotterDrawingPane)getDrawingPane()).setShowDots(
                            showDotsBox.isSelected());
                    getDrawingPane().repaint();
                }
            });
            final JSpinner dotSize = ((TimePlotterProperties)getProperties())
                .getDotSizeSpinner();
            dotSize.addChangeListener(new ChangeListener() {
           
                /**
                 * @see javax.swing.event.ChangeListener#stateChanged(
                 * javax.swing.event.ChangeEvent)
                 */
               public void stateChanged(final ChangeEvent e) {
                    setDotSize((Integer)dotSize.getValue());
                    getDrawingPane().repaint();
                }
            });
            final JSpinner thickness = ((TimePlotterProperties)getProperties())
                .getThicknessSpinner();
            thickness.addChangeListener(new ChangeListener() {
            
            /**
             * @see javax.swing.event.ChangeListener#stateChanged(
             * javax.swing.event.ChangeEvent)
             */
            public void stateChanged(final ChangeEvent e) {
                ((TimePlotterDrawingPane)getDrawingPane()).setLineThickness(
                        (Integer)thickness.getValue());
                getDrawingPane().repaint();
            }
            
        });
        }
        
    }
    
    /**
     * Default constructor.
     *
     */
    public TimePlotter() {
        this(new TimePlotterDrawingPane(), new TimePlotterProperties());
    }
    
    /**
     * sets index value for column to give lavbels on x-axis.
     * @param selectedXColIndex index of column to give x labels
     */
    public void setXColumnIndex(final int selectedXColIndex) {
        m_selectedXColIndex = selectedXColIndex;
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
                float segment = 360f / (float)m_columns2Draw.size();
                int colNr = 0;
                for (DataColumnSpec colSpec : getDataProvider().getDataArray(0)
                        .getDataTableSpec()) {
                    if (colSpec.getType().isCompatible(DoubleValue.class)) {
                        float h = (colNr * segment) / 360f;
                        m_colorMapping.put(colSpec.getName(), 
                                Color.getHSBColor(h, 1, 1));
                        colNr++;
                    }
                }
            }
            calculateCoordinates(array);
            calculateDots();
            // if we have line plotter properties update them
            if (getProperties() instanceof TimePlotterProperties) {
                ((TimePlotterProperties)getProperties()).updateColumnSelection(
                        array.getDataTableSpec(), m_columnNames);
                ((TimePlotterProperties)getProperties()).updateColorLegend(
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
                m_columnNames.add(colSpec.getName());
                m_columns2Draw.add(colIdx++);
                if (colIdx >= DEFAULT_NR_COLS) {
                    getProperties().setSelectedIndex(
                            MultiColumnPlotterProperties.COLUMN_FILTER_IDX);
                    break;
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
        if (!(getDrawingPane() instanceof TimePlotterDrawingPane)) {
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
            ((TimePlotterDrawingPane)getDrawingPane()).setDotInfoArray(
                    new DotInfoArray(new DotInfo[0]));
           
            // first store them in a list to avoid keep tracking of indices
            List<DotInfo> dotList = new ArrayList<DotInfo>();
            int colNr = 0;
            for (String col : m_columnNames) {
                int colIdx = array.getDataTableSpec().findColumnIndex(col);
                Color c = m_colorMapping.get(col);
                if (c == null) {
                    c = Color.black;
                }
                ColorAttr color = ColorAttr.getInstance(c);
                // store the last point with valid value for interpolation
                Point p1 = new Point(-1, -1);
                Point p2;
                List<DotInfo> missingValues = new ArrayList<DotInfo>();
                // create the dots
                for (int row = 0; row < nrOfRows; row++) {
                    DataCell cell = array.getRow(row).getCell(colIdx);
                    int y = -1;
                    DotInfo dot;
                    
                    DataCell dc = 
                        array.getRow(row).getCell(m_selectedXColIndex);
                    
                    int x = (int)getXAxis().getCoordinate()
                    .calculateMappedValue(dc, getDrawingPaneDimension().width, 
                            true);
                    if (!cell.isMissing()) {
                        y = (int)getYAxis().getCoordinate()
                        .calculateMappedValue(cell,
                                getDrawingPaneDimension().height, true);
                        if (missingValues.size() > 0) {
                            // we have some missing values in between, 
                            // thus we have to interpolate
                            p2 = new Point(x, y);
                            DotInfo[] interpolated = interpolate(p1, p2, 
                                    missingValues);
                            // and add them
                            for (DotInfo p : interpolated) {
                                dotList.add(p);
                            }
                            // and clear the list again
                            missingValues.clear();
                        }
                        p1 = new Point(x, y);
                        dot = new DotInfo(x, (int)getScreenYCoordinate(y), 
                                array.getRow(row).getKey(),
                                delegateIsHiLit(array.getRow(row).getKey()),
                                        color, 1, row);
//                        dot.setXDomainValue(array.getRow(row).getKey().getId());
                        dot.setXDomainValue(dc);
                        dot.setYDomainValue(cell);
                        dotList.add(dot);
                    } else if (!m_interpolate) {
//                        LOGGER.debug("missing value");
                        dot = new DotInfo(x, y, array.getRow(row).getKey(),
                            false, color, 1, row);
                        dotList.add(dot);
                    } else {
                        // interpolate
                        dot = new DotInfo(x, y, array.getRow(row).getKey(),
                                false, color, 1, row);
                        missingValues.add(dot);
                    }
                }
                colNr++;
            }
            DotInfo[] dots = new DotInfo[dotList.size()];
            dotList.toArray(dots);
            ((TimePlotterDrawingPane)getDrawingPane()).setNumberOfLines(
                    nrOfRows);
            ((TimePlotterDrawingPane)getDrawingPane()).setDotInfoArray(
                    new DotInfoArray(dots));
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
//            rowKeys.add(row.getKey().getId());
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

    /**
     * @see org.knime.base.node.viz.plotter.scatter.ScatterPlotter#updateSize()
     */
    @Override
    public void updateSize() {
        calculateDots();
    }
   
    
    
}
