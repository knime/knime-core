/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   May 18, 2006 (ritmeier): created
 */
package org.knime.testing.node.differNode;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;

/**
 * 
 * @author ritmeier, University of Konstanz
 */
public class LearnerScoreComperator implements TestEvaluator {

    private int m_lowerTolerance;

    private int m_upperTolerance;

    /**
     * default Constructor.
     */
    public LearnerScoreComperator() {
        super();
    }

    /**
     * Compares the score of a learner with an old score. Throws exception if
     * the actual score is worse than the old or if it is more than 5% better.
     * 
     * @param lastScoreTable - the score to compare with
     * @param newScoreTable - the score of the actual run
     * @see org.knime.testing.node.differNode.TestEvaluator#compare(
     *      org.knime.core.data.DataTable, org.knime.core.data.DataTable)
     */
    public void compare(final DataTable newScoreTable,
            final DataTable lastScoreTable) throws TestEvaluationException {
        double lastError = getError(lastScoreTable);
        double actualError = getError(newScoreTable);
        if (lastError + m_upperTolerance < actualError) {
            throw new TestEvaluationException("\nScore is more than "
                    + m_upperTolerance + "% worse than before." + "\n(Was "
                    + lastError + " is now " + actualError + ")");
        }
        if (lastError - m_lowerTolerance > actualError) {
            throw new TestEvaluationException("\nScore is more than "
                    + m_lowerTolerance
                    + "% better than last time. \nPlease update the test.\n"
                    + "(Was " + lastError + " is now " + actualError + ")");
        }
    }

    /**
     * Set the tolerance for the evaluation. Specified numbers will be
     * interpreted as absolute percent numbers, i.e. if the golden score is
     * X% classification error and the upper and lower tolerance is set to 5,
     * this comparator will accept all actual error from X-5 to X+5.
     * 
     * @param lower - the lower bound of tolerated score differences. Specify
     *            the percentage number.
     * @param upper - the upper bound of the tolerated score differences in
     *            percent.
     */
    public void setTolerance(int lower, int upper) {
        m_lowerTolerance = lower;
        m_upperTolerance = upper;
    }

    /**
     * Calculates the error in the score table.
     * 
     * @param scoreTable - the score table
     * @return - the error in percent.
     */
    private double getError(DataTable scoreTable) {
        double matchCount = 0;
        double errorCount = 0;
        int rowIndex = 0;
        int colIndex = 0;
        for (DataRow row : scoreTable) {
            for (DataCell cell : row) {
                double value = Double.parseDouble(cell.toString());
                if (rowIndex == colIndex) {
                    matchCount += value;
                } else {
                    errorCount += value;
                }
                colIndex++;
            }
            colIndex = 0;
            rowIndex++;
        }
        double error = 0.0d;
        double allCount = matchCount + errorCount;
        error = (100.0d / allCount) * errorCount;
        return error;
    }

}
