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
 *   25.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeStateChangeListener;
import org.knime.core.node.workflow.NodeStateEvent;
import org.knime.workbench.editor2.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * Action to execute a node and open its first view.
 *
 * @author Christoph Sieb, University of Konstanz
 */
public class ExecuteAndOpenViewAction extends AbstractNodeAction {
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(ExecuteAndOpenViewAction.class);

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
        return "Execute and open view";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getImageDescriptor("icons/executeAndView.GIF");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getDisabledImageDescriptor() {
        return ImageRepository
                .getImageDescriptor("icons/executeAndView_diabled.PNG");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return "Execute the selected node and open its first view.";
    }

    /**
     * @return <code>true</code>, if just one node part is selected which is
     *         executable and additionally has at least one view.
     *
     * @see org.eclipse.gef.ui.actions.WorkbenchPartAction#calculateEnabled()
     */
    @Override
    protected boolean calculateEnabled() {
        NodeContainerEditPart[] parts = getSelectedNodeParts();

        // TODO: we have to check if at least one node of the selection
        // has nrOfViews > 0 && if at least one node is configured

        // only if just one node part is selected
        if (parts.length != 1) {
            return false;
        }

        // check if there is at least one view
        return parts[0].getNodeContainer().getState().equals(
                NodeContainer.State.CONFIGURED)
                && parts[0].getNodeContainer().getNrViews() > 0;
    }

    /**
     * This starts an execution job for the selected node. Note that this is all
     * controlled by the WorkflowManager object of the currently open editor.
     *
     * @see org.knime.workbench.editor2.actions.AbstractNodeAction
     *      #runOnNodes(org.knime.workbench.editor2.
     *      editparts.NodeContainerEditPart[])
     */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        // if more than one node part is selected
        if (nodeParts.length != 1) {
            LOGGER.debug("Execution denied as more than one node is "
                    + "selected. Not allowed in 'Execute and "
                    + "open view' action.");
            return;
        }

        LOGGER.debug("Executing and opening view for one node");

        final NodeContainer cont = nodeParts[0].getNodeContainer();

        // for interruptible nodes the view is opened immediatly
        // for all other nodes the view should first opened if the execution is
        // over
        // if (cont.isInterruptible()) {
        // getManager().executeUpToNode(cont.getID(), false);
        // // interruptible nodes are always displayed in a jframe
        // cont.showView(0);
        // } else {
        // register at the node container to receive the executed event
        // thus it is time to start the view
        // in case a cancel event is received the listener is deregistered
        // cont.addListener(new NodeStateListener() {
        // public void stateChanged(final NodeStatus state, final int id) {
        // if (state instanceof NodeStatus.EndExecute) {
        // cont.removeListener(this);
        // if (cont.isExecuted()) {
        // Display.getDefault().syncExec(new Runnable() {
        // public void run() {
        // m_viewAction.run();
        // };
        //
        // });
        //
        // }
        // } else if (state instanceof NodeStatus.ExecutionCanceled) {
        // cont.removeListener(this);
        // }
        // }
        // });
        // another listener must be registered at the workflow manager
        // to receive also thos events from nodes that have just
        // been queued.
        cont.addNodeStateChangeListener(new NodeStateChangeListener() {
            public void stateChanged(final NodeStateEvent state) {

                // check if the node has finished (either executed or
                // removed from the queue)
                // LOGGER.error("Event: " + event.getID() + " Node: "
                // + cont.getID() + "node Referenz: "
                // + System.identityHashCode(cont));
                if (state.getSource() == cont.getID()
                        && state.getState().equals(
                                NodeContainer.State.EXECUTED)) {

                    // if the node was successfully executed
                    // start the view
                    if (cont.getState().equals(NodeContainer.State.EXECUTED)) {
                        Display.getDefault().asyncExec(new Runnable() {
                            public void run() {
                                // set the appropriate action to open the view
                                IAction viewAction = new OpenViewAction(
                                        cont, 0);
                                viewAction.run();
                            }
                        });
                    }
                }
                if (!cont.getState().executionInProgress()) {
                    // in those cases remove the listener
                    cont.removeNodeStateChangeListener(this);
                }
            }

        });
        getManager().executeUpToHere(cont.getID());
        // }

        // Give focus to the editor again. Otherwise the actions (selection)
        // is not updated correctly.
        getWorkbenchPart().getSite().getPage().activate(getWorkbenchPart());
    }
}
