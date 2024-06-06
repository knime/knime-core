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
 *   Mar 15, 2023 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.testing.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.knime.testing.data.TableBackendTestUtils.doubleFactory;
import static org.knime.testing.data.TableBackendTestUtils.intFactory;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InternalTableAPI;
import org.knime.core.table.row.Selection;
import org.knime.testing.data.TableBackendTestUtils.Column;

/**
 * Slicing API.
 *
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 */
final class SlicerAPITester extends AbstractTableBackendAPITester {

    private static final String DOUBLE_COL_NAME = "fooDouble";
    private static final String INT_COL_NAME = "fooInt";

    SlicerAPITester(final ExecutionContext exec) {
        super(exec);
    }

    void testSingleSlice() throws Exception {
        final var colInt = new Column(INT_COL_NAME, intFactory(1, 2, 3, 4, 5, 6));
        final var colDouble = new Column(DOUBLE_COL_NAME, doubleFactory(1.5, 2.5, 3.5, 4.5, 5.5, 6.5));
        final var table = createTable(r -> "Row" + r, colInt, colDouble);

        var sel1 = Selection.all().retainRows(2, 4).retainColumns(0);
        var sel1Table = InternalTableAPI.slice(getExec(), table, sel1);
        var sel1RefTable = createTable(r -> "Row" + (r + 2), new Column(INT_COL_NAME, intFactory(3, 4)));
        TableBackendTestUtils.assertTableEquals(sel1RefTable, sel1Table);

        var sel2 = Selection.all().retainRows(3, table.size()).retainColumns(1);
        var sel2Table = InternalTableAPI.slice(getExec(), table, sel2);
        var sel2RefTable = createTable(r -> "Row" + (r + 3), new Column(DOUBLE_COL_NAME, doubleFactory(4.5, 5.5, 6.5)));
        TableBackendTestUtils.assertTableEquals(sel2RefTable, sel2Table);

        var sel3 = Selection.all().retainRows(-1, /** ignored */ 2).retainColumns(0, 1);
        var sel3Table = InternalTableAPI.slice(getExec(), table, sel3);
        var sel3RefTable = table; // according to the API/javadoc: from < 0 implies no filtering (at all)
        TableBackendTestUtils.assertTableEquals(sel3RefTable, sel3Table);

        var sel4 = Selection.all().retainRows(4, 2).retainColumns(0, 1);
        var sel4Table = InternalTableAPI.slice(getExec(), table, sel4);
        assertEquals(table.getDataTableSpec(), sel4Table.getDataTableSpec(), "same table structure");
        assertEquals(0L, sel4Table.size(), "table to have no rows (to < from)");

        var sel5 = Selection.all().retainRows(0, 100).retainColumns(1, 0); // different col order should be ignored
        var sel5Table = InternalTableAPI.slice(getExec(), table, sel5);
        var sel5RefTable = table;
        TableBackendTestUtils.assertTableEquals(sel5RefTable, sel5Table);

        var sel6 = Selection.all().retainRows(0, 2).retainColumns(); // no columns
        var sel6Table = InternalTableAPI.slice(getExec(), table, sel6);
        assertEquals(2, sel6Table.size(), "Number of rows");
        assertEquals(0, sel6Table.getDataTableSpec().getNumColumns(), "Number of columns");
        try (var it = sel6Table.iterator()) {
            assertEquals("Row0", it.next().getKey().getString(), "RowKey 0th row");
            assertEquals("Row1", it.next().getKey().getString(), "RowKey 1st row");
        }

        // chained calls of retainRows apply to the previously made row selection
        var sel7 = Selection.all().retainRows(1, 5).retainRows(1, 4).retainColumns(0);
        var sel7Table = InternalTableAPI.slice(getExec(), table, sel7);
        var sel7RefTable = createTable(r -> "Row" + (r + 2), new Column(INT_COL_NAME, intFactory(3, 4, 5)));
        TableBackendTestUtils.assertTableEquals(sel7RefTable, sel7Table);

        assertThrows(IndexOutOfBoundsException.class,
            () -> InternalTableAPI.slice(getExec(), table, Selection.all().retainColumns(5)));
    }

    void testMultiSlice() throws Exception {
        final var colInt = new Column(INT_COL_NAME, intFactory(1, 2, 3, 4, 5, 6));
        final var colDouble = new Column(DOUBLE_COL_NAME, doubleFactory(1.5, 2.5, 3.5, 4.5, 5.5, 6.5));
        final var table = createTable(r -> "Row" + r, colInt, colDouble);


        // ---------------------------------------------- //
        var sel1 = Selection.all().retainRows(2, 4).retainColumns(0);
        var sel1RefTable = createTable(r -> "Row" + (r + 2), new Column(INT_COL_NAME, intFactory(3, 4)));

        var sel2 = Selection.all().retainRows(3, table.size()).retainColumns(1);
        var sel2RefTable = createTable(r -> "Row" + (r + 3), new Column(DOUBLE_COL_NAME, doubleFactory(4.5, 5.5, 6.5)));

        var sel3 = Selection.all().retainRows(-1, /** ignored */ 2).retainColumns(0, 1);
        var sel3RefTable = table; // according to the API/javadoc: from < 0 implies no filtering (at all)

        var sel4 = Selection.all().retainRows(4, 2).retainColumns(0, 1);

        var sel5 = Selection.all().retainRows(0, 100).retainColumns(1, 0); // different col order should be ignored
        var sel5RefTable = table;

        var sel6 = Selection.all().retainRows(0, 2).retainColumns(); // no columns

        // chained calls of retainRows apply to the previously made row selection
        var sel7 = Selection.all().retainRows(1, 5).retainRows(1, 4).retainColumns(0);
        var sel7RefTable = createTable(r -> "Row" + (r + 2), new Column(INT_COL_NAME, intFactory(3, 4, 5)));


        // ---------------------------------------------- //
        final var selections = new Selection[] {sel1, sel2, sel3, sel4, sel5, sel6, sel7};
        final BufferedDataTable[] selRefTables = InternalTableAPI.multiSlice(getExec(), table, selections);
        var sel1Table = selRefTables[0];
        TableBackendTestUtils.assertTableEquals(sel1RefTable, sel1Table);

        var sel2Table = selRefTables[1];
        TableBackendTestUtils.assertTableEquals(sel2RefTable, sel2Table);

        var sel3Table = selRefTables[2];
        TableBackendTestUtils.assertTableEquals(sel3RefTable, sel3Table);

        var sel4Table = selRefTables[3];
        assertEquals(table.getDataTableSpec(), sel4Table.getDataTableSpec(), "same table structure");
        assertEquals(0L, sel4Table.size(), "table to have no rows (to < from)");

        var sel5Table = selRefTables[4];
        TableBackendTestUtils.assertTableEquals(sel5RefTable, sel5Table);

        var sel6Table = selRefTables[5];
        assertEquals(2, sel6Table.size(), "Number of rows");
        assertEquals(0, sel6Table.getDataTableSpec().getNumColumns(), "Number of columns");
        try (var it = sel6Table.iterator()) {
            assertEquals("Row0", it.next().getKey().getString(), "RowKey 0th row");
            assertEquals("Row1", it.next().getKey().getString(), "RowKey 1st row");
        }

        var sel7Table = selRefTables[6];
        TableBackendTestUtils.assertTableEquals(sel7RefTable, sel7Table);

        // ---------------------------------------------- //
        // some multi slice with an empty window at the start (rows skipped)
        final var selections2 = new Selection[] {sel2, sel7};
        final BufferedDataTable[] selRefTables2 = InternalTableAPI.multiSlice(getExec(), table, selections2);

        var sel2Table2 = selRefTables2[0];
        TableBackendTestUtils.assertTableEquals(sel2RefTable, sel2Table2);

        var sel7Table2 = selRefTables2[1];
        TableBackendTestUtils.assertTableEquals(sel7RefTable, sel7Table2);
    }
}
