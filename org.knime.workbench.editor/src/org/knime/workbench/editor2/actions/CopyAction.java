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
 *   20.02.2006 (sieb): created
 */
package org.knime.workbench.editor2.actions;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.workbench.editor2.ClipboardObject;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;
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

        ISharedImages sharedImages = PlatformUI.getWorkbench()
                .getSharedImages();
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
     * @see org.eclipse.gef.ui.actions.WorkbenchPartAction#calculateEnabled()
     */
    @Override
    protected boolean calculateEnabled() {
        NodeContainerEditPart[] parts = getSelectedNodeParts();

        return parts.length > 0;
    }

    /*
     * Creates a <code>NodesSettings</code> object from the given edit parts.
     *
     * @return the node settings representing the selected nodes
     */
    private NodeSettings getNodeSettings(
            final NodeContainerEditPart[] nodeParts,
            final ConnectionContainerEditPart[] connectionParts) {

        // TODO: functionality disabled
        /*
        // copy node setttings root
        NodeSettings clipboardRootSettings = new NodeSettings(
                WorkflowEditor.CLIPBOARD_ROOT_NAME);

        // save nodes in an own sub-config object as a series of configs
        NodeSettingsWO nodes = clipboardRootSettings
                .addNodeSettings(WorkflowManager.KEY_NODES);
        for (NodeContainerEditPart nodeEditPart : nodeParts) {

            NodeContainer nextNode = nodeEditPart.getNodeContainer();
            // and save it to it's own config object
            NodeSettingsWO nextNodeConfig = nodes.addNodeSettings("node_"
                    + nextNode.getID());
            nextNode.saveSettings(nextNodeConfig);
            // TODO notify about node settings saved ????
        }

        // save connections in an own sub-config object as a series of configs
        NodeSettingsWO connections = clipboardRootSettings
                .addNodeSettings(WorkflowManager.KEY_CONNECTIONS);

        for (ConnectionContainerEditPart connectionEditPart : connectionParts) {

            ConnectionContainer nextConnection = (ConnectionContainer)connectionEditPart
                    .getModel();

            // // and save it to it's own config object
            NodeSettingsWO nextConnectionConfig = connections
                    .addNodeSettings("connection_" + nextConnection.getID());
            nextConnection.save(nextConnectionConfig);
        }

        return clipboardRootSettings;
        */
        return null;

    }

    /**
     * {@inheritDoc}
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
