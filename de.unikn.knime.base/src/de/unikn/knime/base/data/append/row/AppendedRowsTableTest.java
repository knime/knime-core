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
import de.unikn.knime.core.data.DoubleType;
import de.unikn.knime.core.data.IntType;
import de.unikn.knime.core.data.RowIterator;
import de.unikn.knime.core.data.StringType;
import de.unikn.knime.core.data.def.DefaultDoubleCell;
import de.unikn.knime.core.data.def.DefaultIntCell;
import de.unikn.knime.core.data.def.DefaultRow;
import de.unikn.knime.core.data.def.DefaultStringCell;
import de.unikn.knime.core.data.def.DefaultTable;

/**
 * Test class for <code>AppendedRowsTable</code>.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class AppendedRowsTableTest extends TestCase {

    private static final DataType[] DATA_TYPES = new DataType[] {
            DoubleType.DOUBLE_TYPE, IntType.INT_TYPE, StringType.STRING_TYPE};

    private static final DataCell[] DATA_H = new DefaultStringCell[] {
            new DefaultStringCell("double_col"),
            new DefaultStringCell("int_col"),
            new DefaultStringCell("string_col")};

    private static final DataRow[] DATA = new DataRow[] {
            new DefaultRow(new DefaultStringCell("row_1"), new DataCell[] {
                    new DefaultDoubleCell(1.0), new DefaultIntCell(2),
                    new DefaultStringCell("three")}),
            new DefaultRow(new DefaultStringCell("row_2"), new DataCell[] {
                    new DefaultDoubleCell(4.0), new DefaultIntCell(5),
                    new DefaultStringCell("six")}),
            new DefaultRow(new DefaultStringCell("row_3"), new DataCell[] {
                    new DefaultDoubleCell(7.0), new DefaultIntCell(8),
                    new DefaultStringCell("nine")}),
            new DefaultRow(new DefaultStringCell("row_4"), new DataCell[] {
                    new DefaultDoubleCell(10.0), new DefaultIntCell(11),
                    new DefaultStringCell("twelve")})};

    private static final DataType[] DATA_MISS_LAST_TYPES = new DataType[] {
            DoubleType.DOUBLE_TYPE, IntType.INT_TYPE};

    private static final DataCell[] DATA_MISS_LAST_H = new DefaultStringCell[] {
            new DefaultStringCell("double_col"),
            new DefaultStringCell("int_col")};

    private static final DataRow[] DATA_MISS_LAST = new DataRow[] {
            new DefaultRow(new DefaultStringCell("row_1"), new DataCell[] {
                    new DefaultDoubleCell(1.0), new DefaultIntCell(2)}),
            new DefaultRow(new DefaultStringCell("row_2"), new DataCell[] {
                    new DefaultDoubleCell(4.0), new DefaultIntCell(5)}),
            new DefaultRow(new DefaultStringCell("row_3"), new DataCell[] {
                    new DefaultDoubleCell(7.0), new DefaultIntCell(8)}),
            new DefaultRow(new DefaultStringCell("row_4"), new DataCell[] {
                    new DefaultDoubleCell(10.0), new DefaultIntCell(11)})};

    private static final DataRow[] DATA_2 = new DataRow[] {
            new DefaultRow(new DefaultStringCell("row_13"), new DataCell[] {
                    new DefaultDoubleCell(13.0), new DefaultIntCell(14),
                    new DefaultStringCell("fifteen")}),
            new DefaultRow(new DefaultStringCell("row_16"), new DataCell[] {
                    new DefaultDoubleCell(16.0), new DefaultIntCell(17),
                    new DefaultStringCell("eighteen")}),
            new DefaultRow(new DefaultStringCell("row_19"), new DataCell[] {
                    new DefaultDoubleCell(19.0), new DefaultIntCell(20),
                    new DefaultStringCell("twentyone")})};

    private static final DataType[] DATA_SHUFFLE_TYPES = new DataType[] {
            IntType.INT_TYPE, DoubleType.DOUBLE_TYPE, StringType.STRING_TYPE};

    private static final DataCell[] DATA_SHUFFLE_H = new DefaultStringCell[] {
            new DefaultStringCell("int_col"),
            new DefaultStringCell("double_col"),
            new DefaultStringCell("string_col")};

    private static final DataRow[] DATA_SHUFFLE = new DataRow[] {
            new DefaultRow(new DefaultStringCell("row_5"), new DataCell[] {
                    new DefaultIntCell(2), new DefaultDoubleCell(1.0),
                    new DefaultStringCell("three")}),
            new DefaultRow(new DefaultStringCell("row_6"), new DataCell[] {
                    new DefaultIntCell(5), new DefaultDoubleCell(4.0),
                    new DefaultStringCell("six")}),
            new DefaultRow(new DefaultStringCell("row_7"), new DataCell[] {
                    new DefaultIntCell(8), new DefaultDoubleCell(7.0),
                    new DefaultStringCell("nine")}),
            new DefaultRow(new DefaultStringCell("row_8"), new DataCell[] {
                    new DefaultIntCell(11), new DefaultDoubleCell(10.0),
                    new DefaultStringCell("twelve")})};

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
