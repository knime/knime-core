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
 *   Oct 18, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.data.v2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.IntValue;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.filestore.internal.NotInWorkflowDataRepository;
import org.knime.core.data.filestore.internal.NotInWorkflowWriteFileStoreHandler;
import org.knime.core.data.image.png.PNGImageCellFactory;
import org.knime.core.data.v2.value.DefaultRowKeyValueFactory;
import org.knime.core.data.v2.value.DoubleValueFactory;
import org.knime.core.data.v2.value.IntListValueFactory;
import org.knime.core.data.v2.value.IntSetValueFactory;
import org.knime.core.data.v2.value.IntValueFactory;
import org.knime.core.data.v2.value.ListValueFactory;
import org.knime.core.data.v2.value.SparseListValueFactory;
import org.knime.core.data.v2.value.StringValueFactory;
import org.knime.core.data.v2.value.VoidRowKeyFactory;
import org.knime.core.data.v2.value.VoidValueFactory;
import org.knime.core.data.v2.value.cell.DataCellValueFactory;
import org.knime.core.data.v2.value.cell.DictEncodedDataCellValueFactory;
import org.knime.core.data.xml.XMLCell;
import org.knime.core.node.NodeSettings;
import org.knime.core.table.access.IntAccess.IntReadAccess;
import org.knime.core.table.access.IntAccess.IntWriteAccess;
import org.knime.core.table.schema.DataSpec;
import org.knime.core.table.schema.traits.DataTrait.DictEncodingTrait;
import org.knime.core.table.schema.traits.DataTraits;
import org.knime.core.table.schema.traits.DefaultDataTraits;
import org.knime.core.table.schema.traits.ListDataTraits;
import org.knime.core.table.schema.traits.LogicalTypeTrait;
import org.knime.core.table.schema.traits.StructDataTraits;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Contains unit tests for {@link ValueFactoryUtils}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("static-method")
final class ValueFactoryUtilsTest {

    @SuppressWarnings({"javadoc", "serial"})
    public static class DataCellWithoutValueFactory extends DataCell {
        public static final DataType TYPE = DataType.getType(DataCellWithoutValueFactory.class);

        public DataCellWithoutValueFactory() {

        }

        @Override
        public String toString() {
            return this.getClass().getName();
        }

        @Override
        protected boolean equalsDataCell(final DataCell dc) {
            return (dc instanceof DataCellWithoutValueFactory);
        }

        @Override
        public int hashCode() {
            return 42;
        }

    }

    @SuppressWarnings("javadoc")
    public static class DataCellWithoutValueFactorySerializer implements DataCellSerializer<DataCellWithoutValueFactory> {

        @Override
        public void serialize(final DataCellWithoutValueFactory cell, final DataCellDataOutput output) throws IOException {
            //
        }

        @Override
        public DataCellWithoutValueFactory deserialize(final DataCellDataInput input) throws IOException {
            return new DataCellWithoutValueFactory();
        }

    }

    @Test
    void testAreEqual() {
        // both null
        assertThat(ValueFactoryUtils.areEqual(null, null)).isTrue();

        // first null
        assertThat(ValueFactoryUtils.areEqual(null, IntValueFactory.INSTANCE)).isFalse();

        // second null
        assertThat(ValueFactoryUtils.areEqual(IntValueFactory.INSTANCE, null)).isFalse();

        // same instance
        assertThat(ValueFactoryUtils.areEqual(IntValueFactory.INSTANCE, IntValueFactory.INSTANCE)).isTrue();

        // different types
        assertThat(ValueFactoryUtils.areEqual(IntValueFactory.INSTANCE, DoubleValueFactory.INSTANCE)).isFalse();

        // same type different instances
        assertThat(ValueFactoryUtils.areEqual(IntValueFactory.INSTANCE, new IntValueFactory())).isTrue();

        // dict encoded data cell value factory same type
        assertThat(ValueFactoryUtils.areEqual(createDataCellValueFactory(), createDataCellValueFactory())).isTrue();

        // dict encoded data cell value factory different type
        assertThat(ValueFactoryUtils.areEqual(createDataCellValueFactory(),
            createDataCellValueFactory(PNGImageCellFactory.TYPE))).isFalse();

        // legacy data cell value factory same type
        assertThat(ValueFactoryUtils.areEqual(createLegacyDataCellValueFactory(DataCellWithoutValueFactory.TYPE),
            createLegacyDataCellValueFactory(DataCellWithoutValueFactory.TYPE))).isTrue();

        // legacy dta cell value factory different type
        assertThat(ValueFactoryUtils.areEqual(createLegacyDataCellValueFactory(XMLCell.TYPE),
            createLegacyDataCellValueFactory(PNGImageCellFactory.TYPE))).isFalse();

        // collection same inner type
        assertThat(ValueFactoryUtils.areEqual(createListValueFactory(IntValueFactory.INSTANCE, IntCell.TYPE),
            createListValueFactory(IntValueFactory.INSTANCE, IntCell.TYPE))).isTrue();

        // collection differing inner type
        assertThat(ValueFactoryUtils.areEqual(createListValueFactory(IntValueFactory.INSTANCE, IntCell.TYPE),
            createListValueFactory(DoubleValueFactory.INSTANCE, DoubleCell.TYPE))).isFalse();

        // nested collections same inner type
        assertThat(ValueFactoryUtils.areEqual(
            createListValueFactory(createListValueFactory(IntValueFactory.INSTANCE, IntCell.TYPE),
                DataType.getType(ListCell.class, IntCell.TYPE)),
            createListValueFactory(createListValueFactory(IntValueFactory.INSTANCE, IntCell.TYPE),
                DataType.getType(ListCell.class, IntCell.TYPE)))).isTrue();

        // nested collections different inner type
        assertThat(ValueFactoryUtils.areEqual(
            createListValueFactory(createListValueFactory(IntValueFactory.INSTANCE, IntCell.TYPE),
                DataType.getType(ListCell.class, IntCell.TYPE)),
            createListValueFactory(createListValueFactory(DoubleValueFactory.INSTANCE, DoubleCell.TYPE),
                DataType.getType(ListCell.class, DoubleCell.TYPE)))).isFalse();
    }

    @Test
    void testGetTraitsOnOrdinaryValueFactory() {
        var valueFactory = new StringValueFactory();
        var traits = ValueFactoryUtils.getTraits(valueFactory);
        assertTrue(traits.hasTrait(LogicalTypeTrait.class));
        var logicalTypeJson = extractLogicalTypeJson(traits);
        assertEquals(valueFactory.getClass().getName(), logicalTypeJson.get("value_factory_class").asText());
    }

    private static JsonNode extractLogicalTypeJson(final DataTraits traits) {
        var logicalTypeString = traits.get(LogicalTypeTrait.class).getLogicalType();
        try {
            return new ObjectMapper().readTree(logicalTypeString);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void testGetTraitsOnDataCellValueFactory() {
        var valueFactory = createDataCellValueFactory();
        var traits = ValueFactoryUtils.getTraits(valueFactory);
        assertTrue(traits.hasTrait(LogicalTypeTrait.class));
        assertThat(traits).isInstanceOf(StructDataTraits.class);
        var logicalTypeJson = extractLogicalTypeJson(traits);
        assertThat(logicalTypeJson.get("value_factory_class").asText())//
            .isEqualTo(DictEncodedDataCellValueFactory.class.getName());
        var dataTypeJson = logicalTypeJson.get("data_type");
        assertThat(dataTypeJson.get("cell_class").asText())//
            .isEqualTo(DataCellWithoutValueFactory.TYPE.getCellClass().getName());
        var structTraits = (StructDataTraits)traits;
        assertTrue(structTraits.getDataTraits(0).hasTrait(DictEncodingTrait.class));
        assertTrue(structTraits.getDataTraits(1).hasTrait(DictEncodingTrait.class));
    }

    @Test
    void testGetTraitsOnCollectionValueFactory() {
        var valueFactory = new ListValueFactory();
        valueFactory.initialize(new IntValueFactory(), IntCell.TYPE);
        assertThat(ValueFactoryUtils.getTraits(valueFactory))//
            .isInstanceOf(ListDataTraits.class)//
            .matches(this::hasLogicalTypeTrait)//
            .matches(this::isListTypeTrait).extracting(ListDataTraits.class::cast)//
            .extracting(ListDataTraits::getInner)//
            .matches(this::hasLogicalTypeTrait)//
            .extracting(ValueFactoryUtilsTest::extractValueFactoryClassName)//
            .isEqualTo(IntValueFactory.class.getName());

        // collection of data cell value factories
        var listOfDataCells = createListValueFactory(createDataCellValueFactory(), DataCellWithoutValueFactory.TYPE);
        assertThat(ValueFactoryUtils.getTraits(listOfDataCells))//
            .isInstanceOf(ListDataTraits.class)//
            .matches(this::hasLogicalTypeTrait)//
            .matches(this::isListTypeTrait)//
            .extracting(ListDataTraits.class::cast)//
            .extracting(ListDataTraits::getInner)//
            .extracting(ValueFactoryUtilsTest::extractLogicalTypeJson)//
            .matches(j -> j.get("value_factory_class").asText().equals(DictEncodedDataCellValueFactory.class.getName()))
            .extracting(j -> j.get("data_type"))//
            .extracting(j -> j.get("cell_class").asText())//
            .isEqualTo(DataCellWithoutValueFactory.class.getName());

        // nested collection
        var nestedCollection = createListValueFactory(createListValueFactory(IntValueFactory.INSTANCE, IntCell.TYPE),
            DataType.getType(ListCell.class, IntCell.TYPE));
        assertThat(ValueFactoryUtils.getTraits(nestedCollection))//
            .isInstanceOf(ListDataTraits.class)//
            .matches(this::hasLogicalTypeTrait)//
            .matches(this::isListTypeTrait)//
            .extracting(ListDataTraits.class::cast)//
            .extracting(ListDataTraits::getInner)//
            .matches(this::hasLogicalTypeTrait)//
            .matches(this::isListTypeTrait)//
            .extracting(ListDataTraits.class::cast)//
            .extracting(ListDataTraits::getInner)//
            .matches(this::hasLogicalTypeTrait)//
            .extracting(ValueFactoryUtilsTest::extractValueFactoryClassName)//
            .isEqualTo(IntValueFactory.class.getName());

        // SparseListValueFactory (uses StructDataTraits)
        var sparseList = new SparseListValueFactory();
        sparseList.initialize(IntValueFactory.INSTANCE, IntCell.TYPE);
        assertThat(ValueFactoryUtils.getTraits(sparseList))//
            .isInstanceOf(StructDataTraits.class)//
            .extracting(StructDataTraits.class::cast)//
            .matches(this::hasLogicalTypeTrait)//
            .matches(t -> extractValueFactoryClassName(t).equals(SparseListValueFactory.class.getName()))//
            .extracting(t -> t.getDataTraits(0))//
            .matches(this::hasLogicalTypeTrait)//
            .extracting(ValueFactoryUtilsTest::extractValueFactoryClassName)//
            .isEqualTo(IntValueFactory.class.getName());

    }

    private String extractLogicalTypeTrait(final DataTraits traits) {
        return traits.get(LogicalTypeTrait.class).getLogicalType();
    }

    private boolean isListTypeTrait(final DataTraits traits) {
        return extractValueFactoryClassName(traits).equals(ListValueFactory.class.getName());
    }

    private static String extractValueFactoryClassName(final DataTraits traits) {
        return extractLogicalTypeJson(traits).get("value_factory_class").asText();
    }

    private boolean hasLogicalTypeTrait(final DataTraits traits) {
        return traits.hasTrait(LogicalTypeTrait.class);
    }

    @Test
    void testGetDataTypeForValueFactory() throws Exception {
        // void
        assertThat(ValueFactoryUtils.getDataTypeForValueFactory(VoidValueFactory.INSTANCE))
            .isEqualTo(DataType.getType(DataCell.class));

        // ordinary
        assertThat(ValueFactoryUtils.getDataTypeForValueFactory(IntValueFactory.INSTANCE)).isEqualTo(IntCell.TYPE);

        // specific collection
        assertThat(ValueFactoryUtils.getDataTypeForValueFactory(IntListValueFactory.INSTANCE))//
            .isEqualTo(DataType.getType(ListCell.class, IntCell.TYPE));

        // dict encoded data cell value factory
        assertThat(ValueFactoryUtils.getDataTypeForValueFactory(createDataCellValueFactory()))//
            .isEqualTo(DataCellWithoutValueFactory.TYPE);

        // legacy data cell value factory
        @SuppressWarnings("deprecation")
        var legacyFactory = new DataCellValueFactory(new DataCellSerializerFactory(),
            NotInWorkflowWriteFileStoreHandler.create(), XMLCell.TYPE);
        assertThat(ValueFactoryUtils.getDataTypeForValueFactory(legacyFactory))//
            .isEqualTo(XMLCell.TYPE);

        // nested (general) collection
        // this value factory is not encountered in the wild because there exists a specialized factory
        // however it works well to test the behavior
        assertThat(ValueFactoryUtils
            .getDataTypeForValueFactory(createListValueFactory(IntValueFactory.INSTANCE, IntCell.TYPE)))//
                .isEqualTo(DataType.getType(ListCell.class, IntCell.TYPE));
    }

    @Test
    void testGetRowKeyValueFactoryFromRowKeyType() {
        assertThat(ValueFactoryUtils.getRowKeyValueFactory(RowKeyType.CUSTOM))//
            .isEqualTo(DefaultRowKeyValueFactory.INSTANCE);
        assertThat(ValueFactoryUtils.getRowKeyValueFactory(RowKeyType.NOKEY))//
            .isEqualTo(VoidRowKeyFactory.INSTANCE);
    }

    @Test
    void testGetRowKeyValueFactoryFromTraits() {
        var traits = ValueFactoryUtils.getTraits(DefaultRowKeyValueFactory.INSTANCE);
        assertThat(ValueFactoryUtils.loadRowKeyValueFactory(traits))//
            .isInstanceOf(DefaultRowKeyValueFactory.class);
    }

    @Test
    void testGetDataValueFactoryForType() {
        var fsHandler = NotInWorkflowWriteFileStoreHandler.create();

        // void
        assertThat(ValueFactoryUtils.getValueFactory(null, fsHandler)).isEqualTo(VoidValueFactory.INSTANCE);

        // extension point
        assertThat(ValueFactoryUtils.getValueFactory(IntCell.TYPE, fsHandler))//
            .isInstanceOf(IntValueFactory.class);

        // data cell value factory
        assertThat(ValueFactoryUtils.getValueFactory(DataCellWithoutValueFactory.TYPE, fsHandler))//
            .isInstanceOf(DictEncodedDataCellValueFactory.class)//
            .extracting(DictEncodedDataCellValueFactory.class::cast)//
            .extracting(DictEncodedDataCellValueFactory::getType)//
            .isEqualTo(DataCellWithoutValueFactory.TYPE);

        // specific collection
        var intList = DataType.getType(ListCell.class, IntCell.TYPE);
        assertThat(ValueFactoryUtils.getValueFactory(intList, fsHandler))//
            .isEqualTo(IntListValueFactory.INSTANCE);

        // general nested collection
        assertThat(ValueFactoryUtils.getValueFactory(DataType.getType(ListCell.class, intList), fsHandler))//
            .isInstanceOf(ListValueFactory.class)//
            .extracting(ListValueFactory.class::cast)//
            .extracting(ListValueFactory::getElementValueFactory)//
            .isEqualTo(IntListValueFactory.INSTANCE);
    }

    @Test
    void testGetTypeNameForLogicalTypeString() {
        // void
        String logicalVoid = "{\"value_factory_class\":\"org.knime.core.data.v2.value.VoidValueFactory\"}";
        assertThat(ValueFactoryUtils.getTypeNameForLogicalTypeString(logicalVoid)).isEqualTo("DataCell");

        // JSON contains data_type
        String logicalURI = "{\"value_factory_class\":\"org.knime.core.data.v2.value.cell.DictEncodedDataCellValueFactory\",\"data_type\":{\"cell_class\":\"org.knime.core.data.uri.URIDataCell\"}}";
        assertThat(ValueFactoryUtils.getTypeNameForLogicalTypeString(logicalURI)).isEqualTo("URI");

        // collection, value factory registered in SPECIFIC_COLLECTION_FACTORY_PROVIDER
        String logicalStringSet = "{\"value_factory_class\": \"org.knime.core.data.v2.value.StringSetValueFactory\"}";
        assertThat(ValueFactoryUtils.getTypeNameForLogicalTypeString(logicalStringSet)).isEqualTo("Set");

        // not-registered value factory
        String logicalPeriod = "{\"value_factory_class\":\"org.knime.core.data.v2.time.PeriodValueFactory\"}";
        assertThat(ValueFactoryUtils.getTypeNameForLogicalTypeString(logicalPeriod)).isEqualTo("Period");

        // collection, not-registered value factory
        String logicalSet = "{\"value_factory_class\":\"org.knime.core.data.v2.value.SetValueFactory\"}";
        assertThat(ValueFactoryUtils.getTypeNameForLogicalTypeString(logicalSet)).isEqualTo("Set");
    }

    @Test
    void testLoadValueFactory() {
        var dataRepo = NotInWorkflowDataRepository.newInstance();

        // no logical type trait
        assertThrows(IllegalArgumentException.class,
            () -> ValueFactoryUtils.loadValueFactory(DefaultDataTraits.EMPTY, dataRepo));

        // void
        var voidTrait = ValueFactoryUtils.getTraits(VoidValueFactory.INSTANCE);
        assertThat(ValueFactoryUtils.loadValueFactory(voidTrait, dataRepo)).isEqualTo(VoidValueFactory.INSTANCE);

        // ordinary
        DataTraits traits = ValueFactoryUtils.getTraits(IntValueFactory.INSTANCE);
        assertThat(ValueFactoryUtils.loadValueFactory(traits, dataRepo))//
            .isInstanceOf(IntValueFactory.class);

        // specific collection
        traits = ValueFactoryUtils.getTraits(IntListValueFactory.INSTANCE);
        assertThat(ValueFactoryUtils.loadValueFactory(traits, dataRepo))//
            .isEqualTo(IntListValueFactory.INSTANCE);

        // nested collection
        var nestedListValueFactory = new ListValueFactory();
        nestedListValueFactory.initialize(IntListValueFactory.INSTANCE,
            DataType.getType(ListCell.class, DataType.getType(ListCell.class, IntCell.TYPE)));
        traits = ValueFactoryUtils.getTraits(nestedListValueFactory);
        assertThat(ValueFactoryUtils.loadValueFactory(traits, dataRepo))//
            .isInstanceOf(ListValueFactory.class)//
            .extracting(ListValueFactory.class::cast)//
            .extracting(ListValueFactory::getElementValueFactory)//
            .isEqualTo(IntListValueFactory.INSTANCE);

        // data cell value factory
        var dictEncodedDataCellValueFactory = createDataCellValueFactory();
        traits = ValueFactoryUtils.getTraits(dictEncodedDataCellValueFactory);
        assertThat(ValueFactoryUtils.loadValueFactory(traits, dataRepo))//
            .isInstanceOf(DictEncodedDataCellValueFactory.class)//
            .extracting(DictEncodedDataCellValueFactory.class::cast)//
            .extracting(DictEncodedDataCellValueFactory::getType)//
            .isEqualTo(DataCellWithoutValueFactory.TYPE);

        // try to load an unregistered value factory
        final var finalTraits = createSimpleTypeTraits(DummyDataValueFactory.class.getName());
        assertThrows(IllegalArgumentException.class, () -> ValueFactoryUtils.loadValueFactory(finalTraits, dataRepo));
    }

    @Test
    void testInstantiateValueFactory() {
        // exception in constructor
        assertThrows(IllegalStateException.class,
            () -> ValueFactoryUtils.instantiateValueFactory(ThrowingConstructorValueFactory.class));
        // no empty public constructor
        assertThrows(IllegalStateException.class,
            () -> ValueFactoryUtils.instantiateValueFactory(NoPublicEmptyConstructor.class));
    }


    @Test
    void testSaveToNodeSettings() throws Exception {
        var settings = new NodeSettings("test");
        ValueFactoryUtils.saveValueFactory(new StringValueFactory(), settings.addNodeSettings("simple"));
        ValueFactoryUtils.saveValueFactory(IntSetValueFactory.INSTANCE, settings.addNodeSettings("specificCollection"));
        var listValueFactory = new ListValueFactory();
        listValueFactory.initialize(new StringValueFactory(), StringCell.TYPE);
        ValueFactoryUtils.saveValueFactory(listValueFactory, settings.addNodeSettings("genericCollection"));
        var dataCellValueFactory = new DictEncodedDataCellValueFactory(StringCell.TYPE);
        ValueFactoryUtils.saveValueFactory(dataCellValueFactory, settings.addNodeSettings("dataCell"));
        ValueFactoryUtils.saveValueFactory(VoidValueFactory.INSTANCE, settings.addNodeSettings("void"));

        assertEquals(createExpectedNodeSettings(), settings, "Unexpected settings structure");
    }

    @Test
    void testLoadFromNodeSettings() throws Exception {
        var settings = createExpectedNodeSettings();
        var dataRepo = NotInWorkflowDataRepository.newInstance();
        var simpleValueFactory = ValueFactoryUtils.loadValueFactory(settings.getNodeSettings("simple"), dataRepo);
        assertThat(simpleValueFactory).isExactlyInstanceOf(StringValueFactory.class);
        var specificCollectionValueFactory =
            ValueFactoryUtils.loadValueFactory(settings.getNodeSettings("specificCollection"), dataRepo);
        assertEquals(IntSetValueFactory.INSTANCE, specificCollectionValueFactory);
        var genericCollectionValueFactory =
            ValueFactoryUtils.loadValueFactory(settings.getNodeSettings("genericCollection"), dataRepo);
        assertThat(genericCollectionValueFactory).isExactlyInstanceOf(ListValueFactory.class);
        assertThat(((ListValueFactory)genericCollectionValueFactory).getElementValueFactory())//
            .as("Check CollectionValueFactory type.")//
            .isExactlyInstanceOf(StringValueFactory.class);
        var dataCellValueFactory = ValueFactoryUtils.loadValueFactory(settings.getNodeSettings("dataCell"), dataRepo);
        assertThat(dataCellValueFactory).isExactlyInstanceOf(DictEncodedDataCellValueFactory.class);
        assertEquals(StringCell.TYPE, ((DictEncodedDataCellValueFactory)dataCellValueFactory).getType());
        var voidValueFactory = ValueFactoryUtils.loadValueFactory(settings.getNodeSettings("void"), dataRepo);
        assertEquals(VoidValueFactory.INSTANCE, voidValueFactory, "Unexpected void value factory.");
    }

    private static NodeSettings createExpectedNodeSettings() {
        var settings = new NodeSettings("test");
        var simpleSettings = settings.addNodeSettings("simple");
        simpleSettings.addString("valueFactoryName", StringValueFactory.class.getName());
        var specificCollectionSettings = settings.addNodeSettings("specificCollection");
        specificCollectionSettings.addString("valueFactoryName", IntSetValueFactory.class.getName());
        var genericCollectionSettings = settings.addNodeSettings("genericCollection");
        genericCollectionSettings.addString("valueFactoryName", ListValueFactory.class.getName());
        genericCollectionSettings.addNodeSettings("elementValueFactory").addString("valueFactoryName",
            StringValueFactory.class.getName());
        var dataCellSettings = settings.addNodeSettings("dataCell");
        dataCellSettings.addString("valueFactoryName", DictEncodedDataCellValueFactory.class.getName());
        dataCellSettings.addDataType("dataType", StringCell.TYPE);
        settings.addNodeSettings("void").addString("valueFactoryName", VoidValueFactory.class.getName());
        return settings;
    }

    private static DataTraits createSimpleTypeTraits(final String logicalType) {
        return new DefaultDataTraits(new LogicalTypeTrait(logicalType));
    }

    private static DataTraits createSimpleTypeTraits(final Class<? extends ValueFactory<?, ?>> valueFactoryClass) {
        return createSimpleTypeTraits(valueFactoryClass.getName().toString());
    }

    private static DictEncodedDataCellValueFactory createDataCellValueFactory() {
        return createDataCellValueFactory(DataCellWithoutValueFactory.TYPE);
    }

    private static DictEncodedDataCellValueFactory createDataCellValueFactory(final DataType type) {
        var fsHandler = NotInWorkflowWriteFileStoreHandler.create();
        var factory = new DictEncodedDataCellValueFactory(type);
        factory.initializeForWriting(fsHandler);
        return factory;
    }

    private static ListValueFactory createListValueFactory(final ValueFactory<?, ?> elementValueFactory,
        final DataType elementType) {
        var listValueFactory = new ListValueFactory();
        listValueFactory.initialize(elementValueFactory, elementType);
        return listValueFactory;
    }

    @SuppressWarnings("deprecation")
    private static DataCellValueFactory createLegacyDataCellValueFactory(final DataType type) {
        return new DataCellValueFactory(new DataCellSerializerFactory(), NotInWorkflowWriteFileStoreHandler.create(),
            type);
    }

    private static class DummyDataValueFactory implements ValueFactory<IntReadAccess, IntWriteAccess> {

        @Override
        public ReadValue createReadValue(final IntReadAccess access) {
            return new ReadValue() {

                @Override
                public DataCell getDataCell() {
                    return new IntCell(access.getIntValue());
                }
            };
        }

        @Override
        public WriteValue<?> createWriteValue(final IntWriteAccess access) {
            return new WriteValue<IntValue>() {

                @Override
                public void setValue(final IntValue value) {
                    access.setIntValue(value.getIntValue());
                }

            };
        }

        @Override
        public DataSpec getSpec() {
            return DataSpec.intSpec();
        }
    }

    private static class ThrowingConstructorValueFactory extends DummyDataValueFactory {

        public ThrowingConstructorValueFactory() {
            throw new IllegalStateException("Throwing");
        }

    }

    private static class NoPublicEmptyConstructor extends DummyDataValueFactory {
        public NoPublicEmptyConstructor(final String someArgument) {

        }
    }
}
