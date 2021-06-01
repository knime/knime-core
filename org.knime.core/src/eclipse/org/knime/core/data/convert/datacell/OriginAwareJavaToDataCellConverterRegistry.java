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
 *   Apr 29, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.data.convert.datacell;

import static java.util.stream.Collectors.toList;

import java.lang.annotation.IncompleteAnnotationException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.Platform;
import org.knime.core.data.DataCellFactory;
import org.knime.core.data.DataType;
import org.knime.core.data.DataTypeRegistry;
import org.knime.core.data.convert.DataCellFactoryMethod;
import org.knime.core.data.convert.util.ClassUtil;
import org.knime.core.util.Pair;

/**
 * A secure registry for {@link JavaToDataCellConverterFactory JavaToDataCellConverterFactories}.<br>
 * In contrast to {@link JavaToDataCellConverterRegistry} this class does not blindly overwrite factories when another
 * factory with the same ID is provided. It also prioritizes factories provided by KNIME plugins over factories provided
 * by the community.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @noreference This class is not intended to be referenced by clients.
 */
public final class OriginAwareJavaToDataCellConverterRegistry {

    /**
     * The singleton instance.
     */
    public static final OriginAwareJavaToDataCellConverterRegistry INSTANCE =
        new OriginAwareJavaToDataCellConverterRegistry();

    private final Map<Class<?>, List<FactoryItem>> m_bySourceType = new HashMap<>();

    private final Map<DataType, List<FactoryItem>> m_byDestType = new HashMap<>();

    private final Map<String, List<FactoryItem>> m_byIdentifier = new HashMap<>();

    private OriginAwareJavaToDataCellConverterRegistry() {
        collectConverterFactories();
        m_bySourceType.values().forEach(l -> l.sort(Comparator.naturalOrder()));
        m_byDestType.values().forEach(l -> l.sort(Comparator.naturalOrder()));
        m_byIdentifier.values().forEach(l -> l.sort(Comparator.naturalOrder()));
    }

    private void collectConverterFactories() {
        parseAnnotations();
        collectFromExtensionPoint();
    }

    private void parseAnnotations() {
        final Collection<DataType> availableDataTypes = DataTypeRegistry.getInstance().availableDataTypes();
        for (final DataType dataType : availableDataTypes) {
            final Optional<DataCellFactory> cellFactory = dataType.getCellFactory(null);
            if (cellFactory.isPresent()) {
                extractFactoriesFromCellFactory(dataType, cellFactory.get().getClass());
            }
        }
    }

    private void extractFactoriesFromCellFactory(final DataType dataType,
        final Class<? extends DataCellFactory> cellFactoryClass) {
        for (final Pair<Method, DataCellFactoryMethod> pair : ClassUtil.getMethodsWithAnnotation(cellFactoryClass,
            DataCellFactoryMethod.class)) {
            registerFactoryMethodConverterFactory(dataType, pair.getFirst(), pair.getSecond());
        }
    }

    private void registerFactoryMethodConverterFactory(final DataType dataType, final Method method,
        final DataCellFactoryMethod annotation) {
        try {
            final FactoryMethodToDataCellConverterFactory<?, ?> factory = new FactoryMethodToDataCellConverterFactory<>(
                method, ClassUtil.ensureObjectType(method.getParameterTypes()[0]), dataType, annotation.name());
            // Check name of factory
            if (!JavaToDataCellConverterRegistry.validateFactoryName(factory)) {
                return;
            }
            register(factory, Origin.forClass(method.getDeclaringClass()));
        } catch (IncompleteAnnotationException | NoSuchMethodException | SecurityException ex) {//NOSONAR
            // JavaToDataCellConverterRegistry already logged the error, so wea aren't doing it again
        }
    }

    private void collectFromExtensionPoint() {
        for (final IConfigurationElement configurationElement : Platform.getExtensionRegistry()
            .getConfigurationElementsFor(JavaToDataCellConverterRegistry.EXTENSION_POINT_ID)) {
            registerExtensionPointFactory(configurationElement);
        }
    }

    private void registerExtensionPointFactory(final IConfigurationElement configurationElement) {
        try {
            // the specified class may not implement ConverterFactory, so
            // check this first
            final Object extension = configurationElement.createExecutableExtension("factoryClass");
            final IContributor contributor = configurationElement.getContributor();
            if (extension instanceof JavaToDataCellConverterFactory) {
                final JavaToDataCellConverterFactory<?> factory = (JavaToDataCellConverterFactory<?>)extension;
                // Check name of factory
                if (!JavaToDataCellConverterRegistry.validateFactoryName(factory)) {
                    // JavaToDataCellConverterRegistry already logged a warning, so we aren't doing it again
                    return;
                }
                register(factory, Origin.forContributorName(contributor.getName()));
            } else {
                // object was not an instance of ConverterFactory but JavaToDataCellConverterRegistry already logged the corresponding error
            }
        } catch (final Throwable e) {// NOSONAR
            // JavaToDataCellConverterRegistry already logged the error, so we aren't logging it again
        }
    }

    private void register(final JavaToDataCellConverterFactory<?> factory, final Origin origin) {
        final FactoryItem factoryItem = new FactoryItem(factory, origin);
        m_bySourceType.computeIfAbsent(factory.getSourceType(), c -> new ArrayList<>()).add(factoryItem);
        m_byDestType.computeIfAbsent(factory.getDestinationType(), c -> new ArrayList<>()).add(factoryItem);
        m_byIdentifier.computeIfAbsent(factory.getIdentifier(), c -> new ArrayList<>()).add(factoryItem);
    }

    /**
     * Retrieves the factories registered under the provided identifier.<br>
     * The returned list is ordered by the origin of the factories, i.e. factories from org.knime.core come before
     * factories from some KNIME extension and community factories come last.
     *
     * @param identifier String id of the converter factory to retrieve
     * @return the factories registered under the provided identifier (the list is empty if the identifier is unknown)
     */
    public List<JavaToDataCellConverterFactory<?>> getConverterFactoriesByIdentifier(final String identifier) {//NOSONAR
        return getConverterFactoriesByIdentifier(identifier, EnumSet.allOf(Origin.class));
    }

    /**
     * Retrieves the factories registered under the provided identifier from the provided {@link Origin origins}.<br>
     * The returned list is ordered by the origin of the factories, i.e. factories from org.knime.core come before
     * factories from some KNIME extension and community factories come last.
     *
     * @param identifier String id of the converter factory to retrieve
     * @param origin {@link Origin} of the factories to return
     * @param otherOrigins more origins to include
     * @return the factories registered under the provided identifier from the provided origins (the list is empty if
     *         the identifier is unknown or no such factory is provided by any of the origins)
     */
    public List<JavaToDataCellConverterFactory<?>> getConverterFactoriesByIdentifier(final String identifier,//NOSONAR
        final Origin origin, final Origin... otherOrigins) {
        return getConverterFactoriesByIdentifier(identifier, EnumSet.of(origin, otherOrigins));
    }

    private List<JavaToDataCellConverterFactory<?>> getConverterFactoriesByIdentifier(final String identifier,
        final Set<Origin> origins) {
        if (isCollectionIdentifier(identifier)) {
            return createCollectionFactories(identifier, origins);
        }
        return m_byIdentifier.getOrDefault(identifier, Collections.emptyList()).stream()//
            .filter(f -> origins.contains(f.m_origin))//
            .map(FactoryItem::getFactory)//
            .collect(toList());
    }

    private static boolean isCollectionIdentifier(final String identifier) {
        return identifier.startsWith(ArrayToCollectionConverterFactory.class.getName());
    }

    private List<JavaToDataCellConverterFactory<?>> createCollectionFactories(final String identifier,
        final Set<Origin> origins) {
        // get the element converter factory id:
        final String elemConvFactoryId = identifier
            .substring(ArrayToCollectionConverterFactory.class.getName().length() + 1, identifier.length() - 1);
        final List<JavaToDataCellConverterFactory<?>> factories =
            getConverterFactoriesByIdentifier(elemConvFactoryId, origins);
        return factories.stream()//
            .map(f -> new ArrayToCollectionConverterFactory<>(f))// NOSONAR function references don't support <>
            .collect(toList());
    }

    /**
     * Retrieves the factories registered for the provided {@link DataType destinationType}.<br>
     * The returned list is ordered by the origin of the factories, i.e. factories from org.knime.core come before
     * factories from some KNIME extension and community factories come last.
     *
     * @param destinationType {@link DataType} for which to retrieve factories
     * @return the factories registered for the provided {@link DataType destinationType} (the list is empty if there
     *         are no factories for the destinationType)
     */
    public List<JavaToDataCellConverterFactory<?>>//NOSONAR
        getConverterFactoriesByDestinationType(final DataType destinationType) {
        return getConverterFactoriesByDestinationType(destinationType, EnumSet.allOf(Origin.class));
    }

    /**
     * Retrieves the factories registered for the provided {@link DataType destinationType} from the provided
     * {@link Origin origins}.<br>
     * The returned list is ordered by the origin of the factories, i.e. factories from org.knime.core come before
     * factories from some KNIME extension and community factories come last.
     *
     * @param destinationType {@link DataType} for which to retrieve factories
     * @param origin {@link Origin} of the factories to return
     * @param otherOrigins more origins to include
     * @return the factories registered for the provided {@link DataType destinationType} from the provided origins (the
     *         list is empty if none of the origins provided a factory of destinationType)
     */
    public List<JavaToDataCellConverterFactory<?>> getConverterFactoriesByDestinationType(//NOSONAR
        final DataType destinationType, final Origin origin, final Origin... otherOrigins) {
        return getConverterFactoriesByDestinationType(destinationType, EnumSet.of(origin, otherOrigins));
    }

    private List<JavaToDataCellConverterFactory<?>>
        getConverterFactoriesByDestinationType(final DataType destinationType, final Set<Origin> origins) {
        Stream<FactoryItem> itemsForDestType = getItemsByDestinationType(destinationType, origins);
        if (destinationType.isCollectionType()) {
            final Stream<FactoryItem> itemsForElementType =
                getItemsByDestinationType(destinationType.getCollectionElementType(), origins)
                    .map(OriginAwareJavaToDataCellConverterRegistry::toCollectionFactoryItem);
            itemsForDestType = Stream.concat(itemsForDestType, itemsForElementType);
        }
        return itemsForDestType.sorted()//
            .map(FactoryItem::getFactory)//
            .collect(toList());
    }

    private static FactoryItem toCollectionFactoryItem(final FactoryItem item) {
        return new FactoryItem(new ArrayToCollectionConverterFactory<>(item.getFactory()), item.m_origin);
    }

    private Stream<FactoryItem> getItemsByDestinationType(final DataType destinationType, final Set<Origin> origins) {
        return m_byDestType.getOrDefault(destinationType, Collections.emptyList()).stream()//
            .filter(i -> origins.contains(i.m_origin));
    }

    /**
     * Retrieves the factories registered for the provided sourceType.<br>
     * The returned list is ordered by the origin of the factories, i.e. factories from org.knime.core come before
     * factories from some KNIME extension and community factories come last.
     *
     * @param sourceType for which to retrieve factories
     * @return the factories registered for the provided sourceType (the list is empty if there are no factories for the
     *         sourceType)
     */
    public List<JavaToDataCellConverterFactory<?>> getConverterFactoriesBySourceType(final Class<?> sourceType) {//NOSONAR
        return getConverterFactoriesBySourceType(sourceType, EnumSet.allOf(Origin.class));
    }

    /**
     * Retrieves the factories registered for the provided sourceType from the provided {@link Origin origins}.<br>
     * The returned list is ordered by the origin of the factories, i.e. factories from org.knime.core come before
     * factories from some KNIME extension and community factories come last.
     *
     * @param sourceType for which to retrieve factories
     * @param origin {@link Origin} of the factories to return
     * @param otherOrigins more origins to include
     * @return the factories registered for the provided sourceType from the provided origins (the list is empty if none
     *         of the origins provided a factory of sourceType)
     */
    public List<JavaToDataCellConverterFactory<?>> getConverterFactoriesBySourceType(final Class<?> sourceType,//NOSONAR
        final Origin origin, final Origin... otherOrigins) {
        return getConverterFactoriesBySourceType(sourceType, EnumSet.of(origin, otherOrigins));
    }

    private List<JavaToDataCellConverterFactory<?>> getConverterFactoriesBySourceType(final Class<?> sourceType,
        final Set<Origin> origins) {
        Stream<FactoryItem> itemsForSourceType = getItemsFromJavaHierarchy(sourceType, origins);
        if (sourceType.isArray()) {
            final Stream<FactoryItem> itemsForElementType =
                getItemsFromJavaHierarchy(sourceType.getComponentType(), origins)//
                    .map(OriginAwareJavaToDataCellConverterRegistry::toCollectionFactoryItem);
            itemsForSourceType = Stream.concat(itemsForSourceType, itemsForElementType);
        }
        return itemsForSourceType.sorted()//
            .map(FactoryItem::getFactory)//
            .collect(toList());
    }

    private <S> Stream<FactoryItem> getItemsFromJavaHierarchy(final Class<S> sourceType, final Set<Origin> origins) {
        final Set<Class<?>> classesToConsider = new LinkedHashSet<>();
        ClassUtil.recursiveMapToClassHierarchy(sourceType, classesToConsider::add);
        return classesToConsider.stream()//
            .flatMap(c -> m_bySourceType.getOrDefault(c, Collections.emptyList()).stream())//
            .filter(f -> origins.contains(f.m_origin));
    }

    /**
     * Classifies the origin of a {@link JavaToDataCellConverterFactory}.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    public enum Origin {
            /**
             * The factory was declared in KNIME core.
             */
            KNIME_CORE("org.knime.core.", true),
            /**
             * The factory was declared in a KNIME extension.
             */
            KNIME_EXTENSION("org.knime.", true),
            /**
             * The factory was declared in a community extension.
             */
            COMMUNITY("", false);

        private final String m_prefix;

        private final boolean m_isKnime;

        private Origin(final String prefix, final boolean isKnime) {
            m_prefix = prefix;
            m_isKnime = isKnime;
        }

        boolean isKnime() {
            return m_isKnime;
        }

        static Origin forContributorName(final String contributorName) {
            for (Origin origin : values()) {
                if (contributorName.startsWith(origin.m_prefix)) {
                    return origin;
                }
            }
            // never reached because Community matches everything
            throw new IllegalArgumentException("No origin available for: " + contributorName);
        }

        static Origin forClass(final Class<?> factoryClass) {
            final String name = factoryClass.getName();
            return forContributorName(name);
        }
    }

    private static final class FactoryItem implements Comparable<FactoryItem> {

        private final JavaToDataCellConverterFactory<?> m_factory;

        private final Origin m_origin;

        FactoryItem(final JavaToDataCellConverterFactory<?> factory, final Origin origin) {
            m_origin = origin;
            m_factory = factory;
        }

        private JavaToDataCellConverterFactory<?> getFactory() {
            return m_factory;
        }

        @Override
        public int compareTo(final FactoryItem o) {
            return m_origin.compareTo(o.m_origin);
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (obj instanceof FactoryItem) {
                final FactoryItem other = (FactoryItem)obj;
                return m_factory.equals(other.m_factory) && m_origin == other.m_origin;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()//
                .append(m_factory)//
                .append(m_origin)//
                .toHashCode();
        }
    }
}
