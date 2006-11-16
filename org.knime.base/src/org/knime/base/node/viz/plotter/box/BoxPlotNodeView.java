/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   29.09.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.box;

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
     * @see org.knime.core.node.NodeView#modelChanged()
     */
    @Override
    protected void modelChanged() {
        if (getNodeModel() != null) {
            m_plotter.setDataProvider((BoxPlotNodeModel)getNodeModel());
            m_plotter.updatePaintModel();
        }
    }

    /**
     * @see org.knime.core.node.NodeView#onClose()
     */
    @Override
    protected void onClose() {
        // TODO Auto-generated method stub

    }

    /**
     * @see org.knime.core.node.NodeView#onOpen()
     */
    @Override
    protected void onOpen() {
        m_plotter.fitToScreen();
    }

}
