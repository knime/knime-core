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
 *   Feb 24, 2023 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.testing.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.TableBackend.AppendConfig;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.RowRead;
import org.knime.core.data.v2.value.ValueInterfaces.IntWriteValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InternalTableAPI;

/**
 * Tester for the {@link InternalTableAPI}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class InternalTableAPITester {

    private final ExecutionContext m_exec;

    /**
     * @param exec the ExecutionContext to use for testing
     */
    public InternalTableAPITester(final ExecutionContext exec) {
        m_exec = exec;
    }

    /**
     * Tests {@link InternalTableAPI#append(ExecutionContext, AppendConfig, BufferedDataTable, BufferedDataTable)}.
     *
     * @throws Exception if the tests fail
     */
    public void testAppend() throws Exception {

        String[] leftColNames = {"foo", "bar"};
        int[] foo = {1, 2, 3};
        int[] bar = {4, 5, 6};
        String[] leftIDs = {"Row0", "Row1", "Row2"};
        var left = createIntTable(leftIDs, leftColNames, foo, bar);
        String[] rightIDs = {"Row3", "Row4", "Row5"};
        String[] rightColNames = {"baz", "buz"};
        int[] baz = {7, 8, 9};
        int[] buz = {10, 11, 12};
        var right = createIntTable(rightIDs, rightColNames, baz, buz);

        var appendedRightIDs = InternalTableAPI.append(m_exec, AppendConfig.rowIDsFromTable(1), left, right);

        String[] appendedColNames = ArrayUtils.addAll(leftColNames, rightColNames);
        var expected = createIntTable(rightIDs, appendedColNames, foo, bar, baz, buz);
        assertTableEquals(expected, appendedRightIDs);

        var appendedLeftIDs = InternalTableAPI.append(m_exec, AppendConfig.rowIDsFromTable(0), left, right);
        expected = createIntTable(leftIDs, appendedColNames, foo, bar, baz, buz);
        assertTableEquals(expected, appendedLeftIDs);

        var rightWithMatchingIDs = createIntTable(leftIDs, rightColNames, baz, buz);
        var appendedMatchingIDs =
            InternalTableAPI.append(m_exec, AppendConfig.matchingRowIDs(), left, rightWithMatchingIDs);
        assertTableEquals(expected, appendedMatchingIDs);
        assertThrows(IllegalArgumentException.class,
            () -> InternalTableAPI.append(m_exec, AppendConfig.matchingRowIDs(), left, right));
    }

    private static void assertTableEquals(final BufferedDataTable expected, final BufferedDataTable actual) {
        var expectedSpec = expected.getDataTableSpec();
        var actualSpec = actual.getDataTableSpec();
        assertEquals(expectedSpec, actualSpec,
            String.format("The specs %s and %s don't match", expectedSpec, actualSpec));
        assertEquals(expected.size(), actual.size(),
            String.format("The row counts don't match (%s vs %s)", expected.size(), actual.size()));
        try (var expectedCursor = expected.cursor(); var actualCursor = actual.cursor()) {
            for (long r = 0; expectedCursor.canForward(); r++) {
                var expectedRow = expectedCursor.forward();
                var actualRow = actualCursor.forward();
                assertRowEquals(r, expectedRow, actualRow);
            }
        }
    }

    private static void assertRowEquals(final long r, final RowRead expectedRow, final RowRead actualRow) {
        var expectedKey = expectedRow.getRowKey().getString();
        var actualKey = actualRow.getRowKey().getString();
        assertEquals(expectedKey, actualKey, String.format("The RowID differs in row %s.", r));
        for (var c = 0; c < expectedRow.getNumColumns(); c++) {
            if (expectedRow.isMissing(c)) {
                assertTrue(actualRow.isMissing(c),
                    String.format(
                        "The expected value for column %s in row %s is missing but the actual value is %s.", c,
                        r, toCell(actualRow.getValue(c))));
            } else {
                assertEquals(toCell(expectedRow.getValue(c)), toCell(actualRow.getValue(c)),
                    "Unexpected value in column %c of row %s.");
            }
        }
    }

    private static DataCell toCell(final DataValue value) {
        if (value instanceof ReadValue) {
            var r = (ReadValue)value;
            return r.getDataCell();
        } else {
            return (DataCell)value;
        }
    }

    private BufferedDataTable createIntTable(final String[] rowIDs, final String[] columnNames, final int[]... columns)
        throws Exception {
        var spec = new DataTableSpec("default", columnNames,
            Stream.generate(() -> IntCell.TYPE).limit(columnNames.length).toArray(DataType[]::new));
        try (var rowContainer = m_exec.createRowContainer(spec); var cursor = rowContainer.createCursor()) {
            for (int r = 0; r < rowIDs.length; r++) {
                var rowWrite = cursor.forward();
                rowWrite.setRowKey(rowIDs[r]);
                for (int c = 0; c < columns.length; c++) {
                    rowWrite.<IntWriteValue> getWriteValue(c).setIntValue(columns[c][r]);
                }
            }
            return rowContainer.finish();
        }
    }

}
