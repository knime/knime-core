/* ------------------------------------------------------------------
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
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
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
