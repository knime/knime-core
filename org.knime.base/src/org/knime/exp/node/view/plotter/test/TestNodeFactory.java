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
 *   24.08.2006 (Fabian Dill): created
 */
package org.knime.exp.node.view.plotter.test;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;
import org.knime.exp.node.view.plotter.AbstractPlotter;
import org.knime.exp.node.view.plotter.node.DefaultVisualizationNodeView;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class TestNodeFactory extends NodeFactory {

    /**
     * @see org.knime.core.node.NodeFactory#createNodeDialogPane()
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {

        return null;
    }

    /**
     * @see org.knime.core.node.NodeFactory#createNodeModel()
     */
    @Override
    public NodeModel createNodeModel() {
        return new TestNodeModel();
    }

    /**
     * @see org.knime.core.node.NodeFactory#createNodeView(int, 
     * org.knime.core.node.NodeModel)
     */
    @Override
    public NodeView createNodeView(final int viewIndex, 
            final NodeModel nodeModel) {
        AbstractPlotter[] plotter = ((TestNodeModel)nodeModel).getPlotter();
        AbstractPlotter scatterPlotter = plotter[0];
        AbstractPlotter linePlotter = plotter[1];
        AbstractPlotter parallelCoords = plotter[2];
        DefaultVisualizationNodeView view = new DefaultVisualizationNodeView(
                nodeModel, scatterPlotter, "Scatter Plotter");
        view.addVisualization(linePlotter, "Line Plotter");
        view.addVisualization(parallelCoords, "Parallel Coordinates");
        return view;
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
        return false;
    }

}
