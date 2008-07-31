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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.AbstractWorkflowEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * Abstract base class for actions that do something with a
 * <code>NodeContainer</code> inside the <code>WorkflowEditor</code>. Note
 * that this hooks as a workflow listener as soon as the
 * <code>WorkflowManager</code> is available. This is needed, because
 * enablement of an action may change not only on selection changes but also on
 * workflow changes.
 *
 * @author Florian Georg, University of Konstanz
 */
public abstract class AbstractNodeAction extends SelectionAction {

    private final WorkflowEditor m_editor;

    /**
     *
     * @param editor The editor that is associated with this action
     */
    public AbstractNodeAction(final WorkflowEditor editor) {
        super(editor);
        setLazyEnablementCalculation(true);

        m_editor = editor;

    }

    // /**
    // * Unhook workflow listener when disposed
    // *
    // * @see org.eclipse.gef.Disposable#dispose()
    // */
    // public void dispose() {
    // super.dispose();
    // if (getManager() != null) {
    // getManager().removeListener(this);
    // m_registered = false;
    // }
    // }

    /**
     * @return The manager that is edited by the current editor. Subclasses may
     *         want to have a reference to this.
     *
     * Note that this value may be <code>null</code> if the editor has not
     * already been created completly !
     *
     */
    protected final WorkflowManager getManager() {
        return m_editor.getWorkflowManager();

    }

    /**
     * Calls <code>runOnNodes</code> with the current selected
     * <code>NodeContainerEditPart</code>s.
     *
     * @see org.eclipse.jface.action.IAction#run()
     */
    @Override
    public final void run() {

        // get selected parts...
        final NodeContainerEditPart[] parts = getSelectedNodeParts();

        // call implementation of this action in the SWT UI thread
        Display.getCurrent().syncExec(new Runnable() {

            public void run() {
                runOnNodes(parts);
            }

        });
    }

    /**
     * @return The selected <code>NodeContainerEditParts</code>, may be empty
     */
    protected NodeContainerEditPart[] getSelectedNodeParts() {

        return getNodeParts(getSelectedObjects());
    }

    /**
     * @return The all <code>NodeContainerEditParts</code>, may be empty
     */
    protected NodeContainerEditPart[] getAllNodeParts() {

        return getNodeParts(getAllObjects());
    }

    /*
     * @return The all <code>NodeContainerEditParts</code>, may be empty
     */
    private NodeContainerEditPart[] getNodeParts(final List nodeObjects) {
        ArrayList<NodeContainerEditPart> objects = new ArrayList<NodeContainerEditPart>(
                nodeObjects);

        // clean list, that is, remove all objects that are not edit
        // parts for a NodeContainer
        for (Iterator iter = objects.iterator(); iter.hasNext();) {
            Object element = iter.next();
            if (!(element instanceof NodeContainerEditPart)) {
                iter.remove();
                continue;
            }
        }

        final NodeContainerEditPart[] parts = objects
                .toArray(new NodeContainerEditPart[objects.size()]);

        return parts;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List getSelectedObjects() {
        ISelectionProvider provider = m_editor.getEditorSite()
                .getSelectionProvider();
        if (provider == null) {
            return Collections.EMPTY_LIST;
        }
        ISelection sel = provider.getSelection();
        if (!(sel instanceof IStructuredSelection)) {
            return Collections.EMPTY_LIST;
        }

        return ((IStructuredSelection)sel).toList();
    }

    /**
     * @return all objects of the selected editor site.
     */
    protected List getAllObjects() {

        ScrollingGraphicalViewer provider = (ScrollingGraphicalViewer)m_editor
                .getEditorSite().getSelectionProvider();
        if (provider == null) {
            return Collections.EMPTY_LIST;
        }

        // get parent of the node parts
        EditPart editorPart = (EditPart)provider.getRootEditPart()
                .getChildren().get(0);

        return editorPart.getChildren();
    }

    /**
     * Returns all edit parts with the given ids.
     *
     * @param nodeIds the node container ids to retrieve the edit parts for
     * @param connectionIds the connection container ids to retrieve the edit
     *            parts for
     * @return the edit parts of the specified ids
     */
    protected List<AbstractWorkflowEditPart> getEditPartsById(
            final int[] nodeIds, final int[] connectionIds) {

        throw new UnsupportedOperationException("This method no longer exist!");
        /*
        // the result
        ArrayList<AbstractWorkflowEditPart> parts = new ArrayList<AbstractWorkflowEditPart>();

        // get the parent from the reference part and then the children
        List<EditPart> allParts = m_editor.getViewer().getRootEditPart()
                .getContents().getChildren();

        // check all parts for the given ids
        // if a part has the appropriate id add it to the result
        for (EditPart part : allParts) {
            // get the underlying container (either connection or node)
            // representing the model of the part
            Object container = part.getModel();

            // if this is a node or connection container check the ids
            if (container instanceof NodeContainer) {
                NodeContainer nodeContainer = (NodeContainer)container;

                if (isInArray(nodeContainer.getID(), nodeIds)) {
                    parts.add((AbstractWorkflowEditPart)part);
                }
            } else if (container instanceof ConnectionContainer) {
                ConnectionContainer connectionContainer = (ConnectionContainer)container;

                if (isInArray(connectionContainer.getID(), connectionIds)) {
                    parts.add((AbstractWorkflowEditPart)part);
                }
            }
        }

        return parts;
        */

    }

    /**
     * Determines if the given value is included in the given array. Both is
     * integer typed. Helper method.
     *
     * @param value the int value to check
     * @param array the int array to check if value is included
     *
     * @return true if value is included in array
     *
    private static boolean isInArray(final int value, final int[] array) {
        if (array == null) {
            return false;
        }

        for (int arrayValue : array) {
            if (arrayValue == value) {
                return true;
            }
        }

        return false;
    }

    // /**
    // * On first update we try to hook into as workflow listener.
    // *
    // * @see org.eclipse.gef.ui.actions.UpdateAction#update()
    // */
    // public final void update() {
    // if (!m_registered) {
    // if (getManager() != null) {
    // getManager().addListener(this);
    // m_registered = true;
    // }
    // // register correct selection provider !
    // setSelectionProvider(m_editor.getSite().getSelectionProvider());
    // }
    // // call implementation of this action in the SWT UI thread
    // Display.getDefault().syncExec(new Runnable() {
    // public void run() {
    // AbstractNodeAction.super.update();
    // }
    // });
    //
    // }

    // /**
    // * We must update the action on every change
    // *
    // * @see org.knime.core.node.workflow.WorkflowListener
    // * #workflowChanged(org.knime.core.node.workflow.WorkflowEvent)
    // */
    // public void workflowChanged(final WorkflowEvent event) {
    // update();
    // }

    /**
     * This normally needs to be overridden by subclasses.
     *
     * @see org.eclipse.gef.ui.actions.WorkbenchPartAction#calculateEnabled()
     */
    @Override
    protected boolean calculateEnabled() {
        return getSelectedNodeParts().length > 0;
    }

    /**
     * Clients must provide a unique action ID.
     *
     * @see org.eclipse.jface.action.IAction#getId()
     */
    @Override
    public abstract String getId();

    /**
     * Clients must implement action code here.
     *
     * @param nodeParts The parts that the action should be executed on.
     */
    public abstract void runOnNodes(final NodeContainerEditPart[] nodeParts);

    /**
     * @return the underlying editor for this action
     */
    WorkflowEditor getEditor() {
        return m_editor;
    }
}
