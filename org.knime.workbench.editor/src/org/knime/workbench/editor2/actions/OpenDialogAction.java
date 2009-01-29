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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.workbench.editor2.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.ui.wrapper.WrappedNodeDialog;

/**
 * Action to open the dialog of a node.
 *
 * @author Florian Georg, University of Konstanz
 */
public class OpenDialogAction extends AbstractNodeAction {
    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(OpenDialogAction.class);

    /** unique ID for this action. * */
    public static final String ID = "knime.action.openDialog";

    /**
     *
     * @param editor The workflow editor
     */
    public OpenDialogAction(final WorkflowEditor editor) {
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
        return "Configure";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getImageDescriptor("icons/openDialog.gif");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return "Open configuration dialog for this node";
    }

    /**
     * @return <code>true</code> if at we have a single node which has a
     *         dialog
     * @see org.eclipse.gef.ui.actions.WorkbenchPartAction#calculateEnabled()
     */
    @Override
    protected boolean calculateEnabled() {
        if (getSelectedNodeParts().length != 1) {
            return false;
        }

        NodeContainerEditPart part = getSelectedNodeParts()[0];

        return part.getNodeContainer().hasDialog();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        LOGGER.debug("Opening node dialog...");
        NodeContainer container = (NodeContainer) nodeParts[0].getModel();
        if (!container.hasDialog()) {
            // if short cut key is launched on a selected node without dialog
            LOGGER.debug(
                    "Node " + container.getNameWithID() + " has no dialog!");
            // ignore
            return;
        }
        //
        // This is embedded in a special JFace wrapper dialog
        //
        try {
            WrappedNodeDialog dlg = new WrappedNodeDialog(Display.getCurrent()
                    .getActiveShell(), container);
            dlg.open();
            dlg.close();
        } catch (NotConfigurableException ex) {
            MessageBox mb = new MessageBox(
                    Display.getDefault().getActiveShell(),
                    SWT.ICON_WARNING | SWT.OK);
            mb.setText("Dialog cannot be opened");
            mb.setMessage("The dialog cannot be opened for the following"
                    + " reason:\n" + ex.getMessage());
            mb.open();            
        } catch (Throwable t) {
            MessageBox mb = new MessageBox(
                    Display.getDefault().getActiveShell(),
                    SWT.ICON_ERROR | SWT.OK);
            mb.setText("Dialog cannot be opened");
            mb.setMessage("The dialog cannot be opened for the following"
                    + " reason:\n" + t.getMessage());
            mb.open();
            LOGGER.error("The dialog pane for node '"
                    + container.getNameWithID() + "' has thrown a '"
                    + t.getClass().getSimpleName()
                    + "'. That is most likely an implementation error.", t);
        } 
    }
}
