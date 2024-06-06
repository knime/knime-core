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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Function;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.TableBackend;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.container.DataContainerSettings;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.v2.RowWriteCursor;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.data.v2.value.ValueInterfaces.DoubleWriteValue;
import org.knime.core.data.v2.value.ValueInterfaces.IntWriteValue;
import org.knime.core.data.v2.value.ValueInterfaces.StringWriteValue;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.workflow.NodeContext;

/**
 * Contains utility functions for testing a {@link TableBackend}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class TableBackendTestUtils {

    static BufferedDataTable saveAndLoad(final BufferedDataTable table, final File tblDir)
        throws IOException, CanceledExecutionException, InvalidSettingsException {
        var savedTableIDs = new HashSet<Integer>();
        Node.invokeSave(table, tblDir, savedTableIDs, new ExecutionMonitor());
        // the node context is set by the test suite
        var dataRepo = NodeContext.getContext().getWorkflowManager().getWorkflowDataRepository();
        var referencedFile = new ReferencedFile(tblDir);
        var tblRep = new HashMap<Integer, BufferedDataTable>();
        var loadedTable = Node.loadBufferedDataTableFromFile(referencedFile, new NodeSettings("test"),
            new ExecutionMonitor(), tblRep, dataRepo);
        return loadedTable;
    }

    /**
     * Checks if the table is as expected and also checks if it is still as expected after saving and loading.
     *
     * @param referenceTable the expected table
     * @param actualTable the actual table
     * @throws IOException if saving and loading the table fails
     * @throws CanceledExecutionException not thrown
     * @throws InvalidSettingsException not thrown
     */
    static void checkTable(final BufferedDataTable referenceTable, final BufferedDataTable actualTable)
        throws IOException, CanceledExecutionException, InvalidSettingsException {
        assertTableEquals(referenceTable, actualTable);
        var tmpFolder = Files.createTempDirectory("tbl").toFile();
        try {
            assertTableEquals(referenceTable, saveAndLoad(actualTable, tmpFolder));
        } finally {
            FileUtils.deleteDirectory(tmpFolder);
        }
    }

    static void assertTableEquals(final BufferedDataTable referenceTable, final BufferedDataTable actualTable) {
        var referenceSpec = referenceTable.getDataTableSpec();
        var actualSpec = actualTable.getDataTableSpec();
        assertTrue(referenceSpec.equalStructure(actualSpec),
            "The table specs don't match: Expected: %s Actual: %s".formatted(referenceSpec, actualSpec));
        assertEquals(referenceTable.size(), actualTable.size(), "The tables have varying number of rows.");
        try (var referenceIterator = referenceTable.iterator(); var actualIterator = actualTable.iterator()) {
            for (long r = 0; referenceIterator.hasNext(); r++) {
                var referenceRow = referenceIterator.next();
                var actualRow = actualIterator.next();
                assertRowEquals(referenceRow, actualRow, r);
            }
        }
    }

    static void assertRowEquals(final DataRow referenceRow, final DataRow actualRow, final long rowIndex) {
        assertEquals(referenceRow.getKey(), actualRow.getKey(), "The RowIDs differ in row %s.".formatted(rowIndex));
        assertEquals(referenceRow.getNumCells(), actualRow.getNumCells(),
            "The number of cells differ in row %s.".formatted(rowIndex));
        for (int c = 0; c < referenceRow.getNumCells(); c++) {//NOSONAR
            assertTrue(cellEquals(referenceRow.getCell(c), actualRow.getCell(c)),
                "The rows differ in column %s of row %s.".formatted(c, rowIndex));
        }
    }

    static boolean cellEquals(final DataCell referenceCell, final DataCell actualCell) {
        if (referenceCell instanceof DoubleValue referenceValue && actualCell instanceof DoubleValue actualValue) {
            return  referenceValue.getDoubleValue() == actualValue.getDoubleValue();
        } else {
            return referenceCell.equals(actualCell);
        }
    }

    static BufferedDataTable createTable(final ExecutionContext exec, final Column... columns) throws Exception {
        return createTableViaRowContainerAPI(exec, createSpec(columns), DataContainerSettings.getDefault(), r -> "Row" + r,
            columns);
    }

    /** Creates a table using the RowContainer API (WriteValue). */
    static BufferedDataTable createTableViaRowContainerAPI(final ExecutionContext exec, final DataTableSpec tableSpec,
        final DataContainerSettings containerSettings,
        final LongFunction<String> rowIDFactory, final Column... columns)
        throws IOException {//NOSONAR
        var sizeSummary =
            Stream.of(columns).map(Column::dataFactory).mapToInt(ColumnDataFactory::size).summaryStatistics();
        assertEquals(sizeSummary.getMin(), sizeSummary.getMax(), "The sizes of all columns must match.");
        var size = sizeSummary.getMin();
        var columnDataFactories = Stream.of(columns).map(Column::dataFactory).toArray(ColumnDataFactory[]::new);
        try (var rowContainer = exec.createRowContainer(tableSpec, containerSettings);
                var writeCursor = rowContainer.createCursor()) {
            writeData(rowIDFactory, size, columnDataFactories, writeCursor, columns);
            return rowContainer.finish();
        }
    }

    private static void writeData(final LongFunction<String> rowIDFactory, final int size,
        final ColumnDataFactory[] columnDataFactories, final RowWriteCursor writeCursor, final Column... columns) {
        LongConsumer[] mappers = null;
        for (long r = 0; r < size; r++) {
            var rowWrite = writeCursor.forward();
            if (mappers == null) {
                mappers = IntStream.range(0, columns.length)//
                    .mapToObj(i -> columns[i].dataFactory().createMapper(rowWrite.getWriteValue(i)))
                    .toArray(LongConsumer[]::new);

            }
            rowWrite.setRowKey(rowIDFactory.apply(r));
            final long rowIndex = r;
            for (int c = 0; c < mappers.length; c++) {//NOSONAR
                if (columnDataFactories[c].isMissing(rowIndex)) {
                    rowWrite.setMissing(c);
                } else {
                    mappers[c].accept(rowIndex);
                }
            }
        }
    }

    /** Creates a table using the DataContainer API (DataRow). */
    static BufferedDataTable createTableViaDataContainerAPI(final ExecutionContext exec, final DataTableSpec tableSpec,
        final DataContainerSettings containerSettings, final LongFunction<String> rowIDFactory,
        final Column... columns) {
        var sizeSummary =
                Stream.of(columns).map(Column::dataFactory).mapToInt(ColumnDataFactory::size).summaryStatistics();
        assertEquals(sizeSummary.getMin(), sizeSummary.getMax(), "The sizes of all columns must match.");
        var size = sizeSummary.getMin();
        var columnDataFactories = Stream.of(columns).map(Column::dataFactory).toArray(ColumnDataFactory[]::new);
        var container = exec.createDataContainer(tableSpec, containerSettings);
        writeDataRow(rowIDFactory, size, columnDataFactories, container);
        container.close();
        return container.getTable();
    }

    private static void writeDataRow(final LongFunction<String> rowIDFactory, final int size,
        final ColumnDataFactory[] columnDataFactories, final DataContainer container) {
        for (long r = 0; r < size; r++) {
            final long rowIndex = r;
            final var cells = new DataCell[columnDataFactories.length];
            for (var c = 0; c < cells.length; c++) {
                if (columnDataFactories[c].isMissing(rowIndex)) {
                    cells[c] = DataType.getMissingCell();
                } else {
                    cells[c] = columnDataFactories[c].getDataCellFactory().apply(r);
                }
            }
            container.addRowToTable(new DefaultRow(rowIDFactory.apply(r), cells));
        }
    }

    static DataTableSpec createSpec(final Column... columns) {
        return new DataTableSpec(createColumnSpecs(columns));
    }

    static DataColumnSpec[] createColumnSpecs(final Column... columns) {
        return Stream.of(columns)//
            .map(Column::getSpec)//
            .toArray(DataColumnSpec[]::new);
    }

    record Column(String name, ColumnDataFactory dataFactory) {

        DataColumnSpec getSpec() {
            final DataColumnSpecCreator specCreator = new DataColumnSpecCreator(name, dataFactory.type());
            final LongPredicate missingPredicate = dataFactory::isMissing;
            DataCell[] possibleValues = null;
            DataCell minValue = null;
            DataCell maxValue = null;
            if (dataFactory.type().equals(StringCell.TYPE)) {
                possibleValues = LongStream.range(0, dataFactory.size()) //
                        .filter(missingPredicate.negate()) //
                        .mapToObj(l -> dataFactory.getDataCellFactory().apply(l)) //
                        .toArray(DataCell[]::new);
            }
            if (dataFactory.type().equals(IntCell.TYPE) || dataFactory.type().equals(DoubleCell.TYPE)) {
                minValue = LongStream.range(0, dataFactory.size()) //
                        .filter(missingPredicate.negate()) //
                        .mapToObj(l -> dataFactory.getDataCellFactory().apply(l)) //
                        .min(dataFactory.type().getComparator()).orElse(null);
                maxValue = LongStream.range(0, dataFactory.size()) //
                        .filter(missingPredicate.negate()) //
                        .mapToObj(l -> dataFactory.getDataCellFactory().apply(l)) //
                        .max(dataFactory.type().getComparator()).orElse(null);
            }
            specCreator.setDomain(new DataColumnDomainCreator(possibleValues, minValue, maxValue).createDomain());
            return specCreator.createSpec();
        }
    }

    interface DataFactory {
        LongConsumer createMapper(final WriteValue<?> writeValue);

        LongFunction<DataCell> getDataCellFactory();

        int size();
    }

    interface ColumnDataFactory extends DataFactory {

        DataType type();

        boolean isMissing(long rowIndex);

    }

    private static final class ColumnDataFactoryImpl<W extends WriteValue<?>> implements ColumnDataFactory {

        private final DataType m_type;

        private final Function<W, LongConsumer> m_mapperFactory;

        private final LongFunction<DataCell> m_cellFunction;

        private final int m_size;

        private final LongPredicate m_isMissingIndex;

        ColumnDataFactoryImpl(final LongPredicate isMissingIndex, final Function<W, LongConsumer> mapperFactory,
            final LongFunction<DataCell> cellFunction, final int size, final DataType type) {
            m_type = type;
            m_size = size;
            m_mapperFactory = mapperFactory;
            m_cellFunction = cellFunction;
            m_isMissingIndex = isMissingIndex;
        }

        @Override
        public DataType type() {
            return m_type;
        }

        @Override
        public LongConsumer createMapper(final WriteValue<?> writeValue) {
            @SuppressWarnings("unchecked")
            var casted = (W)writeValue;
            return m_mapperFactory.apply(casted);
        }

        @Override
        public LongFunction<DataCell> getDataCellFactory() {
            return m_cellFunction;
        }

        @Override
        public int size() {
            return m_size;
        }

        @Override
        public boolean isMissing(final long rowIndex) {
            return m_isMissingIndex.test(rowIndex);
        }

    }

    private static LongPredicate nullIndicatesMissing(final Object[] values) {
        var missingIndices = IntStream.range(0, values.length)//
            .filter(i -> values[i] == null)//
            .boxed()//
            .collect(Collectors.toSet());
        return r -> missingIndices.contains((int)r);
    }

    static ColumnDataFactory intFactory(final Integer... values) {
        return new ColumnDataFactoryImpl<IntWriteValue>(nullIndicatesMissing(values),
            w -> r -> w.setIntValue(values[(int)r]), l -> new IntCell(values[(int)l]),  values.length, IntCell.TYPE);
    }

    static ColumnDataFactory stringFactory(final String... values) {
        return new ColumnDataFactoryImpl<StringWriteValue>(nullIndicatesMissing(values),
            w -> r -> w.setStringValue(values[(int)r]), l -> new StringCell(values[(int)l]), values.length,
            StringCell.TYPE);
    }

    static ColumnDataFactory doubleFactory(final Double... values) {
        return new ColumnDataFactoryImpl<DoubleWriteValue>(nullIndicatesMissing(values),
            w -> r -> w.setDoubleValue(values[(int)r]), l -> new DoubleCell(values[(int)l]), values.length,
            DoubleCell.TYPE);
    }

    static LongFunction<String> rowIDFactory(final String... values) {
        return r -> values[(int)r];
    }

}
