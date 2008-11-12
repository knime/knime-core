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
import org.knime.workbench.editor2.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * Action to execute all nodes that are executable.
 *
 * @author Christoph sieb, University of Konstanz
 */
public class ExecuteAllAction extends AbstractNodeAction {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(ExecuteAllAction.class);

    /** unique ID for this action. * */
    public static final String ID = "knime.action.executeall";

    /**
     *
     * @param editor The workflow editor
     */
    public ExecuteAllAction(final WorkflowEditor editor) {
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
        return "Execute all";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getImageDescriptor("icons/executeAll.GIF");
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getDisabledImageDescriptor() {
        return ImageRepository.getImageDescriptor(
                "icons/executeAll_disabled.PNG");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return "Execute all executable nodes.";
    }

    /**
     * @return always <code>true</code>, as the WFM tries to execute as much
     *         as possible
     * @see org.eclipse.gef.ui.actions.WorkbenchPartAction#calculateEnabled()
     */
    @Override
    protected boolean calculateEnabled() {
        NodeContainerEditPart[] parts = getAllNodeParts();
        // enable if we have at least one executable node in our selection
        boolean atLeastOneNodeIsExecutable = false;
        for (int i = 0; i < parts.length; i++) {
            NodeContainer nc = parts[i].getNodeContainer();
            boolean executable = nc.getState().equals(
                    NodeContainer.State.CONFIGURED);
            atLeastOneNodeIsExecutable |= executable;
        }
        return atLeastOneNodeIsExecutable;

    }

    /**
     * This starts an execution job for all executable nodes. Note that this is
     * all controlled by the WorkflowManager object of the currently open
     * editor. The passed nodeParts are not needed here, as not only the
     * selected parts are executed but all executable nodes.
     *
     * {@inheritDoc}
     */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        LOGGER.debug("Starting execution of all nodes");
        getManager().executeAll();

        try {
            // Give focus to the editor again. Otherwise the actions (selection)
            // is not updated correctly.
            getWorkbenchPart().getSite().getPage().activate(getWorkbenchPart());
        } catch (Exception e) {
            LOGGER.error("Failed to complete execute all command: ", e);
            // ignore
        }
    }
}
