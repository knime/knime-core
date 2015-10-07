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
 *   30.09.2015 (thor): created
 */
package org.knime.core.node.port;

import java.util.ArrayList;
import java.util.Collection;
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
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.eclipseUtil.GlobalClassCreator;
import org.knime.core.internal.SerializerMethodLoader;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObject.PortObjectSerializer;
import org.knime.core.node.port.PortObjectSpec.PortObjectSpecSerializer;

/**
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @since 3.0
 */
public final class PortObjectRegistry {
    private static final String EXT_POINT_ID = "org.knime.core.PortType";

    private final Map<Class<? extends PortObject>, PortObjectSerializer<? extends PortObject>> m_objectSerializers =
        new ConcurrentHashMap<>();

    private final Map<Class<? extends PortObjectSpec>, PortObjectSpecSerializer<? extends PortObjectSpec>> m_specSerializers =
        new ConcurrentHashMap<>();

    private final Map<String, Class<? extends PortObject>> m_objectClassMap = new ConcurrentHashMap<>();

    private final Map<String, Class<? extends PortObjectSpec>> m_specClassMap = new ConcurrentHashMap<>();

    private Collection<PortType> m_allPortTypes;

    private static final PortObjectRegistry INSTANCE = new PortObjectRegistry();

    /**
     * Returns the singleton instance.
     *
     * @return the singlet data type registry
     */
    public static PortObjectRegistry getInstance() {
        return INSTANCE;
    }

    private PortObjectRegistry() {
        // read all parser at once, because simply traversing the extension point is quite cheap
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);
        assert point != null : "Invalid extension point id: " + EXT_POINT_ID;

        m_objectClassMap.put(PortObject.class.getName(), PortObject.class);
        m_specClassMap.put(PortObjectSpec.class.getName(), PortObjectSpec.class);
    }

    /**
     * Returns a collection with all known data types (that registered at the extension point
     * <tt>org.knime.core.DataType</tt>.
     *
     * @return a (possibly empty) collection with data types
     */
    public synchronized Collection<PortType> availablePortTypes() {
        // perform lazy initialization
        if (m_allPortTypes != null) {
            return m_allPortTypes;

        }

        List<PortType> types = new ArrayList<>();

        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);
        Stream.of(point.getExtensions()).flatMap(ext -> Stream.of(ext.getConfigurationElements()))
            .map(e -> e.getAttribute("objectClass")).map(cn -> getObjectClass(cn)).filter(Optional::isPresent)
            .map(Optional::get).forEach(c -> types.add(new PortType(c)));

        m_allPortTypes = types;
        return types;
    }

    /**
     * Returns the {@link PortObject} class for the given class name. This method looks through all registered
     * {@link PortObject} implementations. If no port object implementation is found, an empty optional is returned.
     * <br />
     * As a fallback mechanism, the {@link GlobalClassCreator} is used. This will be changed with the next major
     * release. <br />
     *
     * @param className a class name
     * @return an optional containing the requested port object class
     * @throws ClassCastException if the loaded class does not extend {@link PortObject}
     */
    @SuppressWarnings({"deprecation", "unchecked"})
    public Optional<Class<? extends PortObject>> getObjectClass(final String className) {
        Class<? extends PortObject> objectClass = m_objectClassMap.get(className);
        if (objectClass != null) {
            return Optional.of(objectClass);
        }

        Optional<PortObjectSerializer<PortObject>> o = scanExtensionPointForObjectSerializer(className);
        if (o.isPresent()) {
            return Optional.of(m_objectClassMap.get(className));
        }

        try {
            objectClass = (Class<? extends PortObject>)GlobalClassCreator.createClass(className);
            NodeLogger.getLogger(getClass())
                .coding("Port object implementation '" + className + "' is not registered at extension point '"
                    + EXT_POINT_ID
                    + "' via it's serializer, using buddy classloading as fallback. Please change your implementation "
                    + "and use the extension point.");

            if (!PortObject.class.isAssignableFrom(objectClass)) {
                throw new ClassCastException(
                    "Class '" + className + "' is not a subclass of '" + PortObject.class + "'");
            }
            m_objectClassMap.put(className, objectClass);
            return Optional.of(objectClass);
        } catch (ClassNotFoundException ex) {
            NodeLogger.getLogger(getClass())
                .debug("Port object implementation '" + className + "' not found: " + ex.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Returns the {@link DataCell} class for the given class name. This method looks through all registered
     * {@link DataCell} implementations. If no data cell implementation is found, an empty optional is returned. <br />
     * As a fallback mechanism, the {@link GlobalClassCreator} is used. This will be changed with the next major
     * release. <br />
     * This method should only be used by {@link DataType} for creating data types that were saved to disc.
     *
     * @param className a class name
     * @return an optional containing the requested data cell class
     * @throws ClassCastException if the loaded class does not extend {@link DataCell}
     */
    @SuppressWarnings({"deprecation", "unchecked"})
    public Optional<Class<? extends PortObjectSpec>> getSpecClass(final String className) {
        Class<? extends PortObjectSpec> specClass = m_specClassMap.get(className);
        if (specClass != null) {
            return Optional.of(specClass);
        }

        Optional<PortObjectSpecSerializer<PortObjectSpec>> o = scanExtensionPointForSpecSerializer(className);
        if (o.isPresent()) {
            return Optional.of(m_specClassMap.get(className));
        }

        try {
            specClass = (Class<? extends PortObjectSpec>)GlobalClassCreator.createClass(className);
            NodeLogger.getLogger(getClass())
                .coding("Port object spec implementation '" + className + "' is not registered at extension point '"
                    + EXT_POINT_ID + "' via it's serializer, using buddy classloading as fallback. Please change your "
                    + "implementation and use the extension point.");

            if (!PortObjectSpec.class.isAssignableFrom(specClass)) {
                throw new ClassCastException(
                    "Class '" + className + "' is not a subclass of '" + PortObjectSpec.class + "'");
            }
            m_specClassMap.put(className, specClass);
            return Optional.of(specClass);
        } catch (ClassNotFoundException ex) {
            NodeLogger.getLogger(getClass())
                .debug("Port object specimplementation '" + className + "' not found: " + ex.getMessage());
        }

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
    @SuppressWarnings("unchecked")
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
            PortObjectSerializer<? extends PortObject> serializer = SerializerMethodLoader.getSerializer(objectClass,
                PortObjectSerializer.class, "getPortObjectSerializer", true);
            ser = (PortObjectSerializer<PortObject>)serializer;
            NodeLogger.getLogger(getClass())
                .coding("No serializer for port object class '" + objectClass + "' registered at " + "extension point '"
                    + EXT_POINT_ID + "', using static method as fallback. Please change your "
                    + "implementation and use the extension point.");
            m_objectClassMap.put(objectClass.getName(), objectClass);
            m_objectSerializers.put(objectClass, ser);
            return Optional.of(ser);
        } catch (NoSuchMethodException nsme) {
            NodeLogger.getLogger(getClass()).coding(
                "Class \"" + objectClass.getSimpleName() + "\" does not have a custom PortObjectSerializer, "
                    + "using standard (but slow) Java serialization. Consider implementing a PortObjectSerialzer.",
                nsme);
            return Optional.empty();
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
    @SuppressWarnings("unchecked")
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
            PortObjectSpecSerializer<? extends PortObjectSpec> serializer = SerializerMethodLoader
                .getSerializer(specClass, PortObjectSpecSerializer.class, "getPortObjectSpecSerializer", true);
            ser = (PortObjectSpecSerializer<PortObjectSpec>)serializer;
            NodeLogger.getLogger(getClass())
                .coding("No serializer for port object spec class '" + specClass + "' registered at "
                    + "extension point '" + EXT_POINT_ID + "', using static method as fallback. Please change your "
                    + "implementation and use the extension point.");
            m_specClassMap.put(specClass.getName(), specClass);
            m_specSerializers.put(specClass, ser);
            return Optional.of(ser);
        } catch (NoSuchMethodException nsme) {
            NodeLogger.getLogger(getClass()).coding(
                "Class \"" + specClass.getSimpleName() + "\" does not have a custom PortObjectSpecSerializer, "
                    + "using standard (but slow) Java serialization. Consider implementing a PortObjectSpecSerialzer.",
                nsme);
            return Optional.empty();
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
                NodeLogger.getLogger(getClass())
                    .coding("Port object serializer class '" + ser.getClass().getName() + "' does not seem to create '"
                        + objectClassName + "' but instead '" + objectClass.getName()
                        + "'. Please check your implementation and the proper use of generics.");
                return Optional.empty();
            } else {
                m_objectClassMap.put(objectClassName, objectClass);
                m_objectSerializers.put(objectClass, ser);
                return Optional.of(ser);
            }
        } catch (CoreException ex) {
            NodeLogger.getLogger(getClass()).error("Could not create port object serializer for '" + objectClassName
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
                NodeLogger.getLogger(getClass())
                    .coding("Port object spec serializer class '" + ser.getClass().getName()
                        + "' does not seem to create '" + specClassName + "' but instead '" + specClass.getName()
                        + "'. Please check your implementation and the proper use of generics.");
                return Optional.empty();
            } else {
                m_specClassMap.put(specClassName, specClass);
                m_specSerializers.put(specClass, ser);
                return Optional.of(ser);
            }
        } catch (CoreException ex) {
            NodeLogger.getLogger(getClass()).error("Could not create port object spec serializer for '" + specClassName
                + "' from plug-in '" + configElement.getNamespaceIdentifier() + "': " + ex.getMessage(), ex);
            return Optional.empty();
        }
    }
}
