/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   Feb 25, 2008 (sellien): created
 */
package org.knime.base.node.viz.condbox;

import org.knime.base.node.viz.plotter.box.BoxPlotter;
import org.knime.core.node.GenericNodeView;
import org.knime.core.node.NodeModel;

/**
 * Class for a view of the conditional box plot.
 * 
 * @author Stephan Sellien, University of Konstanz
 * 
 */
public class ConditionalBoxPlotNodeView extends GenericNodeView<NodeModel> {

    private final BoxPlotter m_plotter = new BoxPlotter();

    /**
     * Creates a view for the conditional box plot.
     * 
     * @param nodeModel the model
     */
    protected ConditionalBoxPlotNodeView(
            final ConditionalBoxPlotNodeModel nodeModel) {
        super(nodeModel);
        m_plotter.setDataProvider(nodeModel);
        getJMenuBar().add(m_plotter.getHiLiteMenu());
        setComponent(m_plotter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        NodeModel model = getNodeModel();
        if (model != null && model instanceof ConditionalBoxPlotNodeModel) {
            ConditionalBoxPlotNodeModel nodemodel =
                    (ConditionalBoxPlotNodeModel)model;
            m_plotter.reset();
            m_plotter.setHiLiteHandler(nodemodel.getInHiLiteHandler(0));
            m_plotter.setAntialiasing(true);
            m_plotter.setDataProvider(nodemodel);
            m_plotter.updatePaintModel();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
    }

}
