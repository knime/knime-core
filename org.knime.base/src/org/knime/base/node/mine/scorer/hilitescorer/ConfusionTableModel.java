/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
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
 *   29.09.2006 (sieb): created
 */
package org.knime.base.node.mine.scorer.hilitescorer;

import javax.swing.table.AbstractTableModel;

/**
 * A table model representing a confusion matrix.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class ConfusionTableModel extends AbstractTableModel {

    /**
     * The int 2-D array representing the confution matrix.
     */
    private int[][] m_scoreCount;

    /**
     * The names of the attributes.
     */
    private String[] m_headers;

    /**
     * The column name (description) of the scored attributes for the row
     * header.
     */
    private String m_rowHeaderDescription;

    /**
     * The column name (description) of the scored attributes for the column
     * header.
     */
    private String m_columnHeaderDescription;

    /**
     * Constructs confusion table model from the score count and the headers.
     * 
     * @param scoreCount
     *            a 2-D int array representing the confusion matrix.
     * @param headers
     *            the names of the attributes to display in the table
     * @param rowHeaderDescription row header description
     * @param columnHeaderDescription column header description
     */
    public ConfusionTableModel(final int[][] scoreCount,
            final String[] headers, final String rowHeaderDescription,
            final String columnHeaderDescription) {
        m_scoreCount = scoreCount;
        m_headers = headers;
        m_rowHeaderDescription = rowHeaderDescription;
        m_columnHeaderDescription = columnHeaderDescription;
    }

    /**
     * Column and row count are equal as this is a square matrix. Note: + 1 for
     * the column header
     * 
     * {@inheritDoc}
     */
    public int getColumnCount() {

        return m_scoreCount.length + 1;
    }

    /**
     * Column and row count are equal as this is a square matrix. Note: + 1 for
     * the row header
     * 
     * {@inheritDoc}
     */
    public int getRowCount() {

        return m_scoreCount.length;
    }

    /**
     * Returns the confusion matrix value at the corresponding position. Note:
     * the first column contains like the column names also the attribute names.
     * 
     * {@inheritDoc}
     */
    public Object getValueAt(final int rowIndex, final int columnIndex) {

        // convert the column index to the actual one
        int realColumnIndex = columnIndex - 1;

        // if the column index is now -1 => the column name is returned
        if (realColumnIndex < 0) {
            return m_headers[rowIndex];
        }

        return m_scoreCount[rowIndex][realColumnIndex];
    }

    /**
     * Returns the column names, i.e. the attribute names of the confusion
     * matrix.
     * 
     * @param column
     *            the index of the column in the table to display
     * @return the column (attribute) name for the given column index
     */
    @Override
    public String getColumnName(final int column) {

        // convert to real column name
        int realColumnIndex = column - 1;
        if (realColumnIndex < 0) {
            // return the column header for the first column
            return m_rowHeaderDescription + " \\ " + m_columnHeaderDescription;
        }
        return m_headers[realColumnIndex];
    }

}
