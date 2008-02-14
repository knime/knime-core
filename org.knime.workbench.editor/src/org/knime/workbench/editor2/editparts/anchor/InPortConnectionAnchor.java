/* 
 * -------------------------------------------------------------------
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
 *   15.08.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.editparts.anchor;

import org.eclipse.draw2d.ChopboxAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;

/**
 * Anchor that lets the connection end
 * 
 * TODO hardcoded pixel constants ....
 * 
 * @author Florian Georg, University of Konstanz
 */
public class InPortConnectionAnchor extends ChopboxAnchor {
    /**
     * @param figure The owner
     */
    public InPortConnectionAnchor(final IFigure figure) {
        super(figure);
    }

    /**
     * @return The chop box for the in port figure
     */
    @Override
    protected Rectangle getBox() {
        Rectangle b = getOwner().getBounds().getCopy();

        // set width
        b.setSize(16, b.height);

        // shrik height to center of the figure
        b.shrink(0, Math.max(0, (b.height / 2) - 5));

        b.translate(getOwner().getBounds().width - 24, 0);
        return b;
    }
    
    /**
     * The point where the connection is set to the input port is
     * the middle left point of the input port.
     * 
     * @param reference The reference point
     * @return The anchor location
     */
    @Override
    public Point getLocation(final Point reference) {
//        int lineWidth = ((Shape)getOwner()).getLineWidth();
        Point point = getBox().getLeft().getCopy(); //.getTranslated(0, 0);
        getOwner().translateToAbsolute(point);
        // get the box of the input port and get the left middle point
        // translate it one pixel to the left to better see the arrow
        return point;
    }
}
