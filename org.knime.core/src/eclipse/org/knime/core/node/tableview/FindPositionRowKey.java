/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   Jan 31, 2009 (wiswedel): created
 */
package org.knime.core.node.tableview;

/**
 * Position information that is used when searching row keys. It is used to mark
 * and traverse the table.
 * @author Bernd Wiswedel, University of Konstanz
 */
class FindPositionRowKey {
    
    private int m_position;
    
    private int m_markedPosition;
    
    private final int m_rowCount;
    
    /** Creates new  position information.
     * @param rowCount The total number of rows in the table to search.
     */
    FindPositionRowKey(final int rowCount) {
        if (rowCount < 0) {
            throw new IllegalArgumentException("Row Count < 0: " + rowCount);
        }
        m_rowCount = rowCount;
        m_position = 0;
        m_markedPosition = m_position;
    }

    /** Pushes the search position to its next location.
     * @return true if it advanced to the next position, false if it starts
     * from top.
     */
    boolean next() {
        int oldSearchRow = m_position;
        m_position = (m_position + 1) % (m_rowCount + 1);
        return m_position < oldSearchRow;
    }
    
    /** Reset position to row 0. */
    void reset() {
        m_position = 0;
    }
    
    /** Set a mark (memorize last search hit location). */
    void mark() {
        m_markedPosition = m_position;
    }
    
    /** @return true if we reached the mark (again). */
    boolean reachedMark() {
        return m_markedPosition == m_position;
        
    }

    /** @return Current location row. */
    int getSearchRow() {
        return m_position - 1;
    }

    /** @return Current location column (in this class -1). */
    int getSearchColumn() {
        return -1;
    }
    
    /** If to look for IDs only.
     * @return true
     */
    boolean isIDOnly() {
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("Current Row: ");
        b.append(getSearchRow()).append(", Current Col: ");
        int col = getSearchColumn();
        if (col < 0) {
            b.append("<row ID column>");
        } else {
            b.append(col);
        }
        return b.toString();
    }
}
