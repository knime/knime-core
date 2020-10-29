package org.knime.core.data.v2.value;

import java.util.OptionalInt;
import java.util.PrimitiveIterator.OfDouble;

import org.knime.core.data.DoubleValue;
import org.knime.core.data.collection.SparseListCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.data.v2.access.AccessSpec;
import org.knime.core.data.v2.access.DoubleAccess.DoubleAccessSpec;
import org.knime.core.data.v2.access.DoubleAccess.DoubleReadAccess;
import org.knime.core.data.v2.access.DoubleAccess.DoubleWriteAccess;
import org.knime.core.data.v2.access.IntAccess.IntAccessSpec;
import org.knime.core.data.v2.access.IntAccess.IntReadAccess;
import org.knime.core.data.v2.access.IntAccess.IntWriteAccess;
import org.knime.core.data.v2.access.ListAccess.ListAccessSpec;
import org.knime.core.data.v2.access.ListAccess.ListReadAccess;
import org.knime.core.data.v2.access.ListAccess.ListWriteAccess;
import org.knime.core.data.v2.access.StructAccess.StructAccessSpec;
import org.knime.core.data.v2.access.StructAccess.StructReadAccess;
import org.knime.core.data.v2.access.StructAccess.StructWriteAccess;
import org.knime.core.data.v2.value.DoubleListValueFactory.DoubleListReadValue;
import org.knime.core.data.v2.value.DoubleListValueFactory.DoubleListWriteValue;
import org.knime.core.data.v2.value.DoubleValueFactory.DoubleReadValue;
import org.knime.core.data.v2.value.DoubleValueFactory.DoubleWriteValue;
import org.knime.core.data.v2.value.SparseListValueFactory.AbstractSparseIterator;
import org.knime.core.data.v2.value.SparseListValueFactory.DefaultSparseListReadValue;
import org.knime.core.data.v2.value.SparseListValueFactory.DefaultSparseListWriteValue;
import org.knime.core.data.v2.value.SparseListValueFactory.SparseListReadValue;
import org.knime.core.data.v2.value.SparseListValueFactory.SparseListWriteValue;

/**
 * {@link ValueFactory} implementation for {@link SparseListCell} with elements of type {@link DoubleCell}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 */
public final class DoubleSparseListValueFactory implements ValueFactory<StructReadAccess, StructWriteAccess> {

    /** A stateless instance of {@link DoubleSparseListValueFactory} */
    public static final DoubleSparseListValueFactory INSTANCE = new DoubleSparseListValueFactory();

    @Override
    public AccessSpec<StructReadAccess, StructWriteAccess> getSpec() {
        final DoubleAccessSpec defaultAccessSpec = DoubleAccessSpec.INSTANCE;
        final IntAccessSpec sizeAccessSpec = IntAccessSpec.INSTANCE;
        final ListAccessSpec<IntReadAccess, IntWriteAccess> indicesAccessSpec =
            new ListAccessSpec<>(IntValueFactory.INSTANCE);
        final ListAccessSpec<DoubleReadAccess, DoubleWriteAccess> listAccessSpec =
            new ListAccessSpec<>(DoubleValueFactory.INSTANCE);
        return new StructAccessSpec(defaultAccessSpec, sizeAccessSpec, indicesAccessSpec, listAccessSpec);
    }

    @Override
    public DoubleSparseListReadValue createReadValue(final StructReadAccess reader) {
        return new DefaultDoubleSparseListReadValue(reader.getInnerReadAccessAt(0), reader.getInnerReadAccessAt(1),
            reader.getInnerReadAccessAt(2), reader.getInnerReadAccessAt(3));
    }

    @Override
    public DoubleSparseListWriteValue createWriteValue(final StructWriteAccess writer) {
        return new DefaultDoubleSparseListWriteValue(writer.getWriteAccessAt(0), writer.getWriteAccessAt(1),
            writer.getWriteAccessAt(2), writer.getWriteAccessAt(3));
    }

    /*
     * TODO(benjamin) Finalize API in AP-14240
     */

    /**
     * {@link ReadValue} equivalent to {@link SparseListCell} with {@link DoubleCell} elements.
     *
     * @since 4.3
     */
    public static interface DoubleSparseListReadValue extends SparseListReadValue, DoubleListReadValue {
    }

    /**
     * {@link WriteValue} equivalent to {@link SparseListCell} with {@link DoubleCell} elements.
     *
     * @since 4.3
     */
    public static interface DoubleSparseListWriteValue extends SparseListWriteValue {
    }

    private static final class DefaultDoubleSparseListReadValue
        extends DefaultSparseListReadValue<DoubleReadValue, DoubleListReadValue, DoubleReadAccess>
        implements DoubleSparseListReadValue {

        private DefaultDoubleSparseListReadValue(final DoubleReadAccess defaultAccess, final IntReadAccess sizeAccess,
            final ListReadAccess indicesAccess, final ListReadAccess listAccess) {
            super(defaultAccess, sizeAccess, indicesAccess, listAccess, DoubleValueFactory.INSTANCE,
                DoubleListValueFactory.INSTANCE);
        }

        private double getDoubleFromStorage(final int storageIndex) {
            return m_storageList.getDouble(storageIndex);
        }

        @Override
        public double getDouble(final int index) {
            final OptionalInt storageIndex = storageIndexForIndex(index);
            if (storageIndex.isPresent()) {
                return getDoubleFromStorage(storageIndex.getAsInt());
            } else {
                return m_defaultValue.getDoubleValue();
            }
        }

        @Override
        public double[] getDoubleArray() {
            final double[] values = new double[size()];
            final OfDouble iterator = doubleIterator();
            for (int i = 0; i < values.length; i++) {
                values[i] = iterator.nextDouble();
            }
            return values;
        }

        @Override
        public OfDouble doubleIterator() {
            return new SparseDoubleIterator();
        }

        private final class SparseDoubleIterator extends AbstractSparseIterator<Double> implements OfDouble {

            private final double m_default = m_defaultValue.getDoubleValue();

            private SparseDoubleIterator() {
                super(size(), m_storageList.size(), m_storageIndices::getInt);
            }

            @Override
            public double nextDouble() {
                final OptionalInt storageIndex = nextStorageIndex();
                if (storageIndex.isPresent()) {
                    return getDoubleFromStorage(storageIndex.getAsInt());
                } else {
                    return m_default;
                }
            }
        }
    }

    private static final class DefaultDoubleSparseListWriteValue
        extends DefaultSparseListWriteValue<DoubleValue, DoubleWriteValue, DoubleListWriteValue, DoubleWriteAccess>
        implements DoubleSparseListWriteValue {

        protected DefaultDoubleSparseListWriteValue(final DoubleWriteAccess defaultAccess,
            final IntWriteAccess sizeAccess, final ListWriteAccess indicesAccess, final ListWriteAccess listAccess) {
            super(defaultAccess, sizeAccess, indicesAccess, listAccess, DoubleValueFactory.INSTANCE,
                DoubleListValueFactory.INSTANCE);
        }
    }
}