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
 * History
 *   Mar 30, 2006 (wiswedel): created
 */
package org.knime.base.node.mine.regression.polynomial.learner;

import org.knime.core.node.NodeView;

/**
 * This class shows a view with one attribute on the x-axis, its values on the
 * y-axis and the regression curve.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class PolyRegLineNodeView extends
        NodeView<PolyRegLearnerNodeModel> {
    // the scoll and drawing pane
    private final PolyRegLineScatterPlotter m_plot;

    /**
     * Create new view.
     *
     * @param nodeModel the model to look at
     */
    public PolyRegLineNodeView(final PolyRegLearnerNodeModel nodeModel) {
        super(nodeModel);
        m_plot = new PolyRegLineScatterPlotter(nodeModel);
        getJMenuBar().add(m_plot.getHiLiteMenu());
        setComponent(m_plot);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        // could be the property handler,
        m_plot.setHiLiteHandler(getNodeModel().getInHiLiteHandler(0));
        m_plot.modelChanged();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
        m_plot.setHiLiteHandler(getNodeModel().getInHiLiteHandler(0));
    }
}
