/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 * 
 * History
 *   Mar 30, 2011 (wiswedel): created
 */
package org.knime.core.node.workflow.virtual;

import org.knime.core.node.workflow.LoopEndParallelizeNode;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.NodeContainer.State;

/**
 * An object representing the copied content of a branch that is executed in
 * parallel.
 * @author wiswedel, University of Konstanz
 */
public class ParallelizedBranchContent {

	private final WorkflowManager m_manager;
	private final NodeID m_virtualInputID;
	private final NodeID m_virtualOutputID;
	private final NodeID[] m_copiedLoopContent;
	/**
	 * @param manager
	 * @param virtualInputID
	 * @param virtualOutputID
	 * @param copiedLoopContent
	 * @throws IllegalArgumentException If the input/output nodes are not
	 * of the expected type.
	 */
	public ParallelizedBranchContent(final WorkflowManager manager,
			final NodeID virtualInputID, final NodeID virtualOutputID,
			final NodeID[] copiedLoopContent) {
		m_manager = manager;
		// validate types of input/output node models
		m_manager.castNodeModel(
				virtualInputID, VirtualPortObjectInNodeModel.class);
		m_manager.castNodeModel(
				virtualOutputID, VirtualPortObjectOutNodeModel.class);
		m_virtualInputID = virtualInputID;
		m_virtualOutputID = virtualOutputID;
		m_copiedLoopContent = copiedLoopContent;
	}

	/**
	 * @return the virtualInputID
	 */
	public NodeID getVirtualInputID() {
		return m_virtualInputID;
	}
	/**
	 * @return the virtualOutputID
	 */
	public NodeID getVirtualOutputID() {
		return m_virtualOutputID;
	}
	/**
	 * @return the copiedLoopContent
	 */
	public NodeID[] getCopiedLoopContent() {
		return m_copiedLoopContent;
	}
	
	/** Gets the underlying model of the virtual input node.
	 * @return The NodeModel underlying the start node representation.
	 */
	public VirtualPortObjectInNodeModel getVirtualInputModel() {
		return m_manager.castNodeModel(
				m_virtualInputID, VirtualPortObjectInNodeModel.class);
	}
	
	/** Get underlying node model of the end node (no null, no exception).
	 * @return The model underlying the end node.
	 */
	public VirtualPortObjectOutNodeModel getVirtualOutputModel() {
		return m_manager.castNodeModel(
				m_virtualOutputID, VirtualPortObjectOutNodeModel.class);
	}

    /**
     * @param parallelBranchEndNodeModel
     */
    public void registerLoopEndStateChangeListener(
            final LoopEndParallelizeNode nmodel) {
        m_manager.getNodeContainer(m_virtualOutputID)
                .addNodeStateChangeListener(nmodel);
    }
	
    /**
     * Remove all nodes (and connections) of this branch.
     */
    public void removeAllNodesFromWorkflow() {
        for (NodeID id : m_copiedLoopContent) {
            m_manager.removeNode(id);
        }
    }
    
    /**
     * @return true if branch is completely executed.
     */
    public boolean isExecuted() {
        return State.EXECUTED.equals(
                m_manager.getNodeContainer(m_virtualOutputID).getState());
    }
}
