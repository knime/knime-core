/*
 * ------------------------------------------------------------------
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
 *   01.03.2005 (mb): created
 *   23.05.2006 (mb): eliminated member holding ColorAttr
 *   21.06.2006 (bw & po): reviewed
 *   25.10.2006 (tg): cleanup
 *   31.10.2006 (tm, cs): reviewed
 */
package org.knime.core.data;


/**
 * Key for a specific row which holds an identifier of type {@link String}.
 * 
 * @see DataRow
 * @author Michael Berthold, University of Konstanz
 */
public final class RowKey {

    /** Private member holding non-null row id. */
    private final String m_id;
    
    /**
     * Creates a row key based on a {@link String}. 
     * 
     * @param id identifier for this key
     * @throws NullPointerException if argument is <code>null</code>
     */
    public RowKey(final String id) {
        if (id == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        m_id = id;
    }
    
    /** @return Underlying string of this row key. */
    public String getString() {
        return m_id;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getString();
    }

    /** {@inheritDoc} */
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
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_id.hashCode();
    }
    
    /**
     * Converts the given array of <code>RowKey</code>s to an array of
     * <code>String</code> elements by calling {@link RowKey#getString()}.
     * @param rowKeys an array of <code>RowKey</code> elements which can be null
     * @return an array of String elements
     */
    public static String[] toString(final RowKey... rowKeys) {
        if (rowKeys == null) {
            return (String[]) null;
        }
        String[] strs = new String[rowKeys.length];
        for (int i = 0; i < strs.length; i++) {
            strs[i] = rowKeys[i].getString();
        }
        return strs;
    }
    
    /**
     * Converts the given array of <code>String</code>s to an array of
     * <code>RowKey</code> elements by calling {@link #RowKey(String)}.
     * @param strs an array of <code>String</code> elements which can be null
     * @return an array of <code>RowKey</code> elements
     */
    public static RowKey[] toString(final String... strs) {
        if (strs == null) {
            return (RowKey[]) null;
        }
        RowKey[] rowKeys = new RowKey[strs.length];
        for (int i = 0; i < rowKeys.length; i++) {
            rowKeys[i] = new RowKey(strs[i]);
        }
        return rowKeys;
    }
    
}
