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
 *   03.10.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.scattermatrix;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.util.DataArray;
import org.knime.base.node.viz.plotter.PlotterMouseListener;
import org.knime.base.node.viz.plotter.columns.MultiColumnPlotterProperties;
import org.knime.base.node.viz.plotter.scatter.DotInfo;
import org.knime.base.node.viz.plotter.scatter.ScatterPlotter;
import org.knime.base.node.viz.plotter.scatter.ScatterPlotterDrawingPane;
import org.knime.base.util.coordinate.Coordinate;
import org.knime.base.util.coordinate.CoordinateMapping;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.util.ColumnFilterPanel;

/**
 * Creates the scatter matrix elements as rectangles with a x and y coordinates,
 * passes these 
 * {@link org.knime.base.node.viz.plotter.scattermatrix.ScatterMatrixElement}s 
 * to the 
 * {@link 
 * org.knime.base.node.viz.plotter.scattermatrix.ScatterMatrixDrawingPane}.
 * The x and y axis of the plotter axes are nominal with the column names as 
 * values.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class ScatterMatrixPlotter extends ScatterPlotter {
    
//    private static final NodeLogger LOGGER = NodeLogger.getLogger(
//            ScatterMatrixPlotter.class);

    /** space between the single scatter matrix elements. */
    public static final int GAP = 5;

    /** The dot size for the scatter matrix dots. */
    public static final int DOT_SIZE = 2;

    private static final int DEFAULT_NR_COLS = 3;
    
    /** The space at left and right in %. */
    private static final double V_MARGIN_FACTOR = 0.08;
    
    /** The space at top and bottom in %. */
    private static final double H_MARGIN_FACTOR = 0.1;
    

    private Set<String> m_selectedColumns;
    
    private List<Coordinate> m_coordinates;
    
    private int m_matrixElementWidth;
    
    private int m_hMargin;
    
    private int m_vMargin;
    

    /**
     * 
     * 
     */
    public ScatterMatrixPlotter() {
        super(new ScatterMatrixDrawingPane(), new ScatterMatrixProperties());
        /* ------------- listener ------------ */
        // colunm selection
        final ColumnFilterPanel colFilter = 
        ((ScatterMatrixProperties)getProperties()).getColumnFilter();
            colFilter.addChangeListener(new ChangeListener() {
                /**
                 * {@inheritDoc}
                 */
                public void stateChanged(final ChangeEvent e) {
                    m_selectedColumns = colFilter.getIncludedColumnSet();
                    updatePaintModel();
                    getDrawingPane().repaint();
                }
            });
            // dot size
            ((ScatterMatrixProperties)getProperties()).addDotSizeChangeListener(
                    new ChangeListener() {
                        /**
                         * {@inheritDoc}
                         */
                        public void stateChanged(final ChangeEvent e) {
                            setDotSize(
                               ((ScatterMatrixProperties)getProperties())
                               .getDotSize());
                            updatePaintModel();
                        }
                        
                    });
            // jitter
            ((ScatterMatrixProperties)getProperties()).getJitterSlider()
                .setValue(getJitterRate() * 10);
            ((ScatterMatrixProperties)getProperties()).getJitterSlider()
                .addMouseListener(
                    new MouseAdapter() {

                        /**
                         * {@inheritDoc}
                         */
                        @Override
                        public void mouseReleased(final MouseEvent e) {
                            int jitter = 
                                ((ScatterMatrixProperties)getProperties())
                                    .getJitterSlider().getValue();
                            setJitterRate(jitter / 10);
                            updatePaintModel();
                        }

                    });
            ((ScatterPlotterDrawingPane)getDrawingPane()).setDotSize(DOT_SIZE);
            addMouseListener(new TransformationMouseListener());
    }

    
    /**
     * Resets the selected columns.
     * 
     * @see org.knime.base.node.viz.plotter.scatter.ScatterPlotter#reset()
     */
    @Override
    public void reset() {
        super.reset();
        m_selectedColumns = null;
    }

    /**
     * Creates the nominal coordinates with the selected column names, 
     * calculates the surrounding rectangle for the scatter matrix elements, 
     * then maps the points to the screen coordinates, associates the 
     * {@link org.knime.base.node.viz.plotter.scatter.DotInfo}s with the 
     * referring
     * {@link 
     * org.knime.base.node.viz.plotter.scattermatrix.ScatterMatrixElement}
     * and passes them to the 
     * {@link 
     * org.knime.base.node.viz.plotter.scattermatrix.ScatterMatrixDrawingPane}.
     * The {@link 
     * org.knime.base.node.viz.plotter.scattermatrix.ScatterMatrixDrawingPane}
     * then extracts the dots from the 
     *{@link org.knime.base.node.viz.plotter.scattermatrix.ScatterMatrixElement}
     * and stores them in a 
     * {@link org.knime.base.node.viz.plotter.scatter.DotInfoArray}.
     * 
     * @see org.knime.base.node.viz.plotter.AbstractPlotter#updatePaintModel()
     */
    @Override
    public synchronized void updatePaintModel() {
        // clear the drawing pane
        ((ScatterMatrixDrawingPane)getDrawingPane()).setDotInfoArray(null);
        ((ScatterMatrixDrawingPane)getDrawingPane()).setScatterMatrixElements(
                null);

        // get the number of columns c
        if (getDataProvider() == null
                || getDataProvider().getDataArray(getDataArrayIdx()) == null) {
            return;
        }
        
        DataArray data = getDataProvider().getDataArray(getDataArrayIdx());
        // get the first columns
        if (m_selectedColumns == null) {
            m_selectedColumns = new LinkedHashSet<String>();
            for (int i = 0; i < DEFAULT_NR_COLS 
                && i < data.getDataTableSpec().getNumColumns(); i++) {
                // add them to selected columns
                String colName = data.getDataTableSpec().getColumnSpec(i)
                .getName();
                m_selectedColumns.add(colName);
            }
            if (data.getDataTableSpec().getNumColumns() > DEFAULT_NR_COLS) {
                getProperties().setSelectedIndex(MultiColumnPlotterProperties
                        .COLUMN_FILTER_IDX);
            }
            ((ScatterMatrixProperties)getProperties()).updateColumnSelection(
                    data.getDataTableSpec(), m_selectedColumns);
        }
        if (m_selectedColumns.size() == 0) {
            getDrawingPane().repaint();
            return;
        }
        Set<DataCell>selectedColumnCells = new LinkedHashSet<DataCell>();
        m_coordinates = new ArrayList<Coordinate>();
        List<Integer>columnIndices = new ArrayList<Integer>();
        for (String name : m_selectedColumns) {
            int idx = data.getDataTableSpec().findColumnIndex(name);
            if (idx >= 0) {
                selectedColumnCells.add(new StringCell(name));
                DataColumnSpec colSpec = data.getDataTableSpec()
                    .getColumnSpec(idx);
                columnIndices.add(idx);
                Coordinate coordinate = Coordinate.createCoordinate(colSpec);
                m_coordinates.add(coordinate);
            }
        }

        // create coordinates with the column names
        createNominalXCoordinate(selectedColumnCells);
        // reverse list for y axis...
        List<DataCell> reverseList = new ArrayList<DataCell>(
                selectedColumnCells);
        Collections.reverse(reverseList);
        createNominalYCoordinate(new LinkedHashSet<DataCell>(reverseList));
        
        m_hMargin = (int)(getDrawingPaneDimension().height * H_MARGIN_FACTOR);
        m_vMargin = (int)(getDrawingPaneDimension().width * V_MARGIN_FACTOR);
        ((ScatterMatrixDrawingPane)getDrawingPane()).setHorizontalMargin(
                m_hMargin);
        ((ScatterMatrixDrawingPane)getDrawingPane()).setVerticalMargin(
                m_vMargin);
        // set the offset for the column axes
        getXAxis().setStartTickOffset(m_vMargin);
        getYAxis().setStartTickOffset(m_hMargin);

        int nrOfColumns = selectedColumnCells.size();
        // and update the properties
        int width = (getDrawingPaneDimension().width 
                - (nrOfColumns * GAP) - (2 * m_vMargin)) / nrOfColumns;
        m_matrixElementWidth = width;
        int height = (getDrawingPaneDimension().height 
                - (nrOfColumns * GAP) - (2 * m_hMargin)) / nrOfColumns;
        int rowNr = 0;
        ScatterMatrixElement[][] matrixElements 
            = new ScatterMatrixElement[nrOfColumns][nrOfColumns];
        for (DataRow row : data) {
            for (int i = 0; i < nrOfColumns; i++) {
                for (int j = 0; j < nrOfColumns; j++) {
                    Coordinate xCoordinate = m_coordinates.get(i);
                    Coordinate yCoordinate = m_coordinates.get(j);
                    DataCell xValue = row.getCell(columnIndices.get(i));
                    DataCell yValue = row.getCell(columnIndices.get(j));
                    int x = -1;
                    int y = -1;
                    int xOffset = (i * (width + GAP)) + m_vMargin;
                    int yOffset = (j * (height + GAP)) + m_hMargin;
                    ScatterMatrixElement matrixElement = matrixElements[i][j];
                    if (matrixElement == null) {
                        matrixElement = new ScatterMatrixElement(
                                new Point(xOffset, yOffset), width, 
                                height, xCoordinate, yCoordinate);
                        matrixElements[i][j] = matrixElement;
                    }

                    if (!xValue.isMissing()) {
                        x = (int)xCoordinate.calculateMappedValue(xValue,
                                width - (2 * getDotSize()), true);
                        // offset
                        x += xOffset + getDotSize();
                    }
                    if (!yValue.isMissing()) {
                        y = (int)(height - yCoordinate.calculateMappedValue(
                                yValue, height - (2 * getDotSize()), true));
                        // v offset
                        y += yOffset - getDotSize();
                    }
                    boolean hilite = delegateIsHiLit(row.getKey());
                    if (!hilite && isHideMode()) {
                        continue;
                    } 
                    if (isHideMode() && hilite) {
                        hilite = false;
                    }
                    DotInfo dot = new DotInfo(x, y, row.getKey(),
                            hilite, data
                                    .getDataTableSpec().getRowColor(row), data
                                    .getDataTableSpec().getRowSizeFactor(row),
                            rowNr);
                    dot.setShape(data.getDataTableSpec().getRowShape(row));
                    dot.setXDomainValue(xValue);
                    dot.setYDomainValue(yValue);
                    matrixElement.addDot(dot);
//                    dotList.add(dot);
                } // j
            } // i
            rowNr++;
        } // rows
        // jitter
        jitter(matrixElements);
        ((ScatterMatrixDrawingPane)getDrawingPane()).setScatterMatrixElements(
                matrixElements);
        getDrawingPane().repaint();
    }
    
    private void jitter(final ScatterMatrixElement[][] matrixElements) {
        for (int i = 0; i < matrixElements.length; i++) {
            for (int j = 0; j < matrixElements[i].length; j++) {
                ScatterMatrixElement element = matrixElements[i][j];
            	// matrix element might be null (if no rows available) since 
            	// the array is initialized with column length
            	if (element == null) {
            		continue;
            	}                
                Coordinate xCoordinate = element.getXCoordinate();
                Coordinate yCoordinate = element.getYCoordinate();
                if ((xCoordinate.isNominal() || yCoordinate.isNominal())) {
                    // for jittering only 90% of the available space are used
                    // to avoid that the dots of different nominal values 
                    // touches each other
                    int width = element.getWidth();
                    int height = element.getHeight();
                    List<DotInfo>dotList = element.getDots();
                    DotInfo[] dots = new DotInfo[dotList.size()];
                    dotList.toArray(dots);
                    int xAxisJitterRange = (int)(Math.round(xCoordinate
                            .getUnusedDistBetweenTicks(width)) * 0.9);
                    int yAxisJitterRange = (int)(Math.round(yCoordinate
                            .getUnusedDistBetweenTicks(height)) * 0.9);
                    jitterDots(dots, xAxisJitterRange, yAxisJitterRange);
                    matrixElements[i][j].setDots(Arrays.asList(dots));
                }
            }
        }
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public void updateSize() {
//        super.updateSize();
        updatePaintModel();
    }
    
    /**
     * Mouse listener for changing the column position.
     * 
     * @author Fabian Dill, University of Konstanz
     */
    public class TransformationMouseListener extends PlotterMouseListener {
        
        private String m_selectedColumn;
        
        private boolean m_dragged = false;
        
        private final Cursor m_hand = new Cursor(Cursor.HAND_CURSOR);
        
        private final Cursor m_default = Cursor.getDefaultCursor();
        
        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "Transformation";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Cursor getCursor() {
            return super.getCursor();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void mouseDragged(final MouseEvent e) {
            m_dragged = true;
            getDrawingPane().setCursor(m_hand);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void mousePressed(final MouseEvent e) {
            // find out which column was clicked
            CoordinateMapping[] mappings = getXAxis().getCoordinate()
                .getTickPositions(getDrawingPaneDimension().width, true);
                int bucket = (e.getX() - m_vMargin) / m_matrixElementWidth;
                m_selectedColumn = mappings[bucket].getDomainValueAsString();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void mouseReleased(final MouseEvent e) {
            // here change order of selected columns
            // first get new position
            if (m_dragged) {
                int newBucket = (e.getX() - m_vMargin) / m_matrixElementWidth;
                List<String> columns = new ArrayList<String>();
                columns.addAll(m_selectedColumns);
                columns.remove(m_selectedColumn);
                columns.add(newBucket, m_selectedColumn);
                m_selectedColumns = new LinkedHashSet<String>();
                m_selectedColumns.addAll(columns);
                updatePaintModel();
            }
            m_dragged = false;
            getDrawingPane().setCursor(m_default);
        }
        
    }


}
