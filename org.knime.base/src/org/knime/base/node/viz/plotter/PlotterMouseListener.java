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
 *   29.08.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter;

import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

/**
 * A mouse listener for the plotter mouse mode selection box. In addition 
 * to the mouse listener and mouse motion listener methods an 
 * appropriate cursor should be returned.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class PlotterMouseListener extends MouseAdapter
        implements MouseMotionListener {

    /**
     * {@inheritDoc}
     */
    public void mouseDragged(final MouseEvent e) {
    }

    /**
     * {@inheritDoc}
     */
    public void mouseMoved(final MouseEvent e) {
    }
    
    /**
     * 
     * @return the default cursor for this mouse listener.
     */
    public Cursor getCursor() {
        return Cursor.getDefaultCursor();
    }
    

}
