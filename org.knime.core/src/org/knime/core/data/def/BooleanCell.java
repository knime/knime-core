/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 * -------------------------------------------------------------------
 *
 */
package org.knime.core.data.def;

import java.io.IOException;
import java.io.ObjectStreamException;

import org.knime.core.data.BooleanValue;
import org.knime.core.data.BoundedValue;
import org.knime.core.data.ComplexNumberValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.FuzzyIntervalValue;
import org.knime.core.data.FuzzyNumberValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.NominalValue;

/**
 * A data cell implementation holding a boolean value by storing this value in
 * a private <code>boolean</code> member. It provides an boolean value, a double
 * value, a fuzzy number value, as well as a fuzzy interval value.
 *
 * <p>This class is not to be instantiated; use the singleton representing
 * the true and false state.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class BooleanCell extends DataCell implements BooleanValue,
        IntValue, LongValue, DoubleValue, ComplexNumberValue, FuzzyNumberValue,
        FuzzyIntervalValue, BoundedValue, NominalValue {

    private static final long serialVersionUID = -3240706690088236437L;

    /** TRUE instance. */
    public static final BooleanCell TRUE = new BooleanCell(true);

    /** FALSE instance. */
    public static final BooleanCell FALSE = new BooleanCell(false);

    /**
     * Convenience access member for
     * <code>DataType.getType(BooleanCell.class)</code>.
     *
     * @see DataType#getType(Class)
     */
    public static final DataType TYPE = DataType.getType(BooleanCell.class);

    /**
     * Returns the preferred value class of this cell implementation. This
     * method is called per reflection to determine which is the preferred
     * renderer, comparator, etc.
     *
     * @return BooleanValue.class;
     */
    public static final Class<? extends DataValue> getPreferredValueClass() {
        return BooleanValue.class;
    }

    private static final DataCellSerializer<BooleanCell> SERIALIZER =
            new BooleanSerializer();

    /**
     * Returns the factory to read/write DataCells of this class from/to a
     * DataInput/DataOutput. This method is called via reflection.
     *
     * @return A serializer for reading/writing cells of this kind.
     * @see DataCell
     */
    public static final DataCellSerializer<BooleanCell> getCellSerializer() {
        return SERIALIZER;
    }

    private final boolean m_boolean;

    /**
     * Creates new cell for a generic boolean value.
     *
     * @param b The boolean value to store.
     */
    private BooleanCell(final boolean i) {
        m_boolean = i;
    }

    /** {@inheritDoc} */
    @Override
    public boolean getBooleanValue() {
        return m_boolean;
    }

    /** Returns 1 if value is true or 0 for false.
     * {@inheritDoc}
     */
    @Override
    public int getIntValue() {
        return m_boolean ? 1 : 0;
    }

    /** Returns 1 if value is true or 0 for false.
     * {@inheritDoc} */
    @Override
    public long getLongValue() {
        return getIntValue();
    }

    /** Returns 1 if value is true or 0 for false.
     * {@inheritDoc}
     */
    @Override
    public double getDoubleValue() {
        return m_boolean ? 1.0 : 0.0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getCore() {
        return getDoubleValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getMaxSupport() {
        return getDoubleValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getMinSupport() {
        return getDoubleValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getMaxCore() {
        return getDoubleValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getMinCore() {
        return getDoubleValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getCenterOfGravity() {
        return getDoubleValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getImaginaryValue() {
        return 0.0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getRealValue() {
        return getDoubleValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        return ((BooleanCell)dc).m_boolean == m_boolean;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return getIntValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Boolean.toString(m_boolean);
    }

    /** Recommended by java.io.Serializable to return singletons.
     * @throws ObjectStreamException Never actually thrown.
     */
    private Object readResolve() throws ObjectStreamException {
        return m_boolean ? TRUE : FALSE;
    }

    /** Factory for (de-)serializing a BooleanCell. */
    private static class BooleanSerializer
        implements DataCellSerializer<BooleanCell> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void serialize(final BooleanCell cell,
                final DataCellDataOutput output) throws IOException {
            output.writeBoolean(cell.m_boolean);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public BooleanCell deserialize(
                final DataCellDataInput input) throws IOException {
            boolean b = input.readBoolean();
            return b ? TRUE : FALSE;
        }
    }

}
