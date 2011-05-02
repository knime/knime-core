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
 *   04.04.2011 (mb): created
 */
package org.knime.core.node.workflow.virtual;

import org.knime.core.node.workflow.LoopEndParallelizeNode;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeStateChangeListener;
import org.knime.core.node.workflow.NodeStateEvent;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * Represents all parallel chunks (@see{ParallelChunkContext}) together and
 * also encapsulating metanode.
 * 
 * @author M. Berthold, University of Konstanz
 */
public class ParallelizedChunkContentMaster
implements NodeStateChangeListener {

    /** individual chunks */
    ParallelizedChunkContent[] m_chunks;

    /** metanode container for all chunks */
    WorkflowManager m_manager;
    public static final String WFM_NAME = "Parallel Chunks";
    
    /** end node waiting for chunks */
    LoopEndParallelizeNode m_endNode;
    
    /** Create new chunk object master - also knows Workflowmanager
     * the chunks are located in.
     * 
     * @param wfm the workflowmanager holding the chunks
     * @param chunkCount the number of chunks.
     */
    public ParallelizedChunkContentMaster(final WorkflowManager wfm,
            final LoopEndParallelizeNode endNode, final int chunkCount) {
        m_manager = wfm;
        m_endNode = endNode;
        m_chunks = new ParallelizedChunkContent[chunkCount];
        m_endNode.setParallelChunkMaster(this);
    }

    /** Add a new chunk to the list.
     * 
     * @param index of chunk
     * @param pcc content of chunk
     */
    public void addParallelChunk(final int index, final ParallelizedChunkContent pcc) {
        if (m_chunks[index] != null) {
            throw new IllegalArgumentException("Duplicate chunk index: "
                    + index);
        }
        m_chunks[index] = pcc;
        pcc.registerLoopEndStateChangeListener(this);
    }
    
    /**
     * @return number of chunks
     */
    public int nrChunks() {
        return m_chunks.length;
    }
    
    /**
     * @param i index
     * @return chunk of given index
     */
    public ParallelizedChunkContent getChunk(final int i) {
        return m_chunks[i];
    }

    /**
     * Start execution of all chunks.
     */
    public void executeChunks() {
        for (int i = 0; i < m_chunks.length; i++) {
            ParallelizedChunkContent pcc = m_chunks[i];
            if (pcc != null) {
                pcc.executeChunk();
            } else {
                throw new NullPointerException("Chunk " + i + " not set!");
            }
        }
    }

    /**
     * @return number of executed chunks
     */
    public int nrExecutedChunks() {
        int count = 0;
        for (int i = 0; i < m_chunks.length; i++) {
            ParallelizedChunkContent pcc = m_chunks[i];
            if (pcc.isExecuted()) {
                count++;
            }
        }
        return count;
    }

    /**
     * @return number of executing chunks
     */
    public int nrExecutingChunks() {
        int count = 0;
        for (int i = 0; i < m_chunks.length; i++) {
            ParallelizedChunkContent pcc = m_chunks[i];
            if (pcc.executionInProgress()) {
                count++;
            }
        }
        return count;
    }

    /**
     * @return number of failed (==IDLE) chunks
     */
    public int nrFailedChunks() {
        int count = 0;
        for (int i = 0; i < m_chunks.length; i++) {
            ParallelizedChunkContent pcc = m_chunks[i];
            if ((!pcc.executionInProgress()) && (!pcc.isExecuted())) {
                count++;
            }
        }
        return count;
    }

    /**
     * Trigger cancelation of chunk execution
     */
    public void cancelChunkExecution() {
        synchronized (m_chunks) {
            for (int i = 0; i < m_chunks.length; i++) {
                ParallelizedChunkContent pbc = m_chunks[i];
                if (pbc.executionInProgress()) {
                    pbc.cancelExecution();
                }
            }
        }
    }
    
    /**
     * Clean up chunks (and containing WFM).
     */
    public void cleanupChunks() {
        synchronized (m_chunks) {
            for (int i = 0; i < m_chunks.length; i++) {
                ParallelizedChunkContent pbc = m_chunks[i];
                if (pbc != null) {
                    pbc.removeLoopEndStateChangeListener(this);
                    pbc.removeAllNodesFromWorkflow();
                    m_chunks[i] = null;
                }
            }
            if (m_manager.getParent().containsNodeContainer(m_manager.getID())) {
                NodeContainer nc = m_manager.getParent().getNodeContainer(m_manager.getID());
                if (WFM_NAME.equals(nc.getName())) {
                    m_manager.getParent().removeNode(m_manager.getID());
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stateChanged(NodeStateEvent state) {
        // check if there is any executing chunk left (don't count
        // failures...)
        int nrExecutingChunks = 0;
        int nrFailedChunks = 0;
        for (ParallelizedChunkContent pbc : m_chunks) {
            if (!pbc.isExecuted()) {
                if (pbc.executionInProgress()) {
                    nrExecutingChunks++;
                } else {
                    nrFailedChunks++;
                }
            }
        }
        // notify end node about new status
        m_endNode.updateStatus();
    }
    
}
