/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Sep 2, 2011 (wiswedel): created
 */
package org.knime.core.data.sort;

import java.util.Collection;
import java.util.Comparator;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.container.CloseableTable;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.container.DataContainerSettings;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;

/**
 * Table sorter for sorting plain {@link DataTable} objects (use the class <b>{@link BufferedDataTableSorter}</b> if
 * you want to sort buffered data table objects.
 *
 * <p>Usage: Client implementations will initialize this object with the table to be sorted, set properties using the
 * varies set-methods (defaults are generally fine) and finally call the {@link #sort(ExecutionMonitor)} method.
 *
 * <p>For details on the sorting mechanism see the <a href="package.html">package description</a>.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class DataTableSorter extends AbstractTableSorter {

    static final TableIOHandler TABLE_IO_HANDLER = new DataTableSorterTableIOHandler();

    private static final class DataTableSorterTableIOHandler implements TableIOHandler {
        @Override
        public DataContainer createDataContainer(final DataTableSpec spec, final boolean forceOnDisk) {
            DataContainerSettings settings = DataContainerSettings.internalBuilder() //
                .withInitializedDomain(true) // copy domain from input table
                .withDomainUpdate(false) // no new values are added
                .withCheckDuplicateRowKeys(false) // no new row keys are added
                .withMaxCellsInMemory(forceOnDisk ? 0 : -1) // 0 cells in memory -> immediately write to disk
                .build();
            return new DataContainer(spec, settings);
        }

        @Override
        public void clearTable(final DataTable table) {
            // the DataContainer returns ContainerTable
            if (table instanceof ContainerTable ct) {
                ct.clear();
            } else {
                NodeLogger.getLogger(getClass())
                    .warn("Can't clear table instance " + "of \"" + table.getClass().getSimpleName()
                        + "\" - expected \"" + ContainerTable.class.getSimpleName() + "\"");
            }
        }
    }

    /**
     * Inits sorter on argument table with given row comparator.
     *
     * @param inputTable Table to sort.
     * @param rowsCount The number of rows in the table, if known. Specify -1 if you don't know the table row count.
     *            This field is only used to report progress upon {@link #sort(ExecutionMonitor)}.
     * @param rowComparator Passed to {@link #setRowComparator(Comparator)}.
     * @deprecated use {@link #DataTableSorter(DataTable, long, Comparator)} instead which supports more than
     *             {@link Integer#MAX_VALUE} rows
     */
    @Deprecated(since = "3.0", forRemoval = true)
    public DataTableSorter(final DataTable inputTable, final int rowsCount, final Comparator<DataRow> rowComparator) {
        super(inputTable, rowsCount, rowComparator);
    }

    /**
     * Inits table sorter using the sorting according to {@link #setSortColumns(Collection, boolean[])}.
     *
     * @param inputTable The table to sort
     * @param rowsCount The number of rows in the table, if known. Specify -1 if you don't know the table row count.
     *            This field is only used to report progress upon {@link #sort(ExecutionMonitor)}.
     * @param inclList Passed on to {@link #setSortColumns(Collection, boolean[])}.
     * @param sortAscending Passed on to {@link #setSortColumns(Collection, boolean[])}.
     * @throws NullPointerException If any argument is null.
     * @throws IllegalArgumentException If arguments are inconsistent.
     * @deprecated use {@link #DataTableSorter(DataTable, int, Collection, boolean[])} instead which supports more than
     *             {@link Integer#MAX_VALUE} rows
     */
    @Deprecated(since = "3.0", forRemoval = true)
    public DataTableSorter(final DataTable inputTable, final int rowsCount, final Collection<String> inclList,
            final boolean[] sortAscending) {
        super(inputTable, rowsCount, inclList, sortAscending);
    }

    /**
     * Inits table sorter using the sorting according to {@link #setSortColumns(Collection, boolean[], boolean)}.
     *
     * @param inputTable The table to sort
     * @param rowsCount The number of rows in the table, if known. Specify -1 if you don't know the table row count.
     *            This field is only used to report progress upon {@link #sort(ExecutionMonitor)}.
     * @param inclList Passed on to {@link #setSortColumns(Collection, boolean[], boolean)}.
     * @param sortAscending Passed on to {@link #setSortColumns(Collection, boolean[], boolean)}.
     * @param sortMissingToEnd Passed on to {@link #setSortColumns(Collection, boolean[], boolean)}
     * @throws NullPointerException If any argument is null.
     * @throws IllegalArgumentException If arguments are inconsistent.
     * @since 2.6
     * @deprecated use {@link #DataTableSorter(DataTable, long, Collection, boolean[], boolean)} instead which supports
     *             more than {@link Integer#MAX_VALUE} rows
     */
    @Deprecated(since = "3.0", forRemoval = true)
    public DataTableSorter(final DataTable inputTable, final int rowsCount, final Collection<String> inclList,
            final boolean[] sortAscending, final boolean sortMissingToEnd) {
        super(inputTable, rowsCount, inclList, sortAscending);
    }



    /**
     * Inits sorter on argument table with given row comparator.
     *
     * @param inputTable Table to sort.
     * @param rowsCount The number of rows in the table, if known. Specify -1 if you don't know the table row count.
     *            This field is only used to report progress upon {@link #sort(ExecutionMonitor)}.
     * @param rowComparator Passed to {@link #setRowComparator(Comparator)}.
     * @since 3.0
     */
    public DataTableSorter(final DataTable inputTable, final long rowsCount, final Comparator<DataRow> rowComparator) {
        super(inputTable, rowsCount, rowComparator);
    }

    /**
     * Inits table sorter using the sorting according to {@link #setSortColumns(Collection, boolean[])}.
     *
     * @param inputTable The table to sort
     * @param rowsCount The number of rows in the table, if known. Specify -1 if you don't know the table row count.
     *            This field is only used to report progress upon {@link #sort(ExecutionMonitor)}.
     * @param inclList Passed on to {@link #setSortColumns(Collection, boolean[])}.
     * @param sortAscending Passed on to {@link #setSortColumns(Collection, boolean[])}.
     * @throws NullPointerException If any argument is null.
     * @throws IllegalArgumentException If arguments are inconsistent.
     * @since 3.0
     */
    public DataTableSorter(final DataTable inputTable, final long rowsCount, final Collection<String> inclList,
            final boolean[] sortAscending) {
        super(inputTable, rowsCount, inclList, sortAscending);
    }

    /**
     * Inits table sorter using the sorting according to {@link #setSortColumns(Collection, boolean[], boolean)}.
     *
     * @param inputTable The table to sort
     * @param rowsCount The number of rows in the table, if known. Specify -1 if you don't know the table row count.
     *            This field is only used to report progress upon {@link #sort(ExecutionMonitor)}.
     * @param inclList Passed on to {@link #setSortColumns(Collection, boolean[], boolean)}.
     * @param sortAscending Passed on to {@link #setSortColumns(Collection, boolean[], boolean)}.
     * @param sortMissingToEnd Passed on to {@link #setSortColumns(Collection, boolean[], boolean)}
     * @throws NullPointerException If any argument is null.
     * @throws IllegalArgumentException If arguments are inconsistent.
     * @since 3.0
     */
    public DataTableSorter(final DataTable inputTable, final long rowsCount, final Collection<String> inclList,
            final boolean[] sortAscending, final boolean sortMissingToEnd) {
        super(inputTable, rowsCount, inclList, sortAscending);
    }

    /**
     * Package default constructor for the {@link AbstractColumnTableSorter}.
     *
     * @param rowCount the row count
     * @param dataTableSpec the data table spec
     * @param rowComparator the row comparator
     */
    DataTableSorter(final long rowCount, final DataTableSpec dataTableSpec, final Comparator<DataRow> rowComparator) {
        super(rowCount, dataTableSpec, rowComparator);
    }

    /**
     * Sorts the table passed in the constructor according to the settings and returns the sorted output table.
     *
     * @param exec To report progress.
     * @return The sorted output.
     * @throws CanceledExecutionException If canceled.
     * @since 4.2
     */
    public CloseableTable sort(final ExecutionMonitor exec) throws CanceledExecutionException {
        DataTable delegate = super.sortInternal(exec, TABLE_IO_HANDLER);

        // only allow closing this delegate if it's not the input table.
        // FIXME Why does the DataTableSorter need to understand that it shouldn't be able to close a table?
        //       That's state logic which should be caught in the design.
        if (delegate != getInputTable() && delegate instanceof CloseableTable closeable) {
            return closeable;
        } else {
            return new CloseableTable() {
                @Override
                public void close() {
                    // we don't know how to close this guy.
                }

                @Override
                public RowIterator iterator() {
                    return delegate.iterator();
                }

                @Override
                public DataTableSpec getDataTableSpec() {
                    return delegate.getDataTableSpec();
                }
            };

        }
    }

    /**
     * Creates data container, either a buffered data container or a plain one.
     *
     * @param spec The spec of the container/table.
     * @param forceOnDisk false to use default, true to flush data immediately to disk. It's true when used in the
     *            #sortOnDisk(ExecutionMonitor) method and the container is only used temporarily.
     * @return A new fresh container.
     */
    @Deprecated(since="5.2", forRemoval = true)
    protected DataContainer createDataContainer(final DataTableSpec spec, final boolean forceOnDisk) {
        if (forceOnDisk) {
            return new DataContainer(spec, true, 0);
        } else {
            return new DataContainer(spec, true);
        }
    }
}
