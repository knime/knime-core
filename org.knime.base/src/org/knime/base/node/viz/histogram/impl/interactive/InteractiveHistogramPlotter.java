/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 */
package org.knime.base.node.viz.histogram.impl.interactive;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;

import org.knime.base.node.viz.histogram.AbstractHistogramPlotter;
import org.knime.base.node.viz.histogram.AggregationMethod;
import org.knime.base.node.viz.histogram.datamodel.FixedHistogramDataModel;
import org.knime.base.node.viz.histogram.datamodel.FixedHistogramVizModel;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.property.hilite.HiLiteHandler;

/**
 * This class is the controller between the data model of the
 * {@link InteractiveHistogramDataModel} class and the view 
 * {@link org.knime.base.node.viz.histogram.HistogramDrawingPane}. It creates 
 * the {@link org.knime.base.node.viz.histogram.BarVisModel} objects 
 * based on the 
 * {@link 
 * org.knime.base.node.viz.histogram.impl.interactive.InteractiveBarDataModel} 
 * of the 
 * {@link 
 * org.knime.base.node.viz.histogram.impl.interactive.InteractiveHistogramDataModel} 
 * class by enhancing these information 
 * with information about the size of the drawing space like height and width 
 * in pixel.
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class InteractiveHistogramPlotter extends AbstractHistogramPlotter {
    
    private static final long serialVersionUID = -893697601218801524L;
    
    /** <code>DataTable</code> which holds the data rows. */
//    private final Collection<DataRow> m_data;
    
    /**
     * Creates a new PlotterScrolling pane and associates it with the passed
     * view control panel.
     * 
     * @param histogramProps the <code>FixedColumnHistogramProperties</code> 
     * with the view options for the user
     * @param dataModel the data model on which this plotter based on
     * @param tableSpec the table specification
     * @param handler the hilite handler from the input port
     * @param rows all rows from the input table
     */
    public InteractiveHistogramPlotter(
            final InteractiveHistogramProperties histogramProps,
            final FixedHistogramDataModel dataModel, final DataTableSpec tableSpec,
            final HiLiteHandler handler, final Iterator<DataRow> rows) {
        super(histogramProps, tableSpec, handler);
        setHistogramDataModel(dataModel);
        histogramProps.getXColSelectBox().addActionListener(
                new ActionListener() {
                    public void actionPerformed(final ActionEvent e) {
                        final InteractiveHistogramProperties props = 
                            (InteractiveHistogramProperties)
                            getHistogramPropertiesPanel();
                        onXColChanged(props.getSelectedXColumn());
                    }
                });
//        m_data = rows;
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
   
//   @Override
//   protected void onApply() {
//       final InteractiveHistogramProperties interactiveHistoProps = 
//           (InteractiveHistogramProperties)getHistogramPropertiesPanel();
//       setAggregationColumn(interactiveHistoProps.getSelectedAggrColumn(), 
//                   interactiveHistoProps.getSelectedAggrMethod());
//       super.onApply();
//       return;
//   }

    /**
     * Sets the new x column.
     * 
     * @param xColName name of the new x column to plot
     */
    public void setXColumn(final String xColName) {
        if (xColName == null || xColName.trim().length() < 1) {
            return;
        }
        // if it's the same name we don't need to do anything
        if (getXColName() != null && getXColName().equals(xColName)) {
            return;
        }
        // since the tableSpec is final and checked in the constructor
        // we don't need to check for null
        int xIndex = getDataTableSpec().findColumnIndex(xColName);
        if (xIndex >= 0) {
            //reset the histogram data model first
            resetHistogramData();
            //set all values which have no side effect
//          set the name of the selected x column in the plotter class
//            setXColName(xColName);
            // reset the vis bars
//            getHistogramDrawingPane().setVisBars(null);
            // reset the aggregation column to the possible new boundaries
//            setYColName(null); // set the column name to null to force 
            //after setting all needed values set the aggregation column
            //which needs the HistogramDataModel!!!
            final InteractiveHistogramProperties interactiveHistoProps = 
                (InteractiveHistogramProperties)getHistogramPropertiesPanel();
            setAggregationColumn(interactiveHistoProps.getSelectedAggrColumn(), 
                    interactiveHistoProps.getSelectedAggrMethod());
            
            //set the new axis
            setXCoordinates();
            setYCoordinates();
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
//        setAggregationColName(yColName);
//        boolean nameChanged = 
//            getHistogramDataModel().changeAggregationColumn(yColName);
//        boolean coordinatesSet = super.setAggregationMethod(aggrMethod);
//        if (nameChanged && !coordinatesSet) {
//            setYCoordinates();
//        }
        return;
    }
    
    /**
     * @see org.knime.base.node.viz.histogram.AbstractHistogramPlotter
     * #getHistogramVizModel()
     */
    @Override
    public FixedHistogramVizModel getHistogramVizModel() {
        FixedHistogramVizModel histoData = super.getHistogramVizModel();
        if (histoData == null) {
//            histoData = new HistogramDataModel(getDataTableSpec(), 
//                    getXColName(), getAggregationColName(), 
//                    getAggregationMethod());
//            if (m_data != null) {
//                for (Iterator<DataRow> iter = m_data.iterator(); 
//                    iter.hasNext();) {
//                    histoData.addDataRow(iter.next());
//                }
//            }
//            super.setHistogramDataModel(histoData);
        }
        return histoData;
    }
}
