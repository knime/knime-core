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
 * -------------------------------------------------------------------
 *
 */
package org.knime.core.data.append;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.append.AppendedRowsTable.DuplicatePolicy;
import org.knime.core.data.container.BlobSupportDataRow;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.Pair;

import com.google.common.collect.Maps;

/**
 * Iterator over an {@link AppendedRowsTable}.
 * Has {@link BlobSupportDataRow} support.
 *
 * @author Bernd Wiswedel, University of Konstanz
 * @since 3.1
 */
public class AppendedRowsIterator extends CloseableRowIterator {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(AppendedRowsIterator.class);

    /**
     * The spec of the underlying table.
     *
     * @see AppendedRowsTable#getDataTableSpec()
     */
    private final DataTableSpec m_spec;

    /** The to be concatenated inputs. */
    private final PairSupplier[] m_iteratorSuppliers;

    /** Suffix to append or null if to skip rows. */
    private final String m_suffix;

    /** Index of iterator over which is currently iterated. */
    private int m_curItIndex;

    /** The iterator from m_iteratorSuppliers[m_curItIndex]. */
    private RowIterator m_curIterator;

    /** Missing cells to be appended to rows of the current iterator (preferably always of length 0). */
    private DataCell[] m_curMissingCells;

    /**
     * The internal resorting of the columns (must follow the same order as the
     * top table, instantiated with each new internal iterator.
     */
    private int[] m_curMapping;

    /** The next row to be returned. null if atEnd() */
    private DataRow m_nextRow;

    /**
     * A record type to keep track of the source table and row key of duplicate keys
     *
     * @since 5.4
     */
    public record TableIndexAndRowKey(int index, RowKey key) {}

    /** Map to check for duplicates, including indices of the source table */
    private final Map<RowKey, TableIndexAndRowKey> m_duplicateMap;

    /** has printed error message for duplicate entries? */
    private boolean m_hasPrintedError = false;

    /** An execution monitor for progress/cancel or <code>null</code>. */
    private final ExecutionMonitor m_exec;

    /** The current row being processed. (starting with 0 at the first table) */
    private int m_curRowIndex = 0;

    /** The total number of rows (negative if unknown). */
    private final long m_totalRowCount;

    /** The number of rows skipped so far, just for user statistics. */
    private int m_nrRowsSkipped;

    /** Policy for duplicate rows. */
    private final DuplicatePolicy m_duplPolicy;

    private final boolean m_fillDuplicateMap;

    /**
     * Creates new iterator of <code>tables</code> following <code>spec</code>. The iterator may throw an exception in
     * next.
     *
     * @param exec for progress/cancel, may be <code>null</code>
     * @param totalRowCount the total row count or negative if unknown
     * @param fillDuplicateMap if true, the duplicate map ({@link #getDuplicateNameMap()} is also filled for
     *            {@link DuplicatePolicy#Fail} and {@link DuplicatePolicy#CreateNew} (the other policies fill it anyway)
     */
    AppendedRowsIterator(final PairSupplier[] tables, final DuplicatePolicy duplPolicy,
        final String suffix, final DataTableSpec spec, final ExecutionMonitor exec, final long totalRowCount,
        final boolean fillDuplicateMap) {
        m_iteratorSuppliers = CheckUtils.checkArgumentNotNull(tables);
        m_suffix = suffix;
        m_spec = CheckUtils.checkArgumentNotNull(spec);
        m_duplPolicy = CheckUtils.checkArgumentNotNull(duplPolicy);
        m_curItIndex = -1;
        m_duplicateMap = new HashMap<>();
        m_curMapping = new int[m_spec.getNumColumns()];
        m_exec = exec;
        m_totalRowCount = totalRowCount;
        if (m_iteratorSuppliers.length > 0) {
            initNextTable();
            initNextRow();
        }
        m_fillDuplicateMap = fillDuplicateMap;
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
                if (m_curItIndex < m_iteratorSuppliers.length - 1) {
                    initNextTable();
                } else { // final end
                    m_nextRow = null;
                    return; // reached end of this table
                }
            } while (!m_curIterator.hasNext());
        }
        DataRow baseRow = m_curIterator.next(); // row from table
        boolean keyHasChanged = false;
        RowKey origKey = baseRow.getKey();
        RowKey key = origKey;
        if (m_duplPolicy == DuplicatePolicy.CreateNew) {
            key = RowKey.createRowKey((long)m_curRowIndex);
            keyHasChanged = !origKey.equals(key);
        }
        m_curRowIndex++;
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
                m_nrRowsSkipped++;
                if (!m_curIterator.hasNext()) { // end of one table reached
                    // note, this causes one more call on the stack
                    // (but who wants to concatenate 60000 tables...)
                    initNextRow();
                    return;
                }
                if (m_exec != null) {
                    String message = "Skipping row " + m_curRowIndex + " (\""
                            + key.toString() + "\")";
                    if (m_totalRowCount > 0L) {
                        m_exec.setProgress(m_curRowIndex / (double)m_totalRowCount,
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
                    if (m_totalRowCount > 0L) {
                        m_exec.setProgress(m_curRowIndex / (double)m_totalRowCount,
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
            case CreateNew:
                throw new IllegalStateException("No duplicates should be detected if we create new RowIDs.");
            default:
                throw new RuntimeException("Unknown policy: " + m_duplPolicy);
            }
        }
        if (fillDuplicateMap()) {
            m_duplicateMap.put(key, new TableIndexAndRowKey(m_curItIndex, origKey));
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
            if (m_totalRowCount > 0L) {
                m_exec.setProgress(m_curRowIndex / (double)m_totalRowCount, message);
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
            final boolean blobRow = (nextRow instanceof BlobSupportDataRow);
            DataCell[] cells = new DataCell[nextRow.getNumCells()];
            for (int i = 0; i < cells.length; i++) {
                cells[i] = blobRow ? ((BlobSupportDataRow)nextRow).getRawCell(i) : nextRow.getCell(i);
            }
            m_nextRow = new BlobSupportDataRow(key, cells);
        } else {
            m_nextRow = nextRow;
        }
    }

    private boolean fillDuplicateMap() {
        return m_fillDuplicateMap || m_duplPolicy.needsDuplicateMap();
    }

    /**
     * Start iterator on next table.
     */
    private void initNextTable() {
        assert (m_curItIndex < m_iteratorSuppliers.length - 1);
        m_curItIndex++;
        Pair<RowIterator, DataTableSpec> pair = m_iteratorSuppliers[m_curItIndex].get();
        m_curIterator = pair.getFirst();
        DataTableSpec spec = pair.getSecond();
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
        return getDuplicateNameMap().keySet();
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
     * @see #getDuplicateNameMapWithIndices() if you are also interested in which table the row key was provided
     */
    public Map<RowKey, RowKey> getDuplicateNameMap() {
        final var view = Maps.transformValues(m_duplicateMap, pair -> pair.key);
        return Collections.unmodifiableMap(view);
    }


    /** Get a map of keys in the resulting table to the keys in
     * the respective input tables, typically elements such as below.
     * <table>
     * <tr><th>Key</th><th>Value</th></tr>
     * <tr><td>Row1</td><td>(0, Row1)</td></tr>
     * <tr><td>Row2</td><td>(0, Row2)</td></tr>
     * <tr><td>Row1_dup</td><td>(1, Row1)</td></tr>
     * </table>
     * @return Such a map (unmodifiable)
     * @see #getDuplicateNameMap() if you are not interested in the table indices
     * @since 5.4
     */
    public Map<RowKey, TableIndexAndRowKey> getDuplicateNameMapWithIndices() {
        return Collections.unmodifiableMap(m_duplicateMap);
    }

    @Override
    public void close() {
        if (m_curIterator instanceof CloseableRowIterator) {
            ((CloseableRowIterator)m_curIterator).close();
        }
        m_nextRow = null;
        m_curIterator = null;
        m_curItIndex = m_iteratorSuppliers.length - 1;
    }

    /** To be replaced by java 8 java.util.Supplier.
     * @deprecated ... */
    @Deprecated
    static final class PairSupplier {
        private final Pair<RowIterator, DataTableSpec> m_pair;

        PairSupplier(final Pair<RowIterator, DataTableSpec> pair) {
            m_pair = CheckUtils.checkArgumentNotNull(pair);
        }

        public Pair<RowIterator, DataTableSpec> get() {
            return m_pair;
        }
    }

    /**
     * Runtime exception that's thrown when the execution monitor's {@link ExecutionMonitor#checkCanceled()} method
     * throws a {@link CanceledExecutionException}.
     */
    public static final class RuntimeCanceledExecutionException extends RuntimeException {

        /**
         * Inits object.
         *
         * @param cee The exception to wrap.
         */
        private RuntimeCanceledExecutionException(final CanceledExecutionException cee) {
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
