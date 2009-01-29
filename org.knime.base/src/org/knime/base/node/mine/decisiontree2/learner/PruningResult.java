/*
 * ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 *
 * History
 *   12.03.2007 (sieb): created
 */
package org.knime.base.node.mine.decisiontree2.learner;

import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNode;

/**
 * A pruning result is the possibly new node and a quality value (e.g.
 * description length, estimated error) of this node.
 *
 * @author Christoph Sieb, University of Konstanz
 */
public class PruningResult {

    private double m_qualityValue;

    private DecisionTreeNode m_node;

    /**
     * Creates a pruning result from a node and its quality value (e.g.
     * description length, estimated error).
     *
     * @param qualityValue the quality value (e.g. description length, estimated
     *            error) of the node
     *
     * @param node the node of the pruning result
     */
    public PruningResult(final double qualityValue,
            final DecisionTreeNode node) {
        m_qualityValue = qualityValue;
        m_node = node;
    }

    /**
     * Returns the quality value for this node.
     *
     * @return the quality value length for this node
     */
    public double getQualityValue() {
        return m_qualityValue;
    }

    /**
     * Returns the decision tree of this pruning result.
     *
     * @return the decision tree of this pruning result
     */
    public DecisionTreeNode getNode() {
        return m_node;
    }
}
