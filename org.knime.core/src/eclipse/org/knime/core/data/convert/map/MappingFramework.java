package org.knime.core.data.convert.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.knime.core.data.DataTable;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.convert.java.DataCellToJavaConverter;
import org.knime.core.data.convert.java.DataCellToJavaConverterFactory;
import org.knime.core.data.convert.java.DataCellToJavaConverterRegistry;
import org.knime.core.data.convert.map.Sink.ConsumerParameters;

/**
 * Framework which handles writing a {@link DataTable} to a arbitrary destination, e.g. an SQL connection, or reading
 * from an arbitrary source to a DataTable.
 *
 * <h1>Usage</h1>
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
 * MappingFramework.forSinkType(MySink.class).registerConsumer(Integer.class, intConsumer);
 *
 * {@literal /}* Convert and write some KNIME data *{@literal /}
 * MySink sink = new MySink();
 * sink.addTable(table);
 *
 * </pre>
 * </code>
 *
 * @author Jonathan Hale
 */
public class MappingFramework {

    /* Do not instantiate */
    private MappingFramework() {
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
            private HashMap<Class<?>, List<CellValueConsumer<SinkType, ?, ?>>> m_consumers = new HashMap<>();

            /**
             * Constructor
             */
            protected ConsumerRegistry() {
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
                List<CellValueConsumer<SinkType, ?, ?>> list = m_consumers.get(sourceType);
                if (list == null) {
                    list = new ArrayList<CellValueConsumer<SinkType, ?, ?>>(1);
                    m_consumers.put(sourceType, list);
                }

                list.add(consumer);
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
                final List<CellValueConsumer<SinkType, ?, ?>> list = m_consumers.get(sourceType);
                if (list == null) {
                    return null;
                }

                @SuppressWarnings("unchecked")
                // TODO This is now a list...
                final CellValueConsumer<SinkType, T, SP> consumer = (CellValueConsumer<SinkType, T, SP>)list.get(0);
                return consumer;
            }

            /**
             * Get all available consumers to consume given type.
             *
             * @param cls Class that should be consumable by the returned consumer.
             * @return Collection of consumers that are able to consume given type.
             */
            public Collection<CellValueConsumer<SinkType, ?, ?>> getAvailableConsumers(final Class<?> cls) {
                final List<CellValueConsumer<SinkType, ?, ?>> consumers = m_consumers.get(cls);
                if (consumers == null) {
                    return Collections.emptyList();
                }
                return Collections.unmodifiableList(consumers);
            }

            /**
             * A selection of {@link DataCellToJavaConverter} to {@link CellValueConsumer} to write a certain
             * {@link DataValue} to a {@link Sink}.
             *
             * @author Jonathan Hale
             */
            public static class ConsumptionPath {
                final DataCellToJavaConverterFactory<?, ?> m_factory;

                final CellValueConsumer<?, ?, ?> m_consumer;

                /**
                 * Constructor.
                 *
                 * @param factory Factory of the converter used to extract a Java value out a DataCell.
                 * @param consumer CellValueConsumer which accepts the Java value extracted by the converter and writes
                 *            it to some {@link Sink}.
                 */
                public ConsumptionPath(final DataCellToJavaConverterFactory<?, ?> factory,
                    final CellValueConsumer<?, ?, ?> consumer) {
                    this.m_factory = factory;
                    this.m_consumer = consumer;
                }

                @Override
                public String toString() {
                    return String.format("%s --(\"%s\")-> %s ---> %s Consumer",
                        m_factory.getSourceType().getSimpleName(), m_factory.getName(),
                        m_factory.getDestinationType().getSimpleName(), m_consumer.getClass().getSimpleName());
                }

                @Override
                public int hashCode() {
                    final int prime = 31;
                    int result = 1;
                    result = prime * result + ((m_consumer == null) ? 0 : m_consumer.hashCode());
                    result = prime * result + ((m_factory == null) ? 0 : m_factory.hashCode());
                    return result;
                }

                @Override
                public boolean equals(final Object obj) {
                    if (this == obj) {
                        return true;
                    }
                    if (obj == null) {
                        return false;
                    }
                    if (getClass() != obj.getClass()) {
                        return false;
                    }
                    ConsumptionPath other = (ConsumptionPath)obj;
                    if (m_consumer == null) {
                        if (other.m_consumer != null) {
                            return false;
                        }
                    } else if (!m_consumer.equals(other.m_consumer)) {
                        return false;
                    }
                    if (m_factory == null) {
                        if (other.m_factory != null) {
                            return false;
                        }
                    } else if (!m_factory.equals(other.m_factory)) {
                        return false;
                    }
                    return true;
                }
            }

            /**
             * @param type Data type that should be converted.
             * @return List of conversion paths
             */
            public Collection<ConsumptionPath> getAvailableConsumptionPaths(final DataType type) {
                final ArrayList<ConsumptionPath> cp = new ArrayList<>();

                for (final DataCellToJavaConverterFactory<?, ?> f : DataCellToJavaConverterRegistry.getInstance()
                    .getFactoriesForSourceType(type)) {
                    for (final CellValueConsumer<SinkType, ?, ?> c : getAvailableConsumers(f.getDestinationType())) {
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
         * Register a consumer to make it available to {@link Sink}s of
         *
         * @param sinkType {@link Sink} type to register the consumer for.
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

        private static HashMap<Class<? extends Sink<?>>, ConsumerRegistry<?>> m_sinkTypes = new HashMap<>();

        /* Get the consumer registry for given sink type */
        private static <SinkType extends Sink<SinkType>> ConsumerRegistry<SinkType>
            getConsumerRegistry(final Class<SinkType> sinkType) {
            @SuppressWarnings("unchecked")
            final ConsumerRegistry<SinkType> r = (ConsumerRegistry<SinkType>)m_sinkTypes.get(sinkType);
            return r;
        }

        /* Create the consumer registry for given sink type */
        private static <SinkType extends Sink<SinkType>> ConsumerRegistry<SinkType>
            createConsumerRegistry(final Class<SinkType> sinkType) {
            final ConsumerRegistry<SinkType> r = new ConsumerRegistry<SinkType>();
            m_sinkTypes.put(sinkType, r);
            return r;
        }
    }

}
