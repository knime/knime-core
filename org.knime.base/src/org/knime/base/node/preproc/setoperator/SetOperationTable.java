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
 *    22.11.2007 (Tobias Koetter): created
 */

package org.knime.base.node.preproc.setoperator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

import org.knime.base.data.sort.SortedTable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * This class handles the result table creation for the Set node. It provides
 * also a static method to create the result table specification.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class SetOperationTable {

    private class CellIterator implements Iterator<RowKeyCellMap> {

        private final RowIterator m_iterator;

        private final boolean m_useRowID;

        private final int m_colIdx;

        private final int m_rowCount;

        /**Constructor for class CellIterator.
         * @param table
         * @param useRowID
         * @param colIdx
         */
        public CellIterator(final SortedTable table, final boolean useRowID,
                final int colIdx) {
            if (table == null) {
                throw new NullPointerException("table must not be null");
            }
            if (!useRowID && (colIdx < 0
                    || colIdx > table.getDataTableSpec().getNumColumns())) {
                throw new IllegalArgumentException("Invalid column index");
            }
            m_iterator = table.iterator();
            m_rowCount = table.getRowCount();
            m_useRowID = useRowID;
            m_colIdx = colIdx;
        }


        /**
         * @return the total number of rows
         */
        public int getRowCount() {
            return m_rowCount;
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasNext() {
            return m_iterator.hasNext();
        }

        /**
         * {@inheritDoc}
         */
        public RowKeyCellMap next() {
            final DataRow row = m_iterator.next();
            final DataCell cell;
            if (m_useRowID) {
                cell = new StringCell(row.getKey().getString());
            } else {
                cell = row.getCell(m_colIdx);
            }
            return new RowKeyCellMap(row.getKey(), cell);
        }

        /**
         * {@inheritDoc}
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    private final BufferedDataTable m_resultTable;

    private int m_duplicateCounter = 0;
    private int m_missingCounter = 0;

    private final boolean m_skipMisssing;

    private int m_rowId;

    private final boolean m_enableHilite;


    private final Map<RowKey, Set<RowKey>> m_hiliteMapping0;

    private final Map<RowKey, Set<RowKey>> m_hiliteMapping1;

    /**Constructor for class SetOperationTable.
     * @param exec the {@link ExecutionContext}
     * @param useRowID1 <code>true</code> if the row id should be used instead
     * of a column
     * @param col1 the column name of the first set
     * @param table1 the table of the first set
     * @param useRowID2 <code>true</code> if the row id should be used instead
     * of a column
     * @param col2 the column name of the second set
     * @param table2 the table of the second set
     * @param op the {@link SetOperation} to perform
     * @param enableHilite <code>true</code> if hilite translation should
     * be performed
     * @param skipMissing <code>true</code> if missing cells should be skipped
     * @param sortInMemory <code>true</code> if the sorting should be
     * performed in memory
     * @param
     * @throws CanceledExecutionException if the operation was canceled
     */
    public SetOperationTable(final ExecutionContext exec,
            final boolean useRowID1, final String col1,
            final BufferedDataTable table1, final boolean useRowID2,
            final String col2, final BufferedDataTable table2,
            final SetOperation op, final boolean enableHilite,
            final boolean skipMissing, final boolean sortInMemory)
    throws CanceledExecutionException {
        if (exec == null) {
            throw new NullPointerException("exec must not be null");
        }
        if (table1 == null) {
            throw new NullPointerException("table1 must not be null");
        }
        if (table2 == null) {
            throw new NullPointerException("table2 must not be null");
        }
        if (op == null) {
            throw new NullPointerException("op must not be null");
        }
        m_enableHilite = enableHilite;
        if (m_enableHilite) {
            m_hiliteMapping0 = new HashMap<RowKey, Set<RowKey>>();
            m_hiliteMapping1 = new HashMap<RowKey, Set<RowKey>>();
        } else {
            m_hiliteMapping0 = null;
            m_hiliteMapping1 = null;
        }
        final int col1Idx;
        final DataColumnSpec col1Spec;
        if (useRowID1) {
            col1Idx = -1;
            col1Spec = createRowIDSpec("RowID1");
        } else {
            if (col1 == null) {
                throw new NullPointerException("col1 must not be null");
            }
            col1Idx = table1.getDataTableSpec().findColumnIndex(col1);
            if (col1Idx < 0) {
                throw new IllegalArgumentException(
                        "No column spec found for column1 in table1");
            }
            col1Spec = table1.getDataTableSpec().getColumnSpec(col1Idx);
            if (col1Spec == null) {
                throw new NullPointerException("Col1Spec must not be null");
            }
        }
        final int col2Idx;
        final DataColumnSpec col2Spec;
        if (useRowID2) {
            col2Idx = -1;
            col2Spec = createRowIDSpec("RowID2");
        } else {
            if (col2 == null) {
                throw new NullPointerException("col2 must not be null");
            }
            col2Idx = table2.getDataTableSpec().findColumnIndex(col2);
            if (col2Idx < 0) {
                throw new IllegalArgumentException(
                        "No column spec found for column2 in table2");
            }
            col2Spec = table2.getDataTableSpec().getColumnSpec(col2Idx);
            if (col2Spec == null) {
                throw new NullPointerException("Col2Spec must not be null");
            }
        }

        m_skipMisssing = skipMissing;
        final DataValueComparator comp;
        if (useRowID1 || useRowID2) {
            comp = GeneralDataValueComparator.getInstance();
        } else {
            comp = op.getComparator(col1Spec, col2Spec);
        }
        //create own comparator
        final SingleColRowComparator rowComparator =
            new SingleColRowComparator(col1Idx, comp);
        exec.setMessage("Sorting first table...");
        ExecutionContext subExec = exec.createSubExecutionContext(0.3);
        final SortedTable sortedTabel1 =
            new SortedTable(table1, rowComparator, sortInMemory, subExec);
        exec.setMessage("Sorting second table...");
        subExec = exec.createSubExecutionContext(0.3);
        rowComparator.setColumnIndex(col2Idx);
        final SortedTable sortedTabel2 =
            new SortedTable(table2, rowComparator, sortInMemory, subExec);
        exec.setMessage("Performing set operation");
        subExec = exec.createSubExecutionContext(0.6);
        final DataTableSpec resultSpec =
            createResultTableSpec(op, col1Spec, col2Spec);

        final boolean differentType = useRowID1 || useRowID2
                    || !col1Spec.getType().equals(col2Spec.getType());
        //create the set objects
        final CellIterator columnSet1 =
            new CellIterator(sortedTabel1, useRowID1, col1Idx);
        final CellIterator columnSet2 =
            new CellIterator(sortedTabel2, useRowID2, col2Idx);
        m_resultTable = createSetTable(subExec, resultSpec, differentType,
                columnSet1, columnSet2, op, comp);
    }

    private BufferedDataTable createSetTable(final ExecutionContext exec,
            final DataTableSpec resultSpec, final boolean differentType,
            final CellIterator iter1, final CellIterator iter2,
            final SetOperation op, final DataValueComparator comp)
    throws CanceledExecutionException {
        //reset the rowid to minus 1 to use the ++m_rowId
        m_rowId = -1;
        final BufferedDataContainer dc = exec.createDataContainer(resultSpec);
        if ((SetOperation.OR.equals(op) && (iter1.getRowCount() < 1
                && iter2.getRowCount() < 1))
                || iter1.getRowCount() < 1
                || iter2.getRowCount() < 1) {
            dc.close();
            return dc.getTable();
        }

        final int rowCount =
            iter1.getRowCount() + iter2.getRowCount();
        int rowCounter = 0;
        final double progressPerRow = 1.0 / rowCount;
        RowKeyCellMap cell1 = null;
        RowKeyCellMap cell2 = null;
        DataCell oldResult = null;
        while (iter1.hasNext() && iter2.hasNext()) {
            cell1 = iter1.next();
            rowCounter++;
            cell2 = iter2.next();
            rowCounter++;
            int compResult = comp.compare(cell1.getCell(), cell2.getCell());
            while (compResult != 0) {
                if (compResult < 0) {
                    //the first entry is less
                    oldResult = computeResult(op, dc, oldResult, cell1, null,
                            differentType);
                    cell1 = null;
                    if (iter1.hasNext()) {
                        cell1 = iter1.next();
                        rowCounter++;
                        compResult = comp.compare(cell1.getCell(),
                                cell2.getCell());
                        reportProgress(exec, rowCount, rowCounter,
                                progressPerRow);
                    } else {
                        break;
                    }
                } else if (compResult > 0) {
                    //the second entry is less
                    oldResult = computeResult(op, dc, oldResult, null, cell2,
                            differentType);
                    cell2 = null;
                    if (iter2.hasNext()) {
                        cell2 = iter2.next();
                        rowCounter++;
                        compResult = comp.compare(cell1.getCell(),
                                cell2.getCell());
                        reportProgress(exec, rowCount, rowCounter,
                                progressPerRow);
                    } else {
                        break;
                    }
                }
            }
            if (compResult == 0) {
                //they are equal process both
                oldResult = computeResult(op, dc, oldResult, cell1, cell2,
                        differentType);
                cell1 = null;
                cell2 = null;
                reportProgress(exec, rowCount, rowCounter, progressPerRow);
            }
        }
        //that's only the case if all elements of set2 are less than all
        //elements of set1
        if (cell1 != null) {
            oldResult = computeResult(op, dc, oldResult, cell1, null,
                    differentType);
        }
        //process all left elements
        while (iter1.hasNext()) {
            cell1 = iter1.next();
            rowCounter++;
            oldResult = computeResult(op, dc, oldResult, cell1, null,
                    differentType);
            reportProgress(exec, rowCount, rowCounter,
                    progressPerRow);
        }
      //that's only the case if all elements of set1 are less than all
        //elements of set2
        if (cell2 != null) {
            oldResult = computeResult(op, dc, oldResult, null, cell2,
                    differentType);
        }
        while (iter2.hasNext()) {
            cell2 = iter2.next();
            rowCounter++;
            oldResult = computeResult(op, dc, oldResult, null, cell2,
                    differentType);
            reportProgress(exec, rowCount, rowCounter,
                    progressPerRow);
        }
        dc.close();
        return dc.getTable();
    }

    private static void reportProgress(final ExecutionContext exec,
            final int rowCount, final int rowCounter,
            final double progressPerRow)
            throws CanceledExecutionException {
        exec.checkCanceled();
        exec.setProgress(progressPerRow * rowCounter, "Processing row "
                + rowCounter + " of " + rowCount);
    }

    private DataCell computeResult(final SetOperation op,
            final BufferedDataContainer dc, final DataCell oldResult,
            final RowKeyCellMap keyCell0, final RowKeyCellMap keyCell1,
            final boolean differntType) {
        DataCell cell0;
        if (keyCell0 != null) {
            cell0 = keyCell0.getCell();
        } else {
            cell0 = null;
        }
        DataCell cell1;
        if (keyCell1 != null) {
            cell1 = keyCell1.getCell();
        } else {
            cell1 = null;
        }
        final DataCell result =
            op.compute(cell0, cell1, differntType);
        if (result != null) {
            if (result.equals(oldResult)) {
                if (m_enableHilite) {
                    assert (m_rowId >= 0);
                    final RowKey currentKey = RowKey.createRowKey(m_rowId);
                    if (keyCell0 != null) {
                        final Set<RowKey> map0 =
                            m_hiliteMapping0.get(currentKey);
                        map0.add(keyCell0.getRowKey());
                    }
                    if (keyCell1 != null) {
                        final Set<RowKey> map1 =
                            m_hiliteMapping1.get(currentKey);
                        map1.add(keyCell1.getRowKey());
                    }
                }
                m_duplicateCounter++;
                return oldResult;
            } else if (m_skipMisssing && result.isMissing()) {
                m_missingCounter++;
                return oldResult;
            } else {
                final RowKey rowKey = RowKey.createRowKey(++m_rowId);
                dc.addRowToTable(new DefaultRow(rowKey, result));
                if (m_enableHilite) {
                    if (keyCell0 != null) {
                        final HashSet<RowKey> map0 = new HashSet<RowKey>();
                        map0.add(keyCell0.getRowKey());
                        m_hiliteMapping0.put(rowKey, map0);
                    }
                    if (keyCell1 != null) {
                        final HashSet<RowKey> map1 = new HashSet<RowKey>();
                        map1.add(keyCell1.getRowKey());
                        m_hiliteMapping1.put(rowKey, map1);
                    }
                }
                return result;
            }
        }
        return oldResult;
    }


    /**
     * @return the number of duplicates
     */
    public int getDuplicateCounter() {
        return m_duplicateCounter;
    }


    /**
     * @return the number of missing values
     */
    public int getMissingCounter() {
        return m_missingCounter;
    }

    /**
     * @return the resulting set as a {@link BufferedDataTable}
     */
    public BufferedDataTable getBufferedTable() {
        return m_resultTable;
    }

    /**
     * @param name the name of the column
     * @return the column spec with the given column name and the type of the
     * RowID
     */
    public static DataColumnSpec createRowIDSpec(final String name) {
        final DataColumnSpecCreator specCreator =
            new DataColumnSpecCreator(name, StringCell.TYPE);
        return specCreator.createSpec();
    }

    /**
     * @param op the {@link SetOperation}
     * @param col1Spec the {@link DataColumnSpec} of the first set
     * @param col2Spec the {@link DataColumnSpec} of the second set
     * @return the result {@link DataTableSpec}
     */
    public static DataTableSpec createResultTableSpec(final SetOperation op,
            final DataColumnSpec col1Spec, final DataColumnSpec col2Spec) {
        final DataColumnSpec resultColSpec =
            op.createResultColSpec(col1Spec, col2Spec);
        final DataTableSpec tableSpec = new DataTableSpec(resultColSpec);
        return tableSpec;
    }

    /**
     * The hilite translation <code>Map</code> for the first set
     * or <code>null</code> if the enableHilte flag in the constructor was
     * set to <code>false</code>.
     * The key of the <code>Map</code> is the row key of each row and
     * the corresponding value is the <code>Collection</code> with all old row
     * keys which belong to this element.
     * @return the hilite translation <code>Map</code> or <code>null</code> if
     * the enableHilte flag in the constructor was set to <code>false</code>.
     */
    public Map<RowKey, Set<RowKey>> getHiliteMapping0() {
        return m_hiliteMapping0;
    }

    /**
     * The hilite translation <code>Map</code> for the second set
     * or <code>null</code> if the enableHilte flag in the constructor was
     * set to <code>false</code>.
     * The key of the <code>Map</code> is the row key of each row and
     * the corresponding value is the <code>Collection</code> with all old row
     * keys which belong to this element.
     * @return the hilite translation <code>Map</code> or <code>null</code> if
     * the enableHilte flag in the constructor was set to <code>false</code>.
     */
    public Map<RowKey, Set<RowKey>> getHiliteMapping1() {
        return m_hiliteMapping1;
    }
}
