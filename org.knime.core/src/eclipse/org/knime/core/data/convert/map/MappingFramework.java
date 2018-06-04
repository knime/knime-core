package org.knime.core.data.convert.map;

import java.util.HashMap;
import java.util.function.Consumer;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
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
 * @since 3.6
 */
public class MappingFramework {

    /* Do not instantiate */
    private MappingFramework() {
    }

    /**
     * Get the {@link CellValueConsumer} registry for given destination type.
     *
     * @param destinationType {@link Destination} type for which to get the registry
     * @return Per destination type consumer registry for given destination type.
     */
    public static <ExternalType, DestinationType extends Destination<ExternalType>>
        ConsumerRegistry<ExternalType, DestinationType>
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
    private static <ExternalType, DestinationType extends Destination<ExternalType>>
        ConsumerRegistry<ExternalType, DestinationType>
        getConsumerRegistry(final Class<DestinationType> destinationType) {
        @SuppressWarnings("unchecked")
        final ConsumerRegistry<ExternalType, DestinationType> r =
            (ConsumerRegistry<ExternalType, DestinationType>)m_destinationTypes.get(destinationType);
        return r;
    }

    private static <ExternalType, SourceType extends Source<ExternalType>> ProducerRegistry<ExternalType, SourceType>
        getProducerRegistry(final Class<SourceType> sourceType) {
        @SuppressWarnings("unchecked")
        final ProducerRegistry<ExternalType, SourceType> r =
            (ProducerRegistry<ExternalType, SourceType>)m_sourceTypes.get(sourceType);
        return r;
    }

    /* Create the consumer registry for given destination type */
    private static <ExternalType, DestinationType extends Destination<ExternalType>>
        ConsumerRegistry<ExternalType, DestinationType>
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
     * @throws Exception If an exception occurs during conversion or mapping
     */
    public static <ExternalType extends Destination, CP extends ConsumerParameters<ExternalType>> void
        map(final DataRow row, final ExternalType dest, final ConsumptionPath[] mapping, final CP[] params)
            throws Exception {

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
