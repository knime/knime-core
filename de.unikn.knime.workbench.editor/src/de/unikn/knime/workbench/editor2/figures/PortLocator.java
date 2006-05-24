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

    private boolean m_modelPort;

    private int m_maxModelPorts;

    private int m_maxDataPorts;

    private int m_portIndex;

    /**
     * Creates a new locator.
     * 
     * @param parent The parent figure (NodeContainerFigure)
     * @param type The type
     * @param maxModelPorts max number of model ports to locate
     * @param maxDataPorts max number of data ports to locate
     * @param portIndex The port index
     * @param modelPort whether this is a model port locator
     */
    public PortLocator(final NodeContainerFigure parent, final int type,
            final int maxModelPorts, final int maxDataPorts,
            final int portIndex, final boolean modelPort) {
        m_parent = parent;
        m_type = type;
        m_maxModelPorts = maxModelPorts;
        m_maxDataPorts = maxDataPorts;
        m_portIndex = portIndex;
        m_modelPort = modelPort;
    }

    /**
     * @see org.eclipse.draw2d.Locator #relocate(org.eclipse.draw2d.IFigure)
     */
    public void relocate(final IFigure fig) {
        Rectangle parentBounds = m_parent.getContentFigure().getBounds()
                .getCopy();

        int portHeight = parentBounds.height
                / (m_maxDataPorts + m_maxModelPorts);
        int portWidth = parentBounds.width / 2;

        int x = 0;
        int y = 0;
        if (m_type == TYPE_INPORT) {

            x = parentBounds.getLeft().x;

            int position = 0;
            if (m_modelPort) {
                position = m_portIndex - (m_maxModelPorts);
            } else {
                // data port
                position = m_portIndex + (m_maxModelPorts);
            }

            y = parentBounds.getTopLeft().y + (position * portHeight);

        } else {

            x = parentBounds.getCenter().x;

            y = parentBounds.getTopRight().y + (m_portIndex * portHeight);
        }

        Rectangle portBounds = new Rectangle(new Point(x, y), new Dimension(
                portWidth, portHeight));

        fig.setBounds(portBounds);
    }
}
