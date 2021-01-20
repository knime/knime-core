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
 *   Nov 12, 2020 (Benjamin Wilhelm): created
 */
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
 *
 * @noreference This class is not intended to be referenced by clients.
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
                if (Double.doubleToLongBits(value) == Double.doubleToLongBits(values[i])) {
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