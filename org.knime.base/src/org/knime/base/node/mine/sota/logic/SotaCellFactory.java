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
 *   20.02.2007 (thiel): created
 */
package org.knime.base.node.mine.sota.logic;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public final class SotaCellFactory {

    /**
     * The type specifying a <code>SotaDoubleCell</code>.
     */
    public static final String DOUBLE_TYPE = "double";
    
    /**
     * The type specifying a <code>SotaFuzzyCell</code>.
     */
    public static final String FUZZY_TYPE = "fuzzy";
    
    private SotaCellFactory() { } 
    
    /**
     * Creates a new zeroed <code>SotaCell</code>. The concrete implementation 
     * type (<code>SotaDoubleCell</code> or <code>SotaFuzzyCell</code>) depends
     * on the given type string.
     * 
     * @param type Specifies the concrete implementation type to return an 
     * instance of (<code>SotaDoubleCell</code> or <code>SotaFuzzyCell</code>).
     * @return The created <code>SotaCell</code>.
     */
    public static SotaCell createSotaCell(final String type) {
        if (type.equals(SotaCellFactory.DOUBLE_TYPE)) {
            return new SotaDoubleCell(0);
        } else if (type.equals(SotaCellFactory.FUZZY_TYPE)) {
            return new SotaFuzzyCell(0, 0, 0, 0);
        }
        return null;
    }
}
