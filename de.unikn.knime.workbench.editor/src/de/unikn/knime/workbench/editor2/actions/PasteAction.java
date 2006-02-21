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

import java.util.List;

import org.eclipse.gef.EditPartViewer;
import org.eclipse.ui.actions.ActionFactory;

import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeLogger;
import de.unikn.knime.core.node.NodeSettings;
import de.unikn.knime.core.node.workflow.WorkflowManager;
import de.unikn.knime.workbench.editor2.ClipboardObject;
import de.unikn.knime.workbench.editor2.WorkflowEditor;
import de.unikn.knime.workbench.editor2.editparts.AbstractWorkflowEditPart;
import de.unikn.knime.workbench.editor2.editparts.ConnectionContainerEditPart;
import de.unikn.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * Implements the clipboard paste action to paste nodes and connections from the
 * clipboard into the editor.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class PasteAction extends AbstractClipboardAction {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(ExecuteAction.class);

    /**
     * Constructs a new clipboard paste action.
     * 
     * @param editor the workflow editor this action is intended for
     */
    public PasteAction(final WorkflowEditor editor) {

        super(editor);
    }

    /**
     * @see org.eclipse.jface.action.IAction#getId()
     */
    @Override
    public String getId() {

        return ActionFactory.PASTE.getId();
    }

    /**
     * At least one <code>NodeSettings</code> object must be in the clipboard.
     * 
     * @see org.eclipse.gef.ui.actions.WorkbenchPartAction#calculateEnabled()
     */
    protected boolean calculateEnabled() {

        ClipboardObject clipboardContent = getEditor().getClipboardContent();

        if (clipboardContent != null
                && clipboardContent.getContent() instanceof NodeSettings) {

            return true;
        }

        return false;
    }

    /**
     * @see de.unikn.knime.workbench.editor2.actions.AbstractNodeAction#runOnNodes(de.unikn.knime.workbench.editor2.editparts.NodeContainerEditPart[])
     */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {

        // get the workflow manager
        WorkflowManager manager = getManager();

        // get the clipboard object
        ClipboardObject clipboardContent = getEditor().getClipboardContent();
        // ensure there is an object of type NodeSettings
        if (!(clipboardContent != null)
                || !(clipboardContent.getContent() instanceof NodeSettings)) {

            return;
        }

        LOGGER.debug("Clipboard paste action invoked ...");

        // cast the clipboard object representing a sub workflow
        NodeSettings copySettings = (NodeSettings)clipboardContent.getContent();
        int[] newPartIds = null;
        // pass the settings to the workflow manager to create the copied nodes
        try {

            newPartIds = manager.createSubWorkflow(copySettings,
                    clipboardContent.getRetrievalCounter() + 1);

        } catch (InvalidSettingsException ise) {

            LOGGER.error("The retrieved settings object describing the "
                    + "clipboard content is invalid. Reason: "
                    + ise.getMessage());

            return;
        }

        EditPartViewer partViewer = nodeParts[0].getRoot().getViewer();

        // deselect the current selection and select the new pasted parts
        for (NodeContainerEditPart nodePart : nodeParts) {

            partViewer.deselect(nodePart);
        }

        for (ConnectionContainerEditPart connectionPart : getSelectedConnectionParts()) {

            partViewer.deselect(connectionPart);
        }

        // get the new ediparts and select them
        List<AbstractWorkflowEditPart> newParts = getEditPartsById(
                nodeParts[0], newPartIds);

        for (AbstractWorkflowEditPart newPart : newParts) {

            partViewer.appendSelection(newPart);
        }

        // increment the retrieval counter
        clipboardContent.incrementRetrievalCounter();

        // update the actions
        getEditor().updateActions();

        // Give focus to the editor again. Otherwise the actions (selection)
        // is not updated correctly.
        getWorkbenchPart().getSite().getPage().activate(getWorkbenchPart());

    }
}
