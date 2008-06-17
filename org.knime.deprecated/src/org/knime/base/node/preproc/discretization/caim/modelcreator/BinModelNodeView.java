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
 *   15.11.2006 (Christoph Sieb): created
 */
package org.knime.base.node.preproc.discretization.caim.modelcreator;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTabbedPane;

import org.knime.base.node.viz.plotter.AbstractPlotter;
import org.knime.base.node.viz.plotter.DataProvider;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;
import org.knime.core.node.property.hilite.HiLiteHandler;

/**
 * The view to visualize a binning model.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class BinModelNodeView extends NodeView {

    private JTabbedPane m_tabs;

    private List<AbstractPlotter> m_plotters;

    private int m_plotterCounter = 1;

    /**
     * The <code>NodeView</code> which sets the model and calls the right
     * methods of the plotter.
     * 
     * @param model the node model (must implement DataProvider).
     * @param plotter the plotter
     */
    public BinModelNodeView(final NodeModel model, 
            final AbstractPlotter plotter) {
        super(model);
        m_plotters = new ArrayList<AbstractPlotter>();
        m_plotters.add(plotter);
        ((BinModelPlotter)plotter)
                .setDiscretizationModel(((CAIMDiscretizationNodeModel)model)
                        .getDiscretizationModel());
        plotter.setHiLiteHandler(model.getInHiLiteHandler(0));
        plotter.updatePaintModel();
        setComponent(plotter);
    }

    /**
     * A generic NodeView which sets the model and calls the right methods of
     * the abstract plotter.
     * 
     * @param model the node model (must implement DataProvider).
     * @param plotter the plotter
     * @param title the title for the first tab
     */
    public BinModelNodeView(final NodeModel model,
            final AbstractPlotter plotter, final String title) {
        super(model);

        m_plotters = new ArrayList<AbstractPlotter>();
        m_plotters.add(plotter);
        ((BinModelPlotter)plotter)
                .setDiscretizationModel(((CAIMDiscretizationNodeModel)model)
                        .getDiscretizationModel());
        plotter.setDataProvider((DataProvider)model);
        plotter.setHiLiteHandler(model.getInHiLiteHandler(0));
        m_tabs = new JTabbedPane();
        m_tabs.addTab(title, plotter);
        setComponent(m_tabs);
    }

    /**
     * Adds another tab containing a plotter.
     * 
     * @param plotter another visualization
     * @param title the title of the tab (if null a standard name is provided)
     */
    public void addVisualization(final AbstractPlotter plotter,
            final String title) {
        m_plotterCounter++;
        String name = title;
        if (name == null) {
            name = "Visualization#" + m_plotterCounter;
        }
        // check if there is already a tab
        if (m_tabs == null) {
            m_tabs = new JTabbedPane();
            AbstractPlotter oldPlotter = m_plotters.get(1);
            m_tabs.addTab("Visualization#1", oldPlotter);
            m_tabs.addTab(name, plotter);
            setComponent(m_tabs);
        } else {
            m_tabs.addTab(name, plotter);
        }
        m_plotters.add(plotter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        NodeModel model = getNodeModel();
        if (model == null) {
            return;
        }

        HiLiteHandler hiliteHandler = model.getInHiLiteHandler(0);
        if (m_plotters != null) {
            for (AbstractPlotter plotter : m_plotters) {
                plotter.reset();
                plotter.setHiLiteHandler(hiliteHandler);
                ((BinModelPlotter)plotter)
                    .setDiscretizationModel(((CAIMDiscretizationNodeModel)model)
                                .getDiscretizationModel());
                plotter.updatePaintModel();
                plotter.getDrawingPane().repaint();
            }
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
