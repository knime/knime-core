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
import java.util.Arrays;

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
import org.knime.core.node.port.inactive.InactiveBranchConsumer;
import org.knime.core.node.port.inactive.InactiveBranchPortObject;
import org.knime.core.node.port.inactive.InactiveBranchPortObjectSpec;
import org.knime.core.node.workflow.LoopEndParallelizeNode;
import org.knime.core.node.workflow.virtual.ParallelizedChunkContent;
import org.knime.core.node.workflow.virtual.ParallelizedChunkContentMaster;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public class ParallelChunkEndMultiPortNodeModel extends NodeModel
implements LoopEndParallelizeNode, InactiveBranchConsumer {

    private ParallelChunkEndNodeConfiguration m_configuration =
        new ParallelChunkEndNodeConfiguration();

    /* Store chunks */
    private ParallelizedChunkContentMaster m_chunkMaster;
    
    /* remember cancellation for chunk cleanup */
    private boolean m_canceled = false;

    /* number of ports */
    private final int m_nrPorts;
    
	/**
	 */
	ParallelChunkEndMultiPortNodeModel(final int nrPorts) {
		super(createInTypes(nrPorts), createOutTypes(nrPorts));
		m_nrPorts = nrPorts;
	}
	
    private static PortType[] createInTypes(final int nrIns) {
        PortType[] types = new PortType[nrIns];
        Arrays.fill(types, new PortType(BufferedDataTable.class, true));
        types[0] = BufferedDataTable.TYPE;
        return types;
    }

    private static PortType[] createOutTypes(final int nrOuts) {
        PortType[] types = new PortType[nrOuts];
        Arrays.fill(types, BufferedDataTable.TYPE);
        return types;
    }

	/** remember previous spec - see configure FIXME */
	PortObjectSpec[] previousSpecs = null;
    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        // FIXME: fix this hack to avoid errors when a IABO Spec is
        // later (during execute) replaced by an actual table
        if ((previousSpecs != null) 
              && (inSpecs[0] instanceof InactiveBranchPortObjectSpec)) {
            return previousSpecs;
        }
        previousSpecs = inSpecs;
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
	    // set cancelation status (needed to later skip table collection)
	    m_canceled = false;
        // if chunks have not been set, something's wrong.
        if (m_chunkMaster == null) {
            throw new IllegalStateException("Parallel Chunk End node"
                    + " without any registered branches.");
        }
	    // first determine if we received actual data on our inport
	    // (which should be the last chunk) or not (all data in remote branches)
	    boolean hasLocalChunk = (inObjects[0] instanceof BufferedDataTable);
	    // determine which inports are actually connected:
	    boolean[] m_portIsConnected = new boolean[m_nrPorts];
	    for (int p = 0; p < m_nrPorts; p++) {
	        m_portIsConnected[p] = inObjects[p] != null;
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
	            if (hasLocalChunk) {
	                nrChunks++;
	            }
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
            BufferedDataContainer[] bdc = new BufferedDataContainer[m_nrPorts];
            for (int i = 0; i < m_chunkMaster.nrChunks(); i++) {
                int nrChunks = m_chunkMaster.nrChunks();
                if (hasLocalChunk) {
                    nrChunks++;
                }
                double prog = 0.9 + 0.1 * (double)(i) / ((double)nrChunks); 
                exec.setProgress(prog, "Copying chunk " 
                        + (i+1) + " of " + nrChunks + "...");
                ParallelizedChunkContent pcc = m_chunkMaster.getChunk(i);
                if (pcc.isExecuted()) {
                    // copy results from chunk
                    for (int p = 0; p < m_nrPorts; p++) if (m_portIsConnected[p]) {
                        BufferedDataTable bdt
                                = (BufferedDataTable)pcc.getOutportContent()[p];
                        if (bdt == null) {
                            throw new Exception("Chunk " + i
                                    + " (port " + p + ")"
                                    + " has no content!");
                        }
                        if (i == 0) {
                            // init table create with spec from first chunk
                            bdc[p] = exec.createDataContainer(bdt.getDataTableSpec());
                        }
                        for (DataRow row : bdt) {
                            if (m_configuration.addChunkIndexToID()) {
                                row = new DefaultRow(row.getKey()
                                        + "_#" + i, row);
                            }
                            bdc[p].addRowToTable(row);
                        }
                    }
                } else {
                    throw new Exception("Not all chunks finished - check"
                            + " individual chunk branches for details.");
                }
                exec.checkCanceled();
            }
            if (hasLocalChunk) {
                for (int p = 0; p < m_nrPorts; p++) if (m_portIsConnected[p]) {
                    BufferedDataTable bdt = (BufferedDataTable)inObjects[p];
                    if (bdc[p] == null) {
                        // init table create with spec from localChunk if no
                        // remote chunks were copied!
                        bdc[p] = exec.createDataContainer(bdt.getDataTableSpec());
                    }
                    exec.setProgress(0.99, "Copying last chunk " 
                            + " of " + (m_chunkMaster.nrChunks()+1) + "!");
                    for (DataRow row : bdt) {
                        if (m_configuration.addChunkIndexToID()) {
                            row = new DefaultRow(row.getKey()
                                    + "_#" + (m_chunkMaster.nrChunks()+1), row);
                        }
                        bdc[p].addRowToTable(row);
                    }
                }
            }
            PortObject[] result = new PortObject[m_nrPorts];
            for (int p = 0; p < m_nrPorts; p++) {
                if (m_portIsConnected[p]) {
                    bdc[p].close();
                    result[p] = bdc[p].getTable();
                    if (result[p] == null) {
                        throw new Exception("Something went terribly wrong. We are"
                        		+ " sorry for any inconvenience this may cause.");
                    }
                } else {
                    result[p] = InactiveBranchPortObject.INSTANCE;
                }
            }
            // clean up chunks
            m_chunkMaster.cleanupChunks();
            // return aggregated table
    		return result;
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
        previousSpecs = null;
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
        if (m_configuration != null) {
            m_configuration.saveConfiguration(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        ParallelChunkEndNodeConfiguration c =
            new ParallelChunkEndNodeConfiguration();
        c.loadConfigurationModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        ParallelChunkEndNodeConfiguration c =
            new ParallelChunkEndNodeConfiguration();
        c.loadConfigurationModel(settings);
        m_configuration = c;
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
