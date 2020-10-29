package org.knime.core.data.v2.value;

import java.util.Arrays;
import java.util.PrimitiveIterator;
import java.util.PrimitiveIterator.OfInt;

import org.knime.core.data.IntValue;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.data.v2.access.IntAccess.IntReadAccess;
import org.knime.core.data.v2.access.IntAccess.IntWriteAccess;
import org.knime.core.data.v2.access.ListAccess.ListAccessSpec;
import org.knime.core.data.v2.access.ListAccess.ListReadAccess;
import org.knime.core.data.v2.access.ListAccess.ListWriteAccess;
import org.knime.core.data.v2.value.IntValueFactory.IntReadValue;
import org.knime.core.data.v2.value.IntValueFactory.IntWriteValue;
import org.knime.core.data.v2.value.ListValueFactory.DefaultListReadValue;
import org.knime.core.data.v2.value.ListValueFactory.DefaultListWriteValue;
import org.knime.core.data.v2.value.ListValueFactory.ListReadValue;
import org.knime.core.data.v2.value.ListValueFactory.ListWriteValue;

/**
 * {@link ValueFactory} implementation for {@link ListCell} with elements of type {@link IntCell}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 */
public final class IntListValueFactory implements ValueFactory<ListReadAccess, ListWriteAccess> {

    /** A stateless instance of {@link IntListValueFactory} */
    public static final IntListValueFactory INSTANCE = new IntListValueFactory();

    @Override
    public ListAccessSpec<IntReadAccess, IntWriteAccess> getSpec() {
        return new ListAccessSpec<>(IntValueFactory.INSTANCE);
    }

    @Override
    public IntListReadValue createReadValue(final ListReadAccess reader) {
        return new DefaultIntListReadValue(reader);
    }

    @Override
    public IntListWriteValue createWriteValue(final ListWriteAccess writer) {
        return new DefaultIntListWriteValue(writer);
    }

    /*
     * TODO(benjamin) Finalize API in AP-14240
     */

    /**
     * {@link ReadValue} equivalent to {@link ListCell} with {@link IntCell} elements.
     *
     * @since 4.3
     */
    public static interface IntListReadValue extends ListReadValue {

        /**
         * @param index the index in the list
         * @return the integer value at the index
         * @throws IllegalStateException if the value at this index is missing
         */
        int getInt(int index);

        /**
         * @return the list as a integer array
         * @throws IllegalStateException if the value at one index is missing
         */
        int[] getIntArray();

        /**
         * @return an iterator over the integer list
         * @throws IllegalStateException if the value at one index is missing
         */
        PrimitiveIterator.OfInt intIterator();
    }

    /**
     * {@link WriteValue} equivalent to {@link ListCell} with {@link IntCell} elements.
     *
     * @since 4.3
     */
    public static interface IntListWriteValue extends ListWriteValue {

        /**
         * Set the value.
         *
         * @param values a array of int values
         */
        void setValue(int[] values);
    }

    private static final class DefaultIntListReadValue extends DefaultListReadValue implements IntListReadValue {

        private DefaultIntListReadValue(final ListReadAccess reader) {
            super(reader, IntCell.TYPE);
        }

        @Override
        public int getInt(final int index) {
            final IntReadValue v = m_reader.getReadValue(index);
            return v.getIntValue();
        }

        @Override
        public int[] getIntArray() {
            final int[] result = new int[size()];
            for (int i = 0; i < result.length; i++) {
                result[i] = getInt(i);
            }
            return result;
        }

        @Override
        public OfInt intIterator() {
            return Arrays.stream(getIntArray()).iterator();
        }
    }

    private static final class DefaultIntListWriteValue extends DefaultListWriteValue implements IntListWriteValue {

        private DefaultIntListWriteValue(final ListWriteAccess writer) {
            super(writer);
        }

        @Override
        public void setValue(final int[] values) {
            this.<IntValue, IntWriteValue> setValue(values.length, (i, v) -> v.setIntValue(values[i]));
        }
    }
}