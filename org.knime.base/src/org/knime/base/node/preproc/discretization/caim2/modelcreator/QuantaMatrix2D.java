/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
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
