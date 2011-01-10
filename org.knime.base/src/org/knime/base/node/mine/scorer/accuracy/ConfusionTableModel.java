/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
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
 *   29.09.2006 (sieb): created
 */
package org.knime.base.node.mine.scorer.accuracy;

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
