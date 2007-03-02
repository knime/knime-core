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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import org.knime.base.node.viz.histogram.AbstractHistogramPlotter;
import org.knime.base.node.viz.histogram.datamodel.AbstractHistogramVizModel;
import org.knime.base.node.viz.histogram.datamodel.ColorColumn;
import org.knime.base.node.viz.histogram.datamodel.InteractiveHistogramVizModel;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.util.ColumnSelectionComboxBox;

/**
 * This class is the controller between the data model of the
 * {@link InteractiveHistogramDataModel} class and the view 
 * {@link org.knime.base.node.viz.histogram.HistogramDrawingPane}. It creates 
 * the {@link org.knime.base.node.viz.histogram.BarVisModel} objects 
 * based on the 
 * {@link 
 * org.knime.base.node.viz.histogram.impl.interactive.InteractiveBarDataModel} 
 * of the 
 * {@link org.knime.base.node.viz.histogram.impl.interactive.
 * InteractiveHistogramDataModel} 
 * class by enhancing these information 
 * with information about the size of the drawing space like height and width 
 * in pixel.
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class InteractiveHistogramPlotter extends AbstractHistogramPlotter {
    
    private static final long serialVersionUID = -893697601218801524L;
    
    
    /**
     * Creates a new PlotterScrolling pane and associates it with the passed
     * view control panel.
     * 
     * @param histogramProps the <code>FixedColumnHistogramProperties</code> 
     * with the view options for the user
     * @param handler the hilite handler from the input port
     */
    public InteractiveHistogramPlotter(
            final InteractiveHistogramProperties histogramProps,
            final HiLiteHandler handler) {
        super(histogramProps, handler);
        histogramProps.addXColumnChangeListener(
                new ActionListener() {
                    public void actionPerformed(final ActionEvent e) {
                        final ColumnSelectionComboxBox cb = 
                            (ColumnSelectionComboxBox)e.getSource();
                        final String xCol = cb.getSelectedColumn();
                        onXColChanged(xCol);
                    }
                });
        histogramProps.addAggrColumnChangeListener(
                new ActionListener() {
                    public void actionPerformed(final ActionEvent e) {
                        final ColumnSelectionComboxBox cb = 
                            (ColumnSelectionComboxBox)e.getSource();
                        final String aggrCol = cb.getSelectedColumn();
                        onAggrColChanged(aggrCol);
                    }
                });
//        setHistogramDataModel(xColSpec, aggrCols, tableSpec, dataModel);
//        m_data = rows;
    }
   
    /**
    * Called whenever user changes the x column selection.
    * 
    * @param xColName the new selected x column
    */
   protected void onXColChanged(final String xColName) {
       if (xColName == null || xColName.trim().length() < 1) {
           return;
       }
       // if it's the same name we don't need to do anything
       if (getXColName() != null && getXColName().equals(xColName)) {
           return;
       }
       final AbstractHistogramVizModel abstractVizModel = 
           getHistogramVizModel();
       if (abstractVizModel instanceof InteractiveHistogramVizModel) {
           final InteractiveHistogramVizModel vizModel = 
               (InteractiveHistogramVizModel)abstractVizModel;
           final DataColumnSpec xColSpec = 
               getDataTableSpec().getColumnSpec(xColName);
           if (vizModel.setXColumn(xColSpec)) {
               //set the current hilited keys in the new bins
               vizModel.updateHiliteInfo(delegateGetHiLitKeys(), true);
//             set the new axis
               setXCoordinates();
               setYCoordinates();
               // repaint the plotter
               updatePaintModel();
           }
        } else {
            throw new IllegalStateException(
                    "VizModel should be of type interactive");
        }
       
       // update the slider values and the select boxes
//       getHistogramPropertiesPanel().updateHistogramSettings(this);
   }
   
   /**
     * Called whenever the user changes the aggregation column.
     * 
     * @param aggrColName the name of the new selected aggregation column
     */
    protected void onAggrColChanged(final String aggrColName) {
        if (aggrColName == null) {
            return;
        }
        final AbstractHistogramVizModel abstractVizModel = 
            getHistogramVizModel();
        if (abstractVizModel instanceof InteractiveHistogramVizModel) {
            final InteractiveHistogramVizModel vizModel = 
                (InteractiveHistogramVizModel)abstractVizModel;
            final int aggrColIdx = getDataTableSpec().findColumnIndex(
                    aggrColName);
            if (aggrColIdx < 0) {
                throw new IllegalArgumentException(
                        "Selected column: " + aggrColName 
                        + " not found in table specification");
            }
            final ColorColumn aggrColumn = new ColorColumn(Color.LIGHT_GRAY,
                    aggrColIdx, aggrColName);
            final ArrayList<ColorColumn> aggrCols = new ArrayList<ColorColumn>(
                    1);
            aggrCols.add(aggrColumn);
            if (vizModel.setAggregationColumns(aggrCols)) {
                //set the current hilited keys in the new bins
                vizModel.updateHiliteInfo(delegateGetHiLitKeys(), true);
                setYCoordinates();
                updatePaintModel();
            }
        } else {
            throw new IllegalStateException(
                    "VizModel should be of type interactive");
        }
    }
}
