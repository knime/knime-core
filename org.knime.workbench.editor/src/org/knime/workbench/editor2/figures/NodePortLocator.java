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
 *   Aug 8, 2005 (georg): created
 */
package org.knime.workbench.editor2.figures;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.knime.core.node.port.PortType;

/**
 * Locator for port figures. Makes sure that the ports are "near" the centered
 * icon of the surrounding <code>NodeContainerFigure</code>
 *
 * @author Florian Georg, University of Konstanz
 */
public class NodePortLocator extends PortLocator {

    private final NodeContainerFigure m_parent;


//    private static final NodeLogger LOGGER = NodeLogger.getLogger(
//            NodePortLocator.class);

    /**
     * Creates a new locator.
     *
     * @param parent The parent figure (NodeContainerFigure)
     * @param isInport true if it is an in port, false if it's an out port
     * @param maxPorts max number of data ports to locate
     * @param portIndex The port index
     * @param portType type of the port
     */
    public NodePortLocator(final NodeContainerFigure parent,
            final boolean isInport, final int maxPorts, final int portIndex,
            final PortType portType) {
        super(portType, portIndex, isInport, maxPorts);
        m_parent = parent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void relocate(final IFigure fig) {
        Rectangle parentBounds = m_parent.getContentFigure().getBounds()
                .getCopy();
        int portHeight = (parentBounds.height - 10)
                / (getNrPorts());
        int portWidth = parentBounds.width / 2;
        int x = 0;
        int y = 0;
        if (isInPort()) {
            x = parentBounds.getLeft().x - 1;
            int position = getPortIndex();
            y = parentBounds.getTopLeft().y  + 5 + (position * portHeight);
        } else {
            x = parentBounds.getCenter().x + 4;
            y = parentBounds.getTopRight().y + 5
                + (getPortIndex() * portHeight);
        }
        Rectangle portBounds = new Rectangle(new Point(x, y), new Dimension(
                portWidth, portHeight));

        fig.setBounds(portBounds);
    }
}
