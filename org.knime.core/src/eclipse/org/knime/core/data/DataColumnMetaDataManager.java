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
 *   Oct 11, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.knime.core.data.meta.DataColumnMetaData;
import org.knime.core.data.meta.DataColumnMetaDataCreator;
import org.knime.core.data.meta.DataColumnMetaDataRegistry;
import org.knime.core.data.meta.DataColumnMetaDataSerializer;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.core.node.util.CheckUtils;

/**
 * Manages {@link DataColumnMetaData} for a {@link DataColumnSpec}. Currently, only {@link DataColumnMetaData} is
 * supported.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class DataColumnMetaDataManager {

    private final Map<Class<? extends DataColumnMetaData>, DataColumnMetaData> m_valueMetaDataMap;

    static final DataColumnMetaDataManager EMPTY = new DataColumnMetaDataManager(Collections.emptyMap());

    private DataColumnMetaDataManager(
        final Map<Class<? extends DataColumnMetaData>, DataColumnMetaData> valueMetaDataMap) {
        m_valueMetaDataMap = valueMetaDataMap;
    }

    <M extends DataColumnMetaData> Optional<M> getMetaDataOfType(final Class<M> metaDataClass) {
        final DataColumnMetaData wildCardMetaData = m_valueMetaDataMap.get(metaDataClass);
        if (wildCardMetaData == null || !metaDataClass.isInstance(wildCardMetaData)) {
            return Optional.empty();
        }
        @SuppressWarnings("unchecked") // the check above ensures that wildCardMetaData
        // is indeed of type MetaData<T>
        final M typedMetaData = (M)wildCardMetaData;
        return Optional.of(typedMetaData);
    }

    void save(final ConfigWO config) {
        m_valueMetaDataMap.forEach((k, v) -> save(k, v, config));
    }

    @SuppressWarnings("unchecked") // we check compatibility programmatically
    private static <M extends DataColumnMetaData> void save(final Class<M> metaDataClass,
        final DataColumnMetaData metaData, final ConfigWO config) {
        @SuppressWarnings("rawtypes") // unfortunately necessary to satisfy the compiler
        final DataColumnMetaDataSerializer serializer = DataColumnMetaDataRegistry.INSTANCE.getSerializer(metaDataClass)
            .orElseThrow(() -> new IllegalStateException(
                String.format("There is no serializer registered for meta data '%s'.", metaDataClass.getName())));
        serializer.save(metaData, config.addConfig(metaDataClass.getName()));
    }

    static DataColumnMetaDataManager load(final ConfigRO config) throws InvalidSettingsException {
        final Map<Class<? extends DataColumnMetaData>, DataColumnMetaData> metaDataMap = new HashMap<>();

        for (String key : config) {
            final Optional<DataColumnMetaDataSerializer<?>> serializer =
                DataColumnMetaDataRegistry.INSTANCE.getSerializer(key);
            if (serializer.isPresent()) {
                final DataColumnMetaData metaData = serializer.get().load(config.getConfig(key));
                metaDataMap.put(metaData.getClass(), metaData);
            } else {
                NodeLogger.getLogger(DataColumnMetaDataManager.class).errorWithFormat(
                    "There is no serializer registered for meta data '%s'. Are you missing an extension?", key);
            }
        }
        return new DataColumnMetaDataManager(metaDataMap);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj instanceof DataColumnMetaDataManager) {
            return m_valueMetaDataMap.equals(((DataColumnMetaDataManager)obj).m_valueMetaDataMap);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return m_valueMetaDataMap.hashCode();
    }

    /**
     * Allows to create {@link DataColumnMetaDataManager} instances.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static final class Creator {
        private final Map<Class<? extends DataColumnMetaData>, DataColumnMetaDataCreator<?>> m_valueMetaDataMap;

        Creator() {
            m_valueMetaDataMap = new HashMap<>();
        }

        Creator(final DataColumnMetaDataManager metaData) {
            m_valueMetaDataMap = metaData.m_valueMetaDataMap.values().stream().collect(Collectors
                .toMap(DataColumnMetaData::getClass, DataColumnMetaDataRegistry.INSTANCE::getInitializedCreator));
        }

        private <T extends DataColumnMetaData> DataColumnMetaDataCreator<T> getCreator(final Class<T> metaDataClass) {
            final DataColumnMetaDataCreator<?> creator =
                m_valueMetaDataMap.computeIfAbsent(metaDataClass, DataColumnMetaDataRegistry.INSTANCE::getCreator);
            CheckUtils.checkState(creator.getMetaDataClass().equals(metaDataClass), "Illegal Mapping");
            @SuppressWarnings("unchecked") // explicitly checked above
            final DataColumnMetaDataCreator<T> typedCreator = (DataColumnMetaDataCreator<T>)creator;
            return typedCreator;
        }

        <T extends DataColumnMetaData> DataColumnMetaDataManager.Creator addMetaData(final T metaData,
            final boolean overwrite) {
            final Class<T> metaDataClass = DataColumnMetaDataRegistry.INSTANCE.getClass(metaData);
            if (overwrite) {
                m_valueMetaDataMap.put(metaDataClass,
                    DataColumnMetaDataRegistry.INSTANCE.getInitializedCreator(metaData));
            } else {
                final DataColumnMetaDataCreator<T> creator = getCreator(metaDataClass);
                creator.merge(metaData);
            }
            return this;
        }

        DataColumnMetaDataManager.Creator remove(final Class<? extends DataColumnMetaData> metaDataClass) {
            m_valueMetaDataMap.remove(metaDataClass);
            return this;
        }

        DataColumnMetaDataManager.Creator clear() {
            m_valueMetaDataMap.clear();
            return this;
        }

        DataColumnMetaDataManager.Creator merge(final DataColumnMetaDataManager metaData) {
            metaData.m_valueMetaDataMap.values().forEach(this::merge);
            return this;
        }

        private <M extends DataColumnMetaData> void merge(final M metaData) {
            mergeHelper(metaData);
        }

        private <M extends DataColumnMetaData> void mergeHelper(final M metaData) {
            m_valueMetaDataMap.merge(metaData.getClass(),
                DataColumnMetaDataRegistry.INSTANCE.getInitializedCreator(metaData), DataColumnMetaDataCreator::merge);
        }

        DataColumnMetaDataManager create() {
            return new DataColumnMetaDataManager(m_valueMetaDataMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().create())));
        }
    }
}
