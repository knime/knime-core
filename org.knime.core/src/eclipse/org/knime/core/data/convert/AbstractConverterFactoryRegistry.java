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
 *   25.05.2018 (Jonathan Hale): created
 */
package org.knime.core.data.convert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.core.node.NodeLogger;

/**
 * Abstract converter factory registry.
 *
 * Common code for registries which manage {@link ConverterFactory converter factories}.
 *
 * @author Jonathan Hale
 * @param <ST> Type of source types
 * @param <DT> Type of dest types
 * @param <ConverterFactoryType> Type of converter factory to be registered here
 * @param <RegistryImpl> Implementing class, used for proper method chaining
 * @since 3.6
 */
public abstract class AbstractConverterFactoryRegistry<ST, DT, ConverterFactoryType extends ConverterFactory<ST, DT>, RegistryImpl extends AbstractConverterFactoryRegistry<ST, DT, ConverterFactoryType, RegistryImpl>> {

    private final static NodeLogger LOGGER = NodeLogger.getLogger(AbstractConverterFactoryRegistry.class);

    /** Converter factories stored by SourceType/DestType/FactoryName triple */
    protected final HashMap<ConversionKey, ArrayList<ConverterFactoryType>> m_factories = new HashMap<>();

    /** Factories stored by destination type */
    protected final HashMap<DT, Set<ConverterFactoryType>> m_byDestinationType = new HashMap<>();

    /** Factories stored by source type */
    protected final HashMap<ST, Set<ConverterFactoryType>> m_bySourceType = new HashMap<>();

    /** Factories stored by identifier */
    protected final HashMap<String, ConverterFactoryType> m_byIdentifier = new HashMap<>();

    /**
     * Parent of this registry. If a converter factory is not found in the registry, this registry will be queried next.
     */
    protected RegistryImpl m_parent = null;

    /**
     * Get all registered converter factories.
     *
     * @return Collection of registered converter factories.
     * @since 3.3
     */
    public Collection<ConverterFactoryType> getAllConverterFactories() {
        final Set<ConverterFactoryType> factories =
            m_factories.values().stream().flatMap(factoryList -> factoryList.stream()).collect(Collectors.toSet());
        if (m_parent != null) {
            factories.addAll(m_parent.getAllConverterFactories());
        }
        return factories;
    }

    /**
     * Query which types can be converted.
     *
     * @return a {@link Collection} of all possible source types
     */
    public Collection<ST> getAllSourceTypes() {
        final Set<ST> sourceTypes = m_factories.values().stream().flatMap(factoryList -> factoryList.stream())
            .map(factory -> factory.getSourceType()).collect(Collectors.toSet());
        if (m_parent != null) {
            sourceTypes.addAll(m_parent.getAllSourceTypes());
        }
        return sourceTypes;
    }

    /**
     * Query into which types can be converted.
     *
     * @return a {@link Collection} of all possible source types
     */
    public Collection<DT> getAllDestinationTypes() {
        final Set<DT> destTypes = m_factories.values().stream().flatMap(factoryList -> factoryList.stream())
            .map(factory -> factory.getDestinationType()).collect(Collectors.toSet());
        if (m_parent != null) {
            destTypes.addAll(m_parent.getAllDestinationTypes());
        }
        return destTypes;
    }

    /**
     * Register a converter factory.
     *
     * @param factory the factory to register
     * @return self (for method chaining)
     */
    public RegistryImpl register(final ConverterFactoryType factory) {
        if (factory == null) {
            throw new IllegalArgumentException("factory must not be null");
        }

        final ConversionKey key = new ConversionKey(factory);
        ArrayList<ConverterFactoryType> list = m_factories.get(key);
        if (list == null) {
            list = new ArrayList<>();
            m_factories.put(key, list);
        }
        list.add(factory);

        final DT destType = factory.getDestinationType();
        Set<ConverterFactoryType> byDestType = m_byDestinationType.get(destType);
        if (byDestType == null) {
            byDestType = new LinkedHashSet<>();
            m_byDestinationType.put(destType, byDestType);
        }
        byDestType.add(factory);

        final ST sourceType = factory.getSourceType();
        Set<ConverterFactoryType> bySourceType = m_bySourceType.get(sourceType);
        if (bySourceType == null) {
            bySourceType = new LinkedHashSet<>();
            m_bySourceType.put(sourceType, bySourceType);
        }
        bySourceType.add(factory);

        final ConverterFactoryType previous = m_byIdentifier.put(factory.getIdentifier(), factory);
        if (previous != null) {
            LOGGER.coding("Factory identifier is not unique (" + factory.getIdentifier() + ")");
        }

        return (RegistryImpl)this;
    }

    /**
     * Get the converter factory associated with the given identifier.
     *
     * @param id unique identifier for the factory
     * @return an optional converter factory
     */
    public Optional<ConverterFactoryType> getFactory(final String id) {
        if (id == null) {
            return Optional.empty();
        }

        final ConverterFactoryType factory = m_byIdentifier.get(id);

        if (factory == null) {
            return m_parent != null ? m_parent.getFactory(id) : Optional.empty();
        }

        return Optional.of(factory);
    }

    /**
     * Get all converter factories which create Converters which convert into a specific destination type.
     *
     * @param destType Destination type to query converter factories for
     * @return a {@link Collection} of all possible source types which can be converted into the given
     *         <code>destType</code>. The first is always the preferred type.
     */
    public Collection<ConverterFactoryType> getFactoriesForDestinationType(final DT destType) {
        final LinkedHashSet<ConverterFactoryType> set = new LinkedHashSet<>();

        final Set<ConverterFactoryType> types = m_byDestinationType.get(destType);
        if (types != null) {
            set.addAll(types);
        }

        if (m_parent != null) {
            set.addAll(m_parent.getFactoriesForDestinationType(destType));
        }

        return set;
    }

    /**
     * Get all converter factories which create converter which convert into a specific destType.
     *
     * @param sourceType Source type to query converter factories for
     * @return a {@link Collection} of all possible source types which can be converted into the given
     *         <code>destType</code>. The first is always the preferred type.
     */
    public Collection<ConverterFactoryType> getFactoriesForSourceType(final ST sourceType) {
        final LinkedHashSet<ConverterFactoryType> set = new LinkedHashSet<>();

        final Set<ConverterFactoryType> types = m_bySourceType.get(sourceType);
        if (types != null) {
            set.addAll(types);
        }

        if (m_parent != null) {
            set.addAll(m_parent.getFactoriesForSourceType(sourceType));
        }

        return set;
    }

    /**
     * Get all factories from given source to given destination type.
     *
     * If multiple converter factories are returned, they may do conversion in different lossless ways.
     * The factories are distinguishable by {@link ConverterFactory#getIdentifier()} and usually
     * also by {@link ConverterFactory#getName()}.
     *
     * @param sourceType Source type to convert
     * @param destType Destination type to convert into
     * @return Collection of suitable converter factories
     */
    public Collection<ConverterFactoryType> getFactories(final ST sourceType, final DT destType) {
        final ArrayList<ConverterFactoryType> factories = new ArrayList<>();

        final ArrayList<ConverterFactoryType> list = m_factories.get(new ConversionKey(sourceType, destType));
        if (list != null) {
            factories.addAll(list);
        }

        if (m_parent != null) {
            factories.addAll(m_parent.getFactoriesForSourceType(sourceType));
        }

        return factories;
    }
}
