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
 * ------------------------------------------------------------------- *
 */
package org.knime.base.util.kdtree;

import java.util.Arrays;

/**
 * This class represents a terminal node inside a k-d tree. The terminal nodes
 * store the pattern and an optional data object associated with the pattern.
 *
 * @param <T> the type of the data object object associated with the pattern
 * @author Thorsten Meinl, University of Konstanz
 */
final class TerminalNode<T> implements Node {
    private final T m_data;

    private final double[] m_pattern;

    /**
     * Creates a new terminal node.
     *
     * @param pattern the pattern
     * @param data an optional data object
     */
    public TerminalNode(final double[] pattern, final T data) {
        m_pattern = pattern;
        m_data = data;
    }

    /**
     * Returns the optional data object. Can be <code>null</code>.
     *
     * @return the data object
     */
    public T getData() {
        return m_data;
    }

    /**
     * Returns the pattern stored in the terminal node.
     *
     * @return the pattern
     */
    public double[] getPattern() {
        return m_pattern;
    }

    /**
     * Returns the (squared euclidean) distance to a query pattern. The query
     * pattern must have the same dimension as the pattern inside this node.
     *
     * @param query a query pattern.
     * @return the distance
     */
    public double getDistance(final double[] query) {
        double distSum = 0;

        for (int i = 0; i < query.length; i++) {
            double dist = query[i] - m_pattern[i];
            distSum += dist * dist;
        }

        return distSum;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Arrays.toString(m_pattern) + " => " + m_data;
    }
}
