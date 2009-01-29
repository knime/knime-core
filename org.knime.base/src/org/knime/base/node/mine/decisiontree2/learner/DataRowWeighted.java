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
 *   31.07.2007 (sieb): created
 */
package org.knime.base.node.mine.decisiontree2.learner;

/**
 * A data row represented as a double array. Nominal values must be mapped to be
 * used. The class value is also a mapped int value.
 *
 * @author Christoph Sieb, University of Konstanz
 */
public class DataRowWeighted {
    /**
     * Holds the underlying {@link ClassValueDataRow}.
     */
    private final ClassValueDataRow m_dataRow;

    /**
     * Holds the weight for this {@link ClassValueDataRow}.
     */
    private final double m_weight;

    /**
     * Constructs a weighted data row.
     *
     * @param dataRow the underlying {@link ClassValueDataRow}
     * @param weight the weight for this {@link DataRowWeighted}
     */
    public DataRowWeighted(final ClassValueDataRow dataRow, final double weight) {
        m_dataRow = dataRow;
        m_weight = weight;
    }

    /**
     * Constructs a weighted data row from another one and sets the weight to
     * the given value.
     *
     * @param dataRow the other {@link DataRowWeighted}
     * @param weight the weight for this {@link DataRowWeighted}
     */
    public DataRowWeighted(final DataRowWeighted dataRow, final double weight) {
        m_dataRow = dataRow.m_dataRow;
        m_weight = weight;
    }

    /**
     * Returns the class value.
     *
     * @return the class value
     */
    public int getClassValue() {
        return m_dataRow.getClassValue();
    }

    /**
     * Returns the attribute value for the given index.
     *
     * @param index the column index for which to return the double value
     *
     * @return the attribute value for the given index
     */
    public double getValue(final int index) {
        return m_dataRow.getValue(index);
    }

    /**
     * Returns the number of attribute values, excluding the class value.
     *
     * @return the number of attribute values, excluding the class value
     */
    public int getNumAttributes() {
        return m_dataRow.getNumAttributes();
    }

    /**
     * Return the weight of this data row.
     *
     * @return the weight of this data row
     */
    public double getWeight() {
        return m_weight;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[<");
        sb.append(m_dataRow.toString());
        sb.append("><weight:");
        sb.append(m_weight);
        sb.append(">]");

        return sb.toString();
    }
}
