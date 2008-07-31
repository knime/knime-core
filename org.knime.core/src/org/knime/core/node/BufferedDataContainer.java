/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
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
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.Node.MemoryPolicy;

/**
 * <code>DataContainer</code> to be used during a 
 * <code>NodeModel</code>'s execution. 
 * A <code>BufferedDataContainer</code> is special implementation of a 
 * {@link DataContainer} whose <code>getTable()</code> returns a 
 * {@link BufferedDataTable}, i.e. the return value of each 
 * NodeModel's {@link NodeModel#execute(BufferedDataTable[], ExecutionContext) 
 * execute} method.
 * 
 * <p>Use a <code>BufferedDataContainer</code> when new data is aquired during
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
     * @param maxCellsInMemory Number of cells to be kept in memory, if negative
     * use user settings (according to node)
     * @param globalTableRepository 
     *        The global (WFM) table repository for blob (de)serialization.
     * @param localTableRepository 
     *        The local (Node) table repository for blob (de)serialization.
     * @see DataContainer#DataContainer(DataTableSpec, boolean)
     */
    BufferedDataContainer(final DataTableSpec spec, final boolean initDomain, 
            final Node node, final int maxCellsInMemory,
            final Map<Integer, ContainerTable> globalTableRepository,
            final Map<Integer, ContainerTable> localTableRepository) {
        super(spec, initDomain, maxCellsInMemory < 0 
                ? getMaxCellsInMemory(node) : maxCellsInMemory);
        m_node = node;
        m_globalTableRepository = globalTableRepository;
        m_localTableRepository = localTableRepository;
    }
    
    /** Check the node if its outport memory policy says we should keep 
     * everything in memory.
     * @param node The node to check.
     * @return Cells to be kept in memory.
     */
    private static int getMaxCellsInMemory(final Node node) {
        MemoryPolicy p = node.getOutDataMemoryPolicy();
        if (p.equals(MemoryPolicy.CacheInMemory)) {
            return Integer.MAX_VALUE;
        } else if (p.equals(MemoryPolicy.CacheSmallInMemory)) {
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
     * Just delegates to {@link DataContainer#readFromZipDelayed(
     * ReferencedFile, DataTableSpec, int, Map)} 
     * This method is available in this class to enable other classes in this
     * package to use it.
     * @param zipFileRef Delegated.
     * @param spec Delegated.
     * @param bufID Delegated.
     * @param bufferRep Delegated.
     * @return {@link DataContainer#readFromZipDelayed(
     *      ReferencedFile, DataTableSpec, int, Map)}
     */
    protected static ContainerTable readFromZipDelayed(
            final ReferencedFile zipFileRef, final DataTableSpec spec, 
            final int bufID, final Map<Integer, ContainerTable> bufferRep) {
        return DataContainer.readFromZipDelayed(
                zipFileRef, spec, bufID, bufferRep);
    }
}
