package org.knime.core.data.v2.value.cell;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataTypeRegistry;
import org.knime.core.data.container.BlobDataCell;
import org.knime.core.data.container.BlobDataCell.BlobAddress;
import org.knime.core.data.container.LongUTFDataInputStream;
import org.knime.core.data.container.LongUTFDataOutputStream;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreCell;
import org.knime.core.data.v2.DataCellSerializerFactory;
import org.knime.core.data.v2.DataCellSerializerFactory.DataCellSerializerInfo;

/**
 * Class to wrap {@link BlobDataCell}s as {@link FileStoreCell}s.
 *
 * @author Christian Dietz, KNIME GmbH, Grunbach, Germany
 * @apiNote no API
 */
final class BlobFileStoreCell extends FileStoreCell {
    private static final long serialVersionUID = 1L;

    private DataCellSerializer<BlobDataCell> m_deserializer;

    private DataCellSerializer<BlobDataCell> m_serializer;

    private SoftReference<BlobDataCell> m_ref;

    private BlobDataCell m_cell;

    private final byte m_serializerIdx;

    static {
        DataTypeRegistry.getInstance().addRuntimeSerializer(BlobFileStoreCell.class,
            BlobFileStoreCellSerializer.INSTANCE);
    }

    // For reading
    BlobFileStoreCell(final byte serializerIdx) {
        super();
        m_serializerIdx = serializerIdx;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    // For writing
    BlobFileStoreCell(final BlobDataCell cell, //
        final FileStore store, //
        final DataCellSerializerFactory factory) {
        super(store);
        final DataCellSerializerInfo info = factory.getSerializer(cell);
        m_serializerIdx = info.getInternalIndex();
        m_serializer = (DataCellSerializer)info.getSerializer();
        m_cell = cell;
    }

    // needs to be called after deserialization
    @SuppressWarnings({"unchecked", "rawtypes"})
    void init(final DataCellSerializerFactory factory) {
        m_serializer = (DataCellSerializer)factory.getSerializerByIdx(m_serializerIdx).getSerializer();
    }

    synchronized BlobDataCell get() {
        // get it from hard reference
        if (m_cell != null) {
            return m_cell;
        }

        // get it from weak reference
        if (m_ref != null) {
            final BlobDataCell cell = m_ref.get();
            if (cell != null) {
                return cell;
            }
        }

        // deserialize and make it a weak reference for future calls
        try (FileBasedDataCellDataInput input = new FileBasedDataCellDataInput(getFileStores()[0].getFile())) {
            final BlobDataCell deserialized = m_deserializer.deserialize(input);
            BlobAddress.setFileStore(deserialized, getFileStores()[0]);
            m_ref = new SoftReference<BlobDataCell>(deserialized);
            return deserialized;
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    protected synchronized void flushToFileStore() throws IOException {
        final FileStore store = getFileStores()[0];
        if (store.getFile().length() == 0) {
            try (final FileBasedDataCellDataOutput in = new FileBasedDataCellDataOutput(store.getFile())) {
                m_serializer.serialize(m_cell, in);
                in.flush();
            }
        }
        m_ref = new SoftReference<BlobDataCell>(m_cell);
        m_cell = null;
    }

    static final class FileBasedDataCellDataInput extends LongUTFDataInputStream implements DataCellDataInput {

        @SuppressWarnings("resource")
        public FileBasedDataCellDataInput(final File f) throws FileNotFoundException {
            super(new DataInputStream(new BufferedInputStream(new FileInputStream(f))));
        }

        @Override
        public DataCell readDataCell() throws IOException {
            throw new UnsupportedOperationException("Not implemented.");
        }

    }

    static final class FileBasedDataCellDataOutput extends LongUTFDataOutputStream implements DataCellDataOutput {

        @SuppressWarnings("resource")
        public FileBasedDataCellDataOutput(final File f) throws FileNotFoundException {
            super(new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f))));
        }

        @Override
        public void writeDataCell(final DataCell cell) throws IOException {
            throw new UnsupportedOperationException("Not implemented.");
        }

    }

    static class BlobFileStoreCellSerializer implements DataCellSerializer<BlobFileStoreCell> {
        final static BlobFileStoreCellSerializer INSTANCE = new BlobFileStoreCellSerializer();

        private BlobFileStoreCellSerializer() {
        }

        @Override
        public void serialize(final BlobFileStoreCell cell, final DataCellDataOutput output) throws IOException {
            output.writeByte(cell.m_serializerIdx);
        }

        @Override
        public BlobFileStoreCell deserialize(final DataCellDataInput input) throws IOException {
            return new BlobFileStoreCell(input.readByte());
        }
    }
}