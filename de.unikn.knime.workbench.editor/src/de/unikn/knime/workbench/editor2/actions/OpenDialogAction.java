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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;

import de.unikn.knime.core.node.NodeLogger;
import de.unikn.knime.core.node.workflow.NodeContainer;
import de.unikn.knime.workbench.editor2.ImageRepository;
import de.unikn.knime.workbench.editor2.WorkflowEditor;
import de.unikn.knime.workbench.editor2.editparts.NodeContainerEditPart;
import de.unikn.knime.workbench.ui.wrapper.WrappedNodeDialog;

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
     * @see org.eclipse.jface.action.IAction#getId()
     */
    public String getId() {
        return ID;
    }

    /**
     * @see org.eclipse.jface.action.IAction#getText()
     */
    public String getText() {
        return "Configure";
    }

    /**
     * @see org.eclipse.jface.action.IAction#getImageDescriptor()
     */
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getImageDescriptor("icons/openDialog.gif");
    }

    /**
     * @see org.eclipse.jface.action.IAction#getToolTipText()
     */
    public String getToolTipText() {
        return "Open configuration dialog for this node";
    }

    /**
     * @return <code>true</code> if at we have a single node which has a
     *         dialog
     * @see org.eclipse.gef.ui.actions.WorkbenchPartAction#calculateEnabled()
     */
    protected boolean calculateEnabled() {
        if (getSelectedNodeParts().length != 1) {
            return false;
        }

        NodeContainerEditPart part = getSelectedNodeParts()[0];

        return part.getNodeContainer().hasDialog();
    }

    /**
     * 
     * @see de.unikn.knime.workbench.editor2.actions.AbstractNodeAction
     *      #runOnNodes(de.unikn.knime.workbench.editor2.
     *      editparts.NodeContainerEditPart[])
     */
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        LOGGER.debug("Opening node dialog...");
        NodeContainer container = (NodeContainer) nodeParts[0].getModel();

        //  
        // This is embedded in a special JFace wrapper dialog
        //
        WrappedNodeDialog dlg = new WrappedNodeDialog(Display.getCurrent()
                .getActiveShell(), container);
        dlg.open();
    }

}
