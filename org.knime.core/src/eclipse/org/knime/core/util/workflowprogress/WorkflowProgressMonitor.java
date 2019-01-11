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
 *   Jan 10, 2019 (hornm): created
 */
package org.knime.core.util.workflowprogress;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.LoopCountAware;
import org.knime.core.node.workflow.LoopStartNode;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContainerState;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeStateChangeListener;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;

/**
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @since 3.8
 */
public class WorkflowProgressMonitor {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(WorkflowProgressMonitor.class);

    private NodeStateChangeListener m_nodeStateListener = state -> nodeStateChanged(state.getSource());

    private List<NodeContainer> m_monitoringNodeContainers = new ArrayList<NodeContainer>();

    private WorkflowManager m_wfm;

    private int m_numNodesExecuted = 0;

    private int m_numNodesExecuting = 0;

    private int m_numNodesFailed = 0;

    private boolean m_hasLoopCounts = true;

    private boolean m_containsLoops = false;

    private final List<Runnable> m_progressChangedListeners = new ArrayList<Runnable>();

    public WorkflowProgressMonitor(final WorkflowManager wfm) {
        m_wfm = wfm;
        checkAndRegisterNodes(wfm.getNodeContainers(), wfm, 1, 1);
    }

    /**
     * @return a fixed snapshot (i.e. a copy) of the current workflow progress
     */
    public WorkflowProgress getProgress() {
        return new WorkflowProgress(m_numNodesExecuted, m_numNodesExecuting, m_numNodesFailed, m_containsLoops,
            m_hasLoopCounts);
    }

    public void addProgressChangedListener(final Runnable listener) {
        m_progressChangedListeners.add(listener);
    }

    private void notifiyProgressChangedListeners() {
        m_progressChangedListeners.forEach(l -> l.run());
    }

    public void shutdown() {
        m_monitoringNodeContainers.forEach(n -> n.removeNodeStateChangeListener(m_nodeStateListener));
        m_monitoringNodeContainers.clear();
        m_progressChangedListeners.clear();
    }

    private void checkAndRegisterNodes(final Collection<NodeContainer> nodes, final WorkflowManager wfm,
        final long numOfLoopsToGo, final long numOfLoopsFinished) {
        if (numOfLoopsToGo <= 0) {
            return;
        }
        nodes.forEach(n -> {
            if (n instanceof WorkflowManager) {
                WorkflowManager subWfm = (WorkflowManager)n;
                checkAndRegisterNodes(subWfm.getNodeContainers(), subWfm, numOfLoopsToGo, numOfLoopsFinished);
            } else if (n instanceof SingleNodeContainer) {
                if (isLoopStartNode(n)) {
                    recursivleyCheckAndRegisterNodesInLoop((NativeNodeContainer)n, wfm, numOfLoopsToGo,
                        numOfLoopsFinished);
                } else {
                    NodeContainerState state = n.getNodeContainerState();
                    if (state.isExecuted()) {
                        m_numNodesExecuted += numOfLoopsFinished;
                    } else if (state.isExecutionInProgress()) {
                        m_numNodesExecuting += numOfLoopsToGo;
                        n.addNodeStateChangeListener(m_nodeStateListener);
                        m_monitoringNodeContainers.add(n);
                    }
                }
            }
        });

    }

    private void recursivleyCheckAndRegisterNodesInLoop(final NativeNodeContainer loopStart, final WorkflowManager wfm,
        final long numOfLoopsToGo, final long numOfLoopsFinished) {
        m_containsLoops = true;
        if (loopStart.getNodeModel() instanceof LoopCountAware) {
            try {
                long loopCount = ((LoopCountAware)loopStart.getNodeModel()).getLoopCount();
                long iteration = ((LoopCountAware)loopStart.getNodeModel()).getIteration();
                List<NodeContainer> nodesInLoop = getNodesInLoopWithoutLoopStartAndEnd(loopStart, wfm);
                checkAndRegisterNodes(nodesInLoop, wfm, numOfLoopsToGo * loopCount - 1 - iteration,
                    numOfLoopsFinished * iteration);
            } catch (Throwable e) {
                LOGGER.warn("Problem determining loop count for node '" + loopStart.getNameWithID() + "'", e);
                m_hasLoopCounts = false;
            }
        } else {
            m_hasLoopCounts = false;
        }
    }

    private static boolean isLoopStartNode(final NodeContainer nc) {
        return nc instanceof NativeNodeContainer && ((NativeNodeContainer)nc).getNodeModel() instanceof LoopStartNode;
    }

    private static List<NodeContainer> getNodesInLoopWithoutLoopStartAndEnd(final NativeNodeContainer loopStart,
        final WorkflowManager wfm) {
        //assumption here is that the loop-start and -end are the first and last element
        List<NodeContainer> nodesInScope = wfm.getNodesInScope(loopStart);
        return nodesInScope.stream().skip(1).limit(nodesInScope.size() - 2).collect(Collectors.toList());
    }

    private void nodeStateChanged(final NodeID nodeId) {
        NodeContainer n = m_wfm.findNodeContainer(nodeId);
        if (n.getNodeContainerState().isExecuted()) {
            m_numNodesExecuted++;
            m_numNodesExecuting--;
            n.removeNodeStateChangeListener(m_nodeStateListener);
            m_monitoringNodeContainers.remove(n);
        } else if (!n.getNodeContainerState().isExecutionInProgress()) {
            m_numNodesExecuting--;
            m_numNodesFailed++;
        }
        notifiyProgressChangedListeners();
    }
}
