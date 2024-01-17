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
 *   30.09.2015 (thor): created
 */
package org.knime.core.node.port;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.data.DataTableSpec;
import org.knime.core.internal.SerializerMethodLoader;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObject.PortObjectSerializer;
import org.knime.core.node.port.PortObjectSpec.PortObjectSpecSerializer;
import org.knime.core.node.util.CheckUtils;

/**
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 * @since 3.0
 */
public final class PortTypeRegistry {

    private static final String EXT_POINT_ID = "org.knime.core.PortType";

    private final Map<Class<? extends PortObject>, PortObjectSerializer<? extends PortObject>> m_objectSerializers =
        new ConcurrentHashMap<>();

    private final Map<Class<? extends PortObjectSpec>, PortObjectSpecSerializer<? extends PortObjectSpec>> m_specSerializers =
        new ConcurrentHashMap<>();

    private final Map<String, Class<? extends PortObject>> m_objectClassMap = new ConcurrentHashMap<>();

    private final Map<String, Class<? extends PortObjectSpec>> m_specClassMap = new ConcurrentHashMap<>();

    private final Map<Class<? extends PortObject>, PortType> m_allPortTypes = new HashMap<>();

    private final Map<Class<? extends PortObject>, PortType> m_allOptionalPortTypes = new HashMap<>();

    private static final PortTypeRegistry INSTANCE = new PortTypeRegistry();

    private static final String GENERIC_PORT_NAME = "Generic Port";

    /**
     * Returns the singleton instance.
     *
     * @return the singlet data type registry
     */
    public static PortTypeRegistry getInstance() {
        return INSTANCE;
    }

    private PortTypeRegistry() {
        // read all parser at once, because simply traversing the extension point is quite cheap
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);
        assert point != null : "Invalid extension point id: " + EXT_POINT_ID;

        m_objectClassMap.put(PortObject.class.getName(), PortObject.class);
        m_specClassMap.put(PortObjectSpec.class.getName(), PortObjectSpec.class);

        m_objectClassMap.put(BufferedDataTable.class.getName(), BufferedDataTable.class);

        m_specClassMap.put(DataTableSpec.class.getName(), DataTableSpec.class);
        m_specSerializers.put(DataTableSpec.class, new DataTableSpec.Serializer());

        m_allPortTypes.put(PortObject.class,
            new PortType(PortObject.class, false, GENERIC_PORT_NAME, PortType.DEFAULT_COLOR, false));
        m_allOptionalPortTypes.put(PortObject.class,
            new PortType(PortObject.class, true, GENERIC_PORT_NAME, PortType.DEFAULT_COLOR, false));

        m_allPortTypes.put(BufferedDataTable.class, new PortType(BufferedDataTable.class, false, "Table", 0, false));
        m_allOptionalPortTypes.put(BufferedDataTable.class,
            new PortType(BufferedDataTable.class, true, "Table", 0, false));
    }

    /**
     * Get static logger - not using a static class member to avoid eager initialization (PortTypeRegistry might be
     * instantiated early in the application life cycle, causing a logger init chain with side effects, AP-3352).
     *
     * @return NodeLogger.getLogger(PortTypeRegistry.class);
     */
    private static NodeLogger getLogger() {
        return NodeLogger.getLogger(PortTypeRegistry.class);
    }

    private boolean m_allPortTypesRead;

    /**
     * Returns a collection with all known data types (that registered at the extension point
     * <tt>org.knime.core.DataType</tt>. The returned collection is not sorted in any particular order.
     *
     * @return a (possibly empty) collection with data types
     */
    public synchronized Collection<PortType> availablePortTypes() {
        // perform lazy initialization
        if (!m_allPortTypesRead) {
            IExtensionRegistry registry = Platform.getExtensionRegistry();
            IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);
            Stream.of(point.getExtensions()).flatMap(ext -> Stream.of(ext.getConfigurationElements()))
                .filter(e -> getObjectClass(e.getAttribute("objectClass")).isPresent())
                .forEach(this::createAndRegisterPortType);
            m_allPortTypesRead = true;
        }

        return m_allPortTypes.values();
    }

    /** Used in unit test to assert presence of certain port object classes. */
    Map<Class<? extends PortObject>, PortType> getAvailablePortTypeMap() {
        availablePortTypes();
        return m_allPortTypes;
    }

    private void createAndRegisterPortType(final IConfigurationElement e) {
        int color;
        try {
            color = e.getAttribute("color") != null ? Integer.parseInt(e.getAttribute("color").substring(1), 16)
                : PortType.DEFAULT_COLOR;
        } catch (NumberFormatException ex) {
            getLogger().coding(String.format("Illegal color in port type extension for '%s': %s",
                e.getAttribute("name"), e.getAttribute("color")), ex);
            color = PortType.DEFAULT_COLOR;
        }

        final String poClassName = e.getAttribute("objectClass");
        final Optional<Class<? extends PortObject>> poClassOpt = getObjectClass(poClassName);
        if (poClassOpt.isPresent()) {
            final Class<? extends PortObject> poClass = poClassOpt.get();
            final var isHidden = Boolean.parseBoolean(e.getAttribute("hidden"));
            final String name = e.getAttribute("name");
            // The initialization of a PortType can throw a NoClassDefFoundError, see AP-13925.
            try {
                final var type = new PortType(poClass, false, name, color, isHidden);
                final var optionalType = new PortType(poClass, true, name, color, isHidden);
                m_allPortTypes.put(poClass, type);
                m_allOptionalPortTypes.put(poClass, optionalType);
            } catch (NoClassDefFoundError ex) {
                getLogger().error(String.format("Could not create port type for '%s' from plug-in '%s': %s",
                    poClassName, e.getNamespaceIdentifier(), ex.getMessage()), ex);
            }
        }
    }

    /**
     * For a port object class not registered via extension point it will search the "nearest" registered port type
     * by finding the most specific port object class, which has a proper extension point definition
     * (or PortObject.TYPE).
     * @param portClass class of interest
     */
    private void searchAndRegisterCompatiblePortType(final Class<? extends PortObject> portClass) {
        availablePortTypes();
        final var nonRegType = new PortType(portClass, true, null, PortType.DEFAULT_COLOR, false);
        // modifiable list of all registered port types having the argument type as subtype (incl. PortObject.TYPE)
        final var parentTypeCandidates = m_allPortTypes.values().stream() //
                .filter(type -> type.isSuperTypeOf(nonRegType)) //
                .collect(Collectors.toCollection(ArrayList::new));

        // remove all candidates which are itself only parent types of other candidate types
        for (final var parentTypeIterator = parentTypeCandidates.iterator(); parentTypeIterator.hasNext();) {
            final var parentTypeCandidate = parentTypeIterator.next();
            if (parentTypeCandidates.stream().filter(p -> p != parentTypeCandidate)
                .anyMatch(parentTypeCandidate::isSuperTypeOf)) {
                parentTypeIterator.remove();
            }
        }
        final var parentType = parentTypeCandidates.stream().findFirst()
            .orElseThrow(() -> new IllegalStateException(
                String.format("Expected to find %s as parent of port object of class %s",
                    PortObject.TYPE, portClass.getName())));

        getLogger().debugWithFormat("No registered port type for class %s, using parent %s", portClass.getName(),
            parentType);

        m_allPortTypes.put(portClass, parentType);
        m_allOptionalPortTypes.put(portClass, m_allOptionalPortTypes.get(parentType.getPortObjectClass()));
    }

    /**
     * Returns the port type for the given port object class.
     *
     * @param portClass any port object class, must not be <code>null</code>
     * @return a port type, never <code>null</code>
     */
    public synchronized PortType getPortType(final Class<? extends PortObject> portClass) {
        return getPortType(portClass, false);
    }

    /**
     * Returns the port type for the given port object class.
     *
     * @param portClass any port object class, must not be <code>null</code>
     * @param isOptional <code>true</code> for an optional port, <code>false</code> for a required port
     * @return a port type, never <code>null</code>
     */
    public synchronized PortType getPortType(final Class<? extends PortObject> portClass, final boolean isOptional) {

        var mappedPortClass = PortTypeClassMapperRegistry.getInstance()//
                .map(portClass)//
                .orElse(portClass);

        return getPortTypeInternal(mappedPortClass, isOptional);
    }

    private PortType getPortTypeInternal(final Class<? extends PortObject> portClass, final boolean isOptional) {
        final Map<Class<? extends PortObject>, PortType> map = isOptional ? m_allOptionalPortTypes : m_allPortTypes;
        PortType pt = map.get(CheckUtils.checkArgumentNotNull(portClass));
        if (pt == null) {
            IExtensionRegistry registry = Platform.getExtensionRegistry();
            IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);
            Optional<IConfigurationElement> configElement =
                Stream.of(point.getExtensions()).flatMap(ext -> Stream.of(ext.getConfigurationElements()))
                    .filter(e -> portClass.getName().equals(e.getAttribute("objectClass"))).findFirst();
            configElement.ifPresent(this::createAndRegisterPortType);
            pt = map.get(portClass);
            if (pt == null) { // might still be null (error loading extension point contribution)
                searchAndRegisterCompatiblePortType(portClass);
                pt = map.get(portClass); // not null at this point
            }
        }
        return pt;
    }

    /**
     * Returns the {@link PortObject} class for the given class name. This method looks through all registered
     * {@link PortObject} implementations. If no port object implementation is found, an empty optional is returned.
     *
     * @param className a class name
     * @return an optional containing the requested port object class
     * @throws ClassCastException if the loaded class does not extend {@link PortObject}
     */
    public Optional<Class<? extends PortObject>> getObjectClass(final String className) {
        Class<? extends PortObject> objectClass = m_objectClassMap.get(className);
        if (objectClass != null) {
            return Optional.of(objectClass);
        }

        Optional<PortObjectSerializer<PortObject>> o = scanExtensionPointForObjectSerializer(className);
        if (o.isPresent()) {
            return Optional.of(m_objectClassMap.get(className));
        }

        getLogger().coding(
            "Port object implementation '" + className + "' is not registered at extension point '" + EXT_POINT_ID
                + "' via it's serializer. Please change your implementation " + "and use the extension point.");
        return Optional.empty();
    }

    /**
     * Returns the {@link PortObjectSpec} class for the given class name. This method looks through all registered
     * {@link PortObjectSpec} implementations. If port object spec implementation is found, an empty optional is
     * returned.
     *
     * @param className a class name
     * @return an optional containing the requested data cell class
     * @throws ClassCastException if the loaded class does not extend {@link PortObjectSpec}
     */
    public Optional<Class<? extends PortObjectSpec>> getSpecClass(final String className) {
        Class<? extends PortObjectSpec> specClass = m_specClassMap.get(className);
        if (specClass != null) {
            return Optional.of(specClass);
        }

        Optional<PortObjectSpecSerializer<PortObjectSpec>> o = scanExtensionPointForSpecSerializer(className);
        if (o.isPresent()) {
            return Optional.of(m_specClassMap.get(className));
        }

        getLogger().coding("Port object spec implementation '" + className + "' is not registered at extension point '"
            + EXT_POINT_ID + "' via it's serializer. Please change your implementation and use the extension point.");
        return Optional.empty();
    }

    /**
     * Returns a serializer for the given port object class. If no serializer is available an empty optional is
     * returned. Serializers are taken from the extension point <tt>org.knime.core.PortType</tt> and as fall back using
     * buddy classloading. The fallback will be removed with the next major release.
     *
     * @param objectClass a data cell class
     * @return an optional containing a serializer for the port object class
     */
    @SuppressWarnings({"unchecked", "deprecation", "rawtypes"})
    public Optional<PortObjectSerializer<PortObject>>
        getObjectSerializer(final Class<? extends PortObject> objectClass) {
        PortObjectSerializer<PortObject> ser = (PortObjectSerializer<PortObject>)m_objectSerializers.get(objectClass);
        if (ser != null) {
            return Optional.of(ser);
        }

        Optional<PortObjectSerializer<PortObject>> o2 = scanExtensionPointForObjectSerializer(objectClass.getName());
        if (o2.isPresent()) {
            return o2;
        }

        // check old static method
        // TODO remove with next major release
        try {
            m_objectClassMap.put(objectClass.getName(), objectClass);
            PortObjectSerializer<? extends PortObject> serializer = SerializerMethodLoader.getSerializer(objectClass,
                PortObjectSerializer.class, "getPortObjectSerializer", true);
            ser = (PortObjectSerializer<PortObject>)serializer;
            getLogger().coding("No serializer for port object class '" + objectClass + "' registered at "
                + "extension point '" + EXT_POINT_ID + "', using static method as fallback. Please change your "
                + "implementation and use the extension point.");
            m_objectSerializers.put(objectClass, ser);
            return Optional.of(ser);
        } catch (NoSuchMethodException nsme) {
            // check if it's an AbstractSimplePortObject which had a static method in the abstract class
            if (AbstractSimplePortObject.class.isAssignableFrom(objectClass)) {
                getLogger().coding("No serializer for port object class '" + objectClass + "' registered at "
                    + "extension point '" + EXT_POINT_ID + "', using static method as fallback. Please change your "
                    + "implementation and use the extension point.");
                ser = new AbstractSimplePortObject.AbstractSimplePortObjectSerializer() {
                    /**
                     * {@inheritDoc}
                     */
                    @Override
                        Class<? extends PortObject> getObjectClass() {
                        return objectClass;
                    }
                };
                m_objectSerializers.put(objectClass, ser);
                return Optional.of(ser);
            } else {
                getLogger().coding(
                    "Class \"" + objectClass.getSimpleName() + "\" does not have a custom PortObjectSerializer, "
                        + "using standard (but slow) Java serialization. Consider implementing a PortObjectSerialzer.",
                    nsme);
                return Optional.empty();
            }
        }
    }

    /**
     * Returns a serializer for the given port object spec class. If no serializer is available an empty optional is
     * returned. Serializers are taken from the extension point <tt>org.knime.core.PortType</tt> and as fall back using
     * buddy classloading. The fallback will be removed with the next major release.
     *
     * @param specClass a port object spec class
     * @return an optional containing a serializer for the port object spec class
     */
    @SuppressWarnings({"unchecked", "deprecation", "rawtypes"})
    public Optional<PortObjectSpecSerializer<PortObjectSpec>>
        getSpecSerializer(final Class<? extends PortObjectSpec> specClass) {
        PortObjectSpecSerializer<PortObjectSpec> ser =
            (PortObjectSpecSerializer<PortObjectSpec>)m_specSerializers.get(specClass);
        if (ser != null) {
            return Optional.of(ser);
        }

        Optional<PortObjectSpecSerializer<PortObjectSpec>> o2 =
            scanExtensionPointForSpecSerializer(specClass.getName());
        if (o2.isPresent()) {
            return o2;
        }

        // check old static method
        // TODO remove with next major release
        try {
            m_specClassMap.put(specClass.getName(), specClass);
            PortObjectSpecSerializer<? extends PortObjectSpec> serializer = SerializerMethodLoader
                .getSerializer(specClass, PortObjectSpecSerializer.class, "getPortObjectSpecSerializer", true);
            ser = (PortObjectSpecSerializer<PortObjectSpec>)serializer;
            getLogger().coding("No serializer for port object spec class '" + specClass + "' registered at "
                + "extension point '" + EXT_POINT_ID + "', using static method as fallback. Please change your "
                + "implementation and use the extension point.");
            m_specSerializers.put(specClass, ser);
            return Optional.of(ser);
        } catch (NoSuchMethodException nsme) {
            // check if it's an AbstractSimplePortObject which had a static method in the abstract class
            if (AbstractSimplePortObjectSpec.class.isAssignableFrom(specClass)) {
                getLogger().coding("No serializer for port object spec class '" + specClass + "' registered at "
                    + "extension point '" + EXT_POINT_ID + "', using static method as fallback. Please change your "
                    + "implementation and use the extension point.");
                ser = new AbstractSimplePortObjectSpec.AbstractSimplePortObjectSpecSerializer() {
                    /**
                     * {@inheritDoc}
                     */
                    @Override
                    Class<? extends PortObjectSpec> getSpecClass() {
                        return specClass;
                    }
                };
                m_specSerializers.put(specClass, ser);
                return Optional.of(ser);
            } else {
                getLogger().coding("Class \"" + specClass.getSimpleName()
                    + "\" does not have a custom PortObjectSpecSerializer, "
                    + "using standard (but slow) Java serialization. Consider implementing a PortObjectSpecSerialzer.",
                    nsme);
                return Optional.empty();
            }
        }
    }

    private <T extends PortObject> Optional<PortObjectSerializer<T>>
        scanExtensionPointForObjectSerializer(final String objectClassName) {
        // not found => scan extension point
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);

        Optional<IConfigurationElement> o =
            Stream.of(point.getExtensions()).flatMap(ext -> Stream.of(ext.getConfigurationElements()))
                .filter(cfe -> cfe.getAttribute("objectClass").equals(objectClassName)).findFirst();
        if (o.isPresent()) {
            IConfigurationElement configElement = o.get();
            return createObjectSerializer(configElement);
        } else {
            return Optional.empty();
        }
    }

    private <T extends PortObjectSpec> Optional<PortObjectSpecSerializer<T>>
        scanExtensionPointForSpecSerializer(final String specClassName) {
        // not found => scan extension point
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);

        Optional<IConfigurationElement> o =
            Stream.of(point.getExtensions()).flatMap(ext -> Stream.of(ext.getConfigurationElements()))
                .filter(cfe -> cfe.getAttribute("specClass").equals(specClassName)).findFirst();
        if (o.isPresent()) {
            IConfigurationElement configElement = o.get();
            return createSpecSerializer(configElement);
        } else {
            return Optional.empty();
        }
    }

    private <T extends PortObject> Optional<PortObjectSerializer<T>>
        createObjectSerializer(final IConfigurationElement configElement) {
        String objectClassName = configElement.getAttribute("objectClass");
        try {
            @SuppressWarnings("unchecked")
            PortObjectSerializer<T> ser =
                (PortObjectSerializer<T>)configElement.createExecutableExtension("objectSerializer");

            Class<? extends PortObject> objectClass = ser.getObjectClass();

            if (!objectClass.getName().equals(objectClassName)) {
                getLogger().coding("Port object serializer class '" + ser.getClass().getName()
                    + "' does not seem to create '" + objectClassName + "' but instead '" + objectClass.getName()
                    + "'. Please check your implementation and the proper use of generics.");
                return Optional.empty();
            } else {
                m_objectClassMap.put(objectClassName, objectClass);
                m_objectSerializers.put(objectClass, ser);
                return Optional.of(ser);
            }
        } catch (CoreException ex) {
            getLogger().error("Could not create port object serializer for '" + objectClassName
                + "' from plug-in '" + configElement.getNamespaceIdentifier() + "': " + ex.getMessage(), ex);
            return Optional.empty();
        }
    }

    private <T extends PortObjectSpec> Optional<PortObjectSpecSerializer<T>>
        createSpecSerializer(final IConfigurationElement configElement) {
        String specClassName = configElement.getAttribute("specClass");
        try {
            @SuppressWarnings("unchecked")
            PortObjectSpecSerializer<T> ser =
                (PortObjectSpecSerializer<T>)configElement.createExecutableExtension("specSerializer");

            Class<? extends PortObjectSpec> specClass = ser.getSpecClass();

            if (!specClass.getName().equals(specClassName)) {
                getLogger().coding("Port object spec serializer class '" + ser.getClass().getName()
                        + "' does not seem to create '" + specClassName + "' but instead '" + specClass.getName()
                        + "'. Please check your implementation and the proper use of generics.");
                return Optional.empty();
            } else {
                m_specClassMap.put(specClassName, specClass);
                m_specSerializers.put(specClass, ser);
                return Optional.of(ser);
            }
        } catch (CoreException ex) {
            getLogger().error("Could not create port object spec serializer for '" + specClassName
                + "' from plug-in '" + configElement.getNamespaceIdentifier() + "': " + ex.getMessage(), ex);
            return Optional.empty();
        }
    }
}
