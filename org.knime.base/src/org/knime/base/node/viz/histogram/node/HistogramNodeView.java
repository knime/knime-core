/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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

import org.knime.base.node.viz.histogram.datamodel.AbstractHistogramVizModel;
import org.knime.base.node.viz.histogram.impl.interactive.InteractiveHistogramPlotter;
import org.knime.base.node.viz.histogram.impl.interactive.InteractiveHistogramProperties;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeView;

/**
 * The node view which contains the histogram plotter panel.
 *
 * @author Tobias Koetter, University of Konstanz
 *
 */
public class HistogramNodeView extends NodeView<HistogramNodeModel> {

    private InteractiveHistogramPlotter m_plotter;

    /**
     * Creates a new view instance for the histogram node.
     *
     * @param nodeModel the corresponding node model
     */
    HistogramNodeView(final HistogramNodeModel nodeModel) {
        super(nodeModel);
    }

    /**
     * Whenever the model changes an update for the plotter is triggered and new
     * HiLiteHandler are set.
     */
    @Override
    public void modelChanged() {
        final AbstractHistogramNodeModel model = getNodeModel();
        if (model == null) {
            return;
        }
        if (m_plotter != null) {
            m_plotter.reset();
        }
        final DataTableSpec tableSpec = model.getTableSpec();
        final AbstractHistogramVizModel vizModel =
            model.getHistogramVizModel();
        if (vizModel == null) {
            return;
        }
        if (m_plotter == null) {
            final InteractiveHistogramProperties props =
                new InteractiveHistogramProperties(tableSpec, vizModel);
            m_plotter = new InteractiveHistogramPlotter(props,
                    model.getInHiLiteHandler(0));
            // add the hilite menu to the menu bar of the node view
            getJMenuBar().add(m_plotter.getHiLiteMenu());
            setComponent(m_plotter);
        }
        m_plotter.setHiLiteHandler(model.getInHiLiteHandler(0));
        m_plotter.setHistogramVizModel(tableSpec, vizModel);
        m_plotter.updatePaintModel();
        if (getComponent() == null) {
            setComponent(m_plotter);
        }
        if (m_plotter != null) {
            m_plotter.fitToScreen();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
        return;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
        return;
    }
}
