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
 *   30 Aug 2022 Juan Diaz Baquero: created
 */
package org.knime.core.data.statistics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.knime.testing.util.TableTestUtil.assertTableResults;

import org.junit.jupiter.api.Test;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.sort.BufferedDataTableSorter;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.testing.util.TableTestUtil;
import org.knime.testing.util.TableTestUtil.ObjectColumn;

/**
 *
 * @author Juan Diaz Baquero
 */
class StatisticsTableUtilsTest {

    private static final Integer[] intValues = new Integer[]{4, 2, 6, null, 8};

    private static final Double[] doubleValues = new Double[]{4.0, 2.0, Double.NaN, null, 8.0};

    private static final String[] stringValues = new String[]{"c", "a", "d", "b", null};

    public static BufferedDataTable createTestTable() {
        var intColumn = new ObjectColumn("int", IntCell.TYPE, intValues);
        var doubleColumn = new ObjectColumn("double", DoubleCell.TYPE, doubleValues);
        var stringColumn = new ObjectColumn("string", StringCell.TYPE, stringValues);
        return TableTestUtil.createTableFromColumns(intColumn, doubleColumn, stringColumn);
    }

    @Test
    void testSortTableInt() throws CanceledExecutionException {
        final var table = TableTestUtil.createTableFromColumns(new ObjectColumn("Integer", IntCell.TYPE, intValues));
        var exec = TableTestUtil.getExec();

        BufferedDataTable outData = BufferedDataTableSorter.sortTable(table, 0, exec);
        assertTableResults(outData, new String[]{"Integer"}, new Object[][]{{null, 2, 4, 6, 8}});
    }

    @Test
    void testSortTableDouble() throws CanceledExecutionException {
        final var table =
            TableTestUtil.createTableFromColumns(new ObjectColumn("Double", DoubleCell.TYPE, doubleValues));
        var exec = TableTestUtil.getExec();

        BufferedDataTable outData = BufferedDataTableSorter.sortTable(table, 0, exec);
        assertTableResults(outData, new String[]{"Double"}, new Object[][]{{null, 2.0, 4.0, 8.0, Double.NaN}});
    }

    @Test
    void testSortTableString() throws CanceledExecutionException {
        final var table =
            TableTestUtil.createTableFromColumns(new ObjectColumn("String", StringCell.TYPE, stringValues));
        var exec = TableTestUtil.getExec();

        BufferedDataTable outData = BufferedDataTableSorter.sortTable(table, 0, exec);
        assertTableResults(outData, new String[]{"String"}, new Object[][]{{null, "a", "b", "c", "d"}});
    }

    @Test
    void testSplitTableByDimensionWithMissingAndNaNValues() throws CanceledExecutionException {
        final var table = createTestTable();
        var exec = TableTestUtil.getExec();

        var outData =
            StatisticsTableUtil.splitTableByColumnNames(table, new String[]{"string", "double"}, false, false, exec);
        assertTableResults(outData.get("string"), new String[]{"String"}, new Object[][]{stringValues});
        assertTableResults(outData.get("double"), new String[]{"Double"}, new Object[][]{doubleValues});
    }

    @Test
    void testSplitTableByDimensionWithoutMissingValues() throws CanceledExecutionException {
        final var table = createTestTable();
        var exec = TableTestUtil.getExec();

        var outData =
            StatisticsTableUtil.splitTableByColumnNames(table, new String[]{"string", "double"}, true, false, exec);
        assertTableResults(outData.get("string"), new String[]{"String"}, new Object[][]{{"c", "a", "d", "b"}});
        assertTableResults(outData.get("double"), new String[]{"Double"}, new Object[][]{{4.0, 2.0, Double.NaN, 8.0}});
    }

    @Test
    void testSplitTableByDimensionWithoutNaNValues() throws CanceledExecutionException {
        final var table = createTestTable();
        var exec = TableTestUtil.getExec();

        var outData =
            StatisticsTableUtil.splitTableByColumnNames(table, new String[]{"string", "double"}, false, true, exec);
        assertTableResults(outData.get("string"), new String[]{"String"}, new Object[][]{{"c", "a", "d", "b", null}});
        assertTableResults(outData.get("double"), new String[]{"Double"}, new Object[][]{{4.0, 2.0, null, 8.0}});
    }

    @Test
    void testSplitTableByDimensionWithoutBothMissingAndNaNValues() throws CanceledExecutionException {
        final var table = createTestTable();
        var exec = TableTestUtil.getExec();

        var outData =
            StatisticsTableUtil.splitTableByColumnNames(table, new String[]{"string", "double"}, true, true, exec);
        assertTableResults(outData.get("string"), new String[]{"String"}, new Object[][]{{"c", "a", "d", "b"}});
        assertTableResults(outData.get("double"), new String[]{"Double"}, new Object[][]{{4.0, 2.0, 8.0}});
    }

    @Test
    void testCreateEmptyContainersPerDimension() throws CanceledExecutionException {
        var exec = TableTestUtil.getExec();
        var outData = StatisticsTableUtil.createEmptyContainersPerDimension(new String[]{"string", "double"},
            new DataType[]{StringCell.TYPE, DoubleCell.TYPE}, exec);
        assertThat(outData.get("string").size()).as("Empty string table").isZero();
        assertThat(outData.get("double").size()).as("Empty double table").isZero();
    }
}