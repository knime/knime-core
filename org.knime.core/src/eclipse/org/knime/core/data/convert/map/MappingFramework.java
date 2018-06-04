package org.knime.core.data.convert.map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.convert.AbstractConverterFactoryRegistry;
import org.knime.core.data.convert.ConverterFactory;
import org.knime.core.data.convert.datacell.JavaToDataCellConverter;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterFactory;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterRegistry;
import org.knime.core.data.convert.java.DataCellToJavaConverter;
import org.knime.core.data.convert.java.DataCellToJavaConverterFactory;
import org.knime.core.data.convert.java.DataCellToJavaConverterRegistry;
import org.knime.core.data.convert.map.Destination.ConsumerParameters;
import org.knime.core.data.convert.map.Source.ProducerParameters;
import org.knime.core.data.convert.util.SerializeUtil;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.ExecutionContext;

/**
 * Framework to map KNIME to external types vice versa.
 *
 * <p>
 * A frequent use case for KNIME nodes is to write or read data from an external storage. (A storage can be used as a
 * {@link Source source} or {@link Destination destination}.) In this case the value held by a KNIME {@link DataCell}
 * needs to be mapped to an external value.
 * </p>
 *
 * <p>
 * Extracting the Java value from a data cell is solved with {@link DataCellToJavaConverterRegistry} and
 * {@link JavaToDataCellConverterRegistry}. As custom KNIME extensions may implement custom {@link DataType data types},
 * it is impossible for nodes to support all possible types. Asking the custom type provider to implement mapping to all
 * different external sources and destinations (Python, SQL, H2O, Java, R and many more) is an impossible burden and
 * therefore we introduce Java objects as an intermediary representation for the mappings. An external type provider can
 * implement a {@link DataCellToJavaConverterFactory} or {@link JavaToDataCellConverterFactory} to extract a Java object
 * out of his custom cell and implementors or nodes which write to SQL, H2O, R, Python, etc. can then provide mapping to
 * a more limited set of Java types. Some cells may not be able to create a known java type (e.g. only some
 * VeryChemicalType) -- in which case we cannot support the type -- but usually such a type can be serialized to a blob
 * or String.
 * </p>
 *
 * <p>
 * Some external types do not have a Java representation. We can therefore not simply map from Java object to a Java
 * representation of an external type. Instead we wrap the concepts of {@link Source source} and {@link Destination
 * destination} and the concepts of "how to write to" ({@link Consumer}) and "how to read from" ({@link Producer}) them.
 * The destination and source are external equivalent to a KNIME input or output table. How to write/read from them is
 * defined per set of types, but configurable via {@link ConsumerParameters}/{@link ProducerParameters}. These
 * parameters may include column index and row index, but are fully dependent on the type of external storage and meant
 * as a way in which the node communicates with an instance of {@link Consumer} or {@link Producer}. (TODO)
 * </p>
 *
 * <p>
 * With databases as storage, we have another special case where some databases are specializations of other databases
 * (e.g. Oracle databases a specialization of SQL databases). The specialization may support additional Java types or
 * additional external types.
 * </p>
 *
 * <p>
 * Finally, all mapping must be allowed to be done explicitly. Since automatic mappings are often not useful, a user
 * needs control of how to map certain KNIME types to the external types (the intermediate java representation is not
 * relevant) -- via a node dialog panel for example.
 * </p>
 *
 * <p>
 * For the dialog panel to be as generic and reusable as possible, its only input should be the {@link DataTableSpec}
 * and the type of destination or source. It then presents the user a per column list of available external types to map
 * to. This list can be queried from the framework using {@link #getAvailableConsumptionPaths(Class<?>, DataType)} or
 * {@link #getAvailableProductionPaths(Class<?>, DataType)}. Both return a list of glorified pairs which also contain
 * the information on how the intermediate java representation is extracted from or wrapped into a {@link DataCell}.
 * These can then be serialized from the dialog and read in a node model.
 * </p>
 *
 * <h1>Usage</h1>
 *
 * For any given destination type registering consumers is a matter of:
 *
 * <code lang="java"><pre>
 * // One time setup, e.g. in plugin initialisation:
 * MappingFramework.forDestinationType(OracleSQLDatabaseDest.class) //
 *      .setParent(SQLDatabaseDest.class) // inherit less specific consumers
 *      .register(new SimpleDataValueConsumerFactory(
 *          String.class, "TEXT", (dest, value, params) -> { /* ... *{@literal /} }));
 * </pre></code>
 *
 * This will make certain consumption paths (pairs of {@link DataCellToJavaConverterFactory} and
 * {@link DataValueConsumerFactory}) available, which can be queried with
 * {@link ConsumerRegistry#getAvailableConsumptionPaths(DataType)}.
 *
 * For any given source type registering consumers is a matter of:
 *
 * <code><pre>
 * // One time setup, e.g. in plugin initialisation:
 * MappingFramework.forSourceType(MySourceType.class) //
 *      .setParent(MyParentSourceType.class) // inherit less specific producers
 *      .register(new SimpleDataValueProducerFactory(
 *          "TEXT", String.class, (dest, params) -> { /* ... *{@literal /} }));
 * </pre></code>
 *
 * This will make certain production paths (pairs of {@link JavaToDataCellConverterFactory} and
 * {@link DataValueProducerFactory}) available, which can be queried with
 * {@link ProducerRegistry#getAvailableProductionPaths(String)}.
 *
 * @author Jonathan Hale, KNIME, Konstanz, Germany
 * @see SerializeUtil SerializeUtil - for serializing ConsumptionPath or ProductionPath.
 */
public class MappingFramework {

    /* Do not instantiate */
    private MappingFramework() {
    }

    /**
     * Exception thrown when either a {@link CellValueProducer} or converter was missing for a type which needed to be
     * converted.
     *
     * @author Jonathan Hale, KNIME, Konstanz, Germany
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
     * A cell value consumer receives a Java value and writes it to a {@link Destination} using a certain external type.
     *
     * @author Jonathan Hale, KNIME, Konstanz, Germany
     * @param <DestinationType> Type of {@link Destination} this consumer writes to
     * @param <T> Type of Java value the consumer accepts
     * @param <CP> Subtype of {@link ConsumerParameters} that can be used to configure this consumer
     */
    @FunctionalInterface
    public static interface CellValueConsumer<DestinationType extends Destination, T, CP extends Destination.ConsumerParameters<DestinationType>> {

        /**
         * Writes the <code>value</code> to <code>destination</code> using given <code>destinationParams</code>.
         *
         * @param destination The {@link Destination}.
         * @param value The value to write.
         * @param destinationParams The parameters further specifying how to write to the destination, e.g. to which SQL
         *            column or table to write. Specific to the type of {@link Destination} and
         *            {@link CellValueConsumer} that is being used.
         */
        public void consumeCellValue(final DestinationType destination, final T value, final CP destinationParams);
    }

    /**
     * Factory to create {@link CellValueConsumer}.
     *
     * @author Jonathan Hale, KNIME, Konstanz, Germany
     * @param <DestinationType> Type of destination
     * @param <T> Java value to be consumed by the created {@link CellValueConsumer}
     * @param <ExternalType> Type of destination types
     * @param <CP> Subclass of {@link ConsumerParameters} for the given source type
     */
    public static interface CellValueConsumerFactory<DestinationType extends Destination, T, ExternalType, CP extends ConsumerParameters<DestinationType>>
        extends ConverterFactory<Class<?>, ExternalType, CellValueConsumer<DestinationType, ?, CP>> {

        /**
         * Create a {@link CellValueConsumer}.
         *
         * @return The created consumer
         */
        public CellValueConsumer<DestinationType, T, CP> create();

        @Override
        default String getName() {
            return getDestinationType().toString();
        }
    }

    /**
     * Abstract implementation of {@link CellValueConsumerFactory}.
     *
     * @author Jonathan Hale, KNIME, Konstanz, Germany
     * @param <DestinationType> Type of destination
     * @param <T> Java type the created consumer is able to accept
     * @param <ExternalType> Type of destination types
     * @param <CP> Subclass of {@link ConsumerParameters} for given destination type
     */
    public static abstract class AbstractCellValueConsumerFactory<DestinationType extends Destination, T, ExternalType, CP extends ConsumerParameters<DestinationType>>
        implements CellValueConsumerFactory<DestinationType, T, ExternalType, CP> {

        @Override
        public int hashCode() {
            return getIdentifier().hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj instanceof CellValueConsumerFactory) {
                return getIdentifier().equals(((CellValueConsumerFactory)obj).getIdentifier());
            }
            return false;
        }
    }

    /**
     * Simple implementation of {@link CellValueConsumer} that allows passing the consumption procedure as a lambda.
     *
     * @author Jonathan Hale, KNIME, Konstanz, Germany
     * @param <DestinationType> Type of destination
     * @param <T> Java type the created consumer is able to accept
     * @param <ExternalType> Type of destination types
     * @param <CP> Subclass of {@link ConsumerParameters} for given destination type
     */
    public static class SimpleCellValueConsumerFactory<DestinationType extends Destination, T, ExternalType, CP extends ConsumerParameters<DestinationType>>
        extends AbstractCellValueConsumerFactory<DestinationType, T, ExternalType, CP> {

        final ExternalType m_externalType;

        final Class<?> m_sourceType;

        final CellValueConsumer<DestinationType, T, CP> m_consumer;

        /**
         * Constructor
         *
         * @param sourceType Class of the type the created consumer accepts
         * @param destType Identifier of the external type this consumer writes as
         * @param consumer The consumer function (e.g. a Lambda)
         */
        public SimpleCellValueConsumerFactory(final Class<?> sourceType, final ExternalType destType,
            final CellValueConsumer<DestinationType, T, CP> consumer) {
            m_sourceType = sourceType;
            m_externalType = destType;
            m_consumer = consumer;
        }

        @Override
        public String getIdentifier() {
            return m_sourceType.getName() + "->" + m_externalType;
        }

        @Override
        public CellValueConsumer<DestinationType, T, CP> create() {
            return m_consumer;
        }

        @Override
        public ExternalType getDestinationType() {
            return m_externalType;
        }

        @Override
        public Class<?> getSourceType() {
            return m_sourceType;
        }
    }

    /**
     * A cell value producer fetches a value from a {@link Source} which then can be written to a KNIME DataCell.
     *
     * @author Jonathan Hale, KNIME, Konstanz, Germany
     * @param <SourceType> Type of {@link Source} this consumer writes to
     * @param <T> Type of Java value the consumer accepts
     * @param <CP> Subtype of {@link Source.ProducerParameters} that can be used to configure this consumer
     */
    @FunctionalInterface
    public static interface CellValueProducer<SourceType extends Source, T, CP extends Source.ProducerParameters<SourceType>> {

        /**
         * Reads the <code>value</code> to <code>destination</code> using given <code>destinationParams</code>.
         *
         * @param source The {@link Source}.
         * @param params The parameters further specifying how to read from the {@link Source}, e.g. to which SQL column
         *            or table to read from. Specific to the type of {@link Source} and {@link CellValueProducer} that
         *            is being used.
         * @return The value which was read from source
         */
        public T produceCellValue(final SourceType source, final CP params);
    }

    /**
     * Factory for {@link CellValueProducer}.
     *
     * @author Jonathan Hale, KNIME, Konstanz, Germany
     * @param <SourceType> Type of source
     * @param <ExternalType> Type of the external type
     * @param <T> Java type the created consumer is able to accept
     * @param <PP> Subclass of {@link ProducerParameters} for given destination type
     */
    public static interface CellValueProducerFactory<SourceType extends Source<ExternalType>, ExternalType, T, PP extends ProducerParameters<SourceType>>
        extends ConverterFactory<ExternalType, Class<?>, CellValueProducer<SourceType, ?, PP>> {

        @Override
        default String getName() {
            return getDestinationType().getName();
        }

        /**
         * Create a CellValueProducer
         *
         * @return The created producer
         */
        CellValueProducer<SourceType, T, PP> create();
    }

    /**
     * Abstract implementation of {@link CellValueProducerFactory}.
     *
     * @author Jonathan Hale, KNIME, Konstanz, Germany
     * @param <SourceType> Type of source
     * @param <T> Java type the created consumer is able to accept
     * @param <ExternalType> Type of the external type
     * @param <PP> Subclass of {@link ProducerParameters} for given destination type
     */
    public static abstract class AbstractCellValueProducerFactory<SourceType extends Source<ExternalType>, ExternalType, T, PP extends ProducerParameters<SourceType>>
        implements CellValueProducerFactory<SourceType, ExternalType, T, PP> {

        @Override
        public int hashCode() {
            return getIdentifier().hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj instanceof CellValueProducerFactory) {
                return getIdentifier().equals(((CellValueProducerFactory)obj).getIdentifier());
            }
            return false;
        }
    }

    /**
     * Simple implementation of {@link CellValueProducer} that allows passing the production function as a lambda
     *
     * @author Jonathan Hale, KNIME, Konstanz, Germany
     * @param <SourceType> Type of source
     * @param <ExternalType> Type of the external type
     * @param <T> Java type that is produced
     * @param <PP> Producer parameter subclass for the source type
     */
    public static class SimpleCellValueProducerFactory<SourceType extends Source<ExternalType>, ExternalType, T, PP extends ProducerParameters<SourceType>>
        extends AbstractCellValueProducerFactory<SourceType, ExternalType, T, PP> {

        final ExternalType m_externalType;

        final Class<?> m_destType;

        final CellValueProducer<SourceType, T, PP> m_producer;

        /**
         * Constructor
         *
         * @param externalType Identifier of the external type
         * @param destType Target Java type
         * @param producer Cell value producer function (e.g. as lambda)
         */
        public SimpleCellValueProducerFactory(final ExternalType externalType, final Class<?> destType,
            final CellValueProducer<SourceType, T, PP> producer) {
            m_externalType = externalType;
            m_destType = destType;
            m_producer = producer;
        }

        @Override
        public String getIdentifier() {
            return m_externalType + "->" + m_destType.getName();
        }

        @Override
        public Class<?> getDestinationType() {
            return m_destType;
        }

        @Override
        public ExternalType getSourceType() {
            return m_externalType;
        }

        @Override
        public CellValueProducer<SourceType, T, PP> create() {
            return m_producer;
        }
    }

    /**
     * Per destination type consumer registry.
     *
     * Place to register consumers for a specific destination type.
     *
     * @author Jonathan Hale, KNIME, Konstanz, Germany
     * @param <DestinationType> Type of {@link Destination} for which this registry holds consumers.
     * @param <ExternalType> Type of destination types
     */
    public static class ConsumerRegistry<ExternalType, DestinationType extends Destination<ExternalType>> extends
        AbstractConverterFactoryRegistry<Class<?>, ExternalType, CellValueConsumerFactory<DestinationType, ?, ExternalType, ?>, ConsumerRegistry<ExternalType, DestinationType>> {

        /**
         * Constructor
         */
        protected ConsumerRegistry() {
        }

        /**
         * Set parent destination type.
         *
         * Makes this registry inherit all consumers of the parent type. Will always priorize consumers of the more
         * specialized type.
         *
         * @param parentType type of {@link Destination}, which should be this types parent.
         * @return reference to self (for method chaining)
         */
        public ConsumerRegistry<ExternalType, DestinationType> setParent(final Class<? extends Destination> parentType) {
            m_parent = MappingFramework.forDestinationType(parentType);
            return this;
        }

        /**
         * @param type Data type that should be converted.
         * @return List of conversion paths
         */
        public List<ConsumptionPath> getAvailableConsumptionPaths(final DataType type) {
            final ArrayList<ConsumptionPath> cp = new ArrayList<>();

            for (final DataCellToJavaConverterFactory<?, ?> f : DataCellToJavaConverterRegistry.getInstance()
                .getFactoriesForSourceType(type)) {
                for (final CellValueConsumerFactory<DestinationType, ?, ?, ?> c : getFactoriesForSourceType(
                    f.getDestinationType())) {
                    if (c != null) {
                        cp.add(new ConsumptionPath(f, c));
                    }
                }
            }

            return cp;
        }

        /**
         * Unregister all consumers
         *
         * @return self (for method chaining)
         */
        public ConsumerRegistry<ExternalType, DestinationType> unregisterAllConsumers() {
            m_byDestinationType.clear();
            m_bySourceType.clear();
            m_byIdentifier.clear();
            m_factories.clear();
            return this;
        }
    }

    /**
     * Per source type producer registry.
     *
     * Place to register consumers for a specific destination type.
     *
     * @author Jonathan Hale, KNIME, Konstanz, Germany
     * @param <ExternalType> Type of the external type
     * @param <SourceType> Type of {@link Destination} for which this registry holds consumers.
     */
    public static class ProducerRegistry<ExternalType, SourceType extends Source<ExternalType>> extends
        AbstractConverterFactoryRegistry<ExternalType, Class<?>, CellValueProducerFactory<SourceType, ExternalType, ?, ?>, ProducerRegistry<ExternalType, SourceType>> {

        /**
         * Constructor
         */
        protected ProducerRegistry() {
        }

        /**
         * Set parent source type.
         *
         * Makes this registry inherit all producers of the parent type. Will always priorize producers of the more
         * specialized type.
         *
         * @param parentType type of {@link Destination}, which should be this types parent.
         * @return reference to self (for method chaining)
         */
        public ProducerRegistry<ExternalType, SourceType> setParent(final Class<? extends Source> parentType) {
            m_parent = MappingFramework.forSourceType(parentType);
            return this;
        }

        /**
         * Get production paths that can map the given external type to a DataCell.
         *
         * @param externalType The external type
         * @return All possible production paths
         */
        public List<ProductionPath> getAvailableProductionPaths(final ExternalType externalType) {
            final ArrayList<ProductionPath> cp = new ArrayList<>();

            for (final CellValueProducerFactory<SourceType, ExternalType, ?, ?> producerFactory : getFactoriesForSourceType(
                externalType)) {

                for (final JavaToDataCellConverterFactory<?> f : JavaToDataCellConverterRegistry.getInstance()
                    .getFactoriesForSourceType(producerFactory.getDestinationType())) {
                    cp.add(new ProductionPath(producerFactory, f));
                }
            }

            if (m_parent != null) {
                cp.addAll(m_parent.getAvailableProductionPaths(externalType));
            }

            return cp;
        }

        /**
         * Unregister all consumers
         *
         * @return self (for method chaining)
         */
        public ProducerRegistry<ExternalType, SourceType> unregisterAllProducers() {
            m_byDestinationType.clear();
            m_bySourceType.clear();
            m_byIdentifier.clear();
            m_factories.clear();
            return this;
        }
    }

    /**
     * Get the {@link CellValueConsumer} registry for given destination type.
     *
     * @param destinationType {@link Destination} type for which to get the registry
     * @return Per destination type consumer registry for given destination type.
     */
    public static <ExternalType, DestinationType extends Destination<ExternalType>> ConsumerRegistry<ExternalType, DestinationType>
        forDestinationType(final Class<DestinationType> destinationType) {

        final ConsumerRegistry<ExternalType, DestinationType> perDestinationType = getConsumerRegistry(destinationType);
        if (perDestinationType == null) {
            return createConsumerRegistry(destinationType);
        }

        return perDestinationType;
    }

    /**
     * Get the {@link CellValueProducer} registry for given source type.
     *
     * @param sourceType {@link Source} type for which to get the registry
     * @return Per source type producer registry for given source type.
     */
    public static <ExternalType, SourceType extends Source<ExternalType>> ProducerRegistry<ExternalType, SourceType>
        forSourceType(final Class<SourceType> sourceType) {
        final ProducerRegistry<ExternalType, SourceType> perSourceType = getProducerRegistry(sourceType);
        if (perSourceType == null) {
            return createProducerRegistry(sourceType);
        }

        return perSourceType;
    }

    private static HashMap<Class<? extends Destination>, ConsumerRegistry<?, ?>> m_destinationTypes = new HashMap<>();

    private static HashMap<Class<? extends Source>, ProducerRegistry<?, ?>> m_sourceTypes = new HashMap<>();

    /* Get the consumer registry for given destination type */
    private static <ExternalType, DestinationType extends Destination<ExternalType>> ConsumerRegistry<ExternalType, DestinationType>
        getConsumerRegistry(final Class<DestinationType> destinationType) {
        @SuppressWarnings("unchecked")
        final ConsumerRegistry<ExternalType, DestinationType> r =
            (ConsumerRegistry<ExternalType, DestinationType>)m_destinationTypes.get(destinationType);
        return r;
    }

    private static <ExternalType, SourceType extends Source<ExternalType>> ProducerRegistry<ExternalType, SourceType>
        getProducerRegistry(final Class<SourceType> sourceType) {
        @SuppressWarnings("unchecked")
        final ProducerRegistry<ExternalType, SourceType> r = (ProducerRegistry<ExternalType, SourceType>)m_sourceTypes.get(sourceType);
        return r;
    }

    /* Create the consumer registry for given destination type */
    private static <ExternalType, DestinationType extends Destination<ExternalType>> ConsumerRegistry<ExternalType, DestinationType>
        createConsumerRegistry(final Class<DestinationType> destinationType) {
        final ConsumerRegistry<ExternalType, DestinationType> r = new ConsumerRegistry<ExternalType, DestinationType>();
        m_destinationTypes.put(destinationType, r);
        return r;
    }

    private static <ExternalType, SourceType extends Source<ExternalType>> ProducerRegistry<ExternalType, SourceType>
        createProducerRegistry(final Class<SourceType> sourceType) {
        final ProducerRegistry<ExternalType, SourceType> r = new ProducerRegistry<ExternalType, SourceType>();
        m_sourceTypes.put(sourceType, r);
        return r;
    }

    /**
     * Map a row of input data from the given source to a {@link DataRow}.
     *
     * @param key Row key for the created row
     * @param source Source to get data from
     * @param mapping Production paths to take when reading a column and producting a {@link DataCell} from it.
     * @param params Per column parameters for the producers used
     * @param context Execution context potentially required to create converters
     * @return The DataRow which contains the data read from the source
     * @throws Exception If conversion fails
     */
    public static <SourceType extends Source, PP extends ProducerParameters<SourceType>> DataRow map(final RowKey key,
        final SourceType source, final ProductionPath[] mapping, final PP[] params, final ExecutionContext context)
        throws Exception {

        final DataCell[] cells = new DataCell[mapping.length];

        int i = 0;
        for (final ProductionPath path : mapping) {
            final JavaToDataCellConverter<?> converter = path.m_converterFactory.create(context);
            final CellValueProducer<SourceType, ?, PP> producer =
                (CellValueProducer<SourceType, ?, PP>)path.m_producerFactory.create();

            cells[i] = converter.convertUnsafe(producer.produceCellValue(source, params[i]));
            ++i;
        }

        return new DefaultRow(key, cells);
    }

    /**
     * Map data from a {@link DataRow} to an external storage.
     *
     * @param row Row to map
     * @param dest Destination to write to
     * @param mapping Consumption paths to take when extracting a value from a cell and "consuming" to write to an
     *            external type.
     * @param params Per column parameters for the consumers used
     * @throws Exception If conversion fails
     */
    public static <ExternalType extends Destination, CP extends ConsumerParameters<ExternalType>> void map(final DataRow row,
        final ExternalType dest, final ConsumptionPath[] mapping, final CP[] params) throws Exception {

        int i = 0;
        for (final DataCell cell : row) {
            final DataCellToJavaConverter<?, ?> converter = mapping[i].m_converterFactory.create();
            final CellValueConsumer<ExternalType, Object, CP> consumer =
                (CellValueConsumer<ExternalType, Object, CP>)mapping[i].m_consumerFactory.create();

            consumer.consumeCellValue(dest, converter.convertUnsafe(cell), params[i]);
            ++i;
        }
    }
}
