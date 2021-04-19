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
import java.util.Iterator;
import java.util.List;
import java.util.OptionalInt;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.collection.SparseListCell;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.data.v2.value.BooleanListValueFactory.BooleanListReadValue;
import org.knime.core.data.v2.value.BooleanListValueFactory.BooleanListWriteValue;
import org.knime.core.data.v2.value.BooleanValueFactory.BooleanReadValue;
import org.knime.core.data.v2.value.BooleanValueFactory.BooleanWriteValue;
import org.knime.core.data.v2.value.SparseListValueFactory.AbstractSparseIterator;
import org.knime.core.data.v2.value.SparseListValueFactory.DefaultSparseListReadValue;
import org.knime.core.data.v2.value.SparseListValueFactory.DefaultSparseListWriteValue;
import org.knime.core.data.v2.value.SparseListValueFactory.SparseListReadValue;
import org.knime.core.data.v2.value.SparseListValueFactory.SparseListWriteValue;
import org.knime.core.table.access.BooleanAccess.BooleanReadAccess;
import org.knime.core.table.access.BooleanAccess.BooleanWriteAccess;
import org.knime.core.table.access.IntAccess.IntReadAccess;
import org.knime.core.table.access.IntAccess.IntWriteAccess;
import org.knime.core.table.access.ListAccess.ListReadAccess;
import org.knime.core.table.access.ListAccess.ListWriteAccess;
import org.knime.core.table.access.StructAccess.StructReadAccess;
import org.knime.core.table.access.StructAccess.StructWriteAccess;
import org.knime.core.table.schema.BooleanDataSpec;
import org.knime.core.table.schema.DataSpec;
import org.knime.core.table.schema.IntDataSpec;
import org.knime.core.table.schema.ListDataSpec;
import org.knime.core.table.schema.StructDataSpec;

/**
 * {@link ValueFactory} implementation for {@link SparseListCell} with elements of type {@link BooleanCell}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 *
 * @noreference This class is not intended to be referenced by clients.
 */
public final class BooleanSparseListValueFactory implements ValueFactory<StructReadAccess, StructWriteAccess> {

    /** A stateless instance of {@link BooleanSparseListValueFactory} */
    public static final BooleanSparseListValueFactory INSTANCE = new BooleanSparseListValueFactory();

    @Override
    public DataSpec getSpec() {
        final BooleanDataSpec defaultDataSpec = BooleanDataSpec.INSTANCE;
        final IntDataSpec sizeDataSpec = IntDataSpec.INSTANCE;
        final ListDataSpec indicesDataSpec = new ListDataSpec(IntDataSpec.INSTANCE);
        final ListDataSpec listDataSpec = new ListDataSpec(BooleanDataSpec.INSTANCE);
        return new StructDataSpec(defaultDataSpec, sizeDataSpec, indicesDataSpec, listDataSpec);
    }

    @Override
    public BooleanSparseListReadValue createReadValue(final StructReadAccess reader) {
        return new DefaultBooleanSparseListReadValue(reader.getInnerReadAccessAt(0), reader.getInnerReadAccessAt(1),
            reader.getInnerReadAccessAt(2), reader.getInnerReadAccessAt(3));
    }

    @Override
    public BooleanSparseListWriteValue createWriteValue(final StructWriteAccess writer) {
        return new DefaultBooleanSparseListWriteValue(writer.getWriteAccessAt(0), writer.getWriteAccessAt(1),
            writer.getWriteAccessAt(2), writer.getWriteAccessAt(3));
    }

    /**
     * {@link ReadValue} equivalent to {@link SparseListCell} with {@link BooleanCell} elements.
     *
     * @since 4.3
     */
    public static interface BooleanSparseListReadValue extends SparseListReadValue, BooleanListReadValue {
    }

    /**
     * {@link WriteValue} equivalent to {@link SparseListCell} with {@link BooleanCell} elements.
     *
     * @since 4.3
     */
    public static interface BooleanSparseListWriteValue extends SparseListWriteValue {

        /**
         * @param values the values to set
         * @param defaultElement the default element which should not be saved multiple times
         */
        void setValue(boolean[] values, boolean defaultElement);
    }

    private static final class DefaultBooleanSparseListReadValue
        extends DefaultSparseListReadValue<BooleanReadValue, BooleanListReadValue, BooleanReadAccess>
        implements BooleanSparseListReadValue {

        private DefaultBooleanSparseListReadValue(final BooleanReadAccess defaultAccess, final IntReadAccess sizeAccess,
            final ListReadAccess indicesAccess, final ListReadAccess listAccess) {
            super(defaultAccess, sizeAccess, indicesAccess, listAccess, BooleanValueFactory.INSTANCE,
                BooleanListValueFactory.INSTANCE);
        }

        private boolean getBooleanFromStorage(final int storageIndex) {
            return m_storageList.getBoolean(storageIndex);
        }

        @Override
        public boolean getBoolean(final int index) {
            final OptionalInt storageIndex = storageIndexForIndex(index);
            if (storageIndex.isPresent()) {
                return getBooleanFromStorage(storageIndex.getAsInt());
            } else {
                return m_defaultValue.getBooleanValue();
            }
        }

        @Override
        public boolean[] getBooleanArray() {
            final boolean[] values = new boolean[size()];
            final Iterator<Boolean> iterator = booleanIterator();
            for (int i = 0; i < values.length; i++) {
                values[i] = iterator.next();
            }
            return values;
        }

        @Override
        public Iterator<Boolean> booleanIterator() {
            final boolean defaultElement = m_defaultValue.getBooleanValue();
            return new AbstractSparseIterator<Boolean>(size(), m_storageIndices.size(), m_storageIndices::getInt) {

                @Override
                public Boolean next() {
                    final OptionalInt storageIndex = nextStorageIndex();
                    if (storageIndex.isPresent()) {
                        return getBooleanFromStorage(storageIndex.getAsInt());
                    } else {
                        return defaultElement;
                    }
                }
            };
        }
    }

    private static final class DefaultBooleanSparseListWriteValue
        extends DefaultSparseListWriteValue<BooleanValue, BooleanWriteValue, BooleanListWriteValue, BooleanWriteAccess>
        implements BooleanSparseListWriteValue {

        protected DefaultBooleanSparseListWriteValue(final BooleanWriteAccess defaultAccess,
            final IntWriteAccess sizeAccess, final ListWriteAccess indicesAccess, final ListWriteAccess listAccess) {
            super(defaultAccess, sizeAccess, indicesAccess, listAccess, BooleanValueFactory.INSTANCE,
                BooleanListValueFactory.INSTANCE);
        }

        @Override
        public void setValue(final boolean[] values, final boolean defaultElement) {
            final List<Integer> storageIndices = new ArrayList<>();
            final List<Boolean> storageList = new ArrayList<>();

            for (int i = 0; i < values.length; i++) {
                final boolean v = values[i];
                if (v != defaultElement) {
                    storageIndices.add(i);
                    storageList.add(v);
                }
            }

            m_defaultValue.setBooleanValue(defaultElement);
            m_sizeValue.setIntValue(storageList.size());
            m_storageIndices.setValue(storageIndices.stream().mapToInt(Integer::intValue).toArray());
            m_storageList.setValue(ArrayUtils.toPrimitive(storageList.toArray(new Boolean[0])));
        }
    }
}