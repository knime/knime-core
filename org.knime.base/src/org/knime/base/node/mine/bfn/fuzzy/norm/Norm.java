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
 * Fuzzy norm interface which implements the fuzzy operator for conjunction and
 * disjunction, also known as t-norm and tco-norm.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public interface Norm {
    /** Choice of fuzzy norm. */
    public static final String NORM_KEY = "norm";

    /** Number of available norm choices. */
    public static final Norm[] NORMS = new Norm[]{MinMaxNorm.getInstance(),
            ProdNorm.getInstance(), LukaNorm.getInstance(),
            YagerNorm.getInstance()};

    /**
     * Computes the fuzzy disjunktion.
     * 
     * @param a the membership degree of fuzzy membership function A
     * @param b the membership degree of fuzzy membership function B
     * @return the calculated fuzzy membership degree of a and b using TNorm
     */
    double computeTNorm(double a, double b);

    /**
     * Computes fuzzy conjunktion.
     * 
     * @param a the membership degree of fuzzy membership function A
     * @param b the membership degree of fuzzy membership function B
     * @return the calculated fuzzy membership degree of a and b using TCoNorm
     */
    double computeTCoNorm(double a, double b);
}
