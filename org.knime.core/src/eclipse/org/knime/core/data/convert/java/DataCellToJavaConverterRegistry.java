/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 */

package org.knime.core.data.convert.java;

import java.lang.annotation.IncompleteAnnotationException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DataTypeRegistry;
import org.knime.core.data.DataValue;
import org.knime.core.data.MissingValue;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.convert.ConversionKey;
import org.knime.core.data.convert.DataValueAccessMethod;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterRegistry;
import org.knime.core.data.convert.util.ClassUtil;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.Pair;

/**
 * Service which contains all registered {@link DataCellToJavaConverterFactory DataCellToJavaConverterFactories},
 * therefore {@link DataCellToJavaConverterFactory}s for KNIME types to Java type conversions.
 *
 * <p>
 * This class is a singleton, use {@link #getInstance()} to retrieve an instance.
 * </p>
 *
 * <b>Examples:</b>
 * <p>
 * Checking if a type can be converted into another type:
 *
 * <pre>
 * DataType mySourceType = ...;
 * Class&lt;?> myDestType = ...;
 *
 * boolean canConvert = !DataCellToJavaConverterRegistry.getInstance()
 *                      .getConverterFactories(mySourceType, myDestType)
 *                      .isEmpty();
 * </pre>
 * </p>
 * <p>
 * Get all Classes which a certain DataType can be converted to:
 *
 * <pre>
 * Class&lt;?> mySourceType = ...;
 *
 * Set&lt;Class&lt;?>> possibleTypes = DataCellToJavaConverterRegistry.getInstance()
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
 * DataCell myCell = ...;
 * Class&lt;?> myDestType = Integer.class;
 * String factoryName = ""; // "" is default
 *
 * Optional&lt;DataCellToJavaConverterFactory&lt;DataValue, T>> factory =
 *                      DataCellToJavaConverterRegistry.getInstance()
 *                      .getPreferredConverterFactory(myCell.getType(), myDestType);
 * if (!factory.isPresent()) {
 *      // error!
 * }
 *
 * // Instantiate the converter
 * DataCellToJavaConverter&ltDataValue, T> converter = factory.get().create();
 * // Convert the value
 * T convertedValue = converter.convert(myCell);
 *
 * </pre>
 * </p>
 *
 * @author Jonathan Hale
 * @since 3.2
 * @see org.knime.core.data.convert
 * @see JavaToDataCellConverterRegistry
 */
public final class DataCellToJavaConverterRegistry {

    /* converter factories stored by SourceType/DestType/FactoryName triple */
    private final HashMap<ConversionKey, ArrayList<DataCellToJavaConverterFactory<?, ?>>> m_converterFactories =
        new HashMap<>();

    /* converter factories stored by destination type */
    private final HashMap<Class<?>, Set<DataCellToJavaConverterFactory<?, ?>>> m_byDestinationType = new HashMap<>();

    /* converter factories stored by source type */
    private final HashMap<Class<?>, Set<DataCellToJavaConverterFactory<?, ?>>> m_bySourceType = new HashMap<>();

    /* converter factories stored by source type */
    private final HashMap<String, DataCellToJavaConverterFactory<?, ?>> m_byIdentifier = new HashMap<>();

    /* data types stored by their preferred value type */
    private final HashMap<Class<? extends DataValue>, DataType> m_prefferedTypes = new HashMap<>();

    /**
     * Query which {@link DataValue} {@link Class Classes} can be converted.
     *
     * @return a {@link Collection} of all possible source types
     */
    public Collection<Class<?>> getAllSourceTypes() {
        return m_converterFactories.values().stream().flatMap(c -> c.stream()).map((factory) -> factory.getSourceType())
            .collect(Collectors.toSet());
    }

    /**
     * Query into which {@link Class Classes} can be converted.
     *
     * @return a {@link Collection} of all possible source types
     */
    public Collection<Class<?>> getAllDestinationTypes() {
        return m_converterFactories.values().stream().flatMap(c -> c.stream())
            .map((factory) -> factory.getDestinationType()).collect(Collectors.toSet());
    }

    /**
     * Get all {@link DataCellToJavaConverterFactory converter factories} which create Converters which convert into a
     * specific {@link Class destType}.
     *
     * @param destType DataType to query converter factories for
     * @return a {@link Collection} of all possible source types which can be converted into the given
     *         <code>destType</code>. The first is always the preferred type.
     */
    public <T> Collection<DataCellToJavaConverterFactory<?, ?>>
        getFactoriesForDestinationType(final Class<T> destType) {
        final LinkedHashSet<DataCellToJavaConverterFactory<?, ?>> set = new LinkedHashSet<>();

        ClassUtil.recursiveMapToClassHierarchy(destType, (type) -> {
            final Set<DataCellToJavaConverterFactory<?, ?>> factories = m_byDestinationType.get(type);
            if (factories != null) {
                set.addAll(factories);
            }
        });

        if (destType.isArray()) {
            // every single-type converter can also be used to convert
            // components of an array.
            final LinkedHashSet<DataCellToJavaConverterFactory<?, ?>> factories =
                (LinkedHashSet<DataCellToJavaConverterFactory<?, ?>>)getFactoriesForDestinationType(
                    destType.getComponentType());

            if (!factories.isEmpty() && m_prefferedTypes.isEmpty()) {
                DataTypeRegistry.getInstance().availableDataTypes().stream()
                    .forEach((dataType) -> m_prefferedTypes.put(dataType.getPreferredValueClass(), dataType));

                m_prefferedTypes.put(MissingValue.class, DataType.getMissingCell().getType());
            }

            for (final DataCellToJavaConverterFactory<?, ?> cls : factories) {
                final Class<?> sourceType = cls.getSourceType();

                // sourceType could be a DataCell subclass!
                final DataType elementType = (DataCell.class.isAssignableFrom(sourceType))
                    ? DataType.getType((Class<? extends DataCell>)sourceType) : m_prefferedTypes.get(sourceType);
                if (elementType == null) {
                    LOGGER.error("Factory " + cls.getIdentifier() + " source type " + cls.getSourceType().getName()
                        + " has no preffered type.");
                }

                set.addAll(getCollectionConverterFactory(ListCell.getCollectionType(elementType),
                    ClassUtil.getArrayType(cls.getDestinationType())));
            }
        }

        return set;
    }

    /**
     * Get all {@link DataCellToJavaConverterFactory converter factories} which create converter which convert into a
     * specific {@link Class destType}.
     *
     * @param sourceType DataType to query converter factories for
     * @return a {@link Collection} of all possible source types which can be converted into the given
     *         <code>destType</code>. The first is always the preferred type.
     */
    public <T> Collection<DataCellToJavaConverterFactory<?, ?>> getFactoriesForSourceType(final DataType sourceType) {
        final LinkedHashSet<DataCellToJavaConverterFactory<?, ?>> set = new LinkedHashSet<>();

        ClassUtil.recursiveMapToDataTypeClasses(sourceType, (type) -> {
            final Set<DataCellToJavaConverterFactory<?, ?>> types = m_bySourceType.get(type);
            if (types != null) {
                set.addAll(types);
            }
        });

        if (sourceType.isCollectionType()) {
            // every single-type converter can also be used to convert
            // components of an array.
            final LinkedHashSet<DataCellToJavaConverterFactory<?, ?>> factories =
                (LinkedHashSet<DataCellToJavaConverterFactory<?, ?>>)getFactoriesForSourceType(
                    sourceType.getCollectionElementType());

            for (final DataCellToJavaConverterFactory<?, ?> factory : factories) {
                set.addAll(
                    getCollectionConverterFactory(sourceType, ClassUtil.getArrayType(factory.getDestinationType())));
            }
        }

        return set;
    }

    /**
     * Get the {@link DataCellToJavaConverterFactory} with the given identifier.
     *
     * @param id unique identifier for the factory
     * @return an optional of a converter factory or empty if no converter factory with given id could be found
     */
    public Optional<DataCellToJavaConverterFactory<?, ?>> getConverterFactory(final String id) {
        if (id.startsWith(CollectionConverterFactory.class.getName())) {
            // get the element converter factory id:
            final String elemConvFactoryId = id.substring(CollectionConverterFactory.class.getName().length() + 1,
                id.length() - 1);
            Optional<DataCellToJavaConverterFactory<?, ?>> factory = getConverterFactory(elemConvFactoryId);
            if (factory.isPresent()) {
                return Optional.of(new CollectionConverterFactory<>(factory.get()));
            } else {
                return Optional.empty();
            }
        }
        final DataCellToJavaConverterFactory<?, ?> factory = m_byIdentifier.get(id);

        if (factory == null) {
            return Optional.empty();
        }

        return Optional.of(factory);
    }

    /**
     * Get all {@link DataCellToJavaConverterFactory} which create {@link DataCellToJavaConverter}s that convert
     * <code>sourceType</code> into <code>destType</code>. If you do not require more than one converter factory, you
     * should consider using {@link #getPreferredConverterFactory(DataType, Class)} instead.
     *
     * @param sourceType Type the created {@link DataCellToJavaConverter}s convert from
     * @param destType Type the created {@link DataCellToJavaConverter}s convert to
     * @return collection of {@link DataCellToJavaConverterFactory converter factories} which create converters which
     *         convert from <code>sourceType</code> into <code>destType</code>.
     */
    public <D> Collection<DataCellToJavaConverterFactory<? extends DataValue, D>>
        getConverterFactories(final DataType sourceType, final Class<D> destType) {
        if (sourceType.equals(DataType.getMissingCell().getType())) {
            return Arrays.asList(MissingToNullConverterFactory.getInstance());
        }

        final LinkedBlockingQueue<Class<?>> classes = new LinkedBlockingQueue<>();
        classes.add(sourceType.getPreferredValueClass());
        classes.addAll(sourceType.getValueClasses());
        classes.add(sourceType.getCellClass());

        final Collection<DataCellToJavaConverterFactory<? extends DataValue, D>> allFactories = new ArrayList<>();

        while (!classes.isEmpty()) {
            final Class<?> curClass = classes.poll();
            // this conversion is fine, since we guarantee the correct type when
            // inserting into the map.
            final ArrayList<DataCellToJavaConverterFactory<?, ?>> factories =
                m_converterFactories.get(new ConversionKey(curClass, destType));

            if (factories != null) {
                allFactories.addAll((Collection<? extends DataCellToJavaConverterFactory<DataCell, D>>)factories);
            }

            /* check if a supertype has a compatible converter factory */
            classes.addAll(Arrays.asList(curClass.getInterfaces()));
            if (curClass.getSuperclass() != null) {
                classes.add(curClass.getSuperclass());
            }
        }

        if (sourceType.isCollectionType() && destType.isArray()) {
            allFactories.addAll(getCollectionConverterFactory(sourceType, destType));
        }

        return allFactories;
    }

    /**
     * Convenience method to get the first {@link DataCellToJavaConverterFactory} returned by
     * {@link #getConverterFactories(DataType, Class)}, which creates a converter that is able to convert the preferred
     * data value of the given sourceType into the given destType. Since {@link DataCell DataCells} of a certain
     * {@link DataType} are required to implement the preferred {@link DataValue} interface, the resulting converter is
     * therefore guaranteed to be able to convert {@link DataCell DataCells} of the requested type.
     *
     * @param sourceType type which should be convertible
     * @param destType type to which should be converted
     * @return the preferred {@link DataCellToJavaConverterFactory} for given <code>sourceType</code> and
     *         <code>destType</code>.
     */
    public <D> Optional<DataCellToJavaConverterFactory<DataValue, D>>
        getPreferredConverterFactory(final DataType sourceType, final Class<D> destType) {

        // get the optional, we need to cast its contents rather than the Optional directly.
        final Optional<DataCellToJavaConverterFactory<? extends DataValue, D>> first =
            getConverterFactories(sourceType, destType).stream().findFirst();

        if (first.isPresent()) {
            return Optional.of((DataCellToJavaConverterFactory<DataValue, D>)first.get());
        }

        return Optional.empty();
    }

    /**
     * Get a {@link DataCellToJavaConverterFactory} for converters from collection <code>sourceType</code> into array
     * <code>destType</code>.
     *
     * @param destType Type the created converters convert from
     * @param sourceType Type the created converters convert to
     * @return the {@link DataCellToJavaConverterFactory} or <code>null</code> if none matched the given types
     */
    private <D, SE extends DataValue, DE> Collection<DataCellToJavaConverterFactory<CollectionDataValue, D>>
        getCollectionConverterFactory(final DataType sourceType, final Class<D> destType) {

        final ArrayList<DataCellToJavaConverterFactory<CollectionDataValue, D>> allFactories = new ArrayList<>();

        // try creating a dynamic CollectionConverterFactory
        for (final DataCellToJavaConverterFactory<? extends DataValue, ?> factory : getConverterFactories(
            sourceType.getCollectionElementType(), destType.getComponentType())) {
            allFactories.add(new CollectionConverterFactory<D, SE, DE>((DataCellToJavaConverterFactory<SE, DE>)factory));
        }

        return allFactories;
    }

    /**
     * Register a DataCellToJavaConverterFactory.
     *
     * @param factory the factory to register
     */
    public void register(final DataCellToJavaConverterFactory<?, ?> factory) {
        if (factory == null) {
            throw new IllegalArgumentException("factory must not be null");
        }

        final ConversionKey key = new ConversionKey(factory);
        ArrayList<DataCellToJavaConverterFactory<?, ?>> list = m_converterFactories.get(key);
        if (list == null) {
            list = new ArrayList<>();
            m_converterFactories.put(key, list);
        }
        list.add(factory);

        final Class<?> destType = factory.getDestinationType();
        Set<DataCellToJavaConverterFactory<?, ?>> byDestType = m_byDestinationType.get(destType);
        if (byDestType == null) {
            byDestType = new HashSet<>();
            m_byDestinationType.put(destType, byDestType);
        }
        byDestType.add(factory);

        final Class<?> sourceType = factory.getSourceType();
        Set<DataCellToJavaConverterFactory<?, ?>> bySourceType = m_bySourceType.get(sourceType);
        if (bySourceType == null) {
            bySourceType = new HashSet<>();
            m_bySourceType.put(sourceType, bySourceType);
        }
        bySourceType.add(factory);

        final DataCellToJavaConverterFactory<?, ?> previous = m_byIdentifier.put(factory.getIdentifier(), factory);
        if (previous != null) {
            LOGGER.coding("DataCellToJavaConverterFactory identifier is not unique (" + factory.getIdentifier() + ")");
        }
    }

    /* -- Singleton methods -- */

    private final static NodeLogger LOGGER = NodeLogger.getLogger(JavaToDataCellConverterRegistry.class);

    private static final DataCellToJavaConverterRegistry INSTANCE = new DataCellToJavaConverterRegistry();

    /**
     * The extension point ID.
     */
    public static final String EXTENSION_POINT_ID = "org.knime.core.DataCellToJavaConverter";

    private DataCellToJavaConverterRegistry() {
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
                if (extension instanceof DataCellToJavaConverterFactory) {
                    final DataCellToJavaConverterFactory<?, ?> converterFactory =
                        (DataCellToJavaConverterFactory<?, ?>)extension;
                    register(converterFactory);
                } else {
                    // object was not an instance of ConverterProvider
                    LOGGER.error("Extension \"" + configurationElement.getDeclaringExtension()
                        + "\" is invalid: factory-class does not implement "
                        + DataCellToJavaConverterFactory.class.getName());
                }
            } catch (final CoreException e) {
                LOGGER.error("Error while loading extension \"" + configurationElement.getDeclaringExtension() + "\": "
                    + e.getMessage(), e);
            }
        }

        // register "Object -> String" and "MissingValue -> null" converters
        // with lowest priority
        register(new SimpleDataCellToJavaConverterFactory<>(DataValue.class, String.class, (val) -> val.toString()));
        register(new SimpleDataCellToJavaConverterFactory<>(MissingValue.class, Object.class, (val) -> null));
    }

    /*
     * Parse @DataCellFactoryMethod and @DataValueAccessMethod annotations
     */
    private void parseAnnotations() {
        final Collection<DataType> availableDataTypes = DataTypeRegistry.getInstance().availableDataTypes();

        for (final DataType dataType : availableDataTypes) {
            for (final Class<? extends DataValue> valueClass : dataType.getValueClasses()) {
                if (m_bySourceType.containsKey(valueClass)) {
                    // already parsed this value class
                    continue;
                }

                // get methods annotated with DataValueAccessMethod
                final Collection<Pair<Method, DataValueAccessMethod>> methodsWithAnnotation =
                    ClassUtil.getMethodsWithAnnotation(valueClass, DataValueAccessMethod.class);

                // register a converter for every DataValueAccessMethod annotation
                for (final Pair<Method, DataValueAccessMethod> pair : methodsWithAnnotation) {
                    parseAnnotation(valueClass, pair.getFirst(), pair.getSecond());
                }
            }
        }
    }

    private <T> void parseAnnotation(final Class<? extends DataValue> valueClass, final Method method,
        final DataValueAccessMethod annotation) {

        try {
            final Class<T> javaType = (Class<T>)ClassUtil.ensureObjectType(method.getReturnType());
            final String name = annotation.name();

            register(new SimpleDataCellToJavaConverterFactory<>(valueClass, javaType,
                (value) -> (T)method.invoke(value), name));
            LOGGER.debug("Registered DataCellToJavaConverterFactory from DataValueAccessMethod annotation for "
                + javaType.getName() + " to " + valueClass.getName());
        } catch (IncompleteAnnotationException e) {
            LOGGER.coding(
                "Incomplete Annotation for " + valueClass.getName() + "." + method.getName() + ". Will not register.",
                e);
        }
    }

    /**
     * Returns the singleton instance.
     *
     * @return the singleton instance
     */
    public static DataCellToJavaConverterRegistry getInstance() {
        return INSTANCE;
    }
}
