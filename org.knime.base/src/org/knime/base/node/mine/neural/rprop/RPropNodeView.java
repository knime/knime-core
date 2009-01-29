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
 *   14.12.2005 (cebron): created
 */
package org.knime.base.node.mine.neural.rprop;

import java.awt.Color;

import org.knime.base.node.viz.plotter.basic.BasicPlotter;
import org.knime.base.node.viz.plotter.basic.BasicPlotterImpl;
import org.knime.core.node.NodeView;


/**
 * NodeView of the RProp Node. Provides an error plot.
 *
 * @author Nicolas Cebron, University of Konstanz
 */
public class RPropNodeView extends NodeView<RPropNodeModel> {

    private BasicPlotter m_errorplotter;

    /**
     * @param model Underlying NodeModel
     */
    public RPropNodeView(final RPropNodeModel model) {
        super(model);
        m_errorplotter = new BasicPlotterImpl();
        setComponent(m_errorplotter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        RPropNodeModel model = getNodeModel();
        if (model.getErrors() != null) {
            m_errorplotter.reset();
            double[] errors = model.getErrors();
            m_errorplotter.addLine(errors, Color.BLACK, null);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
        m_errorplotter.dispose();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
        // empty.
    }
}
