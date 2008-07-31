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
     * {@inheritDoc}
     */
    @Override
    public abstract void paint(final Graphics2D g2);

}
