/*  
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
 *   Mar 30, 2006 (wiswedel): created
 */
package org.knime.base.node.mine.regression.linear.view;

import java.util.ArrayList;

import org.knime.base.node.mine.regression.linear.LinearRegressionParams;
import org.knime.base.node.util.DataArray;
import org.knime.base.node.viz.scatterplot.ScatterProps;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;

/**
 * 2D plot showing the linear regression line. The plot allows to choose one
 * input column as x-coordinate and has the y-coordinate fixed to the response
 * variable.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class LinRegLineNodeView extends NodeView {
    // the scoll and drawing pane
    private LinRegLineScatterPlotter m_plot;

    // the pane holding the always visible controls
    private ScatterProps m_properties;

    private final int m_initialWIDTH = 300; // the width at zoom 1x

    /**
     * Create new view.
     * 
     * @param nodeModel the model to look at
     */
    public LinRegLineNodeView(final NodeModel nodeModel) {
        super(nodeModel);
        if (!(nodeModel instanceof LinRegDataProvider)) {
            throw new IllegalArgumentException(
                    "NodeModel not instance of LinRegDataProvider");
        }
        m_properties = new ScatterProps();

        DataArray container = getLinRegDataProvider().getRowContainer();
        m_plot = new LinRegLineScatterPlotter(container, m_initialWIDTH,
                m_properties);
        m_plot.setBackground(ColorAttr.getBackground());
        getJMenuBar().add(m_plot.getHiLiteMenu());
        setComponent(m_plot);
    }

    /* Get the data provider interface that the model implements. */
    private LinRegDataProvider getLinRegDataProvider() {
        return (LinRegDataProvider)getNodeModel();
    }

    /**
     * @see org.knime.core.node.NodeView#modelChanged()
     */
    @Override
    protected void modelChanged() {
        LinRegDataProvider provider = getLinRegDataProvider();

        // update the x/y col selectors, this should trigger
        DataArray rows = provider.getRowContainer();
        if (rows != null) {
            m_properties.setSelectables(rows.getDataTableSpec());
        } else {
            m_properties.setSelectables(null);
        }
        LinearRegressionParams params = provider.getParams();
        if (params != null) {
            m_properties.fixYColTo(params.getTargetColumnName());
            ArrayList<String> variables = new ArrayList<String>(params.getMap()
                    .keySet());
            // remove the target attribute from the variables list
            variables.remove(params.getTargetColumnName());
            m_properties.fixXColTo(variables.toArray(new String[0]));
        }
        // clear the plot
        m_plot.clear();
        // could be the property handler,
        m_plot.setHiLiteHandler(getNodeModel().getInHiLiteHandler(0));
        // or the data table.
        m_plot.modelDataChanged(rows, provider.getParams());
    }

    /**
     * @see org.knime.core.node.NodeView#onClose()
     */
    @Override
    protected void onClose() {
        m_plot.shutDown();
    }

    /**
     * @see org.knime.core.node.NodeView#onOpen()
     */
    @Override
    protected void onOpen() {
        m_plot.setHiLiteHandler(getNodeModel().getInHiLiteHandler(0));
    }
}
