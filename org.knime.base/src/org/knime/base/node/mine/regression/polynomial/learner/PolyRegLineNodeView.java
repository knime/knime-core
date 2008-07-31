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
public class PolyRegLineNodeView extends NodeView {
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
