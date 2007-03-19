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
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.viz.histogram.AbstractHistogramPlotter;
import org.knime.base.node.viz.histogram.AbstractHistogramProperties;
import org.knime.base.node.viz.histogram.AggregationMethod;
import org.knime.base.node.viz.histogram.datamodel.AbstractHistogramVizModel;
import org.knime.base.node.viz.histogram.datamodel.InteractiveHistogramVizModel;
import org.knime.base.node.viz.histogram.util.ColorColumn;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.util.ColumnSelectionComboxBox;

/**
 * This class is the controller between the data model of the
 * {@link AbstractHistogramVizModel} class and the view 
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
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(InteractiveHistogramPlotter.class);
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
                new ChangeListener() {
                    public void stateChanged(final ChangeEvent e) {
                        onAggrColChanged();
                    }
                });
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
       if (abstractVizModel == null) {
           LOGGER.debug("VizModel was null");
           return;
       }
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
     */
    protected void onAggrColChanged() {
        final AbstractHistogramVizModel abstractVizModel = 
            getHistogramVizModel();
        if (abstractVizModel == null) {
            LOGGER.debug("VizModel was null");
            return;
        }
        final AbstractHistogramProperties abstractHistogramProperties = 
            getHistogramPropertiesPanel();
        if (abstractHistogramProperties == null) {
            LOGGER.debug("ProeprtiesPanel was null");
            return;
        }
        if (abstractVizModel instanceof InteractiveHistogramVizModel) {
            final InteractiveHistogramVizModel vizModel = 
                (InteractiveHistogramVizModel)abstractVizModel;
            if (abstractHistogramProperties 
                    instanceof InteractiveHistogramProperties) {
                final InteractiveHistogramProperties props =
                (InteractiveHistogramProperties) abstractHistogramProperties;
                List<ColorColumn> aggrCols = props.getSelectedAggrColumns();
                if (vizModel.setAggregationColumns(aggrCols)) {
                    //set the current hilited keys in the new bins
                    vizModel.updateHiliteInfo(delegateGetHiLitKeys(), true);
                    setYCoordinates();
                    updatePaintModel();
                }
            } else {
                throw new IllegalStateException(
                        " PropertiesPanel should be of type interactive");
            }
        } else {
            throw new IllegalStateException(
                    "VizModel should be of type interactive");
        }
    }
    
    /**
     * @see org.knime.base.node.viz.histogram.AbstractHistogramPlotter#
     * setAggregationMethod(org.knime.base.node.viz.histogram.AggregationMethod)
     */
    @Override
    public boolean setAggregationMethod(final AggregationMethod aggrMethod) {
        if (aggrMethod == null) {
            throw new IllegalArgumentException("Aggregation method must not"
                    + " be null");
        }
        final AbstractHistogramVizModel vizModel = getHistogramVizModel();
        final Collection<? extends ColorColumn> oldAggrCols = 
            vizModel.getAggrColumns();
        if (oldAggrCols == null || oldAggrCols.size() < 1) {
            //check if the user hasn't defined any aggregation column yet we 
            //have to set a default one if he has changed the aggregation
            //method form sum to something else where we need an 
            //aggregation column
            if (vizModel instanceof InteractiveHistogramVizModel) {
                final InteractiveHistogramVizModel interactiveVizModel = 
                    (InteractiveHistogramVizModel)vizModel;
                final AbstractHistogramProperties abstHistoProps = 
                    getHistogramPropertiesPanel();
                if (abstHistoProps 
                        instanceof InteractiveHistogramProperties) {
                    final InteractiveHistogramProperties props = 
                        (InteractiveHistogramProperties)abstHistoProps;
                    
                    final DataTableSpec spec = 
                        interactiveVizModel.getTableSpec();
                    final int numColumns = spec.getNumColumns();
                    boolean found = false;
                    for (int i = 0; i < numColumns; i++) {
                        final DataColumnSpec colSpec = spec.getColumnSpec(i);
                        if (AbstractHistogramPlotter.AGGREGATION_COLUMN_FILTER.
                                includeColumn(colSpec)) {
                            final ColorColumn aggrColumn = 
                                new ColorColumn(Color.LIGHT_GRAY, 
                                        colSpec.getName());
                            final ArrayList<ColorColumn> aggrCols = 
                                new ArrayList<ColorColumn>(1);
                            aggrCols.add(aggrColumn);
                            props.updateColumnSelection(
                                    spec, getXColName(), aggrCols, aggrMethod);
                            final List<ColorColumn> selectedAggrCols = 
                                props.getSelectedAggrColumns();
                            interactiveVizModel.setAggregationColumns(
                                    selectedAggrCols);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        props.updateHistogramSettings(interactiveVizModel);
                        return false;
                    }
                } else {
                    throw new IllegalStateException(
                            "ProeprtiesPanel should  be of interactive type.");
                }
            } else {
                throw new IllegalStateException("Visualization model should "
                        + " be of interactive type.");
            }
        }
        return super.setAggregationMethod(aggrMethod);
    }
}
