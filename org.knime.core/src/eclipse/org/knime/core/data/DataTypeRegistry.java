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
 * History
 *   19.08.2015 (thor): created
 */
package org.knime.core.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.eclipseUtil.GlobalClassCreator;
import org.knime.core.internal.SerializerMethodLoader;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.EclipseUtil;

/**
 * A registry for all {@link DataType}s that are registered via the extension point <tt>org.knime.core.DataType</tt>.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @since 3.0
 */
public final class DataTypeRegistry {
    private static final String EXT_POINT_ID = "org.knime.core.DataType";

    private final Map<String, IConfigurationElement> m_factories = new HashMap<>();

    private final Map<Class<? extends DataCell>, DataCellSerializer<? extends DataCell>> m_serializers =
        new ConcurrentHashMap<>();

    private final Map<String, Class<? extends DataCell>> m_cellClassMap = new ConcurrentHashMap<>();
    private final Map<String, Class<? extends DataValue>> m_valueClassMap = new ConcurrentHashMap<>();

    private Collection<DataType> m_allDataTypes;

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
        // read all parser at once, because simply traversing the extension point is quite cheap
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);
        assert point != null : "Invalid extension point id: " + EXT_POINT_ID;

        Stream.of(point.getExtensions())
            .flatMap(ext -> Stream.of(ext.getConfigurationElements()))
            .filter(e -> (e.getAttribute("factoryClass") != null))
            .forEach(e -> m_factories.put(e.getAttribute("cellClass"), e));

        m_cellClassMap.put(DataCell.class.getName(), DataCell.class);
        m_cellClassMap.put(DataType.MissingCell.class.getName(), DataType.MissingCell.class);
        m_valueClassMap.put(DataValue.class.getName(), DataValue.class);
    }

    /**
     * Returns a factory for the given data type. The execution context may be <code>null</code> but then some
     * factories are not able to create cells.
     *
     * @param type the data type for which a factory should be returned
     * @param execContext the current execution context, may be <code>null</code>
     * @return a new cell factory or an empty optional if not factory is registered
     */
    Optional<DataCellFactory> getFactory(final DataType type, final ExecutionContext execContext) {
        return getFactory(type.getCellClass().getName(), execContext);
    }

    /**
     * Returns a factory for the base cell class. The execution context may be <code>null</code> but then some
     * factories are not able to create cells.
     *
     * @param className the fully qualified name of a cell class
     * @param execContext the current execution context, may be <code>null</code>
     * @return a new cell factory or an empty optional if not factory is registered
     */
    Optional<DataCellFactory> getFactory(final String className, final ExecutionContext execContext) {
        IConfigurationElement configElement = m_factories.get(className);
        if (configElement == null) {
            return Optional.empty();
        } else {
            try {
                DataCellFactory fac = (DataCellFactory)configElement.createExecutableExtension("factoryClass");

                if (EclipseUtil.isRunFromSDK()) {
                    if (!fac.getDataType().getCellClass().getName().equals(className)) {
                        NodeLogger.getLogger(getClass()).coding("Data cell factory '"
                                + configElement.getAttribute("factoryClass") + "' for cell implementation '"
                                + className + "' doesn't match with the data type '"
                                + fac.getDataType() + "'. Please fix your implementation.");
                    }
                }
                fac.init(execContext);

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
     * Returns a collection with all known data types (that registered at the extension point
     * <tt>org.knime.core.DataType</tt>.
     *
     * @return a (possibly empty) collection with data types
     */
    public synchronized Collection<DataType> availableDataTypes() {
        // perform lazy initialization
        if (m_allDataTypes != null) {
            return m_allDataTypes;

        }

        List<DataType> types = new ArrayList<>();

        for (IConfigurationElement configElement : m_factories.values()) {
            try {
                DataCellFactory fac = (DataCellFactory)configElement.createExecutableExtension("factoryClass");
                types.add(fac.getDataType());
            } catch (CoreException e) {
                NodeLogger.getLogger(getClass())
                    .error("Could not create data cell factory '" + configElement.getAttribute("factoryClass")
                        + "' for '" + configElement.getAttribute("class") + "' from plug-in '"
                        + configElement.getNamespaceIdentifier() + "': " + e.getMessage(), e);
            }
        }

        m_allDataTypes = types;
        return types;
    }

    /**
     * Returns the {@link DataCell} class for the given class name. This method looks through all registered
     * {@link DataCell} implementations. If no data cell implementation is found, an empty optional is returned.
     * <br />
     * As a fallback mechanism, the {@link GlobalClassCreator} is used. This will be changed with the next major
     * release.
     * <br />
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

        try {
            cellClass = (Class<? extends DataCell>)GlobalClassCreator.createClass(className);
            NodeLogger.getLogger(getClass()).coding(
                "Data cell implementation '" + className + "' is not registered at extension point '" + EXT_POINT_ID
                    + "' via it's serializer, using buddy classloading as fallback. Please change your implementation "
                    + "and use the extension point.");

            if (!DataCell.class.isAssignableFrom(cellClass)) {
                throw new ClassCastException(
                    "Class '" + className + "' is not a subclass of '" + DataCell.class + "'");
            }
            m_cellClassMap.put(className, cellClass);
            collectValueInterfaces(cellClass);
            return Optional.of(cellClass);
        } catch (ClassNotFoundException ex) {
            NodeLogger.getLogger(getClass())
                .debug("Cell class implementation '" + className + "' not found: " + ex.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Returns the {@link DataValue} class for the given class name. This method looks through all registered
     * {@link DataCell} implementations and inspects their implemented interfaces. If no data cell implements the given
     * value class, an empty optional is returned.
     * <br />
     * As a fallback mechanism, the {@link GlobalClassCreator} is used. This will be changed with the next major
     * release.
     *
     * @param className a class name
     * @return an optional containing the requested value class
     * @throws ClassCastException if the loaded class does not extend {@link DataValue}
     */
    @SuppressWarnings({"unchecked"})
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


        // use buddy classloading
        // TODO remove with next major release
        try {
            valueClass = (Class<? extends DataCell>)GlobalClassCreator.createClass(className);
            NodeLogger.getLogger(getClass()).coding(
                "Data value extension '" + className + "' is not available via data cell serializer at extension point "
                    + EXT_POINT_ID + ", using buddy classloading as fallback.");

            if (!DataValue.class.isAssignableFrom(valueClass)) {
                throw new ClassCastException(
                    "Class '" + className + "' is not a subclass of '" + DataValue.class + "'");
            }
            m_valueClassMap.put(className, valueClass);
            return Optional.of(valueClass);
        } catch (ClassNotFoundException ex) {
            NodeLogger.getLogger(getClass())
                .debug("Data value '" + className + "' not found: " + ex.getMessage());
        }
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
            ser = SerializerMethodLoader.getSerializer(cellClass, DataCellSerializer.class, "getCellSerializer", false);
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
                .coding("Class \"" + cellClass.getSimpleName() + "\" does not have a custom DataCellSerializer, "
                    + "using standard (but slow) Java serialization. Consider implementing a DataCellSerialzer.", nsme);
            return Optional.empty();
        }
    }

    private <T extends DataCell> Optional<DataCellSerializer<T>>
        scanExtensionPointForSerializer(final String cellClassName) {
        // not found => scan extension point
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);

        Optional<IConfigurationElement> o =
            Stream.of(point.getExtensions()).flatMap(ext -> Stream.of(ext.getConfigurationElements()))
                .flatMap(cfe -> Stream.of(cfe.getChildren("serializer")))
                .filter(cfe -> cfe.getAttribute("cellClass").equals(cellClassName))
                .findFirst();
        if (o.isPresent()) {
            IConfigurationElement configElement = o.get();
            return createSerializer(configElement);
        } else {
            return Optional.empty();
        }
    }

    private void scanExtensionPointForAllSerializers() {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);

        Stream.of(point.getExtensions())
            .flatMap(ext -> Stream.of(ext.getConfigurationElements()))
            .flatMap(cfe -> Stream.of(cfe.getChildren("serializer")))
            .filter(cfe -> !m_cellClassMap.containsKey(cfe.getAttribute("cellClass")))
            .forEach(cfe -> createSerializer(cfe));
    }


    private <T extends DataCell> Optional<DataCellSerializer<T>>
        createSerializer(final IConfigurationElement configElement) {
        String cellClassName = configElement.getAttribute("cellClass");
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
}
