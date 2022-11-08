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
 *   Nov 26, 2021 (hornm): created
 */
package org.knime.core.webui.node.view.table.data;

import java.lang.ref.Cleaner;
import java.lang.ref.Cleaner.Cleanable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.container.filter.TableFilter;
import org.knime.core.data.sort.DataTableSorter;
import org.knime.core.data.sort.RowComparator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.util.Pair;
import org.knime.core.webui.data.DataServiceContext;
import org.knime.core.webui.data.DataServiceException;
import org.knime.core.webui.node.view.table.data.render.DataValueImageRenderer;
import org.knime.core.webui.node.view.table.data.render.DataValueImageRendererRegistry;
import org.knime.core.webui.node.view.table.data.render.DataValueRenderer;
import org.knime.core.webui.node.view.table.data.render.DataValueRendererFactory;
import org.knime.core.webui.node.view.table.data.render.DataValueTextRenderer;

/**
 * @author Konrad Amtenbrink, KNIME GmbH, Berlin, Germany
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public class TableViewDataServiceImpl implements TableViewDataService {

    private final Supplier<BufferedDataTable> m_tableSupplier;

    private ContainerTable m_cachedFilteredAndSortedTable;

    private String m_cachedGlobalSearchTerm;

    private String[] m_cachedColumns;

    private String[][] m_cachedColumnFilterValue;

    private ContainerTable m_cachedSortedTable;

    private boolean m_cachedTableSortAscending;

    private String m_cachedTableSortColumnName;

    private static final Cleaner CLEANER = Cleaner.create();

    private Cleanable m_cleanable;

    private final DataValueImageRendererRegistry m_rendererRegistry;

    private final DataValueRendererFactory m_rendererFactory;

    /*
     * Caches data value renderers for the key [column-name, renderer-id].
     * Allows one to re-use the renderers and to not create them over and over again (e.g. when paging the table).
     */
    private final Map<Pair<String, String>, DataValueRenderer> m_renderersMap = new HashMap<>();

    private final String m_tableId;

    /**
     * @param tableSupplier supplier for the table from which to obtain data
     * @param tableId TODO
     * @param rendererFactory required to turn data values into text or images
     * @param rendererRegistry lazily supplied image content for cells that are rendered into images (cleared and filled
     *            whenever new rows are being requested, e.g., via
     *            {@link #getTable(String[], long, int, String[], boolean, boolean)})
     */
    public TableViewDataServiceImpl(final Supplier<BufferedDataTable> tableSupplier, final String tableId,
        final DataValueRendererFactory rendererFactory, final DataValueImageRendererRegistry rendererRegistry) {
        Objects.requireNonNull(tableSupplier, () -> "Table supplier must not be null.");
        m_tableSupplier = tableSupplier;
        m_tableId = tableId;
        m_rendererFactory = rendererFactory;
        m_rendererRegistry = rendererRegistry;
    }

    @Override
    public Table getTable(final String[] columns, final long fromIndex, final int numRows, final String[] rendererIds,
        final boolean updateDisplayedColumns, final boolean withAllRowKeys) {
        return getFilteredAndSortedTable(columns, fromIndex, numRows, null, false, null, null, false, rendererIds,
            updateDisplayedColumns, withAllRowKeys);
    }

    @Override
    public Table getFilteredAndSortedTable(final String[] columns, final long fromIndex, final int numRows,
        final String sortColumn, final boolean sortAscending, final String globalSearchTerm,
        final String[][] columnFilterValue, final boolean filterRowKeys, final String[] rendererIdsParam,
        final boolean updateDisplayedColumns, final boolean withAllRowKeys) {
        var bufferedDataTable = m_tableSupplier.get();
        if (bufferedDataTable == null) {
            return createEmptyTable();
        }

        final var displayedColumns =
            updateDisplayedColumns ? filterInvalids(columns, bufferedDataTable.getSpec()) : columns;

        // we sort first (even though it is more expensive) because filtering happens more frequently
        // and therefore we do not have to re-sort every time we filter
        DataTable sortedTable = getOrCreateSortedTable(bufferedDataTable, sortColumn, sortAscending);
        DataTable filteredAndSortedTable =
            getOrCreateFilteredTable(sortedTable, displayedColumns, globalSearchTerm, columnFilterValue, filterRowKeys);

        final var spec = bufferedDataTable.getSpec();
        final var colIndices = spec.columnsToIndices(displayedColumns);
        if (m_rendererRegistry != null) {
            m_rendererRegistry.clear(m_tableId);
        }
        var rendererIds = rendererIdsParam == null || rendererIdsParam.length == 0 ? new String[displayedColumns.length]
            : rendererIdsParam;
        updateRenderersMap(spec, displayedColumns, rendererIds);

        // TODO remove this and use execution context, if UIEXT-243 is implemented
        long tableSize;
        if (filteredAndSortedTable instanceof ContainerTable) {
            tableSize = ((ContainerTable)filteredAndSortedTable).size();
        } else {
            tableSize = ((BufferedDataTable)filteredAndSortedTable).size();
        }
        final var toIndex = Math.min(fromIndex + numRows, tableSize) - 1;
        final var size = (int)(toIndex - fromIndex + 1);

        final String[][] rows;
        if (size > 0) {
            final var filter = new TableFilter.Builder();
            filter.withFromRowIndex(fromIndex); // will throw exception when fromIndex < 0
            filter.withToRowIndex(toIndex); // will throw exception when toIndex < fromIndex
            filter.withMaterializeColumnIndices(colIndices);
            rows = renderRows(displayedColumns, colIndices, rendererIds, size,
                createRowIteratorSupplier(filteredAndSortedTable, filter.build()), m_rendererRegistry, m_renderersMap,
                m_tableId);
        } else {
            rows = new String[0][];
        }
        final var rowKeys = withAllRowKeys ? getAllRowKeys(filteredAndSortedTable, (int)tableSize) : null;
        final var tableSpec = bufferedDataTable.getDataTableSpec();
        final var contentTypes = getColumnContentTypes(displayedColumns, rendererIds, m_renderersMap);
        final var dataTypeIds = getColumnDataTypeIds(displayedColumns, tableSpec);
        return createTable(displayedColumns, contentTypes, dataTypeIds, rows, tableSize, rowKeys);
    }

    private static String[] filterInvalids(final String[] columns, final DataTableSpec spec) {
        final var dataServiceContext = DataServiceContext.getContext();

        final var partition = Stream.of(columns).collect(Collectors.partitioningBy(spec::containsName));

        final var invalidCols = partition.get(false);

        if (!invalidCols.isEmpty()) {
            dataServiceContext.addWarningMessage(String.format("The selected column%s not present in the table.",
                String.format(invalidCols.size() == 1 ? " %s is" : "s %s are", String.join(", ", invalidCols))));
        }
        return partition.get(true).stream().toArray(String[]::new);
    }

    private DataTable getOrCreateSortedTable(final BufferedDataTable table, final String sortColumn,
        final boolean sortAscending) {
        if ((sortColumn != null && sortColumn.equals(m_cachedTableSortColumnName)) //
            && sortAscending == m_cachedTableSortAscending && m_cachedSortedTable != null) {
            return m_cachedSortedTable;
        }
        if (table.size() <= 1) {
            return table;
        }

        if (m_cleanable != null) {
            m_cleanable.clean();
            m_cleanable = null;
        }
        m_cachedTableSortColumnName = sortColumn;
        m_cachedTableSortAscending = sortAscending;
        if (sortColumn != null) {
            final var sortColIndex = table.getSpec().findColumnIndex(sortColumn);
            final Comparator<DataRow> comp =
                new RowComparator(new int[]{sortColIndex}, new boolean[]{sortAscending}, false, table.getSpec());
            try {
                m_cachedSortedTable =
                    (ContainerTable)new DataTableSorter(table, table.size(), comp).sort(new ExecutionMonitor());
            } catch (CanceledExecutionException e) {
                throw new DataServiceException("Table sorting has been cancelled", e);
            }
            m_cleanable = CLEANER.register(this, m_cachedSortedTable::close);
            m_cachedFilteredAndSortedTable = null;
            return m_cachedSortedTable;
        } else {
            if (m_cachedSortedTable != null) {
                m_cachedFilteredAndSortedTable = null;
            }
            m_cachedSortedTable = null;
            return table;
        }
    }

    private DataTable getOrCreateFilteredTable(final DataTable table, final String[] columns,
        final String globalSearchTerm, final String[][] columnFilterValue, final boolean filterRowKeys) {
        final var globalSearchTermsEqual =
            globalSearchTerm != null && globalSearchTerm.equals(m_cachedGlobalSearchTerm);

        final var columnFilterValuesEqual =
            columnFilterValue != null && Arrays.deepEquals(m_cachedColumnFilterValue, columnFilterValue);

        final var columnsEqual = Arrays.deepEquals(m_cachedColumns, columns);

        if (globalSearchTermsEqual && columnFilterValuesEqual && columnsEqual
            && m_cachedFilteredAndSortedTable != null) {
            return m_cachedFilteredAndSortedTable;
        }

        m_cachedGlobalSearchTerm = globalSearchTerm;
        m_cachedColumnFilterValue = columnFilterValue;
        m_cachedColumns = columns;

        if (globalSearchTerm == null && columnFilterValue == null) {
            return table;
        }

        final var spec = table.getDataTableSpec();
        final var colIndices = spec.columnsToIndices(columns);

        var resultContainer = new DataContainer(spec, true);

        // column offset due to rowKey
        final var columnOffset = 1;
        final var numColumns = columns.length + columnOffset;

        try (final var iterator = (CloseableRowIterator)table.iterator()) {
            while (iterator.hasNext()) {
                final var row = iterator.next();
                if (filtersMatch(row, spec, globalSearchTerm, columnFilterValue, colIndices, numColumns, columnOffset,
                    filterRowKeys)) {
                    resultContainer.addRowToTable(row);
                }
            }
        }

        resultContainer.close();
        m_cachedFilteredAndSortedTable = (ContainerTable)resultContainer.getTable();
        return m_cachedFilteredAndSortedTable;
    }

    private static String[] getAllRowKeys(final DataTable table, final int size) {
        final var rowKeys = new String[size];
        try (final var iterator = (CloseableRowIterator)table.iterator()) {
            for (int i = 0; i < size; i++) {
                final var row = iterator.next();
                rowKeys[i] = row.getKey().toString();
            }
        }
        return rowKeys;
    }

    @SuppressWarnings("java:S107") // accept the large number of parameters
    private static boolean filtersMatch(final DataRow row, final DataTableSpec spec, final String globalSearchTerm,
        final String[][] columnFilterValue, final int[] colIndices, final int numColumns, final int columnOffset,
        final boolean filterRowKeys) {
        var colFilterMatch = true;
        var globalMatch = false;
        final var startingIndex = filterRowKeys ? 0 : 1;
        for (var currentIndex = startingIndex; currentIndex < numColumns
            && (!globalMatch || colFilterMatch); currentIndex++) {
            final var isRowKey = currentIndex < columnOffset;

            final String cellStringValue;
            if (isRowKey) {
                cellStringValue = row.getKey().toString().toLowerCase();
            } else {
                final var cell = row.getCell(colIndices[currentIndex - columnOffset]);
                cellStringValue = cell.toString().toLowerCase();
            }

            if (globalSearchTerm == null || cellStringValue.contains(globalSearchTerm.toLowerCase())) {
                globalMatch = true;
            }
            if (columnFilterValue == null || columnFilterValue.length < 1) {
                continue;
            }

            final var currentColumnFilters = columnFilterValue[currentIndex];

            // if the domain values exists we want an exact match, otherwise we
            // just check if the cell value matches the search term
            final var needsExactMatch =
                isRowKey ? false : (spec.getColumnSpec(currentIndex - columnOffset).getDomain().getValues() != null);

            final var currentFilterMatch =
                currentColumnFilters.length == 0 || Arrays.stream(currentColumnFilters).map(String::toLowerCase)
                    .anyMatch(needsExactMatch ? cellStringValue::equals : cellStringValue::contains);
            if (!currentFilterMatch) {
                colFilterMatch = false;
            }
        }
        return colFilterMatch && globalMatch;
    }

    private static String[][] renderRows(final String[] columns, final int[] colIndices, final String[] rendererIds,
        final int size, final Supplier<CloseableRowIterator> rowIteratorSupplier,
        final DataValueImageRendererRegistry rendererRegistry,
        final Map<Pair<String, String>, DataValueRenderer> renderersMap, final String tableId) {
        var renderers = IntStream.range(0, columns.length)
            .mapToObj(i -> renderersMap.get(Pair.create(columns[i], rendererIds[i]))).toArray(DataValueRenderer[]::new);
        final var rows = new String[size][];
        try (final var iterator = rowIteratorSupplier.get()) {
            IntStream.range(0, size).forEach(index -> {
                final var row = iterator.next();
                rows[index] = Stream.concat(Stream.of(row.getKey().toString()), //
                    IntStream.range(0, columns.length) //
                        .mapToObj(i -> {
                            var cell = row.getCell(colIndices[i]);
                            return cell.isMissing() ? null : renderCell(cell, renderers[i], rendererRegistry, tableId);
                        }))
                    .toArray(String[]::new);
            });
        }
        return rows;
    }

    private static String renderCell(final DataCell cell, final DataValueRenderer renderer,
        final DataValueImageRendererRegistry rendererRegistry, final String tableId) {
        if (renderer instanceof DataValueTextRenderer) {
            return ((DataValueTextRenderer)renderer).renderText(cell);
        } else if (renderer instanceof DataValueImageRenderer) {
            return rendererRegistry.addRendererAndGetImgPath(tableId, cell, (DataValueImageRenderer)renderer);
        } else {
            throw new UnsupportedOperationException(
                "Unsupported data value renderer: " + renderer.getClass().getName());
        }
    }

    private void updateRenderersMap(final DataTableSpec spec, final String[] columns, final String[] rendererIds) {
        for (var i = 0; i < columns.length; i++) {
            var key = Pair.create(columns[i], rendererIds[i]);
            m_renderersMap.computeIfAbsent(key,
                k -> m_rendererFactory.createDataValueRenderer(spec.getColumnSpec(k.getFirst()), k.getSecond()));
        }
    }

    private static String[] getColumnContentTypes(final String[] columns, final String[] rendererIds,
        final Map<Pair<String, String>, DataValueRenderer> renderersMap) {
        return IntStream.range(0, columns.length)
            .mapToObj(i -> renderersMap.get(Pair.create(columns[i], rendererIds[i])).getContentType().toString())
            .toArray(String[]::new);
    }

    private static String[] getColumnDataTypeIds(final String[] columns, final DataTableSpec spec) {
        return Arrays.stream(columns).map(c -> String.valueOf(spec.getColumnSpec(c).getType().hashCode()))
            .toArray(String[]::new);
    }

    private static Table createEmptyTable() {
        return createTable(new String[0], new String[0], new String[0], new String[0][], 0, new String[0]);
    }

    private static Table createTable(final String[] displayedColumns, final String[] contentTypes,
        final String[] columnDataTypeIds, final String[][] rows, final long rowCount, final String[] rowKeys) {
        return new Table() {

            @Override
            public String[] getDisplayedColumns() {
                return displayedColumns;
            }

            @Override
            public String[] getColumnContentTypes() {
                return contentTypes;
            }

            @Override
            public String[] getColumnDataTypeIds() {
                return columnDataTypeIds;
            }

            @Override
            public String[][] getRows() {
                return rows;
            }

            @Override
            public long getRowCount() {
                return rowCount;
            }

            @Override
            public String[] getRowKeys() {
                return rowKeys;
            }

        };
    }

    // TODO remove this and use execution context, if UIEXT-243 is implemented
    private static Supplier<CloseableRowIterator> createRowIteratorSupplier(final DataTable table,
        final TableFilter filter) {
        Supplier<CloseableRowIterator> rowIteratorSupplier;
        if (table instanceof ContainerTable) {
            rowIteratorSupplier = () -> ((ContainerTable)table).iteratorWithFilter(filter);
        } else {
            rowIteratorSupplier = () -> ((BufferedDataTable)table).filter(filter).iterator();
        }
        return rowIteratorSupplier;
    }

}
