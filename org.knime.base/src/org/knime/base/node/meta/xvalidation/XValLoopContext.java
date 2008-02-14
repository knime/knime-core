/* ------------------------------------------------------------------
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
 *   12.02.2008 (thor): created
 */
package org.knime.base.node.meta.xvalidation;

import org.knime.core.node.workflow.ScopeContext;

/**
 * This class is the context for the cross validation loop.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
class XValLoopContext extends ScopeContext {
    private int m_iteration = 0;

    private final int m_numIterations;

    /**
     * Creates a new loop context.
     *
     * @param iterations the number of iterations
     */
    XValLoopContext(final int iterations) {
        m_numIterations = iterations;
    }

    /**
     * Returns the current iteration number.
     *
     * @return the iteration number, a value between 1 and
     *         {@link #iterations()}
     */
    public int currentIteration() {
        return m_iteration;
    }

    /**
     * Starts the next iteration.
     */
    public void nextIteration() {
        m_iteration++;
        assert m_iteration <= m_numIterations;
    }

    /**
     * Returns if the loop should be finished, i.e. this is the last iteration.
     *
     * @return <code>true</code> if the loop should be finished,
     *         <code>false</code> otherwise
     */
    public boolean finished() {
        return m_iteration >= m_numIterations;
    }

    /**
     * Returns the number of iterations.
     *
     * @return the number of iterations
     */
    public int iterations() {
        return m_numIterations;
    }
}
