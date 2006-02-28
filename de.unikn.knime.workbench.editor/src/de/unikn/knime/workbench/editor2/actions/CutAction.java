/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
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
 *   20.02.2006 (sieb): created
 */
package de.unikn.knime.workbench.editor2.actions;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;

import de.unikn.knime.core.node.NodeLogger;
import de.unikn.knime.core.node.workflow.WorkflowManager;
import de.unikn.knime.workbench.editor2.WorkflowEditor;
import de.unikn.knime.workbench.editor2.commands.DeleteNodeContainerCommand;
import de.unikn.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * Implements the clipboard cut action to copy nodes and connections into the
 * clipboard and additionally delete them from the workflow.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class CutAction extends AbstractClipboardAction {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(ExecuteAction.class);

    /**
     * Constructs a new clipboard copy action.
     * 
     * @param editor the workflow editor this action is intended for
     */
    public CutAction(final WorkflowEditor editor) {

        super(editor);
    }

    /**
     * @see org.eclipse.jface.action.IAction#getId()
     */
    @Override
    public String getId() {

        return ActionFactory.CUT.getId();
    }
    
    /**
     * @see org.eclipse.jface.action.IAction#getImageDescriptor()
     */
    public ImageDescriptor getImageDescriptor() {

        ISharedImages sharedImages = PlatformUI.getWorkbench()
                .getSharedImages();
        return sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_CUT);
    }
    
    /**
     * @see org.eclipse.jface.action.IAction#getText()
     */
    public String getText() {
        return "Cut";
    }

    /**
     * At least one node must be selected.
     * 
     * @see org.eclipse.gef.ui.actions.WorkbenchPartAction#calculateEnabled()
     */
    protected boolean calculateEnabled() {
        NodeContainerEditPart[] parts = getSelectedNodeParts();

        return parts.length > 0;
    }

    /**
     * Invokes the copy action followed by the delete command.
     * 
     * @see de.unikn.knime.workbench.editor2.actions.AbstractNodeAction#runOnNodes(de.unikn.knime.workbench.editor2.editparts.NodeContainerEditPart[])
     */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {

        LOGGER.debug("Clipboard cut action invoked...");

        // invoke copy action
        CopyAction copy = new CopyAction(getEditor());
        copy.runOnNodes(nodeParts);

        // delete the nodes
        WorkflowManager manager = getEditor().getWorkflowManager();

        for (NodeContainerEditPart nodePart : nodeParts) {

            // create a delete command
            DeleteNodeContainerCommand delete = new DeleteNodeContainerCommand(
                    nodePart, manager);

            // if not locked
            if (delete.canExecute()) {
                
                delete.execute();
            }
        }

        // update the actions
        getEditor().updateActions();

        // Give focus to the editor again. Otherwise the actions (selection)
        // is not updated correctly.
        getWorkbenchPart().getSite().getPage().activate(getWorkbenchPart());
    }
}
