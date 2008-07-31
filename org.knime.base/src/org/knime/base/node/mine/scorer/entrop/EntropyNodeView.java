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
 *   Apr 13, 2006 (wiswedel): created
 */
package org.knime.base.node.mine.scorer.entrop;

import org.knime.core.node.NodeView;

/**
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class EntropyNodeView extends NodeView<EntropyNodeModel> {
    private final EntropyView m_view;

    /**
     * Delegates to super class.
     * 
     * @param nodeModel the node model to look at
     */
    EntropyNodeView(final EntropyNodeModel nodeModel) {
        super(nodeModel);
        m_view = new EntropyView();
        setComponent(m_view);
    }

    /** {@inheritDoc} */
    @Override
    protected void modelChanged() {
        EntropyCalculator calculator = getNodeModel().getCalculator();
        m_view.update(calculator);
        m_view.setHiliteHandler(calculator == null ? null : getNodeModel()
                .getViewHiliteHandler());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
    }
}
