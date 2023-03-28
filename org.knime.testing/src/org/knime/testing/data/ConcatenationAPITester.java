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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.knime.testing.data.TableBackendTestUtils.checkTable;
import static org.knime.testing.data.TableBackendTestUtils.doubleFactory;
import static org.knime.testing.data.TableBackendTestUtils.intFactory;

import java.util.Optional;

import org.knime.core.node.ExecutionContext;
import org.knime.testing.data.TableBackendTestUtils.Column;

/**
 * Tests the concatenation API of TableBackends.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class ConcatenationAPITester extends AbstractTableBackendAPITester {

    ConcatenationAPITester(final ExecutionContext exec) {
        super(exec);
    }

    void testConcatenateWithDifferingSpecs() throws Exception {
        var fooInt = new Column("foo", intFactory(1, 2, 3));
        var fooDouble = new Column("foo", doubleFactory(1.5, 2.5, 3.5));

        var fooIntTable = createTable(r -> "Row" + r, fooInt);
        var fooDoubleTable = createTable(r -> Long.toString(r), fooDouble);

        var fooIntDoubleTable = getExec().createConcatenateTable(getExec(), fooIntTable, fooDoubleTable);
        var expectedFooIntDoubleTable = createTable(r -> r < 3 ? ("Row" + r) : Long.toString(r - 3),
            new Column("foo", doubleFactory(1.0, 2.0, 3.0, 1.5, 2.5, 3.5)));
        checkTable(expectedFooIntDoubleTable, fooIntDoubleTable);

        var fooDoubleIntTable = getExec().createConcatenateTable(getExec(), fooDoubleTable, fooIntTable);
        var expectedFooDoubleIntTable = createTable(r -> r < 3 ? Long.toString(r) : ("Row" + (r - 3)),
            new Column("foo", doubleFactory(1.5, 2.5, 3.5, 1.0, 2.0, 3.0)));
        checkTable(expectedFooDoubleIntTable, fooDoubleIntTable);
    }

    void testFailOnDuplicateRowIDs() throws Exception {
        var foo = new Column("foo", intFactory(1, 2, 3));
        var table = createTable(foo);
        assertThrows(RuntimeException.class, () -> getExec().createConcatenateTable(getExec(), table, table));
    }

    void testDeduplicateRowIDsWithSuffix() throws Exception {
        var foo = new Column("foo", intFactory(1, 2, 3));
        var table = createTable(r -> "Row" + r, foo);
        var otherTable = createTable(r -> "Row" + (r + 1), foo);
        var concatenated = getExec().createConcatenateTable(getExec(), Optional.of("dup"), false, table, otherTable);
        String[] expectedIDs = {"Row0", "Row1", "Row2", "Row1dup", "Row2dup", "Row3"};
        var expected = createTable(r -> expectedIDs[(int)r],
            new Column("foo", intFactory(1, 2, 3, 1, 2, 3)));
        checkTable(expected, concatenated);
    }

}
