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
import java.util.Collection;

import org.knime.base.node.viz.histogram.AbstractHistogramPlotter;
import org.knime.base.node.viz.histogram.AggregationMethod;
import org.knime.base.node.viz.histogram.HistogramLayout;
import org.knime.base.node.viz.histogram.datamodel.ColorColumn;
import org.knime.base.node.viz.histogram.datamodel.AbstractHistogramVizModel;
import org.knime.base.node.viz.histogram.datamodel.InteractiveHistogramDataModel;
import org.knime.base.node.viz.histogram.datamodel.InteractiveHistogramVizModel;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
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
     * @param dataModel the data model on which this plotter based on
     * @param tableSpec the table specification
     * @param handler the hilite handler from the input port
     * @param xColSpec the x column specification
     * @param aggrCols the selected aggregation columns
     */
    public InteractiveHistogramPlotter(
            final InteractiveHistogramProperties histogramProps,
            final InteractiveHistogramDataModel dataModel, 
            final DataTableSpec tableSpec, final HiLiteHandler handler,
            final DataColumnSpec xColSpec, 
            final Collection<ColorColumn> aggrCols) {
        super(histogramProps, handler);
        histogramProps.addXColActionListener(
                new ActionListener() {
                    public void actionPerformed(final ActionEvent e) {
                        final ColumnSelectionComboxBox cb = 
                            (ColumnSelectionComboxBox)e.getSource();
                        final String xCol = cb.getSelectedColumn();
                        onXColChanged(xCol);
                    }
                });
        histogramProps.addAggrColActionListener(
                new ActionListener() {
                    public void actionPerformed(final ActionEvent e) {
                        final ColumnSelectionComboxBox cb = 
                            (ColumnSelectionComboxBox)e.getSource();
                        final String aggrCol = cb.getSelectedColumn();
                        onAggrColChanged(aggrCol);
                    }
                });
        setHistogramDataModel(xColSpec, aggrCols, tableSpec, dataModel);
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
       final AbstractHistogramVizModel abstractHistogramVizModel = getHistogramVizModel();
       if (abstractHistogramVizModel instanceof InteractiveHistogramVizModel) {
           final InteractiveHistogramVizModel vizModel = 
               (InteractiveHistogramVizModel)abstractHistogramVizModel;
           vizModel.setXColumn(getDataTableSpec().getColumnSpec(xColName));
        } else {
            throw new IllegalStateException(
                    "VizModel should be of type interactive");
        }
       //set the new axis
       setXCoordinates();
       setYCoordinates();
       // repaint the plotter
       updatePaintModel();
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
        final AbstractHistogramVizModel abstractHistogramVizModel = getHistogramVizModel();
        if (abstractHistogramVizModel instanceof InteractiveHistogramVizModel) {
            final InteractiveHistogramVizModel vizModel = 
                (InteractiveHistogramVizModel)abstractHistogramVizModel;
            final int aggrColIdx = getDataTableSpec().findColumnIndex(
                    aggrColName);
            if (aggrColIdx < 0) {
                throw new IllegalArgumentException(
                        "Selected column: " + aggrColName 
                        + " not found in table specification");
            }
            final ColorColumn aggrColumn = new ColorColumn(Color.CYAN,
                    aggrColIdx, aggrColName);
            final ArrayList<ColorColumn> aggrCols = new ArrayList<ColorColumn>(
                    1);
            aggrCols.add(aggrColumn);
            vizModel.setAggregationColumns(aggrCols);
        } else {
            throw new IllegalStateException(
                    "VizModel should be of type interactive");
        }
        setYCoordinates();
        updatePaintModel();
    }

    /**
     * @param xColSpec the {@link DataColumnSpec} of the selected x column
     * @param aggrCols the aggregation columns
     * @param tableSpec the {@link DataTableSpec}
     * @param dataModel the  {@link InteractiveHistogramDataModel}
     */
    public void setHistogramDataModel(final DataColumnSpec xColSpec, 
            final Collection<ColorColumn> aggrCols,
            final DataTableSpec tableSpec,
            final InteractiveHistogramDataModel dataModel) {
        final InteractiveHistogramVizModel vizModel = 
            new InteractiveHistogramVizModel(dataModel.getRowColors(), 
                AggregationMethod.getDefaultMethod(), 
                HistogramLayout.getDefaultLayout(), tableSpec,
                dataModel.getDataRows(), xColSpec, aggrCols, 
                AbstractHistogramVizModel.DEFAULT_NO_OF_BINS);
        setHistogramVizModel(tableSpec, vizModel);
    }
    
}
