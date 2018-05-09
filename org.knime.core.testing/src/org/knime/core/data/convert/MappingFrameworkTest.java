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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.convert.MappingFrameworkTest.H2OSink.H2OSinkParameters;
import org.knime.core.data.convert.java.DataCellToJavaConverterRegistry;
import org.knime.core.data.convert.map.AbstractSink;
import org.knime.core.data.convert.map.AbstractSink.UnmappableTypeException;
import org.knime.core.data.convert.map.MappingFramework;
import org.knime.core.data.convert.map.MappingFramework.CellValueConsumer;
import org.knime.core.data.convert.map.MappingFramework.ProducerConsumerRegistry;
import org.knime.core.data.convert.map.MappingFramework.ProducerConsumerRegistry.ConsumerRegistry.ConsumptionPath;
import org.knime.core.data.convert.map.Sink;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Test for the {@link MappingFramework}.
 *
 * @author Jonathan Hale
 */
public class MappingFrameworkTest {

    /**
     * Implementation for sinking stuff into "pseudo-H2O".
     *
     * @author Jonathan Hale
     */
    static class H2OSink extends AbstractSink<H2OSink, H2OSinkParameters> {
        /* Represents the destiny for all the data */
        public Object[] h2oFrame;

        public int numRows = 0;

        /**
         * Implementation for H2O {@link Sink.ConsumerParameters}.
         *
         * @author Jonathan Hale
         */
        protected class H2OSinkParameters implements Sink.ConsumerParameters<H2OSink> {
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
        public List<H2OSinkParameters> createSinkParameters(final DataTableSpec spec) {
            final ArrayList<H2OSinkParameters> params = new ArrayList<>();

            for (int i = 0; i < spec.getNumColumns(); ++i) {
                //final DataColumnSpec columnSpec = spec.getColumnSpec(i);

                final H2OSinkParameters p = new H2OSinkParameters();
                p.columnIndex = i;
                params.add(p);
            }
            return params;
        }
    }

    /**
     * Interface to clean up consumer declarations
     *
     * @param <T> Java type to which the consumer should convert to.
     */
    public static interface H2OConsumer<T> extends CellValueConsumer<H2OSink, T, H2OSinkParameters> {
    }

    /**
     * Tests {@link ConsumptionPath#equals} and {@link ConsumptionPath#toString()}
     */
    @Test
    public void conversionPaths() {
        final H2OConsumer<Integer> intConsumer = (c, v, p) -> {
            c.h2oFrame[p.columnIndex] = v;
        };

        final ConsumptionPath pathA = new ConsumptionPath(DataCellToJavaConverterRegistry.getInstance()
            .getConverterFactories(IntCell.TYPE, Integer.class).stream().findFirst().get(), intConsumer);
        final ConsumptionPath pathB = new ConsumptionPath(DataCellToJavaConverterRegistry.getInstance()
            .getConverterFactories(IntCell.TYPE, Integer.class).stream().findFirst().get(), intConsumer);
        final ConsumptionPath pathC = new ConsumptionPath(DataCellToJavaConverterRegistry.getInstance()
            .getConverterFactories(LongCell.TYPE, Long.class).stream().findFirst().get(), intConsumer);

        assertTrue("ConversionPath should equal itself", pathA.equals(pathA));
        assertTrue(pathA.equals(pathB));
        assertTrue(pathB.equals(pathA));
        assertFalse(pathA.equals(pathC));
        assertFalse(pathA.equals(new Integer(42)));

        assertEquals("IntValue --(\"Integer\")-> Integer ---> MappingFrameworkTest$$Lambda$<Number> Consumer",
            pathA.toString().split("$$Lambda$")[0].replaceAll("\\d+/\\d+", "<Number>"));
    }

    /**
     * @throws UnmappableTypeException
     */
    @Test
    public void conversion() throws UnmappableTypeException {
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
            .registerConsumer(Integer.class, intConsumer).registerConsumer(Long.class, generalConsumer)
            .registerConsumer(Float.class, generalConsumer).registerConsumer(Double.class, generalConsumer)
            .registerConsumer(String.class, stringConsumer);

        assertEquals(1,
            ProducerConsumerRegistry.forSinkType(H2OSink.class).getAvailableConsumers(Integer.class).size());

        final Collection<ConsumptionPath> paths =
            ProducerConsumerRegistry.forSinkType(H2OSink.class).getAvailableConsumptionPaths(IntCell.TYPE);
        assertEquals(4, paths.size());
        final ConsumptionPath expectedPath = new ConsumptionPath(DataCellToJavaConverterRegistry.getInstance()
            .getConverterFactories(IntCell.TYPE, Integer.class).stream().findFirst().get(), intConsumer);
        assertTrue(paths.contains(expectedPath));

        /* No consumers here, should simply return empty list */
        assertTrue(
            ProducerConsumerRegistry.forSinkType(H2OSink.class).getAvailableConsumers(ConsumptionPath.class).isEmpty());
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
}
