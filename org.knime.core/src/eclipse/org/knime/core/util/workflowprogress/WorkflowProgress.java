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
 *   Jan 11, 2019 (hornm): created
 */
package org.knime.core.util.workflowprogress;

/**
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @since 3.8
 */
public class WorkflowProgress {

    private final int m_numNodesExecuted;

    private final int m_numNodesExecuting;

    private final int m_numNodesFailed;

    private final boolean m_hasLoopCounts;

    private final boolean m_containsLoops;

    /**
     * @param numNodesExecuted the number of nodes that successfully finished
     * @param numNodesExecuting the number of nodes queued and executing (loop counts respected, if available)
     * @param numNodesFailed the number of nodes that failed
     * @param containsLoops if loops are contained
     * @param hasLoopCounts if the number of loops (i.e. iterations) are known for all contained loops
     */
    WorkflowProgress(final int numNodesExecuted, final int numNodesExecuting, final int numNodesFailed,
        final boolean containsLoops, final boolean hasLoopCounts) {
        m_numNodesExecuted = numNodesExecuted;
        m_numNodesExecuting = numNodesExecuting;
        m_numNodesFailed = numNodesFailed;
        m_hasLoopCounts = hasLoopCounts;
        m_containsLoops = containsLoops;
    }

    /**
     * @return the number of nodes queued and executing (loop counts respected, if available)
     */
    public int getNumberOfExecutingNodes() {
        return m_numNodesExecuting;
    }

    /**
     * @return the number of nodes that successfully finished
     */
    public int getNumberOfExecutedNodes() {
        return m_numNodesExecuted;
    }

    /**
     * @return the number of nodes that failed
     */
    public int getNumberOfFailedNodes() {
        return m_numNodesFailed;
    }

    /**
     * @return if loops are contained
     */
    public boolean containsLoops() {
        return m_containsLoops;
    }

    /**
     * @return if the number of loops (i.e. iterations) are known for all contained loops
     */
    public boolean hasLoopCounts() {
        return m_hasLoopCounts;
    }

}
