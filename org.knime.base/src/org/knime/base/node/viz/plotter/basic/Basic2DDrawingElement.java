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
 *   06.09.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.basic;

import java.awt.Graphics2D;

/**
 * This class is a specialization of the {@link BasicDrawingElement} by 
 * providing a flag whether the shape should be filled or not.
 * 
 * @see org.knime.base.node.viz.plotter.basic.BasicDrawingElement
 * 
 * @author Fabian Dill, University of Konstanz
 */
public abstract class Basic2DDrawingElement extends BasicDrawingElement {

    /** Flag whether the shpe should be filled or not. */
    private boolean m_filled;
    
    /**
     * Creates a shape either filled or not.
     * 
     * @param filled true, if the shape should be filled, false otherwise.
     */
    public Basic2DDrawingElement(final boolean filled) {
        m_filled = filled;
    }
    
    /**
     * Returns whether the shape should be filled (true) or not (false).
     * 
     * @return true if the shape should be filled
     */
    public boolean isFilled() {
        return m_filled;
    }
    
    /**
     * @see org.knime.base.node.viz.plotter.basic.BasicDrawingElement#paint(
     * java.awt.Graphics2D)
     */
    @Override
    public abstract void paint(final Graphics2D g2);

}
