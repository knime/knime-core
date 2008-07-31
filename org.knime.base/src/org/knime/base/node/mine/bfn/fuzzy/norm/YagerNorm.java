/*
 * --------------------------------------------------------------------- *
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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.bfn.fuzzy.norm;

/**
 * SCHWEIZER and SKLAR norm.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public final class YagerNorm implements Norm {
    /** 
     * Current parameter p.
     * if lim(p) -> 1 the more product norm otherwise min/max norm
     */
    private double m_p;

    /**
     * Inits a new NormYager object.
     * 
     * @param  p the potenz factor for this norm
     * @throws IllegalArgumentException if the parameter <code>p</code> less or 
     *         equal to zero
     */
    private YagerNorm(final double p) {
        // check p
        if (p <= 0.0) { throw new IllegalArgumentException(); }
        m_p = p;
    }

    /**
     * @param p current value of this norm
     * @return instance of this class 
     */
    public static final Norm getInstance(final double p) {
        return new YagerNorm(p);
    }

    /** 
     * @return instance of this class; default p = 2f 
     */
    public static final Norm getInstance() {
        return new YagerNorm(2.0);
    }

    /**
     * @param  a membership degree of fuzzy membership function
     * @param  b membership degree of fuzzy membership function
     * @return calculated fuzzy membership degree of <code>a</code> and 
     *         <code>b</code> using Yager_p norm
     */
    public double computeTNorm(final double a, final double b) {
        return 1 - Math.min(1, Math.pow(
            Math.pow(1 - a, m_p) + Math.pow(1 - b, m_p), 1 / m_p));
    }

    /** 
     * @param  a membership degree of fuzzy membership function
     * @param  b membership degree of fuzzy membership function
     * @return calculated fuzzy membership degree of <code>a</code> and 
     *         <code>b</code> using Yager_p Norm
     */
    public double computeTCoNorm(final double a, final double b) {
        return Math.min(1, Math.pow(
            Math.pow(a, m_p) + Math.pow(b, m_p), 1 / m_p));
    }
    
    /**
     * Returns the string representation as <b>Yaper[p] Norm</b> of this norm.
     * @return an identifier for this norm
     */
    @Override
    public final String toString() {
        return "Yager[" + m_p + "] Norm";
    }
}
