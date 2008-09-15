/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Mar 10, 2008 (sellien): created
 */
package org.knime.base.node.viz.liftchart;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * The factory for a lift chart node.
 * @author Stephan Sellien, University of Konstanz
 */
public class LiftChartNodeFactory extends NodeFactory<LiftChartNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new LiftChartNodeDialog();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LiftChartNodeModel createNodeModel() {
        return new LiftChartNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<LiftChartNodeModel> createNodeView(final int viewIndex,
            final LiftChartNodeModel nodeModel) {
        return new LiftChartNodeView(nodeModel);
        /*
        LinePlotterProperties props = new LinePlotterProperties();
        // this is a hack
        // TODO: make default name static and add constructor with name
        int missValPos = props.indexOfTab("Missing Values");
        props.removeTabAt(missValPos);
        props.setTitleAt(props.indexOfTab("Column Selection"), 
                "Line Selection");
        LinePlotter linePlot = new LinePlotter(new LinePlotterDrawingPane(),
                props);
        linePlot.removeMouseListener(
                AbstractPlotter.SelectionMouseListener.class);
        DefaultVisualizationNodeView nodeView =
                new DefaultVisualizationNodeView(nodeModel, linePlot,
                        "Lift chart");
        
        LinePlotter lineplotter2 = new LinePlotter();
        lineplotter2.setDataProvider(new DataProvider() {

            public DataArray getDataArray(int index) {
                return ((LiftChartNodeModel)nodeModel).getDataArray(1);
            }
            
        });
//        lineplotter2.setDataArrayIdx(1);
        nodeView.addVisualization(lineplotter2,
                "Cumulative Percent of Responses Captured");
                
        return nodeView;
        */
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getNrNodeViews() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasDialog() {
        return true;
    }

}
