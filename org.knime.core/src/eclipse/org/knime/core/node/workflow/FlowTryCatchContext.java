/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 * ------------------------------------------------------------------------
 *
 */
package org.knime.core.node.workflow;

import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.ConvenienceMethods;

/** Pushed on top of the stack inside a try-catch construct. Stores reasons
 * for failures that are caught by the WorkflowManager to be reported by
 * the catch node.
 *
 * @author M. Berthold, KNIME.com, Zurich, Switzerland
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
     * @since 3.1
     */
    public static String ERROR_STACKTRACE = "_error_stacktrace";

    /* variables for information retrieved by the WFM */
    private boolean m_errorCaught = false;
    private String m_node;
    private String m_reason;
    private String m_stacktrace;

    /** Store information about an error that was caught.
     * @param node
     * @param reason
     * @param stacktrace
     * @since 3.1
     */
    public void setError(final String node, final String reason, final String stacktrace) {
        m_errorCaught = true;
        m_node = CheckUtils.checkArgumentNotNull(node);
        m_reason = CheckUtils.checkArgumentNotNull(reason);
        m_stacktrace = stacktrace;
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

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        if (m_errorCaught) {
            return super.hashCode() + m_node.hashCode() + m_reason.hashCode() + m_stacktrace.hashCode();
        }
        return super.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!obj.getClass().equals(getClass())) {
            return false;
        }
        FlowTryCatchContext ftcc = (FlowTryCatchContext) obj;
        return super.equals(obj)
            && ConvenienceMethods.areEqual(ftcc.m_node, m_node)
            && ConvenienceMethods.areEqual(ftcc.m_reason, m_reason)
            && ConvenienceMethods.areEqual(ftcc.m_stacktrace, m_stacktrace);
    }
}
