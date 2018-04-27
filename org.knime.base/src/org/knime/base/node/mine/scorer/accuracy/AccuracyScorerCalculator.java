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
 *   Mar 15, 2018 (Pascal Lee): created
 */
package org.knime.base.node.mine.scorer.accuracy;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.knime.base.util.SortingStrategy;
import org.knime.base.util.StringValueComparator;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;

/**
 * Class calculating the confusion matrix, class statistics and overall statistics.
 * The JavaScript Scorer node uses it.
 * @author Pascal Lee, KNIME GmbH, Berlin, Germany
 * @since 3.6
 */
public class AccuracyScorerCalculator {

    /** The node logger for this class. */
    protected static final NodeLogger LOGGER = NodeLogger.getLogger(AccuracyScorerCalculator.class);

    private final ScorerCalculatorConfiguration m_config;

    private DataTableSpec m_confusionMatrixSpec;

    private String[] m_targetValues;

    private int[][] m_scorerCount;

    private List<RowKey>[][] m_keyStore;

    private int m_rowsNumber;

    private int m_falseCount;

    private int m_correctCount;

    private final List<String> m_warnings;

    /**
     * @param inSpec        input data table specification
     * @param firstCol      name of the column containing actual classes
     * @param secondCol     name of the column containing predicted classes
     * @param config     ScorerCalculator configuration
     * @return the specification of the confusion matrix data table
     * @throws InvalidSettingsException
     */
    public static DataTableSpec createConfusionMatrixSpec(final DataTableSpec inSpec, final String firstCol,
        final String secondCol, final ScorerCalculatorConfiguration config) throws InvalidSettingsException {
        CheckUtils.checkNotNull(inSpec);
        if (inSpec.getNumColumns() < 2) {
            throw new InvalidSettingsException("The input table must have at least two colums to compare");
        }
        if ((firstCol == null) || (secondCol == null)) {
            throw new InvalidSettingsException("No columns selected yet.");
        }
        if (!inSpec.containsName(firstCol)) {
            throw new InvalidSettingsException("Column " + firstCol + " not found.");
        }
        if (!inSpec.containsName(secondCol)) {
            throw new InvalidSettingsException("Column " + secondCol + " not found.");
        }

        final int index1 = inSpec.findColumnIndex(firstCol);
        final int index2 = inSpec.findColumnIndex(secondCol);
        DataCell[] values = determineColValues(inSpec, index1, index2, config);
        Set<DataCell> valuesInCol2 = new HashSet<DataCell>();

        // determining the target values which will become the final ones
        HashSet<String> valuesAsStringSet = new HashSet<String>();
        HashSet<String> duplicateValuesAsString = new HashSet<String>();
        for (DataCell c : values) {
            valuesAsStringSet.add(c.toString());
        }
        for (DataCell c : values) {
            String cAsString = c.toString();
            if (!valuesAsStringSet.remove(cAsString)) {
                duplicateValuesAsString.add(cAsString);
            }
        }
        boolean hasPrintedWarningOnAmbiguousValues = false;
        String[] targetValues = new String[values.length];
        for (int i = 0; i < targetValues.length; i++) {
            DataCell c = values[i];
            String s = c.toString();
            if (duplicateValuesAsString.contains(s)) {
                boolean isInSecondColumn = valuesInCol2.contains(c);
                int uniquifier = 1;
                if (isInSecondColumn) {
                    s = s.concat(" (" + secondCol + ")");
                } else {
                    s = s.concat(" (" + firstCol + ")");
                }
                String newName = s;
                while (!valuesAsStringSet.add(newName)) {
                    newName = s + "#" + (uniquifier++);
                }
                targetValues[i] = newName;
                if (!hasPrintedWarningOnAmbiguousValues) {
                    hasPrintedWarningOnAmbiguousValues = true;
                     LOGGER.warn("Ambiguous value \"" + c.toString() + "\" encountered. Preserving individual instances;"
                             + " consider to convert input columns to string");
                }
            } else {
                int uniquifier = 1;
                String newName = s;
                while (!valuesAsStringSet.add(newName)) {
                    newName = s + "#" + (uniquifier++);
                }
                targetValues[i] = newName;
            }
        }

        DataType[] colTypes = new DataType[targetValues.length];
        Arrays.fill(colTypes, IntCell.TYPE);
        return new DataTableSpec(targetValues, colTypes);
    }

    /**
     * @param config    configuration of the class statistics table
     * @return the specification of the class statistics data table
     */
    public static DataTableSpec createClassStatsSpec(final ClassStatisticsConfiguration config) {
        List<DataColumnSpec> columnList = new ArrayList<DataColumnSpec>();
        if (config.isTpCalculated()) {
            columnList.add(new DataColumnSpecCreator("TruePositives", IntCell.TYPE).createSpec());
        }
        if (config.isFpCalculated()) {
            columnList.add(new DataColumnSpecCreator("FalsePositives", IntCell.TYPE).createSpec());
        }
        if (config.isTnCalculated()) {
            columnList.add(new DataColumnSpecCreator("TrueNegatives", IntCell.TYPE).createSpec());
        }
        if (config.isFnCalculated()) {
            columnList.add(new DataColumnSpecCreator("FalseNegatives", IntCell.TYPE).createSpec());
        }
        if (config.isAccuracyCalculated()) {
            columnList.add(new DataColumnSpecCreator("Accuracy", DoubleCell.TYPE).createSpec());
        }
        if (config.isBalancedAccuracyCalculated()) {
            columnList.add(new DataColumnSpecCreator("BalancedAccuracy", DoubleCell.TYPE).createSpec());
        }
        if (config.isErrorRateCalculated()) {
            columnList.add(new DataColumnSpecCreator("ErrorRate", DoubleCell.TYPE).createSpec());
        }
        if (config.isFalseNegativeRateCalculated()) {
            columnList.add(new DataColumnSpecCreator("FalseNegativeRate", DoubleCell.TYPE).createSpec());
        }
        if (config.isRecallCalculated()) {
            columnList.add(new DataColumnSpecCreator("Recall", DoubleCell.TYPE).createSpec());
        }
        if (config.isPrecisionCalculated()) {
            columnList.add(new DataColumnSpecCreator("Precision", DoubleCell.TYPE).createSpec());
        }
        if (config.isSensitivityCalculated()) {
            columnList.add(new DataColumnSpecCreator("Sensitivity", DoubleCell.TYPE).createSpec());
        }
        if (config.isSpecifityCalculated()) {
            columnList.add(new DataColumnSpecCreator("Specifity", DoubleCell.TYPE).createSpec());
        }
        if (config.isFmeasureCalculated()) {
            columnList.add(new DataColumnSpecCreator("F-measure", DoubleCell.TYPE).createSpec());
        }
        DataColumnSpec[] classStatsSpec = new DataColumnSpec[columnList.size()];
        classStatsSpec = columnList.toArray(classStatsSpec);
        return new DataTableSpec(classStatsSpec);
    }

    /**
     * @param config configuration of the overall statistics table
     * @return the specification of the overall statistics data table
     */
    public static DataTableSpec createOverallStatsSpec(final OverallStatisticsConfiguration config) {
        List<DataColumnSpec> columnList = new ArrayList<DataColumnSpec>();
        if (config.isOverallAccuracyCalculated()) {
            columnList.add(new DataColumnSpecCreator("Overall Accuracy", DoubleCell.TYPE).createSpec());
        }
        if (config.isOverallErrorCalculated()) {
            columnList.add(new DataColumnSpecCreator("Overall Error", DoubleCell.TYPE).createSpec());
        }
        if (config.isCohensKappaCalculated()) {
            columnList.add(new DataColumnSpecCreator("Cohen's kappa", DoubleCell.TYPE).createSpec());
        }
        if (config.isCorrectClassifiedCalculated()) {
            columnList.add(new DataColumnSpecCreator("Correct Classified", IntCell.TYPE).createSpec());
        }
        if (config.isWrongClassifiedCalculated()) {
            columnList.add(new DataColumnSpecCreator("Wrong Classified", IntCell.TYPE).createSpec());
        }
        DataColumnSpec[] overallStatsSpec = new DataColumnSpec[columnList.size()];
        overallStatsSpec = columnList.toArray(overallStatsSpec);
        return new DataTableSpec(overallStatsSpec);
    }

    /**
     * @param inputTable
     * @param firstColumnName
     * @param secondColumnName
     * @param config the Scorer calculator configuration
     * @param exec
     * @return an AccuracyScorerCalculator instance
     * @throws CanceledExecutionException
     * @throws InvalidSettingsException
     */
    public static AccuracyScorerCalculator createCalculator(final BufferedDataTable inputTable,
        final String firstColumnName, final String secondColumnName, final ScorerCalculatorConfiguration config,
        final ExecutionContext exec) throws CanceledExecutionException, InvalidSettingsException {
        AccuracyScorerCalculator calc = new AccuracyScorerCalculator(config);
        calc.calculate(inputTable, firstColumnName, secondColumnName, config, exec);
        return calc;
    }

    private AccuracyScorerCalculator(final ScorerCalculatorConfiguration config) {
        m_config = config;
        m_warnings = new ArrayList<String>();
    }

    /**
     * This is a getter method which creates a buffered data table and fills it with the confusion matrix.
     *
     * @param exec                  the execution context
     * @return BufferedDataTable    contains the confusion matrix
     */
    public BufferedDataTable getConfusionMatrixTable(final ExecutionContext exec) {
        BufferedDataContainer container =  null;
        try {
            container = exec.createDataContainer(m_confusionMatrixSpec);
            for (int i = 0; i < m_targetValues.length; i++) {
                container.addRowToTable(new DefaultRow(m_targetValues[i], m_scorerCount[i]));
            }
        } finally {
            if (container != null) {
                container.close();
            }
        }
        if (container == null) {
         // This will never be called, it's just for removing the compiler warning.
            throw new NullPointerException();
        }
        return container.getTable();
    }

    /**
     * This is a getter method which creates a buffered data table and fills it with the class statistics.
     *
     * the following values can be calculated for each class:
     * True Positives
     * False Positives
     * True Negatives
     * False Negatives
     * Accuracy
     * Balanced Accuracy
     * Error Rate
     * False Negative Rate
     * Recall
     * Precision
     * Sensitivity
     * Specificity
     * F-measure
     *
     * The values which will be effectively present in the data table are the one chosen in the node configuration,
     * in the Class Statistics Options tab.
     *
     * @param config                the class statistics table configuration
     * @param exec                  the execution context
     * @return BufferedDataTable    contains the chosen class statistics
     */
    public BufferedDataTable getClassStatisticsTable(final ClassStatisticsConfiguration config,
        final ExecutionContext exec) {
        BufferedDataContainer container = null;
        try {
            container = exec.createDataContainer(createClassStatsSpec(config));
            for (int r = 0; r < m_targetValues.length; r++) {
                int tp = getTP(r); // true positives
                int fp = getFP(r); // false positives
                int tn = getTN(r); // true negatives
                int fn = getFN(r); // false negatives
                DataCell accuracy = null; // (TP + TN) / (TP + FN + TN + FP)
                if (tp + fn + tn + fp > 0) {
                    accuracy = new DoubleCell(1.0 * (tp + tn) / (tp + fn + tn + fp));
                } else {
                    accuracy = DataType.getMissingCell();
                }
                DataCell balancedAccuracy = null; // (TP / (TP + FN) + TN / (FP + TN)) /2
                if ((tp + fn > 0) && (fp + tn > 0)) {
                    balancedAccuracy = new DoubleCell((1.0 * tp / (tp + fn) + 1.0 * tn / (fp + tn)) / 2);
                } else {
                    balancedAccuracy = DataType.getMissingCell();
                }
                DataCell errorRate = null; // (FP + FN) / (TP + FN + TN + FP)
                if (tp + fn + tn + fp > 0) {
                    errorRate = new DoubleCell(1.0 * (fp + fn) / (tp + fn + tn + fp));
                } else {
                    errorRate = DataType.getMissingCell();
                }
                DataCell falseNegativeRate = null; // FN / (TP + FN)
                if (tp + fn > 0) {
                    falseNegativeRate = new DoubleCell(1.0 * fn / (tp + fn));
                } else {
                    falseNegativeRate = DataType.getMissingCell();
                }
                DataCell sensitivity = null; // TP / (TP + FN)
                DoubleCell recall = null; // TP / (TP + FN)
                if (tp + fn > 0) {
                    sensitivity = new DoubleCell(1.0 * tp / (tp + fn));
                    recall = new DoubleCell(1.0 * tp / (tp + fn));
                } else {
                    sensitivity = DataType.getMissingCell();
                }
                DoubleCell precision = null; // TP / (TP + FP)
                if (tp + fp > 0) {
                    precision = new DoubleCell(1.0 * tp / (tp + fp));
                }
                DataCell specificity = null; // TN / (TN + FP)
                if (tn + fp > 0) {
                    specificity = new DoubleCell(1.0 * tn / (tn + fp));
                } else {
                    specificity = DataType.getMissingCell();
                }
                DataCell fmeasure = null; // 2 * Prec. * Recall / (Prec. + Recall)
                if (recall != null && precision != null) {
                    fmeasure = new DoubleCell(2.0 * precision.getDoubleValue() * recall.getDoubleValue()
                        / (precision.getDoubleValue() + recall.getDoubleValue()));
                } else {
                    fmeasure = DataType.getMissingCell();
                }
                List<DataCell> cellList = new ArrayList<DataCell>();
                if (config.isTpCalculated()) {
                    cellList.add(new IntCell(tp));
                }
                if (config.isFpCalculated()) {
                    cellList.add(new IntCell(fp));
                }
                if (config.isTnCalculated()) {
                    cellList.add(new IntCell(tn));
                }
                if (config.isFnCalculated()) {
                    cellList.add(new IntCell(fn));
                }
                if (config.isAccuracyCalculated()) {
                    cellList.add(accuracy);
                }
                if (config.isBalancedAccuracyCalculated()) {
                    cellList.add(balancedAccuracy);
                }
                if (config.isErrorRateCalculated()) {
                    cellList.add(errorRate);
                }
                if (config.isFalseNegativeRateCalculated()) {
                    cellList.add(falseNegativeRate);
                }
                if (config.isRecallCalculated()) {
                    cellList.add(recall == null ? DataType.getMissingCell() : recall);
                }
                if (config.isPrecisionCalculated()) {
                    cellList.add(precision == null ? DataType.getMissingCell() : precision);
                }
                if (config.isSensitivityCalculated()) {
                    cellList.add(sensitivity);
                }
                if (config.isSpecifityCalculated()) {
                    cellList.add(specificity);
                }
                if (config.isFmeasureCalculated()) {
                    cellList.add(fmeasure);
                }
                DataCell[] cellArray = new DataCell[cellList.size()];
                cellArray = cellList.toArray(cellArray);
                DataRow row = new DefaultRow(new RowKey(m_targetValues[r]), cellArray);
                container.addRowToTable(row);
            }
        } finally {
            if (container != null) {
                container.close();
            }
        }
        if (container == null) {
         // This will never be called, it's just for removing the compiler warning.
            throw new NullPointerException();
        }
        return container.getTable();
    }

    /**
     * This is a getter method which creates a buffered data table and fills it with the overall statistics.
     *
     * the following values can be calculated:
     * Overall Accuracy
     * Overall Error
     * Cohen's kappa
     * Correct Classsified
     * Wrong Classified
     *
     * The values which will be effectively present in the data table are the one chosen in the node configuration,
     * in the Overall Statistics Options tab.
     *
     * @param config                the overall statistics table configuration
     * @param exec                  the execution context
     * @return BufferedDataTable    contains the chosen overall statistics
     */
    public BufferedDataTable getOverallStatisticsTable(final OverallStatisticsConfiguration config,
        final ExecutionContext exec) {
        BufferedDataContainer container = null;
        try {
            container = exec.createDataContainer(createOverallStatsSpec(config));
            List<DataCell> cellList = new ArrayList<DataCell>();
            if (config.isOverallAccuracyCalculated()) {
                cellList.add(new DoubleCell(getOverallAccuracy()));
            }
            if (config.isOverallErrorCalculated()) {
                cellList.add(new DoubleCell(getOverallError()));
            }
            if (config.isCohensKappaCalculated()) {
                cellList.add(new DoubleCell(getCohenKappa()));
            }
            if (config.isCorrectClassifiedCalculated()) {
                cellList.add(new IntCell(m_correctCount));
            }
            if (config.isWrongClassifiedCalculated()) {
                cellList.add(new IntCell(m_falseCount));
            }
            DataCell[] cellArray = new DataCell[cellList.size()];
            cellArray = cellList.toArray(cellArray);
            DataRow row = new DefaultRow(new RowKey("Overall"), cellArray);
            container.addRowToTable(row);
        } finally {
            if (container != null) {
                container.close();
            }
        }
        if (container == null) {
            // This will never be called, it's just for removing the compiler warning.
            throw new NullPointerException();
        }
        return container.getTable();
    }

    /**
     * This is a getter method for the definitive classes (Target Values).
     *
     * @return the classes as a String array
     */
    public String[] getClasses() {
        return m_targetValues;
    }

    /**
     * This is a getter method for the keystore of RowKey associated to the confusion matrix.
     * It will be used in the JS view for slecting the rows related to a given cell of the confusion matrix.
     *
     * @return the keystore as a matrix of RowKey lists
     */
    public List<RowKey>[][] getKeyStore() {
        return m_keyStore;
    }

    /**
     * @return the rows number
     */
    public int getRowsNumber() {
        return m_rowsNumber;
    }

    /**
     * This is a getter for the warnings.
     *
     * @return warnings as a String List
     */
    public List<String> getWarnings() {
        return m_warnings;
    }

    /**
     * Calculates an accuracy scorer confusion matrix, overall statistics and statistics specific to each class
     *
     * @param data the input data
     * @param firstColumnName the column selected containing the real classes
     * @param secondColumnName the column selected containing the predicted classes
     * @param exec Execution context to report progress to
     * @throws CanceledExecutionException when the user cancels the execution
     * @throws
     */
    private void calculate(final BufferedDataTable data, final String firstColumnName, final String secondColumnName, final ScorerCalculatorConfiguration config,
        final ExecutionContext exec) throws InvalidSettingsException, CanceledExecutionException {
        DataTableSpec inSpec = data.getDataTableSpec();
        int index1 = inSpec.findColumnIndex(firstColumnName);
        int index2 = inSpec.findColumnIndex(secondColumnName);
        m_confusionMatrixSpec = createConfusionMatrixSpec(data.getDataTableSpec(), firstColumnName, secondColumnName, config);
        m_targetValues = m_confusionMatrixSpec.getColumnNames();

        DataCell[] values = determineColValues(inSpec, index1, index2, config);
        List<DataCell> valuesList = Arrays.asList(values);
        Set<DataCell> valuesInCol2 = new HashSet<DataCell>();

        // the scorerCount counts the confusions
        m_scorerCount = new int[m_targetValues.length][m_targetValues.length];

        // the key store remembers the row key for later hiliting
        m_keyStore = new List[m_targetValues.length][m_targetValues.length];
        for (int i = 0; i < m_keyStore.length; i++) {
            for (int j = 0; j < m_keyStore[i].length; j++) {
                m_keyStore[i][j] = new ArrayList<RowKey>();
            }
        }

        // filling in the confusion matrix and the keystore
        long rowCnt = data.size();
        m_rowsNumber = 0;
        m_correctCount = 0;
        m_falseCount = 0;
        int missingCount = 0;
        ExecutionMonitor subExec = exec.createSubProgress(0.5);
        for (Iterator<DataRow> it = data.iterator(); it.hasNext(); m_rowsNumber++) {
            DataRow row = it.next();
            subExec.setProgress((1.0 + m_rowsNumber) / rowCnt,
                "Computing score, row " + m_rowsNumber + " (\"" + row.getKey() + "\") of " + data.size());
            try {
                subExec.checkCanceled();
            } catch (CanceledExecutionException cee) {
                reset();
                throw cee;
            }
            DataCell cell1 = row.getCell(index1);
            DataCell cell2 = row.getCell(index2);
            valuesInCol2.add(cell2);
            if (cell1.isMissing() || cell2.isMissing()) {
                ++missingCount;
                CheckUtils.checkState(m_config.isIgnoreMissingValues(), "Missing value in row: " + row.getKey());
                if (m_config.isIgnoreMissingValues()) {
                    continue;
                }
            }
            boolean areEqual = cell1.equals(cell2);

            int i1 = valuesList.indexOf(cell1);
            int i2 = areEqual ? i1 : valuesList.indexOf(cell2);
            assert i1 >= 0 : "column spec lacks possible value " + cell1;
            assert i2 >= 0 : "column spec lacks possible value " + cell2;
            // i2 must be equal to i1 if cells are equal (implication)
            assert (!areEqual || i1 == valuesList.indexOf(cell2));
            m_keyStore[i1][i2].add(row.getKey());
            m_scorerCount[i1][i2]++;

            if (areEqual) {
                m_correctCount++;
            } else {
                m_falseCount++;
            }
        }
        if (missingCount > 0) {
            addWarning("There were missing values in the reference or in the prediction class columns.");
        }
        // print info
        int missing = m_rowsNumber - m_correctCount - m_falseCount;
        LOGGER.info("overall error=" + getOverallError() + ", #correct=" + m_correctCount + ", #false="
            + m_falseCount + ", #rows=" + m_rowsNumber + ", #missing=" + missing);
        return;
    }

    /**
     * Called to determine all possible values in the respective columns.
     *
     * @param specification of the input table
     * @param index1 the first column to compare
     * @param index2 the second column to compare
     * @return the order of rows and columns in the confusion matrix
     * @throws InvalidSettingsException
     */
    static DataCell[] determineColValues(final DataTableSpec inSpec, final int index1, final int index2,
        final ScorerCalculatorConfiguration config) throws InvalidSettingsException {
        DataColumnSpec col1 = inSpec.getColumnSpec(index1);
        DataColumnSpec col2 = inSpec.getColumnSpec(index2);
        Set<DataCell> v1 = col1.getDomain().getValues();
        Set<DataCell> v2 = col2.getDomain().getValues();
        LinkedHashSet<DataCell> values1;
        LinkedHashSet<DataCell> values2;
        if (v1 == null || v2 == null) {
            throw new InvalidSettingsException("The columns selected don't contain any values.");
        } else {
            // clone them, will change the set later on
            values1 = new LinkedHashSet<DataCell>(v1);
            values2 = new LinkedHashSet<DataCell>(v2);
        }
        // number of all possible values in both column (number of rows
        // and columns in the confusion matrix)
        HashSet<DataCell> union = new HashSet<DataCell>(values1);
        union.addAll(values2);
        int unionCount = union.size();

        // intersection of values in columns
        LinkedHashSet<DataCell> intersection = new LinkedHashSet<DataCell>(values1);
        intersection.retainAll(values2);

        // an array of the intersection set
        DataCell[] intSecAr = intersection.toArray(new DataCell[0]);

        // order the elements (classes that occur in both columns first)
        // a good scoring is when the elements in the diagonal are high
        DataCell[] order = new DataCell[unionCount];
        System.arraycopy(intSecAr, 0, order, 0, intSecAr.length);

        // copy all values that are in values1 but not in values2
        values1.removeAll(intersection);
        DataCell[] temp = values1.toArray(new DataCell[0]);
        System.arraycopy(temp, 0, order, intSecAr.length, temp.length);

        // copy all values that are in values2 but not in values1
        values2.removeAll(intersection);
        temp = values2.toArray(new DataCell[0]);
        System.arraycopy(temp, 0, order, order.length - temp.length, temp.length);
        sort(order, config);
        return order;
    }

    /**
     * @param order The cells to sort.
     */
    static void sort(final DataCell[] order, final ScorerCalculatorConfiguration config) {
        if (order.length == 0) {
            return;
        }
        DataType type = order[0].getType();
        for (DataCell dataCell : order) {
            type = DataType.getCommonSuperType(type, dataCell.getType());
        }
        final Comparator<DataCell> comparator;
        switch (config.getSortingStrategy()) {
            case InsertionOrder:
                if (config.isSortingReversed()) {
                    reverse(order);
                }
                return;
            case Unsorted:
                return;
            case Lexical:
                if (StringCell.TYPE.isASuperTypeOf(type)) {
                    Comparator<String> stringComparator;
                    Collator instance = Collator.getInstance();
                    //do not try to combine characters
                    instance.setDecomposition(Collator.NO_DECOMPOSITION);
                    //case and accents matter.
                    instance.setStrength(Collator.IDENTICAL);
                    @SuppressWarnings("unchecked")
                    Comparator<String> collator = (Comparator<String>)(Comparator<?>)instance;
                    stringComparator = collator;
                    comparator = new StringValueComparator(stringComparator);
                } else if (DoubleCell.TYPE.isASuperTypeOf(type)) {
                    comparator = new DataValueComparator() {

                        @Override
                        protected int compareDataValues(final DataValue v1, final DataValue v2) {
                            String s1 = v1.toString();
                            String s2 = v2.toString();
                            return s1.compareTo(s2);
                        }
                    };
                } else {
                    throw new IllegalStateException("Lexical sorting strategy is not supported.");
                }
                break;
            case Numeric:
                if (DoubleCell.TYPE.isASuperTypeOf(type)) {
                    comparator = type.getComparator();
                } else {
                    throw new IllegalStateException("Numerical sorting strategy is not supported.");
                }
                break;
            default:
                throw new IllegalStateException("Unrecognized sorting strategy: " + config.getSortingStrategy());
        }
        Arrays.sort(order, comparator);
        if (config.isSortingReversed()) {
            reverse(order);
        }
    }

    /**
     * Reverses the order of cells.
     *
     * @param order Some cells.
     */
    static void reverse(final DataCell[] order) {
        DataCell tmp;
        for (int i = 0; i < order.length / 2; ++i) {
            int hi = order.length - 1 - i;
            tmp = order[hi];
            order[hi] = order[i];
            order[i] = tmp;
        }
    }

    private void addWarning(final String warning) {
        m_warnings.add(warning);
    }

    /**
     * Resets all internal data.
     */
    public void reset() {
        m_targetValues = null;
        m_scorerCount =  null;
        m_keyStore = null;
        m_correctCount = 0;
        m_falseCount = 0;
    }

    /**
     * Returns the accuracy of the prediction.
     *
     * @return ratio of correct classified and all patterns
     */
    private double getOverallAccuracy() {
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
    private double getOverallError() {
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
     */
    private double getCohenKappa() {
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
    private int getTP(final int classIndex) {
        return m_scorerCount[classIndex][classIndex];
    }

    /**
     * Returns the false negatives for the given class index.
     *
     * @param classIndex the class index
     * @return the false negatives for the given class index
     */
    private int getFN(final int classIndex) {
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
    private int getTN(final int classIndex) {
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
    private int getFP(final int classIndex) {
        int ret = 0;
        for (int i = 0; i < m_scorerCount.length; i++) {
            if (classIndex != i) {
                ret += m_scorerCount[i][classIndex];
            }
        }
        return ret;
    }

    /**
     * Configuration of the Accuracy Scorer Calculator
     *
     * @author Pascal Lee
     */
    public static class ScorerCalculatorConfiguration {
        private SortingStrategy m_sortingStrategy = SortingStrategy.Lexical;

        private boolean m_sortingReversed = false;

        private boolean m_ignoreMissingValues = true;

        /**
         * @return sortingStrategy
         */
        public SortingStrategy getSortingStrategy() {
            return m_sortingStrategy;
        }

        /**
         * @param sortingStrategy
         */
        public void setSortingStrategy(final SortingStrategy sortingStrategy) {
            this.m_sortingStrategy = sortingStrategy;
        }

        /**
         * @return sortingReversed
         */
        public boolean isSortingReversed() {
            return m_sortingReversed;
        }

        /**
         * @param sortingReversed
         */
        public void setSortingReversed(final boolean sortingReversed) {
            this.m_sortingReversed = sortingReversed;
        }

        /**
         * @return ignoreMissingValues
         */
        public boolean isIgnoreMissingValues() {
            return m_ignoreMissingValues;
        }

        /**
         * @param ignoreMissingValues
         */
        public void setIgnoreMissingValues(final boolean ignoreMissingValues) {
            this.m_ignoreMissingValues = ignoreMissingValues;
        }
    }

    /**
     * Configuration of the class statistics data table
     *
     * @author Pascal Lee
     */
    public static class ClassStatisticsConfiguration {
        boolean tpCalculated = true;

        boolean fpCalculated  = true;

        boolean tnCalculated = true;

        boolean fnCalculated = true;

        boolean accuracyCalculated = true;

        boolean balancedAccuracyCalculated = true;

        boolean errorRateCalculated = true;

        boolean falseNegativeRateCalculated = true;

        boolean recallCalculated = true;

        boolean precisionCalculated = true;

        boolean sensitivityCalculated = true;

        boolean specifityCalculated = true;

        boolean fmeasureCalculated = true;

        /**
         * @return the tpCalculated
         */
        public boolean isTpCalculated() {
            return tpCalculated;
        }

        /**
         * @param tpCalculated the tpCalculated to set
         * @return ClassStatisticsConfiguration
         */
        public ClassStatisticsConfiguration withTpCalculated(final boolean tpCalculated) {
            this.tpCalculated = tpCalculated;
            return this;
        }

        /**
         * @return the fpCalculated
         */
        public boolean isFpCalculated() {
            return fpCalculated;
        }

        /**
         * @param fpCalculated the fpCalculated to set
         * @return ClassStatisticsConfiguration
         */
        public ClassStatisticsConfiguration withFpCalculated(final boolean fpCalculated) {
            this.fpCalculated = fpCalculated;
            return this;
        }

        /**
         * @return the tnCalculated
         */
        public boolean isTnCalculated() {
            return tnCalculated;
        }

        /**
         * @param tnCalculated the tnCalculated to set
         * @return ClassStatisticsConfiguration
         */
        public ClassStatisticsConfiguration withTnCalculated(final boolean tnCalculated) {
            this.tnCalculated = tnCalculated;
            return this;
        }

        /**
         * @return the fnCalculated
         */
        public boolean isFnCalculated() {
            return fnCalculated;
        }

        /**
         * @param fnCalculated the fnCalculated to set
         * @return ClassStatisticsConfiguration
         */
        public ClassStatisticsConfiguration withFnCalculated(final boolean fnCalculated) {
            this.fnCalculated = fnCalculated;
            return this;
        }

        /**
         * @return the accuracyCalculated
         */
        public boolean isAccuracyCalculated() {
            return accuracyCalculated;
        }

        /**
         * @param accuracyCalculated the accuracyCalculated to set
         * @return ClassStatisticsConfiguration
         */
        public ClassStatisticsConfiguration withAccuracyCalculated(final boolean accuracyCalculated) {
            this.accuracyCalculated = accuracyCalculated;
            return this;
        }

        /**
         * @return the balancedAccuracyCalculated
         */
        public boolean isBalancedAccuracyCalculated() {
            return balancedAccuracyCalculated;
        }

        /**
         * @param balancedAccuracyCalculated the balancedAccuracyCalculated to set
         * @return ClassStatisticsConfiguration
         */
        public ClassStatisticsConfiguration withBalancedAccuracyCalculated(final boolean balancedAccuracyCalculated) {
            this.balancedAccuracyCalculated = balancedAccuracyCalculated;
            return this;
        }

        /**
         * @return the errorRateCalculated
         */
        public boolean isErrorRateCalculated() {
            return errorRateCalculated;
        }

        /**
         * @param errorRateCalculated the errorRateCalculated to set
         * @return ClassStatisticsConfiguration
         */
        public ClassStatisticsConfiguration withErrorRateCalculated(final boolean errorRateCalculated) {
            this.errorRateCalculated = errorRateCalculated;
            return this;
        }

        /**
         * @return the falseNegativeRateCalculated
         */
        public boolean isFalseNegativeRateCalculated() {
            return falseNegativeRateCalculated;
        }

        /**
         * @param falseNegativeRateCalculated the falseNegativeRateCalculated to set
         * @return ClassStatisticsConfiguration
         */
        public ClassStatisticsConfiguration withFalseNegativeRateCalculated(final boolean falseNegativeRateCalculated) {
            this.falseNegativeRateCalculated = falseNegativeRateCalculated;
            return this;
        }

        /**
         * @return the recallCalculated
         */
        public boolean isRecallCalculated() {
            return recallCalculated;
        }

        /**
         * @param recallCalculated the recallCalculated to set
         * @return ClassStatisticsConfiguration
         */
        public ClassStatisticsConfiguration withRecallCalculated(final boolean recallCalculated) {
            this.recallCalculated = recallCalculated;
            return this;
        }

        /**
         * @return the precisionCalculated
         */
        public boolean isPrecisionCalculated() {
            return precisionCalculated;
        }

        /**
         * @param precisionCalculated the precisionCalculated to set
         * @return ClassStatisticsConfiguration
         */
        public ClassStatisticsConfiguration withPrecisionCalculated(final boolean precisionCalculated) {
            this.precisionCalculated = precisionCalculated;
            return this;
        }

        /**
         * @return the sensitivityCalculated
         */
        public boolean isSensitivityCalculated() {
            return sensitivityCalculated;
        }

        /**
         * @param sensitivityCalculated the sensitivityCalculated to set
         * @return ClassStatisticsConfiguration
         */
        public ClassStatisticsConfiguration withSensitivityCalculated(final boolean sensitivityCalculated) {
            this.sensitivityCalculated = sensitivityCalculated;
            return this;
        }

        /**
         * @return the specifityCalculated
         */
        public boolean isSpecifityCalculated() {
            return specifityCalculated;
        }

        /**
         * @param specifityCalculated the specifityCalculated to set
         * @return ClassStatisticsConfiguration
         */
        public ClassStatisticsConfiguration withSpecifityCalculated(final boolean specifityCalculated) {
            this.specifityCalculated = specifityCalculated;
            return this;
        }

        /**
         * @return the fmeasureCalculated
         */
        public boolean isFmeasureCalculated() {
            return fmeasureCalculated;
        }

        /**
         * @param fmeasureCalculated the fmeasureCalculated to set
         * @return ClassStatisticsConfiguration
         */
        public ClassStatisticsConfiguration withFmeasureCalculated(final boolean fmeasureCalculated) {
            this.fmeasureCalculated = fmeasureCalculated;
            return this;
        }
    }

    /**
     * Configuration of the overall statistics data table
     *
     * @author Pascal Lee
     */
    public static class OverallStatisticsConfiguration {
        boolean overallAccuracyCalculated = true;

        boolean overallErrorCalculated = true;

        boolean cohensKappaCalculated = true;

        boolean correctClassifiedCalculated = true;

        boolean wrongClassifiedCalculated = true;

        /**
         * @return the overallAccuracyCalculated
         */
        public boolean isOverallAccuracyCalculated() {
            return overallAccuracyCalculated;
        }

        /**
         * @param overallAccuracyCalculated the overallAccuracyCalculated to set
         * @return OverallStatisticsConfiguration
         */
        public OverallStatisticsConfiguration withOverallAccuracyCalculated(final boolean overallAccuracyCalculated) {
            this.overallAccuracyCalculated = overallAccuracyCalculated;
            return this;
        }

        /**
         * @return the cohensKappaCalculated
         */
        public boolean isCohensKappaCalculated() {
            return cohensKappaCalculated;
        }

        /**
         * @param cohensKappaCalculated the cohensKappaCalculated to set
         * @return OverallStatisticsConfiguration
         */
        public OverallStatisticsConfiguration withCohensKappaCalculated(final boolean cohensKappaCalculated) {
            this.cohensKappaCalculated = cohensKappaCalculated;
            return this;
        }

        /**
         * @return the overallErrorCalculated
         */
        public boolean isOverallErrorCalculated() {
            return overallErrorCalculated;
        }

        /**
         * @param overallErrorCalculated the overallErrorCalculated to set
         * @return OverallStatisticsConfiguration
         */
        public OverallStatisticsConfiguration withOverallErrorCalculated(final boolean overallErrorCalculated) {
            this.overallErrorCalculated = overallErrorCalculated;
            return this;
        }

        /**
         * @return the correctClassifiedCalculated
         */
        public boolean isCorrectClassifiedCalculated() {
            return correctClassifiedCalculated;
        }

        /**
         * @param correctClassifiedCalculated the correctClassifiedCalculated to set
         */
        public OverallStatisticsConfiguration withCorrectClassifiedCalculated(final boolean correctClassifiedCalculated) {
            this.correctClassifiedCalculated = correctClassifiedCalculated;
            return this;
        }

        /**
         * @return the wrongClassifiedCalculated
         */
        public boolean isWrongClassifiedCalculated() {
            return wrongClassifiedCalculated;
        }

        /**
         * @param wrongClassifiedCalculated the wrongClassifiedCalculated to set
         */
        public OverallStatisticsConfiguration withWrongClassifiedCalculated(final boolean wrongClassifiedCalculated) {
            this.wrongClassifiedCalculated = wrongClassifiedCalculated;
            return this;
        }
    }
}
