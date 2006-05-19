/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
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
 *   Aug 8, 2005 (georg): created
 */
package de.unikn.knime.workbench.editor2.figures;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Locator;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;

/**
 * Locator for port figures. Makes sure that the ports are "near" the centered
 * icon of the surrounding <code>NodeContainerFigure</code>
 * 
 * @author Florian Georg, University of Konstanz
 */
public class PortLocator implements Locator {

    /** in-port type. */
    public static final int TYPE_INPORT = 0;

    /** out-port type. */
    public static final int TYPE_OUTPORT = 1;

    private NodeContainerFigure m_parent;

    private int m_type;

    private int m_maxPorts;

    private int m_portIndex;

    /**
     * Creates a new locator.
     * 
     * @param parent The parent figure (NodeContainerFigure)
     * @param type The type
     * @param maxPorts max Ports
     * @param portIndex The port index
     */
    public PortLocator(final NodeContainerFigure parent, final int type,
            final int maxPorts, final int portIndex) {
        m_parent = parent;
        m_type = type;
        m_maxPorts = maxPorts;
        m_portIndex = portIndex;
    }

    /**
     * @see org.eclipse.draw2d.Locator #relocate(org.eclipse.draw2d.IFigure)
     */
    public void relocate(final IFigure fig) {
        Rectangle parentBounds = m_parent.getContentFigure().getBounds()
                .getCopy();
        if (m_type == TYPE_INPORT) {
            int portHeight = parentBounds.height / m_maxPorts;
            int portWidth = parentBounds.width / 2;
            int x = parentBounds.getLeft().x;

            // for inports use m_maxPorts - 1 - m_portIndex to revert the
            // order this results in modelInPorts arranged at the top of a node
            // and modelOutPorts at the bottom of a figure
            int y = parentBounds.getTopLeft().y
                    + ((m_maxPorts - 1 - m_portIndex) * portHeight);

            Rectangle portBounds = new Rectangle(new Point(x, y),
                    new Dimension(portWidth, portHeight));

            fig.setBounds(portBounds);
        } else {
            int portHeight = parentBounds.height / m_maxPorts;
            int portWidth = parentBounds.width / 2;
            int x = parentBounds.getCenter().x;
            int y = parentBounds.getTopRight().y + (m_portIndex * portHeight);

            Rectangle portBounds = new Rectangle(new Point(x, y),
                    new Dimension(portWidth, portHeight));

            fig.setBounds(portBounds);

        }
    }
}
