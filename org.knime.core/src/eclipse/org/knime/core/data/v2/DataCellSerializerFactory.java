package org.knime.core.data.v2;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataTypeRegistry;
import org.knime.core.data.container.KNIMEStreamConstants;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;

/**
 *
 * Helper class to create and persist a mapping between {@link DataCell}s and their {@link DataCellSerializer} at
 * serialization time.
 *
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 */
public final class DataCellSerializerFactory {

    /* Access to DataTypeRegistry */
    private final static DataTypeRegistry REGISTRY = DataTypeRegistry.getInstance();

    /*
     * Configuration constants for save / load.
     */
    private static final String CFG_SERIALIZER_MAPPINGS = "serializer_mappings";

    private static final String CFG_SERIALIZER_CELL_TYPES = "serializer_types";

    private static final String CFG_SERIALIZER_INDICES = "serializer_indices";

    /*
     * Data structures to maintain mapping.
     */
    private final Map<Class<? extends DataCell>, DataCellSerializerInfo> m_byType;

    private final Map<Byte, DataCellSerializerInfo> m_byIdx;

    /*
     * Internal index
     */
    private byte m_internalIndex = Byte.MIN_VALUE + 10;

    /**
     * Constructor
     */
    public DataCellSerializerFactory() {
        m_byType = new HashMap<>();
        m_byIdx = new HashMap<>();
    }

    /**
     * Get {@link DataCellSerializerInfo} for cell. In case no dedicated {@link DataCellSerializer} can be found, a Java
     * Native Serializer is returned..
     *
     * @param cell the cell which should be serialized
     * @return the {@link DataCellSerializerInfo}
     */
    public final DataCellSerializerInfo getSerializer(final DataCell cell) {
        final Class<? extends DataCell> type = cell.getClass();
        DataCellSerializerInfo res = m_byType.get(type);
        if (res == null) {
            if (m_internalIndex == Byte.MAX_VALUE) {
                throw new IllegalStateException("Too many cell implementations!");
            } else {
                Optional<DataCellSerializer<DataCell>> pair = REGISTRY.getSerializer(type);
                if (pair.isPresent()) {
                    res = new DataCellSerializerInfo(type, m_internalIndex, REGISTRY.getSerializer(type).get());
                    m_byType.put(type, res);
                    m_byIdx.put(m_internalIndex, res);
                    m_internalIndex++;
                } else {
                    res = JavaNativeSerializer.INSTANCE;
                }
            }
        }
        return res;
    }

    /**
     * Get {@link DataCellSerializerInfo} per index.
     *
     * @param index the index
     * @return the {@link DataCellSerializerInfo} for the provided index.
     */
    public final DataCellSerializerInfo getSerializerByIdx(final byte index) {
        if (index == KNIMEStreamConstants.BYTE_TYPE_SERIALIZATION) {
            return JavaNativeSerializer.INSTANCE;
        } else {
            return m_byIdx.get(index);
        }
    }

    /**
     * Save entire state of object.
     *
     * @param settings used to store the object
     */
    public final void saveTo(final ConfigWO settings) {
        final Config childConfig = settings.addConfig(CFG_SERIALIZER_MAPPINGS);
        final String[] cells = new String[m_byIdx.size()];
        final byte[] indices = new byte[cells.length];
        int index = 0;

        for (final Entry<Byte, DataCellSerializerInfo> entry : m_byIdx.entrySet()) {
            cells[index] = entry.getValue().m_cellType.getName();
            indices[index] = entry.getValue().m_internalIndex;
            index++;
        }
        childConfig.addByteArray(CFG_SERIALIZER_INDICES, indices);
        childConfig.addStringArray(CFG_SERIALIZER_CELL_TYPES, cells);
    }

    /**
     * Load state of object
     *
     * @param settings used to store the object
     * @throws InvalidSettingsException
     */
    public void loadFrom(final ConfigRO settings) throws InvalidSettingsException {
        final Config childConfig = settings.getConfig(CFG_SERIALIZER_MAPPINGS);

        final String[] cells = childConfig.getStringArray(CFG_SERIALIZER_CELL_TYPES);
        final byte[] indices = childConfig.getByteArray(CFG_SERIALIZER_INDICES);
        for (int i = 0; i < indices.length; i++) {
            final Class<? extends DataCell> type = REGISTRY.getCellClass(cells[i]).get(); // NOSONAR
            final DataCellSerializerInfo info =
                new DataCellSerializerInfo(type, indices[i], REGISTRY.getSerializer(type).get()); // NOSONAR
            m_byIdx.put(indices[i], info);
            m_byType.put(type, info);
        }
    }

    /**
     * Simple helper class to describe all related information around a certain {@link DataCellSerializer}.
     *
     * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
     * @since 4.3
     */
    public static final class DataCellSerializerInfo {

        private final Class<? extends DataCell> m_cellType;

        private final DataCellSerializer<DataCell> m_serializer;

        private final byte m_internalIndex;

        private DataCellSerializerInfo(final Class<? extends DataCell> cellType, final byte internalIndex,
            final DataCellSerializer<DataCell> serializer) {
            m_cellType = cellType;
            m_serializer = serializer;
            m_internalIndex = internalIndex;
        }

        /**
         * @return type of {@link DataCell}
         */
        public final Class<? extends DataCell> getCellType() {
            return m_cellType;
        }

        /**
         * @return the internal index of the {@link DataCellSerializer}. Usually created on
         *         {@link DataCellSerializerFactory#getSerializer(DataCell)}
         */
        public final byte getInternalIndex() {
            return m_internalIndex;
        }

        /**
         * @return the actual {@link DataCellSerializer}.
         */
        public final DataCellSerializer<DataCell> getSerializer() {
            return m_serializer;
        }
    }

    private static final class JavaNativeSerializer implements DataCellSerializer<DataCell> {

        private static final DataCellSerializerInfo INSTANCE = new DataCellSerializerInfo(DataCell.class,
            KNIMEStreamConstants.BYTE_TYPE_SERIALIZATION, new JavaNativeSerializer());

        private JavaNativeSerializer() {
        }

        @Override
        public void serialize(final DataCell cell, final DataCellDataOutput output) throws IOException {
            final ObjectOutputStream oos = new ObjectOutputStream((OutputStream)output);
            oos.writeObject(cell);
            oos.flush();
        }

        @Override
        public DataCell deserialize(final DataCellDataInput input) throws IOException {
            try {
                final ObjectInputStream ois = new ObjectInputStream((InputStream)input);
                return (DataCell)ois.readObject();
            } catch (ClassNotFoundException e) {
                throw new IOException(e);
            }
        }

    }

}