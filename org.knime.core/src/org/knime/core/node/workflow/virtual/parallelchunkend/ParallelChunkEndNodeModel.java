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
package org.knime.core.node.workflow.virtual.parallelchunkend;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.knime.core.data.DataRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.LoopEndParallelizeNode;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeStateChangeListener;
import org.knime.core.node.workflow.NodeStateEvent;
import org.knime.core.node.workflow.virtual.ParallelizedChunkContent;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public class ParallelChunkEndNodeModel extends NodeModel
implements LoopEndParallelizeNode,
NodeStateChangeListener {

    /* Store map of end node IDs and corresponding chunk objects */
    private LinkedHashMap<NodeID, ParallelizedChunkContent> m_chunks;
    /* hold intermediate BufferedDataTables from parallelize chunks */
    private BufferedDataTable[] m_results;

	/**
	 */
	ParallelChunkEndNodeModel() {
		super(new PortType[]{ BufferedDataTable.TYPE},
		        new PortType[]{ BufferedDataTable.TYPE});
	}
	
    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        // spec of the one chunk arriving here is representative for the
        // entire table.
        return inSpecs;
    }

    /**
	 * {@inheritDoc}
	 */
	@Override
	protected PortObject[] execute(final PortObject[] inObjects,
	        final ExecutionContext exec)
			throws Exception {
	    // if chunks have not been set, something's wrong.
	    if (m_chunks == null) {
	        throw new IllegalStateException("Parallel Chunk End node"
	                + " without any registered branches.");
	    }
	    // reserve space for output chunks
	    m_results = new BufferedDataTable[m_chunks.size() + 1];
	    // start by copying the results of this chunk to the last output...
	    BufferedDataTable lastChunk = (BufferedDataTable)inObjects[0];
	    BufferedDataContainer bdc
	            = exec.createDataContainer(lastChunk.getDataTableSpec());
        for (DataRow row : lastChunk) {
            bdc.addRowToTable(row);
        }
        bdc.close();
        m_results[m_results.length - 1] = bdc.getTable();
        // now wait for other parallel branches to finish...
	    boolean done = false;
	    while (!done) {
	        // wait a bit
	        try {
	            Thread.sleep(500); 
	        } catch (InterruptedException ie) {
	            // nothing to do, just continue
	        }
	        // check if execution was canceled
	        try {
	            exec.checkCanceled();
	        } catch (CanceledExecutionException cee) {
	            // TODO: cancel all chunks
	            throw cee;
	        }
	        // check if any of the chunks are finished
	        for (Iterator<ParallelizedChunkContent> pbc_it
	                = m_chunks.values().iterator(); pbc_it.hasNext();) {
	            ParallelizedChunkContent pbc = pbc_it.next();
	            if (pbc.isExecuted()) {
	                // copy results from chunk
	                bdc = exec.createDataContainer(lastChunk.getDataTableSpec());
	                BufferedDataTable bdt
	                        = (BufferedDataTable)pbc.getOutportContent()[0];
	                for (DataRow row : bdt) {
	                    bdc.addRowToTable(row);
	                }
	                // and put it into the correct slot
	                bdc.close();
	                m_results[pbc.getChunkIndex()] = bdc.getTable();
	                // and finally remove branch and all its nodes
                    pbc_it.remove();
	                pbc.removeAllNodesFromWorkflow();
	                exec.setProgress((double)m_chunks.size()
	                                 /(double)pbc.getChunkCount());
	            }
	            // TODO: also do something with failures and nodes
	            // that do not execute anymore.
	        }
	        if (m_chunks.size() == 0) {
	            done = true;
	        }
	    }
	    BufferedDataTable result = exec.createConcatenateTable(exec, m_results);
        if (result == null) {
            throw new Exception("Something went terribly wrong. We are sorry for any inconvenience this may cause.");
        }
		return new PortObject[] { result };
	}
	
    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // TODO: cancel and delete all chunks
        m_chunks = null;
        m_results = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void addParallelChunk(final ParallelizedChunkContent pbc)
	{
        if (m_chunks == null) {
            m_chunks = new LinkedHashMap<NodeID, ParallelizedChunkContent>();
        }
	    if (m_chunks.containsKey(pbc.getVirtualOutputID())) {
	        throw new IllegalArgumentException("Can't insert chunk with duplicate key!");
	    }
	    m_chunks.put(pbc.getVirtualOutputID(), pbc);
	    pbc.registerLoopEndStateChangeListener(this);
	}
	

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		// no settings
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		// no settings
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		// no settings
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File nodeInternDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// no internals
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File nodeInternDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// no internals
	}

	//////////////////////////////////////////
	// NodeStateChangeListener Methods
	//////////////////////////////////////////
	
    /**
     * {@inheritDoc}
     */
    @Override
	public void stateChanged(final NodeStateEvent state) {
        NodeID endNode = state.getSource();
        if (m_chunks.containsKey(endNode)) {
//            ParallelizedChunkContent pcc = m_chunks.get(endNode);
//            if (pcc.isExecuted()) {
//                // copy results from chunk
//                BufferedDataTable bdt
//                   = (BufferedDataTable)pcc.getOutportContent()[0];
//                BufferedDataContainer bdc
//                   = exec.createDataContainer(bdt.getDataTableSpec());
//                for (DataRow row : bdt) {
//                    bdc.addRowToTable(row);
//                }
//                // and put it into the correct slot
//                bdc.close();
//                m_results[pcc.getChunkIndex()] = bdc.getTable();
//                // and finally remove branch and all its nodes
//                m_chunks.remove(endNode);
//                pcc.removeAllNodesFromWorkflow();
////            this.notify();
//            }
        }
    }

}
