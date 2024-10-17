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

import java.util.Objects;

import org.knime.core.node.util.CheckUtils;

/** Pushed on top of the stack inside a try-catch construct. Stores reasons
 * for failures that are caught by the WorkflowManager to be reported by
 * the catch node.
 *
 * @author M. Berthold, KNIME AG, Zurich, Switzerland
 * @since 2.8
 */
public final class FlowTryCatchContext extends FlowScopeContext {

    /* static variable names for information put on the stack by the WFM */
    /**
     * @since 3.1
     */
    public static String ERROR_FLAG = "_error_caught";
    /**
     * @since 3.1
     */
    public static String ERROR_NODE = "_error_node";
    /**
     * @since 3.1
     */
    public static String ERROR_REASON = "_error_reason";
    /**
     * @since 5.4
     */
    public static final String ERROR_DETAILS = "_error_details";
    /**
     * @since 3.1
     */
    public static String ERROR_STACKTRACE = "_error_stacktrace";

    /* variables for information retrieved by the WFM */
    private boolean m_errorCaught;
    private String m_node;
    private String m_reason;
    private String m_details;
    private String m_stacktrace;

    /** Store information about an error that was caught.
     * @param node
     * @param reason
     * @param details
     * @param stacktrace
     * @since 5.4
     */
    public void setError(final String node, final String reason, final String details, final String stacktrace) {
        m_errorCaught = true;
        m_node = CheckUtils.checkArgumentNotNull(node);
        m_reason = CheckUtils.checkArgumentNotNull(reason);
        m_details = CheckUtils.checkArgumentNotNull(details);
        // stacktrace can be null
        m_stacktrace = stacktrace;
    }

    /**
     * Convenience method that ensures consistency between this {@link FlowTryCatchContext} and the
     * given per-node {@link FlowObjectStack} by passing the same arguments.
     * <p>
     * Uses {@link #setError(String, String, String, String)} internally.
     * </p>
     *
     * @param node the node name
     * @param reason the error reason
     * @param details more error details than the short reason
     * @param stacktrace nullable (thus optional) current stacktrace
     * @param fos the corresponding flow object stack
     * @since 5.4
     */
    public void setErrorToFlowObjectStack(final String node, final String reason, final String details,
        final String stacktrace, final FlowObjectStack fos) {
        final var nonNullDetails = Objects.requireNonNullElse(details, reason);

        // (1) Set to `FlowTryCatchContext`.
        setError(node, reason, nonNullDetails, stacktrace);

        // (2) Set to `FlowObjectStack`.
        fos.push(new FlowVariable(ERROR_FLAG, 1));
        fos.push(new FlowVariable(ERROR_NODE, node));
        fos.push(new FlowVariable(ERROR_REASON, reason));
        fos.push(new FlowVariable(ERROR_DETAILS, nonNullDetails));
        if (stacktrace != null) {
            fos.push(new FlowVariable(ERROR_STACKTRACE, stacktrace));
        }
    }

    /**
     * @return true if an error was caught
     * @since 3.1
     */
    public boolean hasErrorCaught() {
        return m_errorCaught;
    }

    /**
     * @return node
     * @since 3.1
     */
    public String getNode() {
        return m_node;
    }

    /**
     * @return reason
     * @since 3.1
     */
    public String getReason() {
        return m_reason;
    }

    /**
     * @return details
     * @since 5.4
     */
    public String getDetails() {
        return m_details;
    }

    /**
     * @return stacktrace
     * @since 3.1
     */
    public String getStacktrace() {
        return m_stacktrace;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Try-Catch Context";
    }

}
