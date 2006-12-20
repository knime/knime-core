/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *   11.08.2006 (sieb): created
 */
package org.knime.workbench.editor2.editparts;

import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;

/**
 * Abstract part class to do things for all parts involved in a node. These are
 * ports and nodes so far.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public abstract class KnimeAbstractPart extends AbstractGraphicalEditPart
        implements MouseListener {

    private static final long DOUBLE_CLICK_TIME = 500;

    /**
     * To implement manually the double click event.
     */
    private long m_lastClick;

    /**
     * Implements a manual double click. TODO: at the
     * moment every 4th pressed event is not submitted to this listener. Find
     * out why. Seems to be a draw2D problme.
     * 
     * @see org.eclipse.draw2d.MouseListener#
     *      mousePressed(org.eclipse.draw2d.MouseEvent)
     */
    public void mousePressed(final MouseEvent me) {

        // only left click matters
        if (me.button != 1) {
            return;
        }

        if (System.currentTimeMillis() - m_lastClick < DOUBLE_CLICK_TIME) {

            // invoke the method to be overriden by sub classes
            doubleClick(me);

            // me.consume();
        }
        m_lastClick = System.currentTimeMillis();

    }

    /**
     * Does nothing.
     * 
     * @see org.eclipse.draw2d.MouseListener#
     *      mouseReleased(org.eclipse.draw2d.MouseEvent)
     */
    public void mouseReleased(final MouseEvent me) {
        // do nothing yet
    }

    /**
     * Does nothing.
     * 
     * @see org.eclipse.draw2d.MouseListener#
     *      mouseDoubleClicked(org.eclipse.draw2d.MouseEvent)
     */
    public void mouseDoubleClicked(final MouseEvent me) {

        // do nothing yet
    }

    /**
     * To be overriden, if the derived part want to react on double click.
     * 
     * @param me the mouse event
     */
    public void doubleClick(final MouseEvent me) {

    }

}
