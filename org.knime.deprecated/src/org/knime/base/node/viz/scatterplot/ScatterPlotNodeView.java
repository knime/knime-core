/*
 *
 * --------------------------------------------------------------------- *
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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.viz.scatterplot;

import org.knime.base.node.util.DataArray;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.node.NodeView;

/**
 * This view displays a scatter plot of a DataTable. The user has selected two
 * columns for the x- and y-axes. The view will now display the rows with a
 * certain zoom factor and dot sze, that can be set in the view.
 * 
 * This class brings together user settings and plotter.
 * 
 * @author Christoph Sieb, University of Konstanz
 * @author ohl University of Konstanz
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

            setViewTitle(getViewName() + " " 
                    + constructTitle((ScatterPlotNodeModel)getNodeModel()));
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

    private static String constructTitle(final ScatterPlotNodeModel model) {
        StringBuffer result = new StringBuffer("<");
        if (model != null) {
            DataArray rows = model.getRowsContainer();
            if (rows != null) {
                result.append(rows.getDataTableSpec().getName());
                result.append("> shows datapoints ");
                result.append("" + rows.getFirstRowNumber());
                result.append(" to "
                        + (rows.getFirstRowNumber() + rows.size() - 1));
            } else {
                result.append("<no data to display>");
            }
        } else {
            result.append("<no model set>");
        }
        return result.toString();
    }

}
