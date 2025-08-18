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
 *   Sep 27, 2020 (dietzc): created
 */
package org.knime.core.data.v2.schema;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataTypeRegistry;
import org.knime.core.data.IDataRepository;
import org.knime.core.data.RowKey;
import org.knime.core.data.TableBackend;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;
import org.knime.core.data.v2.CollectionValueFactory;
import org.knime.core.data.v2.DataCellSerializerFactory;
import org.knime.core.data.v2.RowKeyType;
import org.knime.core.data.v2.RowKeyValueFactory;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.ValueFactoryUtils;
import org.knime.core.data.v2.value.cell.DataCellValueFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;

/**
 * A ValueSchema wraps a {@link DataTableSpec} by mapping each {@link DataColumnSpec} via it's {@link DataType} to a
 * {@link ValueFactory}. {@link TableBackend} implementations leverage the {@link ValueFactory}s in turn as a canonical,
 * logical access layer, independent from it's physical implementation.
 *
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 *
 * @noreference This class is not intended to be referenced by clients.
 */
@SuppressWarnings("deprecation")
final class SerializerFactoryValueSchema extends DefaultDataTableValueSchema {

    private final Map<DataType, String> m_factoryMapping;

    private final DataCellSerializerFactory m_factory;

    SerializerFactoryValueSchema(final DataTableSpec spec, //
        final ValueFactory<?, ?>[] colFactories, //
        final Map<DataType, String> factoryMapping, //
        final DataCellSerializerFactory factory) {
        super(spec, colFactories);
        m_factoryMapping = factoryMapping;
        m_factory = factory;
    }

    /**
     * Creates a new LegacyValueSchema based up-on the provided {@link DataTableSpec}.
     *
     * @param spec the data table spec to derive the LegacyValueSchema from.
     * @param rowKeyType type of the {@link RowKey}
     * @param fileStoreHandler file-store handler
     * @return the value schema.
     */
    public static final SerializerFactoryValueSchema create(final DataTableSpec spec, final RowKeyType rowKeyType,
        final IWriteFileStoreHandler fileStoreHandler) {

        final var cellSerializerFactory = new DataCellSerializerFactory();
        final Map<DataType, String> factoryMapping = new HashMap<>();
        final var factories = new ValueFactory[spec.getNumColumns() + 1];
        factories[0] = ValueFactoryUtils.getRowKeyValueFactory(rowKeyType);

        for (int i = 1; i < factories.length; i++) {
            final DataType type = spec.getColumnSpec(i - 1).getType();
            factories[i] = findValueFactory(type, factoryMapping, cellSerializerFactory, fileStoreHandler);
        }
        return new SerializerFactoryValueSchema(spec, factories, factoryMapping, cellSerializerFactory);
    }

    /**
     * Creates a new LegacyValueSchema given the provided {@link DataTableSpec spec} and {@link ValueFactory
     * factories}.
     *
     * @param spec the data table spec that the LegacyValueSchema should wrap
     * @param valueFactories one for the row key and one for each column in spec
     * @param cellSerializerFactory used for any {@link DataCellValueFactory DataCellValueFactories} among the
     *            valueFactories
     * @return the value schema
     * @since 4.5
     */
    public static final SerializerFactoryValueSchema create(final DataTableSpec spec, final ValueFactory<?, ?>[] valueFactories,
        final DataCellSerializerFactory cellSerializerFactory) {
        CheckUtils.checkArgument(valueFactories.length == spec.getNumColumns() + 1,
            "The number of value factories must be equal to the number of columns plus 1 (for the row key).");
        CheckUtils.checkArgument(valueFactories[0] instanceof RowKeyValueFactory,
            "The first value factory must be a RowKeyValueFactory.");
        final Map<DataType, String> factoryMapping = new HashMap<>();
        for (int i = 1; i < valueFactories.length; i++) {
            var fac = valueFactories[i].getClass().getName();
            var type = spec.getColumnSpec(i - 1).getType();
            var oldFac = factoryMapping.put(type, fac);
            CheckUtils.checkArgument(oldFac == null || fac.equals(oldFac),
                "Conflicting ValueFactories '%s' and '%s' for data type '%s'.", oldFac, fac, type.toPrettyString());
        }
        return new SerializerFactoryValueSchema(spec, valueFactories.clone(), factoryMapping, cellSerializerFactory);
    }

    /** Find the factory for the given type (or DataCellValueFactory) and add it to the mapping */
    private static final ValueFactory<?, ?> findValueFactory(final DataType type,
        final Map<DataType, String> factoryMapping, final DataCellSerializerFactory cellSerializerFactory,
        final IWriteFileStoreHandler fileStoreHandler) {
        var factory = ValueFactoryUtils.getValueFactory(type,
            t -> new DataCellValueFactory(cellSerializerFactory, fileStoreHandler, t), fileStoreHandler);
        factoryMapping.put(type, factory.getClass().getName());
        return factory;
    }

    /**
     * Serializer to save/load LegacyValueSchema.
     *
     * @author Christian Dietz, KNIME GmbH, Konstanz, Germany.
     * @since 4.3
     */
    public static final class Serializer {

        private static final String CFG_ROW_KEY_CONFIG = "row_key_config";

        private static final String CFG_KEY_FACTORY_MAPPING_KEYS = "factory_mapping_keys";

        private static final String CFG_KEY_FACTORY_MAPPING_VALUES = "factory_mapping_values";

        private Serializer() {
        }

        /**
         * Saves a ValueSchema to the provided settings.
         *
         * @param schema the ValueSchema to save.
         * @param settings the settings to save the ValueSchema to.
         */
        public static final void save(final SerializerFactoryValueSchema schema, final NodeSettingsWO settings) {

            // save row key config
            settings.addString(CFG_ROW_KEY_CONFIG, schema.getValueFactory(0).getClass().getName());

            // We need to remember which datatypes have been mapped to which ValueFactory
            final Map<DataType, String> factoryMapping = schema.m_factoryMapping;
            final DataType[] factoryMappingKeys = factoryMapping.keySet().toArray(new DataType[0]);
            final String[] factoryMappingValues =
                Arrays.stream(factoryMappingKeys).map(factoryMapping::get).toArray(String[]::new);
            settings.addDataTypeArray(CFG_KEY_FACTORY_MAPPING_KEYS, factoryMappingKeys);
            settings.addStringArray(CFG_KEY_FACTORY_MAPPING_VALUES, factoryMappingValues);

            // now store all info required to restore DataCellValueFactories
            schema.m_factory.saveTo(settings);
        }

        /**
         * Loads a ValueSchema from the given settings.
         *
         * @param source the source {@link DataTableSpec}.
         * @param dataRepository the data repository to restore file store cells.
         * @param settings to save the value schema to.
         * @return the loaded LegacyValueSchema.
         *
         * @throws InvalidSettingsException
         */
        public static final SerializerFactoryValueSchema load(final DataTableSpec source, final IDataRepository dataRepository,
            final NodeSettingsRO settings) throws InvalidSettingsException {

            // Load the factory mapping
            final DataType[] factoryMappingKeys = settings.getDataTypeArray(CFG_KEY_FACTORY_MAPPING_KEYS);
            final String[] factoryMappingValues = settings.getStringArray(CFG_KEY_FACTORY_MAPPING_VALUES);
            final Map<DataType, String> factoryMapping = new HashMap<>();
            for (int i = 0; i < factoryMappingKeys.length; i++) {
                factoryMapping.put(factoryMappingKeys[i], factoryMappingValues[i]);
            }

            // Load the cell serializer factory
            final DataCellSerializerFactory cellSerializerFactory = new DataCellSerializerFactory();
            cellSerializerFactory.loadFrom(settings);

            // Get the factories for the specs
            final ValueFactory<?, ?>[] factories = new ValueFactory[source.getNumColumns() + 1];
            factories[0] = instantiateValueFactory(settings.getString(CFG_ROW_KEY_CONFIG));
            for (int i = 1; i < factories.length; i++) {
                final DataType type = source.getColumnSpec(i - 1).getType();
                factories[i] = getValueFactory(type, factoryMapping, cellSerializerFactory, dataRepository);
            }

            return new SerializerFactoryValueSchema(source, factories, factoryMapping, cellSerializerFactory);
        }

        private static ValueFactory<?, ?> getValueFactory(final DataType type,
            final Map<DataType, String> factoryMapping, final DataCellSerializerFactory cellSerializerFactory,
            final IDataRepository dataRepository) {
            final String valueFactoryClassName = factoryMapping.get(type);
            final ValueFactory<?, ?> factory = instantiateValueFactory(valueFactoryClassName);

            // Initialize
            if (factory instanceof CollectionValueFactory) {
                final DataType elementType = type.getCollectionElementType();
                ((CollectionValueFactory<?, ?>)factory).initialize(
                    getValueFactory(elementType, factoryMapping, cellSerializerFactory, dataRepository), elementType);
            } else if (factory instanceof DataCellValueFactory) {
                ((DataCellValueFactory)factory).initialize(cellSerializerFactory, dataRepository, type);
            }
            return factory;
        }

        private static ValueFactory<?, ?> instantiateValueFactory(final String className) {
            var specificCollectionValueFactory =
                ValueFactoryUtils.getSpecificCollectionValueFactory(className);
            if (specificCollectionValueFactory.isPresent()) {
                return specificCollectionValueFactory.get();
            }
            final Optional<Class<? extends ValueFactory<?, ?>>> valueFactoryClass =
                DataTypeRegistry.getInstance().getValueFactoryClass(className);

            final Class<? extends ValueFactory<?, ?>> type;
            if (!valueFactoryClass.isPresent()) {
                // try falling back on DataCellValueFactory
                try {
                    @SuppressWarnings("unchecked")
                    final Class<? extends ValueFactory<?, ?>> cast =
                        (Class<? extends ValueFactory<?, ?>>)Class.forName(className);
                    type = cast;
                } catch (final ClassNotFoundException ex) {
                    throw new IllegalStateException(
                        "The ValueFactory '" + className + "' could not be found. Are you missing a KNIME Extension?",
                        ex);
                }
            } else {
                type = valueFactoryClass.get();
            }
            return ValueFactoryUtils.instantiateValueFactory(type);
        }

    }
}
