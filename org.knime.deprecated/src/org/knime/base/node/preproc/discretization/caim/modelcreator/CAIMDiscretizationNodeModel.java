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
package org.knime.base.node.preproc.discretization.caim.modelcreator;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.knime.base.data.sort.SortedTable;
import org.knime.base.node.preproc.discretization.caim.DiscretizationModel;
import org.knime.base.node.preproc.discretization.caim.DiscretizationScheme;
import org.knime.base.node.preproc.discretization.caim.Interval;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Implements the CAIM discretization algorithm. The algorithm is based on the
 * publication of Kurgan and Cios (2004) and performs a discretization based on
 * a Class-Interval interdependance measure. The algorithm therefore
 * incorporates the class information (supervised discretization)
 * 
 * @author Christoph Sieb, University of Konstanz
 * 
 * @see CAIMDiscretizationNodeFactory
 */
public class CAIMDiscretizationNodeModel extends NodeModel {

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(CAIMDiscretizationNodeModel.class);

    private static final String SAVE_INTERNALS_FILE_NAME = "Binning.model";

    /**
     * Key to store the included columns settings. (Columns to perform the
     * discretization on)
     */
    public static final String INCLUDED_COLUMNS_KEY = "includedColumns";

    /**
     * Key to store the included columns settings. (Columns to perform the
     * discretization on)
     */
    public static final String CLASS_COLUMN_KEY = "classColumn";

    /**
     * Key to store the whether to sort in memory or not.
     */
    public static final String SORT_IN_MEMORY_KEY = "sortInMemory";

    /**
     * Key to store whether the class optimized version should be applied.
     */
    public static final String USE_CLASS_OPTIMIZATION = "classOptimized";

    /** Index of input data port. */
    public static final int DATA_INPORT = 0;

    /** Index of data out port. */
    public static final int DATA_OUTPORT = 0;

    private static final String WARNING_NO_COLS_SELECTED =
            "No columns selected for binning. Output table will be the same.";

    /**
     * The column which contains the classification Information.
     */
    private String m_classColumnName;

    /**
     * The index of the class column.
     */
    private int m_classifyColumnIndex;

    /**
     * The columns included in the discretization.
     */
    private String[] m_includedColumnNames;

    /**
     * Maps the class values to an integer for faster counting of frequencies.
     */
    private HashMap<String, Integer> m_classValueToIndexMap =
            new HashMap<String, Integer>();

    /**
     * Maps the integer index to the class values.
     */
    private HashMap<Integer, String> m_indexToClassValueMap =
            new HashMap<Integer, String>();

    /**
     * String array of class values to be put into the frequency structures.
     */
    private String[] m_classValues;

    /**
     * The learned discretization model for the included columns.
     */
    private DiscretizationModel m_discretizationModel;

    /**
     * Whether to sort the columns in memory or partially in memory only.
     */
    private boolean m_sortInMemory = true;

    /**
     * Whether to use the class optimized version. The class optimized version
     * creates candidate boundaries only at positions where class values chages.
     * At ohter positions boundary checks are not necessary.
     */
    private boolean m_classOptimizedVersion;

    /**
     * If true, the candidate boundaries are only created if the column value
     * and the class value changes.
     */
    private boolean m_reducedBoundaries = false;

    /**
     * Inits a new CAIM model with one data in- and one data output port.
     */
    public CAIMDiscretizationNodeModel() {
        super(1, 1, 0, 1);
        reset();
    }

    private void createClassFromToIndexMaps(final DataTableSpec tableSpec) {
        Set<DataCell> classValues =
                tableSpec.getColumnSpec(m_classColumnName).getDomain()
                        .getValues();

        m_classValues = new String[classValues.size()];

        int index = 0;
        for (DataCell classCell : classValues) {

            m_classValues[index] = ((StringValue)classCell).getStringValue();

            m_classValueToIndexMap.put(m_classValues[index], index);

            m_indexToClassValueMap.put(index, m_classValues[index]);

            index++;
        }
    }

    /**
     * Starts the diescretization.
     * 
     * @param data the input data tables
     * @param exec the execution context for this node
     * @see NodeModel#execute(BufferedDataTable[],ExecutionContext)
     * @throws CanceledExecutionException If canceled.
     * @throws Exception if something else goes wrong.
     * @return the result table with the discretized values
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] data,
            final ExecutionContext exec) throws CanceledExecutionException,
            Exception {

        // measure the time
        long startTime = System.currentTimeMillis();

        // if nothing to discretize return the original data and create an
        // empty model
        if (m_includedColumnNames == null 
                || m_includedColumnNames.length == 0) {
            m_discretizationModel =
                    new DiscretizationModel(new String[0],
                            new DiscretizationScheme[0]);
            return data;
        }
        LOGGER.debug("Start discretizing.");

        // as the algorithm is for binary class problems only
        // (positive, negative) the algorithm is performed for each class value
        // labeled as positive class and the rest as negative
        exec.setProgress(0.0, "Preparing...");
        // check input data
        assert (data != null && data.length == 1 && data[DATA_INPORT] != null);

        BufferedDataTable inData = data[0];

        // get class column index
        m_classifyColumnIndex =
                inData.getDataTableSpec().findColumnIndex(m_classColumnName);

        assert m_classifyColumnIndex > -1;

        // create the class - index mapping
        createClassFromToIndexMaps(inData.getDataTableSpec());

        // create the array with the result discretization schemes for
        // each included column
        DiscretizationScheme[] resultSchemes =
                new DiscretizationScheme[m_includedColumnNames.length];

        // for all included columns do the discretization
        int currentColumn = 0;
        for (String includedColumnName : m_includedColumnNames) {

            LOGGER.debug("Process column: " + includedColumnName);
            exec.setProgress("Discretizing column '" + includedColumnName
                    + "'");
            ExecutionContext subExecPerColumn =
                    exec
                            .createSubExecutionContext(1.0D / (
                                    double)m_includedColumnNames.length);
            subExecPerColumn.checkCanceled();
            // never discretize the column index (should never happen)
            if (m_classColumnName.equals(includedColumnName)) {
                continue;
            }

            // determine the column index of the current column
            int columnIndex =
                    inData.getDataTableSpec().findColumnIndex(
                            includedColumnName);

            DataColumnDomain domain =
                    inData.getDataTableSpec().getColumnSpec(columnIndex)
                            .getDomain();

            double minValue =
                    ((DoubleValue)domain.getLowerBound()).getDoubleValue();
            double maxValue =
                    ((DoubleValue)domain.getUpperBound()).getDoubleValue();

            // find all distinct values of the column and create
            // a table with all possible interval boundaries (midpoint value of
            // adjacent values)
            subExecPerColumn.setProgress("Find possible boundaries.");
            BoundaryScheme boundaryScheme = null;
            
            // create subExec for sorting
            ExecutionContext subExecSort = subExecPerColumn
                    .createSubExecutionContext(0.1);

            // long t1 = System.currentTimeMillis();
            if (m_classOptimizedVersion) {
                boundaryScheme =
                        createAllIntervalBoundaries(inData, columnIndex,
                                subExecSort);
            } else {
                boundaryScheme =
                        createAllIntervalBoundaries2(inData, columnIndex,
                                subExecSort);
            }
            
            subExecSort.setProgress(1.0D);
            // long t2 = System.currentTimeMillis() - t1;
            // LOGGER.error("Create boundaries time: " + (t2 / 1000.0)
            // + " optimized: " + m_classOptimizedVersion);
            // LOGGER.error("Boundaries: " + boundaryScheme.getHead());

            LinkedDouble allIntervalBoundaries = boundaryScheme.getHead();

            // create the initial discretization scheme
            DiscretizationScheme discretizationScheme =
                    new DiscretizationScheme(new Interval(minValue, maxValue,
                            true, true));

            double globalCAIM = 0;

            // performe the iterative search for the best intervals
            int numInsertedBounds = 0;
            double currentCAIM = 0;
            // create subExec for inserted bounds
            ExecutionContext subExecBounds = subExecPerColumn
                    .createSubExecutionContext(0.9);
            while (currentCAIM > globalCAIM
                    || numInsertedBounds < m_classValues.length) {
                subExecPerColumn.checkCanceled();
                
                // create subExec for counting
                ExecutionContext subExecCount = subExecBounds
                        .createSubExecutionContext(1.0D
                                / m_classValues.length);

                // LOGGER.debug("Inserted bounds: " + numInsertedBounds);
                // LOGGER.debug("intervall boundaries: " +
                // allIntervalBoundaries);

                // for all possible interval boundaries
                // insert each one, calculate the caim value and add
                // the one with the biggest caim
                LinkedDouble intervalBoundary = allIntervalBoundaries.m_next;
                currentCAIM = 0;
                LinkedDouble bestBoundary = null;
                
                long currentCountedBoundaries = 0;
                while (intervalBoundary != null) {
                    subExecPerColumn.checkCanceled();
                    
                    // set progress
                    currentCountedBoundaries++;
                    subExecCount.setProgress((double) currentCountedBoundaries
                            / (double) boundaryScheme.getNumBoundaries(),
                            "Count for possible boundary "
                                    + currentCountedBoundaries + " of "
                                    + boundaryScheme.getNumBoundaries());

                    // LOGGER.debug("current caim: " + currentCAIM);
                    DiscretizationScheme tentativeDS =
                            new DiscretizationScheme(discretizationScheme);
                    tentativeDS.insertBound(intervalBoundary.m_value);

                    // create the quanta matrix
                    QuantaMatrix2D quantaMatrix =
                            new QuantaMatrix2D(tentativeDS,
                                    m_classValueToIndexMap);
                    // pass the data for filling the matrix
                    quantaMatrix.countData(inData, columnIndex,
                            m_classifyColumnIndex);
                    // calculate the caim
                    double caim = quantaMatrix.calculateCaim();

                    if (caim > currentCAIM) {
                        currentCAIM = caim;
                        bestBoundary = intervalBoundary;
                    }
                    intervalBoundary = intervalBoundary.m_next;
                }

                // if there is no best boundary, break the first while loop
                if (bestBoundary == null) {
                    break;
                }

                // LOGGER.debug("Best boundary: " + bestBoundary.m_value);
                // remove the best boundary

                // check if the caim is high enough or the number intervals
                // is smaller than the number of class values
                // in this case accept the best discretization scheme
                if (currentCAIM > globalCAIM
                        || numInsertedBounds < m_classValues.length) {

                    int numIntervals = discretizationScheme.getNumIntervals();
                    discretizationScheme.insertBound(bestBoundary.m_value);

                    // remove the linked list element from the list
                    bestBoundary.remove();
                    globalCAIM = currentCAIM;

                    if (numIntervals < discretizationScheme.getNumIntervals()) {
                        numInsertedBounds++;

                        subExecPerColumn.setProgress("Inserted bound "
                                + numInsertedBounds);

                        // LOGGER.debug("Inserted boundary: "
                        // + bestBoundary.m_value);
                    } else {
                        throw new IllegalStateException(
                                "Only usefull bounds should be inserted: "
                                        + bestBoundary.m_value);
                    }
                }
                
                subExecCount.setProgress(1.0D);
            }

            resultSchemes[currentColumn] = discretizationScheme;

            subExecBounds.setProgress(1.0D);
            // ensure the full progress is set for this iteration
            subExecPerColumn.setProgress(1.0D);

            currentColumn++;

        }

        // set the model
        m_discretizationModel =
                new DiscretizationModel(m_includedColumnNames, resultSchemes);

        // create an output table that replaces the included columns by
        // interval values
        BufferedDataTable resultTable =
                createResultTable(exec, inData, m_discretizationModel);

        // log the runtime of the execute method
        long runtime = System.currentTimeMillis() - startTime;

        LOGGER.debug("Binning runtime: " + (runtime / 1000.0) + " sec.");

        return new BufferedDataTable[]{resultTable};
    }

    /**
     * Filters out all schemes for which the corresponding column is not part of
     * the given table spec.
     * 
     * @param schemes the scheme to filter
     * @param schemeColumnNames the column names for the given schemes
     * @param spec the spec used as filter criteria
     * @return filtered discretization schemes only containing schemes for which
     *         the corresponding column is contained in the given spec
     */
    static DiscretizationScheme[] filterNotKnownSchemes(
            final DiscretizationScheme[] schemes,
            final String[] schemeColumnNames, final DataTableSpec spec) {

        Vector<DiscretizationScheme> filteredSchemes =
                new Vector<DiscretizationScheme>();
        for (DataColumnSpec columnSpec : spec) {

            int index = isIncluded(columnSpec, schemeColumnNames);
            if (index > -1) {
                filteredSchemes.add(schemes[index]);
            }
        }

        return filteredSchemes.toArray(new DiscretizationScheme[filteredSchemes
                .size()]);
    }

    /**
     * Creates {@link BufferedDataTable} from a given input table and an
     * appropriate {@link DiscretizationScheme}. The result table has replaced
     * columns according to the {@link DiscretizationScheme}.
     * 
     * @param exec the context from which to create the
     *            {@link BufferedDataTable}
     * @param table the input data table
     * @param discretizationModel the {@link DiscretizationModel} that contains
     *            the mapping from numerical intervals to nominal String values
     *            for the included columns
     * @return the discretized input data
     */
    public static BufferedDataTable createResultTable(
            final ExecutionContext exec, final BufferedDataTable table,
            final DiscretizationModel discretizationModel) {

        DiscretizationScheme[] dSchemes = discretizationModel.getSchemes();

        final String[] includedColumnNames =
                discretizationModel.getIncludedColumnNames();

        // filter the schemes so that only schemes for columns are included
        // which are also included in the table
        dSchemes =
                filterNotKnownSchemes(dSchemes, includedColumnNames, table
                        .getDataTableSpec());

        DataTableSpec originalTableSpec = table.getDataTableSpec();

        DataColumnSpec[] newColumnSpecs =
                new DataColumnSpec[originalTableSpec.getNumColumns()];

        // remembers if an column index is included or not
        boolean[] included = new boolean[newColumnSpecs.length];
        int counter = 0;
        for (DataColumnSpec originalColumnSpec : originalTableSpec) {

            // if the column is included for discretizing, change the spec
            if (isIncluded(originalColumnSpec, includedColumnNames) > -1) {
                // creat a nominal string column spec
                newColumnSpecs[counter] =
                        new DataColumnSpecCreator(originalColumnSpec.getName(),
                                StringCell.TYPE).createSpec();
                included[counter] = true;
            } else {
                // add it as is
                newColumnSpecs[counter] = originalColumnSpec;
                included[counter] = false;
            }

            counter++;
        }

        // create the new table spec
        DataTableSpec newTableSpec = new DataTableSpec(newColumnSpecs);

        // create the result table
        BufferedDataContainer container =
                exec.createDataContainer(newTableSpec);

        // discretize the included column values
        double rowCounter = 0;
        double numRows = (double)table.getRowCount();
        for (DataRow row : table) {

            if (rowCounter % 200 == 0) {
                exec.setProgress(rowCounter / numRows);
            }
            int i = 0;
            DataCell[] newCells = new DataCell[row.getNumCells()];
            int includedCounter = 0;
            for (DataCell cell : row) {

                if (included[i]) {
                    // check for missing values
                    if (cell.isMissing()) {
                        newCells[i] = cell;
                    } else {

                        // transform the value to the discretized one
                        double value = ((DoubleValue)cell).getDoubleValue();

                        String discreteValue =
                                dSchemes[includedCounter]
                                        .getDiscreteValue(value);

                        newCells[i] = new StringCell(discreteValue);
                    }
                    includedCounter++;

                } else {
                    newCells[i] = cell;
                }
                i++;
            }
            container.addRowToTable(new DefaultRow(row.getKey(), newCells));
            rowCounter++;

        }

        container.close();
        return container.getTable();
    }

    /**
     * Returns the index of the column name within the
     * <code>includedColumnNames</code> array and -1 if the column is not
     * included int the array.
     * 
     * @param columnSpec the column spec to check
     * @return the index of the column name within the
     *         <code>includedColumnNames</code> array
     */
    private static int isIncluded(final DataColumnSpec columnSpec,
            final String[] includedColumnNames) {
        int i = 0;
        for (String inludedColumn : includedColumnNames) {
            if (inludedColumn.equals(columnSpec.getName())) {
                return i;
            }
            i++;
        }

        return -1;
    }

    /**
     * Sorts the data table in ascending order on the given column, then all
     * distinct values are determined and finally a new table is created that
     * holds the minimum, the maximum value and the midpoints of all adjacent
     * values. These represent all possible boundaries.
     * 
     * @param table the table with the data
     * @param columnIndex the column of interest
     * @param exec the execution context to set the progress
     */
    private BoundaryScheme createAllIntervalBoundaries2(
            final BufferedDataTable table, final int columnIndex,
            final ExecutionContext exec) throws Exception {

        // sort the data accordint to the column index
        List<String> sortColumn = new ArrayList<String>();
        sortColumn.add(table.getDataTableSpec().getColumnSpec(columnIndex)
                .getName());
        boolean[] sortOrder = new boolean[1];

        // in ascending order
        sortOrder[0] = true;

        SortedTable sortedTable =
                new SortedTable(table, sortColumn, sortOrder, true, exec);

        // the first different value is the minimum value of the sorted list
        RowIterator rowIterator = sortedTable.iterator();
        double lastDifferentValue =
                ((DoubleValue)rowIterator.next().getCell(columnIndex))
                        .getDoubleValue();

        // create the head of the linked double list
        // marked by NaN
        LinkedDouble head = new LinkedDouble(Double.NEGATIVE_INFINITY);

        // set the last added element
        LinkedDouble lastAdded = head;

        // count the number of boundaries
        int numBoundaries = 0;

        while (rowIterator.hasNext()) {

            DataRow row = rowIterator.next();
            DataCell cell = row.getCell(columnIndex);
            double value = ((DoubleValue)cell).getDoubleValue();

            if (value != lastDifferentValue) {

                // a new boundary is the midpoint
                double newBoundary = (value + lastDifferentValue) / 2.0D;
                lastDifferentValue = value;

                // add the new midpoint boundary to the linked list
                lastAdded.m_next = new LinkedDouble(newBoundary);
                numBoundaries++;
                lastAdded.m_next.m_previous = lastAdded;
                lastAdded = lastAdded.m_next;
            }
        }

        return new BoundaryScheme(head, numBoundaries);
    }

    /**
     * Sorts the data table in ascending order on the given column, then all
     * distinct values are determined and finally a new table is created that
     * holds the minimum, the maximum value and the midpoints of all adjacent
     * values. These represent all possible boundaries.
     * 
     * @param table the table with the data
     * @param columnIndex the column of interest
     * @param exec the execution context to set the progress
     */
    private BoundaryScheme createAllIntervalBoundaries(
            final BufferedDataTable table, final int columnIndex,
            final ExecutionContext exec) throws Exception {

        // sort the data accordint to the column index
        List<String> sortColumn = new ArrayList<String>();
        sortColumn.add(table.getDataTableSpec().getColumnSpec(columnIndex)
                .getName());

        // if reduced boundary policy is switched on we additionally sort
        // according to the class column
        if (m_reducedBoundaries) {
            sortColumn.add(m_classColumnName);
        }

        // in ascending order
        // in case the class column is not used as second sort criteria
        // the sort order of field 2 is ignored
        boolean[] sortOrder = new boolean[2];
        sortOrder[0] = true;
        sortOrder[1] = true;

        SortedTable sortedTable =
                new SortedTable(table, sortColumn, sortOrder, m_sortInMemory,
                        exec);

        // the first different value is the minimum value of the sorted list
        RowIterator rowIterator = sortedTable.iterator();

        // get the first valid value (non-missing
        double lastDifferentValue = Double.NaN;
        String firstClassValueOfCurrentValue = null;
        while (rowIterator.hasNext()) {
            DataRow firstRow = rowIterator.next();

            if (!firstRow.getCell(columnIndex).isMissing()) {
                lastDifferentValue =
                        ((DoubleValue)firstRow.getCell(columnIndex))
                                .getDoubleValue();

                // also remember the corresponding class value
                firstClassValueOfCurrentValue =
                        firstRow.getCell(m_classifyColumnIndex).toString();
                break;
            }
        }

        // needed to create a already passed candidate boundary due
        // to a class value change
        double lastChangeValueWithoutNewBoundary = Double.NaN;

        // create the head of the linked double list
        // marked by NaN
        LinkedDouble head = new LinkedDouble(Double.NEGATIVE_INFINITY);

        // set the last added element
        LinkedDouble lastAdded = head;

        // count the number of boundaries
        int numBoundaries = 0;

        // to determine if the class has changed during a single value sequence
        boolean hasClassChanged = false;
        while (rowIterator.hasNext()) {

            DataRow row = rowIterator.next();
            DataCell cell = row.getCell(columnIndex);
            double value = ((DoubleValue)cell).getDoubleValue();

            String classValue = row.getCell(m_classifyColumnIndex).toString();
            if (!hasClassChanged
                    && !firstClassValueOfCurrentValue.equals(classValue)) {
                hasClassChanged = true;

                // if the class changes and also the value has changed
                // reset the remembered value where no boundary was inserted
                // i.e. this value is not necessary any more
                if (value != lastDifferentValue) {
                    lastChangeValueWithoutNewBoundary = Double.NaN;
                }
            }

            // as long as the values do not change no boundary is added
            if (value != lastDifferentValue) {

                // only add a new boundary, if the class values have changed
                // since the last value change
                if (hasClassChanged) {

                    // if the last boundary was not added due to a same
                    // class value, this boundary must now be inserted
                    // if the class value has changed since this time
                    if (!Double.isNaN(lastChangeValueWithoutNewBoundary)) {
                        // a new boundary is the midpoint
                        double newBoundary =
                                (lastDifferentValue 
                                        + lastChangeValueWithoutNewBoundary) 
                                        /  2.0D;

                        // add the new midpoint boundary to the linked list
                        lastAdded.m_next = new LinkedDouble(newBoundary);
                        numBoundaries++;
                        lastAdded.m_next.m_previous = lastAdded;
                        lastAdded = lastAdded.m_next;
                    }

                    // a new boundary is the midpoint
                    double newBoundary = (value + lastDifferentValue) / 2.0D;

                    // add the new midpoint boundary to the linked list
                    lastAdded.m_next = new LinkedDouble(newBoundary);
                    numBoundaries++;
                    lastAdded.m_next.m_previous = lastAdded;
                    lastAdded = lastAdded.m_next;

                    // reset the value
                    lastChangeValueWithoutNewBoundary = Double.NaN;
                } else {
                    lastChangeValueWithoutNewBoundary = lastDifferentValue;
                }

                // remember the value change
                lastDifferentValue = value;

                // remember the first class value of this first value
                firstClassValueOfCurrentValue = classValue;

                // reset the hasClassChanged value
                hasClassChanged = false;
            }
        }

        return new BoundaryScheme(head, numBoundaries);
    }

    /**
     * Resets all internal data.
     */
    @Override
    protected void reset() {

        // nothing to do yet
    }

    /**
     * The number of the class columns must be > 0 and < number of input
     * columns. Also create the output table spec replacing the columns to
     * discretize to nominal String values.
     * 
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        // if no columns are defined to discretize, return the input spec
        if (m_includedColumnNames == null 
                || m_includedColumnNames.length == 0) {
            setWarningMessage(WARNING_NO_COLS_SELECTED);
            return inSpecs;
        }
        // first check if the in specs correspond to the settings
        // i.e. check if the selected columns for binning are
        // contained in the in data table
        for (String includedColName : m_includedColumnNames) {
            if (!inSpecs[DATA_INPORT].containsName(includedColName)) {
                throw new InvalidSettingsException(
                        "The selected column to bin '" + includedColName
                                + "' does not exist in the input data "
                                + "table. Reconfigure this node!");
            }

        }

        // else replace for each included column the attribute type to
        // string
        DataColumnSpec[] newColumnSpecs = 
            new DataColumnSpec[inSpecs[DATA_INPORT]
                .getNumColumns()];

        int counter = 0;
        for (DataColumnSpec originalColumnSpec : inSpecs[DATA_INPORT]) {

            // if the column is included for discretizing, change the spec
            if (isIncluded(originalColumnSpec, m_includedColumnNames) > -1) {
                // creat a nominal string column spec
                newColumnSpecs[counter] = new DataColumnSpecCreator(
                        originalColumnSpec.getName(), StringCell.TYPE)
                        .createSpec();
            } else {
                // add it as is
                newColumnSpecs[counter] = originalColumnSpec;
            }

            counter++;
        }

        DataTableSpec[] newSpecs = new DataTableSpec[1];
        newSpecs[0] = new DataTableSpec(newColumnSpecs);
        return newSpecs;

    }

    /**
     * Loads the class column and the classification value in the model.
     * 
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {

        m_includedColumnNames = settings.getStringArray(INCLUDED_COLUMNS_KEY);

        m_classColumnName = settings.getString(CLASS_COLUMN_KEY);

        m_sortInMemory = settings.getBoolean(SORT_IN_MEMORY_KEY);

        m_classOptimizedVersion = true;
        // m_classOptimizedVersion =
        // settings.getBoolean(USE_CLASS_OPTIMIZATION);

    }

    /**
     * Saves the class column and the classification value in the settings.
     * 
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        assert (settings != null);
        settings.addString(CLASS_COLUMN_KEY, m_classColumnName);
        settings.addStringArray(INCLUDED_COLUMNS_KEY, m_includedColumnNames);

        settings.addBoolean(USE_CLASS_OPTIMIZATION, m_classOptimizedVersion);
        settings.addBoolean(SORT_IN_MEMORY_KEY, m_sortInMemory);
    }

    /**
     * This method validates the settings. That is:
     * <ul>
     * <li>The number of the class column must be an integer > 0</li>
     * <li>The positive value <code>DataCell</code> must not be null</li>
     * </ul>
     * 
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        String classifyColumn = settings.getString(CLASS_COLUMN_KEY);
        if (classifyColumn == null || classifyColumn.equals("")) {
            throw new InvalidSettingsException(
                    "Discretization column not set.");
        }

        String[] includedColumnNames =
                settings.getStringArray(INCLUDED_COLUMNS_KEY);
        if (includedColumnNames == null || includedColumnNames.length == 0) {

            setWarningMessage(WARNING_NO_COLS_SELECTED);
        }

    }

    /**
     * Saves the {@link DiscretizationModel} to a config object at the model
     * outport.
     * 
     * {@inheritDoc}
     */
    @Override
    protected void saveModelContent(final int index,
            final ModelContentWO predParams) throws InvalidSettingsException {

        if (m_discretizationModel != null) {
            m_discretizationModel.saveToModelContent(predParams);
        }
    }

    /**
     * Returns the discretization node model of this node.
     * 
     * @return the discretization node model of this node
     */
    public DiscretizationModel getDiscretizationModel() {
        return m_discretizationModel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

        File internalsFile = new File(nodeInternDir, SAVE_INTERNALS_FILE_NAME);
        if (!internalsFile.exists()) {
            // file to load internals from not available
            throw new IOException("Internal model could not be loaded, file \"" 
                + internalsFile.getAbsoluteFile() + "\" does not exist.");
        }

        BufferedInputStream in =
                new BufferedInputStream(new GZIPInputStream(
                        new FileInputStream(internalsFile)));

        ModelContentRO binModel = ModelContent.loadFromXML(in);

        try {
            m_discretizationModel = new DiscretizationModel(binModel);
        } catch (InvalidSettingsException ise) {
            throw new IOException("Internal model could not be loaded.", ise);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

        ModelContent binModel = new ModelContent(SAVE_INTERNALS_FILE_NAME);
        m_discretizationModel.saveToModelContent(binModel);

        File internalsFile = new File(nodeInternDir, SAVE_INTERNALS_FILE_NAME);
        BufferedOutputStream out =
                new BufferedOutputStream(new GZIPOutputStream(
                        new FileOutputStream(internalsFile)));

        binModel.saveToXML(out);
    }
}
