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
 * LUKASIEWICZ norm. Special case of the Yager Norm Yager(p=1).
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class LukaNorm implements Norm {
    /**
     * Internal instance of this norm.
     */
    private static final LukaNorm NORM = new LukaNorm();

    /**
     * Inits a new instance of this norm.
     */
    private LukaNorm() {
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
     * Computes the T-Norm as <code>max(0,a+b-1)</code>.
     * 
     * @param a the membership degree of fuzzy membership function A
     * @param b the membership degree of fuzzy membership function B
     * @return the calculated fuzzy t-norm
     */
    public final double computeTNorm(final double a, final double b) {
        return Math.max(0, a + b - 1);
    }

    /**
     * Computes the TCo-Norm as <code>min(a+b,1)</code>.
     * 
     * @param a the membership degree of fuzzy membership function A
     * @param b the membership degree of fuzzy membership function B
     * @return the calculated fuzzy tco-norm
     */
    public final double computeTCoNorm(final double a, final double b) {
        return Math.min(a + b, 1);
    }

    /**
     * Returns the string representation <b>Lukasiewicz Norm</b> of this norm.
     * 
     * @return an identifier for this norm
     */
    @Override
    public final String toString() {
        return "Lukasiewicz Norm";
    }
}
