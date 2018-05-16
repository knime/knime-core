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
 *   01.05.2018 (Jonathan Hale): created
 */
package org.knime.core.data.convert;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterFactory;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterRegistry;
import org.knime.core.data.convert.java.DataCellToJavaConverterFactory;
import org.knime.core.data.convert.java.DataCellToJavaConverterRegistry;
import org.knime.core.data.convert.map.AbstractSink;
import org.knime.core.data.convert.map.AbstractSource;
import org.knime.core.data.convert.map.ConsumptionPath;
import org.knime.core.data.convert.map.MappingFramework;
import org.knime.core.data.convert.map.MappingFramework.CellValueConsumer;
import org.knime.core.data.convert.map.MappingFramework.CellValueProducer;
import org.knime.core.data.convert.map.MappingFramework.ProducerConsumerRegistry;
import org.knime.core.data.convert.map.MappingFramework.UnmappableTypeException;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.data.convert.map.Sink;
import org.knime.core.data.convert.map.Source;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Test for the {@link MappingFramework}.
 *
 * Note: This test uses {@link H2OSink}, {@link H2OSource}, {@link H2OParameters} and {@link H2OConsumer} as dummy names
 * rather than <code>Test*</code> for better readability. This are only mocks, though.
 *
 * @author Jonathan Hale
 */
public class MappingFrameworkTest {

    /**
     * Implementation for H2O {@link org.knime.core.data.convert.map.Sink.ConsumerParameters}.
     *
     * Parameters used for both consumers and producers.
     *
     * @author Jonathan Hale
     */
    protected static class H2OParameters
        implements Sink.ConsumerParameters<H2OSink>, Source.ProducerParameters<H2OSource> {

        /** Index of the column to read from or write to */
        public int columnIndex;

        @Override
        public void saveSettingsTo(final NodeSettingsWO settings) {
            settings.addInt("columnIndex", columnIndex);
        }

        @Override
        public void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
            columnIndex = settings.getInt("columnIndex");
        }
    }

    /**
     * Implementation for sinking stuff into "pseudo-H2O".
     *
     * @author Jonathan Hale
     */
    protected static class H2OSink extends AbstractSink<H2OSink, H2OParameters> {
        /** Represents the destiny for all the data */
        public Object[] h2oFrame;

        /** Number of rows received */
        public int numRows = 0;

        /**
         * Constructor
         *
         * @param spec Table spec to initialize the H2O frame with.
         * @throws UnmappableTypeException If any of the columns contains an unmappable type.
         */
        public H2OSink(final DataTableSpec spec) throws UnmappableTypeException {
            /* The class is used to find the Consumers associated with this Sink */
            super(H2OSink.class, spec);

            /* Initialization of connections etc either happens here or can be passed to the constructor. */
            h2oFrame = new Object[spec.getNumColumns()];
        }

        @Override
        public void finishRow() {
            numRows++;
        }

        @Override
        public List<H2OParameters> createSinkParameters(final DataTableSpec spec) {
            final ArrayList<H2OParameters> params = new ArrayList<>();

            for (int i = 0; i < spec.getNumColumns(); ++i) {
                //final DataColumnSpec columnSpec = spec.getColumnSpec(i);

                final H2OParameters p = new H2OParameters();
                p.columnIndex = i;
                params.add(p);
            }
            return params;
        }
    }

    /**
     * Implementation for source stuff from "pseudo-H2O":
     *
     * @author Jonathan Hale
     */
    protected static class H2OSource extends AbstractSource<H2OSource, H2OParameters> {

        /**
         * @param spec
         * @param context
         * @throws UnmappableTypeException
         */
        public H2OSource(final DataTableSpec spec, final ExecutionContext context) throws UnmappableTypeException {
            super(H2OSource.class, spec, context);
        }

        final ArrayList<Object[]> h2oFrame = new ArrayList<Object[]>();

        int curRow = -1;

        @Override
        protected List<H2OParameters> createParameters(final DataTableSpec spec) {
            final ArrayList<H2OParameters> params = new ArrayList<>();

            for (int i = 0; i < spec.getNumColumns(); ++i) {
                //final DataColumnSpec columnSpec = spec.getColumnSpec(i);

                final H2OParameters p = new H2OParameters();
                p.columnIndex = i;
                params.add(p);
            }
            return params;
        }

        @Override
        protected boolean hasMoreRows() {
            return curRow + 1 < h2oFrame.size();
        }

        @Override
        protected int getNumColumns() {
            return h2oFrame.size() > 0 ? h2oFrame.get(0).length : 0;
        }

        @Override
        protected RowKey getRowKey() {
            return RowKey.createRowKey((long)curRow);
        }

        /**
         * Get value from current row
         *
         * @param columnIndex Column index
         * @return Object at column index
         */
        @SuppressWarnings("unchecked")
        public <T> T get(final int columnIndex) {
            return (T)h2oFrame.get(curRow)[columnIndex];
        }

        @Override
        protected void nextRow() {
            curRow++;
        }
    }

    /**
     * Interface to clean up consumer declarations
     *
     * @param <T> Java type to which the consumer should convert to.
     */
    public static interface H2OConsumer<T> extends CellValueConsumer<H2OSink, T, H2OParameters> {
    }

    /**
     * Interface to clean up producer declarations
     *
     * @param <T> Java type to which the consumer should convert to.
     */
    public static interface H2OProducer<T> extends CellValueProducer<H2OSource, T, H2OParameters> {
    }

    /**
     * Tests {@link ConsumptionPath#equals} and {@link ConsumptionPath#toString()}
     */
    @Test
    public void consumptionPaths() {
        final H2OConsumer<Integer> intConsumer = (c, v, p) -> {
            c.h2oFrame[p.columnIndex] = v;
        };
        final DataCellToJavaConverterFactory<? extends DataValue, Integer> intFactory = DataCellToJavaConverterRegistry
            .getInstance().getConverterFactories(IntCell.TYPE, Integer.class).stream().findFirst().get();
        final DataCellToJavaConverterFactory<? extends DataValue, String> stringFactory = DataCellToJavaConverterRegistry
            .getInstance().getConverterFactories(StringCell.TYPE, String.class).stream().findFirst().get();
        final ConsumptionPath pathA = new ConsumptionPath(intFactory, intConsumer);
        final ConsumptionPath pathB = new ConsumptionPath(intFactory, intConsumer);
        final ConsumptionPath pathC = new ConsumptionPath(stringFactory, intConsumer);
        final ConsumptionPath pathD = new ConsumptionPath(null, null);
        final ConsumptionPath pathE = new ConsumptionPath(null, intConsumer);

        assertTrue("ConversionPath should equal itself", pathA.equals(pathA));
        assertTrue(pathA.equals(pathB));
        assertTrue(pathB.equals(pathA));
        assertTrue(pathD.equals(pathD));
        assertFalse(pathA.equals(pathC));
        assertFalse(pathA.equals(new Integer(42)));
        assertFalse(pathA.equals(pathD));
        assertFalse(pathE.equals(pathD));
        assertFalse(pathD.equals(pathE));

        assertEquals("IntValue --(\"Integer\")-> Integer ---> MappingFrameworkTest$$Lambda$<Number> Consumer",
            pathA.toString().split("$$Lambda$")[0].replaceAll("\\d+/\\d+", "<Number>"));

        assertEquals(pathA.hashCode(), pathA.hashCode());
        assertEquals(pathA.hashCode(), pathB.hashCode());
        assertNotEquals(pathA.hashCode(), pathC.hashCode());
    }

    /**
     * Tests {@link ProductionPath#equals} and {@link ProductionPath#toString()}
     */
    @Test
    public void productionPaths() {
        final H2OProducer<Integer> intProducer = (c, p) -> {
            return c.get(p.columnIndex);
        };
        final JavaToDataCellConverterFactory<Integer> intFactory = JavaToDataCellConverterRegistry.getInstance()
            .getConverterFactories(Integer.class, IntCell.TYPE).stream().findFirst().get();
        final JavaToDataCellConverterFactory<String> stringFactory = JavaToDataCellConverterRegistry.getInstance()
            .getConverterFactories(String.class, StringCell.TYPE).stream().findFirst().get();
        final ProductionPath pathA = new ProductionPath(intFactory, intProducer);
        final ProductionPath pathB = new ProductionPath(intFactory, intProducer);
        final ProductionPath pathC = new ProductionPath(stringFactory, intProducer);
        final ProductionPath pathD = new ProductionPath(null, null);
        final ProductionPath pathE = new ProductionPath(null, intProducer);

        assertTrue("ConversionPath should equal itself", pathA.equals(pathA));
        assertTrue(pathA.equals(pathB));
        assertTrue(pathB.equals(pathA));
        assertTrue(pathD.equals(pathD));
        assertFalse(pathA.equals(pathC));
        assertFalse(pathA.equals(new Integer(42)));
        assertFalse(pathA.equals(pathD));
        assertFalse(pathE.equals(pathD));
        assertFalse(pathD.equals(pathE));

        assertEquals("Integer --(\"Integer\")-> Number (integer) ---> MappingFrameworkTest$$Lambda$<Number> Consumer",
            pathA.toString().split("$$Lambda$")[0].replaceAll("\\d+/\\d+", "<Number>"));

        assertEquals(pathA.hashCode(), pathA.hashCode());
        assertEquals(pathA.hashCode(), pathB.hashCode());
        assertNotEquals(pathA.hashCode(), pathC.hashCode());
    }

    /**
     * @throws UnmappableTypeException
     */
    @Test
    public void consumerTest() throws UnmappableTypeException {
        /* One time setup */
        final H2OConsumer<String> stringConsumer = (c, v, p) -> {
            c.h2oFrame[p.columnIndex] = v;
        };
        final H2OConsumer<Integer> intConsumer = (c, v, p) -> {
            c.h2oFrame[p.columnIndex] = v;
        };
        final H2OConsumer<Object> generalConsumer = (c, v, p) -> {
            c.h2oFrame[p.columnIndex] = v;
        };

        ProducerConsumerRegistry.forSinkType(H2OSink.class).unregisterAllConsumers()
            .registerConsumer(Integer.class, intConsumer) //
            .registerConsumer(Long.class, generalConsumer) //
            .registerConsumer(Float.class, generalConsumer) //
            .registerConsumer(Double.class, generalConsumer)
            .registerConsumer(String.class, stringConsumer) //
            .registerConsumer(Integer.class, stringConsumer); // try to re-register (should not override)

        assertEquals(intConsumer, ProducerConsumerRegistry.forSinkType(H2OSink.class).get(Integer.class));

        final Collection<ConsumptionPath> paths =
            ProducerConsumerRegistry.forSinkType(H2OSink.class).getAvailableConsumptionPaths(IntCell.TYPE);
        assertEquals(4, paths.size());
        final ConsumptionPath expectedPath = new ConsumptionPath(DataCellToJavaConverterRegistry.getInstance()
            .getConverterFactories(IntCell.TYPE, Integer.class).stream().findFirst().get(), intConsumer);
        assertTrue(paths.contains(expectedPath));

        assertNull(ProducerConsumerRegistry.forSinkType(H2OSink.class).get(ConsumptionPath.class));

        /* Some example input */
        final DefaultRow row =
            new DefaultRow(RowKey.createRowKey(0L), new StringCell("KNIME"), new IntCell(42), new LongCell(42L));
        final DataTableSpec spec = new DataTableSpec(new String[]{"String", "Integer", "Long"},
            new DataType[]{StringCell.TYPE, IntCell.TYPE, LongCell.TYPE});

        /* Create a table */
        final DataContainer container = new DataContainer(spec);
        container.addRowToTable(row);
        container.close();
        final DataTable table = container.getTable();

        final H2OSink testSink = new H2OSink(spec);
        testSink.addTable(table);

        assertEquals(3, testSink.h2oFrame.length);
        assertEquals(1, testSink.numRows);
        assertArrayEquals(new Object[]{"KNIME", new Integer(42), new Long(42L)}, testSink.h2oFrame);
    }

    /**
     * @throws UnmappableTypeException
     */
    @Test
    public void producerTest() throws UnmappableTypeException {
        /* One time setup */
        final H2OProducer<String> stringProducer = (s, p) -> {
            return s.get(p.columnIndex);
        };
        final H2OProducer<Integer> intProducer = (s, p) -> {
            return s.get(p.columnIndex);
        };
        final H2OProducer<Long> longProducer = (s, p) -> {
            return s.get(p.columnIndex);
        };

        ProducerConsumerRegistry.forSourceType(H2OSource.class) //
            .unregisterAllProducers() //
            .registerProducer(Integer.class, intProducer) //
            .registerProducer(Long.class, longProducer) //
            .registerProducer(String.class, stringProducer) //
            .registerProducer(Integer.class, stringProducer); // try to re-register (should not override)

        assertEquals(intProducer, ProducerConsumerRegistry.forSourceType(H2OSource.class).get(Integer.class));

        final Collection<ProductionPath> paths =
            ProducerConsumerRegistry.forSourceType(H2OSource.class).getAvailableProductionPaths(IntCell.TYPE);
        assertEquals(1, paths.size());
        final ProductionPath expectedPath = new ProductionPath(JavaToDataCellConverterRegistry.getInstance()
            .getConverterFactories(Integer.class, IntCell.TYPE).stream().findFirst().get(), intProducer);
        assertTrue(paths.contains(expectedPath));

        assertNull(ProducerConsumerRegistry.forSinkType(H2OSink.class).get(ConsumptionPath.class));

        /* Some example input */
        final DataTableSpec spec = new DataTableSpec(new String[]{"String", "Integer", "Long"},
            new DataType[]{StringCell.TYPE, IntCell.TYPE, LongCell.TYPE});

        /* Create a table */
        {
            final DataContainer container = new DataContainer(spec);

            final H2OSource testSource = new H2OSource(spec, null);
            testSource.addToContainer(container);
            container.close();

            final DataTable table = container.getTable();

            assertEquals(spec.getNumColumns(), table.getDataTableSpec().getNumColumns());
            assertFalse(table.iterator().hasNext());
        }
        {
            final DataContainer container = new DataContainer(spec);

            final H2OSource testSource = new H2OSource(spec, null);
            testSource.h2oFrame.add(new Object[]{"KNIME", new Integer(42), new Long(42L)});
            testSource.addToContainer(container);
            container.close();

            final DataTable table = container.getTable();

            assertEquals(spec.getNumColumns(), table.getDataTableSpec().getNumColumns());
            assertTrue(table.iterator().hasNext());

            final DataRow row = table.iterator().next();

            assertEquals(StringCell.TYPE, row.getCell(0).getType());
            assertEquals("KNIME", ((StringValue)row.getCell(0)).getStringValue());

            assertEquals(IntCell.TYPE, row.getCell(1).getType());
            assertEquals(42, ((IntValue)row.getCell(1)).getIntValue());

            assertEquals(LongCell.TYPE, row.getCell(2).getType());
            assertEquals(42L, ((LongValue)row.getCell(2)).getLongValue());
        }
    }

    /**
     *
     */
    @Test
    public void unmappableType() {
        ProducerConsumerRegistry.forSourceType(H2OSource.class).unregisterAllProducers();
        assertNull(ProducerConsumerRegistry.forSourceType(H2OSource.class).get(Integer.class));
        try {
            new H2OSource(new DataTableSpec(new String[]{"Unmappable"}, new DataType[]{IntCell.TYPE}), null);
            fail("Excpected UnmappableTypeException");
        } catch (UnmappableTypeException e) {
            // TODO: test message
        }

        ProducerConsumerRegistry.forSinkType(H2OSink.class).unregisterAllConsumers();
        assertNull(ProducerConsumerRegistry.forSinkType(H2OSink.class).get(Integer.class));
        try {
            new H2OSink(new DataTableSpec(new String[]{"Unmappable"}, new DataType[]{IntCell.TYPE}));
            fail("Excpected UnmappableTypeException");
        } catch (UnmappableTypeException e) {
            // TODO: test message
        }
    }
}