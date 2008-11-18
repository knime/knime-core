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
 *   25.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.actions;

import org.eclipse.jface.resource.ImageDescriptor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * Action to execute a node.
 *
 * @author Florian Georg, University of Konstanz
 */
public class ExecuteAction extends AbstractNodeAction {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(ExecuteAction.class);

    /** unique ID for this action. * */
    public static final String ID = "knime.action.execute";

    /**
     *
     * @param editor The workflow editor
     */
    public ExecuteAction(final WorkflowEditor editor) {
        super(editor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return "Execute";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getImageDescriptor("icons/execute.GIF");
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getDisabledImageDescriptor() {
        return ImageRepository.getImageDescriptor("icons/execute_disabled.PNG");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return "Execute the selected node(s)";
    }

    /**
     * @return always <code>true</code>, as the WFM tries to execute as much
     *         as possible
     * @see org.eclipse.gef.ui.actions.WorkbenchPartAction#calculateEnabled()
     */
    @Override
    protected boolean calculateEnabled() {
        NodeContainerEditPart[] parts = getSelectedNodeParts();
        // enable if we have at least one executable node in our selection
        WorkflowManager wm = getEditor().getWorkflowManager();
        for (int i = 0; i < parts.length; i++) {
            NodeContainer nc = parts[i].getNodeContainer();
            if (wm.canExecuteNode(nc.getID())) {
                return true;
            }
        }
        return false;

    }

    /**
     * This starts an execution job for the selected nodes. Note that this is
     * all controlled by the WorkflowManager object of the currently open
     * editor.
     *
     * @see org.knime.workbench.editor2.actions.AbstractNodeAction
     *      #runOnNodes(org.knime.workbench.editor2.
     *      editparts.NodeContainerEditPart[])
     */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        LOGGER.debug("Creating execution job for " + nodeParts.length
                + " node(s)...");
        WorkflowManager manager = getManager();
        NodeID[] ids = new NodeID[nodeParts.length];
        for (int i = 0; i < nodeParts.length; i++) {
            ids[i] = nodeParts[i].getNodeContainer().getID();
        }
        manager.executeUpToHere(ids);
        try {
//            Workbench.getInstance().getActiveWorkbenchWindow().getActivePage()
//                    .showView("org.eclipse.ui.views.ProgressView");
            // Give focus to the editor again. Otherwise the actions (selection)
            // is not updated correctly.
            getWorkbenchPart().getSite().getPage().activate(getWorkbenchPart());
        } catch (Exception e) {
            // ignore
        }
    }
}
