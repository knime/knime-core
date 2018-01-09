/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   03.11.2014 (thor): created
 */
package org.knime.base.node.mine.scorer.accuracy;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.knime.core.data.RowKey;

/**
 * Class that holds the view information for the scorer.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 * @since 3.2
 */
public final class ScorerViewData {
    /**
     * The confusion matrix as int 2-D array.
     */
    private final int[][] m_scorerCount;

    /**
     * Number of rows in the input table. Interesting if you want to know the number of missing values in either of the
     * target columns.
     */
    private final int m_nrRows;

    /** Number of misclassifications. */
    private final int m_falseCount;

    /** Number of correct classifications. */
    private final int m_correctCount;

    /**
     * The first column (usually containing the real values).
     */
    private final String m_firstCompareColumn;

    /**
     * The second column (usually containing the predicted values).
     */
    private final String m_secondCompareColumn;

    /**
     * All possible target values.
     */
    private final String[] m_targetValues;

    /**
     * Stores the row keys for the confusion matrix fields to allow hiliting.
     */
    private final List<RowKey>[][] m_keyStore;

    /**
     * Constructs the scorer view data
     * 
     * @param scorerCount the scorer counts
     * @param nrRows the number of rows
     * @param falseCount the count of false predictions
     * @param correctCount the count of correct predictions
     * @param firstCompareColumn the first column to compare
     * @param secondCompareColumn the second column to compare
     * @param targetValues the target values
     */
    public ScorerViewData(final int[][] scorerCount, final int nrRows, final int falseCount, final int correctCount,
        final String firstCompareColumn, final String secondCompareColumn, final String[] targetValues) {
        m_scorerCount = scorerCount;
        m_nrRows = nrRows;
        m_falseCount = falseCount;
        m_correctCount = correctCount;
        m_firstCompareColumn = firstCompareColumn;
        m_secondCompareColumn = secondCompareColumn;
        m_targetValues = targetValues;
        m_keyStore = null;
    }

    ScorerViewData(final int[][] scorerCount, final int nrRows, final int falseCount, final int correctCount,
        final String firstCompareColumn, final String secondCompareColumn, final String[] targetValues,
        final List<RowKey>[][] keyStore) {
        m_scorerCount = scorerCount;
        m_nrRows = nrRows;
        m_falseCount = falseCount;
        m_correctCount = correctCount;
        m_firstCompareColumn = firstCompareColumn;
        m_secondCompareColumn = secondCompareColumn;
        m_targetValues = targetValues;
        m_keyStore = keyStore;
    }

    /**
     * Returns the name of the first column to compare.
     * 
     * @return the the name of the first compare column
     */
    public String getFirstCompareColumn() {
        return m_firstCompareColumn;
    }


    /**
     * Returns the name of the second column to compare.
     * 
     * @return the name of the second compare column
     */
    public String getSecondCompareColumn() {
        return m_secondCompareColumn;
    }

    /**
     * Returns the count of correct predictions.
     * 
     * @return the correct count
     */
    public int getCorrectCount() {
        return m_correctCount;
    }

    /**
     * Returns the count of false predictions.
     * 
     * @return the false count
     */
    public int getFalseCount() {
        return m_falseCount;
    }

    /**
     * Returns the target values.
     * 
     * @return the target values
     */
    public String[] getTargetValues() {
        return m_targetValues;
    }

    /**
     * Returns the scorer count.
     * 
     * @return the scorer count
     */
    public int[][] getScorerCount() {
        return m_scorerCount;
    }

    int getNrRows() {
        return m_nrRows;
    }

    List<RowKey>[][] getKeyStore() {
        return m_keyStore;
    }

    /**
     * Returns the accuracy of the prediction.
     * 
     * @return ratio of correct classified and all patterns
     */
    public double getAccuracy() {
        double totalNumberDataSets = m_falseCount + m_correctCount;
        if (totalNumberDataSets == 0) {
            return Double.NaN;
        } else {
            return m_correctCount / totalNumberDataSets;
        }
    }

    /**
     * Returns the error of the prediction.
     * 
     * @return ratio of wrong classified and all patterns
     */
    public double getError() {
        double totalNumberDataSets = m_falseCount + m_correctCount;
        if (totalNumberDataSets == 0) {
            return Double.NaN;
        } else {
            return m_falseCount / totalNumberDataSets;
        }
    }

    /**
     * Returns Cohen's Kappa Coefficient of the prediciton.
     * 
     * @return Cohen's Kappa
     * @since 2.9
     */
    public double getCohenKappa() {
        long[] rowSum = new long[m_scorerCount[0].length];
        long[] colSum = new long[m_scorerCount.length];
        //Based on: https://en.wikipedia.org/wiki/Cohen%27s_kappa#
        long agreement = 0, sum = 0;
        for (int i = 0; i < rowSum.length; i++) {
            for (int j = 0; j < colSum.length; j++) {
                rowSum[i] += m_scorerCount[i][j];
                colSum[j] += m_scorerCount[i][j];
                sum += m_scorerCount[i][j];
            }
            //number of correct agreements
            agreement += m_scorerCount[i][i];
        }
        //relative observed agreement among raters
        final double p0 = agreement * 1d / sum;
        //hypothetical probability of chance agreement
        double pe = 0;
        for (int i = 0; i < m_scorerCount.length; i++) {
            //Expected value that they agree by chance for possible value i
            pe += 1d * rowSum[i] * colSum[i] / sum / sum;
        }
        //kappa
        return (p0 - pe) / (1 - pe);
    }

    /**
     * Returns the true positives for the given class index.
     * 
     * @param classIndex the class index
     * @return the true positives for the given class index
     */
    public int getTP(final int classIndex) {
        return m_scorerCount[classIndex][classIndex];
    }

    /**
     * Returns the false negatives for the given class index.
     * 
     * @param classIndex the class index
     * @return the false negatives for the given class index
     */
    public int getFN(final int classIndex) {
        int ret = 0;
        for (int i = 0; i < m_scorerCount[classIndex].length; i++) {
            if (classIndex != i) {
                ret += m_scorerCount[classIndex][i];
            }
        }
        return ret;
    }


    /**
     * Returns the true negatives for the given class index.
     * 
     * @param classIndex the class index
     * @return the true negatives for the given class index
     */
    public int getTN(final int classIndex) {
        int ret = 0;
        for (int i = 0; i < m_scorerCount.length; i++) {
            if (i != classIndex) {
                for (int j = 0; j < m_scorerCount[i].length; j++) {
                    if (classIndex != j) {
                        ret += m_scorerCount[i][j];
                    }
                }
            }
        }
        return ret;
    }

    /**
     * Returns the false positives for the given class index.
     * 
     * @param classIndex the class index
     * @return the false positives for the given class index
     */
    public int getFP(final int classIndex) {
        int ret = 0;
        for (int i = 0; i < m_scorerCount.length; i++) {
            if (classIndex != i) {
                ret += m_scorerCount[i][classIndex];
            }
        }
        return ret;
    }

    /**
     * Checks if the specified confusion matrix cell contains at least one of the given keys.
     *
     * @param x the x value to specify the matrix cell
     * @param y the y value to specify the matrix cell
     * @param keys the keys to check
     *
     * @return true if at least one key is contained in the specified cell
     */
    boolean containsConfusionMatrixKeys(final int x, final int y, final Set<RowKey> keys) {

        // get the list with the keys
        List<RowKey> keyList = m_keyStore[x][y];
        for (RowKey key : keyList) {
            for (RowKey keyToCheck : keys) {
                if (key.equals(keyToCheck)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Returns all cells of the confusion matrix (as Points) if the given key set contains all keys of that cell.
     *
     * @param keys the keys to check for
     *
     * @return the cells that fullfill the above condition
     */
    Point[] getCompleteHilitedCells(final Set<RowKey> keys) {

        List<Point> result = new ArrayList<>();

        // for all cells of the matrix
        for (int i = 0; i < m_keyStore.length; i++) {
            for (int j = 0; j < m_keyStore[i].length; j++) {
                // for all keys to check
                boolean allKeysIncluded = true;

                for (RowKey key : m_keyStore[i][j]) {

                    boolean wasKeyFound = false;
                    Iterator<RowKey> keysToCheckIterator = keys.iterator();
                    while (keysToCheckIterator.hasNext()) {

                        RowKey keyToCheck = keysToCheckIterator.next();

                        if (key.equals(keyToCheck)) {
                            // if the keys equal remove it, as it can only
                            // occur in one cell
                            // keysToCheckIterator.remove();

                            // remember that the key was found
                            wasKeyFound = true;
                        }
                    }

                    if (!wasKeyFound) {
                        // if one key was not found the cell is not represented
                        // completely by "keys" (the keys to check
                        allKeysIncluded = false;
                    }
                }

                if (allKeysIncluded && m_keyStore[i][j].size() > 0) {
                    result.add(new Point(i, j));
                }
            }
        }

        return result.toArray(new Point[result.size()]);
    }

    /**
     * Determines the row keys (as DataCells) which belong to the given cell of the confusion matrix.
     *
     * @param cells the cells of the confusion matrix for which the keys should be returned
     *
     * @return a set of DataCells containing the row keys
     */
    Set<RowKey> getSelectedSet(final Point[] cells) {

        Set<RowKey> keySet = new HashSet<>();

        for (Point cell : cells) {

            int xVal = cell.x;
            int yVal = cell.y;

            // if the specified x and y values of the matrix are out of range
            // continue
            if (xVal < 0 || yVal < 0 || xVal > m_targetValues.length || yVal > m_targetValues.length) {
                continue;
            }

            keySet.addAll(m_keyStore[xVal][yVal]);
        }

        return keySet;
    }
}
