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
 * History
 *   Dec 15, 2006 (wiswedel): created
 */
package org.knime.core.data.container;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.SoftReference;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.container.BlobDataCell.BlobAddress;
import org.knime.core.node.NodeLogger;

/**
 * Wrapper for {@link BlobDataCell}. We explicitly wrap those cells in this
 * package to delay the access to the latest time possible (when someone
 * calls getCell() on the row). If such a cell has been added to an table
 * (by calling e.g.
 * {@link org.knime.core.node.BufferedDataContainer#addRowToTable(
 * org.knime.core.data.DataRow) addRowToTable in a DataContainer},
 * the framework will write the underlying blob to a dedicated file and
 * internally link to this blob file. The blob object itself can be garbage
 * collected and will silently re-instantiated if accessed.
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class BlobWrapperDataCell extends DataCell {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(BlobWrapperDataCell.class);

    private static boolean issuedWarningOnEqualsInvocation = false;

    private Buffer m_buffer;
    private BlobAddress m_blobAddress;
    private CellClassInfo m_blobClass;
    private SoftReference<BlobDataCell> m_cellRef;
    private BlobDataCell m_hardCellRef;

    /**
     * Keeps references.
     * @param b The buffer that owns the cell.
     * @param ba Its address.
     * @param cl The class information of the blob.
     */
    BlobWrapperDataCell(final Buffer b, final BlobAddress ba,
            final CellClassInfo cl) {
        assert b.getBufferID() == ba.getBufferID();
        m_buffer = b;
        m_blobAddress = ba;
        m_blobClass = cl;
    }

    /**
     * Create a new wrapper cell for a blob object. The wrapper will keep a
     * hard reference to the cell unless the cell is added to data container,
     * in which case the reference is softened.
     * @param cell The cell to wrap.
     */
    public BlobWrapperDataCell(final BlobDataCell cell) {
        m_blobClass = CellClassInfo.get(cell);
        m_blobAddress = cell.getBlobAddress();
        m_cellRef = new SoftReference<BlobDataCell>(cell);
        m_hardCellRef = cell;
    }

    /**
     * Fetches the content of the blob cell. May last long. The returned
     * DataCell is an instance of BlobDataCell unless there were problems
     * retrieving the blob from the file system, in which case a missing cell
     * is returned (and a warning message is logged to the logging system).
     * @return The blob Data cell being read.
     */
    public DataCell getCell() {
        if (m_hardCellRef != null) {
            return m_hardCellRef;
        }
        BlobDataCell cell = m_cellRef != null ? m_cellRef.get() : null;
        if (cell == null) {
            try {
                cell = m_buffer.readBlobDataCell(m_blobAddress, m_blobClass);
                m_cellRef = new SoftReference<BlobDataCell>(cell);
            } catch (IOException ioe) {
                String error = ioe.getMessage();
                Throwable cause = ioe.getCause();
                if (cause != null) {
                    error = error.concat(" (caused by \""
                            + cause.getClass().getSimpleName() + ": "
                            + cause.getMessage() + ")");
                }
                LOGGER.warn(error, ioe);
                return DataType.getMissingCell();
            }
        }
        return cell;
    }

    /** Framework method to set buffer and address.
     * @param address Address to set.
     * @param buffer Owner buffer to set.
     */
    void setAddressAndBuffer(final BlobAddress address, final Buffer buffer) {
        if (address == null || buffer == null) {
            throw new NullPointerException("Args must not be null");
        }
        if (m_blobAddress != null && !m_blobAddress.equals(address)) {
            throw new IllegalStateException("Blob wrapper cell has already "
                    + "an assigned blob address (" + m_blobAddress
                    + ", new one is " + address + ")");
        }
        if (m_buffer == null) {
            assert m_hardCellRef != null
            : "Should keep hard reference when buffer is not available.";
            m_hardCellRef.setBlobAddress(address);
            m_blobAddress = address;
            m_buffer = buffer;
            m_hardCellRef = null;
        }
    }

    /** @return The blob address. */
    BlobAddress getAddress() {
        return m_blobAddress;
    }

    /** @return Class of the blob. */
    @SuppressWarnings("unchecked")
    public Class<? extends BlobDataCell> getBlobClass() {
        return (Class<? extends BlobDataCell>)m_blobClass.getCellClass();
    }

    /** @return Class info to the blob. */
    CellClassInfo getBlobClassInfo() {
        return m_blobClass;
    }

    /** @return DataType associated with the underlying blob cell. */
    DataType getBlobDataType() {
        return getBlobClassInfo().getDataType();
    }

    /** @return owning buffer or null if not set yet. */
    Buffer getBuffer() {
        return m_buffer;
    }

    /** {@inheritDoc} */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        if (!issuedWarningOnEqualsInvocation) {
            issuedWarningOnEqualsInvocation = true;
            LOGGER.coding("Unexpected invocation of equalsDataCell on "
                    + "BlobWrapperCells -- this method is not to be called.");
        }
        if (dc instanceof BlobWrapperDataCell) {
            return getCell().equals(((BlobWrapperDataCell)dc).getCell());
        }
        return getCell().equals(dc);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return getCell().hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getCell().toString();
    }

    private static final long serialVersionUID = 461923972324323848L;

    private void writeObject(final ObjectOutputStream out)
        throws IOException {
        // Instances of this class should not be persisted in an ordinary use
        // case (when part of a DataContainer/Buffer). Serialization comes in
        // when the blob wrapper is part of a collection cell (e.g.
        // BlobSupportDataCellList) and the DataTableSpec saves collections
        // as part of the domain information (remotely connected to bug 2189)
        DataCell cell = getCell(); // also deserializes if necessary
        out.writeObject(cell);
        out.writeObject(m_blobAddress);
    }

    private void readObject(final ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        /* see #writeObject for details */
        // in hypothetical cases this could also only be a missing cell
        // (in case there were problems deserializing the blob during write)
        // live with exceptions here
        m_hardCellRef = (BlobDataCell)in.readObject();
        m_blobAddress = (BlobAddress)in.readObject();
        m_blobClass = CellClassInfo.get(m_hardCellRef);
    }

}
