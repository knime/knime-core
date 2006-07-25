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
 * Abstract class describing a coordinate mapping. A coordinate mapping
 * describes the mapping from a real data domain to a fixed length coordinate
 * axis.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public abstract class CoordinateMapping {

    /**
     * The mapping of the domain value.
     */
    private double m_mappingValue;

    /**
     * Domain value as string.
     */
    private String m_stringDomainValue;

    /**
     * Constructs a coordinate mapping from a string representation of the
     * domain value and its mapping value.
     * 
     * @param stringDomainValue the domain value as a string
     * @param mappingValue the coresponding mapping
     */
    CoordinateMapping(final String stringDomainValue, final double mappingValue) {
        m_stringDomainValue = stringDomainValue;
        m_mappingValue = mappingValue;
    }

    /**
     * Each coordinate mapping must return the mapping value as a double.
     * 
     * @return the mapping value
     */
    public double getMappingValue() {
        return m_mappingValue;
    }

    /**
     * A coordinate mapping must also return the coresponding domain value as a
     * string.
     * 
     * @return the domain value as string
     */
    public String getDomainValueAsString() {
        return m_stringDomainValue;
    }

    /**
     * @return the string representation of the domain value.
     */
    String getStringDomainValue() {
        return m_stringDomainValue;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return ("Dom. String Value: " + m_stringDomainValue + ", Map. Value: " + m_mappingValue);
    }
}
