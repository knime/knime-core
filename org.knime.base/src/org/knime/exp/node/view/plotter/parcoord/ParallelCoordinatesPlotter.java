/* -------------------------------------------------------------------
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
 *   22.09.2006 (Fabian Dill): created
 */
package org.knime.exp.node.view.plotter.parcoord;

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
import org.knime.base.util.coordinate.CoordinateMapping;
import org.knime.base.util.coordinate.NominalCoordinate;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.property.hilite.KeyEvent;
import org.knime.exp.node.view.plotter.AbstractPlotter;
import org.knime.exp.node.view.plotter.PlotterMouseListener;
import org.knime.exp.node.view.plotter.basic.BasicPlotter;
import org.knime.exp.node.view.plotter.columns.MultiColumnPlotterProperties;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class ParallelCoordinatesPlotter extends BasicPlotter {
    
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            ParallelCoordinatesPlotter.class);
    
    private List<ParallelAxis> m_axes;
    
    private List<LineInfo> m_lines;
    
    private Set<DataCell> m_selected;
    
    private List<String>m_columnNames;
    
    /** Constant for the sensitivity area around the axis for selection. */
    public static final int SENSITIVITY = 15;
    
    private boolean m_skipMissingValues = true;
    
    private boolean m_hide;
    
    private boolean m_curve;
    
    /** Constant for a missing value. */
    public static final int MISSING = -1;
    
    
    /**
     * Default constructor.
     */
    public ParallelCoordinatesPlotter() {
        super(new ParallelCoordinateDrawingPane(), 
                new ParallelCoordinatePlotterProperties());
        m_selected = new HashSet<DataCell>();
        addMouseListener(new TransformationMouseListener());
        // column selection
        if (getProperties() instanceof MultiColumnPlotterProperties) {
            final MultiColumnPlotterProperties props = 
            ((MultiColumnPlotterProperties)getProperties());
            props.getColumnFilter()
                    .addChangeListener(new ChangeListener() {
                        /**
                         * @see javax.swing.event.ChangeListener#stateChanged(
                         *      javax.swing.event.ChangeEvent)
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
                     * @see java.awt.event.ItemListener#itemStateChanged(
                     * java.awt.event.ItemEvent)
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
                 * @see java.awt.event.ItemListener#itemStateChanged(
                 * java.awt.event.ItemEvent)
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
                 * @see java.awt.event.ItemListener#itemStateChanged(
                 * java.awt.event.ItemEvent)
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
                 * @see java.awt.event.ItemListener#itemStateChanged(
                 * java.awt.event.ItemEvent)
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
                 * @see java.awt.event.ItemListener#itemStateChanged(
                 * java.awt.event.ItemEvent)
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
                 * @see javax.swing.event.ChangeListener#stateChanged(
                 * javax.swing.event.ChangeEvent)
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
     * @see org.knime.exp.node.view.plotter.AbstractPlotter#reset()
     */
    @Override
    public synchronized void reset() {
        m_axes = null;
        m_selected = new HashSet<DataCell>();
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
             * @see java.awt.event.ActionListener#actionPerformed(
             * java.awt.event.ActionEvent)
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
             * @see java.awt.event.ActionListener#actionPerformed(
             * java.awt.event.ActionEvent)
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
             * @see java.awt.event.ActionListener#actionPerformed(
             * java.awt.event.ActionEvent)
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
     * @see org.knime.exp.node.view.plotter.AbstractPlotter#fillPopupMenu(
     * javax.swing.JPopupMenu)
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
     * @see org.knime.exp.node.view.plotter.AbstractPlotter#clearSelection()
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
     * @see org.knime.exp.node.view.plotter.AbstractPlotter#
     *      selectClickedElement(java.awt.Point)
     */
    @Override
    public void selectClickedElement(final Point clicked) {
        // for all lines
        for (LineInfo line : m_lines) {
            if (line.wasClicked(clicked, m_curve)) {
                line.setSelected(true);
                m_selected.add(line.getRowKey().getId());
            }
        }
        getDrawingPane().repaint();
    }
    


    /**
     * @see org.knime.exp.node.view.plotter.AbstractPlotter#
     * selectElementsIn(java.awt.Rectangle)
     */
    @Override
    public void selectElementsIn(final Rectangle selectionRectangle) {
        if (!(getDrawingPane() instanceof ParallelCoordinateDrawingPane)) {
            return;
        }
        for (LineInfo line : m_lines) {
            if (line.isContainedIn(selectionRectangle)) {
                line.setSelected(true);
                m_selected.add(line.getRowKey().getId());
            }
        }
        getDrawingPane().repaint();
    }

    // ---------- hilite ---------------
    
    /**
     * @see org.knime.exp.node.view.plotter.AbstractPlotter#hiLite(
     * org.knime.core.node.property.hilite.KeyEvent)
     */
    @Override
    public void hiLite(final KeyEvent event) {
        Set<DataCell>hilited = event.keys();
        for (LineInfo line : m_lines) {
            if (hilited.contains(line.getRowKey().getId())) {
                line.setHilite(true);
            }
        }
        updatePaintModel();
    }
    
    /**
     * @see org.knime.exp.node.view.plotter.AbstractPlotter#
     * unHiLite(org.knime.core.node.property.hilite.KeyEvent)
     */
    @Override
    public void unHiLite(final KeyEvent event) {
        Set<DataCell>hilited = event.keys();
        for (LineInfo line : m_lines) {
            if (hilited.contains(line.getRowKey().getId())) {
                line.setHilite(false);
            }
        }
        updatePaintModel();
    }
    
    /**
     * @see org.knime.core.node.property.hilite.HiLiteListener#unHiLiteAll()
     */
    public void unHiLiteAll() {
        for (LineInfo line : m_lines) {
            line.setHilite(false);
        }
        updatePaintModel();
    }
    

    /**
     * 
     * @see org.knime.exp.node.view.plotter.AbstractPlotter#hiLiteSelected()
     */
    @Override
    public void hiLiteSelected() {
        changeHiLiteState(true);
    }
    
    /**
     * 
     * @see org.knime.exp.node.view.plotter.AbstractPlotter#unHiLiteSelected()
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
     * @see org.knime.exp.node.view.plotter.AbstractPlotter#updatePaintModel()
     */
    @Override
    public synchronized void updatePaintModel() {
        if (getDataProvider() != null 
                && getDataProvider().getDataArray(0) != null) {
            DataArray array = getDataProvider().getDataArray(0);
            Set<DataCell>columns = new LinkedHashSet<DataCell>();
            m_axes = new LinkedList<ParallelAxis>();
            if (m_columnNames == null) {
                updateColumnNames(array);
            }
            // create the x axis
            for (String columnName : m_columnNames) {
                DataColumnSpec colSpec = array.getDataTableSpec().getColumnSpec(
                        columnName);
                if (colSpec == null) {
                    updateColumnNames(array);
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
    
    private void updateColumnNames(final DataArray array) {
        m_columnNames = new ArrayList<String>();
        for (DataColumnSpec colSpec : array.getDataTableSpec()) {
            m_columnNames.add(colSpec.getName());
        } 
    }

    /**
     * 
     */
    private synchronized List<LineInfo> calculateLines() {
        if (getDataProvider() == null 
                || getDataProvider().getDataArray(0) == null
                || m_axes == null) {
            return new ArrayList<LineInfo>();
        }
        DataArray array = getDataProvider().getDataArray(0);
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
            boolean isHilite = delegateIsHiLit(row.getKey().getId());
            if (!m_hide || (m_hide && isHilite)) {
                LineInfo line = new LineInfo(points, domainValues,
                        m_selected.contains(row.getKey().getId()), 
                        isHilite, array.getDataTableSpec().getRowColor(row),
                        array.getDataTableSpec().getRowSize(row), row.getKey());
                line.setShape(array.getDataTableSpec().getRowShape(row));
                lines.add(line);
            }
        }
        return lines;
    }
    
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
     * @see org.knime.exp.node.view.plotter.basic.BasicPlotter#updateSize()
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
     * 
     * @author Fabian Dill, University of Konstanz
     */
    public class TransformationMouseListener extends PlotterMouseListener {
        
        private ParallelAxis m_axis;
        
        private boolean m_dragged;
        
        private int m_oldX;

        /**
         * @see org.knime.exp.node.view.plotter.PlotterMouseListener#
         * mouseDragged(java.awt.event.MouseEvent)
         */
        @Override
        public void mouseDragged(final MouseEvent e) {
            m_dragged = true;
            m_axis.setXPosition(e.getX());
            calculateLines();
            getDrawingPane().repaint();
        }

        /**
         * @see java.awt.event.MouseAdapter#mousePressed(
         * java.awt.event.MouseEvent)
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
         * @see java.awt.event.MouseAdapter#mouseReleased(
         * java.awt.event.MouseEvent)
         */
        @Override
        public void mouseReleased(final MouseEvent e) {
            synchronized (m_axes) {
            if (!m_dragged) {
                m_axis.setSelected(false);
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
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "Transformation";
        }
    }

}
