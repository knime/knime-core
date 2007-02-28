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
package org.knime.base.node.viz.histogram.impl.fixed;

import org.knime.base.node.viz.histogram.AbstractHistogramPlotter;
import org.knime.base.node.viz.histogram.AggregationMethod;
import org.knime.base.node.viz.histogram.HistogramLayout;
import org.knime.base.node.viz.histogram.datamodel.FixedHistogramDataModel;
import org.knime.base.node.viz.histogram.datamodel.FixedHistogramVizModel;
import org.knime.base.node.viz.histogram.datamodel.AbstractHistogramVizModel;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.property.hilite.HiLiteHandler;

/**
 * This class is the controller between the data model of the
 * {@link FixedColumnHistogramDataModel}
 * class and the view
 * {@link org.knime.base.node.viz.histogram.HistogramDrawingPane}. It creates
 * the {@link FixedHistogramDataModel} which contains the rectangles to draw
 * on the screen.
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class FixedHistogramPlotter extends AbstractHistogramPlotter {

    private static final long serialVersionUID = -3264294894462201355L;

    /**
     * Creates a new PlotterScrolling pane and associates it with the passed
     * view control panel.
     * 
     * @param histogramProps the <code>FixedColumnHistogramProperties</code>
     *            with the view options for the user
     * @param dataModel the data model on which the plotter based on
     * @param tableSpec the table specification
     * @param handler the hilite handler from the input port
     */
    public FixedHistogramPlotter(
            final FixedHistogramProperties histogramProps,
            final FixedHistogramDataModel dataModel, 
            final DataTableSpec tableSpec, final HiLiteHandler handler) {
        super(histogramProps, handler);
        setHistogramDataModel(tableSpec, dataModel);
    }
    
    /**
     * @param spec the new {@link DataTableSpec}
     * @param dataModel the new {@link FixedHistogramDataModel}
     */
    public void setHistogramDataModel(final DataTableSpec spec, 
            final FixedHistogramDataModel dataModel) {
        if (spec == null) {
            throw new IllegalArgumentException(
                    "Table specification shouldn't be null");
        }
        if (dataModel == null) {
            throw new IllegalArgumentException(
                    "Histogram data model shouldn't be null");
        }
        final FixedHistogramVizModel vizModel = new FixedHistogramVizModel(
                dataModel.getRowColors(),
                AggregationMethod.getDefaultMethod(), 
                HistogramLayout.getDefaultLayout(), 
                dataModel.getSortedRows(),
                dataModel.getXColumnSpec(), dataModel.getAggrColumns(),
                AbstractHistogramVizModel.DEFAULT_NO_OF_BINS);
        setHistogramVizModel(spec, vizModel);
    }
}
