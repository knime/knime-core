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
 *   Sep 29, 2021 (hornm): created
 */
package org.knime.gateway.api.entity;

import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeMessage.Type;

/**
 * Represents some basic information on a node required to display node views in the UI (e.g. consumed by the 'page
 * builder' frontend library).
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("javadoc")
public final class NodeInfoEnt {

    private final String m_name;

    private final String m_annotation;

    private final String m_state;

    private final String m_errorMessage;

    private final String m_warningMessage;

    private final Boolean m_canExecute;

    NodeInfoEnt(final NativeNodeContainer nnc) {
        this(nnc, null);
    }

    /**
     * @param nnc
     * @param errorMessage a custom error message (will replace a potentially available error message provided by the
     *            node)
     */
    NodeInfoEnt(final NativeNodeContainer nnc, final String errorMessage) {
        m_name = nnc.getName();
        m_annotation = nnc.getNodeAnnotation().toString();
        var state = nnc.getNodeContainerState();

        if (state.isIdle()) {
            m_state = "idle";
        } else if (state.isConfigured()) {
            m_state = "configured";
        } else if (state.isExecutionInProgress() || state.isExecutingRemotely()) {
            m_state = "executing";
        } else if (state.isExecuted()) {
            m_state = "executed";
        } else {
            m_state = "undefined";
        }
        var message = nnc.getNodeMessage();
        var messageType = message.getMessageType();
        if (errorMessage != null) {
            m_errorMessage = errorMessage;
        } else {
            m_errorMessage = messageType == Type.ERROR ? message.getMessage() : null;
        }
        m_warningMessage = messageType == Type.WARNING ? message.getMessage() : null;
        m_canExecute = state.isExecuted() || state.isExecutionInProgress() || state.isExecutingRemotely() ? null
            : nnc.getParent().canExecuteNode(nnc.getID());
    }

    public String getNodeName() {
        return m_name;
    }

    public String getNodeAnnotation() {
        return m_annotation;
    }

    public String getNodeState() {
        return m_state;
    }

    public String getNodeErrorMessage() {
        return m_errorMessage;
    }

    public String getNodeWarnMessage() {
        return m_warningMessage;
    }

    /**
     * @return whether the node can be executed; only present if the node isn't already executed or executing
     */
    public Boolean getCanExecute() {
        return m_canExecute;
    }

}
