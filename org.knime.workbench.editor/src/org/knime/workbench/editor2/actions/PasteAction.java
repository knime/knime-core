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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.gef.EditPartViewer;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.ConnectionUIInformation;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.workbench.editor2.ClipboardWorkflowManager;
import org.knime.workbench.editor2.WorkflowEditor;
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
//        ClipboardObject clipboardContent = getEditor().getClipboardContent();
//        if (clipboardContent == null) {
//            return false;
//        }
//        return clipboardContent.getNodeIDs().size() > 0;
        return ClipboardWorkflowManager.get().size() > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        Collection<NodeContainer>containers = ClipboardWorkflowManager.get(); 
        NodeID[] ids = new NodeID[containers.size()];
        int index = 0;
        for (NodeContainer container : containers) {
            ids[index++] = container.getID();
        }
        NodeID[] copiedNodes = getManager().copyFromAndPasteHere(
                ClipboardWorkflowManager.getSourceWorkflowManager(), ids);
        Set<NodeID>newIDs = new HashSet<NodeID>();
        int[] moveDist = calculateShift(copiedNodes);
        for (NodeID id : copiedNodes) {
            newIDs.add(id);
            NodeContainer nc = getManager().getNodeContainer(id);
            NodeUIInformation oldUI = (NodeUIInformation)nc.getUIInformation();
            NodeUIInformation newUI = 
                oldUI.createNewWithOffsetPosition(moveDist);
            nc.setUIInformation(newUI);
        }
        for (ConnectionContainer conn 
                    : getManager().getConnectionContainers()) {
            if (newIDs.contains(conn.getDest()) 
                    && newIDs.contains(conn.getSource())) {
                // get bend points and move them
                ConnectionUIInformation oldUI = 
                    (ConnectionUIInformation)conn.getUIInfo();
                if (oldUI != null) {
                    ConnectionUIInformation newUI = 
                        oldUI.createNewWithOffsetPosition(moveDist);
                    conn.setUIInfo(newUI);
                }
            }
        }
        ClipboardWorkflowManager.incrementRetrievalCounter();
        
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
                .setFutureSelection(copiedNodes);
        }
        
        
        // update the actions
        getEditor().updateActions();

        // Give focus to the editor again. Otherwise the actions (selection)
        // is not updated correctly.
        getWorkbenchPart().getSite().getPage().activate(getWorkbenchPart());
    }
    

    /**
     * @param ids the ids of the nodes for which the shift should be 
     *  calculated (in normal {@link PasteAction} the offset is fixed, but in
     *  {@link PasteActionContextMenu} the position of the nodes is set relative
     *  to the mouse position
     *  
     * @return the offset to add to the current node position, which is done by 
     *  the {@link NodeUIInformation#createNewWithOffsetPosition(int[])}
     */
    protected int[] calculateShift(final NodeID[] ids) {
        // simply return the offset 
        // the uiInfo.changePosition(moveDist); adds the distance to the 
        // current location of the node
        int counter = ClipboardWorkflowManager.getRetrievalCounter();
        counter += 1;
        int newX = (OFFSET * counter);
        int newY = (OFFSET * counter);
        return new int[] {newX, newY};
    }
}
