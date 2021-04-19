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
 * History
 *   Mar 29, 2011 (wiswedel): created
 */
package org.knime.core.node.workflow;

import org.knime.core.node.port.PortObjectHolder;
import org.knime.core.node.workflow.virtual.AbstractPortObjectRepositoryNodeModel;
import org.knime.core.node.workflow.virtual.parchunk.FlowVirtualScopeContext;
import org.knime.core.node.workflow.virtual.parchunk.ParallelizedChunkContentMaster;
import org.knime.core.node.workflow.virtual.parchunk.VirtualParallelizedChunkNodeInput;

/**
 * NO API!
 *
 * It is recommended that implementations also implement {@link AbstractPortObjectRepositoryNodeModel} in order to work
 * properly with nodes that (indirectly) use
 * {@link FlowVirtualScopeContext#addPortObjectToRepositoryAndHostNode(org.knime.core.node.port.PortObject)} (e.g.
 * Integrated Deployment).
 *
 * @noimplement This interface is not intended to be implemented by clients.
 *
 * @author wiswedel, University of Konstanz
 */
public interface LoopStartParallelizeNode extends LoopStartNode {

    /**
     * @param chunkIndex index
     * @return virtual input node for the given chunk
     */
	VirtualParallelizedChunkNodeInput getVirtualNodeInput(final int chunkIndex);

	/**
	 * @return overall number of remote chunks (excluding the one that is
	 * processed by the node itself!)
	 */
    int getNrRemoteChunks();

    /** Set parallel chunk master so the start node has access to clean up
     * when reset.
     *
     * @param pccm matching @see{ParallelizedChunkContentMaster}
     */
    void setChunkMaster(final ParallelizedChunkContentMaster pccm);

    /**
     * If the parallel loop start implements the {@link PortObjectHolder}-interface but the internally held port objects
     * are only available after this node finished its execution, this method allows one to notify the framework that
     * all the internally held port objects are now available. The framework then updates the port object references
     * such that they are persisted as soon as the workflow is saved.
     *
     * @param notifier the callback to notify the framework that all internally held port objects are now available
     */
    void setNewInternalPortObjectNotifier(Runnable notifier);

}
