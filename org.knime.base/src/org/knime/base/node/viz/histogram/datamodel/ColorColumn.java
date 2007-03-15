/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *    01.01.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.histogram.datamodel;

import java.awt.Color;
import java.io.Serializable;

/**
 * Holds the color for a given row value.
 * @author Tobias Koetter, University of Konstanz
 */
public class ColorColumn implements Serializable {

    private static final long serialVersionUID = 8377552063347776434L;

    private final Color m_color;
    
    private final String m_columnName;
    
    private final int m_columnIndex;
    
    /**Constructor for class ColorColumn.
     * @param color the color
     * @param colIdx the index of this column from the {@link DataTableSpec}
     * @param colName the name of the column
     */
    public ColorColumn(final Color color, final int colIdx, 
            final String colName) {
        if (color == null) {
            throw new IllegalArgumentException("Color not defined");
        }
        if (colIdx < 0) {
            throw new IllegalArgumentException(
                    "Column index should be positive");
        }
        if (colName == null) {
            throw new IllegalArgumentException("No column name defined.");
        }
        m_color = color;
        m_columnName = colName;
        m_columnIndex = colIdx;
    }

    /**
     * @return the color of the column
     */
    public Color getColor() {
        return m_color;
    }

    /**
     * @return the name of the column
     */
    public String getColumnName() {
        return m_columnName;
    }

    /**
     * @return the index of the column in the table specification
     */
    public int getColumnIndex() {
        return m_columnIndex;
    }
}
