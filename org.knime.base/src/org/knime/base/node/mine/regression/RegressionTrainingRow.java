/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by 
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
 * Created on 04.11.2013 by hofer
 */
package org.knime.base.node.mine.regression;

import java.util.List;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;

import Jama.Matrix;


/**
 * A decorator for a data row.
 *
 * @author Heiko Hofer
 * @since 2.9
 */
public class RegressionTrainingRow {
    /** The value of the target cell. */
    private double m_target;
    /** The independent variables. */
    private Matrix m_parameter;
    /** True when row has missing cells. */
    private boolean m_hasMissingCells;
    /** If true an exception is thrown when a missing cell is observed. */
    private boolean m_failOnMissing;

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
        m_parameter = new Matrix(1, parameterCount);
        m_failOnMissing = failOnMissing;
        int c = 0;
        for (int i : learningCols) {
            if (isNominal.get(i)) {
                DataCell cell = row.getCell(i);
                if (!isMissing(cell)) {
                    int index = domainValues.get(i).indexOf(cell);
                    if (index < 0) {
                        throw new IllegalStateException("DataCell \""
                        + cell.toString()
                        + "\" is not in the DataColumnDomain. Please apply a "
                        + "Domain Calculator on the columns with nominal "
                        + "values.");
                    }
                    for (int k = 1; k < domainValues.get(i).size(); k++) {
                        if (k == index) {
                            m_parameter.set(0, c, 1.0);
                        } else {
                            m_parameter.set(0, c, 0.0);
                        }
                        c++;
                    }
                } else {
                    for (int k = 1; k < domainValues.get(i).size(); k++) {
                        m_parameter.set(0, c, Double.NaN);
                        c++;
                    }
                }
            } else {
                DataCell cell = row.getCell(i);
                if (!isMissing(cell)) {
                    DoubleValue value = (DoubleValue)cell;
                    m_parameter.set(0, c, value.getDoubleValue());
                } else {
                    m_parameter.set(0, c, Double.NaN);
                }
                c++;
            }
        }

        DataCell targetCell = row.getCell(target);
        if (!isMissing(targetCell)) {
            if (isNominal.get(target)) {
                m_target = domainValues.get(target).indexOf(targetCell);
                if (m_target < 0) {
                    throw new IllegalStateException("DataCell \""
                    + row.getCell(target).toString()
                    + "\" is not in the DataColumnDomain of target column. "
                    + "Please apply a "
                    + "Domain Calculator on the target column.");
                }
            } else {
                m_target = ((DoubleValue)targetCell).getDoubleValue();
            }
        } else {
            m_target = Double.NaN;
        }
    }


    private boolean isMissing(final DataCell cell) {
        boolean isMissing = cell.isMissing();
        if (isMissing && m_failOnMissing) {
            throw new IllegalStateException("Observed missing value in input data.");
        }
        m_hasMissingCells = m_hasMissingCells || isMissing;
        return isMissing;
    }

    /**
     * Returns true when row has missing cells.
     * @return whether row has missing cells or not.
     */
    public boolean hasMissingCells() {
        return m_hasMissingCells;
    }

    /**
     * The value of the target for this row.
     * @return the value of the target.
     */
    public double getTarget() {
        return m_target;
    }

    /**
     * Returns a {@link Matrix} with values of the parameters retrieved
     * from the learning columns.
     * @return the parameters
     */
    public Matrix getParameter() {
        return m_parameter;
    }
}

