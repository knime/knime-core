/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 */
package org.knime.base.data.append.column;

import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;


/**
 * A default factory that generates cells based on an underlying Map. This
 * default implementation only allows extension by one column.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class DefaultAppendedCellFactory implements AppendedCellFactory {

    private final Map<RowKey, DataCell> m_map;

    /**
     * Creates new factory. The mapping is based on the argument. It has to map
     * the row key to the new (to be appended) cell.
     * 
     * <p>
     * If the map does not contain requested keys the factory method will throw
     * an {@link java.util.NoSuchElementException}. Thus, make sure you provide
     * a complete list.
     * 
     * @param map mapping {@link DataCell} --&gt; {@link DataCell}
     * @throws NullPointerException if the map is <code>null</code>
     */
    public DefaultAppendedCellFactory(final Map<RowKey, DataCell> map) {
        if (map == null) {
            throw new NullPointerException();
        }
        m_map = map;
    }

    /**
     * Get the value to row's key.
     * 
     * @param row where to get the key from
     * @return the cell from the underlying map
     * @throws NullPointerException if the argument is <code>null</code>
     * @throws IllegalArgumentException if the key is not contained in the map
     *             or the value to the key is not instance of {@link DataCell}
     * @see AppendedCellFactory#getAppendedCell(DataRow)
     */
    public DataCell[] getAppendedCell(final DataRow row) {
        RowKey key = row.getKey();
        DataCell val = m_map.get(key);
        return new DataCell[]{val};
    }
}
