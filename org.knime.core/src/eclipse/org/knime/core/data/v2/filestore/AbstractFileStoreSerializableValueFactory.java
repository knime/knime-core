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

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataValue;
import org.knime.core.data.IDataRepository;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreKey;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;
import org.knime.core.data.filestore.internal.NotInWorkflowWriteFileStoreHandler;
import org.knime.core.data.v2.FileStoreAwareValueFactory;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.node.NodeLogger;
import org.knime.core.table.access.StringAccess.StringReadAccess;
import org.knime.core.table.access.StringAccess.StringWriteAccess;
import org.knime.core.table.access.StructAccess.StructReadAccess;
import org.knime.core.table.access.StructAccess.StructWriteAccess;
import org.knime.core.table.access.VarBinaryAccess.VarBinaryReadAccess;
import org.knime.core.table.access.VarBinaryAccess.VarBinaryWriteAccess;
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
 * @since 4.7
 * @noreference This class is not intended to be referenced by clients.
 */
public abstract class AbstractFileStoreSerializableValueFactory<V extends DataValue>
    implements FileStoreAwareValueFactory, ValueFactory<StructReadAccess, StructWriteAccess> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(AbstractFileStoreSerializableValueFactory.class);

    private IDataRepository m_dataRepository = null;

    private IWriteFileStoreHandler m_fileStoreHandler = null;

    /** used to write the data either into a binary blob or a file **/
    private final ObjectSerializer<V> m_serializer;

    /** used to load the data either from a binary blob or a file **/
    private final ObjectDeserializer<V> m_deserializer;

    /**
     * @param serializer Used to serialize values into bytes for the {@link FileStore} or binary blob output
     * @param deserializer Used to read values from bytes from a {@link FileStore} or binary blob input
     */
    protected AbstractFileStoreSerializableValueFactory(final ObjectSerializer<V> serializer,
        final ObjectDeserializer<V> deserializer) {
        m_serializer = (out, value) -> {
            serializer.serialize(new UnmodifiedLongUTFDataOutput(out), value);
        };
        m_deserializer = (in) -> {
            return deserializer.deserialize(new UnmodifiedLongUTFReadableDataInput(in));
        };
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
    protected class FileStoreSerializableReadValue implements ReadValue {
        private final StructReadAccess m_access;

        // Used to check whether the cached DataCell needs to be updated
        private String m_lastFileStoreKey;

        // Cache the data cell if it was stored in a fileStore to make subsequent accesses fast
        private DataCell m_lastDataCell;

        /**
         * Create a {@link FileStoreSerializableReadValue}
         *
         * @param access
         */
        protected FileStoreSerializableReadValue(final StructReadAccess access) {
            m_access = access;
        }

        @Override
        public DataCell getDataCell() {
            StringReadAccess fileStoreAccess = m_access.getAccess(0);
            final String fileStoreKey = fileStoreAccess.getStringValue();

            if (fileStoreKey != null && !fileStoreKey.isEmpty()) {
                if (m_lastFileStoreKey != null && m_lastFileStoreKey.equals(fileStoreKey)) {
                    // the FSKey has not changed since the last access, we can return the cached data cell
                    return m_lastDataCell;
                }
                FileStoreKey key = FileStoreKey.fromString(fileStoreKey);
                final var fsHandler = m_dataRepository.getHandlerNotNull(key.getStoreUUID());
                final var fs = fsHandler.getFileStore(key);
                try (final var stream = new FileInputStream(fs.getFile());
                        final var dataInStream = new ReadableDataInputStream(stream)) {
                    m_lastDataCell = (DataCell)m_deserializer.deserialize(dataInStream);
                    m_lastFileStoreKey = fileStoreKey;
                    return m_lastDataCell;
                } catch (IOException | IllegalStateException ex) { // IllegalStateException can be thrown by ArrowBufIO
                    LOGGER.warn("Could not read cell value from filestore, retrying from binary blob", ex);
                }
            }

            // We either came here because no fileStore was given or reading from the fileStore failed,
            // then we can try as fallback to read from the binary blob
            VarBinaryReadAccess blobAccess = m_access.getAccess(1);
            return (DataCell)blobAccess.getObject(m_deserializer);
        }
    }

    /**
     * A {@link FileStoreSerializableWriteValue} writes the provided value V either into a {@link FileStore} or into a
     * VarBinary blob
     */
    protected class FileStoreSerializableWriteValue implements WriteValue<V> {
        private final StructWriteAccess m_access;

        /**
         * Create a {@link FileStoreSerializableWriteValue}
         *
         * @param access
         */
        protected FileStoreSerializableWriteValue(final StructWriteAccess access) {
            m_access = access;
        }

        private FileStore createFileStore() throws IOException {
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
            if (value instanceof AbstractFileStoreSerializableValueFactory.FileStoreSerializableReadValue) {
                @SuppressWarnings("rawtypes")
                var fsReadVal = (AbstractFileStoreSerializableValueFactory.FileStoreSerializableReadValue)value;
                m_access.getWriteAccess(0).setFrom(fsReadVal.m_access.getAccess(0));
                m_access.getWriteAccess(1).setFrom(fsReadVal.m_access.getAccess(1));
                return;
            }

            StringWriteAccess fileStoreAccess = m_access.getWriteAccess(0);
            VarBinaryWriteAccess blobAccess = m_access.getWriteAccess(1);

            final boolean storeInFS = shouldBeStoredInFileStore(value);

            if (storeInFS) {
                try {
                    final var fs = createFileStore();
                    try (final var stream = new FileOutputStream(fs.getFile());
                            final var dataOutStream = new DataOutputStream(stream)) {
                        m_serializer.serialize(dataOutStream, value);
                    }
                    fileStoreAccess.setStringValue(fs.toString());
                    return;
                } catch (IOException ex) {
                    LOGGER.warn("Could not write value " + value + " to fileStore, serializing to binary blob instead",
                        ex);
                }
            }

            // We either came here because writing to a fileStore was not requested or failed.
            blobAccess.setObject(value, m_serializer);
        }
    }

    @Override
    public WriteValue<?> createWriteValue(final StructWriteAccess access) {
        return new FileStoreSerializableWriteValue(access);
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
