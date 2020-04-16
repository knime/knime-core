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
 */
package org.knime.core.data.container;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.filter.TableFilter;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.filestore.internal.NotInWorkflowDataRepository;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTable.KnowsRowCountTable;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.Node;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeProgressMonitor;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.virtual.parchunk.VirtualParallelizedChunkPortObjectInNodeFactory;

/**
 * Unit test for {@link KnowsRowCountTable#iteratorWithFilter(TableFilter)}). Tests that filters are correctly
 * transformed and applied for {@link KnowsRowCountTable KnowsRowCountTables} overriding
 * {@link KnowsRowCountTable#iteratorWithFilter(TableFilter)}, i.e., {@link RearrangeColumnsTable
 * RearrangeColumnsTables}, {@link JoinedTable JoinedTables}, and {@link ConcatenateTable ConcatenateTables}.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public class KnowsRowCountTableIteratorWithFilterTest {


    private static final NotInWorkflowDataRepository REPO = NotInWorkflowDataRepository.newInstance();

    private static final NodeProgressMonitor PROGRESS = new DefaultNodeProgressMonitor();

    private static final VirtualParallelizedChunkPortObjectInNodeFactory FACTORY =
        new VirtualParallelizedChunkPortObjectInNodeFactory(new PortType[0]);

    private static final Node NODE = new Node((NodeFactory)FACTORY);

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final ExecutionContext EXEC =
        new ExecutionContext(PROGRESS, NODE, SingleNodeContainer.MemoryPolicy.CacheSmallInMemory, REPO);

    private static BufferedDataTable createTable(final int columnsFrom, final int columnsTo, final int offset,
        final int rowsFrom, final int rowsTo) {

        final DataTableSpec spec = new DataTableSpec(IntStream.range(columnsFrom, columnsTo)
            .mapToObj(i -> new DataColumnSpecCreator(Integer.toString(i), IntCell.TYPE).createSpec())
            .toArray(DataColumnSpec[]::new));

        final DataRow[] rows = IntStream.range(rowsFrom, rowsTo)//
            .mapToObj(i -> new DefaultRow(RowKey.createRowKey((long)i), //
                IntStream.range(columnsFrom, columnsTo)//
                    .mapToObj(j -> new IntCell(i * offset + j))//
                    .toArray(DataCell[]::new)))//
            .toArray(DataRow[]::new);

        final BufferedDataContainer cont = EXEC.createDataContainer(spec);

        // write the data
        for (final DataRow r : rows) {
            cont.addRowToTable(r);
        }

        cont.close();
        return cont.getTable();

    }

    private static TableFilter createFilter(final int columnsFrom, final int columnsTo, final long rowsFrom,
        final long rowsTo) {
        final TableFilter.Builder builder = ((new TableFilter.Builder())//
            .withMaterializeColumnIndices(IntStream.range(columnsFrom, columnsTo + 1).toArray())//
            .withFromRowIndex(rowsFrom)//
            .withToRowIndex(rowsTo));

        return builder.build();
    }

    /**
     * Tests that {@link TableFilter TableFilters} are correctly transformed and applied to {@link JoinedTable
     * JoinedTables}.
     *
     * @throws Exception any kind of exception
     */
    @Test
    public void testJoinedTable() throws Exception {

        final BufferedDataTable fullTable = createTable(0, 16, 16, 0, 16);

        final BufferedDataTable leftTable = createTable(0, 8, 16, 0, 16);
        final BufferedDataTable rightTable = createTable(8, 16, 16, 0, 16);
        final BufferedDataTable concatenateTable = EXEC.createJoinedTable(leftTable, rightTable, EXEC);

        final TableFilter filter = createFilter(4, 12, 4, 12);

        compareTables(fullTable, concatenateTable, filter);

        compareTables(fullTable, concatenateTable, createFilter(4, 12, 4, 12));

        compareTables(fullTable, concatenateTable, createFilter(4, 6, 4, 12));

        compareTables(fullTable, concatenateTable, createFilter(10, 12, 4, 12));

    }

    /**
     * Tests that {@link TableFilter TableFilters} are correctly transformed and applied to permuted
     * {@link RearrangeColumnsTable RearrangeColumnsTables}.
     *
     * @throws Exception any kind of exception
     */
    @Test
    public void testColumnRearrangeTablePermute() throws Exception {

        final BufferedDataTable table = createTable(0, 8, 8, 0, 16);

        ColumnRearranger rearranger = new ColumnRearranger(table.getSpec());
        rearranger.permute(new int[]{7, 6, 5, 4, 3, 2, 1, 0});
        final BufferedDataTable rearrangedTable = EXEC.createColumnRearrangeTable(table, rearranger, EXEC);
        rearranger = new ColumnRearranger(rearrangedTable.getSpec());
        rearranger.permute(new int[]{7, 6, 5, 4, 3, 2, 1, 0});
        final BufferedDataTable reRearrangedTable = EXEC.createColumnRearrangeTable(rearrangedTable, rearranger, EXEC);

        final TableFilter filter = createFilter(2, 6, 4, 12);
        compareTables(table, reRearrangedTable, filter);

    }

    /**
     * Tests that {@link TableFilter TableFilters} are correctly transformed and applied to {@link RearrangeColumnsTable
     * RearrangeColumnsTables} with removed and appended columns.
     *
     * @throws Exception any kind of exception
     */
    @Test
    public void testColumnRearrangeTableDeleteAdd() throws Exception {

        final BufferedDataTable table = createTable(0, 8, 8, 0, 16);

        ColumnRearranger rearranger = new ColumnRearranger(table.getSpec());
        rearranger.remove(4, 5, 6, 7);
        final BufferedDataTable shortenedTable = EXEC.createColumnRearrangeTable(table, rearranger, EXEC);

        rearranger = new ColumnRearranger(shortenedTable.getSpec());
        final BufferedDataTable appendTable = createTable(4, 8, 8, 0, 16);
        final Map<RowKey, DataCell[]> cellsByRowKey = new HashMap<>();
        for (final DataRow row : appendTable) {
            cellsByRowKey.put(row.getKey(),
                IntStream.range(0, row.getNumCells()).mapToObj(i -> row.getCell(i)).toArray(DataCell[]::new));
        }

        rearranger.append(new CellFactory() {

            @Override
            public DataCell[] getCells(final DataRow row) {
                return cellsByRowKey.get(row.getKey());
            }

            @Override
            public DataColumnSpec[] getColumnSpecs() {
                return IntStream.range(0, 4).mapToObj(i -> appendTable.getDataTableSpec().getColumnSpec(i))
                    .toArray(DataColumnSpec[]::new);
            }

            @Deprecated
            @Override
            public void setProgress(final int curRowNr, final int rowCount, final RowKey lastKey,
                final ExecutionMonitor exec) {
            }

            @Override
            public void setProgress(final long curRowNr, final long rowCount, final RowKey lastKey,
                final ExecutionMonitor exec) {
            }

        });

        final BufferedDataTable restoredTable = EXEC.createColumnRearrangeTable(shortenedTable, rearranger, EXEC);

        compareTables(table, restoredTable, createFilter(2, 6, 4, 12));

        compareTables(table, restoredTable, createFilter(1, 2, 4, 12));

        compareTables(table, restoredTable, createFilter(6, 7, 4, 12));

    }

    /**
     * Tests that {@link TableFilter TableFilters} are correctly transformed and applied to {@link ConcatenateTable
     * ConcatenateTables} with removed and appended columns.
     *
     * @throws Exception any kind of exception
     */
    @Test
    public void testConcatenateTable() throws Exception {

        final BufferedDataTable fullTable = createTable(0, 16, 16, 0, 16);

        final BufferedDataTable topTable = createTable(0, 16, 16, 0, 8);
        final BufferedDataTable bottomTable = createTable(0, 16, 16, 8, 16);
        final BufferedDataTable concatenateTable =
            EXEC.createConcatenateTable(new ExecutionMonitor(PROGRESS), topTable, bottomTable);

        compareTables(fullTable, concatenateTable, createFilter(4, 12, 4, 12));

        compareTables(fullTable, concatenateTable, createFilter(4, 12, 4, 6));

        compareTables(fullTable, concatenateTable, createFilter(4, 12, 10, 12));

        compareTables(fullTable, concatenateTable, createFilter(4, 12, 7, 7));

        compareTables(fullTable, concatenateTable, createFilter(4, 12, 8, 8));

        compareTables(fullTable, concatenateTable, createFilter(4, 12, 0, 0));

        compareTables(fullTable, concatenateTable, createFilter(4, 12, 15, 15));

    }

    private static void compareTables(final BufferedDataTable table1, final BufferedDataTable table2,
        final TableFilter filter) {
        compareTables(table1, table2, filter, filter);
    }

    private static void compareTables(final BufferedDataTable table1, final BufferedDataTable table2,
        final TableFilter filter1, final TableFilter filter2) {
        try (final CloseableRowIterator rowIt1 = table1.filter(filter1).iterator();
                final CloseableRowIterator rowIt2 = table2.filter(filter2).iterator()) {
            while (rowIt1.hasNext() && rowIt2.hasNext()) {
                final DataRow row1 = rowIt1.next();
                final DataRow row2 = rowIt2.next();
                assertEquals(row1.getKey(), row2.getKey());
                assertEquals(row1.getNumCells(), row2.getNumCells());
                for (int i = 0; i < row1.getNumCells(); i++) {
                    final DataCell cell1 = row1.getCell(i);
                    final DataCell cell2 = row2.getCell(i);
                    assertEquals(cell1, cell2);
                }
            }
            assertEquals(rowIt1.hasNext(), rowIt2.hasNext());
        }
    }

}
