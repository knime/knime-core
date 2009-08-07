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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.workbench.editor2.ClipboardObject;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * Implements the clipboard copy action to copy nodes and connections into the
 * clipboard.
 *
 * @author Christoph Sieb, University of Konstanz
 */
public class CopyAction extends AbstractClipboardAction {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(CopyAction.class);

    /**
     * Constructs a new clipboard copy action.
     *
     * @param editor the workflow editor this action is intended for
     */
    public CopyAction(final WorkflowEditor editor) {
        super(editor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return ActionFactory.COPY.getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        ISharedImages sharedImages = 
            PlatformUI.getWorkbench().getSharedImages();
        return sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_COPY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return "Copy";
    }

    /**
     * At least one node must be selected.
     *
     * {@inheritDoc}
     */
    @Override
    protected boolean calculateEnabled() {
        NodeContainerEditPart[] parts = getSelectedNodeParts();
        return parts.length > 0;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        LOGGER.debug("Clipboard copy action invoked for " + nodeParts.length
                + " node(s)");

        NodeID[] ids = new NodeID[nodeParts.length];
        for (int i = 0; i < nodeParts.length; i++) {
            NodeContainerEditPart nodeEP = nodeParts[i];
            ids[i] = nodeEP.getNodeContainer().getID();
        }
        
        WorkflowPersistor copyPersistor = getManager().copy(ids);
        
        // ClipboardWorkflowManager.put(getManager(), ids);
        
        // the information about the nodes is stored in the config XML format
        // also used to store workflow information in the kflow files
        // getEditor().getClipboard().setContents(
        // new Object[]{getNodeSettings(nodeParts,
        // connectionParts)},
        // new Transfer[]{ResourceTransfer.getInstance()});
        getEditor().setClipboardContent(new ClipboardObject(copyPersistor));
        
        // update the actions
        getEditor().updateActions();

        // Give focus to the editor again. Otherwise the actions (selection)
        // is not updated correctly.
        getWorkbenchPart().getSite().getPage().activate(getWorkbenchPart());
    }
    
}
