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

import org.knime.core.node.workflow.NodeMessage;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Remote execution result. Derived classes define specialized access
 * methods for SingleNodeContainer and WorkflowManager.
 * @author Bernd Wiswedel, University of Konstanz
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS)
public abstract class NodeContainerExecutionResult implements NodeContainerExecutionStatus {

    private final NodeMessage m_message;

    private final boolean m_needsResetAfterLoad;

    private final boolean m_isSuccess;

    /**
     * Copy constructor.
     *
     * @param toCopy Execution result instance to copy from.
     * @since 3.5
     */
    NodeContainerExecutionResult(final NodeContainerExecutionResultBuilder builder) {
        m_message = builder.m_message;
        m_needsResetAfterLoad = builder.m_needsResetAfterLoad;
        m_isSuccess = builder.m_isSuccess;
    }

    /** Get a node message that was set during execution.
     * @return The node message. */
    @JsonProperty("nodeMessage")
    public NodeMessage getNodeMessage() {
        return m_message;
    }

    /** @return true when the node needs to be reset after loading the results.
     * @see #setNeedsResetAfterLoad()
     */
    @JsonProperty("needsResetAfterLoad")
    public boolean needsResetAfterLoad() {
        return m_needsResetAfterLoad;
    }

    @Override
    public boolean isSuccess() {
        return m_isSuccess;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + (isSuccess() ? "success" : "failure");
    }

    public abstract static class NodeContainerExecutionResultBuilder {

        private NodeMessage m_message;

        private boolean m_needsResetAfterLoad;

        private boolean m_isSuccess;

        NodeContainerExecutionResultBuilder() {
        }

        NodeContainerExecutionResultBuilder(final NodeContainerExecutionResult template) {
            m_message = template.m_message;
            m_needsResetAfterLoad = template.needsResetAfterLoad();
            m_isSuccess = template.isSuccess();
        }

        /** Set a node message.
         * @param message the message to set
         * @return this
         */
        public NodeContainerExecutionResultBuilder setMessage(final NodeMessage message) {
            m_message = message;
            return this;
        }

        /** Request a reset of the node after loading the result. The node is
         * allowed to trigger a reset if the loading process causes errors that
         * invalidate the computed result.
         * @return this */
        public NodeContainerExecutionResultBuilder setNeedsResetAfterLoad() {
            m_needsResetAfterLoad = true;
            return this;
        }

        /**
         * @param isSuccess the isSuccess to set
         * @return this
         */
        public NodeContainerExecutionResultBuilder setSuccess(final boolean isSuccess) {
            m_isSuccess = isSuccess;
            return this;
        }

        public abstract NodeContainerExecutionResult build();

    }

}
