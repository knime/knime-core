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
 *   25.10.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.basic;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;

/**
 * Represents a textline, the startpoint has to be set with the 
 * {@link org.knime.base.node.viz.plotter.basic.BasicDrawingElement
 * #addDomainValue(DataCellPoint)}.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class BasicText extends BasicDrawingElement {
    
    private String m_text;
    
    /**
     * 
     * @param text the text to be displayed.
     */
    public BasicText(final String text) {
        m_text = text;
    }

    /**
     * Paints the text starting at the only set point.
     * 
     * @see org.knime.base.node.viz.plotter.basic.BasicDrawingElement#paint(
     * java.awt.Graphics2D)
     */
    @Override
    public void paint(final Graphics2D g2) {
        if (getPoints() != null && getPoints().size() > 0) {
            Color backup = g2.getColor();
            g2.setColor(getColor());
            Point p = getPoints().get(0);
            g2.drawString(m_text, p.x, p.y);
            g2.setColor(backup);
        }
    }

}
