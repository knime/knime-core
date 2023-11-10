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
 *   Nov 10, 2023 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.data.container;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.stream.Stream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.RowKeyValue;
import org.knime.core.data.container.filter.TableFilter;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.v2.RowCursor;
import org.knime.core.data.v2.RowRead;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTable.KnowsRowCountTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.WorkflowDataRepository;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class IndexedTable implements KnowsRowCountTable {

    private final BufferedDataTable m_source;

    private final DataTableSpec m_specWithIndex;

    IndexedTable(final BufferedDataTable table, final String indexColumnName) {
        m_source = table;
        var indexColumnSpec = new DataColumnSpecCreator(indexColumnName, LongCell.TYPE).createSpec();
        var tableSpecCreator = new DataTableSpecCreator(table.getDataTableSpec());
        tableSpecCreator.dropAllColumns();
        tableSpecCreator.addColumns(indexColumnSpec);
        tableSpecCreator.addColumns(table.getDataTableSpec());
        m_specWithIndex = tableSpecCreator.createSpec();
    }

    @Override
    public DataTableSpec getDataTableSpec() {
        return m_specWithIndex;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getRowCount() {
        return m_source.getRowCount();
    }

    @Override
    public long size() {
        return m_source.size();
    }

    @Override
    public void saveToFile(final File f, final NodeSettingsWO settings, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        throw new UnsupportedOperationException("The IndexedTable is only temporary and cannot be persisted to file.");
    }

    @Override
    public void clear() {
        // nothing to do
    }

    @Override
    public void ensureOpen() {
        // now own data
    }

    @SuppressWarnings("resource")
    @Override
    public CloseableRowIterator iterator() {
        return new IndexedRowIterator(m_source.iterator());
    }

    @SuppressWarnings("resource")
    @Override
    public RowCursor cursor() {
        return new IndexedCursor(m_source.cursor());
    }

    @SuppressWarnings("resource")
    @Override
    public RowCursor cursor(final TableFilter filter) {
        var translatedFilter = translateFilter(filter);
        if (filtersIndex(filter)) {
            return m_source.cursor(translatedFilter);
        } else {
            return new IndexedCursor(m_source.cursor(translatedFilter));
        }
    }

    @SuppressWarnings("resource")
    @Override
    public CloseableRowIterator iteratorWithFilter(final TableFilter filter, final ExecutionMonitor exec) {
        var translatedFilter = translateFilter(filter);
        if (filtersIndex(filter)) {
            return m_source.filter(translatedFilter, exec).iterator();
        } else {
            return new IndexedRowIterator(m_source.filter(translatedFilter, exec).iterator());
        }
    }

    private static boolean filtersIndex(final TableFilter filter) {
        var materializedCols = filter.getMaterializeColumnIndices();
        return materializedCols.isPresent() && !materializedCols.get().contains(0);
    }


    private static TableFilter translateFilter(final TableFilter filter) {
        var materializedCols = filter.getMaterializeColumnIndices();
        if (materializedCols.isEmpty()) {
            return filter;
        }
        var cols = materializedCols.get();
        var translatedMaterializedCols = new int[cols.size()];
        int i = 0;
        for (var colIndex : cols) {
            if (colIndex > 0) {
                translatedMaterializedCols[i] = colIndex - 1;
                i++;
            }
        }
        return new TableFilter.Builder(filter)
                .withMaterializeColumnIndices(translatedMaterializedCols)
                .build();
    }

    @Override
    public BufferedDataTable[] getReferenceTables() {
        return new BufferedDataTable[] {m_source};
    }

    @Override
    public void putIntoTableRepository(final WorkflowDataRepository dataRepository) {
        // no own data
    }

    @Override
    public boolean removeFromTableRepository(final WorkflowDataRepository dataRepository) {
        return false;
    }

    private static final class IndexedRowIterator extends CloseableRowIterator {
        private final CloseableRowIterator m_sourceIterator;
        private long m_index = -1;

        IndexedRowIterator(final CloseableRowIterator sourceIterator) {
            m_sourceIterator = sourceIterator;
        }

        @Override
        public void close() {
            m_sourceIterator.close();
        }

        @Override
        public boolean hasNext() {
            return m_sourceIterator.hasNext();
        }

        @Override
        public DataRow next() {
            m_index++;
            return new IndexedRow(m_sourceIterator.next(), m_index);
        }

    }

    private static final class IndexedRow implements DataRow {

        private final DataRow m_sourceRow;

        private final LongCell m_index;

        IndexedRow(final DataRow sourceRow, final long index) {
            m_sourceRow = sourceRow;
            m_index = new LongCell(index);
        }

        @Override
        public Iterator<DataCell> iterator() {
            return Stream.concat(Stream.of(m_index), m_sourceRow.stream()).iterator();
        }

        @Override
        public int getNumCells() {
            return m_sourceRow.getNumCells() + 1;
        }

        @Override
        public RowKey getKey() {
            return m_sourceRow.getKey();
        }

        @Override
        public DataCell getCell(final int index) {
            return index == 0 ? m_index : m_sourceRow.getCell(index - 1);
        }

    }

    private static final class IndexedCursor implements RowCursor, RowRead {

        private final RowCursor m_sourceCursor;

        private RowRead m_sourceRowRead;

        private long m_index = -1;

        IndexedCursor(final RowCursor sourceCursor) {
            m_sourceCursor = sourceCursor;
        }

        @Override
        public RowRead forward() {
            m_index++;
            m_sourceRowRead = m_sourceCursor.forward();
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <D extends DataValue> D getValue(final int index) {
            return index == 0 ? (D)new LongCell(m_index) : m_sourceRowRead.getValue(index - 1);
        }

        @Override
        public boolean isMissing(final int index) {
            return index != 0 && m_sourceRowRead.isMissing(index - 1);
        }

        @Override
        public RowKeyValue getRowKey() {
            return m_sourceRowRead.getRowKey();
        }

        @Override
        public boolean canForward() {
            return m_sourceCursor.canForward();
        }

        @Override
        public int getNumColumns() {
            return m_sourceCursor.getNumColumns() + 1;
        }

        @Override
        public void close() {
            m_sourceCursor.close();
        }


    }

}
