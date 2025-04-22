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
 *   Jun 6, 2020 (hornm): created
 */
package org.knime.core.node.workflow.action;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.context.ModifiableNodeCreationConfiguration;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.ConnectionUIInformation;
import org.knime.core.node.workflow.NodeAnnotation;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.util.Pair;

/**
 * Result of the replace node operation, e.g.,
 * {@link WorkflowManager#replaceNode(NodeID, ModifiableNodeCreationConfiguration)}.
 *
 * Main purpose is 'undo'.
 *
 * @noreference This class is not intended to be referenced by clients.
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @since 4.2
 */
public final class ReplaceNodeResult {

    private final WorkflowManager m_wfm;

    private final NodeID m_replacedNodeID;

    private final List<ConnectionContainer> m_removedConnections;

    private final ModifiableNodeCreationConfiguration m_nodeCreationConfig;

    private final NodeFactory<?> m_originalNodeFactory;

    private final NodeSettings m_originalNodeSettings;

    private final NodeAnnotation m_originalNodeAnnotation;

    /**
     * Port mapping of the original operation. Only given for case of "custom" port mapping
     */
    private final Pair<Map<Integer, Integer>, Map<Integer, Integer>> m_portMappings;

    /**
     * New ReplaceNodeResult instance as result of a port removal operation
     * Used to store all information required to undo the port removal
     *
     * @param wfm the host workflow manager
     * @param replacedNodeID the id of the newly created node
     * @param removedConnections the connections that couldn't be restored after the replacement
     * @param originalNodeCreationConfig the original creation config of the old node (for the undo)
     * @param originalNodeFactory factory of the deleted node; or {@code null} if the old node is of the same type as
     *            the new one (i.e. replacement happened in order to change the port configuration)
     * @param originalNodeSettings the settings of the deleted node
     * @param originalNodeAnnotation the original node annotation of the deleted node; won't be restored, if
     *            {@code null}
     * @param portMappings mapping old port indices to new ones on port removal, used to reconnect after port removal
     * @since 5.5
     */
    public ReplaceNodeResult(final WorkflowManager wfm, final NodeID replacedNodeID, //NOSONAR
        final List<ConnectionContainer> removedConnections,
        final ModifiableNodeCreationConfiguration originalNodeCreationConfig, final NodeFactory<?> originalNodeFactory,
        final NodeSettings originalNodeSettings, final NodeAnnotation originalNodeAnnotation,
        final Pair<Map<Integer, Integer>, Map<Integer, Integer>> portMappings) {
        CheckUtils.checkNotNull(wfm);
        CheckUtils.checkNotNull(replacedNodeID);
        CheckUtils.checkNotNull(removedConnections);
        CheckUtils.checkNotNull(removedConnections);
        CheckUtils.checkNotNull(originalNodeSettings);
        m_wfm = wfm;
        m_replacedNodeID = replacedNodeID;
        m_removedConnections = removedConnections;
        m_nodeCreationConfig = originalNodeCreationConfig;
        m_originalNodeFactory = originalNodeFactory;
        m_originalNodeSettings = originalNodeSettings;
        m_originalNodeAnnotation = originalNodeAnnotation;
        m_portMappings = portMappings;
    }

    /**
     * New ReplaceNodeResult for any operation resulting in replacement of a node except port removal
     * Mainly used to store information required to undo the replace node operation
     *
     * @param wfm the host workflow manager
     * @param replacedNodeID the id of the newly created node
     * @param removedConnections the connections that couldn't be restored after the replacement
     * @param originalNodeCreationConfig the original creation config of the old node (for the undo)
     * @param originalNodeFactory factory of the deleted node; or {@code null} if the old node is of the same type as
     *            the new one (i.e. replacement happened in order to change the port configuration)
     * @param originalNodeSettings the settings of the deleted node
     * @param originalNodeAnnotation the original node annotation of the deleted node; won't be restored, if
     *            {@code null}
     */
    public ReplaceNodeResult(final WorkflowManager wfm, final NodeID replacedNodeID,
        final List<ConnectionContainer> removedConnections,
        final ModifiableNodeCreationConfiguration originalNodeCreationConfig, final NodeFactory<?> originalNodeFactory,
        final NodeSettings originalNodeSettings, final NodeAnnotation originalNodeAnnotation) {
        this(wfm, replacedNodeID, removedConnections, originalNodeCreationConfig, originalNodeFactory,
            originalNodeSettings, originalNodeAnnotation, null);
    }

    /**
     * @return whether the undo operation can be performed
     */
    public boolean canUndo() {
        return m_wfm.canReplaceNode(m_replacedNodeID);
    }

    /**
     * Performs the undo.
     */
    public void undo() {
        var portMappings = m_portMappings == null ? null : getPortMappingsForPortRemovalUndo();
        m_wfm.replaceNode(m_replacedNodeID, m_nodeCreationConfig, m_originalNodeFactory, false, portMappings);
        m_removedConnections.stream()
            .filter(c -> m_wfm.canAddConnection(c.getSource(), c.getSourcePort(), c.getDest(), c.getDestPort()))
            .forEach(c -> {
                var newConnection = m_wfm.addConnection(c.getSource(), c.getSourcePort(), c.getDest(), c.getDestPort());
                if (c.getUIInfo() != null) {
                    newConnection.setUIInfo(ConnectionUIInformation.builder(c.getUIInfo()).build());
                }
            });
        if (m_originalNodeAnnotation != null && !m_originalNodeAnnotation.getData().isDefault()) {
            m_wfm.getNodeContainer(m_replacedNodeID).getNodeAnnotation().copyFrom(m_originalNodeAnnotation.getData(),
                true);
        }
        try {
            m_wfm.loadNodeSettings(m_replacedNodeID, m_originalNodeSettings);
        } catch (InvalidSettingsException ex) {
            NodeLogger.getLogger(ReplaceNodeResult.class).error("Could not re-apply node settings on undo", ex);
        }
    }

    /*
     * map current port indices (after removal) to new indices (after removal undo)
     */
    private Pair<Map<Integer, Integer>, Map<Integer, Integer>> getPortMappingsForPortRemovalUndo() {
        return new Pair<>( //
            getUndoPortMapping(m_portMappings.getFirst()), //
            getUndoPortMapping(m_portMappings.getSecond()) //
        );
    }

    private static Map<Integer, Integer> getUndoPortMapping(final Map<Integer, Integer> originalMapping) {
        var removedPort = false;
        var incomingPortIndex = -1;
        //check if an input port has previously been removed by finding value -1
        for (var entry : originalMapping.entrySet()) {
            if (entry.getValue().equals(-1)) {
                removedPort = true;
                incomingPortIndex = entry.getKey();
            }
        }

        var map = new HashMap<Integer, Integer>();
        IntStream.range(0, originalMapping.size()).forEach(i -> map.put(i, i));
        if (removedPort) {
            // any connection pointing to port p on the replaced node should point to port p+1 on the replacing node
            //  (on which port p is now the re-added port)
            map.put(incomingPortIndex, incomingPortIndex + 1);
            IntStream.range(incomingPortIndex + 1, map.size()).forEach(i -> map.put(i, i + 1));
        }
        return map;
    }

}
