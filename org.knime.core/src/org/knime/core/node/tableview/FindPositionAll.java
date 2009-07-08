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
 */
package org.knime.core.node.tableview;

/**
 * Postion information when searching occurrences in the entire table.
 * @author Bernd Wiswedel, University of Konstanz
 */
class FindPositionAll extends FindPositionRowKey {
    
    private int m_searchColumnInclRowIDCol;

    private int m_searchColumnInclRowIDColMark;

    private final int m_columnCountInclRowIDCol;

    /** Create new postion object.
     * @param rowCount The total number of rows in the table
     * @param columnCount The total number of columns in the table.
     */
    FindPositionAll(final int rowCount, final int columnCount) {
        super(rowCount);
        if (columnCount < 0) {
            throw new IllegalArgumentException("Column Count < 0: "
                    + columnCount);
        }
        m_columnCountInclRowIDCol = columnCount + 1;
        m_searchColumnInclRowIDCol = 0;
        m_searchColumnInclRowIDColMark = 0;
    }

    /** {@inheritDoc} */
    @Override
    int getSearchColumn() {
        return m_searchColumnInclRowIDCol - 1;
    }

    /** {@inheritDoc} */
    @Override
    boolean next() {
        int oldSearchCol = m_searchColumnInclRowIDCol;
        m_searchColumnInclRowIDCol =
                (m_searchColumnInclRowIDCol + 1) % m_columnCountInclRowIDCol;
        if (m_searchColumnInclRowIDCol < oldSearchCol) {
            return super.next();
        }
        return false;
    }
    
    /** {@inheritDoc} */
    @Override
    void reset() {
        super.reset();
        m_searchColumnInclRowIDCol = 0;
    }
    
    /** {@inheritDoc} */
    @Override
    boolean isIDOnly() {
        return false;
    }
    
    /** {@inheritDoc} */
    @Override
    void mark() {
        super.mark();
        m_searchColumnInclRowIDColMark = m_searchColumnInclRowIDCol;
    }
    
    /** {@inheritDoc} */
    @Override
    boolean reachedMark() {
        return super.reachedMark() 
            && m_searchColumnInclRowIDColMark == m_searchColumnInclRowIDCol;
    }

}
