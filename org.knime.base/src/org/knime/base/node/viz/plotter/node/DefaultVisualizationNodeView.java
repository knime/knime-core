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
 *   30.08.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.node;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JTabbedPane;

import org.knime.base.node.viz.plotter.AbstractPlotter;
import org.knime.base.node.viz.plotter.DataProvider;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;
import org.knime.core.node.property.hilite.HiLiteHandler;

/**
 * Convenient implementation of a {@link org.knime.core.node.NodeView} that
 * can display one or more plotter implementations. One plotter
 * implementation has to be passed to the constructor and additional plotters
 * can be added as tabs with
 * {@link #addVisualization(AbstractPlotter, String)}. The appropriate
 * update methods are called by this class for all added plotters.
 *
 * @author Fabian Dill, University of Konstanz
 */
public class DefaultVisualizationNodeView extends NodeView {

    private JTabbedPane m_tabs;

    private final List<AbstractPlotter>m_plotters;

    private int m_plotterCounter = 1;


    /**
     * A generic {@link org.knime.core.node.NodeView} which sets the model and
     * calls the right methods of the plotters.
     *
     * @param model the node model (must implement DataProvider).
     * @param plotter the plotter
     */
    public DefaultVisualizationNodeView(final NodeModel model,
            final AbstractPlotter plotter) {
        super(model);
        if (!(model instanceof DataProvider)) {
            throw new IllegalArgumentException(
                    "Model must implement the DataProvider interface!");
        }
        m_plotters = new ArrayList<AbstractPlotter>();
        m_plotters.add(plotter);
        plotter.setDataProvider((DataProvider)model);

        plotter.setHiLiteHandler(model.getInHiLiteHandler(0));
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
    public DefaultVisualizationNodeView(final NodeModel model,
            final AbstractPlotter plotter, final String title) {
        super(model);
        if (!(model instanceof DataProvider)) {
            throw new IllegalArgumentException(
                    "Model must implement the DataProvider interface!");
        }
        m_plotters = new ArrayList<AbstractPlotter>();
        m_plotters.add(plotter);
        plotter.setDataProvider((DataProvider)model);
        plotter.setHiLiteHandler(model.getInHiLiteHandler(0));
        m_tabs = new JTabbedPane();
        m_tabs.addTab(title, plotter);
        if (plotter.getHiLiteMenu() != null) {
            getJMenuBar().add(getHiLiteMenu());
        }
        setComponent(m_tabs);
    }

    /**
     * Adds another tab with title <code>title</code> containing a plotter.
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
        if (!(model instanceof DataProvider)) {
            throw new IllegalArgumentException(
                    "Model must implement the DataProvider "
                    + "interface!");
        }
        DataProvider provider = (DataProvider)model;
        HiLiteHandler hiliteHandler = model.getInHiLiteHandler(0);
        // do not care about antialias
        for (AbstractPlotter plotter : m_plotters) {
            plotter.reset();
            plotter.setHiLiteHandler(hiliteHandler);
            plotter.setDataProvider(provider);
            plotter.updatePaintModel();
        }
    }


    /**
     * Dynamically creates a hilite menu with the typical hilite options:
     * hilite, unhilite and clear hilite. Determines the currently selected
     * plotter and forwards to it's corresponding action.
     *
     * @return a hilite menu which forwards the actions to the currently
     * selected plotter.
     */
    protected JMenu getHiLiteMenu() {
        JMenu menu = new JMenu(HiLiteHandler.HILITE);
        menu.add(new AbstractAction(HiLiteHandler.HILITE_SELECTED) {

            public void actionPerformed(final ActionEvent e) {
                getActivePlotter().getHiliteAction().actionPerformed(e);
            }

        });
        menu.add(new AbstractAction(HiLiteHandler.UNHILITE_SELECTED) {

            public void actionPerformed(final ActionEvent e) {
                getActivePlotter().getUnhiliteAction().actionPerformed(e);
            }
        });
        menu.add(new AbstractAction(HiLiteHandler.CLEAR_HILITE) {

            public void actionPerformed(final ActionEvent e) {
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
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
        for (AbstractPlotter plotter : m_plotters) {
            plotter.dispose();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
    }

}
