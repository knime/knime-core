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
 *   25.05.2005 (Florian Georg): created
 */
package de.unikn.knime.workbench.editor2.actions;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.internal.Workbench;

import de.unikn.knime.core.node.NodeLogger;
import de.unikn.knime.core.node.workflow.WorkflowManager;
import de.unikn.knime.workbench.editor2.ImageRepository;
import de.unikn.knime.workbench.editor2.WorkflowEditor;
import de.unikn.knime.workbench.editor2.actions.job.NodeExecutionManagerJob;
import de.unikn.knime.workbench.editor2.editparts.NodeContainerEditPart;

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
     * @see org.eclipse.jface.action.IAction#getId()
     */
    @Override
    public String getId() {
        return ID;
    }

    /**
     * @see org.eclipse.jface.action.IAction#getText()
     */
    @Override
    public String getText() {
        return "Execute";
    }

    /**
     * @see org.eclipse.jface.action.IAction#getImageDescriptor()
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getImageDescriptor("icons/execute.gif");
    }

    /**
     * @see org.eclipse.jface.action.IAction#getToolTipText()
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
        boolean atLeastOneNodeIsExecutable = false;
        for (int i = 0; i < parts.length; i++) {
            atLeastOneNodeIsExecutable |= parts[i].getNodeContainer()
                    .isExecutableUpToHere();
        }
        return atLeastOneNodeIsExecutable;

    }

    /**
     * This starts an execution job for the selected nodes. Note that this is
     * all controlled by the WorkflowManager object of the currently open
     * editor.
     * 
     * @see de.unikn.knime.workbench.editor2.actions.AbstractNodeAction
     *      #runOnNodes(de.unikn.knime.workbench.editor2.
     *      editparts.NodeContainerEditPart[])
     */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        LOGGER.debug("Creating execution job for " + nodeParts.length
                + " node(s)...");
        WorkflowManager manager = getManager();

        // this jobs starts sub-jobs every time new nodes become available for
        // execution
        NodeExecutionManagerJob job = new NodeExecutionManagerJob(manager,
                nodeParts);

        try {
            Workbench.getInstance().getActiveWorkbenchWindow().getActivePage()
                    .showView("org.eclipse.ui.views.ProgressView");

            // Give focus to the editor again. Otherwise the actions (selection)
            // is not updated correctly.
            getWorkbenchPart().getSite().getPage().activate(getWorkbenchPart());

        } catch (PartInitException e) {
            // ignore
        }
        job.setUser(false);
        // Execution monitor should not be presented to user - its "system"
        job.setSystem(true);
        job.setPriority(Job.LONG);
        job.schedule();
    }
}
