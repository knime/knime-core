/*
 * ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 *
 * History
 *   01.08.2007 (sieb): created
 */
package org.knime.base.node.mine.decisiontree2.learner;

/**
 * Implements an atomic double (i.e. synchronized double).
 *
 * @author Christoph Sieb, University of Konstanz
 */
public class AtomicDouble {
    private double m_value;

    /**
     * Creates an atomic double with the given value.
     *
     * @param value the initial value to set
     */
    public AtomicDouble(final double value) {
        m_value = value;
    }

    /**
     * Returns the value.
     *
     * @return the value
     */
    public synchronized double getValue() {
        return m_value;
    }

    /**
     * Decrements this double by the given decrement.
     *
     * @param incrementValue the value to subtract from this double
     * @return the value after incrementing
     */
    public synchronized double incrementAndGet(final double incrementValue) {
        m_value += incrementValue;
        return m_value;
    }
}
