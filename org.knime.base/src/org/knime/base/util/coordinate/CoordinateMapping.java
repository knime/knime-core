/*
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *   02.02.2006 (sieb): created
 */
package org.knime.base.util.coordinate;

import org.knime.core.data.DataValue;

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
     * Text of the tool tip.
     */
    private DataValue[] m_values;

    /**
     * Constructs a coordinate mapping from a string representation of the
     * domain value and its mapping value.
     *
     * @param stringDomValue the domain value as a string
     * @param mappingValue the corresponding mapping
     */
    CoordinateMapping(final String stringDomValue, final double mappingValue) {
        m_stringDomainValue = stringDomValue;
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
     * A coordinate mapping must also return the corresponding domain value as a
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
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return ("Dom. String Value: " + m_stringDomainValue + ", "
                + "Map. Value: " + m_mappingValue);
    }

    /**
     * Returns the values if set.
     *
     * @return the values
     */
    public DataValue[] getValues() {
        return m_values;
    }

    /**
     * Sets values of this mapping.
     *
     * @param values the values
     */
    public void setValues(final DataValue... values) {
        m_values = values;
    }
}
