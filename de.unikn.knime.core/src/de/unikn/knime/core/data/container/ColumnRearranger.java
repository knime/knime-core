/* 
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
 *   Jun 20, 2006 (wiswedel): created
 */
package de.unikn.knime.core.data.container;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Vector;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataColumnSpec;
import de.unikn.knime.core.data.DataRow;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.data.RowIterator;
import de.unikn.knime.core.data.def.DefaultRow;
import de.unikn.knime.core.node.BufferedDataTable;
import de.unikn.knime.core.node.CanceledExecutionException;
import de.unikn.knime.core.node.ExecutionMonitor;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public final class ColumnRearranger {
    
    private final Vector<SpecAndFactoryObject> m_includes;
    private final DataTableSpec m_originalSpec;
    
    public ColumnRearranger(final DataTableSpec original) {
        m_includes = new Vector<SpecAndFactoryObject>();
        m_includes.ensureCapacity(original.getNumColumns());
        for (int i = 0; i < original.getNumColumns(); i++) {
            m_includes.add(new SpecAndFactoryObject(
                    original.getColumnSpec(i), i));
        }
        m_originalSpec = original;
    }
    
    public void keepOnly(final int... colIndices) {
        HashSet<Integer> hash = new HashSet<Integer>();
        final int currentSize = m_includes.size();
        for (int i : colIndices) {
            if (i < 0 || i >= currentSize) {
                throw new IndexOutOfBoundsException("Invalid index: " + i);
            }
            hash.add(i);
        }
        for (int i = currentSize - 1; i >= 0; i--) {
            if (!hash.remove(i)) {
                m_includes.remove(i);
            }
        }
        assert m_includes.size() == colIndices.length;
        assert hash.isEmpty();
    }
    
    public void keepOnly(final String... colNames) {
        HashSet<String> found = new HashSet<String>(Arrays.asList(colNames)); 
        for (Iterator<SpecAndFactoryObject> it = m_includes.iterator(); 
            it.hasNext();) {
            SpecAndFactoryObject cur = it.next();
            if (!found.remove(cur.getColSpec().getName())) {
                it.remove();
            }
        }
        if (!found.isEmpty()) {
            throw new IllegalArgumentException(
                    "No such column name(s) in " + getClass().getSimpleName()
                    + ": " + Arrays.toString(found.toArray()));
        }
        assert m_includes.size() == colNames.length;
    }
    
    public void remove(final int... colIndices) {
        int[] copy = new int[colIndices.length];
        System.arraycopy(colIndices, 0, copy, 0, copy.length);
        Arrays.sort(copy);
        for (int i = copy.length - 1; i >= 0; i--) {
            m_includes.remove(copy[i]);
        }
    }
    
    public void remove(final String... colNames) {
        HashSet<String> found = new HashSet<String>(Arrays.asList(colNames));
        for (Iterator<SpecAndFactoryObject> it = m_includes.iterator(); 
            it.hasNext();) {
                SpecAndFactoryObject cur = it.next();
                if (found.remove(cur.getColSpec().getName())) {
                    it.remove();
                }
            }
        if (!found.isEmpty()) {
            throw new IllegalArgumentException(
                    "No such column name(s) in " + getClass().getSimpleName()
                    + ": " + Arrays.toString(found.toArray()));
        }
    }
    
    public int indexOf(final String colName) {
        if (colName == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        for (int i = 0; i < m_includes.size(); i++) {
            SpecAndFactoryObject cur = m_includes.get(i);
            if (cur.getColSpec().getName().equals(colName)) {
                return i; 
            }
        }
        return -1;
    }
    
    public void insertAt(final int position, final CellFactory fac) {
        DataColumnSpec[] colSpecs = fac.getColumnSpecs();
        SpecAndFactoryObject[] ins = new SpecAndFactoryObject[colSpecs.length];
        for (int i = 0; i < ins.length; i++) {
            ins[i] = new SpecAndFactoryObject(fac, i, colSpecs[i]);
        }
        for (int i = ins.length - 1; i >= 0; i--) {
            m_includes.insertElementAt(ins[i], position);
        }
    }
    
    public void append(final CellFactory fac) {
        insertAt(m_includes.size(), fac);
    }
    
    public void replace(final CellFactory newCol, final String colName) {
        int index = indexOf(colName);
        if (index < 0) {
            throw new IllegalArgumentException("No such column: " + colName);
        }
        replace(newCol, index);
    }
    
    public void replace(final CellFactory fac, final int... colIndex) {
        DataColumnSpec[] colSpecs = fac.getColumnSpecs();
        if (colSpecs.length != colIndex.length) {
            throw new IndexOutOfBoundsException(
                    "Arguments do not apply to same number of columns: "
                    + colSpecs.length + " vs. " + colIndex.length + ")");
        }
        for (int i = 0; i < colSpecs.length; i++) {
            remove(colIndex[i]);
            SpecAndFactoryObject s = 
                new SpecAndFactoryObject(fac, i, colSpecs[i]);
            m_includes.insertElementAt(s, colIndex[i]);
        }
    }
    
    
    
    public DataTableSpec createSpec() {
        final int size = m_includes.size();
        DataColumnSpec[] colSpecs = new DataColumnSpec[size];
        for (int i = 0; i < size; i++) {
            colSpecs[i] = m_includes.get(i).getColSpec();
        }
        return new DataTableSpec(colSpecs);
    }
    
    public BufferedDataTable createTable(
            final BufferedDataTable table, final ExecutionMonitor exec)
        throws CanceledExecutionException {
        // names and types of the specs must match
        if (!table.getDataTableSpec().equals(m_originalSpec)) {
            throw new IllegalArgumentException(
                    "The argument table's spec does not match the original " 
                    + "spec passed in the constructor.");
        }
        int size = m_includes.size();
        DataTableSpec spec = createSpec();
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
            SpecAndFactoryObject s = m_includes.get(i);
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
                            row.getKey(), exec);
                    DataRow appendix = new DefaultRow(row.getKey(), newCells);
                    container.addRowToTable(appendix);
                    exec.checkCanceled();
                }
            } finally {
                container.close();
            }
            appendBuffer = (NoKeyBuffer)container.getBuffer();
        } else {
            appendBuffer = null;
        }
        boolean[] isFromRefTable = new boolean[size];
        int[] includes = new int[size];
        int newColIndex = 0;
        for (int i = 0; i < size; i++) {
            SpecAndFactoryObject c = m_includes.get(i);
            if (c.isNewColumn()) {
                isFromRefTable[i] = false;
                includes[i] = newColIndex++;
            } else {
                isFromRefTable[i] = true;
                includes[i] = c.getOriginalIndex();
            }
        }
        assert newColCount == newColCount;
        RearrangeColumnsTable t = new RearrangeColumnsTable(
                table, includes, isFromRefTable, spec, appendBuffer);
        return new BufferedDataTable(t);
    }
    
    private static class SpecAndFactoryObject {
        private final CellFactory m_factory;
        private final DataColumnSpec m_colSpec;
        private final int m_columnInFactory;
        private final int m_originalIndex;
        
        private SpecAndFactoryObject(
                final DataColumnSpec colSpec, final int originalIndex) {
            m_colSpec = colSpec;
            m_columnInFactory = -1;
            m_factory = null;
            m_originalIndex = originalIndex;
        }
        
        private SpecAndFactoryObject(final CellFactory cellFactory, 
                final int index, final DataColumnSpec colSpec) {
            m_colSpec = colSpec;
            m_factory = cellFactory;
            m_columnInFactory = index;
            m_originalIndex = -1;
        }

        /**
         * @return Returns the colSpec.
         */
        final DataColumnSpec getColSpec() {
            return m_colSpec;
        }

        /**
         * @return Returns the columnInFactory.
         */
        final int getColumnInFactory() {
            return m_columnInFactory;
        }

        /**
         * @return Returns the factory.
         */
        final CellFactory getFactory() {
            return m_factory;
        }
        
        final boolean isNewColumn() {
            return m_factory != null;
        }

        /**
         * @return Returns the originalIndex.
         */
        final int getOriginalIndex() {
            return m_originalIndex;
        }
    }
    
}
