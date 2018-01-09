/*
 * -------------------------------------------------------------------
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
