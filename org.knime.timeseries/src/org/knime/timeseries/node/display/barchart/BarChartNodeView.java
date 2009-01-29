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
package org.knime.timeseries.node.display.barchart;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JTabbedPane;

import org.knime.base.node.viz.plotter.AbstractPlotter;
import org.knime.base.node.viz.plotter.DataProvider;
import org.knime.base.node.viz.plotter.node.DefaultVisualizationNodeView;
import org.knime.core.node.property.hilite.HiLiteHandler;

/**
 * Extension of 
 * {@link org.knime.base.node.viz.plotter.node.DefaultVisualizationNodeView} 
 * The constructor passes the column index to be used to label 
 * the x-axis values to the time plotter.
 * 
 * @author Rosaria Silipo
 */
public class BarChartNodeView extends DefaultVisualizationNodeView {
    
    private JTabbedPane m_tabs;
    
    private List<AbstractPlotter>m_plotters;
    
    
    /**
     * A generic {@link org.knime.core.node.NodeView} which sets the model and 
     * calls the right methods of the plotters.
     * 
     * @param model the node model (must implement DataProvider).
     * @param plotter the plotter
     */
    public BarChartNodeView(final BarChartNodeModel model, 
            final BarChartPlotter plotter) {
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
     * A generic {@link org.knime.core.node.NodeView} which sets the model and 
     * calls the right methods of the plotters the title is the title of the 
     * according tab.
     * 
     * @param model the node model (must implement DataProvider).
     * @param plotter the plotter
     * @param title the title for the first tab
     */
    public BarChartNodeView(final BarChartNodeModel model, 
            final BarChartPlotter plotter, final String title) {
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
    
    /**
     * Dynamically creates a hilite menu with the typical hilite options:
     * hilite, unhilite and clear hilite. Determines the currently selected 
     * plotter and forwards to it's corresponding action.
     * 
     * @return a hilite menu which forwards the actions to the currently 
     * selected plotter.
     */
    @Override
    protected JMenu getHiLiteMenu() {
        JMenu menu = new JMenu(HiLiteHandler.HILITE);
        menu.add(new AbstractAction(HiLiteHandler.HILITE_SELECTED) {

            public void actionPerformed(ActionEvent e) {
                getActivePlotter().getHiliteAction().actionPerformed(e);
            }
            
        });
        menu.add(new AbstractAction(HiLiteHandler.UNHILITE_SELECTED) {

            public void actionPerformed(ActionEvent e) {
                getActivePlotter().getUnhiliteAction().actionPerformed(e);
            }
        });
        menu.add(new AbstractAction(HiLiteHandler.CLEAR_HILITE) {

            public void actionPerformed(ActionEvent e) {
                getActivePlotter().getClearHiliteAction().actionPerformed(e);
            }
        });
        return menu;
    }
    
    private AbstractPlotter getActivePlotter() {
        if (m_tabs == null) {
            return m_plotters.get(0);
        }
        return (AbstractPlotter)m_tabs.getSelectedComponent();
    }

    /**
     * @see org.knime.core.node.NodeView#onClose()
     */
    @Override
    protected void onClose() {
    }

    /**
     * @see org.knime.core.node.NodeView#onOpen()
     */
    @Override
    protected void onOpen() {
    }

}
