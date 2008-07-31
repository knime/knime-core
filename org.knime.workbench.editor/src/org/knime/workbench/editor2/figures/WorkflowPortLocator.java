/*
 * ------------------------------------------------------------------ *
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
 *   22.01.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.figures;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.PortType;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class WorkflowPortLocator extends PortLocator {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            WorkflowPortLocator.class);

    /**
     * @param type port type
     * @param portIndex port index
     * @param isInPort true if in port, false if out port
     * @param nrPorts total number of ports
     */
    public WorkflowPortLocator(final PortType type, final int portIndex,
            final boolean isInPort, final int nrPorts) {
        super(type, portIndex, isInPort, nrPorts);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void relocate(final IFigure target) {
        Rectangle parent = target.getParent().getBounds().getCopy();

        int xPos = parent.x;
        if (isInPort()) {
            xPos += parent.width - AbstractPortFigure.WF_PORT_SIZE;
        }
        int yPos = (int)(parent.y + (((double)parent.height 
                / (double) (getNrPorts() + 1)) * (getPortIndex() + 1)));
        yPos -= (AbstractPortFigure.WF_PORT_SIZE / 2);
        Rectangle portBounds = new Rectangle(new Point(xPos, yPos), 
                new Dimension(
                        AbstractPortFigure.WF_PORT_SIZE, 
                        AbstractPortFigure.WF_PORT_SIZE));
        
        LOGGER.debug("workflow port locator#relocate " + portBounds);
        target.setBounds(portBounds);
        target.repaint();
    }

}
