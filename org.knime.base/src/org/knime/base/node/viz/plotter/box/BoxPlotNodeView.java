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
 * -------------------------------------------------------------------
 * 
 * History
 *   29.09.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.box;

import org.knime.base.node.viz.plotter.DataProvider;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class BoxPlotNodeView extends NodeView {
    
    private BoxPlotter m_plotter;
    
    /**
     * 
     * @param model the model
     * @param plotter the plotter
     */
    public BoxPlotNodeView(final BoxPlotNodeModel model, 
            final BoxPlotter plotter) {
        super(model);
        m_plotter = plotter;
        m_plotter.setDataProvider(model);
        getJMenuBar().add(m_plotter.getHiLiteMenu());
        setComponent(m_plotter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        if (getNodeModel() != null) {
            NodeModel model = getNodeModel();
            m_plotter.reset();
            m_plotter.setHiLiteHandler(model.getInHiLiteHandler(0));
            m_plotter.setAntialiasing(true);
            m_plotter.setDataProvider((DataProvider)model);
            m_plotter.updatePaintModel();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
    }

}
