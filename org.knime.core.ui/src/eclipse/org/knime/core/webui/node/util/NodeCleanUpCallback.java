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
 *   Feb 18, 2022 (hornm): created
 */
package org.knime.core.webui.node.util;

import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeStateChangeListener;
import org.knime.core.node.workflow.NodeStateEvent;
import org.knime.core.node.workflow.WorkflowEvent;
import org.knime.core.node.workflow.WorkflowListener;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * Helper to clean-up after a node removal, workflow disposal or (optionally) node state change. Once a clean-up has
 * been triggered, all the registered listeners (on the node and the workflow) are removed which renders the
 * NodeCleanUpCallback-instance useless afterwards.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public final class NodeCleanUpCallback implements WorkflowListener, NodeStateChangeListener {

    private Runnable m_onCleanUp;

    private NodeContainer m_nc;

    private final boolean m_cleanUpOnNodeStateChange;

    /**
     * A new callback instance.
     *
     * @param nc the node to watch
     * @param onCleanUp the callback to call when the node is being disposed.
     * @param cleanUpOnNodeStateChange if the clean-up callback shall be called on node state change or not
     */
    public NodeCleanUpCallback(final NodeContainer nc, final Runnable onCleanUp,
        final boolean cleanUpOnNodeStateChange) {
        m_nc = nc;
        m_onCleanUp = onCleanUp;
        m_cleanUpOnNodeStateChange = cleanUpOnNodeStateChange;
    }

    /**
     * Activates the node clean-up callback by registering the respective listeners.
     */
    public void activate() {
        WorkflowManager.ROOT.addListener(this);
        m_nc.getParent().addListener(this);
        if (m_cleanUpOnNodeStateChange) {
            m_nc.addNodeStateChangeListener(this);
        }
    }

    @Override
    public void workflowChanged(final WorkflowEvent e) {
        if (m_onCleanUp == null) {
            // already cleaned up
            return;
        }
        if (e.getType() == WorkflowEvent.Type.NODE_REMOVED) {
            if (e.getOldValue() instanceof WorkflowManager && ((WorkflowManager)e.getOldValue()).getID()
                .getIndex() == m_nc.getParent().getProjectWFM().getID().getIndex()) {
                // workflow has been closed
                cleanUp();
            }
            if (e.getOldValue() instanceof NativeNodeContainer
                && ((NativeNodeContainer)e.getOldValue()).getID().equals(m_nc.getID())) {
                // node removed
                cleanUp();
            }
        }
    }

    @Override
    public void stateChanged(final NodeStateEvent state) {
        cleanUp();
    }

    private void cleanUp() {
        WorkflowManager.ROOT.removeListener(this);
        m_nc.getParent().removeListener(this);
        if (m_cleanUpOnNodeStateChange) {
            m_nc.removeNodeStateChangeListener(this);
        }
        m_onCleanUp.run();
        m_nc = null;
        m_onCleanUp = null;
    }
}