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

import java.util.function.IntSupplier;

import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.container.DataContainerDelegate;
import org.knime.core.data.container.DataContainerSettings;
import org.knime.core.data.container.ILocalDataRepository;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;
import org.knime.core.data.v2.RowContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTable.KnowsRowCountTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
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
     * Concatenates the provided tables.
     *
     * @param progressMonitor used to report progress
     * @param tableIdSupplier provides IDs for potentially created ContainerTables
     * @param rowKeyDuplicateSuffix the suffix for duplicate row keys
     * @param duplicatesPreCheck flag to check the row keys for duplicates (only takes effect if rowKeyDuplicateSuffix
     *            is null)
     * @param tables to concatenate
     * @return the concatenated table
     * @throws CanceledExecutionException if execution is canceled by the user
     * @since 4.5
     */
    KnowsRowCountTable concatenate(ExecutionMonitor progressMonitor, IntSupplier tableIdSupplier,
        String rowKeyDuplicateSuffix, boolean duplicatesPreCheck, final BufferedDataTable... tables)
        throws CanceledExecutionException;

    /**
     * Appends the provided tables in the column dimension.
     *
     * @param progressMonitor used to report progress
     * @param tableIdSupplier provides IDs for potentially created ContainerTables
     * @param left the left table
     * @param right the right table
     * @return the appended table
     * @throws CanceledExecutionException if execution is canceled by the user
     * @since 4.5
     */
    KnowsRowCountTable append(ExecutionMonitor progressMonitor, IntSupplier tableIdSupplier,
        final BufferedDataTable left, BufferedDataTable right) throws CanceledExecutionException;

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
     * Slices the input table according to the provided TableFilter.
     * Example: The table filter defines a column selection of 2, 5 and 7 and defines a row range from 1000 to 1005,
     * then the sliced table will have 3 columns and 6 rows (the to index in TableFilter is inclusive).
     *
     * @param exec for reporting progress and the potential creation of tables
     * @param table to slice
     * @param slice the definition of the slice
     * @param tableIdSupplier provides IDs for potentially created ContainerTables
     * @return the sliced table
     */
    KnowsRowCountTable slice(ExecutionContext exec, BufferedDataTable table, Selection slice,
        IntSupplier tableIdSupplier);

}
