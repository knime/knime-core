/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
import org.knime.core.node.NodeStateListener;
import org.knime.core.node.NodeStatus;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.workbench.editor2.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.preferences.PreferenceConstants;

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

    private IAction m_viewAction;

    /**
     * @param editor The workflow editor
     */
    public ExecuteAndOpenViewAction(final WorkflowEditor editor) {
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
        return "Execute and open view";
    }

    /**
     * @see org.eclipse.jface.action.IAction#getImageDescriptor()
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getImageDescriptor("icons/executeAndView.GIF");
    }

    /**
     * @see org.eclipse.jface.action.Action#getDisabledImageDescriptor()
     */
    @Override
    public ImageDescriptor getDisabledImageDescriptor() {
        return ImageRepository
                .getImageDescriptor("icons/executeAndView_diabled.PNG");
    }

    /**
     * @see org.eclipse.jface.action.IAction#getToolTipText()
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

        // only if just one node part is selected
        if (parts.length != 1) {
            return false;
        }

        // check if there is at least one view
        boolean enabled = parts[0].getNodeContainer().getNumViews() > 0;

        // // the node must not be an interruptible node
        // enabled &= !parts[0].getNodeContainer().isInterruptible();

        // check if the node is executable
        enabled &= parts[0].getNodeContainer().isExecutableUpToHere();

        return enabled;
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

        boolean openEmbedded =
                KNIMEUIPlugin.getDefault().getPreferenceStore().getString(
                        PreferenceConstants.P_CHOICE_VIEWMODE).equals(
                        PreferenceConstants.P_CHOICE_VIEWMODE_VIEW);

        LOGGER.debug("Executing and opening view for one node");

        final NodeContainer cont = nodeParts[0].getNodeContainer();

        // set the appropriate action to open the view (jframe or embedded)
        m_viewAction = null;
        if (openEmbedded) {
            m_viewAction = new OpenViewEmbeddedAction(cont, 0);
        } else {
            m_viewAction = new OpenViewAction(cont, 0);
        }

        // for interruptible nodes the view is opened immediatly
        // for all other nodes the view should first opened if the execution is
        // over
        if (cont.isInterruptible()) {
            getManager().executeUpToNode(cont.getID(), false);
            // interruptible nodes are always displayed in a jframe
            cont.showView(0);
        } else {
            cont.addListener(new NodeStateListener() {
                public void stateChanged(final NodeStatus state, final int id) {
                    if (state instanceof NodeStatus.EndExecute) {
                        cont.removeListener(this);
                        if (cont.isExecuted()) {
                            Display.getDefault().syncExec(new Runnable() {
                                public void run() {
                                    m_viewAction.run();
                                };

                            });

                        }
                    } else if (state instanceof NodeStatus.ExecutionCanceled) {
                        cont.removeListener(this);
                    }
                }
            });
            getManager().executeUpToNode(cont.getID(), false);
        }

        // Give focus to the editor again. Otherwise the actions (selection)
        // is not updated correctly.
        getWorkbenchPart().getSite().getPage().activate(getWorkbenchPart());
    }
}
