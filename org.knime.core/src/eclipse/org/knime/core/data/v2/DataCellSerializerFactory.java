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
package org.knime.core.data.v2;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * Helper class to create and persist a mapping between {@link DataCell}s and their {@link DataCellSerializer} at
 * serialization time.
 *
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 *
 * @noreference This class is not intended to be referenced by clients.
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

    private final DataCellSerializerInfo[] m_byIdx;

    /*
     * Internal index
     */
    private byte m_internalIndex = 0;

    /**
     * Constructor
     */
    public DataCellSerializerFactory() {
        m_byType = new HashMap<>();
        m_byIdx = new DataCellSerializerInfo[Byte.MAX_VALUE];
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
                    m_byIdx[m_internalIndex] = res;
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
            return m_byIdx[index];
        }
    }

    /**
     * Save entire state of object.
     *
     * @param settings used to store the object
     */
    public final void saveTo(final ConfigWO settings) {
        final Config childConfig = settings.addConfig(CFG_SERIALIZER_MAPPINGS);
        final List<String> cells = new ArrayList<>();
        final List<Byte> indices = new ArrayList<>();
        for (int i = 0; i < m_byIdx.length; i++) {
            if (m_byIdx[i] != null) {
                cells.add(m_byIdx[i].m_cellType.getName());
                indices.add(m_byIdx[i].m_internalIndex);
            }
        }
        final byte[] indicesArray = new byte[indices.size()];
        final String[] cellsArray = new String[cells.size()];
        for (int i = 0; i < indicesArray.length; i++) {
            indicesArray[i] = indices.get(i);
            cellsArray[i] = cells.get(i);
        }
        childConfig.addByteArray(CFG_SERIALIZER_INDICES, indicesArray);
        childConfig.addStringArray(CFG_SERIALIZER_CELL_TYPES, cellsArray);
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
            m_byIdx[indices[i]] = info;
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