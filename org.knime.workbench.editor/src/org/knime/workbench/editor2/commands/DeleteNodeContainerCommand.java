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
 *   09.06.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.commands;

import org.eclipse.gef.commands.Command;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.WorkflowManagerInput;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.figures.NodeContainerFigure;

/**
 * This is the command to delete <code>NodeContainer</code>s from the
 * workflow.
 *
 * @author Florian Georg, University of Konstanz
 */
public class DeleteNodeContainerCommand extends Command {
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(DeleteNodeContainerCommand.class);

    private final NodeContainerEditPart m_part;

    private final WorkflowManager m_manager;

    /**
     * Creates a new delete command for a <code>NodeContainer</code>.
     *
     * @param nodePart The container edit part to delete
     * @param manager The manager hosting the container
     */
    public DeleteNodeContainerCommand(final NodeContainerEditPart nodePart,
            final WorkflowManager manager) {
        m_part = nodePart;
        m_manager = manager;
    }

    /**
     * If the edit part is locked (= busy), we can't delete the underlying node.
     *
     * {@inheritDoc}
     */
    @Override
    public boolean canExecute() {

        // is the node locked
        boolean isNotLocked = !m_part.isLocked();

        // is the node a deletable node
        // does the workflow status allow deletion of the selected node
        // only if the workflow is not executing
        boolean workflowAllowsDeletion =
                m_manager.canRemoveNode(m_part.getNodeContainer().getID());
        return isNotLocked && workflowAllowsDeletion;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        LOGGER.debug("Deleting node #" + m_part.getNodeContainer().getID()
                + " from Workflow");

        // The WFM removes all connections for us, before the node is
        // removed.
        try {
            m_manager.removeNode(m_part.getNodeContainer().getID());            
            if (m_part.getNodeContainer() instanceof WorkflowManager) {
                WorkflowManagerInput in = new WorkflowManagerInput(
                        (WorkflowManager)m_part.getNodeContainer(),
                        // since the equals method of the WorkflowManagerInput
                        // only looks for the WorkflowManager, we can pass null
                        // as the editor argument 
                        null);
                IEditorPart editor = PlatformUI.getWorkbench()
                    .getActiveWorkbenchWindow().getActivePage().findEditor(in);
                if (editor != null) {
                    editor.getEditorSite().getPage().closeEditor(editor, false);
                }
            }
            
        } catch (Exception ex) {
            LOGGER.warn("Operation not allowed.", ex);
            Display.getDefault().asyncExec(new Runnable() {

                public void run() {                    
                    MessageBox mb =
                        new MessageBox(Display.getDefault().getActiveShell(),
                                SWT.ICON_INFORMATION | SWT.OK);
                    mb.setText("Operation not allowed");
                    mb.setMessage("You cannot remove this node");
                    mb.open();
                    if (m_part.getFigure() instanceof NodeContainerFigure) {
                        ((NodeContainerFigure)m_part.getFigure()).unmark();
                    }
                }
                
            });
        }
    }

    /**
     * TODO FIXME: no undo by now, as the connections can't be restored and the
     * node gets a new ID.
     *
     * {@inheritDoc}
     */
    @Override
    public boolean canUndo() {
        return false;
    }
}
