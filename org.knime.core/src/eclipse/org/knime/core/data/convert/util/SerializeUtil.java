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
 *   25.05.2016 (Jonathan Hale): created
 */
package org.knime.core.data.convert.util;

import java.util.Optional;

import org.knime.core.data.DataType;
import org.knime.core.data.convert.AbstractConverterFactoryRegistry;
import org.knime.core.data.convert.ConverterFactory;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterFactory;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterRegistry;
import org.knime.core.data.convert.java.DataCellToJavaConverterFactory;
import org.knime.core.data.convert.java.DataCellToJavaConverterRegistry;
import org.knime.core.data.convert.map.CellValueConsumerFactory;
import org.knime.core.data.convert.map.CellValueProducerFactory;
import org.knime.core.data.convert.map.ConsumerRegistry;
import org.knime.core.data.convert.map.ConsumptionPath;
import org.knime.core.data.convert.map.Destination;
import org.knime.core.data.convert.map.ProducerRegistry;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.data.convert.map.Source;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.base.ConfigBase;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.config.base.ConfigBaseWO;

/**
 *
 * Utility class for loading and storing converter factories from Configurations.
 *
 * <p>
 * <b> Examples: </b>
 * </p>
 * <p>
 * Storing a factory
 *
 * <pre>
 * NodeSettings settings = ...;
 * JavaToDataCellFactory&lt;?> myFactory = ...;
 *
 * SerializeUtil.storeConverterFactory(myFactory, settings, "theFactory");
 * </pre>
 * </p>
 *
 * <p>
 * Loading a factory
 *
 * <pre>
 * NodeSettings settings = ...;
 *
 * Optional&lt;JavaToDataCellFactory&lt;?>> myFactory = SerializeUtil.loadConverterFactory(settings, "theFactory");
 *
 * if (!myFactory.isPresent()) {
 *      // possibly a plugin missing.
 *      FactoryPlaceholder placeholder = SerializeUtil.getPlaceholder(settings, "theFactory");
 *
 *      // we can use the placeholder for display in the UI or in a warning message.
 * }
 * </pre>
 * </p>
 *
 * @author Jonathan Hale, KNIME, Konstanz, Germany
 * @since 3.2
 */
public final class SerializeUtil {

    private SerializeUtil() {
        // class not meant to be instantiated
    }

    /**
     * Object which can be loaded instead of an actual ConverterFactory, for example to be able to still display a
     * previously existing setting which has become invalid through removal of a required plug-in, in a node dialog for
     * example.
     *
     * @author Jonathan Hale, KNIME, Konstanz, Germany
     */
    public static final class FactoryPlaceholder {

        private final String m_sourceType;

        private final String m_destType;

        private final String m_name;

        private final String m_id;

        /**
         * @param src name of the source type
         * @param dst name of the destination type
         * @param name name of the converter factory
         * @param id id of the converter factory
         */
        private FactoryPlaceholder(final String src, final String dst, final String name, final String id) {
            m_sourceType = src;
            m_destType = dst;
            m_name = name;
            m_id = id;
        }

        /**
         * Get the name of the source type of the factory
         *
         * @return the name of the source type of the factory
         */
        public String getSourceTypeName() {
            return m_sourceType;
        }

        /**
         * Get the name of the destination type of the factory
         *
         * @return the name of the destination type of the factory
         */
        public String getDestinationTypeName() {
            return m_destType;
        }

        /**
         * Get the name of the factory
         *
         * @return the name of the factory
         */
        public String getName() {
            return m_name;
        }

        /**
         * Get the identifier for the factory
         *
         * @return the identifier of the factory
         */
        public String getIdentifier() {
            return m_id;
        }
    }

    /**
     * Store a converter factory in the given config.
     *
     * @param factory factory to store
     * @param config config to store to
     * @param key setting key
     * @since 3.6
     */
    public static void storeConverterFactory(final ConverterFactory<?, ?> factory, final ConfigBaseWO config,
        final String key) {
        config.addString(key, factory.getIdentifier());

        // store information to be able to display something in UI if factories go missing.
        config.addString(key + "_src", typeToString(factory.getSourceType()));
        config.addString(key + "_dst", typeToString(factory.getDestinationType()));
        config.addString(key + "_name", factory.getName());
        final ConfigBase factoryConfig = config.addConfigBase(key + "_config");
        factory.storeAdditionalConfig(factoryConfig);
    }

    /* Helper to get name of class/data type in a backward compatible manor */
    private static String typeToString(final Object type) {
        if (type instanceof DataType) {
            return ((DataType)type).getName();
        } else if (type instanceof Class) {
            return ((Class<?>)type).getName();
        }

        return type.toString();
    }

    /**
     * Load a {@link DataCellToJavaConverterFactory} from given config.
     *
     * @param config config to load from
     * @param key setting key
     * @return an optional {@link DataCellToJavaConverterFactory}, present if the identifier was found in the
     *         {@link DataCellToJavaConverterRegistry}.
     * @throws InvalidSettingsException
     */
    public static Optional<DataCellToJavaConverterFactory<?, ?>> loadDataCellToJavaConverterFactory(
        final ConfigBaseRO config, final String key) throws InvalidSettingsException {
        return loadConverterFactory(config, DataCellToJavaConverterRegistry.getInstance(), key);
    }

    /**
     * Load a {@link JavaToDataCellConverterFactory} from given config.
     *
     * @param config config to load from
     * @param key setting key
     * @return an optional {@link JavaToDataCellConverterFactory}, present if the identifier was found in the
     *         {@link JavaToDataCellConverterRegistry}.
     * @throws InvalidSettingsException
     */
    public static Optional<JavaToDataCellConverterFactory<?>> loadJavaToDataCellConverterFactory(
        final ConfigBaseRO config, final String key) throws InvalidSettingsException {
        return loadConverterFactory(config, JavaToDataCellConverterRegistry.getInstance(), key);
    }

    /**
     * Get a placeholder for a converter factory (for example if the id was not found due to a previously registered
     * plugin missing) which contains source type, destination type, name and identifier of the factory which was stored
     * with the given key.
     *
     * @param config config to load from
     * @param key setting key
     * @return a placeholder object for the factory
     * @throws InvalidSettingsException
     */
    public static FactoryPlaceholder getPlaceholder(final ConfigBaseRO config, final String key)
        throws InvalidSettingsException {
        return new FactoryPlaceholder(config.getString(key + "_src"), config.getString(key + "_dst"),
            config.getString(key + "_name"), config.getString(key));
    }

    /**
     * Store a {@link ConsumptionPath} to node settings.
     *
     * @param path Path to store
     * @param config Config to store to
     * @param key Key to store at in the config
     * @since 3.6
     */
    public static void storeConsumptionPath(final ConsumptionPath path, final ConfigBaseWO config, final String key) {
        storeConverterFactory(path.m_converterFactory, config, key + "_converter");
        storeConverterFactory(path.m_consumerFactory, config, key + "_consumer");
    }

    /**
     * Load a {@link ConsumptionPath} from given config.
     *
     * @param config Config to load from
     * @param registry Registry to load consumer with
     * @param key setting key
     * @return an optional {@link ConsumptionPath}, present if the converter factory identifier was found in the
     *         {@link DataCellToJavaConverterFactory} and consumer factory identifier was found in the registry.
     * @throws InvalidSettingsException
     * @since 3.6
     */
    public static <ExternalType, DestType extends Destination<ExternalType>> Optional<ConsumptionPath>
        loadConsumptionPath(final ConfigBaseRO config, final ConsumerRegistry<ExternalType, DestType> registry,
            final String key) throws InvalidSettingsException {
        final Optional<DataCellToJavaConverterFactory<?, ?>> converter =
            loadDataCellToJavaConverterFactory(config, key + "_converter");

        if (!converter.isPresent()) {
            return Optional.empty();
        }

        final Optional<CellValueConsumerFactory<DestType, ?, ExternalType, ?>> consumer =
            loadConverterFactory(config, registry, key + "_consumer");

        if (!consumer.isPresent()) {
            return Optional.empty();
        }

        return Optional.of(new ConsumptionPath(converter.get(), consumer.get()));
    }

    /**
     * Store a {@link ProductionPath} to node settings.
     *
     * @param path Path to store
     * @param config Config to store to
     * @param key Key to store at in the config
     * @since 3.6
     */
    public static void storeProductionPath(final ProductionPath path, final ConfigBaseWO config, final String key) {
        storeConverterFactory(path.m_converterFactory, config, key + "_converter");
        storeConverterFactory(path.m_producerFactory, config, key + "_producer");
    }

    /**
     * Load a {@link ProductionPath} from given config.
     *
     * @param config Config to load from
     * @param registry Registry to load producer with
     * @param key setting key
     * @return an optional {@link ProductionPath}, present if the converter factory identifier was found in the
     *         {@link JavaToDataCellConverterRegistry} and producer factory identifier was found in the registry.
     * @throws InvalidSettingsException
     * @since 3.6
     */
    public static <ExternalType, SourceType extends Source<ExternalType>> Optional<ProductionPath> loadProductionPath(
        final ConfigBaseRO config, final ProducerRegistry<ExternalType, SourceType> registry, final String key)
        throws InvalidSettingsException {
        final Optional<JavaToDataCellConverterFactory<?>> converter =
            loadJavaToDataCellConverterFactory(config, key + "_converter");

        if (!converter.isPresent()) {
            return Optional.empty();
        }

        final Optional<CellValueProducerFactory<SourceType, ExternalType, ?, ?>> producer =
            loadConverterFactory(config, registry, key + "_producer");
        if (!producer.isPresent()) {
            return Optional.empty();
        }

        return Optional.of(new ProductionPath(producer.get(), converter.get()));
    }

    /**
     * Load a {@link ConverterFactory converter factory} from given config.
     *
     * Use {@link #getPlaceholder(ConfigBaseRO, String)} to get information about the converter factory if it's missing.
     * To store a converter factory use {@link #storeConverterFactory(ConverterFactory, ConfigBaseWO, String)}.
     *
     * @param config Config to load from
     * @param registry Registry to load converter factory with
     * @param key setting key
     * @return an optional {@link ConverterFactory}, present if the factory identifier was found in the given
     *         <code>registry</code>.
     * @throws InvalidSettingsException
     * @since 3.7
     */
    public static <S, D, F extends ConverterFactory<S, D>, R extends AbstractConverterFactoryRegistry<S, D, F, R>>
        Optional<F> loadConverterFactory(final ConfigBaseRO config, final R registry, final String key)
            throws InvalidSettingsException {
        final String id = config.getString(key);
        final String configKey = key + "_config";

        final Optional<F> factory = registry.getFactory(id);
        if (factory.isPresent() && config.containsKey(configKey)) {
            factory.get().loadAdditionalConfig(config.getConfigBase(configKey));
        }
        return factory;
    }

}
