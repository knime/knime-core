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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 *
 * History
 *   Jul 17, 2006 (wiswedel): created
 */
package org.knime.core.node;

import java.util.Map;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.filestore.internal.FileStoreHandlerRepository;
import org.knime.core.data.filestore.internal.WorkflowFileStoreHandlerRepository;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.workflow.SingleNodeContainer.MemoryPolicy;

/**
 * <code>DataContainer</code> to be used during a
 * <code>NodeModel</code>'s execution.
 * A <code>BufferedDataContainer</code> is special implementation of a
 * {@link DataContainer} whose <code>getTable()</code> returns a
 * {@link BufferedDataTable}, i.e. the return value of each
 * NodeModel's {@link NodeModel#execute(BufferedDataTable[], ExecutionContext)
 * execute} method.
 *
 * <p>Use a <code>BufferedDataContainer</code> when new data is acquired during
 * the execution or if it does not pay off to reference a node's input data
 * (it does pay off when you only append a column to the input data, for
 * instance). Please see the {@link ExecutionContext} for more details on how
 * to create <code>BufferedDataTable</code>'s.
 *
 * <p>To get a quick start how to use a <code>BufferedDataTable</code>, see
 * the following code:
 * <pre>
 * protected final BufferedDataTable[] execute(
 *      final BufferedDataTable[] data, final ExecutionContext exec)
 *      throws Exception {
 *  // the DataTableSpec of the final table
 *  DataTableSpec spec = new DataTableSpec(
 *          new DataColumnSpecCreator("A", StringCell.TYPE).createSpec(),
 *          new DataColumnSpecCreator("B", DoubleCell.TYPE).createSpec());
 *  // init the container
 *  BufferedDataContainer container = exec.createDataContainer(spec);
 *
 *  // add arbitrary number of rows to the container
 *  DataRow firstRow = new DefaultRow(new RowKey("first"), new DataCell[]{
 *      new StringCell("A1"), new DoubleCell(1.0)
 *  });
 *  container.addRowToTable(firstRow);
 *  DataRow secondRow = new DefaultRow(new RowKey("second"), new DataCell[]{
 *      new StringCell("B1"), new DoubleCell(2.0)
 *  });
 *  container.addRowToTable(secondRow);
 *
 *  // finally close the container and get the result table.
 *  container.close();
 *  BufferedDataTable result = container.getTable();
 *  ...
 *
 * </pre>
 * <p>For a more detailed explanation refer to the description of the
 * {@link DataContainer} class.
 *
 * @see DataContainer
 * @see ExecutionContext
 * @author Bernd Wiswedel, University of Konstanz
 */
public class BufferedDataContainer extends DataContainer {

    private final Node m_node;
    private final Map<Integer, ContainerTable> m_globalTableRepository;
    private final Map<Integer, ContainerTable> m_localTableRepository;
    private BufferedDataTable m_resultTable;

    /**
     * Creates new container.
     * @param spec The table spec.
     * @param initDomain Whether or not the spec's domain shall be used for
     * initialization.
     * @param node The owner of the outcome table.
     * @param forceCopyOfBlobs The property whether to copy any blob cell
     * @param maxCellsInMemory Number of cells to be kept in memory, if negative
     * use user settings (according to node)
     * being added, see {@link DataContainer#setForceCopyOfBlobs(boolean)}.
     * @param globalTableRepository
     *        The global (WFM) table repository for blob (de)serialization.
     * @param localTableRepository
     *        The local (Node) table repository for blob (de)serialization.
     * @see DataContainer#DataContainer(DataTableSpec, boolean)
     */
    BufferedDataContainer(final DataTableSpec spec, final boolean initDomain,
            final Node node, final MemoryPolicy policy,
            final boolean forceCopyOfBlobs, final int maxCellsInMemory,
            final Map<Integer, ContainerTable> globalTableRepository,
            final Map<Integer, ContainerTable> localTableRepository,
            final IWriteFileStoreHandler fileStoreHandler) {
        // force synchronous IO when the node is a loop end:
        // rows containing blobs need to be written instantly as their owning
        // buffer is discarded in the next loop iteration, see bug 2935
        super(spec, initDomain, maxCellsInMemory < 0
                ? getMaxCellsInMemory(policy) : maxCellsInMemory,
                        node.isForceSychronousIO());
        m_node = node;
        m_globalTableRepository = globalTableRepository;
        m_localTableRepository = localTableRepository;
        super.setFileStoreHandler(fileStoreHandler);
        super.setForceCopyOfBlobs(forceCopyOfBlobs);
    }

    /**
     * Returns the number of cells to be kept in memory according to the
     * passed policy.
     * @param memPolicy the policy to apply
     * @return number of cells to be kept in memory
     */
    private static int getMaxCellsInMemory(final MemoryPolicy memPolicy) {
        if (memPolicy.equals(MemoryPolicy.CacheInMemory)) {
            return Integer.MAX_VALUE;
        } else if (memPolicy.equals(MemoryPolicy.CacheSmallInMemory)) {
            return DataContainer.MAX_CELLS_IN_MEMORY;
        } else {
            return 0;
        }
    }

    /** {@inheritDoc} */
    @Override
    protected int createInternalBufferID() {
        return BufferedDataTable.generateNewID();
    }

    /** Returns the table repository from this workflow.
     * {@inheritDoc}
     */
    @Override
    protected Map<Integer, ContainerTable> getGlobalTableRepository() {
        return m_globalTableRepository;
    }

    /**
     * Returns the local repository of tables. It contains tables that have
     * been created during the execution of a node.
     * {@inheritDoc}
     */
    @Override
    protected Map<Integer, ContainerTable> getLocalTableRepository() {
        return m_localTableRepository;
    }

    /**
     * Returns the content of this container in a BufferedDataTable. The result
     * can be returned, e.g. in a NodeModel's execute method.
     * {@inheritDoc}
     */
    @Override
    public BufferedDataTable getTable() {
        if (m_resultTable == null) {
            ContainerTable buffer = getBufferedTable();
            m_resultTable = new BufferedDataTable(buffer, buffer.getBufferID());
            m_resultTable.setOwnerRecursively(m_node);
        }
        return m_resultTable;
    }

    /**
     * Just delegates to {@link DataContainer#readFromZipDelayed(ReferencedFile,
     * DataTableSpec, int, Map, WorkflowFileStoreHandlerRepository)}
     * This method is available in this class to enable other classes in this
     * package to use it.
     * @param zipFileRef Delegated.
     * @param spec Delegated.
     * @param bufID Delegated.
     * @param bufferRep Delegated.
     * @param fileStoreHandlerRepository Delegated.
     * @return {@link DataContainer#readFromZipDelayed(ReferencedFile,
     * DataTableSpec, int, Map, WorkflowFileStoreHandlerRepository)}
     * @noreference This method is not intended to be referenced by clients.
     */
    protected static ContainerTable readFromZipDelayed(
            final ReferencedFile zipFileRef, final DataTableSpec spec,
            final int bufID, final Map<Integer, ContainerTable> bufferRep,
            final FileStoreHandlerRepository fileStoreHandlerRepository) {
        return DataContainer.readFromZipDelayed(zipFileRef, spec,
                bufID, bufferRep, fileStoreHandlerRepository);
    }
}
