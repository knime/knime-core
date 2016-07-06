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

package org.knime.core.data.convert.datacell;

import java.lang.annotation.IncompleteAnnotationException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
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
import org.knime.core.data.convert.java.DataCellToJavaConverterRegistry;
import org.knime.core.data.convert.util.ClassUtil;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.Pair;

/**
 * Service containing all registered {@link JavaToDataCellConverterFactory JavaToDataCellConverterFactories}.
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
 *
 * boolean canConvert = !JavaToDataCellConverterRegistry.getInstance()
 *                      .getConverterFactories(mySourceType, myDestType)
 *                      .isEmpty();
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
 *                      .getPreferredConverterFactory(myObject.getClass(), myDestType);
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
 * @see org.knime.core.data.convert
 * @see DataCellToJavaConverterRegistry
 */
public final class JavaToDataCellConverterRegistry {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(JavaToDataCellConverterRegistry.class);

    /* factories stored by their key */
    private final HashMap<ConversionKey, ArrayList<JavaToDataCellConverterFactory<?>>> m_converterFactories =
        new HashMap<>();

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
        return m_converterFactories.values().stream().flatMap(c -> c.stream()).map((factory) -> factory.getSourceType())
            .collect(Collectors.toSet());
    }

    /**
     * Query into which DataTypes can be converted.
     *
     * @return a {@link Collection} of all possible data types into which can be converted.
     */
    public Collection<DataType> getAllDestinationTypes() {
        return m_converterFactories.values().stream().flatMap(c -> c.stream())
            .map((factory) -> factory.getDestinationType()).collect(Collectors.toSet());
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
        if (id.startsWith(ArrayToCollectionConverterFactory.class.getName())) {
            // get the element converter factory id:
            final String elemConvFactoryId = id.substring(ArrayToCollectionConverterFactory.class.getName().length() + 1,
                id.length()-1);
            Optional<JavaToDataCellConverterFactory<?>> factory = getConverterFactory(elemConvFactoryId);
            if (factory.isPresent()) {
                return Optional.of(new ArrayToCollectionConverterFactory<>(factory.get()));
            } else {
                return Optional.empty();
            }
        }
        final JavaToDataCellConverterFactory<?> factory = m_byIdentifier.get(id);

        if (factory == null) {
            return Optional.empty();
        }

        return Optional.of(factory);
    }

    /**
     * Get all {@link JavaToDataCellConverterFactory converter factories} which create {@link JavaToDataCellConverter}s
     * that convert <code>sourceType</code> into <code>destType</code>. If you do not require more than one converter
     * factory, you should consider using {@link #getPreferredConverterFactory(Class, DataType)} instead.
     *
     * @param sourceType Source type to convert
     * @param destType {@link DataType} to convert to
     * @return collection of {@link JavaToDataCellConverterFactory converter factories} which create converters which
     *         convert from <code>sourceType</code> to <code>destType</code>
     * @param <S> A JavaToDataCellConverter type (letting java infer the type is highly recommended)
     */
    // we only put JavaToDataCellConverter<T> into the map for Class<T>
    public <S> Collection<JavaToDataCellConverterFactory<S>> getConverterFactories(final Class<S> sourceType,
        final DataType destType) {

        final LinkedBlockingQueue<Class<?>> classes = new LinkedBlockingQueue<>();
        classes.add(sourceType);

        Class<?> curClass = null;
        final ArrayList<JavaToDataCellConverterFactory<S>> factories = new ArrayList<>();

        while ((curClass = classes.poll()) != null) {
            final ArrayList<JavaToDataCellConverterFactory<?>> newFactories =
                m_converterFactories.get(new ConversionKey(curClass, destType));

            if (newFactories != null) {
                factories.addAll((Collection<? extends JavaToDataCellConverterFactory<S>>)newFactories);
            }

            /* check if a supertype has a compatible converter factory */
            classes.addAll(Arrays.asList(curClass.getInterfaces()));
            if (curClass.getSuperclass() != null) {
                classes.add(curClass.getSuperclass());
            }
        }

        if (destType.isCollectionType() && sourceType.isArray()) {
            final Collection<? extends JavaToDataCellConverterFactory<S>> elementFactories =
                (Collection<? extends JavaToDataCellConverterFactory<S>>)getConverterFactories(
                    sourceType.getComponentType(), destType.getCollectionElementType());

            final List<?> arrayFactories =
                elementFactories.stream().map((elementFactory) -> createToCollectionConverterFactory(
                    (JavaToDataCellConverterFactory<?>)elementFactory)).collect(Collectors.toList());
            factories.addAll((Collection<? extends JavaToDataCellConverterFactory<S>>)arrayFactories);
        }

        return factories;
    }

    /**
     * Convenience method to get the first {@link JavaToDataCellConverterFactory} returned by
     * {@link #getConverterFactories(Class, DataType)}.
     *
     * @param source type which should be convertible
     * @param dest type to which should be converted
     * @return the preferred {@link JavaToDataCellConverterFactory} for given <code>sourceType</code> and
     *         <code>destType</code>.
     */
    public <S> Optional<JavaToDataCellConverterFactory<S>> getPreferredConverterFactory(final Class<S> source,
        final DataType dest) {
        return getConverterFactories(source, dest).stream().findFirst();
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
     * Register a DataCellToJavaConverterFactory.
     *
     * @param factory The factory to register
     */
    public synchronized void register(final JavaToDataCellConverterFactory<?> factory) {
        if (factory == null) {
            throw new IllegalArgumentException("factory must not be null");
        }

        final ConversionKey key = new ConversionKey(factory);
        ArrayList<JavaToDataCellConverterFactory<?>> list = m_converterFactories.get(key);
        if (list == null) {
            list = new ArrayList<>();
            m_converterFactories.put(key, list);
        }
        list.add(factory);

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
    public static final String EXTENSION_POINT_ID = "org.knime.core.JavaToDataCellConverter";

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
                    try {
                        register(new FactoryMethodToDataCellConverterFactory<>(cellFactoryClass, method,
                            ClassUtil.ensureObjectType(method.getParameterTypes()[0]), dataType,
                            pair.getSecond().name()));
                        LOGGER.debug(
                            "Registered DataCellToJavaConverterFactorz from DataValueAccessMethod annotation for: "
                                + method.getParameterTypes()[0].getName() + " to " + dataType.getName());
                    } catch (IncompleteAnnotationException e) {
                        LOGGER.coding("Incomplete DataCellFactoryMethod annotation for " + cellFactoryClass.getName()
                            + "." + method.getName() + ". Will not register.", e);
                    } catch (NoSuchMethodException | SecurityException ex) {
                        LOGGER
                            .error(
                                "Could not access default constructor or constructor with parameter 'ExecutionContext' of "
                                    + "class '" + cellFactoryClass.getName() + "'. Converter will not be registered.",
                                ex);
                    }
                }
            }
        }
    }

    /**
     * @return All registered converter factories
     */
    public Collection<JavaToDataCellConverterFactory<?>> getAllFactories() {
        return m_converterFactories.values().stream().flatMap(c -> c.stream()).collect(Collectors.toList());
    }
}
