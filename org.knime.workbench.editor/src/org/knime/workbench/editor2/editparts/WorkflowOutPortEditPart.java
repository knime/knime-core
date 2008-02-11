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
package org.knime.workbench.editor2.editparts;

import java.util.Collections;
import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.knime.core.node.PortType;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.figures.WorkflowOutPortFigure;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class WorkflowOutPortEditPart extends AbstractPortEditPart {

//    private static final NodeLogger LOGGER = NodeLogger.getLogger(
//            WorkflowOutPortEditPart.class);

    /**
     * @param type port type
     * @param portID port id
     */
    public WorkflowOutPortEditPart(final PortType type, final int portID) {
        super(type, portID, false);
    }


    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void refreshVisuals() {
        IFigure fig = getFigure();
        fig.setBounds(new Rectangle(new Point(0, 50), new Dimension(30, 30)));
        super.refreshVisuals();
    }


    /**
     * Convenience, returns the hosting container.
     *
     * {@inheritDoc}
     */
    @Override
    protected final NodeContainer getNodeContainer() {
        if (getParent() == null) {
            return null;
        }
        return ((WorkflowRootEditPart)getParent()).getWorkflowManager();
    }

    /**
     * Convenience, returns the WFM.
     *
     * {@inheritDoc}
     */
    @Override
    protected final WorkflowManager getManager() {
        return (WorkflowManager)getNodeContainer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IFigure createFigure() {
        return new WorkflowOutPortFigure(getType(),
                getManager().getNrOutPorts(), getId(), getManager().getName());
    }


    /**
     * This returns the (single !) connection that has this in-port as a target.
     *
     * @return singleton list containing the connection, or an empty list. Never
     *         <code>null</code>
     *
     * @see org.eclipse.gef.GraphicalEditPart#getTargetConnections()
     */
    @Override
    public List getModelTargetConnections() {
        ConnectionContainer container = getManager().getIncomingConnectionFor(
                getNodeContainer().getID(), getId());

        if (container != null) {
            return Collections.singletonList(container);
        }

        return Collections.EMPTY_LIST;
    }

    /**
     * @return empty list, as in-ports are never source for connections
     *
     * @see org.eclipse.gef.editparts.AbstractGraphicalEditPart
     *      #getModelSourceConnections()
     */
    @Override
    protected List getModelSourceConnections() {
        return Collections.EMPTY_LIST;
    }

}
