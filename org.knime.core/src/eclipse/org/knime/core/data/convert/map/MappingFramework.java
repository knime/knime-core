package org.knime.core.data.convert.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.knime.core.data.DataTable;
import org.knime.core.data.DataType;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterFactory;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterRegistry;
import org.knime.core.data.convert.java.DataCellToJavaConverterFactory;
import org.knime.core.data.convert.java.DataCellToJavaConverterRegistry;
import org.knime.core.data.convert.map.Sink.ConsumerParameters;
import org.knime.core.data.convert.map.Source.ProducerParameters;
import org.knime.core.node.NodeLogger;

/**
 * Framework which handles writing a {@link DataTable} to a arbitrary destination, e.g. an SQL connection, or reading
 * from an arbitrary source to a DataTable.
 *
 * While there is at most one consumer and producer per Java type, each consumer or producer is configurable to write
 * its value in different ways.
 *
 * <h1>Usage</h1>
 *
 * Writing values to a {@link Sink}:
 *
 * <code><pre>
 * class MySink extends AbstractSink<MySink, MySinkParameters> { ... }
 * class MySinkParameters implements Sink.SinkParameters<MySink> { ... }
 *
 * interface MyConsumer<T> extends CellValueConsumer<MySink, T, MySinkParameters> {}
 *
 * {@literal /}* Register some consumers once *{@literal /}
 * final MyConsumer<Integer> intConsumer = (sink, value, parameters) -> {
 *      // write value to sink, configured with parameters
 * }
 *
 * MappingFramework.forSinkType(MySink.class) //
 *                 .registerConsumer(Integer.class, intConsumer);
 *
 * {@literal /}* Convert and write some KNIME data *{@literal /}
 * MySink sink = new MySink(table.getDataTableSpec());
 * sink.addTable(table);
 *
 * </pre>
 * </code>
 *
 * Reading values from a {@link Source}:
 *
 * <code><pre>
 * class MySource extends AbstractSource<MySource, MySourceParameters> { ... }
 * class MySourceParameters implements Source.SourceParameters<MySource> { ... }
 *
 * interface MyProducer<T> extends CellValueProducer<MySource, T, MySinkParameters> {}
 *
 * {@literal /}* Register some producers once *{@literal /}
 * final MyProducer<Integer> intProducer = (sink, parameters) -> {
 *      // read value from source, configured with parameters
 * }
 *
 * MappingFramework.forSourceType(MySource.class) //
 *                 .registerProducer(Integer.class, intProducer);
 *
 * {@literal /}* Convert and read some KNIME data *{@literal /}
 *
 * DataTableSpec spec = ...;
 * DataContainer container = new DataContainer(spec);
 *
 * MySource source = new MySource(spec);
 * source.addToContainer(container);
 * container.close();
 *
 * </pre>
 * </code>
 *
 * @author Jonathan Hale
 */
public class MappingFramework {

    private static NodeLogger m_logger = NodeLogger.getLogger(MappingFramework.class);

    /* Do not instantiate */
    private MappingFramework() {
    }

    /**
     * Exception thrown when either a {@link CellValueProducer} or converter was missing for a type which needed to be
     * converted.
     *
     * @author Jonathan Hale
     */
    public static class UnmappableTypeException extends Exception {

        /* Generated serial version UID */
        private static final long serialVersionUID = 6498668986346262079L;

        private final DataType m_type;

        private final Class<?> m_javaType;

        /**
         * Constructor
         *
         * @param message Error message
         * @param type Type that could not be mapped.
         */
        public UnmappableTypeException(final String message, final DataType type) {
            this(message, type, null);
        }

        /**
         * Constructor
         *
         * @param message Error message
         * @param javaType Java type that could not be mapped
         */
        public UnmappableTypeException(final String message, final Class<?> javaType) {
            this(message, null, javaType);
        }

        /**
         * Constructor
         *
         * @param message Error message
         * @param type Type that could not be mapped.
         * @param javaType Java type that could not be mapped
         */
        public UnmappableTypeException(final String message, final DataType type, final Class<?> javaType) {
            super(message);

            m_type = type;
            m_javaType = javaType;
        }

        /**
         * @return The data type that was not mappable. May be <code>null</code>.
         */
        public DataType getDataType() {
            return m_type;
        }

        /**
         * @return The java type that was not mappable. May be <code>null</code>.
         */
        public Class<?> getJavaType() {
            return m_javaType;
        }
    }

    /**
     * A cell value consumer receives a KNIME data value and writes it to a {@link Sink}.
     *
     * @author Jonathan Hale
     * @param <SinkType> Type of {@link Sink} this consumer writes to
     * @param <T> Type of Java value the consumer accepts
     * @param <SP> Subtype of {@link ConsumerParameters} that can be used to configure this consumer
     */
    @FunctionalInterface
    public static interface CellValueConsumer<SinkType extends Sink<SinkType>, T, SP extends Sink.ConsumerParameters<SinkType>> {

        /**
         * Writes the <code>value</code> to <code>sink</code> using given <code>sinkParams</code>.
         *
         * @param sink The {@link Sink}.
         * @param value The value to write.
         * @param sinkParams The parameters further specifying how to write to the sink, e.g. to which SQL column or
         *            table to write. Specific to the type of {@link Sink} and {@link CellValueConsumer} that is being
         *            used.
         */
        public void consumeCellValue(final SinkType sink, final T value, final SP sinkParams);
    }

    /**
     * A cell value producer fetches a value from a {@link Source} which then can be written to a KNIME DataCell.
     *
     * @author Jonathan Hale
     * @param <SourceType> Type of {@link Source} this consumer writes to
     * @param <T> Type of Java value the consumer accepts
     * @param <SP> Subtype of {@link Source.ProducerParameters} that can be used to configure this consumer
     */
    @FunctionalInterface
    public static interface CellValueProducer<SourceType extends Source<SourceType>, T, SP extends Source.ProducerParameters<SourceType>> {

        /**
         * Reads the <code>value</code> to <code>sink</code> using given <code>sinkParams</code>.
         *
         * @param source The {@link Source}.
         * @param params The parameters further specifying how to read from the {@link Source}, e.g. to which SQL column
         *            or table to read from. Specific to the type of {@link Source} and {@link CellValueProducer} that
         *            is being used.
         * @return The value which was read from source
         */
        public T produceCellValue(final SourceType source, final SP params);
    }

    /**
     * Place to register consumers to make them available to {@link Sink}s.
     *
     * @author Jonathan Hale
     */
    public static class ProducerConsumerRegistry {

        /* Do not instantiate */
        private ProducerConsumerRegistry() {
        }

        /**
         * Per sink type consumer registry.
         *
         * Place to register consumers for a specific sink type.
         *
         * @author Jonathan Hale
         * @param <SinkType> Type of {@link Sink} for which this registry holds consumers.
         */
        public static class ConsumerRegistry<SinkType extends Sink<SinkType>> {
            private HashMap<Class<?>, CellValueConsumer<SinkType, ?, ?>> m_consumers = new HashMap<>();

            private Class<SinkType> m_sinkType;

            /**
             * Constructor
             *
             * @param sinkType Class of the sink type of this registry. Used for log messages.
             */
            protected ConsumerRegistry(final Class<SinkType> sinkType) {
                m_sinkType = sinkType;
            }

            /**
             * Register a consumer to make it available to {@link Sink}s of
             *
             * @param sourceType Type this {@link CellValueConsumer} can accept.
             * @param consumer The {@link CellValueConsumer}.
             * @return self (for method chaining)
             */
            public ConsumerRegistry<SinkType> registerConsumer(final Class<?> sourceType,
                final CellValueConsumer<SinkType, ?, ?> consumer) {
                if (m_consumers.putIfAbsent(sourceType, consumer) != null) {
                    m_logger
                        .warn(String.format("A %s CellValueConsumer was already registered for sink type %s, skipping.",
                            sourceType.getSimpleName(), m_sinkType.getSimpleName()));
                }

                return this;
            }

            /**
             * Get a certain {@link CellValueConsumer}.
             *
             * @param sourceType Source type that should be consumable by the returned {@link CellValueConsumer}.
             * @return a {@link CellValueConsumer} matching the given criteria, or <code>null</code> if none matches
             *         them.
             */
            public <T, SP extends ConsumerParameters<SinkType>> CellValueConsumer<SinkType, T, SP>
                get(final Class<T> sourceType) {
                @SuppressWarnings("unchecked") // ensured while registering
                final CellValueConsumer<SinkType, T, SP> consumer =
                    (CellValueConsumer<SinkType, T, SP>)m_consumers.get(sourceType);
                return consumer;
            }

            /**
             * @param type Data type that should be converted.
             * @return List of conversion paths
             */
            public Collection<ConsumptionPath> getAvailableConsumptionPaths(final DataType type) {
                final ArrayList<ConsumptionPath> cp = new ArrayList<>();

                for (final DataCellToJavaConverterFactory<?, ?> f : DataCellToJavaConverterRegistry.getInstance()
                    .getFactoriesForSourceType(type)) {
                    final CellValueConsumer<SinkType, ?, ?> c = get(f.getDestinationType());
                    if (c != null) {
                        cp.add(new ConsumptionPath(f, c));
                    }
                }

                return cp;
            }

            /**
             * Unregister all consumers
             *
             * @return self (for method chaining)
             */
            public ConsumerRegistry<SinkType> unregisterAllConsumers() {
                m_consumers.clear();
                return this;
            }
        }

        /**
         * Per source type producer registry.
         *
         * Place to register consumers for a specific sink type.
         *
         * @author Jonathan Hale
         * @param <SourceType> Type of {@link Sink} for which this registry holds consumers.
         */
        public static class ProducerRegistry<SourceType extends Source<SourceType>> {
            private HashMap<Class<?>, CellValueProducer<SourceType, ?, ?>> m_producers = new HashMap<>();

            private Class<SourceType> m_sourceType;

            /**
             * Constructor
             *
             * @param sourceType Class of the source type of this registry. Used for log messages.
             */
            protected ProducerRegistry(final Class<SourceType> sourceType) {
                m_sourceType = sourceType;
            }

            /**
             * Register a consumer to make it available to {@link Sink}s of
             *
             * @param destType Type this {@link CellValueConsumer} can accept.
             * @param producer The {@link CellValueConsumer}.
             * @return self (for method chaining)
             */
            public ProducerRegistry<SourceType> registerProducer(final Class<?> destType,
                final CellValueProducer<SourceType, ?, ?> producer) {
                if (m_producers.putIfAbsent(destType, producer) != null) {
                    m_logger.warn(
                        String.format("A %s CellValueProducer was already registered for source type %s, skipping.",
                            destType.getSimpleName(), m_sourceType.getSimpleName()));
                }
                return this;
            }

            /**
             * Get a certain {@link CellValueConsumer}.
             *
             * @param destType Source type that should be consumable by the returned {@link CellValueConsumer}.
             * @return a {@link CellValueConsumer} matching the given criteria, or <code>null</code> if none matches
             *         them.
             */
            public <T, SP extends ProducerParameters<SourceType>> CellValueProducer<SourceType, T, SP>
                get(final Class<T> destType) {
                @SuppressWarnings("unchecked") // ensured while registering
                final CellValueProducer<SourceType, T, SP> producer =
                    (CellValueProducer<SourceType, T, SP>)m_producers.get(destType);
                return producer;
            }

            /**
             * @param type Data type that should be converted.
             * @return List of conversion paths
             */
            public Collection<ProductionPath> getAvailableProductionPaths(final DataType type) {
                final ArrayList<ProductionPath> cp = new ArrayList<>();

                for (final JavaToDataCellConverterFactory<?> f : JavaToDataCellConverterRegistry.getInstance()
                    .getFactoriesForDestinationType(type)) {
                    final CellValueProducer<SourceType, ?, ?> p = get(f.getSourceType());
                    if (p != null) {
                        cp.add(new ProductionPath(f, p));
                    }
                }

                return cp;
            }

            /**
             * Unregister all consumers
             *
             * @return self (for method chaining)
             */
            public ProducerRegistry<SourceType> unregisterAllProducers() {
                m_producers.clear();
                return this;
            }
        }

        /**
         * Get the {@link CellValueConsumer} registry for given sink type.
         *
         * @param sinkType {@link Sink} type for which to get the registry
         * @return Per sink type consumer registry for given sink type.
         */
        public static <SinkType extends Sink<SinkType>> ConsumerRegistry<SinkType>
            forSinkType(final Class<SinkType> sinkType) {

            final ConsumerRegistry<SinkType> perSinkType = getConsumerRegistry(sinkType);
            if (perSinkType == null) {
                return createConsumerRegistry(sinkType);
            }

            return perSinkType;
        }

        /**
         * Get the {@link CellValueProducer} registry for given source type.
         *
         * @param sourceType {@link Source} type for which to get the registry
         * @return Per source type producer registry for given source type.
         */
        public static <SourceType extends Source<SourceType>> ProducerRegistry<SourceType>
            forSourceType(final Class<SourceType> sourceType) {
            final ProducerRegistry<SourceType> perSourceType = getProducerRegistry(sourceType);
            if (perSourceType == null) {
                return createProducerRegistry(sourceType);
            }

            return perSourceType;
        }

        private static HashMap<Class<? extends Sink<?>>, ConsumerRegistry<?>> m_sinkTypes = new HashMap<>();

        private static HashMap<Class<? extends Source<?>>, ProducerRegistry<?>> m_sourceTypes = new HashMap<>();

        /* Get the consumer registry for given sink type */
        private static <SinkType extends Sink<SinkType>> ConsumerRegistry<SinkType>
            getConsumerRegistry(final Class<SinkType> sinkType) {
            @SuppressWarnings("unchecked")
            final ConsumerRegistry<SinkType> r = (ConsumerRegistry<SinkType>)m_sinkTypes.get(sinkType);
            return r;
        }

        private static <SourceType extends Source<SourceType>> ProducerRegistry<SourceType>
            getProducerRegistry(final Class<SourceType> sourceType) {
            @SuppressWarnings("unchecked")
            final ProducerRegistry<SourceType> r = (ProducerRegistry<SourceType>)m_sourceTypes.get(sourceType);
            return r;
        }

        /* Create the consumer registry for given sink type */
        private static <SinkType extends Sink<SinkType>> ConsumerRegistry<SinkType>
            createConsumerRegistry(final Class<SinkType> sinkType) {
            final ConsumerRegistry<SinkType> r = new ConsumerRegistry<SinkType>(sinkType);
            m_sinkTypes.put(sinkType, r);
            return r;
        }

        private static <SourceType extends Source<SourceType>> ProducerRegistry<SourceType>
            createProducerRegistry(final Class<SourceType> sourceType) {
            final ProducerRegistry<SourceType> r = new ProducerRegistry<SourceType>(sourceType);
            m_sourceTypes.put(sourceType, r);
            return r;
        }
    }

}
