/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.mine.scorer.accuracy;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.knime.base.node.util.DataArray;
import org.knime.base.node.viz.plotter.DataProvider;
import org.knime.base.util.SortingOptionPanel;
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
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.FlowVariable;

/**
 * The hilite scorer node's model. The scoring is performed on two given columns set by the dialog. The row keys are
 * stored for later hiliting purpose.
 *
 * @author Christoph Sieb, University of Konstanz
 *
 * @see AccuracyScorerNodeFactory
 */
public class AccuracyScorerNodeModel extends NodeModel implements DataProvider {

    /** The node logger for this class. */
    protected static final NodeLogger LOGGER = NodeLogger.getLogger(AccuracyScorerNodeModel.class);

    /** Identifier in model spec to address first column name to compare. */
    static final String FIRST_COMP_ID = "first";

    /** Identifier in model spec to address first second name to compare. */
    static final String SECOND_COMP_ID = "second";

    /** Identifier in model spec to address the chosen prefix. */
    static final String FLOW_VAR_PREFIX = "flowVarPrefix";

    /** Identifier in model spec to address sorting strategy of labels. */
    static final String SORTING_STRATEGY = SortingOptionPanel.DEFAULT_KEY_SORTING_STRATEGY;

    /** Identifier in model spec to address sorting order of labels. */
    static final String SORTING_REVERSED = SortingOptionPanel.DEFAULT_KEY_SORTING_REVERSED;

    /** Identifier in model spec to specify how to handle missing values. If {@code true}, ignore the problem. */
    static final String ACTION_ON_MISSING_VALUES = "ignore.missing.values";

    /** By default ignore missing values (with a warning). */
    static final boolean DEFAULT_IGNORE_MISSING_VALUES = true;

    /** The input port 0. */
    static final int INPORT = 0;

    /** The output port 0: confusion matrix. */
    static final int OUTPORT_0 = 0;

    /** The output port 1: accuracy measures. */
    static final int OUTPORT_1 = 1;

    /** The prefix added to the flow variable names. **/
    private String m_flowVarPrefix;

    private ScorerViewData m_viewData;

    /**
     * New instance of a HiLiteHandler return on the confusion matrix out-port.
     */
    private final HiLiteHandler m_cmHiLiteHandler = new HiLiteHandler();

    /**
     * New instance of a HiLiteHandler return on the accuracy measures out-port.
     */
    private final HiLiteHandler m_amHiLiteHandler = new HiLiteHandler();

    /** The name of the first column to compare. */
    private String m_firstCompareColumn;

    /** The name of the second column to compare. */
    private String m_secondCompareColumn;

    private boolean m_sortingReversed;

    private SortingStrategy m_sortingStrategy =  SortingStrategy.InsertionOrder;

    private boolean m_ignoreMissingValues = true;

    /** Inits a new <code>ScorerNodeModel</code> with one in- and one output. */
    AccuracyScorerNodeModel() {
        super(1, 2);
    }

    /**
     * Starts the scoring in the scorer.
     *
     * @param data the input data of length one
     * @param exec the execution monitor
     * @return the confusion matrix
     * @throws CanceledExecutionException if user canceled execution
     *
     * @see NodeModel#execute(BufferedDataTable[],ExecutionContext)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] data, final ExecutionContext exec)
        throws CanceledExecutionException {
        // check input data
        assert (data != null && data.length == 1 && data[INPORT] != null);
        // blow away result from last execute (should have been reset anyway)
        // first try to figure out what are the different class values
        // in the two respective columns
        BufferedDataTable in = data[INPORT];
        DataTableSpec inSpec = in.getDataTableSpec();
        final int index1 = inSpec.findColumnIndex(m_firstCompareColumn);
        final int index2 = inSpec.findColumnIndex(m_secondCompareColumn);

        // two elements, first is column names, second row names;
        // these arrays are ordered already, i.e. if both columns have
        // cells in common (e.g. both have Iris-Setosa), they get the same
        // index in the array. thus, the high numbers should appear
        // in the diagonal
        DataCell[] values = determineColValues(in, index1, index2, exec.createSubProgress(0.5));
        List<DataCell> valuesList = Arrays.asList(values);
        Set<DataCell> valuesInCol2 = new HashSet<DataCell>();

        // the key store remembers the row key for later hiliting
        List<RowKey>[][] keyStore = new List[values.length][values.length];
        // the scorerCount counts the confusions
        int[][] scorerCount = new int[values.length][values.length];

        // init the matrix
        for (int i = 0; i < keyStore.length; i++) {
            for (int j = 0; j < keyStore[i].length; j++) {
                keyStore[i][j] = new ArrayList<RowKey>();
            }
        }

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
            scorerCount[i1][i2]++;

            if (areEqual) {
                correctCount++;
            } else {
                falseCount++;
            }
        }

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
                    s = s.concat(" (" + m_secondCompareColumn + ")");
                } else {
                    s = s.concat(" (" + m_firstCompareColumn + ")");
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
            new ScorerViewData(scorerCount, numberOfRows, falseCount, correctCount, m_firstCompareColumn,
                m_secondCompareColumn, targetValues, keyStore);

        // print info
        int missing = numberOfRows - correctCount - falseCount;
        LOGGER.info("error=" + viewData.getError() + ", #correct=" + viewData.getCorrectCount() + ", #false="
            + viewData.getFalseCount() + ", #rows=" + numberOfRows + ", #missing=" + missing);
        // our view displays the table - we must keep a reference in the model.
        BufferedDataTable result = container.getTable();

        // start creating accuracy statistics
        BufferedDataContainer accTable = exec.createDataContainer(new DataTableSpec(QUALITY_MEASURES_SPECS));
        for (int r = 0; r < targetValues.length; r++) {
            int tp = viewData.getTP(r); // true positives
            int fp = viewData.getFP(r); // false positives
            int tn = viewData.getTN(r); // true negatives
            int fn = viewData.getFN(r); // false negatives
            final DataCell sensitivity; // TP / (TP + FN)
            DoubleCell recall = null; // TP / (TP + FN)
            if (tp + fn > 0) {
                recall = new DoubleCell(1.0 * tp / (tp + fn));
                sensitivity = new DoubleCell(1.0 * tp / (tp + fn));
            } else {
                sensitivity = DataType.getMissingCell();
            }
            DoubleCell prec = null; // TP / (TP + FP)
            if (tp + fp > 0) {
                prec = new DoubleCell(1.0 * tp / (tp + fp));
            }
            final DataCell specificity; // TN / (TN + FP)
            if (tn + fp > 0) {
                specificity = new DoubleCell(1.0 * tn / (tn + fp));
            } else {
                specificity = DataType.getMissingCell();
            }
            final DataCell fmeasure; // 2 * Prec. * Recall / (Prec. + Recall)
            if (recall != null && prec != null) {
                fmeasure =
                    new DoubleCell(2.0 * prec.getDoubleValue() * recall.getDoubleValue()
                        / (prec.getDoubleValue() + recall.getDoubleValue()));
            } else {
                fmeasure = DataType.getMissingCell();
            }
            // add complete row for class value to table
            DataRow row =
                new DefaultRow(new RowKey(targetValues[r]), new DataCell[]{new IntCell(tp), new IntCell(fp),
                    new IntCell(tn), new IntCell(fn), recall == null ? DataType.getMissingCell() : recall,
                    prec == null ? DataType.getMissingCell() : prec, sensitivity, specificity, fmeasure,
                    DataType.getMissingCell(), DataType.getMissingCell()});
            accTable.addRowToTable(row);
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
        pushFlowVars(false);

        return new BufferedDataTable[]{result, accTable.getTable()};
    }

    /**
     * @param string
     */
    private void addWarning(final String string) {
        String warningMessage = getWarningMessage();
        if (warningMessage == null || warningMessage.isEmpty()) {
            setWarningMessage(string);
        } else {
            setWarningMessage(warningMessage + "\n" + string);
        }
    }

    /**
     * Pushes the results to flow variables.
     *
     * @param isConfigureOnly true enable overwriting check
     */
    private void pushFlowVars(final boolean isConfigureOnly) {
        Map<String, FlowVariable> vars = getAvailableFlowVariables();
        String prefix = m_flowVarPrefix != null ? m_flowVarPrefix : "";
        String accuracyName = prefix + "Accuracy";
        String errorName = prefix + "Error";
        String correctName = prefix + "#Correct";
        String falseName = prefix + "#False";
        String kappaName = prefix + "Cohen's kappa";
        if (isConfigureOnly
            && (vars.containsKey(accuracyName) || vars.containsKey(errorName) || vars.containsKey(correctName)
                || vars.containsKey(falseName) || vars.containsKey(kappaName))) {
            addWarning("A flow variable was replaced!");
        }

        double accu = isConfigureOnly ? 0.0 : m_viewData.getAccuracy();
        double error = isConfigureOnly ? 0.0 : m_viewData.getError();
        int correctCount = isConfigureOnly ? 0 : m_viewData.getCorrectCount();
        int falseCount = isConfigureOnly ? 0 : m_viewData.getFalseCount();
        double kappa = isConfigureOnly ? 0 : m_viewData.getCohenKappa();
        pushFlowVariableDouble(accuracyName, accu);
        pushFlowVariableDouble(errorName, error);
        pushFlowVariableInt(correctName, correctCount);
        pushFlowVariableInt(falseName, falseCount);
        pushFlowVariableDouble(kappaName, kappa);
    }

    /**
     * Resets all internal data.
     */
    @Override
    protected void reset() {
        m_viewData = null;
    }

    /**
     * Sets the columns that will be compared during execution.
     *
     * @param first the first column
     * @param second the second column
     * @throws NullPointerException if one of the parameters is <code>null</code>
     */
    void setCompareColumn(final String first, final String second) {
        if ((first == null) || (second == null)) {
            throw new IllegalArgumentException("Must specify both columns to compare!");
        }
        m_firstCompareColumn = first;
        m_secondCompareColumn = second;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        if (inSpecs[INPORT].getNumColumns() < 2) {
            throw new InvalidSettingsException("The input table must have at least two colums to compare");
        }
        if ((m_firstCompareColumn == null) || (m_secondCompareColumn == null)) {
            throw new InvalidSettingsException("No columns selected yet.");
        }
        if (!inSpecs[INPORT].containsName(m_firstCompareColumn)) {
            throw new InvalidSettingsException("Column " + m_firstCompareColumn + " not found.");
        }
        if (!inSpecs[INPORT].containsName(m_secondCompareColumn)) {
            throw new InvalidSettingsException("Column " + m_secondCompareColumn + " not found.");
        }

        pushFlowVars(true);
        return new DataTableSpec[]{null, new DataTableSpec(QUALITY_MEASURES_SPECS)};
    }

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
        new DataColumnSpecCreator("Cohen's kappa", DoubleCell.TYPE).createSpec()};


    /**
     * Get the correct classification count, i.e. where both columns agree.
     *
     * @return the count of rows where the two columns have an equal value or -1 if the node is not executed
     * @deprecated use {@link #getViewData()} instead
     */
    @Deprecated
    public int getCorrectCount() {
        return m_viewData.getCorrectCount();
    }

    /**
     * Get the misclassification count, i.e. where both columns have different values.
     *
     * @return the count of rows where the two columns have an unequal value or -1 if the node is not executed
     * @deprecated use {@link #getViewData()} instead
     */
    @Deprecated
    public int getFalseCount() {
        return m_viewData.getFalseCount();
    }


    /**
     * @return ratio of wrong classified and all patterns
     * @deprecated use {@link #getViewData()} instead
     */
    @Deprecated
    public double getError() {
        long totalNumberDataSets = getFalseCount() + getCorrectCount();
        if (totalNumberDataSets == 0) {
            return Double.NaN;
        } else {
            return 1.0 * getFalseCount() / totalNumberDataSets;
        }
    }

    /**
     * @return ratio of correct classified and all patterns
     * @deprecated use {@link #getViewData()} instead
     */
    @Deprecated
    public double getAccuracy() {
        long totalNumberDataSets = getFalseCount() + getCorrectCount();
        if (totalNumberDataSets == 0) {
            return Double.NaN;
        } else {
            return 1.0 * getCorrectCount() / (getCorrectCount() + getFalseCount());
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        String col1 = settings.getString(FIRST_COMP_ID);
        String col2 = settings.getString(SECOND_COMP_ID);
        setCompareColumn(col1, col2);
        // added in 2.6
        m_flowVarPrefix = settings.getString(FLOW_VAR_PREFIX, null);
        // Added sorting strategy, reversed are new in 2.9.0
        m_sortingReversed = settings.getBoolean(SORTING_REVERSED, false);
        m_sortingStrategy =
            SortingStrategy.values()[settings.getInt(SORTING_STRATEGY, SortingStrategy.InsertionOrder.ordinal())];
        m_ignoreMissingValues = settings.getBoolean(ACTION_ON_MISSING_VALUES, DEFAULT_IGNORE_MISSING_VALUES);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_firstCompareColumn != null) {
            settings.addString(FIRST_COMP_ID, m_firstCompareColumn);
        }
        if (m_secondCompareColumn != null) {
            settings.addString(SECOND_COMP_ID, m_secondCompareColumn);
        }

        settings.addString(FLOW_VAR_PREFIX, m_flowVarPrefix);
        // Added sorting strategy, reversed are new in 2.9.0
        settings.addBoolean(SORTING_REVERSED, m_sortingReversed);
        settings.addInt(SORTING_STRATEGY, m_sortingStrategy.ordinal());
        settings.addBoolean(ACTION_ON_MISSING_VALUES, m_ignoreMissingValues);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        settings.getString(FIRST_COMP_ID);
        settings.getString(SECOND_COMP_ID);
        // no flow var prefix in 2.5.x and before
        // sorting strategy and sorting reversed is not present before 2.9.x
        // ignore missing values is not present before 3.0
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
    protected DataCell[] determineColValues(final BufferedDataTable in, final int index1, final int index2,
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

    /**
     * Finds the position where key is located in source. It must be ensured that the key is indeed in the argument
     * array.
     *
     * @param source the source array
     * @param key the key to find
     * @return the index in source where key is located
     */
    protected static int findValue(final DataCell[] source, final DataCell key) {
        for (int i = 0; i < source.length; i++) {
            if (source[i].equals(key)) {
                return i;
            }
        }
        throw new NoSuchElementException("Array does not contain desired value \"" + key + "\".");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec) throws IOException {
        File f = new File(internDir, "internals.xml.gz");
        InputStream in = new GZIPInputStream(new BufferedInputStream(new FileInputStream(f)));
        try {
            NodeSettingsRO set = NodeSettings.loadFromXML(in);
            int correctCount = set.getInt("correctCount");
            int falseCount = set.getInt("falseCount");
            int nrRows = set.getInt("nrRows");
            String[] targetValues = set.getStringArray("values");
            int[][] scorerCount = new int[targetValues.length][];
            List<RowKey>[][] keyStore = new List[targetValues.length][targetValues.length];
            for (int i = 0; i < targetValues.length; i++) {
                NodeSettingsRO sub = set.getNodeSettings(targetValues[i]);
                scorerCount[i] = sub.getIntArray("scorerCount");
                NodeSettingsRO subSub = sub.getNodeSettings("hilightMap");
                for (int j = 0; j < targetValues.length; j++) {
                    NodeSettingsRO sub3 = subSub.getNodeSettings(targetValues[j]);
                    keyStore[i][j] = Arrays.asList(sub3.getRowKeyArray("keyStore"));
                }
            }
            m_viewData =
                new ScorerViewData(scorerCount, nrRows, falseCount, correctCount, m_firstCompareColumn,
                    m_secondCompareColumn, targetValues, keyStore);
        } catch (InvalidSettingsException ise) {
            throw new IOException("Unable to read internals", ise);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec) throws IOException {
        NodeSettings set = new NodeSettings("scorer");
        set.addInt("correctCount", m_viewData.getCorrectCount());
        set.addInt("falseCount", m_viewData.getFalseCount());
        set.addInt("nrRows", m_viewData.getNrRows());
        set.addStringArray("values", m_viewData.getTargetValues());
        final int targetValueCount = m_viewData.getTargetValues().length;
        for (int i = 0; i < targetValueCount; i++) {
            NodeSettingsWO sub = set.addNodeSettings(m_viewData.getTargetValues()[i]);
            sub.addIntArray("scorerCount", m_viewData.getScorerCount()[i]);
            NodeSettingsWO subSub = sub.addNodeSettings("hilightMap");
            for (int j = 0; j < targetValueCount; j++) {
                NodeSettingsWO sub3 = subSub.addNodeSettings(m_viewData.getTargetValues()[j]);
                RowKey[] rowKeys =
                    m_viewData.getKeyStore()[i][j].toArray(new RowKey[m_viewData.getKeyStore()[i][j].size()]);
                sub3.addRowKeyArray("keyStore", rowKeys);
            }
        }

        set.saveToXML(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(new File(internDir,
            "internals.xml.gz")))));
    }

    /**
     * Returns a bit set with data for the ROC curve. A set bit means a correct classified example, an unset bit is a
     * wrong classified example. The number of interesting bits is {@link BitSet#length()} - 1, i.e. the last set bit
     * must be ignored, it is just the end marker.
     *
     * @return a bit set
     */
    @Deprecated
    BitSet getRocCurve() {
        // disabled
        return new BitSet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataArray getDataArray(final int index) {
        // only to make the Plotter happy
        return null;
    }

    /**
     * Returns the first column to compare.
     *
     * @return the first column to compare
     * @deprecated use {@link #getViewData()} instead
     */
    @Deprecated
    public String getFirstCompareColumn() {
        return m_firstCompareColumn;
    }

    /**
     * Returns the second column to compare.
     *
     * @return the second column to compare
     */
    @Deprecated
    public String getSecondCompareColumn() {
        return m_secondCompareColumn;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        if (outIndex == 0) {
            return m_cmHiLiteHandler;
        } else if (outIndex == 1) {
            return m_amHiLiteHandler;
        }
        return null;
    }

    /**
     * @return Cohen's Kappa
     * @since 2.9
     * @deprecated use {@link #getViewData()} instead
     */
    @Deprecated
    public double getCohenKappa() {
        return m_viewData.getCohenKappa();
    }

    /**
     * @deprecated use {@link #getViewData()} instead
     */
    @Deprecated
    public int getNrRows() {
        return m_viewData.getNrRows();
    }

    /**
     * Returns the data that should be displayed in the node's view. May be null if the data has not been computed
     * in {@link #execute(BufferedDataTable[], ExecutionContext)} yet.
     *
     * @return the view data or <code>null</code>
     */
    ScorerViewData getViewData() {
        return m_viewData;
    }
}
