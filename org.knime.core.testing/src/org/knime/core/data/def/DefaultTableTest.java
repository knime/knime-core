/*
 * --------------------------------------------------------------------- *
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
 * --------------------------------------------------------------------- *
 */
package org.knime.core.data.def;

import java.util.Arrays;
import java.util.Vector;

import junit.framework.TestCase;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.StringValue;
import org.knime.core.node.NodeLogger;

/**
 * JUnit test for all public methods in <code>DefaultTable</code>.
 *
 * @author Bernd Wiswedel, University of Konstanz
 * @see DefaultTable
 */
public class DefaultTableTest extends TestCase {

    /** CASE_1: should do. */
    private static final Object[][] CASE_1_CONTENT = new Object[][] {
            new Object[] {"cell_1_1", "cell_1_2", "cell_1_3", "cell_1_4"},
            new Object[] {"cell_2_1", "cell_2_2", "cell_2_3", "cell_2_4"},
            new Object[] {"cell_3_1", "cell_3_2", "cell_3_3", "cell_3_4"},
            new Object[] {"cell_4_1", "cell_4_2", "cell_4_3", "cell_4_4"},
            new Object[] {"cell_5_1", "cell_5_2", "cell_5_3", "cell_5_4"}};

    private static final String[] CASE_1_ROWHEADER = new String[] {"line_1",
            "line_2", "line_3", "line_4", "line_5"};

    private static final String[] CASE_1_COLHEADER = new String[] {"col_1",
            "col_2", "col_3", "col_4"};

    /** CASE_2: should not do. Missing entry in row 2 */
    private static final Object[][] CASE_2_CONTENT = new Object[][] {
            new Object[] {"cell_1_1", "cell_1_2", "cell_1_3", "cell_1_4"},
            new Object[] {"cell_2_1", "cell_2_2", "cell_2_3", "cell_2_4"},
            new Object[] {"cell_3_1", "cell_3_2", "cell_3_3"},
            new Object[] {"cell_4_1", "cell_4_2", "cell_4_3", "cell_4_4"},
            new Object[] {"cell_5_1", "cell_5_2", "cell_5_3", "cell_5_4"}};

    private static final String[] CASE_2_ROWHEADER = CASE_1_ROWHEADER;

    private static final String[] CASE_2_COLHEADER = CASE_1_COLHEADER;

    /** CASE_3: null values must be accepted (missing values). */
    private static final Object[][] CASE_3_CONTENT = new Object[][] {
            new Object[] {"cell_1_1", "cell_1_2", "cell_1_3", "cell_1_4"},
            new Object[] {"cell_2_1", "cell_2_2", null, "cell_2_4"},
            new Object[] {"cell_3_1", null, "cell_3_3", "cell_3_4"},
            new Object[] {"cell_4_1", "cell_4_2", "cell_4_3", "cell_4_4"},
            new Object[] {"cell_5_1", "cell_5_2", "cell_5_3", "cell_5_4"}};

    private static final String[] CASE_3_ROWHEADER = CASE_1_ROWHEADER;

    private static final String[] CASE_3_COLHEADER = CASE_1_COLHEADER;

    /** CASE_4: should do. */
    private static final Object[][] CASE_4_CONTENT = new Object[][] {
            new Object[] {new Integer(1), "cell_1_2", new Double(0.1),
                    "cell_1_4"},
            new Object[] {new Integer(2), "cell_2_2", new Double(0.2),
                    "cell_2_4"},
            new Object[] {new Integer(3), "cell_3_2", new Double(0.3),
                    "cell_3_4"},
            new Object[] {new Integer(4), "cell_4_2", new Double(0.4),
                    "cell_4_4"},
            new Object[] {new Integer(5), "cell_5_2", new Double(0.5),
                    "cell_5_4"}};

    private static final String[] CASE_4_ROWHEADER = CASE_1_ROWHEADER;

    private static final String[] CASE_4_COLHEADER = CASE_1_COLHEADER;

    /**
     * System entry point.
     *
     * @param args command line parameter: ignored.
     */
    public static void main(final String[] args) {
        junit.textui.TestRunner.run(DefaultTableTest.class);
    }

    /**
     * Tester for constructor.
     *
     * @see DefaultTable#DefaultTable(DataRow[], String[], DataType[])
     */
    public final void testOriginalDefaultTable() {

        // should work fine
        new DefaultTable(new DataRow[0], // empty row elements do not matter
                new String[] {"bla", "42", "99.9"},
                new DataType[] {DoubleCell.TYPE, IntCell.TYPE,
                        DoubleCell.TYPE});

        // fails: data row array is null
        try {
            new DefaultTable(null, new String[] {
                    "bla", "42", "99.9"}, new DataType[] {
                    DoubleCell.TYPE, IntCell.TYPE,
                    DoubleCell.TYPE});
            fail("Expected " + NullPointerException.class + " not thrown");
        } catch (NullPointerException e) {
            NodeLogger.getLogger(getClass()).debug("Got expected exception: " + e.getClass(), e);
        }

        // fails: column name array is null
        try {
            new DefaultTable(new DataRow[0], null, new DataType[] {
                    DoubleCell.TYPE, IntCell.TYPE,
                    DoubleCell.TYPE});
            fail("Expected " + NullPointerException.class + " not thrown");
        } catch (NullPointerException e) {
            NodeLogger.getLogger(getClass()).debug("Got expected exception: " + e.getClass(), e);
        }

        // fails: column type array is null
        try {
            new DefaultTable(new DataRow[0], new String[] {
                    "bla", "42", "99.9"}, null);
            fail("Expected " + NullPointerException.class + " not thrown");
        } catch (NullPointerException e) {
            NodeLogger.getLogger(getClass()).debug("Got expected exception: " + e.getClass(), e);
        }

        // fails: inconsistent number of column names vs. types
        try {
            new DefaultTable(new DataRow[0], // empty row elements does not
                    // matter
                    new String[] {"bla", "42"}, new DataType[] {
                            DoubleCell.TYPE, IntCell.TYPE,
                            DoubleCell.TYPE});
            fail("Expected " + IllegalArgumentException.class + " not thrown");
        } catch (IllegalArgumentException e) {
            NodeLogger.getLogger(getClass()).debug("Got expected exception: " + e.getClass(), e);
        }

        // fails: column name array has null value
        try {
            new DefaultTable(new DataRow[0], // empty row elements does not
                    // matter
                    new String[] {"bla", null, "99.9"}, new DataType[] {
                            DoubleCell.TYPE, IntCell.TYPE,
                            StringCell.TYPE});
            fail("Expected " + NullPointerException.class + " not thrown");
        } catch (NullPointerException e) {
            NodeLogger.getLogger(getClass()).debug("Got expected exception: " + e.getClass(), e);
        }

        // fails: column name array contains redundant objects
        try {
            new DefaultTable(new DataRow[0], // empty row elements do not matter
                    new String[] {"bla", "42", "42"}, new DataType[] {
                            DoubleCell.TYPE, IntCell.TYPE,
                            StringCell.TYPE});
            fail("Expected " + IllegalArgumentException.class + " not thrown");
        } catch (IllegalArgumentException e) {
            NodeLogger.getLogger(getClass()).debug("Got expected exception: " + e.getClass(), e);
        }

        // fails: column type array has null value
        try {
            new DefaultTable(
                    new DataRow[0], // empty row elements do not matter
                    new String[] {"bla", "42", "99.9"},
                    new DataType[] {null, StringCell.TYPE,
                            DoubleCell.TYPE});
            fail("Expected " + NullPointerException.class + " not thrown");
        } catch (NullPointerException e) {
            NodeLogger.getLogger(getClass()).debug("Got expected exception: " + e.getClass(), e);
        }

    } // testOriginalDefaultTable()

    /**
     * Tester for constructor. Certainly, the constructor is most prone to
     * errors since it has many arguments (and most of them are optional.
     */
    public final void testDefaultTable() {
        // Constructs some tables. Those calls must not throw an exception
        createWorkingTables();

        // and some constructors with problems
        /* data contains not 4 but 3 values in row 2, exception expected */
        try {
            new DefaultTable(CASE_2_CONTENT, CASE_2_ROWHEADER,
                    CASE_2_COLHEADER);
            fail("Expected " + IllegalArgumentException.class + " not thrown");
        } catch (IllegalArgumentException e) {
            NodeLogger.getLogger(getClass()).debug("Got expected exception: " + e.getClass(), e);
        }

        /* data contains null values, missing values */
        new DefaultTable(CASE_3_CONTENT, CASE_3_ROWHEADER, CASE_3_COLHEADER);

    } // testDefaultTable()

    /**
     * Creates some new <code>DefaultTable</code> objects that should ...
     * theoretically ... do.
     */
    private void createWorkingTables() {
        // some constructors with (supposedly) no problems
        DefaultTable t1 = new DefaultTable(CASE_1_CONTENT, CASE_1_ROWHEADER,
                CASE_1_COLHEADER);
        DataTableSpec t1Spec = t1.getDataTableSpec();
        assertEquals(CASE_1_COLHEADER.length, t1Spec.getNumColumns());
        // check spec
        for (int c = 0; c < CASE_1_COLHEADER.length; c++) {
            DataColumnSpec currentColumnSpec = t1Spec.getColumnSpec(c);
            String colName = currentColumnSpec.getName().toString();
            DataType type = currentColumnSpec.getType();
            assertEquals(colName, CASE_1_COLHEADER[c]);
            assertTrue(type.isCompatible(StringValue.class));
        }

        // check content
        int r = 0;
        for (RowIterator it = t1.iterator(); it.hasNext(); r++) {
            DataRow row = it.next();
             assertEquals(row.getNumCells(), CASE_1_COLHEADER.length);
            for (int i = 0; i < CASE_1_COLHEADER.length; i++) {
                StringValue cell = (StringValue)row.getCell(i);
                assertEquals(cell.getStringValue(), CASE_1_CONTENT[r][i]);
            }
        }

        // all one-dimensional arrays (meta-info) are optional
        new DefaultTable(CASE_1_CONTENT, null, CASE_1_COLHEADER);
        new DefaultTable(CASE_1_CONTENT, CASE_1_ROWHEADER, null);
        new DefaultTable(CASE_1_CONTENT, CASE_1_ROWHEADER, CASE_1_COLHEADER);
        new DefaultTable(CASE_1_CONTENT, null, null);
        new DefaultTable(CASE_1_CONTENT, null, CASE_1_COLHEADER);
        new DefaultTable(CASE_1_CONTENT, null, null);
    } // createWorkingTables(boolean)

    /**
     * Test if the number of columns passed in the constructor is widely
     * accepted.
     *
     * @see DataTableSpec#getNumColumns()
     */
    public final void testGetNumColumns() {
        // some non-zero column counts
        DefaultTable case1 = new DefaultTable(CASE_1_CONTENT, CASE_1_ROWHEADER,
                CASE_1_COLHEADER);
        DataTableSpec spec1 = case1.getDataTableSpec();

        assertTrue(spec1.getNumColumns() == CASE_1_COLHEADER.length);

        DefaultTable case4 = new DefaultTable(CASE_4_CONTENT, CASE_4_ROWHEADER,
                CASE_4_COLHEADER);
        DataTableSpec spec4 = case4.getDataTableSpec();

        assertTrue(spec4.getNumColumns() == CASE_4_COLHEADER.length);

        DefaultTable nullCase = new DefaultTable(new Object[][] {}, null, null);
        DataTableSpec nullSpec = nullCase.getDataTableSpec();

        assertTrue(nullSpec.getNumColumns() == 0);
    } // testGetNumColumns()

    /**
     * Checker if auto-generated column names are like "Column_"+i and - if
     * passed in constructor - are accepted.
     *
     * @see DataTableSpec#getColumnSpec(int)
     * @see org.knime.core.data.DataColumnSpec#getName()
     */
    public final void testGetColumnName() {
        /* no column names provided - auto generated */
        DefaultTable auto = new DefaultTable(CASE_1_CONTENT, CASE_1_ROWHEADER,
                null);
        DataTableSpec aSpec = auto.getDataTableSpec();
        for (int c = 0; c < CASE_1_COLHEADER.length; c++) {
            String expected = "Column_" + c;
            String found = aSpec.getColumnSpec(c).getName().toString();
            assertEquals(expected, found);
        }

        /* column names given - must match */
        DefaultTable given = new DefaultTable(CASE_1_CONTENT, CASE_1_ROWHEADER,
                CASE_1_COLHEADER);
        DataTableSpec gSpec = given.getDataTableSpec();
        for (int c = 0; c < CASE_1_COLHEADER.length; c++) {
            String expected = CASE_1_COLHEADER[c];
            String found = gSpec.getColumnSpec(c).getName().toString();
            assertEquals(expected, found);
        }

    } // testGetColumnName()

    /**
     * Tests if column types as given in constructor are returned when
     * <code>getColumnType(int)</code> is called. If constructor parameter is
     * <code>null</code> it must return <code>DataCell.class</code> for
     * every column.
     *
     * @see DataColumnSpec#getType()
     */
    public final void testGetColumnType() {
        DefaultTable auto = new DefaultTable(CASE_1_CONTENT, CASE_1_ROWHEADER,
                CASE_1_COLHEADER);
        for (int c = 0; c < CASE_1_COLHEADER.length; c++) {
            DataType t = auto.getDataTableSpec().getColumnSpec(c).getType();
            assertTrue(t.isCompatible(StringValue.class));
        }
    } // testGetColumnType()

    /**
     * Tests the row iterator in the table.
     *
     * @see DefaultTable#iterator()
     */
    public final void testGetRowIterator() {
        /*
         * also for empty table the iterator must not be null (but atEnd()
         * immediately
         */
        DefaultTable empty = new DefaultTable(new Object[][] {},
                new String[] {}, CASE_1_COLHEADER);
        RowIterator emptyIt = empty.iterator();
        assertNotNull(emptyIt);
        assertTrue(!emptyIt.hasNext());

        /*
         * create table from CASE4, get data back from row iterator and compare
         * input and output
         */
        DefaultTable case4 = new DefaultTable(CASE_4_CONTENT, CASE_4_ROWHEADER,
                CASE_4_COLHEADER);
        DataTableSpec spec4 = case4.getDataTableSpec();

        Vector<Object[]> content =
            new Vector<Object[]>(); // contains Object[] (row-wise)
        for (DataRow row : case4) {
            assertTrue(row.getNumCells() == spec4.getNumColumns());
            Object[] rowContent = new Object[row.getNumCells()];
            for (int c = 0; c < row.getNumCells(); c++) {
                rowContent[c] = getObjectFromCell(row.getCell(c));
            }
            content.add(rowContent);
        }
        Object[][] contentAsArray = new Object[content.size()][];
        contentAsArray = content.toArray(contentAsArray);
        // contentAsArray.equals(CASE_4_CONTENT) does not work!!
        assertTrue(contentAsArray.length == CASE_4_CONTENT.length);
        for (int i = 0; i < contentAsArray.length; i++) {
            assertTrue(Arrays.equals(contentAsArray[i], CASE_4_CONTENT[i]));
        }
    } // testGetRowIterator()

    /**
     * Creates from a given DataCell an object. A StringDataCell returns a
     * String, an IntDataCell an Integer, and a DoubelDataCell a Double.
     *
     * @param cell to get content from
     * @return cell's content
     * @throws IllegalArgumentException if cell can't be casted as one of the
     *             mentioned DataCells
     */
    private Object getObjectFromCell(final DataCell cell) {
        if (cell instanceof IntValue) {
            return new Integer(((IntValue)cell).getIntValue());
        }
        if (cell instanceof DoubleValue) {
            return new Double(((DoubleValue)cell).getDoubleValue());
        }
        if (cell instanceof StringValue) {
            return ((StringValue)cell).getStringValue();
        }
        throw new IllegalArgumentException("Can't handle \""
                + cell.getClass().getName() + "\".");
    } // getObjectFromCell(DataCell)

}
