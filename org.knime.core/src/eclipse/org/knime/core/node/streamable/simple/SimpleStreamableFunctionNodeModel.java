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
 *   Jun 13, 2012 (wiswedel): created
 */
package org.knime.core.node.streamable.simple;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InternalTableAPI;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.StreamableFunction;
import org.knime.core.node.streamable.StreamableFunctionProducer;
import org.knime.core.table.row.Selection;
import org.knime.core.util.ThreadUtils;

/**
 * Abstract definition of a node that applies a simple function using a {@link ColumnRearranger}. Each input row is
 * mapped to an output row.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @since 2.6
 */
public abstract class SimpleStreamableFunctionNodeModel extends NodeModel implements StreamableFunctionProducer {

    private int m_streamableInPortIdx;
    private int m_streamableOutPortIdx;

    private static final NodeLogger LOGGER = NodeLogger.getLogger(SimpleStreamableFunctionNodeModel.class);

    /**
     * Default constructor, defining one data input and one data output port.
     */
    public SimpleStreamableFunctionNodeModel() {
        super(1, 1);
        m_streamableInPortIdx = 0;
        m_streamableOutPortIdx = 0;
    }

    /**
     * Constructor for a node with multiple in or out ports.
     *
     * @param inPortTypes in-port types. The ports at the index <code>streamableInPortIdx</code> MUST be a non-optional {@link BufferedDataTable}!
     * @param outPortTypes out-port types.The ports at the index <code>streamableOutPortIdx</code> MUST be a non-optional {@link BufferedDataTable}!
     * @param streamableInPortIdx the index of the port that is streamable. All the others are assumed as neither streamable nor distributable.
     * @param streamableOutPortIdx the index of the port that is streamable. All the others are assumed as neither streamable nor distributable.
     * @since 3.1
     */
    public SimpleStreamableFunctionNodeModel(final PortType[] inPortTypes,
        final PortType[] outPortTypes, final int streamableInPortIdx, final int streamableOutPortIdx) {
        super(inPortTypes, outPortTypes);
        assert BufferedDataTable.TYPE.isSuperTypeOf(inPortTypes[streamableInPortIdx]);
        assert BufferedDataTable.TYPE.isSuperTypeOf(outPortTypes[streamableOutPortIdx]);
        assert !inPortTypes[streamableInPortIdx].isOptional();
        assert !outPortTypes[streamableOutPortIdx].isOptional();
        m_streamableInPortIdx = streamableInPortIdx;
        m_streamableOutPortIdx = streamableOutPortIdx;
    }

    // TODO this method really should be final, but it is API already :,(
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        final var in = inData[0];
        final BufferedDataTable out;
        final var r = createColumnRearranger(in.getDataTableSpec());
        if (exec.isTableSlicingEfficient() && isDistributable()) { // TODO more criteria?
            out = applyRearrangerParallel(in, r, exec);
        } else {
            out = exec.createColumnRearrangeTable(in, r, exec);
        }
        return new BufferedDataTable[]{out};
    }

    private static final BufferedDataTable applyRearrangerParallel(final BufferedDataTable in,
        final ColumnRearranger rearranger, final ExecutionContext exec)
        throws CanceledExecutionException, InterruptedException, ExecutionException {
        final var numChunks = Runtime.getRuntime().availableProcessors(); // TODO sensible default
        final var inputChunks = createChunks(in, numChunks, exec);
        final var futures = new Future[inputChunks.length];
        exec.setMessage("Executing in parallel");
        for (var i = 0; i < futures.length; ++i) {
            final var subExec = exec.createSilentSubProgress(0.9 / inputChunks.length);
            final var inputChunk = inputChunks[i];
            futures[i] = KNIMEConstants.GLOBAL_THREAD_POOL.submit(// TODO is the global thread pool the right choice?
                ThreadUtils.callableWithContext(() -> //
                exec.createColumnRearrangeTable(inputChunk, rearranger, subExec)));
        }
        final var outputChunks = new BufferedDataTable[futures.length];
        for (var i = 0; i < futures.length; ++i) {
            outputChunks[i] = (BufferedDataTable)futures[i].get();
        }
        return exec.createConcatenateTable(exec, Optional.empty(), false, outputChunks);
    }

    private static BufferedDataTable[] createChunks(final BufferedDataTable dt, final int numChunks,
        final ExecutionContext exec) {
        final var selections = new Selection[numChunks];
        long tableSize = dt.size();
        long chunkSize = tableSize / numChunks;
        for (var i = 0; i < numChunks; i++) {
            long endIndex = (i == numChunks - 1) ? tableSize : ((i + 1) * chunkSize);
            selections[i] = Selection.all().retainRows(i * chunkSize, endIndex);
        }
        final var subExec = exec.createSubExecutionContext(0.1);
        return InternalTableAPI.multiSlice(subExec, dt, selections);
    }

    /** {@inheritDoc} */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        DataTableSpec in = inSpecs[0];
        ColumnRearranger r = createColumnRearranger(in);
        DataTableSpec out = r.createSpec();
        return new DataTableSpec[]{out};
    }

    /**
     * Can the computation of the individual nodes run in parallel? Default is <code>true</code> but subclasses can
     * enforce sequential access by overwriting this method and returning <code>false</code>.
     *
     * @return true (possibly overwritten).
     */
    protected boolean isDistributable() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public InputPortRole[] getInputPortRoles() {
        InputPortRole[] in = new InputPortRole[getNrInPorts()];
        Arrays.fill(in, InputPortRole.NONDISTRIBUTED_NONSTREAMABLE);
        in[m_streamableInPortIdx] =
            isDistributable() ? org.knime.core.node.streamable.InputPortRole.DISTRIBUTED_STREAMABLE : org.knime.core.node.streamable.InputPortRole.NONDISTRIBUTED_STREAMABLE;
        return in;
    }

    /** {@inheritDoc} */
    @Override
    public OutputPortRole[] getOutputPortRoles() {
        OutputPortRole[] out = new OutputPortRole[getNrOutPorts()];
        Arrays.fill(out, OutputPortRole.NONDISTRIBUTED);
        out[m_streamableOutPortIdx] = isDistributable() ? OutputPortRole.DISTRIBUTED : OutputPortRole.NONDISTRIBUTED;
        return out;
    }

    /**
     * Creates a column rearranger that describes the changes to the input table. Sub classes will check the consistency
     * of the input table with their settings (fail with {@link InvalidSettingsException} if necessary) and then return
     * a customized {@link ColumnRearranger}.
     *
     * @param spec The spec of the input table.
     * @return A column rearranger describing the changes, never null.
     * @throws InvalidSettingsException If the settings or the input are invalid.
     */
    protected abstract ColumnRearranger createColumnRearranger(final DataTableSpec spec)
        throws InvalidSettingsException;

    /**
     * @return the streamableInPortIdx
     * @since 3.1
     */
    protected int getStreamableInPortIdx() {
        return m_streamableInPortIdx;
    }

    /**
     * @return the streamableOutPortIdx
     * @since 3.1
     */
    public int getStreamableOutPortIdx() {
        return m_streamableOutPortIdx;
    }

    /** {@inheritDoc} */
    @Override
    public StreamableFunction
        createStreamableOperator(final PartitionInfo partitionInfo, final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec in = (DataTableSpec)inSpecs[m_streamableInPortIdx];
        // Node developers often forget to check the initialization of fields in #createColumnRearranger because it is
        // not intuitive that it is called even if #configure throws an Exception (leaving the fields uninitialized).
        // Since it is also cumbersome to check all fields for proper initialization we add a fallback here.
        ColumnRearranger rearranger;
        try {
            rearranger = createColumnRearranger(in);
        } catch(RuntimeException | InvalidSettingsException e) {
            getLogger().debug("Could not create the column rearranger. Either this is an internal error or the node is "
                + "not configured yet.");
            throw new InvalidSettingsException("The node is not configured correctly.", e);
        }
        return rearranger.createStreamableFunction(m_streamableInPortIdx, m_streamableOutPortIdx);
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
        // possibly overwritten
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // possibly overwritten
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // possibly overwritten
    }

}
