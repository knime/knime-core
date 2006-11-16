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
 *   05.09.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.basic;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.LinkedList;
import java.util.List;

import org.knime.base.node.viz.plotter.AbstractDrawingPane;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class BasicDrawingPane extends AbstractDrawingPane {
    
    private final List<BasicDrawingElement> m_elements;
    
    /**
     * 
     *
     */
    public BasicDrawingPane() {
        super();
        m_elements = new LinkedList<BasicDrawingElement>();
    }
    
    /**
     * Removes all drawing elements. Repaint has to be triggered. 
     *
     */
    public void clearPlot() {
        m_elements.clear();
    }
    
    /**
     * 
     * @param line to draw.
     */
    public void addDrawingElement(final BasicDrawingElement line) {
        m_elements.add(line);
    }
    
    /**
     * 
     * @return the current stored drawing elements.
     */
    public List<BasicDrawingElement> getDrawingElements() {
        return m_elements;
    }

    /**
     * @see org.knime.base.node.viz.plotter.AbstractDrawingPane#paintContent(
     * java.awt.Graphics)
     */
    @Override
    public void paintContent(final Graphics g) {
        Graphics2D g2 = (Graphics2D)g;
        // paint paths if there are some
        if (getDrawingElements() != null) {
            for (BasicDrawingElement path : getDrawingElements()) {
                path.paint(g2);
            }
        }
    }
    
    


}
