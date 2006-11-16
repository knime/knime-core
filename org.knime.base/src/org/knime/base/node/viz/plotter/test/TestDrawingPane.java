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
 *   24.08.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.test;

import java.awt.Graphics;

import org.knime.base.node.viz.plotter.basic.BasicDrawingPane;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class TestDrawingPane extends BasicDrawingPane {
    
    private int m_nrOfRows;
    
    private int m_nrOfColumns;
    
    /**
     * 
     * @param nrOfRows nr of rows
     */
    public void setNrOfRows(final int nrOfRows) {
        m_nrOfRows = nrOfRows;
    }
    
    /**
     * 
     * @param nrOfColumns nr of column
     */
    public void setNrOfColumn(final int nrOfColumns) {
        m_nrOfColumns = nrOfColumns;
    }

    /**
     * @see org.knime.base.node.viz.plotter.AbstractDrawingPane#
     * paintContent(java.awt.Graphics)
     */
    @Override
    public void paintContent(final Graphics g) {
//        Color[] colors = new Color[] {Color.red, Color.orange, Color.yellow, 
//                Color.green, Color.cyan, Color.blue, Color.pink};
//        int offset = getWidth() / colors.length;
//        for (int i = 0; i < colors.length; i++) {
//            g.setColor(colors[i]);
//            g.drawLine(i * offset, 0, i * offset, getHeight());
//        }
//        offset = getHeight() / colors.length;
//        for (int i = 0; i < colors.length; i++) {
//            g.setColor(colors[i]);
//            g.drawLine(0, i * offset, getWidth(), i * offset);
//        }
        g.drawString("nr of rows: " + m_nrOfRows, getWidth() / 2, 
                getHeight() / 2);
        g.drawString("nr of columns: " + m_nrOfColumns, (getWidth() / 2) + 20, 
                (getHeight() / 2) + 20);
    }

}
