/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   Jul 5, 2006 (wiswedel): created
 */
package de.unikn.knime.core.data.container;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Vector;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataColumnSpec;
import de.unikn.knime.core.data.DataRow;
import de.unikn.knime.core.data.DataTable;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.data.RowIterator;
import de.unikn.knime.core.data.RowKey;
import de.unikn.knime.core.data.container.ColumnRearranger.SpecAndFactoryObject;
import de.unikn.knime.core.data.def.DefaultRow;
import de.unikn.knime.core.node.BufferedDataTable;
import de.unikn.knime.core.node.CanceledExecutionException;
import de.unikn.knime.core.node.ExecutionMonitor;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeSettings;
import de.unikn.knime.core.node.NodeSettingsRO;
import de.unikn.knime.core.node.NodeSettingsWO;
import de.unikn.knime.core.node.BufferedDataTable.KnowsRowCountTable;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public class RearrangeColumnsTable implements DataTable, KnowsRowCountTable {
    
    private static final RowKey DUMMY_KEY = new RowKey("non-existing");
    private static final DataRow DUMMY_ROW = 
        new DefaultRow(DUMMY_KEY, new DataCell[0]);
    
    private static final RowIterator EMPTY_ITERATOR = new RowIterator() {
        public boolean hasNext() {
            return true;
        }
        public DataRow next() {
            return DUMMY_ROW;
        };
    };

    private static final String CFG_INTERNAL_META = "meta_internal";
    private static final String CFG_REFERENCE_ID = "table_reference_ID";
    private static final String CFG_MAP = "table_internal_map";
    private static final String CFG_FLAGS = "table_internal_flags";
    
    private final DataTableSpec m_spec;
    private final BufferedDataTable m_reference;
    private final int[] m_map;
    private final boolean[] m_isFromRefTable;
    private final NoKeyBuffer m_appendBuffer;

    /**
     * 
     */
    RearrangeColumnsTable(final BufferedDataTable reference, final int[] map, 
            final boolean[] isFromRefTable, final DataTableSpec spec, 
            NoKeyBuffer appendBuffer) {
        m_spec = spec;
        m_reference = reference;
        m_appendBuffer = appendBuffer;
        m_map = map;
        m_isFromRefTable = isFromRefTable;
    }
    
    public RearrangeColumnsTable(final File f, final NodeSettingsRO settings,
            final int loadID) throws IOException, InvalidSettingsException {
        NodeSettingsRO subSettings = settings.getNodeSettings(CFG_INTERNAL_META);
        int tableID = subSettings.getInt(CFG_REFERENCE_ID);
        m_reference = BufferedDataTable.getDataTable(loadID, tableID);
        m_map = subSettings.getIntArray(CFG_MAP);
        m_isFromRefTable = subSettings.getBooleanArray(CFG_FLAGS);
        DataColumnSpec[] appendColSpecs;
        boolean containsFalse = false;
        for (int i = 0; !containsFalse && i < m_isFromRefTable.length; i++) {
            if (!m_isFromRefTable[i]) {
                containsFalse = true;
            }
        }
        if (containsFalse) {
            m_appendBuffer = new NoKeyBuffer(f, loadID);
            DataTableSpec appendSpec = m_appendBuffer.getTableSpec();
            appendColSpecs = new DataColumnSpec[appendSpec.getNumColumns()];
            for (int i = 0; i < appendSpec.getNumColumns(); i++) {
                appendColSpecs[i] = appendSpec.getColumnSpec(i);
            }
        } else {
            m_appendBuffer = null;
            appendColSpecs = new DataColumnSpec [0];
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
    
    public BufferedDataTable getReferenceTable() {
        return m_reference;
    }

    /**
     * @see de.unikn.knime.core.data.DataTable#getDataTableSpec()
     */
    public DataTableSpec getDataTableSpec() {
        return m_spec;
    }

    /**
     * @see de.unikn.knime.core.data.DataTable#iterator()
     */
    public RowIterator iterator() {
        RowIterator appendIt;
        if (m_appendBuffer != null) {
            appendIt = m_appendBuffer.iterator();
        } else {
            appendIt = EMPTY_ITERATOR;
        }
        return new JoinTableIterator(
                m_reference.iterator(), appendIt, m_map, m_isFromRefTable);
    }
    
    
    public static RearrangeColumnsTable create(
            final ColumnRearranger rearranger, final BufferedDataTable table, 
            final ExecutionMonitor subProgress) 
        throws CanceledExecutionException {
        DataTableSpec originalSpec = rearranger.getOriginalSpec();
        Vector<SpecAndFactoryObject> includes = rearranger.getIncludes();
        // names and types of the specs must match
        if (!table.getDataTableSpec().equals(originalSpec)) {
            throw new IllegalArgumentException(
                    "The argument table's spec does not match the original " 
                    + "spec passed in the constructor.");
        }
        int size = includes.size();
        DataTableSpec spec = rearranger.createSpec();
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
        NoKeyBuffer appendBuffer;
        // for a pure filter (a table that just hides some columns from
        // the reference table but does not add any new column we avoid to scan
        // the entire table (nothing is written anyway))
        if (newColCount > 0) {
            DataContainer container = new DataContainer(
                    new DataTableSpec(newColSpecs), true) {
                protected Buffer newBuffer(final int rowsInMemory) {
                    return new NoKeyBuffer(rowsInMemory);
                }
            };
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
            appendBuffer = (NoKeyBuffer)container.getBuffer();
        } else {
            appendBuffer = null;
        }
        boolean[] isFromRefTable = new boolean[size];
        int[] includesIndex = new int[size];
        int newColIndex = 0;
        for (int i = 0; i < size; i++) {
            SpecAndFactoryObject c = includes.get(i);
            if (c.isNewColumn()) {
                isFromRefTable[i] = false;
                includesIndex[i] = newColIndex++;
            } else {
                isFromRefTable[i] = true;
                includesIndex[i] = c.getOriginalIndex();
            }
        }
        assert newColCount == newColCount;
        return new RearrangeColumnsTable(
                table, includesIndex, isFromRefTable, spec, appendBuffer);
    }

    /**
     * @see BufferedDataTable.KnowsRowCountTable#getRowCount()
     */
    public int getRowCount() {
        return m_reference.getRowCount();
    }
    
    /**
     * @see KnowsRowCountTable#saveToFile(File, NodeSettings, ExecutionMonitor)
     */
    public void saveToFile(
            final File f, final NodeSettingsWO s, final ExecutionMonitor exec) 
        throws IOException, CanceledExecutionException {
        NodeSettingsWO subSettings = s.addNodeSettings(CFG_INTERNAL_META);
        subSettings.addInt(CFG_REFERENCE_ID, m_reference.getBufferedTableId());
        subSettings.addIntArray(CFG_MAP, m_map);
        subSettings.addBooleanArray(CFG_FLAGS, m_isFromRefTable);
        // create it anyway, even if size is 0
        f.createNewFile();
        if (m_appendBuffer != null) {
            m_appendBuffer.saveToFile(f, exec);
        }
    }
    
    
}
