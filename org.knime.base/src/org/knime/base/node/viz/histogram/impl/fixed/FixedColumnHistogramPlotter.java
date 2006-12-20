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
import org.knime.core.node.property.hilite.HiLiteHandler;

/**
 * This class is the controller between the data model of the
 * {@link org.knime.base.node.viz.histogram.impl.interactive.InteractiveHistogramDataModel}
 * class and the view
 * {@link org.knime.base.node.viz.histogram.HistogramDrawingPane}. It creates
 * the {@link org.knime.base.node.viz.histogram.BarVisModel} objects based on
 * the
 * {@link org.knime.base.node.viz.histogram.impl.interactive.InteractiveHistogramDataModel}
 * of the
 * {@link org.knime.base.node.viz.histogram.impl.interactive.InteractiveHistogramDataModel}
 * class by enhancing these information with information about the size of the
 * drawing space like height and width in pixel.
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class FixedColumnHistogramPlotter extends AbstractHistogramPlotter {

    private static final long serialVersionUID = -3264294894462201355L;

    /**
     * Creates a new PlotterScrolling pane and associates it with the passed
     * view control panel.
     * 
     * @param histogramProps the <code>FixedColumnHistogramProperties</code>
     *            with the view options for the user
     * @param dataModel the data model on which the plotter based on
     * @param handler the hilite handler from the input port
     */
    public FixedColumnHistogramPlotter(
            final FixedColumnHistogramProperties histogramProps,
            final FixedColumnHistogramDataModel dataModel,
            final HiLiteHandler handler) {
        super(histogramProps, dataModel, handler);
    }
}
