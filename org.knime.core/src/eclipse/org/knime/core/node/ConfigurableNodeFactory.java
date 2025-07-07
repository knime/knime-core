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
 *   Oct 10, 2019 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.node;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.core.node.context.ModifiableNodeCreationConfiguration;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.context.ports.ExtendablePortGroup;
import org.knime.core.node.context.ports.ModifiablePortsConfiguration;
import org.knime.core.node.context.ports.PortGroupConfiguration;
import org.knime.core.node.context.ports.impl.DefaultExchangeablePortGroup;
import org.knime.core.node.context.ports.impl.DefaultExtendablePortGroup;
import org.knime.core.node.context.ports.impl.DefaultFixedPortGroup;
import org.knime.core.node.context.ports.impl.DefaultModifiablePortsConfiguration;
import org.knime.core.node.context.ports.impl.TypeBoundExtendablePortGroup;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;
import org.knime.core.node.util.CheckUtils;

/**
 * A node factory that supports the creation of nodes that support, e.g., configurable ports or file drag and drop.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 * @param <T> the concrete type of the {@link NodeModel}
 * @since 4.1
 */
public abstract class ConfigurableNodeFactory<T extends NodeModel> extends NodeFactory<T> {

    /**
     * Creates a new <code>ConfigurableNodeFactory</code> and initializes the node description.
     */
    protected ConfigurableNodeFactory() {
        super();
    }

    /**
     * Creates a new <code>ConfigurableNodeFactory</code> optionally with lazy initialization.
     *
     * @param lazyInitialization if set to <code>true</code> the full initialization is postponed until the
     *            {@link #init()} method is called.
     * @see NodeFactory#NodeFactory(boolean)
     * @since 5.0
     */
    protected ConfigurableNodeFactory(final boolean lazyInitialization) {
        super(lazyInitialization);
    }

    /**
     *
     * {@inheritDoc}
     *
     * @deprecated framework doesn't invoke this method for classes extending {@code ConfigurableNodeFactory}.
     */
    @Override
    @Deprecated
    public T createNodeModel() {
        // We cannot make this method final as it otherwise would break backwards compatibility w.r.t.
        // ContextAwareNodeFactory. Anyways, never invoked expect this method is explicitly called.
        throw new UnsupportedOperationException("Method invocation not supported by ConfigurableNodeFactory");
    }

    @Override
    @Deprecated
    protected NodeDialogPane createNodeDialogPane() {
        // We cannot make this method final as it otherwise would break backwards compatibility w.r.t.
        // ContextAwareNodeFactory. Anyways, never invoked expect this method is explicitly called.
        throw new UnsupportedOperationException("Method invocation not supported by ConfigurableNodeFactory");
    }

    /**
     * Creates the node creation configuration.
     *
     * @return the node creation configuration
     * @noreference This method is not intended to be referenced by clients.
     */
    public final ModifiableNodeCreationConfiguration createNodeCreationConfig() {
        return new ModifiableNodeCreationConfiguration(
            createPortsConfigBuilder().map(PortsConfigurationBuilder::build).orElse(null));
    }

    /**
     * Can the port configuration be controlled via the context menu on the node icon itself. This is true for most of
     * the nodes that implement this interface. It's false for nodes that derive their port configuration via the
     * workflows that they process (integrated deployment / workflow executor).
     *
     * @return <code>true</code> (overwritten in sub-classes)
     * @since 4.5
     * @deprecated Please use {@link ModifiablePortsConfiguration#isInteractive(String)} instead. This property is set
     *             on port group level instead of node level and thus allows for more fine-grained control.
     */
    @Deprecated(since = "5.0", forRemoval = true)
    public boolean isPortConfigurableViaMenu() {
        return createPortsConfigBuilder().isPresent();
    }

    /**
     * Returns an instance of {@code PortConfigurationBuilder} if the node supports configurable input, output or input
     * and output ports.
     *
     * @return an optional instance of {@link PortsConfigurationBuilder}
     */
    protected abstract Optional<PortsConfigurationBuilder> createPortsConfigBuilder();

    @Override
    protected abstract T createNodeModel(final NodeCreationConfiguration creationConfig);

    @Override
    protected abstract NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration creationConfig);

    /**
     * Builder to create an instance of {@linkplain PortGroupConfiguration a port group configurations}. The order in
     * which the port groups are added defines the node's port order.
     *
     * <p>
     * Five different port group types are available.
     * <ol>
     * <li>Fixed port group, i.e., port groups that cannot be modified by the user
     * <li>Optional port group, i.e., a port group with [0,1] ports. Initially the optional port is not set
     * <li>Extendable port group, i.e., a port group with [0,n] fixed ports and [0,n] additional ports
     * <li>Bound extendable port group, i.e., a port group with [0,n] fixed ports and [0,n] additional ports whose type
     * is bound to that of another port group
     * <li>Exchangebale port group, i.e., a port group with exactly one port whose port type can be exchanged
     * </ol>
     * Note that for all but the fixed port group a set of different ports can be defined from which the user can
     * choose.
     * </p>
     *
     * <p>
     * Every port group, independent of its type, can either specify input, output ports, or both. If an extendable port
     * group defines input as well as output ports adding a new port will create a new input port as well as output port
     * of the very same type.
     * <p>
     *
     * <p>
     * Example: The following code will create a node where
     * <ul>
     * <li>the first set of input ports is optional and can either be a BufferedDataTable or PMMLPort
     * <li>the second set of input ports is extendable, but has at least one port, and only BufferedDataTables can be
     * added
     * <li>the third set of input/output ports is fixed with one input-output port of type PMML (fist port within this
     * group) and one of type BufferedDataTable (second port within this group)
     * </ul>
     *
     * <pre>
     * <code>
     *     final PortsConfigurationBuilder builder = new PortsConfigurationBuilder();
     *     builder.addOptionalInputPortGroup("Optional input port example", BufferedDataTable.TYPE,
     *         PMMLPortObject.TYPE);
     *     builder.addExtendableInputPortGroup("Extendable input port example", new PortType[]{BufferedDataTable.TYPE},
     *         BufferedDataTable.TYPE);
     *     builder.addFixedInputPortGroup("Fixed input and output port example", PMMLPortObject.TYPE,
     *         BufferedDataTable.TYPE);
     * </code>
     * </pre>
     * </p>
     *
     * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
     */
    protected static final class PortsConfigurationBuilder {

        private final LinkedHashMap<String, PortGroupConfiguration> m_portConfigs;

        /** Contains the identifiers of the port groups that cannot be modified via the user interface. */
        private final HashSet<String> m_nonInteractivePortGroups = new HashSet<>();

        /**
         * Constructor.
         */
        public PortsConfigurationBuilder() {
            m_portConfigs = new LinkedHashMap<>();
        }

        /**
         * Creates the {@code ModifiablePortsConfiguration}.
         *
         * @return an instance of {@code ModifiablePortsConfiguration}
         * @noreference This method is not intended to be referenced by clients.
         */
        private ModifiablePortsConfiguration build() {
            return new DefaultModifiablePortsConfiguration(m_portConfigs, m_nonInteractivePortGroups);
        }

        /**
         * Adds a static input port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param fixedPortTypes the fixed port types
         */
        public void addFixedInputPortGroup(final String pGrpId, final PortType... fixedPortTypes) {
            addFixedPortGroup(pGrpId, fixedPortTypes, true, false);
        }

        /**
         * Adds a static output port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param fixedPortTypes the fixed port types
         */
        public void addFixedOutputPortGroup(final String pGrpId, final PortType... fixedPortTypes) {
            addFixedPortGroup(pGrpId, fixedPortTypes, false, true);
        }

        /**
         * Adds a static port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param fixedPortTypes the fixed port types
         */
        public void addFixedPortGroup(final String pGrpId, final PortType... fixedPortTypes) {
            addFixedPortGroup(pGrpId, fixedPortTypes, true, true);
        }

        private void addFixedPortGroup(final String pGrpId, final PortType[] fixedPortTypes,
            final boolean definesInputPorts, final boolean definesOutputPorts) {
            validateStaticPortGroupArguments(pGrpId, fixedPortTypes);
            m_portConfigs.put(pGrpId, new DefaultFixedPortGroup(fixedPortTypes, definesInputPorts, definesOutputPorts));
        }

        /**
         * Adds an extendable input port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param supportedTypes the supported port types
         */
        public void addExtendableInputPortGroup(final String pGrpId, final PortType... supportedTypes) {
            addExtendableInputPortGroup(pGrpId, new PortType[0], supportedTypes);
        }

        /**
         * Adds an extendable input port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param supportedTypesPredicate the predicate that identifies supported {@link PortType PortTypes}
         * @since 4.2
         */
        public void addExtendableInputPortGroup(final String pGrpId,
            final Predicate<PortType> supportedTypesPredicate) {
            addExtendableInputPortGroup(pGrpId, new PortType[0], supportedTypesPredicate);
        }

        /**
         * Adds an extendable input port group configuration that cannot be modified via the user interface (it can only
         * be modified internally, e.g., by the implementation of the node dialog).
         *
         * @param pGrpId the port group identifier
         * @param supportedTypesPredicate the predicate that identifies supported {@link PortType PortTypes}
         * @since 4.7
         */
        public void addNonInteractiveExtendableInputPortGroup(final String pGrpId,
            final Predicate<PortType> supportedTypesPredicate) {
            addExtendableInputPortGroup(pGrpId, new PortType[0], supportedTypesPredicate);
            m_nonInteractivePortGroups.add(pGrpId);
        }

        /**
         * Adds an extendable input port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param fixedPortTypes the fixed port types
         * @param supportedTypes the supported port types
         */
        public void addExtendableInputPortGroup(final String pGrpId, final PortType[] fixedPortTypes,
            final PortType... supportedTypes) {
            addExtendableInputPortGroupWithDefault(pGrpId, fixedPortTypes, new PortType[0], supportedTypes);
        }

        /**
         * Adds an extendable input port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param fixedPortTypes the fixed port types
         * @param supportedTypesPredicate the predicate that identifies supported {@link PortType PortTypes}
         * @since 4.2
         */
        public void addExtendableInputPortGroup(final String pGrpId, final PortType[] fixedPortTypes,
            final Predicate<PortType> supportedTypesPredicate) {
            addExtendableInputPortGroupWithDefault(pGrpId, fixedPortTypes, new PortType[0], supportedTypesPredicate);
        }

        /**
         * Adds an extendable input port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param fixedPortTypes the fixed port types
         * @param defaultPortTypes the default port types, that is, the ports that are present when creating the node
         *            for the first time but that can be removed by the user afterwards, unlike the fixed ports
         * @param supportedTypes the supported port types
         * @since 4.3
         */
        public void addExtendableInputPortGroupWithDefault(final String pGrpId, final PortType[] fixedPortTypes,
            final PortType[] defaultPortTypes, final PortType... supportedTypes) {
            addExtendablePortGroup(pGrpId, fixedPortTypes, defaultPortTypes, supportedTypes, true, false);
        }

        /**
         * Adds an extendable input port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param fixedPortTypes the fixed port types
         * @param defaultPortTypes the default port types, that is, the ports that are present when creating the node
         *            for the first time but that can be removed by the user afterwards, unlike the fixed ports
         * @param supportedTypesPredicate the predicate that identifies supported {@link PortType PortTypes}
         * @since 4.3
         */
        public void addExtendableInputPortGroupWithDefault(final String pGrpId, final PortType[] fixedPortTypes,
            final PortType[] defaultPortTypes, final Predicate<PortType> supportedTypesPredicate) {
            addExtendableInputPortGroupWithDefault(pGrpId, fixedPortTypes, defaultPortTypes,
                getSupportedTypes(supportedTypesPredicate));
        }

        /**
         * Adds an extendable output port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param supportedTypes the supported port types
         */
        public void addExtendableOutputPortGroup(final String pGrpId, final PortType... supportedTypes) {
            addExtendableOutputPortGroup(pGrpId, new PortType[0], supportedTypes);
        }

        /**
         * Adds an extendable output port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param supportedTypesPredicate the predicate that identifies supported {@link PortType PortTypes}
         * @since 4.2
         */
        public void addExtendableOutputPortGroup(final String pGrpId,
            final Predicate<PortType> supportedTypesPredicate) {
            addExtendableOutputPortGroup(pGrpId, new PortType[0], supportedTypesPredicate);
        }

        /**
         * Adds an extendable output port group configuration that cannot be modified via the user interface (it can
         * only be modified internally, e.g., by the implementation of the node dialog).
         *
         * @param pGrpId the port group identifier
         * @param supportedTypesPredicate the predicate that identifies supported {@link PortType PortTypes}
         * @since 4.7
         */
        public void addNonInteractiveExtendableOutputPortGroup(final String pGrpId,
            final Predicate<PortType> supportedTypesPredicate) {
            addExtendableOutputPortGroup(pGrpId, new PortType[0], supportedTypesPredicate);
            m_nonInteractivePortGroups.add(pGrpId);
        }

        /**
         * Adds an extendable output port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param fixedPortTypes the fixed port types
         * @param supportedTypes the supported port types
         */
        public void addExtendableOutputPortGroup(final String pGrpId, final PortType[] fixedPortTypes,
            final PortType... supportedTypes) {
            addExtendableOutputPortGroupWithDefault(pGrpId, fixedPortTypes, new PortType[0], supportedTypes);
        }

        /**
         * Adds an extendable output port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param fixedPortTypes the fixed port types
         * @param supportedTypesPredicate the predicate that identifies supported {@link PortType PortTypes}
         * @since 4.2
         */
        public void addExtendableOutputPortGroup(final String pGrpId, final PortType[] fixedPortTypes,
            final Predicate<PortType> supportedTypesPredicate) {
            addExtendableOutputPortGroupWithDefault(pGrpId, fixedPortTypes, new PortType[0], supportedTypesPredicate);
        }

        /**
         * Adds an extendable output port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param fixedPortTypes the fixed port types
         * @param defaultPortTypes the default port types, that is, the ports that are present when creating the node
         *            for the first time but that can be removed by the user afterwards, unlike the fixed ports
         * @param supportedTypes the supported port types
         * @since 4.3
         */
        public void addExtendableOutputPortGroupWithDefault(final String pGrpId, final PortType[] fixedPortTypes,
            final PortType[] defaultPortTypes, final PortType... supportedTypes) {
            addExtendablePortGroup(pGrpId, fixedPortTypes, defaultPortTypes, supportedTypes, false, true);
        }

        /**
         * Adds an extendable output port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param fixedPortTypes the fixed port types
         * @param defaultPortTypes the default port types, that is, the ports that are present when creating the node
         *            for the first time but that can be removed by the user afterwards, unlike the fixed ports
         * @param supportedTypesPredicate the predicate that identifies supported {@link PortType PortTypes}
         * @since 4.3
         */
        public void addExtendableOutputPortGroupWithDefault(final String pGrpId, final PortType[] fixedPortTypes,
            final PortType[] defaultPortTypes, final Predicate<PortType> supportedTypesPredicate) {
            addExtendableOutputPortGroupWithDefault(pGrpId, fixedPortTypes, defaultPortTypes,
                getSupportedTypes(supportedTypesPredicate));
        }

        /**
         * Adds an extendable port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param supportedTypes the supported port types
         */
        public void addExtendablePortGroup(final String pGrpId, final PortType... supportedTypes) {
            addExtendablePortGroup(pGrpId, new PortType[0], supportedTypes);
        }

        /**
         * Adds an extendable port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param supportedTypesPredicate the predicate that identifies supported {@link PortType PortTypes}
         * @since 4.2
         */
        public void addExtendablePortGroup(final String pGrpId, final Predicate<PortType> supportedTypesPredicate) {
            addExtendablePortGroup(pGrpId, new PortType[0], supportedTypesPredicate);
        }

        /**
         * Adds an extendable port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param fixedPortTypes the fixed port types
         * @param supportedTypes the supported port types
         */
        public void addExtendablePortGroup(final String pGrpId, final PortType[] fixedPortTypes,
            final PortType... supportedTypes) {
            addExtendablePortGroupWithDefault(pGrpId, fixedPortTypes, new PortType[0], supportedTypes);
        }

        /**
         * Adds an extendable port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param fixedPortTypes the fixed port types
         * @param supportedTypesPredicate the predicate that identifies supported {@link PortType PortTypes}
         * @since 4.2
         */
        public void addExtendablePortGroup(final String pGrpId, final PortType[] fixedPortTypes,
            final Predicate<PortType> supportedTypesPredicate) {
            addExtendablePortGroupWithDefault(pGrpId, fixedPortTypes, new PortType[0], supportedTypesPredicate);
        }

        /**
         * Adds an extendable port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param fixedPortTypes the fixed port types
         * @param defaultPortTypes the default port types, that is, the ports that are present when creating the node
         *            for the first time but that can be removed by the user afterwards, unlike the fixed ports
         * @param supportedTypes the supported port types
         * @since 4.3
         */
        public void addExtendablePortGroupWithDefault(final String pGrpId, final PortType[] fixedPortTypes,
            final PortType[] defaultPortTypes, final PortType... supportedTypes) {
            addExtendablePortGroup(pGrpId, fixedPortTypes, defaultPortTypes, supportedTypes, true, true);
        }

        /**
         * Adds an extendable port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param fixedPortTypes the fixed port types
         * @param defaultPortTypes the default port types, that is, the ports that are present when creating the node
         *            for the first time but that can be removed by the user afterwards, unlike the fixed ports
         * @param supportedTypesPredicate the predicate that identifies supported {@link PortType PortTypes}
         * @since 4.3
         */
        public void addExtendablePortGroupWithDefault(final String pGrpId, final PortType[] fixedPortTypes,
            final PortType[] defaultPortTypes, final Predicate<PortType> supportedTypesPredicate) {
            addExtendablePortGroupWithDefault(pGrpId, fixedPortTypes, defaultPortTypes,
                getSupportedTypes(supportedTypesPredicate));
        }

        private static PortType[] getSupportedTypes(final Predicate<PortType> predicate) {
            return PortTypeRegistry.getInstance().availablePortTypes().stream().filter(predicate)
                .toArray(PortType[]::new);
        }

        private void addExtendablePortGroup(final String pGrpId, final PortType[] fixedPortTypes,
            final PortType[] defaultPortTypes, final PortType[] supportedTypes, final boolean definesInputPorts,
            final boolean definesOutputPorts) {
            validateExtendablePortGroupArguments(pGrpId, fixedPortTypes, defaultPortTypes, supportedTypes);
            final ExtendablePortGroup group =
                new DefaultExtendablePortGroup(fixedPortTypes, supportedTypes, definesInputPorts, definesOutputPorts);
            for (final PortType port : defaultPortTypes) {
                group.addPort(port);
            }
            m_portConfigs.put(pGrpId, group);
        }

        /**
         * @param clientTypes port types to find subtypes for
         * @return non-hidden port types from the port type registry that are a subtype of one of the given client types
         *         and have no other registered subtypes (most special subtypes)
         */
        private static PortType[] getRegisteredPortTypes(final PortType[] clientTypes) {
            // find all port types that are a subtype of a client type and not hidden
            final var availablePortTypes = PortTypeRegistry.getInstance().availablePortTypes();
            return Arrays.stream(clientTypes) //
                .flatMap(clientType -> availablePortTypes.stream() //
                    .filter(type -> !type.isHidden()) //
                    .filter(clientType::isSuperTypeOf)) //
                .distinct() //
                .toArray(PortType[]::new);
        }

        /**
         * Adds an optional input port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param optionalPorts the optional port types
         */
        public void addOptionalInputPortGroup(final String pGrpId, final PortType... optionalPorts) {
            addOptionalInputPortGroupWithDefault(pGrpId, null, optionalPorts);
        }

        /**
         * Adds an optional input port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param optionalPortsPredicate the predicate identifying the optional port types
         * @since 4.2
         */
        public void addOptionalInputPortGroup(final String pGrpId, final Predicate<PortType> optionalPortsPredicate) {
            addOptionalInputPortGroupWithDefault(pGrpId, null, optionalPortsPredicate);
        }

        /**
         * Adds an optional input port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param defaultPort the default port type, that is, the port that is present when creating the node for the
         *            first time but that can be removed by the user afterwards.
         * @param optionalPorts the optional port types
         * @since 4.3
         */
        public void addOptionalInputPortGroupWithDefault(final String pGrpId, final PortType defaultPort,
            final PortType... optionalPorts) {
            addOptionalPortGroup(pGrpId, defaultPort, optionalPorts, true, false);
        }

        /**
         * Adds an optional input port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param defaultPort the default port type, that is, the port that is present when creating the node for the
         *            first time but that can be removed by the user afterwards.
         * @param optionalPortsPredicate the predicate identifying the optional port types
         * @since 4.3
         */
        public void addOptionalInputPortGroupWithDefault(final String pGrpId, final PortType defaultPort,
            final Predicate<PortType> optionalPortsPredicate) {
            addOptionalInputPortGroupWithDefault(pGrpId, defaultPort, getSupportedTypes(optionalPortsPredicate));
        }

        /**
         * Adds an optional output port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param optionalPorts the optional port types
         */
        public void addOptionalOutputPortGroup(final String pGrpId, final PortType... optionalPorts) {
            addOptionalOutputPortGroupWithDefault(pGrpId, null, optionalPorts);
        }

        /**
         * Adds an optional output port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param optionalPortsPredicate the predicate identifying the optional port types
         * @since 4.2
         */
        public void addOptionalOutputPortGroup(final String pGrpId, final Predicate<PortType> optionalPortsPredicate) {
            addOptionalOutputPortGroupWithDefault(pGrpId, null, optionalPortsPredicate);
        }

        /**
         * Adds an optional output port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param defaultPort the default port type, that is, the port that is present when creating the node for the
         *            first time but that can be removed by the user afterwards.
         * @param optionalPorts the optional port types
         * @since 4.3
         */
        public void addOptionalOutputPortGroupWithDefault(final String pGrpId, final PortType defaultPort,
            final PortType... optionalPorts) {
            addOptionalPortGroup(pGrpId, defaultPort, optionalPorts, false, true);
        }

        /**
         * Adds an optional output port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param defaultPort the default port type, that is, the port that is present when creating the node for the
         *            first time but that can be removed by the user afterwards.
         * @param optionalPortsPredicate the predicate identifying the optional port types
         * @since 4.3
         */
        public void addOptionalOutputPortGroupWithDefault(final String pGrpId, final PortType defaultPort,
            final Predicate<PortType> optionalPortsPredicate) {
            addOptionalOutputPortGroupWithDefault(pGrpId, defaultPort, getSupportedTypes(optionalPortsPredicate));
        }

        /**
         * Adds an optional port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param optionalPorts the optional port types
         */
        public void addOptionalPortGroup(final String pGrpId, final PortType... optionalPorts) {
            addOptionalPortGroupWithDefault(pGrpId, null, optionalPorts);
        }

        /**
         * Adds an optional port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param optionalPortsPredicate the predicate identifying optional port types
         * @since 4.2
         */
        public void addOptionalPortGroup(final String pGrpId, final Predicate<PortType> optionalPortsPredicate) {
            addOptionalPortGroupWithDefault(pGrpId, null, optionalPortsPredicate);
        }

        /**
         * Adds an optional port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param defaultPort the default port type, that is, the port that is present when creating the node for the
         *            first time but that can be removed by the user afterwards.
         * @param optionalPorts the optional port types
         * @since 4.3
         */
        public void addOptionalPortGroupWithDefault(final String pGrpId, final PortType defaultPort,
            final PortType... optionalPorts) {
            addOptionalPortGroup(pGrpId, defaultPort, optionalPorts, true, true);
        }

        /**
         * Adds an optional port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param defaultPort the default port type, that is, the port that is present when creating the node for the
         *            first time but that can be removed by the user afterwards.
         * @param optionalPortsPredicate the predicate identifying the optional port types
         * @since 4.3
         */
        public void addOptionalPortGroupWithDefault(final String pGrpId, final PortType defaultPort,
            final Predicate<PortType> optionalPortsPredicate) {
            addOptionalPortGroupWithDefault(pGrpId, defaultPort, getSupportedTypes(optionalPortsPredicate));
        }

        private void addOptionalPortGroup(final String pGrpId, final PortType defaultPort,
            final PortType[] optionalPorts, final boolean definesInputPorts, final boolean definesOutputPorts) {
            validateOptionalPortGroupArguments(pGrpId, defaultPort, optionalPorts);
            final ExtendablePortGroup group = new DefaultExtendablePortGroup(new PortType[0],
                getRegisteredPortTypes(optionalPorts), definesInputPorts, definesOutputPorts, 1);
            if (defaultPort != null) {
                group.addPort(defaultPort);
            }
            m_portConfigs.put(pGrpId, group);
        }

        private void validatePortGrpIdentifier(final String pGrpId) {
            CheckUtils.checkArgument(!m_portConfigs.containsKey(pGrpId),
                "A port group with the given name already exists");
            CheckUtils.checkArgument(pGrpId != null && !pGrpId.isEmpty(),
                "The group identifier has to contain at least one character");
        }

        private void validateStaticPortGroupArguments(final String pGrpId, final PortType... staticPortTypes) {
            validatePortGrpIdentifier(pGrpId);
            checkArgumentArrayNotNull(staticPortTypes,
                "The static port types cannot be null and cannot contain null entries");
            CheckUtils.checkArgument(staticPortTypes != null && staticPortTypes.length > 0,
                "At least one static port type has to be set");
        }

        private void validateExtendablePortGroupArguments(final String pGrpId, final PortType[] staticPorts,
            final PortType[] defaultPorts, final PortType[] supportedTypes) {
            validatePortGrpIdentifier(pGrpId);
            checkArgumentArrayNotNull(staticPorts, "The static ports cannot be null and cannot contain null entries");
            checkArgumentArrayNotNull(defaultPorts, "The default ports cannot be null and cannot contain null entries");
            checkArgumentArrayNotNull(supportedTypes,
                "The supported port types cannot be null and cannot contain null entries");
            CheckUtils.checkArgument(supportedTypes != null && supportedTypes.length > 0,
                "The supported port types have to contain at least one element");
            checkSupportedTypesContainPorts(supportedTypes, staticPorts,
                "The supported port types have to contain all the static port types");
            checkSupportedTypesContainPorts(supportedTypes, defaultPorts,
                "The supported port types have to contain all the default port types");
        }

        private static <T> void checkSupportedTypesContainPorts(final T[] supportedTypes, final T[] ports,
            final String message) {
            if (!new HashSet<>(Arrays.asList(supportedTypes)).containsAll(Arrays.asList(ports))) {
                throw new IllegalArgumentException(message);
            }
        }

        private void validateOptionalPortGroupArguments(final String pGrpId, final PortType defaultPort,
            final PortType... optionalPorts) {
            validatePortGrpIdentifier(pGrpId);
            checkArgumentArrayNotNull(optionalPorts,
                "The optional ports cannot be null and cannot contain null entries");
            CheckUtils.checkArgument(optionalPorts != null && optionalPorts.length > 0,
                "The optional ports have to contain at least one element");
            CheckUtils.checkArgument(defaultPort == null || ArrayUtils.contains(optionalPorts, defaultPort),
                "The optional port types have to contain the default port type");
        }

        /**
         * Adds a bound extendable input port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param pBndGrpId the name of the group this group should be bound to (the first encountered supported type
         *            will be bound)
         * @since 4.5
         */
        public void addBoundExtendableInputPortGroup(final String pGrpId, final String pBndGrpId) {
            addBoundExtentendablePortWithDefault(pGrpId, pBndGrpId, 0, 0, true, false, Integer.MAX_VALUE);
        }

        /**
         * Adds a bound extendable input port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param pBndGrpId the name of the group this group should be bound to (the first encountered supported type
         *            will be bound)
         * @param fixedPortNumber the number of fixed ports if there is a type
         * @since 4.5
         */
        public void addBoundExtendableInputPortGroup(final String pGrpId, final String pBndGrpId,
            final int fixedPortNumber) {
            addBoundExtentendablePortWithDefault(pGrpId, pBndGrpId, fixedPortNumber, 0, true, false,
                Integer.MAX_VALUE - fixedPortNumber);
        }

        /**
         * Adds a bound extendable output port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param pBndGrpId the name of the group this group should be bound to (the first encountered supported type
         *            will be bound)
         * @param fixedPortNumber the number of fixed ports if there is a type
         * @param defaultPortNumber the number of default ports, that is, the ports that are present if the node gets a
         *            valid type for the first time, that can, unlike fixed ports removed by the user
         * @since 4.5
         */
        public void addBoundExtendableInputPortGroupWithDefault(final String pGrpId, final String pBndGrpId,
            final int fixedPortNumber, final int defaultPortNumber) {
            addBoundExtentendablePortWithDefault(pGrpId, pBndGrpId, fixedPortNumber, defaultPortNumber, true, false,
                Integer.MAX_VALUE - fixedPortNumber);
        }

        /**
         * Adds a bound extendable output port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param pBndGrpId the name of the group this group should be bound to (the first encountered supported type
         *            will be bound)
         * @since 4.5
         */
        public void addBoundExtendableOutputPortGroup(final String pGrpId, final String pBndGrpId) {
            addBoundExtentendablePortWithDefault(pGrpId, pBndGrpId, 0, 0, false, true, Integer.MAX_VALUE);
        }

        /**
         * Adds a bound extendable output port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param pBndGrpId the name of the group this group should be bound to (the first encountered supported type
         *            will be bound)
         * @param fixedPortNumber the number of fixed ports if there is a type
         * @since 4.5
         */
        public void addBoundExtendableOutputPortGroup(final String pGrpId, final String pBndGrpId,
            final int fixedPortNumber) {
            addBoundExtentendablePortWithDefault(pGrpId, pBndGrpId, fixedPortNumber, 0, false, true,
                Integer.MAX_VALUE - fixedPortNumber);
        }

        /**
         * Adds a bound extendable output port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param pBndGrpId the name of the group this group should be bound to (the first encountered supported type
         *            will be bound)
         * @param fixedPortNumber the number of fixed ports if there is a type
         * @param defaultPortNumber the number of default ports, that is, the ports that are present if the node gets a
         *            valid type for the first time, that can, unlike fixed ports removed by the user
         * @since 4.5
         */
        public void addBoundExtendableOutputPortGroupWithDefault(final String pGrpId, final String pBndGrpId,
            final int fixedPortNumber, final int defaultPortNumber) {
            addBoundExtentendablePortWithDefault(pGrpId, pBndGrpId, fixedPortNumber, defaultPortNumber, false, true,
                Integer.MAX_VALUE - fixedPortNumber);
        }

        /**
         * Adds a bound extendable port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param pBndGrpId the name of the group this group should be bound to (the first encountered supported type
         *            will be bound)
         * @since 4.5
         */
        public void addBoundExtendablePortGroup(final String pGrpId, final String pBndGrpId) {
            addBoundExtentendablePortWithDefault(pGrpId, pBndGrpId, 0, 0, true, true, Integer.MAX_VALUE);
        }

        /**
         * Adds a bound extendable port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param pBndGrpId the name of the group this group should be bound to (the first encountered supported type
         *            will be bound)
         * @param fixedPortNumber the number of fixed ports if there is a type
         * @since 4.5
         */
        public void addBoundExtendablePortGroup(final String pGrpId, final String pBndGrpId,
            final int fixedPortNumber) {
            addBoundExtentendablePortWithDefault(pGrpId, pBndGrpId, fixedPortNumber, 0, true, true,
                Integer.MAX_VALUE - fixedPortNumber);
        }

        /**
         * Adds a bound extendable port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param pBndGrpId the name of the group this group should be bound to (the first encountered supported type
         *            will be bound)
         * @param fixedPortNumber the number of fixed ports if there is a type
         * @param defaultPortNumber the number of default ports, that is, the ports that are present if the node gets a
         *            valid type for the first time, that can, unlike fixed ports removed by the user
         * @since 4.5
         */
        public void addBoundExtendablePortGroupWithDefault(final String pGrpId, final String pBndGrpId,
            final int fixedPortNumber, final int defaultPortNumber) {
            addBoundExtentendablePortWithDefault(pGrpId, pBndGrpId, fixedPortNumber, defaultPortNumber, true, true,
                Integer.MAX_VALUE - fixedPortNumber);
        }

        private void addBoundExtentendablePortWithDefault(final String pGrpId, final String pBndGrpId,
            final int fixedPortNumber, final int defaultPortNumber, final boolean definesInputPorts,
            final boolean definesOutputPorts, final int maxAdditionalPorts) {
            validateBoundExtendablePortGroup(pGrpId, pBndGrpId, fixedPortNumber, defaultPortNumber, maxAdditionalPorts);
            final var group = new TypeBoundExtendablePortGroup(pBndGrpId, m_portConfigs, fixedPortNumber,
                defaultPortNumber, definesInputPorts, definesOutputPorts, maxAdditionalPorts);
            m_portConfigs.put(pGrpId, group);

        }

        private void validateBoundExtendablePortGroup(final String pGrpId, final String pBndGrpId,
            final int fixedPortNumber, final int defaultPortNumber, final int maxAdditionalPorts) {
            validatePortGrpIdentifier(pGrpId);
            CheckUtils.checkArgument(m_portConfigs.containsKey(pBndGrpId),
                "A port group to bind with that given name does not exists");
            CheckUtils.checkArgument(fixedPortNumber >= 0, "Number of fixed ports must be positive");
            CheckUtils.checkArgument(defaultPortNumber >= 0, "Number of default ports must be positive");
            CheckUtils.checkArgument(maxAdditionalPorts >= 0, "Maximum number of additional ports must be positive");
            CheckUtils.checkArgument(maxAdditionalPorts + fixedPortNumber >= 0,
                "Too many fixed and additional ports combined");
            CheckUtils.checkArgument(defaultPortNumber <= maxAdditionalPorts,
                "The number of default ports must be smaller than the number of maximum number of additional ports!");
        }

        /**
         * Adds an exchangeable input port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param defaultType the default port type
         * @param supportedTypes the supported port types (has to include the default type itself)
         * @since 4.2
         */
        public void addExchangeableInputPortGroup(final String pGrpId, final PortType defaultType,
            final PortType... supportedTypes) {
            addExchangeablePortGroup(pGrpId, defaultType, supportedTypes, true, false);
        }

        /**
         * Adds an exchangeable input port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param defaultType the default port type
         * @param supportedTypesPredicate the predicate that identifies supported {@link PortType PortTypes}
         * @since 5.6
         */
        public void addExchangeableInputPortGroup(final String pGrpId, final PortType defaultType,
            final Predicate<PortType> supportedTypesPredicate) {
            addExchangeableInputPortGroup(pGrpId, defaultType, getSupportedTypes(supportedTypesPredicate));
        }

        /**
         * Adds an exchangeable output port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param defaultType the default port type
         * @param supportedTypes the supported port types (has to include the default type itself)
         * @since 4.2
         */
        public void addExchangeableOutputPortGroup(final String pGrpId, final PortType defaultType,
            final PortType... supportedTypes) {
            addExchangeablePortGroup(pGrpId, defaultType, supportedTypes, false, true);
        }

        /**
         * Adds an exchangeable output port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param defaultType the default port type
         * @param supportedTypesPredicate the predicate that identifies supported {@link PortType PortTypes}
         * @since 5.6
         */
        public void addExchangeableOutputPortGroup(final String pGrpId, final PortType defaultType,
            final Predicate<PortType> supportedTypesPredicate) {
            addExchangeableOutputPortGroup(pGrpId, defaultType, getSupportedTypes(supportedTypesPredicate));
        }

        /**
         * Adds an exchangeable port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param defaultType the default port type
         * @param supportedTypes the supported port types (has to include the default type itself)
         * @since 4.2
         */
        public void addExchangeablePortGroup(final String pGrpId, final PortType defaultType,
            final PortType... supportedTypes) {
            addExchangeablePortGroup(pGrpId, defaultType, supportedTypes, true, true);
        }

        /**
         * Adds an exchangeable port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param defaultType the default port type
         * @param supportedTypesPredicate the predicate that identifies supported {@link PortType PortTypes}
         * @since 5.6
         */
        public void addExchangeablePortGroup(final String pGrpId, final PortType defaultType,
            final Predicate<PortType> supportedTypesPredicate) {
            addExchangeablePortGroup(pGrpId, defaultType, getSupportedTypes(supportedTypesPredicate));
        }

        private void addExchangeablePortGroup(final String pGrpId, final PortType defaultType,
            final PortType[] supportedTypes, final boolean definesInputPort, final boolean definesOutputPort) {
            validateExchangeablePortGroupArguments(pGrpId, defaultType, supportedTypes);
            m_portConfigs.put(pGrpId,
                new DefaultExchangeablePortGroup(defaultType, supportedTypes, definesInputPort, definesOutputPort));
        }

        private void validateExchangeablePortGroupArguments(final String pGrpId, final PortType defaultType,
            final PortType... supportedTypes) {
            validatePortGrpIdentifier(pGrpId);
            CheckUtils.checkArgumentNotNull(defaultType, "The default port type cannot be null");
            checkArgumentArrayNotNull(supportedTypes,
                "The supported port types cannot be null and cannot contain null entries");
            CheckUtils.checkArgument(supportedTypes != null && supportedTypes.length > 1,
                "The supported types have to contain at least two elements");
            CheckUtils.checkArgument(ArrayUtils.contains(supportedTypes, defaultType),
                "The supported port types have to contain the default port type");
        }

        private static <T> void checkArgumentArrayNotNull(final T[] arrayToCheck, final String message) {
            CheckUtils.checkArgumentNotNull(arrayToCheck, message);
            if (Arrays.stream(arrayToCheck).anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException(message);
            }
        }
    }

    /**
     * {@link NodeDialogPane}s implementing this interface will gain control over the node creation. That is, e.g., port
     * configurations (adding/removing ports) can be adjusted in a dialog and applied as soon as the dialog is closed.
     *
     * When a dialog configuration is applied (or the dialog is closed), the respective node will essentially be
     * deleted, a new node created based on the (new) {@link ModifiableNodeCreationConfiguration} and the (new) settings
     * transfered.
     *
     * @since 4.2
     */
    public static interface ConfigurableNodeDialog {

        /**
         * Provides the current node creation configuration. Will be called shortly before the is dialog is opened.
         *
         * @param config
         */
        void setCurrentNodeCreationConfiguration(ModifiableNodeCreationConfiguration config);

        /**
         * The dialog provides the new node creation configuration here. Will be called and therewith applied right
         * after the dialog has been closed (or the dialog config applied).
         *
         * @return the new node creation configuration or an empty optional if no change should be done to the node
         */
        Optional<ModifiableNodeCreationConfiguration> getNewNodeCreationConfiguration();
    }
}
