/* @(#)$RCSfile$ 
 * $Revision: 280 $ $Date: 2006-02-21 17:39:37 +0100 (Di, 21 Feb 2006) $ $Author: sieb $
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
 *   25.05.2005 (Florian Georg): created
 */
package de.unikn.knime.workbench.editor2.actions;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.internal.Workbench;

import de.unikn.knime.core.node.NodeLogger;
import de.unikn.knime.core.node.workflow.NodeContainer;
import de.unikn.knime.workbench.editor2.ImageRepository;
import de.unikn.knime.workbench.editor2.WorkflowEditor;
import de.unikn.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * Action to set the user name and description of a node.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class SetNameAndDescriptionAction extends AbstractNodeAction {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(SetNameAndDescriptionAction.class);

    /**
     * unique ID for this action.
     */
    public static final String ID = "knime.action.setnameanddescription";

    /**
     * @param editor The workflow editor
     */
    public SetNameAndDescriptionAction(final WorkflowEditor editor) {
        super(editor);
    }

    /**
     * @see org.eclipse.jface.action.IAction#getId()
     */
    public String getId() {
        return ID;
    }

    /**
     * @see org.eclipse.jface.action.IAction#getText()
     */
    public String getText() {
        return "User name and description";
    }

    /**
     * @see org.eclipse.jface.action.IAction#getImageDescriptor()
     */
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getImageDescriptor("icons/setNameDescription.PNG");
    }

    /**
     * @see org.eclipse.jface.action.IAction#getToolTipText()
     */
    public String getToolTipText() {
        return "To set/view the user specified name and a context dependant description.";
    }

    /**
     * @return <code>true</code>, if just one node part is selected.
     * 
     * @see org.eclipse.gef.ui.actions.WorkbenchPartAction#calculateEnabled()
     */
    protected boolean calculateEnabled() {

        NodeContainerEditPart[] parts = getSelectedNodeParts();

        // only if just one node part is selected
        if (parts.length != 1) {
            return false;
        }

        return true;
    }

    /**
     * Opens a dialog and collects the user name and description. After the
     * dialog is closed the new name and description are set to the node
     * container if applicable.
     * 
     * @see de.unikn.knime.workbench.editor2.actions.AbstractNodeAction#
     *      runOnNodes(de.unikn.knime.workbench.editor2.editparts.
     *      NodeContainerEditPart[])
     */
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {

        // if more than one node part is selected
        if (nodeParts.length != 1) {
            LOGGER.debug("Execution denied as more than one node is "
                    + "selected. Not allowed in 'Set name and "
                    + "description' action.");
            return;
        }

        NodeContainer container = nodeParts[0].getNodeContainer();

        LOGGER.debug("Opening 'Set name and description' dialog"
                + " for one node ...");

        try {
            Workbench.getInstance().getActiveWorkbenchWindow().getActivePage()
                    .showView("org.eclipse.ui.views.ProgressView");

            // Give focus to the editor again. Otherwise the actions (selection)
            // is not updated correctly.
            getWorkbenchPart().getSite().getPage().activate(getWorkbenchPart());

        } catch (PartInitException e) {
            // ignore
        }

        // open name and description dialog
        Shell parent = Display.getCurrent().getActiveShell();

        NameDescriptionDialog dialog = new NameDescriptionDialog(parent,
                container.getUserName(), container.getDescription());

        int result = dialog.open();

        // check if ok was pressed
        if (result == NameDescriptionDialog.OK) {

            // if the name or description have been changed
            // the editor must be set dirty
            String description = dialog.getDescription();
            String userName = dialog.getName();
            if (userName.trim().equals("")) {

                if (container.getUserName() != null
                        || container.getDescription() != null) {

                    // mark editor as dirty
                    getEditor().markDirty();
                }
                container.setUserName(null);
                container.setDescription(null);

            } else {

                // if name or description is different mark editor dirty
                if (!userName.equals(container.getUserName())
                        || !description.equals(container.getDescription())) {
                    getEditor().markDirty();
                }

                container.setUserName(userName);
                container.setDescription(description);
            }
        }

        // else do nothing
    }
}
