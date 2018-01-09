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
package org.knime.base.data.join;

import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;

import junit.framework.TestCase;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DefaultTable;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.NodeLogger;

/**
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public class JoinedTableTest extends TestCase {
    private static final DataColumnSpec[] COLS;

    private static final Random RAND = new Random();

    private static final DataCell[] MISSINGS;

    static {
        COLS = new DataColumnSpec[6];
        String name = "Col 1";
        DataType type = StringCell.TYPE;
        COLS[0] = new DataColumnSpecCreator(name, type).createSpec();
        name = "Col 2";
        type = IntCell.TYPE;
        COLS[1] = new DataColumnSpecCreator(name, type).createSpec();
        name = "Col 3";
        type = DoubleCell.TYPE;
        COLS[2] = new DataColumnSpecCreator(name, type).createSpec();
        name = "Col 4";
        type = StringCell.TYPE;
        COLS[3] = new DataColumnSpecCreator(name, type).createSpec();
        name = "Col 5";
        type = IntCell.TYPE;
        COLS[4] = new DataColumnSpecCreator(name, type).createSpec();
        name = "Col 6";
        type = DoubleCell.TYPE;
        COLS[5] = new DataColumnSpecCreator(name, type).createSpec();
        MISSINGS = new DataCell[3];
        MISSINGS[0] = DataType.getMissingCell();
        MISSINGS[1] = DataType.getMissingCell();
        MISSINGS[2] = DataType.getMissingCell();
    }

    /**
     * Main method for single test run .
     *
     * @param args Ignored.
     */
    public static void main(final String[] args) {
        junit.textui.TestRunner.run(JoinedTableTest.class);
    }

    /** Test for constructor. */
    public final void testMergedTable() {
        DataColumnSpec[] leftCols = new DataColumnSpec[3];
        DataColumnSpec[] rightCols = new DataColumnSpec[3];
        System.arraycopy(COLS, 0, leftCols, 0, 3);
        System.arraycopy(COLS, 3, rightCols, 0, 3);
        DataTable leftTable = new DefaultTable(new DataRow[0],
                new DataTableSpec(leftCols));
        DataTable rightTable = new DefaultTable(new DataRow[0],
                new DataTableSpec(rightCols));
        new JoinedTable(leftTable, rightTable);
        try {
            new JoinedTable(null, rightTable);
            fail();
        } catch (NullPointerException ne) {
            NodeLogger.getLogger(JoinedTableTest.class).debug("Got expected exception: " + ne.getClass(), ne);
        }
        try {
            new JoinedTable(leftTable, null);
            fail();
        } catch (NullPointerException ne) {
            NodeLogger.getLogger(JoinedTableTest.class).debug("Got expected exception: " + ne.getClass(), ne);
        }
        try {
            new JoinedTable(leftTable, leftTable);
            fail();
        } catch (IllegalArgumentException iae) {
            NodeLogger.getLogger(JoinedTableTest.class).debug("Got expected exception: " + iae.getClass(), iae);
        }

        try {
            new JoinedTable(leftTable, leftTable);
            fail();
        } catch (IllegalArgumentException iae) {
            NodeLogger.getLogger(JoinedTableTest.class).debug("Got expected exception: " + iae.getClass(), iae);
        }
        rightCols[1] = leftCols[2];
        rightTable = new DefaultTable(new DataRow[0], new DataTableSpec(
                rightCols));
        try {
            new JoinedTable(leftTable, rightTable);
            fail();
        } catch (IllegalArgumentException iae) {
            NodeLogger.getLogger(JoinedTableTest.class).debug("Got expected exception: " + iae.getClass(), iae);
        }
    }

    /** Test for getDataTableSpec(). */
    public final void testGetDataTableSpec() {
        DataColumnSpec[] leftCols = new DataColumnSpec[3];
        DataColumnSpec[] rightCols = new DataColumnSpec[3];
        System.arraycopy(COLS, 0, leftCols, 0, 3);
        System.arraycopy(COLS, 3, rightCols, 0, 3);
        DataTable leftTable = new DefaultTable(new DataRow[0],
                new DataTableSpec(leftCols));
        DataTable rightTable = new DefaultTable(new DataRow[0],
                new DataTableSpec(rightCols));
        JoinedTable t = new JoinedTable(leftTable, rightTable);
        DataTableSpec s = t.getDataTableSpec();
        assert (!t.iterator().hasNext());
        assertEquals(s.getNumColumns(), COLS.length);
        for (int i = 0; i < COLS.length; i++) {
            assertEquals(s.getColumnSpec(i), COLS[i]);
        }
    }

    /**
     * Test for RowIterator. That is the one that is most likely to fail ...
     */
    public final void testGetRowIterator() {
        DataColumnSpec[] leftCols = new DataColumnSpec[3];
        DataColumnSpec[] rightCols = new DataColumnSpec[3];
        System.arraycopy(COLS, 0, leftCols, 0, 3);
        System.arraycopy(COLS, 3, rightCols, 0, 3);
        final int allLength = 100;
        DataRow[] leftRows = new DataRow[allLength];
        DataRow[] rightRows = new DataRow[allLength];
        Hashtable<RowKey, DataRow> rightHash = new Hashtable<RowKey, DataRow>();
        for (int i = 0; i < allLength; i++) {
            String id = "Id_" + i;
            leftRows[i] = getRandomRow(id);
            rightRows[i] = getRandomRow(id);
            rightHash.put(rightRows[i].getKey(), rightRows[i]);
        }
        final DataTable leftTable = new DefaultTable(leftRows,
                new DataTableSpec(leftCols));
        final DataTable rightTable = new DefaultTable(rightRows,
                new DataTableSpec(rightCols));
        JoinedTable t = new JoinedTable(leftTable, rightTable);

        // everything comes in order, shouldn't make a problem.
        int count = checkForEquality(t, leftRows, rightHash);
        assertEquals(count, allLength);

        // shuffle the right table
        DataRow[] shuffledRightRows = new DataRow[allLength];
        System.arraycopy(rightRows, 0, shuffledRightRows, 0, allLength);
        List<DataRow> c = Arrays.asList(shuffledRightRows);
        Collections.shuffle(c, RAND);
        shuffledRightRows = c.toArray(shuffledRightRows);
        DataTable shuffleRightTable = new DefaultTable(shuffledRightRows,
                new DataTableSpec(rightCols));
        t = new JoinedTable(leftTable, shuffleRightTable);

        count = checkForEquality(t, leftRows, rightHash);
        assertEquals(count, allLength);

        // wow, it survived that.
        // let's delete some of the rows in the right table.
        // supposedly, the table will fill it with missing values ...
        final int newLength = (int)(0.8 * allLength);
        DataRow[] shuffledAndTruncRightRows = new DataRow[newLength];
        System.arraycopy(shuffledRightRows, 0, shuffledAndTruncRightRows, 0,
                newLength);
        Hashtable<RowKey, DataRow> newHash = new Hashtable<RowKey, DataRow>(
                rightHash);
        for (int i = newLength; i < allLength; i++) {
            RowKey removeMe = shuffledRightRows[i].getKey();
            newHash.remove(removeMe);
        }
        DataTable shuffleAndTruncRightTable = new DefaultTable(
                shuffledAndTruncRightRows, new DataTableSpec(rightCols));
        t = new JoinedTable(leftTable, shuffleAndTruncRightTable);

        count = checkForEquality(t, leftRows, newHash);
        assertEquals(count, allLength);

        // now cut shorten the left table
        DataRow[] truncLeftRows = new DataRow[newLength];
        System.arraycopy(leftRows, 0, truncLeftRows, 0, newLength);
        DataTable truncLeftTable = new DefaultTable(truncLeftRows,
                new DataTableSpec(leftCols));
        t = new JoinedTable(truncLeftTable, rightTable);
        count = checkForEquality(t, truncLeftRows, rightHash);
        assertEquals(count, allLength);

        // tables share no rows at all
        final int halfLength = allLength / 2;
        DataRow[] halfLeftRows = new DataRow[halfLength];
        DataRow[] halfRightRows = new DataRow[halfLength];
        System.arraycopy(leftRows, 0, halfLeftRows, 0, halfLength);
        System.arraycopy(rightRows, halfLength, halfRightRows, 0, halfLength);
        Hashtable<RowKey, DataRow> halfRightHash = new Hashtable<RowKey, DataRow>();
        for (int i = 0; i < halfLength; i++) {
            DataRow current = halfRightRows[i];
            halfRightHash.put(current.getKey(), current);
        }
        DataTable halfLeftTable = new DefaultTable(halfLeftRows,
                new DataTableSpec(leftCols));
        DataTable halfRightTable = new DefaultTable(halfRightRows,
                new DataTableSpec(rightCols));
        t = new JoinedTable(halfLeftTable, halfRightTable);
        count = checkForEquality(t, halfLeftRows, halfRightHash);
        assertEquals(count, 2 * halfLength);

        // left table is empty
        DataTable emptyLeftTable = new DefaultTable(new DataRow[0],
                new DataTableSpec(leftCols));
        t = new JoinedTable(emptyLeftTable, halfRightTable);
        count = checkForEquality(t, new DataRow[0], halfRightHash);
        assertEquals(count, halfLength);

        // right table is empty
        DataTable emptyRightTable = new DefaultTable(new DataRow[0],
                new DataTableSpec(rightCols));
        t = new JoinedTable(halfLeftTable, emptyRightTable);
        count = checkForEquality(t, halfLeftRows,
                new Hashtable<RowKey, DataRow>());
        assertEquals(count, halfLength);
    }

    private static final DataRow getRandomRow(final String id) {
        DataCell[] cells = new DataCell[3];
        cells[0] = new StringCell(id + "-" + RAND.nextInt(100));
        cells[1] = new IntCell(RAND.nextInt());
        cells[2] = new DoubleCell(RAND.nextDouble());
        return new DefaultRow(id, cells);
    }

    private static int checkForEquality(final DataTable merge,
            final DataRow[] left, final Hashtable<RowKey, DataRow> rightHash) {
        int rowC = 0;
        for (RowIterator it = merge.iterator(); it.hasNext(); rowC++) {
            DataRow next = it.next();
            RowKey nextKey = next.getKey();
            DataRow rightNext = rightHash.get(nextKey);
            DataRow leftNext = (rowC < left.length ? left[rowC] : null);
            // at most one can be missing
            if (rightNext == null) {
                rightNext = new DefaultRow(nextKey, MISSINGS);
            } else if (leftNext == null) {
                leftNext = new DefaultRow(nextKey, MISSINGS);
            }
            assertEquals(next.getNumCells(), COLS.length);
            assertEquals(next.getKey().getString(),
                        leftNext.getKey().getString());
            for (int i = 0; i < COLS.length; i++) {
                DataCell cell = next.getCell(i);
                DataCell compareCell;
                if (i < 3) {
                    compareCell = leftNext.getCell(i);
                } else {
                    compareCell = rightNext.getCell(i - 3);
                }
                assertEquals(cell, compareCell);
            }
        }
        return rowC;
    }
}
