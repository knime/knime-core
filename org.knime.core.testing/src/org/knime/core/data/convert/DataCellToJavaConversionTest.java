/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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

package org.knime.core.data.convert;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.MissingValue;
import org.knime.core.data.blob.BinaryObjectCellFactory;
import org.knime.core.data.blob.BinaryObjectDataCell;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.convert.java.DataCellToJavaConverter;
import org.knime.core.data.convert.java.DataCellToJavaConverterFactory;
import org.knime.core.data.convert.java.DataCellToJavaConverterRegistry;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.xml.XMLCell;
import org.knime.core.data.xml.XMLCellFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Test conversion of Java objects to Java objects.
 *
 * @author Jonathan Hale
 * @see DataCellToJavaConverterRegistry
 */
public class DataCellToJavaConversionTest {

    private static final double FUZZY_DOUBLE_TOLERANCE = 1E-5;

    /**
     * Test a simple converter, i.e. a converter which can be tested by comparing a source value with an expected
     * destination value.
     *
     * @param sourceType source type
     * @param destType destination type
     * @param source value which will be converted
     * @param dest output to expect from the converter when converting <code>source</code>
     * @throws Exception When something went wrong
     */
    protected <D> void testSimpleConverter(final DataType sourceType, final Class<D> destType, final DataCell source,
        final D dest) throws Exception {
        final Optional<? extends DataCellToJavaConverterFactory<? extends DataValue, D>> factory =
            DataCellToJavaConverterRegistry.getInstance().getConverterFactories(sourceType, destType).stream()
                .findFirst();
        assertTrue(factory.isPresent());

        final DataCellToJavaConverter<DataCell, D> converter =
            (DataCellToJavaConverter<DataCell, D>)factory.get().create();
        assertNotNull(converter);

        if (destType == Double.class) {
            assertEquals((Double)converter.convert(source), (Double)dest, FUZZY_DOUBLE_TOLERANCE);
        } else {
            assertEquals(converter.convert(source), dest);
        }
    }

    /**
     * Test BooleanCell -> Boolean conversion.
     *
     * @throws Exception When something went wrong
     */
    @Test
    public void testBoolean() throws Exception {
        testSimpleConverter(BooleanCell.TYPE, Boolean.class, BooleanCell.TRUE, true);
    }

    /**
     * Test StringCell -> String and IntCell -> String conversions.
     *
     * @throws Exception When something went wrong
     */
    @Test
    public void testString() throws Exception {
        testSimpleConverter(StringCell.TYPE, String.class, new StringCell("KNIME"), "KNIME");

        /* general toString() converter: */
        testSimpleConverter(IntCell.TYPE, String.class, new IntCell(42), "42");
    }

    /**
     * @throws Exception When something went wrong
     */
    @Test
    public void testInteger() throws Exception {
        testSimpleConverter(IntCell.TYPE, Integer.class, new IntCell(42), new Integer(42));
    }

    /**
     * Test LongCell -> Long conversion.
     *
     * @throws Exception When something went wrong
     */
    @Test
    public void testLong() throws Exception {
        testSimpleConverter(LongCell.TYPE, Long.class, new LongCell(42L), new Long(42L));
    }

    /**
     * Test DoubleCell -> Double conversion.
     *
     * @throws Exception When something went wrong
     */
    @Test
    public void testDouble() throws Exception {
        testSimpleConverter(DoubleCell.TYPE, Double.class, new DoubleCell(Math.PI), new Double(Math.PI));
    }

    /**
     * Test BinaryObjectDataCell -> InputStream conversion.
     *
     * @throws Exception When something went wrong
     */
    @Test
    public void testBinaryObject() throws Exception {
        /* retrieve converter from DataCellToJavaConverterRegistry */
        final Optional<? extends DataCellToJavaConverterFactory<? extends DataValue, InputStream>> factory =
            DataCellToJavaConverterRegistry.getInstance()
                .getConverterFactories(BinaryObjectDataCell.TYPE, InputStream.class).stream().findFirst();
        assertTrue(factory.isPresent());

        final DataCellToJavaConverter<DataCell, InputStream> converter =
            (DataCellToJavaConverter<DataCell, InputStream>)factory.get().create();
        assertNotNull(converter);

        final BinaryObjectCellFactory cellFactory = new BinaryObjectCellFactory();

        assertTrue(converter.convert(cellFactory.create(new byte[]{4, 2})) instanceof InputStream);
        /* convert a BinaryObjectDataCell */
        InputStream stream = converter.convert(cellFactory.create(new byte[]{4, 2}));
        assertEquals(stream.read(), 4);
        assertEquals(stream.read(), 2);

        stream.close();
    }

    /**
     * Test XMLValue -> Document conversion.
     *
     * @throws Exception When something went wrong
     */
    @Test
    public void testXML() throws Exception {
        final Optional<? extends DataCellToJavaConverterFactory<? extends DataValue, Document>> factory =
            DataCellToJavaConverterRegistry.getInstance().getConverterFactories(XMLCell.TYPE, Document.class).stream()
                .findFirst();
        assertTrue(factory.isPresent());

        final DataCellToJavaConverter<DataCell, Document> converter =
            (DataCellToJavaConverter<DataCell, Document>)factory.get().create();
        assertNotNull(converter);

        final NodeList children = converter.convert(XMLCellFactory.create("<tag/>")).getChildNodes();
        assertEquals(children.getLength(), 1);
        assertEquals(children.item(0).getNodeName(), "tag");
    }

    /**
     * Test ListCell(IntCell) -> Integer[] conversion.
     *
     * @throws Exception When something went wrong
     */
    @Test
    public void testCollectionTypes() throws Exception {
        ArrayList<IntCell> coll = new ArrayList<>();
        for (int i = 0; i < 5; ++i) {
            coll.add(new IntCell(i * i));
        }

        final ListCell listCell = CollectionCellFactory.createListCell(coll);

        final Optional<? extends DataCellToJavaConverterFactory<? extends DataValue, Integer[]>> factory =
            DataCellToJavaConverterRegistry.getInstance().getConverterFactories(listCell.getType(), Integer[].class)
                .stream().findFirst();
        assertTrue(factory.isPresent());

        final DataCellToJavaConverter<DataCell, Integer[]> converter =
            (DataCellToJavaConverter<DataCell, Integer[]>)factory.get().create();
        assertNotNull(converter);

        final Integer[] array = converter.convert(listCell);
        for (int i = 0; i < 5; ++i) {
            assertEquals(new Integer(i * i), array[i]);
        }
    }

    /**
     * Test destination types of IntCell.
     *
     * @throws Exception When something went wrong
     */
    @Test
    public void testDestTypes() throws Exception {
        final Set<Class<?>> destTypes =
            DataCellToJavaConverterRegistry.getInstance().getFactoriesForSourceType(IntCell.TYPE).stream()
                .map((factory) -> factory.getDestinationType()).collect(Collectors.toSet());

        assertThat("Not enough supported destination types for IntCell", destTypes.size(), is(greaterThan(3)));
        assertTrue(destTypes.contains(Integer.class));
        assertTrue(destTypes.contains(Long.class));
        assertTrue(destTypes.contains(Double.class));
    }

    /**
     * Test destination types of IntCell.
     *
     * @throws Exception When something went wrong
     */
    @Test
    public void testCollectionDestTypes() throws Exception {
        final Set<Class<?>> destTypes = DataCellToJavaConverterRegistry.getInstance()
            .getFactoriesForSourceType(ListCell.getCollectionType(IntCell.TYPE)).stream()
            .map((factory) -> factory.getDestinationType()).collect(Collectors.toSet());

        assertThat("Not enough supported destination types for ListCell of IntCell", destTypes.size(),
            is(greaterThan(3)));
        assertTrue(destTypes.contains(Integer[].class));
        assertTrue(destTypes.contains(Long[].class));
        assertTrue(destTypes.contains(Double[].class));
    }

    /**
     * Test destination types of Integer.
     *
     * @throws Exception When something went wrong
     */
    @Test
    public void testSourceTypes() throws Exception {
        final Set<Class<?>> sourceTypes =
            DataCellToJavaConverterRegistry.getInstance().getFactoriesForDestinationType(Integer.class).stream()
                .map((factory) -> factory.getSourceType()).collect(Collectors.toSet());

        assertThat("Not enough converters for conversion to Integer", sourceTypes.size(), is(greaterThanOrEqualTo(3)));
        assertTrue(sourceTypes.contains(IntValue.class));
        assertTrue(sourceTypes.contains(MissingValue.class));
        assertTrue(sourceTypes.contains(StringCell.class)); // Test extension point
    }

    /**
     * Test destination types of Integer.
     *
     * @throws Exception When something went wrong
     */
    @Test
    public void testArraySourceTypes() throws Exception {
        final Set<Class<?>> sourceTypes =
            DataCellToJavaConverterRegistry.getInstance().getFactoriesForDestinationType(Integer[].class).stream()
                .map((factory) -> factory.getSourceType()).collect(Collectors.toSet());

        assertEquals(2, sourceTypes.size());
        assertTrue(sourceTypes.contains(CollectionDataValue.class));
        assertTrue(sourceTypes.contains(MissingValue.class));
    }
}
