/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 * -------------------------------------------------------------------
 * 
 * History
 *   23.10.2006 (sieb): created
 */
package org.knime.base.node.preproc.discretization.caim2.modelcreator;

import java.util.Map;

import org.knime.base.node.preproc.discretization.caim2.DiscretizationScheme;
import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.BufferedDataTable;

/**
 * Implements a 2D quanta matrix that holds for one dimension class values and
 * for the other dimension the intervals of discretization.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class QuantaMatrix2D {

    private DiscretizationScheme m_dScheme;

    private int[] m_classTotals;

    private int[] m_intervalTotals;

    /**
     * First dimension are the intervals, second are the class values.
     */
    private int[][] m_mainMatrix;

    private int m_total;

    private Map<String, Integer> m_classValueToIndexMap;

    /**
     * Initializes a new 2D quanta matrix for class value - interval statistics.
     * 
     * @param dScheme the discretization scheme representing the first dimension
     * @param classValueToIndexMap the class values representing the other
     *            dimension contains a mapping from the name to the index used
     *            for the matrix values
     */
    public QuantaMatrix2D(final DiscretizationScheme dScheme,
            final Map<String, Integer> classValueToIndexMap) {

        m_dScheme = dScheme;
        m_classTotals = new int[classValueToIndexMap.size()];
        m_intervalTotals = new int[m_dScheme.getNumIntervals()];
        m_mainMatrix = new int[m_intervalTotals.length][m_classTotals.length];
        m_total = 0;

        m_classValueToIndexMap = classValueToIndexMap;
    }

    /**
     * Counts the distribution of the given column of the input table and stores
     * it into the quanta matrix.
     * 
     * @param inputTable the table to create the statistics for
     * @param columnIndex the index of the column this matrix should be used for
     * @param classColumnIndex the index of the class column of the input table
     */
    public void countData(final BufferedDataTable inputTable,
            final int columnIndex, final int classColumnIndex) {

        for (DataRow row : inputTable) {

            // skip missing class value rows
            if (row.getCell(classColumnIndex).isMissing()) {
                continue;
            }
            // get the values of the attributes to count
            String classValue = row.getCell(classColumnIndex).toString();

            // skip rows with missing values in the column to discretize
            if (row.getCell(columnIndex).isMissing()) {
                continue;
            }
            double value =
                    ((DoubleValue)row.getCell(columnIndex)).getDoubleValue();

            // convert the values to indices of the matrix
            int classIndex = m_classValueToIndexMap.get(classValue);
            int intervalIndex = m_dScheme.getIntervalIndexOf(value);

            // increase the counters
            m_mainMatrix[intervalIndex][classIndex]++;
            m_classTotals[classIndex]++;
            m_intervalTotals[intervalIndex]++;
            m_total++;
        }
    }

    /**
     * Calculates the current caim value for the quanta matrix.
     * 
     * @return the caim value as defined by Kurgan et al.
     */
    public double calculateCaim() {
        double sum = 0;
        for (int i = 0; i < m_intervalTotals.length; i++) {
            double maxValueOfInterval = (double)getMaxValue(m_mainMatrix[i]);

            sum +=
                    maxValueOfInterval / m_intervalTotals[i]
                            * maxValueOfInterval;
        }

        return sum / m_intervalTotals.length;
    }

    private double getMaxValue(final int[] ints) {
        int max = 0;

        for (int value : ints) {
            if (value > max) {
                max = value;
            }
        }

        return max;
    }
}
