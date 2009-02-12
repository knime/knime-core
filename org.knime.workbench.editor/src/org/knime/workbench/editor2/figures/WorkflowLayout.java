/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 *   29.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.figures;

import org.eclipse.draw2d.FreeformLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;

/**
 * Subclass of XYLayout which ensures that the children bounds are always used
 * as the layouting constraint. (manual layout).
 *
 * @author Florian Georg, University of Konstanz
 */
public class WorkflowLayout extends FreeformLayout {
    /**
     * {@inheritDoc}
     */
    @Override
    public void layout(final IFigure container) {
        super.layout(container);
    }

    /**
     * This ensures that all for all elements an <code>Rectangle</code>
     * objects is returned as contraint.
     *
     * {@inheritDoc}
     */
    @Override
    public Object getConstraint(final IFigure child) {
        Object constraint = constraints.get(child);

        // Do we already have a reactangle constraint ?
        if (constraint != null && constraint instanceof Rectangle) {
            return constraint;
        }

        // determine constraint from figures bounds
        Rectangle currentBounds = child.getBounds();
        int width = currentBounds.width;
        int height = currentBounds.height;
        if (width <= 0 || height <= 0) {
            width = -1;
            height = -1;
        }
        Rectangle rect = new Rectangle(currentBounds.x, currentBounds.y,
                width, height);
        return rect;
    }
}
