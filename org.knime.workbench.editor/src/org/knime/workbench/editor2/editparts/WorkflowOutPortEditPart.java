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
package org.knime.workbench.editor2.editparts;

import java.util.Collections;
import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.GraphicalEditPart;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowOutPort;
import org.knime.workbench.editor2.figures.WorkflowOutPortFigure;
import org.knime.workbench.editor2.model.WorkflowPortBar;

/**
 * Edit part for the {@link WorkflowOutPort}.
 * Model: {@link WorkflowOutPort}
 * View: {@link WorkflowOutPortFigure}
 * Controller: {@link WorkflowOutPortEditPart}
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class WorkflowOutPortEditPart extends AbstractPortEditPart {

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
        return ((WorkflowPortBar)getParent().getModel()).getWorkflowManager();
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
        return new WorkflowOutPortFigure(getType(),
                getManager().getNrOutPorts(), getIndex(), 
                getManager().getDisplayLabel());
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
        if (getManager() == null) {
            return EMPTY_LIST;
        }
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

}
