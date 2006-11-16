/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   20.09.2006 (Fabian Dill): created
 */
package org.knime.exp.node.view.plotter.basic;

import org.knime.core.data.DataCell;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class DataCellPoint {
    
    private DataCell m_x;
    
    private DataCell m_y;
    
    /**
     * A point represented by two data cells.
     * @param x x
     * @param y y
     */
    public DataCellPoint(final DataCell x, final DataCell y) {
        m_x = x;
        m_y = y;
    }
    
    /**
     * 
     * @return x
     */
    public DataCell getX() {
        return m_x;
    }
    
    /**
     * 
     * @param x new x
     */
    public void setX(final DataCell x) {
        m_x = x;
    }
    
    /**
     * 
     * @return y
     */
    public DataCell getY() {
        return m_y;
    }
    
    /**
     * 
     * @param y new y
     */
    public void setY(final DataCell y) {
        m_y = y;
    }

}
