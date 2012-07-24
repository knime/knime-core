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
 * {@link org.knime.base.node.viz.plotter.scatter.ScatterPlotter} to inherit the
 * dot functionality and the hiliting behavior. It determines the overall
 * minimum and maximum of the selected columns, creates the referring
 * coordinates and calculates the mapped values. The so mapped data points are
 * passed to the drawing pane in one large
 * {@link org.knime.base.node.viz.plotter.scatter.DotInfoArray}. The
 * {@link org.knime.base.node.viz.plotter.line.LinePlotterDrawingPane} connects
 * the points by lines. Due to performance issues it initially plots the first
 * five numeric columns.
 *
 * @author Fabian Dill, University of Konstanz
 */
public class LinePlotter extends ScatterPlotter {

    // private static final NodeLogger LOGGER = NodeLogger.getLogger(
    // LinePlotter.class);

    /** Initial dot size. */
    public static final int SIZE = 4;

    private static final int DEFAULT_NR_COLS = 5;

    private Map<String, Color> m_colorMapping;

    private List<Integer> m_columns2Draw;

    private Set<String> m_columnNames;

    private boolean m_interpolate;

    /**
     * The construction kit constructor. Registers all necessary listeners.
     *
     * @param panel
     *            the drawing panel
     * @param properties
     *            the properties
     */
    public LinePlotter(final AbstractDrawingPane panel,
            final AbstractPlotterProperties properties) {
        super(panel, properties);
        setDotSize(SIZE);
        m_columns2Draw = new ArrayList<Integer>();
        if (getProperties() instanceof LinePlotterProperties) {
            final ColumnFilterPanel columnFilter
                = ((LinePlotterProperties) getProperties()).getColumnFilter();
            columnFilter.addChangeListener(new ChangeListener() {
                /**
                 * {@inheritDoc}
                 */
                public void stateChanged(final ChangeEvent e) {
                    if (getDataProvider() != null
                            && getDataProvider()
                                    .getDataArray(getDataArrayIdx()) != null) {
                        DataTableSpec spec = getDataProvider().getDataArray(
                                getDataArrayIdx()).getDataTableSpec();
                        m_columnNames = columnFilter.getIncludedColumnSet();
                        m_columns2Draw.clear();
                        for (String name : m_columnNames) {
                            m_columns2Draw.add(spec.findColumnIndex(name));
                        }
                        updatePaintModel();
                    }
                }
            });
            final ColorLegendTab legend
                = ((LinePlotterProperties) getProperties()).getColorLegend();
            legend.addChangeListener(new ChangeListener() {
                /**
                 * {@inheritDoc}
                 */
                public void stateChanged(final ChangeEvent e) {
                    // get the mapping and update model
                    // TODO: replace existing entries
                    Map<String, Color> mappingUpdate = legend.getColorMapping();
                    for (Map.Entry<String, Color> entry : mappingUpdate
                            .entrySet()) {
                        m_colorMapping.put(entry.getKey(), entry.getValue());
                    }
                    updatePaintModel();
                }
            });
            final JCheckBox box = ((LinePlotterProperties) getProperties())
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
                = ((LinePlotterProperties) getProperties()).getShowDotsBox();
            showDotsBox.addItemListener(new ItemListener() {
                /**
                 * {@inheritDoc}
                 */
                public void itemStateChanged(final ItemEvent e) {
                    ((LinePlotterDrawingPane) getDrawingPane())
                            .setShowDots(showDotsBox.isSelected());
                    getDrawingPane().repaint();
                }
            });
            final JSpinner dotSize = ((LinePlotterProperties) getProperties())
                    .getDotSizeSpinner();
            dotSize.addChangeListener(new ChangeListener() {
                /**
                 * {@inheritDoc}
                 */
                public void stateChanged(final ChangeEvent e) {
                    setDotSize((Integer) dotSize.getValue());
                    updatePaintModel();
                    getDrawingPane().repaint();
                }
            });
            final JSpinner thickness = ((LinePlotterProperties) getProperties())
                    .getThicknessSpinner();
            thickness.addChangeListener(new ChangeListener() {
                /**
                 * {@inheritDoc}
                 */
                public void stateChanged(final ChangeEvent e) {
                    ((LinePlotterDrawingPane) getDrawingPane())
                            .setLineThickness((Integer) thickness.getValue());
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
     * interpolated, if false missing values will be left out and the line will
     * be interrupted.
     *
     * @param enable
     *            true if missing values should be interpolated(linear), false
     *            otherwise
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
            DataTableSpec spec = array.getDataTableSpec();
            if (m_columnNames == null) {
                initColumnNames(array);
                initializeColors(spec);
            }
            // set only displayed columns for the color mapping in the
            // color legend and drawing pane....
            Map<String, Color> displayedColors
                = new LinkedHashMap<String, Color>();
            // color mapping is initially defined for all columns
            for (String colName : m_columnNames) {
                Color c = m_colorMapping.get(colName);
                    displayedColors.put(colName, c);
            }
            displayedColors.keySet().retainAll(m_columnNames);
            calculateCoordinates(array);
            calculateDots();
            // if we have line plotter properties update them
            if (getProperties() instanceof LinePlotterProperties) {
                ((LinePlotterProperties) getProperties())
                        .updateColumnSelection(array.getDataTableSpec(),
                                m_columnNames);
                ((LinePlotterProperties) getProperties())
                        .updateColorLegend(displayedColors);
            }
            getDrawingPane().repaint();
        }
    }

    /**
     * Selects the first five numeric columns. If there are some columns left,
     * the column filter tab is set to be on top.
     *
     * @param array
     *            the data to visualize
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

    private void initializeColors(final DataTableSpec spec) {
        m_colorMapping = new LinkedHashMap<String, Color>();
        int nrOfCols = spec.getNumColumns();
        float segment = 360f / (nrOfCols);
        int colNr = 0;
        for (DataColumnSpec colSpec : spec) {
            // if new columns are added...
            String colName = colSpec.getName();
            float h = (colNr * segment) / 360f;
            Color c = Color.getHSBColor(h, 1, 1);
            m_colorMapping.put(colName, c);
            colNr++;
        }
    }

    /**
     * Calculates the screen coordinates (dots) for the lines and puts them in a
     * large {@link org.knime.base.node.viz.plotter.scatter.DotInfoArray}, which
     * is passed to the
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
            ((ScatterPlotterDrawingPane) getDrawingPane())
                    .setDotInfoArray(new DotInfoArray(new DotInfo[0]));

            // first store them in a list to avoid keep tracking of indices
            List<DotInfo> dotList = new ArrayList<DotInfo>();
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
                    int x = getMappedXValue(new StringCell(array.getRow(row)
                            .getKey().getString()));
                    if (!cell.isMissing()) {
                        y = getMappedYValue(cell);
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
                        dot = new DotInfo(x, y, array.getRow(row).getKey(),
                                delegateIsHiLit(array.getRow(row).getKey()),
                                color, 1, row);
                        dot.setXDomainValue(new StringCell(array.getRow(row)
                                .getKey().getString()));
                        dot.setYDomainValue(cell);
                        dotList.add(dot);
                    } else if (!m_interpolate) {
                        // LOGGER.debug("missing value");
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
            }
            DotInfo[] dots = new DotInfo[dotList.size()];
            dotList.toArray(dots);
            ((LinePlotterDrawingPane) getDrawingPane())
                    .setNumberOfLines(nrOfRows);
            ((ScatterPlotterDrawingPane) getDrawingPane())
                    .setDotInfoArray(new DotInfoArray(dots));
        }
    }

    /**
     * Determines the overall minimum and maximum value of all selected columns.
     *
     * @param array
     *            the data to visualize
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
                double value = ((DoubleValue) cell).getDoubleValue();
                minY = Math.min(minY, value);
                maxY = Math.max(maxY, value);
            }
        }
        createNominalXCoordinate(rowKeys);
        setPreserve(false);
        createYCoordinate(minY, maxY);
        // setPreserve(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateSize() {
        calculateDots();
    }

    /**
     * The nr of intermediate points and the last row index is used to determine
     * the x value (only the y value is interpolated).
     *
     * @param p1
     *            the domain value 1
     * @param p2
     *            the domain value 2
     * @param xValues
     *            an array containing opoints with the right x value but missing
     *            y value.
     * @return the interpolated domain values.
     */
    public DotInfo[] interpolate(final Point p1, final Point p2,
            final List<DotInfo> xValues) {
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
                m = ((p2.getY() - p1.getY()) / (p2.getX() - p1.getX()));
            }
            double y = (m * x) - (m * p1.getX()) + p1.getY();
            DotInfo newDot = xValues.get(i);
            newDot.setYCoord((int) getScreenYCoordinate(y));
            interpolated[i] = newDot;
            x++;
        }
        return interpolated;
    }

}
