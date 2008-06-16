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
 *   22.09.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.parcoord;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.util.DataArray;
import org.knime.base.node.viz.plotter.AbstractPlotter;
import org.knime.base.node.viz.plotter.PlotterMouseListener;
import org.knime.base.node.viz.plotter.basic.BasicPlotter;
import org.knime.base.node.viz.plotter.columns.MultiColumnPlotterProperties;
import org.knime.base.util.coordinate.CoordinateMapping;
import org.knime.base.util.coordinate.NominalCoordinate;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.property.hilite.KeyEvent;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class ParallelCoordinatesPlotter extends BasicPlotter {
    
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            ParallelCoordinatesPlotter.class);
    
    private List<ParallelAxis> m_axes;
    
    private List<LineInfo> m_lines;
    
    private Set<RowKey> m_selected;
    
    private List<String>m_columnNames;
    
    /** Constant for the sensitivity area around the axis for selection. */
    public static final int SENSITIVITY = 15;
    
    private boolean m_skipMissingValues = true;
    
    private boolean m_hide;
    
    private boolean m_curve;
    
    /** Constant for a missing value. */
    public static final int MISSING = -1;
    
    private static final int DEFAULT_NR_COLS = 5;
    
    
    /**
     * Registers listeners to the control elements of the 
     * {@link org.knime.base.node.viz.plotter.parcoord
     * .ParallelCoordinatePlotterProperties}.
     */
    public ParallelCoordinatesPlotter() {
        super(new ParallelCoordinateDrawingPane(), 
                new ParallelCoordinatePlotterProperties());
        m_selected = new HashSet<RowKey>();
        addMouseListener(new TransformationMouseListener());
        // column selection
        if (getProperties() instanceof MultiColumnPlotterProperties) {
            final MultiColumnPlotterProperties props = 
            ((MultiColumnPlotterProperties)getProperties());
            props.getColumnFilter()
                    .addChangeListener(new ChangeListener() {
                        /**
                         * {@inheritDoc}
                         */
                        public void stateChanged(final ChangeEvent e) {
                            Set<String>incl = props.getColumnFilter()
                            .getIncludedColumnSet();
                            if (incl.size() < m_columnNames.size()) {
                                m_columnNames.retainAll(incl);
                            } else {
                                Set<String> newOnes 
                                    = new LinkedHashSet<String>(incl);
                                newOnes.removeAll(m_columnNames);
                                m_columnNames.addAll(newOnes);
                            }
                            updatePaintModel();
                        }
                    });
        }
        if (getProperties() instanceof ParallelCoordinatePlotterProperties) {
            final JCheckBox showBox = 
            ((ParallelCoordinatePlotterProperties)getProperties())
                .getShowDotsBox();
            showBox.addItemListener(new ItemListener() {
                    /**
                     * {@inheritDoc}
                     */
                    public void itemStateChanged(final ItemEvent e) {
                        ((ParallelCoordinateDrawingPane)getDrawingPane())
                            .setShowDots(showBox.isSelected());
                        getDrawingPane().repaint();
                    }
                });
            final JRadioButton skipRow = ((ParallelCoordinatePlotterProperties)
                    getProperties()).getSkipRowButton();
            skipRow.addItemListener(new ItemListener() {
                /**
                 * {@inheritDoc}
                 */
                public void itemStateChanged(final ItemEvent e) {
                    boolean changed = (m_skipMissingValues != skipRow.
                            isSelected()); 
                    m_skipMissingValues = skipRow.isSelected();
                    if (changed) {
                        updatePaintModel();
                    }
                }
            });
            final JRadioButton skipValue = 
                   ((ParallelCoordinatePlotterProperties)getProperties())
                   .getSkipValueButton();
            skipValue.addItemListener(new ItemListener() {
                /**
                 * {@inheritDoc}
                 */
                public void itemStateChanged(final ItemEvent e) {
                    ((ParallelCoordinateDrawingPane)getDrawingPane())
                        .setSkipValues(skipValue.isSelected());
                    updatePaintModel();
                }
            });
            final JRadioButton showBtn = ((ParallelCoordinatePlotterProperties)
                    getProperties()).getShowMissingValsButton();
            showBtn.addItemListener(new ItemListener() {
                /**
                 * {@inheritDoc}
                 */
                public void itemStateChanged(final ItemEvent e) { 
                    ((ParallelCoordinateDrawingPane)getDrawingPane())
                    .setShowMissingValues(showBtn.isSelected());
                updatePaintModel();
                }
            });
            final JCheckBox curves = ((ParallelCoordinatePlotterProperties)
                    getProperties()).getDrawCurvesBox();
            curves.addItemListener(new ItemListener() {
                /**
                 * {@inheritDoc}
                 */
                public void itemStateChanged(final ItemEvent e) {
                    ((ParallelCoordinateDrawingPane)getDrawingPane())
                        .setDrawCurves(curves.isSelected());
                    m_curve = curves.isSelected();
                    getDrawingPane().repaint();
                }
            });
            final JSpinner thickness = ((ParallelCoordinatePlotterProperties)
                    getProperties()).getThicknessSpinner();
            thickness.addChangeListener(new ChangeListener() {
                /**
                 * {@inheritDoc}
                 */
                public void stateChanged(final ChangeEvent e) {
                     ((ParallelCoordinateDrawingPane)getDrawingPane())
                         .setLineThickness((Integer)thickness.getValue());
                     getDrawingPane().repaint();
                }
            });
        }
    }
    
    private ParallelCoordinateDrawingPane getParCoordDrawinPane() {
        return (ParallelCoordinateDrawingPane)getDrawingPane();
    }
    
    /**
     * Sets the axes, the selected data points, the selected columns and the 
     * calculated lines <code>null</code>, triggers a repaint in the 
     * {@link org.knime.base.node.viz.plotter.parcoord
     * .ParallelCoordinateDrawingPane}
     * 
     * @see org.knime.base.node.viz.plotter.AbstractPlotter#reset()
     */
    @Override
    public synchronized void reset() {
        m_axes = null;
        m_selected = new HashSet<RowKey>();
        m_columnNames = null;
        m_lines = null;
        ((ParallelCoordinateDrawingPane)getDrawingPane()).setAxes(null);
        ((ParallelCoordinateDrawingPane)getDrawingPane()).setLines(null);
    }
    
    // ---------- menu -------------
    
    /**
     * 
     * @return the menu item for show all.
     */
    public Action getShowAllAction() {
        Action show = new AbstractAction(AbstractPlotter.SHOW_ALL) {
            /**
             * {@inheritDoc}
             */
            public void actionPerformed(final ActionEvent e) {
                m_hide = false;
                if (getDrawingPane() instanceof ParallelCoordinateDrawingPane) {
                    getParCoordDrawinPane().setFadeUnhilited(false);
                }   
                updatePaintModel();
            }
        };
        return show;
    }
    
    /**
     * 
     * @return the menu item for hide unhilited.
     */
    public Action getHideAction() {
        Action hide = new AbstractAction(AbstractPlotter.HIDE_UNHILITED) {
            /**
             * {@inheritDoc}
             */
            public void actionPerformed(final ActionEvent e) {
                m_hide = true;
                if (getDrawingPane() instanceof ParallelCoordinateDrawingPane) {
                    getParCoordDrawinPane().setFadeUnhilited(false);
                }
                updatePaintModel();
            } 
        };
        return hide;
    }
    
    /**
     * 
     * @return the menu item for fade unhilited.
     */
    public Action getFadeAction() {
        Action fade = new AbstractAction(AbstractPlotter.FADE_UNHILITED) {
            /**
             * {@inheritDoc}
             */
            public void actionPerformed(final ActionEvent e) {
                m_hide = false;
                if (getDrawingPane() instanceof ParallelCoordinateDrawingPane) {
                    getParCoordDrawinPane().setFadeUnhilited(true);
                }
                updatePaintModel();
            };
        };
        return fade;
    }
    

    /**
     * 
     * @return an additional menu for the NodeView's menu bar containing
     * the actions for show, fade and hide unhilited dots.
     */
    public JMenu getShowHideMenu() {
        JMenu menu = new JMenu(SHOW_HIDE);
        menu.add(getShowAllAction());
        menu.add(getHideAction());
        menu.add(getFadeAction());
        return menu;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fillPopupMenu(final JPopupMenu popupMenu) {
        super.fillPopupMenu(popupMenu);
        popupMenu.addSeparator();
        popupMenu.add(getShowAllAction());
        popupMenu.add(getHideAction());
        popupMenu.add(getFadeAction());
    }   
    
    // ------------ selection -------------------
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void clearSelection() {
        for (LineInfo line : m_lines) {
            line.setSelected(false);
        }
        m_selected.clear();
        getDrawingPane().repaint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void selectClickedElement(final Point clicked) {
        // for all lines
        for (LineInfo line : m_lines) {
            if (line.wasClicked(clicked, m_curve)) {
                line.setSelected(true);
                m_selected.add(line.getRowKey());
            }
        }
        getDrawingPane().repaint();
    }
    


    /**
     * {@inheritDoc}
     */
    @Override
    public void selectElementsIn(final Rectangle selectionRectangle) {
        if (!(getDrawingPane() instanceof ParallelCoordinateDrawingPane)) {
            return;
        }
        for (LineInfo line : m_lines) {
            if (line.isContainedIn(selectionRectangle)) {
                line.setSelected(true);
                m_selected.add(line.getRowKey());
            }
        }
        getDrawingPane().repaint();
    }

    // ---------- hilite ---------------
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void hiLite(final KeyEvent event) {
        Set<RowKey>hilited = event.keys();
        for (LineInfo line : m_lines) {
            if (hilited.contains(line.getRowKey().getString())) {
                line.setHilite(true);
            }
        }
        updatePaintModel();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void unHiLite(final KeyEvent event) {
        Set<RowKey>hilited = event.keys();
        for (LineInfo line : m_lines) {
            if (hilited.contains(line.getRowKey())) {
                line.setHilite(false);
            }
        }
        updatePaintModel();
    }
    
    /**
     * {@inheritDoc}
     */
    public void unHiLiteAll(final KeyEvent event) {
        for (LineInfo line : m_lines) {
            line.setHilite(false);
        }
        updatePaintModel();
    }
    

    /**
     * {@inheritDoc}
     */
    @Override
    public void hiLiteSelected() {
        changeHiLiteState(true);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void unHiLiteSelected() {
        changeHiLiteState(false);
    }


    private void changeHiLiteState(final boolean hilite) {
        if (hilite) {
            delegateHiLite(m_selected);
        } else {
            delegateUnHiLite(m_selected);
        }
        for (LineInfo line : m_lines) {
            if (line.isSelected()) {
                line.setHilite(hilite);
            }
        } 
        updatePaintModel();
    }
    
    // ----------- painting ----------------

    /**
     * Creates a nominal x axis with the names of the selected columns,
     * the referring 
     * {@link org.knime.base.node.viz.plotter.parcoord.ParallelAxis} for each
     * column and calculates the lines with the mapped values which are passed
     * together with the axes to the 
     * {@link org.knime.base.node.viz.plotter.parcoord
     * .ParallelCoordinateDrawingPane}.
     * 
     * @see org.knime.base.node.viz.plotter.AbstractPlotter#updatePaintModel()
     */
    @Override
    public synchronized void updatePaintModel() {
        if (getDataProvider() != null 
                && getDataProvider().getDataArray(getDataArrayIdx()) != null) {
            DataArray array = getDataProvider().getDataArray(getDataArrayIdx());
            Set<DataCell>columns = new LinkedHashSet<DataCell>();
            m_axes = new LinkedList<ParallelAxis>();
            if (m_columnNames == null) {
                initColumnNames(array);
            }
            // create the x axis
            for (String columnName : m_columnNames) {
                DataColumnSpec colSpec = array.getDataTableSpec().getColumnSpec(
                        columnName);
                if (colSpec == null) {
                    initColumnNames(array);
                    updatePaintModel();
                    break;
                }
                columns.add(new StringCell(colSpec.getName()));
                m_axes.add(ParallelAxis.createParallelAxis(colSpec));
            }
            createNominalXCoordinate(columns);
            if (getDrawingPane() instanceof ParallelCoordinateDrawingPane) {
                ((ParallelCoordinateDrawingPane)getDrawingPane()).setAxes(
                        m_axes);
                updateAxesPosition();
                m_lines = calculateLines();
                ((ParallelCoordinateDrawingPane)getDrawingPane()).setLines(
                        m_lines);

            }
            if (getProperties() instanceof MultiColumnPlotterProperties) {
                Set<String> selectedColumns = new LinkedHashSet<String>();
                selectedColumns.addAll(m_columnNames);
                ((MultiColumnPlotterProperties)getProperties())
                    .updateColumnSelection(array.getDataTableSpec(),
                            selectedColumns);
            }
        }
        getDrawingPane().repaint();
    }
    
    /**
     * The initial columns are the first five columns.
     * 
     * @param array data to visualize
     */
    private void initColumnNames(final DataArray array) {
        m_columnNames = new ArrayList<String>();
        int colNr = 0;
        for (DataColumnSpec colSpec : array.getDataTableSpec()) {
            m_columnNames.add(colSpec.getName());
            if (colNr == DEFAULT_NR_COLS - 1) {
                getProperties().setSelectedIndex(MultiColumnPlotterProperties
                        .COLUMN_FILTER_IDX);
                break;
            }
            colNr++;
        } 
    }

    /**
     * Calculates the lines, containing the mapped data points.
     */
    private synchronized List<LineInfo> calculateLines() {
        if (getDataProvider() == null 
                || getDataProvider().getDataArray(getDataArrayIdx()) == null
                || m_axes == null) {
            return new ArrayList<LineInfo>();
        }
        DataArray array = getDataProvider().getDataArray(getDataArrayIdx());
//        LOGGER.debug("calculate points: " + m_axes);
        List<LineInfo> lines = new ArrayList<LineInfo>(array.size());
        row: for (DataRow row : array) {
            List<Point> points = new ArrayList<Point>();
            List<DataCell> domainValues = new ArrayList<DataCell>();
            for (ParallelAxis axis : m_axes) {
                int colIdx = array.getDataTableSpec().findColumnIndex(
                        axis.getName());
                DataCell value = row.getCell(colIdx);
                if (value.isMissing() && m_skipMissingValues) {
                    continue row;
                }
                domainValues.add(value);
                int x = (int)getXAxis().getCoordinate().calculateMappedValue(
                        new StringCell(axis.getName()), 
                        getDrawingPaneDimension().width, true);
                int y = MISSING;
                if (!value.isMissing()) {
                    y = getDrawingPaneDimension().height 
                        - ParallelCoordinateDrawingPane.BOTTOM_SPACE
                        - (int)axis.getMappedValue(value);
                }
                Point p = new Point(x, y);
                points.add(p);
            }
            boolean isHilite = delegateIsHiLit(row.getKey());
            if (!m_hide || (m_hide && isHilite)) {
                LineInfo line = new LineInfo(points, domainValues,
                        m_selected.contains(row.getKey()), 
                        isHilite, array.getDataTableSpec().getRowColor(row),
                        array.getDataTableSpec().getRowSizeFactor(row), 
                        row.getKey());
                
                line.setShape(array.getDataTableSpec().getRowShape(row));
                lines.add(line);
            }
        }
        return lines;
    }
    
    /**
     * Updates the x position and the height of the parallel axes.
     *
     */
    private synchronized void updateAxesPosition() {
        int width = getDrawingPaneDimension().width;
        int height = getDrawingPaneDimension().height
                - ParallelCoordinateDrawingPane.TOP_SPACE
                - ParallelCoordinateDrawingPane.BOTTOM_SPACE;
        if (getDrawingPane() instanceof ParallelCoordinateDrawingPane) {
            List<ParallelAxis> axes 
                = ((ParallelCoordinateDrawingPane)getDrawingPane()).getAxes();
//            LOGGER.debug("axes from drawing pane: " + axes);
            // set the x positions
            if (axes != null) {
                for (ParallelAxis axis : axes) {
                    axis.setXPosition((int)getXAxis().getCoordinate()
                                    .calculateMappedValue(
                                            new StringCell(axis.getName()),
                                            width, true));
                    axis.setHeight(height);
                }
                ((ParallelCoordinateDrawingPane)getDrawingPane()).setAxes(axes);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void updateSize() {
        super.updateSize();
        LOGGER.debug("update size");
        updateAxesPosition();
        m_lines = calculateLines(); 
        ((ParallelCoordinateDrawingPane)getDrawingPane()).setLines(
                m_lines);
        getDrawingPane().repaint();
    }
    
    /**
     * MouseListener to change the order of the 
     * {@link org.knime.base.node.viz.plotter.parcoord.ParallelAxis}.
     * 
     * @author Fabian Dill, University of Konstanz
     */
    public class TransformationMouseListener extends PlotterMouseListener {
        
        private ParallelAxis m_axis;
        
        private boolean m_dragged;
        
        private int m_oldX;

        /**
         * {@inheritDoc}
         */
        @Override
        public void mouseDragged(final MouseEvent e) {
            m_dragged = true;
            if (m_axis != null) {
            	m_axis.setXPosition(e.getX());
            	calculateLines();
            	getDrawingPane().repaint();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void mousePressed(final MouseEvent e) {
            m_oldX = e.getX();
            Rectangle selectionRectangle = new Rectangle(
                    e.getX() - (SENSITIVITY / 2), 
                    e.getY() - (SENSITIVITY / 2),
                    SENSITIVITY, SENSITIVITY);
            ParallelCoordinateDrawingPane drawingPane 
            = (ParallelCoordinateDrawingPane)getDrawingPane();
            List<ParallelAxis> axes = drawingPane.getAxes();
            for (ParallelAxis axis : axes) {
                if (axis.isContainedIn(selectionRectangle)) {
                    axis.setSelected(true);
                    m_axis = axis;
                    break;
                }
            }
            getDrawingPane().repaint();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void mouseReleased(final MouseEvent e) {
            synchronized (m_axes) {
            if (!m_dragged) {
                m_axis.setSelected(false);
                return;
            }
            // no axis was selected
            if (m_axis == null) {
            	return;
            }
            // update x axis and axis xpos
            m_axis.setXPosition(e.getX());
            int width = getDrawingPaneDimension().width; 
//            int tickSpace = (int)getXAxis().getCoordinate()
//                .getUnusedDistBetweenTicks(width);
//            int index = (int)(e.getX() / tickSpace);
            CoordinateMapping[] mappings = getXAxis().getCoordinate()
                .getTickPositions(width, true);
            // 
            int index = -1;
            for (int i = 0; i < mappings.length - 1; i++) {
                double x1 = mappings[i].getMappingValue();
                double x2 = mappings[i + 1].getMappingValue();
                // if it lies between two axes
                if (e.getX() > x1 && e.getX() < x2) {
                    if (e.getX() > m_oldX) {
                        index = i;
                    } else {
                        index = i + 1;
                    }
                }
            }
            // not between two axes
            if (index == -1) {
                if (e.getX() > m_oldX) {
                    // dragged over the right border
                    index = mappings.length - 1;
                } else {
                    // dragged over the left border
                    index = 0;
                }
            }
            m_axes.remove(m_axis);
            m_axes.add(index, m_axis);
            m_columnNames.remove(m_axis.getName());
            m_columnNames.add(index, m_axis.getName());
            ((ParallelCoordinateDrawingPane)getDrawingPane()).setAxes(m_axes);
            ((NominalCoordinate)getXAxis().getCoordinate()).changeValuePosition(
                    new StringCell(m_axis.getName()), index);
            getXAxis().repaint();
            if (m_dragged) {
                m_axis.setSelected(false);
                m_dragged = false;
            }
            updateSize();
            }
        }  
        
        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "Transformation";
        }
    }

}
