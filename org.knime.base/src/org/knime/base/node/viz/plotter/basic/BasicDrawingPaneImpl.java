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
 *   12.09.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.basic;

import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class BasicDrawingPaneImpl extends BasicDrawingPane {


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
