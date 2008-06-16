/*
 * --------------------------------------------------------------------- *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 */
package org.knime.base.node.mine.scorer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
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


/**
 * The scorer node's model. The scoring is performed on two given columns set by
 * the dialog.
 * 
 * @author Christoph Sieb, University of Konstanz
 * @author Thomas Gabriel, University of Konstanz
 * 
 * @see ScorerNodeFactory
 */
public class ScorerNodeModel extends NodeModel {
    
    /** The node logger for this class. */
    protected static final NodeLogger LOGGER = NodeLogger
            .getLogger(ScorerNodeModel.class);

    /** Identifier in model spec to address first column name to compare. */
    static final String FIRST_COMP_ID = "first";

    /** Identifier in model spec to address first second name to compare. */
    static final String SECOND_COMP_ID = "second";

    /** The input port 0. */
    static final int INPORT = 0;

    /** The output port 0. */
    static final int OUTPORT = 0;

    /** The name of the first column to compare. */
    private String m_firstCompareColumn;

    /** The name of the second column to compare. */

    private String m_secondCompareColumn;

    /** Counter for correct classification, set in execute. */
    private int m_correctCount;

    /** Counter for misclassification, set in execute. */
    private int m_falseCount;

    /**
     * Number of rows in the input table. Interesting if you want to know the
     * number of missing values in either of the target columns.
     */
    private int m_nrRows;

    /** holds the last scorer table for the view. */
    private DataTable m_lastResult;
    
    private static final String FILE_NAME_INTERNAL_SETT = "scorer_internals";
    private static final String FILE_NAME_INTERNAL_RESULT = 
        "scorer_result_table";
    private static final String CFG_CORRECT_COUNT = "correct_count";
    private static final String CFG_FALSE_COUNT = "false_count";
    private static final String CFG_NUMBER_ROWS = "number_rows";
 
    

    /** Inits a new <code>ScorerNodeModel</code> with one in- and one output. */
    ScorerNodeModel() {
        super(1, 1);
        m_lastResult = null;
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
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] data,
            final ExecutionContext exec) throws CanceledExecutionException {
        // check input data
        assert (data != null && data.length == 1 && data[INPORT] != null);
        // blow away result from last execute (should have been reset anyway)
        m_lastResult = null;
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
        String[] values = determineColValues(in, index1, index2, exec);
        List<String> valuesList = Arrays.asList(values);

        m_correctCount = 0;
        m_falseCount = 0;
        int[][] scorerCount = new int[values.length][values.length];
        int rowNr = 0;
        for (Iterator<DataRow> it = in.iterator(); it.hasNext(); rowNr++) {
            DataRow row = it.next();
            exec.setProgress(rowNr / (double)in.getRowCount(),
                    "Computing score, row " + rowNr + " (\"" + row.getKey()
                            + "\") of " + in.getRowCount());
            try {
                exec.checkCanceled();
            } catch (CanceledExecutionException cee) {
                reset();
                throw cee;
            }
            DataCell cell1 = row.getCell(index1);
            DataCell cell2 = row.getCell(index2);
            if (cell1.isMissing() || cell2.isMissing()) {
                continue;
            }
            boolean areEqual = cell1.equals(cell2);
            // need to cast to string (column keys are strings!)
            int i1 = valuesList.indexOf(cell1.toString());
            int i2 = areEqual ? i1 : valuesList.indexOf(cell2.toString());
            assert i1 >= 0 : "column spec lacks possible value " + cell1;
            assert i2 >= 0 : "column spec lacks possible value " + cell2;
            // i2 must be equal to i1 if cells are equal (implication)
            assert (!areEqual || i1 == valuesList.indexOf(cell2.toString()));
            scorerCount[i1][i2]++;
            if (areEqual) {
                m_correctCount++;
            } else {
                m_falseCount++;
            }
        }
        DataType[] colTypes = new DataType[values.length];
        Arrays.fill(colTypes, IntCell.TYPE);
        m_nrRows = rowNr;
        DataRow[] rows = new DataRow[values.length];
        BufferedDataContainer dc = 
            exec.createDataContainer(new DataTableSpec(values, colTypes));
        for (int i = 0; i < rows.length; i++) {
            // need to make a data cell for the row key
            dc.addRowToTable(new DefaultRow(values[i], scorerCount[i]));
        }
        dc.close();
        // print info
        int correct = getCorrectCount();
        int incorrect = getFalseCount();
        double error = getError();
        int nrRows = getNrRows();
        int missing = nrRows - correct - incorrect;
        LOGGER.info("error=" + error + ", #correct=" + correct + ", #false="
                + incorrect + ", #rows=" + nrRows + ", #missing=" + missing);
        // our view displays the table - we must keep a reference in the model.
        BufferedDataTable bufTable = dc.getTable();
        m_lastResult = bufTable;
        return new BufferedDataTable[]{bufTable};
    } // execute(DataTable[],ExecutionMonitor)

    /**
     * Resets all internal data.
     */
    @Override
    protected void reset() {
        m_correctCount = -1;
        m_falseCount = -1;
        m_lastResult = null;
    }

    /**
     * @return the result of the last run. Will be <code>null</code> before an
     *         execute and after a reset.
     */
    DataTable getScorerTable() {
        return m_lastResult;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (inSpecs[INPORT].getNumColumns() < 2) {
            throw new InvalidSettingsException(
                    "The input table must have at least two colums to compare");
        }
        if ((m_firstCompareColumn == null) || (m_secondCompareColumn == null)) {
            throw new InvalidSettingsException("No columns selected yet.");
        }
        if (!inSpecs[INPORT].containsName(m_firstCompareColumn)) {
            throw new InvalidSettingsException("Column " + m_firstCompareColumn
                    + " not found.");
        }
        if (!inSpecs[INPORT].containsName(m_secondCompareColumn)) {
            throw new InvalidSettingsException("Column "
                    + m_secondCompareColumn + " not found.");
        }
        return new DataTableSpec[1];
    }

    /**
     * Get the correct classification count, i.e. where both columns agree.
     * 
     * @return the count of rows where the two columns have an equal value or -1
     *         if the node is not executed
     */
    public int getCorrectCount() {
        return m_correctCount;
    }

    /**
     * Get the misclassification count, i.e. where both columns have different
     * values.
     * 
     * @return the count of rows where the two columns have an unequal value or
     *         -1 if the node is not executed
     */
    public int getFalseCount() {
        return m_falseCount;
    }

    /**
     * Get the number of rows in the input table. This count can be different
     * from {@link #getFalseCount()} + {@link #getCorrectCount()}, though it
     * must be at least the sum of both. The difference is the number of rows
     * containing a missing value in either of the target columns.
     * 
     * @return number of rows in input table
     */
    public int getNrRows() {
        return m_nrRows;
    }

    /**
     * Returns the error of wrong classified pattern in percentage of the number
     * of patterns.
     * 
     * @return the 1.0 - classification accuracy
     */
    public float getError() {
        float error;
        long totalNumberDataSets = getFalseCount() + getCorrectCount();
        if (totalNumberDataSets == 0) {
            error = Float.NaN;
        } else {
            error = 100.0f * getFalseCount() / totalNumberDataSets;
        }
        return error;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_firstCompareColumn = settings.getString(FIRST_COMP_ID);
        m_secondCompareColumn = settings.getString(SECOND_COMP_ID);
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
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        settings.getString(FIRST_COMP_ID);
        settings.getString(SECOND_COMP_ID);
    }

    /**
     * Called to determine all possible values in the respective columns. This
     * method will "stringify" all values as they show up as row and column key.
     * 
     * @param in the input table
     * @param index1 the first column to compare
     * @param index2 the second column to compare
     * @param exec object to check with if user canceled
     * @return the order of rows and columns in the confusion matrix
     * @throws CanceledExecutionException if user canceled operation
     */
    protected String[] determineColValues(final DataTable in, final int index1,
            final int index2, final ExecutionMonitor exec)
            throws CanceledExecutionException {

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
                exec.checkCanceled(); // throws excepton if user canceled.
                exec.setMessage("Reading possible values, row " + rowNr
                        + " (\"" + row.getKey() + "\")");
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
        LinkedHashSet<DataCell> intersection = new LinkedHashSet<DataCell>(
                values1);
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
        System.arraycopy(temp, 0, order, order.length - temp.length,
                temp.length);
        // make them a string
        String[] result = new String[order.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = order[i].toString();
        }
        return result;
    } // determineColValues(DataTable, int, int)

    /**
     * Finds the position where key is located in source. It must be ensured
     * that the key is indeed in the argument array.
     * 
     * @param source the source array
     * @param key the key to find
     * @return the index in source where key is located
     */
    protected static int findValue(final DataCell[] source, 
            final DataCell key) {
        for (int i = 0; i < source.length; i++) {
            if (source[i].equals(key)) {
                return i;
            }
        }
        throw new RuntimeException("You should never come here.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException {
        // load internal values
        File fSett = new File(internDir, FILE_NAME_INTERNAL_SETT);
        if (fSett.exists() && fSett.canRead()) {
            NodeSettingsRO sett = NodeSettings.loadFromXML(
                    new FileInputStream(fSett));
            try {
                m_correctCount = sett.getInt(CFG_CORRECT_COUNT);
                m_falseCount = sett.getInt(CFG_FALSE_COUNT);
                m_nrRows = sett.getInt(CFG_NUMBER_ROWS);
            } catch (InvalidSettingsException ise) {
                IOException ioe = new IOException();
                ioe.initCause(ise);
                throw ioe;
            }
        } else {
            throw new IOException("Could not load scorer internals. "
                    + "File does not exist or can't be read: " + fSett);
        }
        // load scored table
        File fTable = new File(internDir, FILE_NAME_INTERNAL_RESULT);
        m_lastResult = DataContainer.readFromZip(fTable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException {
        // save internal values
        NodeSettings sett = new NodeSettings("ignored");
        sett.addInt(CFG_CORRECT_COUNT, m_correctCount);
        sett.addInt(CFG_FALSE_COUNT, m_falseCount);
        sett.addInt(CFG_NUMBER_ROWS, m_nrRows);
        File fSett = new File(internDir, FILE_NAME_INTERNAL_SETT);
        sett.saveToXML(new FileOutputStream(fSett));
        // save scored table
        File fTable = new File(internDir, FILE_NAME_INTERNAL_RESULT);
        try {
            DataContainer.writeToZip(m_lastResult, fTable, exec);
        } catch (CanceledExecutionException cee) {
            IOException ioe = new IOException();
            ioe.initCause(cee);
            throw ioe;
        }
    }
}
