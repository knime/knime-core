/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   01.03.2005 (mb): created
 *   23.05.2006 (mb): eliminated member holding ColorAttr
 *   21.06.06 (bw & po): reviewed
 */
package de.unikn.knime.core.data;

import java.io.Serializable;

import de.unikn.knime.core.data.def.StringCell;

/**
 * Key for a specific row which holds an identifier 
 * (of type <code>DataCell</code>).
 * 
 * @author M. Berthold, University of Konstanz
 */
public final class RowKey implements Serializable {

    // private members holding row id
    private final DataCell m_id;

    /**
     * Creates a row key based on a <code>DataCell</code> as id.
     * 
     * @param id identifier for a <code>DataRow</code>.
     * @throws NullPointerException If argument is <code>null</code>.
     */
    public RowKey(final DataCell id) {
        if (id == null) {
            throw new NullPointerException("Can't create RowKey with null id.");
        }
        m_id = id;
    }
    
    /**
     * Creates a row key based on a String as id. 
     * 
     * @param id identifier for a <code>DataRow</code>.
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
     * Returns the string representation of this row id <code>DataCell</code>.
     * 
     * @see Object#toString()
     */
    public String toString() {
        return m_id.toString();
    }

    /**
     * Compares two row keys.
     * @see java.lang.Object#equals(java.lang.Object)
     */
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
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return m_id.hashCode();
    }
    
}
