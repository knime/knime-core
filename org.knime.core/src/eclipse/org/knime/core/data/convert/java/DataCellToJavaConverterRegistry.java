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
 */

package org.knime.core.data.convert.java;

import java.lang.annotation.IncompleteAnnotationException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DataTypeRegistry;
import org.knime.core.data.DataValue;
import org.knime.core.data.MissingValue;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.convert.AbstractConverterFactoryRegistry;
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
public final class DataCellToJavaConverterRegistry extends
    AbstractConverterFactoryRegistry<Class<? extends DataValue>, Class<?>, DataCellToJavaConverterFactory<?, ?>, DataCellToJavaConverterRegistry> {

    /* Data types stored by their preferred value type, only for collection cells, for DataType we can directly use getPreferredValueClass */
    private final HashMap<Class<? extends DataValue>, DataType> m_preferredTypes = new HashMap<>();

    /**
     * Get all {@link DataType} that have at least one {@link DataValue} interface that is convertible by one of the
     * registered converters.
     *
     * @return Set of {@link DataType} that can be converted.
     * @since 3.7
     */
    public Set<DataType> getAllConvertibleDataTypes() {
        final Collection<DataType> availableDataTypes = DataTypeRegistry.getInstance().availableDataTypes();
        return availableDataTypes.stream() //
                .filter(dataType -> !getFactoriesForSourceType(dataType).isEmpty()) //
                .collect(Collectors.toSet());
    }

    /**
     * Get all {@link DataCellToJavaConverterFactory converter factories} which create Converters which convert into a
     * specific {@link Class destType}.
     * <P>
     * Please be aware that converters for {@link Class#isPrimitive() primitive} destination types usually do not
     * support missing values as input.
     *
     * @param destType DataType to query converter factories for
     * @return a {@link Collection} of all possible source types which can be converted into the given
     *         <code>destType</code>. The first is always the preferred type.
     */
    @Override
    public Collection<DataCellToJavaConverterFactory<?, ?>> getFactoriesForDestinationType(final Class<?> destType) {
        final LinkedHashSet<DataCellToJavaConverterFactory<?, ?>> set = new LinkedHashSet<>();

        ClassUtil.recursiveMapToClassHierarchy(destType, (type) -> {
            set.addAll(super.getFactoriesForDestinationType(type));
        });

        if (destType.isArray()) {
            // every single-type converter can also be used to convert
            // components of an array.
            final LinkedHashSet<DataCellToJavaConverterFactory<?, ?>> factories =
                (LinkedHashSet<DataCellToJavaConverterFactory<?, ?>>)getFactoriesForDestinationType(
                    destType.getComponentType());

            if (!factories.isEmpty() && m_preferredTypes.isEmpty()) {
                DataTypeRegistry.getInstance().availableDataTypes().stream()
                    .forEach((dataType) -> m_preferredTypes.put(dataType.getPreferredValueClass(), dataType));

                m_preferredTypes.put(MissingValue.class, DataType.getMissingCell().getType());
            }

            for (final DataCellToJavaConverterFactory<?, ?> cls : factories) {
                final Class<?> sourceType = cls.getSourceType();

                // sourceType could be a DataCell subclass!
                final DataType elementType = (DataCell.class.isAssignableFrom(sourceType))
                    ? DataType.getType((Class<? extends DataCell>)sourceType) : m_preferredTypes.get(sourceType);
                if (elementType == null) {
                    LOGGER.error("Factory " + cls.getIdentifier() + " source type " + cls.getSourceType().getName()
                        + " has no preferred type.");
                }

                set.addAll(getCollectionConverterFactories(ListCell.getCollectionType(elementType),
                    ClassUtil.getArrayType(cls.getDestinationType())));
            }
        }

        return set;
    }

    /**
     * Get all {@link DataCellToJavaConverterFactory converter factories} which create converter which convert into a
     * specific {@link Class destType}.
     * <P>
     * Please be aware that converters of {@link Class#isPrimitive() primitive}
     * {@link DataCellToJavaConverterFactory#getDestinationType() destination types} usually do not support missing
     * values as input.
     *
     * @param sourceType DataType to query converter factories for
     * @return a {@link Collection} of all possible source types which can be converted into the given
     *         <code>destType</code>. The first is always the preferred type.
     */
    public Collection<DataCellToJavaConverterFactory<?, ?>> getFactoriesForSourceType(final DataType sourceType) {
        final LinkedHashSet<DataCellToJavaConverterFactory<?, ?>> set = new LinkedHashSet<>();

        ClassUtil.recursiveMapToDataTypeClasses(sourceType, (type) -> {
            set.addAll(super.getFactoriesForSourceType((Class<? extends DataValue>)type));
        });

        if (sourceType.isCollectionType()) {
            // every single-type converter can also be used to convert
            // components of an array.
            final LinkedHashSet<DataCellToJavaConverterFactory<?, ?>> factories =
                (LinkedHashSet<DataCellToJavaConverterFactory<?, ?>>)getFactoriesForSourceType(
                    sourceType.getCollectionElementType());

            for (final DataCellToJavaConverterFactory<?, ?> factory : factories) {
                // We do not use getCollectionConverterFactories here, because that will recursively check
                // the class hierarchy. This was already done in getFactoriesForSourceType, though.
                set.add(new CollectionConverterFactory<>(factory));
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
    @Override
    public Optional<DataCellToJavaConverterFactory<?, ?>> getFactory(final String id) {
        if (id == null) {
            return Optional.empty();
        }
        if (id.startsWith(CollectionConverterFactory.class.getName())) {
            // get the element converter factory id:
            final String elemConvFactoryId =
                id.substring(CollectionConverterFactory.class.getName().length() + 1, id.length() - 1);
            Optional<DataCellToJavaConverterFactory<?, ?>> factory = getFactory(elemConvFactoryId);
            if (factory.isPresent()) {
                return Optional.of(new CollectionConverterFactory<>(factory.get()));
            } else {
                return Optional.empty();
            }
        }

        return super.getFactory(id);
    }

    /** @deprecated Method has been renamed to {@link #getFactory(String)} */
    @SuppressWarnings("javadoc")
    @Deprecated
    public Optional<DataCellToJavaConverterFactory<?, ?>> getConverterFactory(final String id) {
        return getFactory(id);
    }

    /**
     * Get all {@link DataCellToJavaConverterFactory} which create {@link DataCellToJavaConverter}s that convert
     * <code>sourceType</code> into <code>destType</code>. If you do not require more than one converter factory, you
     * should consider using {@link #getPreferredConverterFactory(DataType, Class)} instead.
     * <P>
     * Please be aware that converters for {@link Class#isPrimitive() primitive} destination types usually do not
     * support missing values as input.
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

        final Collection<DataCellToJavaConverterFactory<? extends DataValue, D>> allFactories = new ArrayList<>();

        for (final Class<? extends DataValue> curClass : sourceType.getValueClasses()) {
            if (DataValue.class.equals(curClass)) {
                // We need to defer DataValue.class to the very end, because these converters are usually very general and low
                // priority (e.g. DataValue.toString() converter)
                continue;
            }

            allFactories.addAll(
                (Collection<? extends DataCellToJavaConverterFactory<? extends DataValue, D>>)getFactories(curClass,
                    destType));
        }

        allFactories.addAll((Collection<? extends DataCellToJavaConverterFactory<DataCell, D>>)getFactories(DataValue.class, destType));

        if (sourceType.isCollectionType() && destType.isArray()) {
            allFactories.addAll(getCollectionConverterFactories(sourceType, destType));
        }

        return allFactories;
    }

    /**
     * Convenience method to get the first {@link DataCellToJavaConverterFactory} returned by
     * {@link #getConverterFactories(DataType, Class)}, which creates a converter that is able to convert the preferred
     * data value of the given sourceType into the given destType. Since {@link DataCell DataCells} of a certain
     * {@link DataType} are required to implement the preferred {@link DataValue} interface, the resulting converter is
     * therefore guaranteed to be able to convert {@link DataCell DataCells} of the requested type.
     * <P>
     * Please be aware that converters for {@link Class#isPrimitive() primitive} destination types usually do not
     * support missing values as input.
     *
     * @param sourceType type which should be convertible
     * @param destType type to which should be converted
     * @return the preferred {@link DataCellToJavaConverterFactory} for given <code>sourceType</code> and
     *         <code>destType</code>.
     */
    public <D> Optional<DataCellToJavaConverterFactory<? extends DataValue, D>>
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
     * Get {@link CollectionConverterFactory CollectionConverterFactories} for converters from collection
     * <code>sourceType</code> into array <code>destType</code>.
     *
     * <P>
     * Please be aware that converters for {@link Class#isPrimitive() primitive} array destination types usually do not
     * support missing values as input.
     *
     * @param destType Type the created converters convert from
     * @param sourceType Type the created converters convert to
     * @return the {@link DataCellToJavaConverterFactory} or <code>null</code> if none matched the given types
     */
    private <D, SE extends DataValue, DE> Collection<DataCellToJavaConverterFactory<CollectionDataValue, D>>
        getCollectionConverterFactories(final DataType sourceType, final Class<D> destType) {

        if (!sourceType.isCollectionType() || !destType.isArray()) {
            return Collections.emptySet();
        }

        final ArrayList<DataCellToJavaConverterFactory<CollectionDataValue, D>> allFactories = new ArrayList<>();

        // try creating a dynamic CollectionConverterFactory
        for (final DataCellToJavaConverterFactory<? extends DataValue, ?> factory : getConverterFactories(
            sourceType.getCollectionElementType(), destType.getComponentType())) {
            allFactories.add(new CollectionConverterFactory<>(factory));
        }

        return allFactories;
    }

    /**
     * Get a converter factory which converts the elements of a CollectionCell to a Java array by executing a converter
     * factory per element.
     *
     * @param elementFactory converter factory to use to convert elements
     * @return the converter factory which converts a collection of the input type of elementFactory to an array of the
     *         output type of elementFactory.
     * @param <DE> Destination element type
     * @param <D> Destination array type
     * @since 3.4
     */
    public <DE, D> DataCellToJavaConverterFactory<CollectionDataValue, D>
        getCollectionConverterFactory(final DataCellToJavaConverterFactory<? extends DataValue, DE> elementFactory) {
        return new CollectionConverterFactory<>(elementFactory);
    }

    /**
     * Get a preferred java type a given {@link DataType} can be converted to.
     *
     * Note that if more than one java type can be converted from the {@link DataType#getPreferredValueClass()}, this
     * method will not return the same type every time.
     *
     * @param type Data type to get a preferred Java type for
     * @return A class that is a preferred type to convert the given DataType to.
     * @since 3.6
     */
    public Optional<Class<?>> getPreferredJavaTypeForCell(final DataType type) {
        final Class<? extends DataValue> preferredValueClass = type.getPreferredValueClass();
        final Set<DataCellToJavaConverterFactory<?, ?>> factories = m_bySourceType.get(preferredValueClass);

        final Optional<DataCellToJavaConverterFactory<?, ?>> firstFactory = factories.stream().findFirst();
        if (firstFactory.isPresent()) {
            return Optional.of(firstFactory.get().getDestinationType());
        }

        return Optional.empty();
    }

    /* -- Singleton methods -- */

    private final static NodeLogger LOGGER = NodeLogger.getLogger(DataCellToJavaConverterRegistry.class);

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
            try {
                // the specified class may not implement ConverterProvider, so
                // check this first
                final Object extension = configurationElement.createExecutableExtension("factoryClass");
                if (extension instanceof DataCellToJavaConverterFactory<?, ?> factory) {

                    // Check name of factory
                    if (!validateFactoryName(factory)) {
                        LOGGER.coding(
                            "Factory name \"" + factory.getName() + "\" of factory with id \"" + factory.getIdentifier()
                                + "\" does not follow naming convention (see DataValueAccessMethod#name()).");
                        LOGGER.coding("Factory will not be registered.");
                        continue;
                    }
                    register(factory);
                    LOGGER.debugWithFormat("Found ConverterProvider extension: %s", factory.getIdentifier());
                } else {
                    // object was not an instance of ConverterProvider
                    LOGGER.error("Extension \"" + configurationElement.getDeclaringExtension()
                        + "\" is invalid: factory-class does not implement "
                        + DataCellToJavaConverterFactory.class.getName());
                }
            } catch (final Throwable e) {
                LOGGER.error("Error while loading extension \"" + configurationElement.getDeclaringExtension() + "\": "
                    + e.getMessage(), e);
            }
        }

        // register "Object -> String" and "MissingValue -> null" converters
        // with lowest priority
        register(new SimpleDataCellToJavaConverterFactory<>(DataValue.class, String.class, (val) -> val.toString(),
            "String (toString())"));
        register(new SimpleDataCellToJavaConverterFactory<>(MissingValue.class, Object.class, (val) -> null,
            "Object (Null)"));
    }

    /*
     * Parse @DataCellFactoryMethod and @DataValueAccessMethod annotations
     */
    private void parseAnnotations() {
        final Collection<DataType> availableDataTypes = DataTypeRegistry.getInstance().availableDataTypes();

        final Set<Class<? extends DataValue>> processedValueClasses = new HashSet<>();
        for (final DataType dataType : availableDataTypes) {
            for (final Class<? extends DataValue> valueClass : dataType.getValueClasses()) {
                if (processedValueClasses.contains(valueClass)) {
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

                processedValueClasses.add(valueClass);
            }
        }
    }

    private <T> void parseAnnotation(final Class<? extends DataValue> valueClass, final Method method,
        final DataValueAccessMethod annotation) {

        try {
            final Class<T> javaType = (Class<T>)ClassUtil.ensureObjectType(method.getReturnType());
            final String name = annotation.name();
            final DataCellToJavaConverterFactory<?, ?> factory = new SimpleDataCellToJavaConverterFactory<>(valueClass,
                javaType, (value) -> (T)method.invoke(value), name);

            // Check name of factory
            if (!validateFactoryName(factory)) {
                LOGGER.coding(
                    "DataCellToJavaFactory name \"" + name + "\" of factory with id \"" + factory.getIdentifier()
                        + "\" does not follow naming convention (see DataValueAccessMethod#name()).");
                LOGGER.coding("Factory will not be registered.");
                return;
            }
            register(factory);
            LOGGER.debug("Registered DataCellToJavaConverterFactory from DataValueAccessMethod annotation for "
                + valueClass.getName() + " to " + javaType.getName());
        } catch (IncompleteAnnotationException e) {
            LOGGER.coding(
                "Incomplete Annotation for " + valueClass.getName() + "." + method.getName() + ". Will not register.",
                e);
        }
    }

    /**
     * Check whether the given factory name matches the naming convention described in
     * {@link DataValueAccessMethod#name()}
     *
     * @param name the name of the
     * @return
     */
    private static boolean validateFactoryName(final DataCellToJavaConverterFactory<?, ?> factory) {
        final String name = factory.getName();
        final String className = factory.getDestinationType().getSimpleName();
        return name.matches(Pattern.quote(className) + "(| \\(.+\\))");
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
