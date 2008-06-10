/*
 * -------------------------------------------------------------------
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
 *   25.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;
import org.eclipse.gef.GraphicalViewer;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeInPort;
import org.knime.core.node.workflow.NodeOutPort;
import org.knime.core.node.workflow.NodePort;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.WorkflowInPort;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowOutPort;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.editparts.NodeInPortEditPart;
import org.knime.workbench.editor2.editparts.NodeOutPortEditPart;
import org.knime.workbench.editor2.editparts.SubWorkFlowOutPortEditPart;
import org.knime.workbench.editor2.editparts.SubworkflowEditPart;
import org.knime.workbench.editor2.editparts.WorkflowInPortBarEditPart;
import org.knime.workbench.editor2.editparts.WorkflowInPortEditPart;
import org.knime.workbench.editor2.editparts.WorkflowOutPortBarEditPart;
import org.knime.workbench.editor2.editparts.WorkflowOutPortEditPart;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;
import org.knime.workbench.editor2.model.WorkflowPortBar;

/**
 * This factory creates the GEF <code>EditPart</code>s instances (the
 * controller objects) for given model objects.
 * 
 * @author Florian Georg, University of Konstanz
 * @author Fabian Dill, University of Konstanz
 */
public final class WorkflowEditPartFactory implements EditPartFactory {

//    private static final NodeLogger LOGGER =
//            NodeLogger.getLogger(WorkflowEditPartFactory.class);

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
    public WorkflowEditPartFactory() {
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
     *  <li>{@link WorkflowInPort}: {@link WorkflowInPortEditPart}</li>
     *  <li>{@link WorkflowOutPort}: {@link WorkflowOutPortEditPart}</li>
     * </ul>
     * 
     * The {@link WorkflowRootEditPart} has its {@link NodeContainer}s and its
     * {@link WorkflowInPort}s and {@link WorkflowOutPort}s as model children. 
     * The {@link NodeContainerEditPart} has its {@link NodePort}s as its 
     * children. 
     * 
     * @see WorkflowRootEditPart#getModelChildren()
     * @see NodeContainerEditPart#getModelChildren()
     * 
     * @throws IllegalArgumentException if any other object is passed
     * 
     * {@inheritDoc}
     */
    public EditPart createEditPart(final EditPart context, final Object model) {
        // instantiated here
        // correct type in the if statement
        // model at the end of method
        EditPart part = null;
        if (model instanceof WorkflowManager) {
            // this is out "root" workflow manager
            if (m_isTop) {
                // all following objects of type WorkflowManager are treated as
                // meta nodes and displayed as NodeContainers
                m_isTop = false;
                part = new WorkflowRootEditPart();
            } else {
                // we already have a "root" workflow manager
                // must be a meta node
//                part = new NodeContainerEditPart();
                part = new SubworkflowEditPart();
            }
        } else if (model instanceof WorkflowPortBar) {
            WorkflowPortBar bar = (WorkflowPortBar)model;
            if (bar.isInPortBar()) {
                part = new WorkflowInPortBarEditPart();
            } else {
                part = new WorkflowOutPortBarEditPart();
            }
        } else if (model instanceof SingleNodeContainer) {
            // SingleNodeContainer -> NodeContainerEditPart
            part = new NodeContainerEditPart();
            
            // we have to test for WorkflowInPort first because it's a 
            // subclass of NodeInPort (same holds for WorkflowOutPort and 
            // NodeOutPort) 
        } else if (model instanceof WorkflowInPort 
                && context instanceof WorkflowInPortBarEditPart) {
            // WorkflowInPort and context WorkflowRootEditPart -> 
            // WorkflowInPortEditPart
            /*
             * if the context is a WorkflowRootEditPart it indicates that the 
             * WorkflowInPort is a model child of the WorkflowRootEditPart, i.e.
             * we look at it as a workflow in port. If the context is a 
             * NodeContainerEditPart the WorkflowInPort is a model child of a
             * NodeContainerEditPart and we look at it as a node in port. 
             */
            WorkflowInPort inport = (WorkflowInPort)model;
            part =
                new WorkflowInPortEditPart(inport.getPortType(),
                        inport.getPortIndex());
        } else if (model instanceof WorkflowOutPort
                && context instanceof WorkflowOutPortBarEditPart) {
            // WorkflowOutPort and context WorkflowRootEditPart -> 
            // WorkflowOutPortEditPart
            /*
             * if the context is a WorkflowRootEditPart it indicates that the 
             * WorkflowOutPort is a model child of the WorkflowRootEditPart, 
             * i.e. we look at it as a workflow out port. If the context is a 
             * NodeContainerEditPart the WorkflowOutPort is a model child of a
             * NodeContainerEditPart and we look at it as a node out port. 
             */
            
         // TODO: return SubWorkFlowOutPortEditPart
            WorkflowOutPort outport = (WorkflowOutPort)model;
                
             part = new WorkflowOutPortEditPart(
                        outport.getPortType(), 
                        outport.getPortIndex());
        } else if (model instanceof WorkflowOutPort) {
         // TODO: return SubWorkFlowOutPortEditPart
            WorkflowOutPort outport = (WorkflowOutPort)model;
            part = new SubWorkFlowOutPortEditPart(
                    outport.getPortType(),
                    outport.getPortIndex());
            
        } else if (model instanceof NodeInPort) {
            // NodeInPort -> NodeInPortEditPart
            NodePort port = (NodeInPort)model;
            part = new NodeInPortEditPart(port.getPortType(), 
                    port.getPortIndex());
        } else if (model instanceof NodeOutPort) {
            // NodeOutPort -> NodeOutPortEditPart
            NodePort port = (NodeOutPort)model;
            part = new NodeOutPortEditPart(port.getPortType(), 
                    port.getPortIndex());
        } else if (model instanceof ConnectionContainer) {
            // ConnectionContainer -> ConnectionContainerEditPart
            part = new ConnectionContainerEditPart();
        } else {
            throw new IllegalArgumentException("unknown model obj: " + model);
        }
        // associate the model with the part (= the controller)
        part.setModel(model);
        return part;
    }
}
