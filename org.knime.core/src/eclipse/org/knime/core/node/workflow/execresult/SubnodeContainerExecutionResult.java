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

import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.SubNodeContainer;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Specialized execution result for {@link SubNodeContainer}. Wraps a {@link WorkflowExecutionResult}.
 *
 * @author Bernd Wiswedel, University of Konstanz
 * @since 2.12
 */
public final class SubnodeContainerExecutionResult extends NodeContainerExecutionResult {

    private final NodeID m_baseID;

    private final WorkflowExecutionResult m_workflowExecutionResult;

    private SubnodeContainerExecutionResult(final SubnodeContainerExecutionResultBuilder builder) {
        super(builder);
        m_baseID = builder.m_baseID;
        m_workflowExecutionResult = builder.m_workflowExecutionResult;
    }

    /**
     * @return the node id of the sub node container.
     * @since 3.5
     */
    @JsonProperty("baseID")
    public NodeID getBaseID() {
        return m_baseID;
    }

    /** @return Inner workflow execution result set vi. */
    @JsonProperty("workflowExecResult")
    public WorkflowExecutionResult getWorkflowExecutionResult() {
        return m_workflowExecutionResult;
    }

    /** {@inheritDoc} */
    @Override
    public NodeContainerExecutionStatus getChildStatus(final int idSuffix) {
        CheckUtils.checkArgument(idSuffix == 0, "Exec result of Component has only one child ('0'), got %d", idSuffix);
        CheckUtils.checkState(m_workflowExecutionResult != null, "No inner workflow result set");
        return m_workflowExecutionResult;
    }

    public static SubnodeContainerExecutionResultBuilder builder(final NodeID baseID) {
        return new SubnodeContainerExecutionResultBuilder(baseID);
    }

    public static SubnodeContainerExecutionResultBuilder builder(final SubnodeContainerExecutionResult template) {
        return new SubnodeContainerExecutionResultBuilder(template);
    }

    public static final class SubnodeContainerExecutionResultBuilder extends NodeContainerExecutionResultBuilder {

        private final NodeID m_baseID;

        private WorkflowExecutionResult m_workflowExecutionResult;

        private SubnodeContainerExecutionResultBuilder(final NodeID baseID) {
            m_baseID = CheckUtils.checkArgumentNotNull(baseID, "ID must not be null");
        }

        /**
         * @param template
         */
        public SubnodeContainerExecutionResultBuilder(final SubnodeContainerExecutionResult template) {
            super(template);
            m_baseID = template.getBaseID();
        }

        /**
         * Sets inner execution result.
         * @param workflowExecutionResult To be set, must have correct baseID.
         * @return this
         * @throws IllegalArgumentException If the id prefix is invalid or argument is null
         */
        @JsonProperty("workflowExecResult")
        public SubnodeContainerExecutionResultBuilder
            setWorkflowExecutionResult(final WorkflowExecutionResult workflowExecutionResult) {
            m_workflowExecutionResult = CheckUtils.checkArgumentNotNull(workflowExecutionResult);
            CheckUtils.checkArgument(new NodeID(m_baseID, 0).equals(m_workflowExecutionResult.getBaseID()),
                "Unexpected ID of inner wfm result, expected %s but got %s", new NodeID(m_baseID, 0),
                m_workflowExecutionResult.getBaseID());
            m_workflowExecutionResult = workflowExecutionResult;
            return this;
        }

        /**
         * @return ....
         */
        @Override
        public SubnodeContainerExecutionResult build() {
            return new SubnodeContainerExecutionResult(this);
        }

        /**
         * @return the workflowExecutionResult
         */
        WorkflowExecutionResult getWorkflowExecutionResult() {
            return m_workflowExecutionResult;
        }

    }
}
