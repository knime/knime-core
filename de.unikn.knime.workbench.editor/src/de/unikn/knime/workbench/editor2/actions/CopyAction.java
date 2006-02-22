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
import de.unikn.knime.core.node.NodeSettings;
import de.unikn.knime.core.node.workflow.ConnectionContainer;
import de.unikn.knime.core.node.workflow.NodeContainer;
import de.unikn.knime.core.node.workflow.WorkflowManager;
import de.unikn.knime.workbench.editor2.ClipboardObject;
import de.unikn.knime.workbench.editor2.WorkflowEditor;
import de.unikn.knime.workbench.editor2.editparts.ConnectionContainerEditPart;
import de.unikn.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * Implements the clipboard copy action to copy nodes and connections into the
 * clipboard.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class CopyAction extends AbstractClipboardAction {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(ExecuteAction.class);

    /**
     * Constructs a new clipboard copy action.
     * 
     * @param editor the workflow editor this action is intended for
     */
    public CopyAction(final WorkflowEditor editor) {

        super(editor);
    }

    /**
     * @see org.eclipse.jface.action.IAction#getId()
     */
    @Override
    public String getId() {

        return ActionFactory.COPY.getId();
    }

    /**
     * @see org.eclipse.jface.action.IAction#getImageDescriptor()
     */
    public ImageDescriptor getImageDescriptor() {

        ISharedImages sharedImages = PlatformUI.getWorkbench()
                .getSharedImages();
        return sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_COPY);
    }

    /**
     * @see org.eclipse.jface.action.IAction#getText()
     */
    public String getText() {
        return "Copy";
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
     * Creates a <code>NodesSettings</code> object from the given edit parts.
     * 
     * @return the node settings representing the selected nodes
     */
    private NodeSettings getNodeSettings(
            final NodeContainerEditPart[] nodeParts,
            final ConnectionContainerEditPart[] connectionParts) {

        // copy node setttings root
        NodeSettings clipboardRootSettings = new NodeSettings(
                WorkflowEditor.CLIPBOARD_ROOT_NAME);

        // save nodes in an own sub-config object as a series of configs
        NodeSettings nodes = clipboardRootSettings
                .addConfig(WorkflowManager.KEY_NODES);
        for (NodeContainerEditPart nodeEditPart : nodeParts) {

            NodeContainer nextNode = nodeEditPart.getNodeContainer();
            // and save it to it's own config object
            NodeSettings nextNodeConfig = nodes.addConfig("node_"
                    + nextNode.getID());
            nextNode.save(nextNodeConfig);
            // TODO notify about node settings saved ????
        }

        // save connections in an own sub-config object as a series of configs
        NodeSettings connections = clipboardRootSettings
                .addConfig(WorkflowManager.KEY_CONNECTIONS);

        for (ConnectionContainerEditPart connectionEditPart : connectionParts) {

            ConnectionContainer nextConnection = (ConnectionContainer)connectionEditPart
                    .getModel();

            // // and save it to it's own config object
            NodeSettings nextConnectionConfig = connections
                    .addConfig("connection_" + nextConnection.getID());
            nextConnection.save(nextConnectionConfig);
        }

        return clipboardRootSettings;
    }

    /**
     * @see de.unikn.knime.workbench.editor2.actions.AbstractNodeAction#runOnNodes(de.unikn.knime.workbench.editor2.editparts.NodeContainerEditPart[])
     */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {

        // additionally to the inherited functionality to get all nodes
        // the connections must be retrieved as they can be copied as
        // well if both nodes defining a connection are copied together
        ConnectionContainerEditPart[] connectionParts = getSelectedConnectionParts();

        LOGGER.debug("Clipboard copy action invoked for " + nodeParts.length
                + " node(s) and " + connectionParts.length + " connection(s).");

        // create the settings object to put in the clipboard

        // the information about the nodes is stored in the config XML format
        // also used to store workflow information in the kflow files
        // getEditor().getClipboard().setContents(
        // new Object[]{getNodeSettings(nodeParts,
        // connectionParts)},
        // new Transfer[]{ResourceTransfer.getInstance()});
        getEditor()
                .setClipboardContent(
                        new ClipboardObject(getNodeSettings(nodeParts,
                                connectionParts)));

        // update the actions
        getEditor().updateActions();

        // Give focus to the editor again. Otherwise the actions (selection)
        // is not updated correctly.
        getWorkbenchPart().getSite().getPage().activate(getWorkbenchPart());
    }
}
