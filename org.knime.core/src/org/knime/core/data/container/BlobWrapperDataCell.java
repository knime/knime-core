/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   Dec 15, 2006 (wiswedel): created
 */
package org.knime.core.data.container;

import java.io.IOException;
import java.lang.ref.SoftReference;

import org.knime.core.data.DataCell;
import org.knime.core.data.container.BlobDataCell.BlobAddress;

/**
 * Wrapper for {@link BlobDataCell}. We explicitly wrap those cells in this
 * package to delay the access to the latest time possible (when someone
 * calls getCell() on the row).
 * @author Bernd Wiswedel, University of Konstanz
 */
final class BlobWrapperDataCell extends DataCell {
    
    private final Buffer m_buffer;
    private final BlobAddress m_blobAddress;
    private final Class<? extends BlobDataCell> m_blobClass;
    private SoftReference<BlobDataCell> m_cellRef;
    
    /**
     * Keeps references.
     * @param b The buffer that owns the cell.
     * @param ba Its address.
     * @param cl The class of the blob.
     */
    BlobWrapperDataCell(final Buffer b, final BlobAddress ba,
            final Class<? extends BlobDataCell> cl) {
        assert b.getBufferID() == ba.getBufferID();
        m_buffer = b;
        m_blobAddress = ba;
        m_blobClass = cl;
    }
    
    /**
     * Fetches the content of the blob cell. May last long.
     * @return The blob Data cell being read.
     */
    BlobDataCell getCell() {
        BlobDataCell cell = m_cellRef != null ? m_cellRef.get() : null;
        if (cell == null) {
            try {
                cell = m_buffer.readBlobDataCell(m_blobAddress, m_blobClass);
                m_cellRef = new SoftReference<BlobDataCell>(cell);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }
        return cell;
    }
    
    /** @return The blob address. */
    BlobAddress getAddress() {
        return m_blobAddress;
    }
    
    /** @return Blob class. */
    Class<? extends BlobDataCell> getBlobClass() {
        return m_blobClass;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        assert false : "equalsDataCell must not be called on Blob wrapper";
        return getCell().equals(dc);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        assert false : "hasCode must not be called on Blob wrapper";
        return getCell().hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getCell().toString();
    }

}
