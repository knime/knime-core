/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * Created on 2013.06.09. by Gabor
 */
package org.knime.base.data.statistics;

import java.text.NumberFormat;
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
import org.knime.core.data.def.StringCell;
import org.knime.core.data.sort.MemoryService;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;

/**
 * Class to find the median/kth element of a table. Based on {@link TableSorter}.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @author Gabor Bakos
 * @see TableSorter
 * @since 2.8
 */
abstract class SelectRank<C extends DataContainer, T extends DataTable> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(SelectRank.class);

    /** Representing column spec to sort according to the row key. */
    public static final DataColumnSpec ROWKEY_SORT_SPEC = new DataColumnSpecCreator("-ROWKEY -",
        DataType.getType(StringCell.class)).createSpec();

    /**
     * Default memory threshold. If this relative amount of memory is filled, the chunk is sorted and flushed to disk.
     */
    public static final double DEF_MEM_THRESHOLD = 0.8;

    /**
     * The maximum number of open containers. See {@link #setMaxOpenContainers(int)} for details.
     */
    public static final int DEF_MAX_OPENCONTAINER = 40;

    private MemoryService m_memService = new MemoryService(DEF_MEM_THRESHOLD);

    private final T m_inputTable;

    private final int m_rowsInInputTable;

    /**
     * The maximal number of open containers. This has an effect when many containers must be merged.
     */
    private int m_maxOpenContainers = DEF_MAX_OPENCONTAINER;

    /**
     * Maximum number of rows. Only used in unit test. Defaults to {@link Integer#MAX_VALUE}.
     */
    private int m_maxRows = Integer.MAX_VALUE;

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
    private SelectRank(final T inputTable, final int rowsCount) {
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
     * Inits table sorter using the sorting according to {@link #setSelectRank(Collection, boolean[], int[][])}.
     *
     * @param inputTable The table to sort
     * @param rowsCount The number of rows in the table
     * @param inclList Passed on to {@link #setSelectRank(Collection, int[][])}.
     * @throws NullPointerException If any argument is null.
     * @throws IllegalArgumentException If arguments are inconsistent.
     */
    public SelectRank(final T inputTable, final int rowsCount, final Collection<String> inclList, final int[][] k) {
        this(inputTable, rowsCount);
        setSelectRank(inclList, k);
    }

    /**
     * Inits table sorter using the sorting according to {@link #setSelectRank(Collection, boolean[], boolean, m_k)}.
     *
     * @param inputTable The table to sort
     * @param rowsCount The number of rows in the table
     * @param inclList Passed on to {@link #setSelectRank(Collection, boolean, int[][])}.
     * @param sortMissingsToEnd Passed on to {@link #setSelectRank(Collection, boolean, int[][])}.
     * @throws NullPointerException If any argument is null.
     * @throws IllegalArgumentException If arguments are inconsistent.
     */
    public SelectRank(final T inputTable, final int rowsCount, final Collection<String> inclList,
        final boolean sortMissingsToEnd, final int[][] k) {
        this(inputTable, rowsCount);
        setSelectRank(inclList, sortMissingsToEnd, k);
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
     */
    public void setSelectRank(final Collection<String> inclList, final int[][] k) {
        setSelectRank(inclList, false, k);
    }

    /**
     * Sets sorting columns and order.
     *
     * @param inclList the list with the columns to sort; the first column name represents the first sort criteria, the
     *            second the second criteria and so on.
     *
     * @param sortMissingsToEnd Whether to sort missing values always to the end independent to the sort oder (if false
     *            missing values are always smaller than non-missings).
     * @param m_k The {@code m_k}th elements to select (the inner array contains the indices for columns, all same number of
     *            rows are selected for each column).
     */
    public void setSelectRank(final Collection<String> inclList, final boolean sortMissingsToEnd, final int[][] k) {
        this.m_k = flip(k);
        for (int[] is : this.m_k) {
            Arrays.sort(is);
        }
        this.m_k = flip(this.m_k);
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
            if (index == -1 && !name.equals(ROWKEY_SORT_SPEC.getName())) {
                throw new IllegalArgumentException("Could not find column name:" + name.toString());
            }
            indices[curIndex++] = index;
        }
        setIndices(indices);
    }

    /**
     * @param is
     * @return
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
     * Changes the number of maximum open containers (=files) during the sorting. Containers are used in the m_k-way merge
     * sort, the higher the number the fewer iterations in the final merge need to be done.
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
     * Set the maximum number of rows per chunk, defaults to {@link Integer#MAX_VALUE}. This field is modified from the
     * testing framework.
     *
     * @param maxRows the maxRows to set
     */
    void setMaxRows(final int maxRows) {
        m_maxRows = maxRows;
    }

    /**
     * Set memory service. Used in unit test.
     *
     * @param memService the memService to set
     */
    void setMemService(final MemoryService memService) {
        m_memService = memService;
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
     * The default value for this option is <b>false</b>.
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
    DataTable sortInternal(final ExecutionMonitor exec) throws CanceledExecutionException {
        DataTable result;
        if (m_sortInMemory) {
            result = sortInMemory(exec);
        } else {
            result = sortOnDisk(exec);
        }
        exec.setProgress(1.0);
        return result;
    }

    private DataTable sortInMemory(final ExecutionMonitor exec) throws CanceledExecutionException {
        final DataTable dataTable = m_inputTable;
        List<List<Double>> colList = new ArrayList<List<Double>>(m_indices.length);
        for (int i = m_indices.length; i-- > 0;) {
            colList.add(new ArrayList<Double>(Math.max(m_maxRows, 100)));
        }

        int progress = 0;
        final int rowCount = m_rowsInInputTable;
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
                    colList.get(i).add(null);
                } else if (cell instanceof DoubleValue) {
                    DoubleValue value = (DoubleValue)cell;
                    colList.get(i).add(value.getDoubleValue());
                } else {
                    colList.get(i).add(null);
                }
            }
            progress++;
        }
        // if there is 0 or 1 row only, return immediately (can't rely on
        // "rowCount" as it might not be set)
        if (colList.get(0).size() <= 1) {
            return m_inputTable;
        }

        exec.setMessage("Sorting");

        for (List<Double> list : colList) {
            Collections.sort(list);
        }

        exec.setMessage("Creating sorted table");

        DataTableSpec newSpec = computeNewSpec(dataTable);
        final DataContainer dc = createDataContainer(newSpec, false);
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
                final Double v = list.get(r[i]);
                row[i] = v == null ? DataType.getMissingCell() : new DoubleCell(v.doubleValue());
            }
            dc.addRowToTable(new DefaultRow(String.valueOf(r), row));
            progress++;
        }
        dc.close();
        return dc.getTable();
    }

    /**
     * @param dataTable
     * @return
     */
    private DataTableSpec computeNewSpec(final DataTable dataTable) {
        final DataTableSpec dataTableSpec = dataTable.getDataTableSpec();
        DataColumnSpec[] specs = new DataColumnSpec[m_indices.length];
        for (int i = 0; i < m_indices.length; i++) {
            int index = m_indices[i];
            specs[i] = dataTableSpec.getColumnSpec(index);
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
    private T sortOnDisk(final ExecutionMonitor exec) throws CanceledExecutionException {
        final T dataTable = m_inputTable;

        double progress = 0.0;
        double incProgress = m_rowsInInputTable <= 0 ? -1.0 : 1.0 / (2.0 * m_rowsInInputTable);
        int counter = 0;
        int cf = 0;
        int chunkStartRow = 0;
        long dummyCounter = 0;

        int[] nonMissingCount = new int[m_indices.length];
        int[] nans = new int[m_indices.length];
        int rowCount = 0;
        exec.setMessage("Reading table");
        for (DataRow row : dataTable) {
            exec.checkCanceled();
            ++rowCount;
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
                        throw new IllegalStateException("Not supported data value: " + cell.getType() + " (" + cell
                            + ") in " + row + ": " + m_indices[i]);
                    }
                }
            }
        }
        return sortOnDisk(exec, dataTable, nonMissingCount, nans);

        //        for (Iterator<DataRow> iter = dataTable.iterator(); iter.hasNext();) {
        //            cf++;
        //            exec.checkCanceled();
        //            if (!m_memService.isMemoryLow() && (cf % m_maxRows != 0 || cf == 0)) {
        //                counter++;
        //                exec.checkCanceled();
        //                String message = "Reading table, " + counter + " rows read";
        //                if (m_rowsInInputTable > 0) {
        //                    progress += incProgress;
        //                    exec.setProgress(progress, message);
        //                } else {
        //                    exec.setMessage(message);
        //                }
        //                DataRow row = iter.next();
        //                DataCell[] r = new DataCell[m_indices.length];
        //                buffer.add(r);
        //            } else {
        //                LOGGER.debug("Writing chunk [" + chunkStartRow + ":" + counter + "] - mem usage: " + getMemUsage());
        //                if (m_rowsInInputTable > 0) {
        //                    int estimatedIncrements = m_rowsInInputTable - counter + buffer.size();
        //                    incProgress = (0.5 - progress) / estimatedIncrements;
        //                }
        //                exec.setMessage("Sorting temporary buffer");
        //                // sort buffer
        //                Collections.sort(buffer);
        //                // write buffer to disk
        //                DataContainer diskCont = createDataContainer(dataTable.getDataTableSpec(), true);
        //                diskCont.setMaxPossibleValues(0);
        //                final int totalBufferSize = buffer.size();
        //                for (int i = 0; i < totalBufferSize; i++) {
        //                    exec.setMessage("Writing temporary table -- " + i + "/" + totalBufferSize);
        //                    // must not use Iterator#remove as it causes
        //                    // array copies
        //                    DataCell[] next = buffer.set(i, null);
        //                    diskCont.addRowToTable(new DefaultRow(Long.toString(dummyCounter++), next));
        //                    exec.checkCanceled();
        //                    if (m_rowsInInputTable > 0) {
        //                        progress += incProgress;
        //                        exec.setProgress(progress);
        //                    }
        //                }
        //                buffer.clear();
        //                diskCont.close();
        //                chunksCont.add(diskCont.getTable());
        //
        //                // Force full gc to be sure that there is not too much
        //                // garbage
        //                LOGGER.debug("Wrote chunk [" + chunkStartRow + ":" + counter + "] - mem usage: " + getMemUsage());
        //                Runtime.getRuntime().gc();
        //
        //                LOGGER.debug("Forced gc() when reading rows, new mem usage: " + getMemUsage());
        //                chunkStartRow = counter + 1;
        //            }
        //        }
        //        // no or one row only in input table, can exit immediately
        //        // (can't rely on global rowCount - might not be set)
        //        if (counter <= 1) {
        //            return m_inputTable;
        //        }
        //        // Add buffer to the chunks
        //        if (!buffer.isEmpty()) {
        //            // sort buffer
        //            Collections.sort(buffer);
        //            chunksCont.add(buffer);
        //        }
        //
        //        exec.setMessage("Merging temporary tables");
        //        // The final output container
        //        DataContainer cont = null;
        //        // merge chunks until there is one left
        //        while (chunksCont.size() > 1 || cont == null) {
        //            exec.setMessage("Merging temporary tables, " + chunksCont.size() + " remaining");
        //            if (chunksCont.size() < m_maxOpenContainers) {
        //                // The final output container, leave it to the
        //                // system to do the caching (bug 1809)
        //                cont = createDataContainer(dataTable.getDataTableSpec(), false);
        //                if (m_rowsInInputTable > 0) {
        //                    incProgress = (1.0 - progress) / m_rowsInInputTable;
        //                }
        //            } else {
        //                cont = createDataContainer(dataTable.getDataTableSpec(), true);
        //                if (m_rowsInInputTable > 0) {
        //                    double estimatedReads =
        //                        Math.ceil(chunksCont.size() / (double)m_maxOpenContainers) * m_rowsInInputTable;
        //                    incProgress = (1.0 - progress) / estimatedReads;
        //                }
        //            }
        //            // isolate lists to merge
        //            List<Iterable<DataRow>> toMergeCont = new ArrayList<Iterable<DataRow>>();
        //            int c = 0;
        //            for (Iterator<Iterable<DataRow>> iter = chunksCont.iterator(); iter.hasNext();) {
        //                c++;
        //                if (c > m_maxOpenContainers) {
        //                    break;
        //                }
        //                toMergeCont.add(iter.next());
        //                // remove container from chunksCont
        //                iter.remove();
        //            }
        //            // merge container in toMergeCont into cont
        //            PriorityQueue<MergeEntry> currentRows = new PriorityQueue<MergeEntry>(toMergeCont.size());
        //            for (int i = 0; i < toMergeCont.size(); i++) {
        //                Iterator<DataRow> iter = toMergeCont.get(i).iterator();
        //                if (iter.hasNext()) {
        //                    currentRows.add(new MergeEntry(iter, i));
        //                }
        //            }
        //            while (currentRows.size() > 0) {
        //                MergeEntry first = currentRows.poll();
        //                DataRow least = first.poll();
        //                cont.addRowToTable(least);
        //                exec.checkCanceled();
        //                if (m_rowsInInputTable > 0) {
        //                    // increment progress
        //                    progress += incProgress;
        //                    exec.setProgress(progress);
        //                }
        //                // read next row in first
        //                if (null != first.peek()) {
        //                    currentRows.add(first);
        //                }
        //            }
        //            cont.close();
        //            // Add cont to the pending containers
        //            chunksCont.add(0, cont.getTable());
        //            // toMergeCont may contain DataTable. These DatatTables can be
        //            // cleared now.
        //            for (Iterable<DataRow> merged : toMergeCont) {
        //                if (merged instanceof DataTable) {
        //                    clearTable((DataTable)merged);
        //                }
        //            }
        //        }
        //        return cont.getTable();
    }

    /**
     * @param exec
     * @param dataTable
     * @param nonMissingCount
     * @param nans
     * @return
     * @throws CanceledExecutionException
     */
    private T sortOnDisk(final ExecutionMonitor exec, final T dataTable, final int[] nonMissingCount, final int[] nans)
        throws CanceledExecutionException {
        final PivotBuffer possiblePivots = new PivotBuffer(nonMissingCount, nans, m_k);

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
        final DataTableSpec singleColumn = new DataTableSpec(new String[]{"x"}, new DataType[]{DoubleCell.TYPE});
        for (int col = 0; col < m_indices.length; col++) {
            double[] ds = pivotValues[col];
            chunkCount += ds.length + 1;
            valueCounter[col] = new int[ds.length];
            if (chunkCount > m_maxOpenContainers) {
                throw new UnsupportedOperationException("TODO");
            }
            containers[col] = new DataContainer[ds.length + 1];
            addedRows[col] = new int[ds.length + 1];
            for (int i = ds.length + 1; i-- > 0;) {
                containers[col][i] = createDataContainer(singleColumn, true);
            }
        }

        for (final DataRow row : dataTable) {
            exec.checkCanceled();
            for (int i = 0; i < m_indices.length; i++) {
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
            for (DataContainer dataContainer : conts) {
                dataContainer.close();
            }
        }

        // col, chunk, (originalK index,transformedK value)
        List<List<Map<Integer, Integer>>> postponed = new ArrayList<List<Map<Integer, Integer>>>(m_indices.length);
        for (int i = m_indices.length; i-- > 0;) {
            final ArrayList<Map<Integer, Integer>> newList = new ArrayList<Map<Integer, Integer>>();
            postponed.add(newList);
            for (int j = this.m_k.length + 1; j-- > 0;) {
                newList.add(new TreeMap<Integer, Integer>());
            }
        }
        //m_k, col
        DataCell[][] result = new DataCell[this.m_k.length][m_indices.length];
        for (int idx = m_k.length; idx-- > 0;) {
            int[] idxKs = this.m_k[idx];
            for (int col = idxKs.length; col-- > 0;) {
                int order = idxKs[col];
                if (order > nonMissingCount[col]) {
                    result[idx][col] = DataType.getMissingCell();
                    break;
                }
                if (order > nonMissingCount[col] - nans[col]) {
                    result[idx][col] = new DoubleCell(Double.NaN);
                    break;
                }
                int found = 0;
                for (int i = 0; i < addedRows[col].length; ++i) {
                    found += addedRows[col][i];
                    if (order < found) {
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

        for (int col = m_indices.length; col-- > 0;) {
            for (int chunk = containers[col].length; chunk-- > 0;) {
                Map<Integer, Integer> map = postponed.get(col).get(chunk);
                int lot = 5000000;
                if (!map.isEmpty()) {
                    DataContainer container = containers[col][chunk];
                    int[] newKs = new int[map.size()];
                    int i = 0;
                    for (Entry<Integer, Integer> entry : map.entrySet()) {
                        newKs[i++] = entry.getValue().intValue();
                    }
                    if (!m_memService.isMemoryLow() && addedRows[col][chunk] < lot) {
                        double[] values = new double[addedRows[col][chunk]];
                        int r = 0;
                        for (final DataRow row : container.getTable()) {
                            values[r++] = ((DoubleValue)row.getCell(0)).getDoubleValue();
                        }
                        Arrays.sort(values);
                        for (Entry<Integer, Integer> entry : map.entrySet()) {
                            result[entry.getKey()][col] = new DoubleCell(values[entry.getValue()]);
                        }
                    } else {
                        BufferedSelectRank tmp =
                            new BufferedSelectRank((BufferedDataTable)container.getTable(),
                                Collections.singletonList(singleColumn.getColumnSpec(0).getName()), new int[][]{newKs});
                        DataTable resultTable = tmp.sortInternal(exec);
                        Iterator<Entry<Integer, Integer>> it = map.entrySet().iterator();
                        for (DataRow dataRow : resultTable) {
                            Entry<Integer, Integer> entry = it.next();
                            result[entry.getKey()][col] = dataRow.getCell(0);
                        }
                    }
                }
            }
        }

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
        return (T)cont.getTable();
    }

    private String getMemUsage() {
        Runtime runtime = Runtime.getRuntime();
        long free = runtime.freeMemory();
        long total = runtime.totalMemory();
        long avail = runtime.maxMemory();
        double freeD = free / (double)(1024 * 1024);
        double totalD = total / (double)(1024 * 1024);
        double availD = avail / (double)(1024 * 1024);
        String freeS = NumberFormat.getInstance().format(freeD);
        String totalS = NumberFormat.getInstance().format(totalD);
        String availS = NumberFormat.getInstance().format(availD);
        return "avail: " + availS + "MB, total: " + totalS + "MB, free: " + freeS + "MB";
    }

    private static class PivotBuffer {
        private static final int PIVOT_CANDIDATE_COUNT = 8;

        private static final Random random = new Random();

        /** 1st dim: which m_k, 2nd dim: col, 3rd dim: buffer */
        PivotEntry[][][] entries;

        private int[] bufferCount;

        private final int[][] ks;

        private final int[] nonMissingCount;

        private final int[] nans;

        @SuppressWarnings("hiding")
        PivotBuffer(final int[] nonMissingCount, final int[] nans, final int[][] k) {
            super();
            this.nonMissingCount = nonMissingCount;
            this.nans = nans;
            ks = k;
            entries = new PivotEntry[k.length][nonMissingCount.length][PIVOT_CANDIDATE_COUNT];
            bufferCount = new int[nonMissingCount.length];
        }

        /**
         * @return
         */
        public double[][] findPivots() {
            List<SortedSet<Double>> pivotValues = new ArrayList<SortedSet<Double>>(nonMissingCount.length);
            for (int col = nonMissingCount.length; col-- > 0;) {
                final TreeSet<Double> set = new TreeSet<Double>();
                pivotValues.add(set);
                for (int[] kVals : ks) {
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
         * @param m_k
         * @param col
         * @return
         */
        private Double findClosest(final int k, final int col) {
            int closestDiff = Integer.MAX_VALUE;
            double closest = 0;
            for (PivotEntry[][] dim23 : entries) {
                for (PivotEntry entry : dim23[col]) {
                    if (entry != null) {
                        int diff = Math.abs(entry.estimatedSmallerCount - k);
                        if (diff < closestDiff) {
                            closestDiff = diff;
                            closest = entry.value;
                        }
                    }
                }
            }
            if (closestDiff == Integer.MAX_VALUE) {
                throw new IllegalStateException("Should not happen: col: " + col + " m_k: " + k + "\n"
                    + Arrays.deepToString(entries));
            }
            return closest;
        }

        private static class PivotEntry {
            private double value;

            private int estimatedSmallerCount;

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return "[v=" + value + ", c=" + estimatedSmallerCount + "]";
            }

        }

        public void update(final double rowIndex, final int col, final double d) {
            for (int i = 0; i < ks.length; i++) {
                final PivotEntry[] kEntry = entries[i][col];
                if (bufferCount[col] < PIVOT_CANDIDATE_COUNT) {
                    insertOrUpdate(rowIndex, col, d);
                } else {
                    int expectedSmaller = (int)(ks[i][col] * rowIndex / nonMissingCount[col]);
                    boolean includeRandom = random.nextDouble() < PIVOT_CANDIDATE_COUNT * 3.0 / nonMissingCount[col];
                    if (includeRandom) {
                        int j = 0;
                        for (; j < kEntry.length; ++j) {
                            if (d < kEntry[j].value) {
                                for (int u = kEntry.length; u-- > j + 1;) {
                                    kEntry[u].value = kEntry[u - 1].value;
                                }
                                kEntry[j].value = d;
                                break;
                            } else if (d == kEntry[j].value) {
                                ++j;
                                break;
                            }
                        }
                        if (j == kEntry.length) {
                            kEntry[j - 1].value = d;
                            kEntry[j - 1].estimatedSmallerCount =
                                Math.max(expectedSmaller,
                                    Math.min(kEntry[j - 1].estimatedSmallerCount + 1, (int)rowIndex - 1));
                        }
                        for (; j < kEntry.length; ++j) {
                            kEntry[j].estimatedSmallerCount++;
                        }
                    } else {
                        int j = 0;
                        for (; j < kEntry.length; ++j) {
                            if (d <= kEntry[j].value) {
                                break;
                            }
                        }
                        if (j < kEntry.length && d == kEntry[j].value) {
                            //increase others
                            ++j;
                            for (; j < kEntry.length; ++j) {
                                kEntry[j].estimatedSmallerCount++;
                            }
                        } else if (j == 0 && kEntry[j].estimatedSmallerCount > expectedSmaller) {
                            //insert before
                            for (j = kEntry.length; j-- > 1;) {
                                kEntry[j].value = kEntry[j - 1].value;
                                kEntry[j].estimatedSmallerCount =
                                        kEntry[j - 1].estimatedSmallerCount + 1;
                            }
                            kEntry[0].value = d;
                            kEntry[0].estimatedSmallerCount = Math.min(expectedSmaller, (int)rowIndex - 1);
                        } else if (j == kEntry.length) {
                            if (kEntry[j - 1].estimatedSmallerCount < expectedSmaller) {
                                //set last
                                kEntry[j - 1].value = d;
                                kEntry[j - 1].estimatedSmallerCount = expectedSmaller;
                            } else {
                                //insert
                                //                                for (int u = entries[i].length; u-- > j + 1;) {
                                //                                    entries[i][u][col].value = entries[i][u - 1][col].value;
                                //                                    entries[i][u][col].estimatedSmallerCount =
                                //                                        entries[i][u - 1][col].estimatedSmallerCount + 1;
                                //                                }
                                --j;
                                kEntry[j].value = d;
                                kEntry[j].estimatedSmallerCount =
                                    (kEntry[j].estimatedSmallerCount + kEntry[j].estimatedSmallerCount) / 2;
                            }
                        }
                    }
                }
            }
        }

        /**
         * @param rowIndex
         * @param col
         * @param d
         */
        private void insertOrUpdate(final double rowIndex, final int col, final double d) {
            boolean found = false;
            int i = 0;
            for (; i < bufferCount[col]; ++i) {
                if (entries[0][col][i] != null && entries[0][col][i].value == d) {
                    found = true;
                    break;
                }
            }
            if (found) {
                for (int k = ks.length; k-- > 0;) {
                    for (int j = i + 1; j < bufferCount[col]; ++j) {
                        entries[k][col][j].estimatedSmallerCount++;
                    }
                }
            } else {
                bufferCount[col]++;
                for (int k = ks.length; k-- > 0;) {
                    entries[k][col][bufferCount[col] - 1] = new PivotEntry();
                    for (int j = bufferCount[col]; j-- > i + 1;) {
                        entries[k][col][j].estimatedSmallerCount = entries[k][col][j - 1].estimatedSmallerCount + 1;
                        entries[k][col][j].value = entries[k][col][j - 1].value;
                    }
                    entries[k][col][i].value = d;
                    entries[k][col][i].estimatedSmallerCount =
                        i == 0 ? 0 : i == bufferCount[col] - 1 ? entries[k][col][i - 1].estimatedSmallerCount + 1
                            : entries[k][col][i].estimatedSmallerCount - 1;
                }
            }
        }
    }
}
