/*
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
                        int dotSize = (Integer)
                        m_properties.getDotSizeSpinner().getValue();
                        m_distancePlotter.setDotSize(dotSize);
                        m_distancePlotter.updateSize();
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
