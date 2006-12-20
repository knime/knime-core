/*
 * ------------------------------------------------------------------- 
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 */

package org.knime.base.node.mine.cluster.hierarchical;

import java.awt.Color;

import org.knime.base.node.util.DataArray;
import org.knime.base.node.viz.plotter.DataProvider;
import org.knime.base.node.viz.plotter.dendrogram.ClusterNode;
import org.knime.base.node.viz.plotter.dendrogram.DendrogramPlotter;
import org.knime.base.node.viz.plotter.node.DefaultVisualizationNodeView;
import org.knime.base.node.viz.plotter.scatter.ScatterPlotter;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.NodeModel;

/**
 * This view displays the scoring results.
 * It needs to be hooked up with a scoring model.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class HierarchicalClusterNodeView extends DefaultVisualizationNodeView {

    private DendrogramPlotter m_dendroPlotter;
    
    private ScatterPlotter m_distancePlotter;

    /**
     * creates a new ScorerNodeView with scroll bars.
     * 
     * The view consists of the table with the example data and the 
     * appropriate scoring in the upper part and the summary of correct
     * and wrong classified examples in the lower part.
     * 
     * @param nodeModel The underlying <code>NodeModel</code>.
     * @param dendrogramPlotter the dendrogram plotter.
     */
    public HierarchicalClusterNodeView(final NodeModel nodeModel, 
            final DendrogramPlotter dendrogramPlotter) { 
        super(nodeModel, dendrogramPlotter, "Dendrogram");
        m_dendroPlotter = dendrogramPlotter;
        m_distancePlotter = new ScatterPlotter();
        addVisualization(m_distancePlotter, "Distance");
    }


    /**
     * 
     * @see org.knime.base.node.viz.plotter.node.DefaultVisualizationNodeView#
     * modelChanged()
     */
    @Override
    public void modelChanged() {
        if (getNodeModel() == null 
                || ((DataProvider)getNodeModel()).getDataArray(0) == null) {
            return;
        }
        NodeModel model = getNodeModel();
        m_dendroPlotter.reset();
        m_distancePlotter.reset();
        m_dendroPlotter.setHiLiteHandler(model.getInHiLiteHandler(0));
        m_dendroPlotter.setAntialiasing(false);
        m_dendroPlotter.setDataProvider((DataProvider)model);
        m_distancePlotter.setDataProvider((DataProvider)model);
        m_distancePlotter.setHiLiteHandler(model.getInHiLiteHandler(0));
        ClusterNode rootNode = ((HierarchicalClusterNodeModel)getNodeModel())
            .getRootNode();
        DataArray distanceTable = ((DataProvider)getNodeModel())
            .getDataArray(0);
        m_dendroPlotter.setRootNode(rootNode);
        m_distancePlotter.createXCoordinate(
                ((DoubleValue)distanceTable.getDataTableSpec().getColumnSpec(0)
                        .getDomain().getLowerBound()).getDoubleValue(),
                        ((DoubleValue)distanceTable.getDataTableSpec()
                                .getColumnSpec(0).getDomain().getUpperBound())
                                .getDoubleValue());
        m_distancePlotter.createYCoordinate(
                ((DoubleValue)distanceTable.getDataTableSpec().getColumnSpec(1)
                        .getDomain().getLowerBound()).getDoubleValue(),
                        ((DoubleValue)distanceTable.getDataTableSpec()
                                .getColumnSpec(1).getDomain().getUpperBound())
                                .getDoubleValue());
        m_distancePlotter.addLine(distanceTable, 1, Color.BLACK, null);
        
        m_distancePlotter.updatePaintModel();
        m_dendroPlotter.updatePaintModel();
    }

    /**
     * @see org.knime.core.node.NodeView#onClose()
     */
    @Override
    protected void onClose() {
        // nothing to do
    }

    /**
     * @see org.knime.core.node.NodeView#onOpen()
     */
    @Override
    protected void onOpen() {
        // do nothing here        
    }

}
