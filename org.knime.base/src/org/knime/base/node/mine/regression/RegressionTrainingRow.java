/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * Created on 04.11.2013 by hofer
 */
package org.knime.base.node.mine.regression;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.collection.ListDataValue;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.data.vector.bytevector.ByteVectorValue;


/**
 * A decorator for a data row.
 *
 * @author Heiko Hofer
 * @author Adrian Nembach, KNIME.com
 * @since 3.3
 */
public class RegressionTrainingRow {
    /**
     * Helper class to handle missing values.
     *
     * @since 3.1
     */
    public static class MissingHandling {
        /** True when row has missing cells. */
        public boolean m_hasMissingCells;

        /** If true an exception is thrown when a missing cell is observed. */
        public final boolean m_failOnMissing;

        /**
         * @param failOnMissing Should it fail on missing values?
         */
        public MissingHandling(final boolean failOnMissing) {
            m_failOnMissing = failOnMissing;
        }

        boolean isMissing(final DataCell cell) {
            boolean isMissing = cell.isMissing();
            if (isMissing && m_failOnMissing) {
                throw new IllegalStateException("Missing value support disabled, see node configuration dialog.");
            }
            m_hasMissingCells = m_hasMissingCells || isMissing;
            return isMissing;
        }
    }

    /** The value of the target cell. */
    private double m_target;
    /** The independent variables. */
    private RealMatrix m_parameter;
    private final MissingHandling m_missingHandling;
    private Map<? extends Integer, ? extends Integer> m_vectorLengths;

    /**
     * @param row The underlying row
     * @param parameterCount number of parameters which will be generated
     * from the learning columns
     * @param learningCols indices of the learning columns
     * @param isNominal whether a learning column is nominal
     * @param domainValues the domain values of the nominal learning columns
     * @param target the index of the target value
     * @param failOnMissing when true an exception is thrown when a missing cell is observed
     */
    public RegressionTrainingRow(final DataRow row,
            final int target,
            final int parameterCount,
            final List<Integer> learningCols,
            final Map<Integer, Boolean> isNominal,
            final Map<Integer, List<DataCell>> domainValues,
            final boolean failOnMissing) {
        this(row, target, parameterCount, learningCols, isNominal, domainValues, Collections.emptyMap(), failOnMissing);
    }

    /**
     * @param row The underlying row
     * @param parameterCount number of parameters which will be generated from the learning columns
     * @param learningCols indices of the learning columns
     * @param isNominal whether a learning column is nominal
     * @param domainValues the domain values of the nominal learning columns
     * @param target the index of the target value
     * @param vectorLengths the (maximal) length of vector columns
     * @param failOnMissing when true an exception is thrown when a missing cell is observed
     * @since 3.1
     */
    public RegressionTrainingRow(final DataRow row, final int target, final int parameterCount,
        final List<Integer> learningCols, final Map<Integer, Boolean> isNominal,
        final Map<Integer, List<DataCell>> domainValues, final Map<? extends Integer, ? extends Integer> vectorLengths,
        final boolean failOnMissing) {
        //Do not copy as there might be many rows, hard to modify anyway
        m_vectorLengths = vectorLengths;
        m_parameter = MatrixUtils.createRealMatrix(1, parameterCount);
        m_missingHandling = new MissingHandling(failOnMissing);
        int c = 0;
        for (int i : learningCols) {
            if (isNominal.get(i)) {
                DataCell cell = row.getCell(i);
                if (!m_missingHandling.isMissing(cell)) {
                    int index = domainValues.get(i).indexOf(cell);
                    if (index < 0) {
                        throw new IllegalStateException(
                            "DataCell \"" + cell.toString() + "\" is not in the DataColumnDomain. Please apply a "
                                + "Domain Calculator on the columns with nominal values.");
                    }
                    for (int k = 1; k < domainValues.get(i).size(); k++) {
                        if (k == index) {
                            m_parameter.setEntry(0, c, 1.0);
                        } else {
                            m_parameter.setEntry(0, c, 0.0);
                        }
                        c++;
                    }
                } else {
                    for (int k = 1; k < domainValues.get(i).size(); k++) {
                        m_parameter.setEntry(0, c, Double.NaN);
                        c++;
                    }
                }
            } else {
                DataCell cell = row.getCell(i);
                if (m_vectorLengths.containsKey(i)) {
                    if (!m_missingHandling.isMissing(cell)) {
                        for (int k = 0; k < m_vectorLengths.get(i); ++k) {
                            m_parameter.setEntry(0, c++, getValue(cell, k));
                        }
                    } else {
                        for (int k = m_vectorLengths.get(i); k-- > 0;) {
                            m_parameter.setEntry(0, c++, Double.NaN);
                        }
                    }
                } else {
                    if (!m_missingHandling.isMissing(cell)) {
                        DoubleValue value = (DoubleValue)cell;
                        m_parameter.setEntry(0, c, value.getDoubleValue());
                    } else {
                        m_parameter.setEntry(0, c, Double.NaN);
                    }
                    c++;
                }
            }
        }

        DataCell targetCell = row.getCell(target);
        if (!m_missingHandling.isMissing(targetCell)) {
            if (isNominal.get(target)) {
                m_target = domainValues.get(target).indexOf(targetCell);
                if (m_target < 0) {
                    throw new IllegalStateException("DataCell \"" + row.getCell(target).toString()
                        + "\" is not in the DataColumnDomain of target column. " + "Please apply a "
                        + "Domain Calculator on the target column.");
                }
            } else {
                m_target = ((DoubleValue)targetCell).getDoubleValue();
            }
        } else {
            m_target = Double.NaN;
        }
    }

    /**
     * Gets the value from the {@code cell} with the {@code 0}-based {@code index} in case it was a collection.
     *
     * @param cell A {@link DataCell}, probably a collection of numbers/bytes/bits.
     * @param index The index to select from a collection.
     * @param missingHandling How to handle missing values.
     * @return The number at the specified position.
     * @since 3.1
     */
    public static double getValue(final DataCell cell, final int index, final MissingHandling missingHandling) {
        if (cell instanceof BitVectorValue) {
            final BitVectorValue bvv = (BitVectorValue)cell;
            if (bvv.length() > index) {
                return bvv.get(index) ? 1d : 0d;
            }
            //TODO NaN, or 0?
            return Double.NaN;
        } else if (cell instanceof ByteVectorValue) {
            final ByteVectorValue bvv = (ByteVectorValue)cell;
            if (bvv.length() > index) {
                return bvv.get(index);
            }
            //Maybe some other value?
            return Double.NaN;
        } else if (cell instanceof ListDataValue) {
            final ListDataValue ldv = (ListDataValue)cell;
            if (ldv.size() > index) {
                DataCell dataCell = ldv.get(index);
                if (dataCell instanceof DoubleValue) {
                    final DoubleValue dv = (DoubleValue)dataCell;
                    return dv.getDoubleValue();
                }
                missingHandling.isMissing(dataCell);
                return Double.NaN;
            }
        }
        throw new IllegalStateException("Not a missing, nor a vector value: " + cell);
    }
    /**
     * @param cell A {@link DataCell}.
     * @param index The {@code 0}-based index.
     * @return The value at the selected position.
     */
    private double getValue(final DataCell cell, final int index) {
        return getValue(cell, index, m_missingHandling);
    }


    /**
     * Returns true when row has missing cells.
     * @return whether row has missing cells or not.
     */
    public boolean hasMissingCells() {
        return m_missingHandling.m_hasMissingCells;
    }

    /**
     * The value of the target for this row.
     * @return the value of the target.
     */
    public double getTarget() {
        return m_target;
    }

    /**
     * Returns a {@link RealMatrix} with values of the parameters retrieved
     * from the learning columns.
     * @return the parameters
     * @since 3.3
     */
    public RealMatrix getParameter() {
        return m_parameter;
    }
}

