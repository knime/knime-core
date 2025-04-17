/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *   Mar 24, 2024 (wiswedel): created
 */
package org.knime.core.customization;

import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.customization.kai.KAICustomization;
import org.knime.core.customization.mountpoint.MountPointCustomization;
import org.knime.core.customization.nodes.NodesCustomization;
import org.knime.core.customization.ui.UICustomization;
import org.knime.core.customization.workflow.WorkflowCustomization;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the customization settings for the KNIME AP, currently only determining which nodes are allowed for use
 * and listing in the node repository.
 *
 * Instances of this class are not created directly, only restored from YAML that looks similar to this:
 *
 * <pre>
 * version: 'customization-v1.0'
 * nodes:
 *   filter:
 *     - scope: view
 *       rule: allow
 *       predicate:
 *         type: pattern
 *         patterns:
 *           - org\\.knime\\.base\\..+
 *         isRegex: true
 * ui:
 *   menuEntries:
 *     - name: 'Company Help Portal'
 *       link: 'https://help.company.com/knime'
 * workflow:
 *   disablePasswordSaving: true
 * kai:
 *   suggestExtensions: true
 * mountpoint:
 *   filter:
 *     - rule: allow
 *       predicate:
 *         type: pattern
 *         patterns:
 *           - .+\\.hub\\.knime\\.com
 *         isRegex: true
 * </pre>
 *
 * @since 5.3
 * @noreference This class is not intended to be referenced by clients.
 * @author Bernd Wiswedel
 */
public final class APCustomization {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(APCustomization.class);

    static final String VERSION_PREFIX = "customization-";

    /**
     * Known customization versions, e.g. {@code customization-v1.0}.
     */
    enum CustomizationVersion {

        V1("v1.0"),

        FUTURE(null);

        private final String m_versionSuffix;

        CustomizationVersion(final String versionSuffix) {
            m_versionSuffix = versionSuffix;
        }

        static CustomizationVersion of(final String str) {
            CheckUtils.checkArgument(StringUtils.isNotBlank(str), "Version field must not be missing/blank");
            CheckUtils.checkArgument(StringUtils.startsWith(str, VERSION_PREFIX),
                "Version string does not start with \"%s\", e.g. \"%s\": \"%s\")", //
                VERSION_PREFIX, VERSION_PREFIX + V1.m_versionSuffix, str);
            final var versionSuffix = StringUtils.removeStart(str, VERSION_PREFIX);
            CheckUtils.checkArgument(StringUtils.isNotBlank(versionSuffix),
                "Version string must not be empty after removing prefix \"%s\": \"%s\"", VERSION_PREFIX, str);
            final CustomizationVersion version = Stream.of(values())
                .filter(v -> StringUtils.equals(v.m_versionSuffix, versionSuffix)).findFirst().orElse(FUTURE);
            if (version == FUTURE) {
                LOGGER.infoWithFormat("Unknown customization version \"%s\", assuming future version", str);
            }
            return version;
        }

        /** @return the full version string that can be parsed by {@link #of(String)}. */
        String fullVersion() {
            return VERSION_PREFIX + m_versionSuffix;
        }

    }

    /**
     * Default (no) customization.
     */
    public static final APCustomization DEFAULT = new APCustomization(CustomizationVersion.V1.fullVersion(), //
        NodesCustomization.DEFAULT, //
        UICustomization.DEFAULT, //
        WorkflowCustomization.DEFAULT, //
        KAICustomization.DEFAULT, //
        MountPointCustomization.DEFAULT //
    );

    private final NodesCustomization m_nodesCustomization;

    private final UICustomization m_uiCustomization;

    private final WorkflowCustomization m_workflowCustomization;

    private final KAICustomization m_kaiCustomization;

    private final MountPointCustomization m_mountpointCustomization;

    /**
     * Only used for deserialization.
     */
    @JsonCreator
    APCustomization( //
        @JsonProperty("version") final String version,
        @JsonProperty("nodes") final NodesCustomization nodesCustomization,
        @JsonProperty("ui") final UICustomization uiCustomization,
        @JsonProperty("workflow") final WorkflowCustomization workflowCustomization,
        @JsonProperty("kai") final KAICustomization kaiCustomization,
        @JsonProperty("mountpoint") final MountPointCustomization mountpointCustomization) {
        CustomizationVersion.of(version); // just to check if version is known
        m_nodesCustomization = Objects.requireNonNullElse(nodesCustomization, NodesCustomization.DEFAULT);
        m_uiCustomization = Objects.requireNonNullElse(uiCustomization, UICustomization.DEFAULT);
        m_workflowCustomization = Objects.requireNonNullElse(workflowCustomization, WorkflowCustomization.DEFAULT);
        m_kaiCustomization = Objects.requireNonNullElse(kaiCustomization, KAICustomization.DEFAULT);
        m_mountpointCustomization =
            Objects.requireNonNullElse(mountpointCustomization, MountPointCustomization.DEFAULT);
    }

    /**
     * @return customization for nodes
     */
    public NodesCustomization nodes() {
        return m_nodesCustomization;
    }

    /**
     * @return customization of the UI.
     */
    public UICustomization ui() {
        return m_uiCustomization;
    }

    /**
     * @return customization of workflow properties.
     */
    public WorkflowCustomization workflow() {
        return m_workflowCustomization;
    }

    /**
     * @return customization of K-AI
     */
    public KAICustomization kai() {
        return m_kaiCustomization;
    }

    /**
     * @return customization for mountpoints
     * @since 5.5
     */
    public MountPointCustomization mountpoint() {
        return m_mountpointCustomization;
    }

    @Override
    public String toString() {
        return String.format(
            "APCustomization{nodesCustomization=%s, uiCustomization=%s, workflowCustomization=%s, kaiCustomization=%s, "
                + "mountpointCustomization=%s}",
            m_nodesCustomization, m_uiCustomization, m_workflowCustomization, m_kaiCustomization,
            m_mountpointCustomization);
    }
}
