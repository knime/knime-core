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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 */
package org.knime.core.node.workflow.execresult;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.ClassUtils;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Specialized execution result for {@link WorkflowManager}. Offers access
 * to all contained node's execution result.
 * @author Bernd Wiswedel, University of Konstanz
 */
public class WorkflowExecutionResult extends NodeContainerExecutionResult {

    private final static NodeLogger LOGGER = NodeLogger.getLogger(WorkflowExecutionResult.class);

    private Map<NodeID, NodeContainerExecutionResult> m_execResultMap =
        new LinkedHashMap<NodeID, NodeContainerExecutionResult>();

    private final NodeID m_baseID;

    /**
     * Creates new workflow execution result with no particular settings.
     * @param baseID The node id of the workflow (the loading procedure in
     * the target workflow will correct the prefix).
     * @throws NullPointerException If the argument is null.
     */
    @JsonCreator
    public WorkflowExecutionResult(@JsonProperty("baseID") final NodeID baseID) {
        if (baseID == null) {
            throw new NullPointerException();
        }
        m_baseID = baseID;
    }

    /**
     * Copy constructor.
     *
     * @param toCopy The instance to copy.
     * @since 3.5
     */
    public WorkflowExecutionResult(final WorkflowExecutionResult toCopy) {
        super(toCopy);
        m_baseID = toCopy.m_baseID;

        for (Entry<NodeID, NodeContainerExecutionResult> entry : toCopy.m_execResultMap.entrySet()) {
            final NodeContainerExecutionResult subresultToCopy = entry.getValue();
            if (subresultToCopy instanceof NativeNodeContainerExecutionResult) {
                m_execResultMap.put(entry.getKey(),
                    new NativeNodeContainerExecutionResult((NativeNodeContainerExecutionResult)subresultToCopy));
            } else if (subresultToCopy instanceof SubnodeContainerExecutionResult) {
                m_execResultMap.put(entry.getKey(),
                    new SubnodeContainerExecutionResult((SubnodeContainerExecutionResult)subresultToCopy));
            } else if (subresultToCopy instanceof WorkflowExecutionResult) {
                m_execResultMap.put(entry.getKey(),
                    new WorkflowExecutionResult((WorkflowExecutionResult)subresultToCopy));
            } else {
                throw new IllegalStateException(
                    "Unknown ExecutionResult class: " + ClassUtils.getShortCanonicalName(subresultToCopy, "<null>"));
            }
        }
    }

    /** @return The base id of the workflow. Used to amend the node ids in
     * {@link #getExecutionResultMap()}. */
    public NodeID getBaseID() {
        return m_baseID;
    }

    /** @return The map containing node id to their execution result,
     * never null. */
    @JsonProperty("execResults")
    public Map<NodeID, NodeContainerExecutionResult> getExecutionResultMap() {
        return m_execResultMap;
    }

    /**
     * Private setter for the internal execution result map. For deserialization purposes only.
     *
     * @param resultMap A map with NodeContainerExecutionResults
     */
    @JsonProperty("execResults")
    private void setExecutionResultMap(final Map<NodeID, NodeContainerExecutionResult> resultMap) {
        m_execResultMap = resultMap;
    }

    /**
     * Adds the execution result for a child node.
     * @param id The node id of the child, it must have the "correct" prefix.
     * @param execResult The execution result for the child
     * @return <code>true</code> if this map did not contain an entry for
     *         this child before.
     * @throws IllegalArgumentException If the id prefix is invalid
     * @throws NullPointerException If either argument is null
     */
    public boolean addNodeExecutionResult(final NodeID id,
            final NodeContainerExecutionResult execResult) {
        if (execResult == null || id == null) {
            throw new NullPointerException();
        }
        if (!id.hasSamePrefix(m_baseID)) {
            throw new IllegalArgumentException("Invalid prefix: " + id
                    + ", expected " + m_baseID);
        }
        return m_execResultMap.put(id, execResult) == null;
    }

    /** {@inheritDoc} */
    @Override
    public NodeContainerExecutionStatus getChildStatus(final int idSuffix) {
        NodeID id = new NodeID(m_baseID, idSuffix);
        NodeContainerExecutionStatus status = m_execResultMap.get(id);
        if (status == null) {
            LOGGER.debug("No execution status for node with suffix "
                    + idSuffix + "; return FAILURE");
            status = NodeContainerExecutionStatus.FAILURE;
        }
        return status;
    }
}
