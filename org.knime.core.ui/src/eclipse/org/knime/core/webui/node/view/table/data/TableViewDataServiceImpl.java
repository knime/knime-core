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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.container.filter.TableFilter;
import org.knime.core.data.def.StringCell;
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
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class TableViewDataServiceImpl implements TableViewDataService {

    private final Supplier<BufferedDataTable> m_tableSupplier;

    private final TableCache m_sortedTableCache = new TableCache();

    private final TableCache m_filteredAndSortedTableCache = new TableCache();

    private static final Cleaner CLEANER = Cleaner.create();

    private final DataValueImageRendererRegistry m_rendererRegistry;

    private final DataValueRendererFactory m_rendererFactory;

    /*
     * Caches data value renderers for the key [column-name, renderer-id].
     * Allows one to re-use the renderers and to not create them over and over again (e.g. when paging the table).
     */
    private final Map<Pair<String, String>, DataValueRenderer> m_renderersMap = new HashMap<>();

    private final String m_tableId;

    private final Supplier<Set<RowKey>> m_selectionSupplier;

    /**
     * @param tableSupplier supplier for the table from which to obtain data
     * @param tableId a globally unique id; used to uniquely identify images in the renderer-registry which belong to
     *            the table supplied here
     * @param rendererFactory required to turn data values into text or images
     * @param rendererRegistry lazily supplied image content for cells that are rendered into images (cleared and filled
     *            whenever new rows are being requested, e.g., via
     *            {@link #getTable(String[], long, int, String[], boolean, boolean)}
     */
    public TableViewDataServiceImpl(final Supplier<BufferedDataTable> tableSupplier, final String tableId,
        final DataValueRendererFactory rendererFactory, final DataValueImageRendererRegistry rendererRegistry) {
        Objects.requireNonNull(tableSupplier, () -> "Table supplier must not be null.");
        m_tableSupplier = tableSupplier;
        m_tableId = tableId;
        m_rendererFactory = rendererFactory;
        m_rendererRegistry = rendererRegistry;
        m_selectionSupplier = null;
        CLEANER.register(this, () -> { // NOSONAR exposing a partially constructed instance is no problem here
            m_sortedTableCache.clear();             // because it's not really used (just to determine whether 'this' is phantom-reachable)
            m_filteredAndSortedTableCache.clear();
        });
    }

    /**
     * @param tableSupplier supplier for the table from which to obtain data
     * @param selectionSupplier provides the currently selected rows, can be {@code null}
     * @param tableId a globally unique id; used to uniquely identify images in the renderer-registry which belong to
     *            the table supplied here
     * @param rendererFactory required to turn data values into text or images
     * @param rendererRegistry lazily supplied image content for cells that are rendered into images (cleared and filled
     *            whenever new rows are being requested, e.g., via
     *            {@link #getTable(String[], long, int, String[], boolean, boolean)})
     */
    public TableViewDataServiceImpl(final Supplier<BufferedDataTable> tableSupplier,
        final Supplier<Set<RowKey>> selectionSupplier, final String tableId,
        final DataValueRendererFactory rendererFactory, final DataValueImageRendererRegistry rendererRegistry) {
        m_selectionSupplier = selectionSupplier;
        Objects.requireNonNull(tableSupplier, () -> "Table supplier must not be null.");
        m_tableSupplier = tableSupplier;
        m_tableId = tableId;
        m_rendererFactory = rendererFactory;
        m_rendererRegistry = rendererRegistry;
        CLEANER.register(this, () -> { // NOSONAR exposing a partially constructed instance is no problem here
            m_sortedTableCache.clear();             // because it's not really used (just to determine whether 'this' is phantom-reachable)
            m_filteredAndSortedTableCache.clear();
        });
    }

    @Override
    public Table getTable(final String[] columns, final long fromIndex, final int numRows, final String[] rendererIds,
        final boolean updateDisplayedColumns, final boolean forceClearImageDataCache) {
        return getFilteredAndSortedTable(columns, fromIndex, numRows, null, false, null, null, false, rendererIds,
            updateDisplayedColumns, false, forceClearImageDataCache);
    }

    @Override
    public Table getFilteredAndSortedTable(final String[] columns, final long fromIndex, final int numRows,
        final String sortColumn, final boolean sortAscending, final String globalSearchTerm,
        final String[][] columnFilterValue, final boolean filterRowKeys, final String[] rendererIdsParam,
        final boolean updateDisplayedColumns, final boolean updateTotalSelected,
        final boolean forceClearImageDataCache) {
        var bufferedDataTable = m_tableSupplier.get();
        if (bufferedDataTable == null) {
            return createEmptyTable();
        }

        final var displayedColumns =
            updateDisplayedColumns ? filterInvalids(columns, bufferedDataTable.getSpec()) : columns;

        // we sort first (even though it is more expensive) because filtering happens more frequently
        // and therefore we do not have to re-sort every time we filter
        m_sortedTableCache.conditionallyUpdateCachedTable(() -> sortTable(bufferedDataTable, sortColumn, sortAscending),
            sortColumn == null || bufferedDataTable.size() <= 1, sortColumn, sortAscending);
        final var sortedTable = m_sortedTableCache.getCachedTable().orElseGet(() -> bufferedDataTable);
        m_filteredAndSortedTableCache.conditionallyUpdateCachedTable(
            () -> filterTable(sortedTable, columns, globalSearchTerm, columnFilterValue, filterRowKeys),
            globalSearchTerm == null && columnFilterValue == null, globalSearchTerm, columnFilterValue, columns,
            sortColumn, sortAscending);
        final var filteredAndSortedTable = m_filteredAndSortedTableCache.getCachedTable().orElseGet(() -> sortedTable);

        final var spec = bufferedDataTable.getSpec();
        final var colIndices = spec.columnsToIndices(displayedColumns);
        if (m_rendererRegistry != null) {
            if (forceClearImageDataCache || m_sortedTableCache.wasUpdated() || m_filteredAndSortedTableCache.wasUpdated()) {
                // Clears the image data cache if it's forced to be cleared. That's usually done when 'pagination' is enabled because in that
                // case a new batch of rows is request with every page change and the is no need to keep the older one.
                // If, however, 'pagination' is disabled (i.e. 'infinite scrolling' is used instead), then it's almost certain
                // that images of two different consecutive(!) 'row-batches' are being requested at the same time. I.e. we must
                // _not_ clear previous row-batches too early.
                // However, we _can_ clear the image data cache if 'pagination' is disabled or if the entire table is being
                // updated/replaced (e.g. because it's sorted or filtered). Because images of row-batches of different tables
                // won't be requested at the same time (e.g. a row-batch of the sorted table won't be displayed together with a
                // row-batch of the un-sorted table).
                m_rendererRegistry.clearImageDataCache(m_tableId);
            }
            if (numRows > 0) {
                m_rendererRegistry.startNewBatchOfTableRows(m_tableId);
            }
        }
        var rendererIds = rendererIdsParam == null || rendererIdsParam.length == 0 ? new String[displayedColumns.length]
            : rendererIdsParam;
        updateRenderersMap(spec, displayedColumns, rendererIds);

        final var tableSize = getTableSize(filteredAndSortedTable);
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
        final var tableSpec = bufferedDataTable.getDataTableSpec();
        final var contentTypes = getColumnContentTypes(displayedColumns, rendererIds, m_renderersMap);
        final var dataTypeIds = getColumnDataTypeIds(displayedColumns, tableSpec);
        var currentSelection = getCurrentSelection();
        var totalSelected = m_filteredAndSortedTableCache.getCachedTable().isEmpty() ? currentSelection.size()
            : countSelectedRows(filteredAndSortedTable, currentSelection);
        return createTable(displayedColumns, contentTypes, dataTypeIds, rows, tableSize, totalSelected);
    }

    @Override
    public Long getTotalSelected() {
        var filteredTable = m_filteredAndSortedTableCache.getCachedTable().orElse(null);
        var currentSelection = getCurrentSelection();
        return filteredTable == null ? currentSelection.size() : countSelectedRows(filteredTable, currentSelection);
    }

    @Override
    public void clearCache() {
        m_sortedTableCache.clear();
        m_filteredAndSortedTableCache.clear();
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

    private static ContainerTable sortTable(final BufferedDataTable table, final String sortColumn,
        final boolean sortAscending) {
        final var dts = table.getSpec();
        final var sortColIndex = dts.findColumnIndex(sortColumn);
        final var rc = RowComparator.on(dts);
        if (sortColIndex < 0) {
            rc.thenComparingRowKey(rk -> rk.withAlphanumericComparison().withDescendingSortOrder(!sortAscending));
        } else {
            final var colType = dts.getColumnSpec(sortColIndex).getType();
            rc.thenComparingColumn(sortColIndex, col -> col
                .withAlphanumericComparison(StringCell.TYPE.equals(colType))
                .withDescendingSortOrder(!sortAscending));
        }
        final Comparator<DataRow> comp = rc.build();
        try {
            return (ContainerTable)new DataTableSorter(table, table.size(), comp).sort(new ExecutionMonitor());
        } catch (CanceledExecutionException e) {
            throw new DataServiceException("Table sorting has been cancelled", e);
        }
    }

    private static ContainerTable filterTable(final DataTable table, final String[] columns,
        final String globalSearchTerm, final String[][] columnFilterValue, final boolean filterRowKeys) {
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
        return (ContainerTable)resultContainer.getTable();
    }

    /**
     * @param table
     * @return the number of selected rows in the given table
     */
    private static Long countSelectedRows(final DataTable table, final Set<RowKey> currentSelection) {
        if (currentSelection.isEmpty()) {
            return 0l;
        }
        var totalSelected = 0l;
        try (final var iterator = createRowIteratorSupplier(table, createRowKeysFilter()).get()) {
            while (iterator.hasNext()) {
                final var row = iterator.next();
                if (currentSelection.contains(row.getKey())) {
                    totalSelected += 1;
                }
            }
        }
        return totalSelected;
    }

    private Set<RowKey> getCurrentSelection() {
        return m_selectionSupplier == null ? Set.of() : m_selectionSupplier.get();
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

            if (globalSearchTerm == null || globalSearchTerm.isEmpty()
                || cellStringValue.contains(globalSearchTerm.toLowerCase())) {
                globalMatch = true;
            }
            if (columnFilterValue == null || columnFilterValue.length < 1) {
                continue;
            }

            final var currentColumnFilters = columnFilterValue[currentIndex];

            // if the domain values exists we want an exact match, otherwise we
            // just check if the cell value matches the search term
            final var needsExactMatch = isRowKey ? false
                : (spec.getColumnSpec(colIndices[currentIndex - columnOffset]).getDomain().getValues() != null);

            final var currentFilterMatch = currentColumnFilters.length == 0 || currentColumnFilters[0].isEmpty()
                || Arrays.stream(currentColumnFilters).map(String::toLowerCase)
                    .anyMatch(needsExactMatch ? cellStringValue::equals : cellStringValue::contains);
            if (!currentFilterMatch) {
                colFilterMatch = false;
            }
        }
        return colFilterMatch && globalMatch;
    }

    private static long getTableSize(final DataTable table) {
        // TODO remove this and use execution context, if UIEXT-243 is implemented
        if (table instanceof ContainerTable) {
            return ((ContainerTable)table).size();
        } else {
            return ((BufferedDataTable)table).size();
        }
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
                            return cell.isMissing() ? null
                                : renderCell(cell, renderers[i], rendererRegistry, tableId);
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
        return createTable(new String[0], new String[0], new String[0], new String[0][], 0, 0l);
    }

    private static Table createTable(final String[] displayedColumns, final String[] contentTypes,
        final String[] columnDataTypeIds, final String[][] rows, final long rowCount, final Long totalSelected) {
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
            public Long getTotalSelected() {
                return totalSelected;
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

    @Override
    public String[] getCurrentRowKeys() {
        final var filteredAndSortedTable =
            m_filteredAndSortedTableCache.getCachedTable().orElseGet(m_tableSupplier::get);
        final var size = (int)getTableSize(filteredAndSortedTable);
        final var rowKeys = new String[size];
        final var filter = new TableFilter.Builder();
        filter.withMaterializeColumnIndices(new int[0]);
        try (final var iterator = createRowIteratorSupplier(filteredAndSortedTable, filter.build()).get()) {
            IntStream.range(0, size).forEach(index -> {
                final var row = iterator.next();
                rowKeys[index] = row.getKey().toString();
            });
        }
        return rowKeys;
    }

    private static TableFilter createRowKeysFilter() {
        final var filter = new TableFilter.Builder();
        filter.withMaterializeColumnIndices(new int[0]);
        return filter.build();
    }

}
