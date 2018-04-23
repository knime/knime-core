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
import org.knime.core.node.util.CheckUtils;

/**
 *
 * @author Pascal Lee, KNIME GmbH, Berlin, Germany
 * @since 3.6
 */
public class AccuracyScorerCalculator {
    private List<ValueStats> m_valueStats; // to delete later

    private BufferedDataTable m_confusionMatrix;

    private BufferedDataTable m_classStats;

    private BufferedDataTable m_overallStats;

    private List<RowKey>[][] m_keyStore;

    private final ScorerCalculatorConfiguration m_config;

    private final List<String> m_warnings;

    static DataTableSpec createConfusionMatrixSpec(final DataTableSpec inSpec, final String firstCol,
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
                    //                    addWarning("Ambiguous value \"" + c.toString() + "\" encountered. Preserving individual instances;"
                    //                        + " consider to convert input columns to string");
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

    static DataTableSpec createClassStatsSpec(final ClassStatisticsConfiguration config) {
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
        DataColumnSpec[] classStatsSpec = (DataColumnSpec[])columnList.toArray();
        return new DataTableSpec(classStatsSpec);
    }

    public static DataTableSpec createOverallStatsSpec(final OverallStatisticsConfiguration config) {
        List<DataColumnSpec> columnList = new ArrayList<DataColumnSpec>();
        if (config.isOverallAccuracyCalculated()) {
            columnList.add(new DataColumnSpecCreator("Overall Accuracy", DoubleCell.TYPE).createSpec());
        }
        if (config.isCohensKappaCalculated()) {
            columnList.add(new DataColumnSpecCreator("Cohen's kappa", DoubleCell.TYPE).createSpec());
        }
        if (config.isOverallErrorCalculated()) {
            columnList.add(new DataColumnSpecCreator("Overall Error", DoubleCell.TYPE).createSpec());
        }
        if (config.isCorrectClassifiedCalculated()) {
            columnList.add(new DataColumnSpecCreator("Correct Classified", IntCell.TYPE).createSpec());
        }
        if (config.isWrongClassifiedCalculated()) {
            columnList.add(new DataColumnSpecCreator("Wrong Classified", IntCell.TYPE).createSpec());
        }
        DataColumnSpec[] classStatsSpec = (DataColumnSpec[])columnList.toArray();
        return new DataTableSpec(classStatsSpec);
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

    public BufferedDataTable getConfusionMatrixTable(final ExecutionContext exec) {
        return m_confusionMatrix;
    }

    public BufferedDataTable getClassStatisticsTable(final ClassStatisticsConfiguration config,
        final ExecutionContext exec) {
        return m_classStats;
    }

    public BufferedDataTable getOverallStatisticsTable(final OverallStatisticsConfiguration config,
        final ExecutionContext exec) {
        return m_overallStats;
    }

    public List<RowKey>[][] getKeyStore() {
        return m_keyStore;
    }

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
        DataTableSpec confusionMatrixSpec = createConfusionMatrixSpec(data.getDataTableSpec(), firstColumnName, secondColumnName, config);
        String[] targetValues = confusionMatrixSpec.getColumnNames();

        DataCell[] values = determineColValues(inSpec, index1, index2, config);
        List<DataCell> valuesList = Arrays.asList(values);
        Set<DataCell> valuesInCol2 = new HashSet<DataCell>();

        // the scorerCount counts the confusions
        int[][] scorerCount = new int[targetValues.length][targetValues.length];

        // the key store remembers the row key for later hiliting
        List<RowKey>[][] keyStore = new List[targetValues.length][targetValues.length];
        for (int i = 0; i < keyStore.length; i++) {
            for (int j = 0; j < keyStore[i].length; j++) {
                keyStore[i][j] = new ArrayList<RowKey>();
            }
        }

        // filling in the confusion matrix and the keystore
        long rowCnt = data.size();
        int numberOfRows = 0;
        int correctCount = 0;
        int falseCount = 0;
        int missingCount = 0;
        ExecutionMonitor subExec = exec.createSubProgress(0.5);
        for (Iterator<DataRow> it = data.iterator(); it.hasNext(); numberOfRows++) {
            DataRow row = it.next();
            subExec.setProgress((1.0 + numberOfRows) / rowCnt,
                "Computing score, row " + numberOfRows + " (\"" + row.getKey() + "\") of " + data.size());
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
            keyStore[i1][i2].add(row.getKey());
            scorerCount[i1][i2]++;

            if (areEqual) {
                correctCount++;
            } else {
                falseCount++;
            }
        }

        //Creating and filling the confusion matrix datatable
        BufferedDataContainer container = exec.createDataContainer(confusionMatrixSpec);
        for (int i = 0; i < targetValues.length; i++) {
            // need to make a datacell for the row key
            container.addRowToTable(new DefaultRow(targetValues[i], scorerCount[i]));
        }
        container.close();
        m_confusionMatrix = container.getTable();

//        ScorerViewData viewData = new ScorerViewData(scorerCount, numberOfRows, falseCount, correctCount,
//            firstColumnName, secondColumnName, targetValues, m_keyStore);

        // start creating accuracy statistics
//        BufferedDataContainer accTable = exec.createDataContainer(new DataTableSpec(QUALITY_MEASURES_SPECS));
//        m_valueStats = new ArrayList<ValueStats>();
//        for (int r = 0; r < targetValues.length; r++) {
//            ValueStats valueStats = new ValueStats();
//            valueStats.setValueName(targetValues[r]);
//            int tp = viewData.getTP(r); // true positives
//            int fp = viewData.getFP(r); // false positives
//            int tn = viewData.getTN(r); // true negatives
//            int fn = viewData.getFN(r); // false negatives
//            valueStats.setTP(tp);
//            valueStats.setFP(fp);
//            valueStats.setTN(tn);
//            valueStats.setFN(fn);
//            DoubleCell accuracy = null; // (TP + TN) / (TP + FN + TN + FP)
//            if (tp + fn + tn + fp > 0) {
//                accuracy = new DoubleCell(1.0 * (tp + tn) / (tp + fn + tn + fp));
//                valueStats.setAccuracy(accuracy.getDoubleValue());
//            } else {
//                valueStats.setAccuracy(Double.NaN);
//            }
//            DoubleCell balancedAccuracy = null; // (TP / (TP + FN) + TN / (FP + TN)) /2
//            if ((tp + fn > 0) && (fp + tn > 0)) {
//                balancedAccuracy = new DoubleCell((1.0 * tp / (tp + fn) + 1.0 * tn / (fp + tn)) / 2);
//                valueStats.setBalancedAccuracy(balancedAccuracy.getDoubleValue());
//            } else {
//                valueStats.setBalancedAccuracy(Double.NaN);
//            }
//            DoubleCell errorRate = null; // (FP + FN) / (TP + FN + TN + FP)
//            if (tp + fn + tn + fp > 0) {
//                errorRate = new DoubleCell(1.0 * (fp + fn) / (tp + fn + tn + fp));
//                valueStats.setErrorRate(errorRate.getDoubleValue());
//            } else {
//                valueStats.setErrorRate(Double.NaN);
//            }
//            DoubleCell falseNegativeRate = null; // FN / (TP + FN)
//            if (tp + fn > 0) {
//                falseNegativeRate = new DoubleCell(1.0 * fn / (tp + fn));
//                valueStats.setFalseNegativeRate(falseNegativeRate.getDoubleValue());
//            } else {
//                valueStats.setFalseNegativeRate(Double.NaN);
//            }
//            final DataCell sensitivity; // TP / (TP + FN)
//            DoubleCell recall = null; // TP / (TP + FN)
//            if (tp + fn > 0) {
//                recall = new DoubleCell(1.0 * tp / (tp + fn));
//                valueStats.setRecall(recall.getDoubleValue());
//                sensitivity = new DoubleCell(1.0 * tp / (tp + fn));
//                valueStats.setSensitivity(((DoubleCell)sensitivity).getDoubleValue());
//            } else {
//                sensitivity = DataType.getMissingCell();
//                valueStats.setRecall(Double.NaN);
//                valueStats.setSensitivity(Double.NaN);
//            }
//            DoubleCell prec = null; // TP / (TP + FP)
//            if (tp + fp > 0) {
//                prec = new DoubleCell(1.0 * tp / (tp + fp));
//                valueStats.setPrecision(prec.getDoubleValue());
//            } else {
//                valueStats.setPrecision(Double.NaN);
//            }
//            final DataCell specificity; // TN / (TN + FP)
//            if (tn + fp > 0) {
//                specificity = new DoubleCell(1.0 * tn / (tn + fp));
//                valueStats.setSpecificity(((DoubleCell)specificity).getDoubleValue());
//            } else {
//                specificity = DataType.getMissingCell();
//                valueStats.setSpecificity(Double.NaN);
//            }
//            final DataCell fmeasure; // 2 * Prec. * Recall / (Prec. + Recall)
//            if (recall != null && prec != null) {
//                fmeasure = new DoubleCell(2.0 * prec.getDoubleValue() * recall.getDoubleValue()
//                    / (prec.getDoubleValue() + recall.getDoubleValue()));
//                valueStats.setFmeasure(((DoubleCell)fmeasure).getDoubleValue());
//            } else {
//                fmeasure = DataType.getMissingCell();
//                valueStats.setFmeasure(Double.NaN);
//            }
//            // add complete row for class value to table
//            DataRow row = new DefaultRow(new RowKey(targetValues[r]),
//                new DataCell[]{new IntCell(tp), new IntCell(fp), new IntCell(tn), new IntCell(fn), accuracy,
//                    balancedAccuracy, errorRate, falseNegativeRate, recall == null ? DataType.getMissingCell() : recall,
//                    prec == null ? DataType.getMissingCell() : prec, sensitivity, specificity, fmeasure,
//                    DataType.getMissingCell(), DataType.getMissingCell()});
//            accTable.addRowToTable(row);
//            // store accuracy statistics in list of AccuracyScorerValue for JS
//            m_valueStats.add(valueStats);
//        }
//        List<String> classIds = Arrays.asList(targetValues);
//        RowKey overallID = new RowKey("Overall");
//        int uniquifier = 1;
//        while (classIds.contains(overallID.getString())) {
//            overallID = new RowKey("Overall (#" + (uniquifier++) + ")");
//        }
//        // append additional row for overall accuracy
//        accTable.addRowToTable(new DefaultRow(overallID, new DataCell[]{DataType.getMissingCell(),
//            DataType.getMissingCell(), DataType.getMissingCell(), DataType.getMissingCell(), DataType.getMissingCell(),
//            DataType.getMissingCell(), DataType.getMissingCell(), DataType.getMissingCell(), DataType.getMissingCell(),
//            DataType.getMissingCell(), DataType.getMissingCell(), DataType.getMissingCell(), DataType.getMissingCell(),
//            new DoubleCell(viewData.getAccuracy()), new DoubleCell(viewData.getCohenKappa())}));
//        accTable.close();

        //        pushFlowVars(false);

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
        //        m_viewData = null;
    }

    /**
     * Class encapsulating the calculated statistics for a given class
     *
     * @author Pascal Lee
     */
    public class ValueStats {
        private String m_valueName;

        private int m_TP; // True Positive

        private int m_TN; // True Negative

        private int m_FP; // False Positive

        private int m_FN; // False Negative

        private double m_accuracy;

        private double m_balancedAccuracy;

        private double m_errorRate;

        private double m_falseNegativeRate;

        private double m_recall;

        private double m_precision;

        private double m_sensitivity;

        private double m_specificity;

        private double m_fmeasure;

        /**
         * ValuesStats constructor
         */
        public ValueStats() {
        }

        /**
         * @return valueName
         */
        public String getValueName() {
            return m_valueName;
        }

        /**
         * @param valueName
         */
        public void setValueName(final String valueName) {
            this.m_valueName = valueName;
        }

        /**
         * @return TP
         */
        public int getTP() {
            return m_TP;
        }

        /**
         * @param TP
         */
        public void setTP(final int TP) {
            this.m_TP = TP;
        }

        /**
         * @return TN
         */
        public int getTN() {
            return m_TN;
        }

        /**
         * @param TN
         */
        public void setTN(final int TN) {
            this.m_TN = TN;
        }

        /**
         * @return FP
         */
        public int getFP() {
            return m_FP;
        }

        /**
         * @param FP
         */
        public void setFP(final int FP) {
            this.m_FP = FP;
        }

        /**
         * @return FN
         */
        public int getFN() {
            return m_FN;
        }

        /**
         * @param FN
         */
        public void setFN(final int FN) {
            this.m_FN = FN;
        }

        /**
         * @return accuracy
         */
        public double getAccuracy() {
            return m_accuracy;
        }

        /**
         * @param accuracy
         */
        public void setAccuracy(final double accuracy) {
            this.m_accuracy = accuracy;
        }

        /**
         * @return balanced accuracy
         */
        public double getBalancedAccuracy() {
            return m_balancedAccuracy;
        }

        /**
         * @param balancedAccuracy
         */
        public void setBalancedAccuracy(final double balancedAccuracy) {
            this.m_balancedAccuracy = balancedAccuracy;
        }

        /**
         * @return error rate
         */
        public double getErrorRate() {
            return m_errorRate;
        }

        /**
         * @param errorRate
         */
        public void setErrorRate(final double errorRate) {
            this.m_errorRate = errorRate;
        }

        /**
         * @return falseNegativeRate
         */
        public double getFalseNegativeRate() {
            return m_falseNegativeRate;
        }

        /**
         * @param falseNegativeRate
         */
        public void setFalseNegativeRate(final double falseNegativeRate) {
            this.m_falseNegativeRate = falseNegativeRate;
        }

        /**
         * @return recall
         */
        public double getRecall() {
            return m_recall;
        }

        /**
         * @param recall
         */
        public void setRecall(final double recall) {
            this.m_recall = recall;
        }

        /**
         * @return precision
         */
        public double getPrecision() {
            return m_precision;
        }

        /**
         * @param precision
         */
        public void setPrecision(final double precision) {
            this.m_precision = precision;
        }

        /**
         * @return sensitivity
         */
        public double getSensitivity() {
            return m_sensitivity;
        }

        /**
         * @param sensitivity
         */
        public void setSensitivity(final double sensitivity) {
            this.m_sensitivity = sensitivity;
        }

        /**
         * @return specificity
         */
        public double getSpecificity() {
            return m_specificity;
        }

        /**
         * @param specificity
         */
        public void setSpecificity(final double specificity) {
            this.m_specificity = specificity;
        }

        /**
         * @return Fmeasure
         */
        public double getFmeasure() {
            return m_fmeasure;
        }

        /**
         * @param Fmeasure
         */
        public void setFmeasure(final double fmeasure) {
            this.m_fmeasure = fmeasure;
        }
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
     * Configuration of the class statistics datatable
     *
     * @author Pascal Lee
     */
    public static class ClassStatisticsConfiguration {
        boolean tpCalculated;

        boolean fpCalculated;

        boolean tnCalculated;

        boolean fnCalculated;

        boolean accuracyCalculated;

        boolean balancedAccuracyCalculated;

        boolean errorRateCalculated;

        boolean falseNegativeRateCalculated;

        boolean recallCalculated;

        boolean precisionCalculated;

        boolean sensitivityCalculated;

        boolean specifityCalculated;

        boolean fmeasureCalculated;

        /**
         * @return the tpCalculated
         */
        public boolean isTpCalculated() {
            return tpCalculated;
        }

        /**
         * @param tpCalculated the tpCalculated to set
         */
        public void setTpCalculated(final boolean tpCalculated) {
            this.tpCalculated = tpCalculated;
        }

        /**
         * @return the fpCalculated
         */
        public boolean isFpCalculated() {
            return fpCalculated;
        }

        /**
         * @param fpCalculated the fpCalculated to set
         */
        public void setFpCalculated(final boolean fpCalculated) {
            this.fpCalculated = fpCalculated;
        }

        /**
         * @return the tnCalculated
         */
        public boolean isTnCalculated() {
            return tnCalculated;
        }

        /**
         * @param tnCalculated the tnCalculated to set
         */
        public void setTnCalculated(final boolean tnCalculated) {
            this.tnCalculated = tnCalculated;
        }

        /**
         * @return the fnCalculated
         */
        public boolean isFnCalculated() {
            return fnCalculated;
        }

        /**
         * @param fnCalculated the fnCalculated to set
         */
        public void setFnCalculated(final boolean fnCalculated) {
            this.fnCalculated = fnCalculated;
        }

        /**
         * @return the accuracyCalculated
         */
        public boolean isAccuracyCalculated() {
            return accuracyCalculated;
        }

        /**
         * @param accuracyCalculated the accuracyCalculated to set
         */
        public void setAccuracyCalculated(final boolean accuracyCalculated) {
            this.accuracyCalculated = accuracyCalculated;
        }

        /**
         * @return the balancedAccuracyCalculated
         */
        public boolean isBalancedAccuracyCalculated() {
            return balancedAccuracyCalculated;
        }

        /**
         * @param balancedAccuracyCalculated the balancedAccuracyCalculated to set
         */
        public void setBalancedAccuracyCalculated(final boolean balancedAccuracyCalculated) {
            this.balancedAccuracyCalculated = balancedAccuracyCalculated;
        }

        /**
         * @return the errorRateCalculated
         */
        public boolean isErrorRateCalculated() {
            return errorRateCalculated;
        }

        /**
         * @param errorRateCalculated the errorRateCalculated to set
         */
        public void setErrorRateCalculated(final boolean errorRateCalculated) {
            this.errorRateCalculated = errorRateCalculated;
        }

        /**
         * @return the falseNegativeRateCalculated
         */
        public boolean isFalseNegativeRateCalculated() {
            return falseNegativeRateCalculated;
        }

        /**
         * @param falseNegativeRateCalculated the falseNegativeRateCalculated to set
         */
        public void setFalseNegativeRateCalculated(final boolean falseNegativeRateCalculated) {
            this.falseNegativeRateCalculated = falseNegativeRateCalculated;
        }

        /**
         * @return the recallCalculated
         */
        public boolean isRecallCalculated() {
            return recallCalculated;
        }

        /**
         * @param recallCalculated the recallCalculated to set
         */
        public void setRecallCalculated(final boolean recallCalculated) {
            this.recallCalculated = recallCalculated;
        }

        /**
         * @return the precisionCalculated
         */
        public boolean isPrecisionCalculated() {
            return precisionCalculated;
        }

        /**
         * @param precisionCalculated the precisionCalculated to set
         */
        public void setPrecisionCalculated(final boolean precisionCalculated) {
            this.precisionCalculated = precisionCalculated;
        }

        /**
         * @return the sensitivityCalculated
         */
        public boolean isSensitivityCalculated() {
            return sensitivityCalculated;
        }

        /**
         * @param sensitivityCalculated the sensitivityCalculated to set
         */
        public void setSensitivityCalculated(final boolean sensitivityCalculated) {
            this.sensitivityCalculated = sensitivityCalculated;
        }

        /**
         * @return the specifityCalculated
         */
        public boolean isSpecifityCalculated() {
            return specifityCalculated;
        }

        /**
         * @param specifityCalculated the specifityCalculated to set
         */
        public void setSpecifityCalculated(final boolean specifityCalculated) {
            this.specifityCalculated = specifityCalculated;
        }

        /**
         * @return the fmeasureCalculated
         */
        public boolean isFmeasureCalculated() {
            return fmeasureCalculated;
        }

        /**
         * @param fmeasureCalculated the fmeasureCalculated to set
         */
        public void setFmeasureCalculated(final boolean fmeasureCalculated) {
            this.fmeasureCalculated = fmeasureCalculated;
        }
    }

    /**
     * Configuration of the overall statistics datatable
     *
     * @author Pascal Lee
     */
    public static class OverallStatisticsConfiguration {
        boolean overallAccuracyCalculated;

        boolean cohensKappaCalculated;

        boolean overallErrorCalculated;

        boolean correctClassifiedCalculated;

        boolean wrongClassifiedCalculated;

        /**
         * @return the overallAccuracyCalculated
         */
        public boolean isOverallAccuracyCalculated() {
            return overallAccuracyCalculated;
        }

        /**
         * @param overallAccuracyCalculated the overallAccuracyCalculated to set
         */
        public void setOverallAccuracyCalculated(final boolean overallAccuracyCalculated) {
            this.overallAccuracyCalculated = overallAccuracyCalculated;
        }

        /**
         * @return the cohensKappaCalculated
         */
        public boolean isCohensKappaCalculated() {
            return cohensKappaCalculated;
        }

        /**
         * @param cohensKappaCalculated the cohensKappaCalculated to set
         */
        public void setCohensKappaCalculated(final boolean cohensKappaCalculated) {
            this.cohensKappaCalculated = cohensKappaCalculated;
        }

        /**
         * @return the overallErrorCalculated
         */
        public boolean isOverallErrorCalculated() {
            return overallErrorCalculated;
        }

        /**
         * @param overallErrorCalculated the overallErrorCalculated to set
         */
        public void setOverallErrorCalculated(final boolean overallErrorCalculated) {
            this.overallErrorCalculated = overallErrorCalculated;
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
        public void setCorrectClassifiedCalculated(final boolean correctClassifiedCalculated) {
            this.correctClassifiedCalculated = correctClassifiedCalculated;
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
        public void setWrongClassifiedCalculated(final boolean wrongClassifiedCalculated) {
            this.wrongClassifiedCalculated = wrongClassifiedCalculated;
        }
    }
}
