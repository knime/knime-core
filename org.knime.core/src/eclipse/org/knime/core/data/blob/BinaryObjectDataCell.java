/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2012
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
 * Created on Sep 12, 2012 by wiswedel
 */
package org.knime.core.data.blob;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;

/**
 *
 * @author wiswedel
 */
public final class BinaryObjectDataCell extends DataCell implements BinaryObjectDataValue {

    public static final DataType TYPE = DataType.getType(BinaryObjectDataCell.class);

    private final byte[] m_bytes;

    public static final DataCellSerializer<BinaryObjectDataCell> getCellSerializer() {
        return new DataCellSerializer<BinaryObjectDataCell>() {

            /** {@inheritDoc} */
            @Override
            public BinaryObjectDataCell deserialize(final DataCellDataInput input)
                    throws IOException {
                int length = input.readInt();
                byte[] bytes = new byte[length];
                input.readFully(bytes);
                return new BinaryObjectDataCell(bytes);
            }

            /** {@inheritDoc} */
            @Override
            public void serialize(final BinaryObjectDataCell cell, final DataCellDataOutput output)
                    throws IOException {
                output.writeInt(cell.m_bytes.length);
                output.write(cell.m_bytes);
            }
        };
    }

    /**
     *
     */
    BinaryObjectDataCell(final byte[] bytes) {
        m_bytes = bytes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        byte[] otherBytes = ((BinaryObjectDataCell)dc).m_bytes;
        return Arrays.equals(otherBytes, m_bytes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(m_bytes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long length() {
        return m_bytes.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream openInputStream() throws IOException {
        return new ByteArrayInputStream(m_bytes);
    }

}
