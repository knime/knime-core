/* Created on 22.01.2007 10:03:38 by thor
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
 * ------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.regression.polynomial.learner;

import org.knime.core.node.NodeView;

/**
 * This view show a simple table with all the coefficients for each attributed
 * in the dataset.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class PolyRegCoefficientView extends
        NodeView<PolyRegLearnerNodeModel> {
    private final CoefficientTable m_coeffTable;

    /**
     * Creates a new new view for showing the learned coefficients.
     *
     * @param nodeModel the node model
     */
    public PolyRegCoefficientView(final PolyRegLearnerNodeModel nodeModel) {
        super(nodeModel);
        m_coeffTable = new CoefficientTable(nodeModel);

        m_coeffTable.update();
        setComponent(m_coeffTable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        if (m_coeffTable != null) {
            m_coeffTable.update();
        }
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
        // nothing to do
    }
}
