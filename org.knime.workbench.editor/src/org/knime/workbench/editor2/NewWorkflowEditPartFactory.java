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
import org.eclipse.gef.GraphicalViewer;
import org.knime.core.node.NodeInPort;
import org.knime.core.node.NodeOutPort;
import org.knime.core.node.NodePort;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.SingleNodeContainer;
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
 * @author Fabian Dill, University of Konstanz
 */
public final class NewWorkflowEditPartFactory implements EditPartFactory {

//    private static final NodeLogger LOGGER =
//            NodeLogger.getLogger(NewWorkflowEditPartFactory.class);

    /* 
     * we need this flag to determine between the "root" workflow manager and
     * all subsequent meta nodes. This means that we implicitely assume that
     * the "root" workflow manager is the first object to come of type
     * WorkflowManager. This assumption is correct, since the top model is 
     * passed to the factory and subsequently is asked for its model children.
     * 
     * @see WorkflowGraphicalViewerCreator#createViewer
     * @see AbstractGraphicalViewer#sertEditPartFactory
     * @see WorkflowRootEditPart#getModelChildren
     */
    private boolean m_isTop = true;

    /**
     * An instance per {@link GraphicalViewer} is necessary, in order to 
     * distinguish between the workflow manager of the editor and contained 
     * subworkflows.  
     * 
     */
    public NewWorkflowEditPartFactory() {
    }

    /**
     * Creates the referring edit parts for the following parts of the model.
     * <ul>
     *  <li>{@link WorkflowManager}: either {@link WorkflowRootEditPart} or 
     *  {@link NodeContainerEditPart} (depending on the currently displayed 
     *  level)</li>
     *  <li>{@link SingleNodeContainer}: {@link NodeContainerEditPart}</li>
     *  <li>{@link NodeInPort}: {@link NodeInPortEditPart}</li>
     *  <li>{@link NodeOutPort}: {@link NodeOutPortEditPart}</li>
     *  <li>{@link ConnectionContainer}: {@link ConnectionContainerEditPart}
     *  </li>
     *  <li>{@link WorkflowInPortProxy}: {@link WorkflowInPortEditPart}</li>
     *  <li>{@link WorkflowOutPortProxy}: {@link WorkflowOutPortEditPart}</li>
     * </ul>
     * 
     * @see WorkflowRootEditPart#getModelChildren()
     * 
     * @throws IllegalArgumentException if any other object is passed
     * 
     * {@inheritDoc}
     */
    public EditPart createEditPart(final EditPart context, final Object model) {
        EditPart part = null;
        if (model instanceof WorkflowManager) {
            if (m_isTop) {
                m_isTop = false;
                part = new WorkflowRootEditPart();
            } else {
                part = new NodeContainerEditPart();
            }
        } else if (model instanceof SingleNodeContainer) {
            part = new NodeContainerEditPart();
        } else if (model instanceof NodeInPort) {
            NodePort port = (NodeInPort)model;
            part = new NodeInPortEditPart(port.getPortType(), port.getPortID());
        } else if (model instanceof NodeOutPort) {
            //
            // NodeOutPort -> NodeOutPortEditPart
            NodePort port = (NodeOutPort)model;
            part =
                    new NodeOutPortEditPart(port.getPortType(), port
                            .getPortID());
        } else if (model instanceof ConnectionContainer) {
            //
            // ConnectionContainer -> ConnectionContainerEditPart
            //
            part = new ConnectionContainerEditPart();
        } else if (model instanceof WorkflowInPortProxy) {
            WorkflowInPortProxy inport = (WorkflowInPortProxy)model;
            part =
                    new WorkflowInPortEditPart(inport.getPort().getPortType(),
                            inport.getPort().getPortID());
        } else if (model instanceof WorkflowOutPortProxy) {
            WorkflowOutPortProxy outport = (WorkflowOutPortProxy)model;
            part =
                    new WorkflowOutPortEditPart(
                            outport.getPort().getPortType(), 
                            outport.getPort().getPortID());
        } else {
            throw new IllegalArgumentException("unknown model obj: " + model);
        }
        // associate the model with the part (= the controller)
        part.setModel(model);
        return part;
    }
}
