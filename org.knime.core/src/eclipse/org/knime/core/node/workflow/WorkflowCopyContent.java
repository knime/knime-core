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
 * ------------------------------------------------------------------------
 *
 */
package org.knime.core.node.workflow;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.knime.core.node.util.CheckUtils;



/**
 * Class representing node IDs and workflow annotations (rather it's ids) that need to be
 * copied from a workflow. Both, node IDs and annotation IDs must be contained in the
 * workflow that is copied from.
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public final class WorkflowCopyContent {

    private final List<NodeID> m_nodeIDs;
    private final List<WorkflowAnnotationID> m_annotationIDs;
    /** see {@link #setIncludeInOutConnections(boolean)}. */
    private final boolean m_isIncludeInOutConnections;
    /** A map which maps old NodeID to preferred ID suffix in the target wfm. Used for template loading. */
    private final Map<NodeID, Integer> m_suggestedNodeIDSuffixMap;
    /** A map which maps old NodeID to UI infos in the target wfm. Used for template loading. */
    private final Map<NodeID, NodeUIInformation> m_uiInfoMap;
    /** Offset by which nodes and connection bend points are moved in the target workflow. Often null. */
    private final Optional<int[]> m_positionOffset;

    /**
     * Creates a new object from the given builder. All mutable fields from the builder are copied!
     */
    private WorkflowCopyContent(final Builder builder) {
        if (builder.m_nodeIDs != null) {
            m_nodeIDs = Collections.unmodifiableList(Arrays.asList(builder.m_nodeIDs));
        } else {
            m_nodeIDs = Collections.emptyList();
        }
        if (builder.m_annotationIDs != null) {
            m_annotationIDs = Collections.unmodifiableList(Arrays.asList(builder.m_annotationIDs));
        } else {
            m_annotationIDs = Collections.emptyList();
        }
        m_isIncludeInOutConnections = builder.m_isIncludeInOutConnections;
        if (builder.m_suggestedNodeIDSuffixMap != null) {
            m_suggestedNodeIDSuffixMap = new HashMap<NodeID, Integer>(builder.m_suggestedNodeIDSuffixMap);
        } else {
            m_suggestedNodeIDSuffixMap = new HashMap<NodeID, Integer>(0);
        }
        if (builder.m_uiInfoMap != null) {
            m_uiInfoMap = new HashMap<NodeID, NodeUIInformation>(builder.m_uiInfoMap);
        } else {
            m_uiInfoMap = new HashMap<NodeID, NodeUIInformation>(0);
        }
        m_positionOffset = Optional.ofNullable(builder.m_positionOffset);
    }

    /** @return the ids as a newly created array */
    public NodeID[] getNodeIDs() {
        return m_nodeIDs.toArray(new NodeID[m_nodeIDs.size()]);
    }

    /** The overwritten NodeID suffix to the given node or null if not overwritten.
     * @param id The ID in question.
     * @return Null or the suffix.
     * @since 3.5*/
    public Integer getSuggestedNodIDSuffix(final NodeID id) {
        return m_suggestedNodeIDSuffixMap == null ? null : m_suggestedNodeIDSuffixMap.get(id);
    }

    /** Get overwritten UIInfo to node with given ID or null.
     * @param id ...
     * @return ...
     * @since 3.5
     */
    public NodeUIInformation getOverwrittenUIInfo(final NodeID id) {
        return m_uiInfoMap == null ? null : m_uiInfoMap.get(id);
    }

    /** see {@link Builder#setIncludeInOutConnections(boolean)}.
     * @return the isIncludeInOutConnections */
    public boolean isIncludeInOutConnections() {
        return m_isIncludeInOutConnections;
    }

    /** @return the annotation ids as a newly created array, never null
     * @since 3.7*/
    public WorkflowAnnotationID[] getAnnotationIDs() {
    	return m_annotationIDs.toArray(new WorkflowAnnotationID[m_annotationIDs.size()]);
    }

    /**
     * An offset by which nodes and connection bend points are shifted when inserted into the final workflow. Value
     * is either <code>null</code> or a two-dimensional array.
     * @return the positionOffset that array
     * @since 4.2
     */
    public Optional<int[]> getPositionOffset() {
        return m_positionOffset;
    }

    /**
     * @return a new {@link Builder} with default values.
     * @since 3.5
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to create immutable objects of {@link WorkflowCopyContent}.
     * @since 3.5
     */
    public static class Builder {

        private NodeID[] m_nodeIDs;
        private WorkflowAnnotationID[] m_annotationIDs;
        /** see {@link #setIncludeInOutConnections(boolean)}. */
        private boolean m_isIncludeInOutConnections;

        /**
         * A map which maps old NodeID to preferred ID suffix in the target wfm. When updating a linked metanode or
         * component, the content will be inserted via copy & paste. Since the new instance is supposed to have the same
         * node ID as the old one it can be set via {@link #setNodeID(NodeID, int, NodeUIInformation)}.
         */
        private Map<NodeID, Integer> m_suggestedNodeIDSuffixMap;

        /**
         * A map which maps old NodeID to UI infos in the target wfm. Similar to {@link #m_suggestedNodeIDSuffixMap},
         * only used for updating metanodes or components. To insert the updated content in the same position as the
         * outdated node, the ui info can be set via {@link #setNodeID(NodeID, int, NodeUIInformation)}
         */
        private Map<NodeID, NodeUIInformation> m_uiInfoMap;

        /** A offset by which nodes and connection bend points are shifted when inserted into the final workflow. Value
         * is either <code>null</code> or a two-dimensional array.
         */
        private int[] m_positionOffset;

        private Builder() {
            //
        }

        /** @param ids the ids to set
         * @return this*/
        public Builder setNodeIDs(final NodeID... ids) {
            m_nodeIDs = ids;
            return this;
        }

        /** Set the offset as described in {@link WorkflowCopyContent#getPositionOffset()}.
         * @param offset null or an array of length 2 with {x, y} shifts
         * @return this
         * @since 4.2
         */
        @SuppressWarnings("null")
        public Builder setPositionOffset(final int[] offset) {
            CheckUtils.checkArgument(offset == null || offset.length == 2,
                "Offset argument must be null or have length 2: %d", offset.length);
            m_positionOffset = offset;
            return this;
        }

        /** Used when copying from metanode template space.
         * @param id The ID of the metanode in the template root workflow
         * @param suggestedNodeIDSuffix The suffix to be used in the target workflow (overwrite it)
         * @param uiInfo The UIInfo the in the target workflow (also overwritten)
         * @return this
         */
        public Builder setNodeID(final NodeID id, final int suggestedNodeIDSuffix, final NodeUIInformation uiInfo) {
            m_nodeIDs = new NodeID[] {id};
            m_suggestedNodeIDSuffixMap = Collections.singletonMap(id, suggestedNodeIDSuffix);
            m_uiInfoMap = Collections.singletonMap(id, uiInfo);
            return this;
        }

        /** Set whether connections that link to or from any of the contained nodes
         * should be included in the copy content. Connections whose source and
         * destination are part of the {@link #getNodeIDs() NodeIDs set} are
         * automatically included, this property determines whether connections
         * connecting to this island are included as well.
         * @param isIncludeInOutConnections the isIncludeInOutConnections to set
         * @return this*/
        public Builder setIncludeInOutConnections(
                final boolean isIncludeInOutConnections) {
            m_isIncludeInOutConnections = isIncludeInOutConnections;
            return this;
        }

        /** Sets annotation ids.
         * @param annotationIDs the annotation ids
         * @return this;
         * @since 3.7
         */
        public Builder setAnnotationIDs(final WorkflowAnnotationID... annotationIDs) {
            m_annotationIDs = annotationIDs;
            return this;
        }

        /** Creates a new instance of {@link WorkflowCopyContent} from the builder. All immutable fields are copied!
         * @return the new instance*/
        public WorkflowCopyContent build() {
            return new WorkflowCopyContent(this);
        }

    }

}
