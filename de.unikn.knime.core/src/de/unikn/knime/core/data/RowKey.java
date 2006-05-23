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
 *   01.03.2005 (M. Berthold): created
 *   09.01.2006(all): reviewed
 */
package de.unikn.knime.core.data;

import java.io.Serializable;

/**
 * Unique key for a specific row. It holds an identifier (of type
 * <code>DataCell</code>) and also properties such as color and later also 
 * size, and shape of this particular row.
 * 
 * @author M. Berthold, University of Konstanz
 */
public final class RowKey implements Serializable {

    // private members holding id and properties
    private final DataCell m_id;

    /**
     * Constructor, initializes properties to default settings.
     * 
     * @param id unique identifier
     * @throws NullPointerException If argument is null.
     */
    public RowKey(final DataCell id) {
        m_id = id;
    }

    /**
     * Initializes properties as specified, copies remaining properties from
     * the argument.
     * 
     * @param key unique Rowkey
     */
    public RowKey(final RowKey key) {
        this(key.getId());
    }

    /**
     * @return A (non-null) unique ID of this row
     */
    public DataCell getId() {
        assert m_id != null;
        return m_id;
    }

    /**
     * Returns the string representation of the RowID <code>DataCell</code>.
     * 
     * @see Object#toString()
     */
    public String toString() {
        return m_id.toString();
    }

    /**
     * Compares only ids, not properties.
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
