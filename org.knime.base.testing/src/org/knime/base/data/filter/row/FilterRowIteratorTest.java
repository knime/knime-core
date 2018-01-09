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
package org.knime.base.data.filter.row;

import java.util.ArrayList;

import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;

import junit.framework.TestCase;

/**
 * JUnit test class for filter row iterator especialy the
 * <code>FilterRowIterator.next()</code> method.
 * 
 * @see FilterRowIterator
 * 
 * @author Thomas Gabriel, University of Konstanz
 * 
 */
public final class FilterRowIteratorTest extends TestCase {

    /**
     * Internal row iterator which return a list of rows with one cell
     * initialize my an array of double values.
     */
    private final class MyRowIterator extends RowIterator {
        private final double[] m_values;

        private int m_cur;

        /**
         * Creates a new test row iterator.
         * 
         * @param values The double values per row.
         */
        MyRowIterator(final double[] values) {
            m_values = values;
            m_cur = 0;
        }

        /**
         * Returns the next row with one <code>DoubleCell</code>.
         * 
         * @return The next row.
         */
        @Override
        public DataRow next() {
            DataRow row = new DefaultRow(new RowKey("Row_" + m_cur),
                    new double[]{m_values[m_cur]});
            m_cur++;
            return row;
        }

        /**
         * @return <b>true</b> if the end of the double array is reached.
         */
        @Override
        public boolean hasNext() {
            return (m_cur != m_values.length);
        }
    } // MyRowIterator

    /**
     * Internal filter row generator which checks if the given row's double cell
     * value at position 0 is inside the double array.
     */
    private final class MyFilterRowGenerator implements FilterRowGenerator {
        private final double[] m_values;

        /**
         * Creates a filter row iterator.
         * 
         * @param values Allows only these double values.
         */
        MyFilterRowGenerator(final double[] values) {
            assertTrue(values != null);
            m_values = values;
        }

        /**
         * Checks if the given row's <code>DoubleCell</code> at position
         * <code>0</code> is inside the double array.
         * 
         * @param row The row to check with.
         * @return <b>true</b> if the value is acepted.
         */
        public boolean isIn(final DataRow row) {
            double d = ((DoubleValue)row.getCell(0)).getDoubleValue();
            for (int i = 0; i < m_values.length; i++) {
                if (m_values[i] == d) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Test: c and d are equal to the number and order of values.
     */
    public void testFilterRowIterator1() {
        double[] d = new double[]{2.0, 3.0, 1.0, 5.0, 4.0};
        double[] c = d;
        check(d, c);
    }

    /**
     * Test: c is empty.
     */
    public void testFilterRowIterator2() {
        double[] d = new double[]{2.0, 3.0, 1.0, 5.0, 4.0};
        double[] c = new double[0];
        check(d, c);
    }

    /**
     * Test: no d matches c.
     */
    public void testFilterRowIterator3() {
        double[] d = new double[]{2.0, 3.0, 1.0, 5.0, 4.0};
        double[] c = new double[]{0.0, 0.0};
        check(d, c);
    }

    /**
     * Test: c's is reverse order of d.
     */
    public void testFilterRowIterator4() {
        double[] d = new double[]{2.0, 3.0, 1.0, 5.0, 4.0};
        double[] c = new double[]{4.0, 5.0, 1.0, 3.0, 2.0};
        check(d, c);
    }

    /**
     * Test: random selection of c's out of d.
     */
    public void testFilterRowIterator5() {
        double[] d = new double[]{2.0, 3.0, 1.0, 5.0, 4.0};
        double[] c = new double[]{3.0, 5.0};
        check(d, c);
    }

    /**
     * Test: random selection of c's out of d.
     */
    public void testFilterRowIterator6() {
        double[] d = new double[]{2.0, 3.0, 1.0, 5.0, 4.0};
        double[] c = new double[]{3.0, 1.0, 4.0};
        check(d, c);
    }

    /**
     * Test: d is empty.
     */
    public void testFilterRowIterator7() {
        double[] d = new double[0];
        double[] c = new double[]{3.0, 1.0, 4.0};
        check(d, c);
    }

    /**
     * Test: no d matches a value in c.
     */
    public void testFilterRowIterator8() {
        double[] d = new double[5];
        double[] c = new double[]{3.0, 1.0, 4.0};
        check(d, c);
    }

    /**
     * Test: c and d are empty.
     */
    public void testFilterRowIterator9() {
        double[] d = new double[0];
        double[] c = new double[0];
        check(d, c);
    }

    /*
     * Used for all test* methods to check if all only these c's appear in the
     * filer. @param d Initial array of possible double values. @param c An
     * array of double values to check.
     */
    private void check(final double[] d, final double[] c) {
        FilterRowGenerator gen = new MyFilterRowGenerator(c);
        RowIterator it = new FilterRowIterator(new MyRowIterator(d), gen);
        ArrayList<Double> list = new ArrayList<Double>();
        // copy c's in list to remove them easily
        for (int i = 0; i < c.length; i++) {
            list.add(c[i]);
        }
        // check all r's in list of c's
        for (int i = 0; it.hasNext(); i++) {
            DataRow row = it.next();
            double r = ((DoubleValue)row.getCell(0)).getDoubleValue();
            assertTrue(list.remove(r));
        }
        // check for no d's in list
        for (int i = 0; i < d.length; i++) {
            assertFalse(list.remove(d[i]));
        }
    }

    /**
     * System entry point.
     * 
     * @param args The command line parameters: ignored.
     */
    public static void main(final String[] args) {
        junit.textui.TestRunner.run(FilterRowIteratorTest.class);
    }

} // FilterRowIteratorTest
