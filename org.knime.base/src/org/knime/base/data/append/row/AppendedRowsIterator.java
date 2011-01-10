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
 */
package org.knime.base.data.append.row;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.knime.base.data.append.column.AppendedColumnRow;
import org.knime.base.data.append.row.AppendedRowsTable.DuplicatePolicy;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;

/**
 * Iterator over an
 * {@link AppendedRowsTable}.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public class AppendedRowsIterator extends RowIterator {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(AppendedRowsIterator.class);

    /**
     * The spec of the underlying table.
     *
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

    /**
     * Missing cells to be appended to rows of the current iterator (preferably
     * always of length 0).
     */
    private DataCell[] m_curMissingCells;

    /**
     * The internal resorting of the columns (must follow the same order as the
     * top table, instantiated with each new internal iterator.
     */
    private int[] m_curMapping;

    /** The next row to be returned. null if atEnd() */
    private DataRow m_nextRow;

    /** Map to check for duplicates. */
    private final Map<RowKey, RowKey> m_duplicateMap;

    /** has printed error message for duplicate entries? */
    private boolean m_hasPrintedError = false;

    /** An execution monitor for progress/cancel or <code>null</code>. */
    private final ExecutionMonitor m_exec;

    /** The current row being processed. (starting with 0 at the first table) */
    private int m_curRowIndex = 0;

    /** The total number of rows, double for floating point operation. */
    private final double m_totalRowCount;

    /** The number of rows skipped so far, just for user statistics. */
    private int m_nrRowsSkipped;

    /** Policy for duplicate rows. */
    private final DuplicatePolicy m_duplPolicy;

    /**
     * Creates new iterator of <code>tables</code> following <code>spec</code>.
     * The iterator may throw an exception in next.
     *
     * @param table Table host.
     *
     * @param exec for progress/cancel, may be <code>null</code>
     * @param totalRowCount the total row count or negative if unknown
     */
    AppendedRowsIterator(final AppendedRowsTable table,
            final ExecutionMonitor exec, final int totalRowCount) {
        m_tables = table.getTables();
        m_suffix = table.getSuffix();
        m_spec = table.getDataTableSpec();
        m_duplPolicy = table.getDuplPolicy();
        m_curTable = -1;
        m_duplicateMap = new HashMap<RowKey, RowKey>();
        m_curMapping = new int[m_spec.getNumColumns()];
        m_exec = exec;
        m_totalRowCount = totalRowCount;
        if (m_tables.length > 0) {
            initNextTable();
            initNextRow();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        return m_nextRow != null;
    }

    /**
     * {@inheritDoc}
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
     * Get the number of rows that have been skipped due to duplicate row
     * keys.
     * @return Number of rows skipped.
     */
    public int getNrRowsSkipped() {
        return m_nrRowsSkipped;
    }

    /**
     * Get next row internally.
     */
    private void initNextRow() {
        // reached end of table's iterator - take next
        if (!m_curIterator.hasNext()) {
            do {
                if (m_curTable < m_tables.length - 1) {
                    initNextTable();
                } else { // final end
                    m_nextRow = null;
                    return; // reached end of this table
                }
            } while (!m_curIterator.hasNext());
        }
        DataRow baseRow = m_curIterator.next(); // row from table
        m_curRowIndex++;
        boolean keyHasChanged = false;
        RowKey origKey = baseRow.getKey();
        RowKey key = origKey;
        while (m_duplicateMap.containsKey(key)) {
            if (m_exec != null) {
                try {
                    m_exec.checkCanceled();
                } catch (CanceledExecutionException cee) {
                    throw new RuntimeCanceledExecutionException(cee);
                }
            }
            switch (m_duplPolicy) {
            case Fail:
                assert false : "Duplicate checking is done in the BDT";
                throw new RuntimeException("Duplicate key \"" + key + "\"");
            case Skip:
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
                    m_nrRowsSkipped++;
                    String message = "Skipping row " + m_curRowIndex + " (\""
                            + key.toString() + "\")";
                    if (m_totalRowCount > 0) {
                        m_exec.setProgress(m_curRowIndex / m_totalRowCount,
                                message);
                    } else {
                        m_exec.setMessage(message);
                    }
                }
                baseRow = m_curIterator.next(); // row from table
                m_curRowIndex++;
                keyHasChanged = false; // stays false! rows have been skipped.
                origKey = baseRow.getKey();
                key = origKey;
                break;
            case AppendSuffix:
                // first time we come here
                if (!keyHasChanged && m_exec != null) {
                    String message = "Unifying row " + m_curRowIndex + " (\""
                            + key.toString() + "\")";
                    if (m_totalRowCount > 0) {
                        m_exec.setProgress(m_curRowIndex / m_totalRowCount,
                                message);
                    } else {
                        m_exec.setMessage(message);
                    }
                }
                keyHasChanged = true;
                String newId = key.toString() + m_suffix;
                key = new RowKey(newId);
                // do not print warning here, user specified explicitly
                // to do duplicate handling.
                break;
            default:
                throw new RuntimeException("Unknown policy: " + m_duplPolicy);
            }
        }
        switch (m_duplPolicy) {
        case Fail:
            // do not put key into map but leave it to the BufferedDataTable
            // to do a efficient duplicate checking
            break;
        default:
            m_duplicateMap.put(key, origKey);
        }
        if (m_exec != null) {
            try {
                m_exec.checkCanceled();
            } catch (CanceledExecutionException cee) {
                throw new RuntimeCanceledExecutionException(cee);
            }
            String message = "Adding row " + m_curRowIndex + " (\""
                    + key.toString() + "\""
                    + (keyHasChanged ? " uniquified)" : ")");
            if (m_totalRowCount > 0) {
                m_exec.setProgress(m_curRowIndex / m_totalRowCount, message);
            } else {
                m_exec.setMessage(message);
            }
        }
        DataRow nextRow;
        if (m_curMissingCells != null) {
            // no missing cells implies the base row is complete
            assert (m_curMissingCells.length + baseRow.getNumCells()
                == m_spec.getNumColumns());
            DataRow filledBaseRow = // row enlarged by "missing" columns
            new AppendedColumnRow(baseRow, m_curMissingCells);
            nextRow = new ResortedCellsRow(filledBaseRow, m_curMapping);
        } else {
            nextRow = baseRow;
        }
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
        boolean leaveUntouched = missingCounter == 0;
        for (int i = 0; leaveUntouched && i < m_curMapping.length; i++) {
            if (m_curMapping[i] != i) {
                leaveUntouched = false;
            }
        }
        if (leaveUntouched) {
            m_curMapping = null;
            m_curMissingCells = null;
        }
        assert missingCounter == missingNumber;
    }

    /**
     * Returns the set of all keys used in the resulting table.
     * @return unmodifiable set of all keys
     * @deprecated Use the key set of {@link #getDuplicateNameMap()} instead.
     */
    @Deprecated
    public Set<RowKey> getDuplicateHash() {
        return Collections.unmodifiableSet(m_duplicateMap.keySet());
    }

    /** Get a map of keys in the resulting table to the keys in (any of)
     * the input tables, typically elements such as below.
     * <table>
     * <tr><th>Key</th><th>Value</th></tr>
     * <tr><td>Row1</td><td>Row1</td></tr>
     * <tr><td>Row2</td><td>Row2</td></tr>
     * <tr><td>Row1_dup</td><td>Row1</td></tr>
     * </table>
     * @return Such a map (unmodifiable)
     */
    public Map<RowKey, RowKey> getDuplicateNameMap() {
        return Collections.unmodifiableMap(m_duplicateMap);
    }

    /**
     * Runtime exception that's thrown when the execution monitor's
     * {@link ExecutionMonitor#checkCanceled()} method throws a
     * {@link CanceledExecutionException}.
     */
    public static final class RuntimeCanceledExecutionException extends
            RuntimeException {

        /**
         * Inits object.
         *
         * @param cee The exception to wrap.
         */
        private RuntimeCanceledExecutionException(
                final CanceledExecutionException cee) {
            super(cee.getMessage(), cee);
        }

        /**
         * Get reference to causing exception.
         *
         * {@inheritDoc}
         */
        @Override
        public CanceledExecutionException getCause() {
            return (CanceledExecutionException)super.getCause();
        }
    }
}
