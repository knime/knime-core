/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
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
 *   07.07.2005 (mb): created
 */
package de.unikn.knime.core.data.def;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataType;
import de.unikn.knime.core.data.StringType;
import de.unikn.knime.core.data.StringValue;

/**
 * A data cell implementation holding a string value by storing this value in a
 * private <code>String</code> member.
 * 
 * @author mb, University of Konstanz
 */
public final class DefaultStringCell extends DataCell implements StringValue {

    private final String m_string;

    /**
     * Creates a new String Cell based on the given String value.
     * 
     * @param str The String value to store.
     * @throws NullPointerException If the given String value is
     *             <code>null</code>.
     */
    public DefaultStringCell(final String str) {
        if (str == null) {
            throw new NullPointerException("String value can't be null.");
        }
        m_string = str;
    }

    /**
     * @see de.unikn.knime.core.data.StringValue#getStringValue()
     */
    public String getStringValue() {
        return m_string;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return getStringValue();
    }

    /**
     * @see de.unikn.knime.core.data.DataCell
     *      #equalsDataCell(de.unikn.knime.core.data.DataCell)
     */
    protected boolean equalsDataCell(final DataCell dc) {
        return m_string.equals(((DefaultStringCell)dc).m_string);
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return m_string.hashCode();
    }

    /**
     * @see de.unikn.knime.core.data.DataCell#getType()
     */
    public DataType getType() {
        return StringType.STRING_TYPE;
    }

}
