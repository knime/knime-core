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

import org.knime.core.data.DataCell;
import org.knime.core.data.DataValue;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreCell;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.table.access.StringAccess.StringReadAccess;
import org.knime.core.table.access.StructAccess.StructReadAccess;
import org.knime.core.table.access.StructAccess.StructWriteAccess;
import org.knime.core.table.access.VarBinaryAccess.VarBinaryReadAccess;
import org.knime.core.table.access.VarBinaryAccess.VarBinaryWriteAccess;
import org.knime.core.table.io.ReadableDataInput;
import org.knime.core.table.io.ReadableDataInputStream;
import org.knime.core.table.schema.DataSpec;
import org.knime.core.table.schema.VarBinaryDataSpec;
import org.knime.core.table.schema.VarBinaryDataSpec.ObjectDeserializer;
import org.knime.core.table.schema.VarBinaryDataSpec.ObjectSerializer;

/**
 * Abstract class providing the basic implementation for file store based {@link ValueFactory}s.
 *
 * Serialization and deserialization of the {@link DataValue} can happen either into a {@link FileStore} or a binary
 * blob directly in the Arrow table. The selection between the two is made by querying
 * {@link TableOrFileStoreValueFactory#shouldBeStoredInFileStore(DataValue)}, which must be implemented in derived
 * classes.
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
public abstract class TableOrFileStoreValueFactory<V extends DataValue> extends AbstractFileStoreValueFactory {

    /** used to write the data either into a binary blob or a file **/
    private final ObjectSerializer<V> m_serializer;

    private final ObjectDeserializer<V> m_deserializer;

    /**
     * @param serializer Used to serialize values into bytes for the {@link FileStore} or binary blob output
     * @param deserializer Used to read values from bytes from a {@link FileStore} or binary blob input
     */
    protected TableOrFileStoreValueFactory(final ObjectSerializer<V> serializer,
        final ObjectDeserializer<V> deserializer) {
        m_serializer = (out, value) -> serializer.serialize(new UnmodifiedLongUTFDataOutput(out), value);
        m_deserializer = in -> deserializer.deserialize(new UnmodifiedLongUTFReadableDataInput(in));
    }

    @Override
    protected DataSpec getTableDataSpec() {
        return VarBinaryDataSpec.INSTANCE;
    }

    /**
     * @param value
     * @return True if the given value should be stored in a file store, e.g. because its size exceeds a threshold
     */
    protected abstract boolean shouldBeStoredInFileStore(final V value);

    /**
     * {@link ReadValue} that can be read from a FileStore or from a VarBinary blob from the table.
     *
     * Derived classes should also implement the respective DataValue.
     */
    protected abstract class TableOrFileStoreReadValue extends AbstractFileStoreReadValue<VarBinaryReadAccess> {

        /**
         * Create a {@link TableOrFileStoreReadValue}
         *
         * @param access
         */
        protected TableOrFileStoreReadValue(final StructReadAccess access) {
            super(access);
        }

        @Override
        protected final DataCell createCell(final VarBinaryReadAccess blobAccess) {
            StringReadAccess stringReadAccess = m_access.getAccess(0);

            if (stringReadAccess.isMissing()) {
                // Saved in the table
                return createCell(blobAccess.getObject(m_deserializer));
            } else {
                // Saved in the file store
                // If there is some data in the blobAccess it is the cached hash code
                Integer hashCode = blobAccess.isMissing() ? null : blobAccess.getObject(ReadableDataInput::readInt);
                return createFileStoreCell(hashCode);
            }
        }

        protected abstract DataCell createCell(V value);

        /**
         * Create a {@link ObjectSerializerFileStoreCell} with the appropriate type. The file stores are loaded into the
         * cell by the framework and will be available before the content is accessed.
         *
         * @param hashCode the cached hash code or <code>null</code> if no hash code was cached. This value should be
         *            forwarded to the constructor of {@link ObjectSerializerFileStoreCell} which will use it as the
         *            hash code if it is not <code>null</code>.
         * @return a new file store cell object
         */
        protected abstract ObjectSerializerFileStoreCell<?> createFileStoreCell(Integer hashCode); // NOSONAR: We do not care about the content type here
    }

    /**
     * A {@link TableOrFileStoreWriteValue} writes the provided value V either into a {@link FileStore} or into a
     * VarBinary blob
     */
    protected abstract class TableOrFileStoreWriteValue extends AbstractFileStoreWriteValue<V, VarBinaryWriteAccess> {

        /**
         * Create a {@link TableOrFileStoreWriteValue}
         *
         * @param access
         */
        protected TableOrFileStoreWriteValue(final StructWriteAccess access) {
            super(access);
        }

        @Override
        protected abstract ObjectSerializerFileStoreCell<?> getFileStoreCell(V value) throws IOException;

        @Override
        public void setValue(final V value) {
            if (isCorrespondingReadValue(value)) {
                copyFromReadValue((AbstractFileStoreReadValue<?>)value);
                return;
            }

            var blobAccess = getTableDataAccess();
            try {
                var fsCell = getFileStoreCell(value);
                if (fsCell != null) {
                    // Set the file store data
                    setFileStoreData(fsCell);

                    // Write the hash code
                    blobAccess.setObject(fsCell.hashCode(), DataOutput::writeInt);
                } else {
                    // Write the content to the table if it is not in a file store
                    setTableData(value, getTableDataAccess());
                }
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }

        @Override
        protected void setTableData(final V value, final VarBinaryWriteAccess access) {
            // NB: Only called when we do not write into the file store
            access.setObject(value, m_serializer);

        }

        //        @Override
        //        protected void setTableData(final V value, final StructWriteAccess structWriteAccess) {
        //            VarBinaryWriteAccess access = structWriteAccess.getWriteAccess(0);
        //            if (!shouldBeStoredInFileStore(value)) {
        //                access.setObject(value, m_serializer);
        //            }
        //            // TODO: handle hash
        //        }
        //
        //        @Override
        //        protected boolean hasFileStoreData(final V value) {
        //            /*
        //             * TODO if the node developer sets a huge value directly via #setValue which is not a cell the
        //             * implementations of shouldBeStoredInFileStore will return false. However, we might want to convert this to
        //             * be stored in a file store.
        //             * For example for JSONValue this would look like the check we do in the JSONCellFactory
        //             */
        //            return shouldBeStoredInFileStore(value);
        //        }
    }

    /**
     * An abstract implementation of {@link FileStoreCell} that uses a {@link DataOutput} to serialize data to a file
     * store (see {@link ObjectSerializerFileStoreCell#serialize(DataOutput)}) and a {@link ReadableDataInput} to load
     * data from the file store (see {@link ObjectSerializerFileStoreCell#deserialize(ReadableDataInput)}.
     */
    public abstract static class ObjectSerializerFileStoreCell<C> extends FileStoreCell {
        private final ObjectSerializer<C> m_serializer;

        private final ObjectDeserializer<C> m_deserializer;

        private C m_content;

        private Integer m_hashCode;

        protected ObjectSerializerFileStoreCell(final FileStore fs, final ObjectSerializer<C> serializer,
            final ObjectDeserializer<C> deserializer) {
            super(fs);
            m_serializer = serializer;
            m_deserializer = deserializer;
        }

        /**
         * @param hashCode the cached hash code or <code>null</code> if no hash code was cached. If <code>null</code>
         *            {@link #hashCode()} will use the hash code of the content.
         */
        protected ObjectSerializerFileStoreCell(final ObjectSerializer<C> serializer,
            final ObjectDeserializer<C> deserializer, final Integer hashCode) {
            m_serializer = serializer;
            m_deserializer = deserializer;
            m_hashCode = hashCode;
        }

        protected final void setContent(final C content) {
            m_content = content;
        }

        protected final C getContent() {
            if (m_content == null) {
                loadContent();
            }
            return m_content;
        }

        private synchronized void loadContent() {
            if (m_content == null) {
                try (final var inputStream = new ReadableDataInputStream(
                    new DataInputStream(new FileInputStream(getFileStores()[0].getFile())))) {
                    m_content = m_deserializer.deserialize(new UnmodifiedLongUTFReadableDataInput(inputStream));
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            }
        }

        @Override
        protected final void flushToFileStore() throws IOException {
            final var file = getFileStores()[0].getFile();
            synchronized (file) {
                if (!file.exists()) {
                    try (final var outputStream = new DataOutputStream(new FileOutputStream(file))) {
                        m_serializer.serialize(new UnmodifiedLongUTFDataOutput(outputStream), getContent());
                    }
                }
            }
        }

        /**
         * @Override
         */
        @Override
        protected boolean equalFileStoreContent(final DataCell dc) {
            return equalContent(dc);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            if (m_hashCode == null) {
                m_hashCode = getContent().hashCode();
            }
            return m_hashCode;
        }
    }
}
