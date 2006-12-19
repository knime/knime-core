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
 * 
 * History
 *   08.06.2006 (Tobias Koetter): created
 */
package org.knime.base.node.viz.histogram.node;

import org.knime.base.node.viz.histogram.impl.interactive.InteractiveHistogramDataModel;
import org.knime.base.node.viz.histogram.impl.interactive.InteractiveHistogramPlotter;
import org.knime.base.node.viz.histogram.impl.interactive.InteractiveHistogramProperties;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;

/**
 * The node view which contains the histogram plotter panel.
 * 
 * @author Tobias Koetter, University of Konstanz
 * 
 */
public class HistogramNodeView extends NodeView {
    
    private final HistogramNodeModel m_model;
    
    private final InteractiveHistogramPlotter m_plotter;

    /**
     * Creates a new view instance for the histogram node.
     * 
     * @param nodeModel the corresponding node model
     */
    HistogramNodeView(final NodeModel nodeModel) {
        super(nodeModel);
        if (!(nodeModel instanceof HistogramNodeModel)) {
            throw new IllegalArgumentException(NodeModel.class.getName()
                    + " not an instance of "
                    + HistogramNodeModel.class.getName());
        }
        m_model = (HistogramNodeModel)nodeModel;
        final InteractiveHistogramDataModel histogramModel = 
            m_model.getHistogramModelClone();
        final InteractiveHistogramProperties props =
            new InteractiveHistogramProperties(
                    histogramModel.getAggregationMethod());
        m_plotter = new InteractiveHistogramPlotter(props, histogramModel, 
                m_model.getInHiLiteHandler(0));
        // add the hilite menu to the menu bar of the node view
        getJMenuBar().add(m_plotter.getHiLiteMenu());
        setComponent(m_plotter);
    }

    /**
     * Clears the plot and unregisters from any hilite handler.
     */
    public void reset() {
        m_model.reset();
    }

    /**
     * Whenever the model changes an update for the plotter is triggered and new
     * HiLiteHandler are set.
     */
    @Override
    public void modelChanged() {
        final InteractiveHistogramDataModel histogramModel = 
            m_model.getHistogramModelClone();
        if (histogramModel == null) {
            return;
        }
        m_plotter.reset();
        m_plotter.setHistogramDataModel(histogramModel);
        m_plotter.setHiLiteHandler(m_model.getInHiLiteHandler(0));
        m_plotter.updatePaintModel();
    }

    /**
     * @see org.knime.core.node.NodeView#onClose()
     */
    @Override
    protected void onClose() {
        return;
    }

    /**
     * @see org.knime.core.node.NodeView#onOpen()
     */
    @Override
    protected void onOpen() {
        return;
    }
}
