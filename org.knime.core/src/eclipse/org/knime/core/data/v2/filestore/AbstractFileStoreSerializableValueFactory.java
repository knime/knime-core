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

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataValue;
import org.knime.core.data.IDataRepository;
import org.knime.core.data.container.BlobDataCell;
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
import org.knime.core.table.access.StringAccess.StringReadAccess;
import org.knime.core.table.access.StringAccess.StringWriteAccess;
import org.knime.core.table.access.StructAccess.StructReadAccess;
import org.knime.core.table.access.StructAccess.StructWriteAccess;
import org.knime.core.table.access.VarBinaryAccess.VarBinaryReadAccess;
import org.knime.core.table.access.VarBinaryAccess.VarBinaryWriteAccess;
import org.knime.core.table.io.ReadableDataInput;
import org.knime.core.table.io.ReadableDataInputStream;
import org.knime.core.table.schema.DataSpec;
import org.knime.core.table.schema.StringDataSpec;
import org.knime.core.table.schema.StructDataSpec;
import org.knime.core.table.schema.VarBinaryDataSpec;
import org.knime.core.table.schema.VarBinaryDataSpec.ObjectDeserializer;
import org.knime.core.table.schema.VarBinaryDataSpec.ObjectSerializer;

/**
 * Abstract class providing the basic implementation for file store based {@link ValueFactory}s.
 *
 * Serialization and deserialization of the {@link DataValue} can happen either into a {@link FileStore} or a binary
 * blob directly in the Arrow table. The selection between the two is made by querying
 * {@link AbstractFileStoreSerializableValueFactory#shouldBeStoredInFileStore(DataValue)}, which must be implemented in
 * derived classes.
 *
 * Derived classes should specialize the ReadValue and also implement the {@link DataValue} V. To make this as explicit
 * as possible, the createReadValue method is abstract here.
 *
 * Serializer and deserializer must be provided in the constructor.
 *
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 * @param <V> The type of {@link DataValue} read and written by this {@link ValueFactory}
 * @since 5.1
 * @noreference This class is not intended to be referenced by clients.
 */
public abstract class AbstractFileStoreSerializableValueFactory<V extends DataValue>
    implements FileStoreAwareValueFactory, ValueFactory<StructReadAccess, StructWriteAccess> {

    private IDataRepository m_dataRepository;

    private IWriteFileStoreHandler m_fileStoreHandler;

    /** used to write the data either into a binary blob or a file **/
    private final ObjectSerializer<V> m_serializer;

    private final ObjectDeserializer<V> m_deserializer;

    /**
     * @param serializer Used to serialize values into bytes for the {@link FileStore} or binary blob output
     * @param deserializer Used to read values from bytes from a {@link FileStore} or binary blob input
     */
    protected AbstractFileStoreSerializableValueFactory(final ObjectSerializer<V> serializer,
        final ObjectDeserializer<V> deserializer) {
        m_serializer = (out, value) -> serializer.serialize(new UnmodifiedLongUTFDataOutput(out), value);
        m_deserializer = in -> deserializer.deserialize(new UnmodifiedLongUTFReadableDataInput(in));
    }

    @Override
    public DataSpec getSpec() {
        return new StructDataSpec(StringDataSpec.INSTANCE, VarBinaryDataSpec.INSTANCE);
    }

    /**
     * @param value
     * @return True if the given value should be stored in a file store, e.g. because its size exceeds a threshold
     */
    protected abstract boolean shouldBeStoredInFileStore(final V value);

    /**
     * {@link ReadValue} that can be read from a FileStore or from a VarBinary blob.
     *
     * Derived classes should also implement the respective DataValue.
     */
    protected abstract class FileStoreSerializableReadValue implements ReadValue {
        private final StructReadAccess m_access;

        //////////////////////////////////////////////////////////////////////////////////////
        // TODO: improve this caching from the single last cell to a LRU cache that is connected to the MemoryAlertSystem
        // TODO: later: make this cache global/static so it works across columns

        // Used to check whether the cached DataCell needs to be updated
        private String m_lastFileStoreKeyString;

        // Cache the data cell if it was stored in a fileStore to make subsequent accesses fast
        private DataCell m_lastDataCell;
        //////////////////////////////////////////////////////////////////////////////////////

        /**
         * Create a {@link FileStoreSerializableReadValue}
         *
         * @param access
         */
        protected FileStoreSerializableReadValue(final StructReadAccess access) {
            m_access = access;
        }

        protected abstract DataCell createCell(V value);

        protected abstract FileStoreCell createFileStoreCell();

        @Override
        public DataCell getDataCell() {
            StringReadAccess fileStoreAccess = m_access.getAccess(0);
            final var fileStoreKeyString = fileStoreAccess.getStringValue();

            if (fileStoreKeyString != null && !fileStoreKeyString.isEmpty()) {
                if (m_lastFileStoreKeyString == null || !m_lastFileStoreKeyString.equals(fileStoreKeyString)) {
                    var fileStoreKeys = Arrays.stream(fileStoreKeyString.split(";")).map(FileStoreKey::fromString)
                        .toArray(FileStoreKey[]::new);
                    // the FSKeys have changed since the last access, so we cannot reuse the cached data cell

                    m_lastDataCell = createFileStoreCell();
                    try {
                        FileStoreUtil.retrieveFileStoreHandlersFrom((FileStoreCell)m_lastDataCell, fileStoreKeys,
                            m_dataRepository);
                    } catch (IOException ex) {
                        throw new IllegalStateException("Could not read cell from fileStores: ", ex);
                    }
                }

                return m_lastDataCell;
            }

            // We either came here because no fileStore was given or reading from the fileStore failed,
            // then we can try as fallback to read from the binary blob
            VarBinaryReadAccess blobAccess = m_access.getAccess(1);

            return createCell(blobAccess.getObject(m_deserializer));
        }
    }

    /**
     * A {@link FileStoreSerializableWriteValue} writes the provided value V either into a {@link FileStore} or into a
     * VarBinary blob
     */
    protected abstract class FileStoreSerializableWriteValue implements WriteValue<V> {
        private final StructWriteAccess m_access;

        /**
         * Create a {@link FileStoreSerializableWriteValue}
         *
         * @param access
         */
        protected FileStoreSerializableWriteValue(final StructWriteAccess access) {
            m_access = access;
        }

        protected FileStore createFileStore() throws IOException {
            final var uuid = UUID.randomUUID().toString();

            if (m_fileStoreHandler instanceof NotInWorkflowWriteFileStoreHandler) {
                // If we have a NotInWorkflowWriteFileStoreHandler then we are only creating a temporary copy of the
                // table (e.g. for the Python Script Dialog) and don't need nested loop information anyways.
                return m_fileStoreHandler.createFileStore(uuid, null, -1);
            } else {
                return m_fileStoreHandler.createFileStore(uuid);
            }
        }

        @Override
        public void setValue(final V value) {
            // addressing the type with outer class because we don't care for the generic type V
            if (value instanceof AbstractFileStoreSerializableValueFactory.FileStoreSerializableReadValue) { // NOSONAR
                @SuppressWarnings("rawtypes")
                var fsReadVal = (AbstractFileStoreSerializableValueFactory.FileStoreSerializableReadValue)value;
                m_access.getWriteAccess(0).setFrom(fsReadVal.m_access.getAccess(0));
                m_access.getWriteAccess(1).setFrom(fsReadVal.m_access.getAccess(1));
                return;
            }

            StringWriteAccess fileStoreAccess = m_access.getWriteAccess(0);

            final boolean storeInFS = shouldBeStoredInFileStore(value);

            if (storeInFS) {
                FileStoreCell fsCell;
                if (value instanceof BlobDataCell blobCell) {
                    fsCell = blobToFileStore(blobCell);
                } else {
                    fsCell = (FileStoreCell)value;

                    if (mustBeFlushedPriorSave(fsCell)) {
                        try {
                            final var fileStores = FileStoreUtil.getFileStores(fsCell);
                            final var fileStoreKeys = new FileStoreKey[fileStores.length];

                            for (var fileStoreIndex = 0; fileStoreIndex < fileStoreKeys.length; fileStoreIndex++) {
                                fileStoreKeys[fileStoreIndex] =
                                    m_fileStoreHandler.translateToLocal(fileStores[fileStoreIndex], fsCell);
                            }

                            // update file store keys without calling post-construct.
                            FileStoreUtil.retrieveFileStoreHandlersFrom(fsCell, fileStoreKeys, m_dataRepository, false);
                        } catch (IOException ex) {
                            throw new IllegalStateException(ex);
                        }
                    }
                }

                try {
                    FileStoreUtil.invokeFlush(fsCell);
                } catch (IOException ex) {
                    throw new IllegalStateException("Couldn't flush to file store", ex);
                }

                var fileStoreKeys = FileStoreUtil.getFileStoreKeys(fsCell);
                var fileStoreKeyString = Arrays.stream(fileStoreKeys)//
                    .map(FileStoreKey::toString)//
                    .collect(Collectors.joining(";"));
                fileStoreAccess.setStringValue(fileStoreKeyString);
                return;
            }

            // We either came here because writing to a fileStore was not requested or failed.
            VarBinaryWriteAccess blobAccess = m_access.getWriteAccess(1);
            blobAccess.setObject(value, m_serializer);
        }

        protected abstract FileStoreCell createFileStoreCell(V content);

        @SuppressWarnings("unchecked")
        private FileStoreCell blobToFileStore(final BlobDataCell blobCell) {
            return createFileStoreCell((V)blobCell);
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

    public abstract static class AbstractFileStoreSerializableCell extends FileStoreCell {
        protected abstract void serialize(DataOutput output);

        protected abstract void deserialize(ReadableDataInput input);

        protected AbstractFileStoreSerializableCell(final FileStore fs) {
            super(fs);
        }

        protected AbstractFileStoreSerializableCell() {
            super();
        }

        @Override
        protected final void flushToFileStore() throws IOException {
            final var file = getFileStores()[0].getFile();
            synchronized (file) {
                if (!file.exists()) {
                    try (final var outputStream = new DataOutputStream(new FileOutputStream(file))) {
                        serialize(new UnmodifiedLongUTFDataOutput(outputStream));
                    }
                }
            }
        }

        @Override
        protected void postConstruct() throws IOException {
            try (final var inputStream =
                new ReadableDataInputStream(new DataInputStream(new FileInputStream(getFileStores()[0].getFile())))) {
                deserialize(new UnmodifiedLongUTFReadableDataInput(inputStream));
            } catch (IOException ex) {
                throw new IllegalStateException("Could not open the FileStore", ex);
            }
        }
    }

    @Override
    public void initializeForReading(final IDataRepository repository) {
        m_dataRepository = repository;
    }

    @Override
    public void initializeForWriting(final IWriteFileStoreHandler fileStoreHandler) {
        m_dataRepository = fileStoreHandler.getDataRepository();
        m_fileStoreHandler = fileStoreHandler;
    }

}
