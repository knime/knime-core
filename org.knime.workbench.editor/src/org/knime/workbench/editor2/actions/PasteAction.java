/*
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   20.02.2006 (sieb): created
 */
package org.knime.workbench.editor2.actions;

import org.eclipse.gef.EditPartViewer;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.ClipboardObject;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.commands.PasteFromWorkflowPersistorCommand;
import org.knime.workbench.editor2.commands.PasteFromWorkflowPersistorCommand.ShiftCalculator;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;

/**
 * Implements the clipboard paste action to paste nodes and connections from the
 * clipboard into the editor.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class PasteAction extends AbstractClipboardAction {
//    private static final NodeLogger LOGGER =
//            NodeLogger.getLogger(PasteAction.class);
    
    private static final int OFFSET = 120;
    

    /**
     * Constructs a new clipboard paste action.
     * 
     * @param editor the workflow editor this action is intended for
     */
    public PasteAction(final WorkflowEditor editor) {
        super(editor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return ActionFactory.PASTE.getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        ISharedImages sharedImages =
                PlatformUI.getWorkbench().getSharedImages();
        return sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_PASTE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return "Paste";
    }

    /**
     * At least one <code>NodeSettings</code> object must be in the clipboard.
     * 
     * {@inheritDoc}
     */
    @Override
    protected boolean calculateEnabled() {
        return getEditor().getClipboardContent() != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        ClipboardObject clipObject = getEditor().getClipboardContent();
        ShiftCalculator shiftCalculator = newShiftCalculator();
        PasteFromWorkflowPersistorCommand pasteCommand =
            new PasteFromWorkflowPersistorCommand(
                    getManager(), clipObject, shiftCalculator);
        getCommandStack().execute(pasteCommand); // enables undo
        
        // change selection (from copied ones to pasted ones)
        EditPartViewer partViewer = getEditor().getViewer();

        // deselect the current selection and select the new pasted parts
        for (NodeContainerEditPart nodePart : nodeParts) {
            partViewer.deselect(nodePart);
        }
        

        for (ConnectionContainerEditPart connectionPart 
                : getSelectedConnectionParts()) {
            partViewer.deselect(connectionPart);
        }
        
        // select the new ones....
        if (partViewer.getRootEditPart().getContents() != null 
                && partViewer.getRootEditPart().getContents() 
                instanceof WorkflowRootEditPart) {
            ((WorkflowRootEditPart)partViewer.getRootEditPart().getContents())
                .setFutureSelection(pasteCommand.getPastedIDs());
        }
        
        
        // update the actions
        getEditor().updateActions();

        // Give focus to the editor again. Otherwise the actions (selection)
        // is not updated correctly.
        getWorkbenchPart().getSite().getPage().activate(getWorkbenchPart());
    }

    
    /**
     * A shift operator that calculates a fixed offset. The sub class
     * {@link PasteActionContextMenu} overrides this method to return a 
     * different shift calculator that respects the current mouse 
     * pointer location.
     * @return A new shift calculator.
     */
    protected ShiftCalculator newShiftCalculator() {
        return new ShiftCalculator() {
            /** {@inheritDoc} */
            @Override
            public int[] calculateShift(final NodeID[] ids, 
                    final WorkflowManager manager, 
                    final ClipboardObject clipObject) {
                final int counter = 
                    clipObject.incrementAndGetRetrievalCounter();
                int newX = (OFFSET * counter);
                int newY = (OFFSET * counter);
                return new int[] {newX, newY};
            }
        };
    }
}
