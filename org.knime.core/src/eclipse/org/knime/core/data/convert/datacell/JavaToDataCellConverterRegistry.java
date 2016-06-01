package org.knime.core.data.convert.datacell;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.knime.core.data.DataCellFactory;
import org.knime.core.data.DataType;
import org.knime.core.data.DataTypeRegistry;
import org.knime.core.data.convert.ConversionKey;
import org.knime.core.data.convert.DataCellFactoryMethod;
import org.knime.core.data.convert.java.DataCellToJavaConverterFactory;
import org.knime.core.data.convert.util.ClassUtil;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.Pair;

/**
 * Service containing all registered {@link JavaToDataCellConverterFactory}s.
 * <p>
 * This class is a singleton, use {@link #getInstance()} to retrieve an instance.
 * </p>
 *
 * <b>Examples:</b>
 * <p>
 * Checking if a type can be converted into another type:
 *
 * <pre>
 * Class&lt;?>  mySourceType = ...;
 * DataType myDestType = ...;
 * String factoryName = ""; // "" is default
 *
 * boolean canConvert = JavaToDataCellConverterRegistry.getInstance()
 *                      .getConverterFactory(mySourceType, myDestType, factoryName)
 *                      .isPresent();
 * </pre>
 * </p>
 * <p>
 * Get all DataTypes which a certain java type can be converted to:
 *
 * <pre>
 * Class&lt;?> mySourceType = ...;
 *
 * Set&lt;DataType> possibleTypes = JavaToDataCellConverterRegistry.getInstance()
 *                      .getFactoriesForSourceType(mySourceType)
 *                      // Get the destination type of factories which can handle mySourceType
 *                      .stream().map((factory) -> factory.getDestinationType())
 *                      // Put all the destination types into a set
 *                      .collect(Collectors.toSet());
 * </pre>
 * </p>
 * <p>
 * Convert a value:
 *
 * <pre>
 * ExecutionContext myExecutionContext = ...;
 * T myObject = ...;
 * DataType myDestType = IntCell.TYPE;
 * String factoryName = ""; // "" is default
 *
 * Optional&lt;JavaToDataCellConverterFactory&lt;T>> factory =
 *                      JavaToDataCellConverterRegistry.getInstance()
 *                      .getConverterFactory(myObject.getClass(), myDestType, factoryName);
 * if (!factory.isPresent()) {
 *      // error!
 * }
 *
 * // Instantiate the converter
 * JavaToDataCellConverter&ltT> converter = factory.get().create(myExecutionContext);
 * // Convert the value
 * DataCell convertedValue = converter.convert(myObject);
 *
 * </pre>
 * </p>
 *
 * @author Jonathan Hale, KNIME, Konstanz, Germany
 * @since 3.2
 */
public final class JavaToDataCellConverterRegistry {

    private final static NodeLogger LOGGER = NodeLogger.getLogger(JavaToDataCellConverterRegistry.class);

    /* factories stored by their key */
    private final HashMap<ConversionKey, JavaToDataCellConverterFactory<?>> m_converterFactories = new HashMap<>();

    /* factories stored by destination type */
    private final HashMap<DataType, Set<JavaToDataCellConverterFactory<?>>> m_byDestinationType = new HashMap<>();

    /* factories stored by source type */
    private final HashMap<Class<?>, Set<JavaToDataCellConverterFactory<?>>> m_bySourceType = new HashMap<>();

    /* factories stored by identifier */
    private final HashMap<String, JavaToDataCellConverterFactory<?>> m_byIdentifier = new HashMap<>();

    /**
     * Query which DataTypes can be converted.
     *
     * @return a {@link Collection} of all possible data types into which can be converted.
     */
    public Collection<Class<?>> getAllSourceTypes() {
        return m_converterFactories.values().stream().map((factory) -> factory.getSourceType())
            .collect(Collectors.toSet());
    }

    /**
     * Query into which DataTypes can be converted.
     *
     * @return a {@link Collection} of all possible data types into which can be converted.
     */
    public Collection<DataType> getAllDestinationTypes() {
        return m_converterFactories.values().stream().map((factory) -> factory.getDestinationType())
            .collect(Collectors.toSet());
    }

    /**
     * @param sourceType the sourceType
     * @return a {@link Collection} of converter factories with the given <code>sourceType</code>
     */
    public <T> Collection<JavaToDataCellConverterFactory<?>> getFactoriesForSourceType(final Class<T> sourceType) {
        final LinkedHashSet<JavaToDataCellConverterFactory<?>> set = new LinkedHashSet<>();

        ClassUtil.recursiveMapToClassHierarchy(sourceType, (type) -> {
            final Set<JavaToDataCellConverterFactory<?>> destTypes = m_bySourceType.get(type);
            if (destTypes != null) {
                set.addAll(destTypes);
            }
        });

        if (sourceType.isArray()) {
            final LinkedHashSet<JavaToDataCellConverterFactory<?>> componentFactories =
                (LinkedHashSet<JavaToDataCellConverterFactory<?>>)getFactoriesForSourceType(
                    sourceType.getComponentType());

            set.addAll(componentFactories.stream().map((factory) -> createToCollectionConverterFactory(factory))
                .collect(Collectors.toList()));
        }

        return set;
    }

    /**
     * Get all factories with the given destination type.
     *
     * @param dataType The destination type
     * @return a {@link Collection} of converters
     */
    public Collection<JavaToDataCellConverterFactory<?>> getFactoriesForDestinationType(final DataType dataType) {
        Set<JavaToDataCellConverterFactory<?>> set = m_byDestinationType.get(dataType);

        if (dataType.isCollectionType()) {
            // every single-type converter can also be used to convert
            // components of an array.
            final Set<JavaToDataCellConverterFactory<?>> destinationTypes =
                (Set<JavaToDataCellConverterFactory<?>>)getFactoriesForDestinationType(
                    dataType.getCollectionElementType());

            if (set == null) {
                set = new HashSet<>();
            }

            for (final JavaToDataCellConverterFactory<?> factory : destinationTypes) {
                set.add(createToCollectionConverterFactory(factory));
            }
        }

        return (set == null) ? Collections.emptySet() : Collections.unmodifiableSet(set);
    }

    /**
     * Get the {@link DataCellToJavaConverterFactory} with the given identifier.
     *
     * @param id unique identifier for the factory
     * @return an optional converter factory
     */
    public Optional<JavaToDataCellConverterFactory<?>> getConverterFactory(final String id) {
        final JavaToDataCellConverterFactory<?> factory = m_byIdentifier.get(id);

        if (factory == null) {
            return Optional.empty();
        }

        return Optional.of(factory);
    }

    /**
     * Get a converterFactory which converts from <code>source</code> to <code>dest</code>
     *
     * @param source Source type to convert
     * @param dest {@link DataType} to convert to
     * @return {@link JavaToDataCellConverterFactory} which converts from <code>source</code> to <code>dest</code>
     * @param <S> A JavaToDataCellConverter type (letting java infer the type is highly recommended)
     */
    // we only put JavaToDataCellConverter<T> into the map for Class<T>
    @SuppressWarnings("unchecked")
    public <S> Optional<JavaToDataCellConverterFactory<S>> getConverterFactory(final Class<S> source,
        final DataType dest) {

        final LinkedBlockingQueue<Class<?>> classes = new LinkedBlockingQueue<>();
        classes.add(source);

        Class<?> curClass = null;
        JavaToDataCellConverterFactory<S> converterFactory = null;

        while ((curClass = classes.poll()) != null) {
            converterFactory =
                (JavaToDataCellConverterFactory<S>)m_converterFactories.get(new ConversionKey(curClass, dest));

            if (converterFactory != null) {
                return Optional.of(converterFactory);
            }

            /* check if a supertype has a compatible converter factory */
            classes.addAll(Arrays.asList(curClass.getInterfaces()));
            if (curClass.getSuperclass() != null) {
                classes.add(curClass.getSuperclass());
            }
        }

        if (dest.isCollectionType() && source.isArray()) {
            final Optional<?> elementFactory =
                getConverterFactory(source.getComponentType(), dest.getCollectionElementType());

            if (elementFactory.isPresent()) {
                return Optional
                    .of(createToCollectionConverterFactory((JavaToDataCellConverterFactory<?>)elementFactory.get()));
            }
        }

        return Optional.empty();
    }

    /*
     * Create a ArrayToCollectionConverterFactory. Separate function to mangle
     * generics.
     */
    private <T, E> ArrayToCollectionConverterFactory<T, E>
        createToCollectionConverterFactory(final JavaToDataCellConverterFactory<E> elementFactory) {
        return new ArrayToCollectionConverterFactory<T, E>(elementFactory);
    }

    /**
     * Register a DataCellToJavaConverterFactory
     *
     * @param factory The factory to register
     */
    public synchronized void register(final JavaToDataCellConverterFactory<?> factory) {
        if (factory == null) {
            throw new NullPointerException("'factory' cannot be null");
        }

        final ConversionKey key = new ConversionKey(factory);
        m_converterFactories.put(key, factory);

        final DataType destType = factory.getDestinationType();
        Set<JavaToDataCellConverterFactory<?>> byDestinationType = m_byDestinationType.get(destType);
        if (byDestinationType == null) {
            byDestinationType = new HashSet<>();
            m_byDestinationType.put(destType, byDestinationType);
        }
        byDestinationType.add(factory);

        final Class<?> sourceType = factory.getSourceType();
        Set<JavaToDataCellConverterFactory<?>> bySourceType = m_bySourceType.get(sourceType);
        if (bySourceType == null) {
            bySourceType = new HashSet<>();
            m_bySourceType.put(sourceType, bySourceType);
        }
        bySourceType.add(factory);

        final JavaToDataCellConverterFactory<?> previous = m_byIdentifier.put(factory.getIdentifier(), factory);
        if (previous != null) {
            LOGGER.coding("JavaToDataCellConverterFactory identifier is not unique (" + factory.getIdentifier() + ")");
        }
    }

    /* --- Singleton methods --- */

    private static final JavaToDataCellConverterRegistry INSTANCE = new JavaToDataCellConverterRegistry();

    /**
     * @return Singleton instance of this Service.
     */
    public static JavaToDataCellConverterRegistry getInstance() {
        return INSTANCE;
    }

    /* --- Annotation parsing and extension point --- */

    /**
     * The extension point ID.
     */
    private static final String EXTENSION_POINT_ID = "org.knime.core.JavaToDataCellConverter";

    private JavaToDataCellConverterRegistry() {
        /* parse all annotations of DataCellFactories */
        parseAnnotations();

        /* add converters from plugin providers */
        for (final IConfigurationElement configurationElement : Platform.getExtensionRegistry()
            .getConfigurationElementsFor(EXTENSION_POINT_ID)) {
            LOGGER.debug("Found ConverterProvider extension: " + configurationElement.getDeclaringExtension());
            try {
                // the specified class may not implement ConverterProvider, so
                // check this first
                final Object extension = configurationElement.createExecutableExtension("factoryClass");
                if (extension instanceof JavaToDataCellConverterFactory) {
                    final JavaToDataCellConverterFactory<?> converterFactory =
                        (JavaToDataCellConverterFactory<?>)extension;
                    register(converterFactory);

                } else {
                    // object was not an instance of ConverterProvider
                    LOGGER.error("Extension \"" + configurationElement.getDeclaringExtension()
                        + "\" is invalid: factory-class does not implement "
                        + JavaToDataCellConverterFactory.class.getName());
                }
            } catch (final CoreException e) {
                LOGGER.error("Error while loading extension \"" + configurationElement.getDeclaringExtension() + "\": "
                    + e.getMessage(), e);
            }
        }

        // register "Object -> StringCell" converter with lowest priority
        register(new SimpleJavaToDataCellConverterFactory<>(Object.class, StringCell.TYPE,
            (val) -> new StringCell(val.toString())));
    }

    private void parseAnnotations() {
        final Collection<DataType> availableDataTypes = DataTypeRegistry.getInstance().availableDataTypes();

        for (final DataType dataType : availableDataTypes) {
            final Optional<DataCellFactory> cellFactory = dataType.getCellFactory(null);
            if (cellFactory.isPresent()) {
                final Class<? extends DataCellFactory> cellFactoryClass = cellFactory.get().getClass();

                for (final Pair<Method, DataCellFactoryMethod> pair : ClassUtil
                    .getMethodsWithAnnotation(cellFactoryClass, DataCellFactoryMethod.class)) {
                    final Method method = pair.getFirst();
                    register(new FactoryMethodToDataCellConverterFactory<>(cellFactoryClass, method,
                        ClassUtil.ensureObjectType(method.getParameterTypes()[0]), dataType, pair.getSecond().name()));
                    LOGGER.debug("Registered DataCellToJavaConverterFactory from DataValueAccessMethod annotation for: "
                        + method.getParameterTypes()[0].getName() + " to " + dataType.getName());
                }
            }
        }
    }
}
