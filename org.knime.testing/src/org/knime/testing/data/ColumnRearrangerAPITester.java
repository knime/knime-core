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
 *   Mar 9, 2023 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.testing.data;

import static org.knime.testing.data.TableBackendTestUtils.assertTableEquals;
import static org.knime.testing.data.TableBackendTestUtils.createColumnSpecs;
import static org.knime.testing.data.TableBackendTestUtils.doubleFactory;
import static org.knime.testing.data.TableBackendTestUtils.intFactory;
import static org.knime.testing.data.TableBackendTestUtils.stringFactory;

import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellTypeConverter;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.testing.data.TableBackendTestUtils.Column;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class ColumnRearrangerAPITester extends AbstractTableBackendAPITester {

    private static final Column BAZ = new Column("baz", doubleFactory(1.5, null, 3.5));

    private static final Column BAR = new Column("bar", stringFactory("some", "boring", null));

    private static final Column FOO = new Column("foo", intFactory(null, 2, 3));

    ColumnRearrangerAPITester(final ExecutionContext exec) {
        super(exec);
    }

    /*====================== Reorder ================*/

    void testReorderingOnlyExisting() throws Exception {
        var inputTable = createTable(FOO, BAR, BAZ);
        var rearranger = new ColumnRearranger(inputTable.getDataTableSpec());
        rearranger.permute(new String[]{"baz", "foo", "bar"});
        ExecutionContext exec = getExec();
        var rearrangedTable = exec.createColumnRearrangeTable(inputTable, rearranger, exec);
        var expectedTable = createTable(BAZ, FOO, BAR);
        assertTableEquals(expectedTable, rearrangedTable);
    }

    /*====================== Filter =================*/

    void testFilteringOnlyExisting() throws Exception {
        var inputTable = createTable(FOO, BAR, BAZ);
        var rearranger = new ColumnRearranger(inputTable.getDataTableSpec());
        rearranger.keepOnly(BAR.name());
        var rearrangedTable = rearrange(inputTable, rearranger);
        var expectedTable = createTable(BAR);
        assertTableEquals(expectedTable, rearrangedTable);
    }

    /*====================== Append =================*/

    void testAppendSingleFactorySingleColumn() throws Exception {
        var inputTable = createTable(FOO, BAR, BAZ);
        var tableSpec = inputTable.getDataTableSpec();
        var rearranger = new ColumnRearranger(tableSpec);
        var expectedAppendedColumn = new Column("foo_", intFactory(null, 1, 2));
        rearranger.append(new DecrementNumberCellFactory(createColumnSpecs(expectedAppendedColumn), 0));
        var rearrangedTable = rearrange(inputTable, rearranger);
        var expectedTable = createTable(FOO, BAR, BAZ, expectedAppendedColumn);
        assertTableEquals(expectedTable, rearrangedTable);
    }

    void testAppendSingleFactoryMultipleColumns() throws Exception {
        var inputTable = createTable(FOO, BAR, BAZ);
        var rearranger = new ColumnRearranger(inputTable.getDataTableSpec());
        var expectedIntColumn = new Column("foo_", intFactory(null, 1, 2));
        var expectedDoubleColumn = new Column("baz_", doubleFactory(0.5, null, 2.5));
        rearranger
            .append(new DecrementNumberCellFactory(createColumnSpecs(expectedIntColumn, expectedDoubleColumn), 0, 2));
        var rearrangedTable = rearrange(inputTable, rearranger);
        var expectedTable = createTable(FOO, BAR, BAZ, expectedIntColumn, expectedDoubleColumn);
        assertTableEquals(expectedTable, rearrangedTable);
    }

    void testAppendMultipleFactoriesOneColumnEach() throws Exception {
        var inputTable = createTable(FOO, BAR, BAZ);
        var rearranger = new ColumnRearranger(inputTable.getDataTableSpec());
        var expectedIntColumn = new Column("foo_", intFactory(null, 1, 2));
        var expectedDoubleColumn = new Column("baz_", doubleFactory(0.5, null, 2.5));
        rearranger.append(new DecrementNumberCellFactory(createColumnSpecs(expectedIntColumn), 0));
        rearranger.append(new DecrementNumberCellFactory(createColumnSpecs(expectedDoubleColumn), 2));
        var rearrangedTable = rearrange(inputTable, rearranger);
        var expectedTable = createTable(FOO, BAR, BAZ, expectedIntColumn, expectedDoubleColumn);
        assertTableEquals(expectedTable, rearrangedTable);
    }

    /* ======================== Replace ============================*/

    void testReplaceSingleFactorySingleColumn() throws Exception {
        var inputTable = createTable(FOO, BAR, BAZ);
        var tableSpec = inputTable.getDataTableSpec();
        var rearranger = new ColumnRearranger(tableSpec);
        var expectedBaz = new Column(BAZ.name(), doubleFactory(0.5, null, 2.5));
        rearranger.replace(new DecrementNumberCellFactory(createColumnSpecs(expectedBaz), 2), BAZ.name());
        var rearrangedTable = rearrange(inputTable, rearranger);
        var expectedTable = createTable(FOO, BAR, expectedBaz);
        assertTableEquals(expectedTable, rearrangedTable);
    }

    void testReplaceSingleFactoryMultipleColumns() throws Exception {
        var inputTable = createTable(FOO, BAR, BAZ);
        var tableSpec = inputTable.getDataTableSpec();
        var rearranger = new ColumnRearranger(tableSpec);
        var expectedFoo = new Column(FOO.name(), intFactory(null, 1, 2));
        var expectedBaz = new Column(BAZ.name(), doubleFactory(0.5, null, 2.5));
        rearranger.replace(new DecrementNumberCellFactory(createColumnSpecs(expectedFoo, expectedBaz), 0, 2), 0, 2);
        var rearrangedTable = rearrange(inputTable, rearranger);
        var expectedTable = createTable(expectedFoo, BAR, expectedBaz);
        assertTableEquals(expectedTable, rearrangedTable);
    }

    void testReplaceMultipleFactoriesOneColumnEach() throws Exception {
        var inputTable = createTable(FOO, BAR, BAZ);
        var tableSpec = inputTable.getDataTableSpec();
        var rearranger = new ColumnRearranger(tableSpec);
        var expectedFoo = new Column(FOO.name(), intFactory(null, 1, 2));
        var expectedBaz = new Column(BAZ.name(), doubleFactory(0.5, null, 2.5));
        rearranger.replace(new DecrementNumberCellFactory(createColumnSpecs(expectedBaz), 2), 2);
        rearranger.replace(new DecrementNumberCellFactory(createColumnSpecs(expectedFoo), 0), 0);
        var rearrangedTable = rearrange(inputTable, rearranger);
        var expectedTable = createTable(expectedFoo, BAR, expectedBaz);
        assertTableEquals(expectedTable, rearrangedTable);
    }

    void testReplaceMultipleCellFactoriesMultipleColumnsEach() throws Exception {
        var bli = new Column("bli", intFactory(4, 5, 6));
        var bla = new Column("bla", doubleFactory(4.5, 5.5, 6.5));
        var inputTable = createTable(FOO, BAR, BAZ, bli, bla);
        var tableSpec = inputTable.getDataTableSpec();
        var rearranger = new ColumnRearranger(tableSpec);
        var expectedFoo = new Column(FOO.name(), intFactory(null, 1, 2));
        var expectedBaz = new Column(BAZ.name(), doubleFactory(0.5, null, 2.5));
        var expectedBli = new Column(bli.name(), intFactory(3, 4, 5));
        var expectedBla = new Column(bla.name(), doubleFactory(3.5, 4.5, 5.5));
        rearranger.replace(new DecrementNumberCellFactory(createColumnSpecs(expectedFoo, expectedBli), 0, 3), 0, 3);
        rearranger.replace(new DecrementNumberCellFactory(createColumnSpecs(expectedBaz, expectedBla), 2, 4), 2, 4);
        var rearrangedTable = rearrange(inputTable, rearranger);
        var expectedTable = createTable(expectedFoo, BAR, expectedBaz, expectedBli, expectedBla);
        assertTableEquals(expectedTable, rearrangedTable);
    }

    void testInputRowMethods() throws Exception {
        var inputTable = createTable(FOO, BAR);
        var rearranger = new ColumnRearranger(inputTable.getDataTableSpec());
        rearranger.append(new WholeRowToStringCellFactory("summary"));
        rearranger.keepOnly("summary");
        var rearrangedTable = rearrange(inputTable, rearranger);
        var expectedSummaryColumn = new Column("summary", stringFactory("Row0?some", "Row12boring", "Row23?"));
        var expectedTable = createTable(expectedSummaryColumn);
        assertTableEquals(expectedTable, rearrangedTable);
    }

    /*====================== Type Conversion ==================*/

    void testSingleTypeConverter() throws Exception {
        var inputTable = createTable(FOO, BAR, BAZ);
        var rearranger = new ColumnRearranger(inputTable.getDataTableSpec());
        rearranger.ensureColumnIsConverted(new ToStringTypeConverter(), 0);
        var rearrangedTable = rearrange(inputTable, rearranger);
        var expectedConvertedColumn = new Column(FOO.name(), stringFactory("?", "2", "3"));
        var expectedTable = createTable(expectedConvertedColumn, BAR, BAZ);
        assertTableEquals(expectedTable, rearrangedTable);
    }

    void testMultipleTypeConverters() throws Exception {
        var inputTable = createTable(FOO, BAR, BAZ);
        var rearranger = new ColumnRearranger(inputTable.getDataTableSpec());
        rearranger.ensureColumnIsConverted(new ToStringTypeConverter(), FOO.name());
        rearranger.ensureColumnIsConverted(new ToStringTypeConverter(), BAZ.name());
        var rearrangedTable = rearrange(inputTable, rearranger);
        var expectedFoo = new Column(FOO.name(), stringFactory("?", "2", "3"));
        var expectedBaz = new Column(BAZ.name(), stringFactory("1.5", "?", "3.5"));
        var expectedTable = createTable(expectedFoo, BAR, expectedBaz);
        assertTableEquals(expectedTable, rearrangedTable);
    }

    void testMultipleConvertersPerColumn() throws Exception {
        var inputTable = createTable(FOO, BAR, BAZ);
        var rearranger = new ColumnRearranger(inputTable.getDataTableSpec());
        rearranger.ensureColumnIsConverted(new ToStringTypeConverter(), 0);
        rearranger.ensureColumnIsConverted(new StringToDoubleConverter(), 0);
        var rearrangedTable = rearrange(inputTable, rearranger);
        var expectedFoo = new Column(FOO.name(), doubleFactory(null, 2.0, 3.0));
        var expectedTable = createTable(expectedFoo, BAR, BAZ);
        assertTableEquals(expectedTable, rearrangedTable);
    }

    /*======================= Combinations ======================*/

    void testAppendConvertedColumn() throws Exception {
        var inputTable = createTable(FOO, BAR, BAZ);
        var tableSpec = inputTable.getDataTableSpec();
        var rearranger = new ColumnRearranger(tableSpec);
        var expectedAppendedColumn = new Column(FOO.name() + "_", stringFactory("?_", "2_", "3_"));
        rearranger.append(new AppendSuffixCellFactory("_", createColumnSpecs(expectedAppendedColumn), 0));
        rearranger.ensureColumnIsConverted(new ToStringTypeConverter(), 0);
        var rearrangedTable = rearrange(inputTable, rearranger);
        var expectedConvertedColumn = new Column(FOO.name(), stringFactory("?", "2", "3"));
        var expectedTable = createTable(expectedConvertedColumn, BAR, BAZ, expectedAppendedColumn);
        assertTableEquals(expectedTable, rearrangedTable);
    }

    //  replacing a converted column is not possible because the CellFactory and the DataCellTypeConverter
    // replace each other

    @Test
    void testFilterThenReorder() throws Exception {
        var inputTable = createTable(FOO, BAR, BAZ);
        var tableSpec = inputTable.getDataTableSpec();
        var rearranger = new ColumnRearranger(tableSpec);
        rearranger.remove(BAR.name());
        rearranger.permute(new String[] {BAZ.name(), FOO.name()});
        var rearrangedTable = rearrange(inputTable, rearranger);
        var expectedTable = createTable(BAZ, FOO);
        assertTableEquals(expectedTable, rearrangedTable);
    }

    @Test
    void testReorderThenFilter() throws Exception {
        var inputTable = createTable(FOO, BAR, BAZ);
        var tableSpec = inputTable.getDataTableSpec();
        var rearranger = new ColumnRearranger(tableSpec);
        rearranger.permute(new String[] {BAZ.name(), BAR.name(), FOO.name()});
        rearranger.remove(BAR.name());
        var rearrangedTable = rearrange(inputTable, rearranger);
        var expectedTable = createTable(BAZ, FOO);
        assertTableEquals(expectedTable, rearrangedTable);
    }

    @Test
    void testReorderThenFilterThenAppend() throws Exception {
        var inputTable = createTable(FOO, BAR, BAZ);
        var tableSpec = inputTable.getDataTableSpec();
        var rearranger = new ColumnRearranger(tableSpec);
        var kaboom = new Column("kaboom", intFactory(null, 1, 2));
        rearranger.permute(new String[] {BAZ.name(), BAR.name(), FOO.name()});
        rearranger.remove(BAR.name());
        // the CellFactories always get the original row order (unless a converter is used)
        rearranger.append(new DecrementNumberCellFactory(createColumnSpecs(kaboom), 0));
        var rearrangedTable = rearrange(inputTable, rearranger);
        var expectedTable = createTable(BAZ, FOO, kaboom);
        assertTableEquals(expectedTable, rearrangedTable);
    }

    @Test
    void testAppendThenFilterOutColumnUsedByAppend() throws Exception {
        var inputTable = createTable(FOO, BAR, BAZ);
        var tableSpec = inputTable.getDataTableSpec();
        var rearranger = new ColumnRearranger(tableSpec);
        var kaboom = new Column("kaboom", intFactory(null, 1, 2));
        rearranger.append(new DecrementNumberCellFactory(createColumnSpecs(kaboom), 0));
        rearranger.remove(FOO.name());
        var rearrangedTable = rearrange(inputTable, rearranger);
        var expectedTable = createTable(BAR, BAZ, kaboom);
        assertTableEquals(expectedTable, rearrangedTable);
    }


    /*======================== Utilities ========================*/

    private static final class StringToDoubleConverter extends DataCellTypeConverter {

        @Override
        public DataCell convert(final DataCell source) throws Exception {
            return new DoubleCell(Integer.parseInt(source.toString()));
        }

        @Override
        public DataType getOutputType() {
            return DoubleCell.TYPE;
        }

    }

    private static final class ToStringTypeConverter extends DataCellTypeConverter {

        @Override
        public DataCell convert(final DataCell source) throws Exception {
            return new StringCell(source.toString());
        }

        @Override
        public DataType getOutputType() {
            return StringCell.TYPE;
        }

    }

    private static final class DecrementNumberCellFactory extends AbstractCellFactory {

        private final int[] m_columns;

        DecrementNumberCellFactory(final DataColumnSpec[] specs, final int... columnIndices) {
            super(specs);
            m_columns = columnIndices;
        }

        @Override
        public DataCell[] getCells(final DataRow row) {
            return IntStream.of(m_columns)//
                .mapToObj(row::getCell)//
                .map(this::decrement)//
                .toArray(DataCell[]::new);
        }

        private DataCell decrement(final DataCell cell) {//NOSONAR
            if (cell.isMissing()) {
                return cell;
            }
            if (cell instanceof IntValue i) {
                return new IntCell(i.getIntValue() - 1);
            } else if (cell instanceof DoubleValue d) {
                return new DoubleCell(d.getDoubleValue() - 1);
            } else {
                throw new IllegalStateException();
            }
        }
    }

    private static final class WholeRowToStringCellFactory extends SingleCellFactory {

        WholeRowToStringCellFactory(final String columnName) {
            super(new DataColumnSpecCreator(columnName, StringCell.TYPE).createSpec());
        }

        @Override
        public DataCell getCell(final DataRow row) {
            var sb = new StringBuilder(row.getKey().getString());
            for (var cell : row) {
                sb.append(cell.toString());
            }
            return new StringCell(sb.toString());
        }

    }

    private static final class AppendSuffixCellFactory extends AbstractCellFactory {
        private final int[] m_columns;

        private final String m_suffix;

        AppendSuffixCellFactory(final String suffix, final DataColumnSpec[] specs, final int... columns) {
            super(specs);
            m_columns = columns;
            m_suffix = suffix;
        }

        @Override
        public DataCell[] getCells(final DataRow row) {
            return IntStream.of(m_columns)//
                .mapToObj(row::getCell)//
                .map(this::appendSuffix)//
                .toArray(DataCell[]::new);
        }

        private DataCell appendSuffix(final DataCell cell) {
            if (cell.isMissing()) {
                return cell;
            } else {
                return new StringCell(((StringValue)cell).getStringValue() + m_suffix);
            }
        }

    }

    private BufferedDataTable rearrange(final BufferedDataTable table, final ColumnRearranger rearranger)
        throws CanceledExecutionException {
        var exec = getExec();
        return exec.createColumnRearrangeTable(table, rearranger, exec);
    }
}
