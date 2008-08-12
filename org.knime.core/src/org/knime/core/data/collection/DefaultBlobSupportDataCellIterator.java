/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   Aug 11, 2008 (wiswedel): created
 */
package org.knime.core.data.collection;

import java.util.Iterator;

import org.knime.core.data.DataCell;
import org.knime.core.data.container.BlobWrapperDataCell;

/**
 * Default implementation to {@link BlobSupportDataCellIterator}.
 * @author Bernd Wiswedel, University of Konstanz
 */
public class DefaultBlobSupportDataCellIterator implements
        BlobSupportDataCellIterator {
    
    private final Iterator<DataCell> m_it;
    
    /** Create new instance by wrapping an existing iterator.
     * @param it To wrap. */
    public DefaultBlobSupportDataCellIterator(final Iterator<DataCell> it) {
        m_it = it;
    }

    /** {@inheritDoc} */
    @Override
    public DataCell nextWithBlobSupport() {
        return m_it.next();
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasNext() {
        return m_it.hasNext();
    }

    /** {@inheritDoc} */
    @Override
    public DataCell next() {
        DataCell next = nextWithBlobSupport();
        if (next instanceof BlobWrapperDataCell) {
            return ((BlobWrapperDataCell)next).getCell();
        }
        return next;
    }

    /** {@inheritDoc} */
    @Override
    public void remove() {
        throw new UnsupportedOperationException(
                "No write DataCell collections");
    }

}
