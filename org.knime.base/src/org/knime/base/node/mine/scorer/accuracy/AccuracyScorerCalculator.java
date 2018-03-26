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
import org.knime.core.node.util.CheckUtils;

/**
 *
 * @author Pascal Lee, KNIME GmbH, Berlin, Germany
 */
public class AccuracyScorerCalculator {
    private String[] m_targetValues;                        //Names of classifications
    private int[][] m_confusionMatrix;
    private List<String>[][] m_keyStore;                    //Keys are stored as strings values
    private List<ValueStats> m_valueStats;
    private double m_accuracy;
    private double m_cohensKappa;
    private BufferedDataTable m_confusionMatrixDatatable;   //Output to the former Java Scorer Node
    private BufferedDataTable m_accuracyDatatable;          //Output to the former Java Scorer Node
    private List<String> m_warnings;


    private SortingStrategy m_sortingStrategy =  SortingStrategy.InsertionOrder;
    private boolean m_sortingReversed = false;  //TODO: configure in dialog options
    private boolean m_ignoreMissingValues = true;   //TODO: configure in dialog options
    private ScorerViewData m_viewData;

    private static final DataColumnSpec[] QUALITY_MEASURES_SPECS = new DataColumnSpec[]{
        new DataColumnSpecCreator("TruePositives", IntCell.TYPE).createSpec(),
        new DataColumnSpecCreator("FalsePositives", IntCell.TYPE).createSpec(),
        new DataColumnSpecCreator("TrueNegatives", IntCell.TYPE).createSpec(),
        new DataColumnSpecCreator("FalseNegatives", IntCell.TYPE).createSpec(),
        new DataColumnSpecCreator("Recall", DoubleCell.TYPE).createSpec(),
        new DataColumnSpecCreator("Precision", DoubleCell.TYPE).createSpec(),
        new DataColumnSpecCreator("Sensitivity", DoubleCell.TYPE).createSpec(),
        new DataColumnSpecCreator("Specifity", DoubleCell.TYPE).createSpec(),
        new DataColumnSpecCreator("F-measure", DoubleCell.TYPE).createSpec(),
        new DataColumnSpecCreator("Accuracy", DoubleCell.TYPE).createSpec(),
        new DataColumnSpecCreator("Cohen's kappa", DoubleCell.TYPE).createSpec()
    };


    public AccuracyScorerCalculator(final BufferedDataTable table, final String firstColumnName, final String secondColumnName, final ExecutionContext exec) {
        try {
            calculate(table, firstColumnName, secondColumnName, exec);
        } catch (CanceledExecutionException e) {
            // TODO Auto-generated catch block
        }
    }

    public void calculate(final BufferedDataTable data, final String firstColumnName, final String secondColumnName, final ExecutionContext exec)
            throws CanceledExecutionException {
        // check input data
        assert (data != null);
        // blow away result from last execute (should have been reset anyway)
        // first try to figure out what are the different class values
        // in the two respective columns
        BufferedDataTable in = data;
        DataTableSpec inSpec = in.getDataTableSpec();
        final int index1 = inSpec.findColumnIndex(firstColumnName);
        final int index2 = inSpec.findColumnIndex(secondColumnName);

        // two elements, first is column names, second row names;
        // these arrays are ordered already, i.e. if both columns have
        // cells in common (e.g. both have Iris-Setosa), they get the same
        // index in the array. thus, the high numbers should appear
        // in the diagonal
        DataCell[] values = determineColValues(in, index1, index2, exec.createSubProgress(0.5));
        List<DataCell> valuesList = Arrays.asList(values);
        Set<DataCell> valuesInCol2 = new HashSet<DataCell>();

        // initializing warnings list
        m_warnings =  new ArrayList<String>();

        // the key store remembers the row key for later hiliting
        List<RowKey>[][] keyStore = new List[values.length][values.length];
        m_keyStore = new List[values.length][values.length];
        // the scorerCount counts the confusions
        int[][] scorerCount = new int[values.length][values.length];

        // init the matrix
        for (int i = 0; i < keyStore.length; i++) {
            for (int j = 0; j < keyStore[i].length; j++) {
                keyStore[i][j] = new ArrayList<RowKey>();
                m_keyStore[i][j] = new ArrayList<String>();
            }
        }

        // filling in the confusion matrix and the keystore
        long rowCnt = in.size();
        int numberOfRows = 0;
        int correctCount = 0;
        int falseCount = 0;
        int missingCount = 0;
        ExecutionMonitor subExec = exec.createSubProgress(0.5);
        for (Iterator<DataRow> it = in.iterator(); it.hasNext(); numberOfRows++) {
            DataRow row = it.next();
            subExec.setProgress((1.0 + numberOfRows) / rowCnt, "Computing score, row " + numberOfRows + " (\"" + row.getKey()
                + "\") of " + in.size());
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
                CheckUtils.checkState(m_ignoreMissingValues, "Missing value in row: " + row.getKey());
                if (m_ignoreMissingValues) {
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
            m_keyStore[i1][i2].add(row.getKey().getString());
            scorerCount[i1][i2]++;

            if (areEqual) {
                correctCount++;
            } else {
                falseCount++;
            }
        }
        m_confusionMatrix = scorerCount;

        // determining the target values
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
                    s = s.concat(" (" + secondColumnName + ")");
                } else {
                    s = s.concat(" (" + firstColumnName + ")");
                }
                String newName = s;
                while (!valuesAsStringSet.add(newName)) {
                    newName = s + "#" + (uniquifier++);
                }
                targetValues[i] = newName;
                if (!hasPrintedWarningOnAmbiguousValues) {
                    hasPrintedWarningOnAmbiguousValues = true;
                    addWarning("Ambiguous value \"" + c.toString()
                        + "\" encountered. Preserving individual instances;"
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
        m_targetValues = targetValues;

        if (missingCount > 0) {
            addWarning("There were missing values in the reference or in the prediction class columns.");
        }

        DataType[] colTypes = new DataType[targetValues.length];
        Arrays.fill(colTypes, IntCell.TYPE);
        BufferedDataContainer container = exec.createDataContainer(new DataTableSpec(targetValues, colTypes));
        for (int i = 0; i < targetValues.length; i++) {
            // need to make a datacell for the row key
            container.addRowToTable(new DefaultRow(targetValues[i], scorerCount[i]));
        }
        container.close();

        ScorerViewData viewData =
            new ScorerViewData(scorerCount, numberOfRows, falseCount, correctCount, firstColumnName,
                secondColumnName, targetValues, keyStore);
        m_accuracy = viewData.getAccuracy();
        m_cohensKappa = viewData.getCohenKappa();

        // print info
        int missing = numberOfRows - correctCount - falseCount;
//        LOGGER.info("error=" + viewData.getError() + ", #correct=" + viewData.getCorrectCount() + ", #false="
//            + viewData.getFalseCount() + ", #rows=" + numberOfRows + ", #missing=" + missing);
        // our view displays the table - we must keep a reference in the model.
        m_confusionMatrixDatatable = container.getTable();

        // start creating accuracy statistics
        BufferedDataContainer accTable = exec.createDataContainer(new DataTableSpec(QUALITY_MEASURES_SPECS));
        m_valueStats =  new ArrayList<ValueStats>();
        for (int r = 0; r < targetValues.length; r++) {
            ValueStats valueStats = new ValueStats();
            valueStats.setValueName(targetValues[r]);
            int tp = viewData.getTP(r); // true positives
            int fp = viewData.getFP(r); // false positives
            int tn = viewData.getTN(r); // true negatives
            int fn = viewData.getFN(r); // false negatives
            valueStats.setTP(tp);
            valueStats.setFP(fp);
            valueStats.setTN(tn);
            valueStats.setFN(fn);
            final DataCell sensitivity; // TP / (TP + FN)
            DoubleCell recall = null; // TP / (TP + FN)
            if (tp + fn > 0) {
                recall = new DoubleCell(1.0 * tp / (tp + fn));
                valueStats.setRecall(recall.getDoubleValue());
                sensitivity = new DoubleCell(1.0 * tp / (tp + fn));
                valueStats.setSensitivity(((DoubleCell)sensitivity).getDoubleValue());
            } else {
                sensitivity = DataType.getMissingCell();
                valueStats.setRecall(Double.NaN);
                valueStats.setSensitivity(Double.NaN);
            }
            DoubleCell prec = null; // TP / (TP + FP)
            if (tp + fp > 0) {
                prec = new DoubleCell(1.0 * tp / (tp + fp));
                valueStats.setPrecision(prec.getDoubleValue());
            } else {
                valueStats.setPrecision(Double.NaN);
            }
            final DataCell specificity; // TN / (TN + FP)
            if (tn + fp > 0) {
                specificity = new DoubleCell(1.0 * tn / (tn + fp));
                valueStats.setSpecificity(((DoubleCell)specificity).getDoubleValue());
            } else {
                specificity = DataType.getMissingCell();
                valueStats.setSpecificity(Double.NaN);
            }
            final DataCell fmeasure; // 2 * Prec. * Recall / (Prec. + Recall)
            if (recall != null && prec != null) {
                fmeasure =
                    new DoubleCell(2.0 * prec.getDoubleValue() * recall.getDoubleValue()
                        / (prec.getDoubleValue() + recall.getDoubleValue()));
                valueStats.setFmeasure(((DoubleCell)fmeasure).getDoubleValue());
            } else {
                fmeasure = DataType.getMissingCell();
                valueStats.setFmeasure(Double.NaN);
            }
            // add complete row for class value to table
            DataRow row =
                new DefaultRow(new RowKey(targetValues[r]), new DataCell[]{new IntCell(tp), new IntCell(fp),
                    new IntCell(tn), new IntCell(fn), recall == null ? DataType.getMissingCell() : recall,
                    prec == null ? DataType.getMissingCell() : prec, sensitivity, specificity, fmeasure,
                    DataType.getMissingCell(), DataType.getMissingCell()});
            accTable.addRowToTable(row);
            // store accuracy statistics in list of AccuracyScorerValue for JS
            m_valueStats.add(valueStats);
        }
        List<String> classIds = Arrays.asList(targetValues);
        RowKey overallID = new RowKey("Overall");
        int uniquifier = 1;
        while (classIds.contains(overallID.getString())) {
            overallID = new RowKey("Overall (#" + (uniquifier++) + ")");
        }
        // append additional row for overall accuracy
        accTable.addRowToTable(new DefaultRow(overallID, new DataCell[]{DataType.getMissingCell(),
            DataType.getMissingCell(), DataType.getMissingCell(), DataType.getMissingCell(), DataType.getMissingCell(),
            DataType.getMissingCell(), DataType.getMissingCell(), DataType.getMissingCell(), DataType.getMissingCell(),
            new DoubleCell(viewData.getAccuracy()), new DoubleCell(viewData.getCohenKappa())}));
        accTable.close();

        m_viewData = viewData;
//        pushFlowVars(false);
        m_accuracyDatatable = accTable.getTable();

        return;
    }

    /**
     * Called to determine all possible values in the respective columns.
     *
     * @param in the input table
     * @param index1 the first column to compare
     * @param index2 the second column to compare
     * @param exec object to check with if user canceled
     * @return the order of rows and columns in the confusion matrix
     * @throws CanceledExecutionException if user canceled operation
     */
    private DataCell[] determineColValues(final BufferedDataTable in, final int index1, final int index2,
        final ExecutionMonitor exec) throws CanceledExecutionException {
        long rowCnt = in.size();
        DataTableSpec inSpec = in.getDataTableSpec();
        DataColumnSpec col1 = inSpec.getColumnSpec(index1);
        DataColumnSpec col2 = inSpec.getColumnSpec(index2);
        Set<DataCell> v1 = col1.getDomain().getValues();
        Set<DataCell> v2 = col2.getDomain().getValues();
        LinkedHashSet<DataCell> values1;
        LinkedHashSet<DataCell> values2;
        if (v1 == null || v2 == null) { // values not available
            values1 = new LinkedHashSet<DataCell>();
            values2 = new LinkedHashSet<DataCell>();
            int rowNr = 0;
            for (Iterator<DataRow> it = in.iterator(); it.hasNext(); rowNr++) {
                DataRow row = it.next();
                exec.checkCanceled(); // throws exception if user canceled.
                exec.setProgress((1.0 + rowNr) / rowCnt,
                    "Reading possible values, row " + rowNr + " (\"" + row.getKey() + "\")");
                DataCell cell1 = row.getCell(index1);
                DataCell cell2 = row.getCell(index2);
                values1.add(cell1);
                values2.add(cell2);
            }
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
        sort(order);
        return order;
    }

    /**
     * @param order The cells to sort.
     */
    private void sort(final DataCell[] order) {
        if (order.length == 0) {
            return;
        }
        DataType type = order[0].getType();
        for (DataCell dataCell : order) {
            type = DataType.getCommonSuperType(type, dataCell.getType());
        }
        final Comparator<DataCell> comparator;
        switch (m_sortingStrategy) {
            case InsertionOrder:
                if (m_sortingReversed) {
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
                throw new IllegalStateException("Unrecognized sorting strategy: " + m_sortingStrategy);
        }
        Arrays.sort(order, comparator);
        if (m_sortingReversed) {
            reverse(order);
        }
    }

    /**
     * Reverses the order of cells.
     *
     * @param order Some cells.
     */
    private void reverse(final DataCell[] order) {
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
        m_viewData = null;
    }

    public String[] getTargetValues() {
        return m_targetValues;
    }

    public int[][] getConfusionMatrix() {
        return m_confusionMatrix;
    }

    public List<String>[][] getKeyStore() {
        return m_keyStore;
    }

    public List<ValueStats> getValueStats() {
        return m_valueStats;
    }

    public BufferedDataTable getConfusionMatrixDatatable() {
        return m_confusionMatrixDatatable;
    }

    public BufferedDataTable getAccuracyDatatable() {
        return m_accuracyDatatable;
    }

    public SortingStrategy getSortingStrategy() {
        return m_sortingStrategy;

    }

    public boolean isSortingReversed() {
        return m_sortingReversed;
    }

    public boolean ignoreMissingValues() {
        return m_ignoreMissingValues;
    }

    public ScorerViewData getViewData() {
        return m_viewData;
    }

    public static DataColumnSpec[] getQualityMeasuresSpecs() {
        return QUALITY_MEASURES_SPECS;
    }

    public double getAccuracy() {
        return m_accuracy;
    }

    public double getCohensKappa() {
        return m_cohensKappa;
    }

    public Iterator<ValueStats> getIterator() {
        return m_valueStats.iterator();
    }

    public List<String> getWarnings() {
        return m_warnings;
    }

    public class ValueStats {
        private String m_valueName;
        private int m_TP;   // True Positive
        private int m_TN;   // True Negative
        private int m_FP;   // False Positive
        private int m_FN;   // False Negative
        private double m_recall;
        private double m_precision;
        private double m_sensitivity;
        private double m_specificity;
        private double m_fmeasure;

        public ValueStats() {
        }

        public String getValueName() {
            return m_valueName;
        }

        public void setValueName(final String valueName) {
            this.m_valueName = valueName;
        }

        public int getTP() {
            return m_TP;
        }

        public void setTP(final int TP) {
            this.m_TP = TP;
        }

        public int getTN() {
            return m_TN;
        }

        public void setTN(final int TN) {
            this.m_TN = TN;
        }

        public int getFP() {
            return m_FP;
        }

        public void setFP(final int FP) {
            this.m_FP = FP;
        }

        public int getFN() {
            return m_FN;
        }

        public void setFN(final int FN) {
            this.m_FN = FN;
        }

        public double getRecall() {
            return m_recall;
        }

        public void setRecall(final double recall) {
            this.m_recall = recall;
        }

        public double getPrecision() {
            return m_precision;
        }

        public void setPrecision(final double precision) {
            this.m_precision = precision;
        }

        public double getSensitivity() {
            return m_sensitivity;
        }

        public void setSensitivity(final double sensitivity) {
            this.m_sensitivity = sensitivity;
        }

        public double getSpecificity() {
            return m_specificity;
        }

        public void setSpecificity(final double specificity) {
            this.m_specificity = specificity;
        }

        public double getFmeasure() {
            return m_fmeasure;
        }

        public void setFmeasure(final double fmeasure) {
            this.m_fmeasure = fmeasure;
        }
    }
}
