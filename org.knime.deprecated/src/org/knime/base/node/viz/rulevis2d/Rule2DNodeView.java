/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
