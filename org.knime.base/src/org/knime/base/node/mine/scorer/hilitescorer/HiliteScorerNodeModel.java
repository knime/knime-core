/*
 * --------------------------------------------------------------------- *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
package org.knime.base.node.mine.scorer.hilitescorer;

import java.awt.Point;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.knime.base.node.util.DataArray;
import org.knime.base.node.viz.plotter.DataProvider;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
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
import org.knime.core.node.property.hilite.HiLiteHandler;

/**
 * The hilite scorer node's model. The scoring is performed on two given columns
 * set by the dialog. The row keys are stored for later hiliting purpose.
 * 
 * @author Christoph Sieb, University of Konstanz
 * 
 * @see HiliteScorerNodeFactory
 */
public class HiliteScorerNodeModel extends NodeModel implements DataProvider {
    
    /** The node logger for this class. */
    protected static final NodeLogger LOGGER = NodeLogger
            .getLogger(HiliteScorerNodeModel.class);

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
     * Stores the row keys for the confusion matrix fields to allow hiliting.
     */
    private List<RowKey>[][] m_keyStore;

    /**
     * The confusion matrix as int 2-D array.
     */
    private int[][] m_scorerCount;

    /**
     * The attribute names of the confusion matrix.
     */
    private String[] m_values;

    /**
     * Number of rows in the input table. Interesting if you want to know the
     * number of missing values in either of the target columns.
     */
    private int m_nrRows;

    /** Inits a new <code>ScorerNodeModel</code> with one in- and one output. */
    HiliteScorerNodeModel() {
        super(1, 1);
    }

    /**
     * Starts the scoring in the scorer.
     * 
     * @param data
     *            the input data of length one
     * @param exec
     *            the execution monitor
     * @return the confusion matrix
     * @throws CanceledExecutionException
     *             if user canceled execution
     * 
     * @see NodeModel#execute(BufferedDataTable[],ExecutionContext)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] data,
            final ExecutionContext exec) throws CanceledExecutionException {
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
        DataCell[] values = determineColValues(in, index1, index2, 
                exec.createSubProgress(0.5));
        List<DataCell> valuesList = Arrays.asList(values);
        Set<DataCell> valuesInCol2 = new HashSet<DataCell>();

        m_correctCount = 0;
        m_falseCount = 0;

        // the key store remembers the row key for later hiliting
        m_keyStore = new List[values.length][values.length];
        // the scorerCount counts the confusions
        m_scorerCount = new int[values.length][values.length];

        // init the matrix
        for (int i = 0; i < m_keyStore.length; i++) {
            for (int j = 0; j < m_keyStore[i].length; j++) {
                m_keyStore[i][j] = new ArrayList<RowKey>();
            }
        }

        int rowCnt = in.getRowCount();
        int rowNr = 0;
        ExecutionMonitor subExec = exec.createSubProgress(0.5);
        for (Iterator<DataRow> it = in.iterator(); it.hasNext(); rowNr++) {
            DataRow row = it.next();
            subExec.setProgress((1.0 + rowNr)  / rowCnt,
                    "Computing score, row " + rowNr + " (\"" + row.getKey()
                            + "\") of " + in.getRowCount());
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
                continue;
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

        m_nrRows = rowNr;
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
        m_values = new String[values.length];
        for (int i = 0; i < m_values.length; i++) {
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
                m_values[i] = newName;
                if (!hasPrintedWarningOnAmbiguousValues) {
                    hasPrintedWarningOnAmbiguousValues = true;
                    setWarningMessage("Ambiguous value \"" + c.toString() 
                            + "\" encountered. Preserving individual instances;"
                            + " consider to convert input columns to string");
                }
            } else {
                int uniquifier = 1;
                String newName = s;
                while (!valuesAsStringSet.add(newName)) {
                    newName = s + "#" + (uniquifier++);
                }
                m_values[i] = newName;
            }
        }
        DataType[] colTypes = new DataType[m_values.length];
        Arrays.fill(colTypes, IntCell.TYPE);
        BufferedDataContainer container = 
            exec.createDataContainer(new DataTableSpec(m_values, colTypes));
        for (int i = 0; i < m_values.length; i++) {
            // need to make a datacell for the row key
            container.addRowToTable(new DefaultRow(m_values[i],
                    m_scorerCount[i]));
        }
        container.close();

        // print info
        int correct = getCorrectCount();
        int incorrect = getFalseCount();
        double error = getError();
        int nrRows = getNrRows();
        int missing = nrRows - correct - incorrect;
        LOGGER.info("error=" + error + ", #correct=" + correct + ", #false="
                + incorrect + ", #rows=" + nrRows + ", #missing=" + missing);
        // our view displays the table - we must keep a reference in the model.
        BufferedDataTable result = container.getTable();
        return new BufferedDataTable[] {result};

    } // execute(DataTable[],ExecutionMonitor)

    /**
     * Resets all internal data.
     */
    @Override
    protected void reset() {
        m_scorerCount = null;
        m_correctCount = -1;
        m_falseCount = -1;
        m_keyStore = null;
    }

    /**
     * Sets the columns that will be compared during execution.
     * 
     * @param first
     *            the first column
     * @param second
     *            the second column
     * @throws NullPointerException
     *             if one of the parameters is <code>null</code>
     */
    void setCompareColumn(final String first, final String second) {
        if ((first == null || second == null)) {
            throw new NullPointerException("Must specify both columns to"
                    + " compare!");
        }
        m_firstCompareColumn = first;
        m_secondCompareColumn = second;
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
        String col1 = settings.getString(FIRST_COMP_ID);
        String col2 = settings.getString(SECOND_COMP_ID);
        setCompareColumn(col1, col2);
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
     * Determines the row keys (as DataCells) which belong to the given cell of
     * the confusion matrix.
     * 
     * @param cells
     *            the cells of the confusion matrix for which the keys should be
     *            returned
     * 
     * @return a set of DataCells containing the row keys
     */
    Set<RowKey> getSelectedSet(final Point[] cells) {

        Set<RowKey> keySet = new HashSet<RowKey>();

        for (Point cell : cells) {

            int xVal = cell.x;
            int yVal = cell.y;

            // if the specified x and y values of the matrix are out of range
            // continue
            if (xVal < 0 || yVal < 0 || xVal > m_values.length
                    || yVal > m_values.length) {
                continue;
            }

            keySet.addAll(m_keyStore[xVal][yVal]);
        }

        return keySet;
    }

    /** Called to determine all possible values in the respective columns. 
     * 
     * @param in the input table
     * @param index1 the first column to compare
     * @param index2 the second column to compare
     * @param exec object to check with if user canceled
     * @return the order of rows and columns in the confusion matrix
     * @throws CanceledExecutionException
     *             if user canceled operation
     */
    protected DataCell[] determineColValues(final BufferedDataTable in, 
            final int index1, final int index2, final ExecutionMonitor exec)
            throws CanceledExecutionException {
        int rowCnt = in.getRowCount();
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
                        "Reading possible values, row " + rowNr
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
        return order;
    } 

    /**
     * Finds the position where key is located in source. It must be ensured
     * that the key is indeed in the argument array.
     * 
     * @param source
     *            the source array
     * @param key
     *            the key to find
     * @return the index in source where key is located
     */
    protected static int findValue(final DataCell[] source, 
            final DataCell key) {
        for (int i = 0; i < source.length; i++) {
            if (source[i].equals(key)) {
                return i;
            }
        }
        throw new RuntimeException("Array does not contain desired value \"" 
                + key + "\".");
    }

    /**
     * Checks if the specified confusion matrix cell contains at least one of
     * the given keys.
     * 
     * @param x
     *            the x value to specify the matrix cell
     * @param y
     *            the y value to specify the matrix cell
     * @param keys
     *            the keys to check
     * 
     * @return true if at least one key is contained in the specified cell
     */
    boolean containsConfusionMatrixKeys(final int x, final int y,
            final Set<RowKey> keys) {

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
     * Returns all cells of the confusion matrix (as Points) if the given key
     * set contains all keys of that cell.
     * 
     * @param keys
     *            the keys to check for
     * 
     * @return the cells that fullfill the above condition
     */
    Point[] getCompleteHilitedCells(final Set<RowKey> keys) {

        Vector<Point> result = new Vector<Point>();

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
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException {
        File f = new File(internDir, "internals.xml.gz");
        InputStream in = new GZIPInputStream(new BufferedInputStream(
                new FileInputStream(f)));
        try {
            NodeSettingsRO set = NodeSettings.loadFromXML(in);
            m_correctCount = set.getInt("correctCount");
            m_falseCount = set.getInt("falseCount");
            m_nrRows = set.getInt("nrRows");
            m_values = set.getStringArray("values");
            m_scorerCount = new int[m_values.length][];
            m_keyStore = new List[m_values.length][m_values.length];
            for (int i = 0; i < m_values.length; i++) {
                NodeSettingsRO sub = set.getNodeSettings(m_values[i]);
                m_scorerCount[i] = sub.getIntArray("scorerCount");
                NodeSettingsRO subSub = sub.getNodeSettings("hilightMap");
                for (int j = 0; j < m_values.length; j++) {
                    NodeSettingsRO sub3 = subSub.getNodeSettings(m_values[j]);
                    m_keyStore[i][j] = new ArrayList<RowKey>();
                    try {
                        // load key stores before 2.0
                        DataCell[] cells = sub3.getDataCellArray("keyStore");
                        for (DataCell dc : cells) {
                            m_keyStore[i][j].add(new RowKey(dc.toString()));
                        }
                    } catch (InvalidSettingsException ise) {
                        m_keyStore[i][j] = Arrays.asList(
                                sub3.getRowKeyArray("keyStore"));
                    }    
                }
            }
        } catch (InvalidSettingsException ise) {
            IOException ioe = new IOException("Unable to read internals");
            ioe.initCause(ise);
            throw ioe;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException {
        NodeSettings set = new NodeSettings("scorer");
        set.addInt("correctCount", m_correctCount);
        set.addInt("falseCount", m_falseCount);
        set.addInt("nrRows", m_nrRows);
        set.addStringArray("values", m_values);
        for (int i = 0; i < m_values.length; i++) {
            NodeSettingsWO sub = set.addNodeSettings(m_values[i]);
            sub.addIntArray("scorerCount", m_scorerCount[i]);
            NodeSettingsWO subSub = sub.addNodeSettings("hilightMap");
            for (int j = 0; j < m_values.length; j++) {
                NodeSettingsWO sub3 = subSub.addNodeSettings(m_values[j]);
                RowKey[] rowKeys = m_keyStore[i][j]
                        .toArray(new RowKey[m_keyStore[i][j].size()]);
                sub3.addRowKeyArray("keyStore", rowKeys);
            }
        }

        set.saveToXML(new GZIPOutputStream(new BufferedOutputStream(
                        new FileOutputStream(new File(internDir,
                                "internals.xml.gz")))));
    }

    /**
     * @return the confusion matrix as int 2-D array
     */
    int[][] getScorerCount() {
        return m_scorerCount;
    }

    /**
     * Returns a bit set with data for the ROC curve. A set bit means a correct
     * classified example, an unset bit is a wrong classified example. The
     * number of interesting bits is {@link BitSet#length()} - 1, i.e. the last
     * set bit must be ignored, it is just the end marker.
     * 
     * @return a bit set
     */
    BitSet getRocCurve() {
        // disabled
        return new BitSet();
    }

    /**
     * @return the attribute names of the confusion matrix
     */
    String[] getValues() {
        return m_values;
    }

    /**
     * {@inheritDoc}
     */
    public DataArray getDataArray(final int index) {
        // only to make the Plotter happy
        return null;
    }

    /**
     * Returns the first column to compare.
     * 
     * @return the first column to compare
     */
    public String getFirstCompareColumn() {
        return m_firstCompareColumn;
    }

    /**
     * Returns the second column to compare.
     * 
     * @return the second column to compare
     */
    public String getSecondCompareColumn() {
        return m_secondCompareColumn;
    }
    
    /** Output hilite handler for confusion matrix. */
    private final HiLiteHandler m_outHiLiteHandler = new HiLiteHandler();

    /** {@inheritDoc} */
    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        return m_outHiLiteHandler;
    }
}
