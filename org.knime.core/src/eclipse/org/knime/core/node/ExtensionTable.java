/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 */
package org.knime.core.node;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.swing.JComponent;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.IDataRepository;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.WrappedTable;
import org.knime.core.data.container.filter.FilterDelegateRowIterator;
import org.knime.core.data.container.filter.TableFilter;
import org.knime.core.data.v2.schema.ValueSchemaLoadContext;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.BufferedDataTable.KnowsRowCountTable;

/**
 * Base class for custom buffered data table types. In addition to the contract
 * defined by {@link KnowsRowCountTable}, ExtensionTables must define a
 * constructor that accepts a single argument of type {@link LoadContext}.
 */
public abstract class ExtensionTable implements ContainerTable {

    private static final String CFG_TABLE_IMPL = "table_implementation";

    private static final String CFG_TABLE_DERIVED_SETTINGS = "table_derived_settings";

    private static final String CFG_TABLE_ID = "table_ID";

    @Override
    public CloseableRowIterator iteratorWithFilter(final TableFilter filter, final ExecutionMonitor exec) {
        return new FilterDelegateRowIterator(iterator(), filter, size(), exec);
    }

    /**
     * Various parameters needed for loading an extension table.
     */
    public static final class LoadContext implements ValueSchemaLoadContext {
        private final ReferencedFile m_dataFileRef;

        private final DataTableSpec m_tableSpec;

        private final NodeSettingsRO m_settings;

        private final Map<Integer, BufferedDataTable> m_tableRepository;

        private final ExecutionMonitor m_executionMonitor;

        private final IDataRepository m_repository;

        private final int m_tableId;

        private LoadContext(final ReferencedFile dataFileRef,
                final DataTableSpec tableSpec,
                final NodeSettingsRO settings,
                final Map<Integer, BufferedDataTable> tableRepository,
                final ExecutionMonitor executionMonitor,
                final IDataRepository repository) throws InvalidSettingsException {
            m_dataFileRef = dataFileRef;
            m_tableSpec = tableSpec;
            // previously, `AbstractColumnarContainerTable`s used ID "-1" because
            // it was not made available via the load context - now it is
            m_tableId = settings.getInt(CFG_TABLE_ID, -1);
            m_settings = settings.getNodeSettings(CFG_TABLE_DERIVED_SETTINGS);
            m_tableRepository = tableRepository;
            m_executionMonitor = executionMonitor;
            m_repository = repository;
        }

        /**
         * @return the location of the zip file where the data is stored.
         */
        public ReferencedFile getDataFileRef() {
            return m_dataFileRef;
        }

        /**
         * @return the data table spec for this table
         */
        @Override
        public DataTableSpec getTableSpec() {
            return m_tableSpec;
        }

        /**
         * @return the id of the {@link ContainerTable}
         * @since 5.9
         */
        public int getTableID() {
            return m_tableId;
        }

        /**
         * @return the settings object, will contain additional meta-data.
         */
        @Override
        public NodeSettingsRO getSettings() {
            return m_settings;
        }

        /**
         * Resolves a reference to another table by id.
         *
         * @param id the id to resolve
         * @return a reference to another table by id
         */
        public BufferedDataTable getTable(final int id) {
            return m_tableRepository.get(id);
        }

        /**
         * @return the execution monitor for progress/cancellation
         */
        public ExecutionMonitor getExecutionMonitor() {
            return m_executionMonitor;
        }

        /**
         * @return the data repository
         * @since 4.3
         */
        @Override
        public IDataRepository getDataRepository() {
            return m_repository;
        }

    }

    /**
     * Default constructor; to be used to create a new table (as opposed to
     * loading an existing table).
     */
    protected ExtensionTable() {
        // no op, overwritten by sub classes
    }

    /**
     * Constructor to be used when loading an existing table. Subclasses must
     * define a constructor of the same signature.
     *
     * @param context various parameters needed for loading
     * @throws InvalidSettingsException If settings are invalid
     * @throws IOException If reading fails
     * @throws CanceledExecutionException If canceled
     */
    protected ExtensionTable(final LoadContext context) throws IOException,
            CanceledExecutionException, InvalidSettingsException {
        // currently empty, leave room for growth
    }

    /**
     * Create the actual buffered data table.
     * @param exec The context, used to set ownership on table.
     * @return a newly created buffered data table
     * @since 4.3
     */
    public final BufferedDataTable create(final ExecutionContext exec) {
        BufferedDataTable table = new BufferedDataTable(this, exec.getDataRepository());
        table.setOwnerRecursively(exec.getNode());
        return table;
    }

    /** Overridden to help preserve backward compatibility. It will simply call {@link #getRowCount()}. Subclasses
     * are encouraged to override.
     * @since 3.0 */
    @SuppressWarnings("deprecation")
    @Override
    public long size() {
        return getRowCount();
    }

    /**
     * Overwrite this method to provide a custom viewer. It will use the default
     * viewer (BufferedDataTableViewer) if the implementation returns null
     * (default) or an empty list. Extensions may provide custom features such
     * as searching.
     * @param owner the owner of this delegate
     * @return an array of views for the port object, each displayed as a tab
     * in the out port view. Null or empty list to rely on the default view.
     */
    protected JComponent[] getViews(final BufferedDataTable owner) {
        return null;
    }

    /**
     * Provides a way to return the underlying extension table if the
     * BufferedDataTable is an extension table. Also (recursively) unwraps
     * WrappedTables in case they are wrapping an extension table.
     *
     * @param table the table to unwrap
     * @return the underlying extension table or null if this is not an
     *         extension table
     */
    protected static ExtensionTable unwrap(final BufferedDataTable table) {
        KnowsRowCountTable delegate = table.getDelegate();
        if (delegate instanceof WrappedTable) {
            return unwrap(delegate.getReferenceTables()[0]);
        } else if (delegate instanceof ExtensionTable) {
            return (ExtensionTable)delegate;
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void saveToFile(final File f, final NodeSettingsWO settings,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        settings.addString(CFG_TABLE_IMPL, getClass().getName());
        NodeSettingsWO derivedSettings =
            settings.addNodeSettings(CFG_TABLE_DERIVED_SETTINGS);
        saveToFileOverwrite(f, derivedSettings, exec);
    }

    /** Saves this extension table to the argument file.
      * @param f To write to.
      * @param settings The (derived) settings object (empty).
      * @param exec Execution monitor for cancelation, progress.
      * @throws IOException If that fails.
      * @throws CanceledExecutionException if canceled. */
    protected abstract void saveToFileOverwrite(final File f,
            final NodeSettingsWO settings, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException;


    /** Load the extension, used internally from {@link BufferedDataTable}.
     * @param fileRef To load from, it's a referenced file so that
     *        implementations can delay the reading to when it's necessary.
     * @param spec The table specification.
     * @param s The settings object
     * @param tblRep The global table map.
     * @param exec Progress monitor.
     * @return the loaded table
     * @throws InvalidSettingsException If settings are invalid
     * @throws IOException If reading fails
     * @throws CanceledExecutionException If canceled
     * @since 4.2
     * @noreference This method is not intended to be referenced by clients.
     * @deprecated Use {@link #loadExtensionTable(ReferencedFile, DataTableSpec, NodeSettingsRO, Map, ExecutionMonitor, IDataRepository)} instead.
     *        This method internally call that method and set {@link IDataRepository} to null.
     */
    @Deprecated
    public static ExtensionTable loadExtensionTable(final ReferencedFile fileRef, final DataTableSpec spec,
        final NodeSettingsRO s, final Map<Integer, BufferedDataTable> tblRep, final ExecutionMonitor exec)
        throws InvalidSettingsException, IOException, CanceledExecutionException {
        return loadExtensionTable(fileRef, spec, s, tblRep, exec, null);

    }

    /** Load the extension, used internally from {@link BufferedDataTable}.
     * @param fileRef To load from, it's a referenced file so that
     *        implementations can delay the reading to when it's necessary.
     * @param spec The table specification.
     * @param s The settings object
     * @param tblRep The global table map.
     * @param exec Progress monitor.
     * @param dataRepository IDataRepository for FileStoreCells and BlobCells
     * @return the loaded table
     * @throws InvalidSettingsException If settings are invalid
     * @since 4.3
     * @noreference This method is not intended to be referenced by clients.
     */
    public static ExtensionTable loadExtensionTable(final ReferencedFile fileRef, final DataTableSpec spec,
        final NodeSettingsRO s, final Map<Integer, BufferedDataTable> tblRep, final ExecutionMonitor exec,
        final IDataRepository dataRepository)
        throws InvalidSettingsException {

        final var context = new LoadContext(fileRef, spec, s, tblRep, exec, dataRepository);
        return ExtensionTableRegistry.getInstance().loadExtensionTable(context, s.getString(CFG_TABLE_IMPL));

    }

}
