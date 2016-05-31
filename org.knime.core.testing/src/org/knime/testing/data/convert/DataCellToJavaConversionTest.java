package org.knime.testing.data.convert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.IntValue;
import org.knime.core.data.MissingValue;
import org.knime.core.data.blob.BinaryObjectCellFactory;
import org.knime.core.data.blob.BinaryObjectDataCell;
import org.knime.core.data.collection.CollectionCellFactory;
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
import org.knime.core.data.xml.XMLValue;
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
    protected <S, D> void testSimpleConverter(final DataType sourceType, final Class<D> destType, final S source,
        final D dest) throws Exception {
        final Optional<DataCellToJavaConverterFactory<S, D>> factory =
            DataCellToJavaConverterRegistry.getInstance().getConverterFactory(sourceType, destType);
        assertTrue(factory.isPresent());

        final DataCellToJavaConverter<S, D> converter = factory.get().create();
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
        final Optional<DataCellToJavaConverterFactory<BinaryObjectDataCell, InputStream>> factory =
            DataCellToJavaConverterRegistry.getInstance().getConverterFactory(BinaryObjectDataCell.TYPE,
                InputStream.class);
        assertTrue(factory.isPresent());

        final DataCellToJavaConverter<BinaryObjectDataCell, InputStream> converter = factory.get().create();
        assertNotNull(converter);

        final BinaryObjectCellFactory cellFactory = new BinaryObjectCellFactory();

        assertTrue(
            converter.convert((BinaryObjectDataCell)cellFactory.create(new byte[]{4, 2})) instanceof InputStream);
        /* convert a BinaryObjectDataCell */
        InputStream stream = converter.convert((BinaryObjectDataCell)cellFactory.create(new byte[]{4, 2}));
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
        final Optional<DataCellToJavaConverterFactory<XMLValue, Document>> factory =
            DataCellToJavaConverterRegistry.getInstance().getConverterFactory(XMLCell.TYPE, Document.class);
        assertTrue(factory.isPresent());

        final DataCellToJavaConverter<XMLValue, Document> converter = factory.get().create();
        assertNotNull(converter);

        final NodeList children = converter.convert((XMLCell)XMLCellFactory.create("<tag/>")).getChildNodes();
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

        final Optional<DataCellToJavaConverterFactory<DataCell, Integer[]>> factory =
            DataCellToJavaConverterRegistry.getInstance().getConverterFactory(listCell.getType(), Integer[].class);
        assertTrue(factory.isPresent());

        final DataCellToJavaConverter<DataCell, Integer[]> converter = factory.get().create();
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

        assertEquals(3, destTypes.size());
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

        assertEquals(3, destTypes.size());
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

        assertEquals(3, sourceTypes.size());
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
        assertTrue(sourceTypes.contains(ListCell.class));
        assertTrue(sourceTypes.contains(MissingValue.class));
    }
}
