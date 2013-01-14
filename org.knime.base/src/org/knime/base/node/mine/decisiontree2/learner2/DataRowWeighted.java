/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   31.07.2007 (sieb): created
 */
package org.knime.base.node.mine.decisiontree2.learner2;

/**
 * A data row represented as a double array. Nominal values must be mapped to be
 * used. The class value is also a mapped int value.
 *
 * @author Christoph Sieb, University of Konstanz
 * @since 2.6
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
