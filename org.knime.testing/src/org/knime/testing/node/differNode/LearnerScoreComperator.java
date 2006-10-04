/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
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

    private int m_lowerTollerance;

    private int m_upperTollerance;

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
     *      org.knime.core.data.DataTable,
     *      org.knime.core.data.DataTable)
     */
    public void compare(final DataTable newScoreTable,
            final DataTable lastScoreTable) throws TestEvaluationException {
        double lastError = getError(lastScoreTable);
        double actualError = getError(newScoreTable);
        if (lastError < actualError - m_upperTollerance) {
            throw new TestEvaluationException("\nScore is more than "
                    + m_upperTollerance + "% worse than before." + "\nWas "
                    + lastError + " is now " + actualError);
        }
        if (lastError > actualError + 5) {
            throw new TestEvaluationException("\nScore is more than "
                    + m_lowerTollerance
                    + "% better than last time. \nPlease update the test.\n" +
                    "Was " + lastError +" is now " + actualError);
        }
    }

    /**
     * Set the tollerance for the evaluation
     * 
     * @param lower - the lower tollerance
     * @param upper - the upper tollerance
     */
    public void setTollerance(int lower, int upper) {
        m_lowerTollerance = lower;
        m_upperTollerance = upper;
    }

    /**
     * Calculates the error in the score table.
     * 
     * @param scoreTable - the score table
     * @return - the error in percent.
     */
    private double getError(DataTable scoreTable) {
        int matchCount = 0;
        int errorCount = 0;
        int rowIndex = 0;
        int colIndex = 0;
        for (DataRow row : scoreTable) {
            for (DataCell cell : row) {
                int value = Integer.parseInt(cell.toString());
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
        System.out.println("ErrorCount: " + errorCount);
        System.out.println("ErrorMatch: " + matchCount);
        double error = 0.0d;
        long allCount = matchCount + errorCount;
        error = (100.0d / allCount) * errorCount;
        System.out.println("score: " + error);
        return error;
    }

}
