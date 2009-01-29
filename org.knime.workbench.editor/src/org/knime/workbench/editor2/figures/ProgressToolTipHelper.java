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
 *   27.10.2006 (sieb): created
 */
package org.knime.workbench.editor2.figures;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PopUpHelper;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.knime.workbench.editor2.WorkflowEditor;

/**
 * Implements a tool tip helper that is able to update a tooltip dynamically.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class ProgressToolTipHelper extends PopUpHelper {

    private ZoomManager m_zoomManager;

    /**
     * Constructs a ProgressToolTipHelper to be associated with Control <i>c</i>.
     * 
     * @param c the control
     */
    public ProgressToolTipHelper(final Control c, ZoomManager zoomManager) {
        super(c, SWT.TOOL | SWT.ON_TOP);
        getShell().setBackground(ColorConstants.tooltipBackground);
        getShell().setForeground(ColorConstants.tooltipForeground);

        m_zoomManager = zoomManager;
    }

    /**
     * Disposes of the tooltip's shell.
     * 
     * @see PopUpHelper#dispose()
     */
    public void dispose() {
        if (isShowing()) {
            hide();
        }
        getShell().dispose();
    }

    /**
     * Sets the LightWeightSystem's contents to the passed tooltip, and displays
     * the tip. The tip will be displayed only if the tip source is different
     * than the previously viewed tip source. (i.e. The cursor has moved off of
     * the previous tooltip source figure.)
     * <p>
     * The tooltip will be painted directly below the cursor if possible,
     * otherwise it will be painted directly above cursor.
     * 
     * No timer is set for this kind of dynamic tool tip.
     * 
     * @param hoverSource the figure over which the hover event was fired
     * @param tip the tooltip to be displayed
     * @param eventX the x coordinate of the hover event
     * @param eventY the y coordinate of the hover event
     * @since 2.0
     */
    public void displayToolTipNear(final IFigure hoverSource,
            final IFigure tip, final int eventX, final int eventY) {

        /*
         * If the cursor is not on any Figures, it has been moved off of the
         * control. Hide the tool tip.
         */
        if (hoverSource == null) {
            if (isShowing()) {
                hide();
            }
            return;
        }

        if (tip != null) {
            getLightweightSystem().setContents(tip);

            org.eclipse.draw2d.geometry.Point position =
                    new org.eclipse.draw2d.geometry.Point(eventX, eventY);

            WorkflowEditor.transposeZoom(m_zoomManager, position, true);

            Point absolute;
            absolute = control.toDisplay(new Point(position.x, position.y));
            Point displayPoint = computeWindowLocation(absolute.x, absolute.y);

            Dimension shellSize =
                    getLightweightSystem().getRootFigure().getPreferredSize()
                            .getExpanded(getShellTrimSize());
            setShellBounds(displayPoint.x, displayPoint.y, shellSize.width,
                    shellSize.height);
            show();
        }
    }

    /**
     * Hides this tooltip.
     */
    public void hideTip() {

        if (isShowing()) {
            hide();
        }
    }

    /*
     * Calculates the location where the tooltip will be painted. Returns this
     * as a Point. Tooltip will be painted directly below the cursor if
     * possible, otherwise it will be painted directly above cursor.
     */
    private Point computeWindowLocation(final int eventX, final int eventY) {
        org.eclipse.swt.graphics.Rectangle clientArea =
                control.getDisplay().getClientArea();
        Point preferredLocation = new Point(eventX, eventY + 26);

        Dimension tipSize =
                getLightweightSystem().getRootFigure().getPreferredSize()
                        .getExpanded(getShellTrimSize());

        // Adjust location if tip is going to fall outside display
        if (preferredLocation.y + tipSize.height > clientArea.height) {
            preferredLocation.y = eventY - tipSize.height;
        }

        if (preferredLocation.x + tipSize.width > clientArea.width) {
            preferredLocation.x -=
                    (preferredLocation.x + tipSize.width) - clientArea.width;
        }

        return preferredLocation;
    }

    // This method does not seem to be necessary at all. Even worse it prevents
    // a closed workflow to get garbage collected, because the created anonymous
    // inner class has a reference chain up to the workflow manager. The object
    // itself is registered at some global UI objects and stays alive until
    // Eclipse is closed.
    /**
     * {@inheritDoc}
     */
    @Override
    protected void hookShellListeners() {
//        // Close the tooltip window if the mouse enters the tooltip
//        getShell().addMouseTrackListener(new MouseTrackAdapter() {
//            public void mouseEnter(final MouseEvent e) {
//                hide();
//            }
//        });
    }
}
