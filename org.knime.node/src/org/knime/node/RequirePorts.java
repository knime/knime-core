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
package org.knime.node;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.port.PortType;
import org.knime.node.DefaultNode.RequireModel;
import org.knime.node.RequirePorts.ConfigurablePort.BoundExtendableConfigurablePort;
import org.knime.node.RequirePorts.ConfigurablePort.ExtendableConfigurablePort;
import org.knime.node.RequirePorts.ConfigurablePort.FinalConfigurablePort;
import org.knime.node.RequirePorts.ConfigurablePort.OptionalConfigurablePort;

/**
 * This is a stage of the fluent {@link DefaultNode} API.
 *
 * It allows for declarative specification of fixed, optional, extendable, bound, and exchangeable port groups. Example
 * usage:
 *
 * <pre>
 * {@code
 * .ports(p -> {
 *     p.addInputTable("Input", "Input table");
 *     p.addOutputTable("Output", "Output table");
 * });
 * }
 * </pre>
 *
 * The following concepts are available:
 * <ul>
 * <li>A port has a name, a description and a type.</li>
 * <li>Ports can be organized into groups. Each group has a shared name and description, and also defines whether the
 * ports in that group are fixed or can be changed by the user or programmatically (see {@link #dynamicPorts}).</li>
 * <li>One can either specify input ports, output ports or both at the same time. (see {@link DynamicPortsAdder})</li>
 *
 * @author Paul Bärnreuther
 */
public interface RequirePorts {
    /**
     * Define input and output ports for the node.
     *
     * <p>
     * Ports defined with this method are static/fixed ports, meaning they are always present and cannot be modified by
     * the user. <b>This is the typical way to define ports for most nodes.</b> For advanced or dynamic port
     * configurations, see {@link #dynamicPorts}. Since it is not possible to call this method and {@link #dynamicPorts}
     * at the same time, it is possible to add fixed ports in the {@link #dynamicPorts} method as well.
     *
     * <p>
     * <b>Example:</b>
     * </p>
     *
     * <pre>
     * .ports(p -> {
     *     p.addInputTable("Input", "Input table");
     *     p.addOutputPort("Output", "Output Flow Variables", FlowVariablePortObject.TYPE);
     * });
     * </pre>
     *
     * @param ports a consumer that receives a {@link PortsAdder} for adding ports
     * @return the next stage of node configuration
     * @see #dynamicPorts(Consumer)
     */
    default RequireModel ports(final Consumer<PortsAdder> ports) {
        return dynamicPorts(d -> {
            final PortsAdder adder = new PortsAdder() {
                @Override
                public PortsAdder addInputPort(final String name, final String description, final PortType portType) {
                    d.addInputPortGroup(name, g -> g.name(name).description(description).fixed(portType));
                    return this;
                }

                @Override
                public PortsAdder addOutputPort(final String name, final String description, final PortType portType) {
                    d.addOutputPortGroup(name, g -> g.name(name).description(description).fixed(portType));
                    return this;
                }

            };
            ports.accept(adder);
        });
    }

    /**
     * Defines dynamic (configurable) input and output ports for the node, such as optional, extendable, bound, or
     * exchangeable port groups.
     * <p>
     * <b>This is for special cases where the presence or type of some ports must be user-configurable or dynamic.</b>
     * For most nodes, use {@link #ports} instead.
     *
     * <p>
     * <b>Example:</b>
     * </p>
     *
     * <pre>
     * .dynamicPorts(d -> d //
     *     .addInputPortGroup("inputs", g -> {
     *        return g.name("Input").description("Dynamic input")
     *          .extendable().supportedTypes(BufferedDataTable.TYPE, FlowVariablePortObject.TYPE)
     *          .defaultNonFixedTypes(BufferedDataTable.TYPE);
     *        }
     *     ) //
     *     .addOutputPortGroup("outputs", g -> {
     *         return g.name("Output").description("Optional output")
     *          .optional().supportedTypes(BufferedDataTable.TYPE);
     *       }
     *     ) //
     *     .addInputAndOutputPortGroup("paired", g -> {
     *        return g.inputName("Paired Input").inputDescription("Input port of a paired group")
     *          .outputName("Paired Output").outputDescription("Output port of a paired group")
     *          .fixed(BufferedDataTable.TYPE);
     *       }
     *     ) //
     * );
     * </pre>
     *
     * @param ports a consumer that receives a {@link DynamicPortsAdder} for adding dynamic port groups
     * @return the next stage of node configuration
     * @see #ports(Consumer)
     */
    RequireModel dynamicPorts(Consumer<DynamicPortsAdder> ports);

    /**
     * Interface for adding static input and output ports to a node.
     */
    interface PortsAdder {
        /**
         * Adds a fixed input port to the node.
         *
         * @param name the name of the port
         * @param description the description of the port
         * @param portType the type of the port
         * @return this builder for chaining
         */
        PortsAdder addInputPort(String name, String description, PortType portType);

        /**
         * Adds a fixed output port to the node.
         *
         * @param name the name of the port
         * @param description the description of the port
         * @param portType the type of the port
         * @return this builder for chaining
         */
        PortsAdder addOutputPort(String name, String description, PortType portType);

        /**
         * Adds a fixed input port of type {@link BufferedDataTable}.
         *
         * @param name the name of the port
         * @param description the description of the port
         * @return this builder for chaining
         */
        default PortsAdder addInputTable(final String name, final String description) {
            return addInputPort(name, description, BufferedDataTable.TYPE);
        }

        /**
         * Adds a fixed output port of type {@link BufferedDataTable}.
         *
         * @param name the name of the port
         * @param description the description of the port
         * @return this builder for chaining
         */
        default PortsAdder addOutputTable(final String name, final String description) {
            return addOutputPort(name, description, BufferedDataTable.TYPE);
        }
    }

    /**
     * Builder for adding dynamic (configurable) port groups to a node, such as optional, extendable, bound, or
     * exchangeable groups.
     */
    interface DynamicPortsAdder {
        /**
         * Adds a dynamic input port group to the node.
         *
         * @param groupId the identifier for the port group. It can be used to access the ports of this group in the
         *            model or to reference this group within a bound extendable port group.
         * @param groupConfiguration a function to configure the port group
         * @return this builder for chaining
         */
        DynamicPortsAdder addInputPortGroup(String groupId,
            Function<RequirePortName, ConfigurablePort> groupConfiguration);

        /**
         * Adds a dynamic output port group to the node.
         *
         * @param groupId the identifier for the port group. It can be used to access the ports of this group in the
         *            model or to reference this group within a bound extendable port group.
         * @param groupConfiguration a function to configure the port group
         * @return this builder for chaining
         */
        DynamicPortsAdder addOutputPortGroup(String groupId,
            Function<RequirePortName, ConfigurablePort> groupConfiguration);

        /**
         * Adds a coupled input/output port group to the node, where input and output ports are configured together.
         * I.e., e.g., if the port group is optional, when the user adds an input port, an output port is automatically
         * added as well, and vice versa.
         *
         * @param groupId the identifier for the port group. It can be used to access the ports of this group in the
         *            model or to reference this group within a bound extendable port group.
         * @param groupConfiguration a function to configure the input-output port group
         * @return this builder for chaining
         */
        DynamicPortsAdder addInputAndOutputPortGroup(String groupId,
            Function<RequireInputPortName, ConfigurablePort> groupConfiguration);
    }

    /**
     * Stage requiring the name for a port in a dynamic port group.
     */
    @FunctionalInterface
    interface RequirePortName {
        /**
         * Specifies the name for the port.
         *
         * @param name the port name
         * @return the next stage of configuration
         */
        RequirePortDescription name(String name);
    }

    /**
     * Stage requiring the description for a port in a dynamic port group.
     */
    @FunctionalInterface
    interface RequirePortDescription {
        /**
         * Specifies the description for the port.
         *
         * @param description the port description
         * @return the next stage of configuration
         */
        RequireConfigurablePortMode description(String description);
    }

    /**
     * Stage requiring the name for the input port of a input-and-output port group.
     */
    @FunctionalInterface
    interface RequireInputPortName {
        /**
         * Specifies the name for the input port(s) of this group.
         *
         * @param name the input port name
         * @return the next stage of configuration
         */
        RequireInputPortDescription inputName(String name);
    }

    /**
     * Stage requiring the description for the input port of a input-and-output port group.
     */
    interface RequireInputPortDescription {
        /**
         * Specifies the description for the input port(s) of this group.
         *
         * @param description the input port description
         * @return the next stage of configuration
         */
        RequireOutputPortName inputDescription(String description);
    }

    /**
     * Stage requiring the name for the output port of a input-and-output port group.
     */
    @FunctionalInterface
    interface RequireOutputPortName {
        /**
         * Specifies the name for the output port(s) of this group.
         *
         * @param name the output port name
         * @return the next stage of configuration
         */
        RequireOutputPortDescription outputName(String name);
    }

    /**
     * Stage requiring the description for the output port of a input-and-output port group.
     */
    @FunctionalInterface
    interface RequireOutputPortDescription {
        /**
         * Specifies the description for the output port(s) of this group.
         *
         * @param description the output port description
         * @return the next stage of configuration
         */
        RequireConfigurablePortMode outputDescription(String description);
    }

    /**
     * Stage for selecting the type of dynamic port group (fixed, optional, extendable, bound, or exchangeable).
     * <p>
     * Five different port group types are available:
     * <ol>
     * <li>Fixed port group, i.e., port groups that cannot be modified by the user.
     * <li>Optional port group, i.e., a port group with [0,1] ports. Initially the optional port is not set
     * <li>Extendable port group, i.e., a port group with [0,n] fixed ports and [0,n] additional ports
     * <li>Bound extendable port group, i.e., a port group with [0,n] fixed ports and [0,n] additional ports whose type
     * is bound to that of another port group
     * <li>Exchangebale port group, i.e., a port group with exactly one port whose port type can be exchanged
     * </ol>
     *
     * <p>
     * Note that fixed port groups are the ones used within {@link #ports} as well, where the port name is used as group
     * id. In the presence of other dynamic port groups, a group id might be needed here to access the index of a fixed
     * port in the model as well.
     * </p>
     */
    interface RequireConfigurablePortMode {
        /**
         * Defines a fixed port group with a single port type.
         *
         * @param portType the type of the port
         * @return the final stage of configuration
         */
        FinalConfigurablePort fixed(PortType portType);

        /**
         * Defines an optional port group ([0,1] ports).
         *
         * @return the next stage for specifying supported types
         */
        RequireSupportedTypes<OptionalConfigurablePort> optional();

        /**
         * Defines an extendable port group ([0,n] ports).
         *
         * @return the next stage for specifying supported types
         */
        RequireSupportedTypes<ExtendableConfigurablePort> extendable();

        /**
         * Defines a bound extendable port group, whose type is bound to another group.
         *
         * @param groupId the group to bind to
         * @return the next stage for configuring the bound group
         */
        BoundExtendableConfigurablePort boundExtendable(String groupId);

        /**
         * Defines an exchangeable port group (exactly one port, type can be exchanged).
         *
         * @param defaultType the default port type
         * @return the next stage for specifying supported types
         */
        RequireSupportedTypes<FinalConfigurablePort> exchangable(PortType defaultType);
    }

    /**
     * Stage for specifying the supported types for optional, extendable, or exchangeable port groups.
     * <p>
     * Specifying supported types is mandatory for these group types.
     *
     * @param <T> the next stage of configuration
     */
    interface RequireSupportedTypes<T> {
        /**
         * Specifies the supported port types for this group.
         *
         * @param portTypes the supported port types
         * @return the next stage of configuration
         */
        T supportedTypes(PortType... portTypes);

        /**
         * Specifies the supported port types for this group using a predicate.
         *
         * @param portTypePredicate a predicate to determine supported types
         * @return the next stage of configuration
         */
        T supportedTypes(Predicate<PortType> portTypePredicate);

    }

    /**
     * Result of port configuration. Contains the different last stages of configuration for each port group type.
     * <p>
     * The permitted sub-interfaces provide optional configuration methods for each group type.
     */
    sealed interface ConfigurablePort permits FinalConfigurablePort, OptionalConfigurablePort,
        ExtendableConfigurablePort, BoundExtendableConfigurablePort {
        /**
         * Final stage for fixed and exchangeable port groups.
         */
        final class FinalConfigurablePort implements ConfigurablePort {
        }

        /**
         * Final stage for optional port groups, with an optional default type.
         */
        non-sealed interface OptionalConfigurablePort extends ConfigurablePort {
            /**
             * Optionally sets a default type for this optional port. If not set, the port is not present initially.
             *
             * @param defaultPortType the initial/default port type
             * @return the final stage of configuration
             * @throws IllegalArgumentException if the port type is not supported
             */
            FinalConfigurablePort defaultPortType(PortType defaultPortType);
        }

        /**
         * Final stage for extendable port groups, with optional fixed and default types.
         */
        non-sealed interface ExtendableConfigurablePort extends ConfigurablePort {
            /**
             * Optionally specifies a fixed list of ports that are part of this group and cannot be removed by the user.
             *
             * @param portTypes the fixed port types
             * @return this stage (do not call twice)
             */
            ExtendableConfigurablePort fixedTypes(PortType... portTypes);

            /**
             * Optionally defines the ports that are present initially but can be removed by the user.
             *
             * @param portTypes the default (non-fixed) port types
             * @return this stage (do not call twice)
             */
            ExtendableConfigurablePort defaultNonFixedTypes(PortType... portTypes);
        }

        /**
         * Final stage for bound extendable port groups, with optional fixed and default port counts.
         */
        non-sealed interface BoundExtendableConfigurablePort extends ConfigurablePort {
            /**
             * Optionally specifies a fixed number of ports that are part of this group and cannot be removed by the
             * user.
             *
             * @param numPorts the number of fixed ports
             * @return this stage (do not call twice)
             */
            BoundExtendableConfigurablePort fixedNumPorts(int numPorts);

            /**
             * Optionally defines the number of ports that are present initially but can be removed by the user.
             *
             * @param numPorts the number of default (non-fixed) ports
             * @return this stage (do not call twice)
             */
            BoundExtendableConfigurablePort defaultNumPorts(int numPorts);
        }
    }

}
