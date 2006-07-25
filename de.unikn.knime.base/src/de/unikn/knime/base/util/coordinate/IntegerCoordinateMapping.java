/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   02.02.2006 (sieb): created
 */
package de.unikn.knime.base.util.coordinate;

/**
 * Holds the original value according to the domain and its mapping.
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class IntegerCoordinateMapping extends CoordinateMapping {

    /**
     * The original domain value.
     */
    private final int m_domainValue;

    /**
     * Constructs a coordinate mapping.
     * 
     * @param stringDomainValue the domain value as string
     * @param domainValue the domain value
     * @param mappingValue the corresponding mapped value
     */
    protected IntegerCoordinateMapping(final String stringDomainValue,
            final int domainValue, final double mappingValue) {

        super(stringDomainValue, mappingValue);
        m_domainValue = domainValue;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return (super.toString() + " integer value: " + m_domainValue);
    }

    /**
     * @return the domain value of this mapping
     */
    protected int getDomainValue() {
        return m_domainValue;
    }
}
