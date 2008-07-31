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
     * {@inheritDoc}
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
