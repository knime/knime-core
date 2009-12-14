/*
 *
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.viz.scatterplot;

import org.knime.base.node.util.DataArray;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.node.NodeView;

/**
 * This view displays a scatter plot of a DataTable. The user has selected two
 * columns for the x- and y-axes. The view will now display the rows with a
 * certain zoom factor and dot size, that can be set in the view.
 * 
 * This class brings together user settings and plotter.
 * 
 * @author Peter Ohl, University of Konstanz
 */
public class ScatterPlotNodeView extends NodeView {
    

    // the scoll and drawing pane
    private ScatterPlotter m_plot;

    // the pane holding the always visible controls
    private ScatterProps m_properties;

    private final int m_initialWIDTH = 300; // the width at zoom 1x

    /**
     * creates a new ScatterPlotNodeView with scroll bars and a little
     * properties panel at the bottom.
     * 
     * @param nodeModel The underlying model.
     */
    public ScatterPlotNodeView(final ScatterPlotNodeModel nodeModel) {
        super(nodeModel);

        m_properties = new ScatterProps();

        assert (nodeModel != null) : "In ScatterPlotNodeView constructor. "
                + "Model is null!!";

        m_plot = new ScatterPlotter(nodeModel.getRowsContainer(),
                m_initialWIDTH, m_properties);
        m_plot.setBackground(ColorAttr.getBackground());
        setComponent(m_plot);

        this.getJMenuBar().add(m_plot.getHiLiteMenu());
    }

    /**
     * This is going to be called by the model if the model data has changed.
     * 
     * @see NodeView#modelChanged()
     */
    @Override
    public synchronized void modelChanged() {

        ScatterPlotNodeModel model = (ScatterPlotNodeModel)getNodeModel();

        if (model != null) {


            // clear the plot
            m_plot.clear();

            // could be the property handler,
            m_plot.setHiLiteHandler(getNodeModel().getInHiLiteHandler(0));

            // or the data table.
            m_plot.modelDataChanged(model.getRowsContainer());
            
            // update the x/y col selectors, this should trigger
            DataArray rows = model.getRowsContainer();
            if (rows != null) {
                m_properties.setSelectables(rows.getDataTableSpec());
            } else {
                m_properties.setSelectables(null);
            }
        }
    }

    /**
     * Clears the plot and unregisters from any hilite handler.
     */
    public void reset() {
        m_plot.clear();
        m_plot.setHiLiteHandler(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
        m_plot.shutDown();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
        m_plot.setHiLiteHandler(getNodeModel().getInHiLiteHandler(0));
    }

}
