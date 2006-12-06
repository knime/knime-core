/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
 */
package org.knime.base.node.viz.histogram.impl.interactive;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.knime.base.node.viz.histogram.AbstractHistogramDataModel;
import org.knime.base.node.viz.histogram.AbstractHistogramPlotter;
import org.knime.base.node.viz.histogram.AggregationMethod;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.property.hilite.HiLiteHandler;

/**
 * This class is the controller between the data model of the
 * {@link InteractiveHistogramDataModel} class and the view 
 * {@link org.knime.base.node.viz.histogram.HistogramDrawingPane}. It creates 
 * the {@link org.knime.base.node.viz.histogram.BarVisModel} objects 
 * based on the {@link InteractiveBarDataModel} of the 
 * {@link InteractiveHistogramDataModel} class by enhancing these information 
 * with information about the size of the drawing space like height and width 
 * in pixel.
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class InteractiveHistogramPlotter extends AbstractHistogramPlotter {
    
    private static final long serialVersionUID = -893697601218801524L;
    
    /** <code>DataTable</code> which holds the data rows. */
    private final Collection<DataRow> m_data;
    
    /**
     * Creates a new PlotterScrolling pane and associates it with the passed
     * view control panel.
     * 
     * @param spec the specification of the input data table
     * @param histogramProps the <code>FixedColumnHistogramProperties</code> 
     * with the view options for the user
     * @param handler the hilite handler from the input port
     * @param xColumn the x axis column which should be selected, if it's
     *            <code>null</code> the first column will be selected
     */
    public InteractiveHistogramPlotter(final DataTableSpec spec,
            final InteractiveHistogramProperties histogramProps,
            final HiLiteHandler handler, final String xColumn) {
        super(spec, histogramProps, handler, xColumn, null);
        setHistogramDataModel(
                new InteractiveHistogramDataModel(spec, xColumn, 
                null, AggregationMethod.getDefaultMethod()));
        histogramProps.getXColSelectBox().addActionListener(
                new ActionListener() {
                    public void actionPerformed(final ActionEvent e) {
                        final InteractiveHistogramProperties props = 
                            (InteractiveHistogramProperties)
                            getHistogramPropertiesPanel();
                        onXColChanged(props.getSelectedXColumn());
                    }
                });
        histogramProps.updateHistogramSettings(this);
        m_data = new ArrayList<DataRow>();
    }
   
    /**
    * Called whenever user changes the x column selection.
    * 
    * @param xColName the new selected x column
    */
   protected void onXColChanged(final String xColName) {
       final InteractiveHistogramProperties interactiveHistoProps = 
           (InteractiveHistogramProperties)getHistogramPropertiesPanel();
       setXColumn(xColName);
       interactiveHistoProps.getXColSelectBox().setToolTipText(xColName);
       // repaint the plotter
       updatePaintModel();
       // update the slider values and the select boxes
       interactiveHistoProps.updateHistogramSettings(this);
   }
   
   /**
     * @see org.knime.base.node.viz.histogram.AbstractHistogramPlotter#onApply()
     */
   @Override
   protected void onApply() {
       final InteractiveHistogramProperties interactiveHistoProps = 
           (InteractiveHistogramProperties)getHistogramPropertiesPanel();
       setAggregationColumn(interactiveHistoProps.getSelectedAggrColumn(), 
                   interactiveHistoProps.getSelectedAggrMethod());
       super.onApply();
       return;
   }
    /**
     * @see org.knime.base.node.viz.histogram.AbstractHistogramPlotter
     * #addDataRow(org.knime.core.data.DataRow)
     */
    @Override
    public void addDataRow(final DataRow row) {
        m_data.add(row);
        AbstractHistogramDataModel histoData = getHistogramDataModel();
        if (histoData == null) {
            setHistogramDataModel(
                    new InteractiveHistogramDataModel(getDataTableSpec(), 
                    getXColName(), getAggregationColName(), 
                    getAggregationMethod()));
        }
        super.addDataRow(row);
    }

    /**
     * Sets the new x columns.
     * 
     * @param xColName name of the new x column to plot
     */
    public void setXColumn(final String xColName) {
        if (xColName == null || xColName.trim().length() < 1) {
            throw new IllegalArgumentException("X axis must be defined.");
        }
        // if it's the same name we don't need to do anything
        if (getXColName() != null && getXColName().equals(xColName)) {
            return;
        }
        // since the tableSpec is final and checked in the constructor
        // we don't need to check for null
        int xIndex = getDataTableSpec().findColumnIndex(xColName);
        if (xIndex >= 0) {
            resetHistogramData();
            // set the name of the selected x column in the plotter class
            setXColName(xColName);
            setXCoordinates();
            // reset the vis bars
            getHistogramDrawingPane().setVisBars(null);
            // reset the aggregation column to the possible new boundaries
            setYColName(null); // set the column name to null to force
            // resetting
            final InteractiveHistogramProperties interactiveHistoProps = 
                (InteractiveHistogramProperties)getHistogramPropertiesPanel();
            setAggregationColumn(interactiveHistoProps.getSelectedAggrColumn(), 
                    interactiveHistoProps.getSelectedAggrMethod());
        } else {
            throw new IllegalArgumentException("No column specification found"
                    + " for column: " + xColName);
        }
    }

    /**
     * Sets new aggregation columns and recalculates/repaints.
     * 
     * @param yColName name of the new y column to plot
     * @param aggrMethod The aggregation method
     */
    public void setAggregationColumn(final String yColName,
            final AggregationMethod aggrMethod) {
        if (aggrMethod == null) {
            throw new IllegalArgumentException("Aggregation method shouldn't"
                    + " be null");
        }
        if (yColName == null && !aggrMethod.equals(AggregationMethod.COUNT)) {
            throw new IllegalArgumentException("No column name only allowed"
                    + " with aggregation method count.");
        }
        setAggregationColName(yColName);
        boolean nameChanged = 
            getHistogramDataModel().changeAggregationColumn(yColName);
        boolean coordinatesSet = super.setAggregationMethod(aggrMethod);
        if (nameChanged && !coordinatesSet) {
            setYCoordinates();
        }
        return;
    }
    
    /**
     * @see org.knime.base.node.viz.histogram.AbstractHistogramPlotter
     * #getHistogramDataModel()
     */
    @Override
    public AbstractHistogramDataModel getHistogramDataModel() {
        AbstractHistogramDataModel histoData = super.getHistogramDataModel();
        if (histoData == null) {
            histoData = new InteractiveHistogramDataModel(getDataTableSpec(), 
                    getXColName(), getAggregationColName(), 
                    getAggregationMethod());
            if (m_data != null) {
                for (Iterator<DataRow> iter = m_data.iterator(); 
                    iter.hasNext();) {
                    histoData.addDataRow(iter.next());
                }
            }
            super.setHistogramDataModel(histoData);
        }
        return histoData;
    }
    
    /*
     * @see org.knime.base.node.viz.histogram.AbstractHistogramPlotter#
     * modelChanged(org.knime.core.data.DataTableSpec, java.lang.String)
    @Override
    public void modelChanged(final DataTableSpec spec, 
            final String selectedXCol, final String aggrCol) {
        setAggregationColName(aggrCol);
        setAggregationMethod(AggregationMethod.getDefaultMethod());
        setXColumn(selectedXCol);
        super.modelChanged(spec, selectedXCol, aggrCol);
    }*/
}
