package org.knime.testing.data.convert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.blob.BinaryObjectDataCell;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.convert.ConversionKey;
import org.knime.core.data.convert.datacell.JavaToDataCellConverter;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterFactory;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterRegistry;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.xml.XMLCell;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * Test for all {@link JavaToDataCellConverterFactory}s and {@link JavaToDataCellConverterRegistry}.
 *
 * Naming of method follows: <code> testTo[DestType]() {
 * // all tests from any source type to DestType
 * } </code>
 *
 * @author Jonathan Hale
 * @see JavaToDataCellConverterRegistry
 * @see JavaToDataCellConverterFactory
 */
public class JavaToDataCellConversionTest {

    private static final double FUZZY_DOUBLE_TOLERANCE = 0.000001;

    /* Class for testing toString() converter */
    private class MyClass {
        @Override
        public String toString() {
            return "toString()";
        }
    }

    /**
     * Test {@link ConversionKey#equals(Object)}.
     */
    @Test
    public void testKeysEqual() {
        ConversionKey key = new ConversionKey(Boolean.class, BooleanCell.TYPE);
        ConversionKey key2 = new ConversionKey(Boolean.class, BooleanCell.TYPE);

        assertEquals(key, key2);
    }

    /**
     * Generic test for simple {@link JavaToDataCellConverterFactory}s.
     *
     * @param sourceType type of the input to convert
     * @param dataType {@link DataType} of the {@link DataCell} to convert to.
     * @param destType type of the expected output {@link DataCell} subtype.
     * @param sourceValue a value to convert.
     * @return <code>sourceValue</code> converted to <code>destType</code>.
     * @throws Exception
     */
    protected <S, D> D testSimpleConversion(final Class<S> sourceType, final DataType dataType, final Class<D> destType,
        final S sourceValue) throws Exception {
        final Optional<JavaToDataCellConverterFactory<S>> factory =
            JavaToDataCellConverterRegistry.getInstance().getConverterFactory(sourceType, dataType);
        assertTrue(factory.isPresent());

        final JavaToDataCellConverter<S> converter = factory.get().create(null);
        assertNotNull(converter);

        final DataCell converted = converter.convert(sourceValue);
        assertTrue(destType.isInstance(converted));

        @SuppressWarnings("unchecked") // checked in the above assert.
        D d = (D)converted;
        return d;
    }

    /**
     * Test Boolean -> BooleanCell conversion.
     *
     * @throws Exception When something went wrong
     */
    @Test
    public void testToBooleanCell() throws Exception {
        final BooleanCell cell =
            testSimpleConversion(Boolean.class, BooleanCell.TYPE, BooleanCell.class, new Boolean(true));
        assertTrue(cell.getBooleanValue());
    }

    /**
     * Test Integer -> IntCell conversion.
     *
     * @throws Exception When something went wrong
     */
    @Test
    public void testToIntCell() throws Exception {
        final IntCell cell = testSimpleConversion(Integer.class, IntCell.TYPE, IntCell.class, new Integer(42));
        assertEquals(cell.getIntValue(), 42);
    }

    /**
     * Test long -> LongCell and Integer -> LongCell conversions.
     *
     * @throws Exception When something went wrong
     */
    @Test
    public void testToLongCell() throws Exception {
        final LongCell cell = testSimpleConversion(Long.class, LongCell.TYPE, LongCell.class, new Long(42L));
        assertEquals(cell.getLongValue(), 42L);

        final LongCell cell1 = testSimpleConversion(Integer.class, LongCell.TYPE, LongCell.class, new Integer(412));
        assertEquals(cell1.getLongValue(), 412L);
    }

    /**
     * Test Double -> DoubleCell conversion.
     *
     * @throws Exception When something went wrong
     */
    @Test
    public void testToDoubleCell() throws Exception {
        final DoubleCell cell =
            testSimpleConversion(Double.class, DoubleCell.TYPE, DoubleCell.class, new Double(Math.PI));
        assertEquals(cell.getDoubleValue(), Math.PI, FUZZY_DOUBLE_TOLERANCE);
    }

    /**
     * Test String -> StringCell and Object -> StringCell conversions.
     *
     * @throws Exception When something went wrong
     */
    @Test
    public void testToStringCell() throws Exception {
        final StringCell cell =
            testSimpleConversion(String.class, StringCell.TYPE, StringCell.class, new String("KNIME"));
        assertEquals(cell.getStringValue(), "KNIME");

        /* .toString() converter */
        final StringCell cell1 = testSimpleConversion(MyClass.class, StringCell.TYPE, StringCell.class, new MyClass());
        assertEquals(cell1.getStringValue(), new MyClass().toString());
    }

    /**
     * Test String -> XMLCell, Document -> XMLCell and InputStream -> XMLCell conversions.
     *
     * @throws Exception When something went wrong
     */
    @Test
    public void testToXMLCell() throws Exception {
        final String xmlString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<KNIME>\n</KNIME>";

        /* from String */
        {
            final XMLCell xmlCell =
                testSimpleConversion(String.class, XMLCell.TYPE, XMLCell.class, new String("<KNIME />"));
            assertEquals(xmlCell.getStringValue(), xmlString);
        }

        /* from Document */
        {
            final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            assertNotNull(documentBuilderFactory);
            final DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
            final XMLCell xmlCell = testSimpleConversion(Document.class, XMLCell.TYPE, XMLCell.class,
                builder.parse(new InputSource(new StringReader(xmlString))));
            assertEquals(xmlCell.getStringValue(), xmlString);
        }

        /* from InputStream */
        {
            final InputStream stream = getClass().getResourceAsStream("test.xml");
            assertTrue(stream.available() > 0);
            final XMLCell xmlCell = testSimpleConversion(InputStream.class, XMLCell.TYPE, XMLCell.class, stream);
            assertEquals(xmlCell.getStringValue(), xmlString);
        }
    }

    /**
     * Test byte[] -> BinaryObjectDataCell and InputStream -> BinaryObjectDataCell conversions.
     *
     * @throws Exception When something went wrong
     */
    @Test
    public void testToBinaryObjectDataCell() throws Exception {
        {
            final BinaryObjectDataCell cell = testSimpleConversion(byte[].class, BinaryObjectDataCell.TYPE,
                BinaryObjectDataCell.class, new String("I am bytes.").getBytes());
            final InputStream stream = cell.openInputStream();
            assertEquals("I am bytes.", new BufferedReader(new InputStreamReader(stream)).readLine());
        }
        {
            final BinaryObjectDataCell cell = testSimpleConversion(InputStream.class, BinaryObjectDataCell.TYPE,
                BinaryObjectDataCell.class, new ByteArrayInputStream("I am bytes.".getBytes()));
            final InputStream stream = cell.openInputStream();
            assertEquals("I am bytes.", new BufferedReader(new InputStreamReader(stream)).readLine());
        }
    }

    /**
     * Test Integer[] -> ListCell(IntCell) conversion.
     *
     * @throws Exception When something went wrong
     */
    @Test
    public void testCollectionTypes() throws Exception {
        final Integer[] coll = {0, 1, 4, 9, 16, 25};

        final Optional<JavaToDataCellConverterFactory<Integer[]>> factory = JavaToDataCellConverterRegistry
            .getInstance().getConverterFactory(Integer[].class, ListCell.getCollectionType(IntCell.TYPE));
        assertTrue(factory.isPresent());

        final JavaToDataCellConverter<Integer[]> converter = factory.get().create(null);
        assertNotNull(converter);

        final DataCell cell = converter.convert(coll);
        assertTrue(cell instanceof ListCell);

        final ListCell listCell = (ListCell)converter.convert(coll);
        for (int i = 0; i < 6; ++i) {
            assertEquals(((IntCell)listCell.get(i)).getIntValue(), i * i);
        }
    }

    /**
     * Test destination types of Integer and FileInputStream.
     */
    @Test
    public void testDestTypes() {
        final Collection<DataType> destTypes =
            JavaToDataCellConverterRegistry.getInstance().getFactoriesForSourceType(Integer.class).stream()
                .map((factory) -> factory.getDestinationType()).collect(Collectors.toSet());

        assertEquals(3, destTypes.size());
        assertTrue(destTypes.contains(IntCell.TYPE));
        assertTrue(destTypes.contains(LongCell.TYPE));
        assertTrue(destTypes.contains(StringCell.TYPE));

        final Collection<DataType> supertypeDestTypes =
            JavaToDataCellConverterRegistry.getInstance().getFactoriesForSourceType(FileInputStream.class).stream()
                .map((factory) -> factory.getDestinationType()).collect(Collectors.toSet());
        assertEquals(3, supertypeDestTypes.size());
        assertTrue(supertypeDestTypes.contains(BinaryObjectDataCell.TYPE));
        assertTrue(supertypeDestTypes.contains(XMLCell.TYPE));
        assertTrue(supertypeDestTypes.contains(StringCell.TYPE));
    }

    /**
     * Test destination types of Integer and FileInputStream.
     */
    @Test
    public void testCollectionDestTypes() {
        final Collection<DataType> destTypes =
            JavaToDataCellConverterRegistry.getInstance().getFactoriesForSourceType(Integer[].class).stream()
                .map((factory) -> factory.getDestinationType()).collect(Collectors.toSet());

        assertEquals(4, destTypes.size());
        assertTrue(destTypes.contains(ListCell.getCollectionType(IntCell.TYPE)));
        assertTrue(destTypes.contains(ListCell.getCollectionType(LongCell.TYPE)));
        assertTrue(destTypes.contains(ListCell.getCollectionType(StringCell.TYPE)));
        assertTrue(destTypes.contains(StringCell.TYPE));

        final Collection<DataType> supertypeDestTypes =
            JavaToDataCellConverterRegistry.getInstance().getFactoriesForSourceType(FileInputStream[].class).stream()
                .map((factory) -> factory.getDestinationType()).collect(Collectors.toSet());
        assertEquals(4, supertypeDestTypes.size());
        assertTrue(supertypeDestTypes.contains(ListCell.getCollectionType(BinaryObjectDataCell.TYPE)));
        assertTrue(supertypeDestTypes.contains(ListCell.getCollectionType(XMLCell.TYPE)));
        assertTrue(supertypeDestTypes.contains(ListCell.getCollectionType(StringCell.TYPE)));
        assertTrue(supertypeDestTypes.contains(StringCell.TYPE));
    }

    /**
     * Test source types for DoubeCell.TYPE.
     */
    @Test
    public void testSourceTypes() {
        final Collection<JavaToDataCellConverterFactory<?>> factories =
            JavaToDataCellConverterRegistry.getInstance().getFactoriesForDestinationType(DoubleCell.TYPE);

        final Collection<Class<?>> set =
            factories.stream().map((factory) -> factory.getSourceType()).collect(Collectors.toSet());

        assertEquals(1, set.size());
        assertTrue(set.contains(Double.class));
    }

    /**
     * Test source types for ListCell(IntCell.TYPE).
     */
    @Test
    public void testArraySourceTypes() {
        final Collection<JavaToDataCellConverterFactory<?>> factories = JavaToDataCellConverterRegistry.getInstance()
            .getFactoriesForDestinationType(ListCell.getCollectionType(IntCell.TYPE));

        final Collection<Class<?>> set =
            factories.stream().map((factory) -> factory.getSourceType()).collect(Collectors.toSet());

        assertEquals(2, factories.size());
        assertTrue(set.contains(Integer[].class));
        assertTrue(set.contains(String[].class));
    }

    /**
     * Test {@link JavaToDataCellConverterRegistry#getAllSourceTypes()}.
     */
    @Test
    public void testAllSourceTypes() {
        final Collection<Class<?>> set = JavaToDataCellConverterRegistry.getInstance().getAllSourceTypes();

        assertEquals(10, set.size());
        assertTrue(set.contains(Double.class));
    }

    /**
     * Test {@link JavaToDataCellConverterRegistry#getAllDestinationTypes()}.
     */
    @Test
    public void testAllDestinationTypes() {
        final Collection<DataType> set = JavaToDataCellConverterRegistry.getInstance().getAllDestinationTypes();

        assertEquals(7, set.size());
        assertTrue(set.contains(DoubleCell.TYPE));
    }
}
