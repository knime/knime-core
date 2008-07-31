/*
 * ------------------------------------------------------------------ *
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
 *   16.01.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.actions;

import javax.swing.SwingUtilities;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.workbench.editor2.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 *
 *
 * @author Fabian Dill, University of Konstanz
 */
public class DefaultOpenViewAction extends AbstractNodeAction {

    private static final NodeLogger LOGGER = NodeLogger
    .getLogger(DefaultOpenViewAction.class);

    /** Unique id for this action. */
    public static final String ID = "knime.action.defaultOpen";

    /**
     *
     * @param editor current editor
     */
    public DefaultOpenViewAction(final WorkflowEditor editor) {
        super(editor);
    }

    /**
     *
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
        return "Open first view";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getImageDescriptor("icons/openView.gif");
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getDisabledImageDescriptor() {
        return ImageRepository.getImageDescriptor(
                "icons/openView_disabled.gif");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return "Opens the first view of the selected node(s)";
    }

    /**
     * @return true if at least one selected node is executing or queued
     * @see org.eclipse.gef.ui.actions.WorkbenchPartAction#calculateEnabled()
     */
    @Override
    protected boolean calculateEnabled() {

        NodeContainerEditPart[] parts = getAllNodeParts();

        // enable if we have at least one executing or queued node in our
        // selection
        boolean atLeastOneNodeIsExecuted = false;
        for (int i = 0; i < parts.length; i++) {
            NodeContainer nc = parts[i].getNodeContainer();
            atLeastOneNodeIsExecuted |= nc.getState().equals(
                    NodeContainer.State.EXECUTED)
            && nc.getNrViews() > 0;
        }
        return atLeastOneNodeIsExecuted;

    }

    /**
     * This opens the first view of all the selected nodes.
     *
     * {@inheritDoc}
     */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        LOGGER.debug("Creating open default view job for " + nodeParts.length
                + " node(s)...");
        NodeContainerEditPart[] parts = getAllNodeParts();
        for (NodeContainerEditPart p : parts) {
            final NodeContainer cont = p.getNodeContainer();
            if (cont.getState().equals(NodeContainer.State.EXECUTED)
                    && cont.getNrViews() > 0) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        try {
                            String title = cont.getID().toString();
                            if (cont.getCustomName() != null) {
                                title = cont.getCustomName();
                            }
                            title += cont.getViewName(0);
                            cont.getView(0).createFrame(title);
                        } catch (Throwable t) {
                            MessageBox mb = new MessageBox(
                                    Display.getDefault().getActiveShell(),
                                    SWT.ICON_ERROR | SWT.OK);
                            mb.setText("View cannot be opened");
                            mb.setMessage("The view cannot be opened for the " 
                                    + "following reason:\n" + t.getMessage());
                            mb.open();
                            LOGGER.error("The view for node '"
                                    + cont.getNameWithID() + "' has thrown a '"
                                    + t.getClass().getSimpleName()
                                    + "'. That is most likely an " 
                                    + "implementation error.", t);
                        } 
                    }
                });
            }
        }


        try {
            // Give focus to the editor again. Otherwise the actions (selection)
            // is not updated correctly.
            getWorkbenchPart().getSite().getPage().activate(getWorkbenchPart());
        } catch (Exception e) {
            // ignore
        }
    }

}
