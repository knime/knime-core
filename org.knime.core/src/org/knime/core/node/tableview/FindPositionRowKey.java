/* ------------------------------------------------------------------
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
