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
 *   Sep 15, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.data.v2;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DataTypeRegistry;
import org.knime.core.data.IDataRepository;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.collection.SetCell;
import org.knime.core.data.collection.SparseListCell;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;
import org.knime.core.data.v2.value.BooleanListValueFactory;
import org.knime.core.data.v2.value.BooleanSetValueFactory;
import org.knime.core.data.v2.value.BooleanSparseListValueFactory;
import org.knime.core.data.v2.value.DefaultRowKeyValueFactory;
import org.knime.core.data.v2.value.DoubleListValueFactory;
import org.knime.core.data.v2.value.DoubleSetValueFactory;
import org.knime.core.data.v2.value.DoubleSparseListValueFactory;
import org.knime.core.data.v2.value.IntListValueFactory;
import org.knime.core.data.v2.value.IntSetValueFactory;
import org.knime.core.data.v2.value.IntSparseListValueFactory;
import org.knime.core.data.v2.value.LongListValueFactory;
import org.knime.core.data.v2.value.LongSetValueFactory;
import org.knime.core.data.v2.value.LongSparseListValueFactory;
import org.knime.core.data.v2.value.StringListValueFactory;
import org.knime.core.data.v2.value.StringSetValueFactory;
import org.knime.core.data.v2.value.StringSparseListValueFactory;
import org.knime.core.data.v2.value.VoidRowKeyFactory;
import org.knime.core.data.v2.value.VoidValueFactory;
import org.knime.core.data.v2.value.cell.DataCellValueFactory;
import org.knime.core.data.v2.value.cell.DictEncodedDataCellValueFactory;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.table.schema.DataSpec;
import org.knime.core.table.schema.traits.DataTraitUtils;
import org.knime.core.table.schema.traits.DataTraits;
import org.knime.core.table.schema.traits.DefaultListDataTraits;
import org.knime.core.table.schema.traits.DefaultStructDataTraits;
import org.knime.core.table.schema.traits.ListDataTraits;
import org.knime.core.table.schema.traits.LogicalTypeTrait;
import org.knime.core.table.schema.traits.StructDataTraits;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * Utility class for dealing with {@link ValueFactory ValueFactories}. Provides means to create ValueFactories from
 * their class name as well as default ValueFactories for certain {@link DataSpec DataSpecs}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @since 4.5
 * @noreference This class is not intended to be referenced by clients.
 */
public final class ValueFactoryUtils {

    private static final SingletonFactoryProvider SPECIFIC_COLLECTION_FACTORY_PROVIDER =
        SingletonFactoryProvider.builder()//
            // sets
            .with(getSetType(DoubleCell.TYPE), DoubleSetValueFactory.INSTANCE)//
            .with(getSetType(IntCell.TYPE), IntSetValueFactory.INSTANCE)//
            .with(getSetType(LongCell.TYPE), LongSetValueFactory.INSTANCE)//
            .with(getSetType(StringCell.TYPE), StringSetValueFactory.INSTANCE)//
            .with(getSetType(BooleanCell.TYPE), BooleanSetValueFactory.INSTANCE)//
            // lists
            .with(getListType(DoubleCell.TYPE), DoubleListValueFactory.INSTANCE)//
            .with(getListType(IntCell.TYPE), IntListValueFactory.INSTANCE)//
            .with(getListType(LongCell.TYPE), LongListValueFactory.INSTANCE)//
            .with(getListType(StringCell.TYPE), StringListValueFactory.INSTANCE)//
            .with(getListType(BooleanCell.TYPE), BooleanListValueFactory.INSTANCE)//
            // sparse lists
            .with(getSparseListType(DoubleCell.TYPE), DoubleSparseListValueFactory.INSTANCE)//
            .with(getSparseListType(IntCell.TYPE), IntSparseListValueFactory.INSTANCE)//
            .with(getSparseListType(LongCell.TYPE), LongSparseListValueFactory.INSTANCE)//
            .with(getSparseListType(StringCell.TYPE), StringSparseListValueFactory.INSTANCE)//
            .with(getSparseListType(BooleanCell.TYPE), BooleanSparseListValueFactory.INSTANCE)//
            .build();

    /**
     * Creates a {@link ValueFactory} from the logical type stored in a ColumnarSchema.
     *
     * @param traits must contain the {@link LogicalTypeTrait}
     * @param dataRepository for resolving file stores
     * @return the ValueFactory
     */
    public static ValueFactory<?, ?> loadValueFactory(final DataTraits traits, //NOSONAR
        final IDataRepository dataRepository) {
        final var valueFactoryName = extractValueFactoryName(traits);
        if (VoidValueFactory.class.getName().equals(valueFactoryName)) {
            return VoidValueFactory.INSTANCE;
        } else if (isDataCellValueFactoryLogicalType(valueFactoryName)) {
            return getDataCellValueFactoryFromLogicalTypeString(valueFactoryName, dataRepository);
        } else if (SPECIFIC_COLLECTION_FACTORY_PROVIDER.hasFactoryFor(valueFactoryName)) {
            return SPECIFIC_COLLECTION_FACTORY_PROVIDER.getFactoryFor(valueFactoryName);
        } else {
            return getValueFactoryFromExtensionPoint(traits, dataRepository, valueFactoryName);
        }
    }

    private static ValueFactory<?, ?> getValueFactoryFromExtensionPoint(final DataTraits traits,
        final IDataRepository dataRepository, final String valueFactoryName) {
        var valueFactory = getValueFactoryFromExtensionPoint(valueFactoryName)
            .orElseThrow(() -> new IllegalArgumentException(String.format(
                "Unknown ValueFactory class '%s' encountered. Are you missing an extension?.", valueFactoryName)));
        if (valueFactory instanceof CollectionValueFactory) {
            CheckUtils.checkArgument(traits instanceof ListDataTraits,
                "The DataTraits for CollectionValueFactory must be ListDataTraits but was '%s'", traits.getClass());
            var elementTraits = ((ListDataTraits)traits).getInner();
            var elementValueFactory = loadValueFactory(elementTraits, dataRepository);
            var elementType = getDataTypeForValueFactory(elementValueFactory);
            ((CollectionValueFactory<?, ?>)valueFactory).initialize(elementValueFactory, elementType);
        }
        return valueFactory;
    }

    private static boolean isDataCellValueFactoryLogicalType(final String logicalType) {
        return logicalType.contains(";");
    }

    @SuppressWarnings("unchecked")
    private static ValueFactory<?, ?> getDataCellValueFactoryFromLogicalTypeString(final String logicalType,
        final IDataRepository dataRepo) {
        var split = logicalType.split(";");
        CheckUtils.checkArgument(split.length == 2, "Expected two parts after splitting at ';' but had %s for '%s'.",
            split.length, logicalType);
        Class<? extends DataCell> cellClass;
        try {
            cellClass = (Class<? extends DataCell>)Class.forName(split[1]);
        } catch (ClassNotFoundException ex) {
            throw new IllegalArgumentException("Can't find cell class with name " + split[1], ex);
        }
        var cellType = DataType.getType(cellClass);
        var valueFactory = new DictEncodedDataCellValueFactory();
        valueFactory.initialize(dataRepo, cellType);
        return valueFactory;
    }

    /**
     * Gets the ValueFactory corresponding to the provided type.
     *
     * @param type of the column the ValueFactory is needed for
     * @param fileStoreHandler for writing file stores
     * @return the ValueFactory corresponding to type
     */
    public static ValueFactory<?, ?> getValueFactory(final DataType type, //NOSONAR
        final IWriteFileStoreHandler fileStoreHandler) {
        return getValueFactory(type, t -> new DictEncodedDataCellValueFactory(fileStoreHandler, type));
    }

    /**
     * Finds the ValueFactory for the provided DataType. The fallback function is used for all types where no
     * specialized value factory is available. Clients should not call this method, use
     * {@link #getValueFactory(DataType, IWriteFileStoreHandler)} instead.
     *
     * @param type for which a factory is needed
     * @param fallbackFactoryProvider provides a catch all ValueFactory for all types where no specific factory exists
     * @return the {@link ValueFactory} for the provided type
     * @noreference This method is not intended to be referenced by clients.
     */
    public static ValueFactory<?, ?> getValueFactory(final DataType type, //NOSONAR
        final Function<DataType, ValueFactory<?, ?>> fallbackFactoryProvider) {
        ValueFactory<?, ?> factory = null;

        // Handle special cases
        // Use special value factories for list/sets of primitive types
        if (type == null) {
            factory = VoidValueFactory.INSTANCE;
        }
        if (SPECIFIC_COLLECTION_FACTORY_PROVIDER.hasFactoryFor(type)) {
            return SPECIFIC_COLLECTION_FACTORY_PROVIDER.getFactoryFor(type);
        }

        // Get the value factory from the extension point
        if (factory == null) {
            // Use the registered value factory
            factory = ValueFactoryUtils.getValueFactoryFromExtensionPoint(type)//
                // Use the fallback which works for all cells
                .orElseGet(() -> fallbackFactoryProvider.apply(type));
        }

        // Collection types need to be initialized
        if (factory instanceof CollectionValueFactory) {
            @SuppressWarnings("null")
            final DataType elementType = type.getCollectionElementType();
            final ValueFactory<?, ?> elementFactory = getValueFactory(elementType, fallbackFactoryProvider);
            ((CollectionValueFactory<?, ?>)factory).initialize(elementFactory, elementType);
        }
        return factory;
    }

    /**
     * Extracts the traits from the provided factory and adds the LogicalTypeTrait to it.
     *
     * @param factory for which to get the traits
     * @return the traits of factory (including the LogicalTypeTrait)
     */
    public static DataTraits getTraits(final ValueFactory<?, ?> factory) {
        if (isDataCellValueFactory(factory)) {
            return getDataCellValueFactoryTraits(factory);
        } else if (factory instanceof CollectionValueFactory) {
            return getCollectionDataTraits((CollectionValueFactory<?, ?>)factory);
        } else {
            return DataTraitUtils.withTrait(factory.getTraits(), new LogicalTypeTrait(factory.getClass().getName()));
        }
    }

    private static DataTraits getCollectionDataTraits(final CollectionValueFactory<?, ?> valueFactory) {
        var traitsWithoutType = valueFactory.getTraits();
        if (DataTraitUtils.isStruct(traitsWithoutType)) {
            return createStructCollectionDataTraits(valueFactory, (StructDataTraits)traitsWithoutType);
        } else if (DataTraitUtils.isList(traitsWithoutType)) {
            return createListCollectionDataTraits(valueFactory, (ListDataTraits)traitsWithoutType);
        } else {
            throw new IllegalArgumentException("Implementation error! "
                + "The provided CollectionValueFactory does not provide traits for its element ValueFactory.");
        }
    }

    private static DataTraits createStructCollectionDataTraits(final CollectionValueFactory<?, ?> valueFactory,
        final StructDataTraits traitsWithoutType) {
        var updatedInnerTraits =
            getTraitsForInnerStructTraits(traitsWithoutType, valueFactory.getElementValueFactory());
        return new DefaultStructDataTraits(
            ArrayUtils.add(traitsWithoutType.getTraits(), new LogicalTypeTrait(valueFactory.getClass().getName())),
            updatedInnerTraits);
    }

    private static DataTraits[] getTraitsForInnerStructTraits(final StructDataTraits traitsWithoutType,
        final ValueFactory<?, ?> elementValueFactory) {
        var elementTraitsWithoutType = elementValueFactory.getTraits();
        var updatedInnerTraits = new DataTraits[traitsWithoutType.size()];
        var elementTraitsEncountered = false;
        for (int i = 0; i < updatedInnerTraits.length; i++) {//NOSONAR
            var innerTraits = traitsWithoutType.getDataTraits(i);
            if (elementTraitsWithoutType.equals(innerTraits)) {
                updatedInnerTraits[i] = getTraits(elementValueFactory);
                elementTraitsEncountered = true;
            } else {
                updatedInnerTraits[i] = innerTraits;
            }
        }
        CheckUtils.checkArgument(elementTraitsEncountered,
            "Implementation error! None of the inner traits correspond to the element value factory.");
        return updatedInnerTraits;
    }

    private static DataTraits createListCollectionDataTraits(final CollectionValueFactory<?, ?> valueFactory,
        final ListDataTraits traitsWithoutType) {
        var elementValueFactory = valueFactory.getElementValueFactory();
        CheckUtils.checkArgument(elementValueFactory.getTraits().equals(traitsWithoutType.getInner()),
            "Implementation error! The inner traits of a CollectionValueFactory with ListDataTraits must "
                + "hold the traits of the element ValueFactory.");
        var dataTraitsWithType = ArrayUtils.add(traitsWithoutType.getTraits(), createLogicalTypeTrait(valueFactory));
        return new DefaultListDataTraits(dataTraitsWithType, getTraits(elementValueFactory));
    }

    private static LogicalTypeTrait createLogicalTypeTrait(final ValueFactory<?, ?> valueFactory) {
        return new LogicalTypeTrait(valueFactory.getClass().getName());
    }

    @SuppressWarnings("deprecation")
    private static boolean isDataCellValueFactory(final ValueFactory<?, ?> factory) {
        return factory instanceof DataCellValueFactory || factory instanceof DictEncodedDataCellValueFactory;
    }

    private static DataTraits getDataCellValueFactoryTraits(final ValueFactory<?, ?> factory) {
        var dataType = getDataTypeForValueFactory(factory);
        var encoded = factory.getClass().getName() + ";" + dataType.getCellClass().getName();
        var logicalTypeTrait = new LogicalTypeTrait(encoded);
        return DataTraitUtils.withTrait(factory.getTraits(), logicalTypeTrait);
    }

    private static DataType getSetType(final DataType elementType) {
        return DataType.getType(SetCell.class, elementType);
    }

    private static DataType getListType(final DataType elementType) {
        return DataType.getType(ListCell.class, elementType);
    }

    private static DataType getSparseListType(final DataType elementType) {
        return DataType.getType(SparseListCell.class, elementType);
    }

    /**
     * Creates the {@link RowKeyValueFactory} corresponding to the provided traits
     *
     * @param traits must contain the {@link LogicalTypeTrait}
     * @return a RowKeyValueFactory
     */
    public static RowKeyValueFactory<?, ?> loadRowKeyValueFactory(final DataTraits traits) {//NOSONAR
        var name = extractValueFactoryName(traits);
        return (RowKeyValueFactory<?, ?>)getValueFactoryFromExtensionPoint(name)//
            .orElseThrow(() -> new IllegalArgumentException(
                String.format("Failed to create a RowKeyValueFactory with class name '%s'.", name)));
    }

    /**
     * Gets the {@link RowKeyValueFactory} corresponding to the provided {@link RowKeyType}.
     *
     * @param rowKeyType the type of row key
     * @return a {@link RowKeyValueFactory}
     */
    public static RowKeyValueFactory<?, ?> getRowKeyValueFactory(final RowKeyType rowKeyType) {//NOSONAR
        switch (rowKeyType) {
            case CUSTOM:
                return DefaultRowKeyValueFactory.INSTANCE;
            case NOKEY:
                return VoidRowKeyFactory.INSTANCE;
            default:
                throw new IllegalArgumentException("Unknown RowKey configuration " + rowKeyType.name() + ".");
        }
    }

    /**
     * Retrieves the DataType corresponding to the provided {@link ValueFactory}.
     *
     * @param factory to retrieve the DataType for
     * @return the {@link DataType} for the provided factory
     */
    @SuppressWarnings("deprecation")
    public static DataType getDataTypeForValueFactory(final ValueFactory<?, ?> factory) {
        if (SPECIFIC_COLLECTION_FACTORY_PROVIDER.hasTypeFor(factory)) {
            return SPECIFIC_COLLECTION_FACTORY_PROVIDER.getTypeFor(factory);
        } else if (factory instanceof DictEncodedDataCellValueFactory) {
            return ((DictEncodedDataCellValueFactory)factory).getType();
        } else if (factory instanceof DataCellValueFactory) {
            return ((DataCellValueFactory)factory).getType();
        } else if (factory instanceof VoidValueFactory) {
            throw new IllegalArgumentException("No data type available for VoidValueFactory.");
        } else {
            return getDataTypeFromExtensionPoint(factory);
        }
    }

    private static DataType getDataTypeFromExtensionPoint(final ValueFactory<?, ?> factory) {
        var cellClass = DataTypeRegistry.getInstance().getCellClassForValueFactory(factory);
        if (factory instanceof CollectionValueFactory) {
            var elementType =
                getDataTypeForValueFactory(((CollectionValueFactory<?, ?>)factory).getElementValueFactory());
            return DataType.getType(cellClass, elementType);
        } else {
            return DataType.getType(cellClass);
        }
    }

    private static String extractValueFactoryName(final DataTraits traits) {
        return DataTraits.getTrait(traits, LogicalTypeTrait.class)//
            .map(LogicalTypeTrait::getLogicalType)//
            .orElseThrow(() -> new IllegalArgumentException("No logical type trait present."));
    }

    /**
     * @param className name of the {@link ValueFactory} class (as returned by {@link Class#getName()})
     * @return the value factory if a value factory with the provided name is known
     */
    private static Optional<ValueFactory<?, ?>> getValueFactoryFromExtensionPoint(final String className) {//NOSONAR
        return DataTypeRegistry.getInstance().getValueFactoryClass(className)//
            .map(ValueFactoryUtils::instantiateValueFactory);
    }

    /**
     * Creates the {@link ValueFactory} for the provided type if there is one registered with it.
     *
     * @param type for which to get the corresponding {@link ValueFactory}
     * @return {@link ValueFactory} corresponding to {@link DataType type} if there is one
     */
    private static Optional<ValueFactory<?, ?>> getValueFactoryFromExtensionPoint(final DataType type) {
        return DataTypeRegistry.getInstance().getValueFactoryFor(type)//
            .map(ValueFactoryUtils::instantiateValueFactory);
    }

    /**
     * Instantiates a {@link ValueFactory} from its class.
     *
     * @param valueFactoryClass class of the ValueFactory
     * @return the newly created {@link ValueFactory}
     */
    public static ValueFactory<?, ?> instantiateValueFactory(final Class<?> valueFactoryClass) { //NOSONAR
        try {
            final Constructor<?> constructor = valueFactoryClass.getConstructor();
            return (ValueFactory<?, ?>)constructor.newInstance();
        } catch (final IllegalAccessException | IllegalArgumentException | NoSuchMethodException ex) {
            throw new IllegalStateException("The ValueFactory must have a public empty constructor.", ex);
        } catch (final InvocationTargetException ex) {
            throw new IllegalStateException("The ValueFactory constructor must not throw an exception.", ex);
        } catch (final SecurityException ex) {
            throw new IllegalStateException("Instantiating of the ValueFactory failed with an SecurityException.", ex);
        } catch (final InstantiationException ex) {
            // This cannot happen because we write the fully qualified class name of instantiated objects
            throw new IllegalStateException("The ValueFactory must not be abstract.", ex);
        }
    }

    /**
     * Checks whether the provided ValueFactories are equal.
     *
     * @param factory to check (can be null)
     * @param other to check (can be null)
     * @return true if the two are equal, false other wise
     */
    @SuppressWarnings("null") // if both are null then the first if returns, if one is null the second returns
    public static boolean areEqual(final ValueFactory<?, ?> factory, final ValueFactory<?, ?> other) {
        if (factory == other) {
            return true;
        } else if (factory == null ^ other == null) {
            return false;
        }

        if (factory.getClass().equals(other.getClass())) {
            return areEqualOfSameClass(factory, other);
        } else {
            return false;
        }
    }

    @SuppressWarnings("deprecation")
    private static boolean areEqualOfSameClass(final ValueFactory<?, ?> factory, final ValueFactory<?, ?> other) {
        if (factory instanceof DictEncodedDataCellValueFactory) {
            return ((DictEncodedDataCellValueFactory)factory).getType()
                .equals(((DictEncodedDataCellValueFactory)other).getType());
        } else if (factory instanceof DataCellValueFactory) {
            return ((DataCellValueFactory)factory).getType().equals(((DataCellValueFactory)other).getType());
        } else if (factory instanceof CollectionValueFactory) {
            return areEqual(((CollectionValueFactory<?, ?>)factory).getElementValueFactory(),
                ((CollectionValueFactory<?, ?>)other).getElementValueFactory());
        } else {
            // all other ValueFactories are stateless by contract
            return true;
        }
    }

    private ValueFactoryUtils() {
        // static utility class
    }

    private static final class SingletonFactoryProvider {

        private final Map<String, ValueFactory<?, ?>> m_factoriesByName;

        private final BiMap<DataType, ValueFactory<?, ?>> m_typeFactoryMap;

        private SingletonFactoryProvider(final Builder builder) {
            m_factoriesByName = new HashMap<>(builder.m_factoriesByName);
            m_typeFactoryMap = HashBiMap.create(builder.m_typeFactoryMap);
        }

        boolean hasFactoryFor(final String className) {
            return m_factoriesByName.containsKey(className);
        }

        boolean hasFactoryFor(final DataType type) {
            return m_typeFactoryMap.containsKey(type);
        }

        boolean hasTypeFor(final ValueFactory<?, ?> factory) {
            return m_typeFactoryMap.containsValue(factory);
        }

        ValueFactory<?, ?> getFactoryFor(final String className) { //NOSONAR
            return m_factoriesByName.get(className);
        }

        ValueFactory<?, ?> getFactoryFor(final DataType type) {//NOSONAR
            return m_typeFactoryMap.get(type);
        }

        DataType getTypeFor(final ValueFactory<?, ?> factory) {
            return m_typeFactoryMap.inverse().get(factory);
        }

        private static Builder builder() {
            return new Builder();
        }

        private static final class Builder {
            private final Map<String, ValueFactory<?, ?>> m_factoriesByName = new HashMap<>();

            private final Map<DataType, ValueFactory<?, ?>> m_typeFactoryMap = new HashMap<>();

            private Builder() {

            }

            Builder with(final DataType type, final ValueFactory<?, ?> factory) {
                m_factoriesByName.put(factory.getClass().getName(), factory);
                m_typeFactoryMap.put(type, factory);
                return this;
            }

            SingletonFactoryProvider build() {
                return new SingletonFactoryProvider(this);
            }
        }

    }

}
