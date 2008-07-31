/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 *   05.09.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.basic;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.LinkedList;
import java.util.List;

import org.knime.base.node.viz.plotter.AbstractDrawingPane;

/**
 * The <code>BasicDrawingPane</code> stores the 
 * {@link org.knime.base.node.viz.plotter.basic.BasicDrawingElement}s and 
 * paints them in the {@link #paintContent(Graphics)} method by calling their 
 * {@link org.knime.base.node.viz.plotter.basic.BasicDrawingElement
 * #paint(Graphics2D)} method. The 
 * {@link org.knime.base.node.viz.plotter.basic.BasicDrawingElement}s can be 
 * added or all can be removed. The mapping of the domain values to the 
 * DrawingPane's dimension is done in the 
 * {@link org.knime.base.node.viz.plotter.basic.BasicPlotter}.  
 * 
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
    public synchronized void clearPlot() {
        m_elements.clear();
    }
    
    /**
     * 
     * @param shape shape to draw
     */
    public synchronized void addDrawingElement(final BasicDrawingElement shape) {
        m_elements.add(shape);
    }
    
    /**
     * 
     * @return the current stored drawing elements
     */
    public synchronized List<BasicDrawingElement> getDrawingElements() {
        return m_elements;
    }

    /**
     * Paints all added 
     * {@link org.knime.base.node.viz.plotter.basic.BasicDrawingElement}s by 
     * calling their 
     * {@link org.knime.base.node.viz.plotter.basic.BasicDrawingElement
     * #paint(Graphics2D)} method.
     * If the BasicDrawingPane is extended this method have to be called with 
     * <code>super.paintContent()</code> in order to maintain the 
     * functionality of painting
     * {@link org.knime.base.node.viz.plotter.basic.BasicDrawingElement}s.
     *  
     * @see org.knime.base.node.viz.plotter.AbstractDrawingPane#paintContent(
     * java.awt.Graphics)
     */
    @Override
    public synchronized void paintContent(final Graphics g) {
        Graphics2D g2 = (Graphics2D)g;
        // paint paths if there are some
        if (getDrawingElements() != null) {
            for (BasicDrawingElement path : getDrawingElements()) {
                path.paint(g2);
            }
        }
    }
    
    


}
