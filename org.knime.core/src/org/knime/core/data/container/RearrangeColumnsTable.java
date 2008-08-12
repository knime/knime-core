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
 *   Jul 5, 2006 (wiswedel): created
 */
package org.knime.core.data.container;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Vector;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.ColumnRearranger.SpecAndFactoryObject;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.BufferedDataTable.KnowsRowCountTable;


/**
 * Table implementation that is created based on a ColumnRearranger. This class
 * is not intended for subclassing or to be used directly in any node 
 * implementation. Instead use the functionality provided through the 
 * {@link ColumnRearranger} and the {@link org.knime.core.node.ExecutionContext}
 * that is provided in the NodeModel's execute method. See 
 * {@link ColumnRearranger} for more details on how to use them.
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class RearrangeColumnsTable 
    implements DataTable, KnowsRowCountTable {
    
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(RearrangeColumnsTable.class);
    
    private static final RowKey DUMMY_KEY = new RowKey("non-existing");
    private static final DataRow DUMMY_ROW = 
        new DefaultRow(DUMMY_KEY, new DataCell[0]);
    
    /** If this table just filters columns from the reference table, we use
     * this dummy iterator to provide empty appended cells.
     */
    private static final CloseableRowIterator EMPTY_ITERATOR = 
        new CloseableRowIterator() {
        @Override
        public boolean hasNext() {
            return true;
        }
        @Override
        public DataRow next() {
            return DUMMY_ROW;
        }
        @Override
        public void close() {
        }
    };

    private static final String CFG_INTERNAL_META = "meta_internal";
    private static final String CFG_REFERENCE_ID = "table_reference_ID";
    private static final String CFG_MAP = "table_internal_map";
    private static final String CFG_FLAGS = "table_internal_flags";
    
    private final DataTableSpec m_spec;
    private final BufferedDataTable m_reference;
    private final int[] m_map;
    private final boolean[] m_isFromRefTable;
    private final ContainerTable m_appendTable;

    /*
     * Used from the factory method, see below.
     * @see #create(ColumnRearranger, BufferedDataTable, ExecutionMonitor)
     */
    private RearrangeColumnsTable(final BufferedDataTable reference, 
            final int[] map, final boolean[] isFromRefTable, 
            final DataTableSpec spec, final ContainerTable appendTbl) {
        m_spec = spec;
        m_reference = reference;
        m_appendTable = appendTbl;
        m_map = map;
        m_isFromRefTable = isFromRefTable;
    }
    
    /**
     * Creates new object based on the content in <code>settings</code> and
     * the content from the file <code>f</code>. Used when the data is restored
     * from disc. 
     * 
     * <p><b>Note</b>: You should not be required to use this constructor!
     * @param f The file to read from the newly appended columns.
     * @param settings The settings containing the information how to assemble
     *         the table.
     * @param tblRep The table repository (only available during start)
     * @param spec The data table spec of the resulting table. This argument
     * is <code>null</code> when the data to restore is written using 
     * KNIME 1.1.x or before.
     * @param tableID buffer ID of underlying buffer.
     * @param bufferRep Repository of buffers for blob (de)serialization.
     * @throws IOException If reading the fails.
     * @throws InvalidSettingsException If the settings are invalid.
     */
    public RearrangeColumnsTable(final ReferencedFile f, 
            final NodeSettingsRO settings, 
            final Map<Integer, BufferedDataTable> tblRep, 
            final DataTableSpec spec, final int tableID,
            final HashMap<Integer, ContainerTable> bufferRep) 
        throws IOException, InvalidSettingsException {
        NodeSettingsRO subSettings = 
            settings.getNodeSettings(CFG_INTERNAL_META);
        int refTableID = subSettings.getInt(CFG_REFERENCE_ID);
        m_reference = BufferedDataTable.getDataTable(tblRep, refTableID);
        m_map = subSettings.getIntArray(CFG_MAP);
        m_isFromRefTable = subSettings.getBooleanArray(CFG_FLAGS);
        DataColumnSpec[] appendColSpecs;
        int appendColCount = 0;
        for (int i = 0; i < m_isFromRefTable.length; i++) {
            if (!m_isFromRefTable[i]) {
                appendColCount += 1;
            }
        }
        // appendColCount may be null for a column filter, for instance.
        if (appendColCount > 0) {
            appendColSpecs = new DataColumnSpec[appendColCount];
            // was written with version 1.1.x or before (i.e. table spec 
            // is contained in zip file)
            if (spec == null) { 
                m_appendTable = DataContainer.readFromZip(
                        f, new NoKeyBufferCreator());
                DataTableSpec appendSpec = m_appendTable.getDataTableSpec();
                if (appendSpec.getNumColumns() != appendColCount) {
                    throw new IOException("Inconsistency in data file \"" 
                            + f + "\", read " + appendSpec.getNumColumns() 
                            + " columns, expected " + appendColCount);
                }
                for (int i = 0; i < appendSpec.getNumColumns(); i++) {
                    appendColSpecs[i] = appendSpec.getColumnSpec(i);
                }
            } else { // version 1.2.0 or later.
                int index = 0;
                for (int i = 0; i < m_isFromRefTable.length; i++) {
                    if (!m_isFromRefTable[i]) {
                        assert m_map[i] == index;
                        appendColSpecs[index++] = spec.getColumnSpec(i);
                    }
                }
                assert index == appendColCount;
                DataTableSpec appendSpec = new DataTableSpec(appendColSpecs);
                CopyOnAccessTask noKeyBufferOnAccessTask = new CopyOnAccessTask(
                        f, appendSpec, tableID, bufferRep, 
                        new NoKeyBufferCreator());
                m_appendTable = DataContainer.readFromZipDelayed(
                        noKeyBufferOnAccessTask, appendSpec);
            }
        } else {
            m_appendTable = null;
            appendColSpecs = new DataColumnSpec[0];
        }
        DataTableSpec refSpec = m_reference.getDataTableSpec();
        DataColumnSpec[] colSpecs = new DataColumnSpec[m_isFromRefTable.length];
        for (int i = 0; i < colSpecs.length; i++) {
            if (m_isFromRefTable[i]) {
                colSpecs[i] = refSpec.getColumnSpec(m_map[i]);
            } else {
                colSpecs[i] = appendColSpecs[m_map[i]];
            }
        }
        m_spec = new DataTableSpec(colSpecs);
    }
    
    /** Get handle to the reference table in an array of length 1.
     * @return The table providing likely most of the columns and the rowkeys.
     */
    public BufferedDataTable[] getReferenceTables() {
        return new BufferedDataTable[]{m_reference};
    }
    
    /** Get reference to the appended table. This table must not be used
     * publicly as the append table is corrupted: It does not contain proper
     * row keys (it contains only the appended columns). This method returns
     * null if this table only filters out some of the columns.
     * @return Reference to append table.
     */
    public ContainerTable getAppendTable() {
        return m_appendTable;
    }

    /**
     * {@inheritDoc}
     */
    public DataTableSpec getDataTableSpec() {
        return m_spec;
    }

    /**
     * {@inheritDoc}
     */
    public CloseableRowIterator iterator() {
        CloseableRowIterator appendIt;
        if (m_appendTable != null) {
            appendIt = m_appendTable.iterator();
        } else {
            appendIt = EMPTY_ITERATOR;
        }
        return new JoinTableIterator(
                m_reference.iterator(), appendIt, m_map, m_isFromRefTable);
    }
    
    /** 
     * This factory method is intended to be used immediately before the 
     * {@link BufferedDataTable} is created. 
     * @param rearranger The meta information how to assemble everything.
     * @param table The reference table.
     * @param subProgress The progress monitor for progress/cancel.
     * @param context Used for data container creation.
     * @return The newly created table.
     * @throws CanceledExecutionException If canceled.
     * @throws IllegalArgumentException If the spec is not equal to the
     * spec of the rearranger.
     */
    public static RearrangeColumnsTable create(
            final ColumnRearranger rearranger, final BufferedDataTable table, 
            final ExecutionMonitor subProgress, final ExecutionContext context)
        throws CanceledExecutionException {
        DataTableSpec originalSpec = rearranger.getOriginalSpec();
        Vector<SpecAndFactoryObject> includes = rearranger.getIncludes();
        // names and types of the specs must match
        if (!table.getDataTableSpec().equalStructure(originalSpec)) {
            throw new IllegalArgumentException(
                    "The argument table's spec does not match the original " 
                    + "spec passed in the constructor.");
        }
        int size = includes.size();
        ArrayList<DataColumnSpec> newColSpecsList = 
            new ArrayList<DataColumnSpec>();
        // the reduced set of SpecAndFactoryObject that models newly
        // appended/inserted columns; this vector is in most cases 
        // considerably smaller than m_includes
        Vector<SpecAndFactoryObject> reducedList = 
            new Vector<SpecAndFactoryObject>();
        int newColCount = 0;
        IdentityHashMap<CellFactory, Object> counter =
            new IdentityHashMap<CellFactory, Object>();
        for (int i = 0; i < size; i++) {
            SpecAndFactoryObject s = includes.get(i);
            if (s.isNewColumn()) {
                counter.put(s.getFactory(), null);
                reducedList.add(s);
                newColSpecsList.add(s.getColSpec());
                newColCount++;
            }
        }
        // number of different factories used, in 99% of all cases 
        // this is either 0 or 1
        final int factoryCount = counter.size();
        DataColumnSpec[] newColSpecs = 
            newColSpecsList.toArray(new DataColumnSpec[newColSpecsList.size()]);
        ContainerTable appendTable;
        DataTableSpec appendTableSpec;
        // for a pure filter (a table that just hides some columns from
        // the reference table but does not add any new column we avoid to scan
        // the entire table (nothing is written anyway))
        if (newColCount > 0) {
            DataContainer container = 
                context.createDataContainer(new DataTableSpec(newColSpecs));
            container.setBufferCreator(new NoKeyBufferCreator());
            assert reducedList.size() == newColCount;
            int finalRowCount = table.getRowCount();
            int r = 0;
            try {
                for (RowIterator it = table.iterator(); it.hasNext(); r++) {
                    DataRow row = it.next();
                    DataCell[] newCells = new DataCell[newColCount];
                    int factoryCountRow = 0;
                    CellFactory facForProgress = null;
                    for (int i = 0; i < newColCount; i++) {
                        // early stopping, if we have just one factory but 
                        // many many columns, this if statement will save a lot
                        if (factoryCount == factoryCountRow) {
                            break;
                        }
                        if (newCells[i] != null) {
                            continue;
                        }
                        SpecAndFactoryObject cur = reducedList.get(i);
                        CellFactory fac = cur.getFactory();
                        if (facForProgress == null) {
                            facForProgress = fac;
                        }
                        factoryCountRow++;
                        DataCell[] fromFac = fac.getCells(row);
                        for (int j = 0; j < newColCount; j++) {
                            SpecAndFactoryObject checkMe = reducedList.get(j);
                            if (checkMe.getFactory() == fac) {
                                assert newCells[j] == null;
                                newCells[j] = 
                                    fromFac[checkMe.getColumnInFactory()];
                            }
                        }
                    }
                    assert facForProgress != null;
                    facForProgress.setProgress(r + 1, finalRowCount, 
                            row.getKey(), subProgress);
                    DataRow appendix = new DefaultRow(row.getKey(), newCells);
                    container.addRowToTable(appendix);
                    subProgress.checkCanceled();
                }
            } finally {
                container.close();
            }
            appendTable = container.getBufferedTable();
            appendTableSpec = appendTable.getDataTableSpec();
        } else {
            appendTable = null;
            appendTableSpec = new DataTableSpec();
        }
        boolean[] isFromRefTable = new boolean[size];
        int[] includesIndex = new int[size];
        // create the new spec. Do not use rearranger.createSpec because
        // that might lack the domain information!
        DataColumnSpec[] colSpecs = new DataColumnSpec[size];
        int newColIndex = 0;
        for (int i = 0; i < size; i++) {
            SpecAndFactoryObject c = includes.get(i);
            if (c.isNewColumn()) {
                isFromRefTable[i] = false;
                includesIndex[i] = newColIndex;
                colSpecs[i] = appendTableSpec.getColumnSpec(newColIndex); 
                newColIndex++;
            } else {
                isFromRefTable[i] = true;
                int originalIndex = c.getOriginalIndex();
                includesIndex[i] = originalIndex;
                colSpecs[i] = originalSpec.getColumnSpec(originalIndex);
            }
        }
        DataTableSpec spec = new DataTableSpec(colSpecs);
        assert newColCount == newColCount;
        return new RearrangeColumnsTable(
                table, includesIndex, isFromRefTable, spec, appendTable);
    }

    /**
     * {@inheritDoc}
     */
    public int getRowCount() {
        return m_reference.getRowCount();
    }
    
    /**
     * {@inheritDoc}
     */
    public void saveToFile(
            final File f, final NodeSettingsWO s, final ExecutionMonitor exec) 
        throws IOException, CanceledExecutionException {
        NodeSettingsWO subSettings = s.addNodeSettings(CFG_INTERNAL_META);
        subSettings.addInt(CFG_REFERENCE_ID, m_reference.getBufferedTableId());
        subSettings.addIntArray(CFG_MAP, m_map);
        subSettings.addBooleanArray(CFG_FLAGS, m_isFromRefTable);
        if (m_appendTable != null) {
            // subSettings argument is ignored in ContainerTable
            m_appendTable.saveToFile(f, subSettings, exec);
        }
    }
    
    /**
     * Do not call this method! It's used internally to delete temp files. 
     * Any iteration on the table will fail!
     * @see KnowsRowCountTable#clear()
     */
    public void clear() {
        if (m_appendTable != null) {
            m_appendTable.clear();
        }
    }
    
    /** Internal use.
     * {@inheritDoc} */
    public void ensureOpen() {
        if (m_appendTable != null) {
            m_appendTable.ensureOpen();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void putIntoTableRepository(
            final HashMap<Integer, ContainerTable> rep) {
        if (m_appendTable != null) {
            rep.put(m_appendTable.getBufferID(), m_appendTable);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void removeFromTableRepository(
            final HashMap<Integer, ContainerTable> rep) {
        if (m_appendTable != null) {
            int id = m_appendTable.getBufferID();
            if (rep.remove(id) == null) {
                LOGGER.debug("Failed to remove appended table with id " 
                        + id + " from global table repository.");
            }
        }
    }
    
    /** Creates NoKeyBuffer objects rather then Buffer objects. */
    private static class NoKeyBufferCreator 
        extends DataContainer.BufferCreator {

        /** @see DataContainer.BufferCreator#createBuffer(int, int, Map, Map) */
        @Override
        Buffer createBuffer(final int rowsInMemory, final int bufferID, 
                final Map<Integer, ContainerTable> globalTableRep, 
                final Map<Integer, ContainerTable> localTableRep) {
            return new NoKeyBuffer(
                    rowsInMemory, bufferID, globalTableRep, localTableRep);
        }
        
        /** @see DataContainer.BufferCreator#createBuffer(
         *       File, File, DataTableSpec, InputStream, int, Map)
        */
        @Override
        Buffer createBuffer(final File binFile, final File blobDir, 
                final DataTableSpec spec, final InputStream metaIn, 
                final int bufID, final Map<Integer, ContainerTable> tblRep) 
            throws IOException {
            return new NoKeyBuffer(
                    binFile, blobDir, spec, metaIn, bufID, tblRep);
        }
    }
}
