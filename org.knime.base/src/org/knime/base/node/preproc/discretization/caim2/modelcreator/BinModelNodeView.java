/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 *
 * History
 *   15.11.2006 (Christoph Sieb): created
 */
package org.knime.base.node.preproc.discretization.caim2.modelcreator;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTabbedPane;

import org.knime.base.node.viz.plotter.AbstractPlotter;
import org.knime.base.node.viz.plotter.DataProvider;
import org.knime.core.node.NodeView;
import org.knime.core.node.property.hilite.HiLiteHandler;

/**
 * The view to visualize a binning model.
 *
 * @author Christoph Sieb, University of Konstanz
 */
public class BinModelNodeView extends
        NodeView<CAIMDiscretizationNodeModel> {

    private JTabbedPane m_tabs;

    private final List<AbstractPlotter> m_plotters;

    private int m_plotterCounter = 1;

    /**
     * The <code>NodeView</code> which sets the model and calls the right
     * methods of the plotter.
     *
     * @param model the node model (must implement DataProvider).
     * @param plotter the plotter
     */
    public BinModelNodeView(final CAIMDiscretizationNodeModel model,
            final AbstractPlotter plotter) {
        super(model);
        m_plotters = new ArrayList<AbstractPlotter>();
        m_plotters.add(plotter);
        ((BinModelPlotter)plotter).setDiscretizationModel(model
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
    public BinModelNodeView(final CAIMDiscretizationNodeModel model,
            final AbstractPlotter plotter, final String title) {
        super(model);

        m_plotters = new ArrayList<AbstractPlotter>();
        m_plotters.add(plotter);
        ((BinModelPlotter)plotter)
                .setDiscretizationModel(model.getDiscretizationModel());
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
        CAIMDiscretizationNodeModel model = getNodeModel();
        if (model == null) {
            return;
        }

        HiLiteHandler hiliteHandler = model.getInHiLiteHandler(0);
        for (AbstractPlotter plotter : m_plotters) {
            plotter.reset();
            plotter.setHiLiteHandler(hiliteHandler);
            ((BinModelPlotter)plotter)
                .setDiscretizationModel((model)
                            .getDiscretizationModel());
            plotter.updatePaintModel();
            plotter.getDrawingPane().repaint();
        }
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
        // empty.
    }

}
