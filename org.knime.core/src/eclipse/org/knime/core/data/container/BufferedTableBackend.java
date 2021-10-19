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

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.IDataRepository;
import org.knime.core.data.TableBackend;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;
import org.knime.core.data.filestore.internal.NotInWorkflowWriteFileStoreHandler;
import org.knime.core.data.v2.RowContainer;
import org.knime.core.data.v2.RowKeyType;
import org.knime.core.data.v2.schema.ValueSchema;
import org.knime.core.data.v2.schema.ValueSchemaUtils;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTable.KnowsRowCountTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;

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
        return "Default";
    }

    @Override
    public String getDescription() {
        return new StringBuilder("<html><body>") //
            .append("<p>Default table backend, which supports all possible data types. <br />") //
            .append("<br /><br /><br /><br /><br /><br />") //
            .append("</body></html>") //
            .toString();
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
    public KnowsRowCountTable concatenate(final ExecutionMonitor exec, final IntSupplier tableIdSupplier,
        final String rowKeyDuplicateSuffix, final boolean duplicatesPreCheck, final BufferedDataTable... tables) throws CanceledExecutionException {
        if (duplicatesPreCheck && rowKeyDuplicateSuffix == null) {
            return ConcatenateTable.create(exec, tables);
        } else {
            return ConcatenateTable.create(exec, Optional.ofNullable(rowKeyDuplicateSuffix), duplicatesPreCheck,
                tables);
        }
    }

    @Override
    public KnowsRowCountTable append(final ExecutionMonitor exec, final IntSupplier tableIdSupplier,
        final BufferedDataTable left, final BufferedDataTable right)
        throws CanceledExecutionException {
        return JoinedTable.create(left, right, exec);
    }

    @Override
    public KnowsRowCountTable rearrange(final ExecutionMonitor progressMonitor,
        final IntSupplier tableIdSupplier, final ColumnRearranger columnRearranger,
        final BufferedDataTable table, final ExecutionContext context)
        throws CanceledExecutionException {
        return RearrangeColumnsTable.create(columnRearranger, table, progressMonitor, context);
    }

}
