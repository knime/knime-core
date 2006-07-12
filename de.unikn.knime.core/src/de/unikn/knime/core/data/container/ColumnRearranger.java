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
 *   Jun 20, 2006 (wiswedel): created
 */
package de.unikn.knime.core.data.container;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataColumnSpec;
import de.unikn.knime.core.data.DataRow;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.data.DataType;
import de.unikn.knime.core.data.DataValue;
import de.unikn.knime.core.node.BufferedDataTable;
import de.unikn.knime.core.node.CanceledExecutionException;
import de.unikn.knime.core.node.ExecutionMonitor;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public final class ColumnRearranger {
    
    private static final DataCell[] EMPTY_CELL_ARRAY = new DataCell[0];
    private static final DataColumnSpec[] EMPTY_COL_ARRAY = 
        new DataColumnSpec[0];
    
    private static final CellFactory EMPTY_FACTORY = new CellFactory() {
        public DataCell[] getCells(final DataRow row) {
            return EMPTY_CELL_ARRAY;
        }
        public DataColumnSpec[] getColumnSpecs() {
            return EMPTY_COL_ARRAY;
        }
    };
    private static final ExecutionMonitor EMPTY_EXEC_MON = 
        new ExecutionMonitor();
    
    private final Vector<SpecAndFactoryObject> m_includes;
    
    public ColumnRearranger(final DataTableSpec original) {
        m_includes = new Vector<SpecAndFactoryObject>();
        m_includes.ensureCapacity(original.getNumColumns());
        for (int i = 0; i < original.getNumColumns(); i++) {
            m_includes.add(new SpecAndFactoryObject(original.getColumnSpec(i)));
        }
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
    }
    
    public void keepOnly(final String... colNames) {
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
    
    public void remove(final int... colIndices) {
        
    }
    
    static DataTableSpec createSpec(final DataTableSpec reference,
            final int[] map, final boolean[] isFromRefTable, 
            final DataColumnSpec[] newSpecs) {
        DataColumnSpec[] finalSpecs = new DataColumnSpec[map.length];
        for (int i = 0; i < map.length; i++) {
            if (isFromRefTable[i]) {
                finalSpecs[i] = reference.getColumnSpec(map[i]);
            } else {
                finalSpecs[i] = newSpecs[map[i]];
            }
        }
        return new DataTableSpec(finalSpecs);
    }
    
    private static final BufferedDataTable filter(
            final BufferedDataTable table, final int[] cols, final boolean include) {
        if (table == null) {
            throw new NullPointerException("Table must not be null.");
        }
        int maxValid = table.getDataTableSpec().getNumColumns() - 1;
        checkForDuplicates(maxValid, cols);
        int[] includes;
        if (!include) {
            // reverse selection
            HashSet<Integer> forbidden = new HashSet<Integer>();
            for (int i : cols) {
                forbidden.add(i);
            }
            int totalCount = table.getDataTableSpec().getNumColumns();
            includes = new int[totalCount - cols.length];
            int counter = 0;
            for (int i = 0; i < totalCount; i++) {
                if (!forbidden.contains(i)) {
                    includes[counter] = i;
                    counter++;
                }
            }
            assert (counter == includes.length);
        } else {
            includes = new int[cols.length];
            System.arraycopy(cols, 0, includes, 0, includes.length);
        }
        boolean[] flags = new boolean[includes.length];
        Arrays.fill(flags, true);
        try {
            return new BufferedDataTable(new RearrangeColumnsTable(
                    table, includes, flags, EMPTY_COL_ARRAY, EMPTY_FACTORY, 
                    EMPTY_EXEC_MON));
        } catch (CanceledExecutionException cee) {
            throw new InternalError("Can't throw canceled exception;" 
                    + " progress monitor is private");
        }
    }
    
    public static final BufferedDataTable filterInclude(
            final BufferedDataTable table, final int... includes) {
        return filter(table, includes, true);
    }
    
    public static final BufferedDataTable filterInclude(
            final BufferedDataTable table, final String... colNamesInclude) {
        DataTableSpec spec = table.getDataTableSpec();
        int[] includes = findColumnIndices(spec, colNamesInclude);
        return filterInclude(table, includes);
    }

    public static final BufferedDataTable filterInclude(
            final BufferedDataTable table, final DataColumnSpec... colInclude) {
        String[] colIncludeNames = new String[colInclude.length];
        for (int i = 0; i < colInclude.length; i++) {
            colIncludeNames[i] = colInclude[i].getName();
        }
        return filterInclude(table, colIncludeNames);
    }

    public static final BufferedDataTable filterInclude(
            final BufferedDataTable table, final Class<? extends DataValue>... valueInclude) {
        int[] includes = findColumnIndices(table.getDataTableSpec(), valueInclude);
        return filterInclude(table, includes);
    }
    
    public static final BufferedDataTable filterExclude(
            final BufferedDataTable table, final int... excludes) {
        return filter(table, excludes, false);
    }
    
    public static final BufferedDataTable filterExclude(
            final BufferedDataTable table, final String... colNamesExclude) {
        DataTableSpec spec = table.getDataTableSpec();
        int[] includes = findColumnIndices(spec, colNamesExclude);
        return filterExclude(table, includes);
    }
    
    public static final BufferedDataTable filterExclude(
            final BufferedDataTable table, final DataColumnSpec... colExcludes) {
        String[] colIncludeNames = new String[colExcludes.length];
        for (int i = 0; i < colExcludes.length; i++) {
            colIncludeNames[i] = colExcludes[i].getName();
        }
        return filterExclude(table, colIncludeNames);
    }
    
    public static final BufferedDataTable filterExclude(
            final BufferedDataTable table, final Class<? extends DataValue>... valueExcludes) {
        int[] includes = findColumnIndices(table.getDataTableSpec(), valueExcludes);
        return filterExclude(table, includes);
    }
    
    public static final BufferedDataTable append(
            final BufferedDataTable table, final ExecutionMonitor exec, 
            final CellFactory cellFactory, final DataColumnSpec... appendSpecs)
        throws CanceledExecutionException {
        int oldCount = table.getDataTableSpec().getNumColumns();
        int colCount = oldCount + appendSpecs.length;
        int[] allIndices = new int[colCount];
        boolean[] allFlags = new boolean[colCount];
        for (int i = 0; i < oldCount; i++) {
            allIndices[i] = i;
            allFlags[i] = true;
        }
        for (int i = 0; i < appendSpecs.length; i++) {
            allIndices[oldCount + i] = i;
            allFlags[oldCount + i] = false;
        }
        return new BufferedDataTable(new RearrangeColumnsTable(table, 
                allIndices, allFlags, appendSpecs, cellFactory, exec));
    }
    
    public static final BufferedDataTable replaceSingle(
            final BufferedDataTable table, final ExecutionMonitor exec, 
            final CellFactory cellFactory, final DataColumnSpec newSpec, 
            final String obsoleteColumn) throws CanceledExecutionException {
        DataTableSpec oldSpec = table.getDataTableSpec();
        int colCount = oldSpec.getNumColumns();
        int[] allIndices = new int[colCount];
        boolean[] allFlags = new boolean[colCount];
        for (int i = 0; i < colCount; i++) {
            allIndices[i] = i;
            allFlags[i] = true;
        }
        int replaceColIndex = oldSpec.findColumnIndex(obsoleteColumn);
        if (replaceColIndex < 0) {
            throw new IllegalArgumentException(
                    "No such column: " + obsoleteColumn);
        }
        allIndices[replaceColIndex] = 0; // first from factory
        allFlags[replaceColIndex] = false;
        return new BufferedDataTable(new RearrangeColumnsTable(table, 
                allIndices, allFlags, new DataColumnSpec[]{newSpec}, 
                cellFactory, exec));
    }
    
    private static void checkForDuplicates(final int max, final int... list) {
        HashSet<Integer> duplicateHash = new HashSet<Integer>();
        for (int i = 0; i < list.length; i++) {
            if (list[i] < 0) {
                throw new IndexOutOfBoundsException("Index < 0: " + list[i]);
            }
            if (list[i] > max) {
                throw new IndexOutOfBoundsException("Index > : " + max + ": " 
                        + list[i]);
            }
            if (!duplicateHash.add(list[i])) {
                throw new IllegalArgumentException(
                        "Duplicate index: " + list[i]);
            }
        }
    }
    
    private static int[] findColumnIndices(
            final DataTableSpec spec, final String[] colNames) {
        int[] includes = new int[colNames.length];
        for (int i = 0; i < includes.length; i++) {
            includes[i] = spec.findColumnIndex(colNames[i]);
            // fail-fast here
            if (includes[i] < 0) {
                throw new IllegalArgumentException("No such column: " 
                        + colNames[i]);
            }
        }
        return includes;
    }
    
    private static int[] findColumnIndices(
            final DataTableSpec spec, final Class<? extends DataValue>[] colValues) {
        ArrayList<Integer> indices = new ArrayList<Integer>();
        for (int i = 0; i < spec.getNumColumns(); i++) {
            DataColumnSpec cs = spec.getColumnSpec(i);
            DataType type = cs.getType();
            boolean isCompatible = false;
            for (int c = 0; !isCompatible && c < colValues.length; c++) {
                if (type.isCompatible(colValues[c])) {
                    isCompatible = true;
                }
            }
            if (isCompatible) {
                indices.add(i);
            }
        }
        int[] arrayIndices = new int[indices.size()];
        for (int i = 0; i < indices.size(); i++) {
            arrayIndices[i] = indices.get(i);
        }
        return arrayIndices;
    }
    
    private static class SpecAndFactoryObject {
        private final CellFactory m_factory;
        private final DataColumnSpec m_colSpec;
        private final int m_columnInFactory;
        
        private SpecAndFactoryObject(final DataColumnSpec colSpec) {
            m_colSpec = colSpec;
            m_columnInFactory = -1;
            m_factory = null;
        }
        
        private SpecAndFactoryObject(final CellFactory cellFactory, 
                final int index, final DataColumnSpec colSpec) {
            m_colSpec = colSpec;
            m_factory = cellFactory;
            m_columnInFactory = index;
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
    }
    
}
