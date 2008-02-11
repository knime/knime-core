/* -------------------------------------------------------------------
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
 *   22.01.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.figures;

import org.eclipse.draw2d.IFigure;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.ui.PlatformUI;
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
        LOGGER.debug("workflow port locator#relocate ");
        Rectangle bounds = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
            .getActivePage().getActiveEditor().getEditorSite().
            getShell().getBounds();
        LOGGER.debug("editor bounds: "  + bounds);
        int offset = bounds.height / getNrPorts();
        int y = getPortIndex() * offset;
        int x;
        if (isInPort()) {
            x = 0;
        } else {
            x = bounds.width;
        }
        target.setBounds(
                new org.eclipse.draw2d.geometry.Rectangle(x, y, 20, 20));
    }

}
