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
*   06.02.2007 (rs): created from line plot node
 */
package org.knime.exp.timeseries.node.display.barchart;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;
import org.knime.base.node.viz.plotter.node.DefaultVisualizationNodeView;

/**
 * 
 * @author Rosaria Silipo
 */
public class BarChartNodeFactory extends NodeFactory {

    /**
     * @see org.knime.core.node.NodeFactory#createNodeDialogPane()
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new BarChartNodeDialog();
    }

    /**
     * @see org.knime.core.node.NodeFactory#createNodeModel()
     */
    @Override
    public NodeModel createNodeModel() {
        return new BarChartNodeModel();
    }

    /**
     * @see org.knime.core.node.NodeFactory#createNodeView(int, 
     * org.knime.core.node.NodeModel)
     */
    @Override
    public NodeView createNodeView(final int viewIndex, 
            final NodeModel nodeModel) {
        return new DefaultVisualizationNodeView(nodeModel, new BarChartPlotter());
    }

    /**
     * @see org.knime.core.node.NodeFactory#getNrNodeViews()
     */
    @Override
    protected int getNrNodeViews() {
        return 1;
    }

    /**
     * @see org.knime.core.node.NodeFactory#hasDialog()
     */
    @Override
    protected boolean hasDialog() {
        return true;
    }

}
