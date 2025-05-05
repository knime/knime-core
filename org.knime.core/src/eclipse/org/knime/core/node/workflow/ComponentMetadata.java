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
 *   Nov 12, 2019 (hornm): created
 */
package org.knime.core.node.workflow;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.metadata.MetadataVersion;
import org.knime.core.node.workflow.metadata.v10.ComponentMetadata.ComponentType;
import org.knime.core.node.workflow.metadata.v10.ComponentMetadataDocument;
import org.knime.core.node.workflow.metadata.v10.PortGroup;
import org.knime.core.util.LoadVersion;
import org.knime.core.util.xml.NoExternalEntityResolver;

/**
 * Represents general metadata associated with a component (i.e. a {@link SubNodeContainer}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 *
 * @since 4.1
 */
public final class ComponentMetadata extends NodeContainerMetadata {

    /**
     * An enum which wraps the {@code NodeFactory.NodeType} enum with display text; for the time being, we are
     *  not wrapping an {@code Image} like we do with DisplayableNodeType for two reasons, both dealing with the
     *  plugin in which this class sits:
     *      1. I've been told we're trying to keep SWT related assets out of knime-core
     *      2. The PNG assets which we use currently sit in, and i assume the new versions will also sit in,
     *              knime-workbench.
     *  @author loki
     */
    public enum ComponentNodeType {
        /** A learning node. */
        LEARNER("Learner", NodeFactory.NodeType.Learner),
        /** A data manipulating node. */
        MANIPULATOR("Manipulator", NodeFactory.NodeType.Manipulator),
        /** A predicting node. */
        PREDICTOR("Predictor", NodeFactory.NodeType.Predictor),
        /** A data consuming node. */
        SINK("Sink", NodeFactory.NodeType.Sink),
        /** A data producing node. */
        SOURCE("Source", NodeFactory.NodeType.Source),
        /** A visualizing node. */
        VISUALIZER("Visualizer", NodeFactory.NodeType.Visualizer),
        /** Any other node. */
        OTHER("Other", NodeFactory.NodeType.Other);

        private static final Map<NodeFactory.NodeType, ComponentNodeType> NODE_TYPE_DISPLAYABLE_MAP =
                new EnumMap<>(NodeFactory.NodeType.class);

        /**
         * Given the {@code NodeFactory.NodeType}, return the mapped instance of this enum.
         *
         * @param nodeType
         * @return the instance of this enum which wraps the parameter value enum
         */
        public static synchronized ComponentNodeType getTypeForNodeType(final NodeFactory.NodeType nodeType) {
            if (NODE_TYPE_DISPLAYABLE_MAP.size() == 0) {
                for (final ComponentNodeType ct : ComponentNodeType.values()) {
                    NODE_TYPE_DISPLAYABLE_MAP.put(ct.getType(), ct);
                }
            }
            return NODE_TYPE_DISPLAYABLE_MAP.get(nodeType);
        }

        private final String m_displayText;
        private final NodeFactory.NodeType m_nodeType;

        ComponentNodeType(final String name, final NodeFactory.NodeType nodeType) {
            m_displayText = name;
            m_nodeType = nodeType;
        }

        /**
         * @return the human readable text representing this enum
         */
        public String getDisplayText() {
            return m_displayText;
        }

        /**
         * @return the {@code NodeFactory.NodeType} which this enum instance wraps.
         */
        public NodeFactory.NodeType getType() {
            return m_nodeType;
        }
    }

    /** Bidirectional mapping between {@link ComponentNodeType} and the IDs of {@link ComponentType.Enum}. */
    private static final BidiMap COMPONENT_TYPE_MAP_V1_0;
    static {
        COMPONENT_TYPE_MAP_V1_0 = new DualHashBidiMap();
        COMPONENT_TYPE_MAP_V1_0.put(ComponentNodeType.LEARNER, ComponentType.LEARNER.intValue());
        COMPONENT_TYPE_MAP_V1_0.put(ComponentNodeType.MANIPULATOR, ComponentType.MANIPULATOR.intValue());
        COMPONENT_TYPE_MAP_V1_0.put(ComponentNodeType.PREDICTOR, ComponentType.PREDICTOR.intValue());
        COMPONENT_TYPE_MAP_V1_0.put(ComponentNodeType.SINK, ComponentType.SINK.intValue());
        COMPONENT_TYPE_MAP_V1_0.put(ComponentNodeType.SOURCE, ComponentType.SOURCE.intValue());
        COMPONENT_TYPE_MAP_V1_0.put(ComponentNodeType.VISUALIZER, ComponentType.VISUALIZER.intValue());
        COMPONENT_TYPE_MAP_V1_0.put(ComponentNodeType.OTHER, ComponentType.OTHER.intValue());
    }

    /**
     * Port description.
     *
     * @param name port name
     * @param description port description
     * @since 5.1
     */
    public record Port(String name, String description) {}

    private static final String CFG_METADATA = "metadata";

    /**
     * An empty metadata object.
     */
    public static final ComponentMetadata NONE = fluentBuilder() //
            .withPlainContent() //
            .withLastModifiedNow() //
            .withDescription("") //
            .build();

    private final ComponentNodeType m_nodeType;

    private byte[] m_icon;

    private final List<Port> m_inPorts;

    private final List<Port> m_outPorts;

    private ComponentMetadata(final ContentType contentType, final ZonedDateTime lastModified, // NOSONAR
            final ZonedDateTime created, final String author, final String description, final List<Link> links,
            final List<String> tags, final ComponentNodeType nodeType, final byte[] icon, final List<Port> inPorts,
            final List<Port> outPorts) {
        super(org.knime.core.node.workflow.metadata.v10.ComponentMetadata.Factory,
            contentType, lastModified, created, author, description, links, tags);
        m_nodeType = nodeType;
        m_icon = icon;
        m_inPorts = inPorts;
        m_outPorts = outPorts;
    }

    /**
     * Parses metadata from an XML file.
     *
     * @param xmlFile file location
     * @param version version of the metadata XML to read
     * @return read metadata
     * @throws IOException if reading has failed
     * @since 5.1
     */
    public static ComponentMetadata fromXML(final Path xmlFile, final MetadataVersion version) throws IOException {
        CheckUtils.checkArgument(version == MetadataVersion.V1_0, "Expected metadata version 1.0, found '%s'.");
        try (final var bufferedReader = Files.newBufferedReader(xmlFile, StandardCharsets.UTF_8)) {
            final var xmlOptions = new XmlOptions();
            xmlOptions.setCharacterEncoding(StandardCharsets.UTF_8.name());
            xmlOptions.setValidateStrict();
            xmlOptions.disallowDocTypeDeclaration();
            xmlOptions.setEntityResolver(NoExternalEntityResolver.getInstance());

            final var componentMetadataDoc = ComponentMetadataDocument.Factory.parse(bufferedReader, xmlOptions);
            final var metadataElement = componentMetadataDoc.getComponentMetadata();

            // set fields for components
            final var componentType = Optional.ofNullable(metadataElement.getComponentType()) //
                    .map(ComponentType.Enum::intValue) //
                    .map(COMPONENT_TYPE_MAP_V1_0.inverseBidiMap()::get) //
                    .map(ComponentNodeType.class::cast) //
                    .orElse(null);
            final var compBuilder = fluentBuilder()
                    .withComponentType(componentType)
                    .withIcon(metadataElement.getIcon());
            for (final var port : metadataElement.getInPorts().getPortArray()) {
                compBuilder.withInPort(port.getName(), port.getStringValue());
            }
            for (final var port : metadataElement.getOutPorts().getPortArray()) {
                compBuilder.withOutPort(port.getName(), port.getStringValue());
            }

            // set fields for general metadata
            return readCommonFields(metadataElement, compBuilder);
        } catch (XmlException e) {
            throw new IOException("Unable to load component metadata: " + e.getMessage(), e);
        }
    }

    @Override
    XmlObject toXMLDocument(final org.knime.core.node.workflow.metadata.v10.NodeContainerMetadata metadataXML) {
        final var componentXML = (org.knime.core.node.workflow.metadata.v10.ComponentMetadata)metadataXML;
        if (m_nodeType != null) {
            final int typeIdx = (Integer) COMPONENT_TYPE_MAP_V1_0.get(m_nodeType);
            componentXML.setComponentType((ComponentType.Enum.forInt(typeIdx)));
        }
        if (m_icon != null) {
            componentXML.setIcon(m_icon);
        }
        final var inPorts = componentXML.addNewInPorts();
        for (final var inPort : m_inPorts) {
            addPortDescription(inPorts, inPort);
        }
        final var outPorts = componentXML.addNewOutPorts();
        for (final var outPort : m_outPorts) {
            addPortDescription(outPorts, outPort);
        }

        final var document = ComponentMetadataDocument.Factory.newInstance();
        document.setComponentMetadata(componentXML);
        return document;
    }

    private static void addPortDescription(final PortGroup group, final Port port) {
        final var portDesc = group.addNewPort();
        portDesc.setName(port.name);
        portDesc.setStringValue(port.description);
    }

    /**
     * @return the type
     */
    public Optional<ComponentNodeType> getNodeType() {
        return Optional.ofNullable(m_nodeType);
    }

    /**
     * @return the icon
     */
    public Optional<byte[]> getIcon() {
        return Optional.ofNullable(m_icon);
    }

    /**
     * @return in-port names
     */
    public Optional<String[]> getInPortNames() {
        return Optional.of(m_inPorts.stream().map(Port::name).toArray(String[]::new));
    }

    /**
     * @return in-port descriptions
     */
    public Optional<String[]> getInPortDescriptions() {
        return Optional.of(m_inPorts.stream().map(Port::description).toArray(String[]::new));
    }

    /**
     * @return out-port names
     */
    public Optional<String[]> getOutPortNames() {
        return Optional.of(m_outPorts.stream().map(Port::name).toArray(String[]::new));
    }

    /**
     * @return out-port descriptions
     */
    public Optional<String[]> getOutPortDescriptions() {
        return Optional.of(m_outPorts.stream().map(Port::description).toArray(String[]::new));
    }

    /**
     * @return in-port descriptions
     * @since 5.1
     */
    public List<Port> getInPorts() {
        return Collections.unmodifiableList(m_inPorts);
    }

    /**
     * @return out-port descriptions
     * @since 5.1
     */
    public List<Port> getOutPorts() {
        return Collections.unmodifiableList(m_outPorts);
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj || (obj instanceof ComponentMetadata other && commonEquals(other) //
                && Objects.equals(m_nodeType, other.m_nodeType) //
                && Objects.deepEquals(m_icon, other.m_icon) //
                && Objects.equals(m_inPorts, other.m_inPorts) //
                && Objects.equals(m_outPorts, other.m_outPorts));
    }

    @Override
    public String toString() {
        final var sb = new StringBuilder(getClass().getSimpleName()).append('[');
        addCommonFields(sb);
        sb.append(", icon=").append(m_icon == null ? "null" : ("'" + Base64.getEncoder().encodeToString(m_icon) + "'"))
                .append(", nodeType=").append(m_nodeType).append(", inPorts=").append(m_inPorts)
                .append(", outPorts=").append(m_outPorts);
        return sb.append(']').toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(commonHash(), m_nodeType, Arrays.hashCode(m_icon), m_inPorts, m_outPorts);
    }

    /**
     * Creates a variant of these metadata with the number of in- and out-ports adjusted to the given numbers.
     *
     * @param numInPorts required number of in-ports
     * @param numOutPorts required number of out-ports
     * @return either this object itself (if it fulfills the requirements) or a copy with adjusted ports
     * @since 5.1
     */
    public ComponentMetadata withNumberOfPorts(final int numInPorts, final int numOutPorts) {
        if (m_inPorts.size() == numInPorts && m_outPorts.size() == numOutPorts) {
            return this;
        }

        List<Port> inPorts = m_inPorts;
        if (inPorts.size() != numInPorts) {
            inPorts = new ArrayList<>(numInPorts);
            for (var i = 0; i < numInPorts; i++) {
                inPorts.add(i < m_inPorts.size() ? m_inPorts.get(i) : new Port("Port " + (i + 1), ""));
            }
        }

        List<Port> outPorts = m_outPorts;
        if (outPorts.size() != numOutPorts) {
            outPorts = new ArrayList<>(numOutPorts);
            for (var i = 0; i < numOutPorts; i++) {
                outPorts.add(i < m_outPorts.size() ? m_outPorts.get(i) : new Port("Port " + (i + 1), ""));
            }
        }

        return new ComponentMetadata(m_contentType, m_lastModified, m_created, m_author, m_description, m_links,
            m_tags, m_nodeType, m_icon, inPorts, outPorts);
    }

    /**
     * Remove an output port from component metadata
     * Used for component port removal only, when removing a port other than the last one
     * Otherwise the port at the very end of the list will be truncated
     *
     * @param index the index of the port that should be removed and therefore moved to the end
     * @return updated {@code ComponentMetadata}
     * @since 5.5
     */
    public ComponentMetadata removeOutputPort(final int index) {
        List<Port> outPorts = new ArrayList<>(m_outPorts);
        outPorts.remove(index);
        return new ComponentMetadata(m_contentType, m_lastModified, m_created, m_author, m_description, m_links,
            m_tags, m_nodeType, m_icon, m_inPorts, outPorts);
    }

    /**
     * Remove an input port from component metadata
     * Used for component port removal only, when removing a port other than the last one
     * Otherwise the port at the very end of the list will be truncated
     *
     * @param index the index of the port that should be removed and therefore moved to the end
     * @return updated {@code ComponentMetadata}
     * @since 5.5
     */
    public ComponentMetadata removeInputPort(final int index) {
        List<Port> inPorts = new ArrayList<>(m_inPorts);
        inPorts.remove(index);
        return new ComponentMetadata(m_contentType, m_lastModified, m_created, m_author, m_description, m_links,
            m_tags, m_nodeType, m_icon, inPorts, m_outPorts);
    }

    /**
     * Load information from argument, throw {@link InvalidSettingsException} if that fails.
     *
     * @param settings To load from.
     * @param version The version this workflow is loading from
     * @return a new metadata object loaded from the argument settings.
     * @throws InvalidSettingsException If that fails.
     * @deprecated
     */
    @Deprecated(since = "5.1")
    public static ComponentMetadata load(final NodeSettingsRO settings, final LoadVersion version)
        throws InvalidSettingsException {
        if (!settings.containsKey(CFG_METADATA)) {
            return ComponentMetadata.NONE;
        }
        NodeSettingsRO nestedSettings = settings.getNodeSettings(CFG_METADATA);
        ComponentOptionalsBuilder builder = ComponentMetadata.fluentBuilder();
        builder.withComponentType(
            nestedSettings.containsKey("type") ? ComponentNodeType.valueOf(nestedSettings.getString("type")) : null);
        builder.withIcon(
            nestedSettings.containsKey("icon") ? Base64.getDecoder().decode(nestedSettings.getString("icon")) : null);

        if (nestedSettings.containsKey("inports")) {
            NodeSettingsRO inports = nestedSettings.getNodeSettings("inports");
            Set<String> keySet = inports.keySet();
            final var names = new String[keySet.size()];
            final var descs = new String[keySet.size()];
            for (String key : keySet) {
                NodeSettingsRO port = inports.getNodeSettings(key);
                final var index = port.getInt("index", -1);
                names[index] = port.getString("name");
                descs[index] = port.getString("description");
            }
            for (var i = 0; i < names.length; i++) {
                builder.withInPort(names[i], descs[i]);
            }
        }

        if (nestedSettings.containsKey("outports")) {
            NodeSettingsRO outports = nestedSettings.getNodeSettings("outports");
            Set<String> keySet = outports.keySet();
            final var names = new String[keySet.size()];
            final var descs = new String[keySet.size()];
            for (String key : keySet) {
                NodeSettingsRO port = outports.getNodeSettings(key);
                final var index = port.getInt("index", -1);
                names[index] = port.getString("name");
                descs[index] = port.getString("description");
            }
            for (var i = 0; i < names.length; i++) {
                builder.withOutPort(names[i], descs[i]);
            }
        }
        return builder.withPlainContent() //
                .withLastModifiedNow() //
                .withDescription(nestedSettings.getString("description", null)) //
                .build();
    }

    /**
     * @return a new builder instance
     * @since 5.1
     */
    public static ComponentOptionalsBuilder fluentBuilder() {
        return new FluentComponentMetadataBuilder();
    }

    /**
     * @return a new builder instance
     * @deprecated use {@link #fluentBuilder()} instead
     */
    @Deprecated(since = "5.1")
    public static ComponentMetadataBuilder builder() {
        return new ComponentMetadataBuilder();
    }

    /**
     * Helps building a component metadata instances.
     * @deprecated This builder is only kept because was used in `org.knime.workbench.editor` before 5.1.
     *      Use the fluent builder instead, via {@link #fluentBuilder()}.
     */
    @Deprecated(since = "5.1")
    public static class ComponentMetadataBuilder {

        private final ComponentOptionalsBuilder m_builder = fluentBuilder();

        private String m_description;

        /**
         * @param description description interpreted as plain text
         * @return this builder
         */
        public ComponentMetadataBuilder description(final String description) {
            m_description = description;
            return this;
        }

        /**
         * @param type
         * @return this builder
         */
        public ComponentMetadataBuilder type(final ComponentNodeType type) {
            m_builder.withComponentType(type);
            return this;
        }

        /**
         * @param icon
         * @return this builder
         */
        public ComponentMetadataBuilder icon(final byte[] icon) {
            m_builder.withIcon(icon);
            return this;
        }

        /**
         * Adds a new in-port name and description. The port index corresponds to the order of adding it.
         *
         * @param name the port name
         * @param description the port description (plain text)
         * @return this builder
         */
        public ComponentMetadataBuilder addInPortNameAndDescription(final String name, final String description) {
            m_builder.withInPort(name, description);
            return this;
        }

        /**
         * Adds a new out-port name and description. The port index corresponds to the order of adding it.
         *
         * @param name the port name
         * @param description the port description (plain text)
         * @return this builder
         */
        public ComponentMetadataBuilder addOutPortNameAndDescription(final String name, final String description) {
            m_builder.withOutPort(name, description);
            return this;
        }

        /**
         * NOT SUPPORTED!
         *
         * @return never
         * @throws UnsupportedOperationException
         */
        public ComponentMetadataBuilder clearInPorts() {
            throw new UnsupportedOperationException("Not supported any more, use the fluent builder");
        }

        /**
         * NOT SUPPORTED!
         *
         * @return never
         * @throws UnsupportedOperationException
         */
        public ComponentMetadataBuilder clearOutPorts() {
            throw new UnsupportedOperationException("Not supported any more, use the fluent builder");
        }

        /**
         * Creates a new instance from this builder.
         *
         * @return the new instance
         */
        public ComponentMetadata build() {
            return m_builder.withPlainContent() //
                    .withLastModifiedNow() //
                    .withDescription(m_description) //
                    .build();
        }

    }

    /**
     * Stage of the component metadata builder.
     * @since 5.1
     */
    public interface ComponentOptionalsBuilder extends NeedsContentType<ComponentMetadata> {

        /**
         * Sets the type of the component.
         *
         * @param componentType component type, e.g. {@link ComponentNodeType#VISUALIZER}
         * @return next stage of the builder
         */
        ComponentOptionalsBuilder withComponentType(ComponentNodeType componentType);

        /**
         * Sets the icon of the component.
         *
         * @param icon file contents of the icon
         * @return next stage of the builder
         */
        ComponentOptionalsBuilder withIcon(byte[] icon);

        /**
         * Adds an in-port of the component.
         *
         * @param name name of the in-port (may be {@code null})
         * @param description description of the in-port (may be {@code null})
         * @return next stage of the builder
         */
        ComponentOptionalsBuilder withInPort(String name, String description);

        /**
         * Adds an out-port of the component.
         *
         * @param name name of the out-port (may be {@code null})
         * @param description description of the out-port (may be {@code null})
         * @return next stage of the builder
         */
        ComponentOptionalsBuilder withOutPort(String name, String description);
    }

    private static final class FluentComponentMetadataBuilder extends NodeContainerMetadataBuilder<ComponentMetadata>
            implements ComponentOptionalsBuilder {

        private ComponentNodeType m_componentType;
        private byte[] m_icon;
        private final List<Port> m_inPorts = new ArrayList<>();
        private final List<Port> m_outPorts = new ArrayList<>();

        FluentComponentMetadataBuilder() {
        }

        @Override
        public ComponentMetadata build() {
            return new ComponentMetadata( //
                CheckUtils.checkArgumentNotNull(m_contentType, "Component metadata need a content type"), //
                CheckUtils.checkArgumentNotNull(m_lastModified, "Component metadata need a last-modified date"), //
                m_created, //
                m_author, //
                CheckUtils.checkArgumentNotNull(m_description, "Component metadata need a description"), //
                new ArrayList<>(m_links), //
                new ArrayList<>(m_tags), //
                m_componentType, //
                m_icon, //
                m_inPorts, //
                m_outPorts);
        }

        @Override
        public ComponentOptionalsBuilder withComponentType(final ComponentNodeType componentType) {
            m_componentType = componentType;
            return this;
        }

        @Override
        public ComponentOptionalsBuilder withIcon(final byte[] icon) {
            m_icon = icon;
            return this;
        }

        @Override
        public ComponentOptionalsBuilder withInPort(final String name, final String description) {
            m_inPorts.add(new Port(name, description));
            return this;
        }

        @Override
        public ComponentOptionalsBuilder withOutPort(final String name, final String description) {
            m_outPorts.add(new Port(name, description));
            return this;
        }
    }
}
