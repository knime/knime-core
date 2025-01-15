/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */
package org.knime.testing.data.blob;

import java.io.IOException;
import java.util.Random;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.container.BlobDataCell;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;

/**
 * Blob cell that keeps a string identifier and whose size is artifically
 * increased by several 100k (random bytes).
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
@SuppressWarnings("serial")
public class LargeBlobCell extends BlobDataCell implements LargeBlobValue {

    /**
     * Serializer, as required by extension point.
     * @since 5.5
     */
    public static final class LargeBlobCellSerializer implements DataCellSerializer<LargeBlobCell> {
        private final Random m_random;

        public LargeBlobCellSerializer() {
            long time = System.currentTimeMillis();
            NodeLogger.getLogger(LargeBlobCell.class).debug(
                    "Using seed " + time);
            m_random = new Random(time);
        }

        /** {@inheritDoc} */
        @Override
        public LargeBlobCell deserialize(final DataCellDataInput input) throws IOException {
            int sizeOfCell = input.readInt();
            CheckUtils.checkArgument(sizeOfCell >= 0, "Negative size %d", sizeOfCell);
            for (int i = 0; i < sizeOfCell / 2; i++) {
                input.readByte();
            }
            String identifier = input.readUTF();
            for (int i = 0; i < sizeOfCell / 2; i++) {
                input.readByte();
            }
            return new LargeBlobCell(identifier, LargeBlobCell.SIZE_OF_CELL);
        }

        /** {@inheritDoc} */
        @Override
        public void serialize(final LargeBlobCell cell, final DataCellDataOutput output) throws IOException {
            output.writeInt(cell.m_sizeOfCell);
            byte[] ar = new byte[cell.m_sizeOfCell / 2];
            m_random.nextBytes(ar);
            output.write(ar);
            output.writeUTF(cell.m_identifier);
            m_random.nextBytes(ar);
            output.write(ar);
        }
    }

    public static final DataType TYPE = DataType.getType(LargeBlobCell.class);

    /** (minimum) size of a single blob in bytes. Some test cases test the size of the persisted workflow so it
     * needs to be sufficiently large to dominate the workflow size. */
    public static final int SIZE_OF_CELL = 1024 * 1024;

    /** Don't compress this cell. */
    public static final boolean USE_COMPRESSION = false;

    private final String m_identifier;

    /** The size in bytes as per constructor. */
    private final int m_sizeOfCell;

    /**
     * @param identifier The identifier saved in the binary garbage.
     * @param sizeOfCell The size of the cell in bytes.
     *
     */
    public LargeBlobCell(final String identifier, final int sizeOfCell) {
        m_identifier = CheckUtils.checkArgumentNotNull(identifier);
        CheckUtils.checkArgument(sizeOfCell >= 0, "Negative size %d", sizeOfCell);
        m_sizeOfCell = sizeOfCell;
    }

    /** {@inheritDoc} */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        return ((LargeBlobCell)dc).m_identifier.equals(m_identifier);
    }

    @Override
    public String getIdentifier() {
        return m_identifier;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return m_identifier.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return m_identifier;
    }

}
