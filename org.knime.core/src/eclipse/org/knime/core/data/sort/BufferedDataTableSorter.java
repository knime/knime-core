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
import org.knime.core.data.DataType;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.container.DataContainerSettings;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;

/**
 * Table sorter for sorting {@link BufferedDataTable} objects. The returned table is also of class
 * {@link BufferedDataTable}. In comparison to {@link DataTableSorter} this class will take advantage of certain flow
 * properties when dealing with table objects, for instance it will respect the node's memory policy or better handle
 * blob objects (which will be referenced in case of {@link BufferedDataTable} but copied for any other type of table).
 *
 * <p>
 * Usage: Client implementations will initialize this object with the table to be sorted, set properties using the
 * varies set-methods (defaults are generally fine) and finally call the {@link #sort(ExecutionContext)} method.
 *
 * <p>
 * For details on the sorting mechanism see the <a href="package.html"> package description</a>.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class BufferedDataTableSorter extends AbstractTableSorter {

    /**
     * Inits table sorter using the sorting according to {@link #setSortColumns(Collection, boolean[])}.
     *
     * @param inputTable The table to sort
     * @param inclList Passed on to {@link #setSortColumns(Collection, boolean[])}.
     * @param sortAscending Passed on to {@link #setSortColumns(Collection, boolean[])}.
     * @throws NullPointerException If any argument is null.
     * @throws IllegalArgumentException If arguments are inconsistent.
     */
    public BufferedDataTableSorter(final BufferedDataTable inputTable, final Collection<String> inclList,
            final boolean[] sortAscending) {
        super(inputTable, inputTable.size(), inclList, sortAscending);
    }

    /**
     * Inits table sorter using the sorting according to {@link #setSortColumns(Collection, boolean[], boolean)}.
     *
     * @param inputTable The table to sort
     * @param inclList Passed on to {@link #setSortColumns(Collection, boolean[], boolean)}.
     * @param sortAscending Passed on to {@link #setSortColumns(Collection, boolean[], boolean)}.
     * @param sortMissingsToEnd Passed on to {@link #setSortColumns(Collection, boolean[], boolean)}.
     * @throws NullPointerException If any argument is null.
     * @throws IllegalArgumentException If arguments are inconsistent.
     * @since 2.6
     */
    public BufferedDataTableSorter(final BufferedDataTable inputTable, final Collection<String> inclList,
        final boolean[] sortAscending, final boolean sortMissingsToEnd) {
        super(inputTable, inputTable.size(), inclList, sortAscending, sortMissingsToEnd);
    }

    /**
     * Inits sorter on argument table with given row comparator.
     *
     * @param inputTable Table to sort.
     * @param rowComparator Passed to {@link #setRowComparator(Comparator)}.
     */
    public BufferedDataTableSorter(final BufferedDataTable inputTable, final Comparator<DataRow> rowComparator) {
        super(inputTable, inputTable.size(), rowComparator);
    }

    /**
     * Package default constructor for the {@link AbstractColumnTableSorter}.
     *
     * @param rowCount the row count
     * @param dataTableSpec the data table spec
     * @param rowComparator the row comparator
     * @param ctx for creating the buffered data table instances
     */
    BufferedDataTableSorter(final long rowCount, final DataTableSpec dataTableSpec,
        final Comparator<DataRow> rowComparator, final ExecutionContext ctx) {
        super(rowCount, dataTableSpec, rowComparator);
    }

    private static Comparator<DataRow> getComparatorFromType(final DataType type, final int index,
        final boolean sortAscending) {
        var comparator = type.getComparator();
        return (final DataRow o1, final DataRow o2) -> {
            final var cell1 = o1.getCell(index);
            final var cell2 = o2.getCell(index);
            if (sortAscending) {
                return comparator.compare(cell1, cell2);
            } else {
                return -1 * comparator.compare(cell1, cell2);
            }
        };
    }

    /**
     * Utility method to sort a given data table.
     *
     * @param table to be sorted
     * @param columnIndex index of the column in the table to be used for the sorting algorithm
     * @param sortAscending sort order
     * @param exec {@link ExecutionContext} where the sorting operation will be performed
     * @return a {@link BufferedDataTable} with the same columns as the given by parameter but sorted according to the
     *         column given
     * @throws CanceledExecutionException If execution context cancelled.
     * @since 5.1
     */
    public static BufferedDataTable sortTable(final BufferedDataTable table, final int columnIndex,
        final boolean sortAscending, final ExecutionContext exec) throws CanceledExecutionException {
        var type = table.getSpec().getColumnSpec(columnIndex).getType();
        var comparator = getComparatorFromType(type, columnIndex, sortAscending);
        return new BufferedDataTableSorter(table, comparator).sort(exec);
    }

    /**
     * Utility method to sort a given data table in ascending order for the column at the given index.
     *
     * @param table to be sorted
     * @param columnIndex index of the column in the table to be used for the sorting algorithm
     * @param exec {@link ExecutionContext} where the sorting operation will be performed
     * @return a {@link BufferedDataTable} with the same columns as the given by parameter but sorted according to the
     *         column given
     * @throws CanceledExecutionException If execution context cancelled.
     * @since 5.1
     */
    public static BufferedDataTable sortTable(final BufferedDataTable table, final int columnIndex,
        final ExecutionContext exec) throws CanceledExecutionException {
        return sortTable(table, columnIndex, true, exec);
    }

    /**
     * Sorts the table passed in the constructor according to the settings and returns the sorted output table.
     *
     * @param ctx To report progress &amp; create temporary and final output tables.
     * @return The sorted output.
     * @throws CanceledExecutionException If canceled.
     */
    public BufferedDataTable sort(final ExecutionContext ctx) throws CanceledExecutionException {
        if (ctx == null) {
            throw new NullPointerException("Argument must not be null.");
        }

        return (BufferedDataTable)super.sortInternal(ctx, createTableIOHandler(ctx));
    }

    /**
     * Sorts the table passed in the constructor according to the settings and returns a resource iterator over the
     * resulting table.
     *
     * @param ctx To report progress &amp; create temporary output tables
     * @return The sorted output as an iterator
     * @throws CanceledExecutionException If canceled
     * @since 5.2
     */
    public CloseableRowIterator sortedIterator(final ExecutionContext ctx) throws CanceledExecutionException {
        return super.sortedIteratorInternal(CheckUtils.checkArgumentNotNull(ctx), createTableIOHandler(ctx));
    }

    static TableIOHandler createTableIOHandler(final ExecutionContext ctx) {
        return new BufferedDataTableSorterTableIOHandler(ctx);
    }

    private static final class BufferedDataTableSorterTableIOHandler implements TableIOHandler {
        private final ExecutionContext m_ctx;

        BufferedDataTableSorterTableIOHandler(final ExecutionContext ctx) {
            m_ctx = ctx;
        }

        @Override
        public DataContainer createDataContainer(final DataTableSpec spec, final boolean forceOnDisk) {
            final var containerSettings = DataContainerSettings.internalBuilder()//
                .withInitializedDomain(true) // copy domain
                .withDomainUpdate(false) // no new values are added
                .withCheckDuplicateRowKeys(false) // no new row keys are added
                .withMaxCellsInMemory(forceOnDisk ? 0 : -1) // no cells in memory -> immediately write to disk
                .build();
            return m_ctx.createDataContainer(spec, containerSettings);
        }

        @Override
        public void clearTable(final DataTable table) {
            if (table instanceof BufferedDataTable bdt) {
                m_ctx.clearTable(bdt);
            } else {
                NodeLogger.getLogger(getClass()).warnWithFormat(
                    "Can't clear table instance of \"%s\" - expected \"%s\"", table.getClass().getSimpleName(),
                    BufferedDataTable.class.getSimpleName());
            }
        }
    }
}
