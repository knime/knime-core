/* @(#)$RCSfile$ 
 * $Revision: 280 $ $Date: 2006-02-21 17:39:37 +0100 (Di, 21 Feb 2006) $ $Author: sieb $
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
import de.unikn.knime.workbench.editor2.ImageRepository;
import de.unikn.knime.workbench.editor2.WorkflowEditor;
import de.unikn.knime.workbench.editor2.actions.job.NodeExecutionManagerJob;
import de.unikn.knime.workbench.editor2.editparts.NodeContainerEditPart;

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
     * @see org.eclipse.jface.action.IAction#getId()
     */
    public String getId() {
        return ID;
    }

    /**
     * @see org.eclipse.jface.action.IAction#getText()
     */
    public String getText() {
        return "Execute all";
    }

    /**
     * @see org.eclipse.jface.action.IAction#getImageDescriptor()
     */
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getImageDescriptor("icons/executeAll.gif");
    }

    /**
     * @see org.eclipse.jface.action.IAction#getToolTipText()
     */
    public String getToolTipText() {
        return "Execute all executable nodes.";
    }

    /**
     * @return always <code>true</code>, as the WFM tries to execute as much
     *         as possible
     * @see org.eclipse.gef.ui.actions.WorkbenchPartAction#calculateEnabled()
     */
    protected boolean calculateEnabled() {
        NodeContainerEditPart[] parts = getAllNodeParts();

        // enable if we have at least one executable node in our selection
        boolean atLeastOneNodeIsExecutable = false;
        for (int i = 0; i < parts.length; i++) {
            atLeastOneNodeIsExecutable |= parts[i].getNodeContainer()
                    .isExecutableUpToHere();
        }
        return atLeastOneNodeIsExecutable;

    }

    /**
     * This starts an execution job for all executable nodes. Note that this is
     * all controlled by the WorkflowManager object of the currently open
     * editor. The passed nodeParts are not needed here, as not only the
     * selected parts are executed but all executable nodes.
     * 
     * @see de.unikn.knime.workbench.editor2.actions.AbstractNodeAction
     *      #runOnNodes(de.unikn.knime.workbench.editor2.
     *      editparts.NodeContainerEditPart[])
     */
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {

        NodeContainerEditPart[] allNodeParts = getAllNodeParts();
        LOGGER.debug("(Exec all) Creating execution job for "
                + allNodeParts.length + " node(s)...");

        // create a job that executes all nodes
        NodeExecutionManagerJob job = new NodeExecutionManagerJob(getManager());

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
