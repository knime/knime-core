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
 * 
 * History
 *   08.06.2006 (Tobias Koetter): created
 */
package org.knime.base.node.viz.histogram.node;

import org.knime.base.node.viz.histogram.AggregationMethod;
import org.knime.base.node.viz.histogram.datamodel.FixedHistogramDataModel;
import org.knime.base.node.viz.histogram.impl.fixed.FixedHistogramPlotter;
import org.knime.base.node.viz.histogram.impl.fixed.FixedHistogramProperties;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;

/**
 * The node view which contains the histogram plotter panel.
 * 
 * @author Tobias Koetter, University of Konstanz
 * 
 */
public class FixedColumnHistogramNodeView extends NodeView {
    
    private final FixedColumnHistogramNodeModel m_nodeModel;
    
    private FixedHistogramPlotter m_plotter;

    /**
     * Creates a new view instance for the histogram node.
     * 
     * @param nodeModel the corresponding node model
     */
    FixedColumnHistogramNodeView(final NodeModel nodeModel) {
        super(nodeModel);
        if (!(nodeModel instanceof FixedColumnHistogramNodeModel)) {
            throw new IllegalArgumentException(NodeModel.class.getName()
                    + " not an instance of "
                    + HistogramNodeModel.class.getName());
        }
        m_nodeModel = (FixedColumnHistogramNodeModel)nodeModel;
    }

    /**
     * Clears the plot and unregisters from any hilite handler.
     */
    public void reset() {
        m_nodeModel.reset();
    }

    /**
     * Whenever the model changes an update for the plotter is triggered and new
     * HiLiteHandler are set.
     */
    @Override
    public void modelChanged() {
        if (m_nodeModel == null) {
            return;
        }
        final FixedHistogramDataModel histogramModel = 
            m_nodeModel.getHistogramModel();
        final DataTableSpec tableSpec = m_nodeModel.getTableSpec();
        if (histogramModel == null) {
            return;
        }
        if (m_plotter == null) {
            final FixedHistogramProperties props =
                new FixedHistogramProperties(
                        AggregationMethod.COUNT);
            m_plotter = new FixedHistogramPlotter(props, histogramModel, 
                    tableSpec, m_nodeModel.getInHiLiteHandler(0));
            // add the hilite menu to the menu bar of the node view
            getJMenuBar().add(m_plotter.getHiLiteMenu());
            setComponent(m_plotter);
        } else {
            m_plotter.reset();
            m_plotter.setDataTableSpec(tableSpec);
            m_plotter.setHistogramDataModel(histogramModel);
            m_plotter.setHiLiteHandler(m_nodeModel.getInHiLiteHandler(0));
            m_plotter.updatePaintModel();
        }
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
