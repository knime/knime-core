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
 * Action to execute a node and open its first view.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class ExecuteAndOpenViewAction extends AbstractNodeAction {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(ExecuteAndOpenViewAction.class);

    /**
     * unique ID for this action.
     */
    public static final String ID = "knime.action.executeandopenview";

    /**
     * @param editor The workflow editor
     */
    public ExecuteAndOpenViewAction(final WorkflowEditor editor) {
        super(editor);
    }

    /**
     * @see org.eclipse.jface.action.IAction#getId()
     */
    public String getId() {
        return ID;
    }

    /**
     * @see org.eclipse.jface.action.IAction#getText()
     */
    public String getText() {
        return "Execute and open view";
    }

    /**
     * @see org.eclipse.jface.action.IAction#getImageDescriptor()
     */
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getImageDescriptor("icons/executeAndView.PNG");
    }

    /**
     * @see org.eclipse.jface.action.IAction#getToolTipText()
     */
    public String getToolTipText() {
        return "Execute the selected node and open its first view.";
    }

    /**
     * @return <code>true</code>, if just one node part is selected which is
     *         executable and additionally has at least one view.
     * 
     * @see org.eclipse.gef.ui.actions.WorkbenchPartAction#calculateEnabled()
     */
    protected boolean calculateEnabled() {

        NodeContainerEditPart[] parts = getSelectedNodeParts();

        // only if just one node part is selected
        if (parts.length != 1) {
            return false;
        }

        // check if there is at least one view
        boolean enabled = parts[0].getNodeContainer().getNumViews() > 0;

        // the node must not be an interruptible node
        enabled &= !parts[0].getNodeContainer().isInterruptible();

        // check if the node is executable
        enabled &= parts[0].getNodeContainer().isExecutableUpToHere();

        return enabled;
    }

    /**
     * This starts an execution job for the selected node. Note that this is all
     * controlled by the WorkflowManager object of the currently open editor.
     * 
     * @see de.unikn.knime.workbench.editor2.actions.AbstractNodeAction
     *      #runOnNodes(de.unikn.knime.workbench.editor2.
     *      editparts.NodeContainerEditPart[])
     */
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {

        // if more than one node part is selected
        if (nodeParts.length != 1) {
            LOGGER.debug("Execution denied as more than one node is "
                    + "selected. Not allowed in 'Execute and "
                    + "open view' action.");
            return;
        }

        LOGGER.debug("Creating execution job for one node ...");
        WorkflowManager manager = getManager();

        // this jobs starts sub-jobs every time new nodes become available for
        // execution
        final NodeExecutionManagerJob job = new NodeExecutionManagerJob(
                manager, nodeParts);

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

        // open the view now in another thread
        new Thread(new Runnable() {
            public void run() {
                // wait until the job has finished execution
                job.waitUntilFinished();

                nodeParts[0].getNodeContainer().showView(0);
            }
        }).start();

    }
}
