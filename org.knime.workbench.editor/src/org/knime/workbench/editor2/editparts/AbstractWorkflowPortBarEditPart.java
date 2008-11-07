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
 *   20.02.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.editparts;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.workbench.editor2.WorkflowSelectionDragEditPartsTracker;
import org.knime.workbench.editor2.editparts.policy.PortGraphicalRoleEditPolicy;
import org.knime.workbench.editor2.figures.AbstractWorkflowPortBarFigure;
import org.knime.workbench.editor2.model.WorkflowPortBar;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public abstract class AbstractWorkflowPortBarEditPart 
    extends AbstractWorkflowEditPart implements ConnectableEditPart {
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected void refreshVisuals() {
        NodeUIInformation uiInfo = ((WorkflowPortBar)getModel())
            .getUIInfo();
        if (uiInfo != null && uiInfo.isFilledProperly()
                && !((AbstractWorkflowPortBarFigure)getFigure())
                .isInitialized()) {
            int[] bounds = uiInfo.getBounds();
            ((AbstractWorkflowPortBarFigure)getFigure()).setBounds(
                    new Rectangle(bounds[0], bounds[1], bounds[2], bounds[3]));
        }
        super.refreshVisuals();
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void createEditPolicies() {

        // This policy provides create/reconnect commands for connections that
        // are associated with ports of this node
        this.installEditPolicy(EditPolicy.GRAPHICAL_NODE_ROLE,
                new PortGraphicalRoleEditPolicy());
        
    }
    
    /**
     * Overridden to return a custom <code>DragTracker</code> for
     * NodeContainerEditParts.
     *
     * @see org.eclipse.gef.EditPart#getDragTracker(Request)
     */
    @Override
    public DragTracker getDragTracker(final Request request) {
        return new WorkflowSelectionDragEditPartsTracker(this);
    }

    
    /**
     * 
     * {@inheritDoc}
     */
    public NodeContainer getNodeContainer() {
        return ((WorkflowPortBar)getModel()).getWorkflowManager();
    }

}
