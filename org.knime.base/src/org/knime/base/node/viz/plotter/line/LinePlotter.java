/*
 * ------------------------------------------------------------------
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
 *   21.09.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.line;

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
import org.knime.base.node.viz.plotter.props.ColorLegendTab;
import org.knime.base.node.viz.plotter.scatter.DotInfo;
import org.knime.base.node.viz.plotter.scatter.DotInfoArray;
import org.knime.base.node.viz.plotter.scatter.ScatterPlotter;
import org.knime.base.node.viz.plotter.scatter.ScatterPlotterDrawingPane;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.node.util.ColumnFilterPanel;

/**
 * Plots the values of all selected numeric columns as lines in a plot, where 
 * the x axis are the rows and the y axis are the values from the minimum of the
 * values in all columns to the maximum of the values of all selected columns.
 * The <code>LinePlotter</code> extends the 
 * {@link org.knime.base.node.viz.plotter.scatter.ScatterPlotter} to inherit 
 * the dot functionality and the hiliting behavior.
 * It determines the overall minimum and maximum of the selected columns, 
 * creates the referring coordinates and calculates the mapped values. The so 
 * mapped data points are passed to the drawing pane in one large
 * {@link org.knime.base.node.viz.plotter.scatter.DotInfoArray}. The 
 * {@link org.knime.base.node.viz.plotter.line.LinePlotterDrawingPane} connects
 * the points by lines. Due to performance issues it initially plots the first 
 * five numeric columns.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class LinePlotter extends ScatterPlotter {

//    private static final NodeLogger LOGGER = NodeLogger.getLogger(
//            LinePlotter.class);
    
    /** Initial dot size. */
    public static final int SIZE = 4;

    private static final int DEFAULT_NR_COLS = 5;
    
    private Map<String, Color>m_colorMapping;
    
    private List<Integer> m_columns2Draw;
    
    private Set<String> m_columnNames;

    private boolean m_interpolate;
    
    
    
    /**
     * The construction kit constructor. Registers all necessary listeners.
     * 
     * @param panel the drawing panel
     * @param properties the properties
     */
    public LinePlotter(final AbstractDrawingPane panel, 
            final AbstractPlotterProperties properties) {
        super(panel, properties);
        setDotSize(SIZE);
        m_columns2Draw = new ArrayList<Integer>();
        if (getProperties() instanceof LinePlotterProperties) {
            final ColumnFilterPanel columnFilter = ((LinePlotterProperties)
                    getProperties()).getColumnFilter(); 
            columnFilter.addChangeListener(
                    new ChangeListener() {
                        /**
                         * {@inheritDoc}
                         */
                        public void stateChanged(final ChangeEvent e) {
                            if (getDataProvider() != null 
                                && getDataProvider().getDataArray(
                                        getDataArrayIdx()) != null) {
                                DataTableSpec spec = getDataProvider()
                                    .getDataArray(getDataArrayIdx())
                                    .getDataTableSpec();
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
                = ((LinePlotterProperties)getProperties()).getColorLegend();
            legend.addChangeListener(new ChangeListener() {
                /**
                 * {@inheritDoc}
                 */
                public void stateChanged(final ChangeEvent e) {
                    // get the mapping and update model
                    m_colorMapping = legend.getColorMapping();
                    updatePaintModel();
                }        
            });
            final JCheckBox box = ((LinePlotterProperties)getProperties())
                .getInterpolationCheckBox();
            box.addItemListener(new ItemListener() {
                /**
                 * {@inheritDoc}
                 */
                public void itemStateChanged(final ItemEvent e) {
                    setInterpolation(box.isSelected());
                    updatePaintModel();
                }
            });
            final JCheckBox showDotsBox 
                = ((LinePlotterProperties)getProperties()).getShowDotsBox();
            showDotsBox.addItemListener(new ItemListener() {
                /**
                 * {@inheritDoc}
                 */
                public void itemStateChanged(final ItemEvent e) {
                    ((LinePlotterDrawingPane)getDrawingPane()).setShowDots(
                            showDotsBox.isSelected());
                    getDrawingPane().repaint();
                }
            });
            final JSpinner dotSize = ((LinePlotterProperties)getProperties())
                .getDotSizeSpinner();
            dotSize.addChangeListener(new ChangeListener() {
                /**
                 * {@inheritDoc}
                 */
                public void stateChanged(final ChangeEvent e) {
                    setDotSize((Integer)dotSize.getValue());
                    updatePaintModel();
                    getDrawingPane().repaint();
                }
            });
            final JSpinner thickness = ((LinePlotterProperties)getProperties())
                .getThicknessSpinner();
            thickness.addChangeListener(new ChangeListener() {
            /**
             * {@inheritDoc}
             */
            public void stateChanged(final ChangeEvent e) {
                ((LinePlotterDrawingPane)getDrawingPane()).setLineThickness(
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
    public LinePlotter() {
        this(new LinePlotterDrawingPane(), new LinePlotterProperties());
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
     * Missing values may be linearly interpolated, if true they will be 
     * interpolated, if false missing values will be left out and the line
     * will be interrupted.
     * 
     * @param enable true if missing values should be interpolated(linear), 
     * false otherwise
     */
    public void setInterpolation(final boolean enable) {
        m_interpolate = enable;
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
                && getDataProvider().getDataArray(getDataArrayIdx()) != null) {
            // draw the points and add the lines
            DataArray array = getDataProvider().getDataArray(getDataArrayIdx());
            if (m_columnNames == null) {
                initColumnNames(array);
            }
            // create the color mapping
            if (m_colorMapping == null) {
                m_colorMapping = new LinkedHashMap<String, Color>();
            } 
            m_colorMapping.clear();
            float segment = 360f / (float)m_columns2Draw.size();
            int colNr = 0;
            for (DataColumnSpec colSpec : getDataProvider().getDataArray(
                    getDataArrayIdx())
                    .getDataTableSpec()) {
                if (m_columns2Draw.contains(colNr)) {
                    float h = (colNr * segment) / 360f;
                    m_colorMapping.put(colSpec.getName(), 
                            Color.getHSBColor(h, 1, 1));
                }
                colNr++;
            }
            calculateCoordinates(array);
            calculateDots();
            // if we have line plotter properties update them
            if (getProperties() instanceof LinePlotterProperties) {
                ((LinePlotterProperties)getProperties()).updateColumnSelection(
                        array.getDataTableSpec(), m_columnNames);
                ((LinePlotterProperties)getProperties()).updateColorLegend(
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
    protected void calculateDots() {
        if (!(getDrawingPane() instanceof ScatterPlotterDrawingPane)) {
            return;
        }
        if (m_columnNames == null) {
            return;
        }
        if (getDataProvider() != null
                && getDataProvider().getDataArray(getDataArrayIdx()) != null) {
            DataArray array = getDataProvider().getDataArray(getDataArrayIdx());
            int nrOfRows = array.size();
            
            // set the empty dots to delete the old ones 
            // if we have no columns to display
            ((ScatterPlotterDrawingPane)getDrawingPane()).setDotInfoArray(
                    new DotInfoArray(new DotInfo[0]));
           
            // the max dot size is subtracted as a dot can vary in size
            int width = getDrawingPaneDimension().width - (getDotSize());
            int height = getDrawingPaneDimension().height - (getDotSize());
            
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
                    int x = (int)getXAxis().getCoordinate()
                    .calculateMappedValue(new StringCell(
                            array.getRow(row).getKey()
                            .getString()), width, true);
                    if (!cell.isMissing()) {
                        y = (int)getYAxis().getCoordinate()
                        .calculateMappedValue(cell, height, true);
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
                        dot.setXDomainValue(new StringCell(
                                array.getRow(row).getKey().getString()));
                        dot.setYDomainValue(cell);
                        dotList.add(dot);
                    } else if (!m_interpolate) {
//                        LOGGER.debug("missing value");
                        dot = new DotInfo(x, -1, array.getRow(row).getKey(),
                                delegateIsHiLit(array.getRow(row).getKey()), 
                                color, 1, row);
                        dotList.add(dot);
                    } else {
                        // interpolate
                        dot = new DotInfo(x, -1, array.getRow(row).getKey(),
                                delegateIsHiLit(array.getRow(row).getKey()), 
                                color, 1, row);
                        missingValues.add(dot);
                    }
                }
                // if we have missing values left, there are some 
                // un-interpolated at the end, we add them anyway
                if (!missingValues.isEmpty()) {
                    DotInfo[] interpolated = interpolate(p1, null, 
                            missingValues);
                    // and add them
                    for (DotInfo p : interpolated) {
                        dotList.add(p);
                    }
                    // and clear the list again
                    missingValues.clear();
                } 
                p1 = new Point(-1, -1);
                colNr++;
            }
            DotInfo[] dots = new DotInfo[dotList.size()];
            dotList.toArray(dots);
            ((LinePlotterDrawingPane)getDrawingPane()).setNumberOfLines(
                    nrOfRows);
            ((ScatterPlotterDrawingPane)getDrawingPane()).setDotInfoArray(
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
            rowKeys.add(new StringCell(row.getKey().getString()));
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
//        setPreserve(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateSize() {
        calculateDots();
    }
    
    /**
     * The nr of intermediate points and the last row index is used to 
     * determine the x value (only the y value is interpolated).
     * @param p1 the domain value 1
     * @param p2 the domain value 2
     * @param xValues an array containing opoints with the right x value but 
     * missing y value.
     * @return the interpolated domain values.
     */
    public DotInfo[] interpolate(final Point p1, 
            final Point p2, final List<DotInfo> xValues) {
        DotInfo[] interpolated = new DotInfo[xValues.size()];
        if (p1 == null || p2 == null || p1.getY() < 0 || p2.getY() < 0) {
            // invalid points (either beginning or end)
            // -> don't interpolate but replace with not visible points
            for (int i = 0; i < xValues.size(); i++) {
                // don't interpolate if one of the points is invalid
                    DotInfo newDot = xValues.get(i);
                    newDot.setYCoord(-1);
                    interpolated[i] = newDot;
            }
            return interpolated;
        }
        // both points are valid -> interpolate
        for (int i = 0; i < xValues.size(); i++) {
            int x = xValues.get(i).getXCoord();
            double m = 0;
            if (!p1.equals(p2)) {
                m = ((p2.getY() - p1.getY()) 
                    / (p2.getX() - p1.getX()));
            }
            double y = (m * x) - (m * p1.getX()) + p1.getY();
            DotInfo newDot = xValues.get(i);
            newDot.setYCoord((int)getScreenYCoordinate(y));
            interpolated[i] = newDot;
            x++;
        }
        return interpolated;
    }
    
    
    
}
