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
 */
package org.knime.base.data.append.row;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DefaultTable;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;

import junit.framework.TestCase;

/**
 * Test class for <code>AppendedRowsTable</code>.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public class AppendedRowsTableTest extends TestCase {

    private static final DataType[] DATA_TYPES = new DataType[] {
            DoubleCell.TYPE, IntCell.TYPE, StringCell.TYPE};

    private static final String[] DATA_H = new String[] {
            "double_col", "int_col", "string_col"};

    private static final DataRow[] DATA = new DataRow[] {
            new DefaultRow("row_1", new DataCell[] {
                    new DoubleCell(1.0), new IntCell(2),
                    new StringCell("three")}),
            new DefaultRow("row_2", new DataCell[] {
                    new DoubleCell(4.0), new IntCell(5),
                    new StringCell("six")}),
            new DefaultRow("row_3", new DataCell[] {
                    new DoubleCell(7.0), new IntCell(8),
                    new StringCell("nine")}),
            new DefaultRow("row_4", new DataCell[] {
                    new DoubleCell(10.0), new IntCell(11),
                    new StringCell("twelve")})};

    private static final DataType[] DATA_MISS_LAST_TYPES = new DataType[] {
            DoubleCell.TYPE, IntCell.TYPE};

    private static final String[] DATA_MISS_LAST_H = new String[] {
            "double_col", "int_col"};

    private static final DataRow[] DATA_MISS_LAST = new DataRow[] {
            new DefaultRow("row_1", new DataCell[] {
                    new DoubleCell(1.0), new IntCell(2)}),
            new DefaultRow("row_2", new DataCell[] {
                    new DoubleCell(4.0), new IntCell(5)}),
            new DefaultRow("row_3", new DataCell[] {
                    new DoubleCell(7.0), new IntCell(8)}),
            new DefaultRow("row_4", new DataCell[] {
                    new DoubleCell(10.0), new IntCell(11)})};

    private static final DataRow[] DATA_2 = new DataRow[] {
            new DefaultRow("row_13", new DataCell[] {
                    new DoubleCell(13.0), new IntCell(14),
                    new StringCell("fifteen")}),
            new DefaultRow("row_16", new DataCell[] {
                    new DoubleCell(16.0), new IntCell(17),
                    new StringCell("eighteen")}),
            new DefaultRow("row_19", new DataCell[] {
                    new DoubleCell(19.0), new IntCell(20),
                    new StringCell("twentyone")})};

    private static final DataType[] DATA_SHUFFLE_TYPES = new DataType[] {
            IntCell.TYPE, DoubleCell.TYPE, StringCell.TYPE};

    private static final String[] DATA_SHUFFLE_H = new String[] {
            "int_col", "double_col", "string_col"};

    private static final DataRow[] DATA_SHUFFLE = new DataRow[] {
            new DefaultRow("row_5", new DataCell[] {
                    new IntCell(2), new DoubleCell(1.0),
                    new StringCell("three")}),
            new DefaultRow("row_6", new DataCell[] {
                    new IntCell(5), new DoubleCell(4.0),
                    new StringCell("six")}),
            new DefaultRow("row_7", new DataCell[] {
                    new IntCell(8), new DoubleCell(7.0),
                    new StringCell("nine")}),
            new DefaultRow("row_8", new DataCell[] {
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

        assertTrue(ap.getDataTableSpec().equalStructure(
                firstTable.getDataTableSpec()));
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
