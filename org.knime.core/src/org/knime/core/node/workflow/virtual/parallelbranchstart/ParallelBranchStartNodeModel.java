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
package org.knime.core.node.workflow.virtual.parallelbranchstart;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
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
import org.knime.core.node.workflow.LoopStartParallelizeNode;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public class ParallelBranchStartNodeModel extends NodeModel implements
		LoopStartParallelizeNode {
	

	private PortObject[] m_splitInTables;
	
	/**
	 * 
	 */
	public ParallelBranchStartNodeModel() {
		super(1, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {
		int threadCount = 2 * Runtime.getRuntime().availableProcessors();
		BufferedDataTable in = inData[0];
        int numOfChunks = threadCount;
        PortObject[] splitInTables; 

        if (in.getRowCount() <= 1) {
            // empty or one-row input table...
            splitInTables = new BufferedDataTable[] {};
            return new BufferedDataTable[]{in};
        }

        int numOfRowsPerChunk = in.getRowCount() / numOfChunks;
        if (numOfRowsPerChunk < 1) {
            // these are pretty small chunks
            assert in.getRowCount() < numOfChunks;
            numOfChunks = in.getRowCount();
            numOfRowsPerChunk = 1;
        }
        int remainingRows = in.getRowCount() % numOfChunks;

        // now split it
        splitInTables = new BufferedDataTable[numOfChunks];
        int i = 0;
        RowIterator inputIterator = in.iterator();
        while (inputIterator.hasNext()) {
            exec.setMessage("Creating split table, chunk " + i
                    + "/" + numOfChunks);
            int rowsInTable = 0;
            BufferedDataContainer split =
                    exec.createDataContainer(in.getSpec(), false, 0);
            while (rowsInTable < numOfRowsPerChunk) {
                if (!inputIterator.hasNext()) {
                    assert false;
                }
                split.addRowToTable(inputIterator.next());
                rowsInTable++;
            }
            if (remainingRows > i) {
                // distribute the remainder
                split.addRowToTable(inputIterator.next());
            }
            split.close();
            splitInTables[i] = split.getTable();
            i++;
        }
        assert i == numOfChunks;
        m_splitInTables = Arrays.copyOf(
        		splitInTables, splitInTables.length - 1);
        return new BufferedDataTable[]{(BufferedDataTable) 
        		splitInTables[splitInTables.length - 1]};
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {
		return inSpecs;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public PortObject[] getPortObjectForChunk(final int i) {
		return new PortObject[]{m_splitInTables[i]};
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
		m_splitInTables = null;
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
	protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// no internals
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// no internals
	}

}
