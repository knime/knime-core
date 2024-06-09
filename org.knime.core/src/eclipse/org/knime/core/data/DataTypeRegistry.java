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
 *   19.08.2015 (thor): created
 */
package org.knime.core.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.value.DefaultRowKeyValueFactory;
import org.knime.core.data.v2.value.VoidRowKeyFactory;
import org.knime.core.internal.SerializerMethodLoader;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.EclipseUtil;

/**
 * A registry for all {@link DataType}s that are registered via the extension point <tt>org.knime.core.DataType</tt>.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 * @since 3.0
 */
public final class DataTypeRegistry {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DataTypeRegistry.class);

    private static final String CELL_CLASS = "cellClass";

    private static final String FACTORY_CLASS = "factoryClass";

    private static final String DEPRECATED_VALUE_FACTORY_ATTR = "factoryValue";

    private static final String DEPRECATED = "deprecated";

    private static final String VALUE_FACTORY_ELEMENT = "ValueFactory";

    private static final String VALUE_FACTORY_CLASS = "valueFactoryClass";

    static final String EXT_POINT_ID = "org.knime.core.DataType";


    private final Map<String, IConfigurationElement> m_factories;

    private final Map<Class<? extends DataCell>, DataCellSerializer<? extends DataCell>> m_serializers;

    private final Map<String, Class<? extends DataCell>> m_cellClassMap;

    private final Map<String, Class<? extends DataValue>> m_valueClassMap;

    private Collection<DataType> m_allDataTypes;

    private final Map<String, Class<? extends ValueFactory<?, ?>>> m_valueFactoryClassMap;

    private boolean m_cellToValueFactoryInitialized;

    private final Map<String, String> m_cellToValueFactoryMap;

    private final Map<String, String> m_valueFactoryToCellMap;

    /**
     * Map of, e.g.
     * <pre>SmilesCell.class -> SmilesAdapterCell.class, SmilesBlobCell.class</pre>
     *
     * See {@link #getImplementationSubDataTypes(DataType)} for details.
     */
    private final Map<String, List<String>> m_cellClassNameToImplementationOfCellClassName;

    private static final DataTypeRegistry INSTANCE = new DataTypeRegistry();

    /**
     * Returns the singleton instance.
     *
     * @return the singlet data type registry
     */
    public static DataTypeRegistry getInstance() {
        return INSTANCE;
    }

    private DataTypeRegistry() {
        m_factories = new LinkedHashMap<>();
        m_serializers = new ConcurrentHashMap<>();
        m_cellClassMap = new ConcurrentHashMap<>();
        m_valueClassMap = new ConcurrentHashMap<>();
        m_valueFactoryClassMap = new ConcurrentHashMap<>();
        m_cellToValueFactoryMap = new ConcurrentHashMap<>();
        m_valueFactoryToCellMap = new ConcurrentHashMap<>();
        m_cellClassNameToImplementationOfCellClassName = new ConcurrentHashMap<>();
        getExtensionStream()//
            .filter(e -> (e.getAttribute(FACTORY_CLASS) != null))//
            .forEach(e -> m_factories.put(e.getAttribute(CELL_CLASS), e));

        m_cellClassMap.put(DataCell.class.getName(), DataCell.class);
        m_cellClassMap.put(DataType.MissingCell.class.getName(), DataType.MissingCell.class);
        m_valueClassMap.put(DataValue.class.getName(), DataValue.class);
        m_valueFactoryClassMap.put(DefaultRowKeyValueFactory.class.getName(), DefaultRowKeyValueFactory.class);
        m_valueFactoryClassMap.put(VoidRowKeyFactory.class.getName(), VoidRowKeyFactory.class);
    }

    /**
     * Returns a factory for the given data type.
     *
     * @param type the data type for which a factory should be returned
     * @param fileStore the {@link FileStoreFactory} to use
     * @return a new cell factory or an empty optional if not factory is registered
     */
    Optional<DataCellFactory> getFactory(final DataType type, final FileStoreFactory fileStore) {
        return getFactory(type.getCellClass().getName(), fileStore);
    }

    /**
     * Returns a factory for the base cell class.
     *
     * @param className the fully qualified name of a cell class
     * @param fileStore the {@link FileStoreFactory} to use
     * @return a new cell factory or an empty optional if not factory is registered
     */
    Optional<DataCellFactory> getFactory(final String className, final FileStoreFactory fileStore) {
        CheckUtils.checkNotNull(fileStore);
        IConfigurationElement configElement = m_factories.get(className);
        if (configElement == null) {
            return Optional.empty();
        } else {
            try {
                DataCellFactory fac = (DataCellFactory)configElement.createExecutableExtension(FACTORY_CLASS);

                if (EclipseUtil.isRunFromSDK()) {
                    if (!fac.getDataType().getCellClass().getName().equals(className)) {
                        NodeLogger.getLogger(getClass())
                            .coding("Data cell factory '" + configElement.getAttribute(FACTORY_CLASS)
                                + "' for cell implementation '" + className + "' doesn't match with the data type '"
                                + fac.getDataType() + "'. Please fix your implementation.");
                    }
                }
                fac.initFactory(fileStore);

                Class<? extends DataCell> cellClass = fac.getDataType().getCellClass();
                if (m_cellClassMap.put(className, cellClass) == null) {
                    collectValueInterfaces(cellClass);
                }

                return Optional.of(fac);
            } catch (CoreException ex) {
                NodeLogger.getLogger(getClass()).error("Could not create data cell factory for '" + className
                    + "' from plug-in '" + configElement.getNamespaceIdentifier() + "': " + ex.getMessage(), ex);
                return Optional.empty();
            }
        }
    }

    /**
     * Returns a collection with all known data types (that are registered at the extension point
     * <tt>org.knime.core.DataType</tt>).
     *
     * @return a (possibly empty) collection with data types
     */
    public synchronized Collection<DataType> availableDataTypes() {
        // perform lazy initialization
        if (m_allDataTypes != null) {
            return m_allDataTypes;
        }

        final var types = new ArrayList<DataType>();

        for (IConfigurationElement configElement : m_factories.values()) {
            final var cellClass = configElement.getAttribute(CELL_CLASS);
            try {
                final var dataCellFactory = (DataCellFactory)configElement.createExecutableExtension(FACTORY_CLASS);
                types.add(dataCellFactory.getDataType());
                final var implementationCellClassList = Stream.of(configElement.getChildren("serializer")) //
                    .map(cfe -> cfe.getAttribute(CELL_CLASS)) //
                    .filter(Objects::nonNull) //
                    .toList();
                m_cellClassNameToImplementationOfCellClassName.put(cellClass, implementationCellClassList);
            } catch (Throwable e) { // NOSONAR - 3rd party extension, see AP-21672
                NodeLogger.getLogger(getClass())
                    .error("Could not create data cell factory '" + configElement.getAttribute(FACTORY_CLASS)
                        + "' for '" + cellClass + "' from plug-in '"
                        + configElement.getNamespaceIdentifier() + "': " + e.getMessage(), e);
            }
        }

        m_allDataTypes = Collections.unmodifiableCollection(types);
        return types;
    }

    /**
     * Returns the {@link DataCell} class for the given class name. This method looks through all registered
     * {@link DataCell} implementations. If no data cell implementation is found, an empty optional is returned. <br>
     * This method should only be used by {@link DataType} for creating data types that were saved to disc.
     *
     * @param className a class name
     * @return an optional containing the requested data cell class
     * @throws ClassCastException if the loaded class does not extend {@link DataCell}
     */
    public Optional<Class<? extends DataCell>> getCellClass(final String className) {
        Class<? extends DataCell> cellClass = m_cellClassMap.get(className);
        if (cellClass != null) {
            return Optional.of(cellClass);
        }

        if (className.startsWith("de.unikn.knime.")) {
            return getCellClass(className.replace("de.unikn.knime.", "org.knime."));
        }

        Optional<DataCellSerializer<DataCell>> o = scanExtensionPointForSerializer(className);
        if (o.isPresent()) {
            return Optional.of(m_cellClassMap.get(className));
        }

        NodeLogger.getLogger(getClass())
            .coding("Data cell implementation '" + className + "' is not registered at extension point '" + EXT_POINT_ID
                + "' via it's serializer. Please change your implementation and use the extension point.");
        return Optional.empty();
    }

    /**
     * Returns the {@link DataValue} class for the given class name. This method looks through all registered
     * {@link DataCell} implementations and inspects their implemented interfaces. If no data cell implements the given
     * value class, an empty optional is returned.
     *
     * @param className a class name
     * @return an optional containing the requested value class
     * @throws ClassCastException if the loaded class does not extend {@link DataValue}
     */
    public Optional<Class<? extends DataValue>> getValueClass(final String className) {
        Class<? extends DataValue> valueClass = m_valueClassMap.get(className);

        if (valueClass == null) {
            // not found => scan extension point
            scanExtensionPointForAllSerializers();
            valueClass = m_valueClassMap.get(className);
        }

        if (valueClass != null) {
            return Optional.of(valueClass);
        }

        NodeLogger.getLogger(getClass()).coding("Data value extension '" + className
            + "' is not available via data cell serializer at extension point " + EXT_POINT_ID + ".");
        return Optional.empty();
    }

    /**
     * Returns a serializer for the given cell class. If no serializer is available an empty optional is returned.
     * Serializers are take from the extention point <tt>org.knime.core.DataType</tt> and as fall back using buddy
     * classloading. The fallback will be removed with the next major release.
     *
     * @param cellClass a data cell class
     * @return an optional containing a serializer for the cell class
     */
    public Optional<DataCellSerializer<DataCell>> getSerializer(final Class<? extends DataCell> cellClass) {
        @SuppressWarnings("unchecked")
        DataCellSerializer<DataCell> ser = (DataCellSerializer<DataCell>)m_serializers.get(cellClass);
        if (ser != null) {
            if (ser instanceof NoSerializer) {
                return Optional.empty();
            } else {
                return Optional.of(ser);
            }
        }

        Optional<DataCellSerializer<DataCell>> o2 = scanExtensionPointForSerializer(cellClass.getName());
        if (o2.isPresent()) {
            if (o2.get() instanceof NoSerializer) {
                return Optional.empty();
            } else {
                return o2;
            }
        }

        // check old static method
        // TODO remove with next major release
        try {
            @SuppressWarnings({"deprecation", "unchecked"})
            DataCellSerializer<? extends DataCell> serializer =
                SerializerMethodLoader.getSerializer(cellClass, DataCellSerializer.class, "getCellSerializer", false);
            ser = (DataCellSerializer<DataCell>)serializer;
            NodeLogger.getLogger(getClass())
                .coding("No serializer for cell class '" + cellClass + "' registered at " + "extension point '"
                    + EXT_POINT_ID + "', using static method as fallback. Please change your "
                    + "implementation and use the extension point.");
            m_cellClassMap.put(cellClass.getName(), cellClass);
            collectValueInterfaces(cellClass);
            m_serializers.put(cellClass, ser);
            return Optional.of(ser);
        } catch (NoSuchMethodException nsme) {
            NodeLogger.getLogger(getClass())
                .coding(
                    "Class \"" + cellClass.getSimpleName() + "\" does not have a custom DataCellSerializer, "
                        + "using standard (but slow) Java serialization. Consider implementing a DataCellSerialzer.",
                    nsme);
            return Optional.empty();
        }
    }

    /**
     * Returns the {@link ValueFactory} class for the given class name. This method looks through all registered
     * {@link ValueFactory} implementations. If no data value factory implementation is found, an empty optional is
     * returned.
     *
     * @param valueFactoryClass the class name
     * @return an optional containing the requested value factory cell class
     * @since 4.3
     * @noreference This method is not intended to be referenced by clients.
     */
    public Optional<Class<? extends ValueFactory<?, ?>>> getValueFactoryClass(final String valueFactoryClass) {//NOSONAR
        final Class<? extends ValueFactory<?, ?>> value = m_valueFactoryClassMap.get(valueFactoryClass);
        if (value != null) {
            return Optional.of(value);
        }
        // Update the Map (might activate the plugin for this class)
        updateValueFactoryClassMap(valueFactoryClass);
        return Optional.ofNullable(m_valueFactoryClassMap.get(valueFactoryClass));
    }

    /**
     * Find the {@link ValueFactory} registered for the given type.
     *
     * @param type the {@link DataType}
     * @return an {@link Optional} holding the class of the {@link ValueFactory} if one is registered.
     *         <code>Optional.empty()</code> if no {@link ValueFactory} is registered for the given type.
     * @since 4.3
     * @noreference This method is not intended to be referenced by clients.
     */
    public Optional<Class<? extends ValueFactory<?, ?>>> getValueFactoryFor(final DataType type) {//NOSONAR
        final Class<? extends DataCell> cellClass = type.getCellClass();
        // No fixed cell class -> There is no according ValueFactory
        if (cellClass == null) {
            return Optional.empty();
        }

        // Get the Factory class (no plugin needs to be activated to get the mapping)
        ensureCellToValueFactoryMapInitialized();
        final String factoryClass = m_cellToValueFactoryMap.get(cellClass.getName());

        // No value factory for this cell class
        if (factoryClass == null) {
            return Optional.empty();
        }

        // Get the value factory class
        return getValueFactoryClass(factoryClass);
    }

    private void ensureCellToValueFactoryMapInitialized() {
        if (!m_cellToValueFactoryInitialized) {
            initCellToValueFactoryMap();
        }
    }

    /**
     * A {@link DataType} represents one specific {@link DataCell} class (if at all) but there might be
     * other {@link DataCell} classes that also represent the same type and this map brings them together
     * (by class name), e.g.
     * <pre>
     *    SmilesCell.class -> SmilesAdapterCell.class, SmilesBlobCell.class
     * </pre>
     *
     * <p>This information is only available for some of the types and derived from registered ValueFactories
     * (added in 4.3). It was added as part of AP-12956, whereby the specific requirement here is to have
     * the information for cell types supporting Adapters.
     * @param type
     * @return A (possible empty) stream of class names of DataCell implementations representing the "same" type.
     *
     * @since 5.2
     * @noreference This method is not intended to be referenced by clients.
     */
    // added as part of AP-12956 (type mapping for chem types), tested in database extension (which use type mapping)
    public Stream<String> getImplementationSubDataTypes(final DataType type) {
        final Class<? extends DataCell> cellClass = type.getCellClass();
        if (cellClass == null) {
            return Stream.empty();
        }
        availableDataTypes();
        return m_cellClassNameToImplementationOfCellClassName.getOrDefault(cellClass.getName(), List.of()).stream();
    }

    /**
     * Retrieves the DataCell class that is linked with the provided ValueFactory.
     *
     * @param valueFactory to get the corresponding cell class for
     * @return the cell class corresponding to ValueFactory+
     * @noreference This method is not intended to be referenced by clients.
     */
    public Class<? extends DataCell> getCellClassForValueFactory(final ValueFactory<?, ?> valueFactory) {
        ensureCellToValueFactoryMapInitialized();
        final var cellClassName = m_valueFactoryToCellMap.get(valueFactory.getClass().getName());
        CheckUtils.checkArgument(cellClassName != null,
            "The provided value factory class '%s' is not properly registered at the DataType extension point.",
            valueFactory);
        return getCellClass(cellClassName)//
            .orElseThrow(() -> new IllegalStateException(String.format(
                "The cell class '%s' is linked to the ValueFactory '%s' but is otherwise unknown to the framework.",
                cellClassName, valueFactory)));
    }

    private <T extends DataCell> Optional<DataCellSerializer<T>>
        scanExtensionPointForSerializer(final String cellClassName) {
        // not found => scan extension point
        Optional<IConfigurationElement> o = getExtensionStream()//
            .flatMap(cfe -> Stream.of(cfe.getChildren("serializer")))//
            .filter(cfe -> cfe.getAttribute(CELL_CLASS).equals(cellClassName))//
            .findFirst();
        if (o.isPresent()) {
            IConfigurationElement configElement = o.get();
            return createSerializer(configElement);
        } else {
            return Optional.empty();
        }
    }

    private static IExtensionPoint getExtensionPoint() {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);
        assert point != null : "Invalid extension point id: " + EXT_POINT_ID;
        return point;
    }

    private static Stream<IConfigurationElement> getExtensionStream() {
        return Stream.of(getExtensionPoint().getExtensions())//
            .flatMap(ext -> Stream.of(ext.getConfigurationElements()));
    }

    private void scanExtensionPointForAllSerializers() {
        getExtensionStream()//
            .flatMap(cfe -> Stream.of(cfe.getChildren("serializer")))//
            .filter(cfe -> !m_cellClassMap.containsKey(cfe.getAttribute(CELL_CLASS)))//
            .forEach(this::createSerializer);
    }

    private <T extends DataCell> Optional<DataCellSerializer<T>>
        createSerializer(final IConfigurationElement configElement) {
        String cellClassName = configElement.getAttribute(CELL_CLASS);
        try {
            @SuppressWarnings("unchecked")
            DataCellSerializer<T> ser =
                (DataCellSerializer<T>)configElement.createExecutableExtension("serializerClass");

            Class<? extends DataCell> cellClass = ser.getCellClass();

            if (!cellClass.getName().equals(cellClassName)) {
                NodeLogger.getLogger(getClass())
                    .coding("Data cell serializer class '" + ser.getClass().getName() + "' does not seem to create '"
                        + cellClassName + "' but instead '" + cellClass.getName()
                        + "'. Please check your implementation and the proper use of generics.");
                return Optional.empty();
            } else {
                if (m_cellClassMap.put(cellClassName, cellClass) == null) {
                    collectValueInterfaces(cellClass);
                }
                m_serializers.put(cellClass, ser);
                return Optional.of(ser);
            }
        } catch (CoreException ex) {
            NodeLogger.getLogger(getClass()).error("Could not create data cell serializer for '" + cellClassName
                + "' from plug-in '" + configElement.getNamespaceIdentifier() + "': " + ex.getMessage(), ex);
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private void collectValueInterfaces(final Class<?> clazz) {
        if (clazz != null) {
            if (DataValue.class.isAssignableFrom(clazz) && clazz.isInterface()) {
                m_valueClassMap.put(clazz.getName(), (Class<? extends DataValue>)clazz);
            }

            collectValueInterfaces(clazz.getSuperclass());
            for (Class<?> c : clazz.getInterfaces()) {
                collectValueInterfaces(c);
            }
        }
    }

    /** Fills {@link #m_cellToValueFactoryMap} if not already filled. Adds all values and does not activate plugins. */
    private synchronized void initCellToValueFactoryMap() {
        if (!m_cellToValueFactoryInitialized) {
            getExtensionStream().forEach(this::collectValueFactories);
            m_cellToValueFactoryInitialized = true;
        }
    }

    private void collectValueFactories(final IConfigurationElement dataTypeExtension) {
        collectValueFactoriesFromDeprecatedAttribute(dataTypeExtension);
        collectValueFactoryElements(dataTypeExtension);
    }

    private void collectValueFactoriesFromDeprecatedAttribute(final IConfigurationElement dataTypeExtension) {
        final var cellClass = dataTypeExtension.getAttribute(CELL_CLASS);
        var deprecatedValueFactoryAttribute = dataTypeExtension.getAttribute(DEPRECATED_VALUE_FACTORY_ATTR);
        if (deprecatedValueFactoryAttribute != null) {
            LOGGER.debugWithFormat(
                "The data type extension for '%s' uses the deprecated %s attribute and will "
                    + "be overwritten if there are any undeprecated ValueFactory child elements.",
                cellClass, DEPRECATED_VALUE_FACTORY_ATTR);
            linkCellAndValueFactory(cellClass, deprecatedValueFactoryAttribute);
        }
    }

    private void collectValueFactoryElements(final IConfigurationElement dataTypeExtension) {
        final var cellClass = dataTypeExtension.getAttribute(CELL_CLASS);
        var valueFactoryChildren = dataTypeExtension.getChildren(VALUE_FACTORY_ELEMENT);
        var cellClassesWithDeprecatedValueFactories = new ArrayList<String>();

        for (IConfigurationElement valueFactoryElement : valueFactoryChildren) {
            var valueFactorySpecificCellClass = cellClass;
            if (valueFactoryElement.getAttribute(CELL_CLASS) != null) {
                valueFactorySpecificCellClass = valueFactoryElement.getAttribute(CELL_CLASS);
            }
            final var valueFactoryClass = valueFactoryElement.getAttribute(VALUE_FACTORY_CLASS);
            if (m_valueFactoryToCellMap.containsKey(valueFactoryClass)) {
                LOGGER.codingWithFormat("Attempting to register ValueFactory " + valueFactoryClass
                    + " for a second cell class. Was registered for "
                    + m_valueFactoryToCellMap.get(valueFactoryClass) + " already, ignoring registration for "
                    + valueFactorySpecificCellClass + ".");
            } else {
                m_valueFactoryToCellMap.put(valueFactoryClass, valueFactorySpecificCellClass);
            }

            if (isNotDeprecated(valueFactoryElement)) {
                if (m_cellToValueFactoryMap.containsKey(valueFactorySpecificCellClass)) {
                    LOGGER.codingWithFormat("More than one ValueFactory for the DataType with cell class '%s' was not "
                        + "marked as deprecated. Only the first one is used as ValueFactory for this DataType.",
                                valueFactorySpecificCellClass);
                } else {
                    m_cellToValueFactoryMap.put(valueFactorySpecificCellClass, valueFactoryClass);
                }
            } else {
                cellClassesWithDeprecatedValueFactories.add(valueFactorySpecificCellClass);
            }
        }

        for (var deprecatedCellClass : cellClassesWithDeprecatedValueFactories) {
            if (!m_cellToValueFactoryMap.containsKey(deprecatedCellClass)) {
                LOGGER.codingWithFormat("All ValueFactories for the DataType with cell class '%s' were deprecated.",
                    deprecatedCellClass);
            }
        }
    }

    private static boolean isNotDeprecated(final IConfigurationElement element) {
        var deprecated = element.getAttribute(DEPRECATED);
        // null is allowed because the deprecation flag is optional and defaults to "false"
        return deprecated == null || deprecated.equalsIgnoreCase("false");
    }

    private void linkCellAndValueFactory(final String cellClass, final String valueFactoryClass) {
        m_cellToValueFactoryMap.put(cellClass, valueFactoryClass);
        m_valueFactoryToCellMap.put(valueFactoryClass, cellClass);
    }

    /**
     * Updates {@link #m_valueFactoryClassMap} to find the value class asked for. Activates the plugin of the given
     * ValueFactory class.
     */
    private void updateValueFactoryClassMap(final String valueFactoryClassName) {
        getExtensionStream()//
            .filter(e -> m_cellClassMap.containsKey(e.getAttribute(CELL_CLASS)) // Extension already loaded
                || containsValueFactory(e, valueFactoryClassName)) // Explicitly asked for this value factory
            .forEach(this::addValueFactoryClassMapping);
    }

    private static boolean containsValueFactory(final IConfigurationElement dataTypeExtension,
        final String valueFactoryClassName) {
        var deprecatedValueFactoryAttribute = dataTypeExtension.getAttribute(DEPRECATED_VALUE_FACTORY_ATTR);
        if (valueFactoryClassName.equals(deprecatedValueFactoryAttribute)) {
            return true;
        } else {
            return Stream.of(dataTypeExtension.getChildren(VALUE_FACTORY_ELEMENT))//
                .map(e -> e.getAttribute(VALUE_FACTORY_CLASS))//
                .anyMatch(valueFactoryClassName::equals);
        }
    }

    /** Put the value factory defined in the given configuration element into {@link #m_valueFactoryClassMap}. */
    private void addValueFactoryClassMapping(final IConfigurationElement dataTypeExtension) {
        final String valueFactoryClassName = dataTypeExtension.getAttribute(DEPRECATED_VALUE_FACTORY_ATTR);
        if (valueFactoryClassName != null) {
            addValueFactoryClassMapping(dataTypeExtension, DEPRECATED_VALUE_FACTORY_ATTR);
        } else {
            Stream.of(dataTypeExtension.getChildren(VALUE_FACTORY_ELEMENT))//
                .forEach(e -> addValueFactoryClassMapping(e, VALUE_FACTORY_CLASS));
        }
    }

    private void addValueFactoryClassMapping(final IConfigurationElement e, final String classNameKey) {
        final var valueFactoryClassName = e.getAttribute(classNameKey);
        try {
            // Create an instance
            final ValueFactory<?, ?> f = (ValueFactory<?, ?>)e.createExecutableExtension(classNameKey);
            @SuppressWarnings("unchecked")
            // Get the class
            final Class<? extends ValueFactory<?, ?>> valueFactoryClass =
                (Class<? extends ValueFactory<?, ?>>)f.getClass();
            // Put the class into the map
            m_valueFactoryClassMap.put(valueFactoryClassName, valueFactoryClass);
        } catch (final CoreException ex) {
            LOGGER.coding("The value factory class '" + valueFactoryClassName + "' registered at extension point '"
                + EXT_POINT_ID + "' could not be created. Ignoring extension.", ex);
        }
    }

}
