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
 *   Mar 10, 2023 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.testing.data;

import static org.knime.testing.data.TableBackendTestUtils.checkTable;
import static org.knime.testing.data.TableBackendTestUtils.doubleFactory;
import static org.knime.testing.data.TableBackendTestUtils.intFactory;
import static org.knime.testing.data.TableBackendTestUtils.stringFactory;

import java.util.function.IntSupplier;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.TableBackend;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.testing.data.TableBackendTestUtils.Column;
import org.knime.testing.data.TableBackendTestUtils.ColumnDataFactory;

/**
 * Tests {@link TableBackend#replaceSpec(ExecutionContext, BufferedDataTable, DataTableSpec, IntSupplier)}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class SpecReplacerAPITester extends AbstractTableBackendAPITester {

    private static final ColumnDataFactory STRING_FACTORY = stringFactory("foo", "bar", "baz");

    private static final ColumnDataFactory DOUBLE_FACTORY = doubleFactory(1.5, 2.5, 3.5);

    private static final ColumnDataFactory INT_FACTORY = intFactory(1, 2, 3);

    private static final Column FOO = new Column("foo", INT_FACTORY);

    private static final Column BAR = new Column("bar", DOUBLE_FACTORY);

    private static final Column BAZ = new Column("baz", STRING_FACTORY);

    SpecReplacerAPITester(final ExecutionContext exec) {
        super(exec);
    }

    /**
     * Tests changing the names of the columns. This is by far the most common use-case for spec replacement.
     *
     * @throws Exception
     */
    void testReplaceNames() throws Exception {
        var table = createTable(FOO, BAR, BAZ);
        var expectedTable = createTable(//
            new Column("bli", INT_FACTORY), //
            new Column("bla", DOUBLE_FACTORY), //
            new Column("blub", STRING_FACTORY)//
        );
        var renamedTable = getExec().createSpecReplacerTable(table, expectedTable.getDataTableSpec());
        checkTable(expectedTable, renamedTable);
    }

    void testUpcast() throws Exception {
        var table = createTable(FOO, BAR, BAZ);
        var expectedTable = createTable(new Column("foo", doubleFactory(1.0, 2.0, 3.0)), BAR, BAZ);
        var upcastedTable = getExec().createSpecReplacerTable(table, expectedTable.getDataTableSpec());
        checkTable(expectedTable, upcastedTable);
    }

    void testDowncastAfterUpcastToDataValue() throws Exception {
        var table = createTable(FOO);
        var upcastedTable = getExec().createSpecReplacerTable(table,
            new DataTableSpec(new DataColumnSpecCreator("foo", DataType.getType(DataCell.class)).createSpec()));
        var downcastedTable = getExec().createSpecReplacerTable(upcastedTable, table.getDataTableSpec());
        checkTable(table, downcastedTable);
    }

}
