/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
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
 *   Sep 17, 2009 (morent): created
 */
package org.knime.base.node.mine.decisiontree2;

import java.util.EnumSet;
import java.util.HashMap;


/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 */
/** Contains the different data types an PMML Array element can hold. */
public enum PMMLArrayType {
    /** Integer type. */
    INT("int"),
    /** Double type. */
    REAL("real"),
    /** String type. */
    STRING("string");

    private String m_represent;

    private static HashMap<String, PMMLArrayType> lookup =
            new HashMap<String, PMMLArrayType>();

    static {
        for (PMMLArrayType t : EnumSet.allOf(PMMLArrayType.class)) {
            lookup.put(t.toString(), t);
        }
    }

    private PMMLArrayType(final String rep) {
        m_represent = rep;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        if (m_represent != null) {
            return m_represent;
        }
        return super.toString();
    }

    /**
     * Returns the corresponding array type for the passed representation.
     *
     * @param represent the representation to find the array type for
     * @return the array type
     * @throws InstantiationException - if no such array type exists
     */
    public static PMMLArrayType get(final String represent)
            throws InstantiationException {
        PMMLArrayType arrayType = lookup.get(represent);
        if (arrayType == null) {
            throw new InstantiationException("Illegal PMML array type '"
                    + represent);
        }
        return arrayType;
    }
}
