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
 * History
 *    18.09.2008 (Tobias Koetter): created
 */

package org.knime.base.node.preproc.setoperator;

import org.knime.core.data.DataCell;
import org.knime.core.data.RowKey;

/**
 * Maps a {@link RowKey} to a {@link DataCell}.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class RowKeyCellMap {

    private final RowKey m_rowKey;
    private final DataCell m_cell;

    /**Constructor for class RowKeyCellMap.
     * @param key the {@link RowKey}
     * @param cell the {@link DataCell}
     */
    public RowKeyCellMap(final RowKey key, final DataCell cell) {
        if (key == null) {
            throw new NullPointerException("key must not be null");
        }
        if (cell == null) {
            throw new NullPointerException("cell must not be null");
        }
        m_rowKey = key;
        m_cell = cell;
    }

    /**
     * @return the {@link RowKey}
     */
    public RowKey getRowKey() {
        return m_rowKey;
    }

    /**
     * @return the {@link DataCell}
     */
    public DataCell getCell() {
        return m_cell;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return m_rowKey.getString() + "->" + m_cell.toString();
    }
}
