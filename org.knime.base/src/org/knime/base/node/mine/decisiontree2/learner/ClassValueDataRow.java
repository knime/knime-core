/*
 * ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   31.07.2007 (sieb): created
 */
package org.knime.base.node.mine.decisiontree2.learner;

/**
 * A data row represented as a double array. Nominal values must be mapped to be
 * used. The class value is also a mapped int value.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class ClassValueDataRow {

    /**
     * Holds the attribute values as doubles.
     */
    private double[] m_attributeValues;

    /**
     * Holds the class value as int.
     */
    private int m_classValue;

    /**
     * Constructs a data row.
     * 
     * @param attributeValues the attribute values (nominal values are mapped
     *            doubles)
     * @param classValue the nominal class value mapped to an integer
     */
    public ClassValueDataRow(final double[] attributeValues, final int classValue) {
        m_attributeValues = attributeValues;
        m_classValue = classValue;
    }

    /**
     * Returns the class value.
     * 
     * @return the class value
     */
    public int getClassValue() {
        return m_classValue;
    }

    /**
     * Returns the attribute value for the given index.
     * 
     * @param index the column index for which to return the double value
     * 
     * @return the attribute value for the given index
     */
    public double getValue(final int index) {
        return m_attributeValues[index];
    }

    /**
     * Returns the number of attribute values, excluding the class value.
     * 
     * @return the number of attribute values, excluding the class value
     */
    public int getNumAttributes() {
        return m_attributeValues.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (double value : m_attributeValues) {
            sb.append(value).append(";");
        }
        sb.append(m_classValue);
        return sb.toString();
    }
}
