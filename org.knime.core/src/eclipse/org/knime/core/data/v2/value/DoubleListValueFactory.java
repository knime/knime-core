package org.knime.core.data.v2.value;

import java.util.Arrays;
import java.util.PrimitiveIterator;
import java.util.PrimitiveIterator.OfDouble;

import org.knime.core.data.DoubleValue;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.data.v2.access.DoubleAccess.DoubleReadAccess;
import org.knime.core.data.v2.access.DoubleAccess.DoubleWriteAccess;
import org.knime.core.data.v2.access.ListAccess.ListAccessSpec;
import org.knime.core.data.v2.access.ListAccess.ListReadAccess;
import org.knime.core.data.v2.access.ListAccess.ListWriteAccess;
import org.knime.core.data.v2.value.DoubleValueFactory.DoubleReadValue;
import org.knime.core.data.v2.value.DoubleValueFactory.DoubleWriteValue;
import org.knime.core.data.v2.value.ListValueFactory.DefaultListReadValue;
import org.knime.core.data.v2.value.ListValueFactory.DefaultListWriteValue;
import org.knime.core.data.v2.value.ListValueFactory.ListReadValue;
import org.knime.core.data.v2.value.ListValueFactory.ListWriteValue;

/**
 * {@link ValueFactory} implementation for {@link ListCell} with elements of type {@link DoubleCell}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 */
public final class DoubleListValueFactory implements ValueFactory<ListReadAccess, ListWriteAccess> {

    /** A stateless instance of {@link DoubleListValueFactory} */
    public static final DoubleListValueFactory INSTANCE = new DoubleListValueFactory();

    @Override
    public ListAccessSpec<DoubleReadAccess, DoubleWriteAccess> getSpec() {
        return new ListAccessSpec<>(DoubleValueFactory.INSTANCE);
    }

    @Override
    public DoubleListReadValue createReadValue(final ListReadAccess reader) {
        return new DefaultDoubleListReadValue(reader);
    }

    @Override
    public DoubleListWriteValue createWriteValue(final ListWriteAccess writer) {
        return new DefaultDoubleListWriteValue(writer);
    }

    /*
     * TODO(benjamin) Finalize API in AP-14240
     */

    /**
     * {@link ReadValue} equivalent to {@link ListCell} with {@link DoubleCell} elements.
     *
     * @since 4.3
     */
    public static interface DoubleListReadValue extends ListReadValue {

        /**
         * @param index the index in the list
         * @return the double value at the index
         * @throws IllegalStateException if the value at this index is missing
         */
        double getDouble(int index);

        /**
         * @return the list as a double array
         * @throws IllegalStateException if the value at one index is missing
         */
        double[] getDoubleArray();

        /**
         * @return an iterator over the double list
         * @throws IllegalStateException if the value at one index is missing
         */
        PrimitiveIterator.OfDouble doubleIterator();
    }

    /**
     * {@link WriteValue} equivalent to {@link ListCell} with {@link DoubleCell} elements.
     *
     * @since 4.3
     */
    public static interface DoubleListWriteValue extends ListWriteValue {

        /**
         * Set the value.
         *
         * @param values a array of double values
         */
        void setValue(double[] values);
    }

    private static final class DefaultDoubleListReadValue extends DefaultListReadValue implements DoubleListReadValue {

        private DefaultDoubleListReadValue(final ListReadAccess reader) {
            super(reader, DoubleCell.TYPE);
        }

        @Override
        public double getDouble(final int index) {
            final DoubleReadValue v = m_reader.getReadValue(index);
            return v.getDoubleValue();
        }

        @Override
        public double[] getDoubleArray() {
            final double[] result = new double[size()];
            for (int i = 0; i < result.length; i++) {
                result[i] = getDouble(i);
            }
            return result;
        }

        @Override
        public OfDouble doubleIterator() {
            return Arrays.stream(getDoubleArray()).iterator();
        }
    }

    private static final class DefaultDoubleListWriteValue extends DefaultListWriteValue
        implements DoubleListWriteValue {

        private DefaultDoubleListWriteValue(final ListWriteAccess writer) {
            super(writer);
        }

        @Override
        public void setValue(final double[] values) {
            this.<DoubleValue, DoubleWriteValue> setValue(values.length, (i, v) -> v.setDoubleValue(values[i]));
        }
    }
}