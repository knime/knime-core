/*
 * ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   28.04.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.editparts;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.draw2d.IFigure;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NodeOutPort;
import org.knime.core.node.workflow.NodeStateChangeListener;
import org.knime.core.node.workflow.NodeStateEvent;
import org.knime.core.node.workflow.WorkflowOutPort;
import org.knime.workbench.editor2.figures.SubWorkFlowOutPortFigure;
import org.knime.workbench.ui.SyncExecQueueDispatcher;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class SubWorkFlowOutPortEditPart extends AbstractPortEditPart 
    implements NodeStateChangeListener {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            SubWorkFlowOutPortEditPart.class);
    
    /**
     * @param type type of port (data, db, model)
     * @param portIndex index of the port
     */
    public SubWorkFlowOutPortEditPart(final PortType type, 
            final int portIndex) {
        super(type, portIndex, false);
        LOGGER.debug("created sub workflow out port edit part with type "
                + type + " and index " + portIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IFigure createFigure() {
        LOGGER.debug("returning new sub work flow out port figure "
                + " with type " + getType() + " index " + getIndex()
                + " nr outports " + getNodeContainer().getNrOutPorts()
                + " and tooltip " + getNodeContainer().getOutPort(getIndex())
                .getPortName());
        WorkflowOutPort model = (WorkflowOutPort)getModel();
        LOGGER.debug("model: " + getModel() 
                + " state: " + model.getNodeState());

        NodeOutPort port = getNodeContainer().getOutPort(getIndex());
        String tooltip = getTooltipText(port.getPortName(), port);
        
        SubWorkFlowOutPortFigure f = new SubWorkFlowOutPortFigure(
                getType(), getIndex(), 
                getNodeContainer().getNrOutPorts(), 
                tooltip, model.getNodeState());
        model.addNodeStateChangeListener(this);
        return f;
    }
    
    /**
     * This returns the (single !) connection that has this in-port as a target.
     *
     * @return singleton list containing the connection, or an empty list. Never
     *         <code>null</code>
     *
     * {@inheritDoc}
     */
    @Override
    public List<ConnectionContainer> getModelSourceConnections() {
        if (getManager() == null) {
            return EMPTY_LIST;
        }
        Set<ConnectionContainer> containers =
                getManager().getOutgoingConnectionsFor(
                        getNodeContainer().getID(),
                        getIndex());
        List<ConnectionContainer>conns = new ArrayList<ConnectionContainer>();
        if (containers != null) {
            conns.addAll(containers);
        }
        return conns;
    }

    /**
     * 
     * @return empty list, as out-ports are never target for connections
     * 
     * {@inheritDoc}
     */
    @Override
    protected List<ConnectionContainer> getModelTargetConnections() {
        return EMPTY_LIST;
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void removeNotify() {
        super.removeNotify();
        // TODO remove the figure from the model
        ((WorkflowOutPort)getModel()).removeNodeStateChangeListener(this);
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void stateChanged(final NodeStateEvent state) {
        SyncExecQueueDispatcher.asyncExec(new Runnable() {
            @Override
            public void run() {
                ((SubWorkFlowOutPortFigure)getFigure()).setState(
                        state.getState());
                rebuildTooltip();
                getFigure().repaint();
            }
        });
    }

}
