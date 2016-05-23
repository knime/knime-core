/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * History
 *   03.09.2008 (ohl): created
 */
package org.knime.core.data.vector.stringvector;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.IntStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.ConvenienceMethods;

/**
 * {@link StringVectorValue} as dense representation, backed by a String[].
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @since 3.2
 */
public class DenseStringVectorCell extends DataCell implements StringVectorValue, CollectionDataValue {

    /**
     *
     */
    private static final long serialVersionUID = 2032495375214103971L;
    private final String[] m_stringVector;

    /**
     * Use the {@link DenseStringVectorCellFactory} to create instances of this cell.
     * @param stringVector underlying vector.
     */
    DenseStringVectorCell(final String[] stringVector) {
        m_stringVector = CheckUtils.checkArgumentNotNull(stringVector);
        for (String s : m_stringVector) {
            if (s == null) {
                throw new IllegalArgumentException("Elements in the string vector must not be null");
            }
        }
    }

    /* ########################
     * StringVectorValue
     * ######################## */

    /** {@inheritDoc} */
    @Override
    public int getLength() {
        return m_stringVector.length;
    }

    /** {@inheritDoc} */
    @Override
    public String getValue(final int index) {
        return m_stringVector[index];
    }

    /* ########################
     * CollectionDataValue
     * ######################## */

    /** {@inheritDoc} */
    @Override
    public int size() {
        return getLength();
    }

    /** {@inheritDoc} */
    @Override
    public DataType getElementType() {
        return StringCell.TYPE;
    }

    /** {@inheritDoc} */
    @Override
    public boolean containsBlobWrapperCells() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public Iterator<DataCell> iterator() {
        return Arrays.stream(m_stringVector).map(d -> (DataCell)new StringCell(d)).iterator();
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Arrays.hashCode(m_stringVector);
    }

    /** {@inheritDoc} */
    @Override
    protected boolean equalContent(final DataValue otherValue) {
        StringVectorValue other = (StringVectorValue)otherValue;
        if (other.getLength() != getLength()) {
            return false;
        }
        return IntStream.range(0, m_stringVector.length).allMatch(i -> getValue(i).equals(other.getValue(i)));
    }

    /** {@inheritDoc} */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        DenseStringVectorCell o = (DenseStringVectorCell)dc;
        return Arrays.equals(m_stringVector, o.m_stringVector);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return ConvenienceMethods.getShortStringFrom(iterator(), m_stringVector.length, 3);
    }

    /**
     * Serializer {@link DenseStringVectorCell}s.
     *
     * @noreference This class is not intended to be referenced by clients.
     */
    public static final class DenseStringVectorSerializer implements DataCellSerializer<DenseStringVectorCell> {
        /** {@inheritDoc} */
        @Override
        public void serialize(final DenseStringVectorCell cell, final DataCellDataOutput out) throws IOException {
            String[] cnts = cell.m_stringVector;
            out.writeInt(cnts.length);
            for (int i = 0; i < cnts.length; i++) {
                out.writeUTF(cnts[i]);
            }
        }

        /** {@inheritDoc} */
        @Override
        public DenseStringVectorCell deserialize(final DataCellDataInput input) throws IOException {
            int arrayLength = input.readInt();
            String[] vector = new String[arrayLength];
            for (int i = 0; i < arrayLength; i++) {
                vector[i] = input.readUTF();
            }
            return new DenseStringVectorCell(vector);
        }
    }
}
