/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * ---------------------------------------------------------------------
 *
 * Created on Oct 30, 2012 by wiswedel
 */
package org.knime.core.data;

import java.io.IOException;

import org.knime.core.node.util.ConvenienceMethods;

/** Cell implementation to {@linkplain MissingValue}.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @since 2.7
 */
@SuppressWarnings("serial")
public final class MissingCell extends DataCell implements MissingValue {

    static final MissingCell INSTANCE = new MissingCell(null);

    /**
     * Returns the preferred value class of this cell implementation. This
     * method is called per reflection to determine which is the preferred
     * renderer, comparator, etc.
     *
     * @return MissingValue.class;
     */
    public static final Class<? extends DataValue> getPreferredValueClass() {
        return MissingValue.class;
    }

    private static final MissingSerializer SERIALIZER = new MissingSerializer();

    /**
     * Returns the factory to read/write DataCells of this class from/to a
     * DataInput/DataOutput. This method is called via reflection.
     *
     * @return A serializer for reading/writing cells of this kind.
     * @see DataCell
     */
    public static final MissingSerializer getCellSerializer() {
        return SERIALIZER;
    }

    private final String m_error;

    /**
     * Creates a new missing cell based on the given error value.
     *
     * @param error The error to store (possibly null).
     */
    public MissingCell(final String error) {
        m_error = error;
    }

    /** {@inheritDoc} */
    @Override
    public String getError() {
        return m_error;
    }

//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    public <V extends DataValue> V getAdapter(final Class<V> valueClass) {
//        throw new IllegalStateException("No adapters on missing cell");
//    }
//
//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    public <V extends DataValue> boolean isAdaptable(final Class<V> valueClass) {
//        return true;
//    }
//
//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    public <V extends DataValue> MissingValue getAdapterError(final Class<V> valueClass) {
//        return this;
//    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "?"; // don't show error here (result is displayed in table view)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        return ConvenienceMethods.areEqual(m_error, ((MissingCell)dc).m_error);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_error == null ? 0 : m_error.hashCode();
    }

    /** Factory for (de-)serializing a MissingCell. */
    private static class MissingSerializer implements DataCellSerializer<MissingCell> {

        // add more later, for instance if we also add a stack trace?
        private static final byte ERROR_SET_BIT = (byte)0x1;

        /** {@inheritDoc} */
        @Override
        public void serialize(final MissingCell cell, final DataCellDataOutput output) throws IOException {
            String error = cell.m_error;
            output.writeByte(error == null ? (byte)0 : ERROR_SET_BIT);
            if (error != null) {
                output.writeUTF(error);
            }
        }

        /** {@inheritDoc} */
        @Override
        public MissingCell deserialize(final DataCellDataInput input) throws IOException {
            byte mask = input.readByte();
            if ((mask & ERROR_SET_BIT) != (byte)0) {
                return new MissingCell(input.readUTF());
            } else {
                return MissingCell.INSTANCE;
            }
        }

    }


}
