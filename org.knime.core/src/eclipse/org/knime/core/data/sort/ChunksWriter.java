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
 *   Sep 12, 2023 (leonard.woerteler): created
 */
package org.knime.core.data.sort;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.DataContainer;

/**
 * This class can be used to write chunks of (temporary) data to disk as {@link DataTable}s. All chunks that have not
 * been extracted via a call to {@link #finish(Consumer)} before the writer is being closed are disposed. This makes it
 * safe to use cancel execution while using a chunks writer inside a {@code try}/{@code finally} block.
 *
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("javadoc")
final class ChunksWriter implements AutoCloseable {

    private final DataTableSpec m_dataTableSpec;
    private final TableIOHandler m_dataHandler;

    private final List<DataTable> m_buffer = new ArrayList<>();

    ChunksWriter(final DataTableSpec spec, final TableIOHandler dataHandler) {
        m_dataTableSpec = spec;
        m_dataHandler = dataHandler;
    }

    /**
     * Opens a new chunk which can be written to via the returned handle.
     *
     * @param forceOnDisk whether the chunk is forced to be written onto the file system
     * @return {@link AutoCloseable closeable} handle used to write the chunk, use with {@code try}/{@code finally}
     */
    public ChunkHandle openChunk(final boolean forceOnDisk) {
        return new ChunkHandle(forceOnDisk);
    }

    /**
     * Handle for safely writing data into a data table. When the handle is closed, the chunk is appended to the
     * surrounding writer if non-empty and otherwise discarded.
     */
    final class ChunkHandle implements AutoCloseable {

        private final DataContainer m_currentContainer;

        private long m_itemCount;

        ChunkHandle(final boolean forceOnDisk) {
            final var container = m_dataHandler.createDataContainer(m_dataTableSpec, forceOnDisk);
            m_currentContainer = container;
        }

        /**
         * Adds a row to the current chunk.
         *
         * @param dataRow the row
         */
        public void addRow(final DataRow dataRow) {
            m_itemCount++;
            m_currentContainer.addRowToTable(dataRow);
        }

        @Override
        public void close() {
            if (m_currentContainer != null) {
                m_currentContainer.close();
                final var dataTable = m_currentContainer.getTable();
                if (m_itemCount > 0) {
                    m_buffer.add(dataTable);
                } else {
                    m_dataHandler.clearTable(dataTable);
                }
            }
        }
    }

    /**
     * Offers the written chunks to the given consumer and then clears the chunk buffer.
     * This hands over the responsibility for disposing the chunks to the caller of this method.
     *
     * @param consumer callback receiving the written chunks
     */
    public void finish(final Consumer<Collection<DataTable>> consumer) {
        consumer.accept(m_buffer);
        m_buffer.clear();
    }

    @Override
    public void close() {
        // clean up all tables that haven't been extracted
        for (final var table : m_buffer) {
            m_dataHandler.clearTable(table);
        }
    }
}
