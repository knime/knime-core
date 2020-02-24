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

import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.function.Predicate;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.core.node.context.ModifiableNodeCreationConfiguration;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.context.ports.ModifiablePortsConfiguration;
import org.knime.core.node.context.ports.PortGroupConfiguration;
import org.knime.core.node.context.ports.impl.DefaultExchangeablePortGroup;
import org.knime.core.node.context.ports.impl.DefaultExtendablePortGroup;
import org.knime.core.node.context.ports.impl.DefaultFixedPortGroup;
import org.knime.core.node.context.ports.impl.DefaultModifiablePortsConfiguration;
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
     * Three different port group types are available.
     * <ol>
     * <li>Fixed port group, i.e., port groups that cannot be modified by the user
     * <li>Optional port group, i.e., a port group with [0,1] ports. Initially the optional port is not set
     * <li>Extendable port group, i.e., a port group with [0,n] fixed ports and [0,n] additional ports
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
            return new DefaultModifiablePortsConfiguration(m_portConfigs);
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
            m_portConfigs.put(pGrpId,
                new DefaultFixedPortGroup(fixedPortTypes, definesInputPorts, definesOutputPorts));
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
         * Adds an extendable input port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param fixedPortTypes the fixed port types
         * @param supportedTypes the supported port types
         */
        public void addExtendableInputPortGroup(final String pGrpId, final PortType[] fixedPortTypes,
            final PortType... supportedTypes) {
            addExtendablePortGroup(pGrpId, fixedPortTypes, supportedTypes, true, false);
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
            addExtendablePortGroup(pGrpId, fixedPortTypes, getSupportedTypes(supportedTypesPredicate), true,
                false);
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
         * Adds an extendable output port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param fixedPortTypes the fixed port types
         * @param supportedTypes the supported port types
         */
        public void addExtendableOutputPortGroup(final String pGrpId, final PortType[] fixedPortTypes,
            final PortType... supportedTypes) {
            addExtendablePortGroup(pGrpId, fixedPortTypes, supportedTypes, false, true);
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
            addExtendablePortGroup(pGrpId, fixedPortTypes, getSupportedTypes(supportedTypesPredicate), false,
                true);
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
        public void addExtendablePortGroup(final String pGrpId,
            final Predicate<PortType> supportedTypesPredicate) {
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
            addExtendablePortGroup(pGrpId, fixedPortTypes, supportedTypes, true, true);
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
            addExtendablePortGroup(pGrpId, fixedPortTypes, getSupportedTypes(supportedTypesPredicate));
        }

        private static PortType[] getSupportedTypes(final Predicate<PortType> predicate) {
            return PortTypeRegistry.getInstance().availablePortTypes().stream().filter(predicate)
                .toArray(PortType[]::new);
        }

        private void addExtendablePortGroup(final String pGrpId, final PortType[] fixedPortTypes,
            final PortType[] supportedTypes, final boolean definesInputPorts, final boolean definesOutputPorts) {
            validateExtendablePortGroupArguments(pGrpId, fixedPortTypes, supportedTypes);
            m_portConfigs.put(pGrpId,
                new DefaultExtendablePortGroup(fixedPortTypes, supportedTypes, definesInputPorts, definesOutputPorts));
        }

        /**
         * Adds an optional input port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param optionalPorts the optional port types
         */
        public void addOptionalInputPortGroup(final String pGrpId, final PortType... optionalPorts) {
            addOptionalPortGroup(pGrpId, optionalPorts, true, false);
        }

        /**
         * Adds an optional input port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param optionalPortsPredicate the predicate identifying the optional port types
         * @since 4.2
         */
        public void addOptionalInputPortGroup(final String pGrpId, final Predicate<PortType> optionalPortsPredicate) {
            addOptionalPortGroup(pGrpId, getSupportedTypes(optionalPortsPredicate), true, false);
        }

        /**
         * Adds an optional output port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param optionalPorts the optional port types
         */
        public void addOptionalOutputPortGroup(final String pGrpId, final PortType... optionalPorts) {
            addOptionalPortGroup(pGrpId, optionalPorts, false, true);
        }

        /**
         * Adds an optional output port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param optionalPortsPredicate the predicate identifying the optional port types
         * @since 4.2
         */
        public void addOptionalOutputPortGroup(final String pGrpId, final Predicate<PortType> optionalPortsPredicate) {
            addOptionalPortGroup(pGrpId, getSupportedTypes(optionalPortsPredicate), false, true);
        }

        /**
         * Adds an optional port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param optionalPorts the optional port types
         */
        public void addOptionalPortGroup(final String pGrpId, final PortType... optionalPorts) {
            addOptionalPortGroup(pGrpId, optionalPorts, true, true);
        }

        /**
         * Adds an optional port group configuration.
         *
         * @param pGrpId the port group identifier
         * @param optionalPortsPredicate the predicate identifying optional port types
         * @since 4.2
         */
        public void addOptionalPortGroup(final String pGrpId,
            final Predicate<PortType> optionalPortsPredicate) {
            addOptionalPortGroup(pGrpId, getSupportedTypes(optionalPortsPredicate), true, true);
        }

        private void addOptionalPortGroup(final String pGrpId, final PortType[] optionalPorts,
            final boolean definesInputPorts, final boolean definesOutputPorts) {
            validateOptionalPortGroupArguments(pGrpId, optionalPorts);
            m_portConfigs.put(pGrpId, new DefaultExtendablePortGroup(new PortType[0], optionalPorts,
                definesInputPorts, definesOutputPorts, 1));
        }

        private void validatePortGrpIdentifier(final String pGrpId) {
            CheckUtils.checkArgument(!m_portConfigs.containsKey(pGrpId),
                "A port group with the given name already exists");
            CheckUtils.checkArgument(pGrpId != null && !pGrpId.isEmpty(),
                "The group identifier has to contain at least one character");
        }

        private void validateStaticPortGroupArguments(final String pGrpId, final PortType... staticPortTypes) {
            validatePortGrpIdentifier(pGrpId);
            CheckUtils.checkArgumentNotNull(staticPortTypes, "The static port types cannot be null");
            CheckUtils.checkArgument(staticPortTypes != null && staticPortTypes.length > 0,
                "At least one static port type has to be set");
        }

        private void validateExtendablePortGroupArguments(final String pGrpId, final PortType[] staticPorts,
            final PortType[] supportedTypes) {
            validatePortGrpIdentifier(pGrpId);
            CheckUtils.checkArgumentNotNull(staticPorts, "The static ports cannot be null");
            CheckUtils.checkArgumentNotNull(supportedTypes, "The supported port types cannot be null");
            CheckUtils.checkArgument(supportedTypes != null && supportedTypes.length > 0,
                "The supported port types have to contain at least element");
        }

        private void validateOptionalPortGroupArguments(final String pGrpId, final PortType... optionalPorts) {
            validatePortGrpIdentifier(pGrpId);
            CheckUtils.checkArgumentNotNull(optionalPorts, "The optional ports cannot be null");
            CheckUtils.checkArgument(optionalPorts != null && optionalPorts.length > 0,
                "The optional ports have to contain at least one element");
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
            CheckUtils.checkArgumentNotNull(supportedTypes, "The supported port types cannot be null");
            CheckUtils.checkArgument(supportedTypes != null && supportedTypes.length > 1,
                "The supported types have to contain at least two elements");
            CheckUtils.checkArgument(ArrayUtils.contains(supportedTypes, defaultType),
                "The supported port types have to contain the default port type");
        }
    }
}
