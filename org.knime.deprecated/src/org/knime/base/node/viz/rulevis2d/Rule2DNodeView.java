/* 
 * -------------------------------------------------------------------
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
 *   11.10.2005 (Fabian Dill): created
 */
package org.knime.base.node.viz.rulevis2d;

import org.knime.base.node.viz.scatterplot.ScatterProps;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.FuzzyIntervalValue;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeView;

/**
 * The view for the the Fuzzy Rule Plotter Node. It opens an instance of the
 * Rule2DPlotter which provides the core functionality for this node.
 * 
 * @see Rule2DPlotter
 * @author Fabian Dill
 */
public class Rule2DNodeView extends NodeView {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(Rule2DNodeView.class);

    /**
     * The Rule2DPlotter is the core of the view.
     */
    private final Rule2DPlotter m_plotter;

    // the pane holding the always visible controls
    private ScatterProps m_properties = new ScatterProps() {
        /**
         * {@inheritDoc}
         */
        @Override
        public void setSelectables(final DataTableSpec tSpec) {
            super.setSelectables(tSpec, FuzzyIntervalValue.class);
        }
    };

    private static final int INITIAL_WIDTH = 300;

    /**
     * Opens a frame with the FuzzyRulePlotterPanel inside.
     * 
     * @param nodeModel - the underlying model, holding the rules and the data
     */
    public Rule2DNodeView(final Rule2DNodeModel nodeModel) {
        super(nodeModel);
        LOGGER.debug("model: " + nodeModel + " rules: " + nodeModel.getRules());
        m_plotter = new Rule2DPlotter(nodeModel.getDataPoints(), nodeModel
                .getRules(), m_properties, INITIAL_WIDTH);
        m_plotter.setBackground(ColorAttr.getBackground());

        // set the HiLiteHandler for the data coming from port 0
        m_plotter.setHiLiteHandler(getNodeModel().getInHiLiteHandler(0));
        // set the HiLiteHandler for the rules coming from port 1

        getJMenuBar().add(m_plotter.getHiLiteMenu());
        setComponent(m_plotter);
    }

    /**
     * Clears the plot and unregisters from any hilite handler.
     */
    public void reset() {
        m_plotter.clear();
        m_plotter.setHiLiteHandler(null);
    }

    /**
     * Returns the Rule2DNodeModel - override in order to adapt the view to a
     * different underlying NodeModel.
     * 
     * @return - the data provider.
     */
    public Rule2DDataProvider getDataProvider() {
        return (Rule2DDataProvider)getNodeModel();
    }

    /**
     * Whenever the model changes an update for the plotter is triggered and new
     * HiLiteHandler are set.
     */
    @Override
    public void modelChanged() {
        Rule2DDataProvider dataProvider = getDataProvider();
        // update the x/y col selectors, this should trigger
        if (dataProvider.getDataPoints() != null) {
            // m_properties.setSelectables(dataProvider.getDataPoints()
            // .getDataTableSpec());

            // clear the plot
            m_plotter.clear();
            // set the HiLiteHandler for the data coming from port 0
            m_plotter.setHiLiteHandler(getNodeModel().getInHiLiteHandler(0));
            // set the HiLiteHandler for the rules coming from port 1
            m_plotter
                    .setRuleHiLiteHandler(getNodeModel().getInHiLiteHandler(1));
            // trigger an update for the plotter
            m_plotter.setRules(dataProvider.getRules());
            m_plotter.modelDataChanged(dataProvider.getDataPoints());
            m_properties.setSelectables(dataProvider.getDataPoints()
                    .getDataTableSpec(), FuzzyIntervalValue.class);
            m_plotter.updatePaintModel();
        }
    }

    /**
     * Closes the view.
     */
    @Override
    public void onClose() {
        if (m_plotter != null) {
            m_plotter.shutDown();
        }
    }

    /**
     * On open a HiLiteHandler is set for the data, another one for the rules.
     */
    @Override
    public void onOpen() {
    }
}
