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
 */
package org.knime.core.data;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.container.DataContainerDelegate;
import org.knime.core.data.container.DataContainerSettings;
import org.knime.core.data.container.ILocalDataRepository;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;
import org.knime.core.data.v2.RowContainer;
import org.knime.core.data.v2.SizeAwareDataTable;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTable.KnowsRowCountTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.table.row.Selection;

/**
 * TableBackends are used to read and write tables within KNIME AP nodes.
 *
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 *
 * @since 4.3
 *
 * @noreference This interface is not intended to be referenced by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 *
 * @apiNote This interface can change with future releases of KNIME Analytics Platform.
 */
public interface TableBackend {

    /**
     * This method should never be called outside {@link DataContainer}s.
     *
     * @param spec the spec of the table
     * @param settings data container settings
     * @param repository data repository
     * @param localRepository the local data repository
     * @param fileStoreHandler the file store handler
     *
     * @return {@link DataContainerDelegate}.
     *
     * @noreference This method is not intended to be referenced by clients.
     */
    DataContainerDelegate create(DataTableSpec spec, DataContainerSettings settings, IDataRepository repository,
        ILocalDataRepository localRepository, IWriteFileStoreHandler fileStoreHandler);

    /**
     * Creates a new {@link RowContainer}.
     *
     * @param context the execution context
     * @param spec of the table
     * @param settings additional settings
     * @param repository the data repository
     * @param handler the file-store handler
     *
     * @return a new {@link RowContainer}
     */
    RowContainer create(ExecutionContext context, DataTableSpec spec, DataContainerSettings settings,
        IDataRepository repository, IWriteFileStoreHandler handler);

    /**
     * @return short, human-readable name of the TableBackend implementation.
     */
    String getShortName();

    /**
     * @return human-readble description of the TableBackend.
     */
    String getDescription();

    /**
     * @return the number of bytes used in off-heap memory
     * @since 5.4
     */
    default long getReservedOffHeapBytes() {
        return 0;
    }

    /**
     * Concatenates the provided tables.
     * @param exec for creating container tables
     * @param progressMonitor used to report progress
     * @param tableIdSupplier provides IDs for potentially created ContainerTables
     * @param rowKeyDuplicateSuffix the suffix for duplicate row keys
     * @param duplicatesPreCheck flag to check the row keys for duplicates (only takes effect if rowKeyDuplicateSuffix
     *            is null)
     * @param tables to concatenate
     *
     * @return the concatenated table
     * @throws CanceledExecutionException if execution is canceled by the user
     * @since 5.1
     */
    KnowsRowCountTable concatenate(ExecutionContext exec, ExecutionMonitor progressMonitor, IntSupplier tableIdSupplier,
        String rowKeyDuplicateSuffix, boolean duplicatesPreCheck, final BufferedDataTable... tables)
        throws CanceledExecutionException;

    /**
     * Concatenates the given tables and generates new RowIDs to ensure uniqueness.
     *
     * @param exec for table creation
     * @param tableIdSupplier provides new table ids
     * @param tables to concatenate
     * @return the concatenated table
     * @since 5.1
     */
    KnowsRowCountTable concatenateWithNewRowIDs(ExecutionContext exec, IntSupplier tableIdSupplier,
        BufferedDataTable... tables);

    /**
     * Appends the provided tables in the column dimension.
     * @param exec ExecutionContext for creating tables
     * @param progressMonitor used to report progress
     * @param tableIdSupplier provides IDs for potentially created ContainerTables
     * @param left the left table
     * @param right the right table
     *
     * @return the appended table
     * @throws CanceledExecutionException if execution is canceled by the user
     * @since 5.1
     */
    KnowsRowCountTable append(ExecutionContext exec, ExecutionMonitor progressMonitor, IntSupplier tableIdSupplier,
        final BufferedDataTable left, BufferedDataTable right) throws CanceledExecutionException;

    /**
     * Appends the provided tables in the column dimension. Note that the tables must have the same number of rows.
     *
     * @param exec used to report progress
     * @param tableIdSupplier provides IDs for potentially created ContainerTables
     * @param config for the append operation
     * @param left the left table
     * @param right the right table
     * @return the appended table
     * @throws CanceledExecutionException if execution is canceled by the user
     * @since 5.1
     */
    KnowsRowCountTable append(ExecutionContext exec, IntSupplier tableIdSupplier, AppendConfig config,
        BufferedDataTable left, BufferedDataTable right) throws CanceledExecutionException;

    /**
     * Config for appending tables.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     * @since 5.1
     */
    final class AppendConfig {

        /**
         * Determines which table provides the RowIDs.
         *
         * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
         */
        public enum RowIDMode {
                /**
                 * All tables have matching RowIDs, hence the RowIDs of the first table are used. The backend will also
                 * check that the RowIDs actually match.
                 */
                MATCHING(false, RowIDMode::checkEqualNumRows),
                /**
                 * The RowIDs of a user specified table are used.
                 */
                FROM_TABLE(true, distribute(List.of(RowIDMode::checkEqualNumRows, RowIDMode::checkTableIndex)));

            private final boolean m_usesTableIndex;

            private final BiConsumer<AppendConfig, BufferedDataTable[]> m_inputTableValidator;

            private static void checkEqualNumRows(final AppendConfig config, final BufferedDataTable[] tables) {
                CheckUtils.checkArgument(Stream.of(tables)//
                    .mapToLong(BufferedDataTable::size)//
                    .distinct()//
                    .count() == 1, "All input tables must have the same number of rows for RowIDMode '%s'.",
                    config.getRowIDMode());
            }

            private static void checkTableIndex(final AppendConfig config, final BufferedDataTable[] tables) {
                CheckUtils.checkArgument(config.getRowIDTableIndex() < tables.length,
                    "The table index (%s) exceeds the number of tables (%s).", config.getRowIDTableIndex(),
                    tables.length);
            }

            private static <S, T> BiConsumer<S, T> distribute(final List<BiConsumer<S, T>> consumers) {
                return (s, t) -> consumers.forEach(c -> c.accept(s, t));
            }

            RowIDMode(final boolean usesTableIndex,
                final BiConsumer<AppendConfig, BufferedDataTable[]> inputTableValidator) {
                m_usesTableIndex = usesTableIndex;
                m_inputTableValidator = inputTableValidator;
            }

            private void validateInputTables(final AppendConfig config, final BufferedDataTable... tables) {
                m_inputTableValidator.accept(config, tables);
            }
        }

        private final RowIDMode m_idMode;

        private final int m_tableIdx;

        private AppendConfig(final RowIDMode idMode, final int tableIndex) {
            m_idMode = idMode;
            m_tableIdx = tableIndex;
        }

        /**
         * @return an AppendConfig for appending with matching RowIDs
         */
        public static AppendConfig matchingRowIDs() {
            return new AppendConfig(RowIDMode.MATCHING, -1);
        }

        /**
         * @param tableIndex the index of the table whose RowIDs should be used for the output table
         * @return an AppendConfig with the user provided RowIDs
         */
        public static AppendConfig rowIDsFromTable(final int tableIndex) {
            CheckUtils.checkArgument(tableIndex >= 0, "The tableIdx must be non-negative.");
            return new AppendConfig(RowIDMode.FROM_TABLE, tableIndex);
        }

        /**
         * Performs preliminary validation e.g. that the tables have the same number of rows.
         *
         * @param tables to validate
         */
        public void validateInputTables(final BufferedDataTable... tables) {
            m_idMode.validateInputTables(this, tables);
        }

        /**
         * @return the RowIDMode
         */
        public RowIDMode getRowIDMode() {
            return m_idMode;
        }

        /**
         * @return the index of the table whose RowIDs should be used
         */
        public int getRowIDTableIndex() {
            CheckUtils.checkState(m_idMode.m_usesTableIndex, "The RowIDMode '%s' does not use the table index.");
            return m_tableIdx;
        }

    }

    /**
     * Applies the provided ColumnRearranger on the provided table.
     *
     * @param progressMonitor for reporting progress
     * @param tableIdSupplier provides IDs for potentially created ContainerTables
     * @param columnRearranger defines how to transform the table
     * @param table to transform
     * @param context for creating containers
     * @return the rearranged table
     * @throws CanceledExecutionException if execution is canceled by the user
     * @since 4.5
     */
    KnowsRowCountTable rearrange(ExecutionMonitor progressMonitor, IntSupplier tableIdSupplier,
        final ColumnRearranger columnRearranger, BufferedDataTable table, ExecutionContext context)
        throws CanceledExecutionException;

    /**
     * Slices the input table according to the provided {@link Selection slices}. Example: The selection defines a
     * column selection of 2, 5 and 7 and defines a row range from 1000 to 1005, then the sliced table will have
     * 3 columns and 5 rows.
     *
     * @param exec for reporting progress and the potential creation of tables
     * @param table to slice
     * @param slices the definition of the slices
     * @param tableIdSupplier provides IDs for potentially created ContainerTables
     * @return the sliced table
     * @since 5.3
     */
    KnowsRowCountTable[] slice(ExecutionContext exec, BufferedDataTable table, Selection[] slices,
        IntSupplier tableIdSupplier);

    /**
     * Chunks the table into cursors according to the given chunk size (the last chunk may be smaller).
     * In contrast to {@link #slice(ExecutionContext, BufferedDataTable, Selection[], IntSupplier)}
     *
     * @param exec for reporting progress and
     * @param table
     * @param chunkSize
     * @param tableIdSupplier
     * @param batchConsumer consumer of produced row batches
     * @throws CanceledExecutionException
     * @since 5.4
     */
    void chunked(ExecutionContext exec, BufferedDataTable table, long chunkSize,
        IntSupplier tableIdSupplier, Predicate<SizeAwareDataTable> batchConsumer) throws CanceledExecutionException;

    /**
     * Replaces the spec of the input table.
     *
     * @param exec for reporting progress and potential creation of tables
     * @param table to replace the spec of
     * @param newSpec the new spec
     * @param tableIDSupplier provides IDs for potentially created tables
     * @return the table with the new spec
     * @since 5.1
     */
    KnowsRowCountTable replaceSpec(ExecutionContext exec, final BufferedDataTable table,
        final DataTableSpec newSpec, IntSupplier tableIDSupplier);

}
