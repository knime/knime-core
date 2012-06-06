/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   May 31, 2012 (wiswedel): created
 */
package org.knime.base.node.preproc.correlation.pmcc;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.base.util.HalfDoubleMatrix;
import org.knime.base.util.HalfIntMatrix;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.util.Pair;

/** Calculates pairwise correlation values for a table. Uses Cramers'V for
 * pairs of categorical columns and the standard linear correlation coefficient
 * for numerical pairs. Missing values are treated as a separated category
 * for categorical columns and pairwise ignored for num columns. This
 * corresponds the option "R<-cor(R, use="pairwise.complete.obs")" in R.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class PMCCCalculator {

    private final DataTableSpec m_tableSpec;
    /** indices of numeric columns. */
    private final int[] m_numericColIndexMap;
    /** indices of categoric columns. */
    private final int[] m_categoricalColIndexMap;

    /** max possible values (user setting). */
    private final int m_maxPossibleValues;

    /** populated in first scan on data. For each categorical column the
     * map of possible values with their ordered index. Array element is null
     * if column has too many distinct values. */
    private LinkedHashMap<DataCell, Integer>[] m_possibleValues;

    /** Square matrix of mean values.
     * m_numericMeanMatrix[i][j] - The mean value of the values in column i
     * where the corresponding value in column j is NOT missing.
     */
    private double[][] m_numericMeanMatrix;

    /** Square matrix of standard deviation values.
     * Similar to m_numericMeanMatrix.
     */
    private double[][] m_numericStdDevMatrix;

    /** Counts the number of valid records for each pair of numeric columns. */
    private HalfIntMatrix m_numericValidCountMatrix;

    /** The list of numeric column indices where we saw missing values. */
    private final Set<Integer> m_numericsWithMissings;

    /** The list of column pair indices where the standard deviation is
     * too small (no correlation possible). If only the first pair value is
     * assigned the column is constant also in the full table. */
    private final List<Pair<Integer, Integer>> m_numericsWithConstantValues;

    /** Inits fields.
     * @param filteredSpec ...
     * @param maxPossibleValues ... */
    PMCCCalculator(final DataTableSpec filteredSpec,
            final int maxPossibleValues) {
        m_tableSpec = filteredSpec;
        int colCount = filteredSpec.getNumColumns();
        m_maxPossibleValues = maxPossibleValues;
        int[] numericColIndexMap = new int[colCount];
        int[] categoricalColIndexMap = new int[colCount];
        int numericColCount = 0;
        int categoricalColCount = 0;
        for (int i = 0; i < colCount; i++) {
            DataColumnSpec cs = filteredSpec.getColumnSpec(i);
            if (cs.getType().isCompatible(DoubleValue.class)) {
                numericColIndexMap[numericColCount++] = i;
            } else if (cs.getType().isCompatible(NominalValue.class)) {
                categoricalColIndexMap[categoricalColCount++] = i;
            }
        }
        m_numericColIndexMap =
            Arrays.copyOf(numericColIndexMap, numericColCount);
        m_categoricalColIndexMap =
            Arrays.copyOf(categoricalColIndexMap, categoricalColCount);
        m_numericsWithConstantValues =
            new LinkedList<Pair<Integer, Integer>>();
        m_numericsWithMissings = new LinkedHashSet<Integer>();
    }

    /** First scan on the data. Calculates (pair wise) means and std dev
     * and determines the list of distinct values for each categorical column.
     * @param table ...
     * @param exec ...
     * @throws CanceledExecutionException */
    @SuppressWarnings("unchecked")
    void calculateStatistics(final BufferedDataTable table,
            final ExecutionContext exec) throws CanceledExecutionException {
        DataTableSpec filterTableSpec = table.getDataTableSpec();
        assert filterTableSpec.equalStructure(m_tableSpec);
        m_possibleValues = new LinkedHashMap[m_categoricalColIndexMap.length];
        for (int i = 0; i < m_possibleValues.length; i++) {
            m_possibleValues[i] = new LinkedHashMap<DataCell, Integer>();
        }
        final int numericColCount = m_numericColIndexMap.length;
        double[][] sumMatrix = new double[numericColCount][numericColCount];
        double[][] sumSqMatrix = new double[numericColCount][numericColCount];
        HalfIntMatrix validCountMatrix =
            new HalfIntMatrix(numericColCount, true);
        final DataCell[] cells = new DataCell[m_tableSpec.getNumColumns()];
        int rowIndex = 0;
        final int rowCount = table.getRowCount();
        for (DataRow r : table) {
            // getCell may be an expensive operation and we may access a cell
            // multiple times, so we buffer it
            for (int i = 0; i < cells.length; i++) {
                cells[i] = r.getCell(i);
            }
            for (int i = 0; i < m_numericColIndexMap.length; i++) {
                DataCell c = cells[m_numericColIndexMap[i]];
                final boolean isMissing = c.isMissing();
                if (isMissing) {
                    m_numericsWithMissings.add(m_numericColIndexMap[i]);
                } else {
                    final double val = ((DoubleValue)c).getDoubleValue();
                    final double valSquare = val * val;
                    for (int j = 0; j < m_numericColIndexMap.length; j++) {
                        if (!cells[m_numericColIndexMap[j]].isMissing()) {
                            sumMatrix[i][j] += val;
                            sumSqMatrix[i][j] += valSquare;
                            if (j >= i) { // don't count twice
                                validCountMatrix.add(i, j, 1);
                            }
                        }
                    }
                }
            }
            for (int i = 0; i < m_categoricalColIndexMap.length; i++) {
                DataCell c = r.getCell(m_categoricalColIndexMap[i]);
                if (m_possibleValues[i] != null) {
                    // note: also take missing value as possible value
                    m_possibleValues[i].put(c, null);
                    if (m_possibleValues[i].size() > m_maxPossibleValues) {
                        m_possibleValues[i] = null;
                    }
                }
            }
            exec.checkCanceled();
            exec.setProgress(rowIndex / (double) rowCount, String.format(
                    "Calculating statistics - %d/%d (\"%s\")", rowIndex,
                    rowCount, r.getKey()));
            rowIndex += 1;
        }

        for (LinkedHashMap<DataCell, Integer> map : m_possibleValues) {
            if (map != null) {
                int index = 0;
                for (Map.Entry<DataCell, Integer> entry : map.entrySet()) {
                    entry.setValue(index++);
                }
            }
        }

        // sumMatrix --> m_numericMeanMatrix
        // sumSqMatrix --> m_numericStdDevMatrix
        for (int i = 0; i < numericColCount; i++) {
            for (int j = 0; j < numericColCount; j++) {
                final int validCount = validCountMatrix.get(i, j);
                if (validCount > 1) {
                    double variance =
                        (sumSqMatrix[i][j] - (sumMatrix[i][j] * sumMatrix[i][j])
                                / validCount) / (validCount - 1);
                    if (variance < PMCCPortObjectAndSpec.ROUND_ERROR_OK) {
                        variance = 0.0;
                    }
                    sumSqMatrix[i][j] = Math.sqrt(variance);
                } else {
                    sumSqMatrix[i][j] = 0.0;
                }
                sumMatrix[i][j] = validCount > 0
                    ? sumMatrix[i][j] / validCount : Double.NaN;
            }
        }
        m_numericMeanMatrix = sumMatrix;
        m_numericStdDevMatrix = sumSqMatrix;
        m_numericValidCountMatrix = validCountMatrix;
    }


    /** Second scan on data. Computes the pair wise correlation for numeric
     * columns and reads the contingency tables of pairs of categorical
     * columns into memory.
     * @param table ...
     * @param exec ...
     * @return the output matrix to be turned into the output model
     * @throws CanceledExecutionException */
    HalfDoubleMatrix calculateOutput(final BufferedDataTable table,
            final ExecutionMonitor exec) throws CanceledExecutionException {
        assert table.getDataTableSpec().equalStructure(m_tableSpec);
        int catCount = m_categoricalColIndexMap.length;
        int categoricalPairsCount = (catCount - 1) * catCount / 2;
        // stores all pair-wise contingency tables,
        // contingencyTables[i] == null <--> either column of the corresponding
        // pair has more than m_maxPossibleValues values
        // http://en.wikipedia.org/wiki/Contingency_table
        int[][][] contingencyTables = new int[categoricalPairsCount][][];
        int valIndex = 0;
        for (int i = 0; i < m_categoricalColIndexMap.length; i++) {
            for (int j = i + 1; j < m_categoricalColIndexMap.length; j++) {
                LinkedHashMap<DataCell, Integer> valuesI = m_possibleValues[i];
                LinkedHashMap<DataCell, Integer> valuesJ = m_possibleValues[j];
                if (valuesI != null && valuesJ != null) {
                    int iSize = valuesI.size();
                    int jSize = valuesJ.size();
                    contingencyTables[valIndex] = new int[iSize][jSize];
                }
                valIndex++;
            }
        }
        final int numColumns = m_tableSpec.getNumColumns();
        HalfDoubleMatrix nominatorMatrix = new HalfDoubleMatrix(
                numColumns, /*includeDiagonal=*/false);
        int rowIndex = 0;
        DataCell[] cells = new DataCell[numColumns];
        final int rowCount = table.getRowCount();
        for (int i = 0; i < m_numericColIndexMap.length; i++) {
            final double stdDevI = m_numericStdDevMatrix[i][i];
            if (stdDevI == 0.0) {
                for (int j = i + 1; j < m_numericColIndexMap.length; j++) {
                    nominatorMatrix.set(m_numericColIndexMap[i],
                            m_numericColIndexMap[j], Double.NaN);
                }
                m_numericsWithConstantValues.add(new Pair<Integer, Integer>(
                        m_numericColIndexMap[i], null));
            } else {
                for (int j = i + 1; j < m_numericColIndexMap.length; j++) {
                    final double stdDevJ = m_numericStdDevMatrix[j][j];
                    if (stdDevJ == 0.0) {
                        // reported later (when j is i in the outer loop)
                    } else {
                        double stdDevIUnderJ = m_numericStdDevMatrix[i][j];
                        double stdDevJUnderI = m_numericStdDevMatrix[j][i];
                        if (stdDevIUnderJ == 0.0) {
                            // all values in column i where j is not missing
                            // are constant
                            m_numericsWithConstantValues.add(
                                    new Pair<Integer, Integer>(
                                            m_numericColIndexMap[i],
                                            m_numericColIndexMap[j]));
                            nominatorMatrix.set(m_numericColIndexMap[i],
                                    m_numericColIndexMap[j], Double.NaN);
                        }
                        if (stdDevJUnderI == 0.0) {
                            // all values in column j where i is not missing
                            // are constant
                            m_numericsWithConstantValues.add(
                                    new Pair<Integer, Integer>(
                                            m_numericColIndexMap[j],
                                            m_numericColIndexMap[i]));
                            nominatorMatrix.set(m_numericColIndexMap[i],
                                    m_numericColIndexMap[j], Double.NaN);
                        }
                    }
                }
            }
        }
        for (DataRow r : table) {
            for (int i = 0; i < cells.length; i++) {
                cells[i] = r.getCell(i);
            }
            for (int i = 0; i < m_numericColIndexMap.length; i++) {
                final DataCell ci = cells[m_numericColIndexMap[i]];
                if (ci.isMissing()) {
                    continue;
                }
                if (m_numericStdDevMatrix[i][i] == 0.0) {
                    continue; // constant column, reported above
                }
                final double di = ((DoubleValue)ci).getDoubleValue();
                for (int j = i + 1; j < m_numericColIndexMap.length; j++) {
                    final DataCell cj = cells[m_numericColIndexMap[j]];
                    if (cj.isMissing()) {
                        continue;
                    }
                    final double meanI = m_numericMeanMatrix[i][j];
                    final double stdDevI = m_numericStdDevMatrix[i][j];
                    final double meanJ = m_numericMeanMatrix[j][i];
                    final double stdDevJ = m_numericStdDevMatrix[j][i];

                    if (stdDevI == 0.0 || stdDevJ == 0.0) {
                        continue; // reported above
                    }

                    final double vi = (di - meanI) / stdDevI;
                    final double dj = ((DoubleValue)cj).getDoubleValue();
                    final double vj = (dj - meanJ) / stdDevJ;
                    nominatorMatrix.add(m_numericColIndexMap[i],
                            m_numericColIndexMap[j], vi * vj);
                }
            }
            valIndex = 0;
            for (int i = 0; i < m_categoricalColIndexMap.length; i++) {
                for (int j = i + 1; j < m_categoricalColIndexMap.length; j++) {
                    LinkedHashMap<DataCell, Integer> possibleValuesI =
                        m_possibleValues[i];
                    LinkedHashMap<DataCell, Integer> possibleValuesJ =
                        m_possibleValues[j];
                    if (possibleValuesI == null || possibleValuesJ == null) {
                        continue;
                    }
                    DataCell ci = r.getCell(m_categoricalColIndexMap[i]);
                    DataCell cj = r.getCell(m_categoricalColIndexMap[j]);
                    Integer indexI = possibleValuesI.get(ci);
                    Integer indexJ = possibleValuesJ.get(cj);
                    assert indexI != null && indexI >= 0 : String.format(
                            "Value unknown in value list of column \"%s-\": %s",
                            table.getDataTableSpec().getColumnSpec(
                                    m_categoricalColIndexMap[i]).getName(), ci);
                    assert indexJ != null && indexJ >= 0 : String.format(
                            "Value unknown in value list of column \"%s-\": %s",
                            table.getDataTableSpec().getColumnSpec(
                                    m_categoricalColIndexMap[j]).getName(), ci);
                    contingencyTables[valIndex][indexI][indexJ]++;
                    valIndex++;
                }
            }
            exec.checkCanceled();
            exec.setProgress(rowIndex / (double) rowCount, String.format(
                    "Calculating statistics - %d/%d (\"%s\")", rowIndex,
                    rowCount, r.getKey()));
            rowIndex += 1;
        }

        for (int i = 0; i < m_numericColIndexMap.length; i++) {
            for (int j = i + 1; j < m_numericColIndexMap.length; j++) {
                final int trueI = m_numericColIndexMap[i];
                final int trueJ = m_numericColIndexMap[j];
                double t = nominatorMatrix.get(trueI, trueJ);
                if (!Double.isNaN(t)) {
                    int validCount = m_numericValidCountMatrix.get(i, j);
                    nominatorMatrix.set(trueI, trueJ, t / (validCount - 1));
                }
            }
        }
        valIndex = 0;
        for (int i = 0; i < m_categoricalColIndexMap.length; i++) {
            for (int j = i + 1; j < m_categoricalColIndexMap.length; j++) {
                int[][] contingencyTable = contingencyTables[valIndex];
                double value;
                if (contingencyTable == null) {
                    value = Double.NaN;
                } else {
                    value = computeCramersV(contingencyTable);
                }
                nominatorMatrix.set(m_categoricalColIndexMap[i],
                        m_categoricalColIndexMap[j], value);
                valIndex++;
            }
        }
        return nominatorMatrix;
    }

    /** Composes warning message (or null) on which columns contain missings.
     * @param maxColsToReport ...
     * @return the warning or null. */
    String getNumericMissingValueWarning(final int maxColsToReport) {
        if (m_numericsWithMissings.isEmpty()) {
            return null;
        }
        StringBuilder b = new StringBuilder(
                "Some numeric column(s) contained missing values: ");
        int c = 0;
        for (int colindex : m_numericsWithMissings) {
            if (c < maxColsToReport) {
                b.append(c > 0 ? ", " : "");
                String col = m_tableSpec.getColumnSpec(colindex).getName();
                b.append('\"').append(col).append('\"');
            } else if (c == maxColsToReport) {
                b.append(", <");
                b.append(m_numericsWithMissings.size() - maxColsToReport);
                b.append(" more>...");
            } else {
                break;
            }
            c += 1;
        }
        return b.toString();
    }

    /** Composes warning message on which columns are constant.
     * @param maxToReport ...
     * @return message or null. */
    String getNumericConstantColumnPairs(final int maxToReport) {
        if (m_numericsWithConstantValues.isEmpty()) {
            return null;
        }
        StringBuilder b = new StringBuilder("Some numeric column(s) have "
                + "low variance (are constant): ");
        int c = 0;
        for (Pair<Integer, Integer> p : m_numericsWithConstantValues) {
            if (c < maxToReport) {
                b.append(c > 0 ? ", " : "");
                String col = m_tableSpec.getColumnSpec(p.getFirst()).getName();
                b.append('\"').append(col).append('\"');
                if (p.getSecond() != null) {
                    b.append(" (when \"");
                    b.append(m_tableSpec.getColumnSpec(
                            p.getSecond()).getName());
                    b.append("\" is not missing)");
                }
            } else if (c == maxToReport) {
                b.append(", <");
                b.append(m_numericsWithConstantValues.size() - maxToReport);
                b.append(" more>...");
            } else {
                break;
            }
            c += 1;
        }
        return b.toString();
    }

    /** Calculates G formula, for details see
     * http://en.wikipedia.org/wiki/Chi-square_test and
     * http://en.wikipedia.org/wiki/Cram%C3%A9r%27s_V_%28statistics%29.
     */
    private static double computeCramersV(final int[][] contingency) {
        if (contingency.length <= 1 || contingency[0].length <= 1) {
            return 0.0;
        }
        double[] rowSums = new double[contingency.length];
        double[] colSums = new double[contingency[0].length];
        double totalSum = 0.0;
        for (int i = 0; i < contingency.length; i++) {
            for (int j = 0; j < contingency[0].length; j++) {
                rowSums[i] += contingency[i][j];
                colSums[j] += contingency[i][j];
                totalSum += contingency[i][j];
            }
        }
        double chisquare = 0.0;
        for (int i = 0; i < contingency.length; i++) {
            for (int j = 0; j < contingency[0].length; j++) {
                double expected = rowSums[i] * colSums[j] / totalSum;
                // this is asserted as each row/column must contain at least
                // one value >= 1
                assert expected > 0.0 : "value should be > 0 " + expected;
                double diff = contingency[i][j] - expected;
                chisquare += diff * diff / expected;
            }
        }
        int minValueCount = Math.min(rowSums.length , colSums.length) - 1;
        return Math.sqrt(chisquare / (totalSum * minValueCount));
    }


}
