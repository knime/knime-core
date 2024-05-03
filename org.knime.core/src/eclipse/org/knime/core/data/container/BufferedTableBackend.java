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
 *   Sep 23, 2020 (dietzc): created
 */
package org.knime.core.data.container;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.IDataRepository;
import org.knime.core.data.TableBackend;
import org.knime.core.data.container.filter.TableFilter;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;
import org.knime.core.data.filestore.internal.NotInWorkflowWriteFileStoreHandler;
import org.knime.core.data.v2.RowContainer;
import org.knime.core.data.v2.RowCursor;
import org.knime.core.data.v2.RowKeyType;
import org.knime.core.data.v2.RowRead;
import org.knime.core.data.v2.RowWrite;
import org.knime.core.data.v2.RowWriteCursor;
import org.knime.core.data.v2.schema.ValueSchema;
import org.knime.core.data.v2.schema.ValueSchemaUtils;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTable.KnowsRowCountTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InternalTableAPI;
import org.knime.core.node.Node;
import org.knime.core.table.row.Selection;
import org.knime.core.table.row.Selection.ColumnSelection;
import org.knime.core.table.row.Selection.RowRangeSelection;

/**
 * Default implementation of {@link TableBackend} using {@link DataContainer} and {@link DataContainerDelegate}s.
 *
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 *
 * @noreference This interface is not intended to be referenced by clients.
 */
public final class BufferedTableBackend implements TableBackend {

    @Override
    public DataContainerDelegate create(final DataTableSpec spec, final DataContainerSettings settings,
        final IDataRepository repository, final ILocalDataRepository localRepository,
        final IWriteFileStoreHandler fileStoreHandler) {
        return new BufferedDataContainerDelegate(spec, settings, repository, localRepository,
            initFileStoreHandler(fileStoreHandler, repository));
    }

    @Override
    public RowContainer create(final ExecutionContext context, final DataTableSpec spec,
        final DataContainerSettings settings, final IDataRepository repository, final IWriteFileStoreHandler handler) {
        final BufferedDataContainer container = context.createDataContainer(spec, settings);
        final ValueSchema schema = ValueSchemaUtils.create(spec, RowKeyType.CUSTOM, handler);
        return new BufferedRowContainer(container, schema);
    }

    @Override
    public String getShortName() {
        return "Row-based Backend";
    }

    @Override
    public String getDescription() {
        // NB: HACK: The final line breaks prevent the backend selection panel from showing
        // scroll bars if the backend is changed from default to columnar
        return "Row-based table backend, which supports all possible data types.    \n\n\n\n";
    }

    private static IWriteFileStoreHandler initFileStoreHandler(final IWriteFileStoreHandler fileStoreHandler,
        final IDataRepository repository) {
        IWriteFileStoreHandler nonNull = fileStoreHandler;
        if (nonNull == null) {
            nonNull = NotInWorkflowWriteFileStoreHandler.create();
            nonNull.addToRepository(repository);
        }
        return nonNull;
    }

    @Override
    public boolean equals(final Object obj) {
        // instance should be a singleton but can't be due to Eclipse Extension Point instantiation
        return obj != null && obj.getClass() == getClass();
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public KnowsRowCountTable concatenate(final ExecutionContext exec, final ExecutionMonitor progressMonitor,
        final IntSupplier tableIdSupplier, final String rowKeyDuplicateSuffix, final boolean duplicatesPreCheck,
        final BufferedDataTable... tables) throws CanceledExecutionException {
        if (duplicatesPreCheck && rowKeyDuplicateSuffix == null) {
            return ConcatenateTable.create(progressMonitor, tables);
        } else {
            return ConcatenateTable.create(progressMonitor, Optional.ofNullable(rowKeyDuplicateSuffix),
                duplicatesPreCheck, tables);
        }
    }

    @Override
    public KnowsRowCountTable concatenateWithNewRowIDs(final ExecutionContext exec,
        final IntSupplier tableIdSupplier, final BufferedDataTable... tables) {
        return ConcatenateTable.createWithNewRowIDs(tables);
    }

    @Override
    public KnowsRowCountTable append(final ExecutionContext exec, final ExecutionMonitor progress,
        final IntSupplier tableIdSupplier, final BufferedDataTable left, final BufferedDataTable right)
        throws CanceledExecutionException {
        return JoinedTable.create(left, right, progress);
    }

    @Override
    public KnowsRowCountTable append(final ExecutionContext exec, final IntSupplier tableIdSupplier,
        final AppendConfig config, final BufferedDataTable left, final BufferedDataTable right)
        throws CanceledExecutionException {
        switch (config.getRowIDMode()) {
            case FROM_TABLE:
                return appendWithIDFromTable(exec, config.getRowIDTableIndex(), left, right);
            case MATCHING:
                return JoinedTable.create(left, right, exec);
            default:
                throw new UnsupportedOperationException("Unsupported RowIDMode: " + config.getRowIDMode());
        }
    }

    private static KnowsRowCountTable appendWithIDFromTable(final ExecutionContext exec, final int tableId,
        final BufferedDataTable left, final BufferedDataTable right) throws CanceledExecutionException {
        if (tableId == 0) {
            return JoinedTable.create(left, right, exec, false);
        } else if (tableId == 1) {
            var joinedTable = InternalTableAPI.append(exec.createSubExecutionContext(0.5),
                AppendConfig.rowIDsFromTable(0), right, left);
            var rearranger = new ColumnRearranger(joinedTable.getDataTableSpec());
            var numColsLeft = left.getDataTableSpec().getNumColumns();
            var numColsRight = right.getDataTableSpec().getNumColumns();
            // move the columns of the left table (which are currently right) left again
            rearranger.permute(IntStream.concat(//
                IntStream.range(0, numColsLeft).map(i -> i + numColsRight), //
                IntStream.range(0, numColsRight)).toArray());
            joinedTable = exec.createColumnRearrangeTable(joinedTable, rearranger, exec.createSubExecutionContext(0.5));
            return Node.invokeGetDelegate(joinedTable);
        } else {
            // shouldn't happen because the framework checks the validity of the table index further up
            throw new IllegalStateException("The index of the table providing the RowIDs must be either 0 or 1.");
        }
    }

    @Override
    public KnowsRowCountTable rearrange(final ExecutionMonitor progressMonitor,
        final IntSupplier tableIdSupplier, final ColumnRearranger columnRearranger,
        final BufferedDataTable table, final ExecutionContext context)
        throws CanceledExecutionException {
        return RearrangeColumnsTable.create(columnRearranger, table, progressMonitor, context);
    }

    @Override
    public KnowsRowCountTable[] slice(final ExecutionContext exec, final BufferedDataTable table,
        final Selection[] slices, final IntSupplier tableIdSupplier) {
        var inputSpec = table.getDataTableSpec();
        var slicer = new TableSlicer(inputSpec, slices);
        return slicer.slice(exec, table);
    }

    private static final class TableSlicer {

        private final Slice[] m_slices;

        private final Selection m_unionSlice;

        TableSlicer(final DataTableSpec spec, final Selection[] slices) {
            m_slices = Stream.of(slices).map(s -> new Slice(s, spec)).toArray(Slice[]::new);
            m_unionSlice = union(slices);
        }

        private static Selection union(final Selection[] slices) {
            var union = Selection.all();
            if (Stream.of(slices).anyMatch(Selection::allSelected)) {
                return union;
            }

            if (Stream.of(slices).map(Selection::rows).noneMatch(RowRangeSelection::allSelected)) {
                long from = Stream.of(slices)//
                    .map(Selection::rows)//
                    .mapToLong(RowRangeSelection::fromIndex)//
                    .min()//
                    .orElseThrow(() -> new IllegalStateException("No slices provided."));
                long to = Stream.of(slices)//
                    .map(Selection::rows)//
                    .mapToLong(RowRangeSelection::toIndex)//
                    .max()//
                    .orElseThrow(() -> new IllegalStateException("No slices provided."));
                union = union.retainRows(from, to);
            }
            if (Stream.of(slices).map(Selection::columns).noneMatch(ColumnSelection::allSelected)) {
                var columns = Stream.of(slices)//
                    .map(Selection::columns)//
                    .map(ColumnSelection::getSelected)//
                    .flatMapToInt(IntStream::of)//
                    .distinct()//
                    .sorted()//
                    .toArray();
                union = union.retainColumns(columns);
            }
            return union;
        }

        KnowsRowCountTable[] slice(final ExecutionContext exec, final BufferedDataTable table) {
            long numRows = getNumRowsToIterate(table.size(), m_unionSlice.rows());
            try (var sliceWriters = new AutoCloseables<>(Stream.of(m_slices).map(s -> new TableSliceWriter(s, exec)))) {
                if (numRows == 0) {
                    exec.setProgress(1);
                } else {
                    writeSlices(exec, table, numRows, sliceWriters);
                }

                return sliceWriters.stream()//
                        .map(TableSliceWriter::finish)//
                        .map(Node::invokeGetDelegate)//
                        .toArray(KnowsRowCountTable[]::new);
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to create the sliced table.", ex);
            }
        }

        private void writeSlices(final ExecutionContext exec, final BufferedDataTable table, final double numRows,
            final AutoCloseables<TableSliceWriter> writers) throws CanceledExecutionException {
            var unionSliceFromFirstRow = Selection.all()
                    .retainColumns(m_unionSlice.columns());
            if (!m_unionSlice.rows().allSelected()) {
                unionSliceFromFirstRow = unionSliceFromFirstRow.retainRows(0, m_unionSlice.rows().toIndex());
            }
            try (var readCursor = table.cursor(TableFilter.fromSelection(m_unionSlice))) {
                long r = moveToSlice(readCursor, exec, numRows);
                for (; readCursor.canForward(); r++) {
                    exec.checkCanceled();
                    var row = readCursor.forward();
                    long rowIndex = r - 1;
                    writers.stream().forEach(w -> w.writeRow(rowIndex, row));
                    exec.setProgress(r / numRows);
                }
            }
        }

        private long moveToSlice(final RowCursor cursor, final ExecutionMonitor progress, final double numRows)
                throws CanceledExecutionException {
            var rows = m_unionSlice.rows();
            if (rows.allSelected() || rows.fromIndex() == 0) {
                return 0;
            }
            var firstRow = rows.fromIndex();
            long r = 1;
            for (; r <= firstRow && cursor.canForward(); r++) {
                progress.checkCanceled();
                cursor.forward();
                progress.setProgress(r / numRows);
            }
            return r;
        }

        private static long getNumRowsToIterate(final long fullSize, final RowRangeSelection slice) {
            if (slice.allSelected(0, fullSize)) {
                return fullSize;
            } else {
                var from = Math.max(0, slice.fromIndex());
                var to = Math.min(slice.toIndex(), fullSize);
                if (to - from == 0) {
                    // the slice is empty, so no iteration is necessary
                    return 0;
                } else {
                    // the old iterators don't allow directly jumping to a row in the table
                    // so we always need to iterate from the 0 until to
                    return to;
                }
            }
        }
    }

    private static final class AutoCloseables<T extends AutoCloseable> implements AutoCloseable {

        private final List<T> m_closeables;

        AutoCloseables(final Stream<T> closeables) {
            m_closeables = closeables.collect(Collectors.toCollection(ArrayList<T>::new));
        }

        Stream<T> stream() {
            return m_closeables.stream();
        }


        @Override
        public void close() throws Exception {
            var exceptions = m_closeables.stream()//
                .map(AutoCloseables::tryClose)//
                .filter(Optional::isPresent)//
                .map(Optional::get)//
                .toList();
            if (!exceptions.isEmpty()) {
                var exIter = exceptions.iterator();
                var first = exIter.next();
                while (exIter.hasNext()) {
                    first.addSuppressed(exIter.next());
                }
                throw first;
            }
        }

        private static Optional<Exception> tryClose(final AutoCloseable closeable) {
            try {
                closeable.close();
                return Optional.empty();
            } catch (Exception ex) {
                return Optional.of(ex);
            }
        }

    }

    private static final class TableSliceWriter implements AutoCloseable {

        private final Slice m_slice;

        private final RowContainer m_container;

        private final RowWriteCursor m_cursor;

        TableSliceWriter(final Slice slice, final ExecutionContext exec) {
            m_slice = slice;
            m_container = exec.createRowContainer(m_slice.getSlicedSpec(), true);
            m_cursor = m_container.createCursor();
        }

        void writeRow(final long rowIndex, final RowRead row) {
            if (m_slice.includeRow(rowIndex)) {
                m_slice.copyRow(m_cursor.forward(), row);
            }
        }

        BufferedDataTable finish() {
            try {
                return m_container.finish();
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to create table slice.", ex);
            }
        }

        @Override
        public void close() throws Exception {
            m_cursor.close();
            m_container.close();
        }
    }

    private static final class Slice {
        private final Selection m_slice;

        private final IntUnaryOperator m_indexMap;

        private final DataTableSpec m_slicedSpec;

        Slice(final Selection slice, final DataTableSpec spec) {
            m_slice = slice;
            var filterColumns = slice.columns();
            if (!filterColumns.allSelected()) {
                var cols = filterColumns.getSelected();
                m_indexMap = i -> cols[i];
                m_slicedSpec = sliceSpec(spec, cols);
            } else {
                m_indexMap = i -> i;
                m_slicedSpec = spec;
            }
        }

        DataTableSpec getSlicedSpec() {
            return m_slicedSpec;
        }

        private static DataTableSpec sliceSpec(final DataTableSpec fullSpec, final int[] columns) {
            var rearranger = new ColumnRearranger(fullSpec);
            rearranger.keepOnly(columns);
            return rearranger.createSpec();
        }

        void copyRow(final RowWrite rowWrite, final RowRead rowRead) {
            rowWrite.setRowKey(rowRead.getRowKey());
            for (int i = 0; i < m_slicedSpec.getNumColumns(); i++) { //NOSONAR
                var oldIndex = m_indexMap.applyAsInt(i);
                if (rowRead.isMissing(oldIndex)) {
                    rowWrite.setMissing(i);
                } else {
                    rowWrite.getWriteValue(i).setValue(rowRead.getValue(oldIndex));
                }
            }
        }

        boolean includeRow(final long rowIndex) {
            return rowIndex >= startRow() && rowIndex < endRow();
        }

        /**
         * inclusive
         */
        long startRow() {
            return m_slice.rows().fromIndex();
        }

        /**
         * exclusive
         */
        long endRow() {
            return m_slice.rows().toIndex();
        }

    }

    @Override
    public KnowsRowCountTable replaceSpec(final ExecutionContext exec, final BufferedDataTable table,
        final DataTableSpec newSpec, final IntSupplier tableIDSupplier) {
        return new TableSpecReplacerTable(table, newSpec);
    }

}
