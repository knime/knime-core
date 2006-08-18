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
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 */
package org.knime.base.node.viz.histogram;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.node.property.hilite.HiLiteHandler;

/**
 * This class is the controller between the data model of the
 * {@link InteractiveHistogramDataModel} class and the view 
 * {@link HistogramDrawingPane}. It creates the {@link BarVisModel} objects 
 * based on the {@link InteractiveBarDataModel} of the 
 * {@link InteractiveHistogramDataModel} class by enhancing these information 
 * with information about the size of the drawing space like height and width 
 * in pixel.
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class FixedColumnHistogramPlotter extends AbstractHistogramPlotter {
    
    /**
     * Creates a new PlotterScrolling pane and associates it with the passed
     * view control panel.
     * 
     * @param initialWidth the width of the dialog at the creation time
     * @param spec the specification of the input data table
     * @param histogramProps the <code>FixedColumnHistogramProperties</code> 
     * with the view options for the user
     * @param handler the hilite handler from the input port
     * @param xColumn the x axis column which should be selected, if it's
     *            <code>null</code> the first column will be selected
     * @param aggregationColName the name of the aggregation column
     */
    public FixedColumnHistogramPlotter(final int initialWidth, 
            final DataTableSpec spec,
            final FixedColumnHistogramProperties histogramProps,
            final HiLiteHandler handler, final String xColumn, 
            final String aggregationColName) {
        super(initialWidth, spec, histogramProps, handler, xColumn);
        setHistoData(new FixedColumnHistogramDataModel(spec, xColumn, 
                aggregationColName, AggregationMethod.getDefaultMethod()));
    }
    
    /**
     * @see org.knime.base.node.viz.histogram.AbstractHistogramPlotter#
     * addDataRow(org.knime.core.data.DataRow)
     */
    @Override
    public void addDataRow(final DataRow row) {
        if (getHistoData() == null) {
            setHistoData(new FixedColumnHistogramDataModel(getTableSpec(), 
                    getXColName(), getAggregationColName(), getAggregationMethod()));
        }
        super.addDataRow(row);
    }

    /**
     * @see org.knime.base.node.viz.histogram.AbstractHistogramPlotter#
     * modelChanged(org.knime.core.data.DataTableSpec, java.lang.String)
     */
    @Override
    public void modelChanged(final DataTableSpec spec, 
            final String selectedXCol) {
        setTableSpec(spec);
        setAggregationColName(null);
        setAggregationMethod(AggregationMethod.getDefaultMethod());
        setBackground(ColorAttr.getBackground());
        AbstractHistogramProperties props = getHistogramPropertiesPanel();
        props.updateColumnSelection(spec, selectedXCol, null);
        props.setUpdateHistogramSettings(this);
        updatePaintModel();
    }
}
