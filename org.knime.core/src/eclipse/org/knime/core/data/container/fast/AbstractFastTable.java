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
 *   May 1, 2020 (dietzc): created
 */
package org.knime.core.data.container.fast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.fast.AdapterRegistry.DataSpecAdapter;
import org.knime.core.data.container.filter.TableFilter;
import org.knime.core.data.table.ReadTable;
import org.knime.core.data.table.column.ColumnType;
import org.knime.core.data.table.row.RowBatchReaderConfig;
import org.knime.core.data.table.store.TableReadStore;
import org.knime.core.data.table.store.TableStoreUtils;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.workflow.WorkflowDataRepository;

/**
 * @author Christian Dietz
 */
abstract class AbstractFastTable implements FastTable {

    private final long m_tableId;

    private final DataTableSpec m_spec;

    private DataSpecAdapter m_adapter;

    protected boolean m_isRowKey;

    AbstractFastTable(final long id, final DataTableSpec spec, final boolean isRowKey,
        final DataSpecAdapter adapter) {
        m_spec = spec;
        m_tableId = id;
        m_isRowKey = isRowKey;
        m_adapter = adapter;
    }

    @Override
    public DataTableSpec getDataTableSpec() {
        return m_spec;
    }

    @Override
    public int getTableId() {
        return (int)m_tableId;
    }

    @Override
    public boolean isRowKeys() {
        return m_isRowKey;
    }

    @Override
    public void putIntoTableRepository(final WorkflowDataRepository dataRepository) {
        // TODO only relevant in case of newly created tables?
        dataRepository.addTable((int)m_tableId, this);
    }

    @Override
    public boolean removeFromTableRepository(final WorkflowDataRepository dataRepository) {
        // TODO only relevant in case of newly created tables?
        dataRepository.removeTable((int)m_tableId);
        return true;
    }

    @SuppressWarnings("resource")
    @Override
    public CloseableRowIterator iterator() {
        return new FastTableRowIterator(TableStoreUtils.createReadTable(getStore()), m_adapter, m_isRowKey);
    }

    @Override
    public CloseableRowIterator iteratorWithFilter(final TableFilter filter, final ExecutionMonitor exec) {
        return iterator(filter, exec);
    }

    // TODO handle exec!
    @SuppressWarnings("resource")
    private CloseableRowIterator iterator(final TableFilter filter,
        @SuppressWarnings("unused") final ExecutionMonitor exec) {
        final ColumnType<?, ?>[] columnTypes = m_adapter.getColumnTypes();
        final TableReadStore store = getStore();

        // TODO use exec? slow :-(
        // TODO implement row index selection as RowBatchReaderConfig (start at...)
        final Optional<Set<Integer>> materializeColumnIndices = filter.getMaterializeColumnIndices();
        // special case this one!
        if (materializeColumnIndices.isPresent()) {
            final int numSelected = materializeColumnIndices.get().size();
            if (numSelected == 0) {
                return m_isRowKey ? new EmptyRowIterator(TableStoreUtils.createReadTable(store), columnTypes.length)
                    : new EmptyRowNoKeyIterator(columnTypes.length, store.size());
            } else if (numSelected < columnTypes.length) {
                final ReadTable table = TableStoreUtils.createReadTable(store, wrap(filter));
                return new PartialRowIterator(table, m_adapter, m_isRowKey,
                    materializeColumnIndices.get().stream().mapToInt((i) -> i).toArray());
            }
        }

        return iterator();
    }

    // TODO efficiency
    private static RowBatchReaderConfig wrap(final TableFilter filter) {
        return new RowBatchReaderConfig() {
            @Override
            public int[] getColumnIndices() {
                Optional<Set<Integer>> materializeColumnIndices = filter.getMaterializeColumnIndices();
                final int[] selected;
                if (materializeColumnIndices.isPresent()) {
                    final List<Integer> indices = new ArrayList<>(materializeColumnIndices.get());
                    Collections.sort(indices);
                    selected = new int[indices.size()];
                    for (int i = 0; i < selected.length; i++) {
                        selected[i] = indices.get(i);
                    }
                } else {
                    selected = null;
                }
                return selected;
            }
        };
    }
}
