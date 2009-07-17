/*
 * ------------------------------------------------------------------ *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 *   21.02.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.commands;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.workbench.editor2.editparts.AbstractWorkflowPortBarEditPart;
import org.knime.workbench.editor2.model.WorkflowPortBar;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class ChangeWorkflowPortBarCommand extends Command {
    
    private final Rectangle m_oldBounds;
    private final Rectangle m_newBounds;

    private final AbstractWorkflowPortBarEditPart m_bar;

    /**
     *
     * @param portBar The workflow port bar to change
     * @param newBounds The new bounds
     */
    public ChangeWorkflowPortBarCommand(
            final AbstractWorkflowPortBarEditPart portBar,
            final Rectangle newBounds) {
        WorkflowPortBar barModel = (WorkflowPortBar)portBar.getModel();
        NodeUIInformation uiInfo = barModel.getUIInfo();
        if (uiInfo != null) {
            int[] bounds = uiInfo.getBounds();
            m_oldBounds = new Rectangle(
                    bounds[0], bounds[1], bounds[2], bounds[3]);
        } else {
            // right info type
            m_oldBounds = new Rectangle(portBar.getFigure().getBounds());
        }
        m_newBounds = newBounds;
        m_bar = portBar;
    }
        
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public boolean canExecute() {
        Dimension min = m_bar.getFigure().getMinimumSize();
        if (m_newBounds.width < min.width 
                || m_newBounds.height < min.height) {
            return false;
        }
        return super.canExecute();
    }

    /**
     * Sets the new bounds.
     *
     * @see org.eclipse.gef.commands.Command#execute()
     */
    @Override
    public void execute() {
        WorkflowPortBar barModel = (WorkflowPortBar)m_bar.getModel();
        NodeUIInformation uiInfo = new NodeUIInformation(
                m_newBounds.x, m_newBounds.y,
                m_newBounds.width, m_newBounds.height, true);
        // must set explicitly so that event is fired by container
        barModel.setUIInfo(uiInfo);
        m_bar.getFigure().setBounds(m_newBounds);
        m_bar.getFigure().getLayoutManager().layout(m_bar.getFigure());
    }

    /**
     * Sets the old bounds.
     *
     * @see org.eclipse.gef.commands.Command#execute()
     */
    @Override
    public void undo() {
        WorkflowPortBar barModel = (WorkflowPortBar)m_bar.getModel();
        NodeUIInformation uiInfo = new NodeUIInformation(
                m_oldBounds.x, m_oldBounds.y,
                m_oldBounds.width, m_oldBounds.height, true);
        // must set explicitly so that event is fired by container
        barModel.setUIInfo(uiInfo);
        m_bar.getFigure().setBounds(m_oldBounds);
        m_bar.getFigure().getLayoutManager().layout(m_bar.getFigure());
    }

}
