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
 *   14 Feb 2022 (albrecht): created
 */
package org.knime.testing.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.filestore.internal.NotInWorkflowDataRepository;
import org.knime.core.data.image.png.PNGImageCell;
import org.knime.core.data.image.png.PNGImageCellFactory;
import org.knime.core.data.property.ColorHandler;
import org.knime.core.data.vector.bitvector.SparseBitVectorCell;
import org.knime.core.data.vector.bitvector.SparseBitVectorCellFactory;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.Node;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.extension.NodeFactoryProvider;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.virtual.parchunk.VirtualParallelizedChunkPortObjectInNodeFactory;

/**
 * Unit test util class which can be used to create sample {@link BufferedDataTable}s.
 *
 * @author Christian Albrecht, KNIME GmbH, Konstanz, Germany
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @author Paul Bärnreuther
 */
public final class TableTestUtil {

    static {
        try {
            NodeFactoryProvider.getInstance();
        } catch (IllegalStateException e) { // NOSONAR
            // HACK to make tests work in the build system where the org.knime.workbench.repository plugin
            // is not present (causes an exception on the first call
            // 'Invalid extension point: org.knime.workbench.repository.nodes')
        }
    }

    private static final ExecutionContext EXEC = TableTestUtil.createTestExecutionContext();

    private static final DataTableSpec DEFAULT_SPEC = TableTestUtil.createDefaultTestSpec();

    /**
     * A builder for creating {@link DataTableSpec DataTableSpecs} for testing purposes.
     */
    public static final class SpecBuilder {

        private final List<DataColumnSpec> m_columnSpecs = new ArrayList<>();

        /**
         * Add a column with a given name and of a given type to this spec.
         *
         * @param name the name of the to-be-added column
         * @param type the type of the to-be-added column
         * @return this builder
         */
        public SpecBuilder addColumn(final String name, final DataType type) {
            return addColumn(name, type, null);
        }

        /**
         * Add a column with a given name, attached colorHandler and of a given type to this spec.
         *
         * @param name the name of the to-be-added column
         * @param type the type of the to-be-added column
         * @param colorHandler the color handler that is to be attached to the to-be-added column. Null if none.
         * @return this builder
         */
        public SpecBuilder addColumn(final String name, final DataType type, final ColorHandler colorHandler) {
            final var colSpecBuilder = new DataColumnSpecCreator(name, type);
            if (colorHandler != null) {
                colSpecBuilder.setColorHandler(colorHandler);
            }
            m_columnSpecs.add(colSpecBuilder.createSpec());
            return this;
        }

        /**
         * Finalizes the assembly of the spec.
         *
         * @return the fully assembled spec
         */
        public DataTableSpec build() {
            return new DataTableSpec(m_columnSpecs.toArray(new DataColumnSpec[0]));
        }
    }

    /**
     * A builder for creating {@link Supplier tables} and {@link BufferedDataTable} for testing purposes.
     */
    public static final class TableBuilder {

        private final BufferedDataContainer m_container;

        private final Function<Object, DataCell> m_cellify;

        private int runningRowId = 0;

        /**
         * Creates a new instance of a table builder.
         *
         * @param spec the spec of the to-be-assembled table
         */
        public TableBuilder(final DataTableSpec spec) {
            this(spec, TableTestUtil::cellify);
        }

        /**
         * Creates a new instance of a table builder.
         *
         * @param spec the spec of the to-be-assembled table
         * @param cellify function for converting {@link Object objects} into {@link DataCell data cells}.
         */
        public TableBuilder(final DataTableSpec spec, final Function<Object, DataCell> cellify) {
            m_container = EXEC.createDataContainer(spec, false, 0);
            m_cellify = cellify;
        }

        /**
         * Add another row to the table. Currently supported types include {@link DataCell DataCells}, {@link Integer},
         * {@link Long}, {@link Double}, {@link String}, {@link Boolean}, and null.
         *
         * Note: the rowId will be prefixed with 'rowkey'.
         *
         * @param cells the cells comprising the to-be-added row
         * @return this builder
         */
        public TableBuilder addRow(final Object... cells) {
            addRowWithId("rowkey " + runningRowId, cells);
            runningRowId++;
            return this;
        }

        /**
         * Add another row to the table. Currently supported types include {@link DataCell DataCells}, {@link Integer},
         * {@link Long}, {@link Double}, {@link String}, {@link Boolean}, and null.
         *
         * @param rowId id to use for the new row
         * @param cells the cells comprising the to-be-added row
         * @return this builder
         */
        public TableBuilder addRowWithId(final String rowId, final Object... cells) {
            m_container.addRowToTable(
                new DefaultRow(new RowKey(rowId), Arrays.stream(cells).map(m_cellify).toArray(DataCell[]::new)));
            return this;
        }

        /**
         * Finalizes the assembly of the table.
         *
         * @return the fully assembled table
         */
        public Supplier<BufferedDataTable> build() {
            m_container.close();
            return () -> m_container.getTable();
        }

        /**
         * Finalizes the assembly of the table.
         *
         * @return the fully assembled table as a BufferedDataTable
         */
        public BufferedDataTable buildDataTable() {
            m_container.close();
            return m_container.getTable();
        }
    }

    private static final Object[][] convertToObjectArray(final BufferedDataTable table, final String[] parseAs) {
        var out = new Object[parseAs.length][table.getRowCount()];
        try (final var cursor = table.cursor()) {
            for (int i = 0; cursor.canForward(); i++) {
                final var row = cursor.forward();
                assertThat(row.getNumColumns()).as("row length is not as expected").isEqualTo(parseAs.length);
                for (int j = 0; j < parseAs.length; j++) {
                    if (row.isMissing(j)) {
                        continue;
                    }
                    var value = row.getValue(j);
                    switch (parseAs[j]) {
                        case "Integer":
                            out[j][i] = ((IntValue)value).getIntValue();
                            break;
                        case "Double":
                            out[j][i] = ((DoubleValue)value).getDoubleValue();
                            break;
                        case "Long":
                            out[j][i] = ((LongValue)value).getLongValue();
                            break;
                        case "String":
                            out[j][i] = ((StringValue)value).getStringValue();
                            break;
                        default:
                            throw new UnsupportedOperationException(
                                String.format("Parsing to %s is not yet implemented", parseAs[j]));
                    }
                }
            }
        }
        return out;
    }

    /**
     * This method can be used to create numeric columns with custom domain bounds
     *
     * @param lowerBound
     * @param upperBound
     * @param name
     * @return a column spec with a given lowerBound, upperBound and name
     */
    public static DataColumnSpec createColumnSpecWithDomain(final double lowerBound, final double upperBound,
        final String name) {
        final var domainCreator = new DataColumnDomainCreator();
        domainCreator.setLowerBound(new DoubleCell(lowerBound));
        domainCreator.setUpperBound(new DoubleCell(upperBound));
        final var colSpecCreator = new DataColumnSpecCreator(name, DoubleCell.TYPE);
        colSpecCreator.setDomain(domainCreator.createDomain());
        return colSpecCreator.createSpec();
    }

    /**
     * @param lowerBound
     * @param upperBound
     * @return a column spec with a given lowerBound, upperBound
     */
    public static DataColumnSpec createColumnSpecWithDomain(final double lowerBound, final double upperBound) {
        return createColumnSpecWithDomain(lowerBound, upperBound, "domainCol");
    }

    /**
     * @return a column spec with no domain
     */
    public static DataColumnSpec createColumnWithNoDomain() {
        return new DataColumnSpecCreator("noDomainCol", DoubleCell.TYPE).createSpec();
    }

    /**
     * Asserts that a given table, if the columns are parsed with respect to given types, is as expected
     *
     * @param table the table that is to be checked
     * @param parseAs an array consisting of "Double" and "String".
     * @param expected the expected parsed array
     */
    public static final void assertTableResults(final BufferedDataTable table, final String[] parseAs,
        final Object[][] expected) {
        Object[][] parsedTable = convertToObjectArray(table, parseAs);
        assert parsedTable.length == expected.length;
        for (int i = 0; i < parsedTable.length; i++) {
            assert parsedTable[i].length == expected[i].length;
            for (int j = 0; j < parsedTable[i].length; j++) {
                if (parsedTable[i][j] == null) {
                    assertThat(expected[i][j]).as("missing value expected").isNull();
                } else {
                    assertThat(parsedTable[i][j]).as("different value expected").isEqualTo(expected[i][j]);
                }
            }
        }
    }

    /**
     * Asserts that a given table has the expected number of rows and columns
     *
     * @param table the table that is to be checked
     * @param expectedNumColumns the expected number of columns
     * @param expectedNumRows the expected number of rows
     */
    public static final void assertTableDimensions(final BufferedDataTable table, final int expectedNumColumns,
        final int expectedNumRows) {
        assert table.getSpec().getNumColumns() == expectedNumColumns;
        assert table.size() == expectedNumRows;
    }

    /**
     * Asserts that a given table has expected row keys
     *
     * @param table the table whose row keys are to be checked
     * @param expected the expected row keys
     */
    public static final void assertRowKeys(final BufferedDataTable table, final String[] expected) {
        RowKey[] rowKeys = getRowKeys(table);
        assert rowKeys.length == expected.length;
        for (int i = 0; i < rowKeys.length; i++) {
            assertThat(expected[i]).as(String.format("Row keys %s expected.", expected[i]))
                .isEqualTo(rowKeys[i].toString());
        }
    }

    /**
     * @param table
     * @return the row keys of the table in the order of the rows
     */
    public static final RowKey[] getRowKeys(final BufferedDataTable table) {
        List<RowKey> rk = new ArrayList<>();
        try (final var cursor = table.cursor()) {
            for (int i = 0; cursor.canForward(); i += 1) {
                final var row = cursor.forward();
                rk.add((RowKey)row.getRowKey());
            }
        }
        return rk.toArray(RowKey[]::new);
    }

    /**
     * Converts {@link Object objects} into {@link DataCell data cells}.
     *
     * @param obj the object to convert
     * @return the data cell resulting from the conversion
     */
    public static DataCell cellify(final Object obj) {
        if (obj instanceof DataCell) {
            return (DataCell)obj;
        } else if (obj == null) {
            return DataType.getMissingCell();
        } else if (obj instanceof Integer) {
            return new IntCell(((Integer)obj).intValue());
        } else if (obj instanceof Long) {
            return new LongCell(((Long)obj).longValue());
        } else if (obj instanceof Double) {
            return new DoubleCell(((Double)obj).doubleValue());
        } else if (obj instanceof String) {
            return new StringCell((String)obj);
        } else if (obj instanceof Boolean) {
            return BooleanCellFactory.create(((Boolean)obj).booleanValue());
        } else {
            throw new UnsupportedOperationException("not yet implemented");
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static ExecutionContext createTestExecutionContext() {
        return new ExecutionContext(new DefaultNodeProgressMonitor(),
            new Node((NodeFactory)new VirtualParallelizedChunkPortObjectInNodeFactory(new PortType[0])),
            SingleNodeContainer.MemoryPolicy.CacheSmallInMemory, NotInWorkflowDataRepository.newInstance());
    }

    private static DataTableSpec createDefaultTestSpec() {
        return new SpecBuilder().addColumn("int", IntCell.TYPE)//
            .addColumn("string", StringCell.TYPE)//
            .addColumn("long", LongCell.TYPE)//
            .addColumn("double", DoubleCell.TYPE)//
            .addColumn("bitvector", SparseBitVectorCell.TYPE)//
            .addColumn("boolean", BooleanCell.TYPE)//
            .addColumn("image", new PNGImageCellFactory().getDataType())//
            .build();
    }

    /**
     * @return the sample {@link ExecutionContext}
     */
    public static ExecutionContext getExec() {
        return EXEC;
    }

    /**
     * @return the sample {@link DataTableSpec}
     */
    public static DataTableSpec getDefaultTestSpec() {
        return DEFAULT_SPEC;
    }

    /**
     * Creates a {@link Supplier}, given a number of rows to create, which can be used for unit testing implementations
     * of view nodes.
     *
     * @param rowCount the number of rows to create which should be contained in the test table
     * @return a {@link Supplier} object containing the created table
     */
    public static Supplier<BufferedDataTable> createDefaultTestTable(final int rowCount) {
        return createDefaultTestTable(rowCount, idx -> idx);
    }

    /**
     * Creates a {@link Supplier}, given a number of rows to create, which can be used for unit testing implementations
     * of view nodes.
     *
     * @param rowCount the number of rows to create which should be contained in the test table
     * @param rowIndexToRandomImageSeedMap the random number generator seed to be used per row for the image column
     * @return a {@link Supplier} object containing the created table
     */
    public static Supplier<BufferedDataTable> createDefaultTestTable(final int rowCount,
        final LongFunction<Long> rowIndexToRandomImageSeedMap) {
        final var builder = new TableBuilder(DEFAULT_SPEC);
        IntStream.range(0, rowCount).mapToObj(i -> new Object[]{//
            new IntCell(i), //
            new StringCell(Integer.toString(i)), //
            new LongCell(i * 11), // multiply by 11 to get 2-digit repdigit
            new DoubleCell(i), //
            new SparseBitVectorCellFactory(Integer.toHexString(i)).createDataCell(), //
            i % 2 == 1 ? BooleanCell.TRUE : BooleanCell.FALSE, //
            createPNGImageCell(rowIndexToRandomImageSeedMap.apply(i))}).forEach(builder::addRow);
        return builder.build();
    }

    /**
     * Creates a {@link Supplier} containing several tables with identical {@link DataTableSpec}.
     *
     * @param spec the common {@link DataTableSpec} used to create the tables.
     * @param rows an array containing the {@link DataCell} instances which should be contained in the test table. It is
     *            three-dimensional, since multiple DataCells are combined to a row, multiple rows are combined to a
     *            table and multiple tables are returned.
     * @return a {@link Supplier} object containing the created table
     */
    public static Supplier<BufferedDataTable[]> createIdenticalSpecTable(final DataTableSpec spec,
        final DataCell[][][] rows) {
        BufferedDataTable[] result = new BufferedDataTable[rows.length];
        for (int i = 0; i < rows.length; i++) {
            var newTable = new TableBuilder(spec);
            for (var row : rows[i]) {
                Arrays.stream(row).forEach(newTable::addRow);
            }
            result[i] = newTable.buildDataTable();
        }
        return () -> result;
    }

    private TableTestUtil() {
        //utility class
    }

    /**
     * @param seed the initial seed for the random number generator (for deterministic random images)
     * @return a {@link PNGImageCell} containing a small png image
     */
    public static DataCell createPNGImageCell(final long seed) {
        var img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        var rand = new Random(seed);
        for (var x = 0; x < img.getHeight(); x++) {
            for (var y = 0; y < img.getWidth(); y++) {
                var val = rand.nextInt(256);
                int p = (0 << 24) | (val << 16) | (val << 8) | val;
                img.setRGB(x, y, p);
            }
        }

        var out = new ByteArrayOutputStream();
        try {
            ImageIO.write(img, "png", out);
            return PNGImageCellFactory.create(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e); // NOSONAR
        }
    }

    /**
     * A class containing necessary column information to build a BufferedDataTable from multiple of its instances.
     *
     * @author BaernreutherPaul
     */
    public static final class ObjectColumn {

        /**
         * the name of the column
         */
        private final String m_name;

        /**
         * the {@link DataType} of the column
         */
        private final DataType m_type;

        /**
         * the source of the row values of the column
         */
        private final Object[] m_data;

        /**
         * the color handler that should be attached to the columns. Null if none
         */
        private final ColorHandler m_colorHandler;

        /**
         * @param name the name of the column
         * @param type the {@link DataType} of the column
         * @param data the source of the row values of the column
         */
        public ObjectColumn(final String name, final DataType type, final Object[] data) {
            this(name, type, null, data);
        }

        /**
         * @param name the name of the column
         * @param type the {@link DataType} of the column
         * @param colorHandler the color handler that should be attached to the columns. Null if none
         * @param data the source of the row values of the column
         */
        public ObjectColumn(final String name, final DataType type, final ColorHandler colorHandler,
            final Object[] data) {
            m_name = name;
            m_type = type;
            m_colorHandler = colorHandler;
            m_data = data;
        }
    }

    /**
     * @param objectColumns an array of columns of equal length
     * @return a {@link BufferedDataTable} consisting of the given columns
     */
    public static BufferedDataTable createTableFromColumns(final ObjectColumn... objectColumns) {
        return createTableFromColumns(TableTestUtil::cellify, objectColumns);
    }

    /**
     * @param cellify function for converting {@link Object objects} into {@link DataCell data cells}.
     * @param objectColumns an array of columns of equal length
     * @return a {@link BufferedDataTable} consisting of the given columns
     */
    public static BufferedDataTable createTableFromColumns(final Function<Object, DataCell> cellify,
        final ObjectColumn... objectColumns) {
        final var columnList = new ArrayList<ObjectColumn>(Arrays.asList(objectColumns));
        final var specBuilder = new SpecBuilder();
        columnList.forEach(col -> specBuilder.addColumn(col.m_name, col.m_type, col.m_colorHandler));
        final var spec = specBuilder.build();
        final var builder = new TableBuilder(spec, cellify);
        if (!columnList.isEmpty()) {
            Integer nRows = columnList.get(0).m_data.length;
            columnList.stream().forEach(col -> {
                if (col.m_data.length != nRows) {
                    throw new IllegalArgumentException("Columns need to be of the same length");
                }
            });
            IntStream.range(0, nRows).mapToObj(i -> columnList.stream().map(col -> col.m_data[i]).toArray())
                .forEach(builder::addRow);
        }
        return builder.build().get();
    }

}
