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
 * ------------------------------------------------------------------- * 
 */
package org.knime.base.util.kdtree;

/**
 * This class represents non-terminal nodes inside the k-d tree. A non-terminal
 * node defines a split of the data in its two sub-trees. The index of the split
 * attribute and the corresponding split-value are stored inside the node,
 * together with the two child nodes.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
class NonterminalNode implements Node {
    private final int m_splitAttribute;

    private final double m_splitValue;

    private final Node m_left, m_right;

    /**
     * Creates a new non-terminal node.
     * 
     * @param splitAttribute the index of the split attribute
     * @param splitValue the split value
     * @param left the left child, can be <code>null</code>
     * @param right the right child, can be <code>null</code>
     */
    public NonterminalNode(final int splitAttribute, final double splitValue,
            final Node left, final Node right) {
        m_splitAttribute = splitAttribute;
        m_splitValue = splitValue;
        m_left = left;
        m_right = right;
    }

    /**
     * Returns the attribute index that is used for the split.
     * 
     * @return the split attribute index
     */
    public int getSplitAttribute() {
        return m_splitAttribute;
    }

    /**
     * Returns the split value.
     * 
     * @return the split value
     */
    public double getSplitValue() {
        return m_splitValue;
    }

    /**
     * Returns the left child node. Can be <code>null</code>.
     * 
     * @return the left child
     */
    public Node getLeft() {
        return m_left;
    }

    /**
     * Returns the right child node. Can be <code>null</code>.
     * 
     * @return the right child
     */
    public Node getRight() {
        return m_right;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "(" + m_splitAttribute + ", " + m_splitValue + ")";
    }
}
