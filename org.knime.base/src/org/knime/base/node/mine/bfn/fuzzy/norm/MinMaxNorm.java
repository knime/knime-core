/*
 * --------------------------------------------------------------------- *
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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.bfn.fuzzy.norm;

/**
 * Minimum/Maximum norm.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class MinMaxNorm implements Norm {
    /**
     * Internal instance of this norm.
     */
    private static final MinMaxNorm NORM = new MinMaxNorm();

    /**
     * Inits a new instance of this norm.
     */
    private MinMaxNorm() {
        // empty
    }

    /**
     * Returns an static instance of this norm.
     * 
     * @return an instance of this class
     */
    public static final Norm getInstance() {
        return NORM;
    }

    /**
     * Computes the T-Norm as <code>min(a,b)</code>.
     * 
     * @param a the membership degree of fuzzy membership function A
     * @param b the membership degree of fuzzy membership function B
     * @return the calculated fuzzy t-norm
     */
    public final double computeTNorm(final double a, final double b) {
        return Math.min(a, b);
    }

    /**
     * Computes the TCo-Norm as <code>max(a,b)</code>.
     * 
     * @param a the membership degree of fuzzy membership function A
     * @param b the membership degree of fuzzy membership function B
     * @return the calculated fuzzy tco-norm
     */
    public final double computeTCoNorm(final double a, final double b) {
        return Math.max(a, b);
    }

    /**
     * Returns the string representation <b>Min/Max Norm</b> of this norm.
     * 
     * @return an identifier for this norm
     */
    @Override
    public final String toString() {
        return "Min/Max Norm";
    }
}
