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

import java.util.Iterator;

import org.knime.core.data.BooleanValue;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.value.ListValueFactory.DefaultListReadValue;
import org.knime.core.data.v2.value.ListValueFactory.DefaultListWriteValue;
import org.knime.core.data.v2.value.ValueInterfaces.BooleanListReadValue;
import org.knime.core.data.v2.value.ValueInterfaces.BooleanListWriteValue;
import org.knime.core.data.v2.value.ValueInterfaces.BooleanWriteValue;
import org.knime.core.table.access.BooleanAccess.BooleanReadAccess;
import org.knime.core.table.access.ListAccess.ListReadAccess;
import org.knime.core.table.access.ListAccess.ListWriteAccess;
import org.knime.core.table.schema.BooleanDataSpec;
import org.knime.core.table.schema.ListDataSpec;
import org.knime.core.table.schema.traits.DataTraits;
import org.knime.core.table.schema.traits.DefaultDataTraits;
import org.knime.core.table.schema.traits.DefaultListDataTraits;

import com.google.common.primitives.Booleans;

/**
 * {@link ValueFactory} implementation for {@link ListCell} with elements of type {@link BooleanCell}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 *
 * @noreference This class is not intended to be referenced by clients.
 */
public final class BooleanListValueFactory implements ValueFactory<ListReadAccess, ListWriteAccess> {

    /** A stateless instance of {@link BooleanListValueFactory} */
    public static final BooleanListValueFactory INSTANCE = new BooleanListValueFactory();

    @Override
    public ListDataSpec getSpec() {
        return new ListDataSpec(BooleanDataSpec.INSTANCE);
    }

    @Override
    public BooleanListReadValue createReadValue(final ListReadAccess reader) {
        return new DefaultBooleanListReadValue(reader);
    }

    @Override
    public BooleanListWriteValue createWriteValue(final ListWriteAccess writer) {
        return new DefaultBooleanListWriteValue(writer);
    }

    @Override
    public DataTraits getTraits() {
        return new DefaultListDataTraits(DefaultDataTraits.EMPTY);
    }


    private static final class DefaultBooleanListReadValue extends DefaultListReadValue
        implements BooleanListReadValue {

        private DefaultBooleanListReadValue(final ListReadAccess reader) {
            super(reader, BooleanValueFactory.INSTANCE, BooleanCell.TYPE);
        }

        @Override
        public boolean getBoolean(final int index) {
            final BooleanReadAccess v = m_reader.getAccess();
            m_reader.setIndex(index);
            return v.getBooleanValue();
        }

        @Override
        public boolean[] getBooleanArray() {
            final var result = new boolean[size()];
            for (int i = 0; i < result.length; i++) { // NOSONAR
                result[i] = getBoolean(i);
            }
            return result;
        }

        @Override
        public Iterator<Boolean> booleanIterator() {
            return Booleans.asList(getBooleanArray()).iterator();
        }
    }

    private static final class DefaultBooleanListWriteValue extends DefaultListWriteValue<BooleanValue, BooleanWriteValue>
        implements BooleanListWriteValue {

        private DefaultBooleanListWriteValue(final ListWriteAccess writer) {
            super(writer, BooleanValueFactory.INSTANCE);
        }

        @Override
        public void setValue(final boolean[] values) {
            setValue(values.length, i -> m_elementWriteValue.setBooleanValue(values[i]));
        }
    }

}