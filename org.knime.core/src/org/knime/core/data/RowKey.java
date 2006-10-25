/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
 *   01.03.2005 (mb): created
 *   23.05.2006 (mb): eliminated member holding ColorAttr
 *   21.06.2006 (bw & po): reviewed
 *   25.10.2006 (tg): cleanup
 */
package org.knime.core.data;

import java.io.Serializable;

import org.knime.core.data.def.StringCell;

/**
 * Key for a specific row which holds an identifier(of type {@link DataCell}).
 * 
 * @see DataRow
 * @see RowIterator
 * 
 * @author Michael Berthold, University of Konstanz
 */
public final class RowKey implements Serializable {

    /** private members holding non-null row id. */
    private final DataCell m_id;

    /**
     * Creates a row key based on a {@link DataCell} as id.
     * 
     * @param id identifier for a {@link DataRow}.
     * @throws NullPointerException If argument is <code>null</code>.
     */
    public RowKey(final DataCell id) {
        if (id == null) {
            throw new NullPointerException("Can't create RowKey with null id.");
        }
        m_id = id;
    }
    
    /**
     * Creates a row key based on a {@link String} as id. 
     * 
     * @param id identifier for a {@link DataRow}.
     * @throws NullPointerException If argument is null.
     */
    public RowKey(final String id) {
        this(new StringCell(id));
    }

    /**
     * @return An non-null, ID for a row.
     */
    public DataCell getId() {
        return m_id;
    }

    /**
     * Returns the string representation of this row id.
     * 
     * @see DataCell#toString()
     */
    @Override
    public String toString() {
        return m_id.toString();
    }

    /**
     * Compares two row keys by their {@link DataCell} id.
     * 
     * @see DataCell#equals(Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof RowKey) {
            return ((RowKey)obj).m_id.equals(m_id);
        }
        return false;
    }

    /**
     * @see DataCell#hashCode()
     */
    @Override
    public int hashCode() {
        return m_id.hashCode();
    }
    
}
