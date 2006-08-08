/*
 * ------------------------------------------------------------------
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
 *   08.06.2006 (Tobias Koetter): created
 */
package org.knime.base.node.viz.histogram.node;

import org.knime.base.node.viz.histogram.AggregationMethod;
import org.knime.base.node.viz.histogram.HistogramPlotter;
import org.knime.base.node.viz.histogram.HistogramProperties;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;

/**
 * The node view which contains the histogram plotter panel.
 * 
 * @author Tobias Koetter, University of Konstanz
 * 
 */
public class HistogramNodeView extends NodeView {
    private final HistogramNodeModel m_model;

    /** The Rule2DPlotter is the core of the view. */
    private HistogramPlotter m_plotter;

    /**
     * The <code>HistogramProps</code> class which holds the properties dialog
     * elements.
     */
    private HistogramProperties m_properties;

    private static final int INITIAL_WIDTH = 300;

    /**
     * Creates a new view instance for the histogram node.
     * 
     * @param nodeModel the corresponding node model
     */
    HistogramNodeView(final NodeModel nodeModel) {
        super(nodeModel);
        if (!(nodeModel instanceof HistogramNodeModel)) {
            throw new IllegalArgumentException(NodeModel.class.getName()
                    + " not an instance of "
                    + HistogramNodeModel.class.getName());
        }
        m_model = (HistogramNodeModel)nodeModel;
        // String selectedColName = m_model.getSelectedColumnName();

    }

    /**
     * Clears the plot and unregisters from any hilite handler.
     */
    public void reset() {
        m_plotter.reset();
    }

    /**
     * Whenever the model changes an update for the plotter is triggered and new
     * HiLiteHandler are set.
     */
    @Override
    public void modelChanged() {
        if (m_model == null || m_model.getData() == null) {
            if (m_plotter != null) {
                m_plotter.reset();
            }
            return;
        }
        DataTable data = m_model.getData();
        if (data == null) {
            return;
        }
        DataTableSpec spec = data.getDataTableSpec();
        if (spec == null) {
            throw new IllegalArgumentException(
                    "Table specification shouldn't be null.");
        }
        setViewTitle(getViewName() + " " + spec.getName());
        if (m_plotter == null) {
            // create the properties panel
            m_properties = new HistogramProperties(AggregationMethod.COUNT);
            // create the plotter
            m_plotter = new HistogramPlotter(INITIAL_WIDTH, data, m_properties,
                    m_model.getInHiLiteHandler(0), null);
            m_plotter.setBackground(ColorAttr.getBackground());
            // add the hilite menu to the menu bar of the node view
            getJMenuBar().add(m_plotter.getHiLiteMenu());
            // add the histogram panel to the root window of the node view
            setComponent(m_plotter);
        } else {
            if (spec.getNumColumns() < 1) {
                return;
            }
            final String xCol = spec.getColumnSpec(0).getName();
            // String selectedColName = m_model.getSelectedColumnName();
            m_plotter.modelChanged(data, xCol);
            m_plotter.setHiLiteHandler(m_model.getInHiLiteHandler(0));
        }
    }

    /**
     * @see org.knime.core.node.NodeView#onClose()
     */
    @Override
    protected void onClose() {
        return;
    }

    /**
     * @see org.knime.core.node.NodeView#onOpen()
     */
    @Override
    protected void onOpen() {
        return;
    }
}
