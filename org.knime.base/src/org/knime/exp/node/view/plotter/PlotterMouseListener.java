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
 *   29.08.2006 (Fabian Dill): created
 */
package org.knime.exp.node.view.plotter;

import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class PlotterMouseListener extends MouseAdapter
        implements MouseMotionListener {

    /**
     * @see java.awt.event.MouseMotionListener#mouseDragged(
     * java.awt.event.MouseEvent)
     */
    public void mouseDragged(final MouseEvent e) {
    }

    /**
     * @see java.awt.event.MouseMotionListener#mouseMoved(
     * java.awt.event.MouseEvent)
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
