/*
 * -------------------------------------------------------------------
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
 */

package org.knime.base.node.mine.cluster.hierarchical;

import java.awt.BasicStroke;
import java.awt.Color;

import javax.swing.JPopupMenu;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.util.DataArray;
import org.knime.base.node.viz.plotter.AbstractPlotter;
import org.knime.base.node.viz.plotter.DataProvider;
import org.knime.base.node.viz.plotter.basic.BasicDrawingPane;
import org.knime.base.node.viz.plotter.dendrogram.DendrogramNode;
import org.knime.base.node.viz.plotter.dendrogram.DendrogramPlotter;
import org.knime.base.node.viz.plotter.node.DefaultVisualizationNodeView;
import org.knime.base.node.viz.plotter.scatter.ScatterPlotter;
import org.knime.base.node.viz.plotter.scatter.ScatterPlotterDrawingPane;
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

    private DistancePlotProperties m_properties;

    private int m_thickness = 1;

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
        m_properties = new DistancePlotProperties();
        m_distancePlotter = new ScatterPlotter(new ScatterPlotterDrawingPane(),
                m_properties) {

            @Override
            public void fillPopupMenu(final JPopupMenu popupMenu) {
                // nothing: no hiliting supported
            }

            @Override
            public void hiLiteSelected() {
                // nothing: no hilite supported
            }

            @Override
            public void unHiLiteSelected() {
                // nothing: no hilite supported
            }

        };
        // no selection should be possible so remove the selection listener
        m_distancePlotter.removeMouseListener(
                AbstractPlotter.SelectionMouseListener.class);


        m_properties.getDotSizeSpinner().addChangeListener(
                new ChangeListener() {
                    public void stateChanged(final ChangeEvent e) {
                        ((ScatterPlotterDrawingPane)m_distancePlotter
                                .getDrawingPane()).setDotSize((Integer)
                                m_properties.getDotSizeSpinner().getValue());
                        m_distancePlotter.getDrawingPane().repaint();
                    }

        });
        m_properties.getThicknessSpinner().addChangeListener(
                new ChangeListener() {
                    public void stateChanged(final ChangeEvent e) {
                        m_thickness = (Integer)m_properties
                            .getThicknessSpinner().getValue();
                        modelChanged();
                    }

        });
        m_properties.getShowHideCheckbox().addChangeListener(
                new ChangeListener() {
                    public void stateChanged(final ChangeEvent e) {
                        m_distancePlotter.setHideMode(
                                !m_properties.getShowHideCheckbox()
                                .isSelected());
                        m_distancePlotter.updatePaintModel();
                    }

        });
        addVisualization(m_distancePlotter, "Distance");
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void modelChanged() {
        if (getNodeModel() == null
                || ((DataProvider)getNodeModel()).getDataArray(0) == null
                || ((DataProvider)getNodeModel()).getDataArray(0).size() == 0) {
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
        DendrogramNode rootNode = ((HierarchicalClusterNodeModel)getNodeModel())
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
        ((BasicDrawingPane)m_distancePlotter.getDrawingPane()).clearPlot();
        m_distancePlotter.addLine(distanceTable, 0, 1, Color.BLACK,
                new BasicStroke(m_thickness));

//        m_distancePlotter.getXAxis().getCoordinate().setPolicy(
//                DescendingNumericTickPolicyStrategy.getInstance());

        m_distancePlotter.updatePaintModel();

        m_dendroPlotter.updatePaintModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
        m_distancePlotter.dispose();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
        // do nothing here
    }

}
