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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.Test;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.MissingCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterFactory;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterRegistry;
import org.knime.core.data.convert.java.DataCellToJavaConverterFactory;
import org.knime.core.data.convert.java.DataCellToJavaConverterRegistry;
import org.knime.core.data.convert.map.CellValueConsumer;
import org.knime.core.data.convert.map.CellValueConsumerFactory;
import org.knime.core.data.convert.map.CellValueProducer;
import org.knime.core.data.convert.map.CellValueProducerFactory;
import org.knime.core.data.convert.map.ConsumptionPath;
import org.knime.core.data.convert.map.Destination;
import org.knime.core.data.convert.map.MappingFramework;
import org.knime.core.data.convert.map.ProducerRegistry;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.data.convert.map.SimpleCellValueConsumerFactory;
import org.knime.core.data.convert.map.SimpleCellValueProducerFactory;
import org.knime.core.data.convert.map.Source;
import org.knime.core.data.convert.util.SerializeUtil;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.config.Config;

/**
 * Test for the {@link MappingFramework}.
 *
 * Note: This test uses {@link H2ODestination}, {@link H2OSource}, {@link H2OParameters} and {@link H2OConsumer} as
 * dummy names rather than <code>Test*</code> for better readability. This are only mocks, though.
 *
 * @author Jonathan Hale
 */
public class MappingFrameworkTest {

    /**
     * Implementation for H2O {@link org.knime.core.data.convert.map.Destination.ConsumerParameters}.
     *
     * Parameters used for both consumers and producers.
     *
     * @author Jonathan Hale
     */
    protected static class H2OParameters
        implements Destination.ConsumerParameters<H2ODestination>, Source.ProducerParameters<H2OSource> {

        /** Index of the column to read from or write to */
        public int columnIndex;

        /** Index of current row to read to or write from */
        public int rowIndex;
    }

    /**
     * Implementation for sinking stuff into "pseudo-H2O".
     *
     * @author Jonathan Hale
     */
    protected static class H2ODestination implements Destination<String> {
        /** Represents the destiny for all the data */
        public ArrayList<Object[]> h2oFrame = new ArrayList<>();
    }

    /**
     * Implementation for source stuff from "pseudo-H2O":
     *
     * @author Jonathan Hale
     */
    protected static class H2OSource implements Source<String> {

        final ArrayList<Object[]> h2oFrame = new ArrayList<Object[]>();
    }

    /**
     * Interface to clean up consumer declarations
     *
     * @param <T> Java type to which the consumer should convert to.
     */
    public static interface H2OConsumer<T> extends CellValueConsumer<H2ODestination, T, H2OParameters> {
    }

    /**
     * Interface to clean up producer declarations
     *
     * @param <T> Java type to which the consumer should convert to.
     */
    public static interface H2OProducer<T> extends CellValueProducer<H2OSource, T, H2OParameters> {
    }

    final SimpleCellValueProducerFactory<H2OSource, String, String, H2OParameters> stringProducer =
        new SimpleCellValueProducerFactory<>("STR", String.class, (c, p) -> {
            return (String)c.h2oFrame.get(p.rowIndex)[p.columnIndex];
        });

    final SimpleCellValueConsumerFactory<H2ODestination, String, String, H2OParameters> stringConsumer =
        new SimpleCellValueConsumerFactory<>(String.class, "STR", (c, v, p) -> {
            c.h2oFrame.get(p.rowIndex)[p.columnIndex] = v;
        });

    final SimpleCellValueConsumerFactory<H2ODestination, Integer, String, H2OParameters> intConsumer =
        new SimpleCellValueConsumerFactory<>(Integer.class, "INT", (c, v, p) -> {
            c.h2oFrame.get(p.rowIndex)[p.columnIndex] = v;
        });

    final SimpleCellValueProducerFactory<H2OSource, String, Integer, H2OParameters> intProducer =
        new SimpleCellValueProducerFactory<>("INT", Integer.class, (c, p) -> {
            return (Integer)c.h2oFrame.get(p.rowIndex)[p.columnIndex];
        });

    /**
     * Tests {@link ConsumptionPath#equals} and {@link ConsumptionPath#toString()}
     */
    @Test
    public void consumptionPaths() {
        final DataCellToJavaConverterFactory<? extends DataValue, Integer> intFactory = DataCellToJavaConverterRegistry
            .getInstance().getConverterFactories(IntCell.TYPE, Integer.class).stream().findFirst().get();
        final DataCellToJavaConverterFactory<? extends DataValue, String> stringFactory =
            DataCellToJavaConverterRegistry.getInstance().getConverterFactories(StringCell.TYPE, String.class).stream()
                .findFirst().get();

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
        assertFalse(pathE.equals(pathA));
        assertFalse(pathA.equals(null));

        assertEquals("IntValue --(\"Integer\")-> Integer ---> INT", pathA.toString());

        assertEquals(pathA.hashCode(), pathA.hashCode());
        assertEquals(pathA.hashCode(), pathB.hashCode());
        assertNotEquals(pathA.hashCode(), pathC.hashCode());

        assertEquals(pathA.getConsumerFactory(), pathA.m_consumerFactory);
        assertEquals(pathA.getConverterFactory(), pathA.m_converterFactory);
    }

    /**
     * Tests {@link ProductionPath#equals} and {@link ProductionPath#toString()}
     */
    @Test
    public void productionPaths() {
        final JavaToDataCellConverterFactory<Integer> intFactory = JavaToDataCellConverterRegistry.getInstance()
            .getConverterFactories(Integer.class, IntCell.TYPE).stream().findFirst().get();
        final JavaToDataCellConverterFactory<String> stringFactory = JavaToDataCellConverterRegistry.getInstance()
            .getConverterFactories(String.class, StringCell.TYPE).stream().findFirst().get();
        final ProductionPath pathA = new ProductionPath(intProducer, intFactory);
        final ProductionPath pathB = new ProductionPath(intProducer, intFactory);
        final ProductionPath pathC = new ProductionPath(intProducer, stringFactory);
        final ProductionPath pathD = new ProductionPath(null, null);
        final ProductionPath pathE = new ProductionPath(intProducer, null);

        assertTrue("ConversionPath should equal itself", pathA.equals(pathA));
        assertTrue(pathA.equals(pathB));
        assertTrue(pathB.equals(pathA));
        assertTrue(pathD.equals(pathD));

        assertFalse(pathA.equals(pathC));
        assertFalse(pathA.equals(new Integer(42)));
        assertFalse(pathA.equals(pathD));
        assertFalse(pathE.equals(pathD));
        assertFalse(pathD.equals(pathE));
        assertFalse(pathE.equals(pathA));
        assertFalse(pathA.equals(null));

        assertEquals("INT ---> Integer --(\"Integer\")-> Number (integer)", pathA.toString());

        assertEquals(pathA.hashCode(), pathA.hashCode());
        assertEquals(pathA.hashCode(), pathB.hashCode());
        assertNotEquals(pathA.hashCode(), pathC.hashCode());

        assertEquals(pathA.getProducerFactory(), pathA.m_producerFactory);
        assertEquals(pathA.getConverterFactory(), pathA.m_converterFactory);
    }

    /**
     * @throws Exception
     */
    @Test
    public void consumerTest() throws Exception {
        /* One time setup */
        final CellValueConsumer<H2ODestination, Object, H2OParameters> generalConsumer = (c, v, p) -> {
            c.h2oFrame.get(p.rowIndex)[p.columnIndex] = v;
        };

        // TODO extension point
        MappingFramework.forDestinationType(H2ODestination.class) //
            .unregisterAllConsumers() //
            .register(intConsumer) //
            .register(new SimpleCellValueConsumerFactory<>(Long.class, "LONG", generalConsumer)) //
            .register(new SimpleCellValueConsumerFactory<>(Float.class, "FLOAT", generalConsumer)) //
            .register(new SimpleCellValueConsumerFactory<>(Double.class, "DOUBLE", generalConsumer)) //
            .register(stringConsumer) //
            .register(stringConsumer); // Will display a CODING error, but should not succeed

        assertEquals(intConsumer, MappingFramework.forDestinationType(H2ODestination.class)
            .getFactories(Integer.class, "INT").stream().findFirst().get());

        final List<ConsumptionPath> paths =
            MappingFramework.forDestinationType(H2ODestination.class).getAvailableConsumptionPaths(IntCell.TYPE);
        assertEquals(4, paths.size());

        final String[] inTypes =
            paths.stream().map(p -> p.m_consumerFactory.getDestinationType()).toArray(n -> new String[n]);
        assertEquals(1, Stream.of(inTypes).filter(s -> s.equals("INT")).count());
        assertEquals(1, Stream.of(inTypes).filter(s -> s.equals("STR")).count());
        assertEquals(1, Stream.of(inTypes).filter(s -> s.equals("LONG")).count());
        assertEquals(1, Stream.of(inTypes).filter(s -> s.equals("DOUBLE")).count());

        assertEquals(intConsumer.getDestinationType(), intConsumer.getName());

        // TODO later: deprecation mechanism?
        // TODO later: priority mechanism (=deprecation?).
        final ConsumptionPath expectedPath = new ConsumptionPath(DataCellToJavaConverterRegistry.getInstance()
            .getConverterFactories(IntCell.TYPE, Integer.class).stream().findFirst().get(), intConsumer);
        assertTrue(paths.contains(expectedPath));

        assertTrue(MappingFramework.forDestinationType(H2ODestination.class).getFactories(ConsumptionPath.class, "INT")
            .isEmpty());

        // TODO
        // Assumption: RESULTSET is stateful...
        //expectedPath.map(Col, RESULTSET);
        //expectedPath.map(Col, RESULTSET);

        /* Some example input */
        final DefaultRow row = new DefaultRow(RowKey.createRowKey(0L), new StringCell("KNIME"), new IntCell(42),
            new LongCell(42L), new MissingCell("missing"));

        final H2ODestination testSink = new H2ODestination();
        testSink.h2oFrame.add(new Object[4]);

        final ConsumptionPath[] mapping = new ConsumptionPath[]{
            new ConsumptionPath(
                DataCellToJavaConverterRegistry.getInstance().getConverterFactories(StringCell.TYPE, String.class)
                    .stream().findFirst().get(),
                MappingFramework.forDestinationType(H2ODestination.class).getFactory("java.lang.String->STR").get()),
            new ConsumptionPath(
                DataCellToJavaConverterRegistry.getInstance().getConverterFactories(IntCell.TYPE, Integer.class)
                    .stream().findFirst().get(),
                MappingFramework.forDestinationType(H2ODestination.class).getFactory("java.lang.Integer->INT").get()),
            new ConsumptionPath(
                DataCellToJavaConverterRegistry.getInstance().getConverterFactories(LongCell.TYPE, Long.class).stream()
                    .findFirst().get(),
                MappingFramework.forDestinationType(H2ODestination.class).getFactory("java.lang.Long->LONG").get()),
            new ConsumptionPath(
                DataCellToJavaConverterRegistry.getInstance().getConverterFactories(LongCell.TYPE, Long.class).stream()
                    .findFirst().get(),
                MappingFramework.forDestinationType(H2ODestination.class).getFactory("java.lang.Long->LONG").get())};

        H2OParameters[] parameters = new H2OParameters[4];
        for (int i = 0; i < parameters.length; ++i) {
            parameters[i] = new H2OParameters();
            parameters[i].columnIndex = i;
            parameters[i].rowIndex = 0;
        }

        MappingFramework.map(row, testSink, mapping, parameters);

        assertEquals(1, testSink.h2oFrame.size());
        assertEquals(4, testSink.h2oFrame.get(0).length);
        assertArrayEquals(new Object[]{"KNIME", new Integer(42), new Long(42L), null}, testSink.h2oFrame.get(0));
    }

    /**
     * @throws Exception
     */
    @Test
    public void producerTest() throws Exception {
        /* One time setup */
        final CellValueProducer<H2OSource, Integer, H2OParameters> lambda = (c, p) -> {
            return (Integer)c.h2oFrame.get(p.rowIndex)[p.columnIndex];
        };
        final SimpleCellValueProducerFactory<H2OSource, String, Integer, H2OParameters> intProducerFromBIGINT =
            new SimpleCellValueProducerFactory<>("BIGINT", Integer.class, lambda);
        final SimpleCellValueProducerFactory<H2OSource, String, Long, H2OParameters> longProducer =
            new SimpleCellValueProducerFactory<>("LONG", Long.class, (c, p) -> {
                return (Long)c.h2oFrame.get(p.rowIndex)[p.columnIndex];
            });

        MappingFramework.forSourceType(H2OSource.class) //
            .unregisterAllProducers() //
            .register(intProducer) //
            .register(longProducer) //
            .register(stringProducer) //
            .register(intProducerFromBIGINT); //

        assertEquals(intProducer, MappingFramework.forSourceType(H2OSource.class).getFactories("INT", Integer.class)
            .stream().findFirst().get());

        final List<ProductionPath> paths =
            MappingFramework.forSourceType(H2OSource.class).getAvailableProductionPaths("INT");
        assertEquals(2, paths.size());

        final DataType[] outTypes =
            paths.stream().map(p -> p.m_converterFactory.getDestinationType()).toArray(n -> new DataType[n]);
        assertEquals(1, Stream.of(outTypes).filter(s -> s.equals(IntCell.TYPE)).count());
        assertEquals(1, Stream.of(outTypes).filter(s -> s.equals(LongCell.TYPE)).count());

        final ProductionPath expectedPath = new ProductionPath(intProducer, JavaToDataCellConverterRegistry
            .getInstance().getConverterFactories(Integer.class, IntCell.TYPE).stream().findFirst().get());
        assertTrue(paths.contains(expectedPath));

        assertTrue(MappingFramework.forDestinationType(H2ODestination.class).getFactories(ConsumptionPath.class, "INT")
            .isEmpty());

        ProductionPath[] mapping = new ProductionPath[]{
            new ProductionPath(
                MappingFramework.forSourceType(H2OSource.class).getFactory("STR->java.lang.String").get(),
                JavaToDataCellConverterRegistry.getInstance().getConverterFactories(String.class, StringCell.TYPE)
                    .stream().findFirst().get()),
            new ProductionPath(
                MappingFramework.forSourceType(H2OSource.class).getFactory("INT->java.lang.Integer").get(),
                JavaToDataCellConverterRegistry.getInstance().getConverterFactories(Integer.class, IntCell.TYPE)
                    .stream().findFirst().get()),
            new ProductionPath(MappingFramework.forSourceType(H2OSource.class).getFactory("LONG->java.lang.Long").get(),
                JavaToDataCellConverterRegistry.getInstance().getConverterFactories(Long.class, LongCell.TYPE).stream()
                    .findFirst().get()),
            new ProductionPath(MappingFramework.forSourceType(H2OSource.class).getFactory("LONG->java.lang.Long").get(),
                JavaToDataCellConverterRegistry.getInstance().getConverterFactories(Long.class, LongCell.TYPE).stream()
                    .findFirst().get())};

        H2OParameters[] parameters = new H2OParameters[4];
        for (int i = 0; i < parameters.length; ++i) {
            parameters[i] = new H2OParameters();
            parameters[i].columnIndex = i;
            parameters[i].rowIndex = 0;
        }

        {
            final H2OSource testSource = new H2OSource();
            testSource.h2oFrame.add(new Object[]{"KNIME", new Integer(42), new Long(42L), null});

            final DataRow row = MappingFramework.map(RowKey.createRowKey(0L), testSource, mapping, parameters, null);

            assertEquals(StringCell.TYPE, row.getCell(0).getType());
            assertEquals("KNIME", ((StringValue)row.getCell(0)).getStringValue());

            assertEquals(IntCell.TYPE, row.getCell(1).getType());
            assertEquals(42, ((IntValue)row.getCell(1)).getIntValue());

            assertEquals(LongCell.TYPE, row.getCell(2).getType());
            assertEquals(42L, ((LongValue)row.getCell(2)).getLongValue());

            assertTrue(row.getCell(3).isMissing());
        }

        DataTableSpec spec = MappingFramework.createSpec(new String[]{"s", "i", "l", "missing"}, mapping);
        assertEquals(StringCell.TYPE, spec.getColumnSpec(0).getType());
        assertEquals(IntCell.TYPE, spec.getColumnSpec(1).getType());
        assertEquals(LongCell.TYPE, spec.getColumnSpec(2).getType());
        assertEquals(LongCell.TYPE, spec.getColumnSpec(2).getType());
    }

    @Test
    public void parentTest() throws Exception {
        class HitchHikersSource extends H2OSource {
            // Marker class
        }
        class HitchHikersDest extends H2ODestination {
            // Marker class
        }
        class HitchHikersParams
            implements Source.ProducerParameters<HitchHikersSource>, Destination.ConsumerParameters<HitchHikersDest> {
            // Marker class
        }

        /* Producers */
        {
            final SimpleCellValueProducerFactory<HitchHikersSource, String, Integer, HitchHikersParams> fourtyTwo =
                new SimpleCellValueProducerFactory<>("INT", Integer.class, (c, p) -> {
                    return new Integer(42);
                });

            MappingFramework.forSourceType(H2OSource.class) //
                .unregisterAllProducers() //
                .register(stringProducer);
            MappingFramework.forSourceType(HitchHikersSource.class) //
                .unregisterAllProducers() //
                .setParent(H2OSource.class) // Now also knows string
                .register(fourtyTwo);

            {
                /* Child registry should know factories of parent */
                Collection<CellValueProducerFactory<HitchHikersSource, String, ?, ?>> factories =
                    MappingFramework.forSourceType(HitchHikersSource.class).getFactoriesForSourceType("STR");
                assertEquals(1, factories.size());

                assertTrue(MappingFramework.forSourceType(HitchHikersSource.class).getFactory("STR->java.lang.String")
                    .isPresent());
                assertArrayEquals(new Object[]{stringProducer}, MappingFramework.forSourceType(HitchHikersSource.class)
                    .getFactories("STR", String.class).toArray());

                /* The order is not important here */
                assertCollectionEquals(new Object[]{fourtyTwo, stringProducer},
                    MappingFramework.forSourceType(HitchHikersSource.class).getAllConverterFactories());
                assertCollectionEquals(new Object[]{"STR", "INT"},
                    MappingFramework.forSourceType(HitchHikersSource.class).getAllSourceTypes());
                assertCollectionEquals(
                    new Object[]{fourtyTwo.getDestinationType(), stringProducer.getDestinationType()},
                    MappingFramework.forSourceType(HitchHikersSource.class).getAllDestinationTypes());
            }
            {
                /* Parent registry should *not* know factories of child */
                Collection<CellValueProducerFactory<H2OSource, String, ?, ?>> factories =
                    MappingFramework.forSourceType(H2OSource.class).getFactoriesForSourceType("INT");
                assertTrue(factories.isEmpty());

                assertFalse(
                    MappingFramework.forSourceType(H2OSource.class).getFactory("INT->java.lang.Integer").isPresent());
                assertTrue(
                    MappingFramework.forSourceType(H2OSource.class).getFactories("INT", Integer.class).isEmpty());
            }
        }

        /* Consumers */
        {
            final SimpleCellValueConsumerFactory<HitchHikersDest, Integer, String, HitchHikersParams> fourtyTwo =
                new SimpleCellValueConsumerFactory<>(Integer.class, "INT", (c, v, p) -> {
                    /* consumed by gnab gib */
                });

            MappingFramework.forDestinationType(H2ODestination.class) //
                .unregisterAllConsumers() //
                .register(stringConsumer);
            MappingFramework.forDestinationType(HitchHikersDest.class) //
                .unregisterAllConsumers() //
                .setParent(H2ODestination.class) // Now also knows string
                .register(fourtyTwo);

            {
                /* Child registry should know factories of parent */
                Collection<CellValueConsumerFactory<HitchHikersDest, ?, String, ?>> factories =
                    MappingFramework.forDestinationType(HitchHikersDest.class).getFactoriesForDestinationType("STR");
                assertEquals(1, factories.size());

                assertTrue(MappingFramework.forDestinationType(HitchHikersDest.class)
                    .getFactory("java.lang.String->STR").isPresent());
                assertArrayEquals(new Object[]{stringConsumer}, MappingFramework
                    .forDestinationType(HitchHikersDest.class).getFactories(String.class, "STR").toArray());

                /* The order is not important here */
                assertCollectionEquals(new Object[]{fourtyTwo, stringConsumer},
                    MappingFramework.forDestinationType(HitchHikersDest.class).getAllConverterFactories());
                assertCollectionEquals(new Object[]{stringConsumer.getSourceType(), fourtyTwo.getSourceType()},
                    MappingFramework.forDestinationType(HitchHikersDest.class).getAllSourceTypes());
                assertCollectionEquals(
                    new Object[]{stringConsumer.getDestinationType(), fourtyTwo.getDestinationType()},
                    MappingFramework.forDestinationType(HitchHikersDest.class).getAllDestinationTypes());
            }
            {
                /* Parent registry should *not* know factories of child */
                Collection<CellValueConsumerFactory<H2ODestination, ?, String, ?>> factories =
                    MappingFramework.forDestinationType(H2ODestination.class).getFactoriesForDestinationType("INT");
                assertTrue(factories.isEmpty());

                assertFalse(MappingFramework.forDestinationType(H2ODestination.class)
                    .getFactory("java.lang.Integer->INT").isPresent());
                assertTrue(MappingFramework.forDestinationType(H2ODestination.class).getFactories(Integer.class, "INT")
                    .isEmpty());
            }
        }
    }

    /** Test getting all available production paths from {@link ProducerRegistry} */
    @Test
    public void availableProductionPathsTest() {
        final ProducerRegistry<String, H2OSource> reg = MappingFramework.forSourceType(H2OSource.class);
        final JavaToDataCellConverterRegistry convReg = JavaToDataCellConverterRegistry.getInstance();
        assertTrue(reg.getAvailableProductionPaths().isEmpty());

        reg.register(intProducer);
        final List<ProductionPath> paths = reg.getAvailableProductionPaths();
        assertCollectionEquals(new Object[]{
            new ProductionPath(intProducer,
                convReg.getConverterFactories(Integer.class, IntCell.TYPE).stream().findFirst().get()),
            new ProductionPath(intProducer,
                convReg.getConverterFactories(Integer.class, LongCell.TYPE).stream().findFirst().get())},
            paths);
    }

    /**
     * Test serialization of production path
     *
     * @throws InvalidSettingsException
     */
    @Test
    public void productionPathSerializeTest() throws InvalidSettingsException {
        MappingFramework.forSourceType(H2OSource.class).register(stringProducer);

        final ProductionPath path = new ProductionPath(
            MappingFramework.forSourceType(H2OSource.class).getFactory("STR->java.lang.String").get(),
            JavaToDataCellConverterRegistry.getInstance().getConverterFactories(String.class, StringCell.TYPE).stream()
                .findFirst().get());

        final Config config = new NodeSettings("Test");

        SerializeUtil.storeProductionPath(path, config, "the_path");

        final Optional<ProductionPath> loadedPath =
            SerializeUtil.loadProductionPath(config, MappingFramework.forSourceType(H2OSource.class), "the_path");

        assertTrue(loadedPath.isPresent());
        assertEquals(path, loadedPath.get());
    }

    /**
     * Test serialization of consumption path
     *
     * @throws InvalidSettingsException
     */
    @Test
    public void consumptionPathSerializeTest() throws InvalidSettingsException {
        MappingFramework.forDestinationType(H2ODestination.class).register(stringConsumer);

        final ConsumptionPath path = new ConsumptionPath(
            DataCellToJavaConverterRegistry.getInstance().getConverterFactories(StringCell.TYPE, String.class).stream()
                .findFirst().get(),
            MappingFramework.forDestinationType(H2ODestination.class).getFactory("java.lang.String->STR").get());

        final Config config = new NodeSettings("Test");
        SerializeUtil.storeConsumptionPath(path, config, "the_path");
        final Optional<ConsumptionPath> loadedPath = SerializeUtil.loadConsumptionPath(config,
            MappingFramework.forDestinationType(H2ODestination.class), "the_path");

        assertTrue(loadedPath.isPresent());
        assertEquals(path, loadedPath.get());
    }

    /** Assert that a collection has given contents independent of order */
    private static void assertCollectionEquals(final Object[] expected, final Collection<?> actual) {
        assertEquals("Sizes of collections differ", expected.length, actual.size());
        assertTrue("Contents of collections differ", actual.containsAll(Arrays.asList(expected)));
    }
}
