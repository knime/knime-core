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
 *   4 Jul 2022 (Carsten Haubold): created
 */
package org.knime.core.data.v2.value;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.knime.core.data.AdapterCell;
import org.knime.core.data.AdapterValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataTypeRegistry;
import org.knime.core.data.DataValue;
import org.knime.core.data.IDataRepository;
import org.knime.core.data.MissingValue;
import org.knime.core.data.container.LongUTFDataInputStream;
import org.knime.core.data.container.LongUTFDataOutputStream;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;
import org.knime.core.data.v2.FileStoreAwareValueFactory;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.data.v2.value.cell.AbstractDataInputDelegator;
import org.knime.core.data.v2.value.cell.AbstractDataOutputDelegator;
import org.knime.core.data.v2.value.cell.DictEncodedDataCellDataInputDelegator;
import org.knime.core.table.access.ReadAccess;
import org.knime.core.table.access.StructAccess.StructReadAccess;
import org.knime.core.table.access.StructAccess.StructWriteAccess;
import org.knime.core.table.access.VarBinaryAccess.VarBinaryReadAccess;
import org.knime.core.table.access.VarBinaryAccess.VarBinaryWriteAccess;
import org.knime.core.table.access.WriteAccess;
import org.knime.core.table.io.ReadableDataInput;
import org.knime.core.table.io.ReadableDataInputStream;
import org.knime.core.table.schema.DataSpec;
import org.knime.core.table.schema.StringDataSpec;
import org.knime.core.table.schema.StructDataSpec;
import org.knime.core.table.schema.VarBinaryDataSpec;

/**
 * ValueFactory for {@link AdapterCell}s. Each AdapterCell is represented in the columnar backend as a struct with two
 * columns: first its "primary" value which allows e.g. reading the value in Python, and second a binary blob containing
 * its serialized adapter values.
 *
 * @since 4.6
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 */
public abstract class AbstractAdapterCellValueFactory
    implements ValueFactory<StructReadAccess, StructWriteAccess>, FileStoreAwareValueFactory { // NOSONAR: cannot be removed

    private static final int VERSION = 1;

    /**
     * The {@link IDataRepository} is used when reading cells with {@link FileStore}s. Is only available after
     * {@link #initializeForReading(IDataRepository)} or {@link #initializeForWriting(IWriteFileStoreHandler)} has been
     * called.
     */
    protected IDataRepository m_dataRepository;

    /**
     * The {@link IWriteFileStoreHandler} is used when writing cells with {@link FileStore}s. Is only available after
     * {@link #initializeForWriting(IWriteFileStoreHandler)} has been called.
     */
    protected IWriteFileStoreHandler m_writeFileStoreHandler;

    @Override
    public final DataSpec getSpec() {
        return new StructDataSpec(getPrimarySpec(), VarBinaryDataSpec.INSTANCE);
    }

    /**
     * @return The data spec used to serialize the primary value type of this {@link AdapterCell}, e.g. for an
     *         SdfAdapterCell this would be {@link StringDataSpec} to serialize the SdfValue as a string.
     */
    protected abstract DataSpec getPrimarySpec();

    @Override
    public void initializeForReading(final IDataRepository repository) {
        m_dataRepository = repository;
    }

    @Override
    public void initializeForWriting(final IWriteFileStoreHandler fileStoreHandler) {
        m_dataRepository = fileStoreHandler.getDataRepository();
        m_writeFileStoreHandler = fileStoreHandler;
    }

    /**
     * This base implementation of {@link WriteValue} for {@link AdapterCell}s takes care of serializing the
     * {@link AdapterValue}s using their serializers from before-Columnar-Backend times. Derived classes need to
     * implement {@link #setPrimaryValue(DataValue, WriteAccess)} to configure how the primary value should be stored.
     *
     * @param <T> The type of the adapter value that can be written
     */
    public abstract static class AbstractAdapterCellWriteValue<T extends DataValue & AdapterValue>
        implements WriteValue<T> {
        private final StructWriteAccess m_access;

        private IWriteFileStoreHandler m_writeFileStoreHandler;

        /**
         * Construct an {@link AbstractAdapterCellWriteValue}
         *
         * @param access The struct access to populate with primary value and serialized adapter values
         * @param fsHandler The file store handler used in case {@link FileStore}s are written during serialization.
         */
        protected AbstractAdapterCellWriteValue(final StructWriteAccess access,
            final IWriteFileStoreHandler fsHandler) {
            m_access = access;
            m_writeFileStoreHandler = fsHandler;
        }

        @Override
        public final void setValue(final T value) {
            if (value instanceof AbstractAdapterCellReadValue readValue) {
                m_access.getWriteAccess(0).setFrom(readValue.m_access.getAccess(0));
                m_access.getWriteAccess(1).setFrom(readValue.m_access.getAccess(1));
                return;
            }

            setPrimaryValue(value, m_access.getWriteAccess(0));

            final var blobAccess = (VarBinaryWriteAccess)m_access.getWriteAccess(1);

            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            final var adapterValueToCellMap = value.getAdapterMap();

            // Invert the map so we only write a cell once, even if it is registered for multiple values.
            // We have to use an identity hash map because the equals method on DataCells might return true
            // even though there are different underlying cells (e.g. adapter cell vs normal cell) and we
            // do not want to miss any of the contained data.
            final Map<DataCell, List<Class<? extends DataValue>>> adapterCellToValuesMap = new IdentityHashMap<>();
            for (var entry : adapterValueToCellMap.entrySet()) {
                final var adapterValue = entry.getKey();
                final var adapterCell = entry.getValue();
                adapterCellToValuesMap.computeIfAbsent(adapterCell, k -> new ArrayList<>());
                adapterCellToValuesMap.get(adapterCell).add(adapterValue);
            }

            try (final var intermediateOutput = new DataOutputStream(outStream);
                    final var longUtfOutStream = new LongUTFDataOutputStream(intermediateOutput);
                    final var dataOutput =
                        new AdapterCellDataOutputDelegator(m_writeFileStoreHandler, longUtfOutStream)) {
                // Since KNIME 5.1 we first write a -1 instead of the num adapters to be able to differentiate
                // before/after opening the correct type of stream.
                dataOutput.writeInt(-1);
                dataOutput.writeByte(VERSION);
                dataOutput.writeInt(adapterCellToValuesMap.size());
                for (var entry : adapterCellToValuesMap.entrySet()) {
                    final var adapterCell = entry.getKey();
                    final var adapterValues = entry.getValue();
                    dataOutput.writeInt(adapterValues.size());
                    for (var adapterValue : adapterValues) {
                        dataOutput.writeUTF(adapterValue.getName());
                    }
                    dataOutput.writeDataCell(adapterCell);
                }
            } catch (IOException e) {
                throw new IllegalStateException("Could not save AdapterCell", e);
            }
            blobAccess.setByteArray(outStream.toByteArray());
        }

        /**
         * Derived classes must implement {@link #setPrimaryValue(DataValue, WriteAccess)} to specify how the provided
         * value should be stored in the columnar backend. {@link AdapterValue}s do not need to be handled here, this
         * happens automaticaly in {@link #setValue(DataValue)}.
         *
         * @param value The primary value to write.
         * @param writeAccess The {@link WriteAccess} where to write the primary value.
         */
        protected abstract void setPrimaryValue(final T value, final WriteAccess writeAccess);
    }

    /**
     * This base implementation of {@link ReadValue} for {@link AdapterCell}s takes care of deserializing the
     * {@link AdapterValue}s using their deserializers from before-Columnar-Backend times. Derived classes need to
     * implement #getAdapterCell to specify how to construct an {@link AdapterCell} for the primary value.
     */
    public abstract static class AbstractAdapterCellReadValue implements ReadValue, AdapterValue {
        private final StructReadAccess m_access;

        private final IDataRepository m_dataRepository;

        /**
         * Construct an {@link AbstractAdapterCellReadValue}.
         *
         * @param access The struct {@link ReadAccess} containing primary value and serialized {@link AdapterValue}s
         * @param dataRepository The {@link IDataRepository} that is used while deserializing {@link FileStore} based
         *            cells
         */
        protected AbstractAdapterCellReadValue(final StructReadAccess access, final IDataRepository dataRepository) {
            m_access = access;
            m_dataRepository = dataRepository;
        }

        /**
         * Access to the primary value of the {@link AdapterCell}.
         *
         * @return primary value of the {@link AdapterCell}
         * @since 5.5
         */
        protected ReadAccess getPrimaryAccess() {
            return m_access.getAccess(0);
        }

        @Override
        public final DataCell getDataCell() {
            var cell = getAdapterCell(m_access.getAccess(0));
            final var blobAccess = (VarBinaryReadAccess)m_access.getAccess(1);

            if (null == blobAccess.getByteArray()) {
                return cell;
            }

            var inStream = new ByteArrayInputStream(blobAccess.getByteArray());
            try (final var dataInputStream = new DataInputStream(inStream)) {
                // before version 5.1 we didn't store a VERSION field and were using a different stream
                int numAdapters = dataInputStream.readInt(); // NOSONAR
                if (numAdapters < 0) {
                    // This is the marker for 5.1 and later: read version and (real) num adapters
                    final int version = dataInputStream.readByte(); // NOSONAR

                    if (version != VERSION) {
                        throw new IllegalStateException(
                            "Encountered unknown AdapterCell serializer version " + version);
                    }

                    numAdapters = dataInputStream.readInt();
                    try (final var readableInputStream = new ReadableLongUTFDataInputStream(dataInputStream)) {
                        cell = readAdapters(cell, numAdapters, readableInputStream);
                    }
                } else {
                    // before 5.1 we only used a DataOutputStream and no LongUTF version
                    try (final var readableInputStream = new ReadableDataInputStream(dataInputStream)) {
                        cell = readAdapters(cell, numAdapters, readableInputStream);
                    }
                }

            } catch (IOException e) {
                throw new IllegalStateException("Could not save AdapterCell", e);
            }

            return cell;
        }

        // due to raw types in cloneAndAddAdapter signature
        @SuppressWarnings("unchecked")
        private AdapterCell readAdapters(AdapterCell cell, final int numAdapters,
            final ReadableDataInput readableDataInput) throws IOException {
            try (final var dataInputDelegator =
                new AdapterCellDataInputDelegator(m_dataRepository, readableDataInput)) {
                for (int adapterIdx = 0; adapterIdx < numAdapters; adapterIdx++) { // NOSONAR
                    int numValues = dataInputDelegator.readInt(); // NOSONAR
                    var adapterValues = new ArrayList<Class<? extends DataValue>>();
                    for (int v = 0; v < numValues; v++) { // NOSONAR
                        var adapterValueName = dataInputDelegator.readUTF();
                        final var adapterValue = DataTypeRegistry.getInstance().getValueClass(adapterValueName)
                            .orElseThrow(() -> new IOException("Did not know how to read " + adapterValueName));
                        adapterValues.add(adapterValue);
                    }
                    var adapterCell = dataInputDelegator.readDataCell();

                    for (var adapterValue : adapterValues) {
                        cell = cell.cloneAndAddAdapter(adapterCell, adapterValue);
                    }
                }
                return cell;
            }
        }

        /**
         * Derived classes must implement this class to read the primary value from the {@link ReadAccess} and construct
         * an {@link AdapterCell} from that. All additional {@link AdapterValue}s will be appended in the
         * {@link #getDataCell()} method.
         *
         * @param readAccess The {@link ReadAccess} providing the primary value
         * @return The {@link AdapterCell} created from the primary value, still without any {@link AdapterValue}s.
         */
        protected abstract AdapterCell getAdapterCell(ReadAccess readAccess);

        @Override
        public <V extends DataValue> boolean isAdaptable(final Class<V> valueClass) {
            return ((AdapterValue)getDataCell()).isAdaptable(valueClass);
        }

        @Override
        public <V extends DataValue> V getAdapter(final Class<V> valueClass) {
            return ((AdapterValue)getDataCell()).getAdapter(valueClass);
        }

        @Override
        public <V extends DataValue> MissingValue getAdapterError(final Class<V> valueClass) {
            return ((AdapterValue)getDataCell()).getAdapterError(valueClass);
        }

        @Override
        public Map<Class<? extends DataValue>, DataCell> getAdapterMap() {
            return ((AdapterValue)getDataCell()).getAdapterMap();
        }
    }

    private static final class AdapterCellDataOutputDelegator extends AbstractDataOutputDelegator {

        public AdapterCellDataOutputDelegator(final IWriteFileStoreHandler fileStoreHandler, final DataOutput output) {
            super(fileStoreHandler, output);
        }

        @Override
        protected void writeDataCellImpl(final DataCell cell) throws IOException {
            // write all contained class names first, followed by the class contents
            final var classNames = DictEncodedDataCellDataInputDelegator.getSerializedCellNames(cell);
            writeUTF(classNames);

            final var optionalSerializer = DataTypeRegistry.getInstance().getSerializer(cell.getClass());

            if (optionalSerializer.isEmpty()) {
                // fall back to Java serialization
                final var oos = new ObjectOutputStream(this);
                oos.writeObject(cell);
                oos.flush();
            } else {
                final var serializer = optionalSerializer.get();
                serializer.serialize(cell, this);
            }
        }
    }

    private static final class AdapterCellDataInputDelegator extends AbstractDataInputDelegator {
        private static final DataTypeRegistry REGISTRY = DataTypeRegistry.getInstance();

        public AdapterCellDataInputDelegator(final IDataRepository dataRepository, final ReadableDataInput input) {
            super(dataRepository, input);
        }

        @Override
        protected DataCell readDataCellImpl() throws IOException {
            // all contained class names are written first, followed by the class contents
            final String classNameString = readUTF();
            final List<String> classNames = Arrays.stream(classNameString.split(";")).collect(Collectors.toList());

            Optional<DataCellSerializer<DataCell>> serializer = Optional.empty();

            if (classNames.isEmpty()) {
                throw new IllegalStateException("No more DataCell type information available for this stream");
            }

            final var className = classNames.remove(0);
            if (className.isEmpty()) {
                throw new IllegalStateException("Cannot read DataCell with empty type information");
            }

            final var cellClass = REGISTRY.getCellClass(className);
            if (!cellClass.isEmpty()) {
                serializer = REGISTRY.getSerializer(cellClass.get());
            }

            if (serializer.isEmpty()) {
                // fall back to java serialization
                try {
                    final ObjectInputStream ois = new ObjectInputStream(this);
                    return (DataCell)ois.readObject();
                } catch (ClassNotFoundException e) {
                    throw new IOException(e);
                }
            }

            return serializer.get().deserialize(this);
        }

    }

    private static final class ReadableLongUTFDataInputStream extends LongUTFDataInputStream
        implements ReadableDataInput {
        private final ReadableDataInput m_delegate;

        ReadableLongUTFDataInputStream(final DataInputStream stream) {
            super(stream);
            m_delegate = new ReadableDataInputStream(stream);
        }

        @Override
        public byte[] readBytes() throws IOException {
            return m_delegate.readBytes();
        }
    }
}