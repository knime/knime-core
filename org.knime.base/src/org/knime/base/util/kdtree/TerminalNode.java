/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
