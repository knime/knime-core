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
 */
package de.unikn.knime.base.data.append.row;

import java.util.HashSet;
import java.util.NoSuchElementException;

import de.unikn.knime.base.data.append.column.AppendedColumnRow;
import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataColumnSpec;
import de.unikn.knime.core.data.DataRow;
import de.unikn.knime.core.data.DataTable;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.data.DataType;
import de.unikn.knime.core.data.RowIterator;
import de.unikn.knime.core.data.RowKey;
import de.unikn.knime.core.data.def.DefaultRow;
import de.unikn.knime.core.node.BufferedDataTable;
import de.unikn.knime.core.node.CanceledExecutionException;
import de.unikn.knime.core.node.ExecutionMonitor;
import de.unikn.knime.core.node.NodeLogger;

/**
 * Iterator over an <code>AppendedRowsTable</code>.
 * @author Bernd Wiswedel, University of Konstanz
 */
public class AppendedRowsIterator extends RowIterator {
    
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(AppendedRowsIterator.class);
    
    /** The spec of the underlying table.
     * @see AppendedRowsTable#getDataTableSpec()
     */
    private final DataTableSpec m_spec;
    
    /** The concatenated tables. */ 
    private final DataTable[] m_tables;
    
    /** Suffix to append or null if to skip rows. */
    private final String m_suffix;
    
    /** The table over which is currently iterated. */ 
    private int m_curTable;
    
    /** The internal iterator over m_tables[m_curTable]. */
    private RowIterator m_curIterator;
    
    /** Missing cells to be appended to rows of the current iterator 
     * (preferably always of length 0). */
    private DataCell[] m_curMissingCells;
    
    /** The internal resorting of the columns (must follow the same order
     * as the top table, instantiated with each new internal iterator. */
    private int[] m_curMapping;
    
    /** The next row to be returned. null if atEnd() */
    private DataRow m_nextRow;
    
    /** HashSet to check for duplicates. */
    private final HashSet<RowKey> m_duplicateHash;
    
    /** has printed error message for duplicate entries? */
    private boolean m_hasPrintedError = false;
    
    /** An execution monitor for progress/cancel or <code>null</code>. */
    private final ExecutionMonitor m_exec;
    
    /** The current row being processed. (starting with 0 at the first table) */
    private int m_curRowIndex = 0;
    
    /** The total number of rows, double for floating point operation. */
    private final double m_totalRowCount;
    
    /**
     * Creates new iterator of <code>tables</code> following <code>spec</code>.
     * @param tables To iterate over.
     * @param spec Table spec of underlying table (used to determine missing
     * @param suffix The suffix to append to duplicate rows or null to skip 
     * duplicates in this iterator (prints warning)
     * columns and order) 
     */
    AppendedRowsIterator(final DataTable[] tables, 
            final DataTableSpec spec, final String suffix) {
        this(tables, spec, suffix, null);
    }
    
    /**
     * Creates new iterator of <code>tables</code> following <code>spec</code>.
     * The iterator may throw an exception in next.
     * @param tables To iterate over.
     * @param spec Table spec of underlying table (used to determine missing
     * @param suffix The suffix to append to duplicate rows or null to skip 
     * duplicates in this iterator (prints warning)
     * columns and order) 
     * @param exec For progress/cancel, may be <code>null</code>.
     */
    AppendedRowsIterator(final DataTable[] tables, final DataTableSpec spec, 
            final String suffix, final ExecutionMonitor exec) {
        m_curMapping = new int[spec.getNumColumns()];
        m_tables = tables;
        m_suffix = suffix;
        m_spec = spec;
        m_curTable = -1;
        m_duplicateHash = new HashSet<RowKey>();
        m_exec = exec;
        int rowCount = 0;
        for (DataTable t : tables) {
            if (t instanceof BufferedDataTable) {
                rowCount += ((BufferedDataTable)t).getRowCount();
            } else {
                rowCount = -1;
                break;
            }
        }
        m_totalRowCount = rowCount;
        initNextTable();
        initNextRow();
    }

    /**
     * @see de.unikn.knime.core.data.RowIterator#hasNext()
     */
    @Override
    public boolean hasNext() {
        return m_nextRow != null; 
    }

    /**
     * @see de.unikn.knime.core.data.RowIterator#next()
     */
    @Override
    public DataRow next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        DataRow next = m_nextRow;
        initNextRow();
        return next;
    }
    
    /**
     * Get next row internally.
     */
    private void initNextRow() {
        if (!m_curIterator.hasNext()) { 
            // reached of table's iterator - take next
            if (m_curTable < m_tables.length - 1) {
                initNextTable();
            } else { // final end
                m_nextRow = null;
                return; // reached end of this table
            }
        }
        DataRow baseRow = m_curIterator.next(); // row from table
        m_curRowIndex++; 
        boolean keyHasChanged = false;
        RowKey key = baseRow.getKey();
        while (!m_duplicateHash.add(key)) {
            if (m_exec != null) {
                try {
                    m_exec.checkCanceled();
                } catch (CanceledExecutionException cee) {
                    throw new RuntimeCanceledExecutionException(cee);
                }
            }
            if (m_suffix == null) {
                if (!m_hasPrintedError) {
                    LOGGER.warn("Table contains duplicate entry \""
                            + key.toString() + "\", skipping this row. "
                            + "Suppress further warnings.");
                    m_hasPrintedError = true;
                }
                if (!m_curIterator.hasNext()) { // end of one table reached
                    // note, this causes one more call on the stack 
                    // (but who wants to concatenate 60000 tables...)
                    initNextRow();
                    return;
                }
                if (m_exec != null) {
                    String message = "Skipping row " + m_curRowIndex 
                        + " (\"" + key.toString() + "\")";  
                    if (m_totalRowCount > 0) {
                        m_exec.setProgress(
                                m_curRowIndex / m_totalRowCount, message);
                    } else {
                        m_exec.setMessage(message);
                    }
                }
                baseRow = m_curIterator.next(); // row from table
                m_curRowIndex++; 
                keyHasChanged = false; // stays false! rows have been skipped.
                key = baseRow.getKey();
            } else {
                // first time we come here
                if (!keyHasChanged && m_exec != null) { 
                    String message = "Unifying row " + m_curRowIndex 
                        + " (\"" + key.toString() + "\")";  
                    if (m_totalRowCount > 0) {
                        m_exec.setProgress(
                                m_curRowIndex / m_totalRowCount, message);
                    } else {
                        m_exec.setMessage(message);
                    }
                }
                keyHasChanged = true;
                DataCell cell = key.getId();
                String newId = cell.toString() + m_suffix;
                key = new RowKey(newId);
                // do not print warning here, user specified explicitly
                // to do duplicate handling.
            }
        }
        if (m_exec != null) {
            try {
                m_exec.checkCanceled();
            } catch (CanceledExecutionException cee) {
                throw new RuntimeCanceledExecutionException(cee);
            }
            String message = "Adding row " + m_curRowIndex 
                + " (\"" + key.toString() + "\"" 
                + (keyHasChanged ? " uniquified)" : ")");  
            if (m_totalRowCount > 0) {
                m_exec.setProgress(m_curRowIndex / m_totalRowCount, message);
            } else {
                m_exec.setMessage(message);
            }
        }
        // no missing cells implies the base row is complete
        assert (m_curMissingCells.length + baseRow.getNumCells() 
                == m_spec.getNumColumns());
        DataRow filledBaseRow = // row enlarged by "missing" columns
            new AppendedColumnRow(baseRow, m_curMissingCells);
        DataRow nextRow = new ResortedCellsRow(filledBaseRow, m_curMapping); 
        if (keyHasChanged) {
            DataCell[] cells = new DataCell[nextRow.getNumCells()];
            for (int i = 0; i < cells.length; i++) {
                cells[i] = nextRow.getCell(i);
            }
            m_nextRow = new DefaultRow(key, cells);
        } else {
            m_nextRow = nextRow;
        }
    }
    
    /**
     * Start iterator on next table.
     */
    private void initNextTable() {
        assert (m_curTable < m_tables.length - 1);
        m_curTable++;
        m_curIterator = m_tables[m_curTable].iterator();
        DataTableSpec spec = m_tables[m_curTable].getDataTableSpec();
        int missingNumber = m_spec.getNumColumns() - spec.getNumColumns();
        m_curMissingCells = new DataCell[missingNumber];
        int missingCounter = 0;
        m_curMapping = new int[m_spec.getNumColumns()];
        for (int c = 0; c < m_spec.getNumColumns(); c++) {
            DataColumnSpec colSpec = m_spec.getColumnSpec(c);
            int targetCol = spec.findColumnIndex(colSpec.getName());
            if (targetCol < 0) { // that is one of the "missing" columns
                targetCol = spec.getNumColumns() + missingCounter;
                // create the missing cell
                m_curMissingCells[missingCounter] = DataType.getMissingCell();
                missingCounter++;
            }
            m_curMapping[c] = targetCol;
        }
        assert missingCounter == missingNumber;
    }
    
    /** Runtime exception that's thrown when the execution monitor's 
     * <code>checkCanceled</code> method throws an CanceledExecutionException.
     */ 
    public static final class RuntimeCanceledExecutionException 
        extends RuntimeException {
        
        /** Inits object.
         * @param cee The exception to wrap.
         */
        private RuntimeCanceledExecutionException(
                final CanceledExecutionException cee) {
            super(cee.getMessage(), cee);
        }
        
        /** Get reference to causing exception.
         * @see java.lang.Throwable#getCause()
         */ 
        public CanceledExecutionException getCause() {
            return (CanceledExecutionException)super.getCause();
        }
    }
    
}
