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

package org.knime.base.node.viz.histogram.util;

import java.awt.Color;

/**
 * Holds the color for a given row value.
 * @author Tobias Koetter, University of Konstanz
 */
public class ColorColumn {

    private final Color m_color;
    
    private final String m_columnName;
    
    /**Constructor for class ColorColumn.
     * @param color the color
     * @param colName the name of the column
     */
    public ColorColumn(final Color color, final String colName) {
        if (color == null) {
            throw new IllegalArgumentException("Color not defined");
        }
        if (colName == null) {
            throw new IllegalArgumentException("No column name defined.");
        }
        m_color = color;
        m_columnName = colName;
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

}
