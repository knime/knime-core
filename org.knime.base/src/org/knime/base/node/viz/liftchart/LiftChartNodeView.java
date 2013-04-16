/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   21.04.2008 (Fabian Dill): created
 */
package org.knime.base.node.viz.liftchart;

import javax.swing.JTabbedPane;

import org.knime.base.node.util.DataArray;
import org.knime.base.node.viz.plotter.AbstractPlotter;
import org.knime.base.node.viz.plotter.DataProvider;
import org.knime.base.node.viz.plotter.line.LinePlotter;
import org.knime.base.node.viz.plotter.line.LinePlotterDrawingPane;
import org.knime.base.node.viz.plotter.line.LinePlotterProperties;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class LiftChartNodeView extends NodeView<LiftChartNodeModel> {
    
    private LinePlotter m_liftChart;
    private LinePlotter m_gainChart;
    private JTabbedPane m_tabs;

    /**
     * @param model the node model
     */
    public LiftChartNodeView(final LiftChartNodeModel model) {
        super(model);
        LinePlotterProperties liftProps = new LinePlotterProperties();
        // this is a hack
        // TODO: make default name static and add constructor with name
        int missValPos = liftProps.indexOfTab("Missing Values");
        liftProps.removeTabAt(missValPos);
        liftProps.removeTabAt(liftProps.indexOfTab("Column Selection")); 
        m_liftChart = new LinePlotter(new LinePlotterDrawingPane(),
                liftProps);
        m_liftChart.removeMouseListener(
                AbstractPlotter.SelectionMouseListener.class);
        m_liftChart.setDataProvider((DataProvider)model);
        
        // TODO: edit properties
        LinePlotterProperties gainProps = new LinePlotterProperties();
        gainProps.removeTabAt(gainProps.indexOfTab("Missing Values"));
        gainProps.removeTabAt(gainProps.indexOfTab("Column Selection")); 
        m_gainChart = new LinePlotter(new LinePlotterDrawingPane(), gainProps);
        m_gainChart.removeMouseListener(
                AbstractPlotter.SelectionMouseListener.class);
        m_gainChart.setDataProvider(new DataProvider() {

            public DataArray getDataArray(final int index) {
                return ((LiftChartNodeModel)model).getDataArray(1);
            }
            
        });
        m_tabs = new JTabbedPane();
        m_tabs.addTab("Lift Chart", m_liftChart);
        
        m_tabs.addTab("Cumulative Gain Chart", m_gainChart);
        
        setComponent(m_tabs);
    }
    

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        final NodeModel model = getNodeModel();
        m_liftChart.setDataProvider((DataProvider)model);
        m_gainChart.setDataProvider(new DataProvider() {

            public DataArray getDataArray(final int index) {
                return ((LiftChartNodeModel)model).getDataArray(1);
            }
            
        });
        m_liftChart.updatePaintModel();
        m_gainChart.updatePaintModel();
    }

    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
        m_liftChart.fitToScreen();
        m_gainChart.fitToScreen();
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
        
    }


}
