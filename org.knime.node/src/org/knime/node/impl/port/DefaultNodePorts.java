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
 *   Jul 1, 2025 (Paul Bärnreuther): created
 */
package org.knime.node.impl.port;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.knime.core.node.port.PortType;
import org.knime.node.RequirePorts;
import org.knime.node.RequirePorts.ConfigurablePort;
import org.knime.node.RequirePorts.ConfigurablePort.BoundExtendableConfigurablePort;
import org.knime.node.RequirePorts.ConfigurablePort.ExtendableConfigurablePort;
import org.knime.node.RequirePorts.ConfigurablePort.FinalConfigurablePort;
import org.knime.node.RequirePorts.ConfigurablePort.OptionalConfigurablePort;
import org.knime.node.RequirePorts.DynamicPortsAdder;
import org.knime.node.RequirePorts.RequireConfigurablePortMode;
import org.knime.node.RequirePorts.RequireInputPortName;
import org.knime.node.RequirePorts.RequirePortName;
import org.knime.node.RequirePorts.RequireSupportedTypes;
import org.knime.node.impl.description.PortDescription;

/**
 * Implementation of {@link RequirePorts}.
 */
public class DefaultNodePorts {

    private final List<PortGroup> m_ports = new ArrayList<>();

    /**
     * Takes the consumer supplied in the fluent node api and extracts a list of port group implementations from it
     *
     * @param ports as required by {@link RequirePorts}.
     * @return the extracted port groups.
     */
    public List<PortGroup> extractPortDefinitions(final Consumer<DynamicPortsAdder> ports) {
        final DynamicPortsAdder adder = new DynamicPortsAdder() {

            @Override
            public DynamicPortsAdder addInputPortGroup(final String groupId,
                final Function<RequirePortName, ConfigurablePort> groupConfiguration) {
                groupConfiguration.apply(name -> description -> new AddPort(
                    g -> m_ports.add(new OneSidePortDefinition(PortGroup.PortLocation.INPUT,
                        new OneSidePortGroup(groupId, g, name, description)))));
                return this;
            }

            @Override
            public DynamicPortsAdder addOutputPortGroup(final String groupId,
                final Function<RequirePortName, ConfigurablePort> groupConfiguration) {
                groupConfiguration.apply(name -> description -> new AddPort(
                    g -> m_ports.add(new OneSidePortDefinition(PortGroup.PortLocation.OUTPUT,
                        new OneSidePortGroup(groupId, g, name, description)))));
                return this;
            }

            @Override
            public DynamicPortsAdder addInputAndOutputPortGroup(final String groupId,
                final Function<RequireInputPortName, ConfigurablePort> groupConfiguration) {
                groupConfiguration
                    .apply(inputName -> inputDescription -> outputName -> outputDescription -> new AddPort(
                        g -> m_ports.add(new BothSidesPortDefinition(groupId, g, inputName, inputDescription,
                            outputName, outputDescription))));
                return this;
            }
        };
        ports.accept(adder);
        return m_ports;
    }

    record OneSidePortDefinition(PortGroup.PortLocation location, OneSidePortGroup portGroup) implements PortGroup {

        @Override
        public void addToPortGroupConfiguration(final PortGroup.PortGroupConfigBuilderLambdas lambdas) {
            portGroup.addToPortGroupConfiguration(getLocation(), lambdas);
        }

        @Override
        public Optional<PortDescription> getDescription(final PortGroup.PortLocation targetLocation) {
            if (targetLocation == location) {
                return Optional.of(portGroup.getDescription());
            }
            return Optional.empty();
        }

        @Override
        public String getId() {
            return portGroup.id();
        }

        @Override
        public PortGroupLocation getLocation() {
            return switch (location) {
                case INPUT -> PortGroupLocation.INPUT;
                case OUTPUT -> PortGroupLocation.OUTPUT;
            };
        }
    }

    record OneSidePortGroup(String id, PortGroupConfiguration portGroupConfig, String name, String description) {

        PortDescription getDescription() {
            return new PortDescription(id, name, description, portGroupConfig.isConfigurable());
        }

        public void addToPortGroupConfiguration(final PortGroup.PortGroupLocation portLocation,
            final PortGroup.PortGroupConfigBuilderLambdas lambdas) {
            portGroupConfig.addToBuilder(lambdas, portLocation, id);
        }

    }

    record BothSidesPortDefinition(String id, PortGroupConfiguration portGroupConfig, String inputName,
        String inputDescription, String outputName, String outputDescription) implements PortGroup {

        @Override
        public Optional<PortDescription> getDescription(final PortGroup.PortLocation portLocation) {
            final var name = portLocation == PortLocation.INPUT ? inputName : outputName;
            final var description = portLocation == PortLocation.INPUT ? inputDescription : outputDescription;
            return Optional.of(new PortDescription(id, name, description, portGroupConfig.isConfigurable()));

        }

        @Override
        public void addToPortGroupConfiguration(final PortGroupConfigBuilderLambdas lambdas) {
            portGroupConfig.addToBuilder(lambdas, PortGroupLocation.BOTH_SIDES, id);
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public PortGroupLocation getLocation() {
            return PortGroupLocation.BOTH_SIDES;
        }

    }

    static final class AddPort implements RequireConfigurablePortMode {

        private final Consumer<PortGroupConfiguration> m_addPortGroup;

        AddPort(final Consumer<PortGroupConfiguration> addPortGroup) {
            m_addPortGroup = addPortGroup;

        }

        @Override
        public FinalConfigurablePort fixed(final PortType portType) {
            m_addPortGroup.accept(new FixedPortGroup(portType));
            return new FinalConfigurablePort();
        }

        @Override
        public RequireSupportedTypes<OptionalConfigurablePort> optional() {
            return new RequireSupportedTypesImpl<>(//
                OptionalPortGroup::new, //
                OptionalPortGroup::new, //
                m_addPortGroup, //
                OptionalConfigurablePortImpl::new//
            );
        }

        @Override
        public RequireSupportedTypes<ExtendableConfigurablePort> extendable() {
            return new RequireSupportedTypesImpl<>(//
                ExtendablePortGroup::new, //
                ExtendablePortGroup::new, //
                m_addPortGroup, //
                ExtendableConfigurablePortImpl::new//
            );

        }

        @Override
        public BoundExtendableConfigurablePort boundExtendable(final String boundGrpId) {
            final var portGroup = new BoundExtendablePortGroup(boundGrpId);
            m_addPortGroup.accept(portGroup);
            return new BoundExtendableConfigurablePortImpl(portGroup);
        }

        @Override
        public RequireSupportedTypes<FinalConfigurablePort> exchangable(final PortType defaultType) {
            return new RequireSupportedTypesImpl<>(//
                portTypes -> new ExchangablePortGroup(defaultType, portTypes), //
                portTypesPredicate -> new ExchangablePortGroup(defaultType, portTypesPredicate), //
                m_addPortGroup, //
                g -> new FinalConfigurablePort());
        }

    }

    static final class RequireSupportedTypesImpl<T, U extends PortGroupConfiguration>
        implements RequireSupportedTypes<T> {

        private final Function<PortType[], U> m_portGroupFromTypes;

        private final Function<Predicate<PortType>, U> m_portGroupFromPredicate;

        private final Function<U, T> m_createNextStage;

        private final Consumer<PortGroupConfiguration> m_addPortGroup;

        RequireSupportedTypesImpl(//
            final Function<PortType[], U> portGroupFromTypes, //
            final Function<Predicate<PortType>, U> portGroupFromPredicate, //
            final Consumer<PortGroupConfiguration> addPortGroup, //
            final Function<U, T> createNextStage //
        ) {
            m_portGroupFromTypes = portGroupFromTypes;
            m_portGroupFromPredicate = portGroupFromPredicate;
            m_addPortGroup = addPortGroup;
            m_createNextStage = createNextStage;
        }

        @Override
        public T supportedTypes(final Predicate<PortType> portTypePredicate) {
            final var portGroup = m_portGroupFromPredicate.apply(portTypePredicate);
            m_addPortGroup.accept(portGroup);
            return m_createNextStage.apply(portGroup);
        }

        @Override
        public T supportedTypes(final PortType... portTypes) {
            final var portGroup = m_portGroupFromTypes.apply(portTypes);
            m_addPortGroup.accept(portGroup);
            return m_createNextStage.apply(portGroup);
        }
    }

    static final class OptionalConfigurablePortImpl implements OptionalConfigurablePort {

        private final OptionalPortGroup m_portGroup;

        OptionalConfigurablePortImpl(final OptionalPortGroup portGroup) {
            m_portGroup = portGroup;
        }

        @Override
        public FinalConfigurablePort defaultPortType(final PortType defaultPortType) {
            m_portGroup.setDefaultPortType(defaultPortType);
            return new FinalConfigurablePort();
        }

    }

    static final class ExtendableConfigurablePortImpl implements ExtendableConfigurablePort {

        private final ExtendablePortGroup m_portGroup;

        ExtendableConfigurablePortImpl(final ExtendablePortGroup portGroup) {
            m_portGroup = portGroup;
        }

        @Override
        public ExtendableConfigurablePort fixedTypes(final PortType... portTypes) {
            if (portTypes == null || portTypes.length == 0) {
                throw new IllegalArgumentException("At least one fixed port type must be specified.");
            }
            m_portGroup.setFixedTypes(portTypes);
            return this;
        }

        @Override
        public ExtendableConfigurablePort defaultNonFixedTypes(final PortType... portTypes) {
            if (portTypes == null || portTypes.length == 0) {
                throw new IllegalArgumentException("At least one default non-fixed port type must be specified.");
            }
            m_portGroup.setDefaultNonFixedTypes(portTypes);
            return this;
        }
    }

    static final class BoundExtendableConfigurablePortImpl implements BoundExtendableConfigurablePort {

        private final BoundExtendablePortGroup m_portGroup;

        BoundExtendableConfigurablePortImpl(final BoundExtendablePortGroup portGroup) {
            m_portGroup = portGroup;
        }

        @Override
        public BoundExtendableConfigurablePort fixedNumPorts(final int numPorts) {
            m_portGroup.setFixedNumPorts(numPorts);
            return this;
        }

        @Override
        public BoundExtendableConfigurablePort defaultNumPorts(final int numPorts) {
            m_portGroup.setDefaultNumPorts(numPorts);
            return this;
        }

    }

    static final class FixedPortGroup implements PortGroupConfiguration {
        final PortType m_portType;

        FixedPortGroup(final PortType portType) {
            m_portType = portType;
        }

        @Override
        public boolean isConfigurable() {
            return false;
        }

        @Override
        public void addToBuilder(final PortGroup.PortGroupConfigBuilderLambdas lambdas,
            final PortGroup.PortGroupLocation location, final String groupId) {
            switch (location) {
                case INPUT -> lambdas.addFixedInputPortGroup().accept(m_portType);
                case OUTPUT -> lambdas.addFixedOutputPortGroup().accept(m_portType);
                case BOTH_SIDES -> lambdas.addFixedPortGroup().accept(m_portType);
            }
        }
    }

    static final class OptionalPortGroup extends PortGroupWithSupportedTypes {

        private PortType m_defaultPortType;

        OptionalPortGroup(final Predicate<PortType> supportedTypePredicate) {
            super(supportedTypePredicate);
        }

        OptionalPortGroup(final PortType[] supportedTypes) {
            super(supportedTypes);
        }

        void setDefaultPortType(final PortType defaultPortType) {
            m_defaultPortType = defaultPortType;

        }

        Optional<PortType> getDefaultPortType() {
            return Optional.ofNullable(m_defaultPortType);
        }

        @Override
        public void addToBuilder(final PortGroup.PortGroupConfigBuilderLambdas lambdas,
            final PortGroup.PortGroupLocation location, final String groupId, final PortType[] supportedTypes) {
            final var defaultType = getDefaultPortType().orElse(null);
            switch (location) {
                case INPUT -> lambdas.addOptionalInputPortGroupWithDefault().accept(defaultType, supportedTypes);
                case OUTPUT -> lambdas.addOptionalOutputPortGroupWithDefault().accept(defaultType, supportedTypes);
                case BOTH_SIDES -> lambdas.addOptionalPortGroupWithDefault().accept(defaultType, supportedTypes);
            }
        }

        @Override
        public void addToBuilder(final PortGroup.PortGroupConfigBuilderLambdas lambdas,
            final PortGroup.PortGroupLocation location, final String groupId,
            final Predicate<PortType> supportedTypesPredicate) {
            final var defaultType = getDefaultPortType().orElse(null);
            switch (location) {
                case INPUT -> lambdas.addOptionalInputPortGroupWithDefaultPredicate().accept(defaultType,
                    supportedTypesPredicate);
                case OUTPUT -> lambdas.addOptionalOutputPortGroupWithDefaultPredicate().accept(defaultType,
                    supportedTypesPredicate);
                case BOTH_SIDES -> lambdas.addOptionalPortGroupWithDefaultPredicate().accept(defaultType,
                    supportedTypesPredicate);
            }
        }
    }

    static final class ExtendablePortGroup extends PortGroupWithSupportedTypes {

        private PortType[] m_fixedTypes;

        private PortType[] m_defaultNonFixedTypes;

        ExtendablePortGroup(final Predicate<PortType> supportedTypePredicate) {
            super(supportedTypePredicate);
        }

        void setDefaultNonFixedTypes(final PortType[] portTypes) {
            m_defaultNonFixedTypes = portTypes;
        }

        public Optional<PortType[]> getDefaultNonFixedTypes() {
            return Optional.ofNullable(m_defaultNonFixedTypes);
        }

        void setFixedTypes(final PortType[] portTypes) {
            m_fixedTypes = portTypes;
        }

        public Optional<PortType[]> getFixedTypes() {
            return Optional.ofNullable(m_fixedTypes);
        }

        ExtendablePortGroup(final PortType[] supportedTypes) {
            super(supportedTypes);
        }

        @Override
        public void addToBuilder(final PortGroup.PortGroupConfigBuilderLambdas lambdas,
            final PortGroup.PortGroupLocation location, final String groupId, final PortType[] supportedTypes) {
            final var fixedTypes = getFixedTypes().orElse(new PortType[0]);
            final var defaultNonFixedTypes = getDefaultNonFixedTypes().orElse(new PortType[0]);
            switch (location) {
                case INPUT -> lambdas.addExtendableInputPortGroupWithDefault().accept(fixedTypes, defaultNonFixedTypes,
                    supportedTypes);
                case OUTPUT -> lambdas.addExtendableOutputPortGroupWithDefault().accept(fixedTypes,
                    defaultNonFixedTypes, supportedTypes);
                case BOTH_SIDES -> lambdas.addExtendablePortGroupWithDefault().accept(fixedTypes, defaultNonFixedTypes,
                    supportedTypes);
            }
        }

        @Override
        public void addToBuilder(final PortGroup.PortGroupConfigBuilderLambdas lambdas,
            final PortGroup.PortGroupLocation location, final String groupId,
            final Predicate<PortType> supportedTypesPredicate) {
            final var fixedTypes = getFixedTypes().orElse(new PortType[0]);
            final var defaultNonFixedTypes = getDefaultNonFixedTypes().orElse(new PortType[0]);
            switch (location) {
                case INPUT -> lambdas.addExtendableInputPortGroupWithDefaultPredicate().accept(fixedTypes,
                    defaultNonFixedTypes, supportedTypesPredicate);
                case OUTPUT -> lambdas.addExtendableOutputPortGroupWithDefaultPredicate().accept(fixedTypes,
                    defaultNonFixedTypes, supportedTypesPredicate);
                case BOTH_SIDES -> lambdas.addExtendablePortGroupWithDefaultPredicate().accept(fixedTypes,
                    defaultNonFixedTypes, supportedTypesPredicate);
            }
        }
    }

    static final class ExchangablePortGroup extends PortGroupWithSupportedTypes {
        final PortType m_defaultType;

        ExchangablePortGroup(final PortType defaultType, final Predicate<PortType> supportedTypePredicate) {
            super(supportedTypePredicate);
            m_defaultType = defaultType;
        }

        ExchangablePortGroup(final PortType defaultType, final PortType[] supportedTypes) {
            super(supportedTypes);
            m_defaultType = defaultType;
        }

        @Override
        public void addToBuilder(final PortGroup.PortGroupConfigBuilderLambdas lambdas,
            final PortGroup.PortGroupLocation location, final String groupId, final PortType[] supportedTypes) {
            switch (location) {
                case INPUT -> lambdas.addExchangeableInputPortGroup().accept(m_defaultType, supportedTypes);
                case OUTPUT -> lambdas.addExchangeableOutputPortGroup().accept(m_defaultType, supportedTypes);
                case BOTH_SIDES -> lambdas.addExchangeablePortGroup().accept(m_defaultType, supportedTypes);
            }
        }

        @Override
        public void addToBuilder(final PortGroup.PortGroupConfigBuilderLambdas lambdas,
            final PortGroup.PortGroupLocation location, final String groupId,
            final Predicate<PortType> supportedTypesPredicate) {
            switch (location) {
                case INPUT -> lambdas.addExchangeableInputPortGroupPredicate().accept(m_defaultType,
                    supportedTypesPredicate);
                case OUTPUT -> lambdas.addExchangeableOutputPortGroupPredicate().accept(m_defaultType,
                    supportedTypesPredicate);
                case BOTH_SIDES -> lambdas.addExchangeablePortGroupPredicate().accept(m_defaultType,
                    supportedTypesPredicate);
            }
        }
    }

    static final class BoundExtendablePortGroup implements PortGroupConfiguration {

        private int m_fixedNumPorts;

        private int m_defaultNumPorts;

        final String m_boundGrpId;

        BoundExtendablePortGroup(final String boundGrpId) {
            m_boundGrpId = boundGrpId;
        }

        void setFixedNumPorts(final int numPorts) {
            if (numPorts <= 0) {
                throw new IllegalArgumentException("Number of fixed ports must be non-negative.");
            }
            if (m_fixedNumPorts > 0) {
                throw new IllegalStateException("Fixed number of ports already set.");
            }
            m_fixedNumPorts = numPorts;
        }

        void setDefaultNumPorts(final int numPorts) {
            if (numPorts <= 0) {
                throw new IllegalArgumentException("Number of default ports must be positive.");
            }
            if (m_defaultNumPorts > 0) {
                throw new IllegalStateException("Default number of ports already set.");
            }
            m_defaultNumPorts = numPorts;
        }

        @Override
        public void addToBuilder(final PortGroup.PortGroupConfigBuilderLambdas lambdas,
            final PortGroup.PortGroupLocation location, final String groupId) {
            switch (location) {
                case INPUT -> lambdas.addBoundExtendableInputPortGroupWithDefault().accept(m_boundGrpId,
                    m_fixedNumPorts, m_defaultNumPorts);
                case OUTPUT -> lambdas.addBoundExtendableOutputPortGroupWithDefault().accept(m_boundGrpId,
                    m_fixedNumPorts, m_defaultNumPorts);
                case BOTH_SIDES -> lambdas.addBoundExtendablePortGroupWithDefault().accept(m_boundGrpId,
                    m_fixedNumPorts, m_defaultNumPorts);
            }
        }
    }

    static abstract class PortGroupWithSupportedTypes implements PortGroupConfiguration {

        final PortType[] m_supportedTypes;

        final Predicate<PortType> m_supportedTypesPredicate;

        PortGroupWithSupportedTypes(final PortType[] supportedTypes) {
            m_supportedTypes = supportedTypes;
            m_supportedTypesPredicate = null;
        }

        PortGroupWithSupportedTypes(final Predicate<PortType> supportedTypesPredicate) {
            m_supportedTypes = null;
            m_supportedTypesPredicate = supportedTypesPredicate;
        }

        @Override
        public final void addToBuilder(final PortGroup.PortGroupConfigBuilderLambdas lambdas,
            final PortGroup.PortGroupLocation location, final String groupId) {
            if (m_supportedTypes == null) {
                addToBuilder(lambdas, location, groupId, m_supportedTypesPredicate);
            } else {
                addToBuilder(lambdas, location, groupId, m_supportedTypes);
            }
        }

        abstract void addToBuilder(PortGroup.PortGroupConfigBuilderLambdas lambdas,
            PortGroup.PortGroupLocation location, String groupId, Predicate<PortType> supportedTypesPredicate);

        abstract void addToBuilder(PortGroup.PortGroupConfigBuilderLambdas lambdas,
            PortGroup.PortGroupLocation location, String groupId, PortType[] supportedTypes);

    }

    public interface PortGroupConfiguration {

        /**
         * Whether this port type is displayed as dynamic port in the node description.
         *
         * @return {@code true} if the port group is configurable, {@code false} otherwise
         */
        default boolean isConfigurable() {
            return true;
        }

        /**
         * TriConsumer is a functional interface that accepts three arguments and returns no result.
         */
        @SuppressWarnings("javadoc")
        @FunctionalInterface
        public interface TriConsumer<T, U, V> {
            void accept(T t, U u, V v);
        }

        /**
         * Add this port group configuration to the builder using the provided lambdas.
         *
         * @param lambdas the record of builder lambdas
         * @param location the port location
         * @param groupId the group id
         */
        void addToBuilder(final PortGroup.PortGroupConfigBuilderLambdas lambdas,
            final PortGroup.PortGroupLocation location, final String groupId);

    }

}
