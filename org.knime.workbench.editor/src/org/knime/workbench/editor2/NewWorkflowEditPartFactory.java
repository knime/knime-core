/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *   25.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;
import org.knime.core.node.NodeInPort;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeOutPort;
import org.knime.core.node.NodePort;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.WorkflowEvent;
import org.knime.core.node.workflow.WorkflowListener;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.editparts.NodeInPortEditPart;
import org.knime.workbench.editor2.editparts.NodeOutPortEditPart;
import org.knime.workbench.editor2.editparts.WorkflowInPortEditPart;
import org.knime.workbench.editor2.editparts.WorkflowOutPortEditPart;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;
import org.knime.workbench.editor2.model.WorkflowInPortProxy;
import org.knime.workbench.editor2.model.WorkflowOutPortProxy;

/**
 * This factory creates the GEF <code>EditPart</code>s instances (the
 * controller objects) for given model objects.
 *
 * @author Florian Georg, University of Konstanz
 */
public final class NewWorkflowEditPartFactory implements EditPartFactory,
    WorkflowListener {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            NewWorkflowEditPartFactory.class);

    private NodeID m_nodeID;

    /**
     * Private singleton constructor.
     *
     */
    public  NewWorkflowEditPartFactory() {
    }

    public void workflowChanged(final WorkflowEvent event) {
        if (event.getType().equals(WorkflowEvent.Type.NODE_REMOVED)) {
            m_nodeID = event.getID();
        }
    }


    /**
     * {@inheritDoc}
     */
    public EditPart createEditPart(final EditPart context, final Object model) {
        EditPart part = null;
        // TODO: what if workflow manager : meta node?
        // could the context be used?
//        LOGGER.debug("creating edit part for: " + model);
        if (model instanceof WorkflowManager) {
            WorkflowManager wfm = (WorkflowManager)model;
            if (m_nodeID == null) {
//                LOGGER.debug("setting node id: " + wfm.getID());
                m_nodeID = wfm.getID();
                wfm.addListener(this);
            }
//                LOGGER.debug("new : " + wfm.getID() + " old: " + m_nodeID);
            if (isMetaNode(wfm.getID())) {
                m_nodeID = wfm.getID();
//                    LOGGER.debug("detected meta node, "
//                            + "return node container edit part");
                part = new NodeContainerEditPart();
                part.setModel(model);
                return part;
            }
            //
            // WorkflowManager -> WorkflowRootEditPart
            //
//            LOGGER.debug("workflow root edit part");
            part = new WorkflowRootEditPart();
        } else if (model instanceof SingleNodeContainer) {
            //
            // NodeContainer -> NodeContainerEditPart
            //
//            LOGGER.debug("node container edit part");
            part = new NodeContainerEditPart();
        } else if (model instanceof NodeInPort) {
            //
            // NodeInPort -> NodeInPortEditPart
            // (We'll need the ID to be able to associate to the correct port)
            NodePort port = (NodeInPort) model;
            part = new NodeInPortEditPart(port.getPortType(), port.getPortID());
        } else if (model instanceof NodeOutPort) {
            //
            // NodeOutPort -> NodeOutPortEditPart
            // (We'll need the ID to be able to associate to the correct port)
            NodePort port = (NodeOutPort) model;
            part = new NodeOutPortEditPart(port.getPortType(),
                    port.getPortID());
        } else if (model instanceof ConnectionContainer) {
            //
            // ConnectionContainer -> ConnectionContainerEditPart
            //
            LOGGER.debug("created connection for : " + model);
            ConnectionContainer conn = (ConnectionContainer)model;
            LOGGER.debug("source: " + conn.getSource() + " target: "
                    + conn.getDest());
            part = new ConnectionContainerEditPart();
        } else if (model instanceof WorkflowInPortProxy) {
            WorkflowInPortProxy inport = (WorkflowInPortProxy)model;
            if (isMetaNode(inport.getID())) {
//                LOGGER.debug("must return node in port for node: "
//                        + inport.getID());
                part = new NodeInPortEditPart(inport.getPort().getPortType(),
                        inport.getPort().getPortID());
                part.setModel(inport.getPort());
                return part;
            } else {
//                LOGGER.debug("must return workflow in port for node: "
//                        + inport.getID());
                // TODO replace with workflow out port
                part = new WorkflowInPortEditPart(
                        inport.getPort().getPortType(),
                        inport.getPort().getPortID());
//                LOGGER.debug("setting model : " + inport.getPort());
            }
        } else if (model instanceof WorkflowOutPortProxy) {
            WorkflowOutPortProxy outport = (WorkflowOutPortProxy)model;
            if (isMetaNode(outport.getID())) {
//                LOGGER.debug("must return node out port for node: "
//                        + outport.getID());
                part = new NodeOutPortEditPart(outport.getPort().getPortType(),
                        outport.getPort().getPortID());
                part.setModel(outport.getPort());
                return part;
            } else {
//                LOGGER.debug("must return workflow out port for node: "
//                        + outport.getID());
//                LOGGER.debug("setting model for: "  + outport.getID() + " "
//                        + outport.getPort());
                part = new WorkflowOutPortEditPart(outport.getPort()
                        .getPortType(), outport.getPort().getPortID());
            }
        } else {
            throw new IllegalArgumentException("unknown model obj: " + model);
        }

        // associate the model with the part (= the controller)
        if (part.getModel() == null) {
            part.setModel(model);
        }

        return part;
    }

    private boolean isMetaNode(final NodeID id) {
        return id.compareTo(m_nodeID) > 0;
    }



}
