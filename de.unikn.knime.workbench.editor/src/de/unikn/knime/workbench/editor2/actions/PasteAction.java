/*
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
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;

import de.unikn.knime.core.node.NodeLogger;
import de.unikn.knime.core.node.NodeSettings;
import de.unikn.knime.core.node.workflow.NodeContainer;
import de.unikn.knime.core.node.workflow.NodeExtraInfo;
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
     * @see org.eclipse.jface.action.IAction#getImageDescriptor()
     */
    @Override
    public ImageDescriptor getImageDescriptor() {

        ISharedImages sharedImages = PlatformUI.getWorkbench()
                .getSharedImages();
        return sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_PASTE);
    }

    /**
     * @see org.eclipse.jface.action.IAction#getText()
     */
    @Override
    public String getText() {
        return "Paste";
    }

    /**
     * At least one <code>NodeSettings</code> object must be in the clipboard.
     * 
     * @see org.eclipse.gef.ui.actions.WorkbenchPartAction#calculateEnabled()
     */
    @Override
    protected boolean calculateEnabled() {
        ClipboardObject clipboardContent = getEditor().getClipboardContent();
        return (clipboardContent != null
                && clipboardContent.getContent() instanceof int[][]);
    }

    /**
     * @see de.unikn.knime.workbench.editor2.actions.AbstractNodeAction
     * #runOnNodes(de.unikn.knime.workbench.editor2.editparts.NodeContainerEditPart[])
     */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {

        // get the workflow manager
        WorkflowManager manager = getManager();

        // get the clipboard object
        ClipboardObject clipboardContent = getEditor().getClipboardContent();
        // ensure there is an object of type NodeSettings
        if (!(clipboardContent != null)
                || !(clipboardContent.getContent() instanceof int[][])) {

            return;
        }

        LOGGER.debug("Clipboard paste action invoked ...");

        // cast the clipboard object representing a sub workflow
        int[][] copySettings = (int[][])clipboardContent.getContent();
        int[][] newPartIds = null;
        try {
            newPartIds = manager.createSubWorkflow(copySettings[0],
                    copySettings[1]);
            
            for (int i = 0; i < newPartIds[0].length; i++) {
                NodeContainer nc =
                    manager.getNodeContainerById(newPartIds[0][i]);
                // finaly change the extra info so that the copies are
                // located differently (if not null)
                NodeExtraInfo extraInfo = nc.getExtraInfo();
                if (extraInfo != null) {
                    extraInfo.changePosition(
                            80 * (clipboardContent.getRetrievalCounter() + 1));
                    nc.setExtraInfo(extraInfo);
                    // this is a bit dirty but
                    // needed to trigger the re-layout of the node
                }

                
            }
        } catch (CloneNotSupportedException ex) {
            LOGGER.error("Could not copy nodes", ex);
        }

        EditPartViewer partViewer = getEditor().getViewer();

        // deselect the current selection and select the new pasted parts
        for (NodeContainerEditPart nodePart : nodeParts) {
            partViewer.deselect(nodePart);
        }

        for (ConnectionContainerEditPart connectionPart : getSelectedConnectionParts()) {
            partViewer.deselect(connectionPart);
        }

        // get the new ediparts and select them
        List<AbstractWorkflowEditPart> newParts =
            getEditPartsById(newPartIds[0], newPartIds[1]);

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
