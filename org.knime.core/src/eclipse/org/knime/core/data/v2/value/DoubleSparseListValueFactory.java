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

import java.util.ArrayList;
import java.util.List;
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
 *
 * @noreference This class is not intended to be referenced by clients.
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

        /**
         * @param values the values to set
         * @param defaultElement the default element which should not be saved multiple times
         */
        void setValue(double[] values, double defaultElement);
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

        @Override
        public int getLength() {
            return size();
        }

        @Override
        public double getValue(final int index) {
            return getDouble(index);
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

        @Override
        public void setValue(final double[] values, final double defaultElement) {
            final List<Integer> storageIndices = new ArrayList<>();
            final List<Double> storageList = new ArrayList<>();

            final long defaultElementBits = Double.doubleToLongBits(defaultElement);
            for (int i = 0; i < values.length; i++) {
                final double v = values[i];
                if (Double.doubleToLongBits(v) != defaultElementBits) {
                    storageIndices.add(i);
                    storageList.add(v);
                }
            }

            m_defaultValue.setDoubleValue(defaultElement);
            m_sizeValue.setIntValue(storageList.size());
            m_storageIndices.setValue(storageIndices.stream().mapToInt(Integer::intValue).toArray());
            m_storageList.setValue(storageList.stream().mapToDouble(Double::doubleValue).toArray());
        }
    }
}