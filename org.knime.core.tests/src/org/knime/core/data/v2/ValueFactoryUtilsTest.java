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

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.IntValue;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.filestore.internal.NotInWorkflowDataRepository;
import org.knime.core.data.filestore.internal.NotInWorkflowWriteFileStoreHandler;
import org.knime.core.data.image.png.PNGImageCellFactory;
import org.knime.core.data.v2.value.DefaultRowKeyValueFactory;
import org.knime.core.data.v2.value.DictEncodedStringValueFactory;
import org.knime.core.data.v2.value.DoubleValueFactory;
import org.knime.core.data.v2.value.IntListValueFactory;
import org.knime.core.data.v2.value.IntValueFactory;
import org.knime.core.data.v2.value.ListValueFactory;
import org.knime.core.data.v2.value.SparseListValueFactory;
import org.knime.core.data.v2.value.VoidRowKeyFactory;
import org.knime.core.data.v2.value.VoidValueFactory;
import org.knime.core.data.v2.value.cell.DataCellValueFactory;
import org.knime.core.data.v2.value.cell.DictEncodedDataCellValueFactory;
import org.knime.core.data.xml.XMLCell;
import org.knime.core.table.access.IntAccess.IntReadAccess;
import org.knime.core.table.access.IntAccess.IntWriteAccess;
import org.knime.core.table.schema.DataSpec;
import org.knime.core.table.schema.traits.DataTrait;
import org.knime.core.table.schema.traits.DataTrait.DictEncodingTrait;
import org.knime.core.table.schema.traits.DataTraits;
import org.knime.core.table.schema.traits.DefaultDataTraits;
import org.knime.core.table.schema.traits.DefaultListDataTraits;
import org.knime.core.table.schema.traits.ListDataTraits;
import org.knime.core.table.schema.traits.LogicalTypeTrait;
import org.knime.core.table.schema.traits.StructDataTraits;

/**
 * Contains unit tests for {@link ValueFactoryUtils}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class ValueFactoryUtilsTest {

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
        assertThat(ValueFactoryUtils.areEqual(createLegacyDataCellValueFactory(XMLCell.TYPE),
            createLegacyDataCellValueFactory(XMLCell.TYPE))).isTrue();

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
        var valueFactory = new DictEncodedStringValueFactory();
        var traits = ValueFactoryUtils.getTraits(valueFactory);
        assertTrue(traits.hasTrait(LogicalTypeTrait.class));
        assertTrue(traits.hasTrait(DictEncodingTrait.class));
        assertEquals(valueFactory.getClass().getName(), traits.get(LogicalTypeTrait.class).getLogicalType());
    }

    @Test
    void testGetTraitsOnDataCellValueFactory() {
        var valueFactory = createDataCellValueFactory();
        var traits = ValueFactoryUtils.getTraits(valueFactory);
        assertTrue(traits.hasTrait(LogicalTypeTrait.class));
        assertThat(traits).isInstanceOf(StructDataTraits.class);
        assertThat(traits.get(LogicalTypeTrait.class).getLogicalType())//
            .isEqualTo(DictEncodedDataCellValueFactory.class.getName() + ";" + XMLCell.TYPE.getCellClass().getName());
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
        .matches(this::isListTypeTrait)
        .extracting(ListDataTraits.class::cast)//
        .extracting(ListDataTraits::getInner)//
        .matches(this::hasLogicalTypeTrait)//
        .extracting(t -> t.get(LogicalTypeTrait.class).getLogicalType())//
        .isEqualTo(IntValueFactory.class.getName());

        // collection of data cell value factories
        var listOfDataCells = createListValueFactory(createDataCellValueFactory(), XMLCell.TYPE);
        assertThat(ValueFactoryUtils.getTraits(listOfDataCells))//
            .isInstanceOf(ListDataTraits.class)//
            .matches(this::hasLogicalTypeTrait)//
            .matches(this::isListTypeTrait)//
            .extracting(ListDataTraits.class::cast)//
            .extracting(ListDataTraits::getInner)//
            .extracting(t -> DataTraits.getTrait(t, LogicalTypeTrait.class))//
            .matches(Optional::isPresent)//
            .extracting(Optional::get)//
            .extracting(LogicalTypeTrait::getLogicalType)//
            .isEqualTo(DictEncodedDataCellValueFactory.class.getName() + ";" + XMLCell.class.getName());

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
            .extracting(this::extractLogicalTypeTrait)//
            .isEqualTo(IntValueFactory.class.getName());

        // SparseListValueFactory (uses StructDataTraits)
        var sparseList = new SparseListValueFactory();
        sparseList.initialize(IntValueFactory.INSTANCE, IntCell.TYPE);
        assertThat(ValueFactoryUtils.getTraits(sparseList))//
        .isInstanceOf(StructDataTraits.class)//
        .extracting(StructDataTraits.class::cast)//
        .matches(this::hasLogicalTypeTrait)//
        .matches(t -> t.get(LogicalTypeTrait.class).getLogicalType().equals(SparseListValueFactory.class.getName()))//
        .extracting(t -> t.getDataTraits(0))//
        .matches(this::hasLogicalTypeTrait)//
        .extracting(t -> t.get(LogicalTypeTrait.class).getLogicalType())
        .isEqualTo(IntValueFactory.class.getName());

    }

    private String extractLogicalTypeTrait(final DataTraits traits) {
        return traits.get(LogicalTypeTrait.class).getLogicalType();
    }

    private boolean isListTypeTrait(final DataTraits traits) {
        return traits.get(LogicalTypeTrait.class).getLogicalType().equals(ListValueFactory.class.getName());
    }

    private boolean hasLogicalTypeTrait(final DataTraits traits) {
        return traits.hasTrait(LogicalTypeTrait.class);
    }

    @Test
    void testGetDataTypeForValueFactory() throws Exception {
        // void
        assertThrows(IllegalArgumentException.class,
            () -> ValueFactoryUtils.getDataTypeForValueFactory(VoidValueFactory.INSTANCE));

        // ordinary
        assertThat(ValueFactoryUtils.getDataTypeForValueFactory(IntValueFactory.INSTANCE)).isEqualTo(IntCell.TYPE);

        // specific collection
        assertThat(ValueFactoryUtils.getDataTypeForValueFactory(IntListValueFactory.INSTANCE))//
            .isEqualTo(DataType.getType(ListCell.class, IntCell.TYPE));

        // dict encoded data cell value factory
        assertThat(ValueFactoryUtils.getDataTypeForValueFactory(createDataCellValueFactory()))//
            .isEqualTo(XMLCell.TYPE);

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
        var traits = new DefaultDataTraits(new LogicalTypeTrait(DefaultRowKeyValueFactory.class.getName()));
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
        assertThat(ValueFactoryUtils.getValueFactory(XMLCell.TYPE, fsHandler))//
            .isInstanceOf(DictEncodedDataCellValueFactory.class)//
            .extracting(DictEncodedDataCellValueFactory.class::cast)//
            .extracting(DictEncodedDataCellValueFactory::getType)//
            .isEqualTo(XMLCell.TYPE);

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
    void testLoadValueFactory() {
        var dataRepo = NotInWorkflowDataRepository.newInstance();

        // no logical type trait
        assertThrows(IllegalArgumentException.class,
            () -> ValueFactoryUtils.loadValueFactory(DefaultDataTraits.EMPTY, dataRepo));

        // void
        var voidTrait = createSimpleTypeTraits(VoidValueFactory.class);
        assertThat(ValueFactoryUtils.loadValueFactory(voidTrait, dataRepo)).isEqualTo(VoidValueFactory.INSTANCE);

        // ordinary
        DataTraits traits = createSimpleTypeTraits(IntValueFactory.class);
        assertThat(ValueFactoryUtils.loadValueFactory(traits, dataRepo))//
            .isInstanceOf(IntValueFactory.class);

        // specific collection
        traits = new DefaultListDataTraits(new DataTrait[]{new LogicalTypeTrait(IntListValueFactory.class.getName())},
            DefaultDataTraits.EMPTY);
        assertThat(ValueFactoryUtils.loadValueFactory(traits, dataRepo))//
            .isEqualTo(IntListValueFactory.INSTANCE);

        // nested collection
        traits =
            new DefaultListDataTraits(new DataTrait[]{new LogicalTypeTrait(ListValueFactory.class.getName())}, traits);
        assertThat(ValueFactoryUtils.loadValueFactory(traits, dataRepo))//
            .isInstanceOf(ListValueFactory.class)//
            .extracting(ListValueFactory.class::cast)//
            .extracting(ListValueFactory::getElementValueFactory)//
            .isEqualTo(IntListValueFactory.INSTANCE);

        // data cell value factory
        traits = createSimpleTypeTraits(
            DictEncodedDataCellValueFactory.class.getName() + ";" + XMLCell.TYPE.getCellClass().getName());
        assertThat(ValueFactoryUtils.loadValueFactory(traits, dataRepo))//
            .isInstanceOf(DictEncodedDataCellValueFactory.class)//
            .extracting(DictEncodedDataCellValueFactory.class::cast)//
            .extracting(DictEncodedDataCellValueFactory::getType)//
            .isEqualTo(XMLCell.TYPE);

        // data cell value factory with no cell type
        final var noCellType = createSimpleTypeTraits(DictEncodedDataCellValueFactory.class.getName() + ";");
        assertThrows(IllegalArgumentException.class, () -> ValueFactoryUtils.loadValueFactory(noCellType, dataRepo));

        final var bogusCellType = createSimpleTypeTraits(DictEncodedDataCellValueFactory.class.getName() + ";bogus");
        assertThrows(IllegalArgumentException.class, () -> ValueFactoryUtils.loadValueFactory(bogusCellType, dataRepo));

        // try to load an unregistered value factory
        final var finalTraits = createSimpleTypeTraits(DummyDataValueFactory.class.getName());
        assertThrows(IllegalArgumentException.class, () -> ValueFactoryUtils.loadValueFactory(finalTraits, dataRepo));

    }

    @Test
    void testInstantiatValueFactory() {
        // exception in constructor
        assertThrows(IllegalStateException.class,
            () -> ValueFactoryUtils.instantiateValueFactory(ThrowingConstructorValueFactory.class));
        // no empty public constructor
        assertThrows(IllegalStateException.class,
            () -> ValueFactoryUtils.instantiateValueFactory(NoPublicEmptyConstructor.class));
    }

    private static DataTraits createSimpleTypeTraits(final String logicalType) {
        return new DefaultDataTraits(new LogicalTypeTrait(logicalType));
    }

    private static DataTraits createSimpleTypeTraits(final Class<? extends ValueFactory<?, ?>> valueFactoryClass) {
        return createSimpleTypeTraits(valueFactoryClass.getName().toString());
    }

    private static DictEncodedDataCellValueFactory createDataCellValueFactory() {
        return createDataCellValueFactory(XMLCell.TYPE);
    }

    private static DictEncodedDataCellValueFactory createDataCellValueFactory(final DataType type) {
        var fsHandler = NotInWorkflowWriteFileStoreHandler.create();
        return new DictEncodedDataCellValueFactory(fsHandler, type);
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
