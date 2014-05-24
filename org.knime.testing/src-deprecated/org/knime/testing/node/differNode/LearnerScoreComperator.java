/*
 * ------------------------------------------------------------------
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
 * --------------------------------------------------------------------- *
 *
 * History
 *   May 18, 2006 (ritmeier): created
 */
package org.knime.testing.node.differNode;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.testing.internal.nodes.image.ImageDifferNodeFactory;

/**
 *
 * @author ritmeier, University of Konstanz
 *
 * @deprecated use the new image comparator {@link ImageDifferNodeFactory} and the extension point for difference
 *             checker instead
 */
@Deprecated
public class LearnerScoreComperator implements TestEvaluator {

    private int m_lowerTolerance;

    private int m_upperTolerance;

    /**
     * Compares the score of a learner with an old score. Throws exception if
     * the actual score is worse than the old or if it is more than 5% better.
     *
     * @param lastScoreTable - the score to compare with
     * @param newScoreTable - the score of the actual run
     * @see org.knime.testing.node.differNode.TestEvaluator#compare(
     *      org.knime.core.data.DataTable, org.knime.core.data.DataTable)
     */
    @Override
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
    public void setTolerance(final int lower, final int upper) {
        m_lowerTolerance = lower;
        m_upperTolerance = upper;
    }

    /**
     * Calculates the error in the score table.
     *
     * @param scoreTable - the score table
     * @return - the error in percent.
     */
    private double getError(final DataTable scoreTable) {
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
