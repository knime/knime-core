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
 *   30.08.2006 (Fabian Dill): created
 */
package org.knime.exp.node.view.plotter.columns;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.util.coordinate.Coordinate;
import org.knime.base.util.coordinate.NumericCoordinate;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeLogger;
import org.knime.exp.node.view.plotter.AbstractDrawingPane;
import org.knime.exp.node.view.plotter.AbstractPlotterProperties;
import org.knime.exp.node.view.plotter.Axis;
import org.knime.exp.node.view.plotter.DataProvider;
import org.knime.exp.node.view.plotter.basic.BasicDrawingPane;
import org.knime.exp.node.view.plotter.basic.BasicPlotter;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public abstract class TwoColumnPlotter extends BasicPlotter {
    
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            TwoColumnPlotter.class);
    
    private DataColumnSpec m_selectedXColumn;
    
    private DataColumnSpec m_selectedYColumn;
    
    private DataTableSpec m_spec;
    

    /**
     * 
     * @param panel the drawing pane
     * @param properties the properties
     */
    public TwoColumnPlotter(final AbstractDrawingPane panel, 
            final AbstractPlotterProperties properties) {
        super(panel, properties);
        if (properties instanceof TwoColumnProperties) {
            TwoColumnProperties properties2D = 
                (TwoColumnProperties)properties;
            properties2D.addXColumnListener(new ItemListener() {

                /**
                 * @see java.awt.event.ItemListener#itemStateChanged(
                 *      java.awt.event.ItemEvent)
                 */
                public void itemStateChanged(final ItemEvent e) {
                    DataColumnSpec x = ((TwoColumnProperties)
                            getProperties()).getSelectedXColumn();
                    if (x != null) {
                        xColumnChanged(x);
                    }
                }
            });
            properties2D.addYColumnListener(new ItemListener() {

                /**
                 * @see java.awt.event.ItemListener#itemStateChanged(
                 *      java.awt.event.ItemEvent)
                 */
                public void itemStateChanged(final ItemEvent e) {
                    DataColumnSpec y = ((TwoColumnProperties)
                            getProperties()).getSelectedYColumn();
                    if (y != null) {
                        yColumnChanged(y);
                    }
                }
            });
        }
        setXAxis(new Axis(Axis.HORIZONTAL, getDrawingPaneDimension().width));
        setYAxis(new Axis(Axis.VERTICAL, getDrawingPaneDimension().height));
        addRangeListener();
    }
    
    /**
     * Default two column plotter.
     *
     */
    public TwoColumnPlotter() {
        this(new BasicDrawingPane(), new TwoColumnProperties());
    }
    
    
    private void addRangeListener() {
        if (!(getProperties() instanceof TwoColumnProperties)) {
            return;
        }
        final TwoColumnProperties props = (TwoColumnProperties)getProperties();
        props.addXMinListener(new ChangeListener() {
            /**
             * @see javax.swing.event.ChangeListener#stateChanged(
             * javax.swing.event.ChangeEvent)
             */
            public void stateChanged(final ChangeEvent e) {
                double newXMin = props.getXMinValue();
                ((NumericCoordinate)getXAxis().getCoordinate())
                    .setMinDomainValue(newXMin);
                sizeChanged();
                getXAxis().repaint();
            }
            
        });
        props.addXMaxListener(new ChangeListener() {
            /**
             * @see javax.swing.event.ChangeListener#stateChanged(
             * javax.swing.event.ChangeEvent)
             */
            public void stateChanged(final ChangeEvent e) {
                double newXMax = props.getXMaxValue();
                ((NumericCoordinate)getXAxis().getCoordinate())
                    .setMaxDomainValue(newXMax);
                sizeChanged();
                getXAxis().repaint();
            }
            
        });
        props.addYMinListener(new ChangeListener() {
            /**
             * @see javax.swing.event.ChangeListener#stateChanged(
             * javax.swing.event.ChangeEvent)
             */
            public void stateChanged(final ChangeEvent e) {
                double newYMin = props.getYMinValue();
                ((NumericCoordinate)getYAxis().getCoordinate())
                    .setMinDomainValue(newYMin);
                sizeChanged();
                getYAxis().repaint();
            }
            
        });
        props.addYMaxListener(new ChangeListener() {
            /**
             * @see javax.swing.event.ChangeListener#stateChanged(
             * javax.swing.event.ChangeEvent)
             */
            public void stateChanged(final ChangeEvent e) {
                double newYMax = props.getYMaxValue();
                ((NumericCoordinate)getYAxis().getCoordinate())
                    .setMaxDomainValue(newYMax);
                sizeChanged();
                getYAxis().repaint();
            }
            
        });
    }
    
    
    private void xColumnChanged(final DataColumnSpec newXColumn) {
        if (!newXColumn.equals(m_selectedXColumn)) {
            LOGGER.debug("x column changed: " 
                    + newXColumn.getName());
            m_selectedXColumn = newXColumn;
            getXAxis().setCoordinate(Coordinate.createCoordinate(
                    m_selectedXColumn));
            getYAxis().setCoordinate(Coordinate.createCoordinate(
                    m_selectedYColumn));
            ((TwoColumnProperties)getProperties()).updateRangeSpinner(
                    m_selectedXColumn, m_selectedYColumn);
            updatePaintModel();
        }
    }
    
    private void yColumnChanged(final DataColumnSpec newYColumn) {
        if (!newYColumn.equals(m_selectedYColumn)) {
            LOGGER.debug("y column changed: " 
                    + newYColumn.getName());
            m_selectedYColumn = newYColumn;
//            m_yAxis.setPreferredLength(getDrawingPaneDimension().height);
            getYAxis().setCoordinate(Coordinate.createCoordinate(
                    m_selectedYColumn));
            getXAxis().setCoordinate(Coordinate.createCoordinate(
                    m_selectedXColumn));
            ((TwoColumnProperties)getProperties()).updateRangeSpinner(
                    m_selectedXColumn, m_selectedYColumn);
            updatePaintModel();
        }
    }
    
    /**
     * 
     * @return the currently selecteed x column.
     */
    public DataColumnSpec getSelectedXColumn() {
        return m_selectedXColumn;
    }
    
    /**
     * 
     * @return selected x column index or -1
     */
    public int getSelectedXColumnIndex() {
        if (m_spec != null && getSelectedXColumn() != null) {
            return m_spec.findColumnIndex(getSelectedXColumn().getName());
        }
        return 0;
    }
    
    /**
     * 
     * @return selected y column index or -1
     */
    public int getSelectedYColumnIndex() {
        if (m_spec != null && getSelectedYColumn() != null) {
            return m_spec.findColumnIndex(getSelectedYColumn().getName());    
        }
        return 1;
    }
    
    
    /**
     * 
     * @return the currently selected y column
     */
    public DataColumnSpec getSelectedYColumn() {
        return m_selectedYColumn;
    }
    
    /**
     * Updates the select boxes for the x and y columns.
     * @param spec the current data table spec.
     */
    public void setSelectableColumns(final DataTableSpec spec) {
        m_spec = spec;
        if (getProperties() instanceof TwoColumnProperties) {
            ((TwoColumnProperties)getProperties()).update(spec);
        }
    }
    
    
    /**
     * @see org.knime.exp.node.view.plotter.AbstractPlotter#setDataProvider(
     * DataProvider)
     */
    @Override
    public void setDataProvider(final DataProvider provider) {
        super.setDataProvider(provider);
        if (getDataProvider() != null 
                && getDataProvider().getDataArray(0) != null) {
            setSelectableColumns(getDataProvider().getDataArray(0)
                    .getDataTableSpec());
        }
    }
    
    
    /**
     * 
     * @see org.knime.exp.node.view.plotter.basic.BasicPlotter#updateSize()
     */
    @Override
    public void updateSize() {
        super.updateSize();
        sizeChanged();
    }

    /**
     * This method is called whenever the size of the plotter has changed and
     * the domain values have to be updated to fit the new visible dimension.
     *
     */
    public abstract void sizeChanged();
    
    /**
     * This method is called whenever the column selection has changed and
     * the view model has to be adapted to the currently selected columns.
     *
     */
    @Override
    public abstract void updatePaintModel();
     
    
}
