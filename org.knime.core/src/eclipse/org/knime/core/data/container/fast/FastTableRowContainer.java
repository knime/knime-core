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

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.RowContainer;
import org.knime.core.data.container.fast.AdapterRegistry.DataSpecAdapter;
import org.knime.core.data.table.preproc.PreProcTableStore;
import org.knime.core.data.table.preproc.PreProcessingConfig;
import org.knime.core.data.table.row.RowWriteCursor;
import org.knime.core.data.table.store.TableReadStore;
import org.knime.core.data.table.store.TableStore;
import org.knime.core.data.table.store.TableStoreConfig;
import org.knime.core.data.table.store.TableStoreFactory;
import org.knime.core.data.table.store.TableStoreUtils;
import org.knime.core.data.table.value.StringWriteValue;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeSettingsWO;

/**
 * TODO
 *
 * @author Christian Dietz, KNIME GmbH
 */
class FastTableRowContainer implements RowContainer {

    private TmpFastTable m_table;

    private PreProcTableStore m_store;

    private int m_size;

    private final long m_tableId;

    private final DataTableSpec m_spec;

    // TODO
    private final StringWriteValue m_rowKeyWriteValue;

    private final RowWriteCursor m_cursor;

    private final DataCellConsumer[] m_writer;

    private final DataSpecAdapter m_adapter;

    /**
     * @param tableId
     * @param spec
     * @param dest
     * @param factory
     */
    @SuppressWarnings("resource")
    public FastTableRowContainer(final long tableId, final File dest, final DataTableSpec spec,
        final TableStoreFactory factory) {
        m_adapter = AdapterRegistry.createAdapter(spec, true);
        m_spec = spec;
        m_tableId = tableId;
        m_store = new PreProcTableStore(
            TableStoreUtils.cache(factory.create(m_adapter.getColumnTypes(), dest, new TableStoreConfig() {

                @Override
                public int getInitialChunkSize() {
                    // TODO make configurable. from where? constant? cache?
                    return 8_00_000;
                }
            })), new PreProcessingConfig() {
                @Override
                public int[] getDomainEnabledIndices() {
                    // TODO efficiency for wide tables ... of course...e.g. implement (isEnabled(int idx)).
                    // Don't compute domain for row key
                    final int[] enabled = new int[m_adapter.getColumnTypes().length - 1];
                    for (int i = 0; i < enabled.length; i++) {
                        enabled[i] = i + 1;
                    }
                    return enabled;
                }
            });

        // TODO Wrap into single object? Performance?
        m_cursor = TableStoreUtils.createWriteTable(m_store).getCursor();
        m_writer = m_adapter.createConsumers(m_cursor);
        m_rowKeyWriteValue = m_cursor.get(0);
    }

    @Override
    public void close() {
        if (m_table != null) {
            return;
        } else {
            try {
                m_cursor.close();
            } catch (Exception ex) {
                // TODO OK?
                throw new IllegalStateException(ex);
            }
            final DataColumnSpec[] colSpecs = new DataColumnSpec[m_spec.getNumColumns()];
            for (int i = 0; i < colSpecs.length; i++) {
                final DataColumnSpecCreator specCreator = new DataColumnSpecCreator(m_spec.getColumnSpec(i));
                // TODO set domain for all enabled domains
                //                specCreator.setDomain(null);
                colSpecs[i] = specCreator.createSpec();
            }
            m_table = new TmpFastTable(m_tableId, m_spec, m_store, m_adapter, true);
        }
    }

    @Override
    public void addRowToTable(final DataRow row) {
        m_cursor.fwd();
        m_rowKeyWriteValue.setStringValue(row.getKey().toString());
        for (int i = 1; i < m_writer.length; i++) {
            m_writer[i].set(row.getCell(i - 1));
        }
        m_size++;
    }

    @Override
    public long size() {
        return m_size;
    }

    @Override
    public ContainerTable getTable() {
        if (m_table == null) {
            // TODO
            throw new IllegalStateException("Close container before calling 'getTable()'");
        }
        return m_table;
    }

    @Override
    public void clear() {
        if (m_table != null) {
            m_table.clear();
        } else {
            try {
                m_cursor.close();
                m_store.close();
            } catch (Exception ex) {
                // TODO
                throw new RuntimeException(ex);
            }
        }
    }

    @Override
    public void setMaxPossibleValues(final int maxPossibleValues) {
        // TODO
        throw new UnsupportedOperationException("nyi");
    }

    @Override
    public DataTableSpec getTableSpec() {
        if (m_table != null) {
            return m_table.getDataTableSpec();
        } else {
            return m_spec;
        }
    }

    static class TmpFastTable extends AbstractFastTable {

        private TableStore m_store;

        TmpFastTable(final long tableId, final DataTableSpec spec, final TableStore store,
            final DataSpecAdapter adapter, final boolean isRowKey) {
            super(tableId, spec, isRowKey, adapter);
            m_store = store;
        }

        @Override
        public TableReadStore getStore() {
            return m_store;
        }

        @Override
        public void ensureOpen() {
            // Ensure open only interesting for LazyFastTables...
            throw new IllegalStateException("Why called?");
        }

        @Override
        public void clear() {
            try {
                // TODO make sure we do the right thing with 'close'.
                // In this case it really means: destroy all (not only kill memory)
                m_store.close();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            m_store = null;
        }

        @Override
        public void saveToFile(final File f, final NodeSettingsWO settings, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
            if (m_store == null) {
                throw new IOException("Can't save a cleared FastTable. Implementation error.");
            }
            // TODO investigate move vs. copy
            m_store.copyDataTo(f);
        }

        @Override
        public long size() {
            return m_store.size();
        }
    }
}
