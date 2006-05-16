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
 */
package de.unikn.knime.base.data.append.row;

import junit.framework.TestCase;
import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataRow;
import de.unikn.knime.core.data.DataTable;
import de.unikn.knime.core.data.DataType;
import de.unikn.knime.core.data.RowIterator;
import de.unikn.knime.core.data.def.DoubleCell;
import de.unikn.knime.core.data.def.IntCell;
import de.unikn.knime.core.data.def.DefaultRow;
import de.unikn.knime.core.data.def.StringCell;
import de.unikn.knime.core.data.def.DefaultTable;

/**
 * Test class for <code>AppendedRowsTable</code>.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class AppendedRowsTableTest extends TestCase {

    private static final DataType[] DATA_TYPES = new DataType[] {
            DoubleCell.TYPE, IntCell.TYPE, StringCell.TYPE};

    private static final DataCell[] DATA_H = new StringCell[] {
            new StringCell("double_col"),
            new StringCell("int_col"),
            new StringCell("string_col")};

    private static final DataRow[] DATA = new DataRow[] {
            new DefaultRow(new StringCell("row_1"), new DataCell[] {
                    new DoubleCell(1.0), new IntCell(2),
                    new StringCell("three")}),
            new DefaultRow(new StringCell("row_2"), new DataCell[] {
                    new DoubleCell(4.0), new IntCell(5),
                    new StringCell("six")}),
            new DefaultRow(new StringCell("row_3"), new DataCell[] {
                    new DoubleCell(7.0), new IntCell(8),
                    new StringCell("nine")}),
            new DefaultRow(new StringCell("row_4"), new DataCell[] {
                    new DoubleCell(10.0), new IntCell(11),
                    new StringCell("twelve")})};

    private static final DataType[] DATA_MISS_LAST_TYPES = new DataType[] {
            DoubleCell.TYPE, IntCell.TYPE};

    private static final DataCell[] DATA_MISS_LAST_H = new StringCell[] {
            new StringCell("double_col"),
            new StringCell("int_col")};

    private static final DataRow[] DATA_MISS_LAST = new DataRow[] {
            new DefaultRow(new StringCell("row_1"), new DataCell[] {
                    new DoubleCell(1.0), new IntCell(2)}),
            new DefaultRow(new StringCell("row_2"), new DataCell[] {
                    new DoubleCell(4.0), new IntCell(5)}),
            new DefaultRow(new StringCell("row_3"), new DataCell[] {
                    new DoubleCell(7.0), new IntCell(8)}),
            new DefaultRow(new StringCell("row_4"), new DataCell[] {
                    new DoubleCell(10.0), new IntCell(11)})};

    private static final DataRow[] DATA_2 = new DataRow[] {
            new DefaultRow(new StringCell("row_13"), new DataCell[] {
                    new DoubleCell(13.0), new IntCell(14),
                    new StringCell("fifteen")}),
            new DefaultRow(new StringCell("row_16"), new DataCell[] {
                    new DoubleCell(16.0), new IntCell(17),
                    new StringCell("eighteen")}),
            new DefaultRow(new StringCell("row_19"), new DataCell[] {
                    new DoubleCell(19.0), new IntCell(20),
                    new StringCell("twentyone")})};

    private static final DataType[] DATA_SHUFFLE_TYPES = new DataType[] {
            IntCell.TYPE, DoubleCell.TYPE, StringCell.TYPE};

    private static final DataCell[] DATA_SHUFFLE_H = new StringCell[] {
            new StringCell("int_col"),
            new StringCell("double_col"),
            new StringCell("string_col")};

    private static final DataRow[] DATA_SHUFFLE = new DataRow[] {
            new DefaultRow(new StringCell("row_5"), new DataCell[] {
                    new IntCell(2), new DoubleCell(1.0),
                    new StringCell("three")}),
            new DefaultRow(new StringCell("row_6"), new DataCell[] {
                    new IntCell(5), new DoubleCell(4.0),
                    new StringCell("six")}),
            new DefaultRow(new StringCell("row_7"), new DataCell[] {
                    new IntCell(8), new DoubleCell(7.0),
                    new StringCell("nine")}),
            new DefaultRow(new StringCell("row_8"), new DataCell[] {
                    new IntCell(11), new DoubleCell(10.0),
                    new StringCell("twelve")})};

    /**
     * Class under test for void AppendedRowsTable(DataTable[]).
     */
    public void testAppendedRowsTableDataTableArray() {
        DataTable firstTable = new DefaultTable(DATA, DATA_H, DATA_TYPES);

        DataTable firstTableMissing = new DefaultTable(DATA_MISS_LAST,
                DATA_MISS_LAST_H, DATA_MISS_LAST_TYPES);

        DataTable firstTableShuffle = new DefaultTable(DATA_SHUFFLE,
                DATA_SHUFFLE_H, DATA_SHUFFLE_TYPES);

        DataTable secondTable = new DefaultTable(DATA_2, DATA_H, DATA_TYPES);
        new AppendedRowsTable(firstTable, secondTable);
        try {
            new AppendedRowsTable(firstTable, null);
            fail();
        } catch (NullPointerException npe) {
            // do nothing
        }
        try {
            new AppendedRowsTable((DataTable)null, secondTable);
            fail();
        } catch (NullPointerException npe) {
            // do nothing
        }
        new AppendedRowsTable(new DataTable[] {firstTableMissing, secondTable});
        new AppendedRowsTable(new DataTable[] {firstTable, firstTableShuffle});
    }

    /** Test method for getDataTableSpec(). */
    public void testGetDataTableSpec() {
        DataTable firstTable = new DefaultTable(DATA, DATA_H, DATA_TYPES);

        DataTable firstTableShuffle = new DefaultTable(DATA_SHUFFLE,
                DATA_SHUFFLE_H, DATA_SHUFFLE_TYPES);

        DataTable ap = new AppendedRowsTable(new DataTable[] {firstTable,
                firstTableShuffle});

        assertEquals(ap.getDataTableSpec(), firstTable.getDataTableSpec());
    }

    /** Test method for getRowIterator(). */
    public void testGetRowIterator() {
        DataTable firstTable = new DefaultTable(DATA, DATA_H, DATA_TYPES);

        DataTable firstTableShuffle = new DefaultTable(DATA_SHUFFLE,
                DATA_SHUFFLE_H, DATA_SHUFFLE_TYPES);

        DataTable ap = new AppendedRowsTable(new DataTable[] {firstTable,
                firstTableShuffle});
        RowIterator apIt = ap.iterator();
        for (RowIterator fiIt = firstTable.iterator(); fiIt.hasNext();) {
            assertTrue(apIt.hasNext());
            DataRow apRow = apIt.next();
            DataRow fiRow = fiIt.next();
            assertEquals(apRow.getKey(), fiRow.getKey());
            assertEquals(apRow.getCell(0), fiRow.getCell(0));
            assertEquals(apRow.getCell(1), fiRow.getCell(1));
            assertEquals(apRow.getCell(2), fiRow.getCell(2));
        }
        for (RowIterator seIt = firstTableShuffle.iterator(); seIt.hasNext();) {
            assertTrue(apIt.hasNext());
            DataRow apRow = apIt.next();
            DataRow seRow = seIt.next();
            assertEquals(apRow.getKey(), seRow.getKey());
            // first and second are swapped!
            assertEquals(apRow.getCell(0), seRow.getCell(1));
            assertEquals(apRow.getCell(1), seRow.getCell(0));
            assertEquals(apRow.getCell(2), seRow.getCell(2));
        }
        assertFalse(apIt.hasNext());

        DataTable duplicateTable = new AppendedRowsTable(new DataTable[] {
                firstTable, firstTable});
        RowIterator dupIt = duplicateTable.iterator();
        for (RowIterator fiIt = firstTable.iterator(); fiIt.hasNext();) {
            dupIt.next();
            fiIt.next();
        }
        assertFalse(dupIt.hasNext()); // it should not return duplicate keys.
    }

}
