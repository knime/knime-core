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
 *   May 18, 2006 (ritmeier): created
 */
package org.knime.testing.node.differNode;

import java.util.Iterator;
import java.util.Vector;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DefaultTable;
import org.knime.core.data.def.StringCell;


/**
 * 
 * @author ritmeier, University of Konstanz
 */
public class DataTableDiffer implements TestEvaluator {

    private DataTable m_dataTable1 = null;

    private DataTable m_dataTable2 = null;

    private DataTable m_DiffTable;

    /**
     * Constructor. Creates a new DataTableDiffer with two DataTables.
     * 
     * @param table1 - 1st Table
     * @param table2 - 2nd Table
     */
    public DataTableDiffer(final DataTable table1, final DataTable table2) {
        super();
        this.m_dataTable1 = table1;
        this.m_dataTable2 = table2;
    }

    /**
     * default Constructor.
     */
    public DataTableDiffer() {
        super();
    }

    /**
     * Checks each cell for equality.
     * 
     * @return true if each cell in the datatables equals, false otherwise
     */
    public boolean compare() {
        if (!m_dataTable1.getDataTableSpec().equalStructure(
                m_dataTable2.getDataTableSpec())) {
            return false;
        }
        Iterator<DataRow> rowIt1 = m_dataTable1.iterator();
        Iterator<DataRow> rowIt2 = m_dataTable2.iterator();
        while (rowIt1.hasNext() && rowIt2.hasNext()) {
            Iterator<DataCell> cellIt1 = rowIt1.next().iterator();
            Iterator<DataCell> cellIt2 = rowIt2.next().iterator();
            while (cellIt1.hasNext() && cellIt2.hasNext()) {
                if (!cellIt1.next().equals(cellIt2.next())) {
                    return false;
                }
            }
        }
        if (rowIt1.hasNext() || rowIt2.hasNext()) {
            return false;
        }
        return true;
    }

    /**
     * 
     * @throws TestEvaluationException
     * @see org.knime.testing.node.differNode.TestEvaluator#compare(
     *      org.knime.core.data.DataTable,
     *      org.knime.core.data.DataTable)
     */
    public void compare(final DataTable table1, final DataTable table2)
            throws TestEvaluationException {
        if (!table1.getDataTableSpec().equalStructure(
                table2.getDataTableSpec())) {
            throw new TestEvaluationException("The specs are not the same");
        }
        Iterator<DataRow> rowIt1 = table1.iterator();
        Iterator<DataRow> rowIt2 = table2.iterator();
        while (rowIt1.hasNext() && rowIt2.hasNext()) {
            Iterator<DataCell> cellIt1 = rowIt1.next().iterator();
            Iterator<DataCell> cellIt2 = rowIt2.next().iterator();
            while (cellIt1.hasNext() && cellIt2.hasNext()) {
                if (!cellIt1.next().equals(cellIt2.next())) {
                    throw new TestEvaluationException(
                            "The tablecontents are not the same");
                }
            }
        }
    }

    /**
     * Compares the two dataTableSpecs and returns the diff positions.
     * 
     * @return diff positions
     */
    public int[] diffTableSpec() {
        DataTableSpec spec1 = m_dataTable1.getDataTableSpec();
        DataTableSpec spec2 = m_dataTable2.getDataTableSpec();
        Vector<Integer> diffs = new Vector<Integer>();
        Iterator specIt1 = spec1.iterator();
        Iterator specIt2 = spec2.iterator();
        int diffpos = 0;
        while (specIt1.hasNext() && specIt2.hasNext()) {
            if (!specIt1.next().equals(specIt2.next())) {
                diffs.add(diffpos);
            }
            diffpos++;
        }
        // add remaining comlums to diff
        if (spec1.getNumColumns() > spec2.getNumColumns()) {
            while (diffpos < spec1.getNumColumns()) {
                diffs.add(diffpos);
                diffpos++;
            }
        }
        if (spec1.getNumColumns() < spec2.getNumColumns()) {
            while (diffpos < spec2.getNumColumns()) {
                diffs.add(diffpos);
                diffpos++;
            }
        }

        int[] result = new int[diffs.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = diffs.get(i).intValue();
        }
        return result;

    }

    /**
     * Returns the difference table, with those cells marked, which are not
     * equal.
     * 
     * @return - the difference table.
     */
    public DataTable getDiffTable() {
        if (m_DiffTable == null) {
            Vector<DataRow> rows = new Vector<DataRow>();
            Iterator<DataRow> rowIt1 = m_dataTable1.iterator();
            Iterator<DataRow> rowIt2 = m_dataTable2.iterator();
            if (rowIt1.hasNext() != rowIt2.hasNext()) {
                throw new IllegalStateException("The tables are not "
                        + "of the same size!");
            }

            while (rowIt1.hasNext() && rowIt2.hasNext()) {
                DataRow currentRow = rowIt1.next();

                Iterator<DataCell> cellIt1 = currentRow.iterator();
                Iterator<DataCell> cellIt2 = rowIt2.next().iterator();
                DataCell[] cells = new DataCell[currentRow.getNumCells()];
                DataCell currentCell1 = null;
                DataCell currentCell2 = null;
                // TODO tg boolean rowDiffers = false;
                int pos = 0;
                while (cellIt1.hasNext() && cellIt2.hasNext()) {
                    currentCell1 = cellIt1.next();
                    currentCell2 = cellIt2.next();
                    if (currentCell1.equals(currentCell2)) {
                        cells[pos] = currentCell1;
                    } else {
                        // TODO tg rowDiffers = true;
                        cells[pos] = new StringCell("CELL DIFFERS:"
                                + currentCell1.toString() + " <> "
                                + currentCell2.toString());
                    }
                    pos++;
                }
                RowKey oldRowKey = currentRow.getKey();
                RowKey rowId = oldRowKey;
                rows.add(new DefaultRow(rowId, cells));
            }
            Object[] tmp = rows.toArray();
            DataRow[] dataRows = new DataRow[tmp.length];
            for (int i = 0; i < tmp.length; i++) {
                dataRows[i] = (DataRow)tmp[i];
            }

            m_DiffTable = new DefaultTable(dataRows, m_dataTable1
                    .getDataTableSpec());
        }
        return m_DiffTable;
    }

}
