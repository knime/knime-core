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
 *   Dec 20, 2022 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.node;

import java.util.function.Predicate;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.TableBackend;
import org.knime.core.data.container.DataContainerSettings;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;
import org.knime.core.data.v2.SizeAwareDataTable;
import org.knime.core.table.row.Selection;

/**
 * Provides internal API for manipulating tables. May change at any point in time.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @noreference This class is not intended to be referenced by clients.
 * @since 5.1
 */
public final class InternalTableAPI {

    private InternalTableAPI() {

    }

    /**
     * Creates a new table consisting of the specified slice of the input table.
     *
     * @param exec for creating the table
     * @param table that is sliced
     * @param slice to extract from the table
     * @return the sliced table
     */
    public static BufferedDataTable slice(final ExecutionContext exec, final BufferedDataTable table,
        final Selection slice) {
        return exec.createSlicedTables(table, slice)[0];
    }

    /**
     * Creates multiple slices of the provided input table.
     *
     * @param exec for creating the tables
     * @param table that is sliced
     * @param slices to extract from the table
     * @return the table slices
     * @since 5.3
     */
    public static BufferedDataTable[] multiSlice(final ExecutionContext exec, final BufferedDataTable table,
        final Selection... slices) {
        return exec.createSlicedTables(table, slices);
    }

    /**
     * Creates batches from the given table.
     *
     * @param exec execution context used to report progress, check for cancellation, or create intermediate tables
     * @param table table to process chunk-wise
     * @param chunkSize maximum chunk size
     * @param batchConsumer consumer for chunks, indicating if chunking should continue after the current chunk
     * @throws CanceledExecutionException if execution was canceled
     *
     * @since 5.4
     */
    public static void chunked(final ExecutionContext exec, final BufferedDataTable table, final long chunkSize,
            final Predicate<SizeAwareDataTable> batchConsumer) throws CanceledExecutionException {
        exec.chunked(table, chunkSize, batchConsumer);
    }

    /**
     * Appends the two tables column-wise according to the provided config.
     *
     * @param exec for creating the table and providing progress
     * @param config for the appending
     * @param left table
     * @param right table
     * @return the appended table
     * @throws CanceledExecutionException if execution is cancelled by the user
     * @since 5.1
     */
    public static BufferedDataTable append(final ExecutionContext exec, final TableBackend.AppendConfig config,
        final BufferedDataTable left, final BufferedDataTable right) throws CanceledExecutionException {
        config.validateInputTables(left, right);
        return exec.appendTables(config, left, right);
    }

    /**
     * Concatenates the given tables and creates new RowIDs to ensure uniqueness.
     *
     * @param exec for concatenating the tables
     * @param tables to concatenate
     * @return the concatenated table
     * @since 5.1
     */
    public static BufferedDataTable concatenateWithNewRowID(final ExecutionContext exec,
        final BufferedDataTable... tables) {
        return exec.createConcatenateTableWithNewRowIDs(tables);
    }

    /**
     * Creates a container to which rows can be added, overwriting the node's default {@link IWriteFileStoreHandler}.
     * This method has the same behavior as {@link ExecutionContext#createDataContainer(DataTableSpec)} except that the
     * provided {@link IWriteFileStoreHandler} is used. This is useful e.g. if the container is created to serve as a
     * preview even while this nodes' fileStoreHandler is closed.
     *
     * @param exec The {@link ExecutionContext} to use when creating the data container.
     * @param spec The spec to open the container.
     * @param writeFileStoreHandler The {@link IWriteFileStoreHandler} to use instead of the one associated with this
     *            {@link ExecutionContext}, useful e.g. if the container is only used as a preview.
     * @return A container to which rows can be added and which provides the <code>BufferedDataTable</code>.
     * @throws NullPointerException If the spec argument is <code>null</code>.
     * @since 5.2
     */
    public static BufferedDataContainer createDataContainer(final ExecutionContext exec, final DataTableSpec spec,
        final IWriteFileStoreHandler writeFileStoreHandler) {
        return exec.createDataContainer(spec, DataContainerSettings.getDefault(), writeFileStoreHandler);
    }
}
