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
 *   24 Oct 2023 (carlwitt): created
 */
package org.knime.core.node.extension;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.knime.core.node.ConfigurableNodeFactory;
import org.knime.core.node.DynamicNodeFactory;
import org.knime.core.node.Node;
import org.knime.core.node.NodeAndBundleInformationPersistor;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.context.ModifiableNodeCreationConfiguration;
import org.knime.core.node.context.ports.ConfigurablePortGroup;
import org.knime.core.node.context.ports.PortGroupConfiguration;
import org.knime.core.node.extension.NodeSpec.Factory;
import org.knime.core.node.extension.NodeSpec.Metadata;
import org.knime.core.node.extension.NodeSpec.Metadata.Vendor;
import org.knime.core.node.extension.NodeSpec.Ports;
import org.knime.core.node.extension.NodeSpec.Ports.Port;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.CoreToDefUtil;
import org.knime.core.node.workflow.def.DefToCoreUtil;
import org.knime.shared.workflow.def.PortTypeDef;
import org.knime.shared.workflow.def.VendorDef;

/**
 * Specification for a node.
 *
 * Either derived from a {@link NodeFactory}-instance and the respective extension or read from file.
 *
 * @param factory node factory information
 * @param type
 * @param ports input and output ports
 * @param metadata node metadata, such as node type, vendor, category path, etc.
 * @param views node view information
 * @param icon node icon
 * @param deprecated whether a newer version of the node exists
 * @param hidden whether the node is hidden from the node repository
 *
 * @since 5.2
 */
public record NodeSpec(Factory factory, NodeType type, Ports ports, Metadata metadata, URL icon,
    boolean deprecated, boolean hidden) {

    public static final Predicate<NodeSpec> IS_HIDDEN = ns -> ns.hidden();

    public static final Predicate<NodeSpec> IS_DEPRECATED = ns -> ns.deprecated();

    /**
     * @param factory node factory information
     * @param categoryPath category path in the node repository as defined by the extension point
     * @param catExts implementations of the node repository category extension point
     * @param afterID the class name of the node factory after which this node factory should be inserted into the node
     *            repository, e.g., "org.knime.rest.nodes.delete.RestDeleteNodeFactory"
     * @param hidden whether the node is hidden from the node repository
     * @return null if the node cannot be instantiated
     */
    static NodeSpec of(final NodeFactory<NodeModel> factory, final String categoryPath,
        final Map<String, CategoryExtension> catExts, final String afterID, final boolean hidden) {

        final Node node;
        try {
            node = new Node(factory);
        } catch (Exception e) {
            // if the node cannot be instantiated, we probably do not want to add it to the node repo anyways
            NodeLogger.getLogger(NodeSpec.class).debug("Cannot instantiate node.", e);
            return null;
        }

        final var fact = NodeSpec.Factory.of(factory);
        final var ports = Ports.of(node, factory);
        final var description = Metadata.of(factory, categoryPath, catExts, afterID);

        final var deprecated = factory.isDeprecated();

        final var icon = factory.getIcon();

        return new NodeSpec(fact, factory.getType(), ports, description, icon, deprecated, hidden);
    }

    /**
     * @param catExts implementations of the node repository category extension point
     * @param nodeFactoryExtension either a node factory or a node set factory
     * @return empty list if the problems with the extension arise. Metadata of nodes that cannot be instantiated is
     *         missing from the returned list.
     */
    static List<NodeSpec> of(final Map<String, CategoryExtension> catExts,
        final INodeFactoryExtension nodeFactoryExtension) {
        try {
            final var hidden = nodeFactoryExtension.isHidden();

            final var nodeSpecs = new ArrayList<NodeSpec>();
            for (final var id : nodeFactoryExtension.getNodeFactoryIds()) {
                final var afterID = nodeFactoryExtension.getAfterID(id);

                var optFactory = nodeFactoryExtension.getNodeFactory(id);
                if (optFactory.isEmpty()) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                final var factory = (NodeFactory<NodeModel>)optFactory.get();
                final var categoryPath = Path.forPosix(nodeFactoryExtension.getCategoryPath(id)) //
                    .removeTrailingSeparator().makeAbsolute().makeUNC(false).toString();

                var nodeSpec = NodeSpec.of(factory, categoryPath, catExts, afterID, hidden);
                nodeSpecs.add(nodeSpec);
            }
            return nodeSpecs;
        } catch (Exception ex) {
            // if the node cannot be instantiated, we probably do not want to add it to the node repo anyways
            NodeLogger.getLogger(NodeSpec.class).debug("Cannot compute node properties.", ex);
            return List.of();
        }
    }

    /**
     * @param id globally unique factory identifier, as provided by the factory
     * @param className fully qualified java class name, e.g., "org.knime.base.node.io.database.DBReaderNodeFactory"
     * @param factorySettings additional factory settings or null if factory is not a subtype of
     *            {@link DynamicNodeFactory}
     */
    public static record Factory(String id, String className, NodeSettings factorySettings) {

        /**
         * @param factory to describe
         * @return extracted information
         */
        public static Factory of(final NodeFactory<?> factory) {
            var id = factory.getFactoryId();
            var className = factory.getClass().getName();
            NodeSettings factorySettings = null;
            //only set settings in case of a dynamic node factory
            if (DynamicNodeFactory.class.isAssignableFrom(factory.getClass())) {
                factorySettings = new NodeSettings("settings");
                factory.saveAdditionalFactorySettings(factorySettings);
            }
            return new Factory(id, className, factorySettings);
        }
    }

    /**
     * @param inputPorts input ports
     * @param supportedInputPortTypes as defined by the node factory
     * @param outputPorts output ports
     */
    public static record Ports(List<Port> inputPorts, List<PortTypeDef> supportedInputPortTypes,
        List<Port> outputPorts) {

        /**
         * @param node to analyze
         * @param factory provides port descriptions
         * @return input and output ports of the node
         */
        private static Ports of(final Node node, final NodeFactory<?> factory) {
            // skip port 0, it is the flow variable input port
            final var inputPorts = ports(1, node.getNrInPorts(), node::getInportName, node::getInportDescriptionName,
                node::getInputType, factory::getInportDescription);
            // skip port 0, it is the flow variable output port
            final var outputPorts = ports(1, node.getNrOutPorts(), node::getOutportName,
                node::getOutportDescriptionName, node::getOutputType, factory::getOutportDescription);

            // what the factory declares as supported input port types
            var optDeclaredInputPortTypes = getCopyOfCreationConfig(factory)//
                .flatMap(ModifiableNodeCreationConfiguration::getPortConfig)//
                .map(portsConfig -> portsConfig.getPortGroupNames().stream()//
                    .filter(portsConfig::isInteractive)//
                    .map(portsConfig::getGroup)//
                    .filter(PortGroupConfiguration::definesInputPorts)//
                    .filter(ConfigurablePortGroup.class::isInstance)//
                    .map(ConfigurablePortGroup.class::cast)//
                    .flatMap(cpg -> Arrays.stream(cpg.getSupportedPortTypes()))//
                    .map(CoreToDefUtil::toPortTypeDef));
            var supportedInputPortTypes =
                Stream.concat(optDeclaredInputPortTypes.orElse(Stream.empty()), inputPorts.stream().map(Port::type))
                    .distinct()//
                    .toList();

            return new Ports(inputPorts, supportedInputPortTypes, outputPorts);
        }

        private static Optional<ModifiableNodeCreationConfiguration>
            getCopyOfCreationConfig(final NodeFactory<? extends NodeModel> factory) {
            if (factory instanceof ConfigurableNodeFactory) {
                var creationConfig = ((ConfigurableNodeFactory<? extends NodeModel>)factory).createNodeCreationConfig();
                return Optional.of(creationConfig);
            } else {
                return Optional.empty();
            }
        }

        private static List<Port> ports(final int firstPort, final int numPorts, final IntFunction<String> portName,
            final IntFunction<String> portDescriptionName, final IntFunction<PortType> portType,
            final IntFunction<String> portDescription) {

            final var ports = new ArrayList<Port>();
            for (var i = firstPort; i < numPorts; i++) {
                // log info if port names provided by factory and node mismatch
                // log info if port description name is not same as port name
                if (!portDescriptionName.apply(i).equals(portName.apply(i))) {
                    NodeLogger.getLogger(Ports.class).warnWithFormat(
                        "Port name mismatch, description name is %s but port name is %s", portDescriptionName.apply(i),
                        portName.apply(i));
                }
                final var index = i;
                final var type = CoreToDefUtil.toPortTypeDef(portType.apply(i));
                final var name = portName.apply(i);
                final var description = portDescription.apply(i);
                ports.add(new Port(index, type, name, description));
            }
            return ports;
        }

        /**
         * @return input ports types
         */
        public Stream<PortType> getInputPortTypes() {
            return inputPorts.stream().map(Port::type).map(DefToCoreUtil::toPortType);
        }

        /**
         * @return output ports types
         */
        public Stream<PortType> getOutputPortTypes() {
            return outputPorts.stream().map(Port::type).map(DefToCoreUtil::toPortType);
        }

        /**
         * @return if the creating factory is a {@link ConfigurableNodeFactory}, this returns the supported port types.
         *         Otherwise it returns the distinct present input port types.
         */
        public Stream<PortType> getSupportedInputPortTypes() { // NOSONAR I want to to return port types, not port type defs
            return supportedInputPortTypes.stream().map(DefToCoreUtil::toPortType);
        }

        /**
         * @param index port index at the node
         * @param type of the input data and its spec, etc.
         * @param name of the port as provided by the node
         * @param description of the port as provided by the node
         */
        public static record Port(int index, PortTypeDef type, String name, String description) {
        }
    }

    /**
     * @param vendor bundle and feature vendor information
     * @param nodeName see {@link NodeFactory#getNodeName()}
     * @param nodeType see {@link NodeFactory#getType()}
     * @param categoryPath category path in the node repository as defined by the extension point, e.g., "/IO/Database"
     * @param afterID the class name of the node factory after which this node factory should be inserted into the node
     *            repository, e.g., "org.knime.rest.nodes.delete.RestDeleteNodeFactory"
     * @param keywords see {@link NodeFactory#getKeywords()}
     * @param tags computed from the category path by splitting it into segments, lowercasing them, and deduplicating
     */
    public static record Metadata(Vendor vendor, String nodeName, NodeType nodeType, String categoryPath,
        String afterID, List<String> keywords, List<String> tags) {

        private static Metadata of(final NodeFactory<? extends NodeModel> factory, final String categoryPath,
            final Map<String, CategoryExtension> catExts, final String afterID) {

            final var vendor = Vendor.of(factory);
            final var nodeName = factory.getNodeName();
            final var nodeType = factory.getType();
            final var keywords = List.of(factory.getKeywords());

            final var tags = new HashSet<String>();
            for (IPath path = new Path(categoryPath); !path.isRoot(); path = path.removeLastSegments(1)) {
                tags.add(path.lastSegment());
                Optional.ofNullable(catExts.get(path.toString())).ifPresent(ext -> tags.add(ext.getName()));
            }

            return new Metadata(vendor, nodeName, nodeType, categoryPath, afterID, keywords, List.copyOf(tags));
        }

        /**
         * @param feature
         * @param bundle
         */
        public static record Vendor(VendorDef feature, VendorDef bundle) {
            private static Vendor of(final NodeFactory<?> factory) {
                var persistor = NodeAndBundleInformationPersistor.create(factory);
                var featureVendor = CoreToDefUtil.toFeatureVendorDef(persistor);
                var bundleVendor = CoreToDefUtil.toBundleVendorDef(persistor);
                return new Vendor(featureVendor, bundleVendor);
            }
        }

    }

    @Override
    public String toString() {
        return Formatter.toString(this);
    }

    static final class Formatter {

        private Formatter() {
        }

        private static void vendor(final StringBuilder sb, final String indent, final Vendor vendor) {
            sb.append(indent).append("vendor: [\n");
            sb.append(indent).append("  feature:\n");
            Formatter.vendorDef(sb, indent + "    ", vendor.feature());
            sb.append(indent).append("  bundle:\n");
            Formatter.vendorDef(sb, indent + "    ", vendor.bundle());
            sb.append(indent).append("]\n");
        }

        private static void vendorDef(final StringBuilder sb, final String indent, final VendorDef vendorDef) {
            sb.append(indent).append("vendorDef: [\n");
            sb.append(indent).append("  name: ").append(vendorDef.getName()).append("\n");
            sb.append(indent).append("  symbolicName: ").append(vendorDef.getSymbolicName()).append("\n");
            sb.append(indent).append("  version: ").append(vendorDef.getVersion()).append("\n");
            sb.append(indent).append("]\n");
        }

        private static void ports(final StringBuilder sb, final String indent, final Ports ports) {
            sb.append(indent).append("ports: [\n");
            sb.append(indent).append("  inputPorts: [\n");
            ports.inputPorts().forEach(port -> Formatter.port(sb, indent + "    ", port));
            sb.append(indent).append("  ],\n");
            sb.append(indent).append("  supportedInputPortTypes: [\n");
            ports.supportedInputPortTypes().forEach(typeDef -> Formatter.portTypeDef(sb, indent + "    ", typeDef));
            sb.append(indent).append("  ],\n");
            sb.append(indent).append("  outputPorts: [\n");
            ports.outputPorts().forEach(port -> Formatter.port(sb, indent + "    ", port));
            sb.append(indent).append("  ]\n");
            sb.append(indent).append("]\n");
        }

        private static void port(final StringBuilder sb, final String indent, final Port port) {
            sb.append(indent).append("port: [\n");
            sb.append(indent).append("  index: ").append(port.index()).append("\n");
            sb.append(indent).append("  name: ").append(port.name()).append("\n");
            sb.append(indent).append("  description: ").append(port.description()).append("\n");
            Formatter.portTypeDef(sb, indent + "  ", port.type());
            sb.append(indent).append("]\n");
        }

        private static void portTypeDef(final StringBuilder sb, final String indent, final PortTypeDef typeDef) {
            sb.append(indent).append("portTypeDef: [\n");
            sb.append(indent).append("  name: ").append(typeDef.getName()).append("\n");
            sb.append(indent).append("  color: ").append(typeDef.getColor()).append("\n");
            sb.append(indent).append("  portObjectClass: ").append(typeDef.getPortObjectClass()).append("\n");
            sb.append(indent).append("  portObjectSpecClass: ").append(typeDef.getPortObjectSpecClass()).append("\n");
            sb.append(indent).append("]\n");
        }

        private static void metadata(final StringBuilder sb, final String indent, final Metadata metadata) {
            sb.append(indent).append("metadata: [\n");
            Formatter.vendor(sb, indent + "  ", metadata.vendor());
            sb.append(indent).append("  nodeName: ").append(metadata.nodeName()).append("\n");
            sb.append(indent).append("  nodeType: ").append(metadata.nodeType()).append("\n");
            sb.append(indent).append("  categoryPath: ").append(metadata.categoryPath()).append("\n");
            sb.append(indent).append("  afterID: ").append(metadata.afterID()).append("\n");
            sb.append(indent).append("  keywords: ").append(metadata.keywords()).append("\n");
            sb.append(indent).append("  tags: ").append(metadata.tags()).append("\n");
            sb.append(indent).append("]\n");
        }

        private static void factory(final StringBuilder sb, final String indent, final Factory factory) {
            sb.append(indent).append("factory: [\n");
            sb.append(indent).append("  id: ").append(factory.id()).append("\n");
            sb.append(indent).append("  className: ").append(factory.className()).append("\n");
            sb.append(indent).append("  factorySettings: ").append(factory.factorySettings()).append("\n");
            sb.append(indent).append("]\n");
        }

        static String toString(final NodeSpec properties) {
            final var sb = new StringBuilder("NodeProperties [\n");
            Formatter.metadata(sb, "  ", properties.metadata());
            Formatter.ports(sb, "  ", properties.ports());
            Formatter.factory(sb, "  ", properties.factory());
            sb.append("  ").append("icon: ").append(properties.icon()).append("\n");
            sb.append("  ").append("deprecated: ").append(properties.deprecated()).append("\n");
            sb.append("  ").append("hidden: ").append(properties.hidden()).append("\n");
            return sb.append("]").toString();
        }
    }
}