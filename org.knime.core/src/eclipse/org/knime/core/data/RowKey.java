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
    public static String[] toStrings(final RowKey... rowKeys) {
        if (rowKeys == null) {
            return null;
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
    public static RowKey[] toRowKeys(final String... strs) {
        if (strs == null) {
            return null;
        }
        RowKey[] rowKeys = new RowKey[strs.length];
        for (int i = 0; i < rowKeys.length; i++) {
            rowKeys[i] = new RowKey(strs[i]);
        }
        return rowKeys;
    }
    
    /** Factory method to create "default" row IDs based on the row index. This
     * method should be used in all cases where row keys are auto-generated
     * (e.g. they are not read from a file) in order to comply to standard
     * naming conventions. The returned key will be in the form of 
     * <code>"Row" + rowIndex</code>.
     * @param rowIndex The index of the row, for which to generate a key. 
     * The first row has index 0. Note that non-negative values for 
     * <code>rowIndex</code> are perfectly legal but not encouraged.
     * @return A new key of the form <code>"Row" + rowIndex</code>
     */
    public static RowKey createRowKey(final int rowIndex) {
        return new RowKey("Row" + rowIndex);
    }
    
}
