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
 *
 */
package org.knime.core.node.workflow.execresult;

import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.SingleNodeContainer;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Specialized execution result for {@link SingleNodeContainer}. Offers access
 * to the node's execution result (containing port objects and possibly
 * internals).
 * @author Bernd Wiswedel, University of Konstanz
 * @since 3.1
 */
public class NativeNodeContainerExecutionResult extends NodeContainerExecutionResult {

    private final NodeExecutionResult m_nodeExecutionResult;

    /**
     * Copy constructor.
     *
     * @param builder Execution result that should be copied.
     */
    NativeNodeContainerExecutionResult(final NativeNodeContainerExecutionResultBuilder builder) {
        super(builder);
        m_nodeExecutionResult = CheckUtils.checkArgumentNotNull(builder.m_nodeExecutionResult);
    }

    /** @return The execution result for the node. */
    @JsonProperty
    public NodeExecutionResult getNodeExecutionResult() {
        return m_nodeExecutionResult;
    }

    /** {@inheritDoc} */
    @Override
    public NodeContainerExecutionStatus getChildStatus(final int idSuffix) {
        throw new IllegalStateException(getClass().getSimpleName() + " has no children");
    }

    public static NativeNodeContainerExecutionResultBuilder builder() {
        return new NativeNodeContainerExecutionResultBuilder();
    }

    public static NativeNodeContainerExecutionResultBuilder builder(final NativeNodeContainerExecutionResult template) {
        return new NativeNodeContainerExecutionResultBuilder(template);
    }

    public static final class NativeNodeContainerExecutionResultBuilder extends NodeContainerExecutionResultBuilder {

        NodeExecutionResult m_nodeExecutionResult;

        private NativeNodeContainerExecutionResultBuilder() {
        }

        private NativeNodeContainerExecutionResultBuilder(final NativeNodeContainerExecutionResult template) {
            super(template);
            setNodeExecutionResult(template.getNodeExecutionResult());
        }

        public NativeNodeContainerExecutionResultBuilder setNodeExecutionResult(final NodeExecutionResult r) {
            m_nodeExecutionResult = r;
            return this;
        }

        @Override
        public NativeNodeContainerExecutionResultBuilder setSuccess(final boolean isSuccess) {
            return (NativeNodeContainerExecutionResultBuilder)super.setSuccess(isSuccess);
        }

        @Override
        public NativeNodeContainerExecutionResult build() {
            return new NativeNodeContainerExecutionResult(this);
        }

    }

}
