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
 * ---------------------------------------------------------------------
 */
package org.knime.core.node.workflow.execresult;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Specialized execution result for {@link WorkflowManager}. Offers access
 * to all contained node's execution result.
 * @author Bernd Wiswedel, University of Konstanz
 */
public class WorkflowExecutionResult extends NodeContainerExecutionResult {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(WorkflowExecutionResult.class);

    private final Map<NodeID, NodeContainerExecutionResult> m_execResultMap;

    private final NodeID m_baseID;

    /**
     * Copy constructor.
     *
     * @param toCopy The instance to copy.
     * @since 3.5
     */
    public WorkflowExecutionResult(final WorkflowExecutionResultBuilder toCopy) {
        super(toCopy);
        m_baseID = toCopy.m_baseID;
        m_execResultMap = Collections.unmodifiableMap(toCopy.m_execResultMap);
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

    /** {@inheritDoc} */
    @Override
    public NodeContainerExecutionStatus getChildStatus(final int idSuffix) {
        NodeID id = new NodeID(m_baseID, idSuffix);
        NodeContainerExecutionStatus status = m_execResultMap.get(id);
        if (status == null) {
            LOGGER.debugWithFormat("No execution status for node with suffix %s; return FAILURE", idSuffix);
            status = NodeContainerExecutionStatus.FAILURE;
        }
        return status;
    }

    public static WorkflowExecutionResultBuilder builder(final NodeID baseID) {
        return new WorkflowExecutionResultBuilder(baseID);
    }

    /**
     * @param template
     * @return
     */
    public static WorkflowExecutionResultBuilder builder(final WorkflowExecutionResult template) {
        var result = new WorkflowExecutionResultBuilder(template);
        template.getExecutionResultMap().entrySet().stream()
            .forEach(e -> result.addNodeExecutionResult(e.getKey(), e.getValue()));
        return result;
    }

    public static final class WorkflowExecutionResultBuilder extends NodeContainerExecutionResultBuilder {

        private Map<NodeID, NodeContainerExecutionResult> m_execResultMap = new LinkedHashMap<>();

        private final NodeID m_baseID;

        /**
         * Creates new workflow execution result with no particular settings.
         * @param baseID The node id of the workflow (the loading procedure in
         * the target workflow will correct the prefix).
         * @throws NullPointerException If the argument is null.
         */
        private WorkflowExecutionResultBuilder(final NodeID baseID) {
            m_baseID = CheckUtils.checkArgumentNotNull(baseID);
        }

        /**
         * @param template
         */
        private WorkflowExecutionResultBuilder(final WorkflowExecutionResult template) {
            super(template);
            m_baseID = template.getBaseID();
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
            CheckUtils.checkArgument(id.hasSamePrefix(m_baseID), "Invalid prefix: %s, expected %s", id, m_baseID);
            return m_execResultMap.put(id, CheckUtils.checkArgumentNotNull(execResult)) == null;
        }

        @Override
        public WorkflowExecutionResult build() {
            return new WorkflowExecutionResult(this);
        }

    }
}
