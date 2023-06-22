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
 *   26 Oct 2022 (Carsten Haubold): created
 */
package org.knime.core.data.v2.filestore;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataValue;
import org.knime.core.data.IDataRepository;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreCell;
import org.knime.core.data.filestore.FileStoreKey;
import org.knime.core.data.filestore.FileStoreUtil;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;
import org.knime.core.data.filestore.internal.NotInWorkflowWriteFileStoreHandler;
import org.knime.core.data.v2.FileStoreAwareValueFactory;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.table.access.ReadAccess;
import org.knime.core.table.access.StringAccess.StringReadAccess;
import org.knime.core.table.access.StringAccess.StringWriteAccess;
import org.knime.core.table.access.StructAccess.StructReadAccess;
import org.knime.core.table.access.StructAccess.StructWriteAccess;
import org.knime.core.table.access.WriteAccess;
import org.knime.core.table.schema.DataSpec;
import org.knime.core.table.schema.StringDataSpec;
import org.knime.core.table.schema.StructDataSpec;

/**
 * @since 5.1
 */
public abstract class AbstractFileStoreValueFactory
    implements FileStoreAwareValueFactory, ValueFactory<StructReadAccess, StructWriteAccess> {

    private IDataRepository m_dataRepository;

    private IWriteFileStoreHandler m_fileStoreHandler;

    /**
     * @return the {@link DataSpec} of data that should be stored inside the table (next to the file store key)
     */
    protected abstract DataSpec getTableDataSpec();

    @Override
    public final DataSpec getSpec() {
        return new StructDataSpec( //
            StringDataSpec.INSTANCE, // file store keys
            getTableDataSpec() // custom data
        );
    }

    @Override
    public final void initializeForReading(final IDataRepository repository) {
        m_dataRepository = repository;
    }

    @Override
    public final void initializeForWriting(final IWriteFileStoreHandler fileStoreHandler) {
        m_dataRepository = fileStoreHandler.getDataRepository();
        m_fileStoreHandler = fileStoreHandler;
    }

    protected abstract class AbstractFileStoreReadValue<R extends ReadAccess> implements ReadValue {

        final StructReadAccess m_access;

        public AbstractFileStoreReadValue(final StructReadAccess access) {
            m_access = access;
        }

        protected R getTableDataAccess() {
            return m_access.getAccess(1);
        }

        @Override
        public final DataCell getDataCell() {
            // TODO do we have to handle missing cells here?
            //            if (m_access.isMissing()) {
            //                return new MissingCell("");
            //            }

            var cell = createCell(getTableDataAccess());

            if (cell instanceof FileStoreCell fsCell) {
                var fileStoreAccess = (StringReadAccess)m_access.getAccess(0);
                final var fileStoreKeyString = fileStoreAccess.getStringValue();

                // Load the file store into the cell if we have a key
                if (fileStoreKeyString != null && !fileStoreKeyString.isEmpty()) {

                    // TODO re-introduce caching
                    var fileStoreKeys = Arrays.stream(fileStoreKeyString.split(";")).map(FileStoreKey::load)
                        .toArray(FileStoreKey[]::new);

                    try {
                        FileStoreUtil.retrieveFileStoreHandlersFrom(fsCell, fileStoreKeys, m_dataRepository);
                    } catch (IOException ex) {
                        throw new IllegalStateException("Could not read cell from fileStores: ", ex);
                    }

                }
            }

            return cell;
        }

        protected abstract DataCell createCell(R r);
    }

    protected abstract class AbstractFileStoreWriteValue<V extends DataValue, W extends WriteAccess>
        implements WriteValue<V> {

        private final StructWriteAccess m_access;

        protected AbstractFileStoreWriteValue(final StructWriteAccess access) {
            m_access = access;
        }

        protected abstract boolean isCorrespondingReadValue(V value);

        /**
         * Get the appropriate {@link FileStoreCell} for the given value. If the value implements the correct
         * {@link FileStoreCell} class already, the method should return the value again and should refer from creating
         * a new cell. Use {@link #createFileStore()} to create file stores for the cell.
         *
         * @param value the value that should be used
         * @return a {@link FileStoreCell}
         * @throws IOException if creating file stores failed
         */
        protected abstract FileStoreCell getFileStoreCell(V value) throws IOException;

        protected abstract void setTableData(V value, W access);

        protected W getTableDataAccess() {
            return m_access.getWriteAccess(1);
        }

        @Override
        public void setValue(final V value) {
            if (isCorrespondingReadValue(value)) {
                copyFromReadValue((AbstractFileStoreReadValue<?>)value);
                return;
            }
            try {
                setFileStoreData(getFileStoreCell(value));
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
            setTableData(value, m_access.getWriteAccess(1));
        }

        /**
         * Copies the data from the given corresponding read value to the {@link WriteAccess} of this value. The caller
         * has to make sure that the {@link ReadAccess} of the given value has the same spec as the {@link WriteAccess}
         * of this value.
         *
         * @param value the value which {@link ReadAccess} will be copied over
         */
        protected void copyFromReadValue(final AbstractFileStoreReadValue<?> value) {
            m_access.setFrom((value).m_access);
        }

        protected final void setFileStoreData(final FileStoreCell fsCell) throws IOException {
            if (mustBeFlushedPriorSave(fsCell)) {
                final var fileStores = FileStoreUtil.getFileStores(fsCell);
                final var fileStoreKeys = new FileStoreKey[fileStores.length];

                for (var fileStoreIndex = 0; fileStoreIndex < fileStoreKeys.length; fileStoreIndex++) {
                    fileStoreKeys[fileStoreIndex] =
                        m_fileStoreHandler.translateToLocal(fileStores[fileStoreIndex], fsCell);
                }

                // update file store keys without calling post-construct.
                FileStoreUtil.retrieveFileStoreHandlersFrom(fsCell, fileStoreKeys, m_dataRepository, false);
            }
            FileStoreUtil.invokeFlush(fsCell);

            // Save the file store key to the table
            var fileStoreKeys = FileStoreUtil.getFileStoreKeys(fsCell);
            var fileStoreKeyString = Arrays.stream(fileStoreKeys)//
                .map(FileStoreKey::saveToString)//
                .collect(Collectors.joining(";"));
            ((StringWriteAccess)m_access.getWriteAccess(0)).setStringValue(fileStoreKeyString);
        }

        protected final FileStore createFileStore() throws IOException {
            final var uuid = UUID.randomUUID().toString();

            if (m_fileStoreHandler instanceof NotInWorkflowWriteFileStoreHandler) {
                // If we have a NotInWorkflowWriteFileStoreHandler then we are only creating a temporary copy of the
                // table (e.g. for the Python Script Dialog) and don't need nested loop information anyways.
                return m_fileStoreHandler.createFileStore(uuid, null, -1);
            } else {
                return m_fileStoreHandler.createFileStore(uuid);
            }
        }

        private boolean mustBeFlushedPriorSave(final FileStoreCell cell) {
            final FileStore[] fileStores = FileStoreUtil.getFileStores(cell);
            for (FileStore fs : fileStores) {
                if (m_fileStoreHandler.mustBeFlushedPriorSave(fs)) {
                    return true;
                }
            }
            return false;
        }
    }
}
