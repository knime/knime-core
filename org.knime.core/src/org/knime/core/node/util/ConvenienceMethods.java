/*
 * ------------------------------------------------------------------ *
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
 *   Mar 1, 2008 (wiswedel): created
 */
package org.knime.core.node.util;

/**
 * Collection of methods that are useful in different contexts.
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class ConvenienceMethods {
    
    private ConvenienceMethods() {
    }
    
    /** Determines if both arguments are equal according to their equals 
     * method (assumed to be symmetric). This method handles null arguments.
     * @param o1 First object for comparison, may be <code>null</code>.
     * @param o2 Second object for comparison, may be <code>null</code>.
     * @return If both arguments are equal 
     * (if either one is null, so must be the other one) 
     */
    public static boolean areEqual(final Object o1, final Object o2) {
        if (o1 == o2) {
            return true;
        }
        if (o1 != null) {
            return o1.equals(o2);
        } else if (o2 != null) {
            return o2.equals(o1);
        }
        assert false : "Both objects are null, hence equal";
        return true;
    }

}
