package org.knime.core.data.v2.value;

import java.util.Arrays;
import java.util.Collection;
import java.util.PrimitiveIterator;
import java.util.PrimitiveIterator.OfDouble;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.core.data.collection.SetCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.data.v2.access.DoubleAccess.DoubleReadAccess;
import org.knime.core.data.v2.access.DoubleAccess.DoubleWriteAccess;
import org.knime.core.data.v2.access.ListAccess.ListAccessSpec;
import org.knime.core.data.v2.access.ListAccess.ListReadAccess;
import org.knime.core.data.v2.access.ListAccess.ListWriteAccess;
import org.knime.core.data.v2.value.DoubleListValueFactory.DoubleListReadValue;
import org.knime.core.data.v2.value.DoubleListValueFactory.DoubleListWriteValue;
import org.knime.core.data.v2.value.SetValueFactory.DefaultSetReadValue;
import org.knime.core.data.v2.value.SetValueFactory.DefaultSetWriteValue;
import org.knime.core.data.v2.value.SetValueFactory.SetReadValue;
import org.knime.core.data.v2.value.SetValueFactory.SetWriteValue;

/**
 * {@link ValueFactory} implementation for {@link SetCell} with elements of type {@link DoubleCell}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 */
public final class DoubleSetValueFactory implements ValueFactory<ListReadAccess, ListWriteAccess> {

    /** A stateless instance of {@link DoubleSetValueFactory} */
    public static final DoubleSetValueFactory INSTANCE = new DoubleSetValueFactory();

    @Override
    public ListAccessSpec<DoubleReadAccess, DoubleWriteAccess> getSpec() {
        return new ListAccessSpec<>(DoubleValueFactory.INSTANCE);
    }

    @Override
    public DoubleSetReadValue createReadValue(final ListReadAccess reader) {
        return new DefaultDoubleSetReadValue(reader);
    }

    @Override
    public DoubleSetWriteValue createWriteValue(final ListWriteAccess writer) {
        return new DefaultDoubleSetWriteValue(writer);
    }

    /*
     * TODO(benjamin) Finalize API in AP-14240
     */

    /**
     * {@link ReadValue} equivalent to {@link SetCell} with {@link DoubleCell} elements.
     *
     * @since 4.3
     */
    public interface DoubleSetReadValue extends SetReadValue {

        /**
         * @param value a double value
         * @return true if the set contains the value
         */
        boolean contains(double value);

        /**
         * @return a {@link Set} containing the {@link Double} values
         */
        Set<Double> getDoubleSet();

        /**
         * @return an iterator of the double set
         * @throws IllegalStateException if the set contains a missing value
         */
        PrimitiveIterator.OfDouble doubleIterator();
    }

    /**
     * {@link WriteValue} equivalent to {@link SetCell} with {@link DoubleCell} elements.
     *
     * @since 4.3
     */
    public interface DoubleSetWriteValue extends SetWriteValue {

        /**
         * Set the value.
         *
         * @param values a collection of double values
         */
        void setDoubleColletionValue(Collection<Double> values);
    }

    private static final class DefaultDoubleSetReadValue extends DefaultSetReadValue<DoubleListReadValue>
        implements DoubleSetReadValue {

        protected DefaultDoubleSetReadValue(final ListReadAccess reader) {
            super(reader, DoubleListValueFactory.INSTANCE);
        }

        @Override
        public boolean contains(final double value) {
            // TODO(benjamin) we can save the values sorted and do binary search
            final double[] values = m_value.getDoubleArray();
            for (int i = 0; i < values.length; i++) {
                if (value == values[i]) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Set<Double> getDoubleSet() {
            return Arrays.stream(m_value.getDoubleArray()).boxed().collect(Collectors.toSet());
        }

        @Override
        public OfDouble doubleIterator() {
            return m_value.doubleIterator();
        }
    }

    private static final class DefaultDoubleSetWriteValue extends DefaultSetWriteValue<DoubleListWriteValue>
        implements DoubleSetWriteValue {

        protected DefaultDoubleSetWriteValue(final ListWriteAccess writer) {
            super(writer, DoubleListValueFactory.INSTANCE);
        }

        @Override
        public void setDoubleColletionValue(final Collection<Double> values) {
            m_value.setValue(values.stream().mapToDouble(Double::doubleValue).distinct().toArray());
        }
    }
}