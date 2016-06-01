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
 * ---------------------------------------------------------------------
 *
 * Created on 2013.06.09. by Gabor
 */
package org.knime.base.data.statistics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.util.memory.MemoryAlertSystem;
import org.knime.core.data.util.memory.MemoryAlertSystem.MemoryActionIndicator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;

/**
 * Class to find the median/kth element of a table.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @author Gabor Bakos
 *
 * @since 2.8
 * @param <C> The {@link DataContainer} type.
 * @param <T> The result and input table type.
 */
abstract class SelectRank<C extends DataContainer, T extends DataTable> {

    /**
     *
     */
    private static final DataTableSpec SINGLE_DATA_TABLE_SPEC = new DataTableSpec(new String[]{"x"},
        new DataType[]{DoubleCell.TYPE});

    /**
     * Below this number of rows the selected m_value is computed by sorting them in memory.
     */
    private static final int MAX_CELLS_SORTED_IN_MEMORY = 5000000;

    private static final NodeLogger LOGGER = NodeLogger.getLogger(SelectRank.class);

    /**
     * Default memory threshold. If this relative amount of memory is filled, the chunk is sorted and flushed to disk.
     */
    public static final double DEF_MEM_THRESHOLD = 0.8;

    /**
     * The maximum number of open containers. See {@link #setMaxOpenContainers(int)} for details.
     */
    public static final int DEF_MAX_OPENCONTAINER = 40;

    private final T m_inputTable;

    private final long m_rowsInInputTable;

    /**
     * The maximal number of open containers. This has an effect when many containers must be merged.
     */
    private int m_maxOpenContainers = DEF_MAX_OPENCONTAINER;

    private boolean m_sortInMemory = false;

    /** The indices of columns in the original table. */
    private int[] m_indices;

    /**
     * We need these values for each column. The first dimension specifies the index-index, the second the index of
     * column in {@link #m_indices}. It is ascending in their first index within each second-index.
     */
    private int[][] m_k;

    /**
     * Private constructor. Assigns input table, checks argument.
     *
     * @param inputTable Table to sort.
     * @param rowsCount The number of rows in the table
     * @throws NullPointerException If arg is null.
     */
    private SelectRank(final T inputTable, final long rowsCount) {
        if (inputTable == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        // for BDT better use the appropriate derived class (blob handling)
        if (getClass().equals(SelectRank.class) && inputTable instanceof BufferedDataTable) {
            LOGGER.coding("Do not use a " + SelectRank.class.getSimpleName() + " to select from" + " a "
                + BufferedDataTable.class.getSimpleName() + " but use a " + BufferedSelectRank.class.getSimpleName());
        }
        m_inputTable = inputTable;
        m_rowsInInputTable = rowsCount;
    }

    /**
     * Inits table sorter using the sorting according to {@link #setSelectRank(Collection, int[][])}.
     *
     * @param inputTable The table to sort
     * @param rowsCount The number of rows in the table
     * @param inclList Passed on to {@link #setSelectRank(Collection, int[][])}.
     * @param k We need these values for each column. The first dimension specifies the index-index, the second the
     *            index of column in order of {@code inclList}. It is ascending in their first index within each
     *            second-index.
     * @throws NullPointerException If any argument is null.
     * @throws IllegalArgumentException If arguments are inconsistent.
     */
    public SelectRank(final T inputTable, final long rowsCount, final Collection<String> inclList, final int[][] k) {
        this(inputTable, rowsCount);
        setSelectRank(inclList, k);
    }

    /** @param indices The selected column indices to set. */
    public void setIndices(final int[] indices) {
        if (indices == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        this.m_indices = indices.clone();
    }

    /**
     * Sets sorting columns and order.
     *
     * @param inclList the list with the columns to sort; the first column name represents the first sort criteria, the
     *            second the second criteria and so on.
     * @param k We need these values for each column. The first dimension specifies the index-index, the second the
     *            index of column in order of {@code inclList}. It is ascending in their first index within each
     *            second-index.
     */
    public void setSelectRank(final Collection<String> inclList, final int[][] k) {
        if ((inclList != null && inclList.isEmpty()) || (k.length > 0 || k[0].length == 0)) {
            this.m_k = k.clone();
        } else {
            this.m_k = flip(k);
            for (int[] is : this.m_k) {
                Arrays.sort(is);
            }
            this.m_k = flip(this.m_k);
        }
        if (inclList == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        if (inclList.contains(null)) {
            throw new IllegalArgumentException("Argument array must not contain null: " + inclList);
        }
        if (!(inclList instanceof Set)) {
            Set<String> noDuplicatesSet = new HashSet<String>(inclList);
            if (noDuplicatesSet.size() != inclList.size()) {
                throw new IllegalArgumentException("Argument collection must " + "not contain duplicates: " + inclList);
            }
        }
        int[] indices = new int[inclList.size()];
        final DataTableSpec spec = m_inputTable.getDataTableSpec();
        int curIndex = 0;
        for (String name : inclList) {
            int index = spec.findColumnIndex(name);
            if (index == -1) {
                throw new IllegalArgumentException("Could not find column name:" + name.toString());
            }
            indices[curIndex++] = index;
        }
        setIndices(indices);
    }

    /**
     * @param is An array of int arrays.
     * @return Transposed version.
     */
    private int[][] flip(final int[][] is) {
        int[][] ret = new int[is[0].length][is.length];
        for (int i = is.length; i-- > 0;) {
            for (int j = is[i].length; j-- > 0;) {
                ret[j][i] = is[i][j];
            }
        }
        return ret;
    }

    /**
     * Get the number of maximum open containers. See {@link #setMaxOpenContainers(int)} for details.
     *
     * @return the maxOpenContainers
     */
    public int getMaxOpenContainers() {
        return m_maxOpenContainers;
    }

    /**
     * Changes the number of maximum open containers (=files) during the sorting. Containers are used in the m_k-way
     * merge sort, the higher the number the fewer iterations in the final merge need to be done.
     *
     * <p>
     * The default is {@value #DEF_MAX_OPENCONTAINER}.
     *
     * @param value the maxOpenContainers to number of maximal open containers.
     * @throws IllegalArgumentException If argument is smaller or equal to 2.
     */
    public void setMaxOpenContainers(final int value) {
        if (value <= 2) {
            throw new IllegalArgumentException("Invalid open container count: " + value);
        }
        m_maxOpenContainers = value;
    }

    /**
     * @return the sortInMemory field, see {@link #setSortInMemory(boolean)} for details.
     */
    public boolean getSortInMemory() {
        return m_sortInMemory;
    }

    /**
     * Forces the sorting to happen in memory (if argument is true). This is not advisable as tables can be large. Note,
     * the sorting may also take place in memory if the table is small (see class description for details).
     *
     * <p>
     * This option is merely to ensure backward compatibility and should not be used anymore.
     *
     * <p>
     * The default m_value for this option is <b>false</b>.
     *
     * @param sortInMemory <code>true</code> if sorting should be done in memory, <code>false</code> if sorting should
     *            be done in a hybrid way as described in the class description.
     */
    public void setSortInMemory(final boolean sortInMemory) {
        m_sortInMemory = sortInMemory;
    }

    /**
     * Sorts the table passed in the constructor according to the settings and returns the sorted output table.
     *
     * @param exec To report progress
     * @return The sorted output.
     * @throws CanceledExecutionException If canceled.
     */
    DataTable selectInternal(final ExecutionMonitor exec) throws CanceledExecutionException {
        DataTable result;
        if (m_sortInMemory) {
            result = sortInMemory(exec);
        } else {
            result = selectOnDisk(exec);
        }
        exec.setProgress(1.0);
        return result;
    }

    private DataTable sortInMemory(final ExecutionMonitor exec) throws CanceledExecutionException {
        final DataTable dataTable = m_inputTable;
        List<List<Double>> colList = new ArrayList<List<Double>>(m_indices.length);
        for (int i = m_indices.length; i-- > 0;) {
            colList.add(new ArrayList<Double>());
        }

        int progress = 0;
        final long rowCount = m_rowsInInputTable;
        exec.setMessage("Reading data");
        ExecutionMonitor readExec = exec.createSubProgress(0.5);
        for (final DataRow r : dataTable) {
            readExec.checkCanceled();
            if (rowCount > 0) {
                readExec.setProgress(progress / (double)rowCount, r.getKey().getString());
            } else {
                readExec.setMessage(r.getKey() + " (row " + progress + ")");
            }
            for (int i = 0; i < m_indices.length; i++) {
                int index = m_indices[i];
                final DataCell cell = r.getCell(index);
                if (cell.isMissing()) {
                    //skipping
                } else if (cell instanceof DoubleValue) {
                    DoubleValue value = (DoubleValue)cell;
                    colList.get(i).add(value.getDoubleValue());
                } else {
                    colList.get(i).add(null);
                }
            }
            progress++;
        }

        exec.setMessage("Sorting");

        for (List<Double> list : colList) {
            Collections.sort(list);
        }

        exec.setMessage("Creating sorted table");

        DataTableSpec newSpec = computeNewSpec(dataTable);
        final DataContainer dc = createDataContainer(newSpec, false);
        if (!dataTable.iterator().hasNext()) {
            dc.close();
            return dc.getTable();
        }
        ExecutionMonitor writeExec = exec.createSubProgress(0.5);
        progress = 0;
        for (int[] r : this.m_k) {
            exec.checkCanceled();
            if (rowCount > 0) {
                writeExec.setProgress(progress / (double)rowCount, "");
            } else {
                writeExec.setMessage(" (row " + progress + ")");
            }
            DataCell[] row = new DataCell[m_indices.length];
            for (int i = 0; i < row.length; i++) {
                List<Double> list = colList.get(i);
                if (r[i] >= list.size()) {
                    // in case this column has less values than the requested rank
                    row[i] = DataType.getMissingCell();
                } else {
                    final Double v = list.get(r[i]);
                    row[i] = v == null ? DataType.getMissingCell() : new DoubleCell(v.doubleValue());
                }
            }
            dc.addRowToTable(new DefaultRow(String.valueOf(r), row));
            progress++;
        }
        dc.close();
        return dc.getTable();
    }

    /**
     * @param dataTable The original table.
     * @return A new {@link DataTableSpec} with only the expected columns.
     */
    private DataTableSpec computeNewSpec(final DataTable dataTable) {
        final DataTableSpec dataTableSpec = dataTable.getDataTableSpec();
        DataColumnSpec[] specs = new DataColumnSpec[m_indices.length];
        for (int i = 0; i < m_indices.length; i++) {
            int index = m_indices[i];
            specs[i] =
                new DataColumnSpecCreator(dataTableSpec.getColumnSpec(index).getName(), DoubleCell.TYPE).createSpec();
        }
        DataTableSpec newSpec = new DataTableSpec(specs);
        return newSpec;
    }

    /**
     * Creates data container, either a buffered data container or a plain one.
     *
     * @param spec The spec of the container/table.
     * @param forceOnDisk false to use default, true to flush data immediately to disk. It's true when used in the
     *            #sortOnDisk(ExecutionMonitor) method and the container is only used temporarily.
     * @return A new fresh container.
     */
    abstract C createDataContainer(final DataTableSpec spec, final boolean forceOnDisk);

    /**
     * Clears the temporary table that was used during the execution but is no longer needed.
     *
     * @param table The table to be cleared.
     */
    abstract void clearTable(final DataTable table);

    /**
     * Sorts the given data table using a disk-based m_k-way merge sort.
     *
     * @param dataTable the data table that sgetRowCounthould be sorted
     * @param exec an execution context for reporting progress and creating BufferedDataContainers
     * @throws CanceledExecutionException if the user has canceled execution
     */
    private T selectOnDisk(final ExecutionMonitor exec) throws CanceledExecutionException {
        final T dataTable = m_inputTable;

        int[] nonMissingCount = new int[m_indices.length];
        int[] nans = new int[m_indices.length];
        exec.setMessage("Reading table");
        for (DataRow row : dataTable) {
            exec.checkCanceled();
            for (int i = 0; i < m_indices.length; i++) {
                DataCell cell = row.getCell(m_indices[i]);
                if (!cell.isMissing()) {
                    nonMissingCount[i]++;
                    if (cell instanceof DoubleValue) {
                        DoubleValue dv = (DoubleValue)cell;
                        if (Double.isNaN(dv.getDoubleValue())) {
                            nans[i]++;
                        }
                    } else {
                        throw new IllegalStateException("Not supported data m_value: " + cell.getType() + " (" + cell
                            + ") in " + row + ": " + m_indices[i]);
                    }
                }
            }
        }
        return selectOnDisk(exec, dataTable, nonMissingCount, nans);

    }

    /**
     * Finds the selected values with some helper statistics.
     *
     * @param exec An {@link ExecutionMonitor}.
     * @param dataTable A {@link DataTable} (of type {@code T}).
     * @param nonMissingCount The number of non-missing values in each (selected) columns.
     * @param nans The number of {@link Double#NaN}s in each (selected) columns.
     * @return A table with k rows for each index with the values in that index.
     * @throws CanceledExecutionException
     */
    private T selectOnDisk(final ExecutionMonitor exec, final T dataTable, final int[] nonMissingCount, final int[] nans)
        throws CanceledExecutionException {
        final PivotBuffer possiblePivots = new PivotBuffer(nonMissingCount, m_k);

        int[] nonMissingCounter = new int[m_indices.length];
        for (final DataRow row : dataTable) {
            exec.checkCanceled();
            for (int i = 0; i < m_indices.length; i++) {
                DataCell cell = row.getCell(m_indices[i]);
                if (!cell.isMissing() && cell instanceof DoubleValue) {
                    nonMissingCounter[i]++;
                    final DoubleValue dv = (DoubleValue)cell;
                    final double d = dv.getDoubleValue();
                    if (!Double.isNaN(d)) {
                        possiblePivots.update(nonMissingCounter[i], i, d);
                    }
                }
            }
        }
        /** 1st dim: col, 2nd dim: pivot values */
        double[][] pivotValues = possiblePivots.findPivots();
        LOGGER.debug("Pivoting values: " + Arrays.deepToString(pivotValues));
        DataContainer[][] containers = new DataContainer[m_indices.length][];
        int[][] valueCounter = new int[m_indices.length][];
        int[][] addedRows = new int[m_indices.length][];
        int chunkCount = 0;

        //k, col
        DataCell[][] result = new DataCell[this.m_k.length][m_indices.length];
        int startCol = 0;
        for (int col = startCol; col < m_indices.length; col++) {
            double[] ds = pivotValues[col];
            if (chunkCount + ds.length + 1 > m_maxOpenContainers) {
                process(exec, dataTable, nonMissingCount, nans, pivotValues, containers, valueCounter, addedRows,
                    result, startCol, col);
                startCol = col;
                chunkCount = 0;
            }
            chunkCount += ds.length + 1;
            valueCounter[col] = new int[ds.length];
            containers[col] = new DataContainer[ds.length + 1];
            addedRows[col] = new int[ds.length + 1];
            for (int i = ds.length + 1; i-- > 0;) {
                containers[col][i] = createDataContainer(SINGLE_DATA_TABLE_SPEC, true);
            }
        }
        process(exec, dataTable, nonMissingCount, nans, pivotValues, containers, valueCounter, addedRows, result,
            startCol, m_indices.length);

        C cont = createDataContainer(computeNewSpec(dataTable), false);
        for (int r = 0; r < result.length; ++r) {
            DefaultRow defaultRow = new DefaultRow(String.valueOf(r), result[r]);
            cont.addRowToTable(defaultRow);
        }
        for (DataContainer[] conts : containers) {
            for (DataContainer dataContainer : conts) {
                clearTable(dataContainer.getTable());
            }
        }
        cont.close();
        @SuppressWarnings("unchecked")
        final T table = (T)cont.getTable();
        return table;
    }

    /**
     * For the selected chunks it goes throw the table and puts the median values to the {@code result} array.
     *
     * @param exec An {@link ExecutionMonitor}.
     * @param dataTable The input data.
     * @param nonMissingCount Number of not missing values for selected columnt..
     * @param nans Number of NaNs.
     * @param pivotValues The pivot values.
     * @param containers The {@link DataContainer}s.
     * @param valueCounter Number of values.
     * @param addedRows The already added rows (for row keys).
     * @param result The result array.
     * @param startCol Start column index in {@link #m_indices}.
     * @param colAfter Index after the last column in {@link #m_indices}.
     * @throws CanceledExecutionException Cancelled.
     */
    private void process(final ExecutionMonitor exec, final T dataTable, final int[] nonMissingCount, final int[] nans,
        final double[][] pivotValues, final DataContainer[][] containers, final int[][] valueCounter,
        final int[][] addedRows, final DataCell[][] result, final int startCol, final int colAfter)
        throws CanceledExecutionException {
        for (final DataRow row : dataTable) {
            exec.checkCanceled();
            for (int i = startCol; i < colAfter; i++) {
                DataCell cell = row.getCell(m_indices[i]);
                if (!cell.isMissing() && cell instanceof DoubleValue) {
                    final DoubleValue dv = (DoubleValue)cell;
                    final double d = dv.getDoubleValue();
                    int key = Arrays.binarySearch(pivotValues[i], d);
                    if (!Double.isNaN(d)) {
                        if (key >= 0) {
                            ++valueCounter[i][key];
                        } else {
                            //key = -(insertion point) - 1
                            int insertionPoint = -1 - key;
                            DataRow defaultRow = new DefaultRow(String.valueOf(addedRows[i][insertionPoint]++), cell);
                            containers[i][insertionPoint].addRowToTable(defaultRow);
                        }
                    }
                }
            }
        }

        for (DataContainer[] conts : containers) {
            if (conts != null) {
                for (DataContainer dataContainer : conts) {
                    dataContainer.close();
                }
            }
        }

        // col, chunk, (originalK index,transformedK m_value)
        List<List<Map<Integer, Integer>>> postponed = new ArrayList<List<Map<Integer, Integer>>>(m_indices.length);
        for (int i = m_indices.length; i-- > 0;) {
            final ArrayList<Map<Integer, Integer>> newList = new ArrayList<Map<Integer, Integer>>();
            postponed.add(newList);
            for (int j = this.m_k.length + 1; j-- > 0;) {
                newList.add(new TreeMap<Integer, Integer>());
            }
        }
        for (int idx = m_k.length; idx-- > 0;) {
            int[] idxKs = this.m_k[idx];
            for (int col = /*idxKs.length*/colAfter; col-- > startCol;) {
                int order = idxKs[col];
                if (order > nonMissingCount[col]) {
                    result[idx][col] = DataType.getMissingCell();
                    continue;
                }
                if (order > nonMissingCount[col] - nans[col]) {
                    result[idx][col] = new DoubleCell(Double.NaN);
                    continue;
                }
                int found = 0;
                for (int i = 0; i < addedRows[col].length; ++i) {
                    found += addedRows[col][i];
                    if (order < found || i == valueCounter[col].length) {
                        //find in container, postpone
                        postponed.get(col).get(i).put(idx, order - found + addedRows[col][i]);
                        break;
                    }
                    found += valueCounter[col][i];
                    if (order < found) {
                        result[idx][col] = new DoubleCell(pivotValues[col][i]);
                        break;
                    }
                }
            }
        }

        MemoryActionIndicator memIndicator = MemoryAlertSystem.getInstance().newIndicator();

        for (int col = colAfter; col-- > startCol;) {
            exec.checkCanceled();
            for (int chunk = containers[col].length; chunk-- > 0;) {
                exec.checkCanceled();
                Map<Integer, Integer> map = postponed.get(col).get(chunk);
                if (!map.isEmpty()) {
                    DataContainer container = containers[col][chunk];
                    int[] newKs = new int[map.size()];
                    int i = 0;
                    for (Entry<Integer, Integer> entry : map.entrySet()) {
                        newKs[i++] = entry.getValue().intValue();
                    }
                    if (!memIndicator.lowMemoryActionRequired() && addedRows[col][chunk] < MAX_CELLS_SORTED_IN_MEMORY) {
                        double[] values = new double[addedRows[col][chunk]];
                        int r = 0;
                        for (final DataRow row : container.getTable()) {
                            values[r++] = ((DoubleValue)row.getCell(0)).getDoubleValue();
                        }
                        Arrays.sort(values);
                        for (Entry<Integer, Integer> entry : map.entrySet()) {
                            result[entry.getKey()][col] =
                                new DoubleCell(values.length <= entry.getValue() ? Double.NaN
                                    : values[entry.getValue()]);
                        }
                    } else {
                        BufferedSelectRank tmp =
                            new BufferedSelectRank((BufferedDataTable)container.getTable(),
                                Collections.singletonList(SINGLE_DATA_TABLE_SPEC.getColumnSpec(0).getName()),
                                new int[][]{newKs});
                        DataTable resultTable = tmp.select((ExecutionContext)exec);
                        Iterator<Entry<Integer, Integer>> it = map.entrySet().iterator();
                        for (DataRow dataRow : resultTable) {
                            Entry<Integer, Integer> entry = it.next();
                            result[entry.getKey()][col] = dataRow.getCell(0);
                        }
                    }
                }
            }
        }
    }

    private static class PivotBuffer {
        private static final int PIVOT_CANDIDATE_COUNT = 8;

        private static final Random m_random = new Random();

        /** 1st dim: which m_k, 2nd dim: col, 3rd dim: buffer */
        PivotEntry[][][] m_entries;

        private int[] m_bufferCount;

        private final int[][] m_ks;

        private final int[] m_nonMissingCount;

        PivotBuffer(final int[] nonMissingCount, final int[][] k) {
            super();
            this.m_nonMissingCount = nonMissingCount;
            m_ks = k;
            m_entries = new PivotEntry[k.length][nonMissingCount.length][PIVOT_CANDIDATE_COUNT];
            m_bufferCount = new int[nonMissingCount.length];
        }

        /**
         * @return The pivoting values expected to be the closest to the k values.
         */
        public double[][] findPivots() {
            List<SortedSet<Double>> pivotValues = new ArrayList<SortedSet<Double>>(m_nonMissingCount.length);
            for (int col = 0; col < m_nonMissingCount.length; ++col) {
                final TreeSet<Double> set = new TreeSet<Double>();
                pivotValues.add(set);
                for (int[] kVals : m_ks) {
                    set.add(findClosest(kVals[col], col));
                }
            }

            double[][] ret = new double[pivotValues.size()][];
            for (int i = pivotValues.size(); i-- > 0;) {
                final SortedSet<Double> set = pivotValues.get(i);
                ret[i] = new double[set.size()];
                int j = 0;
                for (Double d : set) {
                    ret[i][j++] = d;
                }
            }
            return ret;
        }

        /**
         * @param k An index m_value.
         * @param col The column index.
         * @return The closest m_value to find for.
         */
        private Double findClosest(final int k, final int col) {
            int closestDiff = Integer.MAX_VALUE;
            double closest = 0;
            for (PivotEntry[][] dim23 : m_entries) {
                final PivotEntry[] pivotEntries = dim23[col];
                for (PivotEntry entry : pivotEntries) {
                    if (entry != null) {
                        int diff = Math.abs(entry.m_estimatedSmallerCount - k);
                        if (diff < closestDiff) {
                            closestDiff = diff;
                            closest = entry.m_value;
                        }
                    }
                }
            }
            if (closestDiff == Integer.MAX_VALUE) {
                return Double.NaN;
            }
            return closest;
        }

        private static class PivotEntry {
            private double m_value;

            private int m_estimatedSmallerCount;

            private int m_estimatedCount;

            private int m_estimatedLargerCount;

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return "[v=" + m_value + ", <:" + m_estimatedSmallerCount + ", =:" + m_estimatedCount + "]";
            }

        }

        /**
         * Updates statistics based on the values presented.
         *
         * @param rowIndex The number of already consumed rows.
         * @param col The column index.
         * @param d The current value.
         */
        public void update(final double rowIndex, final int col, final double d) {
            for (int i = 0; i < m_ks.length; i++) {
                final PivotEntry[] kEntry = m_entries[i][col];
                if (m_bufferCount[col] < PIVOT_CANDIDATE_COUNT) {
                    insertOrUpdate(rowIndex, col, d);
                    break;
                } else {
                    int expectedSmaller = (int)(m_ks[i][col] * rowIndex / m_nonMissingCount[col]);
                    boolean includeRandom =
                        m_random.nextDouble() < PIVOT_CANDIDATE_COUNT * 3.0 / m_nonMissingCount[col];
                    if (includeRandom) {
                        int j = 0;
                        for (; j < kEntry.length; ++j) {
                            if (d < kEntry[j].m_value) {
                                for (int u = kEntry.length; u-- > j + 1;) {
                                    kEntry[u].m_value = kEntry[u - 1].m_value;
                                    kEntry[u].m_estimatedCount = kEntry[u - 1].m_estimatedCount;
                                    kEntry[u].m_estimatedSmallerCount = kEntry[u - 1].m_estimatedSmallerCount + 1;
                                    kEntry[u].m_estimatedLargerCount = kEntry[u - 1].m_estimatedLargerCount;
                                }
                                kEntry[j].m_value = d;
                                kEntry[j].m_estimatedCount = 1;
                                break;
                            } else if (d == kEntry[j].m_value) {
                                kEntry[j].m_estimatedCount++;
                                ++j;
                                break;
                            }
                        }
                        if (j == kEntry.length) {
                            kEntry[j - 1].m_value = d;
                            kEntry[j - 1].m_estimatedCount = 1;
                            kEntry[j - 1].m_estimatedSmallerCount =
                                Math.max(expectedSmaller, Math.min(kEntry[j - 1].m_estimatedSmallerCount
                                    + kEntry[j - 1].m_estimatedCount, (int)rowIndex - 1));
                        }
                        for (; j < kEntry.length; ++j) {
                            kEntry[j].m_estimatedSmallerCount++;
                        }
                    } else {
                        int j = 0;
                        for (; j < kEntry.length; ++j) {
                            if (d <= kEntry[j].m_value) {
                                break;
                            }
                        }
                        if (j < kEntry.length && d == kEntry[j].m_value) {
                            kEntry[j].m_estimatedCount++;
                            //increase others
                            ++j;
                            for (; j < kEntry.length; ++j) {
                                kEntry[j].m_estimatedSmallerCount++;
                            }
                        } else if (j == 0 && kEntry[j].m_estimatedSmallerCount > expectedSmaller) {
                            //insert before
                            for (j = kEntry.length; j-- > 1;) {
                                kEntry[j].m_value = kEntry[j - 1].m_value;
                                kEntry[j].m_estimatedSmallerCount = kEntry[j - 1].m_estimatedSmallerCount + 1;
                            }
                            kEntry[0].m_value = d;
                            kEntry[0].m_estimatedCount = 1;
                            kEntry[0].m_estimatedSmallerCount = Math.min(expectedSmaller, (int)rowIndex - 1);
                        } else if (j == kEntry.length) {
                            if (kEntry[j - 1].m_estimatedSmallerCount < expectedSmaller) {
                                //set last
                                for (int u = 0; u < j - 1; ++u) {
                                    kEntry[u].m_value = kEntry[u + 1].m_value;
                                    kEntry[u].m_estimatedCount = kEntry[u + 1].m_estimatedCount;
                                    kEntry[u].m_estimatedSmallerCount = kEntry[u + 1].m_estimatedSmallerCount;
                                    kEntry[u].m_estimatedLargerCount = kEntry[u + 1].m_estimatedLargerCount;
                                }
                                kEntry[j - 1].m_value = d;
                                kEntry[j - 1].m_estimatedCount = 1;
                                kEntry[j - 1].m_estimatedSmallerCount =
                                    Math.max(expectedSmaller, (kEntry[j - 2].m_estimatedSmallerCount
                                        + kEntry[j - 2].m_estimatedCount + (int)rowIndex) / 2);
                            }
                        }
                    }
                }
            }
        }

        /**
         * @param rowIndex Consumed number of rows.
         * @param col The column index.
         * @param d The new value.
         */
        private void insertOrUpdate(final double rowIndex, final int col, final double d) {
            boolean found = false;
            int i = 0;
            for (; i < m_bufferCount[col]; ++i) {
                if (m_entries[0][col][i] != null && m_entries[0][col][i].m_value == d) {
                    found = true;
                    break;
                }
            }
            if (found) {
                for (int k = m_ks.length; k-- > 0;) {
                    m_entries[k][col][i].m_estimatedCount++;
                    for (int j = i + 1; j < m_bufferCount[col]; ++j) {
                        m_entries[k][col][j].m_estimatedSmallerCount++;
                    }
                }
            } else {
                m_bufferCount[col]++;
                for (int k = m_ks.length; k-- > 0;) {
                    m_entries[k][col][m_bufferCount[col] - 1] = new PivotEntry();
                    for (int j = m_bufferCount[col]; j-- > i + 1;) {
                        m_entries[k][col][j].m_estimatedSmallerCount =
                            m_entries[k][col][j - 1].m_estimatedSmallerCount + 1;
                        m_entries[k][col][j].m_estimatedCount = m_entries[k][col][j - 1].m_estimatedCount;
                        m_entries[k][col][j].m_value = m_entries[k][col][j - 1].m_value;
                    }
                    m_entries[k][col][i].m_value = d;
                    m_entries[k][col][i].m_estimatedCount = 1;
                    m_entries[k][col][i].m_estimatedSmallerCount =
                        i == 0 ? 0 : /*i == m_bufferCount[col] - 1 ?*/m_entries[k][col][i - 1].m_estimatedSmallerCount
                            + m_entries[k][col][i - 1].m_estimatedCount
                    /*: m_entries[k][col][i].estimatedSmallerCount - 1*/;
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            int i = 0;
            for (PivotEntry[][] ee : m_entries) {
                sb.append("k=").append(i++).append('\n');
                for (PivotEntry[] pivotEntries : ee) {
                    sb.append(Arrays.toString(pivotEntries)).append('\n');
                }
            }
            return sb.toString();
        }

    }
}
