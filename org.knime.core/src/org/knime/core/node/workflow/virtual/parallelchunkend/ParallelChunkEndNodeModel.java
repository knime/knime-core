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

import org.knime.core.data.DataRow;
import org.knime.core.data.def.DefaultRow;
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
import org.knime.core.node.workflow.virtual.ParallelizedChunkContent;
import org.knime.core.node.workflow.virtual.ParallelizedChunkContentMaster;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public class ParallelChunkEndNodeModel extends NodeModel
implements LoopEndParallelizeNode {

    /* Store chunks */
    private ParallelizedChunkContentMaster m_chunkMaster;
    
    /* remember cancellation for chunk cleanup */
    private boolean m_canceled = false;

    /* should we uniqify the Row ID by adding the chunk id? */
    private boolean m_addChunkID = true;

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
        // spec of the chunk arriving here is representative for the
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
	    m_canceled = false;
	    // if chunks have not been set, something's wrong.
	    if (m_chunkMaster == null) {
	        throw new IllegalStateException("Parallel Chunk End node"
	                + " without any registered branches.");
	    }
        // wait for all parallel branches to finish...
	    boolean done = false;
	    while (!done) {
	        // wait a bit
	        try {
	            Thread.sleep(500); 
	        } catch (InterruptedException ie) {
	            // nothing to do, just continue
	        }
	        // check if execution was canceled (if it wasn't already canceled)
	        try {
	            if (!m_canceled) {
	                exec.checkCanceled();
	            }
	        } catch (CanceledExecutionException cee) {
	            m_canceled = true;
	            m_chunkMaster.cancelChunkExecution();
	            // continue as if nothing has happened: wait for all
	            // chunks to finish execution (which can also mean:
	            // stop because canceled!)
	        }
	        if (m_chunkMaster.nrExecutingChunks() <= 0) {
	            done = true;
	        } else {
	            int nrChunks = m_chunkMaster.nrChunks();
	            int nrExecuting = m_chunkMaster.nrExecutingChunks();
	            // report progress: 90% execution - 10% for data copying...
	            double prog = (double)(nrChunks-nrExecuting)*0.9
	                / ((double)nrChunks); 
	            if (!m_canceled) {
	                exec.setProgress(prog, "Total: " + nrChunks
	                    + ", still executing: " + nrExecuting
	                    + ", failed: " + m_chunkMaster.nrFailedChunks());
	            } else {
                    exec.setProgress(prog, "Total: " + nrChunks
                            + ", still executing: " + nrExecuting
                            + ", waiting for cancelation!");
	            }
	        }
	    }
	    if (!m_canceled) try {
	        exec.checkCanceled();
            // copy the results of all chunks to result table...
            BufferedDataTable lastChunk = (BufferedDataTable)inObjects[0];
            BufferedDataContainer bdc
                    = exec.createDataContainer(lastChunk.getDataTableSpec());
            for (int i = 0; i < m_chunkMaster.nrChunks(); i++) {
                int nrChunks = m_chunkMaster.nrChunks();
                double prog = 0.9 + 0.1 * (double)(i) / ((double)nrChunks); 
                exec.setProgress(prog, "Copying chunk " 
                        + i + " of " + nrChunks + "...");
                ParallelizedChunkContent pcc = m_chunkMaster.getChunk(i);
                if (pcc.isExecuted()) {
                    // copy results from chunk
                    BufferedDataTable bdt
                            = (BufferedDataTable)pcc.getOutportContent()[0];
                    if (bdt == null) {
                        throw new Exception("Chunk " + i
                                + " has no content!");
                    }
                    for (DataRow row : bdt) {
                        if (m_addChunkID) {
                            row = new DefaultRow(row.getKey()
                                    + "_#" + i, row);
                        }
                        bdc.addRowToTable(row);
                    }
                } else {
                    throw new Exception("Not all chunks finished - check"
                            + " individual chunk branches for details.");
                }
                exec.checkCanceled();
            }
            // copy last chunk from input port of this node...
            exec.setProgress(0.99, "Copying last chunk of " 
                    + m_chunkMaster.nrChunks() + "...");
            for (DataRow row : lastChunk) {
                if (m_addChunkID) {
                    row = new DefaultRow(row.getKey()
                            + "_#" + m_chunkMaster.nrChunks(), row);
                }
                bdc.addRowToTable(row);
            }
            exec.checkCanceled();
            bdc.close();
    	    BufferedDataTable result = bdc.getTable();
            if (result == null) {
                throw new Exception("Something went terribly wrong. We are sorry for any inconvenience this may cause.");
            }
            // clean up chunks
            m_chunkMaster.cleanupChunks();
            // return aggregated table
    		return new PortObject[] { result };
	    } catch (CanceledExecutionException cee) {
	        // catch cancel in result generation and handle the same
	        // as the others...
	    }
        // clean up chunks 
        m_chunkMaster.cleanupChunks();
        throw new CanceledExecutionException();
	}
	
    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // note that we do NOT delete the chunk-branches here - they should
        // be kept until the START node is reset explicitly so that for
        // debugging purposes they are available even if this node has
        // failed.
    }

    /** Clean up all chunks - called when canceled during execution.
     */
    private void cleanupChunks() {
        synchronized (m_chunkMaster) {
            for (int i =0; i < m_chunkMaster.nrChunks(); i++) {
                ParallelizedChunkContent pbc = m_chunkMaster.getChunk(i);
                if (pbc.executionInProgress()) {
                    pbc.cancelExecution();
                    // branches will be removed when the last node
                    // changes state to IDLE...
                } else {
                    pbc.removeAllNodesFromWorkflow();
                }
            }
        }
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

	///////////////////////////////////////////////////
	// methods to implement LoopEndParallelizeNodeModel
	///////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    @Override
    public void setParallelChunkMaster(final ParallelizedChunkContentMaster pcm)
    {
        if (m_chunkMaster != null) {
            // TODO: cleanup! -- but shouldn't really happen...
        }
        m_chunkMaster = pcm;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void updateStatus() {
//	    this.notify();
	}

}
