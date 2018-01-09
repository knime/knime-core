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
package org.knime.base.data.filter.column;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;

import junit.framework.TestCase;

/**
 * JUnit test class for the filter column table which test column indices,
 * names, types, and its exceptions.
 *
 * @author Thomas Gabriel, University of Konstanz
 *
 * @see FilterColumnTable
 */
public final class FilterColumnTableTest extends TestCase {

    private static final int ROWS = 42;

    /**
     * Test table class used for column filtering.
     */
    private final class MyTestTable implements DataTable {
        private final DataTableSpec m_spec = new DataTableSpec(new String[] {
                "Col_A", "Col_B", "Col_C", "Col_D", "Col_E"}, new DataType[] {
                DoubleCell.TYPE, StringCell.TYPE,
                IntCell.TYPE, DoubleCell.TYPE,
                StringCell.TYPE});

        /**
         * {@inheritDoc}
         */
        public DataTableSpec getDataTableSpec() {
            return m_spec;
        }

        /**
         * {@inheritDoc}
         */
        public RowIterator iterator() {
            return new MyTestRowIterator(m_spec.getNumColumns());
        }

    } // MyTestTable

    /**
     * Iternal row iterator which holds an array of <b>42 </b> rows along with a
     * number of column retrieved from the <code>DataTableSpec</code>.
     */
    private final class MyTestRowIterator extends RowIterator {
        private final DataRow[] m_rows = new DataRow[ROWS];
        private int m_index;

        /**
         * Creates a new iterator with the given number of columns. The row key
         * is formed as <i>row_ </i> plus row index. Each value is a string such
         * as <i>["row","column"] </i>.
         *
         * @param columns The number of columns to generate.
         */
        MyTestRowIterator(final int columns) {
            for (int i = 0; i < m_rows.length; i++) {
                String[] cells = new String[columns];
                for (int c = 0; c < columns; c++) {
                    cells[c] = "[" + i + "," + c + "]";
                }
                m_rows[i] = new DefaultRow("row_" + i, cells);
            }
            m_index = 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            return (m_index != m_rows.length);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataRow next() {
            return m_rows[m_index++];
        }
    }

    /*
     * Keep table for filtering.
     */
    private DataTable m_table;

    /**
     * Init internal members.
     *
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // init test table
        m_table = new MyTestTable();
    }

    /**
     * Destroy internal members.
     *
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        m_table = null;
    }

    /*
     * Invoked on each testXXX() method to test all rows and cells on equality
     * by iterating through the entire table, that is, the filter as well as the
     * original data table. @param The filter table to test equality on.
     */
    private void tableTest(final FilterColumnTable f) {
        final int[] columns = f.getColumnIndices();
        RowIterator fIt = f.iterator();
        RowIterator tIt = m_table.iterator();
        for (; fIt.hasNext() && tIt.hasNext();) {
            DataRow rf = fIt.next();
            DataRow rt = tIt.next();
            // check also if the same rows are compared
            assertTrue(rf.getKey().equals(rt.getKey()));
            for (int i = 0; i < columns.length; i++) {
                // check cell from original with the mapped one
                assertTrue(rf.getCell(i).equals(rt.getCell(columns[i])));
            }
        }
    }

    /**
     * Test all available column indices.
     */
    public void testInConstructorColumnIndices1() {
        FilterColumnTable filter = new FilterColumnTable(m_table, new int[] {0,
                1, 2, 3, 4});
        DataTableSpec fSpec = filter.getDataTableSpec();
        assertTrue(fSpec.getNumColumns() == 5);
        assertTrue(fSpec.getColumnSpec(0).getName().equals("Col_A"));
        assertTrue(fSpec.getColumnSpec(0).getType().equals(DoubleCell.TYPE));
        assertTrue(fSpec.getColumnSpec(1).getName().equals("Col_B"));
        assertTrue(fSpec.getColumnSpec(1).getType().equals(StringCell.TYPE));
        assertTrue(fSpec.getColumnSpec(2).getName().equals("Col_C"));
        assertTrue(fSpec.getColumnSpec(2).getType().equals(IntCell.TYPE));
        assertTrue(fSpec.getColumnSpec(3).getName().equals("Col_D"));
        assertTrue(fSpec.getColumnSpec(3).getType().equals(DoubleCell.TYPE));
        assertTrue(fSpec.getColumnSpec(4).getName().equals("Col_E"));
        assertTrue(fSpec.getColumnSpec(4).getType().equals(StringCell.TYPE));
        // test the filter table
        tableTest(filter);
    }

    /**
     * Test all available column indices reverse ordered.
     */
    public void testInConstructorColumnIndices2() {
        FilterColumnTable filter = new FilterColumnTable(m_table, new int[] {4,
                3, 2, 1, 0});
        DataTableSpec fSpec = filter.getDataTableSpec();
        assertTrue(fSpec.getNumColumns() == 5);
        assertTrue(fSpec.getColumnSpec(4).getName().equals("Col_A"));
        assertTrue(fSpec.getColumnSpec(4).getType().equals(DoubleCell.TYPE));
        assertTrue(fSpec.getColumnSpec(3).getName().equals("Col_B"));
        assertTrue(fSpec.getColumnSpec(3).getType().equals(StringCell.TYPE));
        assertTrue(fSpec.getColumnSpec(2).getName().equals("Col_C"));
        assertTrue(fSpec.getColumnSpec(2).getType().equals(IntCell.TYPE));
        assertTrue(fSpec.getColumnSpec(1).getName().equals("Col_D"));
        assertTrue(fSpec.getColumnSpec(1).getType().equals(DoubleCell.TYPE));
        assertTrue(fSpec.getColumnSpec(0).getName().equals("Col_E"));
        assertTrue(fSpec.getColumnSpec(0).getType().equals(StringCell.TYPE));
        // test the filter table
        tableTest(filter);
    }

    /**
     * Test filtering one column by index.
     */
    public void testInConstructorColumnIndices3() {
        FilterColumnTable filter =
            new FilterColumnTable(m_table, new int[] {3});
        DataTableSpec fSpec = filter.getDataTableSpec();
        assertTrue(fSpec.getNumColumns() == 1);
        assertTrue(fSpec.getColumnSpec(0).getName().equals("Col_D"));
        assertTrue(fSpec.getColumnSpec(0).getType().equals(DoubleCell.TYPE));
        // test the filter table
        tableTest(filter);
    }

    /**
     * Test filtering <code>DoubleCell</code> types which includes
     * <code>IntCell</code> types.
     */
    public void testInConstructorColumnTypes1() {
        FilterColumnTable filter = new FilterColumnTable(m_table,
                DoubleCell.TYPE);
        DataTableSpec fSpec = filter.getDataTableSpec();
        assertTrue(fSpec.getNumColumns() == 3);
        assertTrue(fSpec.getColumnSpec(0).getName().equals("Col_A"));
        assertTrue(fSpec.getColumnSpec(0).getType().equals(DoubleCell.TYPE));
        assertTrue(fSpec.getColumnSpec(1).getName().equals("Col_C"));
        assertTrue(fSpec.getColumnSpec(1).getType().equals(IntCell.TYPE));
        assertTrue(fSpec.getColumnSpec(2).getName().equals("Col_D"));
        assertTrue(fSpec.getColumnSpec(2).getType().equals(DoubleCell.TYPE));
        // test the filter table
        tableTest(filter);
    }

    /**
     * Test filtering <code>StringCell</code> types.
     */
    public void testInConstructorColumnTypes2() {
        FilterColumnTable filter = new FilterColumnTable(m_table,
                StringCell.TYPE);
        DataTableSpec fSpec = filter.getDataTableSpec();
        assertTrue(fSpec.getNumColumns() == 2);
        assertTrue(fSpec.getColumnSpec(0).getName().equals("Col_B"));
        assertTrue(fSpec.getColumnSpec(0).getType().equals(StringCell.TYPE));
        assertTrue(fSpec.getColumnSpec(1).getName().equals("Col_E"));
        assertTrue(fSpec.getColumnSpec(1).getType().equals(StringCell.TYPE));
        // test the filter table
        tableTest(filter);
    }

    /**
     * Test filtering <code>IntCell</code> types.
     */
    public void testInConstructorColumnTypes3() {
        FilterColumnTable filter = new FilterColumnTable(m_table,
                IntCell.TYPE);
        DataTableSpec fSpec = filter.getDataTableSpec();
        assertTrue(fSpec.getNumColumns() == 1);
        assertTrue(fSpec.getColumnSpec(0).getName().equals("Col_C"));
        assertTrue(fSpec.getColumnSpec(0).getType().equals(IntCell.TYPE));
        // test the filter table
        tableTest(filter);
    }

    /**
     * Test all available column names.
     */
    public void testInConstructorColumnNames1() {
        FilterColumnTable filter = new FilterColumnTable(m_table,
                new String[] {"Col_A", "Col_B", "Col_C", "Col_D", "Col_E"});
        DataTableSpec fSpec = filter.getDataTableSpec();
        assertTrue(fSpec.getNumColumns() == 5);
        assertTrue(fSpec.getColumnSpec(0).getName().equals("Col_A"));
        assertTrue(fSpec.getColumnSpec(0).getType().equals(DoubleCell.TYPE));
        assertTrue(fSpec.getColumnSpec(1).getName().equals("Col_B"));
        assertTrue(fSpec.getColumnSpec(1).getType().equals(StringCell.TYPE));
        assertTrue(fSpec.getColumnSpec(2).getName().equals("Col_C"));
        assertTrue(fSpec.getColumnSpec(2).getType().equals(IntCell.TYPE));
        assertTrue(fSpec.getColumnSpec(3).getName().equals("Col_D"));
        assertTrue(fSpec.getColumnSpec(3).getType().equals(DoubleCell.TYPE));
        assertTrue(fSpec.getColumnSpec(4).getName().equals("Col_E"));
        assertTrue(fSpec.getColumnSpec(4).getType().equals(StringCell.TYPE));
        // test the filter table
        tableTest(filter);
    }

    /**
     * Test filtering randomly column names.
     */
    public void testInConstructorColumnNames2() {
        FilterColumnTable filter = new FilterColumnTable(m_table,
                new String[] {"Col_E", "Col_C", "Col_D", "Col_A"});
        DataTableSpec fSpec = filter.getDataTableSpec();
        assertTrue(fSpec.getNumColumns() == 4);
        assertTrue(fSpec.getColumnSpec(3).getName().equals("Col_A"));
        assertTrue(fSpec.getColumnSpec(3).getType().equals(DoubleCell.TYPE));
        assertTrue(fSpec.getColumnSpec(1).getName().equals("Col_C"));
        assertTrue(fSpec.getColumnSpec(1).getType().equals(IntCell.TYPE));
        assertTrue(fSpec.getColumnSpec(2).getName().equals("Col_D"));
        assertTrue(fSpec.getColumnSpec(2).getType().equals(DoubleCell.TYPE));
        assertTrue(fSpec.getColumnSpec(0).getName().equals("Col_E"));
        assertTrue(fSpec.getColumnSpec(0).getType().equals(StringCell.TYPE));
        // test the filter table
        tableTest(filter);
    }

    /**
     * Table is <code>null</code>.
     */
    public void testInConstructorException1() {
        try {
            new FilterColumnTable(null, new int[] {0});
            fail("Exception expected: Table is null.");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /**
     * Array of column indices is <code>null</code>.
     */
    public void testInConstructorException2() {
        try {
            new FilterColumnTable(m_table, (int[])null);
            fail("Exception expected: Column indices are null.");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /**
     * Array of colum indices is empty.
     */
    public void testInConstructorException3() {
        new FilterColumnTable(m_table, new int[] {});
        assertTrue(true);
    }

    /**
     * Column index in array appears twice.
     */
    public void testInConstructorException4() {
        try {
            new FilterColumnTable(m_table, new int[] {0, 1, 0});
            fail("Exception expected: Column index 0 found twice.");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /**
     * Negative column index in array.
     */
    public void testInConstructorException5() {
        try {
            new FilterColumnTable(m_table, new int[] {0, 1, -1});
            fail("Exception expected: Negative column index -1.");
        } catch (ArrayIndexOutOfBoundsException e) {
            assertTrue(true);
        }
    }

    /**
     * Array column index out of range.
     */
    public void testInConstructorException6() {
        try {
            new FilterColumnTable(m_table, new int[] {0, 1, 2, 3, 4, 5});
            fail("Exception expected: Column index out of range 5.");
        } catch (ArrayIndexOutOfBoundsException e) {
            assertTrue(true);
        }
    }

    /**
     * Class type is <code>null</code>.
     */
    public void testInConstructorException7() {
        try {
            new FilterColumnTable(m_table, (DataType)null);
            fail("Exception expected: Class type is null.");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /**
     * Column index in array appears twice.
     */
    public void testInConstructorException8() {
        try {
            new FilterColumnTable(m_table, false, new int[] {0, 0});
            fail("Exception expected: Column index 0 found twice.");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /**
     * Array of column names is empty.
     */
    public void testInConstructorException10() {
        try {
            new FilterColumnTable(m_table, (String[]) null);
            fail("Exception expected: Array of column names is null.");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /**
     * Array of column indices is empty.
     */
    public void testInConstructorException11() {
        new FilterColumnTable(m_table, new String[]{});
        assertTrue(true);
    }

    /**
     * Column name not in org.knime.core.data.
     */
    public void testInConstructorException12() {
        try {
            new FilterColumnTable(m_table, "Bla");
            fail("Exception expected: Column name \"Bla\" not in data.");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /**
     * Column name twice in org.knime.core.data.
     */
    public void testInConstructorException13() {
        try {
            new FilterColumnTable(m_table, new String[] {"Col_A", "Col_A"});
            fail("Exception expected: Column name \"Col_A\" found twice.");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /**
     * Test all available column indices.
     */
    public void testInExConstructorColumnIndices1() {
        FilterColumnTable filter = new FilterColumnTable(m_table, false,
                new int[] {0, 1, 2, 3, 4});
        DataTableSpec fSpec = filter.getDataTableSpec();
        assertTrue(fSpec.getNumColumns() == 0);
        // test the filter table
        tableTest(filter);
    }

    /**
     * Test all available column indices reverse ordered.
     */
    public void testInExConstructorColumnIndices2() {
        FilterColumnTable filter = new FilterColumnTable(m_table, false,
                new int[] {4, 3, 2, 1, 0});
        DataTableSpec fSpec = filter.getDataTableSpec();
        assertTrue(fSpec.getNumColumns() == 0);
        // test the filter table
        tableTest(filter);
    }

    /**
     * Test filtering one column by index.
     */
    public void testInExConstructorColumnIndices3() {
        FilterColumnTable filter = new FilterColumnTable(m_table, false, 3);
        DataTableSpec fSpec = filter.getDataTableSpec();
        assertTrue(fSpec.getNumColumns() == 4);
        assertTrue(fSpec.getColumnSpec(0).getName().equals("Col_A"));
        assertTrue(fSpec.getColumnSpec(0).getType().equals(DoubleCell.TYPE));
        assertTrue(fSpec.getColumnSpec(1).getName().equals("Col_B"));
        assertTrue(fSpec.getColumnSpec(1).getType().equals(StringCell.TYPE));
        assertTrue(fSpec.getColumnSpec(2).getName().equals("Col_C"));
        assertTrue(fSpec.getColumnSpec(2).getType().equals(IntCell.TYPE));
        assertTrue(fSpec.getColumnSpec(3).getName().equals("Col_E"));
        assertTrue(fSpec.getColumnSpec(3).getType().equals(StringCell.TYPE));
        // test the filter table
        tableTest(filter);
    }

    /**
     * Table is <code>null</code>.
     */
    public void testInExConstructorException1() {
        try {
            new FilterColumnTable(null, false, 0);
            fail("Exception expected: Table is null.");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /**
     * Array of column indices is <code>null</code>.
     */
    public void testInExConstructorException2() {
        try {
            new FilterColumnTable(m_table, false, (int[])null);
            fail("Exception expected: Column indices are null.");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /**
     * Array of colum indices is empty.
     */
    public void testInExConstructorException3() {
        new FilterColumnTable(m_table, false, new int[] {});
        assertTrue(true);
    }

    /**
     * Column index in array appears twice.
     */
    public void testInExConstructorException4() {
        try {
            new FilterColumnTable(m_table, false, new int[] {0, 1, 0});
            fail("Exception expected: Column index 0 found twice.");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /**
     * Negative column index in array.
     */
    public void testInExConstructorException5() {
        try {
            new FilterColumnTable(m_table, false, new int[] {0, 1, -1});
            fail("Exception expected: Negative column index -1.");
        } catch (ArrayIndexOutOfBoundsException e) {
            assertTrue(true);
        }
    }

    /**
     * Array column index out of range.
     */
    public void testInExConstructorException6() {
        try {
            new FilterColumnTable(m_table, false,
                    new int[] {0, 1, 2, 3, 4, 5});
            fail("Exception expected: Column index out of range 5.");
        } catch (ArrayIndexOutOfBoundsException e) {
            assertTrue(true);
        }
    }

    /**
     * System entry point.
     *
     * @param args The command line parameters: ignored.
     */
    public static void main(final String[] args) {
        junit.textui.TestRunner.run(FilterColumnTableTest.class);
    }

} // FilterColumnTableTest
