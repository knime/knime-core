/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
import org.knime.core.node.NodeOutPort;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.WorkflowManager;

import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.editparts.NodeInPortEditPart;
import org.knime.workbench.editor2.editparts.NodeOutPortEditPart;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;

/**
 * This factory creates the GEF <code>EditPart</code>s instances (the
 * controller objects) for given model objects.
 * 
 * @author Florian Georg, University of Konstanz
 */
public final class NewWorkflowEditPartFactory implements EditPartFactory {
    /**
     * Singleton instance for this factory.
     */
    public static final NewWorkflowEditPartFactory INSTANCE;
    static {
        INSTANCE = new NewWorkflowEditPartFactory();
    }

    /**
     * Private singleton constructor.
     * 
     */
    private NewWorkflowEditPartFactory() {
    }

    /**
     * 
     * @see org.eclipse.gef.EditPartFactory#
     *      createEditPart(org.eclipse.gef.EditPart, java.lang.Object)
     */
    public EditPart createEditPart(final EditPart context, final Object model) {
        EditPart part = null;

        if (model instanceof WorkflowManager) {
            //
            // WorkflowManager -> WorkflowRootEditPart
            //
            part = new WorkflowRootEditPart();
        } else if (model instanceof NodeContainer) {
            //
            // NodeContainer -> NodeContainerEditPart
            //
            part = new NodeContainerEditPart();
        } else if (model instanceof NodeInPort) {
            //
            // NodeInPort -> NodeInPortEditPart
            // (We'll need the ID to be able to associate to the correct port)
            part = new NodeInPortEditPart(((NodeInPort) model).getPortID());
        } else if (model instanceof NodeOutPort) {
            //
            // NodeOutPort -> NodeOutPortEditPart
            // (We'll need the ID to be able to associate to the correct port)
            part = new NodeOutPortEditPart(((NodeOutPort) model).getPortID());
        } else if (model instanceof ConnectionContainer) {
            //
            // ConnectionContainer -> ConnectionContainerEditPart
            //
            ConnectionContainer cc = (ConnectionContainer)model;
            int srcID = cc.getSourcePortID();
            boolean isModelPortConn = cc.getSource().isPredictorOutPort(srcID);
            part = new ConnectionContainerEditPart(isModelPortConn);
        } else {
            throw new IllegalArgumentException("unknown model obj: " + model);
        }

        // associate the model with the part (= the controller)
        part.setModel(model);

        return part;
    }
}
