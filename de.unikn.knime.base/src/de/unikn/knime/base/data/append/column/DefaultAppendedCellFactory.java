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
 */
package de.unikn.knime.base.data.append.column;

import java.util.Map;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataRow;

/**
 * A default factory that generates cells based on an underlying Map. This
 * default implementation only allows the extension by one column.
 * @author Bernd Wiswedel, University of Konstanz
 */
public class DefaultAppendedCellFactory implements AppendedCellFactory {
    
    private final Map<DataCell, DataCell> m_map;
    
    /**
     * Creates new factory. The mapping is based on the argument. It has to
     * map the row key to the new (to be appended) cell. 
     * 
     * <p>If the map does not contain requested keys the factory method will
     * throw an <code>NoSuchElementException</code>. Thus, make sure you provide
     * a complete list.
     * @param map Mapping <code>DataCell</code> --&gt; <code>DataCell</code>. 
     * @throws NullPointerException If argument is <code>null</code>.
     */
    public DefaultAppendedCellFactory(final Map<DataCell, DataCell> map) {
        if (map == null) {
            throw new NullPointerException();
        }
        m_map = map;
    }

    /**
     * Get the value to row's key. 
     * @param row Where to get the key from.
     * @return The cell from the underlying map.
     * @throws NullPointerException If argument is null.
     * @throws IllegalArgumentException If the key is not contained in the map 
     *         or the value to the key is not instance of DataCell. 
     * @see AppendedCellFactory#getAppendedCell(DataRow)
     */
    public DataCell[] getAppendedCell(final DataRow row) {
        DataCell key = row.getKey().getId();
        DataCell val = m_map.get(key);
        return new DataCell[]{val};
    }

}
