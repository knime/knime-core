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
 * Anchor that lets the connection start at the (left aligned) triangle of a
 * NodeOutPortFigure.
 * 
 * TODO hardcoded pixel constants ....
 * 
 * @author Florian Georg, University of Konstanz
 */
public class OutPortConnectionAnchor extends ChopboxAnchor {
    /**
     * @param figure The owner
     */
    public OutPortConnectionAnchor(final IFigure figure) {
        super(figure);
    }

    /**
     * @return The chop box for the out-port figure
     */
    @Override
    protected Rectangle getBox() {
        Rectangle b = getOwner().getBounds().getCopy();
        // set width
        b.setSize(24, b.height);
        // shrink height to center of the figure
        b.shrink(0, Math.max(0, (b.height / 2) - 5));

        return b;
    }

    /**
     * The point where the connection is starting at the input port is the
     * middle right point of the output port.
     * 
     * @param reference The reference point
     * @return The anchor location
     */
    @Override
    public Point getLocation(final Point reference) {

        Point point = getBox().getRight().getCopy().getTranslated(-2, 0);
        getOwner().translateToAbsolute(point);
        // get the box of the input port and get the left middle point
        // translate it one pixel to the left to better see the arrow
        return point;
    }
}
