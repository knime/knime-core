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
import java.util.PrimitiveIterator.OfLong;

import org.knime.core.data.LongValue;
import org.knime.core.data.collection.SparseListCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.data.v2.access.AccessSpec;
import org.knime.core.data.v2.access.IntAccess.IntAccessSpec;
import org.knime.core.data.v2.access.IntAccess.IntReadAccess;
import org.knime.core.data.v2.access.IntAccess.IntWriteAccess;
import org.knime.core.data.v2.access.ListAccess.ListAccessSpec;
import org.knime.core.data.v2.access.ListAccess.ListReadAccess;
import org.knime.core.data.v2.access.ListAccess.ListWriteAccess;
import org.knime.core.data.v2.access.LongAccess.LongAccessSpec;
import org.knime.core.data.v2.access.LongAccess.LongReadAccess;
import org.knime.core.data.v2.access.LongAccess.LongWriteAccess;
import org.knime.core.data.v2.access.StructAccess.StructAccessSpec;
import org.knime.core.data.v2.access.StructAccess.StructReadAccess;
import org.knime.core.data.v2.access.StructAccess.StructWriteAccess;
import org.knime.core.data.v2.value.LongListValueFactory.LongListReadValue;
import org.knime.core.data.v2.value.LongListValueFactory.LongListWriteValue;
import org.knime.core.data.v2.value.LongValueFactory.LongReadValue;
import org.knime.core.data.v2.value.LongValueFactory.LongWriteValue;
import org.knime.core.data.v2.value.SparseListValueFactory.AbstractSparseIterator;
import org.knime.core.data.v2.value.SparseListValueFactory.DefaultSparseListReadValue;
import org.knime.core.data.v2.value.SparseListValueFactory.DefaultSparseListWriteValue;
import org.knime.core.data.v2.value.SparseListValueFactory.SparseListReadValue;
import org.knime.core.data.v2.value.SparseListValueFactory.SparseListWriteValue;

/**
 * {@link ValueFactory} implementation for {@link SparseListCell} with elements of type {@link LongCell}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 *
 * @noreference This class is not intended to be referenced by clients.
 */
public final class LongSparseListValueFactory implements ValueFactory<StructReadAccess, StructWriteAccess> {

    /** A stateless instance of {@link LongSparseListValueFactory} */
    public static final LongSparseListValueFactory INSTANCE = new LongSparseListValueFactory();

    @Override
    public AccessSpec<StructReadAccess, StructWriteAccess> getSpec() {
        final LongAccessSpec defaultAccessSpec = LongAccessSpec.INSTANCE;
        final IntAccessSpec sizeAccessSpec = IntAccessSpec.INSTANCE;
        final ListAccessSpec<IntReadAccess, IntWriteAccess> indicesAccessSpec =
            new ListAccessSpec<>(IntAccessSpec.INSTANCE);
        final ListAccessSpec<LongReadAccess, LongWriteAccess> listAccessSpec =
            new ListAccessSpec<>(LongAccessSpec.INSTANCE);
        return new StructAccessSpec(defaultAccessSpec, sizeAccessSpec, indicesAccessSpec, listAccessSpec);
    }

    @Override
    public LongSparseListReadValue createReadValue(final StructReadAccess reader) {
        return new DefaultLongSparseListReadValue(reader.getInnerReadAccessAt(0), reader.getInnerReadAccessAt(1),
            reader.getInnerReadAccessAt(2), reader.getInnerReadAccessAt(3));
    }

    @Override
    public LongSparseListWriteValue createWriteValue(final StructWriteAccess writer) {
        return new DefaultLongSparseListWriteValue(writer.getWriteAccessAt(0), writer.getWriteAccessAt(1),
            writer.getWriteAccessAt(2), writer.getWriteAccessAt(3));
    }

    /**
     * {@link ReadValue} equivalent to {@link SparseListCell} with {@link LongCell} elements.
     *
     * @since 4.3
     */
    public static interface LongSparseListReadValue extends SparseListReadValue, LongListReadValue {
    }

    /**
     * {@link WriteValue} equivalent to {@link SparseListCell} with {@link LongCell} elements.
     *
     * @since 4.3
     */
    public static interface LongSparseListWriteValue extends SparseListWriteValue {

        /**
         * @param values the values to set
         * @param defaultElement the default element which should not be saved multiple times
         */
        void setValue(long[] values, long defaultElement);
    }

    private static final class DefaultLongSparseListReadValue
        extends DefaultSparseListReadValue<LongReadValue, LongListReadValue, LongReadAccess>
        implements LongSparseListReadValue {

        private DefaultLongSparseListReadValue(final LongReadAccess defaultAccess, final IntReadAccess sizeAccess,
            final ListReadAccess indicesAccess, final ListReadAccess listAccess) {
            super(defaultAccess, sizeAccess, indicesAccess, listAccess, LongValueFactory.INSTANCE,
                LongListValueFactory.INSTANCE);
        }

        private long getLongFromStorage(final int storageIndex) {
            return m_storageList.getLong(storageIndex);
        }

        @Override
        public long getLong(final int index) {
            final OptionalInt storageIndex = storageIndexForIndex(index);
            if (storageIndex.isPresent()) {
                return getLongFromStorage(storageIndex.getAsInt());
            } else {
                return m_defaultValue.getLongValue();
            }
        }

        @Override
        public long[] getLongArray() {
            final long[] values = new long[size()];
            final OfLong iterator = longIterator();
            for (int i = 0; i < values.length; i++) {
                values[i] = iterator.nextLong();
            }
            return values;
        }

        @Override
        public OfLong longIterator() {
            return new SparseLongIterator();
        }

        private final class SparseLongIterator extends AbstractSparseIterator<Long> implements OfLong {

            private final long m_default = m_defaultValue.getLongValue();

            private SparseLongIterator() {
                super(size(), m_storageList.size(), m_storageIndices::getInt);
            }

            @Override
            public long nextLong() {
                final OptionalInt storageIndex = nextStorageIndex();
                if (storageIndex.isPresent()) {
                    return getLongFromStorage(storageIndex.getAsInt());
                } else {
                    return m_default;
                }
            }
        }
    }

    private static final class DefaultLongSparseListWriteValue
        extends DefaultSparseListWriteValue<LongValue, LongWriteValue, LongListWriteValue, LongWriteAccess>
        implements LongSparseListWriteValue {

        protected DefaultLongSparseListWriteValue(final LongWriteAccess defaultAccess, final IntWriteAccess sizeAccess,
            final ListWriteAccess indicesAccess, final ListWriteAccess listAccess) {
            super(defaultAccess, sizeAccess, indicesAccess, listAccess, LongValueFactory.INSTANCE,
                LongListValueFactory.INSTANCE);
        }

        @Override
        public void setValue(final long[] values, final long defaultElement) {
            final List<Integer> storageIndices = new ArrayList<>();
            final List<Long> storageList = new ArrayList<>();

            for (int i = 0; i < values.length; i++) {
                final long v = values[i];
                if (v != defaultElement) {
                    storageIndices.add(i);
                    storageList.add(v);
                }
            }

            m_defaultValue.setLongValue(defaultElement);
            m_sizeValue.setIntValue(storageList.size());
            m_storageIndices.setValue(storageIndices.stream().mapToInt(Integer::intValue).toArray());
            m_storageList.setValue(storageList.stream().mapToLong(Long::longValue).toArray());
        }
    }
}