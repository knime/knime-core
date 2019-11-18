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
 *   Oct 25, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.data.meta;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.data.DataType;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;

/**
 * The registry for {@link DataColumnMetaData} registered via the MetaData extension point.
 *
 * It allows to retrieve {@link DataColumnMetaDataCreator creators} and {@link DataColumnMetaDataSerializer serializers}
 * for {@link DataColumnMetaData}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @noreference This enum is not intended to be referenced by clients. Pending API.
 * @since 4.1
 */
public enum DataColumnMetaDataRegistry {

        /**
         * The MetaDataRegistry instance.
         */
        INSTANCE;

    private static final String EXT_POINT_ID = "org.knime.core.DataColumnMetaDataType";

    private static final String EXTENSION_ATTRIBUTE_ID = "dataColumnMetaDataExtension";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DataColumnMetaDataRegistry.class);

    private final Map<Class<? extends DataColumnMetaData>, DataColumnMetaDataSerializer<?>> m_serializers;

    private final Map<Class<? extends DataColumnMetaData>, DataColumnMetaDataExtension<?>> m_creatorFactories;

    private Map<String, Class<? extends DataColumnMetaData>> m_metaDataClasses;

    private DataColumnMetaDataRegistry() {
        final IExtensionPoint point = getExtensionPoint();

        m_creatorFactories = new HashMap<>();
        m_serializers = new HashMap<>();
        m_metaDataClasses = new HashMap<>();
        for (IExtension ext : point.getExtensions()) {
            for (IConfigurationElement config : ext.getConfigurationElements()) {
                final String creatorFactoryName = config.getAttribute(EXTENSION_ATTRIBUTE_ID);
                if (creatorFactoryName == null) {
                    continue;
                }
                final DataColumnMetaDataExtension<?> creatorFactory = createInstance(config, EXTENSION_ATTRIBUTE_ID,
                    DataColumnMetaDataExtension.class, "Could not create meta data creator factory from plug-in '%s'",
                    config.getNamespaceIdentifier());
                if (creatorFactory == null) {
                    continue;
                }
                final Class<? extends DataColumnMetaData> metaDataClass = creatorFactory.getMetaDataClass();
                final String metaDataClassName = metaDataClass.getName();
                final DataColumnMetaDataSerializer<?> serializer = creatorFactory.createSerializer();
                m_metaDataClasses.put(metaDataClassName, metaDataClass);
                m_creatorFactories.put(metaDataClass, creatorFactory);
                m_serializers.put(metaDataClass, serializer);
            }
        }

    }

    private static Class<? extends DataColumnMetaData> getMetaDataClass(final String name) {
        final Class<?> clazz;
        try {
            clazz = Class.forName(name);
        } catch (ClassNotFoundException ex) {
            LOGGER.error(String.format("The class declaration for the meta data class '%s' could not be found.", name),
                ex);
            return null;
        }
        return clazz.asSubclass(DataColumnMetaData.class);
    }

    /**
     * Retrieves the typed runtime class of a {@link DataColumnMetaData} object.
     *
     * @param metaData the {@link DataColumnMetaData} object whose runtime class is required
     * @return the typed runtime class of {@link DataColumnMetaData metaData}
     */
    public <M extends DataColumnMetaData> Class<M> getClass(final M metaData) {
        final String name = metaData.getClass().getName();
        final Class<? extends DataColumnMetaData> wildcardClass = m_metaDataClasses.get(name);
        CheckUtils.checkState(wildcardClass != null,
            "The meta data class '%s' is not registered at the MetaDataType extension point.", name);
        // m_metaDataClasses maps from class names to their class instance
        @SuppressWarnings("unchecked")
        final Class<M> typedClass = (Class<M>)wildcardClass;
        return typedClass;
    }

    private static <T> T createInstance(final IConfigurationElement configElement, final String key,
        final Class<T> expectedClass, final String format, final Object... args) {
        try {
            return expectedClass.cast(configElement.createExecutableExtension(key));
        } catch (CoreException ex) {
            NodeLogger.getLogger(DataColumnMetaDataRegistry.class)
                .error(String.format(format + ": " + ex.getMessage(), args), ex);
            return null;
        }
    }

    /**
     * Returns a {@link DataColumnMetaDataCreator} for the provided <b>metaDataClass</b>.
     *
     * @param metaDataClass the class of {@link DataColumnMetaData} for which a creator is required
     * @return a fresh {@link DataColumnMetaDataCreator} for the provided <b>metaDataCreator</b>
     */
    public <T extends DataColumnMetaData> DataColumnMetaDataCreator<T> getCreator(final Class<T> metaDataClass) {
        final DataColumnMetaDataExtension<T> factory = retrieveTyped(m_creatorFactories, metaDataClass);
        return factory.create();
    }

    /**
     * Returns a {@link DataColumnMetaDataCreator} initialized with {@link DataColumnMetaData metaData}.
     *
     * @param metaData the {@link DataColumnMetaData} to use as initialization for the creator
     * @return a new {@link DataColumnMetaDataCreator} initialized with the information from {@link DataColumnMetaData
     *         metaData}
     */
    public <T extends DataColumnMetaData> DataColumnMetaDataCreator<T> getInitializedCreator(final T metaData) {
        Class<T> metaDataClass = getClass(metaData);
        return getCreator(metaDataClass).merge(metaData);
    }

    private static <M extends DataColumnMetaData, F extends DataColumnMetaDataFramework<M>> F retrieveTyped(
        final Map<Class<? extends DataColumnMetaData>, ? extends DataColumnMetaDataFramework<?>> map,
        final Class<M> metaDataClass) {
        DataColumnMetaDataFramework<?> wildcardFrameworkObject = map.get(metaDataClass);
        CheckUtils.checkState(wildcardFrameworkObject != null, "Unregistered meta data '%s' encountered.",
            metaDataClass.getName());
        return checkAndCast(metaDataClass, wildcardFrameworkObject);
    }

    private static <M extends DataColumnMetaData, F extends DataColumnMetaDataFramework<M>> F
        checkAndCast(final Class<M> metaDataClass, final DataColumnMetaDataFramework<?> frameworkObject) {
        CheckUtils.checkState(metaDataClass.equals(frameworkObject.getMetaDataClass()), "Illegal mapping detected.");
        // the check ensures that frameworkObject indeed applies to M
        @SuppressWarnings("unchecked")
        final F typedFrameworkObject = (F)frameworkObject;
        return typedFrameworkObject;
    }

    /**
     * Fetches a collection of all {@link DataColumnMetaDataCreator creators} that can be used to create meta data for
     * columns of {@link DataType} type. </br>
     * An empty collection is returned if there are no {@link DataColumnMetaDataCreator creators} for this type.
     *
     * @param type the {@link DataType type} for which the {@link DataColumnMetaDataCreator MetaDataCreators} are
     *            required
     * @return the {@link DataColumnMetaDataCreator creators} for meta data referring to {@link DataType type}
     */
    public Collection<DataColumnMetaDataCreator<?>> getCreators(final DataType type) {
        CheckUtils.checkNotNull(type);
        return type.getValueClasses().stream()
            .flatMap(d -> m_creatorFactories.values().stream().filter(m -> m.getDataValueClass().isAssignableFrom(d)))
            .map(DataColumnMetaDataExtension::create).collect(Collectors.toList());
    }

    /**
     * Checks if there are any {@link DataColumnMetaDataCreator MetaDataCreators} associated with {@link DataType} type.
     *
     * @param type the {@link DataType} for which to check if there is any {@link DataColumnMetaDataCreator} associated
     *            with it
     * @return true if there is at least one {@link DataColumnMetaDataCreator} that can generate
     *         {@link DataColumnMetaData} for {@link DataType type}
     */
    boolean hasMetaData(final DataType type) {
        return type.getValueClasses().stream().anyMatch(
            d -> m_creatorFactories.values().stream().anyMatch(m -> m.getDataValueClass().isAssignableFrom(d)));
    }

    /**
     * Gets the {@link DataColumnMetaDataSerializer} for {@link DataColumnMetaData} of class <b>metaDataClass</b>.
     *
     * @param metaDataClass the class of {@link DataColumnMetaData} the serializer is required for
     * @return an {@link Optional} of serializer for {@link DataColumnMetaData} of class <b>metaDataClass</b> or
     *         {@link Optional#empty()} if <b>metaDataClass</b> is unknown to the registry
     */
    public <T extends DataColumnMetaData> Optional<DataColumnMetaDataSerializer<T>>
        getSerializer(final Class<T> metaDataClass) {
        final DataColumnMetaDataSerializer<T> retrieveTyped = retrieveTyped(m_serializers, metaDataClass);
        return Optional.ofNullable(retrieveTyped);
    }

    /**
     * Gets the {@link DataColumnMetaDataSerializer} for {@link DataColumnMetaData} with the class name
     * <b>metaDataClass</b>.
     *
     * @param metaDataClassName the name of the class of {@link DataColumnMetaData} the serializer is required for
     * @return an {@link Optional} of serializer for {@link DataColumnMetaData} with class name <b>metaDataClassName</b>
     *         or {@link Optional#empty()} if the meta data class is unknown to the registry
     */
    public Optional<DataColumnMetaDataSerializer<?>> getSerializer(final String metaDataClassName) {
        return Optional.ofNullable(m_serializers.get(getMetaDataClass(metaDataClassName)));
    }

    private static IExtensionPoint getExtensionPoint() {
        final IExtensionRegistry registry = Platform.getExtensionRegistry();
        final IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);
        assert point != null : "Invalid extension point id: " + EXT_POINT_ID;
        return point;
    }

}
