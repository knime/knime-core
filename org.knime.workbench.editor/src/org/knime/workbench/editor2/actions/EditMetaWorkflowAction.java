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
 *   14.12.2005 (Christoph Sieb): created
 */
package org.knime.workbench.editor2.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.meta.MetaNodeModel;
import org.knime.core.node.workflow.NodeContainer;

import org.knime.workbench.editor2.ImageRepository;
import org.knime.workbench.editor2.MetaWorkflowEditor;
import org.knime.workbench.editor2.MetaWorkflowEditorInput;
import org.knime.workbench.editor2.WorkflowEditor;

/**
 * Action to open a new editor tab in which the metaworkflow can be modified.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class EditMetaWorkflowAction extends Action {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(OpenPortViewAction.class);

    /**
     * The meta node to open the editor for.
     */
    private NodeContainer m_nodeContainer;

    /**
     * New action to create a workflow editor for a meta node.
     * 
     * @param nodeContainer The node
     */
    public EditMetaWorkflowAction(final NodeContainer nodeContainer) {
        m_nodeContainer = nodeContainer;
    }

    /**
     * @see org.eclipse.jface.action.IAction#getImageDescriptor()
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getImageDescriptor("icons/openView.gif");
    }

    /**
     * @see org.eclipse.jface.action.IAction#getToolTipText()
     */
    @Override
    public String getToolTipText() {
        return "Opens meta-workflow editor";
    }

    /**
     * @see org.eclipse.jface.action.IAction#getText()
     */
    @Override
    public String getText() {
        return "Open meta-workflow editor";
    }

    /**
     * @see org.eclipse.jface.action.Action#run()
     */
    @Override
    public void run() {
        // check if this node is a meta-workflow
        if (!MetaNodeModel.class.isAssignableFrom(
                m_nodeContainer.getModelClass())) {
            LOGGER.debug("Not a meta-node! " + m_nodeContainer.nodeToString());
            return;
        }

        LOGGER.debug("Open meta-workflow editor "
                + m_nodeContainer.nodeToString());

        // create the editor input wrapper
        IEditorInput editorInput = new MetaWorkflowEditorInput(m_nodeContainer);

        try {

            // get the currently active editor to remember the new opened
            // editor as a child. this is neccessary to recursivly close
            // the editors once a parent editor is closed.
            WorkflowEditor parentEditor = (WorkflowEditor)PlatformUI
                    .getWorkbench().getActiveWorkbenchWindow().getActivePage()
                    .getActiveEditor();

            // before creating a new editor check wether there is an already
            // opened editor representing the given meta node container
            // if so set this editor active instead of creating a new one.
            MetaWorkflowEditor childEditor = parentEditor
                    .getEditor(m_nodeContainer);
            if (childEditor != null && !childEditor.isClosed()) {

                PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                        .getActivePage().activate(childEditor);
                return;
            } else if (childEditor != null && childEditor.isClosed()) {

                // if this editor was created but has been closed already
                // the editor is removed from its parent and a new one is
                // created in the upcoming code
                parentEditor.removeEditor(childEditor);
                childEditor = null;
            }

            childEditor = (MetaWorkflowEditor)PlatformUI
                    .getWorkbench()
                    .getActiveWorkbenchWindow()
                    .getActivePage()
                    .openEditor(editorInput,
                          "org.knime.workbench.editor.MetaWorkflowEditor");

            childEditor.setParentEditor(parentEditor);
            parentEditor.addEditor(childEditor);

        } catch (Exception e) {
            LOGGER.error("Meta-workflow editor could not be created for: "
                    + m_nodeContainer.nodeToString() + ": " + e.getMessage());
        }
    }
}
