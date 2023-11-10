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

import java.util.Optional;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;

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
        final BufferedDataContainer container =
            context.createDataContainer(spec, settings.getInitializeDomain(), settings.getMaxCellsInMemory());
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
    public KnowsRowCountTable slice(final ExecutionContext exec, final BufferedDataTable table, final Selection slice,
        final IntSupplier tableIdSupplier) {
        var inputSpec = table.getDataTableSpec();
        var slicer = new TableSlicer(inputSpec, slice);
        return slicer.slice(exec, table);
    }

    private static final class TableSlicer {

        private final Selection m_slice;

        private final IntUnaryOperator m_indexMap;

        private final DataTableSpec m_slicedSpec;

        TableSlicer(final DataTableSpec spec, final Selection slice) {
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

        KnowsRowCountTable slice(final ExecutionContext exec, final BufferedDataTable table) {
            long numRowsToIterate = getNumRowsToIterate(table.size(), m_slice.rows());

            try (//
                    var container = exec.createRowContainer(m_slicedSpec, true); //
                    var writeCursor = container.createCursor() //
            ) {
                if (numRowsToIterate == 0) {
                    exec.setProgress(1);
                } else {
                    copySlice(exec, table, numRowsToIterate, writeCursor);
                }
                return Node.invokeGetDelegate(container.finish());
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to create the sliced table.", ex);
            }
        }

        private void copySlice(final ExecutionContext exec, final BufferedDataTable table, final double numRows,
            final RowWriteCursor writeCursor) {
            var sliceFromFirstRow = Selection.all()
                    .retainColumns(m_slice.columns());
            if (!m_slice.rows().allSelected()) {
                sliceFromFirstRow = sliceFromFirstRow.retainRows(0, m_slice.rows().toIndex());
            }
            try (var readCursor = table.cursor(TableFilter.fromSelection(sliceFromFirstRow))) {
                long r = moveToSlice(readCursor, exec, numRows);
                for (; readCursor.canForward(); r++) {
                    var rowWrite = writeCursor.forward();
                    var rowRead = readCursor.forward();
                    copyRow(rowWrite, rowRead);
                    exec.setProgress(r / numRows);
                }
            }
        }

        private long moveToSlice(final RowCursor cursor, final ExecutionMonitor progress, final double numRows) {
            var rows = m_slice.rows();
            if (rows.allSelected() || rows.fromIndex() == 0) {
                return 0;
            }
            var firstRow = rows.fromIndex();
            long r = 1;
            for (; r <= firstRow && cursor.canForward(); r++) {
                cursor.forward();
                progress.setProgress(r / numRows);
            }
            return r;
        }

        private void copyRow(final RowWrite rowWrite, final RowRead rowRead) {
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

        private static DataTableSpec sliceSpec(final DataTableSpec fullSpec, final int[] columns) {
            var rearranger = new ColumnRearranger(fullSpec);
            rearranger.keepOnly(columns);
            return rearranger.createSpec();
        }
    }

    @Override
    public KnowsRowCountTable replaceSpec(final ExecutionContext exec, final BufferedDataTable table,
        final DataTableSpec newSpec, final IntSupplier tableIDSupplier) {
        return new TableSpecReplacerTable(table, newSpec);
    }

    @Override
    public KnowsRowCountTable prependRowIndex(final ExecutionContext exec, final IntSupplier tableIdSupplier,
        final BufferedDataTable table, final String columnName) {
        return new IndexedTable(table, columnName);
    }

}
