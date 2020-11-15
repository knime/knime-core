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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.MissingValue;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.SetCell;
import org.knime.core.data.collection.SetDataValue;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.data.v2.access.BooleanAccess.BooleanAccessSpec;
import org.knime.core.data.v2.access.BooleanAccess.BooleanReadAccess;
import org.knime.core.data.v2.access.BooleanAccess.BooleanWriteAccess;
import org.knime.core.data.v2.access.StructAccess.StructAccessSpec;
import org.knime.core.data.v2.access.StructAccess.StructReadAccess;
import org.knime.core.data.v2.access.StructAccess.StructWriteAccess;
import org.knime.core.data.v2.value.SetValueFactory.SetReadValue;
import org.knime.core.data.v2.value.SetValueFactory.SetWriteValue;

import com.google.common.collect.ImmutableList;

/**
 * {@link ValueFactory} implementation for {@link SetCell} with elements of type {@link BooleanCell}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 *
 * @noreference This class is not intended to be referenced by clients.
 */
public final class BooleanSetValueFactory implements ValueFactory<StructReadAccess, StructWriteAccess> {

    /** A stateless instance of {@link BooleanSetValueFactory} */
    public static final BooleanSetValueFactory INSTANCE = new BooleanSetValueFactory();

    @Override
    public StructAccessSpec getSpec() {
        return new StructAccessSpec(BooleanAccessSpec.INSTANCE, BooleanAccessSpec.INSTANCE, BooleanAccessSpec.INSTANCE);
    }

    @Override
    public BooleanSetReadValue createReadValue(final StructReadAccess reader) {
        return new DefaultBooleanSetReadValue(reader.getInnerReadAccessAt(0), reader.getInnerReadAccessAt(1),
            reader.getInnerReadAccessAt(2));
    }

    @Override
    public BooleanSetWriteValue createWriteValue(final StructWriteAccess writer) {
        return new DefaultBooleanSetWriteValue(writer.getWriteAccessAt(0), writer.getWriteAccessAt(1),
            writer.getWriteAccessAt(2));
    }

    /**
     * {@link ReadValue} equivalent to {@link SetCell} with {@link BooleanCell} elements.
     *
     * @since 4.3
     */
    public interface BooleanSetReadValue extends SetReadValue {

        /**
         * @param value a boolean value
         * @return true if the set contains the value
         */
        boolean contains(boolean value);

        /**
         * @return a {@link Set} containing the {@link Boolean} values
         */
        Set<Boolean> getBooleanSet();

        /**
         * @return an iterator of the boolean set
         * @throws IllegalStateException if the set contains a missing value
         */
        Iterator<Boolean> booleanIterator();
    }

    /**
     * {@link WriteValue} equivalent to {@link SetCell} with {@link BooleanCell} elements.
     *
     * @since 4.3
     */
    public interface BooleanSetWriteValue extends SetWriteValue {

        /**
         * Set the value.
         *
         * @param values a collection of boolean values
         */
        void setBooleanColletionValue(Collection<Boolean> values);
    }

    private static final class DefaultBooleanSetReadValue implements BooleanSetReadValue {

        private final BooleanReadAccess m_containsTrue;

        private final BooleanReadAccess m_containsFalse;

        private final BooleanReadAccess m_containsMissing;

        private DefaultBooleanSetReadValue(final BooleanReadAccess containsTrue, final BooleanReadAccess containsFalse,
            final BooleanReadAccess containsMissing) {
            m_containsTrue = containsTrue;
            m_containsFalse = containsFalse;
            m_containsMissing = containsMissing;
        }

        @Override
        public DataCell getDataCell() {
            return CollectionCellFactory.createSetCell(getCells());
        }

        private List<DataCell> getCells() {
            final List<DataCell> cells = new ArrayList<>(3);
            if (m_containsTrue.getBooleanValue()) {
                cells.add(BooleanCell.TRUE);
            }
            if (m_containsFalse.getBooleanValue()) {
                cells.add(BooleanCell.FALSE);
            }
            if (m_containsMissing.getBooleanValue()) {
                cells.add(DataType.getMissingCell());
            }
            return cells;
        }

        @Override
        public boolean contains(final DataCell cell) {
            if (cell.isMissing()) {
                return m_containsMissing.getBooleanValue();
            } else {
                return contains(((BooleanValue)cell).getBooleanValue());
            }
        }

        @Override
        public DataType getElementType() {
            return BooleanCell.TYPE;
        }

        @Override
        public int size() {
            return (m_containsTrue.getBooleanValue() ? 1 : 0) //
                + (m_containsFalse.getBooleanValue() ? 1 : 0) //
                + (m_containsMissing.getBooleanValue() ? 1 : 0);
        }

        @Override
        public boolean containsBlobWrapperCells() {
            return false;
        }

        @Override
        public Iterator<DataCell> iterator() {
            return getCells().iterator();
        }

        @Override
        public boolean contains(final boolean value) {
            if (value) {
                return m_containsTrue.getBooleanValue();
            } else {
                return m_containsFalse.getBooleanValue();
            }
        }

        @Override
        public Set<Boolean> getBooleanSet() {
            final Set<Boolean> set = new HashSet<>();
            if (m_containsTrue.getBooleanValue()) {
                set.add(true);
            }
            if (m_containsFalse.getBooleanValue()) {
                set.add(false);
            }
            return set;
        }

        @Override
        public Iterator<Boolean> booleanIterator() {
            return getBooleanSet().iterator();
        }
    }

    private static final class DefaultBooleanSetWriteValue implements BooleanSetWriteValue {

        private final BooleanWriteAccess m_containsTrue;

        private final BooleanWriteAccess m_containsFalse;

        private final BooleanWriteAccess m_containsMissing;

        private DefaultBooleanSetWriteValue(final BooleanWriteAccess containsTrue,
            final BooleanWriteAccess containsFalse, final BooleanWriteAccess containsMissing) {
            m_containsTrue = containsTrue;
            m_containsFalse = containsFalse;
            m_containsMissing = containsMissing;
        }

        @Override
        public void setValue(final Collection<DataValue> values) {
            boolean containsTrue = false;
            boolean containsFalse = false;
            boolean containsMissing = false;

            for (final DataValue v : values) {
                if (v instanceof MissingValue) {
                    containsMissing = true;
                } else if (((BooleanValue)v).getBooleanValue()) {
                    containsTrue = true;
                } else {
                    containsFalse = true;
                }
                if (containsTrue && containsFalse && containsMissing) {
                    break;
                }
            }

            m_containsTrue.setBooleanValue(containsTrue);
            m_containsFalse.setBooleanValue(containsFalse);
            m_containsMissing.setBooleanValue(containsMissing);
        }

        @Override
        public void setValue(final SetDataValue value) {
            setValue(ImmutableList.copyOf(value.iterator()));
        }

        @Override
        public void setBooleanColletionValue(final Collection<Boolean> values) {
            boolean containsTrue = false;
            boolean containsFalse = false;

            for (final Boolean v : values) {
                if (v.booleanValue()) {
                    containsTrue = true;
                } else {
                    containsFalse = true;
                }
                if (containsTrue && containsFalse) {
                    break;
                }
            }

            m_containsTrue.setBooleanValue(containsTrue);
            m_containsFalse.setBooleanValue(containsFalse);
            m_containsMissing.setBooleanValue(false);
        }
    }
}