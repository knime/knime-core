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
import java.io.Serializable;

/**
 * Holds the color for a given column name.
 * @author Tobias Koetter, University of Konstanz
 */
public class ColorColumn implements Serializable {

    private static final long serialVersionUID = 761641948686587L;

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

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_color == null) ? 0 : m_color.hashCode());
        result = prime * result 
            + ((m_columnName == null) ? 0 : m_columnName.hashCode());
        return result;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ColorColumn other = (ColorColumn)obj;
        if (m_color == null) {
            if (other.m_color != null) {
                return false;
            }
        } else if (!m_color.equals(other.m_color)) {
            return false;
        }
        if (m_columnName == null) {
            if (other.m_columnName != null) {
                return false;
            }
        } else if (!m_columnName.equals(other.m_columnName)) {
            return false;
        }
        return true;
    }

}
