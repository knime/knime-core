/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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

import org.eclipse.draw2d.FreeformListener;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.PortType;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowOutPort;
import org.knime.workbench.editor2.figures.WorkflowFigure;
import org.knime.workbench.editor2.figures.WorkflowOutPortFigure;

/**
 * Edit part for the {@link WorkflowOutPort}.
 * Model: {@link WorkflowOutPort}
 * View: {@link WorkflowOutPortFigure}
 * Controller: {@link WorkflowOutPortEditPart}
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class WorkflowOutPortEditPart extends AbstractPortEditPart 
    implements ControlListener {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            WorkflowOutPortEditPart.class);


    
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
        // if the referring WorkflowManager is displayed as a meta node, then  
        // the parent is a NodeContainerEditPart
        if (getParent() instanceof NodeContainerEditPart) {
            return (NodeContainer) getParent().getModel();
        }
        // if the referring WorkflowManager is the "root" workflow manager of 
        // the open editor then the parent is a WorkflowRootEditPart
        return ((WorkflowRootEditPart)getParent()).getWorkflowManager();
    }
    
    /**
     * We must register *every* port as a listener on the workflow, as we have
     * not real objects for it.
     *
     * @see org.eclipse.gef.EditPart#activate()
     */
    @Override
    public void activate() {
        super.activate();
        getRoot().getViewer().getControl().addControlListener(this);
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
        LOGGER.debug("create figure. Parent's figure: " 
                + ((GraphicalEditPart)getParent()).getFigure());
        ((WorkflowFigure)((GraphicalEditPart)getParent()).getFigure())
            .addFreeformListener(new FreeformListener() {

                public void notifyFreeformExtentChanged() {
                    LOGGER.debug("freeform extend changed: " + 
                            ((GraphicalEditPart)getParent()).getFigure()
                            .getBounds());
//                    ((WorkflowOutPortFigure)getFigure()).setParentSize(
//                            ((GraphicalEditPart)getParent()).getFigure()
//                            .getBounds());
//                    ((WorkflowOutPortFigure)getFigure()).releaseSizeFreeze();
                }
                
            });
        return new WorkflowOutPortFigure(getType(),
                getManager().getNrOutPorts(), getIndex(), 
                getManager().getName());
    }


    /**
     * This returns the (single !) connection that has this workflow out port 
     * as a target.
     *
     * @return singleton list containing the connection, or an empty list. Never
     *         <code>null</code>
     *
     * @see org.eclipse.gef.GraphicalEditPart#getTargetConnections()
     */
    @Override
    public List<ConnectionContainer> getModelTargetConnections() {
        ConnectionContainer container = getManager().getIncomingConnectionFor(
                getNodeContainer().getID(), getIndex());

        if (container != null) {
            return Collections.singletonList(container);
        }

        return EMPTY_LIST;
    }

    /**
     * @return empty list, as workflow out ports are never source for 
     * connections
     *
     * @see org.eclipse.gef.editparts.AbstractGraphicalEditPart
     *      #getModelSourceConnections()
     */
    @Override
    protected List<ConnectionContainer> getModelSourceConnections() {
        return EMPTY_LIST;
    }


    @Override
    public void zoomChanged(double zoom) {
        // TODO store the old size in order to restore it on 
        // zoom out
        // on zoom out restore size and on zoom in restore it
        LOGGER.debug("zoom changed: " + zoom);
        LOGGER.debug(getFigure().getBounds());
        super.zoomChanged(zoom);
    }
        
    /**
     * 
     * {@inheritDoc}
     */
    public void controlMoved(final ControlEvent e) {
    }


    /**
     * 
     * {@inheritDoc}
     */
    public void controlResized(final ControlEvent e) {
        Display.getCurrent().syncExec(new Runnable() {
            public void run() {
                ((WorkflowOutPortFigure)getFigure()).setParentSize(
                        new Rectangle(0, 0,
                                getRoot().getViewer().getControl().getSize().x, 
                                getRoot().getViewer().getControl()
                                .getSize().y));  
            }
        });
    }

}
