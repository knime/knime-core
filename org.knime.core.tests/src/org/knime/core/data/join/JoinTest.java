/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Jun 12, 2020 (carlwitt): created
 */
package org.knime.core.data.join;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.function.BiConsumer;

import org.junit.experimental.theories.DataPoints;
import org.knime.core.data.DataRow;
import org.knime.core.data.container.BlobSupportDataRow;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.join.JoinSpecification.OutputRowOrder;
import org.knime.core.node.BufferedDataTable;

import gnu.trove.set.hash.TCustomHashSet;
import gnu.trove.strategy.HashingStrategy;

/**
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public abstract class JoinTest {

    /**
     * This enum holds all ways of joining the two tables.
     */
    public enum JoinMode {
        INNER("Inner Join", true, false, false),
        LEFT_OUTER("Left Outer Join", true, true, false),
        RIGHT_OUTER("Right Outer Join", true, false, true),
        FULL_OUTER("Full Outer Join", true, true, true),
        LEFT_ANTI("Left Antijoin", false, true, false),
        RIGHT_ANTI("Right Antijoin", false, false, true),
        FULL_ANTI("Full Antijoin", false, true, true),
        EMPTY("Empty Join", false, false, false);

        public final boolean m_retainLeftUnmatched;
        public final boolean m_retainRightUnmatched;
        public final boolean m_retainMatches;
        private final String m_readable;

        private JoinMode(final String readable, final boolean matches, final boolean left, final boolean right) {
            m_retainMatches = matches;
            m_retainLeftUnmatched = left;
            m_retainRightUnmatched = right;
            m_readable = readable;
        }

        @Override
        public String toString() {
            return m_readable;
        }

    }

    public enum Execution {
        IN_MEMORY(0), PARTIAL_IN_MEMORY(1), ON_DISK(Integer.MAX_VALUE);
        final int m_desiredPartitionsOnDisk;
        Execution(final int desiredNumPartitionsOnDisk){
            m_desiredPartitionsOnDisk = desiredNumPartitionsOnDisk;
        }
    }

    public enum OutputOrder {
        ARBITRARY(OutputRowOrder.ARBITRARY, JoinTest::assertSetEquality),
        PROBE_HASH(OutputRowOrder.DETERMINISTIC, JoinTest::assertOrderedEquality),
        LEGACY(OutputRowOrder.LEFT_RIGHT, JoinTest::assertOrderedEquality);
        public OutputRowOrder m_rowOrder;
        public BiConsumer<BufferedDataTable, DataRow[]> m_validator;
        /**
         * @param order
         * @param conform
         */
        private OutputOrder(final OutputRowOrder order, final BiConsumer<BufferedDataTable, DataRow[]> conform) {
            this.m_rowOrder = order;
            this.m_validator = conform;
        }

    }

    @DataPoints
    public static final OutputOrder[] outputOrder = OutputOrder.values();

    @DataPoints
    public static final Execution[] execution = Execution.values();

    @DataPoints
    public static final JoinMode[] joinModes = JoinMode.values();

    /**
     * Test that the table contains the given rows in the given order.
     */
    static void assertOrderedEquality(final BufferedDataTable results, final DataRow... expectedRows) {

        assertEquals(expectedRows.length, results.size());

        // output table contains one match
        try (CloseableRowIterator it = results.iterator()) {
            int i = 0;
            while (it.hasNext()) {
                DataRow expected = expectedRows[i++];
                DataRow actual = it.next();
                assertTrue(String.format("expected %s, got %s", JoinTestInput.dataRowToString(expected), JoinTestInput.dataRowToString(actual)),
                    compareDataRows.equals(actual, expected));
            }
        }
    }

    /**
     * Test that the table contains the given rows in any order (and nothing else).
     */
    static void assertSetEquality(final BufferedDataTable results, final DataRow... rows) {

        TCustomHashSet<DataRow> expectedRows = new TCustomHashSet<>(compareDataRows);
        Arrays.stream(rows).forEach(expectedRows::add);

        // output table contains one match
        try (CloseableRowIterator it = results.iterator()) {
            while (it.hasNext()) {
                DataRow rowInTable = it.next();
                String message = "Unexpected row: " + rowInTable.toString()
                    + ". Expected remaining rows: " + Arrays.toString(rows);
                assertTrue(message, expectedRows.remove(rowInTable));
            }
        }

        assertTrue(String.format("Incomplete result. Missing rows: %s", expectedRows), expectedRows.size()==0);

    }

    /**
     * Some DataRows, e.g., {@link BlobSupportDataRow} don't implement equals so we can't compare them as expected.
     */
    @SuppressWarnings("serial")
    static HashingStrategy<DataRow> compareDataRows = new HashingStrategy<DataRow>() {

        @Override
        public int computeHashCode(final DataRow object) {
            return object.getKey().hashCode();
        }

        @Override
        public boolean equals(final DataRow row1, final DataRow row2) {

            if (row1 == row2) {
                return true;
            }
            if (row1.getNumCells() != row2.getNumCells() || !row1.getKey().equals(row2.getKey())) {
                return false;
            }
            for (int i = 0; i < row2.getNumCells(); i++) {
                if (!row1.getCell(i).equals(row2.getCell(i))) {
                    return false;
                }
            }
            return true;

        }
    };

}