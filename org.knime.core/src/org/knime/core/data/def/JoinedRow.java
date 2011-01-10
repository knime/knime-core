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
 */
package org.knime.core.data.def;

import java.util.Iterator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;

/**
 * Row that concatenates two given rows.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class JoinedRow implements DataRow {

    /** Underlying left row. */
    private final DataRow m_left;

    /** And its right counterpart. */
    private final DataRow m_right;

    /**
     * Creates a new row based on two given rows.
     * 
     * @param left The left row providing the head cells
     * @param right The right row providing the tail cells
     * @throws NullPointerException If either argument is null
     * @throws IllegalArgumentException If row key's ids aren't equal.
     */
    public JoinedRow(final DataRow left, final DataRow right) {
        RowKey lId = left.getKey();
        RowKey rId = right.getKey();
        if (!lId.equals(rId)) {
            throw new IllegalArgumentException("Key of rows do not match: \""
                    + lId + "\" vs. \"" + rId + "\"");
        }
        m_left = left;
        m_right = right;
    }

    /**
     * {@inheritDoc}
     */
    public int getNumCells() {
        return m_left.getNumCells() + m_right.getNumCells();
    }

    /**
     * Returns the key from the left row that was passed in the constructor.
     * 
     * {@inheritDoc}
     */
    public RowKey getKey() {
        return m_left.getKey();
    }

    /**
     * {@inheritDoc}
     */
    public DataCell getCell(final int index) {
        final int leftCellCount = m_left.getNumCells();
        // I guess both implementation will IndexOutOfBounds if out of range,
        // and so do we.
        if (index < leftCellCount) {
            return m_left.getCell(index);
        } else {
            return m_right.getCell(index - leftCellCount);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<DataCell> iterator() {
        return new DefaultCellIterator(this);
    }

    /**
     * @return the row, that was passed to the constructor and that holds the
     *         left cells (low index cells).
     */
    public DataRow getLeftRow() {
        return m_left;
    }

    /**
     * @return the row, that was passed to the constructor and that holds the
     *         right cells (high index cells).
     */
    public DataRow getRightRow() {
        return m_right;
    }
}
