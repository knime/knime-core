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

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.util.LoadVersion;
import org.knime.core.util.Pair;

/**
 * Represents general metadata associated with a component (i.e. a {@link SubNodeContainer}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 *
 * @since 4.1
 */
public final class ComponentMetadata {
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
        VISUALIZER("Visualizer", NodeFactory.NodeType.Visualizer);


        private static final Map<NodeFactory.NodeType, ComponentNodeType> NODE_TYPE_DISPLAYABLE_MAP = new HashMap<>();

        /**
         * Given the {@code NodeFactory.NodeType}, return the mapped instance of this enum.
         *
         * @param nodeType
         * @return the instance of this enum which wraps the parameter value enum
         */
        public synchronized static ComponentNodeType getTypeForNodeType(final NodeFactory.NodeType nodeType) {
            if (NODE_TYPE_DISPLAYABLE_MAP.size() == 0) {
                for (final ComponentNodeType ct : ComponentNodeType.values()) {
                    NODE_TYPE_DISPLAYABLE_MAP.put(ct.getType(), ct);
                }
            }
            return NODE_TYPE_DISPLAYABLE_MAP.get(nodeType);
        }


        private final String m_displayText;
        private final NodeFactory.NodeType m_nodeType;

        private ComponentNodeType(final String name, final NodeFactory.NodeType nodeType) {
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

    /**
     * An empty metadata object.
     */
    public static final ComponentMetadata NONE = ComponentMetadata.builder().build();

    private static final String CFG_METADATA = "metadata";

    private final String m_description;

    private final ComponentNodeType m_type;

    private final byte[] m_icon;

    private final String[] m_inPortNames;

    private final String[] m_inPortDescriptions;

    private final String[] m_outPortNames;

    private final String[] m_outPortDescriptions;

    private ComponentMetadata(final ComponentMetadataBuilder builder) {
        m_description = builder.m_description;
        m_type = builder.m_type;
        m_icon = builder.m_icon != null ? builder.m_icon.clone() : null;
        if (!builder.m_inPorts.isEmpty()) {
            m_inPortNames = builder.m_inPorts.stream().map(p -> p.getFirst()).toArray(s -> new String[s]);
            m_inPortDescriptions = builder.m_inPorts.stream().map(p -> p.getSecond()).toArray(s -> new String[s]);
        } else {
            m_inPortNames = null;
            m_inPortDescriptions = null;
        }
        if (!builder.m_outPorts.isEmpty()) {
            m_outPortNames = builder.m_outPorts.stream().map(p -> p.getFirst()).toArray(s -> new String[s]);
            m_outPortDescriptions = builder.m_outPorts.stream().map(p -> p.getSecond()).toArray(s -> new String[s]);
        } else {
            m_outPortNames = null;
            m_outPortDescriptions = null;
        }
    }

    /**
     * @return the description
     */
    public Optional<String> getDescription() {
        return Optional.ofNullable(m_description);
    }

    /**
     * @return the type
     */
    public Optional<ComponentNodeType> getNodeType() {
        return Optional.ofNullable(m_type);
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
        return Optional.ofNullable(m_inPortNames);
    }

    /**
     * @return in-port descriptions
     */
    public Optional<String[]> getInPortDescriptions() {
        return Optional.ofNullable(m_inPortDescriptions);
    }

    /**
     * @return out-port names
     */
    public Optional<String[]> getOutPortNames() {
        return Optional.ofNullable(m_outPortNames);
    }

    /**
     * @return out-port descriptions
     */
    public Optional<String[]> getOutPortDescriptions() {
        return Optional.ofNullable(m_outPortDescriptions);
    }

    /**
     * Load information from argument, throw {@link InvalidSettingsException} if that fails.
     *
     * @param settings To load from.
     * @param version The version this workflow is loading from
     * @return a new metadata object loaded from the argument settings.
     * @throws InvalidSettingsException If that fails.
     */
    public static ComponentMetadata load(final NodeSettingsRO settings, final LoadVersion version)
        throws InvalidSettingsException {
        if (!settings.containsKey(CFG_METADATA)) {
            return ComponentMetadata.NONE;
        }
        NodeSettingsRO nestedSettings = settings.getNodeSettings(CFG_METADATA);
        ComponentMetadataBuilder builder = ComponentMetadata.builder();
        builder.description(nestedSettings.getString("description", null));
        builder.type(
            nestedSettings.containsKey("type") ? ComponentNodeType.valueOf(nestedSettings.getString("type")) : null);
        builder.icon(
            nestedSettings.containsKey("icon") ? Base64.getDecoder().decode(nestedSettings.getString("icon")) : null);

        if (nestedSettings.containsKey("inports")) {
            NodeSettingsRO inports = nestedSettings.getNodeSettings("inports");
            Set<String> keySet = inports.keySet();
            String[] names = new String[keySet.size()];
            String[] descs = new String[keySet.size()];
            for (String key : keySet) {
                NodeSettingsRO port = inports.getNodeSettings(key);
                int index = port.getInt("index", -1);
                names[index] = port.getString("name");
                descs[index] = port.getString("description");
            }
            for (int i = 0; i < names.length; i++) {
                builder.addInPortNameAndDescription(names[i], descs[i]);
            }
        }

        if (nestedSettings.containsKey("outports")) {
            NodeSettingsRO outports = nestedSettings.getNodeSettings("outports");
            Set<String> keySet = outports.keySet();
            String[] names = new String[keySet.size()];
            String[] descs = new String[keySet.size()];
            for (String key : keySet) {
                NodeSettingsRO port = outports.getNodeSettings(key);
                int index = port.getInt("index", -1);
                names[index] = port.getString("name");
                descs[index] = port.getString("description");
            }
            for (int i = 0; i < names.length; i++) {
                builder.addOutPortNameAndDescription(names[i], descs[i]);
            }
        }
        return builder.build();
    }

    /**
     * Saves this object to the argument settings.
     *
     * @param settings To save to.
     */
    public void save(final NodeSettingsWO settings) {
        if (this == NONE) {
            return;
        }
        NodeSettingsWO nestedSettings = settings.addNodeSettings(CFG_METADATA);
        if (m_description != null) {
            nestedSettings.addString("description", m_description);
        }
        if (m_type != null) {
            nestedSettings.addString("type", m_type.toString());
        }
        if (m_icon != null) {
            nestedSettings.addString("icon", Base64.getEncoder().encodeToString(m_icon));
        }
        if (m_inPortNames != null) {
            NodeSettingsWO ports = nestedSettings.addNodeSettings("inports");
            for (int i = 0; i < m_inPortNames.length; i++) {
                NodeSettingsWO port = ports.addNodeSettings("inport_" + i);
                port.addString("name", m_inPortNames[i]);
                port.addString("description", m_inPortDescriptions[i]);
                port.addInt("index", i);
            }
        }
        if (m_outPortNames != null) {
            NodeSettingsWO ports = nestedSettings.addNodeSettings("outports");
            for (int i = 0; i < m_outPortNames.length; i++) {
                NodeSettingsWO port = ports.addNodeSettings("outport_" + i);
                port.addString("name", m_outPortNames[i]);
                port.addString("description", m_outPortDescriptions[i]);
                port.addInt("index", i);
            }
        }
    }

    /**
     * @return a new builder instance
     */
    public static ComponentMetadataBuilder builder() {
        return new ComponentMetadataBuilder();
    }

    /**
     * Copy builder.
     *
     * @param metadata the metadata to initialize the builder with
     * @return a new builder instance with pre-initialized with the properties of the passed metadata object
     */
    public static ComponentMetadataBuilder builder(final ComponentMetadata metadata) {
        ComponentMetadataBuilder builder = new ComponentMetadataBuilder();
        builder.m_description = metadata.m_description;
        if (metadata.m_icon != null) {
            builder.m_icon = metadata.m_icon.clone();
        }
        builder.m_type = metadata.m_type;
        builder.m_inPorts = new ArrayList<>();
        if (metadata.m_inPortNames != null) {
            for (int i = 0; i < metadata.m_inPortNames.length; i++) {
                builder.m_inPorts.add(Pair.create(metadata.m_inPortNames[i], metadata.m_inPortDescriptions[i]));
            }
        }
        if (metadata.m_outPortNames != null) {
            for (int i = 0; i < metadata.m_outPortNames.length; i++) {
                builder.m_outPorts.add(Pair.create(metadata.m_outPortNames[i], metadata.m_outPortDescriptions[i]));
            }
        }
        return builder;
    }

    /**
     * Helps building a component metadata instances.
     */
    public static class ComponentMetadataBuilder {

        private String m_description;

        private byte[] m_icon;

        private ComponentNodeType m_type;

        private List<Pair<String, String>> m_inPorts = new ArrayList<>();

        private List<Pair<String, String>> m_outPorts = new ArrayList<>();

        /**
         * @param description
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
            m_type = type;
            return this;
        }

        /**
         * @param icon
         * @return this builder
         */
        public ComponentMetadataBuilder icon(final byte[] icon) {
            m_icon = icon;
            return this;
        }

        /**
         * Adds a new in-port name and description. The port index corresponds to the order of adding it.
         *
         * @param name the port name
         * @param description the port description
         * @return this builder
         */
        public ComponentMetadataBuilder addInPortNameAndDescription(final String name, final String description) {
            m_inPorts.add(Pair.create(name, description));
            return this;
        }

        /**
         * Adds a new out-port name and description. The port index corresponds to the order of adding it.
         *
         * @param name the port name
         * @param description the port description
         * @return this builder
         */
        public ComponentMetadataBuilder addOutPortNameAndDescription(final String name, final String description) {
            m_outPorts.add(Pair.create(name, description));
            return this;
        }

        /**
         * Removes the already added in-port names and descriptions.
         *
         * @return this builder
         */
        public ComponentMetadataBuilder clearInPorts() {
            m_inPorts.clear();
            return this;
        }

        /**
         * Removes the already added out-port names and descriptions.
         *
         * @return this builder
         */
        public ComponentMetadataBuilder clearOutPorts() {
            m_outPorts.clear();
            return this;
        }

        /**
         * Creates a new instance from this builder.
         *
         * @return the new instance
         */
        public ComponentMetadata build() {
            return new ComponentMetadata(this);
        }

    }

}
