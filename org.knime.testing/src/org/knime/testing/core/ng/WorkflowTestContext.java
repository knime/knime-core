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
 * ---------------------------------------------------------------------
 *
 * History
 *   22.08.2013 (thor): created
 */
package org.knime.testing.core.ng;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.core.node.AbstractNodeView;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.util.Pair;

/**
 * Shared context for all testcases. It is used to exchange information that is needed by several testcases, such as the
 * workflow manager, a list of pre-executed nodes, etc.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
public class WorkflowTestContext {
    private final Map<SingleNodeContainer, List<AbstractNodeView<? extends NodeModel>>> m_views =
            new HashMap<SingleNodeContainer, List<AbstractNodeView<? extends NodeModel>>>();

    private final Set<NodeID> m_preExecutedNodes = new HashSet<NodeID>();

    private final Set<String> m_nodesUnderTest = new HashSet<String>();

    private final List<Pair<Thread, Throwable>> m_uncaughtExceptions = new ArrayList<Pair<Thread, Throwable>>();

    private TestflowConfiguration m_flowConfiguration;

    private WorkflowManager m_manager;

    /**
     * Returns a map with the node views for each node (if there are any). This map is intended to be modified by
     * clients. If a node does not have a view, the corresponding entry is <code>null</code>.
     *
     * @return a map between node containers and their views
     */
    public Map<SingleNodeContainer, List<AbstractNodeView<? extends NodeModel>>> getNodeViews() {
        return m_views;
    }

    /**
     * Returns the workflow manager. This may be <code>null</code> until the workflow has been loaded.
     *
     * @return a workflow manager or <code>null</code>
     */
    public WorkflowManager getWorkflowManager() {
        return m_manager;
    }

    /**
     * Sets the workflow manager and records the nodes (executed and loaded) in the workflow. In addition also the
     * testflow configuration is read.
     *
     * @param manager a workflow manager
     * @throws IOException if an I/O error occurs while reading the testflow configuration
     * @throws InvalidSettingsException if the settings of the Testflow Configuration node are invalid
     */
    public void setWorkflowManager(final WorkflowManager manager) throws InvalidSettingsException, IOException {
        if (manager != null) {
            recordNodes(manager);
        }
        m_flowConfiguration = new TestflowConfiguration(manager);
        m_manager = manager;
    }

    /**
     * Returns a map between threads and their uncaught exceptions. This map is intended to be modified by clients.
     *
     * @return a map between threads and uncaught exceptions
     */
    public List<Pair<Thread, Throwable>> getUncaughtExceptions() {
        return m_uncaughtExceptions;
    }

    /**
     * Returns whether the given node was pre-executed or not when the workflow was loaded.
     *
     * @param node a node container
     * @return <code>true</code> if the node was already executed, <code>false</code> otherwise
     */
    public boolean isPreExecutedNode(final NodeContainer node) {
        return m_preExecutedNodes.contains(node.getID());
    }

    /**
     * Returns the configuration for this test workflow. Maybe <code>null</code> until the the workflow manager has been
     * set via {@link #setWorkflowManager(WorkflowManager)}.
     *
     * @return a testflow configuration or <code>null</code>
     */
    public TestflowConfiguration getTestflowConfiguration() {
        return m_flowConfiguration;
    }

    /**
     * Records all nodes on the given workflow manager that are already executed.
     *
     * @param manager a workflow manager
     */
    private void recordNodes(final WorkflowManager manager) {
        for (NodeContainer node : manager.getNodeContainers()) {
            if (node instanceof NativeNodeContainer) {
                if (((NativeNodeContainer)node).getNodeContainerState().isExecuted()) {
                    m_preExecutedNodes.add(node.getID());
                } else {
                    m_nodesUnderTest.add(((NativeNodeContainer)node).getNode().getFactory().getClass().getName());
                }
            } else if (node instanceof SubNodeContainer) {
                recordNodes(((SubNodeContainer)node).getWorkflowManager());
            } else if (node instanceof WorkflowManager) {
                recordNodes((WorkflowManager)node);
            } else {
                throw new IllegalStateException("Unknown node container type: " + node.getClass());
            }
        }
    }

    /**
     * Returns a set with node factory class names for all nodes in the current workflow manager.
     *
     * @return a set with node factory class names
     */
    public Set<String> getNodesUnderTest() {
        return Collections.unmodifiableSet(m_nodesUnderTest);
    }

    /**
     * Clears the context.
     */
    public void clear() {
        m_views.clear();
        m_uncaughtExceptions.clear();
        m_preExecutedNodes.clear();
        m_manager = null;
        m_flowConfiguration = null;
        // m_nodesUnderTest is deliberately not cleared since the values are used later on
    }
}
