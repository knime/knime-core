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
 *   07.02.2007 (Rosaria Silipo): created
 */
package org.knime.timeseries.node.display.timeplot;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTabbedPane;

import org.knime.base.node.viz.plotter.AbstractPlotter;
import org.knime.base.node.viz.plotter.DataProvider;
import org.knime.base.node.viz.plotter.node.DefaultVisualizationNodeView;

/**
 * Extension of 
 * {@link org.knime.base.node.viz.plotter.node.DefaultVisualizationNodeView} 
 * The constructor passes the column index to be used to label 
 * the x-axis values to the time plotter.
 * 
 * @author Rosaria Silipo
 */
public class TimePlotNodeView extends DefaultVisualizationNodeView {
    
    private JTabbedPane m_tabs;
    
    private List<AbstractPlotter>m_plotters;
    
    
    /**
     * A generic {@link org.knime.core.node.NodeView} which sets the model and 
     * calls the right methods of the plotters.
     * 
     * @param model the node model (must implement DataProvider).
     * @param plotter the plotter
     */
    public TimePlotNodeView(final TimePlotNodeModel model, 
            final TimePlotter plotter) {
        super(model, plotter);
        if (!(model instanceof DataProvider)) {
            throw new IllegalArgumentException(
                    "Model must implement the DataProvider interface!");
        }
        m_plotters = new ArrayList<AbstractPlotter>();
        m_plotters.add(plotter);
        plotter.setDataProvider((DataProvider)model);
        plotter.setHiLiteHandler(model.getInHiLiteHandler(0));
        plotter.setXColumnIndex(model.getColXIndex());
        if (plotter.getHiLiteMenu() != null) {
            getJMenuBar().add(getHiLiteMenu());
        }
        setComponent(plotter);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        for (AbstractPlotter plotterle : m_plotters) {
            if (plotterle instanceof TimePlotter) {
                ((TimePlotter)plotterle)
                       .setXColumnIndex(((TimePlotNodeModel)getNodeModel())
                       .getColXIndex());
            }
        }
        // TODO Auto-generated method stub
        super.modelChanged();
    }
    
    /**
     * A generic {@link org.knime.core.node.NodeView} which sets the model and 
     * calls the right methods of the plotters the title is the title of the 
     * according tab.
     * 
     * @param model the node model (must implement DataProvider).
     * @param plotter the plotter
     * @param title the title for the first tab
     */
    public TimePlotNodeView(final TimePlotNodeModel model, 
            final TimePlotter plotter, final String title) {
        super(model, plotter);
        if (!(model instanceof DataProvider)) {
            throw new IllegalArgumentException(
                    "Model must implement the DataProvider interface!");
        }
        m_plotters = new ArrayList<AbstractPlotter>();
        m_plotters.add(plotter);
        plotter.setDataProvider((DataProvider)model);
        plotter.setHiLiteHandler(model.getInHiLiteHandler(0));
        plotter.setXColumnIndex(model.getColXIndex());

        m_tabs = new JTabbedPane();
        m_tabs.addTab(title, plotter);
        if (plotter.getHiLiteMenu() != null) {
            getJMenuBar().add(getHiLiteMenu());
        }
        setComponent(m_tabs);
    }    
    
}
